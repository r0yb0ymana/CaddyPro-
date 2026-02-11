import XCTest
@testable import App
import Speech
import AVFoundation

/**
 * Unit tests for voice input components.
 *
 * Tests:
 * - VoiceInputState state machine
 * - VoicePermissionManager permission checks
 * - VoiceInputManager state transitions
 * - Error handling and user messages
 *
 * Spec reference: navcaddy-engine.md R7
 * Plan reference: navcaddy-engine-plan.md Task 21
 */
@MainActor
final class VoiceInputTests: XCTestCase {

    // MARK: - VoiceInputError Tests

    func testVoiceInputErrorMessages() {
        // Test all error types have user-friendly messages
        XCTAssertEqual(
            VoiceInputError.notAvailable.userMessage,
            "Speech recognition is not available on this device."
        )

        XCTAssertEqual(
            VoiceInputError.permissionDenied.userMessage,
            "Please enable Speech Recognition in Settings to use voice input."
        )

        XCTAssertEqual(
            VoiceInputError.microphonePermissionDenied.userMessage,
            "Please enable Microphone access in Settings to use voice input."
        )

        XCTAssertEqual(
            VoiceInputError.cancelled.userMessage,
            "Voice input was cancelled."
        )

        XCTAssertEqual(
            VoiceInputError.noSpeechDetected.userMessage,
            "No speech detected. Please try again."
        )

        XCTAssertEqual(
            VoiceInputError.audioError.userMessage,
            "Audio recording error. Please check your microphone."
        )

        XCTAssertEqual(
            VoiceInputError.localeNotSupported.userMessage,
            "Speech recognition is not available for your language."
        )

        XCTAssertEqual(
            VoiceInputError.networkError.userMessage,
            "Network error. Please check your connection."
        )

        XCTAssertEqual(
            VoiceInputError.unknown(nil).userMessage,
            "Voice input error. Please try again."
        )

        XCTAssertEqual(
            VoiceInputError.unknown("Test error").userMessage,
            "Voice input error: Test error"
        )
    }

    func testVoiceInputErrorRecoverability() {
        // Non-recoverable errors
        XCTAssertFalse(VoiceInputError.notAvailable.isRecoverable)
        XCTAssertFalse(VoiceInputError.permissionDenied.isRecoverable)
        XCTAssertFalse(VoiceInputError.microphonePermissionDenied.isRecoverable)
        XCTAssertFalse(VoiceInputError.localeNotSupported.isRecoverable)

        // Recoverable errors
        XCTAssertTrue(VoiceInputError.cancelled.isRecoverable)
        XCTAssertTrue(VoiceInputError.noSpeechDetected.isRecoverable)
        XCTAssertTrue(VoiceInputError.audioError.isRecoverable)
        XCTAssertTrue(VoiceInputError.networkError.isRecoverable)
        XCTAssertTrue(VoiceInputError.unknown(nil).isRecoverable)
        XCTAssertTrue(VoiceInputError.unknown("Test").isRecoverable)
    }

    // MARK: - VoicePermissionStatus Tests

    func testCombinedPermissionStatus_bothAuthorized() {
        let status = VoicePermissionStatus.combined(
            speech: .authorized,
            microphone: .authorized
        )
        XCTAssertEqual(status, .authorized)
    }

    func testCombinedPermissionStatus_speechDenied() {
        let status = VoicePermissionStatus.combined(
            speech: .denied,
            microphone: .authorized
        )
        XCTAssertEqual(status, .denied)
    }

    func testCombinedPermissionStatus_microphoneDenied() {
        let status = VoicePermissionStatus.combined(
            speech: .authorized,
            microphone: .denied
        )
        XCTAssertEqual(status, .denied)
    }

    func testCombinedPermissionStatus_bothDenied() {
        let status = VoicePermissionStatus.combined(
            speech: .denied,
            microphone: .denied
        )
        XCTAssertEqual(status, .denied)
    }

    func testCombinedPermissionStatus_speechRestricted() {
        let status = VoicePermissionStatus.combined(
            speech: .restricted,
            microphone: .authorized
        )
        XCTAssertEqual(status, .restricted)
    }

    func testCombinedPermissionStatus_microphoneRestricted() {
        let status = VoicePermissionStatus.combined(
            speech: .authorized,
            microphone: .restricted
        )
        XCTAssertEqual(status, .restricted)
    }

    func testCombinedPermissionStatus_notDetermined() {
        let status = VoicePermissionStatus.combined(
            speech: .notDetermined,
            microphone: .authorized
        )
        XCTAssertEqual(status, .notDetermined)
    }

    func testCombinedPermissionStatus_bothNotDetermined() {
        let status = VoicePermissionStatus.combined(
            speech: .notDetermined,
            microphone: .notDetermined
        )
        XCTAssertEqual(status, .notDetermined)
    }

    // MARK: - VoicePermissionManager Tests

    func testPermissionManager_isAuthorized() {
        let manager = VoicePermissionManager()

        // Authorization depends on system state
        // Just verify the property is accessible
        _ = manager.isAuthorized
    }

    func testPermissionManager_combinedStatus() {
        let manager = VoicePermissionManager()

        // Combined status should be computed from speech + microphone
        let combined = manager.combinedStatus
        XCTAssertTrue(
            [.notDetermined, .denied, .restricted, .authorized].contains(combined),
            "Combined status should be one of the valid states"
        )
    }

    func testPermissionManager_permissionError_whenAuthorized() {
        let manager = VoicePermissionManager()

        // If not authorized, should return an error
        if !manager.isAuthorized {
            let error = manager.permissionError()
            XCTAssertNotNil(error)
        }
    }

    // MARK: - VoiceInputState Tests

    func testVoiceInputState_idle() {
        let state = VoiceInputState.idle
        XCTAssertEqual(state, .idle)
    }

    func testVoiceInputState_listening() {
        let state = VoiceInputState.listening
        XCTAssertEqual(state, .listening)
    }

    func testVoiceInputState_processing() {
        let state = VoiceInputState.processing
        XCTAssertEqual(state, .processing)
    }

    func testVoiceInputState_partialResult() {
        let transcription = "Test transcription"
        let state = VoiceInputState.partialResult(transcription)

        if case .partialResult(let text) = state {
            XCTAssertEqual(text, transcription)
        } else {
            XCTFail("Expected partialResult state")
        }
    }

    func testVoiceInputState_result() {
        let transcription = "Final transcription"
        let state = VoiceInputState.result(transcription)

        if case .result(let text) = state {
            XCTAssertEqual(text, transcription)
        } else {
            XCTFail("Expected result state")
        }
    }

    func testVoiceInputState_error() {
        let error = VoiceInputError.noSpeechDetected
        let state = VoiceInputState.error(error)

        if case .error(let err) = state {
            XCTAssertEqual(err, error)
        } else {
            XCTFail("Expected error state")
        }
    }

    func testVoiceInputState_equality() {
        // Test equality for different states
        XCTAssertEqual(VoiceInputState.idle, .idle)
        XCTAssertEqual(VoiceInputState.listening, .listening)
        XCTAssertEqual(VoiceInputState.processing, .processing)
        XCTAssertEqual(
            VoiceInputState.partialResult("test"),
            .partialResult("test")
        )
        XCTAssertEqual(
            VoiceInputState.result("test"),
            .result("test")
        )
        XCTAssertEqual(
            VoiceInputState.error(.cancelled),
            .error(.cancelled)
        )

        // Test inequality
        XCTAssertNotEqual(VoiceInputState.idle, .listening)
        XCTAssertNotEqual(
            VoiceInputState.partialResult("test1"),
            .partialResult("test2")
        )
        XCTAssertNotEqual(
            VoiceInputState.error(.cancelled),
            .error(.networkError)
        )
    }

    // MARK: - VoiceInputManager Tests

    func testVoiceInputManager_initialState() {
        let manager = VoiceInputManager()
        XCTAssertEqual(manager.state, .idle)
    }

    func testVoiceInputManager_isAvailable() {
        let manager = VoiceInputManager()

        // Availability depends on device and locale
        // Just verify the property is accessible
        _ = manager.isAvailable
    }

    func testVoiceInputManager_cancel_fromIdle() {
        let manager = VoiceInputManager()

        // Cancel from idle should remain idle
        manager.cancel()
        XCTAssertEqual(manager.state, .idle)
    }

    // MARK: - State Transition Tests

    func testStateTransitions_idle_to_error_whenNotAvailable() async {
        let manager = VoiceInputManager()

        // If speech recognition is not available, should go to error state
        if !manager.isAvailable {
            await manager.startListening()

            // Should be in error state
            if case .error(let error) = manager.state {
                XCTAssertEqual(error, .notAvailable)
            } else {
                XCTFail("Expected error state when not available")
            }
        }
    }

    // MARK: - Integration Tests

    func testVoiceInputError_equatable() {
        // Test equality
        XCTAssertEqual(VoiceInputError.notAvailable, .notAvailable)
        XCTAssertEqual(VoiceInputError.permissionDenied, .permissionDenied)
        XCTAssertEqual(VoiceInputError.cancelled, .cancelled)
        XCTAssertEqual(VoiceInputError.unknown("test"), .unknown("test"))

        // Test inequality
        XCTAssertNotEqual(VoiceInputError.notAvailable, .permissionDenied)
        XCTAssertNotEqual(VoiceInputError.unknown("test1"), .unknown("test2"))
        XCTAssertNotEqual(VoiceInputError.unknown("test"), .unknown(nil))
    }

    func testVoicePermissionStatus_equatable() {
        // Test equality
        XCTAssertEqual(VoicePermissionStatus.notDetermined, .notDetermined)
        XCTAssertEqual(VoicePermissionStatus.denied, .denied)
        XCTAssertEqual(VoicePermissionStatus.authorized, .authorized)
        XCTAssertEqual(VoicePermissionStatus.restricted, .restricted)

        // Test inequality
        XCTAssertNotEqual(VoicePermissionStatus.notDetermined, .denied)
        XCTAssertNotEqual(VoicePermissionStatus.authorized, .restricted)
    }

    // MARK: - Error Message Completeness Tests

    func testAllErrorsHaveMessages() {
        // Ensure all error types have non-empty user messages
        let errors: [VoiceInputError] = [
            .notAvailable,
            .permissionDenied,
            .microphonePermissionDenied,
            .cancelled,
            .noSpeechDetected,
            .audioError,
            .localeNotSupported,
            .networkError,
            .unknown(nil),
            .unknown("Test error")
        ]

        for error in errors {
            XCTAssertFalse(
                error.userMessage.isEmpty,
                "Error \(error) should have a non-empty user message"
            )
        }
    }

    func testErrorMessagesAreUserFriendly() {
        // Verify error messages don't contain technical jargon
        let errors: [VoiceInputError] = [
            .notAvailable,
            .permissionDenied,
            .microphonePermissionDenied,
            .cancelled,
            .noSpeechDetected,
            .audioError,
            .localeNotSupported,
            .networkError
        ]

        for error in errors {
            let message = error.userMessage
            // Messages should not contain code-like terms
            XCTAssertFalse(
                message.contains("null") || message.contains("nil"),
                "Error message should be user-friendly: \(message)"
            )
        }
    }
}
