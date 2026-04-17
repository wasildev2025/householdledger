package com.example.householdledger.ui.dairy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.DairyLog
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.DairyRepository
import com.example.householdledger.data.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DairyViewModel @Inject constructor(
    private val dairyRepository: DairyRepository,
    private val householdRepository: HouseholdRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DairyUiState())
    val uiState: StateFlow<DairyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val householdId = authRepository.currentUser.value?.householdId
            if (householdId != null) {
                dairyRepository.syncLogs(householdId)
                
                combine(
                    dairyRepository.getLogs(householdId),
                    householdRepository.getHousehold(householdId)
                ) { logs, household ->
                    DairyUiState(
                        logs = logs,
                        milkPrice = household?.milkPrice ?: 150.0,
                        yogurtPrice = household?.yogurtPrice ?: 200.0,
                        totalMonthlyBill = logs.sumOf { it.totalBill }
                    )
                }.collect { state ->
                    _uiState.value = state
                }
            }
        }
    }

    fun addLog(milkQty: Double, yogurtQty: Double) {
        val householdId = authRepository.currentUser.value?.householdId ?: return
        val currentPrices = _uiState.value
        
        val total = (milkQty * currentPrices.milkPrice) + (yogurtQty * currentPrices.yogurtPrice)
        
        val log = DairyLog(
            id = UUID.randomUUID().toString(),
            date = LocalDate.now().toString(),
            milkQty = milkQty,
            milkPrice = currentPrices.milkPrice,
            yogurtQty = yogurtQty,
            yogurtPrice = currentPrices.yogurtPrice,
            totalBill = total,
            householdId = householdId
        )
        
        viewModelScope.launch {
            dairyRepository.addLog(log)
        }
    }
}

data class DairyUiState(
    val logs: List<DairyLog> = emptyList(),
    val milkPrice: Double = 150.0,
    val yogurtPrice: Double = 200.0,
    val totalMonthlyBill: Double = 0.0
)
