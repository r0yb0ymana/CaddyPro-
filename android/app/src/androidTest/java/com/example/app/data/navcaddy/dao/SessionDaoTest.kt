package caddypro.data.navcaddy.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import caddypro.data.navcaddy.NavCaddyDatabase
import caddypro.data.navcaddy.entities.ConversationTurnEntity
import caddypro.data.navcaddy.entities.SessionEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for SessionDao using Room in-memory database.
 *
 * Verifies session persistence and conversation history management.
 *
 * Spec reference: navcaddy-engine.md R6
 */
@RunWith(AndroidJUnit4::class)
class SessionDaoTest {

    private lateinit var database: NavCaddyDatabase
    private lateinit var sessionDao: SessionDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            NavCaddyDatabase::class.java
        ).build()
        sessionDao = database.sessionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ========================================================================
    // Session Tests
    // ========================================================================

    @Test
    fun saveSession_persistsData() = runTest {
        // Given
        val session = SessionEntity(
            id = "current",
            roundId = "round-123",
            currentHole = 7,
            lastRecommendation = "Use 8-iron",
            updatedAt = System.currentTimeMillis()
        )

        // When
        sessionDao.saveSession(session)

        // Then
        sessionDao.getCurrentSession().test {
            val retrieved = awaitItem()
            assertNotNull(retrieved)
            assertEquals("round-123", retrieved?.roundId)
            assertEquals(7, retrieved?.currentHole)
            assertEquals("Use 8-iron", retrieved?.lastRecommendation)
            cancel()
        }
    }

    @Test
    fun saveSession_replacesExisting() = runTest {
        // Given
        val session1 = SessionEntity(
            id = "current",
            roundId = "round-1",
            currentHole = 5,
            lastRecommendation = "First",
            updatedAt = System.currentTimeMillis()
        )
        val session2 = SessionEntity(
            id = "current",
            roundId = "round-2",
            currentHole = 10,
            lastRecommendation = "Second",
            updatedAt = System.currentTimeMillis()
        )

        // When
        sessionDao.saveSession(session1)
        sessionDao.saveSession(session2)

        // Then
        sessionDao.getCurrentSession().test {
            val retrieved = awaitItem()
            assertEquals("round-2", retrieved?.roundId)
            assertEquals(10, retrieved?.currentHole)
            assertEquals("Second", retrieved?.lastRecommendation)
            cancel()
        }
    }

    @Test
    fun deleteCurrentSession_removesSession() = runTest {
        // Given
        val session = SessionEntity(
            id = "current",
            roundId = "round-123",
            currentHole = 7,
            lastRecommendation = "Test",
            updatedAt = System.currentTimeMillis()
        )
        sessionDao.saveSession(session)

        // When
        sessionDao.deleteCurrentSession()

        // Then
        sessionDao.getCurrentSession().test {
            val retrieved = awaitItem()
            assertNull(retrieved)
            cancel()
        }
    }

    // ========================================================================
    // Conversation History Tests
    // ========================================================================

    @Test
    fun insertConversationTurn_andRetrieve() = runTest {
        // Given
        val turn = ConversationTurnEntity(
            sessionId = "current",
            role = "USER",
            content = "What club should I use?",
            timestamp = System.currentTimeMillis()
        )

        // When
        sessionDao.insertConversationTurn(turn)

        // Then
        sessionDao.getConversationHistory().test {
            val history = awaitItem()
            assertEquals(1, history.size)
            assertEquals("USER", history[0].role)
            assertEquals("What club should I use?", history[0].content)
            cancel()
        }
    }

    @Test
    fun getConversationHistory_respectsLimit() = runTest {
        // Given - insert 15 turns
        val baseTime = System.currentTimeMillis()
        repeat(15) { index ->
            sessionDao.insertConversationTurn(
                ConversationTurnEntity(
                    sessionId = "current",
                    role = if (index % 2 == 0) "USER" else "ASSISTANT",
                    content = "Turn $index",
                    timestamp = baseTime + (index * 1000L)
                )
            )
        }

        // When - request limit of 10
        sessionDao.getConversationHistory(limit = 10).test {
            val history = awaitItem()

            // Then
            assertEquals(10, history.size)
            // Should get most recent 10 (turns 5-14)
            assertEquals("Turn 14", history[0].content) // Most recent first
            cancel()
        }
    }

    @Test
    fun trimConversationHistory_keepsOnlyRecentTurns() = runTest {
        // Given - insert 20 turns
        val baseTime = System.currentTimeMillis()
        repeat(20) { index ->
            sessionDao.insertConversationTurn(
                ConversationTurnEntity(
                    sessionId = "current",
                    role = "USER",
                    content = "Turn $index",
                    timestamp = baseTime + (index * 1000L)
                )
            )
        }

        // When - trim to keep only 5 most recent
        sessionDao.trimConversationHistory(keepCount = 5)

        // Then
        sessionDao.getConversationHistory(limit = 100).test {
            val history = awaitItem()
            assertEquals(5, history.size)
            // Should have turns 15-19
            assertEquals("Turn 19", history[0].content)
            assertEquals("Turn 15", history[4].content)
            cancel()
        }
    }

    @Test
    fun deleteConversationHistory_clearsAllTurns() = runTest {
        // Given
        repeat(5) { index ->
            sessionDao.insertConversationTurn(
                ConversationTurnEntity(
                    sessionId = "current",
                    role = "USER",
                    content = "Turn $index",
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        // When
        sessionDao.deleteConversationHistory()

        // Then
        sessionDao.getConversationHistory().test {
            val history = awaitItem()
            assertEquals(0, history.size)
            cancel()
        }
    }

    @Test
    fun conversationHistory_isolatedBySessionId() = runTest {
        // Given - turns for different sessions
        sessionDao.insertConversationTurn(
            ConversationTurnEntity(
                sessionId = "current",
                role = "USER",
                content = "Current session",
                timestamp = System.currentTimeMillis()
            )
        )
        sessionDao.insertConversationTurn(
            ConversationTurnEntity(
                sessionId = "other",
                role = "USER",
                content = "Other session",
                timestamp = System.currentTimeMillis()
            )
        )

        // When
        sessionDao.getConversationHistory(sessionId = "current").test {
            val history = awaitItem()

            // Then
            assertEquals(1, history.size)
            assertEquals("Current session", history[0].content)
            cancel()
        }
    }

    @Test
    fun deleteAllConversationTurns_wipesAllSessions() = runTest {
        // Given
        sessionDao.insertConversationTurn(
            ConversationTurnEntity(
                sessionId = "current",
                role = "USER",
                content = "Turn 1",
                timestamp = System.currentTimeMillis()
            )
        )
        sessionDao.insertConversationTurn(
            ConversationTurnEntity(
                sessionId = "other",
                role = "USER",
                content = "Turn 2",
                timestamp = System.currentTimeMillis()
            )
        )

        // When
        sessionDao.deleteAllConversationTurns()

        // Then
        sessionDao.getConversationHistory(sessionId = "current").test {
            assertEquals(0, awaitItem().size)
            cancel()
        }
        sessionDao.getConversationHistory(sessionId = "other").test {
            assertEquals(0, awaitItem().size)
            cancel()
        }
    }
}
