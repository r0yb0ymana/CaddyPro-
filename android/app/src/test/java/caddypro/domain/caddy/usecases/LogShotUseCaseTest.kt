package caddypro.domain.caddy.usecases

import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.domain.navcaddy.context.RoundState
import caddypro.domain.navcaddy.context.SessionContextManager
import caddypro.domain.navcaddy.models.ClubType
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.Shot
import caddypro.domain.navcaddy.models.Club
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LogShotUseCase.
 *
 * Tests cover:
 * - Successful shot logging with hole context
 * - Shot logging without active round (error case)
 * - Shot result validation (lie + miss direction)
 * - Offline-first persistence behavior
 *
 * Spec reference: live-caddy-mode.md R6, A4
 * Plan reference: live-caddy-mode-plan.md Task 9 step 2
 */
class LogShotUseCaseTest {

    private lateinit var navCaddyRepository: NavCaddyRepository
    private lateinit var sessionContextManager: SessionContextManager
    private lateinit var useCase: LogShotUseCase

    private val mockClub = Club(
        id = "club-1",
        name = "7-iron",
        type = ClubType.IRON,
        estimatedCarry = 150
    )

    private val mockRoundState = RoundState(
        roundId = "round-123",
        courseName = "Pebble Beach",
        currentHole = 7,
        currentPar = 4,
        totalScore = 28,
        holesCompleted = 6,
        conditions = null
    )

    @Before
    fun setup() {
        navCaddyRepository = mockk(relaxed = true)
        sessionContextManager = mockk()
        useCase = LogShotUseCase(navCaddyRepository, sessionContextManager)
    }

    @Test
    fun `when shot logged then writes to repository with hole context`() = runTest {
        // Given: Active round exists
        coEvery { sessionContextManager.getCurrentRoundState() } returns mockRoundState

        val shotResult = ShotResult(
            lie = Lie.FAIRWAY,
            missDirection = null
        )

        // When: Shot is logged
        val result = useCase(mockClub, shotResult)

        // Then: Success result
        assertTrue(result.isSuccess)

        // Then: Shot is recorded to repository
        val shotSlot = slot<Shot>()
        coVerify { navCaddyRepository.recordShot(capture(shotSlot)) }

        // Verify shot details
        val capturedShot = shotSlot.captured
        assertEquals(mockClub.id, capturedShot.club.id)
        assertEquals(mockClub.name, capturedShot.club.name)
        assertEquals(mockClub.type, capturedShot.club.type)
        assertEquals(Lie.FAIRWAY, capturedShot.lie)
        assertNull(capturedShot.missDirection)
        assertEquals(7, capturedShot.holeNumber) // From round state
        assertNull(capturedShot.notes)
        assertTrue(capturedShot.timestamp > 0)
    }

    @Test
    fun `when shot logged with miss direction then includes in shot data`() = runTest {
        // Given: Active round exists
        coEvery { sessionContextManager.getCurrentRoundState() } returns mockRoundState

        val shotResult = ShotResult(
            lie = Lie.ROUGH,
            missDirection = MissDirection.SLICE
        )

        // When: Shot is logged
        val result = useCase(mockClub, shotResult)

        // Then: Success
        assertTrue(result.isSuccess)

        // Then: Miss direction is captured
        val shotSlot = slot<Shot>()
        coVerify { navCaddyRepository.recordShot(capture(shotSlot)) }

        val capturedShot = shotSlot.captured
        assertEquals(Lie.ROUGH, capturedShot.lie)
        assertEquals(MissDirection.SLICE, capturedShot.missDirection)
    }

    @Test
    fun `when no active round then returns failure`() = runTest {
        // Given: No active round
        coEvery { sessionContextManager.getCurrentRoundState() } returns null

        val shotResult = ShotResult(
            lie = Lie.FAIRWAY,
            missDirection = null
        )

        // When: Attempt to log shot
        val result = useCase(mockClub, shotResult)

        // Then: Failure result
        assertTrue(result.isFailure)
        assertEquals("No active round", result.exceptionOrNull()?.message)

        // Then: No shot recorded
        coVerify(exactly = 0) { navCaddyRepository.recordShot(any()) }
    }

    @Test
    fun `when shot logged to hazard then records hazard lie`() = runTest {
        // Given: Active round exists
        coEvery { sessionContextManager.getCurrentRoundState() } returns mockRoundState

        val shotResult = ShotResult(
            lie = Lie.HAZARD,
            missDirection = MissDirection.PULL
        )

        // When: Shot is logged
        val result = useCase(mockClub, shotResult)

        // Then: Success
        assertTrue(result.isSuccess)

        // Then: Hazard lie is captured with miss direction
        val shotSlot = slot<Shot>()
        coVerify { navCaddyRepository.recordShot(capture(shotSlot)) }

        val capturedShot = shotSlot.captured
        assertEquals(Lie.HAZARD, capturedShot.lie)
        assertEquals(MissDirection.PULL, capturedShot.missDirection)
    }

    @Test
    fun `when shot logged to green then records green lie`() = runTest {
        // Given: Active round exists
        coEvery { sessionContextManager.getCurrentRoundState() } returns mockRoundState

        val shotResult = ShotResult(
            lie = Lie.GREEN,
            missDirection = null // On target
        )

        // When: Shot is logged
        val result = useCase(mockClub, shotResult)

        // Then: Success
        assertTrue(result.isSuccess)

        // Then: Green lie is captured
        val shotSlot = slot<Shot>()
        coVerify { navCaddyRepository.recordShot(capture(shotSlot)) }

        val capturedShot = shotSlot.captured
        assertEquals(Lie.GREEN, capturedShot.lie)
        assertNull(capturedShot.missDirection) // Good shot
    }

    @Test
    fun `when multiple shots logged then each has unique ID and timestamp`() = runTest {
        // Given: Active round exists
        coEvery { sessionContextManager.getCurrentRoundState() } returns mockRoundState

        val shotResult1 = ShotResult(Lie.FAIRWAY, null)
        val shotResult2 = ShotResult(Lie.ROUGH, MissDirection.SLICE)

        // When: Two shots are logged
        val result1 = useCase(mockClub, shotResult1)
        val result2 = useCase(mockClub, shotResult2)

        // Then: Both succeed
        assertTrue(result1.isSuccess)
        assertTrue(result2.isSuccess)

        // Then: Both shots recorded
        val shotSlots = mutableListOf<Shot>()
        coVerify(exactly = 2) { navCaddyRepository.recordShot(capture(shotSlots)) }

        // Then: Each has unique ID
        val shot1 = shotSlots[0]
        val shot2 = shotSlots[1]
        assertTrue(shot1.id != shot2.id)

        // Then: Timestamps are different (or equal if very fast)
        assertTrue(shot2.timestamp >= shot1.timestamp)
    }

    @Test
    fun `when shot logged with different club types then club data is preserved`() = runTest {
        // Given: Active round exists
        coEvery { sessionContextManager.getCurrentRoundState() } returns mockRoundState

        val driverClub = Club(
            id = "driver-1",
            name = "Driver",
            type = ClubType.DRIVER,
            estimatedCarry = 250
        )

        val shotResult = ShotResult(Lie.FAIRWAY, null)

        // When: Shot is logged with driver
        val result = useCase(driverClub, shotResult)

        // Then: Success
        assertTrue(result.isSuccess)

        // Then: Driver club data is captured
        val shotSlot = slot<Shot>()
        coVerify { navCaddyRepository.recordShot(capture(shotSlot)) }

        val capturedShot = shotSlot.captured
        assertEquals("driver-1", capturedShot.club.id)
        assertEquals("Driver", capturedShot.club.name)
        assertEquals(ClubType.DRIVER, capturedShot.club.type)
    }

    @Test
    fun `when bunker shot logged then records bunker lie`() = runTest {
        // Given: Active round exists
        coEvery { sessionContextManager.getCurrentRoundState() } returns mockRoundState

        val shotResult = ShotResult(
            lie = Lie.BUNKER,
            missDirection = MissDirection.FAT
        )

        // When: Shot is logged
        val result = useCase(mockClub, shotResult)

        // Then: Success
        assertTrue(result.isSuccess)

        // Then: Bunker lie with fat contact is captured
        val shotSlot = slot<Shot>()
        coVerify { navCaddyRepository.recordShot(capture(shotSlot)) }

        val capturedShot = shotSlot.captured
        assertEquals(Lie.BUNKER, capturedShot.lie)
        assertEquals(MissDirection.FAT, capturedShot.missDirection)
    }

    @Test
    fun `when repository throws exception then returns failure`() = runTest {
        // Given: Active round exists but repository fails
        coEvery { sessionContextManager.getCurrentRoundState() } returns mockRoundState
        coEvery { navCaddyRepository.recordShot(any()) } throws Exception("Database error")

        val shotResult = ShotResult(Lie.FAIRWAY, null)

        // When: Attempt to log shot
        val result = useCase(mockClub, shotResult)

        // Then: Failure result
        assertTrue(result.isFailure)
        assertEquals("Database error", result.exceptionOrNull()?.message)
    }
}

/**
 * Tests for ShotResult data class validation.
 */
class ShotResultTest {

    @Test
    fun `when creating shot result with fairway then valid`() {
        val result = ShotResult(Lie.FAIRWAY, null)
        assertEquals(Lie.FAIRWAY, result.lie)
        assertNull(result.missDirection)
    }

    @Test
    fun `when creating shot result with miss direction then valid`() {
        val result = ShotResult(Lie.ROUGH, MissDirection.SLICE)
        assertEquals(Lie.ROUGH, result.lie)
        assertEquals(MissDirection.SLICE, result.missDirection)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `when hazard shot without miss direction then throws exception`() {
        // This validates that hazard shots should include miss direction
        // for better analytics downstream
        ShotResult(Lie.HAZARD, null)
    }

    @Test
    fun `when hazard shot with miss direction then valid`() {
        val result = ShotResult(Lie.HAZARD, MissDirection.HOOK)
        assertEquals(Lie.HAZARD, result.lie)
        assertEquals(MissDirection.HOOK, result.missDirection)
    }

    @Test
    fun `when green shot with straight then valid`() {
        val result = ShotResult(Lie.GREEN, MissDirection.STRAIGHT)
        assertEquals(Lie.GREEN, result.lie)
        assertEquals(MissDirection.STRAIGHT, result.missDirection)
    }
}
