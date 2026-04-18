package com.example.householdledger.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.local.PreferenceManager
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.CategoryRepository
import com.example.householdledger.data.repository.PeopleRepository
import com.example.householdledger.data.repository.DairyRepository
import com.example.householdledger.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val dairyRepository: DairyRepository
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
            val profile = authRepository.currentUser.value
            if (profile?.householdId != null) {
                transactionRepository.syncTransactions()
                categoryRepository.syncCategories()
                peopleRepository.syncPeople()
                dairyRepository.syncDairyLogs()
                categoryRepository.subscribeRealtime()
                transactionRepository.subscribeRealtime()
                peopleRepository.subscribeRealtime()
                dairyRepository.subscribeRealtime()
            }
        }
    }

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
        }
    }
}
