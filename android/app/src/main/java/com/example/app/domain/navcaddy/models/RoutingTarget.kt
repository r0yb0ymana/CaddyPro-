package caddypro.domain.navcaddy.models

import com.google.gson.annotations.SerializedName

/**
 * Domain model representing a routing target for an intent.
 *
 * Defines where to navigate the user and what state to provide.
 *
 * Spec reference: navcaddy-engine.md R3
 *
 * @property module Target module
 * @property screen Target screen name within the module
 * @property parameters Key-value parameters to pass to the destination
 */
data class RoutingTarget(
    @SerializedName("module")
    val module: Module,

    @SerializedName("screen")
    val screen: String,

    @SerializedName("parameters")
    val parameters: Map<String, Any> = emptyMap()
)
