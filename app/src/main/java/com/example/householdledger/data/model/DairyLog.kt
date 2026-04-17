package com.example.householdledger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "dairy_logs")
data class DairyLog(
    @PrimaryKey val id: String,
    val date: String,
    val milkQty: Double,
    val milkPrice: Double,
    val yogurtQty: Double,
    val yogurtPrice: Double,
    val totalBill: Double,
    val householdId: String
)
