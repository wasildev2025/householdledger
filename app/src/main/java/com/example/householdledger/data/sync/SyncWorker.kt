package com.example.householdledger.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.householdledger.data.local.OfflineQueueDao
import com.example.householdledger.data.model.Transaction
import com.example.householdledger.data.model.DairyLog
import com.example.householdledger.data.model.Category
import com.example.householdledger.data.repository.AuthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.Json

/**
 * WorkManager worker that processes the offline mutation queue.
 * Picks up pending insert/update/delete operations and syncs them to Supabase.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val offlineQueueDao: OfflineQueueDao,
    private val postgrest: Postgrest,
    private val authRepository: AuthRepository
) : CoroutineWorker(context, workerParams) {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun doWork(): Result {
        // Remove items that have exceeded max retries
        offlineQueueDao.removeStaleItems(10)

        val pendingItems = offlineQueueDao.getAllPending()
        if (pendingItems.isEmpty()) return Result.success()

        var hasFailure = false

        for (item in pendingItems) {
            try {
                when (item.tableName) {
                    "transactions" -> processTransaction(item.operation, item.payload, item.entityId)
                    "categories" -> processCategory(item.operation, item.payload, item.entityId)
                    "dairy_logs" -> processDairyLog(item.operation, item.payload, item.entityId)
                }
                // Success — remove from queue
                offlineQueueDao.dequeue(item)
            } catch (e: Exception) {
                e.printStackTrace()
                offlineQueueDao.incrementRetry(item.id)
                hasFailure = true
            }
        }

        return if (hasFailure) Result.retry() else Result.success()
    }

    private suspend fun processTransaction(operation: String, payload: String, entityId: String) {
        when (operation) {
            "insert" -> {
                val transaction = json.decodeFromString<Transaction>(payload)
                postgrest.from("transactions").insert(transaction)
            }
            "update" -> {
                val transaction = json.decodeFromString<Transaction>(payload)
                postgrest.from("transactions").update(transaction) {
                    filter { eq("id", entityId) }
                }
            }
            "delete" -> {
                postgrest.from("transactions").delete {
                    filter { eq("id", entityId) }
                }
            }
        }
    }

    private suspend fun processCategory(operation: String, payload: String, entityId: String) {
        when (operation) {
            "insert" -> {
                val category = json.decodeFromString<Category>(payload)
                postgrest.from("categories").insert(category)
            }
            "update" -> {
                val category = json.decodeFromString<Category>(payload)
                postgrest.from("categories").update(category) {
                    filter { eq("id", entityId) }
                }
            }
            "delete" -> {
                postgrest.from("categories").delete {
                    filter { eq("id", entityId) }
                }
            }
        }
    }

    private suspend fun processDairyLog(operation: String, payload: String, entityId: String) {
        when (operation) {
            "insert" -> {
                val log = json.decodeFromString<DairyLog>(payload)
                postgrest.from("dairy_logs").insert(log)
            }
            "delete" -> {
                postgrest.from("dairy_logs").delete {
                    filter { eq("id", entityId) }
                }
            }
        }
    }

    companion object {
        const val WORK_NAME = "offline_sync_worker"
    }
}
