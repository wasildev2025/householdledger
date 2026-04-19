package com.example.householdledger.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.local.PreferenceManager
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.CategoryRepository
import com.example.householdledger.data.repository.DairyRepository
import com.example.householdledger.data.repository.HouseholdRepository
import com.example.householdledger.data.repository.MessageRepository
import com.example.householdledger.data.repository.PeopleRepository
import com.example.householdledger.data.repository.RecurringRepository
import com.example.householdledger.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AppBootState { Loading, Onboarding, Auth, JoinHousehold, Ready }

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val prefs: PreferenceManager,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val peopleRepository: PeopleRepository,
    private val dairyRepository: DairyRepository,
    private val recurringRepository: RecurringRepository,
    private val messageRepository: MessageRepository,
    private val householdRepository: HouseholdRepository
) : ViewModel() {

    val currentUser = authRepository.currentUser.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), null
    )

    val darkMode = prefs.darkMode.stateIn(viewModelScope, SharingStarted.Eagerly, "system")
    val currency = prefs.currency.stateIn(viewModelScope, SharingStarted.Eagerly, "PKR")
    val onboardingComplete = prefs.onboardingComplete.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _bootReady = MutableStateFlow(false)

    val bootState: StateFlow<AppBootState> = combine(
        currentUser, onboardingComplete, _bootReady
    ) { user, done, ready ->
        when {
            !ready -> AppBootState.Loading
            !done -> AppBootState.Onboarding
            user == null -> AppBootState.Auth
            user.householdId == null -> AppBootState.JoinHousehold
            else -> AppBootState.Ready
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppBootState.Loading)

    init {
        viewModelScope.launch {
            authRepository.loadProfile()
            _bootReady.value = true
        }
        // React to the profile becoming available (or changing households) and sync everything.
        viewModelScope.launch {
            authRepository.currentUser
                .filterNotNull()
                .map { it.householdId }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { householdId ->
                    Log.d(TAG, "bootSync: starting for household=$householdId")
                    householdRepository.loadHousehold()
                    transactionRepository.syncTransactions()
                    categoryRepository.syncCategories()
                    peopleRepository.syncPeople()
                    dairyRepository.syncDairyLogs()
                    recurringRepository.syncTemplates()
                    messageRepository.syncMessages(householdId)
                    categoryRepository.subscribeRealtime()
                    transactionRepository.subscribeRealtime()
                    peopleRepository.subscribeRealtime()
                    dairyRepository.subscribeRealtime()
                    messageRepository.subscribeToMessages(householdId)
                    Log.d(TAG, "bootSync: done for household=$householdId")
                }
        }
    }

    companion object { private const val TAG = "MainVM" }

    fun completeOnboarding() {
        viewModelScope.launch { prefs.setOnboardingComplete(true) }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            categoryRepository.unsubscribeRealtime()
            transactionRepository.unsubscribeRealtime()
            peopleRepository.unsubscribeRealtime()
            dairyRepository.unsubscribeRealtime()
            messageRepository.unsubscribe()
        }
    }
}
