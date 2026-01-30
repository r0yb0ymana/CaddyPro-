package caddypro.domain.navcaddy.offline

import caddypro.domain.navcaddy.fallback.LocalIntentSuggestions
import caddypro.domain.navcaddy.models.IntentType
import caddypro.domain.navcaddy.normalizer.InputNormalizer
import caddypro.domain.navcaddy.normalizer.NormalizationResult
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for OfflineIntentHandler.
 *
 * Tests local intent processing without LLM classification.
 *
 * Spec reference: navcaddy-engine.md C6 (Offline behavior)
 * Plan reference: navcaddy-engine-plan.md Task 24
 */
class OfflineIntentHandlerTest {

    private lateinit var inputNormalizer: InputNormalizer
    private lateinit var localSuggestions: LocalIntentSuggestions
    private lateinit var offlineIntentHandler: OfflineIntentHandler

    @Before
    fun setup() {
        inputNormalizer = mockk(relaxed = true)
        localSuggestions = mockk(relaxed = true)
        offlineIntentHandler = OfflineIntentHandler(inputNormalizer, localSuggestions)

        // Default: normalizer returns input unchanged
        every { inputNormalizer.normalize(any()) } answers {
            NormalizationResult(
                normalized = firstArg<String>().lowercase(),
                modifications = emptyList()
            )
        }
    }

    @Test
    fun `processOffline matches score entry intent with strong confidence`() = runTest {
        // Given: Input clearly indicates score entry
        val input = "enter score for hole 5"

        // When
        val result = offlineIntentHandler.processOffline(input)

        // Then: Should match SCORE_ENTRY intent
        assertTrue("Should match offline intent", result is OfflineIntentHandler.OfflineResult.Match)
        val matchResult = result as OfflineIntentHandler.OfflineResult.Match
        assertEquals(
            "Should match SCORE_ENTRY intent",
            IntentType.SCORE_ENTRY,
            matchResult.parsedIntent.intent
        )
        assertTrue(
            "Should have high confidence",
            matchResult.parsedIntent.confidence >= 0.7f
        )
    }

    @Test
    fun `processOffline matches stats lookup intent`() = runTest {
        // Given: Input requests stats
        val input = "show my stats"

        // When
        val result = offlineIntentHandler.processOffline(input)

        // Then: Should match STATS_LOOKUP intent
        assertTrue("Should match offline intent", result is OfflineIntentHandler.OfflineResult.Match)
        val matchResult = result as OfflineIntentHandler.OfflineResult.Match
        assertEquals(
            "Should match STATS_LOOKUP intent",
            IntentType.STATS_LOOKUP,
            matchResult.parsedIntent.intent
        )
    }

    @Test
    fun `processOffline matches equipment info intent`() = runTest {
        // Given: Input requests equipment information
        val input = "what's in my bag"

        // When
        val result = offlineIntentHandler.processOffline(input)

        // Then: Should match EQUIPMENT_INFO intent
        assertTrue("Should match offline intent", result is OfflineIntentHandler.OfflineResult.Match)
        val matchResult = result as OfflineIntentHandler.OfflineResult.Match
        assertEquals(
            "Should match EQUIPMENT_INFO intent",
            IntentType.EQUIPMENT_INFO,
            matchResult.parsedIntent.intent
        )
    }

    @Test
    fun `processOffline matches round start intent`() = runTest {
        // Given: Input starts a round
        val input = "start new round"

        // When
        val result = offlineIntentHandler.processOffline(input)

        // Then: Should match ROUND_START intent
        assertTrue("Should match offline intent", result is OfflineIntentHandler.OfflineResult.Match)
        val matchResult = result as OfflineIntentHandler.OfflineResult.Match
        assertEquals(
            "Should match ROUND_START intent",
            IntentType.ROUND_START,
            matchResult.parsedIntent.intent
        )
    }

    @Test
    fun `processOffline matches settings intent`() = runTest {
        // Given: Input requests settings
        val input = "open settings"

        // When
        val result = offlineIntentHandler.processOffline(input)

        // Then: Should match SETTINGS_CHANGE intent
        assertTrue("Should match offline intent", result is OfflineIntentHandler.OfflineResult.Match)
        val matchResult = result as OfflineIntentHandler.OfflineResult.Match
        assertEquals(
            "Should match SETTINGS_CHANGE intent",
            IntentType.SETTINGS_CHANGE,
            matchResult.parsedIntent.intent
        )
    }

    @Test
    fun `processOffline detects online-only shot recommendation intent`() = runTest {
        // Given: Input requests shot recommendation (online-only)
        val input = "what club should I use"

        // When
        val result = offlineIntentHandler.processOffline(input)

        // Then: Should detect online requirement
        assertTrue(
            "Should detect online-only intent",
            result is OfflineIntentHandler.OfflineResult.RequiresOnline
        )
        val requiresOnlineResult = result as OfflineIntentHandler.OfflineResult.RequiresOnline
        assertEquals(
            "Should identify SHOT_RECOMMENDATION intent",
            IntentType.SHOT_RECOMMENDATION,
            requiresOnlineResult.intentType
        )
        assertTrue(
            "Should provide helpful message",
            requiresOnlineResult.message.isNotEmpty()
        )
    }

    @Test
    fun `processOffline detects online-only recovery check intent`() = runTest {
        // Given: Input requests recovery check (online-only)
        val input = "how's my recovery"

        // When
        val result = offlineIntentHandler.processOffline(input)

        // Then: Should detect online requirement
        assertTrue(
            "Should detect online-only intent",
            result is OfflineIntentHandler.OfflineResult.RequiresOnline
        )
        val requiresOnlineResult = result as OfflineIntentHandler.OfflineResult.RequiresOnline
        assertEquals(
            "Should identify RECOVERY_CHECK intent",
            IntentType.RECOVERY_CHECK,
            requiresOnlineResult.intentType
        )
    }

    @Test
    fun `processOffline detects online-only weather check intent`() = runTest {
        // Given: Input requests weather (online-only)
        val input = "what's the weather"

        // When
        val result = offlineIntentHandler.processOffline(input)

        // Then: Should detect online requirement
        assertTrue(
            "Should detect online-only intent",
            result is OfflineIntentHandler.OfflineResult.RequiresOnline
        )
    }

    @Test
    fun `processOffline returns no match for unclear input`() = runTest {
        // Given: Ambiguous input that doesn't match any intent
        val input = "hello"

        // When
        val result = offlineIntentHandler.processOffline(input)

        // Then: Should return no match
        assertTrue(
            "Should return no match for unclear input",
            result is OfflineIntentHandler.OfflineResult.NoMatch ||
                    result is OfflineIntentHandler.OfflineResult.Clarify
        )
    }

    @Test
    fun `processOffline extracts score from score entry input`() = runTest {
        // Given: Input with score number
        val input = "enter score 5"

        // When
        val result = offlineIntentHandler.processOffline(input)

        // Then: Should extract score entity
        assertTrue("Should match score entry", result is OfflineIntentHandler.OfflineResult.Match)
        val matchResult = result as OfflineIntentHandler.OfflineResult.Match
        assertEquals(
            "Should extract score value",
            5.0,
            matchResult.parsedIntent.entities.score ?: 0.0,
            0.001
        )
    }

    @Test
    fun `processOffline extracts hole number from input`() = runTest {
        // Given: Input with hole number
        val input = "score for hole 7"

        // When
        val result = offlineIntentHandler.processOffline(input)

        // Then: Should extract hole entity
        assertTrue("Should match score entry", result is OfflineIntentHandler.OfflineResult.Match)
        val matchResult = result as OfflineIntentHandler.OfflineResult.Match
        assertEquals(
            "Should extract hole number",
            7,
            matchResult.parsedIntent.entities.hole
        )
    }

    @Test
    fun `getOfflineIntents returns only offline-capable intents`() {
        // Given: Mock local suggestions
        val offlineIntents = listOf(
            LocalIntentSuggestions.IntentSuggestion(
                intentType = IntentType.SCORE_ENTRY,
                label = "Enter Score",
                description = "Record score",
                module = caddypro.domain.navcaddy.models.Module.CADDY,
                isOfflineAvailable = true
            ),
            LocalIntentSuggestions.IntentSuggestion(
                intentType = IntentType.STATS_LOOKUP,
                label = "View Stats",
                description = "Check statistics",
                module = caddypro.domain.navcaddy.models.Module.CADDY,
                isOfflineAvailable = true
            )
        )
        every { localSuggestions.getOfflineIntents() } returns offlineIntents

        // When
        val result = offlineIntentHandler.getOfflineIntents()

        // Then
        assertEquals("Should return offline intents", offlineIntents, result)
        assertTrue("All intents should be offline-available", result.all { it.isOfflineAvailable })
    }

    @Test
    fun `processOffline requests clarification for weak matches`() = runTest {
        // Given: Input that weakly matches multiple intents
        val input = "my round" // Could be start or end

        // When
        val result = offlineIntentHandler.processOffline(input)

        // Then: Should clarify or provide suggestions
        assertTrue(
            "Should request clarification or provide no match",
            result is OfflineIntentHandler.OfflineResult.Clarify ||
                    result is OfflineIntentHandler.OfflineResult.NoMatch
        )
    }

    @Test
    fun `processOffline handles empty input gracefully`() = runTest {
        // Given: Empty input
        val input = ""

        // When
        val result = offlineIntentHandler.processOffline(input)

        // Then: Should not crash and return no match
        assertTrue(
            "Should handle empty input",
            result is OfflineIntentHandler.OfflineResult.NoMatch
        )
    }

    @Test
    fun `processOffline handles special characters in input`() = runTest {
        // Given: Input with special characters
        val input = "enter score!!!"

        // When
        val result = offlineIntentHandler.processOffline(input)

        // Then: Should still match intent
        assertTrue(
            "Should match despite special characters",
            result is OfflineIntentHandler.OfflineResult.Match
        )
    }
}
