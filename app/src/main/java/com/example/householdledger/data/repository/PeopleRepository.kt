package com.example.householdledger.data.repository

import com.example.householdledger.data.local.MemberDao
import com.example.householdledger.data.local.ServantDao
import com.example.householdledger.data.model.Member
import com.example.householdledger.data.model.Servant
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PeopleRepository @Inject constructor(
    private val servantDao: ServantDao,
    private val memberDao: MemberDao,
    private val postgrest: Postgrest,
    private val authRepository: AuthRepository
) {
    val servants: Flow<List<Servant>> = servantDao.getAllServants()
    val members: Flow<List<Member>> = memberDao.getAllMembers()

    suspend fun syncPeople() {
        val profile = authRepository.currentUser.value ?: return
        val householdId = profile.householdId ?: return

        try {
            val remoteServants = postgrest.from("servants")
                .select {
                    filter {
                        eq("household_id", householdId)
                    }
                }
                .decodeList<Servant>()
            servantDao.insertServants(remoteServants)

            val remoteMembers = postgrest.from("members")
                .select {
                    filter {
                        eq("household_id", householdId)
                    }
                }
                .decodeList<Member>()
            memberDao.insertMembers(remoteMembers)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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
