package com.example.householdledger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String = "",
    val color: String = "",
    val budget: Double = 0.0,
    @SerialName("household_id") val householdId: String
)
