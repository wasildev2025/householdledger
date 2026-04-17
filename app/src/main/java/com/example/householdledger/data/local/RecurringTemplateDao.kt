package com.example.householdledger.data.local

import androidx.room.*
import com.example.householdledger.data.model.RecurringTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTemplateDao {
    @Query("SELECT * FROM recurring_templates WHERE householdId = :householdId ORDER BY createdAt DESC")
    fun getTemplates(householdId: String): Flow<List<RecurringTemplate>>

    @Query("SELECT * FROM recurring_templates WHERE isActive = 1 AND nextRun <= :dateStr")
    suspend fun getDueTemplates(dateStr: String): List<RecurringTemplate>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: RecurringTemplate)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<RecurringTemplate>)

    @Update
    suspend fun updateTemplate(template: RecurringTemplate)

    @Delete
    suspend fun deleteTemplate(template: RecurringTemplate)

    @Query("DELETE FROM recurring_templates")
    suspend fun clearAll()
}
