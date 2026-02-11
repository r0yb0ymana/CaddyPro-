package com.caddypro.app.ui.clubs

import com.caddypro.app.domain.model.Club
import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.domain.model.MissBias
import com.caddypro.app.domain.repository.ClubRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ClubDetailViewModel
 *
 * Tests club validation:
 * - AC12: Carry distance must be <= total distance
 * - AC14: Miss bias selector
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClubDetailViewModelTest {

    private lateinit var viewModel: ClubDetailViewModel
    private lateinit var clubRepository: ClubRepository
    private val testDispatcher = StandardTestDispatcher()

    private val testBagId = "test-bag-id"

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        clubRepository = mockk(relaxed = true)
        viewModel = ClubDetailViewModel(clubRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Initialization Tests

    @Test
    fun `initForAdd sets bagId and default values`() = runTest {
        viewModel.initForAdd(testBagId)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(testBagId, state.bagId)
        assertNull(state.clubId)
        assertEquals("", state.name)
        assertEquals(ClubType.IRON, state.clubType)
    }

    @Test
    fun `initForEdit populates state with club data`() = runTest {
        val club = Club(
            id = "club1",
            bagId = testBagId,
            name = "7 Iron",
            type = ClubType.IRON,
            loft = 34f,
            carryDistance = 145,
            totalDistance = 155,
            missBias = MissBias.SLICE
        )

        viewModel.initForEdit(club)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("club1", state.clubId)
        assertEquals(testBagId, state.bagId)
        assertEquals("7 Iron", state.name)
        assertEquals(ClubType.IRON, state.clubType)
        assertEquals("34.0", state.loft)
        assertEquals("145", state.carryDistance)
        assertEquals("155", state.totalDistance)
        assertEquals(MissBias.SLICE, state.missBias)
    }

    // Name Validation Tests

    @Test
    fun `updateName validates blank name`() = runTest {
        viewModel.onAction(ClubDetailAction.UpdateName(""))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Club name is required", viewModel.uiState.value.nameError)
    }

    @Test
    fun `updateName validates name too short`() = runTest {
        viewModel.onAction(ClubDetailAction.UpdateName("D"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Club name must be at least 2 characters",
            viewModel.uiState.value.nameError
        )
    }

    @Test
    fun `updateName validates name too long`() = runTest {
        val longName = "A".repeat(31)
        viewModel.onAction(ClubDetailAction.UpdateName(longName))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Club name must be less than 30 characters",
            viewModel.uiState.value.nameError
        )
    }

    @Test
    fun `updateName accepts valid name`() = runTest {
        viewModel.onAction(ClubDetailAction.UpdateName("7 Iron"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.nameError)
        assertEquals("7 Iron", viewModel.uiState.value.name)
    }

    // AC12: Carry <= Total Distance Validation Tests

    @Test
    fun `updateCarryDistance validates carry exceeds total`() = runTest {
        viewModel.initForAdd(testBagId)
        viewModel.onAction(ClubDetailAction.UpdateTotalDistance("150"))
        viewModel.onAction(ClubDetailAction.UpdateCarryDistance("160"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Carry distance cannot exceed total distance",
            viewModel.uiState.value.carryDistanceError
        )
    }

    @Test
    fun `updateCarryDistance accepts carry less than total`() = runTest {
        viewModel.initForAdd(testBagId)
        viewModel.onAction(ClubDetailAction.UpdateTotalDistance("155"))
        viewModel.onAction(ClubDetailAction.UpdateCarryDistance("145"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.carryDistanceError)
    }

    @Test
    fun `updateCarryDistance accepts carry equal to total`() = runTest {
        viewModel.initForAdd(testBagId)
        viewModel.onAction(ClubDetailAction.UpdateTotalDistance("150"))
        viewModel.onAction(ClubDetailAction.UpdateCarryDistance("150"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.carryDistanceError)
    }

    @Test
    fun `updateTotalDistance validates when less than carry`() = runTest {
        viewModel.initForAdd(testBagId)
        viewModel.onAction(ClubDetailAction.UpdateCarryDistance("160"))
        viewModel.onAction(ClubDetailAction.UpdateTotalDistance("150"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Carry distance cannot exceed total distance",
            viewModel.uiState.value.carryDistanceError
        )
    }

    @Test
    fun `updateCarryDistance validates non-numeric input`() = runTest {
        viewModel.onAction(ClubDetailAction.UpdateCarryDistance("abc"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Carry distance must be a valid number",
            viewModel.uiState.value.carryDistanceError
        )
    }

    @Test
    fun `updateCarryDistance validates negative distance`() = runTest {
        viewModel.onAction(ClubDetailAction.UpdateCarryDistance("-10"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Carry distance cannot be negative",
            viewModel.uiState.value.carryDistanceError
        )
    }

    @Test
    fun `updateCarryDistance validates unrealistic distance`() = runTest {
        viewModel.onAction(ClubDetailAction.UpdateCarryDistance("600"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Carry distance seems unrealistic (max 500 yards)",
            viewModel.uiState.value.carryDistanceError
        )
    }

    // Miss Bias Tests (AC14)

    @Test
    fun `updateMissBias changes selected bias`() = runTest {
        viewModel.initForAdd(testBagId)
        viewModel.onAction(ClubDetailAction.UpdateMissBias(MissBias.SLICE))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(MissBias.SLICE, viewModel.uiState.value.missBias)
    }

    @Test
    fun `updateMissBias accepts all bias options`() = runTest {
        viewModel.initForAdd(testBagId)

        MissBias.values().forEach { bias ->
            viewModel.onAction(ClubDetailAction.UpdateMissBias(bias))
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(bias, viewModel.uiState.value.missBias)
        }
    }

    // Save Club Tests

    @Test
    fun `saveClub succeeds with valid data`() = runTest {
        coEvery { clubRepository.createClub(any()) } returns Result.success(Unit)

        viewModel.initForAdd(testBagId)
        viewModel.onAction(ClubDetailAction.UpdateName("7 Iron"))
        viewModel.onAction(ClubDetailAction.UpdateCarryDistance("145"))
        viewModel.onAction(ClubDetailAction.UpdateTotalDistance("155"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(ClubDetailAction.SaveClub)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        coVerify { clubRepository.createClub(any()) }
    }

    @Test
    fun `saveClub fails with invalid name`() = runTest {
        viewModel.initForAdd(testBagId)
        viewModel.onAction(ClubDetailAction.UpdateName(""))
        viewModel.onAction(ClubDetailAction.UpdateCarryDistance("145"))
        viewModel.onAction(ClubDetailAction.UpdateTotalDistance("155"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(ClubDetailAction.SaveClub)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaved)
        assertNotNull(viewModel.uiState.value.nameError)
        coVerify(exactly = 0) { clubRepository.createClub(any()) }
    }

    @Test
    fun `saveClub fails when carry exceeds total`() = runTest {
        viewModel.initForAdd(testBagId)
        viewModel.onAction(ClubDetailAction.UpdateName("7 Iron"))
        viewModel.onAction(ClubDetailAction.UpdateCarryDistance("160"))
        viewModel.onAction(ClubDetailAction.UpdateTotalDistance("150"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(ClubDetailAction.SaveClub)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaved)
        assertEquals(
            "Please fix the errors before saving",
            viewModel.uiState.value.errorMessage
        )
        coVerify(exactly = 0) { clubRepository.createClub(any()) }
    }

    @Test
    fun `saveClub updates existing club`() = runTest {
        val existingClub = Club(
            id = "club1",
            bagId = testBagId,
            name = "7 Iron",
            type = ClubType.IRON,
            carryDistance = 145,
            totalDistance = 155
        )

        coEvery { clubRepository.updateClub(any()) } returns Result.success(Unit)

        viewModel.initForEdit(existingClub)
        viewModel.onAction(ClubDetailAction.UpdateCarryDistance("150"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(ClubDetailAction.SaveClub)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        coVerify { clubRepository.updateClub(any()) }
    }

    @Test
    fun `saveClub handles repository error`() = runTest {
        coEvery { clubRepository.createClub(any()) } returns
                Result.failure(Exception("Maximum 14 clubs allowed per bag"))

        viewModel.initForAdd(testBagId)
        viewModel.onAction(ClubDetailAction.UpdateName("15th Club"))
        viewModel.onAction(ClubDetailAction.UpdateCarryDistance("145"))
        viewModel.onAction(ClubDetailAction.UpdateTotalDistance("155"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(ClubDetailAction.SaveClub)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaved)
        assertEquals(
            "Maximum 14 clubs allowed per bag",
            viewModel.uiState.value.errorMessage
        )
    }

    // Loft Validation Tests (optional field)

    @Test
    fun `updateLoft accepts blank value`() = runTest {
        viewModel.onAction(ClubDetailAction.UpdateLoft(""))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.loftError)
    }

    @Test
    fun `updateLoft validates negative loft`() = runTest {
        viewModel.onAction(ClubDetailAction.UpdateLoft("-5"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Loft cannot be negative",
            viewModel.uiState.value.loftError
        )
    }

    @Test
    fun `updateLoft validates loft exceeds 90`() = runTest {
        viewModel.onAction(ClubDetailAction.UpdateLoft("95"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "Loft cannot exceed 90 degrees",
            viewModel.uiState.value.loftError
        )
    }

    @Test
    fun `updateLoft accepts valid loft`() = runTest {
        viewModel.onAction(ClubDetailAction.UpdateLoft("34.5"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.loftError)
        assertEquals("34.5", viewModel.uiState.value.loft)
    }
}
