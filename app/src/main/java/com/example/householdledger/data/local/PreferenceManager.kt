package com.example.householdledger.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "household_settings")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        val DARK_MODE = stringPreferencesKey("dark_mode") // "system", "light", "dark"
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val CURRENCY = stringPreferencesKey("currency")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val MONTHLY_BUDGET = doublePreferencesKey("monthly_budget")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val SESSION_TOKEN = stringPreferencesKey("session_token")
    }

    val darkMode: Flow<String> = context.dataStore.data.map { it[DARK_MODE] ?: "system" }
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { it[NOTIFICATIONS_ENABLED] ?: true }
    val currency: Flow<String> = context.dataStore.data.map { it[CURRENCY] ?: "PKR" }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[BIOMETRIC_ENABLED] ?: false }
    val monthlyBudget: Flow<Double> = context.dataStore.data.map { it[MONTHLY_BUDGET] ?: 0.0 }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_COMPLETE] ?: false }

    suspend fun setDarkMode(mode: String) {
        context.dataStore.edit { it[DARK_MODE] = mode }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setCurrency(currency: String) {
        context.dataStore.edit { it[CURRENCY] = currency }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setMonthlyBudget(budget: Double) {
        context.dataStore.edit { it[MONTHLY_BUDGET] = budget }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        context.dataStore.edit { it[ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setSessionToken(token: String?) {
        context.dataStore.edit {
            if (token != null) it[SESSION_TOKEN] = token
            else it.remove(SESSION_TOKEN)
        }
    }

    val sessionToken: Flow<String?> = context.dataStore.data.map { it[SESSION_TOKEN] }
}
