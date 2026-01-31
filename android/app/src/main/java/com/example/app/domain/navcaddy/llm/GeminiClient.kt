package caddypro.domain.navcaddy.llm

import caddypro.domain.navcaddy.intent.IntentRegistry
import caddypro.domain.navcaddy.models.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Gemini-based LLM client for intent classification.
 *
 * This implementation builds prompts with intent schemas,
 * makes API calls to Gemini 3 Flash, and parses JSON responses
 * into structured intents with entities.
 *
 * Spec reference: navcaddy-engine.md R2, navcaddy-engine-plan.md Task 6
 *
 * @property apiKey Gemini API key
 * @property modelName Gemini model to use (default: gemini-3-flash)
 * @property gson JSON serializer
 */
class GeminiClient(
    private val apiKey: String,
    private val modelName: String = "gemini-3-flash",
    private val gson: Gson = Gson()
) : LLMClient {

    /**
     * Classify user input using Gemini API.
     *
     * Flow:
     * 1. Build system prompt with intent schemas
     * 2. Build user prompt with input and context
     * 3. Make API call (currently mocked - TODO: implement actual API)
     * 4. Parse JSON response
     * 5. Map to ParsedIntent
     *
     * @param input Raw user input
     * @param context Optional session context
     * @return LLM response with parsed intent
     * @throws LLMException on timeout, network error, or parse failure
     */
    override suspend fun classify(input: String, context: SessionContext?): LLMResponse =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            try {
                // Build the classification prompt
                val systemPrompt = buildSystemPrompt()
                val userPrompt = buildUserPrompt(input, context)

                // TODO: Implement actual Gemini API call
                // For now, return a mock response
                val rawResponse = mockGeminiCall(systemPrompt, userPrompt)

                val latency = System.currentTimeMillis() - startTime

                LLMResponse(
                    rawResponse = rawResponse,
                    latencyMs = latency,
                    modelName = modelName,
                    tokenCount = null // Will be populated from API response
                )
            } catch (e: Exception) {
                throw LLMException("Gemini classification failed: ${e.message}", e)
            }
        }

    /**
     * Build system prompt with intent schemas and entity definitions.
     *
     * The system prompt includes:
     * - Role definition (golf caddy assistant)
     * - List of all supported intents with descriptions
     * - Entity schemas with types and examples
     * - Output format specification (JSON)
     */
    private fun buildSystemPrompt(): String {
        val intents = IntentRegistry.getAllSchemas()

        return buildString {
            appendLine("You are a golf caddy assistant that classifies user intents.")
            appendLine()
            appendLine("# Supported Intents")
            appendLine()
            intents.forEach { schema ->
                appendLine("## ${schema.intentType.name}")
                appendLine("Description: ${schema.description}")
                appendLine("Required entities: ${schema.requiredEntities.joinToString(", ")}")
                appendLine("Optional entities: ${schema.optionalEntities.joinToString(", ")}")
                appendLine("Example phrases:")
                schema.examplePhrases.take(3).forEach { phrase ->
                    appendLine("  - \"$phrase\"")
                }
                appendLine()
            }
            appendLine("# Entity Schemas")
            appendLine()
            appendLine("- club: Golf club (e.g., \"7-iron\", \"driver\", \"PW\")")
            appendLine("- yardage: Distance in yards (positive integer)")
            appendLine("- lie: Ball position (fairway, rough, bunker, green)")
            appendLine("- wind: Wind description (e.g., \"10mph left-to-right\")")
            appendLine("- fatigue: Fatigue level (1-10 scale)")
            appendLine("- pain: Pain description or body location")
            appendLine("- score_context: Scoring situation (e.g., \"1 under\", \"leading by 2\")")
            appendLine("- hole_number: Specific hole (1-18)")
            appendLine()
            appendLine("# Output Format")
            appendLine()
            appendLine("Return JSON with:")
            appendLine("- intent_type: One of the supported intent type names")
            appendLine("- confidence: Float between 0 and 1")
            appendLine("- entities: Object with extracted entity values (null if not present)")
            appendLine("- user_goal: Brief description of user's goal (optional)")
            appendLine()
            appendLine("Example output:")
            appendLine("""
                {
                  "intent_type": "CLUB_ADJUSTMENT",
                  "confidence": 0.92,
                  "entities": {
                    "club": "7-iron",
                    "yardage": null
                  },
                  "user_goal": "Adjust 7-iron distance"
                }
            """.trimIndent())
        }
    }

    /**
     * Build user prompt with input and optional context.
     *
     * @param input Raw user input
     * @param context Optional session context for follow-ups
     */
    private fun buildUserPrompt(input: String, context: SessionContext?): String {
        return buildString {
            if (context != null && context.conversationHistory.isNotEmpty()) {
                appendLine("# Conversation Context")
                appendLine()
                context.conversationHistory.takeLast(3).forEach { turn ->
                    appendLine("User: ${turn.userInput}")
                    appendLine("Assistant: ${turn.assistantResponse}")
                    appendLine()
                }
            }

            if (context?.currentRound != null) {
                appendLine("# Round Context")
                appendLine("Current hole: ${context.currentHole ?: "Unknown"}")
                context.lastShot?.let {
                    appendLine("Last shot: ${it.club.name} from ${it.lie}")
                }
                appendLine()
            }

            appendLine("# User Input")
            appendLine(input)
        }
    }

    /**
     * Parse JSON response from Gemini into ParsedIntent.
     *
     * @param json Raw JSON string from Gemini
     * @return Parsed intent with entities
     * @throws LLMException if JSON is invalid or missing required fields
     */
    fun parseResponse(json: String): ParsedIntent {
        try {
            val jsonObject = JsonParser.parseString(json).asJsonObject

            val intentTypeStr = jsonObject.get("intent_type").asString
            val intentType = try {
                IntentType.valueOf(intentTypeStr)
            } catch (e: IllegalArgumentException) {
                throw LLMException("Invalid intent type: $intentTypeStr")
            }

            val confidence = jsonObject.get("confidence").asFloat
            val userGoal = jsonObject.get("user_goal")?.asString

            // Parse entities
            val entitiesObj = jsonObject.getAsJsonObject("entities")
            val entities = parseEntities(entitiesObj)

            // Get routing target from registry
            val schema = IntentRegistry.getSchema(intentType)
            val routingTarget = schema.defaultRoutingTarget

            return ParsedIntent(
                intentId = UUID.randomUUID().toString(),
                intentType = intentType,
                confidence = confidence,
                entities = entities,
                userGoal = userGoal,
                routingTarget = routingTarget
            )
        } catch (e: Exception) {
            throw LLMException("Failed to parse LLM response: ${e.message}", e)
        }
    }

    /**
     * Parse entities from JSON object.
     *
     * Handles missing/null values gracefully and validates extracted values.
     */
    private fun parseEntities(entitiesObj: JsonObject): ExtractedEntities {
        // Parse club if present
        val club = entitiesObj.get("club")?.asString?.let { clubStr ->
            parseClub(clubStr)
        }

        val yardage = entitiesObj.get("yardage")?.asInt
        val lie = entitiesObj.get("lie")?.asString?.let { lieStr ->
            parseLie(lieStr)
        }
        val wind = entitiesObj.get("wind")?.asString
        val fatigue = entitiesObj.get("fatigue")?.asInt
        val pain = entitiesObj.get("pain")?.asString
        val scoreContext = entitiesObj.get("score_context")?.asString
        val holeNumber = entitiesObj.get("hole_number")?.asInt

        return ExtractedEntities(
            club = club,
            yardage = yardage,
            lie = lie,
            wind = wind,
            fatigue = fatigue,
            pain = pain,
            scoreContext = scoreContext,
            holeNumber = holeNumber
        )
    }

    /**
     * Parse club string into Club data class.
     * Handles common variations (7i, 7-iron, seven iron, etc.)
     */
    private fun parseClub(clubStr: String): Club? {
        val normalized = clubStr.lowercase().replace("-", "").replace(" ", "")

        // Extract club type and create Club instance
        return when {
            normalized.contains("driver") || normalized == "d" || normalized == "1w" ->
                Club(name = "Driver", type = ClubType.DRIVER, loft = 10.5, estimatedCarry = 230)

            normalized.contains("3wood") || normalized == "3w" ->
                Club(name = "3-Wood", type = ClubType.WOOD, loft = 15.0, estimatedCarry = 210)

            normalized.contains("5wood") || normalized == "5w" ->
                Club(name = "5-Wood", type = ClubType.WOOD, loft = 18.0, estimatedCarry = 195)

            normalized.contains("hybrid") || normalized.startsWith("h") -> {
                // Try to extract number (e.g., 3h, 4h)
                val num = normalized.filter { it.isDigit() }.toIntOrNull() ?: 3
                when (num) {
                    3 -> Club(name = "3-Hybrid", type = ClubType.HYBRID, loft = 19.0, estimatedCarry = 185)
                    4 -> Club(name = "4-Hybrid", type = ClubType.HYBRID, loft = 22.0, estimatedCarry = 175)
                    5 -> Club(name = "5-Hybrid", type = ClubType.HYBRID, loft = 25.0, estimatedCarry = 165)
                    else -> null
                }
            }

            normalized.matches(Regex("\\d+i(ron)?")) -> {
                // Extract number from patterns like "7i", "7iron"
                val num = normalized.filter { it.isDigit() }.toIntOrNull() ?: return null
                when (num) {
                    3 -> Club(name = "3-Iron", type = ClubType.IRON, loft = 20.0, estimatedCarry = 180)
                    4 -> Club(name = "4-Iron", type = ClubType.IRON, loft = 23.0, estimatedCarry = 170)
                    5 -> Club(name = "5-Iron", type = ClubType.IRON, loft = 26.0, estimatedCarry = 160)
                    6 -> Club(name = "6-Iron", type = ClubType.IRON, loft = 29.0, estimatedCarry = 150)
                    7 -> Club(name = "7-Iron", type = ClubType.IRON, loft = 33.0, estimatedCarry = 140)
                    8 -> Club(name = "8-Iron", type = ClubType.IRON, loft = 37.0, estimatedCarry = 130)
                    9 -> Club(name = "9-Iron", type = ClubType.IRON, loft = 41.0, estimatedCarry = 120)
                    else -> null
                }
            }

            normalized.contains("pw") || normalized.contains("pitching") ->
                Club(name = "Pitching Wedge", type = ClubType.WEDGE, loft = 46.0, estimatedCarry = 110)

            normalized.contains("gw") || normalized.contains("gap") ->
                Club(name = "Gap Wedge", type = ClubType.WEDGE, loft = 50.0, estimatedCarry = 100)

            normalized.contains("sw") || normalized.contains("sand") ->
                Club(name = "Sand Wedge", type = ClubType.WEDGE, loft = 54.0, estimatedCarry = 85)

            normalized.contains("lw") || normalized.contains("lob") ->
                Club(name = "Lob Wedge", type = ClubType.WEDGE, loft = 60.0, estimatedCarry = 70)

            normalized.contains("putter") || normalized == "p" ->
                Club(name = "Putter", type = ClubType.PUTTER, loft = 3.0, estimatedCarry = 0)

            else -> null
        }
    }

    /**
     * Parse lie string into Lie enum.
     */
    private fun parseLie(lieStr: String): Lie? {
        val normalized = lieStr.lowercase()
        return when {
            normalized.contains("fairway") -> Lie.FAIRWAY
            normalized.contains("rough") -> Lie.ROUGH
            normalized.contains("bunker") || normalized.contains("sand") -> Lie.BUNKER
            normalized.contains("green") -> Lie.GREEN
            else -> null
        }
    }

    /**
     * Mock Gemini API call for testing.
     * TODO: Replace with actual Gemini API implementation.
     */
    private fun mockGeminiCall(systemPrompt: String, userPrompt: String): String {
        // This is a placeholder that returns a valid JSON response
        // In production, this would make an actual HTTP request to Gemini API
        return """
            {
              "intent_type": "SHOT_RECOMMENDATION",
              "confidence": 0.85,
              "entities": {
                "club": null,
                "yardage": 150,
                "lie": "fairway",
                "wind": "10mph headwind"
              },
              "user_goal": "Get shot recommendation"
            }
        """.trimIndent()
    }
}
