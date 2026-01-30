package caddypro.data.navcaddy.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import caddypro.data.navcaddy.entities.ConversationTurnEntity
import caddypro.data.navcaddy.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for session and conversation history database operations.
 *
 * Spec reference: navcaddy-engine.md R6
 */
@Dao
interface SessionDao {
    /**
     * Save or update the current session.
     *
     * Uses REPLACE strategy to update the single "current" session.
     *
     * @param session The session to save
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSession(session: SessionEntity)

    /**
     * Get the current session.
     *
     * @return Flow of current session (null if not initialized)
     */
    @Query("SELECT * FROM sessions WHERE id = 'current' LIMIT 1")
    fun getCurrentSession(): Flow<SessionEntity?>

    /**
     * Get conversation history for a session.
     *
     * Returns the most recent turns up to the specified limit.
     *
     * @param sessionId The session ID (default is "current")
     * @param limit Maximum number of turns to return (default is 10 per R6)
     * @return Flow of conversation turns ordered by timestamp ascending
     */
    @Query(
        """
        SELECT * FROM conversation_turns
        WHERE session_id = :sessionId
        ORDER BY timestamp DESC
        LIMIT :limit
        """
    )
    fun getConversationHistory(
        sessionId: String = "current",
        limit: Int = 10
    ): Flow<List<ConversationTurnEntity>>

    /**
     * Insert a conversation turn.
     *
     * @param turn The conversation turn to insert
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversationTurn(turn: ConversationTurnEntity)

    /**
     * Delete conversation history for a session.
     *
     * @param sessionId The session ID to clear history for
     */
    @Query("DELETE FROM conversation_turns WHERE session_id = :sessionId")
    suspend fun deleteConversationHistory(sessionId: String = "current")

    /**
     * Delete the current session.
     *
     * Used for user-requested memory wipe (C4).
     */
    @Query("DELETE FROM sessions WHERE id = 'current'")
    suspend fun deleteCurrentSession()

    /**
     * Delete all conversation turns.
     *
     * Used for user-requested memory wipe (C4).
     */
    @Query("DELETE FROM conversation_turns")
    suspend fun deleteAllConversationTurns()

    /**
     * Trim conversation history to keep only the most recent N turns.
     *
     * @param sessionId The session ID to trim
     * @param keepCount Number of recent turns to keep
     */
    @Query(
        """
        DELETE FROM conversation_turns
        WHERE session_id = :sessionId
        AND id NOT IN (
            SELECT id FROM conversation_turns
            WHERE session_id = :sessionId
            ORDER BY timestamp DESC
            LIMIT :keepCount
        )
        """
    )
    suspend fun trimConversationHistory(sessionId: String = "current", keepCount: Int = 10)
}
