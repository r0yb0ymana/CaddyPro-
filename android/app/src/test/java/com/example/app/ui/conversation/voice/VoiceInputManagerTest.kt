package caddypro.ui.conversation.voice

import android.content.Context
import android.speech.SpeechRecognizer
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for VoiceInputManager.
 *
 * Tests the voice input state machine and error handling.
 *
 * Spec reference: navcaddy-engine.md R7
 * Plan reference: navcaddy-engine-plan.md Task 20
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoiceInputManagerTest {

    private lateinit var context: Context
    private lateinit var permissionHandler: VoicePermissionHandler
    private lateinit var voiceInputManager: VoiceInputManager

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        permissionHandler = mockk(relaxed = true)

        // Mock SpeechRecognizer static methods
        mockkStatic(SpeechRecognizer::class)

        voiceInputManager = VoiceInputManager(context, permissionHandler)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `initial state is Idle`() = runTest {
        // Given: Fresh VoiceInputManager
        // When: Check initial state
        val state = voiceInputManager.state.first()

        // Then: State should be Idle
        assertTrue(state is VoiceInputState.Idle)
    }

    @Test
    fun `isRecognitionAvailable returns true when available`() {
        // Given: SpeechRecognizer is available
        every { SpeechRecognizer.isRecognitionAvailable(context) } returns true

        // When: Check availability
        val isAvailable = voiceInputManager.isRecognitionAvailable()

        // Then: Should return true
        assertTrue(isAvailable)
    }

    @Test
    fun `isRecognitionAvailable returns false when not available`() {
        // Given: SpeechRecognizer is not available
        every { SpeechRecognizer.isRecognitionAvailable(context) } returns false

        // When: Check availability
        val isAvailable = voiceInputManager.isRecognitionAvailable()

        // Then: Should return false
        assertFalse(isAvailable)
    }

    @Test
    fun `startListening without permission emits error state`() = runTest {
        // Given: Permission not granted
        every { permissionHandler.hasRecordAudioPermission() } returns false
        every { permissionHandler.getTemporarilyDeniedMessage() } returns "Permission needed"

        // When: Start listening
        voiceInputManager.startListening()

        // Then: State should be Error with PERMISSION_DENIED
        val state = voiceInputManager.state.first()
        assertTrue(state is VoiceInputState.Error)
        assertEquals(VoiceInputError.PERMISSION_DENIED, (state as VoiceInputState.Error).error)
        assertTrue(state.isRecoverable)
    }

    @Test
    fun `startListening when recognizer not available emits error state`() = runTest {
        // Given: Permission granted but recognizer not available
        every { permissionHandler.hasRecordAudioPermission() } returns true
        every { SpeechRecognizer.isRecognitionAvailable(context) } returns false

        // When: Start listening
        voiceInputManager.startListening()

        // Then: State should be Error with RECOGNIZER_NOT_AVAILABLE
        val state = voiceInputManager.state.first()
        assertTrue(state is VoiceInputState.Error)
        assertEquals(
            VoiceInputError.RECOGNIZER_NOT_AVAILABLE,
            (state as VoiceInputState.Error).error
        )
        assertFalse(state.isRecoverable)
    }

    @Test
    fun `stopListening resets state to Idle`() = runTest {
        // Given: VoiceInputManager in some state
        // When: Stop listening
        voiceInputManager.stopListening()

        // Then: State should be Idle
        val state = voiceInputManager.state.first()
        assertTrue(state is VoiceInputState.Idle)
    }

    @Test
    fun `cancelListening resets state to Idle`() = runTest {
        // Given: VoiceInputManager in some state
        // When: Cancel listening
        voiceInputManager.cancelListening()

        // Then: State should be Idle
        val state = voiceInputManager.state.first()
        assertTrue(state is VoiceInputState.Idle)
    }

    @Test
    fun `resetState resets to Idle`() = runTest {
        // Given: VoiceInputManager in some state
        // When: Reset state
        voiceInputManager.resetState()

        // Then: State should be Idle
        val state = voiceInputManager.state.first()
        assertTrue(state is VoiceInputState.Idle)
    }
}
