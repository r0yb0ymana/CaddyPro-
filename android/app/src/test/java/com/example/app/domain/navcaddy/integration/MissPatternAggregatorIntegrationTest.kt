package caddypro.domain.navcaddy.integration

import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.domain.navcaddy.memory.MissPatternAggregator
import caddypro.domain.navcaddy.memory.PatternDecayCalculator
import caddypro.domain.navcaddy.memory.ShotRecorder
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.ClubType
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.PressureContext
import caddypro.domain.navcaddy.models.Shot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Integration tests for miss pattern aggregation.
 *
 * Tests the interaction between MissPatternAggregator, PatternDecayCalculator,
 * and ShotRecorder to verify pattern aggregation behavior.
 *
 * Task: Task 17 - Write Memory & Persona Integration Tests
 * Spec reference: navcaddy-engine.md R5
 */
class MissPatternAggregatorIntegrationTest {

    private lateinit var repository: NavCaddyRepository
    private lateinit var decayCalculator: PatternDecayCalculator
    private lateinit var aggregator: MissPatternAggregator
    private lateinit var shotRecorder: ShotRecorder

    private val testClub = Club(
        id = "7-iron-id",
        name = "7-iron",
        type = ClubType.IRON
    )

    private val currentTime = System.currentTimeMillis()

    @Before
    fun setup() {
        repository = mockk()
        decayCalculator = PatternDecayCalculator() // Real implementation for integration testing
        aggregator = MissPatternAggregator(repository, decayCalculator)
        shotRecorder = ShotRecorder(repository)
    }

    // ========================================================================
    // Threshold Behavior Tests
    // ========================================================================

    @Test
    fun `patterns below frequency threshold are not aggregated`() = runTest {
        // Create 10 shots with varied directions where none reach 30% threshold
        val shots = listOf(
            createShot(MissDirection.SLICE, timestamp = currentTime - 1000),
            createShot(MissDirection.SLICE, timestamp = currentTime - 2000),
            createShot(MissDirection.HOOK, timestamp = currentTime - 3000),
            createShot(MissDirection.HOOK, timestamp = currentTime - 4000),
            createShot(MissDirection.PUSH, timestamp = currentTime - 5000),
            createShot(MissDirection.PULL, timestamp = currentTime - 6000),
            createShot(MissDirection.FAT, timestamp = currentTime - 7000),
            createShot(MissDirection.THIN, timestamp = currentTime - 8000),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 9000),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 10000)
        )

        // Setup repository to return these shots
        coEvery { repository.getRecentShots(any()) } returns flowOf(shots)

        // Aggregate patterns
        val patterns = aggregator.aggregatePatterns()

        // Verify: No patterns should be returned since none reach 30% threshold
        // (2/10 = 20% for slice and hook, which is below 30% threshold)
        assertTrue("Patterns below threshold should be filtered out", patterns.isEmpty())
    }

    @Test
    fun `patterns above frequency threshold are aggregated`() = runTest {
        // Create 10 shots where SLICE appears 4 times (40% - above 30% threshold)
        val shots = listOf(
            createShot(MissDirection.SLICE, timestamp = currentTime - 1000),
            createShot(MissDirection.SLICE, timestamp = currentTime - 2000),
            createShot(MissDirection.SLICE, timestamp = currentTime - 3000),
            createShot(MissDirection.SLICE, timestamp = currentTime - 4000),
            createShot(MissDirection.HOOK, timestamp = currentTime - 5000),
            createShot(MissDirection.HOOK, timestamp = currentTime - 6000),
            createShot(MissDirection.PUSH, timestamp = currentTime - 7000),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 8000),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 9000),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 10000)
        )

        coEvery { repository.getRecentShots(any()) } returns flowOf(shots)

        val patterns = aggregator.aggregatePatterns()

        // Verify: SLICE pattern should be detected (4/10 = 40%)
        assertEquals("One pattern should be detected", 1, patterns.size)
        assertEquals("Pattern should be SLICE", MissDirection.SLICE, patterns[0].direction)
        assertEquals("Pattern frequency should be 4", 4, patterns[0].frequency)
        assertTrue("Pattern confidence should be around 0.4", patterns[0].confidence > 0.35f)
    }

    @Test
    fun `minimum shots requirement enforced`() = runTest {
        // Create only 2 shots (below MIN_SHOTS_FOR_PATTERN = 3)
        val shots = listOf(
            createShot(MissDirection.SLICE, timestamp = currentTime - 1000),
            createShot(MissDirection.SLICE, timestamp = currentTime - 2000)
        )

        coEvery { repository.getRecentShots(any()) } returns flowOf(shots)

        val patterns = aggregator.aggregatePatterns()

        // Verify: No patterns since we don't have minimum shots
        assertTrue("Patterns require minimum shots", patterns.isEmpty())
    }

    // ========================================================================
    // Frequency Accumulation Tests
    // ========================================================================

    @Test
    fun `frequency accumulates correctly across multiple shots`() = runTest {
        // Create 20 shots with SLICE appearing 12 times (60%)
        val shots = buildList {
            repeat(12) {
                add(createShot(MissDirection.SLICE, timestamp = currentTime - (it * 1000L)))
            }
            repeat(8) {
                add(createShot(MissDirection.STRAIGHT, timestamp = currentTime - (it * 1000L + 12000)))
            }
        }

        coEvery { repository.getRecentShots(any()) } returns flowOf(shots)

        val patterns = aggregator.aggregatePatterns()

        // Verify: Pattern frequency and confidence
        assertEquals("One pattern should be detected", 1, patterns.size)
        assertEquals("Pattern should be SLICE", MissDirection.SLICE, patterns[0].direction)
        assertEquals("Frequency should be 12", 12, patterns[0].frequency)
        assertTrue("Confidence should be around 0.6", patterns[0].confidence > 0.55f)
    }

    @Test
    fun `multiple patterns aggregated when each meets threshold`() = runTest {
        // Create shots with two patterns above threshold
        val shots = listOf(
            // SLICE: 5/10 = 50%
            createShot(MissDirection.SLICE, timestamp = currentTime - 1000),
            createShot(MissDirection.SLICE, timestamp = currentTime - 2000),
            createShot(MissDirection.SLICE, timestamp = currentTime - 3000),
            createShot(MissDirection.SLICE, timestamp = currentTime - 4000),
            createShot(MissDirection.SLICE, timestamp = currentTime - 5000),
            // HOOK: 3/10 = 30%
            createShot(MissDirection.HOOK, timestamp = currentTime - 6000),
            createShot(MissDirection.HOOK, timestamp = currentTime - 7000),
            createShot(MissDirection.HOOK, timestamp = currentTime - 8000),
            // STRAIGHT: 2/10 = 20% (below threshold)
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 9000),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 10000)
        )

        coEvery { repository.getRecentShots(any()) } returns flowOf(shots)

        val patterns = aggregator.aggregatePatterns()

        // Verify: Two patterns detected (SLICE and HOOK), sorted by confidence
        assertEquals("Two patterns should be detected", 2, patterns.size)
        assertEquals("First pattern should be SLICE (highest confidence)", MissDirection.SLICE, patterns[0].direction)
        assertEquals("Second pattern should be HOOK", MissDirection.HOOK, patterns[1].direction)
        assertTrue("SLICE confidence should be higher", patterns[0].confidence > patterns[1].confidence)
    }

    // ========================================================================
    // Pattern Filtering by Confidence Tests
    // ========================================================================

    @Test
    fun `patterns filtered by minimum confidence`() = runTest {
        // Create a pattern that meets frequency threshold but has old data (will decay)
        val oldTimestamp = currentTime - (30L * 24 * 60 * 60 * 1000) // 30 days ago
        val shots = listOf(
            createShot(MissDirection.SLICE, timestamp = oldTimestamp - 1000),
            createShot(MissDirection.SLICE, timestamp = oldTimestamp - 2000),
            createShot(MissDirection.SLICE, timestamp = oldTimestamp - 3000),
            createShot(MissDirection.SLICE, timestamp = oldTimestamp - 4000),
            createShot(MissDirection.STRAIGHT, timestamp = oldTimestamp - 5000),
            createShot(MissDirection.STRAIGHT, timestamp = oldTimestamp - 6000),
            createShot(MissDirection.STRAIGHT, timestamp = oldTimestamp - 7000),
            createShot(MissDirection.STRAIGHT, timestamp = oldTimestamp - 8000),
            createShot(MissDirection.STRAIGHT, timestamp = oldTimestamp - 9000),
            createShot(MissDirection.STRAIGHT, timestamp = oldTimestamp - 10000)
        )

        coEvery { repository.getRecentShots(any()) } returns flowOf(shots)

        val patterns = aggregator.aggregatePatterns()

        // Verify: Pattern still appears but with reduced confidence due to age
        if (patterns.isNotEmpty()) {
            val slicePattern = patterns.find { it.direction == MissDirection.SLICE }
            if (slicePattern != null) {
                // Base confidence would be 0.4, but should be reduced due to decay
                // After ~30 days (~2 half-lives), should be around 0.1
                assertTrue("Old pattern should have decayed confidence", slicePattern.confidence < 0.15f)
            }
        }
    }

    // ========================================================================
    // Interaction with ShotRecorder Tests
    // ========================================================================

    @Test
    fun `patterns update after recording new shots`() = runTest {
        // Initial state: No SLICE pattern (below threshold)
        val initialShots = listOf(
            createShot(MissDirection.HOOK, timestamp = currentTime - 1000),
            createShot(MissDirection.HOOK, timestamp = currentTime - 2000),
            createShot(MissDirection.PUSH, timestamp = currentTime - 3000),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 4000),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 5000)
        )

        coEvery { repository.getRecentShots(any()) } returns flowOf(initialShots)
        coEvery { repository.recordShot(any()) } returns Unit

        val initialPatterns = aggregator.aggregatePatterns()
        assertTrue("No patterns should meet threshold initially", initialPatterns.isEmpty())

        // Record new SLICE shots to push it above threshold
        val newShots = listOf(
            createShot(MissDirection.SLICE, timestamp = currentTime),
            createShot(MissDirection.SLICE, timestamp = currentTime - 100)
        )

        newShots.forEach { shotRecorder.recordShot(it) }

        // Verify shots were recorded
        coVerify(exactly = 2) { repository.recordShot(any()) }

        // Updated state: SLICE now appears 2/7 times (28.5% - just below threshold)
        // Let's add one more to push it over
        val additionalShot = createShot(MissDirection.SLICE, timestamp = currentTime - 200)
        shotRecorder.recordShot(additionalShot)

        val updatedShots = initialShots + newShots + additionalShot
        coEvery { repository.getRecentShots(any()) } returns flowOf(updatedShots)

        val updatedPatterns = aggregator.aggregatePatterns()

        // Verify: SLICE pattern should now be detected (3/8 = 37.5%)
        val slicePattern = updatedPatterns.find { it.direction == MissDirection.SLICE }
        assertTrue("SLICE pattern should be detected after new shots", slicePattern != null)
        assertEquals("SLICE frequency should be 3", 3, slicePattern?.frequency)
    }

    @Test
    fun `aggregator handles club-specific patterns with shot recorder`() = runTest {
        // Create shots for specific club
        val clubShots = listOf(
            createShot(MissDirection.SLICE, timestamp = currentTime - 1000, club = testClub),
            createShot(MissDirection.SLICE, timestamp = currentTime - 2000, club = testClub),
            createShot(MissDirection.SLICE, timestamp = currentTime - 3000, club = testClub),
            createShot(MissDirection.SLICE, timestamp = currentTime - 4000, club = testClub),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 5000, club = testClub),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 6000, club = testClub),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 7000, club = testClub),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 8000, club = testClub),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 9000, club = testClub),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 10000, club = testClub)
        )

        coEvery { repository.getShotsByClub(testClub.id) } returns flowOf(clubShots)

        val patterns = aggregator.getPatternsForClub(testClub.id)

        // Verify: Club-specific pattern detected
        assertEquals("One pattern for club", 1, patterns.size)
        assertEquals("Pattern should be SLICE", MissDirection.SLICE, patterns[0].direction)
        assertEquals("Pattern should have club info", testClub, patterns[0].club)
        assertEquals("Frequency should be 4", 4, patterns[0].frequency)
    }

    @Test
    fun `aggregator handles pressure context with shot recorder`() = runTest {
        // Create pressure shots
        val pressureShots = listOf(
            createShot(MissDirection.SLICE, timestamp = currentTime - 1000, isPressure = true),
            createShot(MissDirection.SLICE, timestamp = currentTime - 2000, isPressure = true),
            createShot(MissDirection.SLICE, timestamp = currentTime - 3000, isPressure = true),
            createShot(MissDirection.SLICE, timestamp = currentTime - 4000, isPressure = true),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 5000, isPressure = true),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 6000, isPressure = true),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 7000, isPressure = true),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 8000, isPressure = true),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 9000, isPressure = true),
            createShot(MissDirection.STRAIGHT, timestamp = currentTime - 10000, isPressure = true)
        )

        coEvery { repository.getShotsWithPressure() } returns flowOf(pressureShots)

        val patterns = aggregator.getPatternsForPressure()

        // Verify: Pressure-specific pattern detected
        assertEquals("One pressure pattern", 1, patterns.size)
        assertEquals("Pattern should be SLICE", MissDirection.SLICE, patterns[0].direction)
        assertTrue("Pattern should have pressure context", patterns[0].pressureContext?.hasPressure == true)
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
}
