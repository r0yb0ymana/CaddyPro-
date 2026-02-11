package com.caddypro.app.data.remote.overpass

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Overpass API JSON response DTOs
 */
@Serializable
data class OverpassResponse(
    val elements: List<OsmElement> = emptyList()
)

@Serializable
data class OsmElement(
    val type: String,
    val id: Long,
    val tags: Map<String, String>? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val geometry: List<OsmCoord>? = null,
    val bounds: OsmBounds? = null,
    val nodes: List<Long>? = null,
    val members: List<OsmMember>? = null
)

@Serializable
data class OsmCoord(
    val lat: Double,
    val lon: Double
)

@Serializable
data class OsmBounds(
    val minlat: Double,
    val minlon: Double,
    val maxlat: Double,
    val maxlon: Double
)

@Serializable
data class OsmMember(
    val type: String,
    @SerialName("ref") val ref: Long,
    val role: String? = null,
    val geometry: List<OsmCoord>? = null
)
