package com.example.householdledger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "members")
data class Member(
    @PrimaryKey val id: String,
    val name: String,
    val role: String = "member",
    val pin: String? = null,
    val householdId: String? = null,
    val inviteCode: String? = null
)
