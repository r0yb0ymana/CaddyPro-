package caddypro.domain.caddy.usecases

import caddypro.domain.caddy.models.Location
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
 * Unit tests for StartRoundUseCase.
 *
 * Validates:
 * - Successful round start with valid parameters
 * - Session manager is updated correctly
 * - Conversation history is cleared
 * - Resume functionality with saved state
 * - Validation of input parameters
 *
 * Spec reference: live-caddy-mode.md R1 (Live Round Context)
 * Plan reference: live-caddy-mode-plan.md Task 9
 */
class StartRoundUseCaseTest {

    private lateinit var sessionContextManager: SessionContextManager
    private lateinit var useCase: StartRoundUseCase

    private val testLocation = Location(
        latitude = 36.5674,
        longitude = -121.9500
    )

    @Before
    fun setup() {
        sessionContextManager = mockk()
        useCase = StartRoundUseCase(sessionContextManager)
    }

    /**
     * Test: Successful round start with default parameters.
     *
     * Validates:
     * - Round is created with unique ID
     * - Default starting hole is 1
     * - Default starting par is 4
     * - Session manager is updated
     * - Conversation history is cleared
     */
    @Test
    fun `invoke with default parameters starts round successfully`() = runTest {
        // Given
        justRun { sessionContextManager.updateRound(any(), any(), any(), any()) }
        justRun { sessionContextManager.clearConversationHistory() }

        // When
        val result = useCase(
            courseName = "Pebble Beach",
            location = testLocation
        )

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val roundState = result.getOrNull()
        assertNotNull("Round state should not be null", roundState)
        assertNotNull("Round ID should be generated", roundState?.roundId)
        assertEquals("Course name should match", "Pebble Beach", roundState?.courseName)
        assertEquals("Starting hole should be 1", 1, roundState?.currentHole)
        assertEquals("Starting par should be 4", 4, roundState?.currentPar)
        assertEquals("Initial score should be 0", 0, roundState?.totalScore)
        assertEquals("Initial holes completed should be 0", 0, roundState?.holesCompleted)

        verify(exactly = 1) {
            sessionContextManager.updateRound(
                roundId = any(),
                courseName = "Pebble Beach",
                startingHole = 1,
                startingPar = 4
            )
        }
        verify(exactly = 1) { sessionContextManager.clearConversationHistory() }
    }

    /**
     * Test: Round start with custom starting hole.
     *
     * Validates:
     * - Custom starting hole is respected
     * - Custom starting par is respected
     */
    @Test
    fun `invoke with custom starting hole starts correctly`() = runTest {
        // Given
        justRun { sessionContextManager.updateRound(any(), any(), any(), any()) }
        justRun { sessionContextManager.clearConversationHistory() }

        // When
        val result = useCase(
            courseName = "St Andrews",
            location = testLocation,
            startingHole = 10,
            startingPar = 5
        )

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val roundState = result.getOrNull()
        assertEquals("Starting hole should be 10", 10, roundState?.currentHole)
        assertEquals("Starting par should be 5", 5, roundState?.currentPar)

        verify {
            sessionContextManager.updateRound(
                roundId = any(),
                courseName = "St Andrews",
                startingHole = 10,
                startingPar = 5
            )
        }
    }

    /**
     * Test: Invalid course name fails validation.
     */
    @Test
    fun `invoke with blank course name returns failure`() = runTest {
        // When
        val result = useCase(
            courseName = "",
            location = testLocation
        )

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Error should mention blank course name",
            exception?.message?.contains("Course name cannot be blank") == true)
    }

    /**
     * Test: Invalid starting hole fails validation.
     */
    @Test
    fun `invoke with invalid starting hole returns failure`() = runTest {
        // When
        val result = useCase(
            courseName = "Augusta National",
            location = testLocation,
            startingHole = 19
        )

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Error should mention hole range",
            exception?.message?.contains("Starting hole must be between 1 and 18") == true)
    }

    /**
     * Test: Invalid starting par fails validation.
     */
    @Test
    fun `invoke with invalid starting par returns failure`() = runTest {
        // When
        val result = useCase(
            courseName = "Augusta National",
            location = testLocation,
            startingPar = 2
        )

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Error should mention par range",
            exception?.message?.contains("Starting par must be between 3 and 5") == true)
    }

    /**
     * Test: Resume round with saved state.
     *
     * Validates:
     * - Round is restored with saved state
     * - Score and holes completed are preserved
     * - Session manager is updated with all state
     */
    @Test
    fun `resume with saved state restores round correctly`() = runTest {
        // Given
        justRun { sessionContextManager.updateRound(any(), any(), any(), any()) }
        justRun { sessionContextManager.updateScore(any(), any()) }

        // When
        val result = useCase.resume(
            roundId = "round-456",
            courseName = "Torrey Pines",
            currentHole = 9,
            currentPar = 4,
            totalScore = 40,
            holesCompleted = 8
        )

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val roundState = result.getOrNull()
        assertNotNull("Round state should not be null", roundState)
        assertEquals("Round ID should match", "round-456", roundState?.roundId)
        assertEquals("Course name should match", "Torrey Pines", roundState?.courseName)
        assertEquals("Current hole should match", 9, roundState?.currentHole)
        assertEquals("Current par should match", 4, roundState?.currentPar)
        assertEquals("Total score should match", 40, roundState?.totalScore)
        assertEquals("Holes completed should match", 8, roundState?.holesCompleted)

        verify(exactly = 1) {
            sessionContextManager.updateRound(
                roundId = "round-456",
                courseName = "Torrey Pines",
                startingHole = 9,
                startingPar = 4
            )
        }
        verify(exactly = 1) {
            sessionContextManager.updateScore(
                totalScore = 40,
                holesCompleted = 8
            )
        }
    }

    /**
     * Test: Resume with invalid state fails validation.
     */
    @Test
    fun `resume with invalid holes completed returns failure`() = runTest {
        // When
        val result = useCase.resume(
            roundId = "round-789",
            courseName = "Pinehurst",
            currentHole = 5,
            currentPar = 4,
            totalScore = 20,
            holesCompleted = 19
        )

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Error should mention holes completed range",
            exception?.message?.contains("Holes completed must be between 0 and 18") == true)
    }

    /**
     * Test: Resume with negative score fails validation.
     */
    @Test
    fun `resume with negative score returns failure`() = runTest {
        // When
        val result = useCase.resume(
            roundId = "round-999",
            courseName = "Oakmont",
            currentHole = 5,
            currentPar = 4,
            totalScore = -5,
            holesCompleted = 4
        )

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Error should mention non-negative score",
            exception?.message?.contains("Total score must be non-negative") == true)
    }
}
