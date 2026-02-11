package com.caddypro.app.data.repository

import com.caddypro.app.data.local.dao.ClubDao
import com.caddypro.app.data.local.entities.ClubEntity
import com.caddypro.app.data.sync.SyncManager
import com.caddypro.app.domain.model.Club
import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.domain.model.MissBias
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ClubRepositoryImpl
 *
 * Tests club business rules:
 * - AC11: Quick Add populates standard 14-club set
 * - AC12: Carry distance must be <= total distance
 * - AC15: Maximum 14 clubs per bag
 */
class ClubRepositoryImplTest {

    private lateinit var repository: ClubRepositoryImpl
    private lateinit var clubDao: ClubDao
    private lateinit var syncManager: SyncManager

    private val testBagId = "test-bag-id"

    @Before
    fun setup() {
        clubDao = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        repository = ClubRepositoryImpl(clubDao, syncManager)
    }

    // AC11: Quick Add populates standard 14-club set

    @Test
    fun `quickAddStandardSet creates 14 clubs when bag is empty`() = runTest {
        coEvery { clubDao.getClubCount(testBagId) } returns 0
        coEvery { clubDao.insertAll(any()) } returns Unit

        val result = repository.quickAddStandardSet(testBagId)

        assertTrue("Quick Add should succeed with empty bag", result.isSuccess)
        coVerify {
            clubDao.insertAll(match { clubs ->
                clubs.size == 14 &&
                        clubs.any { it.type == ClubType.DRIVER } &&
                        clubs.any { it.type == ClubType.PUTTER }
            })
        }
    }

    @Test
    fun `quickAddStandardSet fails when bag already has clubs`() = runTest {
        coEvery { clubDao.getClubCount(testBagId) } returns 5

        val result = repository.quickAddStandardSet(testBagId)

        assertTrue("Quick Add should fail with existing clubs", result.isFailure)
        assertEquals(
            "Bag already contains clubs. Remove existing clubs before using Quick Add.",
            result.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { clubDao.insertAll(any()) }
    }

    // AC12: Carry distance must be <= total distance

    @Test
    fun `createClub succeeds when carry equals total distance`() = runTest {
        val club = Club(
            bagId = testBagId,
            name = "Test Club",
            type = ClubType.IRON,
            carryDistance = 150,
            totalDistance = 150  // AC12: carry == total is valid
        )

        coEvery { clubDao.getClubCount(testBagId) } returns 5
        coEvery { clubDao.insert(any()) } returns Unit

        val result = repository.createClub(club)

        assertTrue("Club with carry == total should be valid", result.isSuccess)
        coVerify { clubDao.insert(any()) }
    }

    @Test
    fun `createClub succeeds when carry is less than total distance`() = runTest {
        val club = Club(
            bagId = testBagId,
            name = "Test Club",
            type = ClubType.IRON,
            carryDistance = 145,
            totalDistance = 155  // AC12: carry < total is valid
        )

        coEvery { clubDao.getClubCount(testBagId) } returns 5
        coEvery { clubDao.insert(any()) } returns Unit

        val result = repository.createClub(club)

        assertTrue("Club with carry < total should be valid", result.isSuccess)
        coVerify { clubDao.insert(any()) }
    }

    @Test
    fun `createClub fails when carry exceeds total distance`() = runTest {
        val club = Club(
            bagId = testBagId,
            name = "Test Club",
            type = ClubType.IRON,
            carryDistance = 160,
            totalDistance = 155  // AC12: carry > total is invalid
        )

        coEvery { clubDao.getClubCount(testBagId) } returns 5

        val result = repository.createClub(club)

        assertTrue("Club with carry > total should fail", result.isFailure)
        assertEquals(
            "Carry distance cannot exceed total distance",
            result.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { clubDao.insert(any()) }
    }

    @Test
    fun `updateClub fails when carry exceeds total distance`() = runTest {
        val club = Club(
            bagId = testBagId,
            name = "Test Club",
            type = ClubType.IRON,
            carryDistance = 160,
            totalDistance = 150  // AC12: carry > total is invalid
        )

        val result = repository.updateClub(club)

        assertTrue("Update with carry > total should fail", result.isFailure)
        assertEquals(
            "Carry distance cannot exceed total distance",
            result.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { clubDao.update(any()) }
    }

    // AC15: Maximum 14 clubs per bag

    @Test
    fun `createClub succeeds when bag has 13 clubs`() = runTest {
        val club = Club(
            bagId = testBagId,
            name = "14th Club",
            type = ClubType.IRON,
            carryDistance = 145,
            totalDistance = 155
        )

        coEvery { clubDao.getClubCount(testBagId) } returns 13  // AC15: 13 clubs allows one more
        coEvery { clubDao.insert(any()) } returns Unit

        val result = repository.createClub(club)

        assertTrue("Adding 14th club should succeed", result.isSuccess)
        coVerify { clubDao.insert(any()) }
    }

    @Test
    fun `createClub fails when bag already has 14 clubs`() = runTest {
        val club = Club(
            bagId = testBagId,
            name = "15th Club",
            type = ClubType.IRON,
            carryDistance = 145,
            totalDistance = 155
        )

        coEvery { clubDao.getClubCount(testBagId) } returns 14  // AC15: 14 is max

        val result = repository.createClub(club)

        assertTrue("Adding 15th club should fail", result.isFailure)
        assertEquals(
            "Maximum 14 clubs allowed per bag",
            result.exceptionOrNull()?.message
        )
        coVerify(exactly = 0) { clubDao.insert(any()) }
    }

    @Test
    fun `createClub fails when bag has more than 14 clubs`() = runTest {
        val club = Club(
            bagId = testBagId,
            name = "Extra Club",
            type = ClubType.IRON,
            carryDistance = 145,
            totalDistance = 155
        )

        coEvery { clubDao.getClubCount(testBagId) } returns 15

        val result = repository.createClub(club)

        assertTrue("Adding club beyond 14 should fail", result.isFailure)
        coVerify(exactly = 0) { clubDao.insert(any()) }
    }

    // Edge Cases

    @Test
    fun `createClub fails with zero carry distance`() = runTest {
        val club = Club(
            bagId = testBagId,
            name = "Test Club",
            type = ClubType.IRON,
            carryDistance = 0,  // Invalid
            totalDistance = 150
        )

        coEvery { clubDao.getClubCount(testBagId) } returns 5

        val result = repository.createClub(club)

        assertTrue("Club with zero carry should fail", result.isFailure)
        assertEquals(
            "Carry distance must be greater than 0",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `createClub fails with negative total distance`() = runTest {
        val club = Club(
            bagId = testBagId,
            name = "Test Club",
            type = ClubType.IRON,
            carryDistance = 150,
            totalDistance = -10  // Invalid
        )

        coEvery { clubDao.getClubCount(testBagId) } returns 5

        val result = repository.createClub(club)

        assertTrue("Club with negative total should fail", result.isFailure)
        assertEquals(
            "Total distance must be greater than 0",
            result.exceptionOrNull()?.message
        )
    }

    @Test
    fun `deleteClub succeeds when club exists`() = runTest {
        val clubEntity = ClubEntity(
            id = "club1",
            bagId = testBagId,
            name = "Test Club",
            type = ClubType.IRON,
            carryDistance = 145,
            totalDistance = 155
        )

        coEvery { clubDao.getClubById("club1") } returns clubEntity
        coEvery { clubDao.delete(clubEntity) } returns Unit

        val result = repository.deleteClub("club1")

        assertTrue("Delete should succeed", result.isSuccess)
        coVerify { clubDao.delete(clubEntity) }
    }

    @Test
    fun `deleteClub fails when club not found`() = runTest {
        coEvery { clubDao.getClubById("nonexistent") } returns null

        val result = repository.deleteClub("nonexistent")

        assertTrue("Delete of nonexistent club should fail", result.isFailure)
        assertEquals("Club not found", result.exceptionOrNull()?.message)
        coVerify(exactly = 0) { clubDao.delete(any()) }
    }
}
