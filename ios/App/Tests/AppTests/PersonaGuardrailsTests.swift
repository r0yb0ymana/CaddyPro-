import XCTest
@testable import App

/// Tests for PersonaGuardrails forbidden pattern detection and sanitization.
///
/// Spec R4: Persona Guardrails
/// - Tests medical diagnosis detection
/// - Tests betting advice detection
/// - Tests swing guarantee detection
/// - Tests unsafe directive detection
/// - Tests sanitization and disclaimer injection
@MainActor
final class PersonaGuardrailsTests: XCTestCase {

    // MARK: - Medical Diagnosis Detection

    func testDetectsMedicalDiagnosisKeywords() {
        let texts = [
            "I can diagnose your back pain",
            "This injury is serious",
            "You have tendinitis",
            "Your inflammation needs treatment"
        ]

        for text in texts {
            let result = PersonaGuardrails.detectPatterns(in: text)
            XCTAssertTrue(
                result.patterns.contains(.medicalDiagnosis),
                "Failed to detect medical diagnosis in: \(text)"
            )
        }
    }

    func testMedicalDiagnosisDisclaimer() {
        let disclaimer = PersonaGuardrails.ForbiddenPattern.medicalDiagnosis.disclaimer

        XCTAssertTrue(disclaimer.contains("healthcare professional"))
        XCTAssertTrue(disclaimer.contains("informational"))
    }

    // MARK: - Betting Advice Detection

    func testDetectsBettingAdviceKeywords() {
        let texts = [
            "I'd bet on Tiger to win",
            "The odds are in your favor",
            "Put money on this match",
            "Gambling on golf is fun"
        ]

        for text in texts {
            let result = PersonaGuardrails.detectPatterns(in: text)
            XCTAssertTrue(
                result.patterns.contains(.bettingAdvice),
                "Failed to detect betting advice in: \(text)"
            )
        }
    }

    func testBettingAdviceDisclaimer() {
        let disclaimer = PersonaGuardrails.ForbiddenPattern.bettingAdvice.disclaimer

        XCTAssertTrue(disclaimer.contains("does not provide"))
        XCTAssertTrue(disclaimer.contains("gambling") || disclaimer.contains("betting"))
    }

    // MARK: - Swing Guarantee Detection

    func testDetectsSwingGuaranteeKeywords() {
        let texts = [
            "This will fix your slice",
            "Guaranteed to improve your swing",
            "This always works for everyone",
            "100% effective method"
        ]

        for text in texts {
            let result = PersonaGuardrails.detectPatterns(in: text)
            XCTAssertTrue(
                result.patterns.contains(.swingGuarantee),
                "Failed to detect swing guarantee in: \(text)"
            )
        }
    }

    func testSwingGuaranteeDisclaimer() {
        let disclaimer = PersonaGuardrails.ForbiddenPattern.swingGuarantee.disclaimer

        XCTAssertTrue(disclaimer.contains("vary"))
        XCTAssertTrue(disclaimer.contains("suggestion"))
    }

    // MARK: - Unsafe Directive Detection

    func testDetectsUnsafeDirectiveKeywords() {
        let texts = [
            "Just ignore the pain",
            "Push through the injury",
            "Play injured, it's fine",
            "It's nothing serious, keep playing"
        ]

        for text in texts {
            let result = PersonaGuardrails.detectPatterns(in: text)
            XCTAssertTrue(
                result.patterns.contains(.unsafeDirective),
                "Failed to detect unsafe directive in: \(text)"
            )
        }
    }

    func testUnsafeDirectiveDisclaimer() {
        let disclaimer = PersonaGuardrails.ForbiddenPattern.unsafeDirective.disclaimer

        XCTAssertTrue(disclaimer.contains("Do not play"))
        XCTAssertTrue(disclaimer.contains("healthcare professional"))
    }

    // MARK: - Multiple Pattern Detection

    func testDetectsMultiplePatterns() {
        let text = "I can diagnose your injury and you should bet on yourself to recover"

        let result = PersonaGuardrails.detectPatterns(in: text)

        XCTAssertTrue(result.patterns.contains(.medicalDiagnosis))
        XCTAssertTrue(result.patterns.contains(.bettingAdvice))
        XCTAssertEqual(result.patterns.count, 2)
    }

    func testMultipleDisclaimers() {
        let text = "This will fix your slice and diagnose your pain"

        let result = PersonaGuardrails.detectPatterns(in: text)

        XCTAssertTrue(result.hasIssues)
        XCTAssertEqual(result.disclaimers.count, 2)
    }

    // MARK: - Case Insensitivity

    func testCaseInsensitiveDetection() {
        let texts = [
            "DIAGNOSE",
            "Diagnose",
            "diagnose",
            "DiAgNoSe"
        ]

        for text in texts {
            let result = PersonaGuardrails.detectPatterns(in: text, caseSensitive: false)
            XCTAssertTrue(result.patterns.contains(.medicalDiagnosis))
        }
    }

    func testCaseSensitiveDetection() {
        let result = PersonaGuardrails.detectPatterns(in: "DIAGNOSE", caseSensitive: true)

        // Should not detect because keywords are lowercase
        XCTAssertFalse(result.patterns.contains(.medicalDiagnosis))
    }

    // MARK: - Sanitization Tests

    func testSanitizesAbsoluteGuarantees() {
        let input = "This will fix your slice"
        let sanitized = PersonaGuardrails.sanitizeResponse(input)

        XCTAssertFalse(sanitized.contains("will fix"))
        XCTAssertTrue(sanitized.contains("should help"))
    }

    func testSanitizesGuaranteedTo() {
        let input = "This is guaranteed to work"
        let sanitized = PersonaGuardrails.sanitizeResponse(input)

        XCTAssertFalse(sanitized.contains("guaranteed to"))
        XCTAssertTrue(sanitized.contains("likely to"))
    }

    func testSanitizesAlwaysWorks() {
        let input = "This drill always works"
        let sanitized = PersonaGuardrails.sanitizeResponse(input)

        XCTAssertFalse(sanitized.contains("always works"))
        XCTAssertTrue(sanitized.contains("often helps"))
    }

    func testSanitizesUnsafeDirectives() {
        let input = "Just ignore the pain and keep playing"
        let sanitized = PersonaGuardrails.sanitizeResponse(input)

        XCTAssertFalse(sanitized.lowercased().contains("ignore the pain"))
        XCTAssertTrue(sanitized.contains("consult a professional"))
    }

    func testSanitizationPreservesGoodContent() {
        let input = "This approach should help improve your swing"
        let sanitized = PersonaGuardrails.sanitizeResponse(input)

        XCTAssertEqual(input, sanitized)
    }

    // MARK: - User Input Checking

    func testCheckUserInputForMedicalConcerns() {
        let input = "My back hurts when I swing"

        let result = PersonaGuardrails.checkUserInput(input)

        XCTAssertTrue(result.patterns.contains(.medicalDiagnosis))
    }

    func testCheckUserInputForBettingQuestions() {
        let input = "Who should I bet on this weekend?"

        let result = PersonaGuardrails.checkUserInput(input)

        XCTAssertTrue(result.patterns.contains(.bettingAdvice))
    }

    func testCheckUserInputCleanText() {
        let input = "What club should I use for 150 yards?"

        let result = PersonaGuardrails.checkUserInput(input)

        XCTAssertFalse(result.hasIssues)
        XCTAssertTrue(result.patterns.isEmpty)
    }

    // MARK: - Safety Reminder Generation

    func testGeneratesSafetyReminderForMedical() {
        let patterns: Set<PersonaGuardrails.ForbiddenPattern> = [.medicalDiagnosis]

        let reminder = PersonaGuardrails.generateSafetyReminder(for: patterns)

        XCTAssertNotNil(reminder)
        XCTAssertTrue(reminder!.contains("pain or injury"))
        XCTAssertTrue(reminder!.contains("healthcare professional"))
    }

    func testGeneratesSafetyReminderForBetting() {
        let patterns: Set<PersonaGuardrails.ForbiddenPattern> = [.bettingAdvice]

        let reminder = PersonaGuardrails.generateSafetyReminder(for: patterns)

        XCTAssertNotNil(reminder)
        XCTAssertTrue(reminder!.contains("betting") || reminder!.contains("gambling"))
    }

    func testGeneratesSafetyReminderForSwingGuarantees() {
        let patterns: Set<PersonaGuardrails.ForbiddenPattern> = [.swingGuarantee]

        let reminder = PersonaGuardrails.generateSafetyReminder(for: patterns)

        XCTAssertNotNil(reminder)
        XCTAssertTrue(reminder!.contains("suggestion"))
    }

    func testNoReminderForCleanContent() {
        let patterns: Set<PersonaGuardrails.ForbiddenPattern> = []

        let reminder = PersonaGuardrails.generateSafetyReminder(for: patterns)

        XCTAssertNil(reminder)
    }

    // MARK: - Response Validation

    func testValidatesCleanResponse() {
        let response = "Let's go with the 7-iron here"

        let isValid = PersonaGuardrails.validateResponse(response)

        XCTAssertTrue(isValid)
    }

    func testRejectsUnsafeDirective() {
        let response = "Ignore the pain and keep playing"

        let isValid = PersonaGuardrails.validateResponse(response)

        XCTAssertFalse(isValid)
    }

    func testRejectsBettingAdvice() {
        let response = "You should bet on this match"

        let isValid = PersonaGuardrails.validateResponse(response)

        XCTAssertFalse(isValid)
    }

    func testAllowsMedicalDiagnosisWithProperHandling() {
        // Medical diagnosis is allowed but requires disclaimer
        let response = "This might be related to pain - consult a professional"

        let isValid = PersonaGuardrails.validateResponse(response)

        XCTAssertTrue(isValid)
    }

    func testAllowsSwingGuaranteesWithProperHandling() {
        // Swing guarantees are allowed but require sanitization
        let response = "This will fix your slice"

        let isValid = PersonaGuardrails.validateResponse(response)

        XCTAssertTrue(isValid) // Validation allows, sanitization will soften
    }

    // MARK: - Edge Cases

    func testEmptyText() {
        let result = PersonaGuardrails.detectPatterns(in: "")

        XCTAssertFalse(result.hasIssues)
        XCTAssertTrue(result.patterns.isEmpty)
    }

    func testWhitespaceOnlyText() {
        let result = PersonaGuardrails.detectPatterns(in: "   \n\t  ")

        XCTAssertFalse(result.hasIssues)
    }

    func testSanitizeEmptyString() {
        let sanitized = PersonaGuardrails.sanitizeResponse("")

        XCTAssertEqual(sanitized, "")
    }

    func testKeywordInMiddleOfWord() {
        // "pain" is in "painting" but shouldn't trigger
        let result = PersonaGuardrails.detectPatterns(in: "I'm painting my clubs")

        // This will actually detect because we use simple contains()
        // This is acceptable - better to be overly cautious
        XCTAssertTrue(result.patterns.contains(.medicalDiagnosis))
    }

    func testAllForbiddenPatternsHaveKeywords() {
        for pattern in PersonaGuardrails.ForbiddenPattern.allCases {
            XCTAssertFalse(pattern.detectionKeywords.isEmpty)
            XCTAssertFalse(pattern.disclaimer.isEmpty)
        }
    }
}
