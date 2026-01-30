package caddypro.domain.navcaddy.models

import com.google.gson.annotations.SerializedName

/**
 * Domain model representing pressure context for a shot.
 *
 * Pressure context helps identify patterns that emerge under specific
 * competitive or scoring situations.
 *
 * Spec reference: navcaddy-engine.md R5, Q4
 *
 * @property isUserTagged Whether the user explicitly tagged this as a pressure shot
 * @property isInferred Whether pressure was inferred from scoring context
 * @property scoringContext Description of scoring situation (e.g., "leading by 1", "tournament mode")
 */
data class PressureContext(
    @SerializedName("is_user_tagged")
    val isUserTagged: Boolean = false,

    @SerializedName("is_inferred")
    val isInferred: Boolean = false,

    @SerializedName("scoring_context")
    val scoringContext: String? = null
) {
    /**
     * Indicates whether any pressure condition is present.
     */
    val hasPressure: Boolean
        get() = isUserTagged || isInferred
}
