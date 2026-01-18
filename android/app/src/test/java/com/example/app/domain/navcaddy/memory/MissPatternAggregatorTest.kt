package caddypro.domain.navcaddy.memory

import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.ClubType
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.MissPattern
import caddypro.domain.navcaddy.models.PressureContext
import caddypro.domain.navcaddy.models.Shot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for MissPatternAggregator.
 *
 * Tests pattern aggregation, filtering, and decay application.
 */
class MissPatternAggregatorTest {

    private lateinit var repository: NavCaddyRepository
    private lateinit var decayCalculator: PatternDecayCalculator
    private lateinit var aggregator: MissPatternAggregator

    private val currentTime = System.currentTimeMillis()
    private val oneDayMillis = 24 * 60 * 60 * 1000L

    private val testClub = Club(
        id = "7i",
        name = "7-iron",
        type = ClubType.IRON,
        distance = 150
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        decayCalculator = mockk()

        // Default decay behavior: no decay for recent shots
        every { decayCalculator.isWithinRetentionWindow(any(), any(), any()) } returns true
        every { decayCalculator.calculateDecayedConfidence(any(), any(), any()) } answers {
            firstArg() as Float // Return base confidence unchanged
        }

        aggregator = MissPatternAggregator(repository, decayCalculator)
    }

    // ========================================================================
    // Pattern Aggregation Tests
    // ========================================================================

    @Test
    fun `aggregatePatterns returns empty list for insufficient shots`() = runTest {
        val shots = listOf(createTestShot(), createTestShot())

        val patterns = aggregator.aggregatePatterns(shots)

        assertTrue(patterns.isEmpty())
    }

    @Test
    fun `aggregatePatterns returns empty list for all straight shots`() = runTest {
        val shots = (1..10).map {
            createTestShot(missDirection = MissDirection.STRAIGHT)
        }

        val patterns = aggregator.aggregatePatterns(shots)

        assertTrue(patterns.isEmpty())
    }

    @Test
    fun `aggregatePatterns creates pattern for dominant miss direction`() = runTest {
        // 7 slices out of 10 shots = 70% frequency
        val shots = listOf(
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.HOOK),
            createTestShot(missDirection = MissDirection.STRAIGHT),
            createTestShot(missDirection = MissDirection.STRAIGHT)
        )

        val patterns = aggregator.aggregatePatterns(shots)

        assertEquals(1, patterns.size)
        val pattern = patterns.first()
        assertEquals(MissDirection.SLICE, pattern.direction)
        assertEquals(7, pattern.frequency)
        assertTrue(pattern.confidence > 0.6f) // 70% frequency should give high confidence
    }

    @Test
    fun `aggregatePatterns creates multiple patterns for mixed misses`() = runTest {
        // 4 slices (40%) and 3 hooks (30%) - both above threshold
        val shots = listOf(
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.HOOK),
            createTestShot(missDirection = MissDirection.HOOK),
            createTestShot(missDirection = MissDirection.HOOK),
            createTestShot(missDirection = MissDirection.STRAIGHT),
            createTestShot(missDirection = MissDirection.STRAIGHT),
            createTestShot(missDirection = MissDirection.STRAIGHT)
        )

        val patterns = aggregator.aggregatePatterns(shots)

        assertEquals(2, patterns.size)
        assertTrue(patterns.any { it.direction == MissDirection.SLICE && it.frequency == 4 })
        assertTrue(patterns.any { it.direction == MissDirection.HOOK && it.frequency == 3 })
    }

    @Test
    fun `aggregatePatterns filters out patterns below threshold`() = runTest {
        // 2 slices out of 10 = 20%, below 30% threshold
        val shots = listOf(
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.STRAIGHT),
            createTestShot(missDirection = MissDirection.STRAIGHT),
            createTestShot(missDirection = MissDirection.STRAIGHT),
            createTestShot(missDirection = MissDirection.STRAIGHT),
            createTestShot(missDirection = MissDirection.STRAIGHT),
            createTestShot(missDirection = MissDirection.STRAIGHT),
            createTestShot(missDirection = MissDirection.STRAIGHT),
            createTestShot(missDirection = MissDirection.STRAIGHT)
        )

        val patterns = aggregator.aggregatePatterns(shots)

        assertTrue(patterns.isEmpty())
    }

    @Test
    fun `aggregatePatterns orders patterns by confidence descending`() = runTest {
        // 5 slices (50%) and 4 hooks (40%)
        val shots = listOf(
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.SLICE),
            createTestShot(missDirection = MissDirection.HOOK),
            createTestShot(missDirection = MissDirection.HOOK),
            createTestShot(missDirection = MissDirection.HOOK),
            createTestShot(missDirection = MissDirection.HOOK),
            createTestShot(missDirection = MissDirection.STRAIGHT)
        )

        val patterns = aggregator.aggregatePatterns(shots)

        assertEquals(2, patterns.size)
        assertEquals(MissDirection.SLICE, patterns[0].direction)
        assertEquals(MissDirection.HOOK, patterns[1].direction)
        assertTrue(patterns[0].confidence > patterns[1].confidence)
    }

    @Test
    fun `aggregatePatterns applies decay to confidence`() = runTest {
        val shots = (1..10).map {
            createTestShot(missDirection = MissDirection.SLICE)
        }

        // Mock decay to reduce confidence by half
        every { decayCalculator.calculateDecayedConfidence(any(), any(), any()) } answers {
            val baseConfidence = firstArg<Float>()
            baseConfidence * 0.5f
        }

        val patterns = aggregator.aggregatePatterns(shots)

        assertEquals(1, patterns.size)
        assertTrue(patterns.first().confidence < 0.6f) // Should be reduced by decay
    }

    // ========================================================================
    // Club-Specific Pattern Tests
    // ========================================================================

    @Test
    fun `getPatternsForClub filters shots by club ID`() = runTest {
        val shots = (1..5).map {
            createTestShot(clubId = "7i", missDirection = MissDirection.SLICE)
        }
        coEvery { repository.getShotsByClub("7i") } returns flowOf(shots)

        val patterns = aggregator.getPatternsForClub("7i")

        assertEquals(1, patterns.size)
        assertNotNull(patterns.first().club)
        assertEquals("7i", patterns.first().club?.id)
    }

    @Test
    fun `getPatternsForClub respects rolling window`() = runTest {
        val recentShot = createTestShot(
            timestamp = currentTime - (10 * oneDayMillis),
            missDirection = MissDirection.SLICE
        )
        val oldShot = createTestShot(
            timestamp = currentTime - (40 * oneDayMillis),
            missDirection = MissDirection.SLICE
        )

        // Mock retention window to exclude shots older than 30 days
        every { decayCalculator.isWithinRetentionWindow(recentShot.timestamp, 30, any()) } returns true
        every { decayCalculator.isWithinRetentionWindow(oldShot.timestamp, 30, any()) } returns false

        coEvery { repository.getShotsByClub("7i") } returns flowOf(listOf(recentShot, oldShot))

        val patterns = aggregator.getPatternsForClub("7i", days = 30)

        // Should not create pattern as only 1 recent shot (need 3 minimum)
        assertTrue(patterns.isEmpty())
    }

    // ========================================================================
    // Pressure Pattern Tests
    // ========================================================================

    @Test
    fun `getPatternsForPressure filters pressure shots`() = runTest {
        val pressureShots = (1..5).map {
            createTestShot(
                pressureContext = PressureContext(isUserTagged = true),
                missDirection = MissDirection.HOOK
            )
        }
        coEvery { repository.getShotsWithPressure() } returns flowOf(pressureShots)

        val patterns = aggregator.getPatternsForPressure()

        assertEquals(1, patterns.size)
        assertNotNull(patterns.first().pressureContext)
        assertTrue(patterns.first().pressureContext?.hasPressure == true)
    }

    @Test
    fun `getPatternsForClubAndPressure filters by both club and pressure`() = runTest {
        val shots = (1..5).map {
            createTestShot(
                clubId = "7i",
                pressureContext = PressureContext(isUserTagged = true),
                missDirection = MissDirection.SLICE
            )
        }
        coEvery { repository.getShotsByClub("7i") } returns flowOf(shots)

        val patterns = aggregator.getPatternsForClubAndPressure("7i")

        assertEquals(1, patterns.size)
        val pattern = patterns.first()
        assertNotNull(pattern.club)
        assertEquals("7i", pattern.club?.id)
        assertNotNull(pattern.pressureContext)
        assertTrue(pattern.pressureContext?.hasPressure == true)
    }

    // ========================================================================
    // Persisted Pattern Tests
    // ========================================================================

    @Test
    fun `getPatternsWithDecay applies decay to persisted patterns`() = runTest {
        val pattern = MissPattern(
            id = UUID.randomUUID().toString(),
            direction = MissDirection.SLICE,
            frequency = 5,
            confidence = 0.8f,
            lastOccurrence = currentTime - (14 * oneDayMillis) // One half-life ago
        )

        // Mock decay to halve confidence
        every { decayCalculator.calculateDecayedConfidence(0.8f, pattern.lastOccurrence, any()) } returns 0.4f

        coEvery { repository.getMissPatterns() } returns flowOf(listOf(pattern))

        val result = aggregator.getPatternsWithDecay()

        coVerify { repository.getMissPatterns() }
    }

    @Test
    fun `getPatternsWithDecay filters out nearly-decayed patterns`() = runTest {
        val pattern = MissPattern(
            id = UUID.randomUUID().toString(),
            direction = MissDirection.SLICE,
            frequency = 5,
            confidence = 0.8f,
            lastOccurrence = currentTime - (90 * oneDayMillis) // Very old
        )

        // Mock decay to near-zero
        every { decayCalculator.calculateDecayedConfidence(0.8f, pattern.lastOccurrence, any()) } returns 0.005f

        coEvery { repository.getMissPatterns() } returns flowOf(listOf(pattern))

        val result = aggregator.getPatternsWithDecay()

        coVerify { repository.getMissPatterns() }
    }

    // ========================================================================
    // Pattern Persistence Tests
    // ========================================================================

    @Test
    fun `savePatterns persists all patterns`() = runTest {
        val patterns = listOf(
            createTestPattern(MissDirection.SLICE),
            createTestPattern(MissDirection.HOOK)
        )

        aggregator.savePatterns(patterns)

        coVerify(exactly = 2) { repository.updatePattern(any()) }
    }

    @Test
    fun `clearStalePatterns delegates to repository`() = runTest {
        coEvery { repository.deleteStalePatterns() } returns 5

        val deleted = aggregator.clearStalePatterns()

        assertEquals(5, deleted)
        coVerify { repository.deleteStalePatterns() }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private fun createTestShot(
        id: String = UUID.randomUUID().toString(),
        clubId: String = "7i",
        missDirection: MissDirection = MissDirection.SLICE,
        timestamp: Long = currentTime,
        pressureContext: PressureContext = PressureContext()
    ): Shot {
        return Shot(
            id = id,
            timestamp = timestamp,
            club = testClub.copy(id = clubId),
            missDirection = missDirection,
            lie = Lie.FAIRWAY,
            pressureContext = pressureContext,
            holeNumber = null,
            notes = null
        )
    }

    private fun createTestPattern(
        direction: MissDirection,
        confidence: Float = 0.7f
    ): MissPattern {
        return MissPattern(
            id = UUID.randomUUID().toString(),
            direction = direction,
            frequency = 5,
            confidence = confidence,
            lastOccurrence = currentTime
        )
    }
}
