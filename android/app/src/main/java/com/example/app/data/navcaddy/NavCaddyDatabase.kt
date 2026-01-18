package caddypro.data.navcaddy

import androidx.room.Database
import androidx.room.RoomDatabase
import caddypro.data.navcaddy.dao.BagClubDao
import caddypro.data.navcaddy.dao.BagProfileDao
import caddypro.data.navcaddy.dao.DistanceAuditDao
import caddypro.data.navcaddy.dao.MissPatternDao
import caddypro.data.navcaddy.dao.SessionDao
import caddypro.data.navcaddy.dao.ShotDao
import caddypro.data.navcaddy.entities.BagClubEntity
import caddypro.data.navcaddy.entities.BagProfileEntity
import caddypro.data.navcaddy.entities.ConversationTurnEntity
import caddypro.data.navcaddy.entities.DistanceAuditEntity
import caddypro.data.navcaddy.entities.MissPatternEntity
import caddypro.data.navcaddy.entities.SessionEntity
import caddypro.data.navcaddy.entities.ShotEntity

/**
 * Room database for NavCaddy engine persistence.
 *
 * Stores shots, miss patterns, session context, conversation history,
 * bag profiles, club data, and distance audit trail to support
 * contextual memory, personalization, and bag management.
 *
 * Spec reference: navcaddy-engine.md R5, R6, C4, C5
 *                 player-profile-bag-management.md R1, R2, R3
 *
 * Database version history:
 * - Version 1: Initial schema with shots, miss_patterns, sessions, conversation_turns
 * - Version 2: Added bag_profiles, bag_clubs, distance_audits for bag management
 */
@Database(
    entities = [
        ShotEntity::class,
        MissPatternEntity::class,
        SessionEntity::class,
        ConversationTurnEntity::class,
        BagProfileEntity::class,
        BagClubEntity::class,
        DistanceAuditEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class NavCaddyDatabase : RoomDatabase() {
    /**
     * DAO for shot operations.
     */
    abstract fun shotDao(): ShotDao

    /**
     * DAO for miss pattern operations.
     */
    abstract fun missPatternDao(): MissPatternDao

    /**
     * DAO for session and conversation history operations.
     */
    abstract fun sessionDao(): SessionDao

    /**
     * DAO for bag profile operations.
     */
    abstract fun bagProfileDao(): BagProfileDao

    /**
     * DAO for bag-club association operations.
     */
    abstract fun bagClubDao(): BagClubDao

    /**
     * DAO for distance audit trail operations.
     */
    abstract fun distanceAuditDao(): DistanceAuditDao

    companion object {
        /** Database name */
        const val DATABASE_NAME = "navcaddy_database"
    }
}
