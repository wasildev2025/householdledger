package com.example.householdledger.data.local

import androidx.room.*
import com.example.householdledger.data.model.Message
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE householdId = :householdId ORDER BY createdAt ASC")
    fun getMessages(householdId: String): Flow<List<Message>>

    @Query("SELECT COUNT(*) FROM messages WHERE householdId = :householdId AND isRead = 0 AND senderId != :currentUserId")
    fun getUnreadCount(householdId: String, currentUserId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<Message>)

    @Query("UPDATE messages SET isRead = 1 WHERE householdId = :householdId AND senderId != :currentUserId")
    suspend fun markAllRead(householdId: String, currentUserId: String)

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}
