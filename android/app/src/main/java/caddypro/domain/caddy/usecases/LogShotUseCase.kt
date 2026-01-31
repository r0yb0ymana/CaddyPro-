package caddypro.domain.caddy.usecases

import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.domain.navcaddy.context.SessionContextManager
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.PressureContext
import caddypro.domain.navcaddy.models.Shot
import caddypro.domain.navcaddy.models.Club
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to log a shot during an active round.
 *
 * Records shot details including club, lie, and miss direction to the repository.
 * This feeds both the round history and downstream analytics for miss pattern analysis.
 *
 * Provides one-second logging flow for real-time shot tracking during play.
 * Supports offline-first functionality by writing to local database immediately.
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
 * Plan reference: live-caddy-mode-plan.md Task 9 step 2
 * Acceptance criteria: A4 (Shot logger speed and persistence)
 *
 * @see NavCaddyRepository
 * @see SessionContextManager
 * @see ShotResult
 */
@Singleton
class LogShotUseCase @Inject constructor(
    private val navCaddyRepository: NavCaddyRepository,
    private val sessionContextManager: SessionContextManager
) {

    /**
     * Log a shot with club and result details.
     *
     * Records the shot to the repository with automatic hole context from the
     * current round state. Shot is persisted locally for offline-first support
     * and will sync automatically when connection returns.
     *
     * Pressure context is null for manual logging (can be inferred by downstream analytics).
     *
     * @param club The club used for the shot
     * @param result The shot result containing lie and optional miss direction
     * @return Result containing Unit on success, error if no active round
     */
    suspend operator fun invoke(
        club: Club,
        result: ShotResult
    ): Result<Unit> {
        return try {
            // Verify active round exists to get hole context
            val roundState = sessionContextManager.getCurrentRoundState()
                ?: return Result.failure(Exception("No active round"))

            // Create shot with current timestamp and hole context
            val shot = Shot(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                club = club,
                lie = result.lie,
                missDirection = result.missDirection,
                pressureContext = PressureContext(), // No pressure for manual logging
                holeNumber = roundState.currentHole,
                notes = null
            )

            // Record to repository (offline-first)
            navCaddyRepository.recordShot(shot)

            // TODO: Track analytics event when analytics framework is available
            // analytics.trackEvent(AnalyticsEvent.ShotLogged(
            //     clubType = club.type.name,
            //     lie = result.lie.name,
            //     latencyMs = 0  // Instant logging
            // ))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Shot result data for logging.
 *
 * Contains the essential information captured during the one-second logging flow.
 * Designed for minimal friction during play:
 * 1. Select club (tap)
 * 2. Select result (fairway/rough/bunker/etc.)
 * 3. Optional: Select miss direction modifier
 *
 * Spec reference: live-caddy-mode.md R6
 * Plan reference: live-caddy-mode-plan.md Task 9 step 2
 *
 * @property lie Where the ball ended up (fairway, rough, bunker, water, OB, green)
 * @property missDirection Optional directional modifier (left, right, short, long)
 */
data class ShotResult(
    val lie: Lie,
    val missDirection: MissDirection? = null
) {
    init {
        require(lie != Lie.HAZARD || missDirection != null) {
            "Hazard shots must specify miss direction for analytics (water/OB direction)"
        }
    }
}
