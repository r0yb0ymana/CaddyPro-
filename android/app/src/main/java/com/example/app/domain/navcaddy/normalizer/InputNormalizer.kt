package caddypro.domain.navcaddy.normalizer

import java.util.Locale

/**
 * Normalizes user input before intent classification.
 *
 * Pipeline:
 * 1. Language detection (simple English check)
 * 2. Profanity filtering
 * 3. Golf slang expansion
 * 4. Number normalization (spoken -> digits)
 * 5. Whitespace cleanup
 *
 * Spec reference: navcaddy-engine.md R2, navcaddy-engine-plan.md Task 7
 * Acceptance criteria: A1, A2
 */
class InputNormalizer {

    companion object {
        /**
         * Common profanity words to filter.
         * Kept minimal to avoid false positives.
         */
        private val profanityWords = setOf(
            "fuck", "shit", "damn", "hell", "ass", "bitch",
            "crap", "piss", "bastard", "cock", "dick"
        )

        /**
         * Supported language (currently English only).
         */
        private const val SUPPORTED_LANGUAGE = "en"
    }

    /**
     * Normalize user input through the full pipeline.
     *
     * @param input Raw user input (text or transcribed voice)
     * @return Normalization result with normalized text and modifications
     */
    fun normalize(input: String): NormalizationResult {
        if (input.isBlank()) {
            return NormalizationResult.unchanged(input)
        }

        val modifications = mutableListOf<Modification>()
        var normalized = input

        // 1. Language detection (simple check for now)
        if (!isEnglish(normalized)) {
            // For MVP, just proceed - non-English will fail classification
            // Future: return error or translate
        }

        // 2. Filter profanity
        val (afterProfanity, profanityMods) = filterProfanity(normalized)
        normalized = afterProfanity
        modifications.addAll(profanityMods)

        // 3. Normalize compound numbers first (before single words)
        val (afterCompoundNumbers, compoundNumberMods) = normalizeCompoundNumbers(normalized)
        normalized = afterCompoundNumbers
        modifications.addAll(compoundNumberMods)

        // 4. Expand golf slang
        val (afterSlang, slangMods) = expandGolfSlang(normalized)
        normalized = afterSlang
        modifications.addAll(slangMods)

        // 5. Normalize single number words
        val (afterNumbers, numberMods) = normalizeNumberWords(normalized)
        normalized = afterNumbers
        modifications.addAll(numberMods)

        // 6. Clean up whitespace
        normalized = cleanWhitespace(normalized)

        return NormalizationResult(
            normalizedInput = normalized,
            originalInput = input,
            wasModified = modifications.isNotEmpty() || normalized != input,
            modifications = modifications
        )
    }

    /**
     * Check if text is likely English.
     * Simple heuristic: check for common English words.
     */
    private fun isEnglish(text: String): Boolean {
        val englishWords = setOf(
            "the", "a", "an", "my", "your", "how", "what", "where", "when", "why",
            "is", "are", "was", "were", "be", "been", "have", "has", "had",
            "club", "iron", "wood", "shot", "ball", "green", "fairway", "yard"
        )

        val words = text.lowercase(Locale.US).split(Regex("\\s+"))
        val englishCount = words.count { it in englishWords }

        // If at least 10% of words are common English words, consider it English
        return words.isEmpty() || (englishCount.toFloat() / words.size) >= 0.1f
    }

    /**
     * Filter profanity by replacing with asterisks.
     */
    private fun filterProfanity(text: String): Pair<String, List<Modification>> {
        val modifications = mutableListOf<Modification>()
        var result = text

        profanityWords.forEach { word ->
            // Use word boundary regex for whole word matching
            val pattern = Regex("\\b$word\\b", RegexOption.IGNORE_CASE)
            val matches = pattern.findAll(result)

            matches.forEach { match ->
                val replacement = "*".repeat(match.value.length)
                modifications.add(
                    Modification(
                        type = ModificationType.PROFANITY,
                        original = match.value,
                        replacement = replacement
                    )
                )
            }

            result = pattern.replace(result, "*".repeat(word.length))
        }

        return result to modifications
    }

    /**
     * Normalize compound number patterns (e.g., "one fifty" -> "150").
     * Must run before single word normalization to avoid conflicts.
     */
    private fun normalizeCompoundNumbers(text: String): Pair<String, List<Modification>> {
        val modifications = mutableListOf<Modification>()
        var result = text

        // Sort by length (longest first) to match "one hundred fifty" before "one fifty"
        val sortedPatterns = GolfSlangDictionary.compoundNumbers.sortedByDescending { it.first.length }

        sortedPatterns.forEach { (pattern, replacement) ->
            val regex = Regex("\\b$pattern\\b", RegexOption.IGNORE_CASE)
            val matches = regex.findAll(result)

            matches.forEach { match ->
                modifications.add(
                    Modification(
                        type = ModificationType.NUMBER,
                        original = match.value,
                        replacement = replacement
                    )
                )
            }

            result = regex.replace(result, replacement)
        }

        return result to modifications
    }

    /**
     * Expand golf slang (e.g., "7i" -> "7-iron", "PW" -> "pitching wedge").
     */
    private fun expandGolfSlang(text: String): Pair<String, List<Modification>> {
        val modifications = mutableListOf<Modification>()
        var result = text

        // Combine club abbreviations and common terms
        val allSlang = GolfSlangDictionary.clubAbbreviations + GolfSlangDictionary.commonTerms

        allSlang.forEach { (slang, expansion) ->
            // Use word boundary regex for whole word matching
            val pattern = Regex("\\b$slang\\b", RegexOption.IGNORE_CASE)
            val matches = pattern.findAll(result)

            matches.forEach { match ->
                modifications.add(
                    Modification(
                        type = ModificationType.SLANG,
                        original = match.value,
                        replacement = expansion
                    )
                )
            }

            result = pattern.replace(result, expansion)
        }

        return result to modifications
    }

    /**
     * Normalize single number words (e.g., "one" -> "1", "fifty" -> "50").
     */
    private fun normalizeNumberWords(text: String): Pair<String, List<Modification>> {
        val modifications = mutableListOf<Modification>()
        var result = text

        GolfSlangDictionary.numberWords.forEach { (word, digit) ->
            val pattern = Regex("\\b$word\\b", RegexOption.IGNORE_CASE)
            val matches = pattern.findAll(result)

            matches.forEach { match ->
                modifications.add(
                    Modification(
                        type = ModificationType.NUMBER,
                        original = match.value,
                        replacement = digit
                    )
                )
            }

            result = pattern.replace(result, digit)
        }

        return result to modifications
    }

    /**
     * Clean up multiple spaces and trim.
     */
    private fun cleanWhitespace(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim()
    }
}
