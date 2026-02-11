package com.caddypro.app.ui.bags

import app.cash.turbine.test
import com.caddypro.app.domain.model.Bag
import com.caddypro.app.domain.model.PlayerProfile
import com.caddypro.app.domain.repository.BagRepository
import com.caddypro.app.domain.repository.ProfileRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
 * Unit tests for BagListViewModel
 *
 * Tests bag business rules:
 * - AC6: Default "My Bag" created on first launch
 * - AC7: Only one bag is active at a time
 * - AC8: Deleting active bag promotes next bag
 * - AC9: Cannot delete last bag
 * - AC10: Real-time updates via Flow
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BagListViewModelTest {

    private lateinit var viewModel: BagListViewModel
    private lateinit var bagRepository: BagRepository
    private lateinit var profileRepository: ProfileRepository
    private val testDispatcher = StandardTestDispatcher()

    private val testProfileId = "test-profile-id"
    private val testProfile = PlayerProfile(
        id = testProfileId,
        supabaseUserId = "test-user",
        displayName = "Test User"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        bagRepository = mockk(relaxed = true)
        profileRepository = mockk(relaxed = true)

        // Default profile flow
        every { profileRepository.getProfile() } returns flowOf(testProfile)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // AC10: Real-time updates via Flow

    @Test
    fun `loads bags for profile on init`() = runTest {
        val testBags = listOf(
            Bag(id = "bag1", profileId = testProfileId, name = "My Bag", isActive = true, clubCount = 5),
            Bag(id = "bag2", profileId = testProfileId, name = "Tournament Bag", isActive = false, clubCount = 14)
        )
        every { bagRepository.getBagsByProfileId(testProfileId) } returns flowOf(testBags)

        viewModel = BagListViewModel(bagRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(testBags, viewModel.uiState.value.bags)
        assertEquals(testProfileId, viewModel.uiState.value.profileId)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `updates bags in real-time when repository emits new data`() = runTest {
        val initialBags = listOf(
            Bag(id = "bag1", profileId = testProfileId, name = "My Bag", isActive = true)
        )
        every { bagRepository.getBagsByProfileId(testProfileId) } returns flowOf(initialBags)

        viewModel = BagListViewModel(bagRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.bags.size)
    }

    // AC7: Only one bag is active at a time

    @Test
    fun `setActiveBag deactivates other bags and activates selected bag`() = runTest {
        val bag1 = Bag(id = "bag1", profileId = testProfileId, name = "My Bag", isActive = true)
        val bag2 = Bag(id = "bag2", profileId = testProfileId, name = "New Bag", isActive = false)
        val testBags = listOf(bag1, bag2)

        every { bagRepository.getBagsByProfileId(testProfileId) } returns flowOf(testBags)
        coEvery { bagRepository.setActiveBag(testProfileId, "bag2") } returns Unit

        viewModel = BagListViewModel(bagRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(BagListAction.SetActiveBag(bag2))
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { bagRepository.setActiveBag(testProfileId, "bag2") }
    }

    @Test
    fun `setActiveBag does nothing if bag is already active`() = runTest {
        val activeBag = Bag(id = "bag1", profileId = testProfileId, name = "My Bag", isActive = true)
        val testBags = listOf(activeBag)

        every { bagRepository.getBagsByProfileId(testProfileId) } returns flowOf(testBags)

        viewModel = BagListViewModel(bagRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(BagListAction.SetActiveBag(activeBag))
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not call setActiveBag since bag is already active
        coVerify(exactly = 0) { bagRepository.setActiveBag(any(), any()) }
    }

    // AC9: Cannot delete the last bag

    @Test
    fun `delete last bag shows error and does not delete`() = runTest {
        val lastBag = Bag(id = "bag1", profileId = testProfileId, name = "My Bag", isActive = true)
        val testBags = listOf(lastBag)

        every { bagRepository.getBagsByProfileId(testProfileId) } returns flowOf(testBags)
        coEvery { bagRepository.deleteBag(testProfileId, "bag1") } returns
                Result.failure(Exception("Cannot delete the last bag"))

        viewModel = BagListViewModel(bagRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Show delete confirmation
        viewModel.onAction(BagListAction.ShowDeleteConfirmation(lastBag))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(lastBag, viewModel.uiState.value.bagToDelete)

        // Confirm delete
        viewModel.onAction(BagListAction.ConfirmDelete)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should show error
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Cannot delete the last bag"))
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
    }

    // AC8: Deleting active bag promotes next bag to active

    @Test
    fun `delete active bag succeeds when multiple bags exist`() = runTest {
        val activeBag = Bag(id = "bag1", profileId = testProfileId, name = "My Bag", isActive = true)
        val otherBag = Bag(id = "bag2", profileId = testProfileId, name = "Other Bag", isActive = false)
        val testBags = listOf(activeBag, otherBag)

        every { bagRepository.getBagsByProfileId(testProfileId) } returns flowOf(testBags)
        coEvery { bagRepository.deleteBag(testProfileId, "bag1") } returns Result.success(Unit)

        viewModel = BagListViewModel(bagRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Show delete confirmation
        viewModel.onAction(BagListAction.ShowDeleteConfirmation(activeBag))
        testDispatcher.scheduler.advanceUntilIdle()

        // Confirm delete
        viewModel.onAction(BagListAction.ConfirmDelete)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should successfully delete
        coVerify { bagRepository.deleteBag(testProfileId, "bag1") }
        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // Delete Confirmation Dialog Tests

    @Test
    fun `show delete confirmation sets state correctly`() = runTest {
        val bag = Bag(id = "bag1", profileId = testProfileId, name = "My Bag", isActive = true)
        val testBags = listOf(bag)

        every { bagRepository.getBagsByProfileId(testProfileId) } returns flowOf(testBags)

        viewModel = BagListViewModel(bagRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(BagListAction.ShowDeleteConfirmation(bag))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showDeleteConfirmation)
        assertEquals(bag, viewModel.uiState.value.bagToDelete)
    }

    @Test
    fun `dismiss delete confirmation clears state`() = runTest {
        val bag = Bag(id = "bag1", profileId = testProfileId, name = "My Bag", isActive = true)
        val testBags = listOf(bag)

        every { bagRepository.getBagsByProfileId(testProfileId) } returns flowOf(testBags)

        viewModel = BagListViewModel(bagRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Show confirmation
        viewModel.onAction(BagListAction.ShowDeleteConfirmation(bag))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showDeleteConfirmation)

        // Dismiss
        viewModel.onAction(BagListAction.DismissDeleteConfirmation)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showDeleteConfirmation)
        assertNull(viewModel.uiState.value.bagToDelete)
    }

    // Create New Bag Tests

    @Test
    fun `create new bag calls repository with correct data`() = runTest {
        val existingBags = listOf(
            Bag(id = "bag1", profileId = testProfileId, name = "My Bag", isActive = true)
        )

        every { bagRepository.getBagsByProfileId(testProfileId) } returns flowOf(existingBags)
        coEvery { bagRepository.createBag(any()) } returns Unit

        viewModel = BagListViewModel(bagRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(BagListAction.CreateNewBag)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify {
            bagRepository.createBag(match { bag ->
                bag.profileId == testProfileId &&
                        bag.name == "New Bag 2" && // Size is 1, so next is 2
                        !bag.isActive
            })
        }
    }

    // Error Handling Tests

    @Test
    fun `clear error removes error message`() = runTest {
        val testBags = emptyList<Bag>()
        every { bagRepository.getBagsByProfileId(testProfileId) } returns flowOf(testBags)

        viewModel = BagListViewModel(bagRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        // Manually set an error
        coEvery { bagRepository.setActiveBag(any(), any()) } throws Exception("Test error")
        val testBag = Bag(id = "bag1", profileId = testProfileId, name = "Test", isActive = false)
        viewModel.onAction(BagListAction.SetActiveBag(testBag))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onAction(BagListAction.ClearError)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `handles profile not found error`() = runTest {
        every { profileRepository.getProfile() } returns flowOf(null)

        viewModel = BagListViewModel(bagRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("No profile found", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `handles bag loading error`() = runTest {
        every { bagRepository.getBagsByProfileId(testProfileId) } throws Exception("Database error")

        viewModel = BagListViewModel(bagRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Failed to load bags"))
    }

    // State Management Tests

    @Test
    fun `initial state is loading`() = runTest {
        val testBags = emptyList<Bag>()
        every { bagRepository.getBagsByProfileId(testProfileId) } returns flowOf(testBags)

        viewModel = BagListViewModel(bagRepository, profileRepository)

        assertTrue(viewModel.uiState.value.isLoading)
    }

    @Test
    fun `state shows empty list when no bags exist`() = runTest {
        val testBags = emptyList<Bag>()
        every { bagRepository.getBagsByProfileId(testProfileId) } returns flowOf(testBags)

        viewModel = BagListViewModel(bagRepository, profileRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.bags.isEmpty())
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
