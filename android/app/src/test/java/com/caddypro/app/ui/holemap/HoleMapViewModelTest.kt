package com.caddypro.app.ui.holemap

import android.location.Location
import com.caddypro.app.data.local.entities.PreferredUnits
import com.caddypro.app.data.repository.LocationRepository
import com.caddypro.app.domain.model.CourseOverlayBundle
import com.caddypro.app.domain.model.GreenData
import com.caddypro.app.domain.model.HazardData
import com.caddypro.app.domain.model.HazardType
import com.caddypro.app.domain.model.LatLngPoint
import com.caddypro.app.domain.model.PlayerProfile
import com.caddypro.app.domain.repository.CourseDataRepository
import com.caddypro.app.domain.repository.ProfileRepository
import com.caddypro.app.domain.usecase.DistanceCalculator
import io.mockk.coEvery
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HoleMapViewModelTest {

    private lateinit var locationRepository: LocationRepository
    private lateinit var courseDataRepository: CourseDataRepository
    private lateinit var distanceCalculator: DistanceCalculator
    private lateinit var profileRepository: ProfileRepository
    private lateinit var viewModel: HoleMapViewModel

    private val testDispatcher = StandardTestDispatcher()

    private val testLocation = mockk<Location>().apply {
        every { latitude } returns -37.8136
        every { longitude } returns 144.9631
    }

    private val testBundle = CourseOverlayBundle(
        greens = listOf(
            GreenData("g1", -37.814, 144.964, 1),
            GreenData("g2", -37.815, 144.965, 2),
            GreenData("g3", -37.816, 144.966, 3)
        ),
        hazards = listOf(
            HazardData(
                "h1", HazardType.BUNKER, "Bunker",
                listOf(LatLngPoint(-37.814, 144.964), LatLngPoint(-37.815, 144.965), LatLngPoint(-37.814, 144.965))
            )
        ),
        courseName = "Royal Melbourne"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        locationRepository = mockk()
        courseDataRepository = mockk()
        distanceCalculator = DistanceCalculator()
        profileRepository = mockk()

        // Default: no permission, no profile
        every { locationRepository.hasLocationPermission() } returns false
        coEvery { locationRepository.getLastKnownLocation() } returns null
        coEvery { locationRepository.getCurrentLocation() } returns null
        every { profileRepository.getProfile() } returns flowOf(null)
        coEvery { courseDataRepository.getCourseData(any(), any()) } returns Result.success(CourseOverlayBundle())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HoleMapViewModel {
        return HoleMapViewModel(
            locationRepository = locationRepository,
            courseDataRepository = courseDataRepository,
            distanceCalculator = distanceCalculator,
            profileRepository = profileRepository
        )
    }

    @Test
    fun `init checks location permission`() = runTest {
        every { locationRepository.hasLocationPermission() } returns false
        viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.hasLocationPermission)
    }

    @Test
    fun `init loads location when permission granted`() = runTest {
        every { locationRepository.hasLocationPermission() } returns true
        coEvery { locationRepository.getLastKnownLocation() } returns testLocation

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(-37.8136, viewModel.uiState.value.playerLatitude!!, 0.001)
        assertEquals(144.9631, viewModel.uiState.value.playerLongitude!!, 0.001)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // AC24: Location permission requested
    @Test
    fun `location permission granted triggers location load`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { locationRepository.getLastKnownLocation() } returns testLocation
        viewModel.onAction(HoleMapAction.LocationPermissionGranted)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.hasLocationPermission)
        assertNotNull(viewModel.uiState.value.playerLatitude)
    }

    @Test
    fun `location permission denied shows error`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(HoleMapAction.LocationPermissionDenied)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasLocationPermission)
        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    // AC4: Hazard overlays
    @Test
    fun `course data loaded after location available`() = runTest {
        every { locationRepository.hasLocationPermission() } returns true
        coEvery { locationRepository.getLastKnownLocation() } returns testLocation
        coEvery { courseDataRepository.getCourseData(any(), any()) } returns Result.success(testBundle)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.greens.size)
        assertEquals(1, viewModel.uiState.value.hazards.size)
        assertEquals("Royal Melbourne", viewModel.uiState.value.courseName)
        assertTrue(viewModel.uiState.value.hasCourseData)
    }

    // AC8: Distance to green
    @Test
    fun `distance calculated to selected green`() = runTest {
        every { locationRepository.hasLocationPermission() } returns true
        coEvery { locationRepository.getLastKnownLocation() } returns testLocation
        coEvery { courseDataRepository.getCourseData(any(), any()) } returns Result.success(testBundle)

        viewModel = createViewModel()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.distanceToGreenYards)
        assertNotNull(viewModel.uiState.value.distanceToGreenMeters)
        assertTrue(viewModel.uiState.value.distanceToGreenYards!! > 0)
    }

    // AC9: Hole navigation
    @Test
    fun `next hole advances selected green`() = runTest {
        every { locationRepository.hasLocationPermission() } returns true
        coEvery { locationRepository.getLastKnownLocation() } returns testLocation
        coEvery { courseDataRepository.getCourseData(any(), any()) } returns Result.success(testBundle)

        viewModel = createViewModel()
        advanceUntilIdle()

        val initialIndex = viewModel.uiState.value.selectedGreenIndex
        viewModel.onAction(HoleMapAction.NextHole)

        assertEquals(initialIndex + 1, viewModel.uiState.value.selectedGreenIndex)
    }

    @Test
    fun `previous hole decreases selected green`() = runTest {
        every { locationRepository.hasLocationPermission() } returns true
        coEvery { locationRepository.getLastKnownLocation() } returns testLocation
        coEvery { courseDataRepository.getCourseData(any(), any()) } returns Result.success(testBundle)

        viewModel = createViewModel()
        advanceUntilIdle()

        // Move forward first, then back
        viewModel.onAction(HoleMapAction.NextHole)
        val index = viewModel.uiState.value.selectedGreenIndex
        viewModel.onAction(HoleMapAction.PreviousHole)

        assertEquals(index - 1, viewModel.uiState.value.selectedGreenIndex)
    }

    @Test
    fun `cannot go past last green`() = runTest {
        every { locationRepository.hasLocationPermission() } returns true
        coEvery { locationRepository.getLastKnownLocation() } returns testLocation
        coEvery { courseDataRepository.getCourseData(any(), any()) } returns Result.success(testBundle)

        viewModel = createViewModel()
        advanceUntilIdle()

        // Try to go past last green
        repeat(10) { viewModel.onAction(HoleMapAction.NextHole) }

        assertEquals(testBundle.greens.size - 1, viewModel.uiState.value.selectedGreenIndex)
    }

    @Test
    fun `cannot go before first green`() = runTest {
        every { locationRepository.hasLocationPermission() } returns true
        coEvery { locationRepository.getLastKnownLocation() } returns testLocation
        coEvery { courseDataRepository.getCourseData(any(), any()) } returns Result.success(testBundle)

        viewModel = createViewModel()
        advanceUntilIdle()

        // Ensure we're at index 0
        repeat(10) { viewModel.onAction(HoleMapAction.PreviousHole) }
        assertEquals(0, viewModel.uiState.value.selectedGreenIndex)
    }

    // AC22: Flag incorrect data
    @Test
    fun `show flag dialog sets state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(HoleMapAction.ShowFlagDialog)
        assertTrue(viewModel.uiState.value.showFlagDialog)
    }

    @Test
    fun `dismiss flag dialog clears state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(HoleMapAction.ShowFlagDialog)
        viewModel.onAction(HoleMapAction.DismissFlagDialog)
        assertFalse(viewModel.uiState.value.showFlagDialog)
    }

    @Test
    fun `submit flag closes dialog and shows confirmation`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(HoleMapAction.ShowFlagDialog)
        viewModel.onAction(HoleMapAction.SubmitFlag("Wrong bunker"))

        assertFalse(viewModel.uiState.value.showFlagDialog)
        assertTrue(viewModel.uiState.value.flagSubmitted)
    }

    // Profile preferences
    @Test
    fun `loads metric preference from profile`() = runTest {
        every { profileRepository.getProfile() } returns flowOf(
            PlayerProfile(displayName = "Test", preferredUnits = PreferredUnits.METRIC)
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.useMetric)
    }

    @Test
    fun `loads imperial preference from profile`() = runTest {
        every { profileRepository.getProfile() } returns flowOf(
            PlayerProfile(displayName = "Test", preferredUnits = PreferredUnits.IMPERIAL)
        )

        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.useMetric)
    }

    @Test
    fun `map ready action sets state`() = runTest {
        viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onAction(HoleMapAction.MapReady)
        assertTrue(viewModel.uiState.value.mapReady)
    }

    // AC21: Graceful degradation
    @Test
    fun `handles course data failure gracefully`() = runTest {
        every { locationRepository.hasLocationPermission() } returns true
        coEvery { locationRepository.getLastKnownLocation() } returns testLocation
        coEvery { courseDataRepository.getCourseData(any(), any()) } returns Result.failure(RuntimeException("API Error"))

        viewModel = createViewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.hasCourseData)
        assertFalse(viewModel.uiState.value.isFetchingCourseData)
    }

    @Test
    fun `no location available shows error`() = runTest {
        every { locationRepository.hasLocationPermission() } returns true
        coEvery { locationRepository.getLastKnownLocation() } returns null
        coEvery { locationRepository.getCurrentLocation() } returns null

        viewModel = createViewModel()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.playerLatitude)
        assertNotNull(viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isLoading)
    }
}
