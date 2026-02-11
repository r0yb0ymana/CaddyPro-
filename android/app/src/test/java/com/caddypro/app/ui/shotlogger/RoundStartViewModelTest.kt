package com.caddypro.app.ui.shotlogger

import com.caddypro.app.domain.model.Bag
import com.caddypro.app.domain.model.PlayerProfile
import com.caddypro.app.data.local.entities.PreferredUnits
import com.caddypro.app.domain.repository.BagRepository
import com.caddypro.app.domain.repository.ProfileRepository
import com.caddypro.app.domain.repository.RoundRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RoundStartViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var roundRepository: RoundRepository
    private lateinit var profileRepository: ProfileRepository
    private lateinit var bagRepository: BagRepository

    private val testProfile = PlayerProfile(
        id = "profile-123",
        displayName = "Test Player",
        handicapIndex = 15.0f,
        preferredUnits = PreferredUnits.IMPERIAL
    )

    private val testBag = Bag(
        id = "bag-1",
        profileId = "profile-123",
        name = "Main Bag",
        isActive = true
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        roundRepository = mockk(relaxed = true)
        profileRepository = mockk(relaxed = true)
        bagRepository = mockk(relaxed = true)

        every { profileRepository.getProfile() } returns flowOf(testProfile)
        coEvery { bagRepository.getActiveBagSync("profile-123") } returns testBag
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = RoundStartViewModel(
        roundRepository, profileRepository, bagRepository
    )

    // AC2: Active bag auto-selected and shown
    @Test
    fun `init loads active bag name`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals("Main Bag", vm.uiState.value.activeBagName)
        assertTrue(vm.uiState.value.hasActiveBag)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test
    fun `init shows no bag when none active`() = runTest {
        coEvery { bagRepository.getActiveBagSync("profile-123") } returns null
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals("No bag", vm.uiState.value.activeBagName)
        assertFalse(vm.uiState.value.hasActiveBag)
    }

    @Test
    fun `updateCourseName updates state`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onAction(RoundStartAction.UpdateCourseName("Kingston Heath"))

        assertEquals("Kingston Heath", vm.uiState.value.courseName)
        assertNull(vm.uiState.value.courseNameError)
    }

    @Test
    fun `updateHolesPlayed updates state`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onAction(RoundStartAction.UpdateHolesPlayed(9))

        assertEquals(9, vm.uiState.value.holesPlayed)
    }

    // AC1: User can start a round with course name
    @Test
    fun `startRound validates course name required`() = runTest {
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onAction(RoundStartAction.StartRound)

        assertEquals("Course name is required", vm.uiState.value.courseNameError)
    }

    @Test
    fun `startRound validates active bag required`() = runTest {
        coEvery { bagRepository.getActiveBagSync("profile-123") } returns null
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onAction(RoundStartAction.UpdateCourseName("Royal Melbourne"))
        vm.onAction(RoundStartAction.StartRound)

        assertEquals("Set up a bag before starting a round", vm.uiState.value.errorMessage)
    }

    // AC1: Start round with valid data
    @Test
    fun `startRound creates round with valid data`() = runTest {
        coEvery { roundRepository.createRound(any()) } returns Result.success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onAction(RoundStartAction.UpdateCourseName("Royal Melbourne"))
        vm.onAction(RoundStartAction.StartRound)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.roundStarted)
        coVerify {
            roundRepository.createRound(match {
                it.courseName == "Royal Melbourne" && it.holesPlayed == 18
            })
        }
    }

    @Test
    fun `startRound trims course name`() = runTest {
        coEvery { roundRepository.createRound(any()) } returns Result.success(Unit)
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onAction(RoundStartAction.UpdateCourseName("  Royal Melbourne  "))
        vm.onAction(RoundStartAction.StartRound)
        advanceUntilIdle()

        coVerify {
            roundRepository.createRound(match { it.courseName == "Royal Melbourne" })
        }
    }

    // AC3: Only one active round at a time
    @Test
    fun `startRound shows error when active round exists`() = runTest {
        coEvery { roundRepository.createRound(any()) } returns Result.failure(
            IllegalStateException("An active round already exists.")
        )
        val vm = createViewModel()
        advanceUntilIdle()

        vm.onAction(RoundStartAction.UpdateCourseName("Royal Melbourne"))
        vm.onAction(RoundStartAction.StartRound)
        advanceUntilIdle()

        assertFalse(vm.uiState.value.roundStarted)
        assertEquals("An active round already exists.", vm.uiState.value.errorMessage)
    }
}
