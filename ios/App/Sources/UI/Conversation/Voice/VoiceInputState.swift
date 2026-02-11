import Foundation

/**
 * State definitions for voice input.
 *
 * Defines all possible states for voice recognition and related error types.
 *
 * Spec reference: navcaddy-engine.md R7
 * Plan reference: navcaddy-engine-plan.md Task 21
 */

// MARK: - Voice Input State

/// Represents the current state of voice recognition.
enum VoiceInputState: Equatable {
    /// Voice input is not active.
    case idle

    /// Listening for speech input.
    case listening

    /// Processing captured audio (transcription in progress).
    case processing

    /// Partial transcription result available (real-time feedback).
    case partialResult(String)

    /// Final transcription result available.
    case result(String)

    /// Error occurred during voice input.
    case error(VoiceInputError)
}

// MARK: - Voice Input Error

/// Represents voice input errors with user-friendly messages.
enum VoiceInputError: Error, Equatable {
    /// Speech recognition is not available on this device.
    case notAvailable

    /// User denied speech recognition permission.
    case permissionDenied

    /// User denied microphone permission.
    case microphonePermissionDenied

    /// Speech recognition was cancelled by user.
    case cancelled

    /// No speech was detected.
    case noSpeechDetected

    /// Audio recording error.
    case audioError

    /// Speech recognizer is not available for the current locale.
    case localeNotSupported

    /// Network error (if using server-based recognition).
    case networkError

    /// Unknown error with optional message.
    case unknown(String?)

    /// User-friendly error message for display.
    var userMessage: String {
        switch self {
        case .notAvailable:
            return "Speech recognition is not available on this device."
        case .permissionDenied:
            return "Please enable Speech Recognition in Settings to use voice input."
        case .microphonePermissionDenied:
            return "Please enable Microphone access in Settings to use voice input."
        case .cancelled:
            return "Voice input was cancelled."
        case .noSpeechDetected:
            return "No speech detected. Please try again."
        case .audioError:
            return "Audio recording error. Please check your microphone."
        case .localeNotSupported:
            return "Speech recognition is not available for your language."
        case .networkError:
            return "Network error. Please check your connection."
        case .unknown(let message):
            if let message = message {
                return "Voice input error: \(message)"
            }
            return "Voice input error. Please try again."
        }
    }

    /// Whether the error is recoverable by retrying.
    var isRecoverable: Bool {
        switch self {
        case .notAvailable, .permissionDenied, .microphonePermissionDenied, .localeNotSupported:
            return false
        case .cancelled, .noSpeechDetected, .audioError, .networkError, .unknown:
            return true
        }
    }
}

// MARK: - Voice Permission Status

/// Represents the authorization status for voice input.
enum VoicePermissionStatus: Equatable {
    /// Permission status is not determined yet.
    case notDetermined

    /// Permission was denied by user.
    case denied

    /// Permission was granted by user.
    case authorized

    /// Permission is restricted (e.g., parental controls).
    case restricted

    /// Combined status of both speech and microphone permissions.
    static func combined(speech: VoicePermissionStatus, microphone: VoicePermissionStatus) -> VoicePermissionStatus {
        // Both must be authorized for voice input to work
        if speech == .authorized && microphone == .authorized {
            return .authorized
        }

        // If either is denied, consider it denied
        if speech == .denied || microphone == .denied {
            return .denied
        }

        // If either is restricted, consider it restricted
        if speech == .restricted || microphone == .restricted {
            return .restricted
        }

        // Otherwise, not determined
        return .notDetermined
    }
}
