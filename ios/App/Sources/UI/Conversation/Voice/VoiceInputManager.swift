import Foundation
import Speech
import AVFoundation
import Observation

/**
 * Manages speech recognition for voice input.
 *
 * Features:
 * - Real-time speech recognition using SFSpeechRecognizer
 * - Audio capture with AVAudioEngine
 * - State machine: Idle -> Listening -> Processing -> Result/Error
 * - Partial and final transcription results
 * - Proper error handling with user-friendly messages
 *
 * Spec reference: navcaddy-engine.md R7
 * Plan reference: navcaddy-engine-plan.md Task 21
 */
@Observable
@MainActor
final class VoiceInputManager {

    // MARK: - State

    /// Current state of voice input.
    private(set) var state: VoiceInputState = .idle

    /// Permission manager for checking/requesting permissions.
    private let permissionManager = VoicePermissionManager()

    // MARK: - Speech Recognition

    /// Speech recognizer for the user's locale.
    private var speechRecognizer: SFSpeechRecognizer?

    /// Audio engine for capturing microphone input.
    private var audioEngine: AVAudioEngine?

    /// Current recognition request.
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest?

    /// Current recognition task.
    private var recognitionTask: SFSpeechRecognitionTask?

    /// Delegate for monitoring speech recognizer availability (must be retained).
    private var speechRecognizerDelegate: SpeechRecognizerDelegate?

    // MARK: - Initialization

    init() {
        // Create speech recognizer for user's locale
        self.speechRecognizer = SFSpeechRecognizer(locale: Locale.current)

        // Monitor availability changes (retain delegate)
        let delegate = SpeechRecognizerDelegate(manager: self)
        self.speechRecognizerDelegate = delegate
        self.speechRecognizer?.delegate = delegate
    }

    // MARK: - Public Interface

    /// Check if speech recognition is available.
    var isAvailable: Bool {
        guard let recognizer = speechRecognizer else { return false }
        return recognizer.isAvailable
    }

    /// Start listening for voice input.
    ///
    /// Requests permissions if needed, then starts speech recognition.
    func startListening() async {
        // Check if already listening
        guard state == .idle else {
            return
        }

        // Check availability
        guard isAvailable else {
            state = .error(.notAvailable)
            return
        }

        // Request permissions if needed
        if !permissionManager.isAuthorized {
            let status = await permissionManager.requestAllPermissions()
            if status != .authorized {
                if let error = permissionManager.permissionError() {
                    state = .error(error)
                }
                return
            }
        }

        // Start recognition
        do {
            try await startRecognition()
        } catch {
            handleRecognitionError(error)
        }
    }

    /// Stop listening and finalize transcription.
    func stopListening() {
        guard state != .idle else { return }

        // Stop audio engine
        audioEngine?.stop()
        audioEngine?.inputNode.removeTap(onBus: 0)

        // End recognition request
        recognitionRequest?.endAudio()

        // Clean up will happen in completion handler
    }

    /// Cancel voice input.
    func cancel() {
        guard state != .idle else { return }

        // Cancel recognition task
        recognitionTask?.cancel()

        // Stop audio engine
        audioEngine?.stop()
        audioEngine?.inputNode.removeTap(onBus: 0)

        // Reset state
        cleanup()
        state = .idle
    }

    // MARK: - Private Implementation

    /// Start speech recognition.
    private func startRecognition() async throws {
        // Cancel any ongoing recognition
        recognitionTask?.cancel()
        recognitionTask = nil

        // Configure audio session
        let audioSession = AVAudioSession.sharedInstance()
        try audioSession.setCategory(.record, mode: .measurement, options: .duckOthers)
        try audioSession.setActive(true, options: .notifyOthersOnDeactivation)

        // Create recognition request
        let request = SFSpeechAudioBufferRecognitionRequest()
        recognitionRequest = request

        // Configure request
        request.shouldReportPartialResults = true
        request.requiresOnDeviceRecognition = false // Use server-based for better accuracy

        // Create audio engine
        let engine = AVAudioEngine()
        audioEngine = engine

        let inputNode = engine.inputNode
        let recordingFormat = inputNode.outputFormat(forBus: 0)

        // Install tap on audio input
        inputNode.installTap(onBus: 0, bufferSize: 1024, format: recordingFormat) { buffer, _ in
            request.append(buffer)
        }

        // Start audio engine
        engine.prepare()
        try engine.start()

        // Update state to listening
        state = .listening

        // Start recognition task
        guard let recognizer = speechRecognizer else {
            throw VoiceInputError.notAvailable
        }

        recognitionTask = recognizer.recognitionTask(with: request) { [weak self] result, error in
            Task { @MainActor [weak self] in
                guard let self = self else { return }

                // Handle error
                if let error = error {
                    self.handleRecognitionError(error)
                    return
                }

                guard let result = result else { return }

                // Get transcription
                let transcription = result.bestTranscription.formattedString

                if result.isFinal {
                    // Final result
                    self.state = .result(transcription)
                    self.cleanup()
                } else {
                    // Partial result
                    if self.state == .listening || self.state == .processing {
                        self.state = .partialResult(transcription)
                    }
                }
            }
        }
    }

    /// Handle recognition error.
    private func handleRecognitionError(_ error: Error) {
        cleanup()

        // Map NSError to VoiceInputError
        let nsError = error as NSError

        let voiceError: VoiceInputError
        switch nsError.code {
        case 203: // kLSRErrorCodeRequestCancelled
            voiceError = .cancelled
        case 201, 216: // No speech detected
            voiceError = .noSpeechDetected
        case 300...399: // Audio errors
            voiceError = .audioError
        case 1100...1199: // Network errors
            voiceError = .networkError
        default:
            voiceError = .unknown(error.localizedDescription)
        }

        state = .error(voiceError)
    }

    /// Clean up resources.
    private func cleanup() {
        audioEngine?.stop()
        audioEngine?.inputNode.removeTap(onBus: 0)
        audioEngine = nil

        recognitionRequest = nil
        recognitionTask = nil

        // Deactivate audio session
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    // MARK: - Availability Monitoring

    /// Handle availability change.
    fileprivate func handleAvailabilityChange(available: Bool) {
        if !available && state != .idle {
            state = .error(.notAvailable)
            cleanup()
        }
    }
}

// MARK: - Speech Recognizer Delegate

/// Delegate to monitor speech recognizer availability.
private class SpeechRecognizerDelegate: NSObject, SFSpeechRecognizerDelegate {
    weak var manager: VoiceInputManager?

    init(manager: VoiceInputManager) {
        self.manager = manager
    }

    func speechRecognizer(_ speechRecognizer: SFSpeechRecognizer, availabilityDidChange available: Bool) {
        Task { @MainActor in
            manager?.handleAvailabilityChange(available: available)
        }
    }
}
