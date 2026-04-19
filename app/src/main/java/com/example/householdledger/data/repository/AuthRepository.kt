package com.example.householdledger.data.repository

import android.util.Log
import com.example.householdledger.BuildConfig
import com.example.householdledger.data.model.UserProfile
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
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

            val resolvedName = profile.name?.takeIf { it.isNotBlank() }
                ?: resolveNameFromAuth(user.userMetadata, user.email)

            if (resolvedName != null && profile.name.isNullOrBlank()) {
                try {
                    postgrest.from("profiles").update(
                        buildJsonObject { put("name", JsonPrimitive(resolvedName)) }
                    ) {
                        filter { eq("id", user.id) }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to backfill profile.name", e)
                }
            }

            val resolved = profile.copy(name = resolvedName ?: profile.name)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "loadProfile: id=${user.id} profileName=${profile.name} resolved=$resolvedName")
            }
            _currentUser.value = resolved
        } catch (e: Exception) {
            Log.e(TAG, "loadProfile failed", e)
        }
    }

    private fun resolveNameFromAuth(metadata: JsonObject?, email: String?): String? {
        val keys = listOf("name", "full_name", "display_name", "given_name")
        for (k in keys) {
            val value = metadata?.get(k)?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            if (value != null) return value
        }
        return email?.substringBefore('@')?.takeIf { it.isNotBlank() }
    }

    companion object {
        private const val TAG = "AuthRepository"
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
