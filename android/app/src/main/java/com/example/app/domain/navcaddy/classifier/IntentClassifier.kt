package caddypro.domain.navcaddy.classifier

import caddypro.domain.navcaddy.intent.IntentRegistry
import caddypro.domain.navcaddy.intent.ValidationResult
import caddypro.domain.navcaddy.llm.LLMClient
import caddypro.domain.navcaddy.llm.LLMException
import caddypro.domain.navcaddy.llm.GeminiClient
import caddypro.domain.navcaddy.models.*
import caddypro.domain.navcaddy.normalizer.InputNormalizer

/**
 * Service that classifies user intents using an LLM and determines routing action.
 *
 * Classification pipeline:
 * 1. Normalize input (profanity, slang, numbers)
 * 2. Call LLM to get intent classification
 * 3. Parse response into ParsedIntent
 * 4. Validate entities against intent schema
 * 5. Determine action based on confidence thresholds
 * 6. Return appropriate ClassificationResult
 *
 * Spec reference: navcaddy-engine.md R2, navcaddy-engine-plan.md Task 6, Task 7
 *
 * @property llmClient LLM client for classification
 * @property normalizer Input normalizer for preprocessing
 */
class IntentClassifier(
    private val llmClient: LLMClient,
    private val normalizer: InputNormalizer = InputNormalizer()
) {
    /**
     * Classify user input and determine routing action.
     *
     * Flow:
     * 1. Normalize input (filter profanity, expand slang, normalize numbers)
     * 2. Call LLM to get classification
     * 3. Parse response into ParsedIntent
     * 4. Validate entities against intent schema
     * 5. Apply confidence thresholds
     * 6. Return appropriate ClassificationResult
     *
     * @param input Raw user input (text or transcribed voice)
     * @param context Optional session context
     * @return Classification result with routing decision
     */
    suspend fun classify(input: String, context: SessionContext? = null): ClassificationResult {
        return try {
            // Validate input
            if (input.isBlank()) {
                return ClassificationResult.Error(
                    cause = IllegalArgumentException("Input cannot be blank"),
                    message = "Please say or type something."
                )
            }

            // Normalize input before classification
            val normalizationResult = normalizer.normalize(input)
            val normalizedInput = normalizationResult.normalizedInput

            // Get classification from LLM using normalized input
            val llmResponse = llmClient.classify(normalizedInput, context)

            // Parse the response into a ParsedIntent
            // For GeminiClient, we need to parse the raw JSON response
            val parsedIntent = if (llmClient is GeminiClient) {
                llmClient.parseResponse(llmResponse.rawResponse)
            } else {
                throw LLMException("Unsupported LLM client type")
            }

            // Validate entities against schema
            val validationResult = IntentRegistry.validateEntities(
                intentType = parsedIntent.intentType,
                entities = parsedIntent.entities
            )

            // Handle entity validation failures
            if (validationResult is ValidationResult.MissingEntities) {
                return ClassificationResult.Clarify(
                    suggestions = listOf(parsedIntent.intentType),
                    message = buildMissingEntitiesMessage(parsedIntent.intentType, validationResult.missing),
                    originalInput = input
                )
            }

            // Determine action based on confidence
            when (ConfidenceThresholds.actionForConfidence(parsedIntent.confidence)) {
                ThresholdAction.ROUTE -> {
                    val target = parsedIntent.routingTarget
                    if (target != null) {
                        ClassificationResult.Route(
                            intent = parsedIntent,
                            target = target
                        )
                    } else {
                        // No-navigation intent (pure answer) - return as confirm
                        ClassificationResult.Confirm(
                            intent = parsedIntent,
                            message = "I'll help you with that."
                        )
                    }
                }

                ThresholdAction.CONFIRM -> {
                    val message = buildConfirmationMessage(parsedIntent)
                    ClassificationResult.Confirm(
                        intent = parsedIntent,
                        message = message
                    )
                }

                ThresholdAction.CLARIFY -> {
                    val suggestions = findSimilarIntents(normalizedInput, parsedIntent)
                    ClassificationResult.Clarify(
                        suggestions = suggestions,
                        message = buildClarificationMessage(normalizedInput),
                        originalInput = input
                    )
                }
            }
        } catch (e: LLMException) {
            ClassificationResult.Error(
                cause = e,
                message = "Unable to process your request. Please try again."
            )
        } catch (e: Exception) {
            ClassificationResult.Error(
                cause = e,
                message = "Something went wrong. Please try again."
            )
        }
    }

    /**
     * Build message for missing required entities.
     */
    private fun buildMissingEntitiesMessage(
        intentType: IntentType,
        missingEntities: List<caddypro.domain.navcaddy.intent.EntityType>
    ): String {
        val schema = IntentRegistry.getSchema(intentType)
        val entityNames = missingEntities.joinToString(", ") { it.name.lowercase() }
        return "To ${schema.description.lowercase()}, I need more information about: $entityNames"
    }

    /**
     * Build confirmation message for medium-confidence intents.
     */
    private fun buildConfirmationMessage(intent: ParsedIntent): String {
        val schema = IntentRegistry.getSchema(intent.intentType)
        return buildString {
            append("Did you want to ${schema.displayName.lowercase()}?")

            // Add entity details if present
            val entityDetails = mutableListOf<String>()
            intent.entities.club?.let { entityDetails.add("with ${it.name}") }
            intent.entities.yardage?.let { entityDetails.add("at $it yards") }
            intent.entities.lie?.let { entityDetails.add("from $it") }

            if (entityDetails.isNotEmpty()) {
                append(" (${entityDetails.joinToString(", ")})")
            }
        }
    }

    /**
     * Build clarification message for low-confidence intents.
     */
    private fun buildClarificationMessage(input: String): String {
        return "I'm not quite sure what you're asking. Did you want to:"
    }

    /**
     * Find similar intents based on user input and parsed intent.
     * Returns up to 3 suggestions.
     *
     * @param input Original user input
     * @param parsedIntent The low-confidence parsed intent
     * @return List of suggested intent types (max 3)
     */
    private fun findSimilarIntents(input: String, parsedIntent: ParsedIntent): List<IntentType> {
        // Start with the classified intent (even though confidence is low)
        val suggestions = mutableListOf(parsedIntent.intentType)

        // Add common fallback intents
        val commonIntents = listOf(
            IntentType.SHOT_RECOMMENDATION,
            IntentType.CLUB_ADJUSTMENT,
            IntentType.HELP_REQUEST
        )

        // Add common intents that aren't already in the list
        commonIntents.forEach { intent ->
            if (intent !in suggestions && suggestions.size < 3) {
                suggestions.add(intent)
            }
        }

        return suggestions.take(3)
    }
}
