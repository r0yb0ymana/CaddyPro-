package caddypro.domain.navcaddy.models

import com.google.gson.annotations.SerializedName

/**
 * Domain model representing entities extracted from user input.
 *
 * Entities provide structured parameters for intent execution and routing.
 *
 * Spec reference: navcaddy-engine.md R2
 *
 * @property club Identified club (e.g., "7-iron", "driver")
 * @property yardage Distance in yards
 * @property lie Ball lie position
 * @property wind Wind description (e.g., "10mph left-to-right")
 * @property fatigue User fatigue level (1-10 scale)
 * @property pain Pain description or location
 * @property scoreContext Scoring situation (e.g., "1 under", "leading by 2")
 * @property holeNumber Specific hole number mentioned
 */
data class ExtractedEntities private constructor(
    @SerializedName("club")
    val club: Club?,

    @SerializedName("yardage")
    val yardage: Int?,

    @SerializedName("lie")
    val lie: Lie?,

    @SerializedName("wind")
    val wind: String?,

    @SerializedName("fatigue")
    val fatigue: Int?,

    @SerializedName("pain")
    val pain: String?,

    @SerializedName("score_context")
    val scoreContext: String?,

    @SerializedName("hole_number")
    val holeNumber: Int?
) {
    companion object {
        /**
         * Creates ExtractedEntities with validation.
         * Invalid values are sanitized (clamped or rejected) rather than throwing exceptions.
         * This is safer for LLM output parsing.
         */
        operator fun invoke(
            club: Club? = null,
            yardage: Int? = null,
            lie: Lie? = null,
            wind: String? = null,
            fatigue: Int? = null,
            pain: String? = null,
            scoreContext: String? = null,
            holeNumber: Int? = null
        ): ExtractedEntities = ExtractedEntities(
            club = club,
            // Reject invalid yardage (must be positive)
            yardage = yardage?.takeIf { it > 0 },
            lie = lie,
            wind = wind,
            // Clamp fatigue to valid range [1, 10]
            fatigue = fatigue?.coerceIn(1, 10),
            pain = pain,
            scoreContext = scoreContext,
            // Reject invalid hole numbers (must be 1-18)
            holeNumber = holeNumber?.takeIf { it in 1..18 }
        )
    }
}
