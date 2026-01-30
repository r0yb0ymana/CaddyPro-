package caddypro.data.caddy.repository

import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.domain.caddy.repositories.MissPatternRepository
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.MissPattern
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder implementation of MissPatternRepository for MVP.
 *
 * Delegates to NavCaddyRepository until miss pattern logic is fully extracted.
 * This allows PinSeekerEngine to use a clean domain interface while the underlying
 * storage remains in NavCaddyRepository.
 *
 * Future: Extract miss pattern storage to dedicated tables and migrate logic here.
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 8
 *
 * @see MissPatternRepository
 * @see NavCaddyRepository
 */
@Singleton
class MissPatternRepositoryImpl @Inject constructor(
    private val navCaddyRepository: NavCaddyRepository
) : MissPatternRepository {

    override fun getMissPatterns(): Flow<List<MissPattern>> {
        return navCaddyRepository.getMissPatterns()
    }

    override fun getDominantMiss(): Flow<MissDirection?> {
        return navCaddyRepository.getMissPatterns().map { patterns ->
            patterns
                .maxByOrNull { it.decayedConfidence(System.currentTimeMillis()) }
                ?.direction
        }
    }

    override fun getPatternsByClub(clubId: String): Flow<List<MissPattern>> {
        return navCaddyRepository.getPatternsByClub(clubId)
    }

    override suspend fun updatePattern(pattern: MissPattern) {
        navCaddyRepository.updatePattern(pattern)
    }

    override suspend fun deleteStalePatterns(): Int {
        return navCaddyRepository.deleteStalePatterns()
    }
}
