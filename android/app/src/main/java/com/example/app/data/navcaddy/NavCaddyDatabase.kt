package caddypro.data.navcaddy

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import caddypro.data.caddy.local.dao.ReadinessScoreDao
import caddypro.data.caddy.local.entities.ReadinessScoreEntity
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
 * bag profiles, club data, distance audit trail, and readiness scores to support
 * contextual memory, personalization, bag management, and live caddy mode.
 *
 * Spec reference: navcaddy-engine.md R5, R6, C4, C5
 *                 player-profile-bag-management.md R1, R2, R3
 *                 live-caddy-mode.md R3 (BodyCaddy)
 *
 * Database version history:
 * - Version 1: Initial schema with shots, miss_patterns, sessions, conversation_turns
 * - Version 2: Added bag_profiles, bag_clubs, distance_audits for bag management
 * - Version 3: Added readiness_scores for Live Caddy mode BodyCaddy feature
 */
@Database(
    entities = [
        ShotEntity::class,
        MissPatternEntity::class,
        SessionEntity::class,
        ConversationTurnEntity::class,
        BagProfileEntity::class,
        BagClubEntity::class,
        DistanceAuditEntity::class,
        ReadinessScoreEntity::class
    ],
    version = 3,
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

    /**
     * DAO for readiness score operations.
     */
    abstract fun readinessScoreDao(): ReadinessScoreDao

    companion object {
        /** Database name */
        const val DATABASE_NAME = "navcaddy_database"

        /**
         * Migration from version 2 to 3: Add readiness_scores table.
         *
         * Creates the readiness_scores table for Live Caddy mode BodyCaddy feature.
         * Stores historical readiness data for trend analysis and offline access.
         *
         * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
         * Plan reference: live-caddy-mode-plan.md Task 10
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS readiness_scores (
                        timestamp INTEGER PRIMARY KEY NOT NULL,
                        overall INTEGER NOT NULL,
                        hrv_score REAL,
                        sleep_score REAL,
                        stress_score REAL,
                        source TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                // Create index on timestamp for efficient queries
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_readiness_scores_timestamp ON readiness_scores(timestamp)"
                )
            }
        }
    }
}
