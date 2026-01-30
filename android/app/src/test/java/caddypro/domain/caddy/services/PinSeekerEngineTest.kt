package caddypro.domain.caddy.services

import caddypro.domain.navcaddy.models.BagProfile
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.ClubType
import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.domain.caddy.models.CourseHole
import caddypro.domain.caddy.models.HazardLocation
import caddypro.domain.caddy.models.HazardType
import caddypro.domain.caddy.models.HazardZone
import caddypro.domain.caddy.models.MetricScore
import caddypro.domain.caddy.models.ReadinessBreakdown
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.ReadinessSource
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.MissPattern
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for PinSeekerEngine.
 *
 * Validates strategy personalization based on:
 * - Player handicap and miss bias
 * - Readiness-adjusted safety margins
 * - Hazard filtering for player's dominant miss
 * - Risk callout generation
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 8
 * Acceptance criteria: A3 (Hazard-aware landing zone)
 */
class PinSeekerEngineTest {

    private lateinit var engine: PinSeekerEngine
    private lateinit var mockRepository: NavCaddyRepository

    // Test data
    private val testHandicap = 9
    private val currentTime = System.currentTimeMillis()

    @Before
    fun setup() {
        mockRepository = mockk()
        engine = PinSeekerEngine(mockRepository)
    }

    // ===========================
    // Readiness Impact Tests
    // ===========================

    @Test
    fun `computeStrategy with low readiness doubles safety margin`() = runTest {
        // Given: Low readiness (score = 40)
        val lowReadiness = createReadinessScore(40)
        val hole = createTestHole(number = 1, par = 4)

        setupMockRepository(
            missPatterns = emptyList(),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, lowReadiness)

        // Then: Safety margin should be doubled
        // Base margin = 9 * 2 = 18m
        // Low readiness factor = 0.5
        // Final margin = 18 / 0.5 = 36m
        assertEquals("Low readiness should double safety margin", 36, strategy.recommendedLandingZone.safetyMargin)
    }

    @Test
    fun `computeStrategy with high readiness uses normal safety margin`() = runTest {
        // Given: High readiness (score = 80)
        val highReadiness = createReadinessScore(80)
        val hole = createTestHole(number = 1, par = 4)

        setupMockRepository(
            missPatterns = emptyList(),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, highReadiness)

        // Then: Safety margin should be normal
        // Base margin = 9 * 2 = 18m
        // High readiness factor = 1.0
        // Final margin = 18 / 1.0 = 18m
        assertEquals("High readiness should use normal safety margin", 18, strategy.recommendedLandingZone.safetyMargin)
    }

    @Test
    fun `computeStrategy with medium readiness scales safety margin linearly`() = runTest {
        // Given: Medium readiness (score = 50)
        val mediumReadiness = createReadinessScore(50)
        val hole = createTestHole(number = 1, par = 4)

        setupMockRepository(
            missPatterns = emptyList(),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, mediumReadiness)

        // Then: Safety margin should be between 18m and 36m
        // Readiness factor at 50 = 0.5 + ((50 - 40) / 20.0) * 0.5 = 0.75
        // Final margin = 18 / 0.75 = 24m
        val margin = strategy.recommendedLandingZone.safetyMargin
        assertTrue("Medium readiness should scale margin (18-36m)", margin in 20..28)
    }

    // ===========================
    // Dominant Miss Tests
    // ===========================

    @Test
    fun `computeStrategy filters hazards by dominant slice`() = runTest {
        // Given: Player has dominant slice pattern
        val readiness = createReadinessScore(60)
        val slicePattern = createMissPattern(MissDirection.SLICE, confidence = 0.8f)

        val hole = createTestHole(
            number = 1,
            par = 4,
            hazards = listOf(
                createHazard(HazardType.WATER, "right", affectedBy = listOf(MissDirection.SLICE)),
                createHazard(HazardType.BUNKER, "left", affectedBy = listOf(MissDirection.HOOK)),
                createHazard(HazardType.OB, "right", affectedBy = listOf(MissDirection.SLICE, MissDirection.PUSH))
            )
        )

        setupMockRepository(
            missPatterns = listOf(slicePattern),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, readiness)

        // Then: Only right-side hazards (affected by slice) should be in danger zones
        assertEquals("Should identify 2 hazards for slice", 2, strategy.dangerZones.size)
        assertTrue("All danger zones should affect slice",
            strategy.dangerZones.all { it.affectedMisses.contains(MissDirection.SLICE) })
        assertEquals("Personalization should show SLICE", MissDirection.SLICE, strategy.personalizedFor.dominantMiss)
    }

    @Test
    fun `computeStrategy filters hazards by dominant hook`() = runTest {
        // Given: Player has dominant hook pattern
        val readiness = createReadinessScore(60)
        val hookPattern = createMissPattern(MissDirection.HOOK, confidence = 0.9f)

        val hole = createTestHole(
            number = 1,
            par = 4,
            hazards = listOf(
                createHazard(HazardType.WATER, "right", affectedBy = listOf(MissDirection.SLICE)),
                createHazard(HazardType.BUNKER, "left", affectedBy = listOf(MissDirection.HOOK)),
                createHazard(HazardType.PENALTY_ROUGH, "left", affectedBy = listOf(MissDirection.HOOK, MissDirection.PULL))
            )
        )

        setupMockRepository(
            missPatterns = listOf(hookPattern),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, readiness)

        // Then: Only left-side hazards (affected by hook) should be in danger zones
        assertEquals("Should identify 2 hazards for hook", 2, strategy.dangerZones.size)
        assertTrue("All danger zones should affect hook",
            strategy.dangerZones.all { it.affectedMisses.contains(MissDirection.HOOK) })
    }

    @Test
    fun `computeStrategy with no miss patterns defaults to STRAIGHT`() = runTest {
        // Given: No miss patterns in database
        val readiness = createReadinessScore(60)
        val hole = createTestHole(number = 1, par = 4)

        setupMockRepository(
            missPatterns = emptyList(),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, readiness)

        // Then: Should default to STRAIGHT
        assertEquals("Should default to STRAIGHT miss", MissDirection.STRAIGHT, strategy.personalizedFor.dominantMiss)
    }

    @Test
    fun `computeStrategy uses decayed confidence to find dominant miss`() = runTest {
        // Given: Multiple patterns with different decay rates
        val oldSlicePattern = createMissPattern(
            direction = MissDirection.SLICE,
            confidence = 0.9f,
            lastOccurrence = currentTime - (30L * 24 * 60 * 60 * 1000) // 30 days ago
        )
        val recentHookPattern = createMissPattern(
            direction = MissDirection.HOOK,
            confidence = 0.6f,
            lastOccurrence = currentTime - (2L * 24 * 60 * 60 * 1000) // 2 days ago
        )

        val readiness = createReadinessScore(60)
        val hole = createTestHole(number = 1, par = 4)

        setupMockRepository(
            missPatterns = listOf(oldSlicePattern, recentHookPattern),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, readiness)

        // Then: Recent hook should win over old slice due to decay
        // Old slice: 0.9 * 0.5^(30/14) ≈ 0.23
        // Recent hook: 0.6 * 0.5^(2/14) ≈ 0.55
        assertEquals("Recent hook should be dominant after decay", MissDirection.HOOK, strategy.personalizedFor.dominantMiss)
    }

    // ===========================
    // Risk Callout Tests
    // ===========================

    @Test
    fun `computeStrategy generates risk callouts for slice with water right`() = runTest {
        // Given: Slice player with water right
        val readiness = createReadinessScore(60)
        val slicePattern = createMissPattern(MissDirection.SLICE, confidence = 0.8f)

        val hole = createTestHole(
            number = 1,
            par = 4,
            hazards = listOf(
                createHazard(HazardType.WATER, "right", affectedBy = listOf(MissDirection.SLICE))
            )
        )

        setupMockRepository(
            missPatterns = listOf(slicePattern),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, readiness)

        // Then: Should have 1 risk callout mentioning water and slice
        assertEquals("Should have 1 risk callout", 1, strategy.riskCallouts.size)
        val callout = strategy.riskCallouts[0]
        assertTrue("Callout should mention water", callout.contains("water", ignoreCase = true))
        assertTrue("Callout should mention slice", callout.contains("slice", ignoreCase = true))
    }

    @Test
    fun `computeStrategy limits risk callouts to 3`() = runTest {
        // Given: Many hazards for slice player
        val readiness = createReadinessScore(60)
        val slicePattern = createMissPattern(MissDirection.SLICE, confidence = 0.8f)

        val hole = createTestHole(
            number = 1,
            par = 4,
            hazards = listOf(
                createHazard(HazardType.WATER, "right", affectedBy = listOf(MissDirection.SLICE)),
                createHazard(HazardType.OB, "right", affectedBy = listOf(MissDirection.SLICE)),
                createHazard(HazardType.BUNKER, "right", affectedBy = listOf(MissDirection.SLICE)),
                createHazard(HazardType.PENALTY_ROUGH, "right", affectedBy = listOf(MissDirection.SLICE)),
                createHazard(HazardType.TREES, "right", affectedBy = listOf(MissDirection.SLICE))
            )
        )

        setupMockRepository(
            missPatterns = listOf(slicePattern),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, readiness)

        // Then: Should have max 3 callouts
        assertTrue("Should have max 3 callouts", strategy.riskCallouts.size <= 3)
    }

    @Test
    fun `computeStrategy prioritizes high-penalty hazards in callouts`() = runTest {
        // Given: Mix of high and low penalty hazards
        val readiness = createReadinessScore(60)
        val slicePattern = createMissPattern(MissDirection.SLICE, confidence = 0.8f)

        val hole = createTestHole(
            number = 1,
            par = 4,
            hazards = listOf(
                createHazard(HazardType.BUNKER, "right", affectedBy = listOf(MissDirection.SLICE)),  // Low priority
                createHazard(HazardType.WATER, "right", affectedBy = listOf(MissDirection.SLICE)),   // High priority
                createHazard(HazardType.OB, "right", affectedBy = listOf(MissDirection.SLICE))       // High priority
            )
        )

        setupMockRepository(
            missPatterns = listOf(slicePattern),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, readiness)

        // Then: Water and OB should appear before bunker
        assertTrue("Should mention water", strategy.riskCallouts.any { it.contains("water", ignoreCase = true) })
        assertTrue("Should mention OB", strategy.riskCallouts.any { it.contains("OB") })
    }

    // ===========================
    // Landing Zone Tests
    // ===========================

    @Test
    fun `computeStrategy aims left for slice player`() = runTest {
        // Given: Slice player
        val readiness = createReadinessScore(60)
        val slicePattern = createMissPattern(MissDirection.SLICE, confidence = 0.8f)
        val hole = createTestHole(number = 1, par = 4)

        setupMockRepository(
            missPatterns = listOf(slicePattern),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, readiness)

        // Then: Target line should be left of center (< 180)
        assertTrue("Slice player should aim left", strategy.recommendedLandingZone.targetLine < 180)
        assertTrue("Visual cue should mention left",
            strategy.recommendedLandingZone.visualCue.contains("left", ignoreCase = true))
    }

    @Test
    fun `computeStrategy aims right for hook player`() = runTest {
        // Given: Hook player
        val readiness = createReadinessScore(60)
        val hookPattern = createMissPattern(MissDirection.HOOK, confidence = 0.8f)
        val hole = createTestHole(number = 1, par = 4)

        setupMockRepository(
            missPatterns = listOf(hookPattern),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, readiness)

        // Then: Target line should be right of center (> 180)
        assertTrue("Hook player should aim right", strategy.recommendedLandingZone.targetLine > 180)
        assertTrue("Visual cue should mention right",
            strategy.recommendedLandingZone.visualCue.contains("right", ignoreCase = true))
    }

    @Test
    fun `computeStrategy on par 3 aims for green`() = runTest {
        // Given: Par 3 hole (150m)
        val readiness = createReadinessScore(60)
        val hole = createTestHole(number = 3, par = 3, lengthMeters = 150)

        setupMockRepository(
            missPatterns = emptyList(),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, readiness)

        // Then: Ideal distance should equal hole length (aim for green)
        assertEquals("Par 3 should aim for green", 150, strategy.recommendedLandingZone.idealDistance)
    }

    @Test
    fun `computeStrategy on par 4 aims for layup position`() = runTest {
        // Given: Par 4 hole (360m)
        val readiness = createReadinessScore(60)
        val hole = createTestHole(number = 1, par = 4, lengthMeters = 360)

        setupMockRepository(
            missPatterns = emptyList(),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, readiness)

        // Then: Ideal distance should be ~70% of hole length (252m)
        assertEquals("Par 4 should aim for layup", 252, strategy.recommendedLandingZone.idealDistance)
    }

    // ===========================
    // Handicap Tests
    // ===========================

    @Test
    fun `computeStrategy with higher handicap gets larger safety margin`() = runTest {
        // Given: High handicap (18)
        val readiness = createReadinessScore(60)
        val hole = createTestHole(number = 1, par = 4)

        setupMockRepository(
            missPatterns = emptyList(),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, handicap = 18, readiness)

        // Then: Safety margin should be 36m (18 * 2)
        assertEquals("18 handicap should have 36m margin", 36, strategy.recommendedLandingZone.safetyMargin)
    }

    @Test
    fun `computeStrategy with lower handicap gets smaller safety margin`() = runTest {
        // Given: Low handicap (2)
        val readiness = createReadinessScore(60)
        val hole = createTestHole(number = 1, par = 4)

        setupMockRepository(
            missPatterns = emptyList(),
            activeBag = null
        )

        // When
        val strategy = engine.computeStrategy(hole, handicap = 2, readiness)

        // Then: Safety margin should be 4m (2 * 2)
        assertEquals("2 handicap should have 4m margin", 4, strategy.recommendedLandingZone.safetyMargin)
    }

    // ===========================
    // Club Distance Tests
    // ===========================

    @Test
    fun `computeStrategy includes club distances in personalization context`() = runTest {
        // Given: Active bag with clubs
        val readiness = createReadinessScore(60)
        val hole = createTestHole(number = 1, par = 4)

        val bag = createBagProfile("Test Bag")
        val clubs = listOf(
            createClub("Driver", ClubType.DRIVER, 220),
            createClub("7-iron", ClubType.IRON, 150)
        )

        setupMockRepository(
            missPatterns = emptyList(),
            activeBag = bag,
            bagClubs = clubs
        )

        // When
        val strategy = engine.computeStrategy(hole, testHandicap, readiness)

        // Then: Personalization context should include club distances
        assertEquals("Should have 2 clubs in context", 2, strategy.personalizedFor.clubDistances.size)
        assertEquals("Driver distance should be 220", 220, strategy.personalizedFor.clubDistances["Driver"])
        assertEquals("7-iron distance should be 150", 150, strategy.personalizedFor.clubDistances["7-iron"])
    }

    // ===========================
    // Helper Functions
    // ===========================

    private fun createReadinessScore(overall: Int): ReadinessScore {
        return ReadinessScore(
            overall = overall,
            breakdown = ReadinessBreakdown(
                hrv = MetricScore(overall.toDouble(), 0.4),
                sleepQuality = MetricScore(overall.toDouble(), 0.4),
                stressLevel = MetricScore(overall.toDouble(), 0.2)
            ),
            timestamp = currentTime,
            source = ReadinessSource.WEARABLE_SYNC
        )
    }

    private fun createTestHole(
        number: Int,
        par: Int,
        lengthMeters: Int = 360,
        hazards: List<HazardZone> = emptyList()
    ): CourseHole {
        return CourseHole(
            number = number,
            par = par,
            lengthMeters = lengthMeters,
            hazards = hazards,
            pinPosition = null
        )
    }

    private fun createHazard(
        type: HazardType,
        side: String,
        affectedBy: List<MissDirection>,
        distanceRange: IntRange = 100..300,
        penalty: Double = 1.0
    ): HazardZone {
        return HazardZone(
            type = type,
            location = HazardLocation(side, distanceRange),
            penaltyStrokes = penalty,
            affectedMisses = affectedBy
        )
    }

    private fun createMissPattern(
        direction: MissDirection,
        confidence: Float,
        lastOccurrence: Long = currentTime
    ): MissPattern {
        return MissPattern(
            direction = direction,
            club = null,
            frequency = 10,
            confidence = confidence,
            pressureContext = null,
            lastOccurrence = lastOccurrence
        )
    }

    private fun createBagProfile(name: String): BagProfile {
        return BagProfile(
            id = "test-bag-1",
            name = name,
            isActive = true,
            isArchived = false
        )
    }

    private fun createClub(name: String, type: ClubType, carryMeters: Int): Club {
        return Club(
            id = "club-${name.lowercase()}",
            name = name,
            type = type,
            estimatedCarry = carryMeters
        )
    }

    private fun setupMockRepository(
        missPatterns: List<MissPattern>,
        activeBag: BagProfile?,
        bagClubs: List<Club> = emptyList()
    ) {
        every { mockRepository.getMissPatterns() } returns flowOf(missPatterns)
        every { mockRepository.getActiveBag() } returns flowOf(activeBag)

        if (activeBag != null) {
            every { mockRepository.getClubsForBag(activeBag.id) } returns flowOf(bagClubs)
        }
    }
}
