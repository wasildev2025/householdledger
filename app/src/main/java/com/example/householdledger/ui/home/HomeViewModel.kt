package com.example.householdledger.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Category
import com.example.householdledger.data.model.Transaction
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.CategoryRepository
import com.example.householdledger.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val today: String = "",
    val balance: Double = 0.0,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val incomeDeltaPercent: Float = 0f,
    val expenseDeltaPercent: Float = 0f,
    val monthLabel: String = "",
    val budgetCap: Double = 0.0,
    val budgetUsedPercent: Float = 0f,
    val recent: List<TransactionRow> = emptyList(),
    val isLoading: Boolean = true
)

data class TransactionRow(
    val transaction: Transaction,
    val category: Category?
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    categoryRepository: CategoryRepository,
    authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        transactionRepository.transactions,
        categoryRepository.categories,
        authRepository.currentUser
    ) { transactions, categories, user ->
        val now = LocalDate.now()
        val thisMonth = YearMonth.of(now.year, now.monthValue)
        val prevMonth = thisMonth.minusMonths(1)
        val categoryById = categories.associateBy { it.id }

        val thisMonthTxns = transactions.filter { parsedMonth(it.date) == thisMonth }
        val prevMonthTxns = transactions.filter { parsedMonth(it.date) == prevMonth }

        val income = thisMonthTxns.filter { it.type == "income" }.sumOf { it.amount }
        val expense = thisMonthTxns.filter { it.type == "expense" }.sumOf { it.amount }
        val prevIncome = prevMonthTxns.filter { it.type == "income" }.sumOf { it.amount }
        val prevExpense = prevMonthTxns.filter { it.type == "expense" }.sumOf { it.amount }

        val recent = transactions
            .sortedByDescending { parseDate(it.date) ?: LocalDate.MIN.atStartOfDay().toLocalDate() }
            .take(10)
            .map { TransactionRow(it, it.categoryId?.let(categoryById::get)) }

        val budgetCap = if (income > 0) income else 0.0
        val budgetUsed = if (budgetCap > 0) (expense / budgetCap).toFloat().coerceIn(0f, 1.5f) else 0f

        HomeUiState(
            userName = user?.name?.substringBefore(' ') ?: "there",
            today = now.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy")),
            balance = income - expense,
            income = income,
            expense = expense,
            incomeDeltaPercent = deltaPercent(income, prevIncome),
            expenseDeltaPercent = deltaPercent(expense, prevExpense),
            monthLabel = now.month.name.lowercase().replaceFirstChar { it.uppercase() },
            budgetCap = budgetCap,
            budgetUsedPercent = budgetUsed,
            recent = recent,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            transactionRepository.syncTransactions()
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
}
