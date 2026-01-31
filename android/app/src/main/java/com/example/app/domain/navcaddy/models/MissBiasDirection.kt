package caddypro.domain.navcaddy.models

/**
 * Enum representing dominant miss direction for club bias.
 *
 * This is a simplified version of MissDirection used specifically
 * for club bias tracking in bag management.
 *
 * Spec reference: player-profile-bag-management.md R4
 * Plan reference: player-profile-bag-management-plan.md Task 2
 */
enum class MissBiasDirection {
    /** Dominant tendency is to miss left */
    LEFT,

    /** No dominant miss direction */
    STRAIGHT,

    /** Dominant tendency is to miss right */
    RIGHT
}
