package com.example.householdledger.data.local

import androidx.room.*
import com.example.householdledger.data.model.OfflineQueueItem

@Dao
interface OfflineQueueDao {
    @Query("SELECT * FROM offline_queue ORDER BY createdAt ASC")
    suspend fun getAllPending(): List<OfflineQueueItem>

    @Query("SELECT COUNT(*) FROM offline_queue")
    suspend fun getPendingCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueue(item: OfflineQueueItem)

    @Delete
    suspend fun dequeue(item: OfflineQueueItem)

    @Query("UPDATE offline_queue SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetry(id: Long)

    @Query("DELETE FROM offline_queue WHERE retryCount >= :maxRetries")
    suspend fun removeStaleItems(maxRetries: Int = 10)

    @Query("DELETE FROM offline_queue")
    suspend fun clearAll()
}
