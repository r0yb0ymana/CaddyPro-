package caddypro.domain.navcaddy.models

/**
 * Enum representing the primary app modules for intent routing.
 *
 * Each module represents a major functional area of the app that can be
 * targeted by user intents.
 *
 * Spec reference: navcaddy-engine.md R3
 */
enum class Module {
    /** Live caddy mode - shot recommendations, yardage, strategy */
    CADDY,

    /** Coaching and drill suggestions */
    COACH,

    /** Recovery tracking and wellness insights */
    RECOVERY,

    /** App settings and preferences */
    SETTINGS
}
