package caddypro.domain.navcaddy.models

import com.google.gson.annotations.SerializedName

/**
 * Domain model representing a parsed user intent.
 *
 * This is the primary output of the intent classification pipeline and
 * the input to the routing orchestrator.
 *
 * Spec reference: navcaddy-engine.md R2
 *
 * @property intentId Unique identifier for this intent instance
 * @property intentType Classified intent type
 * @property confidence Classification confidence (0-1)
 * @property entities Extracted entities from user input
 * @property userGoal Optional free-text description of user's goal
 * @property routingTarget Optional routing target (null for no-navigation intents)
 */
data class ParsedIntent(
    @SerializedName("intent_id")
    val intentId: String,

    @SerializedName("intent_type")
    val intentType: IntentType,

    @SerializedName("confidence")
    val confidence: Float,

    @SerializedName("entities")
    val entities: ExtractedEntities,

    @SerializedName("user_goal")
    val userGoal: String? = null,

    @SerializedName("routing_target")
    val routingTarget: RoutingTarget? = null
) {
    init {
        require(confidence in 0f..1f) { "Confidence must be between 0 and 1" }
    }
}
