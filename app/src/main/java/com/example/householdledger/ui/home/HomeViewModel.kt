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
import java.time.format.DateTimeParseException
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val balance: Double = 0.0,
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val monthLabel: String = "",
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
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        transactionRepository.transactions,
        categoryRepository.categories,
        authRepository.currentUser
    ) { transactions, categories, user ->
        val now = LocalDate.now()
        val categoryById = categories.associateBy { it.id }

        val thisMonth = transactions.filter { txn ->
            parseDate(txn.date)?.let { it.year == now.year && it.monthValue == now.monthValue } ?: false
        }

        val income = thisMonth.filter { it.type == "income" }.sumOf { it.amount }
        val expense = thisMonth.filter { it.type == "expense" }.sumOf { it.amount }

        val recent = transactions
            .sortedByDescending { parseDate(it.date) ?: LocalDate.MIN.atStartOfDay().toLocalDate() }
            .take(10)
            .map { TransactionRow(it, it.categoryId?.let(categoryById::get)) }

        HomeUiState(
            userName = user?.name?.substringBefore(' ') ?: "there",
            balance = income - expense,
            income = income,
            expense = expense,
            monthLabel = now.month.name.lowercase().replaceFirstChar { it.uppercase() },
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
}
