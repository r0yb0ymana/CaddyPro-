package caddypro.domain.navcaddy.models

/**
 * Enum representing shot miss directions and strike patterns.
 *
 * Used for tracking miss patterns and providing targeted advice.
 *
 * Spec reference: navcaddy-engine.md R5
 */
enum class MissDirection {
    /** Ball starts right of target (right-handed golfer) */
    PUSH,

    /** Ball starts left of target (right-handed golfer) */
    PULL,

    /** Ball curves significantly right */
    SLICE,

    /** Ball curves significantly left */
    HOOK,

    /** Strike hits ground before ball (loss of distance) */
    FAT,

    /** Strike hits ball above center (thin contact) */
    THIN,

    /** Shot was on target */
    STRAIGHT
}
