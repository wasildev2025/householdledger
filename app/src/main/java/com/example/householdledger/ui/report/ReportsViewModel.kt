package com.example.householdledger.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Category
import com.example.householdledger.data.model.Transaction
import com.example.householdledger.data.repository.CategoryRepository
import com.example.householdledger.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject
import kotlin.math.max

data class CategoryBreakdown(
    val category: Category?,
    val categoryName: String,
    val total: Double,
    val share: Float,
    val colorHex: String?,
    val icon: String
)

data class ReportsUiState(
    val month: YearMonth = YearMonth.now(),
    val canGoForward: Boolean = false,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0,
    val dailyExpense: List<Double> = emptyList(),
    val peakDailyExpense: Double = 0.0,
    val breakdown: List<CategoryBreakdown> = emptyList(),
    val transactionCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<ReportsUiState> = combine(
        transactionRepository.transactions,
        categoryRepository.categories,
        selectedMonth
    ) { transactions, categories, month ->
        val categoryById = categories.associateBy { it.id }
        val daysInMonth = month.lengthOfMonth()

        val monthTxns = transactions.filter { txn ->
            parseDate(txn.date)?.let { YearMonth.of(it.year, it.monthValue) == month } ?: false
        }

        val income = monthTxns.filter { it.type == "income" }.sumOf { it.amount }
        val expense = monthTxns.filter { it.type == "expense" }.sumOf { it.amount }

        val daily = DoubleArray(daysInMonth)
        monthTxns.filter { it.type == "expense" }.forEach { txn ->
            parseDate(txn.date)?.let { date ->
                val idx = date.dayOfMonth - 1
                if (idx in 0 until daysInMonth) daily[idx] += txn.amount
            }
        }

        val totalExpense = max(expense, 1.0)
        val breakdown = monthTxns
            .filter { it.type == "expense" }
            .groupBy { it.categoryId }
            .map { (catId, txns) ->
                val cat = catId?.let(categoryById::get)
                val total = txns.sumOf { it.amount }
                CategoryBreakdown(
                    category = cat,
                    categoryName = cat?.name ?: "Uncategorized",
                    total = total,
                    share = (total / totalExpense).toFloat().coerceIn(0f, 1f),
                    colorHex = cat?.color,
                    icon = cat?.icon?.ifBlank { (cat.name.take(1).uppercase()) } ?: "•"
                )
            }
            .sortedByDescending { it.total }

        ReportsUiState(
            month = month,
            canGoForward = month < YearMonth.now(),
            income = income,
            expense = expense,
            balance = income - expense,
            dailyExpense = daily.toList(),
            peakDailyExpense = daily.maxOrNull() ?: 0.0,
            breakdown = breakdown,
            transactionCount = monthTxns.size,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    fun shiftMonth(delta: Int) {
        val next = selectedMonth.value.plusMonths(delta.toLong())
        if (next <= YearMonth.now()) selectedMonth.value = next
    }

    fun formatMonthLabel(month: YearMonth): String =
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))

    private fun parseDate(raw: String): LocalDate? = try {
        LocalDateTime.parse(raw).toLocalDate()
    } catch (_: DateTimeParseException) {
        try { LocalDate.parse(raw) } catch (_: DateTimeParseException) { null }
    }
}
