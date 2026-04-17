package com.example.householdledger.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.householdledger.data.repository.RecurringRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Daily worker that processes due recurring transaction templates.
 */
@HiltWorker
class RecurringWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val recurringRepository: RecurringRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            recurringRepository.processDueTemplates()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "recurring_transaction_worker"
    }
}
