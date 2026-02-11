package com.caddypro.app.domain.model

/**
 * Golf course hazard types with overlay display colors
 */
enum class HazardType(val displayName: String) {
    BUNKER("Bunker"),
    WATER("Water"),
    OUT_OF_BOUNDS("Out of Bounds"),
    TREES("Trees"),
    FAIRWAY("Fairway")
}
