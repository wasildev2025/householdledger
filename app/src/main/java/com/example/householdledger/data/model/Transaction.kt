package com.example.householdledger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val amount: Double,
    val date: String,
    val type: String, // 'expense', 'income', 'transfer'
    val categoryId: String? = null,
    val servantId: String? = null,
    val memberId: String? = null,
    val description: String,
    val householdId: String? = null
)
