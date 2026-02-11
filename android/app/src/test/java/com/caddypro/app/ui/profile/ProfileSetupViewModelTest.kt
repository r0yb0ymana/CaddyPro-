package com.caddypro.app.ui.profile

import app.cash.turbine.test
import com.caddypro.app.data.local.entities.PreferredUnits
import com.caddypro.app.domain.model.PlayerProfile
import com.caddypro.app.domain.repository.ProfileRepository
import com.caddypro.app.domain.usecase.CreateDefaultBagUseCase
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
 * Unit tests for ProfileSetupViewModel
 *
 * Tests validation logic for:
 * - Display Name (required, 2-50 characters)
 * - Handicap Index (optional, 0.0-54.0, one decimal place)
 * - Profile saving
 * - State management
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSetupViewModelTest {

    private lateinit var viewModel: ProfileSetupViewModel
    private lateinit var profileRepository: ProfileRepository
    private lateinit var createDefaultBagUseCase: CreateDefaultBagUseCase
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        profileRepository = mockk(relaxed = true)
        createDefaultBagUseCase = mockk(relaxed = true)
        viewModel = ProfileSetupViewModel(profileRepository, createDefaultBagUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // Display Name Validation Tests

    @Test
    fun `display name validation - blank name shows error`() {
        val error = viewModel.validateHandicap("")
        assertNull(error) // Blank is valid for handicap (optional field)

        viewModel.onAction(ProfileSetupAction.UpdateDisplayName(""))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.displayNameError)
    }

    @Test
    fun `display name validation - name too short shows error`() {
        viewModel.onAction(ProfileSetupAction.UpdateDisplayName("A"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Display name must be at least 2 characters", viewModel.uiState.value.displayNameError)
    }

    @Test
    fun `display name validation - name too long shows error`() {
        val longName = "A".repeat(51)
        viewModel.onAction(ProfileSetupAction.UpdateDisplayName(longName))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Display name must be less than 50 characters", viewModel.uiState.value.displayNameError)
    }

    @Test
    fun `display name validation - valid name has no error`() {
        viewModel.onAction(ProfileSetupAction.UpdateDisplayName("John Doe"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.displayNameError)
    }

    // Handicap Validation Tests - AC4: Handicap accepts 0.0-54.0 with one decimal place

    @Test
    fun `handicap validation - blank handicap is valid (optional)`() {
        val error = viewModel.validateHandicap("")
        assertNull(error)
    }

    @Test
    fun `handicap validation - negative handicap shows error`() {
        val error = viewModel.validateHandicap("-5.0")
        assertEquals("Handicap cannot be negative", error)
    }

    @Test
    fun `handicap validation - handicap above 54 shows error`() {
        val error = viewModel.validateHandicap("55.0")
        assertEquals("Handicap cannot exceed 54.0", error)
    }

    @Test
    fun `handicap validation - handicap exactly 0 is valid`() {
        val error = viewModel.validateHandicap("0.0")
        assertNull(error)
    }

    @Test
    fun `handicap validation - handicap exactly 54 is valid`() {
        val error = viewModel.validateHandicap("54.0")
        assertNull(error)
    }

    @Test
    fun `handicap validation - valid handicap with one decimal is accepted`() {
        val error = viewModel.validateHandicap("12.5")
        assertNull(error)
    }

    @Test
    fun `handicap validation - valid handicap without decimal is accepted`() {
        val error = viewModel.validateHandicap("12")
        assertNull(error)
    }

    @Test
    fun `handicap validation - handicap with two decimals shows error`() {
        val error = viewModel.validateHandicap("12.55")
        assertEquals("Handicap must have at most one decimal place", error)
    }

    @Test
    fun `handicap validation - non-numeric handicap shows error`() {
        val error = viewModel.validateHandicap("abc")
        assertEquals("Handicap must be a valid number", error)
    }

    @Test
    fun `handicap validation - typical handicaps are valid`() {
        val testCases = listOf("0.0", "5.5", "10.0", "15.8", "20.3", "36.0", "54.0")
        testCases.forEach { handicap ->
            val error = viewModel.validateHandicap(handicap)
            assertNull("Handicap $handicap should be valid", error)
        }
    }

    // State Management Tests

    @Test
    fun `initial state has default values`() {
        val state = viewModel.uiState.value

        assertEquals("", state.displayName)
        assertEquals("", state.handicapIndex)
        assertEquals(PreferredUnits.METRIC, state.preferredUnits)
        assertEquals("", state.homeCourse)
        assertFalse(state.isLoading)
        assertFalse(state.isSaved)
        assertNull(state.errorMessage)
    }

    @Test
    fun `update display name changes state`() {
        viewModel.onAction(ProfileSetupAction.UpdateDisplayName("Test User"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Test User", viewModel.uiState.value.displayName)
    }

    @Test
    fun `update handicap changes state`() {
        viewModel.onAction(ProfileSetupAction.UpdateHandicapIndex("12.5"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("12.5", viewModel.uiState.value.handicapIndex)
    }

    @Test
    fun `update preferred units changes state`() {
        viewModel.onAction(ProfileSetupAction.UpdatePreferredUnits(PreferredUnits.IMPERIAL))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PreferredUnits.IMPERIAL, viewModel.uiState.value.preferredUnits)
    }

    @Test
    fun `update home course changes state`() {
        viewModel.onAction(ProfileSetupAction.UpdateHomeCourse("Royal Melbourne"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Royal Melbourne", viewModel.uiState.value.homeCourse)
    }

    // Form Validation Tests

    @Test
    fun `form is invalid when display name is blank`() {
        viewModel.onAction(ProfileSetupAction.UpdateDisplayName(""))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isValid())
    }

    @Test
    fun `form is valid with only required fields`() {
        viewModel.onAction(ProfileSetupAction.UpdateDisplayName("John Doe"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isValid())
    }

    @Test
    fun `form is invalid with invalid handicap`() {
        viewModel.onAction(ProfileSetupAction.UpdateDisplayName("John Doe"))
        viewModel.onAction(ProfileSetupAction.UpdateHandicapIndex("55.0"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isValid())
    }

    @Test
    fun `form is valid with all fields correctly filled`() {
        viewModel.onAction(ProfileSetupAction.UpdateDisplayName("John Doe"))
        viewModel.onAction(ProfileSetupAction.UpdateHandicapIndex("12.5"))
        viewModel.onAction(ProfileSetupAction.UpdatePreferredUnits(PreferredUnits.METRIC))
        viewModel.onAction(ProfileSetupAction.UpdateHomeCourse("Royal Melbourne"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isValid())
    }

    // Save Profile Tests

    @Test
    fun `save profile with valid data succeeds`() = runTest {
        coEvery { profileRepository.saveProfile(any()) } returns Unit

        viewModel.onAction(ProfileSetupAction.UpdateDisplayName("John Doe"))
        viewModel.onAction(ProfileSetupAction.UpdateHandicapIndex("12.5"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(ProfileSetupAction.SaveProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isSaved)
        coVerify { profileRepository.saveProfile(any()) }
    }

    @Test
    fun `save profile with blank display name fails`() = runTest {
        viewModel.onAction(ProfileSetupAction.UpdateDisplayName(""))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(ProfileSetupAction.SaveProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaved)
        assertNotNull(viewModel.uiState.value.displayNameError)
        coVerify(exactly = 0) { profileRepository.saveProfile(any()) }
    }

    @Test
    fun `save profile with invalid handicap fails`() = runTest {
        viewModel.onAction(ProfileSetupAction.UpdateDisplayName("John Doe"))
        viewModel.onAction(ProfileSetupAction.UpdateHandicapIndex("60.0"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(ProfileSetupAction.SaveProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaved)
        assertNotNull(viewModel.uiState.value.handicapError)
        coVerify(exactly = 0) { profileRepository.saveProfile(any()) }
    }

    @Test
    fun `save profile handles repository error`() = runTest {
        coEvery { profileRepository.saveProfile(any()) } throws Exception("Database error")

        viewModel.onAction(ProfileSetupAction.UpdateDisplayName("John Doe"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(ProfileSetupAction.SaveProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isSaved)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Failed to save profile"))
    }

    @Test
    fun `save profile trims display name whitespace`() = runTest {
        var savedProfile: PlayerProfile? = null
        coEvery { profileRepository.saveProfile(any()) } answers {
            savedProfile = firstArg()
        }

        viewModel.onAction(ProfileSetupAction.UpdateDisplayName("  John Doe  "))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(ProfileSetupAction.SaveProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("John Doe", savedProfile?.displayName)
    }

    @Test
    fun `save profile converts handicap string to float`() = runTest {
        var savedProfile: PlayerProfile? = null
        coEvery { profileRepository.saveProfile(any()) } answers {
            savedProfile = firstArg()
        }

        viewModel.onAction(ProfileSetupAction.UpdateDisplayName("John Doe"))
        viewModel.onAction(ProfileSetupAction.UpdateHandicapIndex("12.5"))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(ProfileSetupAction.SaveProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(12.5f, savedProfile?.handicapIndex)
    }

    @Test
    fun `save profile with blank handicap saves null`() = runTest {
        var savedProfile: PlayerProfile? = null
        coEvery { profileRepository.saveProfile(any()) } answers {
            savedProfile = firstArg()
        }

        viewModel.onAction(ProfileSetupAction.UpdateDisplayName("John Doe"))
        viewModel.onAction(ProfileSetupAction.UpdateHandicapIndex(""))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(ProfileSetupAction.SaveProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(savedProfile?.handicapIndex)
    }

    @Test
    fun `clear error action removes error message`() {
        viewModel.onAction(ProfileSetupAction.UpdateDisplayName(""))
        viewModel.onAction(ProfileSetupAction.SaveProfile)
        testDispatcher.scheduler.advanceUntilIdle()

        // Should have error
        assertNotNull(viewModel.uiState.value.errorMessage)

        viewModel.onAction(ProfileSetupAction.ClearError)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.uiState.value.errorMessage)
    }
}
