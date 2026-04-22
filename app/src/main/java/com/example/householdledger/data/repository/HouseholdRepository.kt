package com.example.householdledger.data.repository

import com.example.householdledger.data.model.Household
import com.example.householdledger.data.model.Member
import com.example.householdledger.data.model.Servant
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HouseholdRepository @Inject constructor(
    private val postgrest: Postgrest,
    private val authRepository: AuthRepository
) {
    private val _household = MutableStateFlow<Household?>(null)
    val household: StateFlow<Household?> = _household

    suspend fun updateHouseholdName(name: String) {
        val profile = authRepository.currentUser.value ?: return
        val householdId = profile.householdId ?: return
        try {
            postgrest.from("households").update(mapOf("name" to name)) {
                filter { eq("id", householdId) }
            }
            loadHousehold()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateDairyPrices(milkPrice: Double, yogurtPrice: Double) {
        val profile = authRepository.currentUser.value ?: return
        val householdId = profile.householdId ?: return
        try {
            postgrest.from("households").update(
                mapOf("milk_price" to milkPrice, "yogurt_price" to yogurtPrice)
            ) {
                filter { eq("id", householdId) }
            }
            loadHousehold()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateCycleSettings(startDay: Int, budget: Double) {
        val profile = authRepository.currentUser.value ?: return
        val householdId = profile.householdId ?: return
        try {
            postgrest.from("households").update(
                mapOf("cycle_start_day" to startDay, "monthly_budget" to budget)
            ) {
                filter { eq("id", householdId) }
            }
            loadHousehold()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun loadHousehold() {
        val user = authRepository.getSupabaseUser() ?: return
        val profile = authRepository.currentUser.value ?: return
        val householdId = profile.householdId ?: return

        try {
            val result = postgrest.from("households")
                .select {
                    filter {
                        eq("id", householdId)
                    }
                }
                .decodeSingle<Household>()
            _household.value = result
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun createHousehold(name: String) {
        val user = authRepository.getSupabaseUser() ?: return
        try {
            val newHousehold = postgrest.from("households")
                .insert(mapOf("name" to name, "owner_id" to user.id)) {
                    select()
                }
                .decodeSingle<Household>()
            
            // Update profile role to admin
            postgrest.from("profiles")
                .update(mapOf("role" to "admin", "household_id" to newHousehold.id)) {
                    filter { eq("id", user.id) }
                }
            
            authRepository.loadProfile()
            loadHousehold()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun joinHousehold(inviteCode: String) {
        val user = authRepository.getSupabaseUser() ?: return
        try {
            // Check servants
            val servant = postgrest.from("servants")
                .select {
                    filter { eq("invite_code", inviteCode) }
                }
                .decodeSingleOrNull<Servant>()

            if (servant != null) {
                postgrest.from("profiles")
                    .update(mapOf(
                        "role" to "servant",
                        "servant_id" to servant.id,
                        "household_id" to servant.householdId,
                        "name" to servant.name
                    )) {
                        filter { eq("id", user.id) }
                    }
                // Invalidate invite code
                postgrest.from("servants")
                    .update(mapOf("invite_code" to null)) {
                        filter { eq("id", servant.id) }
                    }
                authRepository.loadProfile()
                loadHousehold()
                return
            }

            // Check members
            val member = postgrest.from("members")
                .select {
                    filter { eq("invite_code", inviteCode) }
                }
                .decodeSingleOrNull<Member>()

            if (member != null) {
                postgrest.from("profiles")
                    .update(mapOf(
                        "role" to "member",
                        "member_id" to member.id,
                        "household_id" to member.householdId,
                        "name" to member.name
                    )) {
                        filter { eq("id", user.id) }
                    }
                // Invalidate invite code
                postgrest.from("members")
                    .update(mapOf("invite_code" to null)) {
                        filter { eq("id", member.id) }
                    }
                authRepository.loadProfile()
                loadHousehold()
                return
            }
            
            throw Exception("Invalid or expired invite code")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
