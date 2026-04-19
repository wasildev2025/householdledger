package com.example.householdledger.data.repository

import android.util.Log
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

    /**
     * Flow of active recurring templates whose `nextRun` falls within the next
     * `days` days. Used by the Upcoming Bills section on Home.
     */
    fun upcomingBills(householdId: String, days: Int = 14): Flow<List<RecurringTemplate>> {
        return kotlinx.coroutines.flow.combine(
            recurringDao.getTemplates(householdId),
            kotlinx.coroutines.flow.flowOf(LocalDate.now())
        ) { templates, today ->
            val cutoff = today.plusDays(days.toLong())
            templates.filter { t ->
                if (!t.isActive) return@filter false
                val next = runCatching { LocalDate.parse(t.nextRun) }.getOrNull() ?: return@filter false
                !next.isBefore(today) && !next.isAfter(cutoff)
            }.sortedBy { it.nextRun }
        }
    }

    suspend fun syncTemplates() {
        val profile = authRepository.currentUser.value
        if (profile == null) { Log.w(TAG, "syncTemplates: no profile"); return }
        val householdId = profile.householdId
        if (householdId == null) { Log.w(TAG, "syncTemplates: no householdId"); return }
        try {
            val remote = postgrest.from("recurring_transactions")
                .select {
                    filter { eq("household_id", householdId) }
                }
                .decodeList<RecurringTemplate>()
            val withNextRun = remote.map { it.copy(nextRun = computeNextRun(it.startDate, it.lastGeneratedDate, it.frequency)) }
            Log.d(TAG, "syncTemplates: fetched ${withNextRun.size} rows for household=$householdId")
            recurringDao.insertTemplates(withNextRun)
        } catch (e: Exception) {
            Log.e(TAG, "syncTemplates failed", e)
        }
    }

    suspend fun addTemplate(template: RecurringTemplate) {
        val withNextRun = template.copy(
            nextRun = computeNextRun(template.startDate, template.lastGeneratedDate, template.frequency)
        )
        recurringDao.insertTemplate(withNextRun)
        try {
            postgrest.from("recurring_transactions").insert(withNextRun)
        } catch (e: Exception) {
            Log.e(TAG, "addTemplate failed", e)
        }
    }

    suspend fun deleteTemplate(template: RecurringTemplate) {
        recurringDao.deleteTemplate(template)
        try {
            postgrest.from("recurring_transactions").delete {
                filter { eq("id", template.id) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteTemplate failed", e)
        }
    }

    /**
     * Process all due recurring templates. Creates transactions and advances lastGeneratedDate.
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

            // Advance the lastGeneratedDate; nextRun will be recomputed from it.
            val updated = template.copy(
                lastGeneratedDate = today,
                nextRun = computeNextRun(template.startDate, today, template.frequency)
            )
            recurringDao.updateTemplate(updated)
            try {
                postgrest.from("recurring_transactions").update(
                    mapOf("last_generated_date" to today)
                ) {
                    filter { eq("id", template.id) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "processDueTemplates: failed to advance ${template.id}", e)
            }
        }
    }

    companion object { private const val TAG = "RecurringRepo" }
}

/**
 * Compute the next scheduled run date for a recurring template.
 * Mirrors the React app's logic: start from `lastGeneratedDate` (or `startDate` if never run),
 * then add one unit of `frequency`.
 */
internal fun computeNextRun(startDate: String, lastGeneratedDate: String?, frequency: String): String {
    val base = (lastGeneratedDate ?: startDate).ifBlank { return "" }
    val date = try { LocalDate.parse(base) } catch (_: Exception) { return "" }
    val next = when (frequency) {
        "daily" -> date.plusDays(1)
        "weekly" -> date.plusWeeks(1)
        "monthly" -> date.plusMonths(1)
        else -> date.plusMonths(1)
    }
    return next.format(DateTimeFormatter.ISO_LOCAL_DATE)
}
