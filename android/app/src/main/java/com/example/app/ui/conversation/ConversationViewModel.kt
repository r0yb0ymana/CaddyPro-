package caddypro.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import caddypro.analytics.NavCaddyAnalytics
import caddypro.analytics.AnalyticsEvent
import caddypro.domain.navcaddy.classifier.ClassificationResult
import caddypro.domain.navcaddy.classifier.IntentClassifier
import caddypro.domain.navcaddy.context.SessionContextManager
import com.example.app.domain.navcaddy.error.ErrorContext
import com.example.app.domain.navcaddy.error.NavCaddyError
import com.example.app.domain.navcaddy.error.NavCaddyErrorHandler
import com.example.app.domain.navcaddy.error.RecoveryAction
// import caddypro.domain.navcaddy.fallback.LocalIntentSuggestions
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
// import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// import javax.inject.Inject

/**
 * ViewModel for the conversation screen.
 */
// @HiltViewModel
class ConversationViewModel /* @Inject */ constructor(
    private val intentClassifier: IntentClassifier,
    private val routingOrchestrator: RoutingOrchestrator,
    private val sessionContextManager: SessionContextManager,
    private val navigator: NavCaddyNavigator,
    private val responseFormatter: BonesResponseFormatter,
    private val voiceInputManager: VoiceInputManager,
    private val analytics: NavCaddyAnalytics,
    // private val errorHandler: NavCaddyErrorHandler,
    // private val localSuggestions: LocalIntentSuggestions,
    private val networkMonitor: NetworkMonitor,
    private val offlineIntentHandler: OfflineIntentHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationState())
    val uiState: StateFlow<ConversationState> = _uiState.asStateFlow()

    private var retryAttempts = 0
    private var lastUserInput: String? = null
    private var isOffline = false

    init {
        analytics.startSession()

        addAssistantMessage(
            "Hi! I'm Bones, your digital caddy. Ask me about club selection, " +
                "check your recovery, enter scores, or get coaching tips. How can I help?"
        )

        observeVoiceInputState()
        observeNetworkState()
    }

    private fun observeNetworkState() {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                val wasOffline = isOffline
                isOffline = !isOnline

                _uiState.update { it.copy(isOffline = !isOnline) }

                if (!isOnline && !wasOffline) {
                    addAssistantMessage(OfflineCapability.getOfflineModeMessage())
                    showOfflineSuggestions()

                    analytics.logError(
                        errorType = AnalyticsEvent.ErrorOccurred.ErrorType.NETWORK_ERROR,
                        message = "Device went offline",
                        isRecoverable = true
                    )
                } else if (isOnline && wasOffline) {
                    addAssistantMessage(OfflineCapability.getOnlineModeMessage())
                    hideFallbackSuggestions()
                    // Network reconnected - no custom event needed
                }
            }
        }
    }

    private fun observeVoiceInputState() {
        viewModelScope.launch {
            voiceInputManager.state.collect { voiceState ->
                when (voiceState) {
                    is VoiceInputState.Idle -> {
                        _uiState.update { it.copy(isVoiceInputActive = false) }
                    }

                    is VoiceInputState.Listening -> {
                        _uiState.update { it.copy(isVoiceInputActive = true) }
                        analytics.startLatencyTracking("voice_transcription")
                    }

                    is VoiceInputState.Processing -> {
                        _uiState.update { it.copy(isVoiceInputActive = true) }
                    }

                    is VoiceInputState.Result -> {
                        val latency = analytics.stopLatencyTracking("voice_transcription")
                        analytics.logVoiceTranscription(
                            latencyMs = latency,
                            transcription = voiceState.transcription,
                            wasSuccessful = true
                        )
                        handleVoiceInputComplete(voiceState.transcription)
                    }

                    is VoiceInputState.Error -> {
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

    private fun handleSendMessage(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return

        retryAttempts = 0
        lastUserInput = trimmedText

        analytics.logInputReceived(
            inputType = AnalyticsEvent.InputReceived.InputType.TEXT,
            input = trimmedText
        )

        addUserMessage(trimmedText)

        _uiState.update {
            it.copy(
                currentInput = "",
                showFallbackSuggestions = false
            )
        }

        processUserInput(trimmedText)
    }

    private fun processUserInput(input: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                if (isOffline) {
                    handleOfflineInput(input)
                    return@launch
                }

                analytics.startLatencyTracking("classification")

                val context = sessionContextManager.getCurrentContext()
                val classificationResult = intentClassifier.classify(input, context)

                val classificationLatency = analytics.stopLatencyTracking("classification")

                when (classificationResult) {
                    is ClassificationResult.Route -> {
                        analytics.logIntentClassified(
                            intent = classificationResult.intent.intentType.name,
                            confidence = classificationResult.intent.confidence,
                            latencyMs = classificationLatency,
                            wasSuccessful = true
                        )
                        handleRouteClassification(classificationResult)
                    }

                    is ClassificationResult.Confirm -> {
                        analytics.logIntentClassified(
                            intent = classificationResult.intent.intentType.name,
                            confidence = classificationResult.intent.confidence,
                            latencyMs = classificationLatency,
                            wasSuccessful = true
                        )
                        handleConfirmClassification(classificationResult)
                    }

                    is ClassificationResult.Clarify -> {
                        analytics.logClarificationRequested(
                            originalInput = input,
                            confidence = 0f,
                            suggestionsCount = classificationResult.suggestions.size
                        )
                        handleClarifyClassification(classificationResult)
                    }

                    is ClassificationResult.Error -> {
                        analytics.logIntentClassified(
                            intent = "ERROR",
                            confidence = 0f,
                            latencyMs = classificationLatency,
                            wasSuccessful = false
                        )
                        handleErrorClassification(classificationResult, input)
                    }
                }

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

    private suspend fun handleOfflineInput(input: String) {
        try {
            analytics.startLatencyTracking("offline_classification")

            val result = offlineIntentHandler.processOffline(input)

            val latency = analytics.stopLatencyTracking("offline_classification")

            when (result) {
                is OfflineIntentHandler.OfflineResult.Match -> {
                    analytics.logIntentClassified(
                        intent = result.parsedIntent.intentType.name,
                        confidence = result.parsedIntent.confidence,
                        latencyMs = latency,
                        wasSuccessful = true
                    )

                    // For offline matches, handle directly without routing
                    handleOfflineMatchResult(result.parsedIntent)
                }

                is OfflineIntentHandler.OfflineResult.Clarify -> {
                    analytics.logClarificationRequested(
                        originalInput = input,
                        confidence = 0f, // Offline doesn't track confidence at clarification level
                        suggestionsCount = result.clarification.suggestions.size
                    )

                    // Convert IntentSuggestion list to suggestions format for UI
                    val suggestions = result.clarification.suggestions.map { intentSuggestion ->
                        ClarificationSuggestion(
                            intentType = intentSuggestion.intentType,
                            label = intentSuggestion.label,
                            description = intentSuggestion.description
                        )
                    }
                    addClarificationMessage(
                        message = result.clarification.message,
                        suggestions = suggestions
                    )
                }

                is OfflineIntentHandler.OfflineResult.RequiresOnline -> {
                    addAssistantMessage(result.message)
                    showOfflineSuggestions()
                }

                is OfflineIntentHandler.OfflineResult.NoMatch -> {
                    addAssistantMessage(result.message)
                    showOfflineSuggestions()
                }
            }

        } catch (e: Exception) {
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

    private suspend fun handleRouteClassification(result: ClassificationResult.Route) {
        analytics.startLatencyTracking("routing")

        val routingResult = routingOrchestrator.route(result)

        val routingLatency = analytics.stopLatencyTracking("routing")

        when (routingResult) {
            is RoutingResult.Navigate -> {
                analytics.logRouteExecuted(
                    module = routingResult.target.module.name,
                    screen = routingResult.target.screen,
                    latencyMs = routingLatency,
                    parameters = routingResult.target.parameters.mapValues { it.value.toString() }
                )

                navigator.navigate(
                    caddypro.domain.navcaddy.navigation.NavCaddyDestination.fromRoutingTarget(
                        routingResult.target
                    )
                )
                addAssistantMessage("Let's go to ${routingResult.target.module.name.lowercase()}.")
            }

            is RoutingResult.NoNavigation -> {
                addAssistantMessage(routingResult.response)
            }

            is RoutingResult.PrerequisiteMissing -> {
                addAssistantMessage(routingResult.message)
            }

            is RoutingResult.ConfirmationRequired -> {
                addAssistantMessage(routingResult.message)
            }
        }
    }

    private fun handleConfirmClassification(result: ClassificationResult.Confirm) {
        addAssistantMessage(result.message)
    }

    /**
     * Handle offline match result by providing a response based on intent type.
     */
    private fun handleOfflineMatchResult(parsedIntent: caddypro.domain.navcaddy.models.ParsedIntent) {
        val intentType = parsedIntent.intentType
        val response = when (intentType) {
            IntentType.SCORE_ENTRY -> "Ready to enter your score. Use the scorecard below."
            IntentType.STATS_LOOKUP -> "Here are your offline stats. Some data may not be current."
            IntentType.EQUIPMENT_INFO -> "Here's your equipment info from local storage."
            IntentType.ROUND_START -> "Starting a new round. I'll sync when you're back online."
            IntentType.ROUND_END -> "Ending your round. Results will sync when online."
            IntentType.SETTINGS_CHANGE -> "Opening settings."
            IntentType.HELP_REQUEST -> "Here's what I can help with while offline."
            else -> "I understood you. Let me help with that."
        }
        addAssistantMessage(response)
    }

    private fun handleClarifyClassification(result: ClassificationResult.Clarify) {
        val suggestions = result.suggestions.map { intentType ->
            ClarificationSuggestion(
                intentType = intentType,
                label = intentType.displayName,
                description = intentType.description
            )
        }

        addClarificationMessage(
            message = result.message,
            suggestions = suggestions
        )
    }

    private fun handleErrorClassification(result: ClassificationResult.Error, userInput: String) {
        retryAttempts++

        // Stub - errorHandler commented out
        addErrorMessage(
            message = "Sorry, I had trouble with that. Please try again.",
            isRecoverable = true,
            recoveryActions = listOf(RecoveryAction.Retry, RecoveryAction.ShowHelp)
        )
    }

    private fun handleVoiceTranscriptionError(errorMessage: String) {
        addErrorMessage(
            message = "Sorry, I couldn't understand that. Try speaking again.",
            isRecoverable = true,
            recoveryActions = listOf(RecoveryAction.RetryVoice, RecoveryAction.UseTextInput)
        )
    }

    private fun handleUnexpectedError(throwable: Throwable, userInput: String?) {
        addErrorMessage(
            message = "Something went wrong. Please try again.",
            isRecoverable = true,
            recoveryActions = listOf(RecoveryAction.Retry, RecoveryAction.RestartConversation)
        )
    }

    private fun handleSuggestionSelected(intentType: IntentType, label: String) {
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

        addUserMessage(label)
        processUserInput(label)
    }

    private fun handleRecoveryAction(action: RecoveryAction) {
        when (action) {
            RecoveryAction.Retry, RecoveryAction.RetryWithNetwork, RecoveryAction.RetryLater -> {
                handleRetry()
            }
            RecoveryAction.RetryVoice -> {
                startVoiceInput()
            }
            RecoveryAction.UseTextInput -> {
                dismissError()
            }
            RecoveryAction.UseOfflineMode -> {
                showOfflineSuggestions()
            }
            RecoveryAction.ShowLocalSuggestions, RecoveryAction.ShowCommonIntents -> {
                showFallbackSuggestions()
            }
            RecoveryAction.ShowClarification -> {
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
                dismissError()
            }
        }
    }

    private fun handleFallbackSuggestion(intentType: IntentType, label: String) {
        analytics.logSuggestionSelected(
            intentType = intentType.name,
            suggestionIndex = -1
        )

        addUserMessage(label)
        processUserInput(label)

        hideFallbackSuggestions()
    }

    private fun showFallbackSuggestions() {
        // Stub - localSuggestions commented out
        val suggestions = listOf(
            FallbackSuggestion(
                intentType = IntentType.HELP_REQUEST,
                label = "Help",
                description = "Get help using the app"
            ),
            FallbackSuggestion(
                intentType = IntentType.STATS_LOOKUP,
                label = "View Stats",
                description = "View your golf statistics"
            )
        )

        _uiState.update {
            it.copy(
                fallbackSuggestions = suggestions,
                showFallbackSuggestions = true
            )
        }
    }

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

    private fun showOfflineSuggestions() {
        // Stub - localSuggestions commented out
        val suggestions = listOf(
            FallbackSuggestion(
                intentType = IntentType.SCORE_ENTRY,
                label = "Enter Score",
                description = "Enter or update your score"
            ),
            FallbackSuggestion(
                intentType = IntentType.STATS_LOOKUP,
                label = "View Stats",
                description = "View your golf statistics"
            )
        )

        _uiState.update {
            it.copy(
                fallbackSuggestions = suggestions,
                showFallbackSuggestions = true
            )
        }
    }

    private fun hideFallbackSuggestions() {
        _uiState.update {
            it.copy(showFallbackSuggestions = false)
        }
    }

    private fun handleConfirmIntent(intentId: String) {
        addUserMessage("Yes")
        addAssistantMessage("Great! Let me help you with that.")
    }

    private fun handleRejectIntent(intentId: String) {
        addUserMessage("No")
        addAssistantMessage("No problem. What did you need help with?")
    }

    private fun updateInput(text: String) {
        _uiState.update { it.copy(currentInput = text) }
    }

    private fun startVoiceInput() {
        voiceInputManager.startListening()
    }

    private fun stopVoiceInput() {
        voiceInputManager.stopListening()
    }

    private fun handleVoiceInputComplete(transcription: String) {
        _uiState.update { it.copy(isVoiceInputActive = false) }
        voiceInputManager.resetState()

        if (transcription.isNotBlank()) {
            analytics.logInputReceived(
                inputType = AnalyticsEvent.InputReceived.InputType.VOICE,
                input = transcription
            )
            handleSendMessage(transcription)
        }
    }

    private fun handleVoiceInputError(error: String) {
        _uiState.update { it.copy(isVoiceInputActive = false) }
        voiceInputManager.resetState()
        handleVoiceTranscriptionError(error)
    }

    private fun dismissError() {
        _uiState.update {
            it.copy(
                error = null,
                recoveryActions = emptyList()
            )
        }
    }

    private fun handleRetry() {
        val lastInput = lastUserInput
        if (lastInput != null) {
            retryAttempts++
            processUserInput(lastInput)
        } else {
            addAssistantMessage("What would you like help with?")
        }
    }

    private fun clearConversation() {
        _uiState.update { ConversationState(isOffline = isOffline) }
        sessionContextManager.clearConversationHistory()
        retryAttempts = 0
        lastUserInput = null

        analytics.startSession()

        addAssistantMessage(
            "Hi! I'm Bones, your digital caddy. How can I help?"
        )
    }

    private fun addUserMessage(text: String) {
        val message = ConversationMessage.User(text = text)
        _uiState.update { state ->
            state.copy(messages = state.messages + message)
        }
    }

    private fun addAssistantMessage(text: String) {
        val message = ConversationMessage.Assistant(text = text)
        _uiState.update { state ->
            state.copy(messages = state.messages + message)
        }
    }

    private fun addClarificationMessage(message: String, suggestions: List<ClarificationSuggestion>) {
        val clarificationMessage = ConversationMessage.Clarification(
            message = message,
            suggestions = suggestions
        )
        _uiState.update { state ->
            state.copy(messages = state.messages + clarificationMessage)
        }
    }

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

    fun getAnalytics(): NavCaddyAnalytics = analytics

    override fun onCleared() {
        super.onCleared()
        voiceInputManager.cancelListening()
        analytics.clearTracking()
    }
}

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
        IntentType.BAILOUT_QUERY -> "Bailout Zone"
        IntentType.READINESS_CHECK -> "Check Readiness"
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
        IntentType.BAILOUT_QUERY -> "Find safe bailout zones"
        IntentType.READINESS_CHECK -> "Check your readiness score"
    }
