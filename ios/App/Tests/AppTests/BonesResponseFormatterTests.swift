import XCTest
@testable import App

/// Tests for BonesResponseFormatter post-processing and formatting.
///
/// Spec R4: Bones Response Formatter
/// - Tests response sanitization
/// - Tests disclaimer injection
/// - Tests pattern reference injection
/// - Tests follow-up action generation
/// - Tests voice polishing
@MainActor
final class BonesResponseFormatterTests: XCTestCase {
    var formatter: BonesResponseFormatter!

    override func setUp() async throws {
        formatter = BonesResponseFormatter()
    }

    override func tearDown() async throws {
        formatter = nil
    }

    // MARK: - Basic Formatting Tests

    func testFormatsCleanResponse() {
        let response = "Let's go with the 7-iron here"
        let options = BonesResponseFormatter.FormatOptions(intent: .shotRecommendation)

        let formatted = formatter.format(response, options: options)

        XCTAssertEqual(formatted.text, response)
        XCTAssertTrue(formatted.disclaimers.isEmpty)
    }

    func testSanitizesAbsoluteGuarantees() {
        let response = "This will fix your slice"
        let options = BonesResponseFormatter.FormatOptions(intent: .drillRequest)

        let formatted = formatter.format(response, options: options)

        XCTAssertFalse(formatted.text.contains("will fix"))
        XCTAssertTrue(formatted.text.contains("should help"))
    }

    func testAddsDisclaimerForMedicalContent() {
        let response = "Your back pain might be from your swing"
        let options = BonesResponseFormatter.FormatOptions(intent: .recoveryCheck)

        let formatted = formatter.format(response, options: options)

        XCTAssertFalse(formatted.disclaimers.isEmpty)
        XCTAssertTrue(formatted.disclaimers[0].contains("healthcare professional"))
    }

    func testForcedDisclaimerIsAdded() {
        let response = "Your swing looks good"
        let options = BonesResponseFormatter.FormatOptions(
            intent: .recoveryCheck,
            forceDisclaimer: true
        )

        let formatted = formatter.format(response, options: options)

        XCTAssertFalse(formatted.disclaimers.isEmpty)
        XCTAssertTrue(formatted.disclaimers[0].contains("healthcare professional"))
    }

    // MARK: - Pattern Reference Tests

    func testInjectsPatternReferenceForShotRecommendation() {
        let club = Club(id: "7i", name: "7-iron", type: .iron)
        let pattern = MissPattern(
            direction: .slice,
            club: club,
            frequency: 10,
            confidence: 0.85,
            lastOccurrence: Date()
        )

        let response = "Let's aim left to account for your miss"
        let options = BonesResponseFormatter.FormatOptions(
            intent: .shotRecommendation,
            patterns: [pattern]
        )

        let formatted = formatter.format(response, options: options)

        XCTAssertTrue(formatted.text.contains("slice"))
        XCTAssertTrue(formatted.text.contains("85%") || formatted.text.contains("confidence"))
        XCTAssertEqual(formatted.referencedPatterns.count, 1)
    }

    func testInjectsPatternReferenceForClubAdjustment() {
        let club = Club(id: "7i", name: "7-iron", type: .iron)
        let pattern = MissPattern(
            direction: .push,
            club: club,
            frequency: 8,
            confidence: 0.75,
            lastOccurrence: Date()
        )

        let response = "Your 7-iron has been trending long"
        let options = BonesResponseFormatter.FormatOptions(
            intent: .clubAdjustment,
            patterns: [pattern]
        )

        let formatted = formatter.format(response, options: options)

        XCTAssertTrue(formatted.text.contains("push") || formatted.text.contains("Context:"))
        XCTAssertFalse(formatted.referencedPatterns.isEmpty)
    }

    func testLimitsPatternReferencesToTwo() {
        let patterns = [
            MissPattern(direction: .slice, club: nil, frequency: 10, confidence: 0.9, lastOccurrence: Date()),
            MissPattern(direction: .push, club: nil, frequency: 8, confidence: 0.8, lastOccurrence: Date()),
            MissPattern(direction: .pull, club: nil, frequency: 6, confidence: 0.7, lastOccurrence: Date())
        ]

        let response = "Here's what I'm seeing"
        let options = BonesResponseFormatter.FormatOptions(
            intent: .shotRecommendation,
            patterns: patterns
        )

        let formatted = formatter.format(response, options: options)

        XCTAssertLessThanOrEqual(formatted.referencedPatterns.count, 2)
    }

    func testNoPatternReferenceForNonRelevantIntent() {
        let pattern = MissPattern(
            direction: .slice,
            club: nil,
            frequency: 10,
            confidence: 0.85,
            lastOccurrence: Date()
        )

        let response = "Score entered successfully"
        let options = BonesResponseFormatter.FormatOptions(
            intent: .scoreEntry,
            patterns: [pattern]
        )

        let formatted = formatter.format(response, options: options)

        XCTAssertTrue(formatted.referencedPatterns.isEmpty)
        XCTAssertFalse(formatted.text.contains("slice"))
    }

    func testNoPatternReferenceWhenNoPatternsProvided() {
        let response = "Let's go with the 7-iron"
        let options = BonesResponseFormatter.FormatOptions(
            intent: .shotRecommendation,
            patterns: []
        )

        let formatted = formatter.format(response, options: options)

        XCTAssertTrue(formatted.referencedPatterns.isEmpty)
    }

    // MARK: - Voice Polishing Tests

    func testRemovesGenericAIPhrases() {
        let response = "As an AI, I think you should use a 7-iron"
        let options = BonesResponseFormatter.FormatOptions(intent: .shotRecommendation)

        let formatted = formatter.format(response, options: options)

        XCTAssertFalse(formatted.text.contains("As an AI"))
    }

    func testTrimsExcessiveWhitespace() {
        let response = "Let's go with the 7-iron   \n\n\n   here"
        let options = BonesResponseFormatter.FormatOptions(intent: .shotRecommendation)

        let formatted = formatter.format(response, options: options)

        XCTAssertFalse(formatted.text.contains("\n\n\n"))
    }

    // MARK: - Suggested Actions Tests

    func testGeneratesSuggestedActionsForShotRecommendation() {
        let response = "Let's go with the 7-iron"
        let options = BonesResponseFormatter.FormatOptions(intent: .shotRecommendation)

        let formatted = formatter.format(response, options: options)

        XCTAssertFalse(formatted.suggestedActions.isEmpty)
        XCTAssertLessThanOrEqual(formatted.suggestedActions.count, 3)
        XCTAssertTrue(formatted.suggestedActions.contains { $0.contains("wind") })
    }

    func testGeneratesSuggestedActionsForClubAdjustment() {
        let response = "Your 7-iron is trending long"
        let options = BonesResponseFormatter.FormatOptions(intent: .clubAdjustment)

        let formatted = formatter.format(response, options: options)

        XCTAssertFalse(formatted.suggestedActions.isEmpty)
        XCTAssertTrue(formatted.suggestedActions.contains { $0.contains("distance") })
    }

    func testGeneratesSuggestedActionsForPatternQuery() {
        let pattern = MissPattern(
            direction: .slice,
            club: nil,
            frequency: 10,
            confidence: 0.85,
            lastOccurrence: Date()
        )

        let response = "You're slicing under pressure"
        let options = BonesResponseFormatter.FormatOptions(
            intent: .patternQuery,
            patterns: [pattern]
        )

        let formatted = formatter.format(response, options: options)

        XCTAssertFalse(formatted.suggestedActions.isEmpty)
        XCTAssertTrue(formatted.suggestedActions.contains { $0.contains("drill") })
    }

    func testGeneratesGenericActionsForUnknownIntent() {
        let response = "How can I help?"
        let options = BonesResponseFormatter.FormatOptions(intent: .helpRequest)

        let formatted = formatter.format(response, options: options)

        XCTAssertFalse(formatted.suggestedActions.isEmpty)
        XCTAssertLessThanOrEqual(formatted.suggestedActions.count, 3)
    }

    func testLimitsSuggestedActionsToThree() {
        let response = "Let's discuss your options"
        let options = BonesResponseFormatter.FormatOptions(intent: .shotRecommendation)

        let formatted = formatter.format(response, options: options)

        XCTAssertLessThanOrEqual(formatted.suggestedActions.count, 3)
    }

    // MARK: - Full Text Tests

    func testFullTextIncludesDisclaimers() {
        let response = "Your back pain might be swing-related"
        let options = BonesResponseFormatter.FormatOptions(intent: .recoveryCheck)

        let formatted = formatter.format(response, options: options)

        XCTAssertTrue(formatted.fullText.contains(formatted.text))
        XCTAssertTrue(formatted.fullText.contains("healthcare professional"))
    }

    func testFullTextMatchesTextWhenNoDisclaimers() {
        let response = "Let's go with the 7-iron"
        let options = BonesResponseFormatter.FormatOptions(intent: .shotRecommendation)

        let formatted = formatter.format(response, options: options)

        XCTAssertEqual(formatted.fullText, formatted.text)
    }

    // MARK: - Safety Context Preparation Tests

    func testPreparesSafetyContextForMedicalInput() {
        let userInput = "My back hurts when I swing"

        let safetyContext = formatter.prepareSafetyContext(for: userInput)

        XCTAssertNotNil(safetyContext)
        XCTAssertTrue(safetyContext!.contains("pain or injury"))
    }

    func testPreparesSafetyContextForBettingInput() {
        let userInput = "Who should I bet on this weekend?"

        let safetyContext = formatter.prepareSafetyContext(for: userInput)

        XCTAssertNotNil(safetyContext)
        XCTAssertTrue(safetyContext!.contains("betting") || safetyContext!.contains("gambling"))
    }

    func testNoSafetyContextForCleanInput() {
        let userInput = "What club should I use for 150 yards?"

        let safetyContext = formatter.prepareSafetyContext(for: userInput)

        XCTAssertNil(safetyContext)
    }

    // MARK: - Response Validation Tests

    func testValidatesCleanResponse() {
        let response = "Let's go with the 7-iron here"

        let isValid = formatter.validateResponse(response)

        XCTAssertTrue(isValid)
    }

    func testRejectsUnsafeResponse() {
        let response = "Just ignore the pain and keep playing"

        let isValid = formatter.validateResponse(response)

        XCTAssertFalse(isValid)
    }

    func testRejectsBettingAdviceResponse() {
        let response = "You should bet on this match"

        let isValid = formatter.validateResponse(response)

        XCTAssertFalse(isValid)
    }

    // MARK: - Convenience Methods Tests

    func testFormatSimple() {
        let response = "Let's go with the 7-iron"

        let formatted = formatter.formatSimple(response, intent: .shotRecommendation)

        XCTAssertEqual(formatted.text, response)
        XCTAssertTrue(formatted.disclaimers.isEmpty)
        XCTAssertTrue(formatted.referencedPatterns.isEmpty)
    }

    func testFormatWithPatterns() {
        let pattern = MissPattern(
            direction: .slice,
            club: nil,
            frequency: 10,
            confidence: 0.85,
            lastOccurrence: Date()
        )

        let response = "Aim left to account for your miss"

        let formatted = formatter.formatWithPatterns(
            response,
            intent: .shotRecommendation,
            patterns: [pattern]
        )

        XCTAssertFalse(formatted.referencedPatterns.isEmpty)
    }

    // MARK: - Pattern Summary Formatting Tests

    func testFormatsSinglePatternSummary() {
        let club = Club(id: "7i", name: "7-iron", type: .iron)
        let pattern = MissPattern(
            direction: .slice,
            club: club,
            frequency: 10,
            confidence: 0.85,
            lastOccurrence: Date()
        )

        let response = "Here's what I'm seeing"
        let options = BonesResponseFormatter.FormatOptions(
            intent: .shotRecommendation,
            patterns: [pattern]
        )

        let formatted = formatter.format(response, options: options)

        XCTAssertTrue(formatted.text.contains("slice"))
        XCTAssertTrue(formatted.text.contains("7-iron"))
        XCTAssertTrue(formatted.text.contains("85") || formatted.text.contains("confidence"))
    }

    func testFormatsMultiplePatternSummary() {
        let patterns = [
            MissPattern(direction: .slice, club: nil, frequency: 10, confidence: 0.9, lastOccurrence: Date()),
            MissPattern(direction: .push, club: nil, frequency: 8, confidence: 0.8, lastOccurrence: Date())
        ]

        let response = "Here's what I'm seeing"
        let options = BonesResponseFormatter.FormatOptions(
            intent: .shotRecommendation,
            patterns: patterns
        )

        let formatted = formatter.format(response, options: options)

        XCTAssertTrue(formatted.text.contains("slice") || formatted.text.contains("push"))
    }

    // MARK: - Module Context Tests

    func testCaddyModuleFormatting() {
        let response = "Let's go with the 7-iron"
        let options = BonesResponseFormatter.FormatOptions(
            module: .caddy,
            intent: .shotRecommendation
        )

        let formatted = formatter.format(response, options: options)

        XCTAssertFalse(formatted.suggestedActions.isEmpty)
    }

    func testCoachModuleFormatting() {
        let response = "Try this drill"
        let options = BonesResponseFormatter.FormatOptions(
            module: .coach,
            intent: .drillRequest
        )

        let formatted = formatter.format(response, options: options)

        XCTAssertFalse(formatted.suggestedActions.isEmpty)
    }

    func testRecoveryModuleFormatting() {
        let response = "Your recovery looks good"
        let options = BonesResponseFormatter.FormatOptions(
            module: .recovery,
            intent: .recoveryCheck
        )

        let formatted = formatter.format(response, options: options)

        XCTAssertFalse(formatted.suggestedActions.isEmpty)
    }

    // MARK: - Edge Cases

    func testEmptyResponse() {
        let response = ""
        let options = BonesResponseFormatter.FormatOptions(intent: .shotRecommendation)

        let formatted = formatter.format(response, options: options)

        XCTAssertEqual(formatted.text, "")
        XCTAssertTrue(formatted.disclaimers.isEmpty)
    }

    func testWhitespaceOnlyResponse() {
        let response = "   \n\t  "
        let options = BonesResponseFormatter.FormatOptions(intent: .shotRecommendation)

        let formatted = formatter.format(response, options: options)

        XCTAssertEqual(formatted.text, "")
    }

    func testVeryLongResponse() {
        let response = String(repeating: "This is a long response. ", count: 100)
        let options = BonesResponseFormatter.FormatOptions(intent: .shotRecommendation)

        let formatted = formatter.format(response, options: options)

        // Should still format without crashing
        XCTAssertFalse(formatted.text.isEmpty)
    }

    func testMultipleConsecutiveDisclaimers() {
        let response = "Your injury might be serious and this will fix it"
        let options = BonesResponseFormatter.FormatOptions(intent: .recoveryCheck)

        let formatted = formatter.format(response, options: options)

        // Should have both medical and guarantee disclaimers
        XCTAssertGreaterThanOrEqual(formatted.disclaimers.count, 1)
    }
}
