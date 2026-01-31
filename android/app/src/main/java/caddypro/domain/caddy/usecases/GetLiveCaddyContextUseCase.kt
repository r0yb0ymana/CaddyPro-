package caddypro.domain.caddy.usecases

import caddypro.data.caddy.repository.ReadinessRepository
import caddypro.data.caddy.repository.WeatherRepository
import caddypro.domain.caddy.models.CourseHole
import caddypro.domain.caddy.models.HoleStrategy
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.WeatherData
import caddypro.domain.caddy.services.PinSeekerEngine
import caddypro.domain.navcaddy.context.RoundState
import caddypro.domain.navcaddy.context.SessionContextManager
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case that aggregates all live caddy context for the HUD.
 *
 * Combines current weather, readiness score, hole strategy, and round state
 * into a single context object for the Live Caddy Mode UI.
 *
 * This is the primary orchestration use case that coordinates:
 * - Weather from WeatherRepository (R2 Forecaster HUD)
 * - Readiness from ReadinessRepository (R3 BodyCaddy)
 * - Hole strategy from PinSeekerEngine (R4 PinSeeker AI Map)
 * - Round state from SessionContextManager (R1 Live Round Context)
 *
 * Spec reference: live-caddy-mode.md R1, R2, R3, R4
 * Plan reference: live-caddy-mode-plan.md Task 9
 * Acceptance criteria: A1, A2, A3 (indirect - provides context for HUD)
 *
 * @see LiveCaddyContext
 * @see WeatherRepository
 * @see ReadinessRepository
 * @see PinSeekerEngine
 * @see SessionContextManager
 */
@Singleton
class GetLiveCaddyContextUseCase @Inject constructor(
    private val sessionContextManager: SessionContextManager,
    private val weatherRepository: WeatherRepository,
    private val readinessRepository: ReadinessRepository,
    private val pinSeekerEngine: PinSeekerEngine
) {

    /**
     * Get current live caddy context.
     *
     * Aggregates all data sources required for the Live Caddy HUD:
     * - Round state (current hole, shot count)
     * - Real-time weather
     * - Current readiness score
     * - Hole strategy with hazard awareness
     *
     * Returns failure if no active round exists.
     * Weather and readiness gracefully degrade if unavailable.
     *
     * @return Result containing LiveCaddyContext on success, error if no active round
     */
    suspend operator fun invoke(): Result<LiveCaddyContext> {
        // Verify active round exists
        val roundState = sessionContextManager.getCurrentRoundState()
            ?: return Result.failure(Exception("No active round"))

        // Fetch weather (nullable if unavailable)
        val weather = weatherRepository.getCurrentWeather(
            caddypro.domain.caddy.models.Location(
                latitude = 0.0,  // TODO: Get from round/user location
                longitude = 0.0
            )
        ).getOrNull()

        // Get most recent readiness score (with default fallback)
        val readiness = readinessRepository.getMostRecent()
            ?: ReadinessScore.default()

        // Compute hole strategy (nullable if hole data unavailable)
        // TODO: Get CourseHole from course data repository when available
        // For now, strategy will be null until course data is integrated
        val holeStrategy: HoleStrategy? = null

        return Result.success(
            LiveCaddyContext(
                roundState = roundState,
                weather = weather,
                readiness = readiness,
                holeStrategy = holeStrategy
            )
        )
    }

    /**
     * Get current hole strategy for the active hole.
     *
     * Computes personalized strategy based on hole geometry, hazards,
     * player profile, and current readiness.
     *
     * @param hole Course hole data with geometry and hazards
     * @param handicap Player's handicap index
     * @return HoleStrategy with landing zones and risk callouts
     */
    suspend fun getHoleStrategy(
        hole: CourseHole,
        handicap: Int
    ): HoleStrategy {
        val readiness = readinessRepository.getMostRecent()
            ?: ReadinessScore.default()

        return pinSeekerEngine.computeStrategy(
            hole = hole,
            handicap = handicap,
            readinessScore = readiness
        )
    }
}

/**
 * Aggregated context for Live Caddy Mode.
 *
 * Combines all real-time data sources required for on-course decision support.
 *
 * @property roundState Current round state (hole, score, course)
 * @property weather Current weather conditions (null if unavailable)
 * @property readiness Current readiness score (affects strategy conservatism)
 * @property holeStrategy Personalized hole strategy (null if hole data unavailable)
 */
data class LiveCaddyContext(
    val roundState: RoundState,
    val weather: WeatherData?,
    val readiness: ReadinessScore,
    val holeStrategy: HoleStrategy?
)

// ReadinessScore.default() is now defined in the ReadinessScore companion object
