package caddypro.data.navcaddy.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import caddypro.data.navcaddy.NavCaddyDatabase
import caddypro.data.navcaddy.entities.BagProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Instrumented test for BagProfileDao.
 *
 * Spec reference: player-profile-bag-management.md R1
 * Plan reference: player-profile-bag-management-plan.md Task 3
 */
@RunWith(AndroidJUnit4::class)
class BagProfileDaoTest {
    private lateinit var database: NavCaddyDatabase
    private lateinit var dao: BagProfileDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            NavCaddyDatabase::class.java
        ).build()
        dao = database.bagProfileDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertBag_andRetrieveById() = runTest {
        // Given
        val bag = BagProfileEntity(
            id = UUID.randomUUID().toString(),
            name = "Test Bag",
            isActive = false,
            isArchived = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        // When
        dao.insertBag(bag)
        val retrieved = dao.getBagById(bag.id).first()

        // Then
        assertNotNull(retrieved)
        assertEquals(bag.id, retrieved.id)
        assertEquals(bag.name, retrieved.name)
        assertFalse(retrieved.isActive)
        assertFalse(retrieved.isArchived)
    }

    @Test
    fun getActiveBag_returnsNull_whenNoBagIsActive() = runTest {
        // Given
        val bag = BagProfileEntity(
            id = UUID.randomUUID().toString(),
            name = "Inactive Bag",
            isActive = false,
            isArchived = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertBag(bag)

        // When
        val activeBag = dao.getActiveBag().first()

        // Then
        assertNull(activeBag)
    }

    @Test
    fun getActiveBag_returnsActiveBag() = runTest {
        // Given
        val inactiveBag = BagProfileEntity(
            id = UUID.randomUUID().toString(),
            name = "Inactive Bag",
            isActive = false,
            isArchived = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val activeBag = BagProfileEntity(
            id = UUID.randomUUID().toString(),
            name = "Active Bag",
            isActive = true,
            isArchived = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertBag(inactiveBag)
        dao.insertBag(activeBag)

        // When
        val retrieved = dao.getActiveBag().first()

        // Then
        assertNotNull(retrieved)
        assertEquals(activeBag.id, retrieved.id)
        assertEquals(activeBag.name, retrieved.name)
        assertTrue(retrieved.isActive)
    }

    @Test
    fun switchActiveBag_deactivatesOldAndActivatesNew() = runTest {
        // Given
        val bag1 = BagProfileEntity(
            id = UUID.randomUUID().toString(),
            name = "Bag 1",
            isActive = true,
            isArchived = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val bag2 = BagProfileEntity(
            id = UUID.randomUUID().toString(),
            name = "Bag 2",
            isActive = false,
            isArchived = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertBag(bag1)
        dao.insertBag(bag2)

        // When
        dao.switchActiveBag(bag2.id)

        // Then
        val activeBag = dao.getActiveBag().first()
        assertNotNull(activeBag)
        assertEquals(bag2.id, activeBag.id)
        assertTrue(activeBag.isActive)

        val oldBag = dao.getBagById(bag1.id).first()
        assertNotNull(oldBag)
        assertFalse(oldBag.isActive)
    }

    @Test
    fun archiveBag_setsArchivedFlag() = runTest {
        // Given
        val bag = BagProfileEntity(
            id = UUID.randomUUID().toString(),
            name = "Test Bag",
            isActive = true,
            isArchived = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertBag(bag)

        // When
        dao.archiveBag(bag.id)

        // Then
        val retrieved = dao.getBagById(bag.id).first()
        assertNotNull(retrieved)
        assertTrue(retrieved.isArchived)
        assertFalse(retrieved.isActive) // Should also deactivate
    }

    @Test
    fun getAllBags_excludesArchivedBags() = runTest {
        // Given
        val activeBag = BagProfileEntity(
            id = UUID.randomUUID().toString(),
            name = "Active Bag",
            isActive = false,
            isArchived = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val archivedBag = BagProfileEntity(
            id = UUID.randomUUID().toString(),
            name = "Archived Bag",
            isActive = false,
            isArchived = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertBag(activeBag)
        dao.insertBag(archivedBag)

        // When
        val bags = dao.getAllBags().first()

        // Then
        assertEquals(1, bags.size)
        assertEquals(activeBag.id, bags[0].id)
    }

    @Test
    fun getBagCount_countsOnlyNonArchivedBags() = runTest {
        // Given
        val bag1 = BagProfileEntity(
            id = UUID.randomUUID().toString(),
            name = "Bag 1",
            isActive = false,
            isArchived = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val bag2 = BagProfileEntity(
            id = UUID.randomUUID().toString(),
            name = "Bag 2",
            isActive = false,
            isArchived = false,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        val archivedBag = BagProfileEntity(
            id = UUID.randomUUID().toString(),
            name = "Archived",
            isActive = false,
            isArchived = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        dao.insertBag(bag1)
        dao.insertBag(bag2)
        dao.insertBag(archivedBag)

        // When
        val count = dao.getBagCount().first()

        // Then
        assertEquals(2, count)
    }
}
