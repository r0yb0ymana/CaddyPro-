package caddypro.domain.navcaddy.intent

import caddypro.domain.navcaddy.models.*

/**
 * Registry of all supported intents with their schemas.
 * Provides lookup and query capabilities.
 *
 * This is the single source of truth for intent configuration.
 * All MVP intents are registered here with their required/optional entities,
 * routing targets, and example phrases.
 *
 * Spec reference: navcaddy-engine.md R2, navcaddy-engine-plan.md Task 5
 * Live Caddy reference: live-caddy-mode.md R5, live-caddy-mode-plan.md Task 24
 */
object IntentRegistry {
    private val schemas: Map<IntentType, IntentSchema> = buildSchemas()

    /**
     * Get schema for a specific intent type.
     *
     * @throws IllegalArgumentException if intent type is not registered
     */
    fun getSchema(intentType: IntentType): IntentSchema =
        schemas[intentType] ?: throw IllegalArgumentException("Unknown intent: $intentType")

    /**
     * Get all registered schemas.
     */
    fun getAllSchemas(): List<IntentSchema> = schemas.values.toList()

    /**
     * Find intents that can be routed to a specific module.
     */
    fun getIntentsForModule(module: Module): List<IntentSchema> =
        schemas.values.filter { it.defaultRoutingTarget?.module == module }

    /**
     * Find intents that don't require navigation (pure answers).
     */
    fun getNoNavigationIntents(): List<IntentSchema> =
        schemas.values.filter { !it.requiresNavigation }

    /**
     * Validate if extracted entities satisfy intent requirements.
     *
     * @return ValidationResult.Valid if all required entities are present,
     *         ValidationResult.MissingEntities otherwise
     */
    fun validateEntities(intentType: IntentType, entities: ExtractedEntities): ValidationResult {
        val schema = getSchema(intentType)
        val missingRequired = schema.requiredEntities.filter { entityType ->
            !hasEntity(entities, entityType)
        }
        return if (missingRequired.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.MissingEntities(missingRequired)
        }
    }

    /**
     * Check if an entity is present in the extracted entities.
     */
    private fun hasEntity(entities: ExtractedEntities, entityType: EntityType): Boolean = when (entityType) {
        EntityType.CLUB -> entities.club != null
        EntityType.YARDAGE -> entities.yardage != null
        EntityType.LIE -> entities.lie != null
        EntityType.WIND -> entities.wind != null
        EntityType.FATIGUE -> entities.fatigue != null
        EntityType.PAIN -> entities.pain != null
        EntityType.SCORE_CONTEXT -> entities.scoreContext != null
        EntityType.HOLE_NUMBER -> entities.holeNumber != null
        // For new entity types not yet in ExtractedEntities, return true
        // They will be validated differently or added in future updates
        else -> true
    }

    /**
     * Build all MVP intent schemas including Live Caddy mode intents.
     */
    private fun buildSchemas(): Map<IntentType, IntentSchema> = mapOf(
        IntentType.CLUB_ADJUSTMENT to IntentSchema(
            intentType = IntentType.CLUB_ADJUSTMENT,
            displayName = "Club Adjustment",
            description = "Adjust club distances or yardage expectations",
            requiredEntities = setOf(EntityType.CLUB),
            optionalEntities = setOf(EntityType.YARDAGE),
            defaultRoutingTarget = RoutingTarget(
                module = Module.CADDY,
                screen = "ClubAdjustmentScreen",
                parameters = emptyMap()
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "My 7-iron feels long today",
                "I need to adjust my driver distance",
                "Update my pitching wedge to 120 yards",
                "Change 5-iron yardage",
                "Recalibrate my 3-wood"
            )
        ),

        IntentType.RECOVERY_CHECK to IntentSchema(
            intentType = IntentType.RECOVERY_CHECK,
            displayName = "Recovery Check",
            description = "Check recovery status and readiness",
            requiredEntities = emptySet(),
            optionalEntities = setOf(EntityType.FATIGUE, EntityType.PAIN),
            defaultRoutingTarget = RoutingTarget(
                module = Module.RECOVERY,
                screen = "RecoveryOverviewScreen",
                parameters = emptyMap()
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "How's my recovery looking?",
                "Am I ready to play today?",
                "Check my recovery status",
                "What's my readiness score?",
                "How am I feeling today?"
            )
        ),

        IntentType.SHOT_RECOMMENDATION to IntentSchema(
            intentType = IntentType.SHOT_RECOMMENDATION,
            displayName = "Shot Recommendation",
            description = "Get shot advice based on current situation",
            requiredEntities = emptySet(), // Context from active round session
            optionalEntities = setOf(EntityType.CLUB, EntityType.YARDAGE, EntityType.LIE, EntityType.WIND),
            defaultRoutingTarget = RoutingTarget(
                module = Module.CADDY,
                screen = "LiveCaddyScreen",
                parameters = mapOf("expandStrategy" to true)
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "What club should I hit?",
                "150 yards into the wind, what's the play?",
                "Big tee shot, what should I do?",
                "Recommend a shot from the rough",
                "Help me with this approach shot",
                "What's the play off the tee?"
            )
        ),

        IntentType.SCORE_ENTRY to IntentSchema(
            intentType = IntentType.SCORE_ENTRY,
            displayName = "Score Entry",
            description = "Enter or update score for a hole",
            requiredEntities = emptySet(),
            optionalEntities = setOf(EntityType.HOLE_NUMBER, EntityType.SCORE_CONTEXT),
            defaultRoutingTarget = RoutingTarget(
                module = Module.CADDY,
                screen = "ScoreEntryScreen",
                parameters = emptyMap()
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "I got a birdie on this hole",
                "Mark down a par",
                "Enter score for hole 7",
                "I made a 5 on the last hole",
                "Update my score"
            )
        ),

        IntentType.PATTERN_QUERY to IntentSchema(
            intentType = IntentType.PATTERN_QUERY,
            displayName = "Pattern Query",
            description = "Ask about historical miss patterns or tendencies",
            requiredEntities = emptySet(),
            optionalEntities = setOf(EntityType.CLUB, EntityType.LIE),
            defaultRoutingTarget = null, // Pure answer, no navigation
            requiresNavigation = false,
            examplePhrases = listOf(
                "What are my miss patterns with 7-iron?",
                "Do I slice when I'm under pressure?",
                "Show my tendencies off the tee",
                "What's my common miss with wedges?",
                "Am I pushing my irons lately?"
            )
        ),

        IntentType.DRILL_REQUEST to IntentSchema(
            intentType = IntentType.DRILL_REQUEST,
            displayName = "Drill Request",
            description = "Request a practice drill or training exercise",
            requiredEntities = emptySet(),
            optionalEntities = setOf(EntityType.CLUB, EntityType.DRILL_TYPE),
            defaultRoutingTarget = RoutingTarget(
                module = Module.COACH,
                screen = "DrillScreen",
                parameters = emptyMap()
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "Give me a drill for my slice",
                "I need putting practice",
                "What drill can fix my push?",
                "Recommend a chipping drill",
                "Show me some driver drills"
            )
        ),

        IntentType.WEATHER_CHECK to IntentSchema(
            intentType = IntentType.WEATHER_CHECK,
            displayName = "Weather Check",
            description = "Check current or forecast weather conditions",
            requiredEntities = emptySet(),
            optionalEntities = emptySet(),
            defaultRoutingTarget = RoutingTarget(
                module = Module.CADDY,
                screen = "LiveCaddyScreen",
                parameters = mapOf("expandWeather" to true)
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "What's the weather looking like?",
                "How's the wind today?",
                "Check the forecast",
                "Is it going to rain?",
                "Show me the weather",
                "How's the wind?"
            )
        ),

        IntentType.STATS_LOOKUP to IntentSchema(
            intentType = IntentType.STATS_LOOKUP,
            displayName = "Stats Lookup",
            description = "Look up statistics and performance data",
            requiredEntities = emptySet(),
            optionalEntities = setOf(EntityType.STAT_TYPE, EntityType.CLUB),
            defaultRoutingTarget = RoutingTarget(
                module = Module.CADDY,
                screen = "StatsScreen",
                parameters = emptyMap()
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "Show my stats",
                "What's my average score?",
                "How am I doing with my driver?",
                "Show my fairways hit percentage",
                "What are my putting stats?"
            )
        ),

        IntentType.ROUND_START to IntentSchema(
            intentType = IntentType.ROUND_START,
            displayName = "Round Start",
            description = "Start a new round of golf",
            requiredEntities = emptySet(),
            optionalEntities = setOf(EntityType.COURSE_NAME),
            defaultRoutingTarget = RoutingTarget(
                module = Module.CADDY,
                screen = "RoundSetupScreen",
                parameters = emptyMap()
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "Start a new round",
                "I'm playing at Pebble Beach today",
                "Begin round",
                "Let's tee off",
                "Starting a round at my home course"
            )
        ),

        IntentType.ROUND_END to IntentSchema(
            intentType = IntentType.ROUND_END,
            displayName = "Round End",
            description = "End the current round and view summary",
            requiredEntities = emptySet(),
            optionalEntities = emptySet(),
            defaultRoutingTarget = RoutingTarget(
                module = Module.CADDY,
                screen = "RoundSummaryScreen",
                parameters = emptyMap()
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "Finish this round",
                "End round",
                "I'm done playing",
                "Show me the round summary",
                "Complete this round"
            )
        ),

        IntentType.EQUIPMENT_INFO to IntentSchema(
            intentType = IntentType.EQUIPMENT_INFO,
            displayName = "Equipment Info",
            description = "Get information about equipment and bag contents",
            requiredEntities = emptySet(),
            optionalEntities = setOf(EntityType.EQUIPMENT_TYPE, EntityType.CLUB),
            defaultRoutingTarget = RoutingTarget(
                module = Module.SETTINGS,
                screen = "EquipmentScreen",
                parameters = emptyMap()
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "What's in my bag?",
                "Show my club specs",
                "Tell me about my driver",
                "What equipment am I using?",
                "Show my club distances"
            )
        ),

        IntentType.COURSE_INFO to IntentSchema(
            intentType = IntentType.COURSE_INFO,
            displayName = "Course Info",
            description = "Get course information and hole details",
            requiredEntities = emptySet(),
            optionalEntities = setOf(EntityType.COURSE_NAME, EntityType.HOLE_NUMBER),
            defaultRoutingTarget = RoutingTarget(
                module = Module.CADDY,
                screen = "CourseInfoScreen",
                parameters = emptyMap()
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "Tell me about this hole",
                "What's the yardage on hole 7?",
                "Show the course layout",
                "Course information",
                "What's the layout of this hole?"
            )
        ),

        IntentType.SETTINGS_CHANGE to IntentSchema(
            intentType = IntentType.SETTINGS_CHANGE,
            displayName = "Settings Change",
            description = "Change app settings or preferences",
            requiredEntities = emptySet(),
            optionalEntities = setOf(EntityType.SETTING_KEY),
            defaultRoutingTarget = RoutingTarget(
                module = Module.SETTINGS,
                screen = "SettingsScreen",
                parameters = emptyMap()
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "Change my settings",
                "Update my preferences",
                "Turn on notifications",
                "Change units to metric",
                "Open settings"
            )
        ),

        IntentType.HELP_REQUEST to IntentSchema(
            intentType = IntentType.HELP_REQUEST,
            displayName = "Help Request",
            description = "Get help or instructions about the app",
            requiredEntities = emptySet(),
            optionalEntities = emptySet(),
            defaultRoutingTarget = null, // Pure answer, no navigation
            requiresNavigation = false,
            examplePhrases = listOf(
                "Help me",
                "How do I use this?",
                "What can you do?",
                "I need help",
                "Show me what you can do"
            )
        ),

        IntentType.FEEDBACK to IntentSchema(
            intentType = IntentType.FEEDBACK,
            displayName = "Feedback",
            description = "Provide feedback about the app",
            requiredEntities = emptySet(),
            optionalEntities = setOf(EntityType.FEEDBACK_TEXT),
            defaultRoutingTarget = RoutingTarget(
                module = Module.SETTINGS,
                screen = "FeedbackScreen",
                parameters = emptyMap()
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "I have feedback",
                "Report a problem",
                "Send feedback",
                "I found a bug",
                "Suggestion for improvement"
            )
        ),

        // Live Caddy Mode intents (Task 24)

        IntentType.BAILOUT_QUERY to IntentSchema(
            intentType = IntentType.BAILOUT_QUERY,
            displayName = "Bailout Query",
            description = "Ask where to aim for safe miss or bailout area",
            requiredEntities = emptySet(),
            optionalEntities = emptySet(),
            defaultRoutingTarget = RoutingTarget(
                module = Module.CADDY,
                screen = "LiveCaddyScreen",
                parameters = mapOf("expandStrategy" to true, "highlightBailout" to true)
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "Where's the bailout?",
                "Where should I miss?",
                "What's the safe play?",
                "Where can I bail out?",
                "What's the safest area to miss?"
            )
        ),

        IntentType.READINESS_CHECK to IntentSchema(
            intentType = IntentType.READINESS_CHECK,
            displayName = "Readiness Check",
            description = "Check readiness score and how it affects strategy",
            requiredEntities = emptySet(),
            optionalEntities = emptySet(),
            defaultRoutingTarget = RoutingTarget(
                module = Module.CADDY,
                screen = "LiveCaddyScreen",
                parameters = mapOf("expandReadiness" to true)
            ),
            requiresNavigation = true,
            examplePhrases = listOf(
                "How am I feeling?",
                "What's my readiness?",
                "Am I ready for this shot?",
                "How's my body doing?",
                "Should I play conservative today?"
            )
        )
    )
}

/**
 * Result of validating entities against an intent schema.
 */
sealed class ValidationResult {
    /** All required entities are present */
    object Valid : ValidationResult()

    /** Some required entities are missing */
    data class MissingEntities(val missing: List<EntityType>) : ValidationResult()
}
