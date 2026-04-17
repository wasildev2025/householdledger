package com.example.householdledger.data.repository

import com.example.householdledger.data.local.RecurringTemplateDao
import com.example.householdledger.data.model.RecurringTemplate
import com.example.householdledger.data.model.Transaction
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecurringRepository @Inject constructor(
    private val recurringDao: RecurringTemplateDao,
    private val transactionRepository: TransactionRepository,
    private val postgrest: Postgrest,
    private val authRepository: AuthRepository
) {
    fun getTemplates(householdId: String): Flow<List<RecurringTemplate>> {
        return recurringDao.getTemplates(householdId)
    }

    suspend fun syncTemplates() {
        val profile = authRepository.currentUser.value ?: return
        val householdId = profile.householdId ?: return
        try {
            val remote = postgrest.from("recurring_templates")
                .select {
                    filter { eq("household_id", householdId) }
                }
                .decodeList<RecurringTemplate>()
            recurringDao.insertTemplates(remote)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addTemplate(template: RecurringTemplate) {
        recurringDao.insertTemplate(template)
        try {
            postgrest.from("recurring_templates").insert(template)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteTemplate(template: RecurringTemplate) {
        recurringDao.deleteTemplate(template)
        try {
            postgrest.from("recurring_templates").delete {
                filter { eq("id", template.id) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Process all due recurring templates. Creates transactions and advances nextRun.
     * Called from RecurringWorker on a daily schedule.
     */
    suspend fun processDueTemplates() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dueTemplates = recurringDao.getDueTemplates(today)

        for (template in dueTemplates) {
            val transaction = Transaction(
                id = UUID.randomUUID().toString(),
                amount = template.amount,
                date = today,
                type = template.type,
                categoryId = template.categoryId,
                description = template.description,
                householdId = template.householdId,
                servantId = template.servantId,
                memberId = template.memberId,
                createdAt = java.time.Instant.now().toString()
            )
            transactionRepository.addTransaction(transaction)

            // Advance the nextRun date
            val nextRunDate = calculateNextRun(template.nextRun, template.frequency)
            val updated = template.copy(nextRun = nextRunDate)
            recurringDao.updateTemplate(updated)
            try {
                postgrest.from("recurring_templates").update(
                    mapOf("next_run" to nextRunDate)
                ) {
                    filter { eq("id", template.id) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateNextRun(currentNextRun: String, frequency: String): String {
        val date = try {
            LocalDate.parse(currentNextRun)
        } catch (_: Exception) {
            LocalDate.now()
        }
        val nextDate = when (frequency) {
            "daily" -> date.plusDays(1)
            "weekly" -> date.plusWeeks(1)
            "monthly" -> date.plusMonths(1)
            else -> date.plusMonths(1)
        }
        return nextDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}
