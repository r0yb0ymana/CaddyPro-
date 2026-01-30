package caddypro.data.caddy.repository

import caddypro.domain.navcaddy.models.BagProfile
import caddypro.domain.navcaddy.models.Club
import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.domain.caddy.repositories.ClubBagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Placeholder implementation of ClubBagRepository for MVP.
 *
 * Delegates to NavCaddyRepository until bag logic is fully extracted.
 * This allows PinSeekerEngine to use a clean domain interface while the underlying
 * storage remains in NavCaddyRepository.
 *
 * Future: Extract bag profile storage to dedicated tables and migrate logic here.
 *
 * Spec reference: live-caddy-mode.md R4 (PinSeeker AI Map)
 * Plan reference: live-caddy-mode-plan.md Task 8
 *
 * @see ClubBagRepository
 * @see NavCaddyRepository
 */
@Singleton
class ClubBagRepositoryImpl @Inject constructor(
    private val navCaddyRepository: NavCaddyRepository
) : ClubBagRepository {

    override fun getActiveBag(): Flow<BagProfile?> {
        return navCaddyRepository.getActiveBag()
    }

    override fun getActiveBagClubs(): Flow<List<Club>> {
        return navCaddyRepository.getActiveBag().flatMapLatest { bag ->
            if (bag != null) {
                navCaddyRepository.getClubsForBag(bag.id)
            } else {
                flowOf(emptyList())
            }
        }
    }

    override fun getClubDistances(): Flow<Map<String, Int>> {
        return navCaddyRepository.getActiveBag().flatMapLatest { bag ->
            if (bag != null) {
                navCaddyRepository.getClubsForBag(bag.id).map { clubs ->
                    clubs.associate { it.name to it.estimatedCarry }
                }
            } else {
                flowOf(emptyMap())
            }
        }
    }

    override fun getClubById(clubId: String): Flow<Club?> {
        return navCaddyRepository.getActiveBag().flatMapLatest { bag ->
            if (bag != null) {
                navCaddyRepository.getClubsForBag(bag.id).map { clubs ->
                    clubs.find { it.id == clubId }
                }
            } else {
                flowOf(null)
            }
        }
    }

    override fun getAllBags(): Flow<List<BagProfile>> {
        return navCaddyRepository.getAllBags()
    }

    override suspend fun switchActiveBag(bagId: String) {
        navCaddyRepository.switchActiveBag(bagId)
    }
}
