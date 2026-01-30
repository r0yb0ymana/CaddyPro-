import Foundation
import Speech
import AVFoundation

/**
 * Manages permissions for speech recognition and microphone access.
 *
 * Handles requesting and checking permissions for:
 * - Speech Recognition (SFSpeechRecognizer.requestAuthorization)
 * - Microphone (AVAudioSession.recordPermission)
 *
 * Spec reference: navcaddy-engine.md R7
 * Plan reference: navcaddy-engine-plan.md Task 21
 */
@MainActor
final class VoicePermissionManager {

    // MARK: - Permission Status

    /// Check current speech recognition authorization status.
    var speechRecognitionStatus: VoicePermissionStatus {
        switch SFSpeechRecognizer.authorizationStatus() {
        case .notDetermined:
            return .notDetermined
        case .denied:
            return .denied
        case .restricted:
            return .restricted
        case .authorized:
            return .authorized
        @unknown default:
            return .notDetermined
        }
    }

    /// Check current microphone authorization status.
    var microphoneStatus: VoicePermissionStatus {
        switch AVAudioApplication.shared.recordPermission {
        case .undetermined:
            return .notDetermined
        case .denied:
            return .denied
        case .granted:
            return .authorized
        @unknown default:
            return .notDetermined
        }
    }

    /// Combined authorization status (both speech and microphone).
    var combinedStatus: VoicePermissionStatus {
        VoicePermissionStatus.combined(
            speech: speechRecognitionStatus,
            microphone: microphoneStatus
        )
    }

    /// Whether voice input is currently authorized.
    var isAuthorized: Bool {
        combinedStatus == .authorized
    }

    // MARK: - Permission Requests

    /// Request speech recognition permission.
    ///
    /// - Returns: The granted authorization status.
    func requestSpeechRecognitionPermission() async -> VoicePermissionStatus {
        await withCheckedContinuation { continuation in
            SFSpeechRecognizer.requestAuthorization { status in
                Task { @MainActor in
                    let permissionStatus: VoicePermissionStatus
                    switch status {
                    case .notDetermined:
                        permissionStatus = .notDetermined
                    case .denied:
                        permissionStatus = .denied
                    case .restricted:
                        permissionStatus = .restricted
                    case .authorized:
                        permissionStatus = .authorized
                    @unknown default:
                        permissionStatus = .notDetermined
                    }
                    continuation.resume(returning: permissionStatus)
                }
            }
        }
    }

    /// Request microphone permission.
    ///
    /// - Returns: The granted authorization status.
    func requestMicrophonePermission() async -> VoicePermissionStatus {
        let granted = await AVAudioApplication.requestRecordPermission()
        return granted ? .authorized : .denied
    }

    /// Request all required permissions for voice input.
    ///
    /// Requests both speech recognition and microphone permissions.
    ///
    /// - Returns: Combined authorization status.
    func requestAllPermissions() async -> VoicePermissionStatus {
        // Request speech recognition first
        let speechStatus = await requestSpeechRecognitionPermission()

        guard speechStatus == .authorized else {
            return speechStatus
        }

        // Then request microphone
        let microphoneStatus = await requestMicrophonePermission()

        return VoicePermissionStatus.combined(
            speech: speechStatus,
            microphone: microphoneStatus
        )
    }

    // MARK: - Permission Checks

    /// Check if all permissions are granted.
    ///
    /// - Returns: `true` if both speech and microphone are authorized.
    func checkPermissions() -> Bool {
        return isAuthorized
    }

    /// Get error for current permission state, if any.
    ///
    /// - Returns: VoiceInputError if permissions are not granted, nil otherwise.
    func permissionError() -> VoiceInputError? {
        let speechStatus = speechRecognitionStatus
        let micStatus = microphoneStatus

        // Check speech recognition permission
        if speechStatus == .denied || speechStatus == .restricted {
            return .permissionDenied
        }

        // Check microphone permission
        if micStatus == .denied || micStatus == .restricted {
            return .microphonePermissionDenied
        }

        // Not determined yet
        if speechStatus == .notDetermined || micStatus == .notDetermined {
            return .permissionDenied
        }

        return nil
    }
}
