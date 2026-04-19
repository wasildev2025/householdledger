package com.example.householdledger.ui.dairy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.DairyLog
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.DairyRepository
import com.example.householdledger.data.repository.HouseholdRepository
import com.example.householdledger.util.DateUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject

data class DairyUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val canGoForward: Boolean = false,
    /** Logs filtered to [selectedMonth] — used by history list and the monthly bill. */
    val monthLogs: List<DairyLog> = emptyList(),
    /** All household logs — useful for computing "first log date" (nav bounds). */
    val allLogs: List<DairyLog> = emptyList(),
    val milkPrice: Double = 150.0,
    val yogurtPrice: Double = 200.0,
    val monthlyBill: Double = 0.0,
    val monthlyMilkQty: Double = 0.0,
    val monthlyYogurtQty: Double = 0.0
)

@HiltViewModel
class DairyViewModel @Inject constructor(
    private val dairyRepository: DairyRepository,
    private val householdRepository: HouseholdRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val selectedMonth = MutableStateFlow(YearMonth.now())

    init {
        viewModelScope.launch { dairyRepository.syncDairyLogs() }
    }

    val uiState: StateFlow<DairyUiState> = combine(
        dairyRepository.dairyLogs,
        householdRepository.household,
        selectedMonth
    ) { logs, household, month ->
        val householdId = authRepository.currentUser.value?.householdId
        val householdLogs = logs.filter { it.householdId == householdId }

        val monthLogs = householdLogs
            .filter { monthFor(it.date) == month }
            .sortedByDescending { DateUtil.parseDate(it.date) ?: LocalDate.MIN }

        val now = YearMonth.now()
        DairyUiState(
            selectedMonth = month,
            canGoForward = month < now,
            monthLogs = monthLogs,
            allLogs = householdLogs,
            milkPrice = household?.milkPrice ?: 150.0,
            yogurtPrice = household?.yogurtPrice ?: 200.0,
            monthlyBill = monthLogs.sumOf { it.totalBill },
            monthlyMilkQty = monthLogs.sumOf { it.milkQty },
            monthlyYogurtQty = monthLogs.sumOf { it.yogurtQty }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DairyUiState())

    fun shiftMonth(delta: Int) {
        val target = selectedMonth.value.plusMonths(delta.toLong())
        if (target <= YearMonth.now()) {
            selectedMonth.value = target
        }
    }

    fun addLog(milkQty: Double, yogurtQty: Double) {
        val householdId = authRepository.currentUser.value?.householdId ?: return
        val current = uiState.value
        val total = (milkQty * current.milkPrice) + (yogurtQty * current.yogurtPrice)

        val log = DairyLog(
            id = UUID.randomUUID().toString(),
            // Record under the currently-viewed month so users can log for prior months too.
            date = dateForSelectedMonth(current.selectedMonth).toString(),
            milkQty = milkQty,
            milkPrice = current.milkPrice,
            yogurtQty = yogurtQty,
            yogurtPrice = current.yogurtPrice,
            totalBill = total,
            householdId = householdId
        )

        viewModelScope.launch { dairyRepository.addDairyLog(log) }
    }

    fun updateLog(
        log: DairyLog,
        milkQty: Double,
        yogurtQty: Double,
        date: String
    ) {
        val current = uiState.value
        val total = (milkQty * current.milkPrice) + (yogurtQty * current.yogurtPrice)
        val updated = log.copy(
            date = date,
            milkQty = milkQty,
            yogurtQty = yogurtQty,
            totalBill = total
        )
        viewModelScope.launch { dairyRepository.updateDairyLog(updated) }
    }

    fun deleteLog(log: DairyLog) {
        viewModelScope.launch { dairyRepository.deleteDairyLog(log) }
    }

    fun updatePrices(milkPrice: Double, yogurtPrice: Double) {
        viewModelScope.launch { householdRepository.updateDairyPrices(milkPrice, yogurtPrice) }
    }

    // ---- helpers ----
    private fun monthFor(raw: String): YearMonth? =
        DateUtil.parseDate(raw)?.let { YearMonth.of(it.year, it.monthValue) }

    /** Today if viewing the current month; otherwise the last day of the selected month. */
    private fun dateForSelectedMonth(month: YearMonth): LocalDate {
        val today = LocalDate.now()
        return when {
            month == YearMonth.from(today) -> today
            month.isBefore(YearMonth.from(today)) -> month.atEndOfMonth()
            else -> month.atDay(1)
        }
    }
}
