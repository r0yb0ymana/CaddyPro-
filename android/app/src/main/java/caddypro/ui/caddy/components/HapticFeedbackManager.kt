package caddypro.ui.caddy.components

import android.view.HapticFeedbackConstants
import android.view.View

/**
 * Manager for haptic feedback patterns in Live Caddy Mode.
 *
 * Provides semantic haptic feedback types aligned with Material Design guidelines:
 * - SUCCESS: Single tap confirmation (CONFIRM) - for successful operations
 * - ERROR: Double tap rejection (REJECT) - for failed operations
 * - WARNING: Long press (LONG_PRESS) - for warnings or important notifications
 *
 * All haptic feedback respects the user's settings and can be globally disabled.
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger), R7 (haptic feedback setting)
 * Plan reference: live-caddy-mode-plan.md Task 22
 * Acceptance criteria: A4 (haptic confirmation)
 *
 * Material Design haptic patterns:
 * - Success: CONFIRM (single tap) - light, crisp feedback
 * - Error: REJECT (double tap) - heavier, more pronounced feedback
 * - Warning: LONG_PRESS - extended vibration for attention
 *
 * @property view The Android View used to trigger haptic feedback
 * @property enabled Whether haptic feedback is enabled (from settings)
 */
class HapticFeedbackManager(
    private val view: View,
    private val enabled: Boolean
) {

    /**
     * Trigger success haptic feedback.
     *
     * Used for:
     * - Shot successfully logged
     * - Round successfully started/ended
     * - Settings saved
     *
     * Material Design pattern: Single tap (CONFIRM)
     */
    fun success() {
        if (!enabled) return

        view.performHapticFeedback(
            HapticFeedbackConstants.CONFIRM,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    /**
     * Trigger error haptic feedback.
     *
     * Used for:
     * - Shot logging failed
     * - Network error
     * - Validation error
     *
     * Material Design pattern: Double tap (REJECT)
     */
    fun error() {
        if (!enabled) return

        view.performHapticFeedback(
            HapticFeedbackConstants.REJECT,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    /**
     * Trigger warning haptic feedback.
     *
     * Used for:
     * - Low battery warning
     * - GPS signal lost
     * - Offline mode activated
     *
     * Material Design pattern: Long press (LONG_PRESS)
     */
    fun warning() {
        if (!enabled) return

        view.performHapticFeedback(
            HapticFeedbackConstants.LONG_PRESS,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    /**
     * Trigger generic tap feedback.
     *
     * Used for:
     * - Button presses
     * - Selection changes
     * - General UI interactions
     *
     * Material Design pattern: Context click
     */
    fun tap() {
        if (!enabled) return

        view.performHapticFeedback(
            HapticFeedbackConstants.CONTEXT_CLICK,
            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
        )
    }

    companion object {
        /**
         * Create a no-op haptic feedback manager for disabled state.
         *
         * @return HapticFeedbackManager that performs no haptic feedback
         */
        fun disabled(view: View): HapticFeedbackManager {
            return HapticFeedbackManager(view, enabled = false)
        }
    }
}
