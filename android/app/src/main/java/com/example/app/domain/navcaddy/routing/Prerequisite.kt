package caddypro.domain.navcaddy.routing

/**
 * Enum of prerequisites that certain intents require before routing.
 *
 * Prerequisites are data requirements that must be satisfied before
 * the system can fulfill the user's intent.
 *
 * Spec reference: navcaddy-engine.md R3
 */
enum class Prerequisite {
    /**
     * Recovery data required.
     * Needed for: RECOVERY_CHECK
     *
     * User must have at least one recovery datapoint logged
     * (e.g., sleep, HRV, readiness score).
     */
    RECOVERY_DATA,

    /**
     * Active round required.
     * Needed for: SCORE_ENTRY, ROUND_END
     *
     * User must have an in-progress round to enter scores or end round.
     */
    ROUND_ACTIVE,

    /**
     * Bag configuration required.
     * Needed for: CLUB_ADJUSTMENT, SHOT_RECOMMENDATION
     *
     * User must have configured their club distances and bag contents.
     */
    BAG_CONFIGURED,

    /**
     * Course selection required.
     * Needed for: COURSE_INFO, (optionally) SHOT_RECOMMENDATION
     *
     * User must have selected the course they're playing.
     */
    COURSE_SELECTED
}
