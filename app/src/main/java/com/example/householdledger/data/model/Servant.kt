package com.example.householdledger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "servants")
data class Servant(
    @PrimaryKey val id: String,
    val name: String,
    val role: String,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val salary: Double? = null,
    val budget: Double? = null,
    val pin: String? = null,
    val balance: Double = 0.0,
    @SerialName("invite_code") val inviteCode: String? = null,
    @SerialName("household_id") val householdId: String? = null
)
