package caddypro.domain.navcaddy.memory

import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.PressureContext
import caddypro.domain.navcaddy.models.Shot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Records shots with miss information for pattern analysis.
 *
 * Provides methods to log shots and retrieve shot history.
 * Manual entry only for MVP (Q3 resolution).
 *
 * Spec reference: navcaddy-engine.md R5, Q3 (manual entry only)
 */
@Singleton
class ShotRecorder @Inject constructor(
    private val repository: NavCaddyRepository
) {
    /**
     * Record a shot with miss information.
     *
     * Shot is persisted to the database for pattern analysis.
     * Automatically enforces retention policy on insert.
     *
     * @param shot The shot to record
     */
    suspend fun recordShot(shot: Shot) {
        repository.recordShot(shot)
    }

    /**
     * Record a shot with individual parameters.
     *
     * Convenience method for creating and recording a shot in one call.
     *
     * @param club Club used for the shot
     * @param missDirection Direction of miss (null if straight)
     * @param lie Ball lie before the shot
     * @param pressureContext Pressure context for the shot
     * @param holeNumber Hole number if shot was during a round (optional)
     * @param notes Optional user notes about the shot
     * @return The recorded shot instance
     */
    suspend fun recordShot(
        club: Club,
        missDirection: MissDirection?,
        lie: Lie,
        pressureContext: PressureContext,
        holeNumber: Int? = null,
        notes: String? = null
    ): Shot {
        val shot = Shot(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            club = club,
            missDirection = missDirection,
            lie = lie,
            pressureContext = pressureContext,
            holeNumber = holeNumber,
            notes = notes
        )
        recordShot(shot)
        return shot
    }

    /**
     * Get recent shots within specified days.
     *
     * @param days Number of days to look back (default 90 per Q5)
     * @return Flow of recent shots ordered by timestamp descending
     */
    fun getRecentShots(days: Int = 90): Flow<List<Shot>> {
        return repository.getRecentShots(days)
    }

    /**
     * Get the N most recent shots.
     *
     * @param count Maximum number of shots to retrieve
     * @return List of recent shots ordered by timestamp descending
     */
    suspend fun getRecentShots(count: Int): List<Shot> {
        return repository.getRecentShots()
            .first()
            .take(count)
    }

    /**
     * Get shots for a specific club.
     *
     * @param clubId The club ID to filter by
     * @return Flow of shots for the club ordered by timestamp descending
     */
    fun getShotsByClub(clubId: String): Flow<List<Shot>> {
        return repository.getShotsByClub(clubId)
    }

    /**
     * Get the N most recent shots for a specific club.
     *
     * @param clubId The club ID to filter by
     * @param limit Maximum number of shots to retrieve
     * @return List of club shots ordered by timestamp descending
     */
    suspend fun getShotsByClub(clubId: String, limit: Int): List<Shot> {
        return repository.getShotsByClub(clubId)
            .first()
            .take(limit)
    }

    /**
     * Get shots with pressure context.
     *
     * @return Flow of pressure shots ordered by timestamp descending
     */
    fun getShotsWithPressure(): Flow<List<Shot>> {
        return repository.getShotsWithPressure()
    }

    /**
     * Get shots with pressure context for a specific club.
     *
     * @param clubId The club ID to filter by
     * @param limit Maximum number of shots to retrieve (0 = unlimited)
     * @return List of pressure shots for the club
     */
    suspend fun getPressureShotsByClub(clubId: String, limit: Int = 0): List<Shot> {
        val shots = repository.getShotsByClub(clubId)
            .first()
            .filter { it.pressureContext.hasPressure }

        return if (limit > 0) shots.take(limit) else shots
    }

    /**
     * Clear all shot history.
     *
     * Part of user-requested memory wipe (C4).
     */
    suspend fun clearHistory() {
        repository.clearMemory()
    }

    /**
     * Enforce retention policy by deleting old shots.
     *
     * @return Number of shots deleted
     */
    suspend fun enforceRetentionPolicy(): Int {
        return repository.enforceRetentionPolicy()
    }
}
