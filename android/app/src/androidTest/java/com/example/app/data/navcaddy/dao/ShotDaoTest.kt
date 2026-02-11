package caddypro.data.navcaddy.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import caddypro.data.navcaddy.NavCaddyDatabase
import caddypro.data.navcaddy.entities.ShotEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for ShotDao using Room in-memory database.
 *
 * Verifies DAO query behavior and database operations.
 *
 * Spec reference: navcaddy-engine.md R5, Q5 (retention policy)
 */
@RunWith(AndroidJUnit4::class)
class ShotDaoTest {

    private lateinit var database: NavCaddyDatabase
    private lateinit var shotDao: ShotDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            NavCaddyDatabase::class.java
        ).build()
        shotDao = database.shotDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertShot_andRetrieveByClub() = runTest {
        // Given
        val shot = createTestShot(id = "shot-1", clubId = "club-7i")

        // When
        shotDao.insertShot(shot)

        // Then
        shotDao.getShotsByClub("club-7i").test {
            val shots = awaitItem()
            assertEquals(1, shots.size)
            assertEquals("shot-1", shots[0].id)
            assertEquals("club-7i", shots[0].clubId)
            cancel()
        }
    }

    @Test
    fun getRecentShots_filtersOldShots() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)
        val hundredDaysAgo = now - (100L * 24 * 60 * 60 * 1000)

        val recentShot = createTestShot(id = "recent", timestamp = now)
        val mediumShot = createTestShot(id = "medium", timestamp = thirtyDaysAgo)
        val oldShot = createTestShot(id = "old", timestamp = hundredDaysAgo)

        shotDao.insertShot(recentShot)
        shotDao.insertShot(mediumShot)
        shotDao.insertShot(oldShot)

        // When - query for shots in last 60 days
        val sinceTimestamp = now - (60L * 24 * 60 * 60 * 1000)

        // Then
        shotDao.getRecentShots(sinceTimestamp).test {
            val shots = awaitItem()
            assertEquals(2, shots.size)
            assertTrue(shots.any { it.id == "recent" })
            assertTrue(shots.any { it.id == "medium" })
            assertTrue(shots.none { it.id == "old" })
            cancel()
        }
    }

    @Test
    fun getShotsWithPressure_filtersCorrectly() = runTest {
        // Given
        val userTagged = createTestShot(
            id = "user-tagged",
            isUserTaggedPressure = true,
            isInferredPressure = false
        )
        val inferred = createTestShot(
            id = "inferred",
            isUserTaggedPressure = false,
            isInferredPressure = true
        )
        val both = createTestShot(
            id = "both",
            isUserTaggedPressure = true,
            isInferredPressure = true
        )
        val neither = createTestShot(
            id = "neither",
            isUserTaggedPressure = false,
            isInferredPressure = false
        )

        shotDao.insertShot(userTagged)
        shotDao.insertShot(inferred)
        shotDao.insertShot(both)
        shotDao.insertShot(neither)

        // When
        shotDao.getShotsWithPressure().test {
            val shots = awaitItem()

            // Then
            assertEquals(3, shots.size)
            assertTrue(shots.any { it.id == "user-tagged" })
            assertTrue(shots.any { it.id == "inferred" })
            assertTrue(shots.any { it.id == "both" })
            assertTrue(shots.none { it.id == "neither" })
            cancel()
        }
    }

    @Test
    fun deleteOldShots_enforcesRetention() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val ninetyDaysAgo = now - (90L * 24 * 60 * 60 * 1000)

        val recentShot = createTestShot(id = "recent", timestamp = now)
        val oldShot = createTestShot(id = "old", timestamp = ninetyDaysAgo - 1000)

        shotDao.insertShot(recentShot)
        shotDao.insertShot(oldShot)

        // When
        val deletedCount = shotDao.deleteOldShots(ninetyDaysAgo)

        // Then
        assertEquals(1, deletedCount)

        shotDao.getRecentShots(0).test {
            val remaining = awaitItem()
            assertEquals(1, remaining.size)
            assertEquals("recent", remaining[0].id)
            cancel()
        }
    }

    @Test
    fun getShotCount_returnsCorrectCount() = runTest {
        // Given
        shotDao.insertShot(createTestShot(id = "shot-1"))
        shotDao.insertShot(createTestShot(id = "shot-2"))
        shotDao.insertShot(createTestShot(id = "shot-3"))

        // When/Then
        shotDao.getShotCount().test {
            assertEquals(3, awaitItem())
            cancel()
        }
    }

    @Test
    fun deleteAllShots_wipesAllData() = runTest {
        // Given
        shotDao.insertShot(createTestShot(id = "shot-1"))
        shotDao.insertShot(createTestShot(id = "shot-2"))

        // When
        shotDao.deleteAllShots()

        // Then
        shotDao.getShotCount().test {
            assertEquals(0, awaitItem())
            cancel()
        }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private fun createTestShot(
        id: String = "shot-test",
        timestamp: Long = System.currentTimeMillis(),
        clubId: String = "club-test",
        isUserTaggedPressure: Boolean = false,
        isInferredPressure: Boolean = false
    ) = ShotEntity(
        id = id,
        timestamp = timestamp,
        clubId = clubId,
        clubName = "Test Club",
        clubType = "IRON",
        missDirection = "SLICE",
        lie = "FAIRWAY",
        isUserTaggedPressure = isUserTaggedPressure,
        isInferredPressure = isInferredPressure,
        scoringContext = null,
        holeNumber = null,
        notes = null
    )
}
