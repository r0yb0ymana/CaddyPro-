package com.caddypro.app.ui.forecaster

import com.caddypro.app.domain.model.AdjustedClubDistance
import com.caddypro.app.domain.model.ShotDirection

/**
 * Forecaster HUD user actions
 */
sealed class ForecasterAction {
    data object RefreshWeather : ForecasterAction()
    data class UpdateShotDirection(val direction: ShotDirection) : ForecasterAction()
    data class SelectClub(val club: AdjustedClubDistance) : ForecasterAction()
    data object DismissDetail : ForecasterAction()
    data object LocationPermissionGranted : ForecasterAction()
    data object LocationPermissionDenied : ForecasterAction()
}
