package com.caddypro.app.ui.holemap

/**
 * Hole Map screen user actions
 */
sealed class HoleMapAction {
    data object LocationPermissionGranted : HoleMapAction()
    data object LocationPermissionDenied : HoleMapAction()
    data object RefreshLocation : HoleMapAction()
    data object MapReady : HoleMapAction()
    data object NextHole : HoleMapAction()
    data object PreviousHole : HoleMapAction()
    data object ShowFlagDialog : HoleMapAction()
    data object DismissFlagDialog : HoleMapAction()
    data class SubmitFlag(val description: String) : HoleMapAction()
}
