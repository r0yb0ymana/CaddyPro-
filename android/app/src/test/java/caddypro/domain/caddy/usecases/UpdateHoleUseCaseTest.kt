package caddypro.domain.caddy.usecases

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
 * Unit tests for UpdateHoleUseCase.
 *
 * Validates:
 * - Successful hole update
 * - Complete hole and advance to next
 * - Score updates
 * - Failure when no active round
 * - Input validation
 *
 * Spec reference: live-caddy-mode.md R1 (Live Round Context)
 * Plan reference: live-caddy-mode-plan.md Task 9
 */
class UpdateHoleUseCaseTest {

    private lateinit var sessionContextManager: SessionContextManager
    private lateinit var useCase: UpdateHoleUseCase

    private val initialRoundState = RoundState(
        roundId = "round-123",
        courseName = "Pebble Beach",
        currentHole = 7,
        currentPar = 4,
        totalScore = 35,
        holesCompleted = 6
    )

    private val updatedRoundState = RoundState(
        roundId = "round-123",
        courseName = "Pebble Beach",
        currentHole = 8,
        currentPar = 3,
        totalScore = 35,
        holesCompleted = 6
    )

    private val completedRoundState = RoundState(
        roundId = "round-123",
        courseName = "Pebble Beach",
        currentHole = 8,
        currentPar = 3,
        totalScore = 39,
        holesCompleted = 7
    )

    @Before
    fun setup() {
        sessionContextManager = mockk()
        useCase = UpdateHoleUseCase(sessionContextManager)
    }

    /**
     * Test: Successful hole update.
     *
     * Validates:
     * - Hole number and par are updated
     * - Session manager is called correctly
     * - Updated round state is returned
     */
    @Test
    fun `invoke updates hole successfully`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns initialRoundState andThen updatedRoundState
        justRun { sessionContextManager.updateHole(any(), any()) }

        // When
        val result = useCase(
            holeNumber = 8,
            par = 3
        )

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val roundState = result.getOrNull()
        assertNotNull("Round state should not be null", roundState)
        assertEquals("Hole should be updated to 8", 8, roundState?.currentHole)
        assertEquals("Par should be updated to 3", 3, roundState?.currentPar)

        verify(exactly = 1) {
            sessionContextManager.updateHole(
                holeNumber = 8,
                par = 3
            )
        }
    }

    /**
     * Test: Complete hole and advance to next.
     *
     * Validates:
     * - Score is updated with hole score
     * - Holes completed is incremented
     * - Hole number and par are updated
     * - All session manager methods are called in order
     */
    @Test
    fun `completeHoleAndAdvance updates score and moves to next hole`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns initialRoundState andThen completedRoundState
        justRun { sessionContextManager.updateScore(any(), any()) }
        justRun { sessionContextManager.updateHole(any(), any()) }

        // When
        val result = useCase.completeHoleAndAdvance(
            nextHoleNumber = 8,
            nextHolePar = 3,
            holeScore = 4
        )

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val roundState = result.getOrNull()
        assertNotNull("Round state should not be null", roundState)
        assertEquals("Total score should include hole score", 39, roundState?.totalScore)
        assertEquals("Holes completed should increment", 7, roundState?.holesCompleted)
        assertEquals("Should move to next hole", 8, roundState?.currentHole)

        verify(exactly = 1) {
            sessionContextManager.updateScore(
                totalScore = 39,
                holesCompleted = 7
            )
        }
        verify(exactly = 1) {
            sessionContextManager.updateHole(
                holeNumber = 8,
                par = 3
            )
        }
    }

    /**
     * Test: Update score without advancing hole.
     *
     * Validates:
     * - Additional strokes are added to total
     * - Hole number doesn't change
     * - Holes completed doesn't change
     */
    @Test
    fun `updateScore adds strokes without advancing hole`() = runTest {
        // Given
        val scoreUpdatedState = initialRoundState.copy(totalScore = 37)
        every { sessionContextManager.getCurrentRoundState() } returns initialRoundState andThen scoreUpdatedState
        justRun { sessionContextManager.updateScore(any(), any()) }

        // When
        val result = useCase.updateScore(
            additionalStrokes = 2
        )

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val roundState = result.getOrNull()
        assertNotNull("Round state should not be null", roundState)
        assertEquals("Score should be updated", 37, roundState?.totalScore)
        assertEquals("Hole should not change", 7, roundState?.currentHole)
        assertEquals("Holes completed should not change", 6, roundState?.holesCompleted)

        verify(exactly = 1) {
            sessionContextManager.updateScore(
                totalScore = 37,
                holesCompleted = 6
            )
        }
    }

    /**
     * Test: No active round returns failure.
     */
    @Test
    fun `invoke with no active round returns failure`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns null

        // When
        val result = useCase(
            holeNumber = 8,
            par = 3
        )

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertEquals("Error message should indicate no active round", "No active round", exception?.message)
    }

    /**
     * Test: Invalid hole number fails validation.
     */
    @Test
    fun `invoke with invalid hole number returns failure`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns initialRoundState

        // When
        val result = useCase(
            holeNumber = 19,
            par = 4
        )

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Error should mention hole range",
            exception?.message?.contains("Hole number must be between 1 and 18") == true)
    }

    /**
     * Test: Invalid par fails validation.
     */
    @Test
    fun `invoke with invalid par returns failure`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns initialRoundState

        // When
        val result = useCase(
            holeNumber = 8,
            par = 6
        )

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Error should mention par range",
            exception?.message?.contains("Par must be between 3 and 5") == true)
    }

    /**
     * Test: Complete hole with zero score fails validation.
     */
    @Test
    fun `completeHoleAndAdvance with zero score returns failure`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns initialRoundState

        // When
        val result = useCase.completeHoleAndAdvance(
            nextHoleNumber = 8,
            nextHolePar = 3,
            holeScore = 0
        )

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Error should mention positive score",
            exception?.message?.contains("Hole score must be positive") == true)
    }

    /**
     * Test: Update score with zero strokes fails validation.
     */
    @Test
    fun `updateScore with zero strokes returns failure`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns initialRoundState

        // When
        val result = useCase.updateScore(
            additionalStrokes = 0
        )

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertTrue("Error should mention positive strokes",
            exception?.message?.contains("Additional strokes must be positive") == true)
    }

    /**
     * Test: Advancing to hole 18.
     *
     * Validates:
     * - Can advance to final hole (18)
     * - State is updated correctly
     */
    @Test
    fun `completeHoleAndAdvance to hole 18 works correctly`() = runTest {
        // Given
        val hole17State = initialRoundState.copy(currentHole = 17, holesCompleted = 16)
        val hole18State = hole17State.copy(currentHole = 18, totalScore = 70, holesCompleted = 17)
        every { sessionContextManager.getCurrentRoundState() } returns hole17State andThen hole18State
        justRun { sessionContextManager.updateScore(any(), any()) }
        justRun { sessionContextManager.updateHole(any(), any()) }

        // When
        val result = useCase.completeHoleAndAdvance(
            nextHoleNumber = 18,
            nextHolePar = 4,
            holeScore = 5
        )

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val roundState = result.getOrNull()
        assertEquals("Should be on hole 18", 18, roundState?.currentHole)
        assertEquals("Holes completed should be 17", 17, roundState?.holesCompleted)
    }
}
