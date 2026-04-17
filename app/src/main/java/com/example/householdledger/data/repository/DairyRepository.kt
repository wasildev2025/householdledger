package com.example.householdledger.data.repository

import com.example.householdledger.data.local.DairyDao
import com.example.householdledger.data.model.DairyLog
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DairyRepository @Inject constructor(
    private val dairyDao: DairyDao,
    private val postgrest: Postgrest,
    private val authRepository: AuthRepository
) {
    val dairyLogs: Flow<List<DairyLog>> = dairyDao.getAllLogs()

    suspend fun syncDairyLogs() {
        val profile = authRepository.currentUser.value ?: return
        val householdId = profile.householdId ?: return

        try {
            val remoteLogs = postgrest.from("dairy_logs")
                .select {
                    filter {
                        eq("household_id", householdId)
                    }
                }
                .decodeList<DairyLog>()
            
            dairyDao.insertLogs(remoteLogs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun addDairyLog(log: DairyLog) {
        dairyDao.insertLog(log)
        try {
            postgrest.from("dairy_logs").insert(log)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteDairyLog(log: DairyLog) {
        dairyDao.deleteLog(log)
        try {
            postgrest.from("dairy_logs").delete {
                filter { eq("id", log.id) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
