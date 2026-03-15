package com.openbiblescholar.services.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

data class TtsPlaybackState(
    val isInitialized: Boolean = false,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isSpeaking: Boolean = false,
    val currentUtteranceId: String = "",
    val currentWordStart: Int = -1,
    val currentWordEnd: Int = -1,
    val errorMessage: String? = null,
    val availableVoices: List<Voice> = emptyList()
)

@Singleton
class BibleTtsService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var tts: TextToSpeech? = null

    private val _state = MutableStateFlow(TtsPlaybackState())
    val state: StateFlow<TtsPlaybackState> = _state.asStateFlow()

    private var speed: Float = 1.0f
    private var pitch: Float = 1.0f
    private var onComplete: (() -> Unit)? = null
    private var utteranceQueue = mutableListOf<Pair<String, String>>() // id -> text
    private var currentQueueIndex = 0

    fun initialize(onReady: (Boolean) -> Unit = {}) {
        if (tts != null) { onReady(true); return }

        tts = TextToSpeech(context) { status ->
            val ok = status == TextToSpeech.SUCCESS
            if (ok) {
                setupProgressListener()
                tts?.language = Locale.US
                _state.value = _state.value.copy(
                    isInitialized = true,
                    availableVoices = tts?.voices?.toList() ?: emptyList()
                )
            } else {
                _state.value = _state.value.copy(errorMessage = "TTS engine unavailable. Please install a TTS engine in system settings.")
            }
            onReady(ok)
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                _state.value = _state.value.copy(
                    isSpeaking = true, isPlaying = true, currentUtteranceId = utteranceId
                )
            }

            override fun onDone(utteranceId: String) {
                _state.value = _state.value.copy(isSpeaking = false, currentWordStart = -1, currentWordEnd = -1)
                onComplete?.invoke()
            }

            override fun onError(utteranceId: String) {
                _state.value = _state.value.copy(
                    isSpeaking = false, isPlaying = false,
                    errorMessage = "TTS error on utterance: $utteranceId"
                )
            }

            // Word-level highlighting (API 26+)
            override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
                _state.value = _state.value.copy(currentWordStart = start, currentWordEnd = end)
            }
        })
    }

    fun speak(text: String, utteranceId: String = "obs_${System.currentTimeMillis()}", onDone: (() -> Unit)? = null) {
        if (tts == null) initialize { if (it) speak(text, utteranceId, onDone) }

        onComplete = onDone
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        tts?.apply {
            setSpeechRate(speed)
            setPitch(pitch)
            speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
        _state.value = _state.value.copy(isPlaying = true, isPaused = false)
    }

    fun speakQueue(items: List<Pair<String, String>>, onAllDone: (() -> Unit)? = null) {
        if (items.isEmpty()) return
        utteranceQueue = items.toMutableList()
        currentQueueIndex = 0

        fun speakNext() {
            if (currentQueueIndex >= utteranceQueue.size) {
                onAllDone?.invoke()
                _state.value = _state.value.copy(isPlaying = false)
                return
            }
            val (id, text) = utteranceQueue[currentQueueIndex++]
            speak(text, id) { speakNext() }
        }
        speakNext()
    }

    fun pause() {
        tts?.stop()
        _state.value = _state.value.copy(isPlaying = false, isPaused = true, isSpeaking = false)
    }

    fun resume() {
        // Re-speak current item from queue if paused
        _state.value = _state.value.copy(isPlaying = true, isPaused = false)
    }

    fun stop() {
        tts?.stop()
        utteranceQueue.clear()
        currentQueueIndex = 0
        _state.value = _state.value.copy(
            isPlaying = false, isPaused = false, isSpeaking = false,
            currentWordStart = -1, currentWordEnd = -1
        )
    }

    fun setSpeed(speed: Float) {
        this.speed = speed.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(this.speed)
    }

    fun setPitch(pitch: Float) {
        this.pitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setPitch(this.pitch)
    }

    fun setVoice(voice: Voice) {
        tts?.voice = voice
    }

    fun getAvailableVoices(): List<Voice> = tts?.voices?.toList() ?: emptyList()

    fun isPlaying() = _state.value.isPlaying

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        _state.value = TtsPlaybackState()
    }
}
