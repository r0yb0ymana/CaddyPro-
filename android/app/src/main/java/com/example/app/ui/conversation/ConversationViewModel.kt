package caddypro.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import caddypro.analytics.AnalyticsEvent
import caddypro.analytics.NavCaddyAnalytics
import caddypro.domain.navcaddy.classifier.ClassificationResult
import caddypro.domain.navcaddy.classifier.IntentClassifier
import caddypro.domain.navcaddy.context.SessionContextManager
import caddypro.domain.navcaddy.error.ErrorContext
import caddypro.domain.navcaddy.error.NavCaddyError
import caddypro.domain.navcaddy.error.NavCaddyErrorHandler
import caddypro.domain.navcaddy.error.RecoveryAction
import caddypro.domain.navcaddy.fallback.LocalIntentSuggestions
import caddypro.domain.navcaddy.models.IntentType
import caddypro.domain.navcaddy.navigation.NavCaddyNavigator
import caddypro.domain.navcaddy.offline.NetworkMonitor
import caddypro.domain.navcaddy.offline.OfflineCapability
import caddypro.domain.navcaddy.offline.OfflineIntentHandler
import caddypro.domain.navcaddy.persona.BonesResponseFormatter
import caddypro.domain.navcaddy.routing.RoutingOrchestrator
import caddypro.domain.navcaddy.routing.RoutingResult
import caddypro.ui.conversation.voice.VoiceInputManager
import caddypro.ui.conversation.voice.VoiceInputState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the conversation screen.
 *
 * Integrates the full NavCaddy pipeline:
 * 1. Intent classification
 * 2. Routing orchestration
 * 3. Session context management
 * 4. Navigation execution
 * 5. Response formatting with Bones persona
 * 6. Voice input integration (Task 20)
 * 7. Analytics and observability (Task 22)
 * 8. Error handling and fallbacks (Task 23)
 * 9. Offline mode support (Task 24)
 *
 * Spec reference: navcaddy-engine.md R1, R2, R3, R4, R7, R8, G6, C6
 * Plan reference: navcaddy-engine-plan.md Task 18, Task 20, Task 22, Task 23, Task 24
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val intentClassifier: IntentClassifier,
    private val routingOrchestrator: RoutingOrchestrator,
    private val sessionContextManager: SessionContextManager,
    private val navigator: NavCaddyNavigator,
    private val responseFormatter: BonesResponseFormatter,
    private val voiceInputManager: VoiceInputManager,
    private val analytics: NavCaddyAnalytics,
    private val errorHandler: NavCaddyErrorHandler,
    private val localSuggestions: LocalIntentSuggestions,
    private val networkMonitor: NetworkMonitor,
    private val offlineIntentHandler: OfflineIntentHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationState())
    val uiState: StateFlow<ConversationState> = _uiState.asStateFlow()

    // Track error retry attempts
    private var retryAttempts = 0
    private var lastUserInput: String? = null

    // Track offline state
    private var isOffline = false

    init {
        // Start analytics session
        analytics.startSession()

        // Add welcome message
        addAssistantMessage(
            "Hi! I'm Bones, your digital caddy. Ask me about club selection, " +
                    "check your recovery, enter scores, or get coaching tips. How can I help?"
        )

        // Observe voice input state
        observeVoiceInputState()

        // Observe network connectivity
        observeNetworkState()
    }

    /**
     * Observe network connectivity state changes.
     *
     * Shows user notifications when entering/exiting offline mode.
     */
    private fun observeNetworkState() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                val wasOffline = isOffline
                isOffline = !isOnline

                // Update UI state
                _uiState.update { it.copy(isOffline = !isOnline) }

                // Show user notification when network state changes
                if (!isOnline && !wasOffline) {
                    // Just went offline
                    addAssistantMessage(OfflineCapability.getOfflineModeMessage())
                    showOfflineSuggestions()

                    // Log offline event
                    analytics.logError(
                        errorType = AnalyticsEvent.ErrorOccurred.ErrorType.NETWORK_ERROR,
                        message = "Device went offline",
                        isRecoverable = true
                    )
                } else if (isOnline && wasOffline) {
                    // Just came back online
                    addAssistantMessage(OfflineCapability.getOnlineModeMessage())
                    hideFallbackSuggestions()

                    // Log reconnection
                    analytics.logEvent(
                        AnalyticsEvent.Custom(
                            eventName = "network_reconnected",
                            parameters = emptyMap()
                        )
                    )
                }
            }
        }
    }

    /**
     * Observe voice input state changes and update UI accordingly.
     */
    private fun observeVoiceInputState() {
        viewModelScope.launch {
            voiceInputManager.state.collect { voiceState ->
                when (voiceState) {
                    is VoiceInputState.Idle -> {
                        _uiState.update { it.copy(isVoiceInputActive = false) }
                    }

                    is VoiceInputState.Listening -> {
                        _uiState.update { it.copy(isVoiceInputActive = true) }
                        // Start tracking voice input latency
                        analytics.startLatencyTracking("voice_transcription")
                    }

                    is VoiceInputState.Processing -> {
                        _uiState.update { it.copy(isVoiceInputActive = true) }
                    }

                    is VoiceInputState.Result -> {
                        // Voice input completed successfully
                        val latency = analytics.stopLatencyTracking("voice_transcription")
                        analytics.logVoiceTranscription(
                            latencyMs = latency,
                            transcription = voiceState.transcription,
                            wasSuccessful = true
                        )
                        handleVoiceInputComplete(voiceState.transcription)
                    }

                    is VoiceInputState.Error -> {
                        // Voice input error - use error handler
                        val latency = analytics.stopLatencyTracking("voice_transcription")
                        analytics.logVoiceTranscription(
                            latencyMs = latency,
                            transcription = "",
                            wasSuccessful = false
                        )
                        analytics.logError(
                            errorType = AnalyticsEvent.ErrorOccurred.ErrorType.VOICE_TRANSCRIPTION_ERROR,
                            message = voiceState.message,
                            isRecoverable = true
                        )
                        handleVoiceTranscriptionError(voiceState.message)
                    }
                }
            }
        }
    }

    /**
     * Handle user actions.
     */
    fun onAction(action: ConversationAction) {
        when (action) {
            is ConversationAction.SendMessage -> handleSendMessage(action.text)
            is ConversationAction.UpdateInput -> updateInput(action.text)
            is ConversationAction.StartVoiceInput -> startVoiceInput()
            is ConversationAction.StopVoiceInput -> stopVoiceInput()
            is ConversationAction.VoiceInputComplete -> handleVoiceInputComplete(action.transcription)
            is ConversationAction.VoiceInputError -> handleVoiceInputError(action.error)
            is ConversationAction.SelectSuggestion -> handleSuggestionSelected(action.intentType, action.label)
            is ConversationAction.DismissError -> dismissError()
            is ConversationAction.ConfirmIntent -> handleConfirmIntent(action.intentId)
            is ConversationAction.RejectIntent -> handleRejectIntent(action.intentId)
            is ConversationAction.Retry -> handleRetry()
            is ConversationAction.ClearConversation -> clearConversation()
            is ConversationAction.SelectRecoveryAction -> handleRecoveryAction(action.action)
            is ConversationAction.SelectFallbackSuggestion -> handleFallbackSuggestion(action.intentType, action.label)
            is ConversationAction.ShowFallbackSuggestions -> showFallbackSuggestions()
            is ConversationAction.HideFallbackSuggestions -> hideFallbackSuggestions()
        }
    }

    /**
     * Handle sending a text message.
     */
    private fun handleSendMessage(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return

        // Reset retry counter on new input
        retryAttempts = 0
        lastUserInput = trimmedText

        // Log input received
        analytics.logInputReceived(
            inputType = AnalyticsEvent.InputReceived.InputType.TEXT,
            input = trimmedText
        )

        // Add user message to conversation
        addUserMessage(trimmedText)

        // Clear input and hide fallback suggestions
        _uiState.update {
            it.copy(
                currentInput = "",
                showFallbackSuggestions = false
            )
        }

        // Process the message
        processUserInput(trimmedText)
    }

    /**
     * Process user input through the NavCaddy pipeline.
     *
     * Routes to offline handler if device is offline, otherwise uses full LLM pipeline.
     */
    private fun processUserInput(input: String) {
        viewModelScope.launch {
            try {
                // Show loading state
                _uiState.update { it.copy(isLoading = true) }

                // Check if offline - use offline handler
                if (isOffline) {
                    handleOfflineInput(input)
                    return@launch
                }

                // Start tracking classification latency
                analytics.startLatencyTracking("classification")

                // Get session context
                val context = sessionContextManager.getCurrentContext()

                // Classify intent
                val classificationResult = intentClassifier.classify(input, context)

                // Stop tracking classification latency
                val classificationLatency = analytics.stopLatencyTracking("classification")

                // Handle classification result
                when (classificationResult) {
                    is ClassificationResult.Route -> {
                        // Log successful classification
                        analytics.logIntentClassified(
                            intent = classificationResult.parsedIntent.intent.name,
                            confidence = classificationResult.parsedIntent.confidence,
                            latencyMs = classificationLatency,
                            wasSuccessful = true
                        )
                        handleRouteClassification(classificationResult)
                    }

                    is ClassificationResult.Confirm -> {
                        // Log mid-confidence classification
                        analytics.logIntentClassified(
                            intent = classificationResult.parsedIntent.intent.name,
                            confidence = classificationResult.parsedIntent.confidence,
                            latencyMs = classificationLatency,
                            wasSuccessful = true
                        )
                        handleConfirmClassification(classificationResult)
                    }

                    is ClassificationResult.Clarify -> {
                        // Log clarification request
                        analytics.logClarificationRequested(
                            originalInput = input,
                            confidence = 0f, // Low confidence
                            suggestionsCount = classificationResult.suggestions.size
                        )
                        handleClarifyClassification(classificationResult)
                    }

                    is ClassificationResult.Error -> {
                        // Log classification error
                        analytics.logIntentClassified(
                            intent = "ERROR",
                            confidence = 0f,
                            latencyMs = classificationLatency,
                            wasSuccessful = false
                        )
                        handleErrorClassification(classificationResult, input)
                    }
                }

                // Update session context with this turn
                val lastAssistantMessage = _uiState.value.messages.lastOrNull {
                    it is ConversationMessage.Assistant
                } as? ConversationMessage.Assistant

                if (lastAssistantMessage != null) {
                    sessionContextManager.addConversationTurn(
                        userInput = input,
                        assistantResponse = lastAssistantMessage.text
                    )
                }

            } catch (e: Exception) {
                // Log unexpected error
                analytics.logError(
                    errorType = AnalyticsEvent.ErrorOccurred.ErrorType.UNKNOWN,
                    message = e.message ?: "Unknown error",
                    isRecoverable = true,
                    throwable = e
                )
                handleUnexpectedError(e, input)
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                analytics.clearTracking()
            }
        }
    }

    /**
     * Handle user input when device is offline.
     *
     * Uses local keyword matching instead of LLM classification.
     * Per spec C6: "If offline: disable LLM calls and provide a limited local-intent menu"
     */
    private suspend fun handleOfflineInput(input: String) {
        try {
            analytics.startLatencyTracking("offline_classification")

            // Process with offline intent handler
            val result = offlineIntentHandler.processOffline(input)

            val latency = analytics.stopLatencyTracking("offline_classification")

            when (result) {
                is OfflineIntentHandler.OfflineResult.Match -> {
                    // Successfully matched an offline-available intent
                    analytics.logIntentClassified(
                        intent = result.parsedIntent.intent.name,
                        confidence = result.parsedIntent.confidence,
                        latencyMs = latency,
                        wasSuccessful = true
                    )

                    // Route through orchestrator
                    val routeResult = ClassificationResult.Route(result.parsedIntent)
                    handleRouteClassification(routeResult)
                }

                is OfflineIntentHandler.OfflineResult.Clarify -> {
                    // Needs clarification
                    analytics.logClarificationRequested(
                        originalInput = input,
                        confidence = result.clarification.confidence,
                        suggestionsCount = result.clarification.suggestions.size
                    )

                    val clarifyResult = ClassificationResult.Clarify(
                        message = result.clarification.message,
                        suggestions = result.clarification.suggestions
                    )
                    handleClarifyClassification(clarifyResult)
                }

                is OfflineIntentHandler.OfflineResult.RequiresOnline -> {
                    // User tried to use online-only feature
                    addAssistantMessage(result.message)
                    showOfflineSuggestions()
                }

                is OfflineIntentHandler.OfflineResult.NoMatch -> {
                    // No match found
                    addAssistantMessage(result.message)
                    showOfflineSuggestions()
                }
            }

        } catch (e: Exception) {
            // Even offline processing failed
            analytics.logError(
                errorType = AnalyticsEvent.ErrorOccurred.ErrorType.UNKNOWN,
                message = "Offline processing failed: ${e.message}",
                isRecoverable = true,
                throwable = e
            )
            addAssistantMessage(
                "Sorry, I had trouble understanding that. Here are some things you can do offline:"
            )
            showOfflineSuggestions()
        } finally {
            _uiState.update { it.copy(isLoading = false) }
            analytics.clearTracking()
        }
    }

    /**
     * Handle high-confidence route classification.
     */
    private suspend fun handleRouteClassification(result: ClassificationResult.Route) {
        // Start tracking routing latency
        analytics.startLatencyTracking("routing")

        // Route through orchestrator
        val routingResult = routingOrchestrator.route(result)

        // Stop tracking routing latency
        val routingLatency = analytics.stopLatencyTracking("routing")

        when (routingResult) {
            is RoutingResult.Navigate -> {
                // Log route execution
                analytics.logRouteExecuted(
                    module = routingResult.target.module.name,
                    screen = routingResult.target.screen,
                    latencyMs = routingLatency,
                    parameters = routingResult.target.parameters
                )

                // Navigate to target
                navigator.navigate(
                    caddypro.domain.navcaddy.navigation.NavCaddyDestination.fromRoutingTarget(
                        routingResult.target
                    )
                )
                // Add confirmation message
                addAssistantMessage("Let's go to ${routingResult.target.module.name.lowercase()}.")
            }

            is RoutingResult.NoNavigation -> {
                // Display response without navigation
                addAssistantMessage(routingResult.response)
            }

            is RoutingResult.PrerequisiteMissing -> {
                // Display prerequisite message
                addAssistantMessage(routingResult.message)
            }

            is RoutingResult.ConfirmationRequired -> {
                // Display confirmation message
                addAssistantMessage(routingResult.message)
            }
        }
    }

    /**
     * Handle mid-confidence confirmation classification.
     */
    private fun handleConfirmClassification(result: ClassificationResult.Confirm) {
        addAssistantMessage(result.message)
    }

    /**
     * Handle low-confidence clarification classification.
     */
    private fun handleClarifyClassification(result: ClassificationResult.Clarify) {
        // Create clarification suggestions
        val suggestions = result.suggestions.map { intentType ->
            ClarificationSuggestion(
                intentType = intentType,
                label = intentType.displayName,
                description = intentType.description
            )
        }

        // Add clarification message with chips
        addClarificationMessage(
            message = result.message,
            suggestions = suggestions
        )
    }

    /**
     * Handle classification error with error handler.
     */
    private fun handleErrorClassification(result: ClassificationResult.Error, userInput: String) {
        retryAttempts++

        // Create NavCaddyError from classification error
        val navCaddyError = NavCaddyError.fromThrowable(
            throwable = result.cause ?: IllegalStateException("Classification failed"),
            userInput = userInput
        )

        // Get error context
        val errorContext = ErrorContext(
            userInput = userInput,
            attemptCount = retryAttempts,
            isOffline = isOffline
        )

        // Get recovery strategy
        val recoveryStrategy = errorHandler.handleError(navCaddyError, errorContext)

        // Add error message with recovery options
        addErrorMessage(
            message = recoveryStrategy.userMessage,
            isRecoverable = true,
            recoveryActions = recoveryStrategy.actions
        )

        // Show fallback suggestions if available
        if (recoveryStrategy.suggestedIntents.isNotEmpty()) {
            showFallbackSuggestionsForIntents(recoveryStrategy.suggestedIntents)
        }
    }

    /**
     * Handle voice transcription error.
     */
    private fun handleVoiceTranscriptionError(errorMessage: String) {
        val error = NavCaddyError.TranscriptionError(
            message = errorMessage
        )

        val context = ErrorContext(
            attemptCount = retryAttempts,
            isOffline = isOffline
        )

        val strategy = errorHandler.handleError(error, context)

        addErrorMessage(
            message = strategy.userMessage,
            isRecoverable = true,
            recoveryActions = strategy.actions
        )
    }

    /**
     * Handle unexpected errors.
     */
    private fun handleUnexpectedError(throwable: Throwable, userInput: String?) {
        val error = NavCaddyError.fromThrowable(throwable, userInput)

        val context = ErrorContext(
            userInput = userInput,
            attemptCount = retryAttempts,
            isOffline = isOffline
        )

        val strategy = errorHandler.handleError(error, context)

        addErrorMessage(
            message = strategy.userMessage,
            isRecoverable = errorHandler.isRecoverable(error),
            recoveryActions = strategy.actions
        )

        // Show fallback suggestions for unknown errors
        if (strategy.suggestedIntents.isNotEmpty()) {
            showFallbackSuggestionsForIntents(strategy.suggestedIntents)
        }
    }

    /**
     * Handle suggestion chip selection.
     */
    private fun handleSuggestionSelected(intentType: IntentType, label: String) {
        // Log suggestion selection
        val suggestions = (_uiState.value.messages.lastOrNull {
            it is ConversationMessage.Clarification
        } as? ConversationMessage.Clarification)?.suggestions ?: emptyList()

        val index = suggestions.indexOfFirst { it.intentType == intentType }
        if (index >= 0) {
            analytics.logSuggestionSelected(
                intentType = intentType.name,
                suggestionIndex = index
            )
        }

        // Add user message showing selection
        addUserMessage(label)

        // Process the selected intent as if user typed it
        processUserInput(label)
    }

    /**
     * Handle recovery action selection.
     */
    private fun handleRecoveryAction(action: RecoveryAction) {
        when (action) {
            RecoveryAction.Retry, RecoveryAction.RetryWithNetwork, RecoveryAction.RetryLater -> {
                handleRetry()
            }
            RecoveryAction.RetryVoice -> {
                startVoiceInput()
            }
            RecoveryAction.UseTextInput -> {
                // Dismiss error and show input bar
                dismissError()
            }
            RecoveryAction.UseOfflineMode -> {
                showOfflineSuggestions()
            }
            RecoveryAction.ShowLocalSuggestions, RecoveryAction.ShowCommonIntents -> {
                showFallbackSuggestions()
            }
            RecoveryAction.ShowClarification -> {
                // Already shown in error message
                dismissError()
            }
            RecoveryAction.ShowHelp -> {
                processUserInput("help")
            }
            RecoveryAction.RestartConversation -> {
                clearConversation()
            }
            RecoveryAction.Cancel -> {
                dismissError()
            }
            else -> {
                // Other actions not yet implemented
                dismissError()
            }
        }
    }

    /**
     * Handle fallback suggestion selection.
     */
    private fun handleFallbackSuggestion(intentType: IntentType, label: String) {
        analytics.logSuggestionSelected(
            intentType = intentType.name,
            suggestionIndex = -1 // Fallback suggestion
        )

        // Add user message and process
        addUserMessage(label)
        processUserInput(label)

        // Hide fallback suggestions
        hideFallbackSuggestions()
    }

    /**
     * Show fallback suggestions.
     */
    private fun showFallbackSuggestions() {
        val lastInput = lastUserInput ?: ""
        val suggestions = localSuggestions.getSuggestions(
            input = lastInput,
            isOffline = isOffline,
            maxSuggestions = 3  // Per spec A3: 3 suggested intents
        ).map { suggestion ->
            FallbackSuggestion(
                intentType = suggestion.intentType,
                label = suggestion.label,
                description = suggestion.description
            )
        }

        _uiState.update {
            it.copy(
                fallbackSuggestions = suggestions,
                showFallbackSuggestions = true
            )
        }
    }

    /**
     * Show fallback suggestions for specific intents.
     */
    private fun showFallbackSuggestionsForIntents(intents: List<IntentType>) {
        val suggestions = intents.take(5).map { intentType ->
            FallbackSuggestion(
                intentType = intentType,
                label = intentType.displayName,
                description = intentType.description
            )
        }

        _uiState.update {
            it.copy(
                fallbackSuggestions = suggestions,
                showFallbackSuggestions = true
            )
        }
    }

    /**
     * Show offline-available suggestions only.
     */
    private fun showOfflineSuggestions() {
        val suggestions = localSuggestions.getOfflineIntents().map { suggestion ->
            FallbackSuggestion(
                intentType = suggestion.intentType,
                label = suggestion.label,
                description = suggestion.description
            )
        }

        _uiState.update {
            it.copy(
                fallbackSuggestions = suggestions,
                showFallbackSuggestions = true
            )
        }
    }

    /**
     * Hide fallback suggestions.
     */
    private fun hideFallbackSuggestions() {
        _uiState.update {
            it.copy(showFallbackSuggestions = false)
        }
    }

    /**
     * Handle intent confirmation.
     */
    private fun handleConfirmIntent(intentId: String) {
        addUserMessage("Yes")
        // TODO: Process confirmed intent
        addAssistantMessage("Great! Let me help you with that.")
    }

    /**
     * Handle intent rejection.
     */
    private fun handleRejectIntent(intentId: String) {
        addUserMessage("No")
        addAssistantMessage("No problem. What did you need help with?")
    }

    /**
     * Update input text.
     */
    private fun updateInput(text: String) {
        _uiState.update { it.copy(currentInput = text) }
    }

    /**
     * Start voice input using VoiceInputManager.
     */
    private fun startVoiceInput() {
        voiceInputManager.startListening()
    }

    /**
     * Stop voice input.
     */
    private fun stopVoiceInput() {
        voiceInputManager.stopListening()
    }

    /**
     * Handle voice input completion.
     */
    private fun handleVoiceInputComplete(transcription: String) {
        _uiState.update { it.copy(isVoiceInputActive = false) }
        voiceInputManager.resetState()

        if (transcription.isNotBlank()) {
            // Log voice input
            analytics.logInputReceived(
                inputType = AnalyticsEvent.InputReceived.InputType.VOICE,
                input = transcription
            )
            handleSendMessage(transcription)
        }
    }

    /**
     * Handle voice input error.
     */
    private fun handleVoiceInputError(error: String) {
        _uiState.update { it.copy(isVoiceInputActive = false) }
        voiceInputManager.resetState()
        handleVoiceTranscriptionError(error)
    }

    /**
     * Dismiss error.
     */
    private fun dismissError() {
        _uiState.update {
            it.copy(
                error = null,
                recoveryActions = emptyList()
            )
        }
    }

    /**
     * Retry after error.
     */
    private fun handleRetry() {
        // Get last user message and retry
        val lastInput = lastUserInput
        if (lastInput != null) {
            retryAttempts++
            processUserInput(lastInput)
        } else {
            addAssistantMessage("What would you like help with?")
        }
    }

    /**
     * Clear conversation history.
     */
    private fun clearConversation() {
        _uiState.update { ConversationState(isOffline = isOffline) }
        sessionContextManager.clearConversationHistory()
        retryAttempts = 0
        lastUserInput = null

        // Start new analytics session
        analytics.startSession()

        // Re-add welcome message
        addAssistantMessage(
            "Hi! I'm Bones, your digital caddy. How can I help?"
        )
    }

    /**
     * Add user message to conversation.
     */
    private fun addUserMessage(text: String) {
        val message = ConversationMessage.User(text = text)
        _uiState.update { state ->
            state.copy(messages = state.messages + message)
        }
    }

    /**
     * Add assistant message to conversation.
     */
    private fun addAssistantMessage(text: String) {
        val message = ConversationMessage.Assistant(text = text)
        _uiState.update { state ->
            state.copy(messages = state.messages + message)
        }
    }

    /**
     * Add clarification message with suggestions.
     */
    private fun addClarificationMessage(message: String, suggestions: List<ClarificationSuggestion>) {
        val clarificationMessage = ConversationMessage.Clarification(
            message = message,
            suggestions = suggestions
        )
        _uiState.update { state ->
            state.copy(messages = state.messages + clarificationMessage)
        }
    }

    /**
     * Add error message to conversation with recovery options.
     */
    private fun addErrorMessage(
        message: String,
        isRecoverable: Boolean,
        recoveryActions: List<RecoveryAction> = emptyList()
    ) {
        val errorMessage = ConversationMessage.Error(
            message = message,
            isRecoverable = isRecoverable,
            recoveryActions = recoveryActions
        )
        _uiState.update { state ->
            state.copy(
                messages = state.messages + errorMessage,
                recoveryActions = recoveryActions
            )
        }
    }

    /**
     * Get analytics instance for debug view.
     */
    fun getAnalytics(): NavCaddyAnalytics = analytics

    override fun onCleared() {
        super.onCleared()
        // Clean up voice input when ViewModel is cleared
        voiceInputManager.cancelListening()
        // Clear analytics tracking
        analytics.clearTracking()
    }
}

/**
 * Extension to map IntentType to display info.
 */
private val IntentType.displayName: String
    get() = when (this) {
        IntentType.CLUB_ADJUSTMENT -> "Adjust Club"
        IntentType.RECOVERY_CHECK -> "Check Recovery"
        IntentType.SHOT_RECOMMENDATION -> "Shot Advice"
        IntentType.SCORE_ENTRY -> "Enter Score"
        IntentType.PATTERN_QUERY -> "View Patterns"
        IntentType.DRILL_REQUEST -> "Practice Drill"
        IntentType.WEATHER_CHECK -> "Check Weather"
        IntentType.STATS_LOOKUP -> "View Stats"
        IntentType.ROUND_START -> "Start Round"
        IntentType.ROUND_END -> "End Round"
        IntentType.EQUIPMENT_INFO -> "Equipment Info"
        IntentType.COURSE_INFO -> "Course Info"
        IntentType.SETTINGS_CHANGE -> "Settings"
        IntentType.HELP_REQUEST -> "Help"
        IntentType.FEEDBACK -> "Feedback"
    }

private val IntentType.description: String
    get() = when (this) {
        IntentType.CLUB_ADJUSTMENT -> "Adjust club distances or selection"
        IntentType.RECOVERY_CHECK -> "Check your recovery status"
        IntentType.SHOT_RECOMMENDATION -> "Get shot recommendations"
        IntentType.SCORE_ENTRY -> "Enter or update your score"
        IntentType.PATTERN_QUERY -> "View your miss patterns"
        IntentType.DRILL_REQUEST -> "Get practice drill suggestions"
        IntentType.WEATHER_CHECK -> "Check weather conditions"
        IntentType.STATS_LOOKUP -> "View your golf statistics"
        IntentType.ROUND_START -> "Start a new round"
        IntentType.ROUND_END -> "Complete current round"
        IntentType.EQUIPMENT_INFO -> "View equipment information"
        IntentType.COURSE_INFO -> "Get course information"
        IntentType.SETTINGS_CHANGE -> "Change app settings"
        IntentType.HELP_REQUEST -> "Get help using the app"
        IntentType.FEEDBACK -> "Provide feedback"
    }
