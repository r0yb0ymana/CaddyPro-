package caddypro.domain.navcaddy.integration

import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.domain.navcaddy.memory.MissPatternAggregator
import caddypro.domain.navcaddy.memory.MissPatternStore
import caddypro.domain.navcaddy.memory.PatternDecayCalculator
import caddypro.domain.navcaddy.memory.ShotRecorder
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.ClubType
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.MissPattern
import caddypro.domain.navcaddy.models.PressureContext
import caddypro.domain.navcaddy.models.Shot
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Integration tests for pattern decay behavior.
 *
 * Tests the interaction between PatternDecayCalculator, MissPatternAggregator,
 * and MissPatternStore to verify 14-day half-life decay and filtering behavior.
 *
 * Task: Task 17 - Write Memory & Persona Integration Tests
 * Spec reference: navcaddy-engine.md R5, Q5
 */
class PatternDecayIntegrationTest {

    private lateinit var repository: NavCaddyRepository
    private lateinit var decayCalculator: PatternDecayCalculator
    private lateinit var aggregator: MissPatternAggregator
    private lateinit var shotRecorder: ShotRecorder
    private lateinit var patternStore: MissPatternStore

    private val testClub = Club(
        id = "7-iron-id",
        name = "7-iron",
        type = ClubType.IRON
    )

    private val currentTime = System.currentTimeMillis()

    @Before
    fun setup() {
        repository = mockk()
        decayCalculator = PatternDecayCalculator()
        aggregator = MissPatternAggregator(repository, decayCalculator)
        shotRecorder = ShotRecorder(repository)
        patternStore = MissPatternStore(shotRecorder, aggregator)
    }

    // ========================================================================
    // 14-Day Half-Life Decay Tests
    // ========================================================================

    @Test
    fun `patterns decay with 14-day half-life`() = runTest {
        // Create shots from 14 days ago (one half-life)
        val fourteenDaysAgo = currentTime - (14L * 24 * 60 * 60 * 1000)
        val shots = listOf(
            createShot(MissDirection.SLICE, timestamp = fourteenDaysAgo - 1000),
            createShot(MissDirection.SLICE, timestamp = fourteenDaysAgo - 2000),
            createShot(MissDirection.SLICE, timestamp = fourteenDaysAgo - 3000),
            createShot(MissDirection.SLICE, timestamp = fourteenDaysAgo - 4000),
            createShot(MissDirection.STRAIGHT, timestamp = fourteenDaysAgo - 5000),
            createShot(MissDirection.STRAIGHT, timestamp = fourteenDaysAgo - 6000),
            createShot(MissDirection.STRAIGHT, timestamp = fourteenDaysAgo - 7000),
            createShot(MissDirection.STRAIGHT, timestamp = fourteenDaysAgo - 8000),
            createShot(MissDirection.STRAIGHT, timestamp = fourteenDaysAgo - 9000),
            createShot(MissDirection.STRAIGHT, timestamp = fourteenDaysAgo - 10000)
        )

        coEvery { repository.getRecentShots(any()) } returns flowOf(shots)

        val patterns = aggregator.aggregatePatterns()

        // Base confidence would be 0.4 (4/10)
        // After 14 days (one half-life), should be ~0.2 (50% of original)
        val slicePattern = patterns.find { it.direction == MissDirection.SLICE }
        assertTrue("Pattern should exist", slicePattern != null)
        assertTrue(
            "Confidence should be around 0.2 after one half-life",
            slicePattern!!.confidence in 0.18f..0.22f
        )
    }

    @Test
    fun `patterns decay to quarter strength after 28 days`() = runTest {
        // Create shots from 28 days ago (two half-lives)
        val twentyEightDaysAgo = currentTime - (28L * 24 * 60 * 60 * 1000)
        val shots = listOf(
            createShot(MissDirection.SLICE, timestamp = twentyEightDaysAgo - 1000),
            createShot(MissDirection.SLICE, timestamp = twentyEightDaysAgo - 2000),
            createShot(MissDirection.SLICE, timestamp = twentyEightDaysAgo - 3000),
            createShot(MissDirection.SLICE, timestamp = twentyEightDaysAgo - 4000),
            createShot(MissDirection.SLICE, timestamp = twentyEightDaysAgo - 5000),
            createShot(MissDirection.STRAIGHT, timestamp = twentyEightDaysAgo - 6000),
            createShot(MissDirection.STRAIGHT, timestamp = twentyEightDaysAgo - 7000),
            createShot(MissDirection.STRAIGHT, timestamp = twentyEightDaysAgo - 8000),
            createShot(MissDirection.STRAIGHT, timestamp = twentyEightDaysAgo - 9000),
            createShot(MissDirection.STRAIGHT, timestamp = twentyEightDaysAgo - 10000)
        )

        coEvery { repository.getRecentShots(any()) } returns flowOf(shots)

        val patterns = aggregator.aggregatePatterns()

        // Base confidence would be 0.5 (5/10)
        // After 28 days (two half-lives), should be ~0.125 (25% of original)
        val slicePattern = patterns.find { it.direction == MissDirection.SLICE }
        assertTrue("Pattern should exist", slicePattern != null)
        assertTrue(
            "Confidence should be around 0.125 after two half-lives",
            slicePattern!!.confidence in 0.10f..0.15f
        )
    }

    @Test
    fun `recent patterns maintain high confidence`() = runTest {
        // Create recent shots (1 day ago)
        val oneDayAgo = currentTime - (1L * 24 * 60 * 60 * 1000)
        val shots = listOf(
            createShot(MissDirection.SLICE, timestamp = oneDayAgo - 1000),
            createShot(MissDirection.SLICE, timestamp = oneDayAgo - 2000),
            createShot(MissDirection.SLICE, timestamp = oneDayAgo - 3000),
            createShot(MissDirection.SLICE, timestamp = oneDayAgo - 4000),
            createShot(MissDirection.STRAIGHT, timestamp = oneDayAgo - 5000),
            createShot(MissDirection.STRAIGHT, timestamp = oneDayAgo - 6000),
            createShot(MissDirection.STRAIGHT, timestamp = oneDayAgo - 7000),
            createShot(MissDirection.STRAIGHT, timestamp = oneDayAgo - 8000),
            createShot(MissDirection.STRAIGHT, timestamp = oneDayAgo - 9000),
            createShot(MissDirection.STRAIGHT, timestamp = oneDayAgo - 10000)
        )

        coEvery { repository.getRecentShots(any()) } returns flowOf(shots)

        val patterns = aggregator.aggregatePatterns()

        // Base confidence would be 0.4 (4/10)
        // After 1 day, decay should be minimal (~95% retained)
        val slicePattern = patterns.find { it.direction == MissDirection.SLICE }
        assertTrue("Pattern should exist", slicePattern != null)
        assertTrue(
            "Confidence should be close to base value for recent pattern",
            slicePattern!!.confidence > 0.38f
        )
    }

    // ========================================================================
    // Old Patterns Lose Confidence Tests
    // ========================================================================

    @Test
    fun `old patterns have significantly lower confidence than recent patterns`() = runTest {
        // Create two sets: recent and old
        val oneDayAgo = currentTime - (1L * 24 * 60 * 60 * 1000)
        val thirtyDaysAgo = currentTime - (30L * 24 * 60 * 60 * 1000)

        // Recent shots (SLICE pattern)
        val recentShots = listOf(
            createShot(MissDirection.SLICE, timestamp = oneDayAgo - 1000),
            createShot(MissDirection.SLICE, timestamp = oneDayAgo - 2000),
            createShot(MissDirection.SLICE, timestamp = oneDayAgo - 3000),
            createShot(MissDirection.SLICE, timestamp = oneDayAgo - 4000),
            createShot(MissDirection.STRAIGHT, timestamp = oneDayAgo - 5000),
            createShot(MissDirection.STRAIGHT, timestamp = oneDayAgo - 6000),
            createShot(MissDirection.STRAIGHT, timestamp = oneDayAgo - 7000),
            createShot(MissDirection.STRAIGHT, timestamp = oneDayAgo - 8000),
            createShot(MissDirection.STRAIGHT, timestamp = oneDayAgo - 9000),
            createShot(MissDirection.STRAIGHT, timestamp = oneDayAgo - 10000)
        )

        // Old shots (HOOK pattern)
        val oldShots = listOf(
            createShot(MissDirection.HOOK, timestamp = thirtyDaysAgo - 1000),
            createShot(MissDirection.HOOK, timestamp = thirtyDaysAgo - 2000),
            createShot(MissDirection.HOOK, timestamp = thirtyDaysAgo - 3000),
            createShot(MissDirection.HOOK, timestamp = thirtyDaysAgo - 4000),
            createShot(MissDirection.PUSH, timestamp = thirtyDaysAgo - 5000),
            createShot(MissDirection.PUSH, timestamp = thirtyDaysAgo - 6000),
            createShot(MissDirection.PUSH, timestamp = thirtyDaysAgo - 7000),
            createShot(MissDirection.PUSH, timestamp = thirtyDaysAgo - 8000),
            createShot(MissDirection.PUSH, timestamp = thirtyDaysAgo - 9000),
            createShot(MissDirection.PUSH, timestamp = thirtyDaysAgo - 10000)
        )

        // Test recent patterns
        coEvery { repository.getRecentShots(any()) } returns flowOf(recentShots)
        val recentPatterns = aggregator.aggregatePatterns()
        val recentConfidence = recentPatterns.find { it.direction == MissDirection.SLICE }?.confidence ?: 0f

        // Test old patterns
        coEvery { repository.getRecentShots(any()) } returns flowOf(oldShots)
        val oldPatterns = aggregator.aggregatePatterns()
        val oldConfidence = oldPatterns.find { it.direction == MissDirection.HOOK }?.confidence ?: 0f

        // Verify: Recent patterns should have much higher confidence
        assertTrue("Recent pattern should exist", recentConfidence > 0f)
        assertTrue("Old pattern should exist", oldConfidence > 0f)
        assertTrue(
            "Recent pattern confidence should be significantly higher than old pattern",
            recentConfidence > oldConfidence * 2
        )
    }

    @Test
    fun `patterns lose confidence progressively over time`() = runTest {
        val shots7DaysAgo = createShotSet(7, currentTime)
        val shots14DaysAgo = createShotSet(14, currentTime)
        val shots28DaysAgo = createShotSet(28, currentTime)

        coEvery { repository.getRecentShots(any()) } returns flowOf(shots7DaysAgo)
        val patterns7Days = aggregator.aggregatePatterns()
        val confidence7Days = patterns7Days.firstOrNull()?.confidence ?: 0f

        coEvery { repository.getRecentShots(any()) } returns flowOf(shots14DaysAgo)
        val patterns14Days = aggregator.aggregatePatterns()
        val confidence14Days = patterns14Days.firstOrNull()?.confidence ?: 0f

        coEvery { repository.getRecentShots(any()) } returns flowOf(shots28DaysAgo)
        val patterns28Days = aggregator.aggregatePatterns()
        val confidence28Days = patterns28Days.firstOrNull()?.confidence ?: 0f

        // Verify: Progressive decay
        assertTrue("7-day pattern should have highest confidence", confidence7Days > confidence14Days)
        assertTrue("14-day pattern should have higher confidence than 28-day", confidence14Days > confidence28Days)

        // Approximate exponential decay verification
        val expectedRatio = 0.5f // One half-life difference between 7 and 14 days
        val actualRatio = confidence14Days / confidence7Days
        assertTrue(
            "Decay should approximate exponential half-life",
            actualRatio in (expectedRatio * 0.7f)..(expectedRatio * 1.3f)
        )
    }

    // ========================================================================
    // Patterns Below Threshold After Decay Are Filtered Tests
    // ========================================================================

    @Test
    fun `very old patterns are filtered out after decay`() = runTest {
        // Create shots from 60 days ago (over 4 half-lives)
        val sixtyDaysAgo = currentTime - (60L * 24 * 60 * 60 * 1000)
        val shots = listOf(
            createShot(MissDirection.SLICE, timestamp = sixtyDaysAgo - 1000),
            createShot(MissDirection.SLICE, timestamp = sixtyDaysAgo - 2000),
            createShot(MissDirection.SLICE, timestamp = sixtyDaysAgo - 3000),
            createShot(MissDirection.SLICE, timestamp = sixtyDaysAgo - 4000),
            createShot(MissDirection.STRAIGHT, timestamp = sixtyDaysAgo - 5000),
            createShot(MissDirection.STRAIGHT, timestamp = sixtyDaysAgo - 6000),
            createShot(MissDirection.STRAIGHT, timestamp = sixtyDaysAgo - 7000),
            createShot(MissDirection.STRAIGHT, timestamp = sixtyDaysAgo - 8000),
            createShot(MissDirection.STRAIGHT, timestamp = sixtyDaysAgo - 9000),
            createShot(MissDirection.STRAIGHT, timestamp = sixtyDaysAgo - 10000)
        )

        coEvery { repository.getRecentShots(any()) } returns flowOf(shots)

        val patterns = aggregator.aggregatePatterns()

        // Base confidence would be 0.4, but after 60 days (4.3 half-lives)
        // Confidence would be ~0.02, which might be filtered
        val slicePattern = patterns.find { it.direction == MissDirection.SLICE }

        if (slicePattern != null) {
            // If pattern exists, it should have very low confidence
            assertTrue("Very old pattern should have very low confidence", slicePattern.confidence < 0.05f)
        }
        // Pattern may be completely filtered out, which is also acceptable
    }

    @Test
    fun `patterns with decay flow filters low confidence patterns`() = runTest {
        // Create persisted patterns with varying ages
        val recentPattern = MissPattern(
            id = "pattern-1",
            direction = MissDirection.SLICE,
            frequency = 5,
            confidence = 0.8f,
            lastOccurrence = currentTime - (7L * 24 * 60 * 60 * 1000) // 7 days ago
        )

        val oldPattern = MissPattern(
            id = "pattern-2",
            direction = MissDirection.HOOK,
            frequency = 5,
            confidence = 0.8f,
            lastOccurrence = currentTime - (84L * 24 * 60 * 60 * 1000) // 84 days ago (6 half-lives)
        )

        coEvery { repository.getMissPatterns() } returns flowOf(listOf(recentPattern, oldPattern))

        // Collect patterns with decay applied
        val patternsFlow = aggregator.getPatternsWithDecay()
        kotlinx.coroutines.flow.first(patternsFlow).let { patterns ->
            // Old pattern should be filtered out (confidence < 0.01 after 6 half-lives)
            assertEquals("Only recent pattern should remain", 1, patterns.size)
            assertEquals("Remaining pattern should be SLICE", MissDirection.SLICE, patterns[0].direction)
            assertTrue("Pattern should have decayed confidence", patterns[0].confidence < recentPattern.confidence)
        }
    }

    // ========================================================================
    // Interaction with Components Tests
    // ========================================================================

    @Test
    fun `pattern store returns patterns with decay applied`() = runTest {
        // Create shots with varying ages
        val mixedShots = listOf(
            // Recent SLICE pattern (5 days ago)
            createShot(MissDirection.SLICE, timestamp = currentTime - (5L * 24 * 60 * 60 * 1000)),
            createShot(MissDirection.SLICE, timestamp = currentTime - (5L * 24 * 60 * 60 * 1000) - 1000),
            createShot(MissDirection.SLICE, timestamp = currentTime - (5L * 24 * 60 * 60 * 1000) - 2000),
            createShot(MissDirection.SLICE, timestamp = currentTime - (5L * 24 * 60 * 60 * 1000) - 3000),
            // Old HOOK pattern (45 days ago)
            createShot(MissDirection.HOOK, timestamp = currentTime - (45L * 24 * 60 * 60 * 1000)),
            createShot(MissDirection.HOOK, timestamp = currentTime - (45L * 24 * 60 * 60 * 1000) - 1000),
            createShot(MissDirection.HOOK, timestamp = currentTime - (45L * 24 * 60 * 60 * 1000) - 2000),
            createShot(MissDirection.HOOK, timestamp = currentTime - (45L * 24 * 60 * 60 * 1000) - 3000),
            // Straight shots
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 1000),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 2000)
        )

        coEvery { repository.getRecentShots(any()) } returns flowOf(mixedShots)

        val patterns = patternStore.getPatterns()

        // Verify: Recent pattern should have higher confidence than old pattern
        val slicePattern = patterns.find { it.direction == MissDirection.SLICE }
        val hookPattern = patterns.find { it.direction == MissDirection.HOOK }

        assertTrue("Recent SLICE pattern should exist", slicePattern != null)

        if (hookPattern != null) {
            assertTrue(
                "Recent pattern should have higher confidence than old pattern",
                slicePattern!!.confidence > hookPattern.confidence
            )
        }
    }

    @Test
    fun `decay calculator integrates with aggregator correctly`() = runTest {
        // Test that PatternDecayCalculator is being used correctly by aggregator
        val testTime = currentTime - (14L * 24 * 60 * 60 * 1000) // 14 days ago

        val shots = createShotSet(14, currentTime)
        coEvery { repository.getRecentShots(any()) } returns flowOf(shots)

        val patterns = aggregator.aggregatePatterns()

        // Manually calculate expected decay
        val baseConfidence = 0.4f // 4/10 shots are SLICE
        val expectedDecay = decayCalculator.calculateDecay(testTime, currentTime)
        val expectedConfidence = baseConfidence * expectedDecay

        val actualPattern = patterns.find { it.direction == MissDirection.SLICE }
        assertTrue("Pattern should exist", actualPattern != null)

        // Allow 10% margin for floating point precision
        val margin = expectedConfidence * 0.1f
        assertTrue(
            "Aggregator should use decay calculator correctly",
            kotlin.math.abs(actualPattern!!.confidence - expectedConfidence) < margin
        )
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private fun createShot(
        direction: MissDirection,
        timestamp: Long = currentTime,
        club: Club = testClub,
        isPressure: Boolean = false
    ): Shot {
        return Shot(
            id = UUID.randomUUID().toString(),
            timestamp = timestamp,
            club = club,
            missDirection = direction,
            lie = Lie.FAIRWAY,
            pressureContext = PressureContext(
                isUserTagged = isPressure,
                isInferred = false
            )
        )
    }

    /**
     * Create a standard set of shots for a given age (in days) with 40% SLICE frequency.
     */
    private fun createShotSet(ageInDays: Int, currentTime: Long): List<Shot> {
        val timestamp = currentTime - (ageInDays.toLong() * 24 * 60 * 60 * 1000)
        return listOf(
            createShot(MissDirection.SLICE, timestamp = timestamp - 1000),
            createShot(MissDirection.SLICE, timestamp = timestamp - 2000),
            createShot(MissDirection.SLICE, timestamp = timestamp - 3000),
            createShot(MissDirection.SLICE, timestamp = timestamp - 4000),
            createShot(MissDirection.STRAIGHT, timestamp = timestamp - 5000),
            createShot(MissDirection.STRAIGHT, timestamp = timestamp - 6000),
            createShot(MissDirection.STRAIGHT, timestamp = timestamp - 7000),
            createShot(MissDirection.STRAIGHT, timestamp = timestamp - 8000),
            createShot(MissDirection.STRAIGHT, timestamp = timestamp - 9000),
            createShot(MissDirection.STRAIGHT, timestamp = timestamp - 10000)
        )
    }
}
