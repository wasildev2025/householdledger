package com.example.householdledger.data.repository

import com.example.householdledger.data.local.MessageDao
import com.example.householdledger.data.model.Message
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.*
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository
) {
    private var realtimeChannel: RealtimeChannel? = null
    // SupervisorJob so one failing child collect doesn't kill the scope, and the
    // scope can be cancelled cleanly on sign-out to stop any in-flight collects.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var realtimeJob: Job? = null

    fun getMessages(householdId: String): Flow<List<Message>> {
        return messageDao.getMessages(householdId)
    }

    fun getUnreadCount(householdId: String): Flow<Int> {
        val userId = authRepository.getSupabaseUser()?.id ?: ""
        return messageDao.getUnreadCount(householdId, userId)
    }

    suspend fun syncMessages(householdId: String) {
        try {
            val remoteMessages = supabaseClient.postgrest
                .from("messages")
                .select {
                    filter { eq("household_id", householdId) }
                }
                .decodeList<Message>()
            messageDao.insertMessages(remoteMessages)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sendTextMessage(householdId: String, content: String) {
        val profile = authRepository.currentUser.value ?: return
        val user = authRepository.getSupabaseUser() ?: return
        val message = Message(
            id = UUID.randomUUID().toString(),
            householdId = householdId,
            senderId = user.id,
            senderName = profile.name.orEmpty(),
            senderRole = profile.role,
            type = "text",
            content = content,
            createdAt = java.time.Instant.now().toString()
        )
        messageDao.insertMessage(message)
        try {
            supabaseClient.postgrest.from("messages").insert(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sendMediaMessage(
        householdId: String,
        type: String, // "image" or "voice"
        fileBytes: ByteArray,
        fileName: String,
        mimeType: String
    ) {
        val profile = authRepository.currentUser.value ?: return
        val user = authRepository.getSupabaseUser() ?: return
        try {
            val storagePath = "chat/$householdId/${UUID.randomUUID()}_$fileName"
            supabaseClient.storage
                .from("chat-media")
                .upload(storagePath, fileBytes)

            val publicUrl = supabaseClient.storage
                .from("chat-media")
                .publicUrl(storagePath)

            val message = Message(
                id = UUID.randomUUID().toString(),
                householdId = householdId,
                senderId = user.id,
                senderName = profile.name.orEmpty(),
                senderRole = profile.role,
                type = type,
                content = if (type == "image") "📷 Image" else "🎤 Voice message",
                mediaUrl = publicUrl,
                createdAt = java.time.Instant.now().toString()
            )
            messageDao.insertMessage(message)
            supabaseClient.postgrest.from("messages").insert(message)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markAsRead(householdId: String) {
        val userId = authRepository.getSupabaseUser()?.id ?: return
        messageDao.markAllRead(householdId, userId)
    }

    /**
     * Subscribe to realtime inserts on the messages table for this household.
     * Safe to call repeatedly — a prior subscription is cancelled first so
     * collect-jobs don't stack up as a leak.
     */
    fun subscribeToMessages(householdId: String) {
        // Cancel any previous subscription before creating a new one.
        realtimeJob?.cancel()
        realtimeChannel?.let { old ->
            scope.launch {
                runCatching { supabaseClient.realtime.removeChannel(old) }
            }
        }
        realtimeJob = scope.launch {
            try {
                val channel = supabaseClient.realtime.channel("messages:$householdId")
                realtimeChannel = channel
                channel.subscribe()
                channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                    table = "messages"
                }
                    .catch { it.printStackTrace() }
                    .collect { change ->
                        runCatching {
                            val newMessage = change.decodeRecord<Message>()
                            if (newMessage.householdId == householdId) {
                                messageDao.insertMessage(newMessage)
                            }
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun unsubscribe() {
        realtimeJob?.cancel()
        realtimeJob = null
        realtimeChannel?.let {
            runCatching { supabaseClient.realtime.removeChannel(it) }
        }
        realtimeChannel = null
    }
}
