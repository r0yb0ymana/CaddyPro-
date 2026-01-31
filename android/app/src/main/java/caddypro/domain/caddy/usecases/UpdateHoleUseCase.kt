package caddypro.domain.caddy.usecases

import caddypro.domain.navcaddy.context.RoundState
import caddypro.domain.navcaddy.context.SessionContextManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to update the current hole during an active round.
 *
 * Moves to the next hole and updates the round state accordingly.
 * Optionally updates the score and holes completed count.
 *
 * This satisfies the "Maintain current hole, tee position, and shot count context"
 * requirement from R1.
 *
 * Spec reference: live-caddy-mode.md R1 (Live Round Context)
 * Plan reference: live-caddy-mode-plan.md Task 9
 *
 * @see SessionContextManager
 * @see RoundState
 */
@Singleton
class UpdateHoleUseCase @Inject constructor(
    private val sessionContextManager: SessionContextManager
) {

    /**
     * Move to the next hole.
     *
     * Updates the current hole number and par in the session context.
     * Typically called when completing a hole and moving to the next.
     *
     * @param holeNumber New hole number (1-18)
     * @param par Par for the new hole (3-5)
     * @return Result containing updated RoundState on success, error if no active round
     */
    suspend operator fun invoke(
        holeNumber: Int,
        par: Int
    ): Result<RoundState> {
        return try {
            require(holeNumber in 1..18) { "Hole number must be between 1 and 18" }
            require(par in 3..5) { "Par must be between 3 and 5" }

            // Verify active round exists
            val currentRound = sessionContextManager.getCurrentRoundState()
                ?: return Result.failure(Exception("No active round"))

            // Update hole in session manager
            sessionContextManager.updateHole(
                holeNumber = holeNumber,
                par = par
            )

            // Get updated round state
            val updatedRound = sessionContextManager.getCurrentRoundState()
                ?: return Result.failure(Exception("Failed to update round state"))

            Result.success(updatedRound)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Complete the current hole and move to the next.
     *
     * Updates hole number, par, total score, and holes completed count.
     * Automatically increments holes completed.
     *
     * @param nextHoleNumber Next hole number (1-18)
     * @param nextHolePar Par for the next hole (3-5)
     * @param holeScore Score for the hole just completed
     * @return Result containing updated RoundState on success
     */
    suspend fun completeHoleAndAdvance(
        nextHoleNumber: Int,
        nextHolePar: Int,
        holeScore: Int
    ): Result<RoundState> {
        return try {
            require(nextHoleNumber in 1..18) { "Next hole number must be between 1 and 18" }
            require(nextHolePar in 3..5) { "Next hole par must be between 3 and 5" }
            require(holeScore > 0) { "Hole score must be positive" }

            // Verify active round exists
            val currentRound = sessionContextManager.getCurrentRoundState()
                ?: return Result.failure(Exception("No active round"))

            // Update score and holes completed
            val newTotalScore = currentRound.totalScore + holeScore
            val newHolesCompleted = currentRound.holesCompleted + 1

            sessionContextManager.updateScore(
                totalScore = newTotalScore,
                holesCompleted = newHolesCompleted
            )

            // Move to next hole
            sessionContextManager.updateHole(
                holeNumber = nextHoleNumber,
                par = nextHolePar
            )

            // Get updated round state
            val updatedRound = sessionContextManager.getCurrentRoundState()
                ?: return Result.failure(Exception("Failed to update round state"))

            Result.success(updatedRound)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update just the score without advancing to next hole.
     *
     * Useful for mid-hole updates or score corrections.
     *
     * @param additionalStrokes Strokes to add to total score
     * @return Result containing updated RoundState on success
     */
    suspend fun updateScore(
        additionalStrokes: Int
    ): Result<RoundState> {
        return try {
            require(additionalStrokes > 0) { "Additional strokes must be positive" }

            // Verify active round exists
            val currentRound = sessionContextManager.getCurrentRoundState()
                ?: return Result.failure(Exception("No active round"))

            // Update score
            val newTotalScore = currentRound.totalScore + additionalStrokes

            sessionContextManager.updateScore(
                totalScore = newTotalScore,
                holesCompleted = currentRound.holesCompleted
            )

            // Get updated round state
            val updatedRound = sessionContextManager.getCurrentRoundState()
                ?: return Result.failure(Exception("Failed to update round state"))

            Result.success(updatedRound)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
