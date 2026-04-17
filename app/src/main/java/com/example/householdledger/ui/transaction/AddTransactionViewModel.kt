package com.example.householdledger.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Category
import com.example.householdledger.data.model.Transaction
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.CategoryRepository
import com.example.householdledger.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val categories: StateFlow<List<Category>> = categoryRepository.categories.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    fun addTransaction(
        amount: Double,
        description: String,
        type: String,
        categoryId: String?,
        date: String
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val profile = authRepository.currentUser.value
            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                amount = amount,
                description = description,
                type = type,
                categoryId = categoryId,
                date = date,
                householdId = profile?.householdId,
                servantId = profile?.servantId,
                memberId = profile?.memberId
            )
            transactionRepository.addTransaction(transaction)
            _isSaving.value = false
        }
    }
}
