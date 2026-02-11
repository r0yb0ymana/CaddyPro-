package com.caddypro.app.data.remote.overpass

import com.caddypro.app.domain.model.CourseOverlayBundle
import com.caddypro.app.domain.model.GreenData
import com.caddypro.app.domain.model.HazardData
import com.caddypro.app.domain.model.HazardType
import com.caddypro.app.domain.model.LatLngPoint
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses Overpass API response into domain models.
 *
 * Maps OSM tags to HazardType:
 * - golf=green → GreenData (centroid as green center)
 * - golf=bunker → BUNKER
 * - golf=water_hazard, golf=lateral_water_hazard, natural=water → WATER
 * - golf=fairway → FAIRWAY
 * - leisure=golf_course (relation) → extract course name
 */
@Singleton
class OverpassParser @Inject constructor() {

    fun parse(response: OverpassResponse): CourseOverlayBundle {
        val greens = mutableListOf<GreenData>()
        val hazards = mutableListOf<HazardData>()
        var courseName: String? = null

        for (element in response.elements) {
            val tags = element.tags ?: continue
            val coords = extractCoordinates(element)

            when {
                // Golf course (relation or way) — extract name
                tags["leisure"] == "golf_course" -> {
                    courseName = tags["name"]
                }

                // Green → extract centroid as green center
                tags["golf"] == "green" -> {
                    if (coords.isNotEmpty()) {
                        val centroid = calculateCentroid(coords)
                        val holeNumber = tags["ref"]?.toIntOrNull()
                        greens.add(
                            GreenData(
                                id = element.id.toString(),
                                latitude = centroid.latitude,
                                longitude = centroid.longitude,
                                holeNumber = holeNumber
                            )
                        )
                    }
                }

                // Bunker
                tags["golf"] == "bunker" -> {
                    if (coords.isNotEmpty()) {
                        hazards.add(
                            HazardData(
                                id = element.id.toString(),
                                type = HazardType.BUNKER,
                                name = tags["name"],
                                coordinates = coords
                            )
                        )
                    }
                }

                // Water hazards
                tags["golf"] == "water_hazard" ||
                tags["golf"] == "lateral_water_hazard" ||
                (tags["natural"] == "water" && tags.containsKey("golf").not()) -> {
                    if (coords.isNotEmpty()) {
                        hazards.add(
                            HazardData(
                                id = element.id.toString(),
                                type = HazardType.WATER,
                                name = tags["name"],
                                coordinates = coords
                            )
                        )
                    }
                }

                // Water that happens to be on a golf course
                tags["natural"] == "water" -> {
                    if (coords.isNotEmpty()) {
                        hazards.add(
                            HazardData(
                                id = element.id.toString(),
                                type = HazardType.WATER,
                                name = tags["name"],
                                coordinates = coords
                            )
                        )
                    }
                }

                // Fairway
                tags["golf"] == "fairway" -> {
                    if (coords.isNotEmpty()) {
                        hazards.add(
                            HazardData(
                                id = element.id.toString(),
                                type = HazardType.FAIRWAY,
                                name = tags["name"],
                                coordinates = coords
                            )
                        )
                    }
                }
            }
        }

        // Sort greens by hole number where available
        val sortedGreens = greens.sortedBy { it.holeNumber ?: Int.MAX_VALUE }

        return CourseOverlayBundle(
            greens = sortedGreens,
            hazards = hazards,
            courseName = courseName
        )
    }

    private fun extractCoordinates(element: OsmElement): List<LatLngPoint> {
        // For ways with `out geom;`, coordinates are inline in the geometry field
        val geom = element.geometry
        if (geom != null && geom.isNotEmpty()) {
            return geom.map { LatLngPoint(it.lat, it.lon) }
        }

        // For relations, try to extract from member geometries
        val members = element.members
        if (members != null) {
            val coords = mutableListOf<LatLngPoint>()
            for (member in members) {
                member.geometry?.forEach { coord ->
                    coords.add(LatLngPoint(coord.lat, coord.lon))
                }
            }
            return coords
        }

        // For nodes (single point)
        if (element.lat != null && element.lon != null) {
            return listOf(LatLngPoint(element.lat, element.lon))
        }

        return emptyList()
    }

    private fun calculateCentroid(points: List<LatLngPoint>): LatLngPoint {
        if (points.isEmpty()) return LatLngPoint(0.0, 0.0)
        val avgLat = points.sumOf { it.latitude } / points.size
        val avgLon = points.sumOf { it.longitude } / points.size
        return LatLngPoint(avgLat, avgLon)
    }
}
