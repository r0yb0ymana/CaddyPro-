package com.example.app.domain.navcaddy.fallback

import caddypro.domain.navcaddy.models.IntentType
import caddypro.domain.navcaddy.models.Module
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for LocalIntentSuggestions.
 *
 * Tests cover:
 * - Keyword matching to relevant intents
 * - Offline intent filtering
 * - Common intent suggestions
 * - Maximum suggestion limits
 * - Module-based filtering
 *
 * Spec reference: navcaddy-engine.md G6, A6, C6
 * Plan reference: navcaddy-engine-plan.md Task 23
 */
class LocalIntentSuggestionsTest {

    private lateinit var localSuggestions: LocalIntentSuggestions

    @Before
    fun setup() {
        localSuggestions = LocalIntentSuggestions()
    }

    // Test: Club keywords match club adjustment intent
    @Test
    fun `club keywords match club adjustment intent`() {
        // GIVEN: Input with club keywords
        val inputs = listOf("adjust my driver", "change club distance", "7 iron yardage")

        inputs.forEach { input ->
            // WHEN: Get suggestions
            val suggestions = localSuggestions.getSuggestions(input, maxSuggestions = 5)

            // THEN: Club adjustment is suggested
            val intentTypes = suggestions.map { it.intentType }
            assertTrue(
                "Expected CLUB_ADJUSTMENT for '$input'",
                intentTypes.contains(IntentType.CLUB_ADJUSTMENT)
            )
        }
    }

    // Test: Recovery keywords match recovery intent
    @Test
    fun `recovery keywords match recovery intent`() {
        // GIVEN: Input with recovery keywords
        val inputs = listOf("check my recovery", "am I ready to play", "feeling sore")

        inputs.forEach { input ->
            // WHEN: Get suggestions
            val suggestions = localSuggestions.getSuggestions(input, maxSuggestions = 5)

            // THEN: Recovery check is suggested
            val intentTypes = suggestions.map { it.intentType }
            assertTrue(
                "Expected RECOVERY_CHECK for '$input'",
                intentTypes.contains(IntentType.RECOVERY_CHECK)
            )
        }
    }

    // Test: Shot keywords match shot recommendation intent
    @Test
    fun `shot keywords match shot recommendation intent`() {
        // GIVEN: Input with shot keywords
        val inputs = listOf("what club should I hit", "shot advice", "recommend a club")

        inputs.forEach { input ->
            // WHEN: Get suggestions
            val suggestions = localSuggestions.getSuggestions(input, maxSuggestions = 5)

            // THEN: Shot recommendation is suggested
            val intentTypes = suggestions.map { it.intentType }
            assertTrue(
                "Expected SHOT_RECOMMENDATION for '$input'",
                intentTypes.contains(IntentType.SHOT_RECOMMENDATION)
            )
        }
    }

    // Test: Score keywords match score entry intent
    @Test
    fun `score keywords match score entry intent`() {
        // GIVEN: Input with score keywords
        val inputs = listOf("enter my score", "record a birdie", "hole score")

        inputs.forEach { input ->
            // WHEN: Get suggestions
            val suggestions = localSuggestions.getSuggestions(input, maxSuggestions = 5)

            // THEN: Score entry is suggested
            val intentTypes = suggestions.map { it.intentType }
            assertTrue(
                "Expected SCORE_ENTRY for '$input'",
                intentTypes.contains(IntentType.SCORE_ENTRY)
            )
        }
    }

    // Test: Pattern keywords match pattern query intent
    @Test
    fun `pattern keywords match pattern query intent`() {
        // GIVEN: Input with pattern keywords
        val inputs = listOf("show my patterns", "miss tendency", "slice problem")

        inputs.forEach { input ->
            // WHEN: Get suggestions
            val suggestions = localSuggestions.getSuggestions(input, maxSuggestions = 5)

            // THEN: Pattern query is suggested
            val intentTypes = suggestions.map { it.intentType }
            assertTrue(
                "Expected PATTERN_QUERY for '$input'",
                intentTypes.contains(IntentType.PATTERN_QUERY)
            )
        }
    }

    // Test: Offline mode filters suggestions
    @Test
    fun `offline mode only returns offline-capable intents`() {
        // GIVEN: Any input
        val input = "help me"

        // WHEN: Get suggestions in offline mode
        val onlineSuggestions = localSuggestions.getSuggestions(input, isOffline = false)
        val offlineSuggestions = localSuggestions.getSuggestions(input, isOffline = true)

        // THEN: Offline suggestions are subset of online suggestions
        assertTrue(offlineSuggestions.size <= onlineSuggestions.size)

        // AND: All offline suggestions are marked as offline-available
        offlineSuggestions.forEach { suggestion ->
            assertTrue(
                "${suggestion.intentType} should be offline-available",
                suggestion.isOfflineAvailable
            )
        }
    }

    // Test: getOfflineIntents returns only offline-capable intents
    @Test
    fun `getOfflineIntents returns only offline-capable intents`() {
        // WHEN: Get offline intents
        val offlineIntents = localSuggestions.getOfflineIntents()

        // THEN: All intents are offline-available
        offlineIntents.forEach { suggestion ->
            assertTrue(
                "${suggestion.intentType} should be offline-available",
                suggestion.isOfflineAvailable
            )
        }

        // AND: Includes expected offline intents
        val intentTypes = offlineIntents.map { it.intentType }
        assertTrue(intentTypes.contains(IntentType.SCORE_ENTRY))
        assertTrue(intentTypes.contains(IntentType.CLUB_ADJUSTMENT))
        assertTrue(intentTypes.contains(IntentType.STATS_LOOKUP))
        assertTrue(intentTypes.contains(IntentType.EQUIPMENT_INFO))
    }

    // Test: Offline intents exclude network-dependent intents
    @Test
    fun `offline intents exclude network-dependent intents`() {
        // WHEN: Get offline intents
        val offlineIntents = localSuggestions.getOfflineIntents()
        val intentTypes = offlineIntents.map { it.intentType }

        // THEN: Network-dependent intents not included
        assertFalse(intentTypes.contains(IntentType.RECOVERY_CHECK))
        assertFalse(intentTypes.contains(IntentType.SHOT_RECOMMENDATION))
        assertFalse(intentTypes.contains(IntentType.WEATHER_CHECK))
        assertFalse(intentTypes.contains(IntentType.COURSE_INFO))
    }

    // Test: Common intents are always available
    @Test
    fun `getCommonIntents returns frequently used intents`() {
        // WHEN: Get common intents
        val commonIntents = localSuggestions.getCommonIntents()

        // THEN: Returns common user intents
        assertTrue(commonIntents.isNotEmpty())

        val intentTypes = commonIntents.map { it.intentType }
        assertTrue(intentTypes.contains(IntentType.SHOT_RECOMMENDATION))
        assertTrue(intentTypes.contains(IntentType.SCORE_ENTRY))
        assertTrue(intentTypes.contains(IntentType.CLUB_ADJUSTMENT))
    }

    // Test: Max suggestions limit is respected
    @Test
    fun `max suggestions limit is respected`() {
        // GIVEN: Input that matches many keywords
        val input = "club shot score pattern drill weather stats round equipment course settings help feedback"

        // WHEN: Get suggestions with limit
        val maxSuggestions = 3
        val suggestions = localSuggestions.getSuggestions(input, maxSuggestions = maxSuggestions)

        // THEN: Returns at most maxSuggestions
        assertTrue(suggestions.size <= maxSuggestions)
    }

    // Test: Empty input returns common intents
    @Test
    fun `empty input returns common intents`() {
        // GIVEN: Empty input
        val input = ""

        // WHEN: Get suggestions
        val suggestions = localSuggestions.getSuggestions(input, maxSuggestions = 5)

        // THEN: Returns common intents
        assertTrue(suggestions.isNotEmpty())
    }

    // Test: Module filtering works
    @Test
    fun `getSuggestionsByModule filters by module correctly`() {
        // WHEN: Get suggestions for CADDY module
        val caddySuggestions = localSuggestions.getSuggestionsByModule(Module.CADDY)

        // THEN: All suggestions are for CADDY module
        caddySuggestions.forEach { suggestion ->
            assertEquals(Module.CADDY, suggestion.module)
        }

        // AND: Includes expected CADDY intents
        val intentTypes = caddySuggestions.map { it.intentType }
        assertTrue(intentTypes.contains(IntentType.SHOT_RECOMMENDATION))
        assertTrue(intentTypes.contains(IntentType.SCORE_ENTRY))
        assertTrue(intentTypes.contains(IntentType.CLUB_ADJUSTMENT))
    }

    // Test: COACH module suggestions
    @Test
    fun `getSuggestionsByModule returns coach intents for COACH module`() {
        // WHEN: Get suggestions for COACH module
        val coachSuggestions = localSuggestions.getSuggestionsByModule(Module.COACH)

        // THEN: All suggestions are for COACH module
        coachSuggestions.forEach { suggestion ->
            assertEquals(Module.COACH, suggestion.module)
        }

        // AND: Includes expected COACH intents
        val intentTypes = coachSuggestions.map { it.intentType }
        assertTrue(intentTypes.contains(IntentType.PATTERN_QUERY))
        assertTrue(intentTypes.contains(IntentType.DRILL_REQUEST))
    }

    // Test: RECOVERY module suggestions
    @Test
    fun `getSuggestionsByModule returns recovery intents for RECOVERY module`() {
        // WHEN: Get suggestions for RECOVERY module
        val recoverySuggestions = localSuggestions.getSuggestionsByModule(Module.RECOVERY)

        // THEN: All suggestions are for RECOVERY module
        recoverySuggestions.forEach { suggestion ->
            assertEquals(Module.RECOVERY, suggestion.module)
        }

        // AND: Includes expected RECOVERY intents
        val intentTypes = recoverySuggestions.map { it.intentType }
        assertTrue(intentTypes.contains(IntentType.RECOVERY_CHECK))
    }

    // Test: SETTINGS module suggestions
    @Test
    fun `getSuggestionsByModule returns settings intents for SETTINGS module`() {
        // WHEN: Get suggestions for SETTINGS module
        val settingsSuggestions = localSuggestions.getSuggestionsByModule(Module.SETTINGS)

        // THEN: All suggestions are for SETTINGS module
        settingsSuggestions.forEach { suggestion ->
            assertEquals(Module.SETTINGS, suggestion.module)
        }

        // AND: Includes expected SETTINGS intents
        val intentTypes = settingsSuggestions.map { it.intentType }
        assertTrue(intentTypes.contains(IntentType.SETTINGS_CHANGE))
        assertTrue(intentTypes.contains(IntentType.HELP_REQUEST))
    }

    // Test: Module filtering with offline mode
    @Test
    fun `getSuggestionsByModule respects offline mode`() {
        // WHEN: Get CADDY suggestions in offline mode
        val onlineSuggestions = localSuggestions.getSuggestionsByModule(Module.CADDY, isOffline = false)
        val offlineSuggestions = localSuggestions.getSuggestionsByModule(Module.CADDY, isOffline = true)

        // THEN: Offline suggestions are subset
        assertTrue(offlineSuggestions.size <= onlineSuggestions.size)

        // AND: All offline suggestions are offline-capable
        offlineSuggestions.forEach { suggestion ->
            assertTrue(suggestion.isOfflineAvailable)
        }
    }

    // Test: All suggestions have required fields
    @Test
    fun `all suggestions have label and description`() {
        // GIVEN: Various inputs
        val inputs = listOf("club", "score", "pattern", "help", "")

        inputs.forEach { input ->
            // WHEN: Get suggestions
            val suggestions = localSuggestions.getSuggestions(input, maxSuggestions = 10)

            // THEN: All suggestions have required fields
            suggestions.forEach { suggestion ->
                assertFalse("Label should not be blank", suggestion.label.isBlank())
                assertFalse("Description should not be blank", suggestion.description.isBlank())
                assertNotNull("Intent type should not be null", suggestion.intentType)
                assertNotNull("Module should not be null", suggestion.module)
            }
        }
    }

    // Test: Case insensitive matching
    @Test
    fun `keyword matching is case insensitive`() {
        // GIVEN: Same input with different cases
        val inputs = listOf("CLUB", "club", "Club", "cLuB")

        inputs.forEach { input ->
            // WHEN: Get suggestions
            val suggestions = localSuggestions.getSuggestions(input, maxSuggestions = 5)

            // THEN: All produce same results
            val intentTypes = suggestions.map { it.intentType }
            assertTrue(
                "Expected CLUB_ADJUSTMENT for '$input'",
                intentTypes.contains(IntentType.CLUB_ADJUSTMENT)
            )
        }
    }

    // Test: Multiple keyword matches return multiple suggestions
    @Test
    fun `multiple keyword matches return multiple relevant suggestions`() {
        // GIVEN: Input with multiple keywords
        val input = "club and score"

        // WHEN: Get suggestions
        val suggestions = localSuggestions.getSuggestions(input, maxSuggestions = 5)
        val intentTypes = suggestions.map { it.intentType }

        // THEN: Multiple relevant intents suggested
        assertTrue(intentTypes.contains(IntentType.CLUB_ADJUSTMENT))
        assertTrue(intentTypes.contains(IntentType.SCORE_ENTRY))
    }

    // Test: Suggestions are unique (no duplicates)
    @Test
    fun `suggestions list contains no duplicate intents`() {
        // GIVEN: Input with repeated keywords
        val input = "club club club adjustment adjustment"

        // WHEN: Get suggestions
        val suggestions = localSuggestions.getSuggestions(input, maxSuggestions = 10)
        val intentTypes = suggestions.map { it.intentType }

        // THEN: No duplicate intent types
        val uniqueIntentTypes = intentTypes.distinct()
        assertEquals(
            "Should have no duplicate intents",
            uniqueIntentTypes.size,
            intentTypes.size
        )
    }

    // Test: Fallback to common intents when no keywords match
    @Test
    fun `unknown input falls back to common intents`() {
        // GIVEN: Input with no matching keywords
        val input = "xyzabc123"

        // WHEN: Get suggestions
        val suggestions = localSuggestions.getSuggestions(input, maxSuggestions = 5)

        // THEN: Returns common intents as fallback
        assertTrue(suggestions.isNotEmpty())

        val intentTypes = suggestions.map { it.intentType }
        // Should contain at least some common intents
        assertTrue(
            intentTypes.contains(IntentType.SHOT_RECOMMENDATION) ||
            intentTypes.contains(IntentType.SCORE_ENTRY) ||
            intentTypes.contains(IntentType.HELP_REQUEST)
        )
    }

    // Test: Weather keywords work
    @Test
    fun `weather keywords match weather check intent`() {
        // GIVEN: Input with weather keywords
        val inputs = listOf("check weather", "wind conditions", "is it going to rain")

        inputs.forEach { input ->
            // WHEN: Get suggestions
            val suggestions = localSuggestions.getSuggestions(input, maxSuggestions = 5)

            // THEN: Weather check is suggested
            val intentTypes = suggestions.map { it.intentType }
            assertTrue(
                "Expected WEATHER_CHECK for '$input'",
                intentTypes.contains(IntentType.WEATHER_CHECK)
            )
        }
    }

    // Test: Help keywords work
    @Test
    fun `help keywords match help request intent`() {
        // GIVEN: Input with help keywords
        val inputs = listOf("help", "how to use", "instructions")

        inputs.forEach { input ->
            // WHEN: Get suggestions
            val suggestions = localSuggestions.getSuggestions(input, maxSuggestions = 5)

            // THEN: Help request is suggested
            val intentTypes = suggestions.map { it.intentType }
            assertTrue(
                "Expected HELP_REQUEST for '$input'",
                intentTypes.contains(IntentType.HELP_REQUEST)
            )
        }
    }
}
