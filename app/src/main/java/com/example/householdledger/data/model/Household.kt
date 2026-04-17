package com.example.householdledger.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Household(
    val id: String,
    val name: String,
    val ownerId: String,
    val createdAt: String,
    val milkPrice: Double = 150.0,
    val yogurtPrice: Double = 200.0
)
