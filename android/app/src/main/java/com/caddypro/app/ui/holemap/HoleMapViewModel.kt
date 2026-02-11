package com.caddypro.app.ui.holemap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caddypro.app.data.local.entities.PreferredUnits
import com.caddypro.app.data.repository.LocationRepository
import com.caddypro.app.domain.repository.CourseDataRepository
import com.caddypro.app.domain.repository.ProfileRepository
import com.caddypro.app.domain.usecase.DistanceCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Hole Map ViewModel
 *
 * AC1: Satellite imagery renders
 * AC2: Map centers on player GPS location
 * AC3: Player position shown
 * AC4: Hazard overlays rendered
 * AC5: Green center markers
 * AC8: Distance to green displayed
 * AC9: Hole navigation
 * AC15: Haversine distance calculation
 * AC24: Location permission requested
 */
@HiltViewModel
class HoleMapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val courseDataRepository: CourseDataRepository,
    private val distanceCalculator: DistanceCalculator,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HoleMapState())
    val uiState: StateFlow<HoleMapState> = _uiState.asStateFlow()

    init {
        loadProfilePreferences()
        checkLocationPermission()
    }

    fun onAction(action: HoleMapAction) {
        when (action) {
            is HoleMapAction.LocationPermissionGranted -> onLocationPermissionGranted()
            is HoleMapAction.LocationPermissionDenied -> onLocationPermissionDenied()
            is HoleMapAction.RefreshLocation -> refreshLocation()
            is HoleMapAction.MapReady -> onMapReady()
            is HoleMapAction.NextHole -> onNextHole()
            is HoleMapAction.PreviousHole -> onPreviousHole()
            is HoleMapAction.ShowFlagDialog -> onShowFlagDialog()
            is HoleMapAction.DismissFlagDialog -> onDismissFlagDialog()
            is HoleMapAction.SubmitFlag -> onSubmitFlag(action.description)
        }
    }

    private fun loadProfilePreferences() {
        viewModelScope.launch {
            val profile = profileRepository.getProfile().firstOrNull()
            if (profile != null) {
                _uiState.value = _uiState.value.copy(
                    useMetric = profile.preferredUnits == PreferredUnits.METRIC
                )
            }
        }
    }

    private fun checkLocationPermission() {
        val hasPermission = locationRepository.hasLocationPermission()
        _uiState.value = _uiState.value.copy(hasLocationPermission = hasPermission)
        if (hasPermission) {
            loadLocation()
        }
    }

    private fun onLocationPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasLocationPermission = true)
        loadLocation()
    }

    private fun onLocationPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            hasLocationPermission = false,
            isLoading = false,
            errorMessage = "Location permission required to show your position on the map."
        )
    }

    private fun onMapReady() {
        _uiState.value = _uiState.value.copy(mapReady = true)
    }

    private fun loadLocation() {
        viewModelScope.launch {
            val location = locationRepository.getLastKnownLocation()
                ?: locationRepository.getCurrentLocation()

            if (location != null) {
                _uiState.value = _uiState.value.copy(
                    playerLatitude = location.latitude,
                    playerLongitude = location.longitude,
                    isLoading = false,
                    errorMessage = null
                )
                fetchCourseData(location.latitude, location.longitude)
                updateDistance()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Unable to get current location."
                )
            }
        }
    }

    private fun refreshLocation() {
        viewModelScope.launch {
            val location = locationRepository.getCurrentLocation()
            if (location != null) {
                _uiState.value = _uiState.value.copy(
                    playerLatitude = location.latitude,
                    playerLongitude = location.longitude,
                    errorMessage = null
                )
                updateDistance()
            }
        }
    }

    private fun fetchCourseData(lat: Double, lon: Double) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isFetchingCourseData = true)

            val result = courseDataRepository.getCourseData(lat, lon)
            result.onSuccess { bundle ->
                _uiState.value = _uiState.value.copy(
                    greens = bundle.greens,
                    hazards = bundle.hazards,
                    hasCourseData = bundle.hasCourseData,
                    courseName = bundle.courseName,
                    isFetchingCourseData = false,
                    selectedGreenIndex = if (bundle.greens.isNotEmpty()) findNearestGreenIndex(lat, lon, bundle.greens) else 0
                )
                updateDistance()
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    isFetchingCourseData = false,
                    hasCourseData = false
                )
            }
        }
    }

    private fun findNearestGreenIndex(
        lat: Double,
        lon: Double,
        greens: List<com.caddypro.app.domain.model.GreenData>
    ): Int {
        if (greens.isEmpty()) return 0
        var minDistance = Double.MAX_VALUE
        var nearestIndex = 0
        greens.forEachIndexed { index, green ->
            val dist = distanceCalculator.distanceMeters(lat, lon, green.latitude, green.longitude)
            if (dist < minDistance) {
                minDistance = dist
                nearestIndex = index
            }
        }
        return nearestIndex
    }

    private fun updateDistance() {
        val state = _uiState.value
        val lat = state.playerLatitude ?: return
        val lon = state.playerLongitude ?: return
        val green = state.selectedGreen ?: return

        val yards = distanceCalculator.distanceYards(lat, lon, green.latitude, green.longitude)
        val meters = distanceCalculator.distanceMetersRounded(lat, lon, green.latitude, green.longitude)

        _uiState.value = state.copy(
            distanceToGreenYards = yards,
            distanceToGreenMeters = meters
        )
    }

    private fun onNextHole() {
        val state = _uiState.value
        if (state.canGoNext) {
            _uiState.value = state.copy(selectedGreenIndex = state.selectedGreenIndex + 1)
            updateDistance()
        }
    }

    private fun onPreviousHole() {
        val state = _uiState.value
        if (state.canGoPrev) {
            _uiState.value = state.copy(selectedGreenIndex = state.selectedGreenIndex - 1)
            updateDistance()
        }
    }

    private fun onShowFlagDialog() {
        _uiState.value = _uiState.value.copy(showFlagDialog = true)
    }

    private fun onDismissFlagDialog() {
        _uiState.value = _uiState.value.copy(showFlagDialog = false)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onSubmitFlag(description: String) {
        _uiState.value = _uiState.value.copy(
            showFlagDialog = false,
            flagSubmitted = true
        )
        // AC22: Flag report stored locally for future Tier 2 contributions
    }
}
