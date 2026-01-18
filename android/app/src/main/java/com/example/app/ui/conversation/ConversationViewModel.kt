package caddypro.ui.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import caddypro.domain.navcaddy.classifier.ClassificationResult
import caddypro.domain.navcaddy.classifier.IntentClassifier
import caddypro.domain.navcaddy.context.SessionContextManager
import caddypro.domain.navcaddy.models.IntentType
import caddypro.domain.navcaddy.models.Role
import caddypro.domain.navcaddy.navigation.NavCaddyNavigator
import caddypro.domain.navcaddy.persona.BonesResponseFormatter
import caddypro.domain.navcaddy.routing.RoutingOrchestrator
import caddypro.domain.navcaddy.routing.RoutingResult
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
 *
 * Spec reference: navcaddy-engine.md R1, R2, R3, R4, R7
 * Plan reference: navcaddy-engine-plan.md Task 18
 */
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val intentClassifier: IntentClassifier,
    private val routingOrchestrator: RoutingOrchestrator,
    private val sessionContextManager: SessionContextManager,
    private val navigator: NavCaddyNavigator,
    private val responseFormatter: BonesResponseFormatter
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationState())
    val uiState: StateFlow<ConversationState> = _uiState.asStateFlow()

    init {
        // Add welcome message
        addAssistantMessage(
            "Hi! I'm Bones, your digital caddy. Ask me about club selection, " +
                    "check your recovery, enter scores, or get coaching tips. How can I help?"
        )
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
        }
    }

    /**
     * Handle sending a text message.
     */
    private fun handleSendMessage(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return

        // Add user message to conversation
        addUserMessage(trimmedText)

        // Clear input
        _uiState.update { it.copy(currentInput = "") }

        // Process the message
        processUserInput(trimmedText)
    }

    /**
     * Process user input through the NavCaddy pipeline.
     */
    private fun processUserInput(input: String) {
        viewModelScope.launch {
            try {
                // Show loading state
                _uiState.update { it.copy(isLoading = true) }

                // Get session context
                val context = sessionContextManager.getCurrentContext()

                // Classify intent
                val classificationResult = intentClassifier.classify(input, context)

                // Handle classification result
                when (classificationResult) {
                    is ClassificationResult.Route -> handleRouteClassification(classificationResult)
                    is ClassificationResult.Confirm -> handleConfirmClassification(classificationResult)
                    is ClassificationResult.Clarify -> handleClarifyClassification(classificationResult)
                    is ClassificationResult.Error -> handleErrorClassification(classificationResult)
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
                addErrorMessage(
                    message = "Something went wrong. Please try again.",
                    isRecoverable = true
                )
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Handle high-confidence route classification.
     */
    private suspend fun handleRouteClassification(result: ClassificationResult.Route) {
        // Route through orchestrator
        val routingResult = routingOrchestrator.route(result)

        when (routingResult) {
            is RoutingResult.Navigate -> {
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
     * Handle classification error.
     */
    private fun handleErrorClassification(result: ClassificationResult.Error) {
        val errorType = when {
            result.cause is java.net.UnknownHostException -> BonesResponseFormatter.ErrorType.NETWORK_ERROR
            result.cause is java.util.concurrent.TimeoutException -> BonesResponseFormatter.ErrorType.TIMEOUT
            else -> BonesResponseFormatter.ErrorType.SERVICE_UNAVAILABLE
        }

        val formattedError = responseFormatter.formatError(errorType)
        addErrorMessage(formattedError, isRecoverable = true)
    }

    /**
     * Handle suggestion chip selection.
     */
    private fun handleSuggestionSelected(intentType: IntentType, label: String) {
        // Add user message showing selection
        addUserMessage(label)

        // Process the selected intent as if user typed it
        processUserInput(label)
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
     * Start voice input (placeholder for Task 20).
     */
    private fun startVoiceInput() {
        _uiState.update { it.copy(isVoiceInputActive = true) }
        // TODO: Implement voice input in Task 20
    }

    /**
     * Stop voice input.
     */
    private fun stopVoiceInput() {
        _uiState.update { it.copy(isVoiceInputActive = false) }
    }

    /**
     * Handle voice input completion.
     */
    private fun handleVoiceInputComplete(transcription: String) {
        _uiState.update { it.copy(isVoiceInputActive = false) }
        if (transcription.isNotBlank()) {
            handleSendMessage(transcription)
        }
    }

    /**
     * Handle voice input error.
     */
    private fun handleVoiceInputError(error: String) {
        _uiState.update { it.copy(isVoiceInputActive = false) }
        addErrorMessage("Voice input failed: $error", isRecoverable = true)
    }

    /**
     * Dismiss error.
     */
    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Retry after error.
     */
    private fun handleRetry() {
        // Get last user message and retry
        val lastUserMessage = _uiState.value.messages.lastOrNull {
            it is ConversationMessage.User
        } as? ConversationMessage.User

        if (lastUserMessage != null) {
            processUserInput(lastUserMessage.text)
        }
    }

    /**
     * Clear conversation history.
     */
    private fun clearConversation() {
        _uiState.update { ConversationState() }
        sessionContextManager.clearConversationHistory()
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
     * Add error message to conversation.
     */
    private fun addErrorMessage(message: String, isRecoverable: Boolean) {
        val errorMessage = ConversationMessage.Error(
            message = message,
            isRecoverable = isRecoverable
        )
        _uiState.update { state ->
            state.copy(messages = state.messages + errorMessage)
        }
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
