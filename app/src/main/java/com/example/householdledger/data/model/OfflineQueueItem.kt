package com.example.householdledger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a queued offline mutation that needs to be synced to the remote backend.
 * When the user performs an action while offline (add, update, delete a transaction, etc.),
 * we store it here. The SyncWorker picks these up and processes them in order.
 */
@Entity(tableName = "offline_queue")
data class OfflineQueueItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tableName: String,         // e.g. "transactions", "categories", "dairy_logs"
    val operation: String,         // "insert", "update", "delete"
    val payload: String,           // JSON-serialized entity
    val entityId: String,          // primary key of the entity
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0
)
