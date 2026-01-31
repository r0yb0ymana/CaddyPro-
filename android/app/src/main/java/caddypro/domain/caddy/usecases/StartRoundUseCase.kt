package caddypro.domain.caddy.usecases

import caddypro.domain.caddy.models.Location
import caddypro.domain.navcaddy.context.RoundState
import caddypro.domain.navcaddy.context.SessionContextManager
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to start a new golf round.
 *
 * Initializes the round state with course information, starting hole,
 * and tee position. Updates the session context manager with the new round.
 *
 * This satisfies the "Start/Resume/End a round" requirement from R1.
 *
 * Spec reference: live-caddy-mode.md R1 (Live Round Context)
 * Plan reference: live-caddy-mode-plan.md Task 9
 *
 * @see SessionContextManager
 * @see RoundState
 */
@Singleton
class StartRoundUseCase @Inject constructor(
    private val sessionContextManager: SessionContextManager
) {

    /**
     * Start a new round.
     *
     * Initializes round state with course name, starting hole, and tee position.
     * Clears any existing conversation history while preserving the new round state.
     *
     * @param courseName Name of the golf course
     * @param location Geographic location of the course (for weather)
     * @param startingHole Hole number to start at (default: 1)
     * @param startingPar Par for the starting hole (default: 4)
     * @return Result containing the created RoundState on success
     */
    suspend operator fun invoke(
        courseName: String,
        location: Location,
        startingHole: Int = 1,
        startingPar: Int = 4
    ): Result<RoundState> {
        return try {
            require(courseName.isNotBlank()) { "Course name cannot be blank" }
            require(startingHole in 1..18) { "Starting hole must be between 1 and 18" }
            require(startingPar in 3..5) { "Starting par must be between 3 and 5" }

            // Generate unique round ID
            val roundId = UUID.randomUUID().toString()

            // Create initial round state
            val roundState = RoundState(
                roundId = roundId,
                courseName = courseName,
                currentHole = startingHole,
                currentPar = startingPar,
                totalScore = 0,
                holesCompleted = 0,
                conditions = null  // Will be updated with weather
            )

            // Update session context manager
            sessionContextManager.updateRound(
                roundId = roundId,
                courseName = courseName,
                startingHole = startingHole,
                startingPar = startingPar
            )

            // Clear conversation history for new round
            sessionContextManager.clearConversationHistory()

            Result.success(roundState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Resume an existing round.
     *
     * Restores round state from saved session data. Useful for app backgrounding
     * and state restoration (PWA safe state restoration per R1).
     *
     * @param roundId Unique identifier for the round to resume
     * @param courseName Name of the golf course
     * @param currentHole Current hole being played
     * @param currentPar Par for the current hole
     * @param totalScore Running total score
     * @param holesCompleted Number of holes completed so far
     * @return Result containing the resumed RoundState
     */
    suspend fun resume(
        roundId: String,
        courseName: String,
        currentHole: Int,
        currentPar: Int,
        totalScore: Int,
        holesCompleted: Int
    ): Result<RoundState> {
        return try {
            require(roundId.isNotBlank()) { "Round ID cannot be blank" }
            require(courseName.isNotBlank()) { "Course name cannot be blank" }
            require(currentHole in 1..18) { "Current hole must be between 1 and 18" }
            require(currentPar in 3..5) { "Current par must be between 3 and 5" }
            require(holesCompleted in 0..18) { "Holes completed must be between 0 and 18" }
            require(totalScore >= 0) { "Total score must be non-negative" }

            // Create round state with saved data
            val roundState = RoundState(
                roundId = roundId,
                courseName = courseName,
                currentHole = currentHole,
                currentPar = currentPar,
                totalScore = totalScore,
                holesCompleted = holesCompleted,
                conditions = null
            )

            // Update session context manager
            sessionContextManager.updateRound(
                roundId = roundId,
                courseName = courseName,
                startingHole = currentHole,
                startingPar = currentPar
            )

            sessionContextManager.updateScore(
                totalScore = totalScore,
                holesCompleted = holesCompleted
            )

            Result.success(roundState)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
