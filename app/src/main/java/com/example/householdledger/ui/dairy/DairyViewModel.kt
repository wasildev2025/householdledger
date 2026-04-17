package com.example.householdledger.ui.dairy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.DairyLog
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.DairyRepository
import com.example.householdledger.data.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DairyViewModel @Inject constructor(
    private val dairyRepository: DairyRepository,
    private val householdRepository: HouseholdRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val dairyLogs: StateFlow<List<DairyLog>> = dairyRepository.dairyLogs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val household = householdRepository.household

    init {
        viewModelScope.launch {
            dairyRepository.syncDairyLogs()
            householdRepository.loadHousehold()
        }
    }

    fun addEntry(milkQty: Double, yogurtQty: Double) {
        viewModelScope.launch {
            val h = household.value ?: return@launch
            val profile = authRepository.currentUser.value ?: return@launch
            
            val milkPrice = h.milkPrice
            val yogurtPrice = h.yogurtPrice
            val totalBill = (milkQty * milkPrice) + (yogurtQty * yogurtPrice)
            
            val log = DairyLog(
                id = UUID.randomUUID().toString(),
                date = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                milkQty = milkQty,
                milkPrice = milkPrice,
                yogurtQty = yogurtQty,
                yogurtPrice = yogurtPrice,
                totalBill = totalBill,
                householdId = h.id
            )
            dairyRepository.addDairyLog(log)
        }
    }

    fun deleteEntry(log: DairyLog) {
        viewModelScope.launch {
            dairyRepository.deleteDairyLog(log)
        }
    }
}
