package com.example.householdledger.data.repository

import android.util.Log
import com.example.householdledger.data.local.MemberDao
import com.example.householdledger.data.local.ServantDao
import com.example.householdledger.data.model.Member
import com.example.householdledger.data.model.Servant
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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleRepository @Inject constructor(
    private val servantDao: ServantDao,
    private val memberDao: MemberDao,
    private val postgrest: Postgrest,
    private val realtime: Realtime,
    private val authRepository: AuthRepository
) {
    val servants: Flow<List<Servant>> = servantDao.getAllServants()
    val members: Flow<List<Member>> = memberDao.getAllMembers()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var servantJob: Job? = null
    private var memberJob: Job? = null

    fun subscribeRealtime() {
        val householdId = authRepository.currentUser.value?.householdId ?: return
        servantJob?.cancel(); memberJob?.cancel()
        servantJob = scope.launch {
            try {
                val channel = realtime.channel("servants:$householdId")
                val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "servants"
                }
                channel.subscribe()
                changes.catch { it.printStackTrace() }.collect { _ -> syncPeople() }
            } catch (e: Exception) { e.printStackTrace() }
        }
        memberJob = scope.launch {
            try {
                val channel = realtime.channel("members:$householdId")
                val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "members"
                }
                channel.subscribe()
                changes.catch { it.printStackTrace() }.collect { _ -> syncPeople() }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun unsubscribeRealtime() {
        servantJob?.cancel(); memberJob?.cancel()
        servantJob = null; memberJob = null
    }

    suspend fun syncPeople() {
        val profile = authRepository.currentUser.value
        if (profile == null) { Log.w(TAG, "syncPeople: no profile"); return }
        val householdId = profile.householdId
        if (householdId == null) { Log.w(TAG, "syncPeople: no householdId"); return }

        try {
            val remoteServants = postgrest.from("servants")
                .select {
                    filter {
                        eq("household_id", householdId)
                    }
                }
                .decodeList<Servant>()
            Log.d(TAG, "syncPeople: fetched ${remoteServants.size} servants for household=$householdId")
            servantDao.insertServants(remoteServants)

            val remoteMembers = postgrest.from("members")
                .select {
                    filter {
                        eq("household_id", householdId)
                    }
                }
                .decodeList<Member>()
            Log.d(TAG, "syncPeople: fetched ${remoteMembers.size} members for household=$householdId")
            memberDao.insertMembers(remoteMembers)
        } catch (e: Exception) {
            Log.e(TAG, "syncPeople failed", e)
        }
    }

    companion object { private const val TAG = "PeopleRepo" }

    suspend fun addServant(name: String, role: String, phoneNumber: String?, salary: Double?, budget: Double?): String? {
        val profile = authRepository.currentUser.value ?: return null
        val householdId = profile.householdId ?: return null
        val inviteCode = (100000..999999).random().toString()
        
        val servant = Servant(
            id = UUID.randomUUID().toString(),
            name = name,
            role = role,
            phoneNumber = phoneNumber,
            salary = salary,
            budget = budget,
            inviteCode = inviteCode,
            householdId = householdId
        )
        
        try {
            postgrest.from("servants").insert(servant)
            syncPeople()
            return inviteCode
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun addMember(name: String): String? {
        val profile = authRepository.currentUser.value ?: return null
        val householdId = profile.householdId ?: return null
        val inviteCode = (100000..999999).random().toString()
        
        val member = Member(
            id = UUID.randomUUID().toString(),
            name = name,
            inviteCode = inviteCode,
            householdId = householdId
        )
        
        try {
            postgrest.from("members").insert(member)
            syncPeople()
            return inviteCode
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    suspend fun deleteServant(servant: Servant) {
        servantDao.deleteServant(servant)
        try {
            postgrest.from("servants").delete {
                filter { eq("id", servant.id) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteMember(member: Member) {
        memberDao.deleteMember(member)
        try {
            postgrest.from("members").delete {
                filter { eq("id", member.id) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
