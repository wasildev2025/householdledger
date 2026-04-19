package com.example.householdledger.ui.transaction

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.householdledger.data.model.Category
import com.example.householdledger.data.model.Member
import com.example.householdledger.data.model.Servant
import com.example.householdledger.data.repository.CategoryRepository
import com.example.householdledger.data.repository.PeopleRepository
import com.example.householdledger.util.ParsedTransaction
import com.example.householdledger.util.TransactionParser
import com.example.householdledger.util.VoiceEvent
import com.example.householdledger.util.VoiceRecognizer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

enum class VoicePhase {
    Idle,
    Listening,     // mic on, may have partial text
    Processing,    // final result received, parsing
    Ready,         // parsed — user reviews
    Error          // recognition failure
}

data class VoiceUiState(
    val phase: VoicePhase = VoicePhase.Idle,
    val partialText: String = "",
    val finalText: String = "",
    val rmsLevel: Float = 0f,             // 0..1 normalized for waveform
    val parsed: ParsedTransaction? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class VoiceAddViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val categoryRepository: CategoryRepository,
    private val peopleRepository: PeopleRepository
) : ViewModel() {

    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    private var listenJob: Job? = null

    fun isAvailable(): Boolean = VoiceRecognizer.isAvailable(context)

    fun start() {
        listenJob?.cancel()
        _state.value = VoiceUiState(phase = VoicePhase.Listening)
        listenJob = viewModelScope.launch {
            VoiceRecognizer.listen(context).collect { event ->
                when (event) {
                    is VoiceEvent.ReadyForSpeech -> Unit
                    is VoiceEvent.BeginSpeaking -> Unit
                    is VoiceEvent.Rms -> _state.value = _state.value.copy(
                        rmsLevel = normalizeRms(event.level)
                    )
                    is VoiceEvent.Partial -> _state.value = _state.value.copy(
                        partialText = event.text
                    )
                    is VoiceEvent.EndSpeaking -> _state.value = _state.value.copy(
                        phase = VoicePhase.Processing
                    )
                    is VoiceEvent.Final -> handleFinal(event.text)
                    is VoiceEvent.Error -> _state.value = _state.value.copy(
                        phase = VoicePhase.Error,
                        errorMessage = event.message
                    )
                }
            }
        }
    }

    fun cancel() {
        listenJob?.cancel()
        _state.value = VoiceUiState()
    }

    fun reset() = cancel()

    private suspend fun handleFinal(text: String) {
        if (text.isBlank()) {
            _state.value = _state.value.copy(
                phase = VoicePhase.Error,
                errorMessage = "Didn't catch that — try again"
            )
            return
        }
        val categories = runCatching { categoryRepository.categories.first() }.getOrDefault(emptyList<Category>())
        val servants = runCatching { peopleRepository.servants.first() }.getOrDefault(emptyList<Servant>())
        val members = runCatching { peopleRepository.members.first() }.getOrDefault(emptyList<Member>())
        val parsed = TransactionParser.parse(text, categories, servants, members)
        _state.value = VoiceUiState(
            phase = VoicePhase.Ready,
            finalText = text,
            parsed = parsed
        )
    }

    private fun normalizeRms(db: Float): Float {
        // Android's onRmsChanged returns a value in dB-ish range (−2..10).
        // Map to 0..1 with mild compression so the waveform looks lively without clipping.
        val clamped = max(0f, db + 2f) / 12f
        return clamped.coerceIn(0f, 1f)
    }
}
