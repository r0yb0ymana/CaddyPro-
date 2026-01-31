package caddypro.domain.navcaddy.memory

import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.ClubType
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.MissPattern
import caddypro.domain.navcaddy.models.PressureContext
import caddypro.domain.navcaddy.models.Shot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for MissPatternStore.
 *
 * Tests the facade interface for miss pattern memory system.
 */
class MissPatternStoreTest {

    private lateinit var shotRecorder: ShotRecorder
    private lateinit var patternAggregator: MissPatternAggregator
    private lateinit var store: MissPatternStore

    private val testClub = Club(
        id = "7i",
        name = "7-iron",
        type = ClubType.IRON,
        distance = 150
    )

    @Before
    fun setup() {
        shotRecorder = mockk(relaxed = true)
        patternAggregator = mockk(relaxed = true)
        store = MissPatternStore(shotRecorder, patternAggregator)
    }

    // ========================================================================
    // Shot Recording Tests
    // ========================================================================

    @Test
    fun `recordMiss with full parameters records shot`() = runTest {
        val pressureContext = PressureContext(isUserTagged = true)

        store.recordMiss(
            club = testClub,
            direction = MissDirection.SLICE,
            lie = Lie.FAIRWAY,
            pressure = pressureContext,
            holeNumber = 5,
            notes = "Test miss"
        )

        coVerify {
            shotRecorder.recordShot(
                club = testClub,
                missDirection = MissDirection.SLICE,
                lie = Lie.FAIRWAY,
                pressureContext = pressureContext,
                holeNumber = 5,
                notes = "Test miss"
            )
        }
    }

    @Test
    fun `recordMiss with simplified parameters creates shot`() = runTest {
        store.recordMiss(
            clubId = "7i",
            clubName = "7-iron",
            direction = MissDirection.HOOK,
            lie = Lie.ROUGH,
            isUserTaggedPressure = true
        )

        coVerify {
            shotRecorder.recordShot(
                club = match { it.id == "7i" && it.name == "7-iron" },
                missDirection = MissDirection.HOOK,
                lie = Lie.ROUGH,
                pressureContext = match { it.isUserTagged },
                holeNumber = null,
                notes = null
            )
        }
    }

    @Test
    fun `recordMiss infers club type from name - driver`() = runTest {
        store.recordMiss(
            clubId = "driver",
            clubName = "Driver",
            direction = MissDirection.SLICE,
            lie = Lie.TEE
        )

        coVerify {
            shotRecorder.recordShot(
                club = match { it.type == ClubType.DRIVER },
                missDirection = any(),
                lie = any(),
                pressureContext = any(),
                holeNumber = any(),
                notes = any()
            )
        }
    }

    @Test
    fun `recordMiss infers club type from name - wedge`() = runTest {
        store.recordMiss(
            clubId = "pw",
            clubName = "Pitching Wedge",
            direction = MissDirection.FAT,
            lie = Lie.FAIRWAY
        )

        coVerify {
            shotRecorder.recordShot(
                club = match { it.type == ClubType.WEDGE },
                missDirection = any(),
                lie = any(),
                pressureContext = any(),
                holeNumber = any(),
                notes = any()
            )
        }
    }

    @Test
    fun `recordMiss infers club type from name - putter`() = runTest {
        store.recordMiss(
            clubId = "putter",
            clubName = "Putter",
            direction = MissDirection.PUSH,
            lie = Lie.GREEN
        )

        coVerify {
            shotRecorder.recordShot(
                club = match { it.type == ClubType.PUTTER },
                missDirection = any(),
                lie = any(),
                pressureContext = any(),
                holeNumber = any(),
                notes = any()
            )
        }
    }

    // ========================================================================
    // Pattern Retrieval Tests
    // ========================================================================

    @Test
    fun `getPatterns with no filters returns all patterns`() = runTest {
        val patterns = listOf(createTestPattern())
        coEvery { patternAggregator.aggregatePatterns(any(), any()) } returns patterns

        val result = store.getPatterns()

        assertEquals(patterns, result)
        coVerify { patternAggregator.aggregatePatterns(any(), any()) }
    }

    @Test
    fun `getPatterns with club ID filters by club`() = runTest {
        val patterns = listOf(createTestPattern())
        coEvery { patternAggregator.getPatternsForClub("7i", any(), any()) } returns patterns

        val result = store.getPatterns(clubId = "7i")

        assertEquals(patterns, result)
        coVerify { patternAggregator.getPatternsForClub("7i", any(), any()) }
    }

    @Test
    fun `getPatterns with pressure context filters by pressure`() = runTest {
        val patterns = listOf(createTestPattern())
        val pressureContext = PressureContext(isUserTagged = true)
        coEvery { patternAggregator.getPatternsForPressure(any(), any()) } returns patterns

        val result = store.getPatterns(pressure = pressureContext)

        assertEquals(patterns, result)
        coVerify { patternAggregator.getPatternsForPressure(any(), any()) }
    }

    @Test
    fun `getPatterns with both club and pressure filters by both`() = runTest {
        val patterns = listOf(createTestPattern())
        val pressureContext = PressureContext(isUserTagged = true)
        coEvery { patternAggregator.getPatternsForClubAndPressure("7i", any(), any()) } returns patterns

        val result = store.getPatterns(clubId = "7i", pressure = pressureContext)

        assertEquals(patterns, result)
        coVerify { patternAggregator.getPatternsForClubAndPressure("7i", any(), any()) }
    }

    @Test
    fun `getPatternsForClub delegates to aggregator`() = runTest {
        val patterns = listOf(createTestPattern())
        coEvery { patternAggregator.getPatternsForClub("7i", any(), any()) } returns patterns

        val result = store.getPatternsForClub("7i")

        assertEquals(patterns, result)
        coVerify { patternAggregator.getPatternsForClub("7i", any(), any()) }
    }

    @Test
    fun `getPatternsForPressure delegates to aggregator`() = runTest {
        val patterns = listOf(createTestPattern())
        coEvery { patternAggregator.getPatternsForPressure(any(), any()) } returns patterns

        val result = store.getPatternsForPressure()

        assertEquals(patterns, result)
        coVerify { patternAggregator.getPatternsForPressure(any(), any()) }
    }

    @Test
    fun `getDominantPattern returns highest confidence pattern`() = runTest {
        val patterns = listOf(
            createTestPattern(MissDirection.SLICE, confidence = 0.8f),
            createTestPattern(MissDirection.HOOK, confidence = 0.5f),
            createTestPattern(MissDirection.PUSH, confidence = 0.3f)
        )
        coEvery { patternAggregator.aggregatePatterns(any(), any()) } returns patterns

        val result = store.getDominantPattern()

        assertNotNull(result)
        assertEquals(MissDirection.SLICE, result?.direction)
        assertEquals(0.8f, result?.confidence)
    }

    @Test
    fun `getDominantPattern returns null when no patterns exist`() = runTest {
        coEvery { patternAggregator.aggregatePatterns(any(), any()) } returns emptyList()

        val result = store.getDominantPattern()

        assertEquals(null, result)
    }

    @Test
    fun `getDominantPattern with club ID filters by club`() = runTest {
        val patterns = listOf(createTestPattern(confidence = 0.7f))
        coEvery { patternAggregator.getPatternsForClub("7i", any(), any()) } returns patterns

        val result = store.getDominantPattern(clubId = "7i")

        assertNotNull(result)
        coVerify { patternAggregator.getPatternsForClub("7i", any(), any()) }
    }

    // ========================================================================
    // Shot History Tests
    // ========================================================================

    @Test
    fun `getRecentShots delegates to recorder`() = runTest {
        val shots = listOf(createTestShot())
        coEvery { shotRecorder.getMostRecentShots(10) } returns shots

        val result = store.getRecentShots(10)

        assertEquals(shots, result)
        coVerify { shotRecorder.getMostRecentShots(10) }
    }

    @Test
    fun `getShotsByClub delegates to recorder`() = runTest {
        val shots = listOf(createTestShot())
        coEvery { shotRecorder.getShotsByClub("7i", 5) } returns shots

        val result = store.getShotsByClub("7i", 5)

        assertEquals(shots, result)
        coVerify { shotRecorder.getShotsByClub("7i", 5) }
    }

    // ========================================================================
    // Memory Management Tests
    // ========================================================================

    @Test
    fun `clearHistory clears all memory`() = runTest {
        store.clearHistory()

        coVerify { shotRecorder.clearHistory() }
    }

    @Test
    fun `enforceRetentionPolicy delegates to recorder and aggregator`() = runTest {
        coEvery { shotRecorder.enforceRetentionPolicy() } returns 10
        coEvery { patternAggregator.clearStalePatterns() } returns 3

        val deleted = store.enforceRetentionPolicy()

        assertEquals(13, deleted)
        coVerify { shotRecorder.enforceRetentionPolicy() }
        coVerify { patternAggregator.clearStalePatterns() }
    }

    @Test
    fun `refreshPatterns aggregates and saves patterns`() = runTest {
        val patterns = listOf(
            createTestPattern(MissDirection.SLICE),
            createTestPattern(MissDirection.HOOK)
        )
        coEvery { patternAggregator.aggregatePatterns(any(), any()) } returns patterns

        val count = store.refreshPatterns()

        assertEquals(2, count)
        coVerify { patternAggregator.aggregatePatterns(any(), any()) }
        coVerify { patternAggregator.savePatterns(patterns) }
    }

    // ========================================================================
    // Club Type Inference Tests
    // ========================================================================

    @Test
    fun `club type inference handles fairway woods`() = runTest {
        store.recordMiss(
            clubId = "3w",
            clubName = "3-wood",
            direction = MissDirection.SLICE,
            lie = Lie.FAIRWAY
        )

        coVerify {
            shotRecorder.recordShot(
                club = match { it.type == ClubType.WOOD },
                missDirection = any(),
                lie = any(),
                pressureContext = any(),
                holeNumber = any(),
                notes = any()
            )
        }
    }

    @Test
    fun `club type inference handles hybrids`() = runTest {
        store.recordMiss(
            clubId = "4h",
            clubName = "4-hybrid",
            direction = MissDirection.THIN,
            lie = Lie.ROUGH
        )

        coVerify {
            shotRecorder.recordShot(
                club = match { it.type == ClubType.HYBRID },
                missDirection = any(),
                lie = any(),
                pressureContext = any(),
                holeNumber = any(),
                notes = any()
            )
        }
    }

    @Test
    fun `club type inference handles irons`() = runTest {
        store.recordMiss(
            clubId = "7i",
            clubName = "7-iron",
            direction = MissDirection.PUSH,
            lie = Lie.FAIRWAY
        )

        coVerify {
            shotRecorder.recordShot(
                club = match { it.type == ClubType.IRON },
                missDirection = any(),
                lie = any(),
                pressureContext = any(),
                holeNumber = any(),
                notes = any()
            )
        }
    }

    @Test
    fun `club type inference defaults to iron for unknown`() = runTest {
        store.recordMiss(
            clubId = "unknown",
            clubName = "Mystery Club",
            direction = MissDirection.FAT,
            lie = Lie.FAIRWAY
        )

        coVerify {
            shotRecorder.recordShot(
                club = match { it.type == ClubType.IRON },
                missDirection = any(),
                lie = any(),
                pressureContext = any(),
                holeNumber = any(),
                notes = any()
            )
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private fun createTestShot(
        id: String = UUID.randomUUID().toString(),
        missDirection: MissDirection = MissDirection.SLICE
    ): Shot {
        return Shot(
            id = id,
            timestamp = System.currentTimeMillis(),
            club = testClub,
            missDirection = missDirection,
            lie = Lie.FAIRWAY,
            pressureContext = PressureContext(),
            holeNumber = null,
            notes = null
        )
    }

    private fun createTestPattern(
        direction: MissDirection = MissDirection.SLICE,
        confidence: Float = 0.7f
    ): MissPattern {
        return MissPattern(
            id = UUID.randomUUID().toString(),
            direction = direction,
            frequency = 5,
            confidence = confidence,
            lastOccurrence = System.currentTimeMillis()
        )
    }
}
