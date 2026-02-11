package com.caddypro.app.data.remote

import com.caddypro.app.data.remote.dto.BagDto
import com.caddypro.app.data.remote.dto.ClubDto
import com.caddypro.app.data.remote.dto.ProfileDto
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source for Supabase Postgrest operations
 *
 * AC3: Profile syncs to Supabase when online
 * AC18: Sync completes automatically when connectivity returns
 * AC19: Conflicting edits resolve via last-write-wins
 */
@Singleton
class SupabaseDataSource @Inject constructor(
    private val postgrest: Postgrest
) {

    // --- Profiles ---

    suspend fun upsertProfile(dto: ProfileDto) {
        postgrest.from("player_profiles").upsert(dto)
    }

    suspend fun getProfile(userId: String): ProfileDto? {
        return postgrest.from("player_profiles")
            .select {
                filter { eq("supabase_user_id", userId) }
            }
            .decodeSingleOrNull<ProfileDto>()
    }

    suspend fun deleteProfile(id: String) {
        postgrest.from("player_profiles")
            .delete { filter { eq("id", id) } }
    }

    // --- Bags ---

    suspend fun upsertBag(dto: BagDto) {
        postgrest.from("bags").upsert(dto)
    }

    suspend fun getBagsByProfileId(profileId: String): List<BagDto> {
        return postgrest.from("bags")
            .select {
                filter { eq("profile_id", profileId) }
            }
            .decodeList<BagDto>()
    }

    suspend fun deleteBag(id: String) {
        postgrest.from("bags")
            .delete { filter { eq("id", id) } }
    }

    // --- Clubs ---

    suspend fun upsertClub(dto: ClubDto) {
        postgrest.from("clubs").upsert(dto)
    }

    suspend fun upsertClubs(dtos: List<ClubDto>) {
        if (dtos.isNotEmpty()) {
            postgrest.from("clubs").upsert(dtos)
        }
    }

    suspend fun getClubsByBagId(bagId: String): List<ClubDto> {
        return postgrest.from("clubs")
            .select {
                filter { eq("bag_id", bagId) }
            }
            .decodeList<ClubDto>()
    }

    suspend fun deleteClub(id: String) {
        postgrest.from("clubs")
            .delete { filter { eq("id", id) } }
    }
}
