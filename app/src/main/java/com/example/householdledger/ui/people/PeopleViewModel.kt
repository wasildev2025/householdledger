package com.example.householdledger.ui.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Member
import com.example.householdledger.data.model.Servant
import com.example.householdledger.data.repository.PeopleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PeopleViewModel @Inject constructor(
    private val peopleRepository: PeopleRepository
) : ViewModel() {

    val servants: StateFlow<List<Servant>> = peopleRepository.servants.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val members: StateFlow<List<Member>> = peopleRepository.members.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        refreshPeople()
    }

    fun refreshPeople() {
        viewModelScope.launch {
            peopleRepository.syncPeople()
        }
    }

    fun deleteServant(servant: Servant) {
        viewModelScope.launch {
            peopleRepository.deleteServant(servant)
        }
    }

    fun deleteMember(member: Member) {
        viewModelScope.launch {
            peopleRepository.deleteMember(member)
        }
    }
}
