package caddypro.domain.navcaddy.models

/**
 * Enum representing all supported MVP intent types in the NavCaddy Engine.
 *
 * These intents map user natural language inputs to structured routing targets
 * across the app's modules (Caddy, Coach, Recovery, Settings).
 *
 * Spec reference: navcaddy-engine.md R2, Q1
 * Live Caddy reference: live-caddy-mode.md R5, live-caddy-mode-plan.md Task 24
 */
enum class IntentType {
    /** Request to adjust club distances or yardage expectations */
    CLUB_ADJUSTMENT,

    /** Query about current recovery status or readiness */
    RECOVERY_CHECK,

    /** Request for shot recommendation (club, strategy, aim) */
    SHOT_RECOMMENDATION,

    /** User wants to enter or update score for a hole */
    SCORE_ENTRY,

    /** Query about historical miss patterns or tendencies */
    PATTERN_QUERY,

    /** Request for a specific drill or practice suggestion */
    DRILL_REQUEST,

    /** Query about current or forecast weather conditions */
    WEATHER_CHECK,

    /** Request for statistics (scoring, club performance, etc.) */
    STATS_LOOKUP,

    /** User is starting a new round */
    ROUND_START,

    /** User is ending the current round */
    ROUND_END,

    /** Query about equipment (club specs, bag contents) */
    EQUIPMENT_INFO,

    /** Query about course information (hole details, layout) */
    COURSE_INFO,

    /** Request to change app settings or preferences */
    SETTINGS_CHANGE,

    /** User needs help or instructions */
    HELP_REQUEST,

    /** User providing feedback about the app */
    FEEDBACK,

    // Live Caddy Mode intents (Task 24)

    /** Query about bailout location or safe miss area */
    BAILOUT_QUERY,

    /** Query about readiness score and how it affects strategy in Live Caddy mode */
    READINESS_CHECK
}
