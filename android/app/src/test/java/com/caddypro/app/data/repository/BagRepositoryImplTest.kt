package com.caddypro.app.data.repository

import com.caddypro.app.data.local.dao.BagDao
import com.caddypro.app.data.local.dao.ClubDao
import com.caddypro.app.data.local.entities.BagEntity
import com.caddypro.app.data.sync.SyncManager
import com.caddypro.app.domain.model.Bag
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BagRepositoryImpl
 *
 * Tests repository-level business rules:
 * - AC6: Default "My Bag" creation
 * - AC7: Only one bag active at a time
 * - AC8: Deleting active bag promotes next bag
 * - AC9: Cannot delete last bag
 */
class BagRepositoryImplTest {

    private lateinit var repository: BagRepositoryImpl
    private lateinit var bagDao: BagDao
    private lateinit var clubDao: ClubDao
    private lateinit var syncManager: SyncManager

    private val testProfileId = "test-profile-id"

    @Before
    fun setup() {
        bagDao = mockk(relaxed = true)
        clubDao = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        repository = BagRepositoryImpl(bagDao, clubDao, syncManager)

        // Default club count to 0
        coEvery { clubDao.getClubCount(any()) } returns 0
    }

    // AC6: Default "My Bag" created on first launch

    @Test
    fun `createDefaultBag creates bag with correct name and active status`() = runTest {
        coEvery { bagDao.insert(any()) } returns Unit

        val result = repository.createDefaultBag(testProfileId)

        assertEquals(Bag.DEFAULT_BAG_NAME, result.name)
        assertEquals(testProfileId, result.profileId)
        assertTrue(result.isActive)
        assertFalse(result.synced)

        coVerify {
            bagDao.insert(match { entity ->
                entity.name == "My Bag" &&
                        entity.profileId == testProfileId &&
                        entity.isActive
            })
        }
    }

    // AC7: Only one bag active at a time

    @Test
    fun `setActiveBag calls DAO setActiveBag which deactivates others`() = runTest {
        coEvery { bagDao.setActiveBag(testProfileId, "bag2") } returns Unit

        repository.setActiveBag(testProfileId, "bag2")

        coVerify { bagDao.setActiveBag(testProfileId, "bag2") }
    }

    // AC9: Cannot delete the last bag

    @Test
    fun `deleteBag fails when only one bag exists`() = runTest {
        coEvery { bagDao.getBagCount(testProfileId) } returns 1

        val result = repository.deleteBag(testProfileId, "bag1")

        assertTrue(result.isFailure)
        assertEquals("Cannot delete the last bag", result.exceptionOrNull()?.message)

        // Should not call delete
        coVerify(exactly = 0) { bagDao.delete(any()) }
    }

    @Test
    fun `deleteBag succeeds when multiple bags exist`() = runTest {
        val bagEntity = BagEntity(
            id = "bag1",
            profileId = testProfileId,
            name = "My Bag",
            isActive = false
        )

        coEvery { bagDao.getBagCount(testProfileId) } returns 2
        coEvery { bagDao.getBagById("bag1") } returns bagEntity
        coEvery { bagDao.delete(bagEntity) } returns Unit

        val result = repository.deleteBag(testProfileId, "bag1")

        assertTrue(result.isSuccess)
        coVerify { bagDao.delete(bagEntity) }
    }

    @Test
    fun `deleteBag fails when bag not found`() = runTest {
        coEvery { bagDao.getBagCount(testProfileId) } returns 2
        coEvery { bagDao.getBagById("nonexistent") } returns null

        val result = repository.deleteBag(testProfileId, "nonexistent")

        assertTrue(result.isFailure)
        assertEquals("Bag not found", result.exceptionOrNull()?.message)

        coVerify(exactly = 0) { bagDao.delete(any()) }
    }

    // AC8: Deleting active bag promotes next bag to active

    @Test
    fun `deleteBag promotes next bag to active when deleting active bag`() = runTest {
        val activeBag = BagEntity(
            id = "bag1",
            profileId = testProfileId,
            name = "Active Bag",
            isActive = true
        )
        val nextBag = BagEntity(
            id = "bag2",
            profileId = testProfileId,
            name = "Next Bag",
            isActive = false
        )

        coEvery { bagDao.getBagCount(testProfileId) } returns 2
        coEvery { bagDao.getBagById("bag1") } returns activeBag
        coEvery { bagDao.delete(activeBag) } returns Unit
        coEvery { bagDao.getBagsByProfileIdSync(testProfileId) } returns listOf(nextBag)
        coEvery { bagDao.setActiveBag(testProfileId, "bag2") } returns Unit

        val result = repository.deleteBag(testProfileId, "bag1")

        assertTrue(result.isSuccess)
        coVerify { bagDao.delete(activeBag) }
        coVerify { bagDao.setActiveBag(testProfileId, "bag2") }
    }

    @Test
    fun `deleteBag does not promote when deleting inactive bag`() = runTest {
        val inactiveBag = BagEntity(
            id = "bag2",
            profileId = testProfileId,
            name = "Inactive Bag",
            isActive = false
        )

        coEvery { bagDao.getBagCount(testProfileId) } returns 2
        coEvery { bagDao.getBagById("bag2") } returns inactiveBag
        coEvery { bagDao.delete(inactiveBag) } returns Unit

        val result = repository.deleteBag(testProfileId, "bag2")

        assertTrue(result.isSuccess)
        coVerify { bagDao.delete(inactiveBag) }
        // Should not call setActiveBag since deleted bag wasn't active
        coVerify(exactly = 0) { bagDao.setActiveBag(any(), any()) }
    }

    // CRUD Operations Tests

    @Test
    fun `getBagsByProfileId returns bags with club counts`() = runTest {
        val bagEntity = BagEntity(
            id = "bag1",
            profileId = testProfileId,
            name = "My Bag",
            isActive = true
        )

        every { bagDao.getBagsByProfileId(testProfileId) } returns flowOf(listOf(bagEntity))
        coEvery { clubDao.getClubCount("bag1") } returns 5

        val result = repository.getBagsByProfileId(testProfileId).first()

        assertEquals(1, result.size)
        assertEquals("bag1", result[0].id)
        assertEquals(5, result[0].clubCount)
    }

    @Test
    fun `getBagById returns bag with club count`() = runTest {
        val bagEntity = BagEntity(
            id = "bag1",
            profileId = testProfileId,
            name = "My Bag",
            isActive = true
        )

        coEvery { bagDao.getBagById("bag1") } returns bagEntity
        coEvery { clubDao.getClubCount("bag1") } returns 10

        val result = repository.getBagById("bag1")

        assertNotNull(result)
        assertEquals("bag1", result!!.id)
        assertEquals(10, result.clubCount)
    }

    @Test
    fun `createBag inserts bag with synced false`() = runTest {
        val bag = Bag(
            id = "new-bag",
            profileId = testProfileId,
            name = "New Bag",
            isActive = false
        )

        coEvery { bagDao.insert(any()) } returns Unit

        repository.createBag(bag)

        coVerify {
            bagDao.insert(match { entity ->
                entity.id == "new-bag" &&
                        entity.name == "New Bag" &&
                        !entity.synced
            })
        }
    }

    @Test
    fun `updateBag updates with new timestamp and synced false`() = runTest {
        val bag = Bag(
            id = "bag1",
            profileId = testProfileId,
            name = "Updated Bag",
            isActive = true
        )

        coEvery { bagDao.update(any()) } returns Unit

        repository.updateBag(bag)

        coVerify {
            bagDao.update(match { entity ->
                entity.id == "bag1" &&
                        entity.name == "Updated Bag" &&
                        !entity.synced
            })
        }
    }

    @Test
    fun `getBagCount returns correct count`() = runTest {
        coEvery { bagDao.getBagCount(testProfileId) } returns 3

        val result = repository.getBagCount(testProfileId)

        assertEquals(3, result)
    }
}
