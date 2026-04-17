package com.example.householdledger.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String? = null,
    val role: String, // 'admin', 'servant', 'member'
    @SerialName("servant_id") val servantId: String? = null,
    @SerialName("member_id") val memberId: String? = null,
    @SerialName("household_id") val householdId: String? = null,
    val name: String
)
