package com.caddypro.app.data.remote.overpass

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.parameters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Overpass API service for fetching OSM golf course data
 *
 * AC10: Hazard data fetched from Overpass API (OSM)
 */
@Singleton
class OverpassApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    companion object {
        private const val OVERPASS_URL = "https://overpass-api.de/api/interpreter"
    }

    /**
     * Fetch golf course features near a location.
     * Uses `out geom;` to get inline coordinates on ways.
     */
    suspend fun getGolfFeatures(lat: Double, lon: Double, radiusMeters: Int = 2000): OverpassResponse {
        val query = buildOverpassQuery(lat, lon, radiusMeters)
        return httpClient.submitForm(
            url = OVERPASS_URL,
            formParameters = parameters {
                append("data", query)
            }
        ).body()
    }

    private fun buildOverpassQuery(lat: Double, lon: Double, radius: Int): String {
        return """
            [out:json][timeout:25];
            (
              way["golf"="green"](around:$radius,$lat,$lon);
              way["golf"="bunker"](around:$radius,$lat,$lon);
              way["golf"="fairway"](around:$radius,$lat,$lon);
              way["golf"="tee"](around:$radius,$lat,$lon);
              way["golf"="water_hazard"](around:$radius,$lat,$lon);
              way["golf"="lateral_water_hazard"](around:$radius,$lat,$lon);
              way["natural"="water"](around:$radius,$lat,$lon);
              way["leisure"="golf_course"](around:$radius,$lat,$lon);
              relation["leisure"="golf_course"](around:$radius,$lat,$lon);
            );
            out geom;
        """.trimIndent()
    }
}
