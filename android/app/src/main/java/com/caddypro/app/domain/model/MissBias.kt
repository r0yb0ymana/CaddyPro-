package com.caddypro.app.domain.model

/**
 * Miss bias enumeration for shot tendencies
 *
 * AC14: Miss bias selector shows visual ball flight diagram
 */
enum class MissBias(val displayName: String) {
    STRAIGHT("Straight"),
    SLICE("Slice"),
    HOOK("Hook"),
    PUSH("Push"),
    PULL("Pull");

    companion object {
        fun fromDisplayName(name: String): MissBias? {
            return values().find { it.displayName == name }
        }
    }
}
