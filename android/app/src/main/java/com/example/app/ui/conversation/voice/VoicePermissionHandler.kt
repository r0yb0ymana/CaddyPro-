package caddypro.ui.conversation.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles voice input permission checks and requests.
 *
 * Manages the RECORD_AUDIO permission with proper rationale and
 * graceful handling of denial scenarios.
 *
 * Spec reference: navcaddy-engine.md R7
 * Plan reference: navcaddy-engine-plan.md Task 20
 */
@Singleton
class VoicePermissionHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val PERMISSION_RECORD_AUDIO = Manifest.permission.RECORD_AUDIO
    }

    /**
     * Check if RECORD_AUDIO permission is granted.
     *
     * @return true if permission is granted, false otherwise
     */
    fun hasRecordAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            PERMISSION_RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Get permission rationale message.
     *
     * This message explains why the app needs microphone access.
     *
     * @return User-friendly rationale message
     */
    fun getPermissionRationale(): String {
        return "Microphone access is needed to use voice input. " +
                "This allows you to speak to Bones instead of typing."
    }

    /**
     * Get message for permanently denied permission.
     *
     * This message is shown when user selects "Never ask again".
     *
     * @return User-friendly message with guidance to enable in settings
     */
    fun getPermanentlyDeniedMessage(): String {
        return "Microphone permission is required for voice input. " +
                "Please enable it in app settings to use this feature."
    }

    /**
     * Get message for temporarily denied permission.
     *
     * This message is shown after initial permission denial.
     *
     * @return User-friendly message encouraging permission grant
     */
    fun getTemporarilyDeniedMessage(): String {
        return "Microphone permission is needed to use voice input. " +
                "Please grant permission to continue."
    }
}
