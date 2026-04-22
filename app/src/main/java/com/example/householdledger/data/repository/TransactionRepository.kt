package com.example.householdledger.data.repository

import android.util.Log
import com.example.householdledger.data.local.TransactionDao
import com.example.householdledger.data.local.OfflineQueueDao
import com.example.householdledger.data.model.OfflineQueueItem
import com.example.householdledger.data.model.Transaction
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao,
    private val offlineQueueDao: OfflineQueueDao,
    private val postgrest: Postgrest,
    private val realtime: Realtime,
    private val authRepository: AuthRepository
) {
    val transactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var realtimeJob: Job? = null

    fun subscribeRealtime() {
        val householdId = authRepository.currentUser.value?.householdId ?: return
        realtimeJob?.cancel()
        realtimeJob = scope.launch {
            try {
                val channel = realtime.channel("transactions:$householdId")
                val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "transactions"
                }
                channel.subscribe()
                changes.catch { it.printStackTrace() }.collect { _ ->
                    syncTransactions()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unsubscribeRealtime() {
        realtimeJob?.cancel()
        realtimeJob = null
    }

    /**
     * Sync remote transactions into the local cache.
     * Role-based filtering: servants and members only see their own transactions.
     */
    suspend fun syncTransactions() {
        val profile = authRepository.currentUser.value
        if (profile == null) { Log.w(TAG, "syncTransactions: no profile"); return }
        val householdId = profile.householdId
        if (householdId == null) { Log.w(TAG, "syncTransactions: no householdId"); return }

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

            Log.d(TAG, "syncTransactions: fetched ${remoteTransactions.size} rows for household=$householdId role=${profile.role}")
            
            // Fix for #6: Clear local transactions and replace with remote to handle deletions
            transactionDao.clearAll()
            transactionDao.insertTransactions(remoteTransactions)
        } catch (e: Exception) {
            Log.e(TAG, "syncTransactions failed", e)
        }
    }

    companion object { private const val TAG = "TxRepo" }

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
