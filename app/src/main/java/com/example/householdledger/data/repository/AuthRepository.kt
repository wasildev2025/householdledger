package com.example.householdledger.data.repository

import com.example.householdledger.data.model.UserProfile
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: Auth,
    private val postgrest: Postgrest
) {
    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser

    fun getSupabaseUser() = auth.currentUserOrNull()

    suspend fun loadProfile() {
        val user = auth.currentUserOrNull() ?: return
        try {
            val profile = postgrest.from("profiles")
                .select {
                    filter {
                        eq("id", user.id)
                    }
                }
                .decodeSingle<UserProfile>()
            _currentUser.value = profile
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun signIn(emailInput: String, passwordInput: String) {
        auth.signInWith(Email) {
            email = emailInput
            password = passwordInput
        }
        loadProfile()
    }

    suspend fun signUp(emailInput: String, passwordInput: String, name: String) {
        auth.signUpWith(Email) {
            email = emailInput
            password = passwordInput
            data = buildJsonObject {
                put("name", name)
            }
        }
    }

    suspend fun signOut() {
        auth.signOut()
        _currentUser.value = null
    }
}
