package com.caddypro.app.data.repository

import com.caddypro.app.data.local.dao.RoundDao
import com.caddypro.app.data.local.dao.ShotDao
import com.caddypro.app.data.local.entities.RoundEntity
import com.caddypro.app.data.sync.SyncManager
import com.caddypro.app.domain.model.Round
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

class RoundRepositoryImplTest {

    private lateinit var roundDao: RoundDao
    private lateinit var shotDao: ShotDao
    private lateinit var syncManager: SyncManager
    private lateinit var repository: RoundRepositoryImpl

    private val testProfileId = "profile-123"
    private val testRoundId = "round-456"

    private val testRoundEntity = RoundEntity(
        id = testRoundId,
        profileId = testProfileId,
        courseName = "Royal Melbourne",
        holesPlayed = 18,
        startedAt = System.currentTimeMillis(),
        endedAt = null,
        isActive = true,
        totalShots = 0,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        synced = false
    )

    @Before
    fun setup() {
        roundDao = mockk(relaxed = true)
        shotDao = mockk(relaxed = true)
        syncManager = mockk(relaxed = true)
        repository = RoundRepositoryImpl(roundDao, shotDao, syncManager)
    }

    // AC3: Only one active round at a time
    @Test
    fun `createRound succeeds when no active round exists`() = runTest {
        coEvery { roundDao.getActiveRound() } returns null

        val round = Round(
            id = testRoundId,
            profileId = testProfileId,
            courseName = "Royal Melbourne",
            holesPlayed = 18
        )
        val result = repository.createRound(round)

        assertTrue(result.isSuccess)
        coVerify { roundDao.insert(match { it.id == testRoundId }) }
        coVerify { syncManager.queueSync() }
    }

    // AC3: Only one active round at a time
    @Test
    fun `createRound fails when active round already exists`() = runTest {
        coEvery { roundDao.getActiveRound() } returns testRoundEntity

        val round = Round(
            id = "new-round",
            profileId = testProfileId,
            courseName = "Kingston Heath"
        )
        val result = repository.createRound(round)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { roundDao.insert(any()) }
    }

    @Test
    fun `getActiveRound returns active round`() = runTest {
        coEvery { roundDao.getActiveRound() } returns testRoundEntity

        val round = repository.getActiveRound()

        assertNotNull(round)
        assertEquals("Royal Melbourne", round!!.courseName)
        assertEquals(testRoundId, round.id)
    }

    @Test
    fun `getActiveRound returns null when no active round`() = runTest {
        coEvery { roundDao.getActiveRound() } returns null

        val round = repository.getActiveRound()

        assertNull(round)
    }

    @Test
    fun `getRoundById returns round when exists`() = runTest {
        coEvery { roundDao.getRoundById(testRoundId) } returns testRoundEntity

        val round = repository.getRoundById(testRoundId)

        assertNotNull(round)
        assertEquals(testRoundId, round!!.id)
    }

    @Test
    fun `getRoundById returns null when not found`() = runTest {
        coEvery { roundDao.getRoundById("nonexistent") } returns null

        val round = repository.getRoundById("nonexistent")

        assertNull(round)
    }

    @Test
    fun `getRoundsByProfileId returns flow of rounds`() = runTest {
        val entities = listOf(testRoundEntity, testRoundEntity.copy(id = "round-789"))
        coEvery { roundDao.getRoundsByProfileId(testProfileId) } returns flowOf(entities)

        val rounds = repository.getRoundsByProfileId(testProfileId).first()

        assertEquals(2, rounds.size)
    }

    @Test
    fun `endRound calculates total shots and marks round as ended`() = runTest {
        coEvery { roundDao.getRoundById(testRoundId) } returns testRoundEntity
        coEvery { shotDao.getTotalShotCount(testRoundId) } returns 85

        val result = repository.endRound(testRoundId)

        assertTrue(result.isSuccess)
        coVerify {
            roundDao.endRound(
                roundId = testRoundId,
                endedAt = any(),
                totalShots = 85,
                updatedAt = any()
            )
        }
        coVerify { syncManager.queueSync() }
    }

    @Test
    fun `endRound fails when round not found`() = runTest {
        coEvery { roundDao.getRoundById("nonexistent") } returns null

        val result = repository.endRound("nonexistent")

        assertTrue(result.isFailure)
    }

    @Test
    fun `updateShotCount updates via dao`() = runTest {
        repository.updateShotCount(testRoundId, 42)

        coVerify { roundDao.updateShotCount(testRoundId, 42, any()) }
    }

    // AC24: Active round survives app restart (round persisted to Room)
    @Test
    fun `getActiveRoundFlow emits updates`() = runTest {
        coEvery { roundDao.getActiveRoundFlow() } returns flowOf(testRoundEntity)

        val round = repository.getActiveRoundFlow().first()

        assertNotNull(round)
        assertEquals("Royal Melbourne", round!!.courseName)
    }
}
