package caddypro.data.navcaddy.repository

import app.cash.turbine.test
import caddypro.data.navcaddy.NavCaddyDatabase
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.ClubType
import caddypro.domain.navcaddy.models.ConversationTurn
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.MissPattern
import caddypro.domain.navcaddy.models.PressureContext
import caddypro.domain.navcaddy.models.Role
import caddypro.domain.navcaddy.models.SessionContext
import caddypro.domain.navcaddy.models.Shot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NavCaddyRepositoryImpl.
 *
 * Uses MockK to test repository logic without a real database.
 */
class NavCaddyRepositoryTest {

    private lateinit var database: NavCaddyDatabase
    private lateinit var repository: NavCaddyRepositoryImpl

    private val testClub = Club(id = "club-7i", name = "7-iron", type = ClubType.IRON, estimatedCarry = 150)
    private val testShot = Shot(
        id = "shot-1",
        timestamp = System.currentTimeMillis(),
        club = testClub,
        missDirection = MissDirection.SLICE,
        lie = Lie.FAIRWAY,
        pressureContext = PressureContext(isUserTagged = true),
        holeNumber = 14,
        notes = "Big slice"
    )

    @Before
    fun setUp() {
        database = mockk(relaxed = true)
        repository = NavCaddyRepositoryImpl(database)
    }

    // ========================================================================
    // Shot Operations Tests
    // ========================================================================

    @Test
    fun `recordShot inserts shot into database`() = runTest {
        // When
        repository.recordShot(testShot)

        // Then
        coVerify { database.shotDao().insertShot(any()) }
    }

    @Test
    fun `getRecentShots calculates correct timestamp window`() = runTest {
        // Given
        val days = 30
        val expectedWindow = days * 24 * 60 * 60 * 1000L

        coEvery { database.shotDao().getRecentShots(any()) } returns flowOf(emptyList())

        // When
        repository.getRecentShots(days).test {
            awaitItem()
            awaitComplete()
        }

        // Then
        coVerify {
            database.shotDao().getRecentShots(
                match { timestamp ->
                    val now = System.currentTimeMillis()
                    val diff = now - timestamp
                    // Allow 1 second tolerance
                    kotlin.math.abs(diff - expectedWindow) < 1000
                }
            )
        }
    }

    @Test
    fun `enforceRetentionPolicy deletes shots older than 90 days`() = runTest {
        // Given
        val expectedDeleted = 42
        coEvery { database.shotDao().deleteOldShots(any()) } returns expectedDeleted

        // When
        val deleted = repository.enforceRetentionPolicy()

        // Then
        assertEquals(expectedDeleted, deleted)
        coVerify {
            database.shotDao().deleteOldShots(
                match { timestamp ->
                    val expectedCutoff = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
                    // Allow 1 second tolerance
                    kotlin.math.abs(timestamp - expectedCutoff) < 1000
                }
            )
        }
    }

    // ========================================================================
    // Miss Pattern Operations Tests
    // ========================================================================

    @Test
    fun `updatePattern upserts pattern in database`() = runTest {
        // Given
        val pattern = MissPattern(
            id = "pattern-1",
            direction = MissDirection.HOOK,
            club = testClub,
            frequency = 10,
            confidence = 0.8f,
            pressureContext = null,
            lastOccurrence = System.currentTimeMillis()
        )

        // When
        repository.updatePattern(pattern)

        // Then
        coVerify { database.missPatternDao().upsertPattern(any()) }
    }

    @Test
    fun `deletePattern removes pattern from database`() = runTest {
        // Given
        val patternId = "pattern-123"

        // When
        repository.deletePattern(patternId)

        // Then
        coVerify { database.missPatternDao().deletePattern(patternId) }
    }

    @Test
    fun `deleteStalePatterns enforces 90-day retention`() = runTest {
        // Given
        val expectedDeleted = 7
        coEvery { database.missPatternDao().deleteStalePatterns(any()) } returns expectedDeleted

        // When
        val deleted = repository.deleteStalePatterns()

        // Then
        assertEquals(expectedDeleted, deleted)
        coVerify {
            database.missPatternDao().deleteStalePatterns(
                match { timestamp ->
                    val expectedCutoff = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000)
                    kotlin.math.abs(timestamp - expectedCutoff) < 1000
                }
            )
        }
    }

    // ========================================================================
    // Session Operations Tests
    // ========================================================================

    @Test
    fun `addConversationTurn inserts turn and trims history`() = runTest {
        // Given
        val turn = ConversationTurn(
            role = Role.USER,
            content = "Help me with my slice",
            timestamp = System.currentTimeMillis()
        )

        // When
        repository.addConversationTurn(turn)

        // Then
        coVerify { database.sessionDao().insertConversationTurn(any()) }
        coVerify { database.sessionDao().trimConversationHistory(keepCount = 10) }
    }

    @Test
    fun `saveSession persists session context`() = runTest {
        // Given
        val session = SessionContext(
            currentHole = 7,
            lastRecommendation = "Use 8-iron"
        )

        // When
        repository.saveSession(session)

        // Then
        coVerify { database.sessionDao().saveSession(any()) }
    }

    @Test
    fun `clearConversationHistory deletes all turns`() = runTest {
        // When
        repository.clearConversationHistory()

        // Then
        coVerify { database.sessionDao().deleteConversationHistory() }
    }

    // ========================================================================
    // Memory Management Tests
    // ========================================================================

    @Test
    fun `clearMemory wipes all data`() = runTest {
        // When
        repository.clearMemory()

        // Then - verify all deletion methods called
        coVerify { database.shotDao().deleteAllShots() }
        coVerify { database.missPatternDao().deleteAllPatterns() }
        coVerify { database.sessionDao().deleteCurrentSession() }
        coVerify { database.sessionDao().deleteAllConversationTurns() }
    }
}
