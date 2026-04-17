package com.example.householdledger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "recurring_templates")
data class RecurringTemplate(
    @PrimaryKey val id: String,
    @SerialName("household_id") val householdId: String,
    val amount: Double,
    val type: String, // 'expense', 'income', 'transfer'
    @SerialName("category_id") val categoryId: String? = null,
    val description: String = "",
    val frequency: String = "monthly", // 'daily', 'weekly', 'monthly'
    @SerialName("servant_id") val servantId: String? = null,
    @SerialName("member_id") val memberId: String? = null,
    @SerialName("next_run") val nextRun: String = "",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = ""
)
