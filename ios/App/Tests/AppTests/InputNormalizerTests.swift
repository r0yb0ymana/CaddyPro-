import XCTest
@testable import App

/// Unit tests for InputNormalizer.
///
/// Spec R2: Input normalization pipeline.
/// Spec A1, A2: Normalized input improves classification accuracy.
final class InputNormalizerTests: XCTestCase {

    private var normalizer: InputNormalizer!

    override func setUp() {
        super.setUp()
        normalizer = InputNormalizer()
    }

    override func tearDown() {
        normalizer = nil
        super.tearDown()
    }

    // MARK: - Golf Slang Expansion Tests

    func testExpandClubAbbreviations() {
        // Test iron abbreviations
        let result1 = normalizer.normalize("My 7i feels long")
        XCTAssertEqual(result1.normalizedInput, "My 7-iron feels long")
        XCTAssertTrue(result1.wasModified)
        XCTAssertEqual(result1.modifications.filter { $0.type == .slang }.count, 1)

        // Test wedge abbreviations
        let result2 = normalizer.normalize("Hit the PW from 120")
        XCTAssertEqual(result2.normalizedInput, "Hit the pitching wedge from 120")
        XCTAssertTrue(result2.wasModified)

        let result3 = normalizer.normalize("sw from the bunker")
        XCTAssertEqual(result3.normalizedInput, "sand wedge from the bunker")

        let result4 = normalizer.normalize("LW for this shot")
        XCTAssertEqual(result4.normalizedInput, "lob wedge for this shot")

        let result5 = normalizer.normalize("GW is my favorite")
        XCTAssertEqual(result5.normalizedInput, "gap wedge is my favorite")

        // Test wood abbreviations
        let result6 = normalizer.normalize("Hit the 3w off the deck")
        XCTAssertEqual(result6.normalizedInput, "Hit the 3-wood off the deck")

        let result7 = normalizer.normalize("Driver or D")
        XCTAssertEqual(result7.normalizedInput, "Driver or driver")

        // Test hybrid abbreviations
        let result8 = normalizer.normalize("4h from the rough")
        XCTAssertEqual(result8.normalizedInput, "4-hybrid from the rough")
    }

    func testExpandCommonGolfTerms() {
        // Test common slang
        let result1 = normalizer.normalize("Grab a stick")
        XCTAssertTrue(result1.normalizedInput.contains("club"))

        let result2 = normalizer.normalize("On the dance floor")
        XCTAssertTrue(result2.normalizedInput.contains("green"))

        let result3 = normalizer.normalize("Hit the short stuff")
        XCTAssertTrue(result3.normalizedInput.contains("fairway"))

        let result4 = normalizer.normalize("Use the big stick")
        XCTAssertTrue(result4.normalizedInput.contains("driver"))

        let result5 = normalizer.normalize("flatstick time")
        XCTAssertTrue(result5.normalizedInput.contains("putter"))

        let result6 = normalizer.normalize("Made a birdie")
        XCTAssertTrue(result6.normalizedInput.contains("one under par"))
    }

    func testMultipleSlangExpansions() {
        let result = normalizer.normalize("My 7i hit the dance floor")
        XCTAssertTrue(result.normalizedInput.contains("7-iron"))
        XCTAssertTrue(result.normalizedInput.contains("green"))
        XCTAssertTrue(result.wasModified)
        XCTAssertGreaterThanOrEqual(result.modifications.filter { $0.type == .slang }.count, 2)
    }

    // MARK: - Number Normalization Tests

    func testNormalizeCompositeNumbers() {
        // Test "one fifty" pattern
        let result1 = normalizer.normalize("from one fifty")
        XCTAssertTrue(result1.normalizedInput.contains("150"))
        XCTAssertTrue(result1.wasModified)

        let result2 = normalizer.normalize("two thirty out")
        XCTAssertTrue(result2.normalizedInput.contains("230"))

        let result3 = normalizer.normalize("one twenty to the flag")
        XCTAssertTrue(result3.normalizedInput.contains("120"))

        // Test "twenty three" pattern
        let result4 = normalizer.normalize("twenty three yards")
        XCTAssertTrue(result4.normalizedInput.contains("23"))

        let result5 = normalizer.normalize("thirty-seven feet")
        XCTAssertTrue(result5.normalizedInput.contains("37"))

        let result6 = normalizer.normalize("fifty nine degrees")
        XCTAssertTrue(result6.normalizedInput.contains("59"))
    }

    func testNormalizeSpokenClubNames() {
        // Test "seven iron" -> "7-iron"
        let result1 = normalizer.normalize("seven iron feels good")
        XCTAssertTrue(result1.normalizedInput.contains("7-iron"))
        XCTAssertTrue(result1.wasModified)

        let result2 = normalizer.normalize("five iron from here")
        XCTAssertTrue(result2.normalizedInput.contains("5-iron"))

        // Test "three wood" -> "3-wood"
        let result3 = normalizer.normalize("three wood off the tee")
        XCTAssertTrue(result3.normalizedInput.contains("3-wood"))

        let result4 = normalizer.normalize("five wood shot")
        XCTAssertTrue(result4.normalizedInput.contains("5-wood"))

        // Test "four hybrid" -> "4-hybrid"
        let result5 = normalizer.normalize("four hybrid is perfect")
        XCTAssertTrue(result5.normalizedInput.contains("4-hybrid"))
    }

    func testNormalizeSimpleNumbers() {
        let result1 = normalizer.normalize("five yards short")
        XCTAssertTrue(result1.normalizedInput.contains("5"))

        let result2 = normalizer.normalize("ten feet left")
        XCTAssertTrue(result2.normalizedInput.contains("10"))

        let result3 = normalizer.normalize("fifty yards")
        XCTAssertTrue(result3.normalizedInput.contains("50"))

        let result4 = normalizer.normalize("one hundred yards")
        XCTAssertTrue(result4.normalizedInput.contains("100"))
    }

    func testNumberNormalizationPriority() {
        // Composite patterns should be processed first
        let result1 = normalizer.normalize("one fifty yards")
        XCTAssertTrue(result1.normalizedInput.contains("150"))
        // Should not have "1 50" as separate replacements

        let result2 = normalizer.normalize("seven iron from one fifty")
        XCTAssertTrue(result2.normalizedInput.contains("7-iron"))
        XCTAssertTrue(result2.normalizedInput.contains("150"))
    }

    // MARK: - Profanity Filter Tests

    func testFilterProfanity() {
        let result1 = normalizer.normalize("damn it missed")
        XCTAssertTrue(result1.normalizedInput.contains("****"))
        XCTAssertFalse(result1.normalizedInput.contains("damn"))
        XCTAssertTrue(result1.wasModified)

        let result2 = normalizer.normalize("what the hell")
        XCTAssertTrue(result2.normalizedInput.contains("****"))
        XCTAssertFalse(result2.normalizedInput.contains("hell"))
    }

    func testProfanityDoesNotBreakIntent() {
        // Intent should still be clear after profanity filtering
        let result = normalizer.normalize("My damn 7i is long today")
        XCTAssertTrue(result.normalizedInput.contains("7-iron"))
        XCTAssertTrue(result.normalizedInput.contains("long"))
        XCTAssertTrue(result.normalizedInput.contains("****"))
    }

    func testMultipleProfanityInstances() {
        let result = normalizer.normalize("damn this shit is hard")
        let asteriskRanges = result.normalizedInput.ranges(of: "****")
        XCTAssertGreaterThanOrEqual(asteriskRanges.count, 2)
    }

    // MARK: - Language Detection Tests

    func testEnglishDetection() {
        XCTAssertTrue(normalizer.isEnglish("My 7-iron feels long"))
        XCTAssertTrue(normalizer.isEnglish("What is my recovery?"))
        XCTAssertTrue(normalizer.isEnglish("The shot was great"))
        XCTAssertTrue(normalizer.isEnglish("How are you?"))
        XCTAssertTrue(normalizer.isEnglish("I need help"))
    }

    func testNonEnglishDetection() {
        // Basic heuristic - may not catch all cases
        XCTAssertFalse(normalizer.isEnglish("Bonjour comment allez-vous"))
        XCTAssertFalse(normalizer.isEnglish("Hola como estas"))
        XCTAssertFalse(normalizer.isEnglish("Guten Tag wie geht es dir"))
    }

    func testEmptyAndShortInputLanguageDetection() {
        // Empty string should return false
        XCTAssertFalse(normalizer.isEnglish(""))

        // Very short strings without common English words
        XCTAssertFalse(normalizer.isEnglish("xyz"))

        // Short English phrases should pass
        XCTAssertTrue(normalizer.isEnglish("I am"))
        XCTAssertTrue(normalizer.isEnglish("the ball"))
    }

    // MARK: - Edge Cases and Integration Tests

    func testAlreadyNormalizedInput() {
        let result = normalizer.normalize("My 7-iron is feeling long today")
        // Should apply cleanup but not modify the core content
        XCTAssertTrue(result.normalizedInput.contains("7-iron"))
    }

    func testMixedInputNormalization() {
        // Test combining slang, numbers, and cleanup
        let result = normalizer.normalize("  My 7i from  one fifty  to the dance floor  ")
        XCTAssertTrue(result.normalizedInput.contains("7-iron"))
        XCTAssertTrue(result.normalizedInput.contains("150"))
        XCTAssertTrue(result.normalizedInput.contains("green"))
        XCTAssertEqual(result.normalizedInput.first, "M") // No leading space
        XCTAssertNotEqual(result.normalizedInput.last, " ") // No trailing space
        XCTAssertTrue(result.wasModified)
    }

    func testEmptyInput() {
        let result = normalizer.normalize("")
        XCTAssertEqual(result.normalizedInput, "")
        XCTAssertFalse(result.wasModified)
        XCTAssertEqual(result.modifications.count, 0)
    }

    func testWhitespaceOnlyInput() {
        let result = normalizer.normalize("   \n\t  ")
        XCTAssertEqual(result.normalizedInput, "")
        XCTAssertTrue(result.wasModified) // Cleanup was applied
    }

    func testMultipleSpacesNormalization() {
        let result = normalizer.normalize("My    7i    feels    long")
        XCTAssertFalse(result.normalizedInput.contains("    "))
        XCTAssertTrue(result.normalizedInput.contains(" "))
        XCTAssertTrue(result.wasModified)
    }

    func testPunctuationNormalization() {
        let result = normalizer.normalize("What!!!???")
        XCTAssertEqual(result.normalizedInput.filter { $0 == "!" }.count, 1)
        XCTAssertTrue(result.wasModified)
    }

    func testCaseSensitivity() {
        // Slang expansion should be case-insensitive
        let result1 = normalizer.normalize("My 7I feels long")
        XCTAssertTrue(result1.normalizedInput.contains("7-iron"))

        let result2 = normalizer.normalize("HIT THE PW")
        XCTAssertTrue(result2.normalizedInput.contains("pitching wedge"))

        let result3 = normalizer.normalize("SEVEN IRON")
        XCTAssertTrue(result3.normalizedInput.contains("7-iron"))
    }

    func testComplexRealWorldInputs() {
        // Test realistic user inputs
        let result1 = normalizer.normalize("My 7i from one fifty feels long today")
        XCTAssertTrue(result1.normalizedInput.contains("7-iron"))
        XCTAssertTrue(result1.normalizedInput.contains("150"))
        XCTAssertTrue(result1.wasModified)

        let result2 = normalizer.normalize("hit my pw to the dance floor from one twenty")
        XCTAssertTrue(result2.normalizedInput.contains("pitching wedge"))
        XCTAssertTrue(result2.normalizedInput.contains("green"))
        XCTAssertTrue(result2.normalizedInput.contains("120"))

        let result3 = normalizer.normalize("How's my recovery looking?")
        // Should pass through with minimal changes
        XCTAssertTrue(result3.normalizedInput.contains("recovery"))

        let result4 = normalizer.normalize("damn my 7i from one fifty missed the green")
        XCTAssertTrue(result4.normalizedInput.contains("****")) // Profanity filtered
        XCTAssertTrue(result4.normalizedInput.contains("7-iron"))
        XCTAssertTrue(result4.normalizedInput.contains("150"))
        XCTAssertTrue(result4.normalizedInput.contains("green"))
    }

    // MARK: - Modification Tracking Tests

    func testModificationTracking() {
        let result = normalizer.normalize("My 7i from one fifty")

        // Should have multiple modifications
        XCTAssertTrue(result.wasModified)
        XCTAssertGreaterThan(result.modifications.count, 0)

        // Check that modifications contain expected types
        let slangMods = result.modifications.filter { $0.type == .slang }
        let numberMods = result.modifications.filter { $0.type == .number }

        XCTAssertGreaterThan(slangMods.count, 0)
        XCTAssertGreaterThan(numberMods.count, 0)

        // Check that modifications contain original and replacement
        for modification in result.modifications {
            XCTAssertFalse(modification.original.isEmpty)
            XCTAssertFalse(modification.replacement.isEmpty)
        }
    }

    func testNoModificationTracking() {
        let result = normalizer.normalize("Hello there")
        XCTAssertFalse(result.wasModified)
        XCTAssertEqual(result.modifications.count, 0)
    }

    // MARK: - Performance Tests

    func testNormalizationPerformance() {
        let testInputs = [
            "My 7i from one fifty to the dance floor",
            "Hit the pw from one twenty",
            "How's my recovery looking?",
            "seven iron feels long",
            "damn that was a bad shot",
            "What club should I hit from one fifty?",
            "sw from the beach",
            "three wood off the tee"
        ]

        measure {
            for input in testInputs {
                _ = normalizer.normalize(input)
            }
        }
    }
}

// MARK: - String Extension for Testing

private extension String {
    func ranges(of searchString: String) -> [Range<String.Index>] {
        var ranges: [Range<String.Index>] = []
        var searchStartIndex = startIndex

        while searchStartIndex < endIndex,
              let range = range(of: searchString, range: searchStartIndex..<endIndex),
              !range.isEmpty {
            ranges.append(range)
            searchStartIndex = range.upperBound
        }

        return ranges
    }
}
