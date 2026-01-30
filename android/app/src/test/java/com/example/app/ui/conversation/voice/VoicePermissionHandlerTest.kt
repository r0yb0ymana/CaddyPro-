package caddypro.ui.conversation.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for VoicePermissionHandler.
 *
 * Tests permission checking and rationale messages.
 *
 * Spec reference: navcaddy-engine.md R7
 * Plan reference: navcaddy-engine-plan.md Task 20
 */
class VoicePermissionHandlerTest {

    private lateinit var context: Context
    private lateinit var permissionHandler: VoicePermissionHandler

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        permissionHandler = VoicePermissionHandler(context)

        // Mock ContextCompat static methods
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `hasRecordAudioPermission returns true when granted`() {
        // Given: Permission is granted
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_GRANTED

        // When: Check permission
        val hasPermission = permissionHandler.hasRecordAudioPermission()

        // Then: Should return true
        assertTrue(hasPermission)
    }

    @Test
    fun `hasRecordAudioPermission returns false when denied`() {
        // Given: Permission is denied
        every {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        } returns PackageManager.PERMISSION_DENIED

        // When: Check permission
        val hasPermission = permissionHandler.hasRecordAudioPermission()

        // Then: Should return false
        assertFalse(hasPermission)
    }

    @Test
    fun `getPermissionRationale returns non-empty message`() {
        // When: Get permission rationale
        val rationale = permissionHandler.getPermissionRationale()

        // Then: Should return non-empty message
        assertNotNull(rationale)
        assertTrue(rationale.isNotBlank())
        assertTrue(rationale.contains("microphone", ignoreCase = true))
    }

    @Test
    fun `getPermanentlyDeniedMessage returns non-empty message`() {
        // When: Get permanently denied message
        val message = permissionHandler.getPermanentlyDeniedMessage()

        // Then: Should return non-empty message
        assertNotNull(message)
        assertTrue(message.isNotBlank())
        assertTrue(message.contains("settings", ignoreCase = true))
    }

    @Test
    fun `getTemporarilyDeniedMessage returns non-empty message`() {
        // When: Get temporarily denied message
        val message = permissionHandler.getTemporarilyDeniedMessage()

        // Then: Should return non-empty message
        assertNotNull(message)
        assertTrue(message.isNotBlank())
        assertTrue(message.contains("permission", ignoreCase = true))
    }
}
