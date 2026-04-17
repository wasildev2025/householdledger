package com.example.householdledger.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.repository.PeopleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddServantViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository
) : ViewModel() {

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    fun addServant(name: String, role: String, phoneNumber: String?, salary: Double?, budget: Double?) {
        viewModelScope.launch {
            _isSaving.value = true
            val code = peopleRepository.addServant(name, role, phoneNumber, salary, budget)
            _inviteCode.value = code
            _isSaving.value = false
        }
    }
}
