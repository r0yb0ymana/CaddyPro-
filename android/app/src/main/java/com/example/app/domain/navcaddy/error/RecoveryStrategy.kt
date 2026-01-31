package com.example.app.domain.navcaddy.error

import caddypro.domain.navcaddy.models.IntentType

/**
 * Defines recovery strategies for different error types.
 *
 * Each strategy specifies:
 * - User-facing message (Bones persona)
 * - Available recovery actions
 * - Suggested intents or fallback options
 *
 * Spec reference: navcaddy-engine.md G6, A6 (Failure-mode resilience)
 * Plan reference: navcaddy-engine-plan.md Task 23
 */
sealed class RecoveryStrategy {
    abstract val userMessage: String
    abstract val actions: List<RecoveryAction>
    abstract val suggestedIntents: List<IntentType>

    /**
     * Strategy for network errors.
     *
     * Offers offline mode and retry when connection is restored.
     */
    data class NetworkRecovery(
        override val userMessage: String = "I'm having trouble connecting right now. You can still use offline features or try again when you're back online.",
        override val actions: List<RecoveryAction> = listOf(
            RecoveryAction.RetryWithNetwork,
            RecoveryAction.UseOfflineMode,
            RecoveryAction.ShowHelp
        ),
        override val suggestedIntents: List<IntentType> = listOf(
            IntentType.SCORE_ENTRY,
            IntentType.STATS_LOOKUP,
            IntentType.EQUIPMENT_INFO
        )
    ) : RecoveryStrategy()

    /**
     * Strategy for timeout errors.
     *
     * Offers retry with fallback to local suggestions.
     */
    data class TimeoutRecovery(
        override val userMessage: String = "That's taking longer than expected. Let's try again, or I can show you some quick options.",
        override val actions: List<RecoveryAction> = listOf(
            RecoveryAction.Retry,
            RecoveryAction.ShowLocalSuggestions,
            RecoveryAction.Cancel
        ),
        override val suggestedIntents: List<IntentType> = listOf(
            IntentType.SHOT_RECOMMENDATION,
            IntentType.CLUB_ADJUSTMENT,
            IntentType.STATS_LOOKUP
        )
    ) : RecoveryStrategy()

    /**
     * Strategy for transcription errors.
     *
     * Offers text input alternative and voice retry.
     */
    data class TranscriptionRecovery(
        override val userMessage: String = "I couldn't catch that. Try typing it out, or we can try voice again.",
        override val actions: List<RecoveryAction> = listOf(
            RecoveryAction.UseTextInput,
            RecoveryAction.RetryVoice,
            RecoveryAction.ShowVoiceTips
        ),
        override val suggestedIntents: List<IntentType> = emptyList()
    ) : RecoveryStrategy()

    /**
     * Strategy for classification errors.
     *
     * Falls back to clarification with common intents.
     */
    data class ClassificationRecovery(
        override val userMessage: String = "I didn't quite catch that. Could you rephrase, or pick from these common options?",
        override val actions: List<RecoveryAction> = listOf(
            RecoveryAction.ShowClarification,
            RecoveryAction.ShowCommonIntents,
            RecoveryAction.ShowHelp
        ),
        override val suggestedIntents: List<IntentType> = listOf(
            IntentType.SHOT_RECOMMENDATION,
            IntentType.CLUB_ADJUSTMENT,
            IntentType.RECOVERY_CHECK,
            IntentType.SCORE_ENTRY,
            IntentType.HELP_REQUEST
        )
    ) : RecoveryStrategy()

    /**
     * Strategy for service unavailable errors.
     *
     * Informs user and offers offline alternatives.
     */
    data class ServiceRecovery(
        override val userMessage: String = "I'm having technical difficulties at the moment. You can still browse manually or try again in a bit.",
        override val actions: List<RecoveryAction> = listOf(
            RecoveryAction.RetryLater,
            RecoveryAction.UseOfflineMode,
            RecoveryAction.ManualNavigation
        ),
        override val suggestedIntents: List<IntentType> = listOf(
            IntentType.SCORE_ENTRY,
            IntentType.EQUIPMENT_INFO,
            IntentType.SETTINGS_CHANGE
        )
    ) : RecoveryStrategy()

    /**
     * Strategy for invalid input errors.
     *
     * Provides format hints and examples.
     */
    data class InvalidInputRecovery(
        override val userMessage: String = "Something doesn't look quite right with that input. Can you double-check and try again?",
        override val actions: List<RecoveryAction> = listOf(
            RecoveryAction.ShowInputHints,
            RecoveryAction.ShowExamples,
            RecoveryAction.Cancel
        ),
        override val suggestedIntents: List<IntentType> = emptyList()
    ) : RecoveryStrategy()

    /**
     * Strategy for missing prerequisite errors.
     *
     * Guides user to provide required data.
     */
    data class PrerequisiteRecovery(
        override val userMessage: String,
        val missingPrerequisite: String? = null,
        override val actions: List<RecoveryAction> = listOf(
            RecoveryAction.ProvidePrerequisite,
            RecoveryAction.ShowSetupGuide,
            RecoveryAction.Cancel
        ),
        override val suggestedIntents: List<IntentType> = emptyList()
    ) : RecoveryStrategy()

    /**
     * Strategy for unknown errors.
     *
     * Generic fallback with help options.
     */
    data class UnknownRecovery(
        override val userMessage: String = "Something unexpected happened. Let's try starting fresh or get some help.",
        override val actions: List<RecoveryAction> = listOf(
            RecoveryAction.Retry,
            RecoveryAction.ShowHelp,
            RecoveryAction.RestartConversation
        ),
        override val suggestedIntents: List<IntentType> = listOf(
            IntentType.HELP_REQUEST
        )
    ) : RecoveryStrategy()
}

/**
 * Available recovery actions that users can take.
 */
enum class RecoveryAction {
    /** Retry the same request */
    Retry,

    /** Retry when network is available */
    RetryWithNetwork,

    /** Retry after a delay */
    RetryLater,

    /** Retry voice input */
    RetryVoice,

    /** Switch to text input */
    UseTextInput,

    /** Switch to offline mode */
    UseOfflineMode,

    /** Show local intent suggestions */
    ShowLocalSuggestions,

    /** Show clarification dialog */
    ShowClarification,

    /** Show common intent options */
    ShowCommonIntents,

    /** Show input format hints */
    ShowInputHints,

    /** Show input examples */
    ShowExamples,

    /** Show voice input tips */
    ShowVoiceTips,

    /** Show help documentation */
    ShowHelp,

    /** Show setup guide for missing prerequisites */
    ShowSetupGuide,

    /** Provide missing prerequisite data */
    ProvidePrerequisite,

    /** Navigate manually through UI */
    ManualNavigation,

    /** Restart the conversation */
    RestartConversation,

    /** Cancel current operation */
    Cancel
}
