package caddypro.domain.caddy.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for LiveCaddySettings model.
 *
 * Validates:
 * - Default values match spec
 * - Companion factory methods produce correct presets
 * - Data class equality and immutability
 *
 * Spec reference: live-caddy-mode.md R7 (Safety and Distraction Controls)
 * Plan reference: live-caddy-mode-plan.md Task 12
 */
class LiveCaddySettingsTest {

    /**
     * Test: Default constructor creates settings with spec-defined defaults.
     *
     * Validates:
     * - lowDistractionMode = false
     * - autoLockPrevention = false
     * - largeTouchTargets = false
     * - reducedAnimations = false
     * - hapticFeedback = true (enabled for shot logging)
     */
    @Test
    fun `default constructor creates correct default settings`() {
        // When
        val settings = LiveCaddySettings()

        // Then
        assertFalse("lowDistractionMode should default to false", settings.lowDistractionMode)
        assertFalse("autoLockPrevention should default to false", settings.autoLockPrevention)
        assertFalse("largeTouchTargets should default to false", settings.largeTouchTargets)
        assertFalse("reducedAnimations should default to false", settings.reducedAnimations)
        assertTrue("hapticFeedback should default to true", settings.hapticFeedback)
    }

    /**
     * Test: default() factory method matches default constructor.
     *
     * Validates:
     * - Factory method is consistent with constructor defaults
     * - Provides explicit API for default settings
     */
    @Test
    fun `default factory method matches default constructor`() {
        // When
        val constructorDefaults = LiveCaddySettings()
        val factoryDefaults = LiveCaddySettings.default()

        // Then
        assertEquals(
            "default() should match default constructor",
            constructorDefaults,
            factoryDefaults
        )
    }

    /**
     * Test: outdoor() factory method enables all distraction-reducing features.
     *
     * Validates:
     * - All distraction controls enabled
     * - Haptic feedback enabled
     * - Optimized for on-course use
     */
    @Test
    fun `outdoor factory method enables all distraction features`() {
        // When
        val settings = LiveCaddySettings.outdoor()

        // Then
        assertTrue("lowDistractionMode should be enabled", settings.lowDistractionMode)
        assertTrue("autoLockPrevention should be enabled", settings.autoLockPrevention)
        assertTrue("largeTouchTargets should be enabled", settings.largeTouchTargets)
        assertTrue("reducedAnimations should be enabled", settings.reducedAnimations)
        assertTrue("hapticFeedback should be enabled", settings.hapticFeedback)
    }

    /**
     * Test: Data class copy with individual field changes.
     *
     * Validates:
     * - Immutability via copy
     * - Individual field updates preserve other values
     * - Typical ViewModel/UI interaction pattern
     */
    @Test
    fun `copy allows individual field updates`() {
        // Given
        val original = LiveCaddySettings.default()

        // When
        val updated = original.copy(lowDistractionMode = true)

        // Then
        assertTrue("lowDistractionMode should be updated", updated.lowDistractionMode)
        assertFalse("autoLockPrevention should be unchanged", updated.autoLockPrevention)
        assertFalse("largeTouchTargets should be unchanged", updated.largeTouchTargets)
        assertFalse("reducedAnimations should be unchanged", updated.reducedAnimations)
        assertTrue("hapticFeedback should be unchanged", updated.hapticFeedback)

        // Original unchanged
        assertFalse("Original should be immutable", original.lowDistractionMode)
    }

    /**
     * Test: Equality comparison works correctly.
     *
     * Validates:
     * - Data class structural equality
     * - Settings with same values are equal
     */
    @Test
    fun `equality comparison works correctly`() {
        // Given
        val settings1 = LiveCaddySettings(
            lowDistractionMode = true,
            largeTouchTargets = true,
            hapticFeedback = false
        )
        val settings2 = LiveCaddySettings(
            lowDistractionMode = true,
            largeTouchTargets = true,
            hapticFeedback = false
        )
        val settings3 = LiveCaddySettings(
            lowDistractionMode = false,
            largeTouchTargets = true,
            hapticFeedback = false
        )

        // Then
        assertEquals("Identical settings should be equal", settings1, settings2)
        assertTrue("Equal settings should have same hashCode", settings1.hashCode() == settings2.hashCode())
        assertFalse("Different settings should not be equal", settings1 == settings3)
    }

    /**
     * Test: Custom settings configuration.
     *
     * Validates:
     * - Can create arbitrary combinations
     * - All fields settable independently
     */
    @Test
    fun `custom settings can be created with any combination`() {
        // When
        val customSettings = LiveCaddySettings(
            lowDistractionMode = true,
            autoLockPrevention = false,
            largeTouchTargets = true,
            reducedAnimations = false,
            hapticFeedback = true
        )

        // Then
        assertTrue(customSettings.lowDistractionMode)
        assertFalse(customSettings.autoLockPrevention)
        assertTrue(customSettings.largeTouchTargets)
        assertFalse(customSettings.reducedAnimations)
        assertTrue(customSettings.hapticFeedback)
    }

    /**
     * Test: Haptic feedback can be disabled.
     *
     * Validates:
     * - Haptic feedback is configurable
     * - Users can opt out of tactile feedback
     */
    @Test
    fun `haptic feedback can be disabled`() {
        // When
        val settings = LiveCaddySettings(hapticFeedback = false)

        // Then
        assertFalse("hapticFeedback should be disableable", settings.hapticFeedback)
    }

    /**
     * Test: All distraction features can be enabled independently.
     *
     * Validates:
     * - Each feature is independent
     * - Users can enable subset of features
     */
    @Test
    fun `distraction features can be enabled independently`() {
        // When
        val onlyLargeTargets = LiveCaddySettings(largeTouchTargets = true)
        val onlyReducedAnimations = LiveCaddySettings(reducedAnimations = true)
        val onlyAutoLock = LiveCaddySettings(autoLockPrevention = true)

        // Then
        assertTrue(onlyLargeTargets.largeTouchTargets)
        assertFalse(onlyLargeTargets.lowDistractionMode)
        assertFalse(onlyLargeTargets.reducedAnimations)

        assertTrue(onlyReducedAnimations.reducedAnimations)
        assertFalse(onlyReducedAnimations.lowDistractionMode)
        assertFalse(onlyReducedAnimations.largeTouchTargets)

        assertTrue(onlyAutoLock.autoLockPrevention)
        assertFalse(onlyAutoLock.lowDistractionMode)
        assertFalse(onlyAutoLock.largeTouchTargets)
    }

    /**
     * Test: toString provides readable representation.
     *
     * Validates:
     * - Data class toString includes all fields
     * - Useful for logging and debugging
     */
    @Test
    fun `toString provides readable representation`() {
        // When
        val settings = LiveCaddySettings(lowDistractionMode = true, hapticFeedback = false)
        val string = settings.toString()

        // Then
        assertTrue("toString should include lowDistractionMode", string.contains("lowDistractionMode"))
        assertTrue("toString should include hapticFeedback", string.contains("hapticFeedback"))
        assertTrue("toString should include values", string.contains("true") && string.contains("false"))
    }
}
