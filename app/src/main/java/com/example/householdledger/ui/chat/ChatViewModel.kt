package com.example.householdledger.ui.chat

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Message
import com.example.householdledger.data.repository.AuthRepository
import com.example.householdledger.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val messageRepository: MessageRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    val currentUserId: String?
        get() = authRepository.getSupabaseUser()?.id

    init {
        viewModelScope.launch {
            val householdId = authRepository.currentUser.value?.householdId ?: return@launch
            messageRepository.syncMessages(householdId)
            messageRepository.getMessages(householdId).collect {
                _messages.value = it
                messageRepository.markAsRead(householdId)
            }
        }
        viewModelScope.launch {
            val householdId = authRepository.currentUser.value?.householdId ?: return@launch
            messageRepository.subscribeToMessages(householdId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch { messageRepository.unsubscribe() }
    }

    fun sendTextMessage(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val householdId = authRepository.currentUser.value?.householdId ?: return@launch
            messageRepository.sendTextMessage(householdId, content)
        }
    }

    fun sendImage(uri: Uri) {
        val householdId = authRepository.currentUser.value?.householdId ?: return
        viewModelScope.launch {
            _isSending.value = true
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes != null) {
                    val name = "image_${UUID.randomUUID()}.jpg"
                    messageRepository.sendMediaMessage(
                        householdId = householdId,
                        type = "image",
                        fileBytes = bytes,
                        fileName = name,
                        mimeType = "image/jpeg"
                    )
                }
            } catch (e: Exception) { e.printStackTrace() }
            _isSending.value = false
        }
    }

    fun sendVoiceFile(file: File) {
        val householdId = authRepository.currentUser.value?.householdId ?: return
        if (!file.exists()) return
        viewModelScope.launch {
            _isSending.value = true
            try {
                val bytes = file.readBytes()
                val name = "voice_${UUID.randomUUID()}.m4a"
                messageRepository.sendMediaMessage(
                    householdId = householdId,
                    type = "voice",
                    fileBytes = bytes,
                    fileName = name,
                    mimeType = "audio/mp4"
                )
                file.delete()
            } catch (e: Exception) { e.printStackTrace() }
            _isSending.value = false
        }
    }
}
