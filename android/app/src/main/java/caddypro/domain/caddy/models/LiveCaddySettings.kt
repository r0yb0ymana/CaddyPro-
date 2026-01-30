package caddypro.domain.caddy.models

/**
 * Settings for Low Distraction Mode in Live Caddy.
 *
 * These settings optimize the on-course experience for outdoor visibility
 * and reduced phone interaction during a round.
 *
 * Spec reference: live-caddy-mode.md R7 (Safety and Distraction Controls)
 * Plan reference: live-caddy-mode-plan.md Task 12
 *
 * @property lowDistractionMode Master toggle for low distraction mode.
 *                              When enabled, activates all distraction-reducing features.
 * @property autoLockPrevention Prevents screen from auto-locking during active HUD use.
 *                              Android: FLAG_KEEP_SCREEN_ON when HUD is visible.
 *                              Default: false (respect system auto-lock)
 * @property largeTouchTargets Increases touch target sizes to minimum 48dp per Material 3.
 *                             Improves usability with gloves or in motion.
 *                             Default: false
 * @property reducedAnimations Disables or simplifies UI animations for faster visual feedback.
 *                             Reduces motion and improves focus.
 *                             Default: false
 * @property hapticFeedback Enables haptic feedback for key actions (shot logging, etc.).
 *                         Provides tactile confirmation without looking at screen.
 *                         Default: true (haptics enabled)
 */
data class LiveCaddySettings(
    val lowDistractionMode: Boolean = false,
    val autoLockPrevention: Boolean = false,
    val largeTouchTargets: Boolean = false,
    val reducedAnimations: Boolean = false,
    val hapticFeedback: Boolean = true
) {
    companion object {
        /**
         * Default settings instance with recommended values.
         *
         * Haptic feedback is enabled by default to support quick shot logging.
         * All other distraction controls are opt-in.
         */
        fun default() = LiveCaddySettings()

        /**
         * Recommended settings for outdoor round play.
         *
         * Enables all distraction-reducing features for optimal on-course experience.
         */
        fun outdoor() = LiveCaddySettings(
            lowDistractionMode = true,
            autoLockPrevention = true,
            largeTouchTargets = true,
            reducedAnimations = true,
            hapticFeedback = true
        )
    }
}
