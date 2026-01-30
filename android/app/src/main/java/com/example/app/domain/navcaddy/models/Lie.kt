package caddypro.domain.navcaddy.models

/**
 * Enum representing golf ball lie positions.
 *
 * Used for shot tracking and contextual recommendations.
 */
enum class Lie {
    /** Ball on tee box */
    TEE,

    /** Ball on fairway */
    FAIRWAY,

    /** Ball in rough */
    ROUGH,

    /** Ball in bunker/sand trap */
    BUNKER,

    /** Ball on green */
    GREEN,

    /** Ball on fringe/apron */
    FRINGE,

    /** Ball in hazard (water, penalty area) */
    HAZARD
}
