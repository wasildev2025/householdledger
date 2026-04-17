package com.example.householdledger.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Message
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    val currentUserId: String?
        get() = authRepository.getSupabaseUser()?.id

    init {
        viewModelScope.launch {
            val householdId = authRepository.currentUser.value?.householdId
            if (householdId != null) {
                // Initial sync & load
                messageRepository.syncMessages(householdId)
                messageRepository.getMessages(householdId).collect {
                    _messages.value = it
                    // Mark read when observing
                    messageRepository.markAsRead(householdId)
                }
            }
        }

        // Subscribe to real-time updates
        viewModelScope.launch {
            val householdId = authRepository.currentUser.value?.householdId
            if (householdId != null) {
                messageRepository.subscribeToMessages(householdId)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            messageRepository.unsubscribe()
        }
    }

    fun sendTextMessage(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val householdId = authRepository.currentUser.value?.householdId ?: return@launch
            messageRepository.sendTextMessage(householdId, content)
        }
    }
}
