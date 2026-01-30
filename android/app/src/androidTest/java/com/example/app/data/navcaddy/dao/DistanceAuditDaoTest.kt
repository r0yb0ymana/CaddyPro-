package caddypro.data.navcaddy.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import caddypro.data.navcaddy.NavCaddyDatabase
import caddypro.data.navcaddy.entities.DistanceAuditEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Instrumented test for DistanceAuditDao.
 *
 * Spec reference: player-profile-bag-management.md R3
 * Plan reference: player-profile-bag-management-plan.md Task 3
 */
@RunWith(AndroidJUnit4::class)
class DistanceAuditDaoTest {
    private lateinit var database: NavCaddyDatabase
    private lateinit var dao: DistanceAuditDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            NavCaddyDatabase::class.java
        ).build()
        dao = database.distanceAuditDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAudit_andRetrieveHistory() = runTest {
        // Given
        val clubId = "test-club"
        val audit = DistanceAuditEntity(
            id = UUID.randomUUID().toString(),
            clubId = clubId,
            oldEstimated = 200,
            newEstimated = 210,
            inferredValue = 212,
            confidence = 0.85,
            reason = "Accepted suggestion based on 25 recent shots",
            timestamp = System.currentTimeMillis(),
            wasAccepted = true
        )

        // When
        dao.insertAudit(audit)
        val history = dao.getAuditHistory(clubId).first()

        // Then
        assertEquals(1, history.size)
        assertEquals(audit.id, history[0].id)
        assertEquals(audit.oldEstimated, history[0].oldEstimated)
        assertEquals(audit.newEstimated, history[0].newEstimated)
        assertTrue(history[0].wasAccepted)
    }

    @Test
    fun getAuditHistory_orderedByTimestampDescending() = runTest {
        // Given
        val clubId = "test-club"
        val baseTimestamp = System.currentTimeMillis()

        val audit1 = createAudit(clubId, timestamp = baseTimestamp - 2000) // Oldest
        val audit2 = createAudit(clubId, timestamp = baseTimestamp - 1000) // Middle
        val audit3 = createAudit(clubId, timestamp = baseTimestamp) // Newest

        dao.insertAudit(audit1)
        dao.insertAudit(audit2)
        dao.insertAudit(audit3)

        // When
        val history = dao.getAuditHistory(clubId).first()

        // Then
        assertEquals(3, history.size)
        assertEquals(audit3.id, history[0].id) // Newest first
        assertEquals(audit2.id, history[1].id)
        assertEquals(audit1.id, history[2].id) // Oldest last
    }

    @Test
    fun getAuditHistoryLimited_respectsLimit() = runTest {
        // Given
        val clubId = "test-club"
        repeat(10) { i ->
            dao.insertAudit(createAudit(clubId, timestamp = System.currentTimeMillis() + i))
        }

        // When
        val history = dao.getAuditHistoryLimited(clubId, limit = 5).first()

        // Then
        assertEquals(5, history.size)
    }

    @Test
    fun getAuditsSince_filtersCorrectly() = runTest {
        // Given
        val clubId = "test-club"
        val cutoffTime = System.currentTimeMillis()

        val oldAudit = createAudit(clubId, timestamp = cutoffTime - 10000)
        val newAudit1 = createAudit(clubId, timestamp = cutoffTime)
        val newAudit2 = createAudit(clubId, timestamp = cutoffTime + 10000)

        dao.insertAudit(oldAudit)
        dao.insertAudit(newAudit1)
        dao.insertAudit(newAudit2)

        // When
        val recentAudits = dao.getAuditsSince(cutoffTime).first()

        // Then
        assertEquals(2, recentAudits.size)
        assertTrue(recentAudits.all { it.timestamp >= cutoffTime })
    }

    @Test
    fun getAuditCount_countsCorrectly() = runTest {
        // Given
        val clubId1 = "club1"
        val clubId2 = "club2"

        repeat(3) { dao.insertAudit(createAudit(clubId1)) }
        repeat(2) { dao.insertAudit(createAudit(clubId2)) }

        // When
        val count1 = dao.getAuditCount(clubId1).first()
        val count2 = dao.getAuditCount(clubId2).first()

        // Then
        assertEquals(3, count1)
        assertEquals(2, count2)
    }

    @Test
    fun deleteOldAudits_deletesOldRecords() = runTest {
        // Given
        val clubId = "test-club"
        val cutoffTime = System.currentTimeMillis()

        val oldAudit1 = createAudit(clubId, timestamp = cutoffTime - 20000)
        val oldAudit2 = createAudit(clubId, timestamp = cutoffTime - 10000)
        val newAudit = createAudit(clubId, timestamp = cutoffTime + 10000)

        dao.insertAudit(oldAudit1)
        dao.insertAudit(oldAudit2)
        dao.insertAudit(newAudit)

        // When
        val deletedCount = dao.deleteOldAudits(cutoffTime)

        // Then
        assertEquals(2, deletedCount)
        val remaining = dao.getAuditHistory(clubId).first()
        assertEquals(1, remaining.size)
        assertEquals(newAudit.id, remaining[0].id)
    }

    @Test
    fun deleteAuditsForClub_deletesAllForClub() = runTest {
        // Given
        val clubId = "test-club"
        repeat(5) { dao.insertAudit(createAudit(clubId)) }

        // When
        dao.deleteAuditsForClub(clubId)

        // Then
        val audits = dao.getAuditHistory(clubId).first()
        assertEquals(0, audits.size)
    }

    private fun createAudit(
        clubId: String,
        timestamp: Long = System.currentTimeMillis()
    ): DistanceAuditEntity {
        return DistanceAuditEntity(
            id = UUID.randomUUID().toString(),
            clubId = clubId,
            oldEstimated = 200,
            newEstimated = 210,
            inferredValue = 212,
            confidence = 0.85,
            reason = "Test audit entry",
            timestamp = timestamp,
            wasAccepted = true
        )
    }
}
