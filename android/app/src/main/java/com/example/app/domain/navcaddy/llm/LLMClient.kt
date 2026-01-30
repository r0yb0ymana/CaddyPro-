package caddypro.domain.navcaddy.llm

import caddypro.domain.navcaddy.models.SessionContext

/**
 * Interface for LLM-based intent classification.
 *
 * Abstraction layer that allows testing with mocked responses and
 * provides flexibility to switch between different LLM providers.
 *
 * Spec reference: navcaddy-engine.md R2, navcaddy-engine-plan.md Task 6
 */
interface LLMClient {
    /**
     * Classify user input into a structured intent with entities.
     *
     * @param input Raw user input (text or transcribed voice)
     * @param context Optional session context for conversational follow-ups
     * @return LLM response with classification result and metadata
     * @throws LLMException if classification fails
     */
    suspend fun classify(input: String, context: SessionContext? = null): LLMResponse
}

/**
 * Exception thrown when LLM classification fails.
 *
 * @property message Error description
 * @property cause Original throwable if available
 */
class LLMException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
