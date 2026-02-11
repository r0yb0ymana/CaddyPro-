package caddypro.domain.navcaddy.intent

import caddypro.domain.navcaddy.models.*

/**
 * Schema definition for an intent type.
 * Defines required/optional entities and default routing.
 *
 * Spec reference: navcaddy-engine.md R2
 *
 * @property intentType The type of intent this schema defines
 * @property displayName Human-readable name for this intent
 * @property description Brief explanation of what this intent does
 * @property requiredEntities Entities that must be present for valid routing
 * @property optionalEntities Entities that enhance the experience if present
 * @property defaultRoutingTarget Where to navigate by default (null for pure answers)
 * @property requiresNavigation Whether this intent needs UI navigation (false for Q&A)
 * @property examplePhrases Example user inputs that match this intent
 */
data class IntentSchema(
    val intentType: IntentType,
    val displayName: String,
    val description: String,
    val requiredEntities: Set<EntityType>,
    val optionalEntities: Set<EntityType>,
    val defaultRoutingTarget: RoutingTarget?,
    val requiresNavigation: Boolean = true,
    val examplePhrases: List<String> = emptyList()
)

/**
 * Types of entities that can be extracted from user input.
 *
 * Each entity type corresponds to a field in ExtractedEntities or
 * represents a category of information that can be extracted.
 */
enum class EntityType {
    /** Golf club (e.g., "7-iron", "driver") */
    CLUB,

    /** Distance in yards */
    YARDAGE,

    /** Ball lie position (fairway, rough, etc.) */
    LIE,

    /** Wind conditions description */
    WIND,

    /** User fatigue level (1-10) */
    FATIGUE,

    /** Pain description or location */
    PAIN,

    /** Scoring situation context */
    SCORE_CONTEXT,

    /** Specific hole number (1-18) */
    HOLE_NUMBER,

    /** Type of drill requested */
    DRILL_TYPE,

    /** Type of statistic to look up */
    STAT_TYPE,

    /** Course name */
    COURSE_NAME,

    /** Equipment type or category */
    EQUIPMENT_TYPE,

    /** Setting key or preference name */
    SETTING_KEY,

    /** Free-form feedback text */
    FEEDBACK_TEXT
}
