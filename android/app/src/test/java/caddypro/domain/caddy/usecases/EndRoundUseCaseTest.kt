package caddypro.domain.caddy.usecases

import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.domain.navcaddy.context.RoundState
import caddypro.domain.navcaddy.context.SessionContextManager
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for EndRoundUseCase.
 *
 * Validates:
 * - Successful round completion and session clearing
 * - Round summary with final statistics
 * - Abandon round without saving
 * - Failure when no active round
 * - Input validation for final scores
 *
 * Spec reference: live-caddy-mode.md R1 (Live Round Context)
 * Plan reference: live-caddy-mode-plan.md Task 9
 */
class EndRoundUseCaseTest {

    private lateinit var sessionContextManager: SessionContextManager
    private lateinit var navCaddyRepository: NavCaddyRepository
    private lateinit var useCase: EndRoundUseCase

    private val completedRoundState = RoundState(
        roundId = "round-123",
        courseName = "Pebble Beach",
        currentHole = 18,
        currentPar = 5,
        totalScore = 85,
        holesCompleted = 18
    )

    private val partialRoundState = RoundState(
        roundId = "round-456",
        courseName = "St Andrews",
        currentHole = 9,
        currentPar = 4,
        totalScore = 42,
        holesCompleted = 8
    )

    @Before
    fun setup() {
        sessionContextManager = mockk()
        navCaddyRepository = mockk()
        useCase = EndRoundUseCase(
            sessionContextManager = sessionContextManager,
            navCaddyRepository = navCaddyRepository
        )
    }

    /**
     * Test: End complete 18-hole round.
     *
     * Validates:
     * - Round summary is created with correct data
     * - Session is cleared
     * - Final score matches round state
     */
    @Test
    fun `invoke ends complete round and clears session`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns completedRoundState
        justRun { sessionContextManager.clearSession() }

        // When
        val result = useCase()

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val summary = result.getOrNull()
        assertNotNull("Summary should not be null", summary)
        assertEquals("Round ID should match", "round-123", summary?.roundId)
        assertEquals("Course name should match", "Pebble Beach", summary?.courseName)
        assertEquals("Final score should match", 85, summary?.finalScore)
        assertEquals("Holes played should be 18", 18, summary?.holesPlayed)
        assertTrue("Should be complete 18 holes", summary?.isComplete18Holes() == true)

        verify(exactly = 1) { sessionContextManager.clearSession() }
    }

    /**
     * Test: End partial round (9 holes).
     *
     * Validates:
     * - Partial rounds are supported
     * - Holes played reflects actual count
     * - Summary indicates not complete 18
     */
    @Test
    fun `invoke ends partial round correctly`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns partialRoundState
        justRun { sessionContextManager.clearSession() }

        // When
        val result = useCase()

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val summary = result.getOrNull()
        assertNotNull("Summary should not be null", summary)
        assertEquals("Holes played should be 8", 8, summary?.holesPlayed)
        assertEquals("Final score should match", 42, summary?.finalScore)
        assertTrue("Should not be complete 18 holes", summary?.isComplete18Holes() == false)
    }

    /**
     * Test: End round with custom final score.
     *
     * Validates:
     * - Custom final score overrides round state
     * - Useful for score corrections
     */
    @Test
    fun `invoke with custom final score overrides round state`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns completedRoundState
        justRun { sessionContextManager.clearSession() }

        // When
        val result = useCase(
            finalScore = 90,
            holesPlayed = 18
        )

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val summary = result.getOrNull()
        assertEquals("Final score should use custom value", 90, summary?.finalScore)
        assertEquals("Holes played should use custom value", 18, summary?.holesPlayed)
    }

    /**
     * Test: Average score per hole calculation.
     *
     * Validates:
     * - Average is calculated correctly
     * - Handles 18-hole rounds
     * - Handles partial rounds
     */
    @Test
    fun `round summary calculates average score correctly`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns completedRoundState
        justRun { sessionContextManager.clearSession() }

        // When
        val result = useCase()

        // Then
        val summary = result.getOrNull()
        val average = summary?.averageScorePerHole()
        assertNotNull("Average should not be null", average)
        assertEquals("Average should be 85/18", 85.0 / 18.0, average!!, 0.01)
    }

    /**
     * Test: Average score with zero holes.
     *
     * Validates:
     * - Average is 0.0 when no holes played
     */
    @Test
    fun `round summary with zero holes returns zero average`() = runTest {
        // Given
        val emptyRoundState = completedRoundState.copy(
            totalScore = 0,
            holesCompleted = 0
        )
        every { sessionContextManager.getCurrentRoundState() } returns emptyRoundState
        justRun { sessionContextManager.clearSession() }

        // When
        val result = useCase(
            finalScore = 0,
            holesPlayed = 0
        )

        // Then
        val summary = result.getOrNull()
        assertEquals("Average should be 0.0", 0.0, summary?.averageScorePerHole(), 0.01)
    }

    /**
     * Test: No active round returns failure.
     */
    @Test
    fun `invoke with no active round returns failure`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns null

        // When
        val result = useCase()

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertEquals("Error message should indicate no active round",
            "No active round to end", exception?.message)
    }

    /**
     * Test: Abandon round without saving.
     *
     * Validates:
     * - Round is not persisted
     * - Session is cleared
     * - No summary is returned
     */
    @Test
    fun `abandon clears session without saving`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns completedRoundState
        justRun { sessionContextManager.clearSession() }

        // When
        val result = useCase.abandon()

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        verify(exactly = 1) { sessionContextManager.clearSession() }
        // Verify no repository calls were made (no persistence)
        verify(exactly = 0) { navCaddyRepository.saveSession(any()) }
    }

    /**
     * Test: Abandon with no active round returns failure.
     */
    @Test
    fun `abandon with no active round returns failure`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns null

        // When
        val result = useCase.abandon()

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertEquals("Error message should indicate no active round",
            "No active round to abandon", exception?.message)
    }

    /**
     * Test: Negative final score fails validation.
     */
    @Test
    fun `invoke with negative final score returns failure`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns completedRoundState

        // When
        val result = useCase(
            finalScore = -5,
            holesPlayed = 18
        )

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Error should mention non-negative score",
            exception?.message?.contains("Final score must be non-negative") == true)
    }

    /**
     * Test: Invalid holes played fails validation.
     */
    @Test
    fun `invoke with invalid holes played returns failure`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns completedRoundState

        // When
        val result = useCase(
            finalScore = 85,
            holesPlayed = 20
        )

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Error should mention holes range",
            exception?.message?.contains("Holes played must be between 0 and 18") == true)
    }

    /**
     * Test: Round summary validation on construction.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `round summary with blank course name throws exception`() {
        RoundSummary(
            roundId = "round-123",
            courseName = "",
            finalScore = 85,
            holesPlayed = 18,
            endTime = System.currentTimeMillis()
        )
    }

    /**
     * Test: Round summary validation on construction.
     */
    @Test(expected = IllegalArgumentException::class)
    fun `round summary with blank round id throws exception`() {
        RoundSummary(
            roundId = "",
            courseName = "Pebble Beach",
            finalScore = 85,
            holesPlayed = 18,
            endTime = System.currentTimeMillis()
        )
    }
}
