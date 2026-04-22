package com.example.householdledger.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Household(
    val id: String,
    val name: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("milk_price") val milkPrice: Double = 150.0,
    @SerialName("yogurt_price") val yogurtPrice: Double = 200.0,
    @SerialName("cycle_start_day") val cycleStartDay: Int = 1,
    @SerialName("monthly_budget") val monthlyBudget: Double = 0.0
)
