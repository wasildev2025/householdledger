package com.example.householdledger.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.local.PreferenceManager
import com.example.householdledger.data.model.Transaction
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.TransactionRepository
import com.example.householdledger.util.DataExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val darkMode: String = "system", // "system" | "light" | "dark"
    val notificationsEnabled: Boolean = true,
    val currency: String = "PKR",
    val biometricEnabled: Boolean = false,
    val monthlyBudget: Double = 0.0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: PreferenceManager,
    private val transactionRepository: TransactionRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val state: StateFlow<SettingsState> = combine(
        prefs.darkMode,
        prefs.notificationsEnabled,
        prefs.currency,
        prefs.biometricEnabled,
        prefs.monthlyBudget
    ) { dark, notif, curr, bio, budget ->
        SettingsState(
            darkMode = dark,
            notificationsEnabled = notif,
            currency = curr,
            biometricEnabled = bio,
            monthlyBudget = budget
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsState())

    private val _exportResult = MutableStateFlow<String?>(null)
    val exportResult: StateFlow<String?> = _exportResult.asStateFlow()

    fun setDarkMode(value: String) { viewModelScope.launch { prefs.setDarkMode(value) } }
    fun setNotifications(value: Boolean) { viewModelScope.launch { prefs.setNotificationsEnabled(value) } }
    fun setCurrency(value: String) { viewModelScope.launch { prefs.setCurrency(value) } }
    fun setBiometric(value: Boolean) { viewModelScope.launch { prefs.setBiometricEnabled(value) } }
    fun setMonthlyBudget(value: Double) { viewModelScope.launch { prefs.setMonthlyBudget(value) } }

    fun exportJson() {
        viewModelScope.launch {
            val txns: List<Transaction> = transactionRepository.transactions.first()
            val ok = DataExporter.exportToJson(context, txns)
            _exportResult.value = if (ok) "JSON saved to Downloads" else "JSON export failed"
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            val txns: List<Transaction> = transactionRepository.transactions.first()
            val ok = DataExporter.exportToCsv(context, txns)
            _exportResult.value = if (ok) "CSV saved to Downloads" else "CSV export failed"
        }
    }

    fun clearExportResult() { _exportResult.value = null }

    fun signOut() {
        viewModelScope.launch { authRepository.signOut() }
    }
}
