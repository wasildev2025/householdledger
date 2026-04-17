package com.example.householdledger.data.repository

import com.example.householdledger.data.local.TransactionDao
import com.example.householdledger.data.model.Transaction
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val postgrest: Postgrest,
    private val authRepository: AuthRepository
) {
    val transactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun syncTransactions() {
        val profile = authRepository.currentUser.value ?: return
        val householdId = profile.householdId ?: return

        try {
            val remoteTransactions = postgrest.from("transactions")
                .select {
                    filter {
                        eq("household_id", householdId)
                    }
                }
                .decodeList<Transaction>()
            
            transactionDao.insertTransactions(remoteTransactions)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
        try {
            postgrest.from("transactions").insert(transaction)
        } catch (e: Exception) {
            // Handle offline/error (could add to an offline queue like the React app)
            e.printStackTrace()
        }
    }
}
