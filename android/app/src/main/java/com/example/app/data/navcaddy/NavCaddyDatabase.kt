package caddypro.data.navcaddy

import androidx.room.Database
import androidx.room.RoomDatabase
import caddypro.data.navcaddy.dao.MissPatternDao
import caddypro.data.navcaddy.dao.SessionDao
import caddypro.data.navcaddy.dao.ShotDao
import caddypro.data.navcaddy.entities.ConversationTurnEntity
import caddypro.data.navcaddy.entities.MissPatternEntity
import caddypro.data.navcaddy.entities.SessionEntity
import caddypro.data.navcaddy.entities.ShotEntity

/**
 * Room database for NavCaddy engine persistence.
 *
 * Stores shots, miss patterns, session context, and conversation history
 * to support contextual memory and personalization.
 *
 * Spec reference: navcaddy-engine.md R5, R6, C4, C5
 *
 * Database version: 1
 * - Initial schema with shots, miss_patterns, sessions, conversation_turns
 */
@Database(
    entities = [
        ShotEntity::class,
        MissPatternEntity::class,
        SessionEntity::class,
        ConversationTurnEntity::class
    ],
    version = 1,
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

    companion object {
        /** Database name */
        const val DATABASE_NAME = "navcaddy_database"
    }
}
