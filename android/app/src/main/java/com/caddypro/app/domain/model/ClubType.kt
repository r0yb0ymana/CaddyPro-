package com.caddypro.app.domain.model

/**
 * Club type enumeration
 *
 * AC13: Club type determines sort order (Driver first, Putter last)
 */
enum class ClubType(val sortOrder: Int, val displayName: String) {
    DRIVER(1, "Driver"),
    WOOD(2, "Woods"),
    HYBRID(3, "Hybrids"),
    IRON(4, "Irons"),
    WEDGE(5, "Wedges"),
    PUTTER(6, "Putter");

    companion object {
        fun fromDisplayName(name: String): ClubType? {
            return values().find { it.displayName == name }
        }
    }
}
