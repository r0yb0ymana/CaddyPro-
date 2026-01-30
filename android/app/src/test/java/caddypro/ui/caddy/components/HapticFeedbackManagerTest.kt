package caddypro.ui.caddy.components

import android.view.HapticFeedbackConstants
import android.view.View
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for HapticFeedbackManager.
 *
 * Tests haptic feedback patterns and settings enforcement:
 * - Success haptic (CONFIRM)
 * - Error haptic (REJECT)
 * - Warning haptic (LONG_PRESS)
 * - Tap haptic (CONTEXT_CLICK)
 * - Settings-aware enable/disable
 * - No-op when disabled
 *
 * Spec reference: live-caddy-mode.md R6 (Real-Time Shot Logger), R7 (haptic feedback setting)
 * Plan reference: live-caddy-mode-plan.md Task 22
 * Acceptance criteria: A4 (haptic confirmation)
 */
class HapticFeedbackManagerTest {

    private lateinit var view: View
    private lateinit var hapticFeedbackManager: HapticFeedbackManager

    @Before
    fun setup() {
        view = mockk(relaxed = true)
    }

    @Test
    fun `success triggers CONFIRM haptic when enabled`() {
        // Given: Haptic feedback is enabled
        hapticFeedbackManager = HapticFeedbackManager(view, enabled = true)

        // When: Success haptic is triggered
        hapticFeedbackManager.success()

        // Then: View performs CONFIRM haptic
        verify {
            view.performHapticFeedback(
                HapticFeedbackConstants.CONFIRM,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    @Test
    fun `success does not trigger haptic when disabled`() {
        // Given: Haptic feedback is disabled
        hapticFeedbackManager = HapticFeedbackManager(view, enabled = false)

        // When: Success haptic is attempted
        hapticFeedbackManager.success()

        // Then: No haptic feedback is performed
        verify(exactly = 0) {
            view.performHapticFeedback(any(), any())
        }
    }

    @Test
    fun `error triggers REJECT haptic when enabled`() {
        // Given: Haptic feedback is enabled
        hapticFeedbackManager = HapticFeedbackManager(view, enabled = true)

        // When: Error haptic is triggered
        hapticFeedbackManager.error()

        // Then: View performs REJECT haptic
        verify {
            view.performHapticFeedback(
                HapticFeedbackConstants.REJECT,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    @Test
    fun `error does not trigger haptic when disabled`() {
        // Given: Haptic feedback is disabled
        hapticFeedbackManager = HapticFeedbackManager(view, enabled = false)

        // When: Error haptic is attempted
        hapticFeedbackManager.error()

        // Then: No haptic feedback is performed
        verify(exactly = 0) {
            view.performHapticFeedback(any(), any())
        }
    }

    @Test
    fun `warning triggers LONG_PRESS haptic when enabled`() {
        // Given: Haptic feedback is enabled
        hapticFeedbackManager = HapticFeedbackManager(view, enabled = true)

        // When: Warning haptic is triggered
        hapticFeedbackManager.warning()

        // Then: View performs LONG_PRESS haptic
        verify {
            view.performHapticFeedback(
                HapticFeedbackConstants.LONG_PRESS,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    @Test
    fun `warning does not trigger haptic when disabled`() {
        // Given: Haptic feedback is disabled
        hapticFeedbackManager = HapticFeedbackManager(view, enabled = false)

        // When: Warning haptic is attempted
        hapticFeedbackManager.warning()

        // Then: No haptic feedback is performed
        verify(exactly = 0) {
            view.performHapticFeedback(any(), any())
        }
    }

    @Test
    fun `tap triggers CONTEXT_CLICK haptic when enabled`() {
        // Given: Haptic feedback is enabled
        hapticFeedbackManager = HapticFeedbackManager(view, enabled = true)

        // When: Tap haptic is triggered
        hapticFeedbackManager.tap()

        // Then: View performs CONTEXT_CLICK haptic
        verify {
            view.performHapticFeedback(
                HapticFeedbackConstants.CONTEXT_CLICK,
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }

    @Test
    fun `tap does not trigger haptic when disabled`() {
        // Given: Haptic feedback is disabled
        hapticFeedbackManager = HapticFeedbackManager(view, enabled = false)

        // When: Tap haptic is attempted
        hapticFeedbackManager.tap()

        // Then: No haptic feedback is performed
        verify(exactly = 0) {
            view.performHapticFeedback(any(), any())
        }
    }

    @Test
    fun `disabled factory method creates no-op manager`() {
        // Given: Factory method creates disabled manager
        hapticFeedbackManager = HapticFeedbackManager.disabled(view)

        // When: Success haptic is attempted
        hapticFeedbackManager.success()

        // Then: No haptic feedback is performed
        verify(exactly = 0) {
            view.performHapticFeedback(any(), any())
        }
    }

    @Test
    fun `multiple haptic calls respect enabled state`() {
        // Given: Haptic feedback is enabled
        hapticFeedbackManager = HapticFeedbackManager(view, enabled = true)

        // When: Multiple haptic types are triggered
        hapticFeedbackManager.success()
        hapticFeedbackManager.tap()
        hapticFeedbackManager.warning()

        // Then: All haptics are performed
        verify(exactly = 3) {
            view.performHapticFeedback(any(), any())
        }
    }

    @Test
    fun `haptic feedback always ignores global setting`() {
        // Given: Haptic feedback is enabled
        hapticFeedbackManager = HapticFeedbackManager(view, enabled = true)

        // When: Any haptic is triggered
        hapticFeedbackManager.success()

        // Then: FLAG_IGNORE_GLOBAL_SETTING is always used
        verify {
            view.performHapticFeedback(
                any(),
                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
        }
    }
}
