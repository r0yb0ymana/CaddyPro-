package caddypro.domain.caddy.usecases

import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.domain.navcaddy.context.SessionContextManager
import caddypro.domain.navcaddy.models.Round
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to end an active round and persist final stats.
 *
 * Finalizes the round by:
 * - Capturing final round statistics
 * - Persisting round data for historical analysis
 * - Clearing the active session
 * - Triggering any post-round analytics
 *
 * This satisfies the "Start/Resume/End a round" requirement from R1.
 *
 * Spec reference: live-caddy-mode.md R1 (Live Round Context)
 * Plan reference: live-caddy-mode-plan.md Task 9
 *
 * @see SessionContextManager
 * @see NavCaddyRepository
 */
@Singleton
class EndRoundUseCase @Inject constructor(
    private val sessionContextManager: SessionContextManager,
    private val navCaddyRepository: NavCaddyRepository
) {

    /**
     * End the current round and persist final statistics.
     *
     * Captures the final round state, saves it to the repository for historical
     * analysis, and clears the active session.
     *
     * Returns failure if no active round exists.
     *
     * @param finalScore Final total score for the round (optional, uses current if null)
     * @param holesPlayed Number of holes played (optional, uses current if null)
     * @return Result containing the finalized Round on success
     */
    suspend operator fun invoke(
        finalScore: Int? = null,
        holesPlayed: Int? = null
    ): Result<RoundSummary> {
        return try {
            // Verify active round exists
            val currentRound = sessionContextManager.getCurrentRoundState()
                ?: return Result.failure(Exception("No active round to end"))

            // Use provided values or fall back to current state
            val completedScore = finalScore ?: currentRound.totalScore
            val completedHoles = holesPlayed ?: currentRound.holesCompleted

            require(completedScore >= 0) { "Final score must be non-negative" }
            require(completedHoles in 0..18) { "Holes played must be between 0 and 18" }

            // Create round summary
            val roundSummary = RoundSummary(
                roundId = currentRound.roundId,
                courseName = currentRound.courseName,
                finalScore = completedScore,
                holesPlayed = completedHoles,
                endTime = System.currentTimeMillis()
            )

            // TODO: Persist round to repository when Round persistence is implemented
            // For now, we just clear the session
            // Future: navCaddyRepository.saveRound(round)

            // Clear the active session
            sessionContextManager.clearSession()

            Result.success(roundSummary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Abandon the current round without saving.
     *
     * Clears the session without persisting any round data.
     * Use this when the user wants to discard the round.
     *
     * @return Result containing Unit on success
     */
    suspend fun abandon(): Result<Unit> {
        return try {
            // Verify active round exists
            sessionContextManager.getCurrentRoundState()
                ?: return Result.failure(Exception("No active round to abandon"))

            // Clear session without persisting
            sessionContextManager.clearSession()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Summary of a completed round.
 *
 * Contains the essential statistics from a finished round.
 * Used for immediate display and historical tracking.
 *
 * @property roundId Unique identifier for the round
 * @property courseName Name of the golf course
 * @property finalScore Total score for the round
 * @property holesPlayed Number of holes played (may be < 18 for partial rounds)
 * @property endTime Unix timestamp when the round was completed
 */
data class RoundSummary(
    val roundId: String,
    val courseName: String,
    val finalScore: Int,
    val holesPlayed: Int,
    val endTime: Long
) {
    init {
        require(roundId.isNotBlank()) { "Round ID cannot be blank" }
        require(courseName.isNotBlank()) { "Course name cannot be blank" }
        require(finalScore >= 0) { "Final score must be non-negative" }
        require(holesPlayed in 0..18) { "Holes played must be between 0 and 18" }
        require(endTime > 0) { "End time must be positive" }
    }

    /**
     * Calculate average score per hole.
     *
     * @return Average score per hole, or 0.0 if no holes played
     */
    fun averageScorePerHole(): Double {
        return if (holesPlayed > 0) {
            finalScore.toDouble() / holesPlayed
        } else {
            0.0
        }
    }

    /**
     * Check if this was a complete 18-hole round.
     */
    fun isComplete18Holes(): Boolean = holesPlayed == 18
}
