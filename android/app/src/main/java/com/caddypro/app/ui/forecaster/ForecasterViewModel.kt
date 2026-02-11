package com.caddypro.app.ui.forecaster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.caddypro.app.data.repository.LocationRepository
import com.caddypro.app.domain.model.Club
import com.caddypro.app.domain.model.ShotDirection
import com.caddypro.app.domain.model.WeatherData
import com.caddypro.app.domain.repository.BagRepository
import com.caddypro.app.domain.repository.ClubRepository
import com.caddypro.app.domain.repository.ProfileRepository
import com.caddypro.app.domain.repository.WeatherRepository
import com.caddypro.app.domain.usecase.CarryAdjustmentCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Forecaster HUD ViewModel
 *
 * Combines weather data + active bag clubs + location to produce adjusted distances.
 * AC2: Auto-refresh every 10 minutes
 * AC5: Every club shows adjusted carry
 * AC8: Adjustments recalculate when weather refreshes
 */
@HiltViewModel
class ForecasterViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val bagRepository: BagRepository,
    private val clubRepository: ClubRepository,
    private val profileRepository: ProfileRepository,
    private val locationRepository: LocationRepository,
    private val calculator: CarryAdjustmentCalculator
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForecasterState())
    val uiState: StateFlow<ForecasterState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var clubs: List<Club> = emptyList()

    init {
        checkLocationPermission()
        loadCachedWeather()
        loadClubs()
    }

    fun onAction(action: ForecasterAction) {
        when (action) {
            is ForecasterAction.RefreshWeather -> refreshWeather()
            is ForecasterAction.UpdateShotDirection -> updateShotDirection(action.direction)
            is ForecasterAction.SelectClub -> selectClub(action.club)
            is ForecasterAction.DismissDetail -> dismissDetail()
            is ForecasterAction.LocationPermissionGranted -> onLocationPermissionGranted()
            is ForecasterAction.LocationPermissionDenied -> onLocationPermissionDenied()
        }
    }

    private fun checkLocationPermission() {
        val hasPermission = locationRepository.hasLocationPermission()
        _uiState.value = _uiState.value.copy(hasLocationPermission = hasPermission)
    }

    private fun loadCachedWeather() {
        viewModelScope.launch {
            val cached = weatherRepository.getCachedWeather()
            if (cached != null) {
                _uiState.value = _uiState.value.copy(
                    weather = cached,
                    hasWeatherData = true,
                    isWeatherStale = cached.isStale,
                    isLoading = false
                )
                recalculateAdjustments()
            }
        }
    }

    private fun loadClubs() {
        viewModelScope.launch {
            try {
                // Get profile to determine units and profileId
                val profile = profileRepository.getProfile().firstOrNull()
                if (profile == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No profile found. Set up your profile first."
                    )
                    return@launch
                }

                val useMetric = profile.preferredUnits.name == "METRIC"
                _uiState.value = _uiState.value.copy(useMetric = useMetric)

                // Get active bag
                val activeBag = bagRepository.getActiveBagSync(profile.id)
                if (activeBag == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "No active bag. Set up a bag first."
                    )
                    return@launch
                }

                // Collect clubs from the active bag
                clubRepository.getClubsByBagId(activeBag.id).collect { clubList ->
                    clubs = clubList
                    recalculateAdjustments()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load clubs: ${e.message}"
                )
            }
        }
    }

    private fun onLocationPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasLocationPermission = true)
        fetchWeatherWithLocation()
        startAutoRefresh()
    }

    private fun onLocationPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            hasLocationPermission = false,
            isLoading = false
        )
    }

    fun startAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            // Initial fetch
            fetchWeatherWithLocation()
            // AC2: Refresh every 10 minutes
            while (true) {
                delay(WeatherData.REFRESH_INTERVAL_MS)
                fetchWeatherWithLocation()
            }
        }
    }

    fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    private fun fetchWeatherWithLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)

            val location = locationRepository.getCurrentLocation()
            if (location != null) {
                val altitudeM = if (location.hasAltitude()) location.altitude else 0.0
                _uiState.value = _uiState.value.copy(altitudeM = altitudeM)

                val result = weatherRepository.getWeather(location.latitude, location.longitude)
                result.fold(
                    onSuccess = { weather ->
                        _uiState.value = _uiState.value.copy(
                            weather = weather,
                            hasWeatherData = true,
                            isWeatherStale = weather.isStale,
                            isRefreshing = false,
                            errorMessage = null
                        )
                        recalculateAdjustments()
                    },
                    onFailure = {
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            errorMessage = if (_uiState.value.hasWeatherData) null
                            else "Unable to fetch weather data"
                        )
                    }
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    errorMessage = if (!_uiState.value.hasWeatherData)
                        "Location unavailable" else null
                )
            }
        }
    }

    private fun refreshWeather() {
        if (_uiState.value.hasLocationPermission) {
            fetchWeatherWithLocation()
        }
    }

    private fun updateShotDirection(direction: ShotDirection) {
        _uiState.value = _uiState.value.copy(shotDirection = direction)
        recalculateAdjustments()
    }

    private fun recalculateAdjustments() {
        val weather = _uiState.value.weather ?: return
        if (clubs.isEmpty()) return

        val adjustments = calculator.calculateAdjustments(
            clubs = clubs,
            weather = weather,
            altitudeM = _uiState.value.altitudeM,
            shotDirection = _uiState.value.shotDirection
        )
        _uiState.value = _uiState.value.copy(adjustedDistances = adjustments)
    }

    private fun selectClub(club: com.caddypro.app.domain.model.AdjustedClubDistance) {
        _uiState.value = _uiState.value.copy(
            selectedClub = club,
            showAdjustmentDetail = true
        )
    }

    private fun dismissDetail() {
        _uiState.value = _uiState.value.copy(
            showAdjustmentDetail = false,
            selectedClub = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
