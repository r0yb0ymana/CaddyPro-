package caddypro.domain.navcaddy.models

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for NavCaddy Engine domain models.
 *
 * Verifies:
 * - All enums have expected values
 * - Data class equality works correctly
 * - Serialization roundtrip works with Gson
 * - Validation rules are enforced
 */
class NavCaddyModelsTest {

    private val gson = Gson()

    // region Enum Tests

    @Test
    fun `IntentType enum has all 15 MVP intents`() {
        val expectedIntents = setOf(
            IntentType.CLUB_ADJUSTMENT,
            IntentType.RECOVERY_CHECK,
            IntentType.SHOT_RECOMMENDATION,
            IntentType.SCORE_ENTRY,
            IntentType.PATTERN_QUERY,
            IntentType.DRILL_REQUEST,
            IntentType.WEATHER_CHECK,
            IntentType.STATS_LOOKUP,
            IntentType.ROUND_START,
            IntentType.ROUND_END,
            IntentType.EQUIPMENT_INFO,
            IntentType.COURSE_INFO,
            IntentType.SETTINGS_CHANGE,
            IntentType.HELP_REQUEST,
            IntentType.FEEDBACK
        )

        val actualIntents = IntentType.values().toSet()
        assertEquals(expectedIntents, actualIntents)
        assertEquals(15, IntentType.values().size)
    }

    @Test
    fun `Module enum has all 4 modules`() {
        val expectedModules = setOf(
            Module.CADDY,
            Module.COACH,
            Module.RECOVERY,
            Module.SETTINGS
        )

        val actualModules = Module.values().toSet()
        assertEquals(expectedModules, actualModules)
        assertEquals(4, Module.values().size)
    }

    @Test
    fun `ClubType enum has all 6 club types`() {
        val expectedTypes = setOf(
            ClubType.DRIVER,
            ClubType.WOOD,
            ClubType.HYBRID,
            ClubType.IRON,
            ClubType.WEDGE,
            ClubType.PUTTER
        )

        val actualTypes = ClubType.values().toSet()
        assertEquals(expectedTypes, actualTypes)
        assertEquals(6, ClubType.values().size)
    }

    @Test
    fun `MissDirection enum has all 7 directions`() {
        val expectedDirections = setOf(
            MissDirection.PUSH,
            MissDirection.PULL,
            MissDirection.SLICE,
            MissDirection.HOOK,
            MissDirection.FAT,
            MissDirection.THIN,
            MissDirection.STRAIGHT
        )

        val actualDirections = MissDirection.values().toSet()
        assertEquals(expectedDirections, actualDirections)
        assertEquals(7, MissDirection.values().size)
    }

    @Test
    fun `Lie enum has all 7 lie types`() {
        val expectedLies = setOf(
            Lie.TEE,
            Lie.FAIRWAY,
            Lie.ROUGH,
            Lie.BUNKER,
            Lie.GREEN,
            Lie.FRINGE,
            Lie.HAZARD
        )

        val actualLies = Lie.values().toSet()
        assertEquals(expectedLies, actualLies)
        assertEquals(7, Lie.values().size)
    }

    @Test
    fun `Role enum has USER and ASSISTANT`() {
        val expectedRoles = setOf(Role.USER, Role.ASSISTANT)
        val actualRoles = Role.values().toSet()
        assertEquals(expectedRoles, actualRoles)
        assertEquals(2, Role.values().size)
    }

    // endregion

    // region Data Class Equality Tests

    @Test
    fun `Club equality works correctly`() {
        val club1 = Club(name = "7-iron", type = ClubType.IRON, loft = 34f, distance = 150)
        val club2 = Club(name = "7-iron", type = ClubType.IRON, loft = 34f, distance = 150)
        val club3 = Club(name = "8-iron", type = ClubType.IRON, loft = 38f, distance = 140)

        assertEquals(club1, club2)
        assertTrue(club1 != club3)
    }

    @Test
    fun `PressureContext equality works correctly`() {
        val context1 = PressureContext(
            isUserTagged = true,
            isInferred = false,
            scoringContext = "leading by 1"
        )
        val context2 = PressureContext(
            isUserTagged = true,
            isInferred = false,
            scoringContext = "leading by 1"
        )
        val context3 = PressureContext(
            isUserTagged = false,
            isInferred = true,
            scoringContext = "trailing by 2"
        )

        assertEquals(context1, context2)
        assertTrue(context1 != context3)
    }

    @Test
    fun `Shot equality works correctly`() {
        val club = Club(name = "Driver", type = ClubType.DRIVER)
        val shot1 = Shot(
            id = "shot-1",
            timestamp = 1234567890L,
            club = club,
            missDirection = MissDirection.SLICE,
            lie = Lie.TEE,
            pressureContext = PressureContext(),
            holeNumber = 1
        )
        val shot2 = Shot(
            id = "shot-1",
            timestamp = 1234567890L,
            club = club,
            missDirection = MissDirection.SLICE,
            lie = Lie.TEE,
            pressureContext = PressureContext(),
            holeNumber = 1
        )

        assertEquals(shot1, shot2)
    }

    @Test
    fun `ParsedIntent equality works correctly`() {
        val intent1 = ParsedIntent(
            intentId = "intent-1",
            intentType = IntentType.SHOT_RECOMMENDATION,
            confidence = 0.85f,
            entities = ExtractedEntities()
        )
        val intent2 = ParsedIntent(
            intentId = "intent-1",
            intentType = IntentType.SHOT_RECOMMENDATION,
            confidence = 0.85f,
            entities = ExtractedEntities()
        )

        assertEquals(intent1, intent2)
    }

    // endregion

    // region Serialization Tests

    @Test
    fun `Club serializes and deserializes correctly`() {
        val original = Club(
            name = "Pitching Wedge",
            type = ClubType.WEDGE,
            loft = 46f,
            distance = 120
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, Club::class.java)

        assertEquals(original, deserialized)
        assertTrue(json.contains("\"name\":\"Pitching Wedge\""))
        assertTrue(json.contains("\"type\":\"WEDGE\""))
    }

    @Test
    fun `Shot serializes and deserializes correctly`() {
        val original = Shot(
            id = "shot-123",
            timestamp = 1700000000L,
            club = Club(name = "7-iron", type = ClubType.IRON),
            missDirection = MissDirection.PUSH,
            lie = Lie.FAIRWAY,
            pressureContext = PressureContext(isUserTagged = true),
            holeNumber = 5,
            notes = "Into the wind"
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, Shot::class.java)

        assertEquals(original, deserialized)
        assertEquals("shot-123", deserialized.id)
        assertEquals(MissDirection.PUSH, deserialized.missDirection)
        assertTrue(deserialized.pressureContext.isUserTagged)
    }

    @Test
    fun `ParsedIntent serializes and deserializes correctly`() {
        val original = ParsedIntent(
            intentId = "intent-456",
            intentType = IntentType.CLUB_ADJUSTMENT,
            confidence = 0.92f,
            entities = ExtractedEntities(
                club = Club(name = "Driver", type = ClubType.DRIVER),
                yardage = 280
            ),
            userGoal = "Adjust driver distance",
            routingTarget = RoutingTarget(
                module = Module.CADDY,
                screen = "club_adjustment",
                parameters = mapOf("club" to "driver")
            )
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, ParsedIntent::class.java)

        assertEquals(original.intentId, deserialized.intentId)
        assertEquals(original.intentType, deserialized.intentType)
        assertEquals(original.confidence, deserialized.confidence, 0.001f)
        assertEquals(280, deserialized.entities.yardage)
    }

    @Test
    fun `ExtractedEntities serializes and deserializes correctly`() {
        val original = ExtractedEntities(
            club = Club(name = "9-iron", type = ClubType.IRON),
            yardage = 135,
            lie = Lie.ROUGH,
            wind = "10mph headwind",
            fatigue = 7,
            scoreContext = "2 over par",
            holeNumber = 12
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, ExtractedEntities::class.java)

        assertEquals(original, deserialized)
        assertEquals(135, deserialized.yardage)
        assertEquals(Lie.ROUGH, deserialized.lie)
        assertEquals(7, deserialized.fatigue)
    }

    @Test
    fun `MissPattern serializes and deserializes correctly`() {
        val original = MissPattern(
            direction = MissDirection.SLICE,
            club = Club(name = "Driver", type = ClubType.DRIVER),
            frequency = 8,
            confidence = 0.78f,
            pressureContext = PressureContext(isInferred = true, scoringContext = "tournament"),
            lastOccurrence = 1700000000L
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, MissPattern::class.java)

        assertEquals(original, deserialized)
        assertEquals(MissDirection.SLICE, deserialized.direction)
        assertEquals(8, deserialized.frequency)
        assertEquals(0.78f, deserialized.confidence, 0.001f)
    }

    @Test
    fun `SessionContext serializes and deserializes correctly`() {
        val round = Round(
            id = "round-789",
            startTime = 1700000000L,
            courseName = "Pebble Beach",
            scores = mapOf(1 to 4, 2 to 3, 3 to 5)
        )

        val original = SessionContext(
            currentRound = round,
            currentHole = 4,
            lastRecommendation = "Take the 7-iron",
            conversationHistory = listOf(
                ConversationTurn(Role.USER, "What club?", 1700000000L),
                ConversationTurn(Role.ASSISTANT, "7-iron", 1700000001L)
            )
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, SessionContext::class.java)

        assertEquals(4, deserialized.currentHole)
        assertEquals("Pebble Beach", deserialized.currentRound?.courseName)
        assertEquals(2, deserialized.conversationHistory.size)
    }

    @Test
    fun `Round serializes and deserializes correctly`() {
        val original = Round(
            id = "round-abc",
            startTime = 1700000000L,
            courseName = "Augusta National",
            scores = mapOf(1 to 4, 2 to 3, 3 to 5, 4 to 4)
        )

        val json = gson.toJson(original)
        val deserialized = gson.fromJson(json, Round::class.java)

        assertEquals(original, deserialized)
        assertEquals("Augusta National", deserialized.courseName)
        assertEquals(4, deserialized.scores.size)
        assertEquals(3, deserialized.scores[2])
    }

    // endregion

    // region Validation Tests

    @Test
    fun `ParsedIntent rejects invalid confidence`() {
        assertThrows(IllegalArgumentException::class.java) {
            ParsedIntent(
                intentId = "test",
                intentType = IntentType.SHOT_RECOMMENDATION,
                confidence = 1.5f,
                entities = ExtractedEntities()
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            ParsedIntent(
                intentId = "test",
                intentType = IntentType.SHOT_RECOMMENDATION,
                confidence = -0.1f,
                entities = ExtractedEntities()
            )
        }
    }

    @Test
    fun `MissPattern rejects invalid confidence`() {
        assertThrows(IllegalArgumentException::class.java) {
            MissPattern(
                direction = MissDirection.SLICE,
                frequency = 5,
                confidence = 2.0f,
                lastOccurrence = 1700000000L
            )
        }
    }

    @Test
    fun `MissPattern rejects invalid frequency`() {
        assertThrows(IllegalArgumentException::class.java) {
            MissPattern(
                direction = MissDirection.SLICE,
                frequency = 0,
                confidence = 0.8f,
                lastOccurrence = 1700000000L
            )
        }
    }

    @Test
    fun `ExtractedEntities clamps invalid fatigue to valid range`() {
        // Fatigue below range is clamped to 1
        val tooLow = ExtractedEntities(fatigue = 0)
        assertEquals(1, tooLow.fatigue)

        // Fatigue above range is clamped to 10
        val tooHigh = ExtractedEntities(fatigue = 11)
        assertEquals(10, tooHigh.fatigue)

        // Valid fatigue is preserved
        val valid = ExtractedEntities(fatigue = 5)
        assertEquals(5, valid.fatigue)
    }

    @Test
    fun `ExtractedEntities rejects invalid yardage by setting to null`() {
        // Negative yardage is rejected
        val negative = ExtractedEntities(yardage = -10)
        assertEquals(null, negative.yardage)

        // Zero yardage is rejected
        val zero = ExtractedEntities(yardage = 0)
        assertEquals(null, zero.yardage)

        // Valid yardage is preserved
        val valid = ExtractedEntities(yardage = 150)
        assertEquals(150, valid.yardage)
    }

    @Test
    fun `ExtractedEntities rejects invalid hole number by setting to null`() {
        // Hole 0 is rejected
        val tooLow = ExtractedEntities(holeNumber = 0)
        assertEquals(null, tooLow.holeNumber)

        // Hole 19 is rejected
        val tooHigh = ExtractedEntities(holeNumber = 19)
        assertEquals(null, tooHigh.holeNumber)

        // Valid hole numbers are preserved
        val valid = ExtractedEntities(holeNumber = 9)
        assertEquals(9, valid.holeNumber)
    }

    @Test
    fun `SessionContext rejects invalid current hole`() {
        assertThrows(IllegalArgumentException::class.java) {
            SessionContext(currentHole = 0)
        }

        assertThrows(IllegalArgumentException::class.java) {
            SessionContext(currentHole = 19)
        }
    }

    @Test
    fun `Round rejects invalid hole numbers in scores`() {
        assertThrows(IllegalArgumentException::class.java) {
            Round(
                id = "test",
                startTime = 1700000000L,
                scores = mapOf(0 to 4)
            )
        }

        assertThrows(IllegalArgumentException::class.java) {
            Round(
                id = "test",
                startTime = 1700000000L,
                scores = mapOf(19 to 4)
            )
        }
    }

    @Test
    fun `ExtractedEntities accepts valid values`() {
        val entities = ExtractedEntities(
            fatigue = 5,
            yardage = 150,
            holeNumber = 9
        )

        assertEquals(5, entities.fatigue)
        assertEquals(150, entities.yardage)
        assertEquals(9, entities.holeNumber)
    }

    // endregion

    // region Confidence Thresholds Tests

    @Test
    fun `ConfidenceThresholds has correct values`() {
        assertEquals(0.75f, ConfidenceThresholds.ROUTE_THRESHOLD, 0.001f)
        assertEquals(0.50f, ConfidenceThresholds.CONFIRM_THRESHOLD, 0.001f)
        assertEquals(0.50f, ConfidenceThresholds.CLARIFY_THRESHOLD, 0.001f)
    }

    @Test
    fun `ConfidenceThresholds ROUTE_THRESHOLD is higher than CONFIRM_THRESHOLD`() {
        assertTrue(ConfidenceThresholds.ROUTE_THRESHOLD > ConfidenceThresholds.CONFIRM_THRESHOLD)
    }

    // endregion

    // region Default Values Tests

    @Test
    fun `Club uses default null values for optional fields`() {
        val club = Club(name = "Driver", type = ClubType.DRIVER)
        assertEquals(null, club.loft)
        assertEquals(null, club.distance)
    }

    @Test
    fun `ExtractedEntities has all fields optional`() {
        val entities = ExtractedEntities()
        assertEquals(null, entities.club)
        assertEquals(null, entities.yardage)
        assertEquals(null, entities.lie)
        assertEquals(null, entities.wind)
        assertEquals(null, entities.fatigue)
        assertEquals(null, entities.pain)
        assertEquals(null, entities.scoreContext)
        assertEquals(null, entities.holeNumber)
    }

    @Test
    fun `PressureContext defaults to not tagged and not inferred`() {
        val context = PressureContext()
        assertEquals(false, context.isUserTagged)
        assertEquals(false, context.isInferred)
        assertEquals(null, context.scoringContext)
    }

    @Test
    fun `SessionContext defaults to empty state`() {
        val context = SessionContext()
        assertEquals(null, context.currentRound)
        assertEquals(null, context.currentHole)
        assertEquals(null, context.lastShot)
        assertEquals(null, context.lastRecommendation)
        assertTrue(context.conversationHistory.isEmpty())
    }

    @Test
    fun `Round defaults to empty scores`() {
        val round = Round(id = "test", startTime = 1700000000L)
        assertTrue(round.scores.isEmpty())
        assertEquals(null, round.courseName)
    }

    @Test
    fun `RoutingTarget defaults to empty parameters`() {
        val target = RoutingTarget(module = Module.CADDY, screen = "home")
        assertTrue(target.parameters.isEmpty())
    }

    // endregion
}
