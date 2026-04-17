package com.example.householdledger.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Transaction
import com.example.householdledger.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    val transactions: Flow<List<Transaction>> = transactionRepository.transactions

    init {
        refreshTransactions()
    }

    fun refreshTransactions() {
        viewModelScope.launch {
            transactionRepository.syncTransactions()
        }
    }
}
