package com.caddypro.app.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Room entity for PlayerProfile
 *
 * Stores player's basic information and preferences.
 */
@Entity(tableName = "player_profiles")
data class PlayerProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "supabase_user_id")
    val supabaseUserId: String,

    @ColumnInfo(name = "display_name")
    val displayName: String,

    @ColumnInfo(name = "handicap_index")
    val handicapIndex: Float? = null,

    @ColumnInfo(name = "preferred_units")
    val preferredUnits: PreferredUnits = PreferredUnits.METRIC,

    @ColumnInfo(name = "home_course_id")
    val homeCourseId: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "synced")
    val synced: Boolean = false
)

/**
 * Unit preference for distance displays
 */
enum class PreferredUnits {
    METRIC,    // Metres (default for Australia)
    IMPERIAL   // Yards
}
