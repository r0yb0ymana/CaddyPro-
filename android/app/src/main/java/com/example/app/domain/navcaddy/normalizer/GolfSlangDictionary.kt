package caddypro.domain.navcaddy.normalizer

/**
 * Dictionary of golf slang and common terminology.
 *
 * Spec reference: navcaddy-engine.md R2, navcaddy-engine-plan.md Task 7
 *
 * Mappings include:
 * - Club abbreviations (7i, PW, SW, etc.)
 * - Woods and driver abbreviations (3w, 5w, D)
 * - Hybrid abbreviations (3h, 4h)
 * - Common golf terms ("stick" -> "club", "dance floor" -> "green")
 * - Number words ("one" -> "1", "fifty" -> "50")
 */
object GolfSlangDictionary {

    /**
     * Club abbreviations to full names.
     * Case-insensitive matching is applied by the normalizer.
     */
    val clubAbbreviations = mapOf(
        // Irons with 'i' suffix
        "3i" to "3-iron",
        "4i" to "4-iron",
        "5i" to "5-iron",
        "6i" to "6-iron",
        "7i" to "7-iron",
        "8i" to "8-iron",
        "9i" to "9-iron",

        // Wedges
        "pw" to "pitching wedge",
        "gw" to "gap wedge",
        "aw" to "approach wedge",
        "sw" to "sand wedge",
        "lw" to "lob wedge",

        // Woods with 'w' suffix
        "3w" to "3-wood",
        "5w" to "5-wood",
        "7w" to "7-wood",

        // Driver
        "d" to "driver",

        // Hybrids with 'h' suffix
        "2h" to "2-hybrid",
        "3h" to "3-hybrid",
        "4h" to "4-hybrid",
        "5h" to "5-hybrid"
    )

    /**
     * Common golf terms and slang.
     */
    val commonTerms = mapOf(
        "stick" to "club",
        "sticks" to "clubs",
        "dance floor" to "green",
        "the dance floor" to "green",
        "tin cup" to "hole",
        "bunker" to "sand trap",
        "putting surface" to "green",
        "fairway metal" to "fairway wood",
        "big stick" to "driver",
        "big dog" to "driver",
        "flat stick" to "putter"
    )

    /**
     * Number words to digits for yardages and scores.
     * These handle basic number conversions.
     */
    val numberWords = mapOf(
        // Single digits
        "one" to "1",
        "two" to "2",
        "three" to "3",
        "four" to "4",
        "five" to "5",
        "six" to "6",
        "seven" to "7",
        "eight" to "8",
        "nine" to "9",

        // Teens
        "ten" to "10",
        "eleven" to "11",
        "twelve" to "12",
        "thirteen" to "13",
        "fourteen" to "14",
        "fifteen" to "15",
        "sixteen" to "16",
        "seventeen" to "17",
        "eighteen" to "18",
        "nineteen" to "19",

        // Tens
        "twenty" to "20",
        "thirty" to "30",
        "forty" to "40",
        "fifty" to "50",
        "sixty" to "60",
        "seventy" to "70",
        "eighty" to "80",
        "ninety" to "90",

        // Hundreds
        "hundred" to "100",
        "hundred and" to "100"
    )

    /**
     * Compound number patterns (e.g., "one fifty" -> "150").
     * These are ordered by specificity (most specific first).
     */
    val compoundNumbers = listOf(
        // Hundred patterns (e.g., "one hundred fifty" -> "150")
        "one hundred fifty" to "150",
        "one hundred sixty" to "160",
        "one hundred seventy" to "170",
        "one hundred eighty" to "180",
        "one hundred ninety" to "190",
        "two hundred" to "200",

        // Shortened hundred patterns (e.g., "one fifty" -> "150")
        "one fifty" to "150",
        "one sixty" to "160",
        "one seventy" to "170",
        "one eighty" to "180",
        "one ninety" to "190",
        "two hundred" to "200",

        // Common yardage patterns
        "one twenty" to "120",
        "one thirty" to "130",
        "one forty" to "140",
        "one ten" to "110",

        // Twenty patterns (e.g., "twenty five" -> "25")
        "twenty one" to "21",
        "twenty two" to "22",
        "twenty three" to "23",
        "twenty four" to "24",
        "twenty five" to "25",
        "twenty six" to "26",
        "twenty seven" to "27",
        "twenty eight" to "28",
        "twenty nine" to "29",

        // Thirty patterns
        "thirty one" to "31",
        "thirty two" to "32",
        "thirty three" to "33",
        "thirty four" to "34",
        "thirty five" to "35",
        "thirty six" to "36",
        "thirty seven" to "37",
        "thirty eight" to "38",
        "thirty nine" to "39",

        // Iron patterns (e.g., "seven iron" -> "7-iron")
        "three iron" to "3-iron",
        "four iron" to "4-iron",
        "five iron" to "5-iron",
        "six iron" to "6-iron",
        "seven iron" to "7-iron",
        "eight iron" to "8-iron",
        "nine iron" to "9-iron",

        // Wood patterns
        "three wood" to "3-wood",
        "five wood" to "5-wood",
        "seven wood" to "7-wood"
    )

    /**
     * Get all mappings as a single map for easy lookup.
     */
    fun getAllMappings(): Map<String, String> {
        return clubAbbreviations + commonTerms + numberWords
    }
}
