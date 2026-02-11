package com.caddypro.app.domain.repository

import com.caddypro.app.domain.model.Shot
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Shot operations
 */
interface ShotRepository {
    fun getShotsByRoundId(roundId: String): Flow<List<Shot>>
    fun getShotsByHole(roundId: String, holeNumber: Int): Flow<List<Shot>>
    suspend fun getShotCountForHole(roundId: String, holeNumber: Int): Int
    suspend fun getTotalShotCount(roundId: String): Int
    suspend fun logShot(shot: Shot): Result<Unit>
    suspend fun updateShot(shot: Shot): Result<Unit>
    suspend fun deleteShot(shotId: String): Result<Unit>
    suspend fun getLastShot(roundId: String): Shot?
}
