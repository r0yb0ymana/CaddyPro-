package caddypro.data.navcaddy

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import caddypro.data.caddy.local.dao.ReadinessScoreDao
import caddypro.data.caddy.local.dao.SyncQueueDao
import caddypro.data.caddy.local.entities.ReadinessScoreEntity
import caddypro.data.caddy.local.entities.SyncQueueEntity
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
 * bag profiles, club data, distance audit trail, readiness scores, and
 * offline sync queue to support contextual memory, personalization,
 * bag management, live caddy mode, and offline-first functionality.
 *
 * Spec reference: navcaddy-engine.md R5, R6, C4, C5
 *                 player-profile-bag-management.md R1, R2, R3
 *                 live-caddy-mode.md R3 (BodyCaddy), R6 (Real-Time Shot Logger)
 *
 * Database version history:
 * - Version 1: Initial schema with shots, miss_patterns, sessions, conversation_turns
 * - Version 2: Added bag_profiles, bag_clubs, distance_audits for bag management
 * - Version 3: Added readiness_scores for Live Caddy mode BodyCaddy feature
 * - Version 4: Added sync_queue for offline-first shot logging
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
        ReadinessScoreEntity::class,
        SyncQueueEntity::class
    ],
    version = 4,
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

    /**
     * DAO for offline sync queue operations.
     */
    abstract fun syncQueueDao(): SyncQueueDao

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

        /**
         * Migration from version 3 to 4: Add sync_queue table.
         *
         * Creates the sync_queue table for offline-first shot logging with
         * background sync when connectivity returns.
         *
         * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger)
         * Plan reference: live-caddy-mode-plan.md Task 19
         * Acceptance criteria: A4 (Shot logger persistence)
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_queue (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        operation_type TEXT NOT NULL,
                        payload TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        retry_count INTEGER NOT NULL DEFAULT 0,
                        last_attempt_timestamp INTEGER,
                        error_message TEXT
                    )
                    """.trimIndent()
                )

                // Create index on status for efficient pending operation queries
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sync_queue_status ON sync_queue(status)"
                )

                // Create index on timestamp for FIFO ordering
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sync_queue_timestamp ON sync_queue(timestamp)"
                )
            }
        }
    }
}
