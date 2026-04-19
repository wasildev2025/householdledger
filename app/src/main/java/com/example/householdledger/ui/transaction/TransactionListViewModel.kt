package com.example.householdledger.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Category
import com.example.householdledger.data.model.Transaction
import com.example.householdledger.data.local.PreferenceManager
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.CategoryRepository
import com.example.householdledger.data.repository.TransactionRepository
import com.example.householdledger.util.Cycle
import com.example.householdledger.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class TxnFilter { All, Expense, Income, Transfer }
enum class PersonFilter { All, Admin, Servants, Members }

data class TxnListState(
    val items: List<TxnListRow> = emptyList(),
    val sections: List<DaySection> = emptyList(),
    val categorySlices: List<CategorySliceVm> = emptyList(),
    val monthSpend: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthTransfers: Double = 0.0,
    val cycleLabel: String = "",
    val cycleDayIndex: Int = 0,
    val cycleLengthDays: Int = 30,
    val filter: TxnFilter = TxnFilter.All,
    val personFilter: PersonFilter = PersonFilter.All,
    val visibleCount: Int = PAGE_SIZE,
    val isRefreshing: Boolean = false,
    val isAdmin: Boolean = false,
    val totalMatching: Int = 0
) {
    val canLoadMore: Boolean get() = visibleCount < totalMatching
}

data class TxnListRow(
    val transaction: Transaction,
    val category: Category?
)

data class DaySection(
    val label: String,       // "Today", "Yesterday", "Mon, 14 Apr"
    val total: Double,       // net for the day (income - expense)
    val rows: List<TxnListRow>
)

data class CategorySliceVm(
    val name: String,
    val colorHex: String?,
    val total: Double,
    val share: Float         // 0..1
)

const val PAGE_SIZE = 25

@HiltViewModel
class TransactionListViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    authRepository: AuthRepository,
    preferenceManager: PreferenceManager
) : ViewModel() {

    private val filter = MutableStateFlow(TxnFilter.All)
    private val personFilter = MutableStateFlow(PersonFilter.All)
    private val visibleCount = MutableStateFlow(PAGE_SIZE)
    private val refreshing = MutableStateFlow(false)

    val state: StateFlow<TxnListState> = combineSix(
        transactionRepository.transactions,
        categoryRepository.categories,
        authRepository.currentUser,
        filter,
        personFilter,
        preferenceManager.cycleStartDay
    ) { txns, cats, user, f, pf, cycleStartDay ->
        val categoryById = cats.associateBy { it.id }
        val filteredType = when (f) {
            TxnFilter.All -> txns
            TxnFilter.Expense -> txns.filter { it.type == "expense" }
            TxnFilter.Income -> txns.filter { it.type == "income" }
            TxnFilter.Transfer -> txns.filter { it.type == "transfer" }
        }
        val filtered = when (pf) {
            PersonFilter.All -> filteredType
            PersonFilter.Admin -> filteredType.filter { it.servantId == null && it.memberId == null }
            PersonFilter.Servants -> filteredType.filter { it.servantId != null }
            PersonFilter.Members -> filteredType.filter { it.memberId != null }
        }.sortedByDescending { parseDate(it.date) ?: LocalDate.MIN }

        val count = visibleCount.value
        val items = filtered.take(count).map { TxnListRow(it, it.categoryId?.let(categoryById::get)) }

        val sections = buildDaySections(items)

        // Category breakdown for the current CYCLE (unaffected by pagination)
        val cycle = Cycle.current(LocalDate.now(), cycleStartDay)
        val cycleTxns = filtered.filter { parseDate(it.date)?.let(cycle::contains) == true }
        val monthSpend = cycleTxns.filter { it.type == "expense" }.sumOf { it.amount }
        val monthIncome = cycleTxns.filter { it.type == "income" }.sumOf { it.amount }
        val monthTransfers = cycleTxns.filter { it.type == "transfer" }.sumOf { it.amount }

        val byCat = cycleTxns.filter { it.type == "expense" }
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val cat = catId?.let(categoryById::get)
                val total = txns.sumOf { it.amount }
                CategorySliceVm(
                    name = cat?.name ?: "Uncategorized",
                    colorHex = cat?.color,
                    total = total,
                    share = if (monthSpend > 0) (total / monthSpend).toFloat() else 0f
                )
            }.sortedByDescending { it.total }

        val cycleLengthDays = java.time.temporal.ChronoUnit.DAYS
            .between(cycle.start, cycle.endExclusive).toInt().coerceAtLeast(1)
        val dayIndex = java.time.temporal.ChronoUnit.DAYS
            .between(cycle.start, LocalDate.now()).toInt()
            .coerceIn(0, cycleLengthDays - 1)
        val fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM")
        val cycleLabel = "${cycle.start.format(fmt)} – ${cycle.endExclusive.minusDays(1).format(fmt)}"

        TxnListState(
            items = items,
            sections = sections,
            categorySlices = byCat,
            monthSpend = monthSpend,
            monthIncome = monthIncome,
            monthTransfers = monthTransfers,
            cycleLabel = cycleLabel,
            cycleDayIndex = dayIndex,
            cycleLengthDays = cycleLengthDays,
            filter = f,
            personFilter = pf,
            visibleCount = count,
            isRefreshing = refreshing.value,
            isAdmin = user?.role == "admin",
            totalMatching = filtered.size
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TxnListState())

    private fun buildDaySections(items: List<TxnListRow>): List<DaySection> {
        val today = LocalDate.now()
        val groups = items.groupBy { parseDate(it.transaction.date) ?: LocalDate.MIN }
        return groups.entries
            .sortedByDescending { it.key }
            .map { (date, rows) ->
                val income = rows.filter { it.transaction.type == "income" }.sumOf { it.transaction.amount }
                val expense = rows.filter { it.transaction.type == "expense" }.sumOf { it.transaction.amount }
                DaySection(
                    label = dayLabel(date, today),
                    total = income - expense,
                    rows = rows
                )
            }
    }

    private fun dayLabel(date: LocalDate, today: LocalDate): String = when {
        date == LocalDate.MIN -> "—"
        date == today -> "Today"
        date == today.minusDays(1) -> "Yesterday"
        date.year == today.year ->
            date.format(java.time.format.DateTimeFormatter.ofPattern("EEE, d MMM"))
        else ->
            date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
    }

    init { refresh() }

    fun setFilter(f: TxnFilter) {
        filter.value = f
        visibleCount.value = PAGE_SIZE
    }

    fun setPersonFilter(pf: PersonFilter) {
        personFilter.value = pf
        visibleCount.value = PAGE_SIZE
    }

    fun loadMore() {
        val s = state.value
        if (s.canLoadMore) visibleCount.value = (visibleCount.value + PAGE_SIZE).coerceAtMost(s.totalMatching)
    }

    fun refresh() {
        viewModelScope.launch {
            refreshing.value = true
            transactionRepository.syncTransactions()
            refreshing.value = false
        }
    }

    fun delete(transaction: Transaction) {
        viewModelScope.launch { transactionRepository.deleteTransaction(transaction) }
    }

    private fun parseDate(raw: String): LocalDate? = DateUtil.parseDate(raw)
}

@Suppress("UNCHECKED_CAST")
private fun <T1, T2, T3, T4, T5, T6, R> combineSix(
    f1: Flow<T1>,
    f2: Flow<T2>,
    f3: Flow<T3>,
    f4: Flow<T4>,
    f5: Flow<T5>,
    f6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R
): Flow<R> = combine(f1, f2, f3, f4, f5, f6) { values ->
    transform(
        values[0] as T1,
        values[1] as T2,
        values[2] as T3,
        values[3] as T4,
        values[4] as T5,
        values[5] as T6
    )
}
