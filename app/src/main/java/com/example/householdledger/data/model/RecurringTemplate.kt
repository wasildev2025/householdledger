package com.example.householdledger.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
    @SerialName("start_date") val startDate: String = "",
    @SerialName("last_generated_date") val lastGeneratedDate: String? = null,
    @SerialName("active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String = "",
    // Local-only: computed from startDate + lastGeneratedDate + frequency at sync time
    // so the rest of the app can treat it as a simple scalar.
    @Transient val nextRun: String = ""
)
