package com.caddypro.app.domain.model

/**
 * Approximate shot direction for wind factor calculation
 * For MVP: user selects compass direction. Full Hole Map integration in R2.
 */
enum class ShotDirection(val degrees: Int, val displayName: String) {
    N(0, "N"),
    NE(45, "NE"),
    E(90, "E"),
    SE(135, "SE"),
    S(180, "S"),
    SW(225, "SW"),
    W(270, "W"),
    NW(315, "NW")
}
