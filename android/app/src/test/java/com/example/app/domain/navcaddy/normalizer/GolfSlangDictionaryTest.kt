package caddypro.domain.navcaddy.normalizer

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for GolfSlangDictionary.
 *
 * Ensures all golf slang mappings are correct and complete.
 *
 * Spec reference: navcaddy-engine.md R2, navcaddy-engine-plan.md Task 7
 */
class GolfSlangDictionaryTest {

    @Test
    fun `clubAbbreviations contains all iron abbreviations`() {
        val expected = listOf("3i", "4i", "5i", "6i", "7i", "8i", "9i")
        expected.forEach { abbrev ->
            assertTrue(
                "Missing iron abbreviation: $abbrev",
                GolfSlangDictionary.clubAbbreviations.containsKey(abbrev)
            )
        }
    }

    @Test
    fun `clubAbbreviations expands irons correctly`() {
        assertEquals("7-iron", GolfSlangDictionary.clubAbbreviations["7i"])
        assertEquals("8-iron", GolfSlangDictionary.clubAbbreviations["8i"])
        assertEquals("9-iron", GolfSlangDictionary.clubAbbreviations["9i"])
    }

    @Test
    fun `clubAbbreviations contains all wedge abbreviations`() {
        val expected = listOf("pw", "gw", "aw", "sw", "lw")
        expected.forEach { abbrev ->
            assertTrue(
                "Missing wedge abbreviation: $abbrev",
                GolfSlangDictionary.clubAbbreviations.containsKey(abbrev)
            )
        }
    }

    @Test
    fun `clubAbbreviations expands wedges correctly`() {
        assertEquals("pitching wedge", GolfSlangDictionary.clubAbbreviations["pw"])
        assertEquals("gap wedge", GolfSlangDictionary.clubAbbreviations["gw"])
        assertEquals("sand wedge", GolfSlangDictionary.clubAbbreviations["sw"])
        assertEquals("lob wedge", GolfSlangDictionary.clubAbbreviations["lw"])
    }

    @Test
    fun `clubAbbreviations contains wood abbreviations`() {
        val expected = listOf("3w", "5w", "7w")
        expected.forEach { abbrev ->
            assertTrue(
                "Missing wood abbreviation: $abbrev",
                GolfSlangDictionary.clubAbbreviations.containsKey(abbrev)
            )
        }
    }

    @Test
    fun `clubAbbreviations expands woods correctly`() {
        assertEquals("3-wood", GolfSlangDictionary.clubAbbreviations["3w"])
        assertEquals("5-wood", GolfSlangDictionary.clubAbbreviations["5w"])
    }

    @Test
    fun `clubAbbreviations contains driver abbreviation`() {
        assertEquals("driver", GolfSlangDictionary.clubAbbreviations["d"])
    }

    @Test
    fun `clubAbbreviations contains hybrid abbreviations`() {
        val expected = listOf("2h", "3h", "4h", "5h")
        expected.forEach { abbrev ->
            assertTrue(
                "Missing hybrid abbreviation: $abbrev",
                GolfSlangDictionary.clubAbbreviations.containsKey(abbrev)
            )
        }
    }

    @Test
    fun `clubAbbreviations expands hybrids correctly`() {
        assertEquals("3-hybrid", GolfSlangDictionary.clubAbbreviations["3h"])
        assertEquals("4-hybrid", GolfSlangDictionary.clubAbbreviations["4h"])
    }

    @Test
    fun `commonTerms contains golf slang`() {
        assertTrue(GolfSlangDictionary.commonTerms.containsKey("stick"))
        assertTrue(GolfSlangDictionary.commonTerms.containsKey("dance floor"))
        assertTrue(GolfSlangDictionary.commonTerms.containsKey("big stick"))
    }

    @Test
    fun `commonTerms expands correctly`() {
        assertEquals("club", GolfSlangDictionary.commonTerms["stick"])
        assertEquals("green", GolfSlangDictionary.commonTerms["dance floor"])
        assertEquals("driver", GolfSlangDictionary.commonTerms["big stick"])
        assertEquals("driver", GolfSlangDictionary.commonTerms["big dog"])
    }

    @Test
    fun `numberWords contains single digits`() {
        val expected = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine")
        expected.forEach { word ->
            assertTrue(
                "Missing single digit: $word",
                GolfSlangDictionary.numberWords.containsKey(word)
            )
        }
    }

    @Test
    fun `numberWords expands single digits correctly`() {
        assertEquals("1", GolfSlangDictionary.numberWords["one"])
        assertEquals("5", GolfSlangDictionary.numberWords["five"])
        assertEquals("9", GolfSlangDictionary.numberWords["nine"])
    }

    @Test
    fun `numberWords contains teens`() {
        val expected = listOf(
            "ten", "eleven", "twelve", "thirteen", "fourteen",
            "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
        )
        expected.forEach { word ->
            assertTrue(
                "Missing teen number: $word",
                GolfSlangDictionary.numberWords.containsKey(word)
            )
        }
    }

    @Test
    fun `numberWords expands teens correctly`() {
        assertEquals("10", GolfSlangDictionary.numberWords["ten"])
        assertEquals("15", GolfSlangDictionary.numberWords["fifteen"])
        assertEquals("18", GolfSlangDictionary.numberWords["eighteen"])
    }

    @Test
    fun `numberWords contains tens`() {
        val expected = listOf("twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
        expected.forEach { word ->
            assertTrue(
                "Missing tens number: $word",
                GolfSlangDictionary.numberWords.containsKey(word)
            )
        }
    }

    @Test
    fun `numberWords expands tens correctly`() {
        assertEquals("20", GolfSlangDictionary.numberWords["twenty"])
        assertEquals("50", GolfSlangDictionary.numberWords["fifty"])
        assertEquals("90", GolfSlangDictionary.numberWords["ninety"])
    }

    @Test
    fun `compoundNumbers contains common yardage patterns`() {
        val expected = listOf(
            "one fifty",
            "one sixty",
            "one seventy",
            "one eighty",
            "one hundred fifty"
        )

        val patterns = GolfSlangDictionary.compoundNumbers.map { it.first }
        expected.forEach { pattern ->
            assertTrue(
                "Missing yardage pattern: $pattern",
                patterns.contains(pattern)
            )
        }
    }

    @Test
    fun `compoundNumbers expands yardages correctly`() {
        val yardages = GolfSlangDictionary.compoundNumbers.toMap()
        assertEquals("150", yardages["one fifty"])
        assertEquals("160", yardages["one sixty"])
        assertEquals("170", yardages["one seventy"])
    }

    @Test
    fun `compoundNumbers contains iron patterns`() {
        val expected = listOf(
            "seven iron",
            "eight iron",
            "nine iron"
        )

        val patterns = GolfSlangDictionary.compoundNumbers.map { it.first }
        expected.forEach { pattern ->
            assertTrue(
                "Missing iron pattern: $pattern",
                patterns.contains(pattern)
            )
        }
    }

    @Test
    fun `compoundNumbers expands irons correctly`() {
        val patterns = GolfSlangDictionary.compoundNumbers.toMap()
        assertEquals("7-iron", patterns["seven iron"])
        assertEquals("8-iron", patterns["eight iron"])
    }

    @Test
    fun `compoundNumbers contains wood patterns`() {
        val expected = listOf("three wood", "five wood")
        val patterns = GolfSlangDictionary.compoundNumbers.map { it.first }

        expected.forEach { pattern ->
            assertTrue(
                "Missing wood pattern: $pattern",
                patterns.contains(pattern)
            )
        }
    }

    @Test
    fun `getAllMappings combines all dictionaries`() {
        val allMappings = GolfSlangDictionary.getAllMappings()

        // Should contain entries from all dictionaries
        assertTrue(allMappings.containsKey("7i")) // from clubAbbreviations
        assertTrue(allMappings.containsKey("stick")) // from commonTerms
        assertTrue(allMappings.containsKey("fifty")) // from numberWords

        // Should not contain compound numbers (they're processed separately)
        assertFalse(allMappings.containsKey("one fifty"))
    }

    @Test
    fun `getAllMappings has no duplicate keys`() {
        val allMappings = GolfSlangDictionary.getAllMappings()
        val expectedSize = GolfSlangDictionary.clubAbbreviations.size +
                GolfSlangDictionary.commonTerms.size +
                GolfSlangDictionary.numberWords.size

        // Size should match sum of individual dictionaries (no duplicates)
        assertEquals(expectedSize, allMappings.size)
    }

    @Test
    fun `no mapping has empty value`() {
        val allMappings = GolfSlangDictionary.getAllMappings()
        allMappings.forEach { (key, value) ->
            assertFalse("Empty value for key: $key", value.isEmpty())
        }

        GolfSlangDictionary.compoundNumbers.forEach { (key, value) ->
            assertFalse("Empty value for compound: $key", value.isEmpty())
        }
    }

    @Test
    fun `no mapping has whitespace-only value`() {
        val allMappings = GolfSlangDictionary.getAllMappings()
        allMappings.forEach { (key, value) ->
            assertFalse("Whitespace-only value for key: $key", value.trim().isEmpty())
        }
    }

    @Test
    fun `club abbreviations use consistent format`() {
        // Irons should be formatted as "N-iron"
        GolfSlangDictionary.clubAbbreviations.filter { it.key.endsWith("i") }.forEach { (key, value) ->
            assertTrue("Iron format incorrect for $key: $value", value.matches(Regex("\\d-iron")))
        }

        // Woods should be formatted as "N-wood"
        GolfSlangDictionary.clubAbbreviations.filter { it.key.endsWith("w") }.forEach { (key, value) ->
            assertTrue("Wood format incorrect for $key: $value", value.matches(Regex("\\d-wood")))
        }

        // Hybrids should be formatted as "N-hybrid"
        GolfSlangDictionary.clubAbbreviations.filter { it.key.endsWith("h") }.forEach { (key, value) ->
            assertTrue("Hybrid format incorrect for $key: $value", value.matches(Regex("\\d-hybrid")))
        }
    }

    @Test
    fun `compound numbers are ordered by length descending`() {
        val patterns = GolfSlangDictionary.compoundNumbers.map { it.first }

        // Check first few patterns to ensure longer ones come first
        val oneHundredFifty = patterns.indexOf("one hundred fifty")
        val oneFifty = patterns.indexOf("one fifty")

        // "one hundred fifty" should come before "one fifty"
        assertTrue(
            "Compound numbers not ordered by length",
            oneHundredFifty < oneFifty
        )
    }
}
