package com.example.householdledger.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.Locale

/**
 * Events emitted during a single voice capture session.
 */
sealed class VoiceEvent {
    data object ReadyForSpeech : VoiceEvent()
    data object BeginSpeaking : VoiceEvent()
    data object EndSpeaking : VoiceEvent()
    data class Rms(val level: Float) : VoiceEvent()           // -2..10 range
    data class Partial(val text: String) : VoiceEvent()
    data class Final(val text: String) : VoiceEvent()
    data class Error(val code: Int, val message: String) : VoiceEvent()
}

/** Voice recognition helper around Android's on-device [SpeechRecognizer]. */
object VoiceRecognizer {

    fun isAvailable(context: Context): Boolean =
        SpeechRecognizer.isRecognitionAvailable(context)

    /**
     * Starts a single speech-to-text session. The returned flow emits
     * [VoiceEvent]s and closes when the session ends. Cancelling the flow
     * (e.g. collector scope cancelled) also cancels the recognizer.
     */
    fun listen(context: Context, locale: Locale = Locale.getDefault()): Flow<VoiceEvent> =
        callbackFlow {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
            }

            val listener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    trySend(VoiceEvent.ReadyForSpeech)
                }
                override fun onBeginningOfSpeech() {
                    trySend(VoiceEvent.BeginSpeaking)
                }
                override fun onRmsChanged(rmsdB: Float) {
                    trySend(VoiceEvent.Rms(rmsdB))
                }
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() {
                    trySend(VoiceEvent.EndSpeaking)
                }
                override fun onError(error: Int) {
                    trySend(VoiceEvent.Error(error, describe(error)))
                    close()
                }
                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    trySend(VoiceEvent.Final(text))
                    close()
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull().orEmpty()
                    if (text.isNotBlank()) trySend(VoiceEvent.Partial(text))
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            }

            recognizer.setRecognitionListener(listener)
            recognizer.startListening(intent)

            awaitClose {
                runCatching { recognizer.stopListening() }
                runCatching { recognizer.destroy() }
            }
        }

    private fun describe(code: Int): String = when (code) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Recognizer client error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that — try again"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
        SpeechRecognizer.ERROR_SERVER -> "Recognizer server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
        else -> "Recognition failed ($code)"
    }
}
