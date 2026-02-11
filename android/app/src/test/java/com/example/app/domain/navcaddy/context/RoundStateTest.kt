package caddypro.domain.navcaddy.context

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for RoundState and CourseConditions.
 *
 * Spec reference: navcaddy-engine.md R6, navcaddy-engine-plan.md Task 15
 */
class RoundStateTest {

    @Test
    fun `RoundState creates successfully with valid data`() {
        val roundState = RoundState(
            roundId = "round-123",
            courseName = "Pebble Beach",
            currentHole = 7,
            currentPar = 4,
            totalScore = 35,
            holesCompleted = 8
        )

        assertEquals("round-123", roundState.roundId)
        assertEquals("Pebble Beach", roundState.courseName)
        assertEquals(7, roundState.currentHole)
        assertEquals(4, roundState.currentPar)
        assertEquals(35, roundState.totalScore)
        assertEquals(8, roundState.holesCompleted)
    }

    @Test
    fun `RoundState validates current hole range`() {
        assertThrows(IllegalArgumentException::class.java) {
            RoundState(
                roundId = "round-1",
                courseName = "Test",
                currentHole = 0,
                currentPar = 4
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            RoundState(
                roundId = "round-1",
                courseName = "Test",
                currentHole = 19,
                currentPar = 4
            )
        }
    }

    @Test
    fun `RoundState validates par range`() {
        assertThrows(IllegalArgumentException::class.java) {
            RoundState(
                roundId = "round-1",
                courseName = "Test",
                currentHole = 1,
                currentPar = 2
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            RoundState(
                roundId = "round-1",
                courseName = "Test",
                currentHole = 1,
                currentPar = 6
            )
        }
    }

    @Test
    fun `RoundState validates holes completed`() {
        assertThrows(IllegalArgumentException::class.java) {
            RoundState(
                roundId = "round-1",
                courseName = "Test",
                currentHole = 1,
                currentPar = 4,
                holesCompleted = -1
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            RoundState(
                roundId = "round-1",
                courseName = "Test",
                currentHole = 1,
                currentPar = 4,
                holesCompleted = 19
            )
        }
    }

    @Test
    fun `RoundState validates total score`() {
        assertThrows(IllegalArgumentException::class.java) {
            RoundState(
                roundId = "round-1",
                courseName = "Test",
                currentHole = 1,
                currentPar = 4,
                totalScore = -1
            )
        }
    }

    @Test
    fun `RoundState accepts all valid par values`() {
        // Par 3
        val par3Round = RoundState(
            roundId = "round-1",
            courseName = "Test",
            currentHole = 3,
            currentPar = 3
        )
        assertEquals(3, par3Round.currentPar)

        // Par 4
        val par4Round = RoundState(
            roundId = "round-1",
            courseName = "Test",
            currentHole = 4,
            currentPar = 4
        )
        assertEquals(4, par4Round.currentPar)

        // Par 5
        val par5Round = RoundState(
            roundId = "round-1",
            courseName = "Test",
            currentHole = 5,
            currentPar = 5
        )
        assertEquals(5, par5Round.currentPar)
    }

    @Test
    fun `RoundState with conditions`() {
        val conditions = CourseConditions(
            weather = "Sunny",
            windSpeed = 15,
            windDirection = "NW",
            temperature = 72
        )

        val roundState = RoundState(
            roundId = "round-1",
            courseName = "Test",
            currentHole = 1,
            currentPar = 4,
            conditions = conditions
        )

        assertNotNull(roundState.conditions)
        assertEquals("Sunny", roundState.conditions?.weather)
        assertEquals(15, roundState.conditions?.windSpeed)
        assertEquals("NW", roundState.conditions?.windDirection)
        assertEquals(72, roundState.conditions?.temperature)
    }

    @Test
    fun `RoundState default values`() {
        val roundState = RoundState(
            roundId = "round-1",
            courseName = "Test",
            currentHole = 1,
            currentPar = 4
        )

        assertEquals(0, roundState.totalScore)
        assertEquals(0, roundState.holesCompleted)
        assertNull(roundState.conditions)
    }

    // CourseConditions tests

    @Test
    fun `CourseConditions creates with all fields`() {
        val conditions = CourseConditions(
            weather = "Cloudy",
            windSpeed = 20,
            windDirection = "SE",
            temperature = 65
        )

        assertEquals("Cloudy", conditions.weather)
        assertEquals(20, conditions.windSpeed)
        assertEquals("SE", conditions.windDirection)
        assertEquals(65, conditions.temperature)
    }

    @Test
    fun `CourseConditions validates wind speed`() {
        assertThrows(IllegalArgumentException::class.java) {
            CourseConditions(windSpeed = -1)
        }
    }

    @Test
    fun `CourseConditions allows zero wind speed`() {
        val conditions = CourseConditions(windSpeed = 0)
        assertEquals(0, conditions.windSpeed)
    }

    @Test
    fun `CourseConditions toDescription with all fields`() {
        val conditions = CourseConditions(
            weather = "Sunny",
            windSpeed = 10,
            windDirection = "NW",
            temperature = 75
        )

        val description = conditions.toDescription()

        assertTrue(description.contains("Sunny"))
        assertTrue(description.contains("10 mph NW wind"))
        assertTrue(description.contains("75°F"))
    }

    @Test
    fun `CourseConditions toDescription with partial fields`() {
        val conditions = CourseConditions(
            weather = "Rainy",
            windSpeed = 5
        )

        val description = conditions.toDescription()

        assertTrue(description.contains("Rainy"))
        assertTrue(description.contains("5 mph wind"))
        assertFalse(description.contains("°F"))
    }

    @Test
    fun `CourseConditions toDescription with only weather`() {
        val conditions = CourseConditions(weather = "Partly Cloudy")

        val description = conditions.toDescription()

        assertEquals("Partly Cloudy", description)
    }

    @Test
    fun `CourseConditions toDescription with only wind speed no direction`() {
        val conditions = CourseConditions(windSpeed = 12)

        val description = conditions.toDescription()

        assertEquals("12 mph wind", description)
    }

    @Test
    fun `CourseConditions toDescription with only temperature`() {
        val conditions = CourseConditions(temperature = 80)

        val description = conditions.toDescription()

        assertEquals("80°F", description)
    }

    @Test
    fun `CourseConditions toDescription empty for no fields`() {
        val conditions = CourseConditions()

        val description = conditions.toDescription()

        assertEquals("", description)
    }

    @Test
    fun `CourseConditions toDescription formats correctly`() {
        val conditions = CourseConditions(
            weather = "Overcast",
            windSpeed = 15,
            windDirection = "headwind",
            temperature = 68
        )

        val description = conditions.toDescription()

        // Should be comma-separated
        assertEquals("Overcast, 15 mph headwind wind, 68°F", description)
    }
}
