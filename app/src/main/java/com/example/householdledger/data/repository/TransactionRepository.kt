package com.example.householdledger.data.repository

import com.example.householdledger.data.local.TransactionDao
import com.example.householdledger.data.local.OfflineQueueDao
import com.example.householdledger.data.model.OfflineQueueItem
import com.example.householdledger.data.model.Transaction
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val offlineQueueDao: OfflineQueueDao,
    private val postgrest: Postgrest,
    private val authRepository: AuthRepository
) {
    val transactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Sync remote transactions into the local cache.
     * Role-based filtering: servants and members only see their own transactions.
     */
    suspend fun syncTransactions() {
        val profile = authRepository.currentUser.value ?: return
        val householdId = profile.householdId ?: return

        try {
            val remoteTransactions = postgrest.from("transactions")
                .select {
                    filter {
                        eq("household_id", householdId)
                        when (profile.role) {
                            "servant" -> profile.servantId?.let { eq("servant_id", it) }
                            "member" -> profile.memberId?.let { eq("member_id", it) }
                            // admin sees all
                        }
                    }
                }
                .decodeList<Transaction>()

            transactionDao.insertTransactions(remoteTransactions)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Add transaction with optimistic local insert and offline queue fallback.
     */
    suspend fun addTransaction(transaction: Transaction) {
        // Optimistic local insert
        transactionDao.insertTransaction(transaction)
        try {
            postgrest.from("transactions").insert(transaction)
        } catch (e: Exception) {
            // Queue for later sync
            offlineQueueDao.enqueue(
                OfflineQueueItem(
                    tableName = "transactions",
                    operation = "insert",
                    payload = json.encodeToString(transaction),
                    entityId = transaction.id
                )
            )
            e.printStackTrace()
        }
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
        try {
            postgrest.from("transactions").update(transaction) {
                filter { eq("id", transaction.id) }
            }
        } catch (e: Exception) {
            offlineQueueDao.enqueue(
                OfflineQueueItem(
                    tableName = "transactions",
                    operation = "update",
                    payload = json.encodeToString(transaction),
                    entityId = transaction.id
                )
            )
            e.printStackTrace()
        }
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
        try {
            postgrest.from("transactions").delete {
                filter { eq("id", transaction.id) }
            }
        } catch (e: Exception) {
            offlineQueueDao.enqueue(
                OfflineQueueItem(
                    tableName = "transactions",
                    operation = "delete",
                    payload = json.encodeToString(transaction),
                    entityId = transaction.id
                )
            )
            e.printStackTrace()
        }
    }
}
