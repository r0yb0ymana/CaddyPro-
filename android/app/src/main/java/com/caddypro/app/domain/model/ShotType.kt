package com.caddypro.app.domain.model

/**
 * Shot type enumeration
 */
enum class ShotType(val displayName: String) {
    TEE("Tee"),
    FAIRWAY("Fairway"),
    APPROACH("Approach"),
    CHIP("Chip"),
    PUTT("Putt"),
    PENALTY("Penalty")
}
