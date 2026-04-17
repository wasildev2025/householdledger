package com.example.householdledger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "dairy_logs")
data class DairyLog(
    @PrimaryKey val id: String,
    val date: String,
    @SerialName("milk_qty") val milkQty: Double,
    @SerialName("milk_price") val milkPrice: Double,
    @SerialName("yogurt_qty") val yogurtQty: Double,
    @SerialName("yogurt_price") val yogurtPrice: Double,
    @SerialName("total_bill") val totalBill: Double,
    @SerialName("household_id") val householdId: String
)
