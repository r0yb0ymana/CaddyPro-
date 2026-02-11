package com.caddypro.app.domain.repository

import com.caddypro.app.domain.model.Round
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Round operations
 */
interface RoundRepository {
    fun getRoundsByProfileId(profileId: String): Flow<List<Round>>
    suspend fun getRoundById(id: String): Round?
    suspend fun getActiveRound(): Round?
    fun getActiveRoundFlow(): Flow<Round?>
    suspend fun createRound(round: Round): Result<Unit>
    suspend fun endRound(roundId: String): Result<Unit>
    suspend fun updateShotCount(roundId: String, totalShots: Int)
}
