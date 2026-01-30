package caddypro.domain.navcaddy.normalizer

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for InputNormalizer.
 *
 * Tests cover:
 * - Golf slang expansion
 * - Number normalization (spoken -> digits)
 * - Profanity filtering
 * - Edge cases (mixed input, already normalized)
 *
 * Spec reference: navcaddy-engine.md R2, navcaddy-engine-plan.md Task 7
 * Acceptance criteria: A1, A2
 */
class InputNormalizerTest {

    private lateinit var normalizer: InputNormalizer

    @Before
    fun setUp() {
        normalizer = InputNormalizer()
    }

    // Golf Slang Expansion Tests

    @Test
    fun `normalize expands iron abbreviations`() {
        val result = normalizer.normalize("My 7i feels long today")

        assertEquals("My 7-iron feels long today", result.normalizedInput)
        assertTrue(result.wasModified)
        assertEquals(1, result.modifications.size)
        assertEquals(ModificationType.SLANG, result.modifications[0].type)
        assertEquals("7i", result.modifications[0].original)
        assertEquals("7-iron", result.modifications[0].replacement)
    }

    @Test
    fun `normalize expands multiple club abbreviations`() {
        val result = normalizer.normalize("Should I use 7i or pw?")

        assertEquals("Should I use 7-iron or pitching wedge?", result.normalizedInput)
        assertTrue(result.wasModified)
        assertEquals(2, result.modifications.size)
    }

    @Test
    fun `normalize expands wedge abbreviations`() {
        val testCases = mapOf(
            "pw" to "pitching wedge",
            "sw" to "sand wedge",
            "lw" to "lob wedge",
            "gw" to "gap wedge",
            "aw" to "approach wedge"
        )

        testCases.forEach { (abbrev, expected) ->
            val result = normalizer.normalize("Use my $abbrev")
            assertEquals("Use my $expected", result.normalizedInput)
        }
    }

    @Test
    fun `normalize expands wood abbreviations`() {
        val testCases = mapOf(
            "3w" to "3-wood",
            "5w" to "5-wood",
            "7w" to "7-wood"
        )

        testCases.forEach { (abbrev, expected) ->
            val result = normalizer.normalize("Hit the $abbrev")
            assertEquals("Hit the $expected", result.normalizedInput)
        }
    }

    @Test
    fun `normalize expands driver abbreviation`() {
        val result = normalizer.normalize("Pull out the d")

        assertEquals("Pull out the driver", result.normalizedInput)
        assertTrue(result.wasModified)
    }

    @Test
    fun `normalize expands hybrid abbreviations`() {
        val result = normalizer.normalize("Try the 3h or 4h")

        assertEquals("Try the 3-hybrid or 4-hybrid", result.normalizedInput)
        assertTrue(result.wasModified)
    }

    @Test
    fun `normalize expands common golf terms`() {
        val testCases = mapOf(
            "my stick" to "my club",
            "the dance floor" to "the green",
            "big stick" to "driver"
        )

        testCases.forEach { (input, expected) ->
            val result = normalizer.normalize(input)
            assertEquals(expected, result.normalizedInput)
        }
    }

    @Test
    fun `normalize handles case insensitive slang`() {
        val result1 = normalizer.normalize("My 7I feels good")
        val result2 = normalizer.normalize("My 7i feels good")
        val result3 = normalizer.normalize("My 7I FEELS GOOD")

        assertEquals(result1.normalizedInput, result2.normalizedInput)
        assertTrue(result1.normalizedInput.contains("7-iron"))
        assertTrue(result3.normalizedInput.contains("7-iron"))
    }

    // Number Normalization Tests

    @Test
    fun `normalize converts spoken yardages to digits`() {
        val result = normalizer.normalize("I have one fifty to the green")

        assertEquals("I have 150 to the green", result.normalizedInput)
        assertTrue(result.wasModified)
        assertTrue(result.modifications.any { it.type == ModificationType.NUMBER })
    }

    @Test
    fun `normalize converts compound numbers`() {
        val testCases = mapOf(
            "one fifty" to "150",
            "one sixty" to "160",
            "one seventy" to "170",
            "one eighty" to "180",
            "one hundred fifty" to "150"
        )

        testCases.forEach { (input, expected) ->
            val result = normalizer.normalize("I have $input yards")
            assertTrue(result.normalizedInput.contains(expected))
        }
    }

    @Test
    fun `normalize converts spoken club numbers`() {
        val result = normalizer.normalize("Use the seven iron")

        assertEquals("Use the 7-iron", result.normalizedInput)
        assertTrue(result.wasModified)
    }

    @Test
    fun `normalize converts single number words`() {
        val testCases = mapOf(
            "one" to "1",
            "five" to "5",
            "ten" to "10",
            "twenty" to "20",
            "fifty" to "50"
        )

        testCases.forEach { (word, digit) ->
            val result = normalizer.normalize("hole $word")
            assertEquals("hole $digit", result.normalizedInput)
        }
    }

    @Test
    fun `normalize handles compound numbers with additional context`() {
        val result = normalizer.normalize("twenty five yards out")

        assertEquals("25 yards out", result.normalizedInput)
        assertTrue(result.wasModified)
    }

    // Profanity Filtering Tests

    @Test
    fun `normalize filters profanity`() {
        val result = normalizer.normalize("This shot is shit")

        assertTrue(result.normalizedInput.contains("****"))
        assertFalse(result.normalizedInput.contains("shit"))
        assertTrue(result.wasModified)
        assertTrue(result.modifications.any { it.type == ModificationType.PROFANITY })
    }

    @Test
    fun `normalize filters multiple profanity words`() {
        val result = normalizer.normalize("Damn that was a bad shot")

        assertTrue(result.normalizedInput.contains("****"))
        assertFalse(result.normalizedInput.contains("damn"))
        assertTrue(result.wasModified)
    }

    @Test
    fun `normalize preserves intent after profanity filtering`() {
        val result = normalizer.normalize("I keep slicing this damn club")

        // Intent should still be clear
        assertTrue(result.normalizedInput.contains("slicing"))
        assertTrue(result.normalizedInput.contains("club"))
        // Profanity should be filtered
        assertFalse(result.normalizedInput.contains("damn"))
    }

    @Test
    fun `normalize only filters whole word profanity`() {
        val result = normalizer.normalize("I need assistance")

        // "ass" in "assistance" should not be filtered
        assertEquals("I need assistance", result.normalizedInput)
        assertFalse(result.wasModified)
    }

    // Edge Cases Tests

    @Test
    fun `normalize handles empty input`() {
        val result = normalizer.normalize("")

        assertEquals("", result.normalizedInput)
        assertFalse(result.wasModified)
        assertTrue(result.modifications.isEmpty())
    }

    @Test
    fun `normalize handles blank input`() {
        val result = normalizer.normalize("   ")

        assertEquals("", result.normalizedInput)
    }

    @Test
    fun `normalize handles already normalized input`() {
        val result = normalizer.normalize("What club should I use for 150 yards?")

        // Should not modify already normalized text
        assertEquals("What club should I use for 150 yards?", result.normalizedInput)
        assertFalse(result.wasModified)
    }

    @Test
    fun `normalize handles mixed modifications`() {
        val result = normalizer.normalize("My 7i is one fifty to the damn green")

        // Should expand slang, normalize numbers, and filter profanity
        assertTrue(result.normalizedInput.contains("7-iron"))
        assertTrue(result.normalizedInput.contains("150"))
        assertTrue(result.normalizedInput.contains("****"))
        assertTrue(result.wasModified)
        assertTrue(result.modifications.size >= 3)

        // Check all modification types present
        val types = result.modifications.map { it.type }.toSet()
        assertTrue(types.contains(ModificationType.SLANG))
        assertTrue(types.contains(ModificationType.NUMBER))
        assertTrue(types.contains(ModificationType.PROFANITY))
    }

    @Test
    fun `normalize cleans up multiple spaces`() {
        val result = normalizer.normalize("My  7i   feels   long")

        assertEquals("My 7-iron feels long", result.normalizedInput)
        assertFalse(result.normalizedInput.contains("  "))
    }

    @Test
    fun `normalize trims whitespace`() {
        val result = normalizer.normalize("  My 7i feels long  ")

        assertEquals("My 7-iron feels long", result.normalizedInput)
        assertFalse(result.normalizedInput.startsWith(" "))
        assertFalse(result.normalizedInput.endsWith(" "))
    }

    @Test
    fun `normalize handles special characters`() {
        val result = normalizer.normalize("My 7i? PW! What club...")

        assertTrue(result.normalizedInput.contains("7-iron"))
        assertTrue(result.normalizedInput.contains("pitching wedge"))
        assertTrue(result.normalizedInput.contains("?"))
        assertTrue(result.normalizedInput.contains("!"))
    }

    @Test
    fun `normalize preserves case for non-slang words`() {
        val result = normalizer.normalize("My Name uses 7i")

        // Should preserve "My Name" case
        assertTrue(result.normalizedInput.contains("My Name"))
        // But expand slang
        assertTrue(result.normalizedInput.contains("7-iron"))
    }

    // Complex Scenario Tests

    @Test
    fun `normalize handles realistic user input 1`() {
        val result = normalizer.normalize("I have one fifty with my 7i what should I hit")

        assertEquals("I have 150 with my 7-iron what should I hit", result.normalizedInput)
        assertTrue(result.wasModified)
    }

    @Test
    fun `normalize handles realistic user input 2`() {
        val result = normalizer.normalize("pw from the dance floor")

        assertEquals("pitching wedge from the green", result.normalizedInput)
        assertTrue(result.wasModified)
    }

    @Test
    fun `normalize handles realistic user input 3`() {
        val result = normalizer.normalize("Should I use my 3w or 5w for this one seventy shot")

        assertTrue(result.normalizedInput.contains("3-wood"))
        assertTrue(result.normalizedInput.contains("5-wood"))
        assertTrue(result.normalizedInput.contains("170"))
        assertTrue(result.wasModified)
    }

    @Test
    fun `normalize handles question format`() {
        val result = normalizer.normalize("How far is my 8i?")

        assertEquals("How far is my 8-iron?", result.normalizedInput)
        assertTrue(result.wasModified)
    }

    @Test
    fun `normalize handles command format`() {
        val result = normalizer.normalize("Show me my pw stats")

        assertEquals("Show me my pitching wedge stats", result.normalizedInput)
        assertTrue(result.wasModified)
    }

    // Language Detection Tests

    @Test
    fun `normalize handles English input`() {
        val result = normalizer.normalize("What club should I use for this shot?")

        // Should process normally (English is supported)
        assertEquals("What club should I use for this shot?", result.normalizedInput)
    }

    @Test
    fun `normalize handles input with golf terminology`() {
        val result = normalizer.normalize("7i to the green")

        // Should recognize and process golf terms
        assertEquals("7-iron to the green", result.normalizedInput)
        assertTrue(result.wasModified)
    }

    // Original Input Preservation Tests

    @Test
    fun `normalize preserves original input`() {
        val original = "My 7i feels long"
        val result = normalizer.normalize(original)

        assertEquals(original, result.originalInput)
        assertNotEquals(original, result.normalizedInput)
    }

    @Test
    fun `normalize tracks all modifications`() {
        val result = normalizer.normalize("My 7i and pw")

        assertEquals(2, result.modifications.size)
        assertEquals("7i", result.modifications[0].original)
        assertEquals("7-iron", result.modifications[0].replacement)
        assertEquals("pw", result.modifications[1].original)
        assertEquals("pitching wedge", result.modifications[1].replacement)
    }

    @Test
    fun `normalize modification order is deterministic`() {
        val result1 = normalizer.normalize("My 7i is one fifty")
        val result2 = normalizer.normalize("My 7i is one fifty")

        assertEquals(result1.normalizedInput, result2.normalizedInput)
        assertEquals(result1.modifications.size, result2.modifications.size)
    }

    // Performance Tests

    @Test
    fun `normalize handles long input efficiently`() {
        val longInput = "My 7i feels long. " +
                "I have one fifty to the green. " +
                "Should I use pw or sw? " +
                "The big stick is too much. " +
                "Maybe try 3w or 5w instead."

        val result = normalizer.normalize(longInput)

        assertTrue(result.wasModified)
        assertTrue(result.normalizedInput.contains("7-iron"))
        assertTrue(result.normalizedInput.contains("150"))
        assertTrue(result.normalizedInput.contains("pitching wedge"))
        assertTrue(result.normalizedInput.contains("sand wedge"))
    }
}
