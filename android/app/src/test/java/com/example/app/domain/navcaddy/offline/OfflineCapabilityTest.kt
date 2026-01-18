package caddypro.domain.navcaddy.offline

import caddypro.domain.navcaddy.models.IntentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for OfflineCapability.
 *
 * Tests offline intent classification and capability checks.
 *
 * Spec reference: navcaddy-engine.md C6 (Offline behavior), NG4 (limited local intents)
 * Plan reference: navcaddy-engine-plan.md Task 24
 */
class OfflineCapabilityTest {

    @Test
    fun `offline available intents include score entry`() {
        // Given: SCORE_ENTRY intent
        val intent = IntentType.SCORE_ENTRY

        // Then: Should be offline available
        assertTrue(
            "SCORE_ENTRY should be offline available",
            OfflineCapability.isOfflineAvailable(intent)
        )
        assertTrue(
            "SCORE_ENTRY should be in offline intents set",
            OfflineCapability.OFFLINE_AVAILABLE_INTENTS.contains(intent)
        )
    }

    @Test
    fun `offline available intents include stats lookup`() {
        // Given: STATS_LOOKUP intent
        val intent = IntentType.STATS_LOOKUP

        // Then: Should be offline available
        assertTrue(
            "STATS_LOOKUP should be offline available",
            OfflineCapability.isOfflineAvailable(intent)
        )
    }

    @Test
    fun `offline available intents include equipment info`() {
        // Given: EQUIPMENT_INFO intent
        val intent = IntentType.EQUIPMENT_INFO

        // Then: Should be offline available
        assertTrue(
            "EQUIPMENT_INFO should be offline available",
            OfflineCapability.isOfflineAvailable(intent)
        )
    }

    @Test
    fun `offline available intents include round management`() {
        // Given: ROUND_START and ROUND_END intents
        val roundStart = IntentType.ROUND_START
        val roundEnd = IntentType.ROUND_END

        // Then: Both should be offline available
        assertTrue(
            "ROUND_START should be offline available",
            OfflineCapability.isOfflineAvailable(roundStart)
        )
        assertTrue(
            "ROUND_END should be offline available",
            OfflineCapability.isOfflineAvailable(roundEnd)
        )
    }

    @Test
    fun `offline available intents include settings`() {
        // Given: SETTINGS_CHANGE intent
        val intent = IntentType.SETTINGS_CHANGE

        // Then: Should be offline available
        assertTrue(
            "SETTINGS_CHANGE should be offline available",
            OfflineCapability.isOfflineAvailable(intent)
        )
    }

    @Test
    fun `offline available intents include help`() {
        // Given: HELP_REQUEST intent
        val intent = IntentType.HELP_REQUEST

        // Then: Should be offline available
        assertTrue(
            "HELP_REQUEST should be offline available",
            OfflineCapability.isOfflineAvailable(intent)
        )
    }

    @Test
    fun `offline available intents include club adjustment`() {
        // Given: CLUB_ADJUSTMENT intent
        val intent = IntentType.CLUB_ADJUSTMENT

        // Then: Should be offline available
        assertTrue(
            "CLUB_ADJUSTMENT should be offline available",
            OfflineCapability.isOfflineAvailable(intent)
        )
    }

    @Test
    fun `offline available intents include pattern query`() {
        // Given: PATTERN_QUERY intent
        val intent = IntentType.PATTERN_QUERY

        // Then: Should be offline available
        assertTrue(
            "PATTERN_QUERY should be offline available",
            OfflineCapability.isOfflineAvailable(intent)
        )
    }

    @Test
    fun `shot recommendation requires online`() {
        // Given: SHOT_RECOMMENDATION intent
        val intent = IntentType.SHOT_RECOMMENDATION

        // Then: Should require online
        assertFalse(
            "SHOT_RECOMMENDATION should not be offline available",
            OfflineCapability.isOfflineAvailable(intent)
        )
        assertTrue(
            "SHOT_RECOMMENDATION should require online",
            OfflineCapability.requiresOnline(intent)
        )
        assertTrue(
            "SHOT_RECOMMENDATION should be in online required set",
            OfflineCapability.ONLINE_REQUIRED_INTENTS.contains(intent)
        )
    }

    @Test
    fun `recovery check requires online`() {
        // Given: RECOVERY_CHECK intent
        val intent = IntentType.RECOVERY_CHECK

        // Then: Should require online
        assertFalse(
            "RECOVERY_CHECK should not be offline available",
            OfflineCapability.isOfflineAvailable(intent)
        )
        assertTrue(
            "RECOVERY_CHECK should require online",
            OfflineCapability.requiresOnline(intent)
        )
    }

    @Test
    fun `drill request requires online`() {
        // Given: DRILL_REQUEST intent
        val intent = IntentType.DRILL_REQUEST

        // Then: Should require online
        assertFalse(
            "DRILL_REQUEST should not be offline available",
            OfflineCapability.isOfflineAvailable(intent)
        )
        assertTrue(
            "DRILL_REQUEST should require online",
            OfflineCapability.requiresOnline(intent)
        )
    }

    @Test
    fun `weather check requires online`() {
        // Given: WEATHER_CHECK intent
        val intent = IntentType.WEATHER_CHECK

        // Then: Should require online
        assertFalse(
            "WEATHER_CHECK should not be offline available",
            OfflineCapability.isOfflineAvailable(intent)
        )
        assertTrue(
            "WEATHER_CHECK should require online",
            OfflineCapability.requiresOnline(intent)
        )
    }

    @Test
    fun `course info requires online`() {
        // Given: COURSE_INFO intent
        val intent = IntentType.COURSE_INFO

        // Then: Should require online
        assertFalse(
            "COURSE_INFO should not be offline available",
            OfflineCapability.isOfflineAvailable(intent)
        )
        assertTrue(
            "COURSE_INFO should require online",
            OfflineCapability.requiresOnline(intent)
        )
    }

    @Test
    fun `feedback requires online`() {
        // Given: FEEDBACK intent
        val intent = IntentType.FEEDBACK

        // Then: Should require online
        assertFalse(
            "FEEDBACK should not be offline available",
            OfflineCapability.isOfflineAvailable(intent)
        )
        assertTrue(
            "FEEDBACK should require online",
            OfflineCapability.requiresOnline(intent)
        )
    }

    @Test
    fun `offline limitation messages are helpful`() {
        // Given: Online-only intents
        val onlineIntents = listOf(
            IntentType.SHOT_RECOMMENDATION,
            IntentType.RECOVERY_CHECK,
            IntentType.DRILL_REQUEST,
            IntentType.WEATHER_CHECK,
            IntentType.COURSE_INFO,
            IntentType.FEEDBACK
        )

        // Then: Each should have a helpful message
        onlineIntents.forEach { intent ->
            val message = OfflineCapability.getOfflineLimitationMessage(intent)
            assertTrue(
                "Message for $intent should not be empty",
                message.isNotEmpty()
            )
            assertTrue(
                "Message for $intent should mention offline or connection",
                message.contains("offline", ignoreCase = true) ||
                        message.contains("connection", ignoreCase = true) ||
                        message.contains("internet", ignoreCase = true)
            )
        }
    }

    @Test
    fun `offline intents are prioritized correctly`() {
        // When: Get prioritized offline intents
        val prioritized = OfflineCapability.getOfflineIntentsPrioritized()

        // Then: Should have correct order and content
        assertTrue(
            "Prioritized list should not be empty",
            prioritized.isNotEmpty()
        )
        assertTrue(
            "SCORE_ENTRY should be first priority",
            prioritized.first() == IntentType.SCORE_ENTRY
        )
        assertTrue(
            "All prioritized intents should be offline available",
            prioritized.all { OfflineCapability.isOfflineAvailable(it) }
        )
    }

    @Test
    fun `offline mode message is user-friendly`() {
        // When: Get offline mode message
        val message = OfflineCapability.getOfflineModeMessage()

        // Then: Should be helpful and informative
        assertTrue(
            "Message should not be empty",
            message.isNotEmpty()
        )
        assertTrue(
            "Message should mention offline",
            message.contains("offline", ignoreCase = true)
        )
        assertTrue(
            "Message should mention available features",
            message.contains("score", ignoreCase = true) ||
                    message.contains("stats", ignoreCase = true)
        )
    }

    @Test
    fun `online mode message is user-friendly`() {
        // When: Get online mode message
        val message = OfflineCapability.getOnlineModeMessage()

        // Then: Should be positive and informative
        assertTrue(
            "Message should not be empty",
            message.isNotEmpty()
        )
        assertTrue(
            "Message should mention online or connection",
            message.contains("online", ignoreCase = true) ||
                    message.contains("back", ignoreCase = true)
        )
    }

    @Test
    fun `no intent is both offline and online-required`() {
        // Given: All offline and online intent sets
        val offlineIntents = OfflineCapability.OFFLINE_AVAILABLE_INTENTS
        val onlineIntents = OfflineCapability.ONLINE_REQUIRED_INTENTS

        // Then: Sets should be mutually exclusive
        val intersection = offlineIntents.intersect(onlineIntents)
        assertTrue(
            "No intent should be in both offline and online-required sets",
            intersection.isEmpty()
        )
    }

    @Test
    fun `offline available intents count is reasonable`() {
        // When: Get offline intents
        val offlineIntents = OfflineCapability.OFFLINE_AVAILABLE_INTENTS

        // Then: Should have reasonable number (enough to be useful)
        assertTrue(
            "Should have at least 5 offline intents for useful offline mode",
            offlineIntents.size >= 5
        )
    }

    @Test
    fun `all defined intents are categorized`() {
        // Given: All intent types
        val allIntents = IntentType.values().toSet()
        val offlineIntents = OfflineCapability.OFFLINE_AVAILABLE_INTENTS
        val onlineIntents = OfflineCapability.ONLINE_REQUIRED_INTENTS

        // When: Combine offline and online intents
        val categorized = offlineIntents + onlineIntents

        // Then: All intents should be categorized
        assertEquals(
            "All intents should be categorized as offline or online",
            allIntents,
            categorized
        )
    }
}
