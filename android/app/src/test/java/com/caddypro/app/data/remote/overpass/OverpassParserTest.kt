package com.caddypro.app.data.remote.overpass

import com.caddypro.app.domain.model.HazardType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OverpassParserTest {

    private lateinit var parser: OverpassParser

    @Before
    fun setup() {
        parser = OverpassParser()
    }

    @Test
    fun `parse empty response returns empty bundle`() {
        val response = OverpassResponse(elements = emptyList())
        val bundle = parser.parse(response)
        assertTrue(bundle.greens.isEmpty())
        assertTrue(bundle.hazards.isEmpty())
        assertNull(bundle.courseName)
    }

    @Test
    fun `parse green way extracts centroid as green center`() {
        val response = OverpassResponse(
            elements = listOf(
                OsmElement(
                    type = "way",
                    id = 100,
                    tags = mapOf("golf" to "green", "ref" to "5"),
                    geometry = listOf(
                        OsmCoord(lat = -37.80, lon = 144.96),
                        OsmCoord(lat = -37.81, lon = 144.97),
                        OsmCoord(lat = -37.82, lon = 144.96)
                    )
                )
            )
        )
        val bundle = parser.parse(response)
        assertEquals(1, bundle.greens.size)
        assertEquals(5, bundle.greens[0].holeNumber)
        assertEquals("100", bundle.greens[0].id)
        assertEquals(-37.81, bundle.greens[0].latitude, 0.001)
        assertEquals(144.9633, bundle.greens[0].longitude, 0.001)
    }

    @Test
    fun `parse green without ref has null hole number`() {
        val response = OverpassResponse(
            elements = listOf(
                OsmElement(
                    type = "way",
                    id = 200,
                    tags = mapOf("golf" to "green"),
                    geometry = listOf(OsmCoord(lat = -37.80, lon = 144.96))
                )
            )
        )
        val bundle = parser.parse(response)
        assertEquals(1, bundle.greens.size)
        assertNull(bundle.greens[0].holeNumber)
    }

    @Test
    fun `parse bunker creates BUNKER hazard`() {
        val response = OverpassResponse(
            elements = listOf(
                OsmElement(
                    type = "way",
                    id = 300,
                    tags = mapOf("golf" to "bunker", "name" to "Greenside Bunker"),
                    geometry = listOf(
                        OsmCoord(-37.80, 144.96),
                        OsmCoord(-37.81, 144.97),
                        OsmCoord(-37.80, 144.97)
                    )
                )
            )
        )
        val bundle = parser.parse(response)
        assertEquals(1, bundle.hazards.size)
        assertEquals(HazardType.BUNKER, bundle.hazards[0].type)
        assertEquals("Greenside Bunker", bundle.hazards[0].name)
        assertEquals(3, bundle.hazards[0].coordinates.size)
    }

    @Test
    fun `parse water hazard creates WATER hazard`() {
        val response = OverpassResponse(
            elements = listOf(
                OsmElement(
                    type = "way",
                    id = 400,
                    tags = mapOf("golf" to "water_hazard"),
                    geometry = listOf(
                        OsmCoord(-37.80, 144.96),
                        OsmCoord(-37.81, 144.97),
                        OsmCoord(-37.80, 144.97)
                    )
                )
            )
        )
        val bundle = parser.parse(response)
        assertEquals(1, bundle.hazards.size)
        assertEquals(HazardType.WATER, bundle.hazards[0].type)
    }

    @Test
    fun `parse natural water creates WATER hazard`() {
        val response = OverpassResponse(
            elements = listOf(
                OsmElement(
                    type = "way",
                    id = 500,
                    tags = mapOf("natural" to "water", "name" to "Lake"),
                    geometry = listOf(
                        OsmCoord(-37.80, 144.96),
                        OsmCoord(-37.81, 144.97),
                        OsmCoord(-37.80, 144.97)
                    )
                )
            )
        )
        val bundle = parser.parse(response)
        assertEquals(1, bundle.hazards.size)
        assertEquals(HazardType.WATER, bundle.hazards[0].type)
        assertEquals("Lake", bundle.hazards[0].name)
    }

    @Test
    fun `parse fairway creates FAIRWAY hazard`() {
        val response = OverpassResponse(
            elements = listOf(
                OsmElement(
                    type = "way",
                    id = 600,
                    tags = mapOf("golf" to "fairway"),
                    geometry = listOf(
                        OsmCoord(-37.80, 144.96),
                        OsmCoord(-37.81, 144.97),
                        OsmCoord(-37.80, 144.97)
                    )
                )
            )
        )
        val bundle = parser.parse(response)
        assertEquals(1, bundle.hazards.size)
        assertEquals(HazardType.FAIRWAY, bundle.hazards[0].type)
    }

    @Test
    fun `parse golf course extracts course name`() {
        val response = OverpassResponse(
            elements = listOf(
                OsmElement(
                    type = "relation",
                    id = 700,
                    tags = mapOf("leisure" to "golf_course", "name" to "Royal Melbourne")
                )
            )
        )
        val bundle = parser.parse(response)
        assertEquals("Royal Melbourne", bundle.courseName)
    }

    @Test
    fun `greens sorted by hole number`() {
        val response = OverpassResponse(
            elements = listOf(
                OsmElement(
                    type = "way", id = 1,
                    tags = mapOf("golf" to "green", "ref" to "9"),
                    geometry = listOf(OsmCoord(-37.80, 144.96))
                ),
                OsmElement(
                    type = "way", id = 2,
                    tags = mapOf("golf" to "green", "ref" to "1"),
                    geometry = listOf(OsmCoord(-37.81, 144.97))
                ),
                OsmElement(
                    type = "way", id = 3,
                    tags = mapOf("golf" to "green", "ref" to "5"),
                    geometry = listOf(OsmCoord(-37.82, 144.98))
                )
            )
        )
        val bundle = parser.parse(response)
        assertEquals(3, bundle.greens.size)
        assertEquals(1, bundle.greens[0].holeNumber)
        assertEquals(5, bundle.greens[1].holeNumber)
        assertEquals(9, bundle.greens[2].holeNumber)
    }

    @Test
    fun `element without tags is skipped`() {
        val response = OverpassResponse(
            elements = listOf(
                OsmElement(type = "node", id = 800, tags = null, lat = -37.80, lon = 144.96)
            )
        )
        val bundle = parser.parse(response)
        assertTrue(bundle.greens.isEmpty())
        assertTrue(bundle.hazards.isEmpty())
    }

    @Test
    fun `element without coordinates is skipped`() {
        val response = OverpassResponse(
            elements = listOf(
                OsmElement(
                    type = "way",
                    id = 900,
                    tags = mapOf("golf" to "bunker"),
                    geometry = null
                )
            )
        )
        val bundle = parser.parse(response)
        assertTrue(bundle.hazards.isEmpty())
    }

    @Test
    fun `parse relation with member geometries extracts coordinates`() {
        val response = OverpassResponse(
            elements = listOf(
                OsmElement(
                    type = "relation",
                    id = 1000,
                    tags = mapOf("golf" to "green", "ref" to "18"),
                    members = listOf(
                        OsmMember(
                            type = "way",
                            ref = 1001,
                            role = "outer",
                            geometry = listOf(
                                OsmCoord(-37.80, 144.96),
                                OsmCoord(-37.81, 144.97)
                            )
                        )
                    )
                )
            )
        )
        val bundle = parser.parse(response)
        assertEquals(1, bundle.greens.size)
        assertEquals(18, bundle.greens[0].holeNumber)
    }
}
