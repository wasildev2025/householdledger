package com.example.householdledger.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Category
import com.example.householdledger.data.model.Member
import com.example.householdledger.data.model.RecurringTemplate
import com.example.householdledger.data.model.Servant
import com.example.householdledger.data.model.Transaction
import com.example.householdledger.data.local.PreferenceManager
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.CategoryRepository
import com.example.householdledger.data.repository.PeopleRepository
import com.example.householdledger.data.repository.RecurringRepository
import com.example.householdledger.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

enum class HomeFilter { All, Servants, Members }

data class WalletSummary(
    val id: String,
    val name: String,
    val balance: Double,
    val monthlySpend: Double,
    val allocation: Double,
    val kind: String // "servant" | "member"
)

data class HomeUiState(
    val userName: String = "",
    val role: String = "member",
    val today: String = "",
    val balance: Double = 0.0,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val transfers: Double = 0.0,
    val incomeDeltaPercent: Float = 0f,
    val expenseDeltaPercent: Float = 0f,
    val monthLabel: String = "",
    val budgetCap: Double = 0.0,
    val budgetUsedPercent: Float = 0f,
    val recent: List<TransactionRow> = emptyList(),
    val wallets: List<WalletSummary> = emptyList(),
    val upcomingBills: List<UpcomingBill> = emptyList(),
    val filter: HomeFilter = HomeFilter.All,
    val unreadMessages: Int = 0,
    val isOffline: Boolean = false,
    val isLoading: Boolean = true,
    val aiInsight: String? = null
)

data class TransactionRow(
    val transaction: Transaction,
    val category: Category?
)

data class UpcomingBill(
    val id: String,
    val name: String,
    val amount: Double,
    val dueDate: String,           // "Tomorrow", "Thu, 24 Apr"
    val daysUntil: Int,
    val categoryColorHex: String?
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val peopleRepository: PeopleRepository,
    private val recurringRepository: RecurringRepository,
    private val authRepository: AuthRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val filter = MutableStateFlow(HomeFilter.All)
    val filterFlow: StateFlow<HomeFilter> = filter.asStateFlow()

    private val aiInsight = MutableStateFlow<String?>(null)
    private val isOffline = MutableStateFlow(false)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private val upcomingBillsFlow: Flow<List<UpcomingBill>> = authRepository.currentUser
        .let { userFlow ->
            kotlinx.coroutines.flow.flow {
                userFlow.collect { user ->
                    if (user?.householdId == null) {
                        emit(emptyList())
                    } else {
                        recurringRepository.upcomingBills(user.householdId, days = 14)
                            .collect { templates -> emit(mapUpcoming(templates)) }
                    }
                }
            }
        }

    val uiState: StateFlow<HomeUiState> = combineN(
        transactionRepository.transactions,
        categoryRepository.categories,
        peopleRepository.servants,
        peopleRepository.members,
        authRepository.currentUser,
        filter,
        preferenceManager.monthlyBudget,
        aiInsight,
        isOffline,
        upcomingBillsFlow
    ) { arr ->
        @Suppress("UNCHECKED_CAST") val transactions = arr[0] as List<Transaction>
        @Suppress("UNCHECKED_CAST") val categories = arr[1] as List<Category>
        @Suppress("UNCHECKED_CAST") val servants = arr[2] as List<Servant>
        @Suppress("UNCHECKED_CAST") val members = arr[3] as List<Member>
        val user = arr[4] as com.example.householdledger.data.model.UserProfile?
        val f = arr[5] as HomeFilter
        val userBudget = arr[6] as Double
        val insight = arr[7] as String?
        val offline = arr[8] as Boolean
        @Suppress("UNCHECKED_CAST") val upcoming = arr[9] as List<UpcomingBill>

        val now = LocalDate.now()
        val thisMonth = YearMonth.of(now.year, now.monthValue)
        val prevMonth = thisMonth.minusMonths(1)
        val categoryById = categories.associateBy { it.id }

        // Role-based scoping
        val scoped = when (user?.role) {
            "servant" -> transactions.filter { it.servantId == user.servantId }
            "member" -> transactions.filter { it.memberId == user.memberId }
            else -> transactions
        }

        val byFilter = when (f) {
            HomeFilter.All -> scoped
            HomeFilter.Servants -> scoped.filter { it.servantId != null }
            HomeFilter.Members -> scoped.filter { it.memberId != null }
        }

        val thisMonthTxns = byFilter.filter { parsedMonth(it.date) == thisMonth }
        val prevMonthTxns = byFilter.filter { parsedMonth(it.date) == prevMonth }

        val income = thisMonthTxns.filter { it.type == "income" }.sumOf { it.amount }
        val expense = thisMonthTxns.filter { it.type == "expense" }.sumOf { it.amount }
        val transfers = thisMonthTxns.filter { it.type == "transfer" }.sumOf { it.amount }
        val prevIncome = prevMonthTxns.filter { it.type == "income" }.sumOf { it.amount }
        val prevExpense = prevMonthTxns.filter { it.type == "expense" }.sumOf { it.amount }

        val recent = byFilter
            .sortedByDescending { parseDate(it.date) ?: LocalDate.MIN.atStartOfDay().toLocalDate() }
            .take(8)
            .map { TransactionRow(it, it.categoryId?.let(categoryById::get)) }

        val budgetCap = if (userBudget > 0) userBudget else income.takeIf { it > 0 } ?: 0.0
        val budgetUsed = if (budgetCap > 0) (expense / budgetCap).toFloat().coerceIn(0f, 1.5f) else 0f

        val wallets = buildWallets(servants, members, thisMonthTxns, f)

        HomeUiState(
            userName = user?.name?.substringBefore(' ') ?: "there",
            role = user?.role ?: "member",
            today = now.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")),
            balance = income - expense,
            income = income,
            expense = expense,
            transfers = transfers,
            incomeDeltaPercent = deltaPercent(income, prevIncome),
            expenseDeltaPercent = deltaPercent(expense, prevExpense),
            monthLabel = now.month.name.lowercase().replaceFirstChar { it.uppercase() },
            budgetCap = budgetCap,
            budgetUsedPercent = budgetUsed,
            recent = recent,
            wallets = wallets,
            upcomingBills = upcoming,
            filter = f,
            isOffline = offline,
            isLoading = false,
            aiInsight = insight
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        refresh()
    }

    fun setFilter(value: HomeFilter) { filter.value = value }
    fun setOffline(offline: Boolean) { isOffline.value = offline }
    fun setInsight(text: String?) { aiInsight.value = text }

    fun refresh() {
        viewModelScope.launch {
            transactionRepository.syncTransactions()
            categoryRepository.syncCategories()
            peopleRepository.syncPeople()
        }
    }

    private fun buildWallets(
        servants: List<Servant>,
        members: List<Member>,
        monthTxns: List<Transaction>,
        filter: HomeFilter
    ): List<WalletSummary> {
        val servantSpend = monthTxns.groupBy { it.servantId }.mapValues { (_, v) -> v.sumOf { it.amount } }
        val memberSpend = monthTxns.groupBy { it.memberId }.mapValues { (_, v) -> v.sumOf { it.amount } }

        val servantWallets = servants.map { s ->
            WalletSummary(
                id = s.id,
                name = s.name,
                balance = s.balance,
                monthlySpend = servantSpend[s.id] ?: 0.0,
                allocation = s.budget ?: 0.0,
                kind = "servant"
            )
        }
        val memberWallets = members.map { m ->
            WalletSummary(
                id = m.id,
                name = m.name,
                balance = 0.0,
                monthlySpend = memberSpend[m.id] ?: 0.0,
                allocation = 0.0,
                kind = "member"
            )
        }
        return when (filter) {
            HomeFilter.All -> servantWallets + memberWallets
            HomeFilter.Servants -> servantWallets
            HomeFilter.Members -> memberWallets
        }
    }

    private fun parseDate(raw: String): LocalDate? = try {
        LocalDateTime.parse(raw).toLocalDate()
    } catch (_: DateTimeParseException) {
        try { LocalDate.parse(raw) } catch (_: DateTimeParseException) { null }
    }

    private fun parsedMonth(raw: String): YearMonth? =
        parseDate(raw)?.let { YearMonth.of(it.year, it.monthValue) }

    private fun deltaPercent(current: Double, previous: Double): Float {
        if (previous <= 0) return if (current > 0) 100f else 0f
        return (((current - previous) / previous) * 100).toFloat()
    }

    private fun mapUpcoming(templates: List<RecurringTemplate>): List<UpcomingBill> {
        val today = LocalDate.now()
        return templates.mapNotNull { t ->
            val next = runCatching { LocalDate.parse(t.nextRun) }.getOrNull() ?: return@mapNotNull null
            val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, next).toInt()
            val label = when (daysUntil) {
                0 -> "Today"
                1 -> "Tomorrow"
                in 2..6 -> next.format(DateTimeFormatter.ofPattern("EEE"))
                else -> next.format(DateTimeFormatter.ofPattern("d MMM"))
            }
            UpcomingBill(
                id = t.id,
                name = t.description.ifBlank { "Recurring ${t.type}" },
                amount = t.amount,
                dueDate = label,
                daysUntil = daysUntil,
                categoryColorHex = null
            )
        }
    }
}

@Suppress("UNCHECKED_CAST")
private fun <R> combineN(
    vararg flows: Flow<*>,
    transform: suspend (Array<Any?>) -> R
): Flow<R> {
    val typed: Array<Flow<Any?>> = flows.map { it as Flow<Any?> }.toTypedArray()
    return kotlinx.coroutines.flow.combine(*typed) { values: Array<Any?> ->
        transform(values)
    }
}

