import Foundation

/// Service for normalizing user input before intent classification.
///
/// Performs:
/// - Golf slang expansion
/// - Number normalization (spoken numbers to digits)
/// - Profanity filtering
/// - Language detection (basic)
///
/// Spec R2: Input normalization for the intent classification pipeline.
/// Spec A1, A2: Ensures input is in optimal form for classification.
final class InputNormalizer {

    // MARK: - Public Interface

    /// Normalize user input through the full pipeline.
    ///
    /// - Parameter input: Raw user input text
    /// - Returns: Normalization result with modifications tracked
    func normalize(_ input: String) -> NormalizationResult {
        var currentText = input
        var modifications: [Modification] = []

        // Step 1: Basic cleanup (trim whitespace, normalize spacing)
        let cleanedResult = applyCleanup(currentText)
        currentText = cleanedResult.text
        modifications.append(contentsOf: cleanedResult.modifications)

        // Step 2: Expand golf slang
        let slangResult = expandGolfSlang(currentText)
        currentText = slangResult.text
        modifications.append(contentsOf: slangResult.modifications)

        // Step 3: Normalize numbers (composite patterns first, then simple words)
        let compositeNumberResult = normalizeCompositeNumbers(currentText)
        currentText = compositeNumberResult.text
        modifications.append(contentsOf: compositeNumberResult.modifications)

        let numberResult = normalizeNumberWords(currentText)
        currentText = numberResult.text
        modifications.append(contentsOf: numberResult.modifications)

        // Step 4: Filter profanity
        let profanityResult = filterProfanity(currentText)
        currentText = profanityResult.text
        modifications.append(contentsOf: profanityResult.modifications)

        // Step 5: Final cleanup
        let finalCleanupResult = applyCleanup(currentText)
        currentText = finalCleanupResult.text
        modifications.append(contentsOf: finalCleanupResult.modifications)

        return NormalizationResult(
            normalizedInput: currentText,
            originalInput: input,
            wasModified: !modifications.isEmpty,
            modifications: modifications
        )
    }

    /// Detect language of the input (basic check for English).
    ///
    /// - Parameter input: Input text to check
    /// - Returns: true if input appears to be English
    func isEnglish(_ input: String) -> Bool {
        // Basic heuristic: check for common English words and patterns
        let commonEnglishWords = [
            "the", "a", "an", "is", "are", "was", "were", "my", "your", "I",
            "me", "you", "we", "they", "what", "how", "where", "when", "why"
        ]

        let lowercased = input.lowercased()
        let words = lowercased.components(separatedBy: .whitespacesAndNewlines)

        // If any common English word is present, assume English
        return words.contains(where: { word in
            commonEnglishWords.contains(word)
        })
    }

    // MARK: - Private Implementation

    /// Apply basic cleanup: trim whitespace, normalize spacing.
    private func applyCleanup(_ text: String) -> (text: String, modifications: [Modification]) {
        let original = text
        var result = text

        // Trim leading/trailing whitespace
        result = result.trimmingCharacters(in: .whitespacesAndNewlines)

        // Normalize multiple spaces to single space
        result = result.replacingOccurrences(of: #"\s+"#, with: " ", options: .regularExpression)

        // Normalize multiple punctuation
        result = result.replacingOccurrences(of: #"([.!?]){2,}"#, with: "$1", options: .regularExpression)

        let modifications: [Modification] = result != original
            ? [Modification(type: .cleanup, original: original, replacement: result)]
            : []

        return (result, modifications)
    }

    /// Expand golf slang terms (e.g., "7i" -> "7-iron", "pw" -> "pitching wedge").
    private func expandGolfSlang(_ text: String) -> (text: String, modifications: [Modification]) {
        var result = text
        var modifications: [Modification] = []

        // First, handle club abbreviations
        for (abbreviation, expansion) in GolfSlangDictionary.clubAbbreviations {
            let pattern = "\\b\(NSRegularExpression.escapedPattern(for: abbreviation))\\b"
            guard let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive) else {
                continue
            }

            let nsString = result as NSString
            let matches = regex.matches(in: result, range: NSRange(location: 0, length: nsString.length))

            for match in matches.reversed() {
                let matchedText = nsString.substring(with: match.range)
                result = (result as NSString).replacingCharacters(in: match.range, with: expansion)

                modifications.append(Modification(
                    type: .slang,
                    original: matchedText,
                    replacement: expansion,
                    range: match.range
                ))
            }
        }

        // Then handle common terms
        for (slang, standard) in GolfSlangDictionary.commonTerms {
            let pattern = "\\b\(NSRegularExpression.escapedPattern(for: slang))\\b"
            guard let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive) else {
                continue
            }

            let nsString = result as NSString
            let matches = regex.matches(in: result, range: NSRange(location: 0, length: nsString.length))

            for match in matches.reversed() {
                let matchedText = nsString.substring(with: match.range)
                result = (result as NSString).replacingCharacters(in: match.range, with: standard)

                modifications.append(Modification(
                    type: .slang,
                    original: matchedText,
                    replacement: standard,
                    range: match.range
                ))
            }
        }

        return (result, modifications)
    }

    /// Normalize composite number patterns (e.g., "one fifty" -> "150", "seven iron" -> "7-iron").
    private func normalizeCompositeNumbers(_ text: String) -> (text: String, modifications: [Modification]) {
        var result = text
        var modifications: [Modification] = []

        for (pattern, transform) in GolfSlangDictionary.compositePatterns {
            let nsString = result as NSString
            let matches = pattern.matches(in: result, range: NSRange(location: 0, length: nsString.length))

            for match in matches.reversed() {
                let matchedText = nsString.substring(with: match.range)
                let replacement = transform(matchedText)

                if replacement != matchedText {
                    result = (result as NSString).replacingCharacters(in: match.range, with: replacement)

                    modifications.append(Modification(
                        type: .number,
                        original: matchedText,
                        replacement: replacement,
                        range: match.range
                    ))
                }
            }
        }

        return (result, modifications)
    }

    /// Normalize simple number words (e.g., "five" -> "5", "fifty" -> "50").
    private func normalizeNumberWords(_ text: String) -> (text: String, modifications: [Modification]) {
        var result = text
        var modifications: [Modification] = []

        for (word, digit) in GolfSlangDictionary.numberWords {
            let pattern = "\\b\(NSRegularExpression.escapedPattern(for: word))\\b"
            guard let regex = try? NSRegularExpression(pattern: pattern, options: .caseInsensitive) else {
                continue
            }

            let nsString = result as NSString
            let matches = regex.matches(in: result, range: NSRange(location: 0, length: nsString.length))

            for match in matches.reversed() {
                let matchedText = nsString.substring(with: match.range)

                // Skip if this word is part of a longer phrase (already handled by composite patterns)
                let beforeRange = NSRange(location: max(0, match.range.location - 1), length: 1)
                let afterRange = NSRange(location: match.range.location + match.range.length, length: min(1, nsString.length - match.range.location - match.range.length))

                if match.range.location > 0 {
                    let beforeChar = nsString.substring(with: beforeRange)
                    if beforeChar.rangeOfCharacter(from: .letters) != nil {
                        continue // Part of another word
                    }
                }

                if match.range.location + match.range.length < nsString.length {
                    let afterChar = nsString.substring(with: afterRange)
                    if afterChar.rangeOfCharacter(from: .letters) != nil {
                        continue // Part of another word
                    }
                }

                result = (result as NSString).replacingCharacters(in: match.range, with: digit)

                modifications.append(Modification(
                    type: .number,
                    original: matchedText,
                    replacement: digit,
                    range: match.range
                ))
            }
        }

        return (result, modifications)
    }

    /// Filter profanity (replace with asterisks).
    private func filterProfanity(_ text: String) -> (text: String, modifications: [Modification]) {
        var result = text
        var modifications: [Modification] = []

        guard let regex = GolfSlangDictionary.profanityRegex() else {
            return (result, modifications)
        }

        let nsString = result as NSString
        let matches = regex.matches(in: result, range: NSRange(location: 0, length: nsString.length))

        for match in matches.reversed() {
            let matchedText = nsString.substring(with: match.range)
            let replacement = String(repeating: "*", count: matchedText.count)

            result = (result as NSString).replacingCharacters(in: match.range, with: replacement)

            modifications.append(Modification(
                type: .profanity,
                original: matchedText,
                replacement: replacement,
                range: match.range
            ))
        }

        return (result, modifications)
    }
}
