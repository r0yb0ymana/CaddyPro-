package caddypro.ui.conversation.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages voice input using Android SpeechRecognizer API.
 *
 * Implements a state machine for voice input lifecycle:
 * - Idle: Ready to start
 * - Listening: Actively recording
 * - Processing: Converting speech to text
 * - Result: Transcription complete
 * - Error: Something went wrong
 *
 * Spec reference: navcaddy-engine.md R7
 * Plan reference: navcaddy-engine-plan.md Task 20
 */
@Singleton
class VoiceInputManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionHandler: VoicePermissionHandler
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val _state = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)
    val state: StateFlow<VoiceInputState> = _state.asStateFlow()

    /**
     * Check if speech recognition is available on the device.
     *
     * @return true if SpeechRecognizer is available, false otherwise
     */
    fun isRecognitionAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Start voice input.
     *
     * Checks permissions and availability before starting recognition.
     * Emits state changes via StateFlow.
     */
    fun startListening() {
        // Check if already listening
        if (_state.value is VoiceInputState.Listening) {
            return
        }

        // Check permission
        if (!permissionHandler.hasRecordAudioPermission()) {
            _state.value = VoiceInputState.Error(
                error = VoiceInputError.PERMISSION_DENIED,
                message = permissionHandler.getTemporarilyDeniedMessage(),
                isRecoverable = true
            )
            return
        }

        // Check if recognition is available
        if (!isRecognitionAvailable()) {
            _state.value = VoiceInputState.Error(
                error = VoiceInputError.RECOGNIZER_NOT_AVAILABLE,
                message = "Speech recognition is not available on this device.",
                isRecoverable = false
            )
            return
        }

        try {
            // Destroy existing recognizer if present
            destroyRecognizer()

            // Create new recognizer
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }

            // Create recognition intent
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                // Add a prompt for user guidance
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak to Bones")
            }

            // Start listening
            speechRecognizer?.startListening(intent)
            _state.value = VoiceInputState.Listening()

        } catch (e: Exception) {
            _state.value = VoiceInputState.Error(
                error = VoiceInputError.UNKNOWN,
                message = "Failed to start voice input. Please try again.",
                isRecoverable = true
            )
        }
    }

    /**
     * Stop voice input.
     *
     * Stops the current recognition session and returns to idle state.
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            // Ignore errors during stop
        }
        destroyRecognizer()
        _state.value = VoiceInputState.Idle
    }

    /**
     * Cancel voice input.
     *
     * Cancels the current recognition session immediately.
     */
    fun cancelListening() {
        try {
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            // Ignore errors during cancel
        }
        destroyRecognizer()
        _state.value = VoiceInputState.Idle
    }

    /**
     * Reset state to idle.
     *
     * Clears any error or result state and returns to idle.
     */
    fun resetState() {
        destroyRecognizer()
        _state.value = VoiceInputState.Idle
    }

    /**
     * Destroy the speech recognizer instance.
     *
     * Properly cleans up the recognizer to prevent memory leaks.
     */
    private fun destroyRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            // Ignore errors during destroy
        } finally {
            speechRecognizer = null
        }
    }

    /**
     * RecognitionListener implementation.
     *
     * Handles all speech recognition callbacks and updates state accordingly.
     */
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = VoiceInputState.Listening()
        }

        override fun onBeginningOfSpeech() {
            _state.value = VoiceInputState.Listening()
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Audio level changes - could be used for visual feedback
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Raw audio buffer - not needed for our use case
        }

        override fun onEndOfSpeech() {
            _state.value = VoiceInputState.Processing
        }

        override fun onError(error: Int) {
            val voiceError = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> VoiceInputError.AUDIO_ERROR
                SpeechRecognizer.ERROR_CLIENT -> VoiceInputError.RECOGNITION_ERROR
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> VoiceInputError.INSUFFICIENT_PERMISSIONS
                SpeechRecognizer.ERROR_NETWORK -> VoiceInputError.NETWORK_ERROR
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> VoiceInputError.TIMEOUT
                SpeechRecognizer.ERROR_NO_MATCH -> VoiceInputError.NO_SPEECH
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> VoiceInputError.SERVICE_BUSY
                SpeechRecognizer.ERROR_SERVER -> VoiceInputError.SERVER_ERROR
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> VoiceInputError.TIMEOUT
                else -> VoiceInputError.UNKNOWN
            }

            val message = when (voiceError) {
                VoiceInputError.AUDIO_ERROR -> "Audio recording error. Please try again."
                VoiceInputError.RECOGNITION_ERROR -> "Recognition error. Please try again."
                VoiceInputError.INSUFFICIENT_PERMISSIONS -> permissionHandler.getPermanentlyDeniedMessage()
                VoiceInputError.NETWORK_ERROR -> "Network error. Please check your connection and try again."
                VoiceInputError.TIMEOUT -> "No speech detected. Please try again."
                VoiceInputError.NO_SPEECH -> "No speech detected. Please speak clearly and try again."
                VoiceInputError.SERVICE_BUSY -> "Speech service is busy. Please try again in a moment."
                VoiceInputError.SERVER_ERROR -> "Server error. Please try again."
                else -> "Voice input failed. Please try again."
            }

            val isRecoverable = voiceError != VoiceInputError.INSUFFICIENT_PERMISSIONS

            _state.value = VoiceInputState.Error(
                error = voiceError,
                message = message,
                isRecoverable = isRecoverable
            )

            destroyRecognizer()
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val transcription = matches?.firstOrNull() ?: ""

            if (transcription.isNotBlank()) {
                _state.value = VoiceInputState.Result(transcription)
            } else {
                _state.value = VoiceInputState.Error(
                    error = VoiceInputError.NO_SPEECH,
                    message = "No speech detected. Please try again.",
                    isRecoverable = true
                )
            }

            destroyRecognizer()
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val partialText = matches?.firstOrNull() ?: ""

            // Update listening state with partial result
            if (_state.value is VoiceInputState.Listening) {
                _state.value = VoiceInputState.Listening(partialText)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Additional events - not needed for our use case
        }
    }
}
