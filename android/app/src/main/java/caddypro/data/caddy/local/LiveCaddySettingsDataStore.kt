package caddypro.data.caddy.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import caddypro.domain.caddy.models.LiveCaddySettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore-based persistence for LiveCaddySettings.
 *
 * Provides reactive Flow-based access to settings with automatic persistence
 * across app restarts. All settings changes are atomic and isolated.
 *
 * Spec reference: live-caddy-mode.md R7 (Safety and Distraction Controls)
 * Plan reference: live-caddy-mode-plan.md Task 12
 * Acceptance criteria: Settings persist across app restarts, Flow-based updates work reactively
 *
 * @property dataStore Application-level DataStore instance provided by Hilt.
 *                     Shared with other app preferences.
 */
@Singleton
class LiveCaddySettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    /**
     * Preference keys for Live Caddy settings.
     *
     * Keys are scoped with "live_caddy_" prefix to avoid collisions with
     * other app preferences in the shared DataStore.
     */
    private object PreferencesKeys {
        val LOW_DISTRACTION_MODE = booleanPreferencesKey("live_caddy_low_distraction_mode")
        val AUTO_LOCK_PREVENTION = booleanPreferencesKey("live_caddy_auto_lock_prevention")
        val LARGE_TOUCH_TARGETS = booleanPreferencesKey("live_caddy_large_touch_targets")
        val REDUCED_ANIMATIONS = booleanPreferencesKey("live_caddy_reduced_animations")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("live_caddy_haptic_feedback")
    }

    /**
     * Reactive Flow of current settings.
     *
     * Emits LiveCaddySettings whenever any setting changes. Handles DataStore
     * IOExceptions gracefully by emitting default settings.
     *
     * @return Flow that emits current settings on subscription and on every update.
     */
    fun getSettings(): Flow<LiveCaddySettings> {
        return dataStore.data
            .catch { exception ->
                // Handle read errors (e.g., corrupted file, disk full)
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                LiveCaddySettings(
                    lowDistractionMode = preferences[PreferencesKeys.LOW_DISTRACTION_MODE] ?: false,
                    autoLockPrevention = preferences[PreferencesKeys.AUTO_LOCK_PREVENTION] ?: false,
                    largeTouchTargets = preferences[PreferencesKeys.LARGE_TOUCH_TARGETS] ?: false,
                    reducedAnimations = preferences[PreferencesKeys.REDUCED_ANIMATIONS] ?: false,
                    hapticFeedback = preferences[PreferencesKeys.HAPTIC_FEEDBACK] ?: true // Default enabled
                )
            }
    }

    /**
     * Save updated settings to DataStore.
     *
     * Operation is atomic - all settings are updated in a single transaction.
     * If the write fails (e.g., disk full), the existing settings remain unchanged.
     *
     * @param settings New settings to persist.
     * @throws IOException if DataStore write fails (propagated to caller for error handling).
     */
    suspend fun saveSettings(settings: LiveCaddySettings) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOW_DISTRACTION_MODE] = settings.lowDistractionMode
            preferences[PreferencesKeys.AUTO_LOCK_PREVENTION] = settings.autoLockPrevention
            preferences[PreferencesKeys.LARGE_TOUCH_TARGETS] = settings.largeTouchTargets
            preferences[PreferencesKeys.REDUCED_ANIMATIONS] = settings.reducedAnimations
            preferences[PreferencesKeys.HAPTIC_FEEDBACK] = settings.hapticFeedback
        }
    }

    /**
     * Reset all settings to default values.
     *
     * Convenience method for restoring factory defaults.
     */
    suspend fun resetToDefaults() {
        saveSettings(LiveCaddySettings.default())
    }

    /**
     * Enable outdoor mode preset.
     *
     * Convenience method for quickly activating all distraction-reducing features.
     */
    suspend fun enableOutdoorMode() {
        saveSettings(LiveCaddySettings.outdoor())
    }
}
