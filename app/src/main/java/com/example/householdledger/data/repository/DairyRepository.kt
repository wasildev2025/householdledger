package com.example.householdledger.data.repository

import android.util.Log
import com.example.householdledger.data.local.DairyDao
import com.example.householdledger.data.model.DairyLog
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DairyRepository @Inject constructor(
    private val dairyDao: DairyDao,
    private val postgrest: Postgrest,
    private val realtime: Realtime,
    private val authRepository: AuthRepository
) {
    val dairyLogs: Flow<List<DairyLog>> = dairyDao.getAllLogs()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var realtimeJob: Job? = null

    suspend fun syncDairyLogs() {
        val profile = authRepository.currentUser.value
        if (profile == null) { Log.w(TAG, "syncDairyLogs: no profile"); return }
        val householdId = profile.householdId
        if (householdId == null) { Log.w(TAG, "syncDairyLogs: no householdId"); return }
        try {
            val remote = postgrest.from("dairy_logs")
                .select { filter { eq("household_id", householdId) } }
                .decodeList<DairyLog>()
            Log.d(TAG, "syncDairyLogs: fetched ${remote.size} rows for household=$householdId")
            dairyDao.insertLogs(remote)
        } catch (e: Exception) {
            Log.e(TAG, "syncDairyLogs failed", e)
        }
    }

    companion object { private const val TAG = "DairyRepo" }

    suspend fun addDairyLog(log: DairyLog) {
        dairyDao.insertLog(log)
        try { postgrest.from("dairy_logs").insert(log) }
        catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun deleteDairyLog(log: DairyLog) {
        dairyDao.deleteLog(log)
        try {
            postgrest.from("dairy_logs").delete { filter { eq("id", log.id) } }
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun updateDairyLog(log: DairyLog) {
        dairyDao.insertLog(log) // REPLACE strategy — effectively update
        try {
            postgrest.from("dairy_logs").update(log) { filter { eq("id", log.id) } }
        } catch (e: Exception) { Log.e(TAG, "updateDairyLog failed", e) }
    }

    fun subscribeRealtime() {
        val householdId = authRepository.currentUser.value?.householdId ?: return
        realtimeJob?.cancel()
        realtimeJob = scope.launch {
            try {
                val channel = realtime.channel("dairy_logs:$householdId")
                val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "dairy_logs"
                }
                channel.subscribe()
                changes.catch { it.printStackTrace() }.collect { _ -> syncDairyLogs() }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun unsubscribeRealtime() {
        realtimeJob?.cancel(); realtimeJob = null
    }
}
