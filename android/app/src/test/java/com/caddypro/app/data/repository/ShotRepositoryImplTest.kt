package com.caddypro.app.data.repository

import com.caddypro.app.data.local.dao.ShotDao
import com.caddypro.app.data.local.entities.ShotEntity
import com.caddypro.app.data.sync.SyncManager
import com.caddypro.app.domain.model.Shot
import com.caddypro.app.domain.model.ShotType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ShotRepositoryImplTest {

    private lateinit var shotDao: ShotDao
    private lateinit var syncManager: SyncManager
    private lateinit var repository: ShotRepositoryImpl

    private val testRoundId = "round-456"
    private val testShotId = "shot-789"

    private val testShotEntity = ShotEntity(
        id = testShotId,
        roundId = testRoundId,
        holeNumber = 1,
        shotNumber = 1,
        clubId = "club-1",
        clubName = "Driver",
        shotType = ShotType.TEE,
        startLatitude = -37.8136,
        startLongitude = 144.9631,
        endLatitude = null,
        endLongitude = null,
        timestamp = System.currentTimeMillis(),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        synced = false
    )

    @Before
    fun setup() {
        shotDao = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        repository = ShotRepositoryImpl(shotDao, syncManager)
    }

    // AC9: Shot logging works
    @Test
    fun `logShot inserts shot and queues sync`() = runTest {
        val shot = Shot(
            id = testShotId,
            roundId = testRoundId,
            holeNumber = 1,
            shotNumber = 1,
            clubId = "club-1",
            clubName = "Driver",
            shotType = ShotType.TEE
        )

        val result = repository.logShot(shot)

        assertTrue(result.isSuccess)
        coVerify { shotDao.insert(match { it.id == testShotId }) }
        coVerify { syncManager.queueSync() }
    }

    @Test
    fun `logShot returns failure on exception`() = runTest {
        coEvery { shotDao.insert(any()) } throws RuntimeException("DB error")

        val shot = Shot(
            id = testShotId,
            roundId = testRoundId,
            holeNumber = 1,
            shotNumber = 1,
            clubId = "club-1",
            clubName = "Driver",
            shotType = ShotType.TEE
        )

        val result = repository.logShot(shot)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getShotsByRoundId returns flow of shots`() = runTest {
        val entities = listOf(
            testShotEntity,
            testShotEntity.copy(id = "shot-2", shotNumber = 2, shotType = ShotType.FAIRWAY)
        )
        coEvery { shotDao.getShotsByRoundId(testRoundId) } returns flowOf(entities)

        val shots = repository.getShotsByRoundId(testRoundId).first()

        assertEquals(2, shots.size)
        assertEquals(ShotType.TEE, shots[0].shotType)
        assertEquals(ShotType.FAIRWAY, shots[1].shotType)
    }

    @Test
    fun `getShotsByHole returns flow of hole-specific shots`() = runTest {
        val entities = listOf(testShotEntity)
        coEvery { shotDao.getShotsByHole(testRoundId, 1) } returns flowOf(entities)

        val shots = repository.getShotsByHole(testRoundId, 1).first()

        assertEquals(1, shots.size)
        assertEquals(1, shots[0].holeNumber)
    }

    @Test
    fun `getShotCountForHole returns count`() = runTest {
        coEvery { shotDao.getShotCountForHole(testRoundId, 1) } returns 4

        val count = repository.getShotCountForHole(testRoundId, 1)

        assertEquals(4, count)
    }

    @Test
    fun `getTotalShotCount returns total`() = runTest {
        coEvery { shotDao.getTotalShotCount(testRoundId) } returns 72

        val count = repository.getTotalShotCount(testRoundId)

        assertEquals(72, count)
    }

    // AC20: Shots deletable with shot renumbering
    @Test
    fun `deleteShot removes shot and renumbers remaining`() = runTest {
        coEvery { shotDao.getShotById(testShotId) } returns testShotEntity

        val result = repository.deleteShot(testShotId)

        assertTrue(result.isSuccess)
        coVerify { shotDao.delete(testShotEntity) }
        coVerify { shotDao.renumberShotsAfterDelete(testRoundId, 1, 1) }
        coVerify { syncManager.queueSync() }
    }

    @Test
    fun `deleteShot fails when shot not found`() = runTest {
        coEvery { shotDao.getShotById("nonexistent") } returns null

        val result = repository.deleteShot("nonexistent")

        assertTrue(result.isFailure)
    }

    @Test
    fun `getLastShot returns last shot`() = runTest {
        coEvery { shotDao.getLastShot(testRoundId) } returns testShotEntity

        val shot = repository.getLastShot(testRoundId)

        assertNotNull(shot)
        assertEquals(testShotId, shot!!.id)
    }

    @Test
    fun `getLastShot returns null when no shots`() = runTest {
        coEvery { shotDao.getLastShot(testRoundId) } returns null

        val shot = repository.getLastShot(testRoundId)

        assertNull(shot)
    }

    @Test
    fun `updateShot updates entity and queues sync`() = runTest {
        val shot = Shot(
            id = testShotId,
            roundId = testRoundId,
            holeNumber = 1,
            shotNumber = 1,
            clubId = "club-1",
            clubName = "Driver",
            shotType = ShotType.FAIRWAY
        )

        val result = repository.updateShot(shot)

        assertTrue(result.isSuccess)
        coVerify { shotDao.update(match { it.id == testShotId && !it.synced }) }
        coVerify { syncManager.queueSync() }
    }
}
