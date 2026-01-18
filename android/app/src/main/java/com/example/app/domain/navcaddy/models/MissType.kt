package com.example.app.domain.navcaddy.models

/**
 * Enum representing detailed miss types for club bias tracking.
 *
 * These provide more granular information than simple direction,
 * helping to identify swing patterns and club fit issues.
 *
 * Spec reference: player-profile-bag-management.md R4
 * Plan reference: player-profile-bag-management-plan.md Task 2
 */
enum class MissType {
    /** Ball curves significantly right (for right-handed golfer) */
    SLICE,

    /** Ball curves significantly left (for right-handed golfer) */
    HOOK,

    /** Ball starts right of target without significant curve */
    PUSH,

    /** Ball starts left of target without significant curve */
    PULL,

    /** Strike hits ground before ball (loss of distance) */
    FAT,

    /** Strike hits ball above center (thin contact) */
    THIN
}
