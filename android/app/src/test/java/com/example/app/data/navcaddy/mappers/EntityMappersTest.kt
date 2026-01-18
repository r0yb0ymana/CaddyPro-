package caddypro.data.navcaddy.mappers

import caddypro.data.navcaddy.entities.ConversationTurnEntity
import caddypro.data.navcaddy.entities.MissPatternEntity
import caddypro.data.navcaddy.entities.SessionEntity
import caddypro.data.navcaddy.entities.ShotEntity
import caddypro.data.navcaddy.toDomain
import caddypro.data.navcaddy.toEntity
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.ClubType
import caddypro.domain.navcaddy.models.ConversationTurn
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.MissPattern
import caddypro.domain.navcaddy.models.PressureContext
import caddypro.domain.navcaddy.models.Role
import caddypro.domain.navcaddy.models.SessionContext
import caddypro.domain.navcaddy.models.Shot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for entity-to-domain mappers.
 *
 * Verifies bidirectional conversion between Room entities and domain models.
 */
class EntityMappersTest {

    // ========================================================================
    // Shot Mapping Tests
    // ========================================================================

    @Test
    fun `shotEntity toDomain maps all fields correctly`() {
        // Given
        val entity = ShotEntity(
            id = "shot-123",
            timestamp = 1234567890L,
            clubId = "club-7i",
            clubName = "7-iron",
            clubType = "IRON",
            missDirection = "SLICE",
            lie = "FAIRWAY",
            isUserTaggedPressure = true,
            isInferredPressure = false,
            scoringContext = "leading by 1",
            holeNumber = 14,
            notes = "Felt good"
        )

        // When
        val shot = entity.toDomain()

        // Then
        assertEquals("shot-123", shot.id)
        assertEquals(1234567890L, shot.timestamp)
        assertEquals("club-7i", shot.club.id)
        assertEquals("7-iron", shot.club.name)
        assertEquals(ClubType.IRON, shot.club.type)
        assertEquals(MissDirection.SLICE, shot.missDirection)
        assertEquals(Lie.FAIRWAY, shot.lie)
        assertEquals(true, shot.pressureContext.isUserTagged)
        assertEquals(false, shot.pressureContext.isInferred)
        assertEquals("leading by 1", shot.pressureContext.scoringContext)
        assertEquals(14, shot.holeNumber)
        assertEquals("Felt good", shot.notes)
    }

    @Test
    fun `shot toEntity maps all fields correctly`() {
        // Given
        val shot = Shot(
            id = "shot-456",
            timestamp = 9876543210L,
            club = Club(id = "club-pw", name = "PW", type = ClubType.WEDGE),
            missDirection = MissDirection.FAT,
            lie = Lie.ROUGH,
            pressureContext = PressureContext(
                isUserTagged = false,
                isInferred = true,
                scoringContext = "tournament mode"
            ),
            holeNumber = 8,
            notes = "Need to work on tempo"
        )

        // When
        val entity = shot.toEntity()

        // Then
        assertEquals("shot-456", entity.id)
        assertEquals(9876543210L, entity.timestamp)
        assertEquals("club-pw", entity.clubId)
        assertEquals("PW", entity.clubName)
        assertEquals("WEDGE", entity.clubType)
        assertEquals("FAT", entity.missDirection)
        assertEquals("ROUGH", entity.lie)
        assertEquals(false, entity.isUserTaggedPressure)
        assertEquals(true, entity.isInferredPressure)
        assertEquals("tournament mode", entity.scoringContext)
        assertEquals(8, entity.holeNumber)
        assertEquals("Need to work on tempo", entity.notes)
    }

    @Test
    fun `shot roundtrip conversion preserves data`() {
        // Given
        val originalShot = Shot(
            id = "shot-roundtrip",
            timestamp = 1111111111L,
            club = Club(id = "club-driver", name = "Driver", type = ClubType.DRIVER),
            missDirection = null,
            lie = Lie.TEE,
            pressureContext = PressureContext(),
            holeNumber = null,
            notes = null
        )

        // When
        val convertedShot = originalShot.toEntity().toDomain()

        // Then
        assertEquals(originalShot.id, convertedShot.id)
        assertEquals(originalShot.timestamp, convertedShot.timestamp)
        assertEquals(originalShot.club.id, convertedShot.club.id)
        assertEquals(originalShot.missDirection, convertedShot.missDirection)
        assertEquals(originalShot.lie, convertedShot.lie)
    }

    // ========================================================================
    // MissPattern Mapping Tests
    // ========================================================================

    @Test
    fun `missPatternEntity toDomain maps all fields correctly`() {
        // Given
        val entity = MissPatternEntity(
            id = "pattern-123",
            direction = "HOOK",
            clubId = "club-3w",
            clubName = "3-wood",
            clubType = "WOOD",
            frequency = 15,
            confidence = 0.85f,
            isUserTaggedPressure = true,
            isInferredPressure = false,
            scoringContext = "tight fairway",
            lastOccurrence = 1234567890L
        )

        // When
        val pattern = entity.toDomain()

        // Then
        assertEquals("pattern-123", pattern.id)
        assertEquals(MissDirection.HOOK, pattern.direction)
        assertNotNull(pattern.club)
        assertEquals("club-3w", pattern.club?.id)
        assertEquals("3-wood", pattern.club?.name)
        assertEquals(ClubType.WOOD, pattern.club?.type)
        assertEquals(15, pattern.frequency)
        assertEquals(0.85f, pattern.confidence, 0.001f)
        assertNotNull(pattern.pressureContext)
        assertEquals(true, pattern.pressureContext?.isUserTagged)
        assertEquals(1234567890L, pattern.lastOccurrence)
    }

    @Test
    fun `missPatternEntity without club maps to null club`() {
        // Given
        val entity = MissPatternEntity(
            id = "pattern-456",
            direction = "PUSH",
            clubId = null,
            clubName = null,
            clubType = null,
            frequency = 8,
            confidence = 0.65f,
            isUserTaggedPressure = null,
            isInferredPressure = null,
            scoringContext = null,
            lastOccurrence = 9876543210L
        )

        // When
        val pattern = entity.toDomain()

        // Then
        assertNull(pattern.club)
        assertNull(pattern.pressureContext)
    }

    @Test
    fun `missPattern toEntity maps all fields correctly`() {
        // Given
        val pattern = MissPattern(
            id = "pattern-789",
            direction = MissDirection.PULL,
            club = Club(id = "club-5i", name = "5-iron", type = ClubType.IRON),
            frequency = 12,
            confidence = 0.72f,
            pressureContext = PressureContext(isUserTagged = false, isInferred = true),
            lastOccurrence = 5555555555L
        )

        // When
        val entity = pattern.toEntity()

        // Then
        assertEquals("pattern-789", entity.id)
        assertEquals("PULL", entity.direction)
        assertEquals("club-5i", entity.clubId)
        assertEquals("5-iron", entity.clubName)
        assertEquals("IRON", entity.clubType)
        assertEquals(12, entity.frequency)
        assertEquals(0.72f, entity.confidence, 0.001f)
        assertEquals(false, entity.isUserTaggedPressure)
        assertEquals(true, entity.isInferredPressure)
        assertEquals(5555555555L, entity.lastOccurrence)
    }

    // ========================================================================
    // ConversationTurn Mapping Tests
    // ========================================================================

    @Test
    fun `conversationTurnEntity toDomain maps correctly`() {
        // Given
        val entity = ConversationTurnEntity(
            id = 1,
            sessionId = "current",
            role = "USER",
            content = "What club should I use?",
            timestamp = 1234567890L
        )

        // When
        val turn = entity.toDomain()

        // Then
        assertEquals(Role.USER, turn.role)
        assertEquals("What club should I use?", turn.content)
        assertEquals(1234567890L, turn.timestamp)
    }

    @Test
    fun `conversationTurn toEntity maps correctly`() {
        // Given
        val turn = ConversationTurn(
            role = Role.ASSISTANT,
            content = "I'd recommend the 7-iron here.",
            timestamp = 9876543210L
        )

        // When
        val entity = turn.toEntity(sessionId = "current")

        // Then
        assertEquals("current", entity.sessionId)
        assertEquals("ASSISTANT", entity.role)
        assertEquals("I'd recommend the 7-iron here.", entity.content)
        assertEquals(9876543210L, entity.timestamp)
    }

    // ========================================================================
    // Session Mapping Tests
    // ========================================================================

    @Test
    fun `sessionEntity toDomain maps correctly`() {
        // Given
        val entity = SessionEntity(
            id = "current",
            roundId = "round-123",
            currentHole = 7,
            lastRecommendation = "Use 8-iron",
            updatedAt = 1234567890L
        )

        val conversationHistory = listOf(
            ConversationTurn(Role.USER, "Help me", 1234567800L),
            ConversationTurn(Role.ASSISTANT, "Sure", 1234567850L)
        )

        // When
        val context = entity.toDomain(conversationHistory)

        // Then
        assertEquals(7, context.currentHole)
        assertEquals("Use 8-iron", context.lastRecommendation)
        assertEquals(2, context.conversationHistory.size)
    }

    @Test
    fun `sessionContext toEntity maps correctly`() {
        // Given
        val context = SessionContext(
            currentRound = null,
            currentHole = 12,
            lastShot = null,
            lastRecommendation = "Play it safe",
            conversationHistory = emptyList()
        )

        // When
        val entity = context.toEntity()

        // Then
        assertEquals("current", entity.id)
        assertNull(entity.roundId)
        assertEquals(12, entity.currentHole)
        assertEquals("Play it safe", entity.lastRecommendation)
    }
}
