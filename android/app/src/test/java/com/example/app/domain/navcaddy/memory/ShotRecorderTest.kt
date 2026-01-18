package caddypro.domain.navcaddy.memory

import caddypro.data.navcaddy.repository.NavCaddyRepository
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.ClubType
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.PressureContext
import caddypro.domain.navcaddy.models.Shot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ShotRecorder.
 *
 * Tests shot recording, retrieval, and filtering operations.
 */
class ShotRecorderTest {

    private lateinit var repository: NavCaddyRepository
    private lateinit var shotRecorder: ShotRecorder

    private val testClub = Club(
        id = "7i",
        name = "7-iron",
        type = ClubType.IRON,
        distance = 150
    )

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        shotRecorder = ShotRecorder(repository)
    }

    // ========================================================================
    // Recording Tests
    // ========================================================================

    @Test
    fun `recordShot stores shot in repository`() = runTest {
        val shot = createTestShot()

        shotRecorder.recordShot(shot)

        coVerify { repository.recordShot(shot) }
    }

    @Test
    fun `recordShot with parameters creates and stores shot`() = runTest {
        val pressureContext = PressureContext(isUserTagged = true)

        val recordedShot = shotRecorder.recordShot(
            club = testClub,
            missDirection = MissDirection.SLICE,
            lie = Lie.FAIRWAY,
            pressureContext = pressureContext,
            holeNumber = 3,
            notes = "Test shot"
        )

        assertNotNull(recordedShot)
        assertEquals(testClub, recordedShot.club)
        assertEquals(MissDirection.SLICE, recordedShot.missDirection)
        assertEquals(Lie.FAIRWAY, recordedShot.lie)
        assertEquals(pressureContext, recordedShot.pressureContext)
        assertEquals(3, recordedShot.holeNumber)
        assertEquals("Test shot", recordedShot.notes)

        coVerify { repository.recordShot(any()) }
    }

    @Test
    fun `recordShot generates unique IDs`() = runTest {
        val shot1 = shotRecorder.recordShot(
            club = testClub,
            missDirection = MissDirection.SLICE,
            lie = Lie.FAIRWAY,
            pressureContext = PressureContext()
        )

        val shot2 = shotRecorder.recordShot(
            club = testClub,
            missDirection = MissDirection.HOOK,
            lie = Lie.FAIRWAY,
            pressureContext = PressureContext()
        )

        assert(shot1.id != shot2.id) { "Shot IDs should be unique" }
    }

    // ========================================================================
    // Retrieval Tests
    // ========================================================================

    @Test
    fun `getRecentShots returns shots from repository`() = runTest {
        val shots = listOf(createTestShot(), createTestShot())
        coEvery { repository.getRecentShots(30) } returns flowOf(shots)

        val result = shotRecorder.getRecentShots(30)

        coVerify { repository.getRecentShots(30) }
    }

    @Test
    fun `getRecentShots with count limits results`() = runTest {
        val shots = (1..10).map { createTestShot(id = "shot-$it") }
        coEvery { repository.getRecentShots(any()) } returns flowOf(shots)

        val result = shotRecorder.getRecentShots(count = 5)

        assertEquals(5, result.size)
    }

    @Test
    fun `getShotsByClub filters by club ID`() = runTest {
        val shots = listOf(createTestShot())
        coEvery { repository.getShotsByClub("7i") } returns flowOf(shots)

        val result = shotRecorder.getShotsByClub("7i")

        coVerify { repository.getShotsByClub("7i") }
    }

    @Test
    fun `getShotsByClub with limit returns limited results`() = runTest {
        val shots = (1..10).map { createTestShot(id = "shot-$it") }
        coEvery { repository.getShotsByClub("7i") } returns flowOf(shots)

        val result = shotRecorder.getShotsByClub(clubId = "7i", limit = 3)

        assertEquals(3, result.size)
    }

    @Test
    fun `getShotsWithPressure returns pressure shots`() = runTest {
        val pressureShot = createTestShot(
            pressureContext = PressureContext(isUserTagged = true)
        )
        coEvery { repository.getShotsWithPressure() } returns flowOf(listOf(pressureShot))

        val result = shotRecorder.getShotsWithPressure()

        coVerify { repository.getShotsWithPressure() }
    }

    @Test
    fun `getPressureShotsByClub filters by club and pressure`() = runTest {
        val pressureShot = createTestShot(
            clubId = "7i",
            pressureContext = PressureContext(isUserTagged = true)
        )
        val normalShot = createTestShot(
            clubId = "7i",
            pressureContext = PressureContext()
        )
        coEvery { repository.getShotsByClub("7i") } returns flowOf(listOf(pressureShot, normalShot))

        val result = shotRecorder.getPressureShotsByClub(clubId = "7i")

        assertEquals(1, result.size)
        assertEquals(pressureShot, result.first())
    }

    @Test
    fun `getPressureShotsByClub with limit returns limited results`() = runTest {
        val pressureShots = (1..5).map {
            createTestShot(
                id = "shot-$it",
                clubId = "7i",
                pressureContext = PressureContext(isUserTagged = true)
            )
        }
        coEvery { repository.getShotsByClub("7i") } returns flowOf(pressureShots)

        val result = shotRecorder.getPressureShotsByClub(clubId = "7i", limit = 2)

        assertEquals(2, result.size)
    }

    @Test
    fun `getPressureShotsByClub with no limit returns all results`() = runTest {
        val pressureShots = (1..5).map {
            createTestShot(
                id = "shot-$it",
                clubId = "7i",
                pressureContext = PressureContext(isUserTagged = true)
            )
        }
        coEvery { repository.getShotsByClub("7i") } returns flowOf(pressureShots)

        val result = shotRecorder.getPressureShotsByClub(clubId = "7i", limit = 0)

        assertEquals(5, result.size)
    }

    // ========================================================================
    // Memory Management Tests
    // ========================================================================

    @Test
    fun `clearHistory clears all memory`() = runTest {
        shotRecorder.clearHistory()

        coVerify { repository.clearMemory() }
    }

    @Test
    fun `enforceRetentionPolicy delegates to repository`() = runTest {
        coEvery { repository.enforceRetentionPolicy() } returns 10

        val deleted = shotRecorder.enforceRetentionPolicy()

        assertEquals(10, deleted)
        coVerify { repository.enforceRetentionPolicy() }
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    private fun createTestShot(
        id: String = "test-shot-1",
        clubId: String = "7i",
        missDirection: MissDirection = MissDirection.SLICE,
        pressureContext: PressureContext = PressureContext()
    ): Shot {
        return Shot(
            id = id,
            timestamp = System.currentTimeMillis(),
            club = testClub.copy(id = clubId),
            missDirection = missDirection,
            lie = Lie.FAIRWAY,
            pressureContext = pressureContext,
            holeNumber = null,
            notes = null
        )
    }
}
