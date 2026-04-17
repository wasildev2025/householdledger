package com.example.householdledger.ui.household

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.repository.HouseholdRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class JoinState {
    object Idle : JoinState()
    object Loading : JoinState()
    object Success : JoinState()
    data class Error(val message: String) : JoinState()
}

@HiltViewModel
class JoinHouseholdViewModel @Inject constructor(
    private val householdRepository: HouseholdRepository
) : ViewModel() {

    private val _state = MutableStateFlow<JoinState>(JoinState.Idle)
    val state: StateFlow<JoinState> = _state

    fun join(inviteCode: String) {
        viewModelScope.launch {
            _state.value = JoinState.Loading
            try {
                householdRepository.joinHousehold(inviteCode)
                _state.value = JoinState.Success
            } catch (e: Exception) {
                _state.value = JoinState.Error(e.message ?: "Invalid invite code")
            }
        }
    }

    fun create(name: String) {
        viewModelScope.launch {
            _state.value = JoinState.Loading
            try {
                householdRepository.createHousehold(name)
                _state.value = JoinState.Success
            } catch (e: Exception) {
                _state.value = JoinState.Error(e.message ?: "Failed to create household")
            }
        }
    }
}
