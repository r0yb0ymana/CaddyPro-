import Foundation
import Observation

/**
 * ViewModel for the conversation screen.
 *
 * Integrates the full NavCaddy pipeline:
 * 1. Intent classification
 * 2. Routing orchestration
 * 3. Session context management
 * 4. Navigation execution
 * 5. Response formatting with Bones persona
 * 6. Voice input with speech recognition (Task 21)
 * 7. Analytics and observability (Task 22)
 *
 * Spec reference: navcaddy-engine.md R1, R2, R3, R4, R7, R8
 * Plan reference: navcaddy-engine-plan.md Task 19, Task 21, Task 22
 */
@Observable
@MainActor
final class ConversationViewModel {
    // MARK: - State

    private(set) var state = ConversationState()

    // MARK: - Dependencies

    private let intentClassifier: IntentClassifier
    private let routingOrchestrator: RoutingOrchestrator
    private let sessionContextManager: SessionContextManager
    private let navigationExecutor: NavigationExecutor
    private let responseFormatter: BonesResponseFormatter
    private let voiceInputManager: VoiceInputManager
    private let analytics: NavCaddyAnalytics

    // MARK: - Session

    private let sessionId: String

    // MARK: - Initialization

    init(dependencies: DependencyContainer = .shared) {
        // Create classifier with Gemini client
        let llmClient = GeminiClient(apiKey: "PLACEHOLDER_API_KEY") // TODO: Move to secure storage
        let classifier = IntentClassifier(llmClient: llmClient)

        // Create prerequisite checker
        let prerequisiteChecker = PrerequisiteChecker()

        // Generate session ID
        self.sessionId = UUID().uuidString

        // Assign dependencies
        self.intentClassifier = classifier
        self.routingOrchestrator = RoutingOrchestrator(prerequisiteChecker: prerequisiteChecker)
        self.sessionContextManager = dependencies.sessionContextManager
        self.navigationExecutor = dependencies.navigationExecutor
        self.responseFormatter = BonesResponseFormatter()
        self.voiceInputManager = VoiceInputManager()
        self.analytics = dependencies.analytics

        // Add welcome message
        addAssistantMessage(
            "Hi! I'm Bones, your digital caddy. Ask me about club selection, " +
            "check your recovery, enter scores, or get coaching tips. How can I help?"
        )

        // Start monitoring voice input state
        startMonitoringVoiceInput()
    }

    // MARK: - Action Handling

    /// Handle user actions.
    func handle(_ action: ConversationAction) {
        switch action {
        case .sendMessage(let text):
            handleSendMessage(text)
        case .updateInput(let text):
            updateInput(text)
        case .startVoiceInput:
            startVoiceInput()
        case .stopVoiceInput:
            stopVoiceInput()
        case .voiceInputComplete(let transcription):
            handleVoiceInputComplete(transcription)
        case .voiceInputError(let error):
            handleVoiceInputError(error)
        case .selectSuggestion(let intentType, let label):
            handleSuggestionSelected(intentType: intentType, label: label)
        case .dismissError:
            dismissError()
        case .confirmIntent(let intentId):
            handleConfirmIntent(intentId)
        case .rejectIntent(let intentId):
            handleRejectIntent(intentId)
        case .retry:
            handleRetry()
        case .clearConversation:
            clearConversation()
        }
    }

    // MARK: - Private Handlers

    /// Handle sending a text message.
    private func handleSendMessage(_ text: String) {
        let trimmedText = text.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmedText.isEmpty else { return }

        // Add user message to conversation
        addUserMessage(trimmedText)

        // Clear input
        state.currentInput = ""

        // Track input received event
        analytics.log(.inputReceived(InputReceivedEvent(
            timestamp: Date(),
            inputType: .text,
            sessionId: sessionId
        )))

        // Process the message
        processUserInput(trimmedText)
    }

    /// Process user input through the NavCaddy pipeline.
    private func processUserInput(_ input: String) {
        Task {
            let pipelineStartTime = Date()

            do {
                // Show loading state
                state.isLoading = true

                // Get session context
                let context = await sessionContextManager.getCurrentContext()

                // Classify intent (with latency tracking)
                let classificationStartTime = Date()
                let classificationResult = await intentClassifier.classify(input: input, context: context)
                let classificationLatency = Int(Date().timeIntervalSince(classificationStartTime) * 1000)

                // Track classification latency
                analytics.trackLatency(
                    operation: "intent_classification",
                    latencyMs: classificationLatency,
                    sessionId: sessionId
                )

                // Handle classification result
                switch classificationResult {
                case .route(let intent, let target):
                    // Track classified intent
                    analytics.log(.intentClassified(IntentClassifiedEvent(
                        intent: intent.intentType.rawValue,
                        confidence: intent.confidence,
                        latencyMs: classificationLatency,
                        sessionId: sessionId
                    )))

                    await handleRouteClassification(intent: intent, target: target)

                case .confirm(let intent, let message):
                    // Track classified intent (confirmation required)
                    analytics.log(.intentClassified(IntentClassifiedEvent(
                        intent: intent.intentType.rawValue,
                        confidence: intent.confidence,
                        latencyMs: classificationLatency,
                        sessionId: sessionId
                    )))

                    handleConfirmClassification(intent: intent, message: message)

                case .clarify(let response):
                    // Track clarification (low confidence)
                    analytics.log(.intentClassified(IntentClassifiedEvent(
                        intent: "CLARIFY",
                        confidence: 0.0,
                        latencyMs: classificationLatency,
                        sessionId: sessionId
                    )))

                    handleClarifyClassification(response: response)

                case .error(let error):
                    // Track error
                    analytics.log(.errorOccurred(ErrorOccurredEvent(
                        errorType: .classification,
                        message: error.userMessage,
                        isRecoverable: error.isRecoverable,
                        sessionId: sessionId
                    )))

                    handleErrorClassification(error: error)
                }

                // Update session context with this turn
                let lastAssistantMessage = state.messages.last(where: {
                    if case .assistant = $0 { return true }
                    return false
                })

                if case .assistant(let msg) = lastAssistantMessage {
                    await sessionContextManager.addConversationTurn(
                        userInput: input,
                        assistantResponse: msg.text
                    )
                }

            } catch {
                // Track unexpected error
                analytics.log(.errorOccurred(ErrorOccurredEvent(
                    errorType: .unknown,
                    message: error.localizedDescription,
                    isRecoverable: true,
                    sessionId: sessionId
                )))

                addErrorMessage(
                    message: "Something went wrong. Please try again.",
                    isRecoverable: true
                )
            }

            // Hide loading state
            state.isLoading = false

            // Track total pipeline latency
            let totalLatency = Int(Date().timeIntervalSince(pipelineStartTime) * 1000)
            analytics.trackLatency(
                operation: "total_pipeline",
                latencyMs: totalLatency,
                sessionId: sessionId
            )
        }
    }

    /// Handle high-confidence route classification.
    private func handleRouteClassification(intent: ParsedIntent, target: RoutingTarget) async {
        let routingStartTime = Date()

        // Route through orchestrator
        let routingResult = await routingOrchestrator.route(.route(intent: intent, target: target))

        let routingLatency = Int(Date().timeIntervalSince(routingStartTime) * 1000)

        switch routingResult {
        case .navigate(let target, _):
            // Track routing execution
            analytics.trackLatency(
                operation: "routing",
                latencyMs: routingLatency,
                sessionId: sessionId
            )

            analytics.log(.routeExecuted(RouteExecutedEvent(
                module: target.module.rawValue,
                screen: target.screen,
                latencyMs: routingLatency,
                sessionId: sessionId
            )))

            // Navigate to target
            let destination = NavCaddyDestination.from(routingTarget: target)
            await navigationExecutor.navigate(to: destination)
            // Add confirmation message
            addAssistantMessage("Let's go to \(target.module.rawValue.lowercased()).")

        case .noNavigation(_, let response):
            // Display response without navigation
            addAssistantMessage(response)

        case .prerequisiteMissing(_, _, let message):
            // Display prerequisite message
            addAssistantMessage(message)

        case .confirmationRequired(_, let message):
            // Display confirmation message
            addAssistantMessage(message)
        }
    }

    /// Handle mid-confidence confirmation classification.
    private func handleConfirmClassification(intent: ParsedIntent, message: String) {
        addAssistantMessage(message)
    }

    /// Handle low-confidence clarification classification.
    private func handleClarifyClassification(response: ClarificationResponse) {
        // Create clarification suggestions
        let suggestions = response.suggestions.map { intentSuggestion in
            ClarificationSuggestion(
                intentType: intentSuggestion.intentType,
                label: intentSuggestion.label,
                description: intentSuggestion.description
            )
        }

        // Add clarification message with chips
        addClarificationMessage(
            message: response.message,
            suggestions: suggestions
        )
    }

    /// Handle classification error.
    private func handleErrorClassification(error: LLMError) {
        let errorMessage: String
        let errorType: ErrorOccurredEvent.ErrorType

        switch error {
        case .networkError:
            errorMessage = "Network error. Please check your connection and try again."
            errorType = .network
        case .timeout:
            errorMessage = "Request timed out. Please try again."
            errorType = .timeout
        case .apiError(let code, let message):
            errorMessage = "API error (\(code)): \(message)"
            errorType = .classification
        case .parseError:
            errorMessage = "I couldn't understand the response. Please try rephrasing."
            errorType = .classification
        case .unauthorized:
            errorMessage = "Authentication error. Please check your settings."
            errorType = .classification
        case .unknown(let message):
            errorMessage = "Error: \(message)"
            errorType = .unknown
        }

        addErrorMessage(errorMessage, isRecoverable: true)
    }

    /// Handle suggestion chip selection.
    private func handleSuggestionSelected(intentType: IntentType, label: String) {
        // Add user message showing selection
        addUserMessage(label)

        // Process the selected intent as if user typed it
        processUserInput(label)
    }

    /// Handle intent confirmation.
    private func handleConfirmIntent(_ intentId: String) {
        addUserMessage("Yes")
        // TODO: Process confirmed intent
        addAssistantMessage("Great! Let me help you with that.")
    }

    /// Handle intent rejection.
    private func handleRejectIntent(_ intentId: String) {
        addUserMessage("No")
        addAssistantMessage("No problem. What did you need help with?")
    }

    /// Update input text.
    private func updateInput(_ text: String) {
        state.currentInput = text
    }

    // MARK: - Voice Input (Task 21)

    /// Start monitoring voice input state changes.
    private func startMonitoringVoiceInput() {
        Task {
            // Monitor voice input state and update UI accordingly
            await observeVoiceInputState()
        }
    }

    /// Observe voice input state changes.
    private func observeVoiceInputState() async {
        // Use withObservationTracking for reactive updates
        withObservationTracking {
            let _ = voiceInputManager.state
        } onChange: {
            Task { @MainActor in
                self.handleVoiceInputStateChange()
                await self.observeVoiceInputState()
            }
        }
    }

    /// Handle voice input state changes.
    private func handleVoiceInputStateChange() {
        let voiceState = voiceInputManager.state

        switch voiceState {
        case .idle:
            state.isVoiceInputActive = false
            state.currentInput = ""

        case .listening, .processing:
            state.isVoiceInputActive = true

        case .partialResult(let transcription):
            // Update input field with partial transcription
            state.currentInput = transcription

        case .result(let transcription):
            // Final result - send message
            state.isVoiceInputActive = false
            if !transcription.isEmpty {
                // Track voice input event
                analytics.log(.inputReceived(InputReceivedEvent(
                    timestamp: Date(),
                    inputType: .voice,
                    sessionId: sessionId
                )))

                handleSendMessage(transcription)
            }

        case .error(let error):
            // Track voice input error
            analytics.log(.errorOccurred(ErrorOccurredEvent(
                errorType: .transcription,
                message: error.userMessage,
                isRecoverable: error.isRecoverable,
                sessionId: sessionId
            )))

            // Handle voice input error
            state.isVoiceInputActive = false
            addErrorMessage(error.userMessage, isRecoverable: error.isRecoverable)
        }
    }

    /// Start voice input.
    private func startVoiceInput() {
        Task {
            let startTime = Date()
            await voiceInputManager.startListening()

            // Track transcription latency when complete
            if case .result(let transcription) = voiceInputManager.state {
                let latencyMs = Int(Date().timeIntervalSince(startTime) * 1000)
                let wordCount = transcription.split(separator: " ").count

                analytics.log(.voiceTranscription(VoiceTranscriptionEvent(
                    latencyMs: latencyMs,
                    wordCount: wordCount,
                    sessionId: sessionId
                )))

                analytics.trackLatency(
                    operation: "voice_transcription",
                    latencyMs: latencyMs,
                    sessionId: sessionId
                )
            }
        }
    }

    /// Stop voice input.
    private func stopVoiceInput() {
        voiceInputManager.stopListening()
    }

    /// Handle voice input completion.
    private func handleVoiceInputComplete(_ transcription: String) {
        state.isVoiceInputActive = false
        if !transcription.isEmpty {
            handleSendMessage(transcription)
        }
    }

    /// Handle voice input error.
    private func handleVoiceInputError(_ error: String) {
        state.isVoiceInputActive = false
        addErrorMessage("Voice input failed: \(error)", isRecoverable: true)
    }

    /// Dismiss error.
    private func dismissError() {
        state.error = nil
    }

    /// Retry after error.
    private func handleRetry() {
        // Get last user message and retry
        let lastUserMessage = state.messages.last(where: {
            if case .user = $0 { return true }
            return false
        })

        if case .user(let msg) = lastUserMessage {
            processUserInput(msg.text)
        }
    }

    /// Clear conversation history.
    private func clearConversation() {
        state = ConversationState()
        Task {
            await sessionContextManager.clearConversationHistory()
        }
        // Re-add welcome message
        addAssistantMessage(
            "Hi! I'm Bones, your digital caddy. How can I help?"
        )
    }

    // MARK: - Message Helpers

    /// Add user message to conversation.
    private func addUserMessage(_ text: String) {
        let message = ConversationMessage.user(UserMessage(text: text))
        state.messages.append(message)
    }

    /// Add assistant message to conversation.
    private func addAssistantMessage(_ text: String) {
        let message = ConversationMessage.assistant(AssistantMessage(text: text))
        state.messages.append(message)
    }

    /// Add clarification message with suggestions.
    private func addClarificationMessage(message: String, suggestions: [ClarificationSuggestion]) {
        let clarificationMessage = ConversationMessage.clarification(
            ClarificationMessage(
                message: message,
                suggestions: suggestions
            )
        )
        state.messages.append(clarificationMessage)
    }

    /// Add error message to conversation.
    private func addErrorMessage(_ message: String, isRecoverable: Bool) {
        let errorMessage = ConversationMessage.error(
            ErrorMessage(
                message: message,
                isRecoverable: isRecoverable
            )
        )
        state.messages.append(errorMessage)
    }

    // MARK: - Debug Helpers

    #if DEBUG
    /// Get the session ID for debugging
    var debugSessionId: String {
        sessionId
    }

    /// Get the analytics service for debugging
    var debugAnalytics: NavCaddyAnalytics {
        analytics
    }
    #endif
}
