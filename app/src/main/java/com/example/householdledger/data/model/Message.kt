package com.example.householdledger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String,
    @SerialName("household_id") val householdId: String,
    @SerialName("sender_id") val senderId: String,
    @SerialName("sender_name") val senderName: String = "",
    @SerialName("sender_role") val senderRole: String = "",
    val type: String = "text", // 'text', 'image', 'voice'
    val content: String = "",
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("is_read") val isRead: Boolean = false
)
