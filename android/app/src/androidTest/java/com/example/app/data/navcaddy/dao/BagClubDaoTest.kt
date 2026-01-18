package caddypro.data.navcaddy.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import caddypro.data.navcaddy.NavCaddyDatabase
import caddypro.data.navcaddy.entities.BagClubEntity
import caddypro.data.navcaddy.entities.BagProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Instrumented test for BagClubDao.
 *
 * Spec reference: player-profile-bag-management.md R2
 * Plan reference: player-profile-bag-management-plan.md Task 3
 */
@RunWith(AndroidJUnit4::class)
class BagClubDaoTest {
    private lateinit var database: NavCaddyDatabase
    private lateinit var bagProfileDao: BagProfileDao
    private lateinit var bagClubDao: BagClubDao
    private lateinit var testBagId: String

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            NavCaddyDatabase::class.java
        ).build()
        bagProfileDao = database.bagProfileDao()
        bagClubDao = database.bagClubDao()

        // Create a test bag
        testBagId = UUID.randomUUID().toString()
        val testBag = BagProfileEntity(
            id = testBagId,
            name = "Test Bag",
            isActive = true,
            isArchived = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        bagProfileDao.insertBag(testBag)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertClub_andRetrieve() = runTest {
        // Given
        val clubId = UUID.randomUUID().toString()
        val club = BagClubEntity(
            id = UUID.randomUUID().toString(),
            bagId = testBagId,
            clubId = clubId,
            position = 1,
            name = "Driver",
            type = "DRIVER",
            loft = 10.5,
            estimatedCarry = 250,
            inferredCarry = null,
            inferredConfidence = null,
            missBiasDirection = null,
            missBiasType = null,
            missBiasIsUserDefined = null,
            missBiasConfidence = null,
            missBiasLastUpdated = null,
            shaft = "Regular",
            flex = "Regular",
            notes = "My favorite driver"
        )

        // When
        bagClubDao.insertClub(club)
        val clubs = bagClubDao.getClubsForBag(testBagId).first()

        // Then
        assertEquals(1, clubs.size)
        assertEquals(club.clubId, clubs[0].clubId)
        assertEquals(club.name, clubs[0].name)
        assertEquals(club.estimatedCarry, clubs[0].estimatedCarry)
    }

    @Test
    fun getClubsForBag_orderedByPosition() = runTest {
        // Given
        val club1 = createTestClub(clubId = "club1", position = 2, name = "7-iron")
        val club2 = createTestClub(clubId = "club2", position = 1, name = "Driver")
        val club3 = createTestClub(clubId = "club3", position = 3, name = "Putter")

        bagClubDao.insertClub(club1)
        bagClubDao.insertClub(club2)
        bagClubDao.insertClub(club3)

        // When
        val clubs = bagClubDao.getClubsForBag(testBagId).first()

        // Then
        assertEquals(3, clubs.size)
        assertEquals("Driver", clubs[0].name) // position 1
        assertEquals("7-iron", clubs[1].name) // position 2
        assertEquals("Putter", clubs[2].name) // position 3
    }

    @Test
    fun updateClubDistance_updatesEstimatedCarry() = runTest {
        // Given
        val clubId = "test-club"
        val club = createTestClub(clubId = clubId, position = 1, estimatedCarry = 200)
        bagClubDao.insertClub(club)

        // When
        bagClubDao.updateClubDistance(testBagId, clubId, 220)

        // Then
        val updated = bagClubDao.getClubFromBag(testBagId, clubId).first()
        assertNotNull(updated)
        assertEquals(220, updated.estimatedCarry)
    }

    @Test
    fun updateInferredDistance_updatesInferredValues() = runTest {
        // Given
        val clubId = "test-club"
        val club = createTestClub(clubId = clubId, position = 1)
        bagClubDao.insertClub(club)

        // When
        bagClubDao.updateInferredDistance(testBagId, clubId, 215, 0.85)

        // Then
        val updated = bagClubDao.getClubFromBag(testBagId, clubId).first()
        assertNotNull(updated)
        assertEquals(215, updated.inferredCarry)
        assertEquals(0.85, updated.inferredConfidence)
    }

    @Test
    fun updateMissBias_updatesMissBiasFields() = runTest {
        // Given
        val clubId = "test-club"
        val club = createTestClub(clubId = clubId, position = 1)
        bagClubDao.insertClub(club)

        // When
        val timestamp = System.currentTimeMillis()
        bagClubDao.updateMissBias(
            testBagId,
            clubId,
            "RIGHT",
            "SLICE",
            true,
            0.75,
            timestamp
        )

        // Then
        val updated = bagClubDao.getClubFromBag(testBagId, clubId).first()
        assertNotNull(updated)
        assertEquals("RIGHT", updated.missBiasDirection)
        assertEquals("SLICE", updated.missBiasType)
        assertEquals(true, updated.missBiasIsUserDefined)
        assertEquals(0.75, updated.missBiasConfidence)
        assertEquals(timestamp, updated.missBiasLastUpdated)
    }

    @Test
    fun removeClubFromBag_deletesClub() = runTest {
        // Given
        val clubId = "test-club"
        val club = createTestClub(clubId = clubId, position = 1)
        bagClubDao.insertClub(club)

        // When
        bagClubDao.removeClubFromBag(testBagId, clubId)

        // Then
        val clubs = bagClubDao.getClubsForBag(testBagId).first()
        assertEquals(0, clubs.size)
    }

    @Test
    fun foreignKeyConstraint_deleteBag_cascadesClubDeletion() = runTest {
        // Given
        val club = createTestClub(clubId = "test-club", position = 1)
        bagClubDao.insertClub(club)

        // When - Delete the bag
        bagProfileDao.deleteAllBags()

        // Then - Clubs should also be deleted (cascade)
        val clubs = bagClubDao.getClubsForBag(testBagId).first()
        assertEquals(0, clubs.size)
    }

    @Test
    fun getClubCountForBag_countsCorrectly() = runTest {
        // Given
        bagClubDao.insertClub(createTestClub(clubId = "club1", position = 1))
        bagClubDao.insertClub(createTestClub(clubId = "club2", position = 2))
        bagClubDao.insertClub(createTestClub(clubId = "club3", position = 3))

        // When
        val count = bagClubDao.getClubCountForBag(testBagId).first()

        // Then
        assertEquals(3, count)
    }

    private fun createTestClub(
        clubId: String,
        position: Int,
        name: String = "Test Club",
        estimatedCarry: Int = 200
    ): BagClubEntity {
        return BagClubEntity(
            id = UUID.randomUUID().toString(),
            bagId = testBagId,
            clubId = clubId,
            position = position,
            name = name,
            type = "IRON",
            loft = 30.0,
            estimatedCarry = estimatedCarry,
            inferredCarry = null,
            inferredConfidence = null,
            missBiasDirection = null,
            missBiasType = null,
            missBiasIsUserDefined = null,
            missBiasConfidence = null,
            missBiasLastUpdated = null,
            shaft = null,
            flex = null,
            notes = null
        )
    }
}
