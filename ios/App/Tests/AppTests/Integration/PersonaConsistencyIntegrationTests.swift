import XCTest
@testable import App

/// Integration tests for Bones persona consistency across response types.
///
/// Tests interaction between PersonaGuardrails and BonesResponseFormatter.
///
/// Spec reference: navcaddy-engine.md R4, A5
/// Task reference: navcaddy-engine-plan.md Task 17
///
/// Tests verify:
/// - Forbidden phrase detection across all response types
/// - Disclaimers added consistently for medical content
/// - Response formatting maintains Bones voice
/// - Interaction between PersonaGuardrails and BonesResponseFormatter
@MainActor
final class PersonaConsistencyIntegrationTests: XCTestCase {
    var formatter: BonesResponseFormatter!

    override func setUp() {
        super.setUp()
        formatter = BonesResponseFormatter()
    }

    override func tearDown() {
        formatter = nil
        super.tearDown()
    }

    // MARK: - Forbidden Phrase Detection Across Response Types

    func testForbiddenPhrasesDetected_InClubRecommendation() {
        // GIVEN: Club recommendation with guarantee language
        let response = "Use your 7-iron here. This will fix your miss pattern completely."

        let options = BonesResponseFormatter.FormatOptions(
            intent: .clubAdjustment
        )

        // WHEN: Formatting response
        let formatted = formatter.format(response, options: options)

        // THEN: Should sanitize guarantee and add disclaimer
        XCTAssertFalse(formatted.text.contains("will fix"),
                      "Guarantee language should be sanitized")
        XCTAssertTrue(formatted.text.contains("should help"),
                     "Should replace with softer language")
        XCTAssertFalse(formatted.disclaimers.isEmpty,
                      "Should add swing guarantee disclaimer")
    }

    func testForbiddenPhrasesDetected_InRecoveryAdvice() {
        // GIVEN: Recovery advice with medical diagnosis
        let response = "Based on your symptoms, you have tendinitis. Keep playing through the pain."

        let options = BonesResponseFormatter.FormatOptions(
            intent: .recoveryCheck
        )

        // WHEN: Formatting response
        let formatted = formatter.format(response, options: options)

        // THEN: Should sanitize unsafe directive and add disclaimers
        XCTAssertFalse(formatted.text.lowercased().contains("keep playing through the pain"),
                      "Unsafe directive should be removed")
        XCTAssertTrue(formatted.disclaimers.count >= 1,
                     "Should have medical disclaimer")

        let hasMedicalDisclaimer = formatted.disclaimers.contains { disclaimer in
            disclaimer.contains("healthcare professional")
        }
        XCTAssertTrue(hasMedicalDisclaimer, "Should include medical disclaimer")
    }

    func testForbiddenPhrasesDetected_InDrillSuggestion() {
        // GIVEN: Drill suggestion with absolute guarantee
        let response = "This drill is guaranteed to eliminate your slice. It works 100% of the time."

        let options = BonesResponseFormatter.FormatOptions(
            intent: .drillRequest
        )

        // WHEN: Formatting response
        let formatted = formatter.format(response, options: options)

        // THEN: Should sanitize both guarantees
        XCTAssertFalse(formatted.text.contains("guaranteed to eliminate"),
                      "Guarantee should be sanitized")
        XCTAssertFalse(formatted.text.contains("100%"),
                      "Absolute claim should be softened")
        XCTAssertTrue(formatted.text.contains("likely to") || formatted.text.contains("help reduce"),
                     "Should use softer language")
    }

    func testBettingAdviceRejected_InAnyContext() {
        // GIVEN: Response with betting advice
        let response = "You should bet on making this putt. The odds are in your favor."

        let options = BonesResponseFormatter.FormatOptions(
            intent: .shotRecommendation
        )

        // WHEN: Formatting response
        let formatted = formatter.format(response, options: options)

        // THEN: Should include betting disclaimer
        let hasBettingDisclaimer = formatted.disclaimers.contains { disclaimer in
            disclaimer.contains("gambling") || disclaimer.contains("betting")
        }
        XCTAssertTrue(hasBettingDisclaimer, "Should add betting disclaimer")
    }

    func testMultipleForbiddenPatterns_AllDetected() {
        // GIVEN: Response with multiple violations
        let response = """
        Your injury is probably just inflammation. This drill will fix it guaranteed.
        Push through the pain and you'll be fine.
        """

        let options = BonesResponseFormatter.FormatOptions(
            intent: .recoveryCheck
        )

        // WHEN: Formatting response
        let formatted = formatter.format(response, options: options)

        // THEN: Should detect and handle multiple patterns
        XCTAssertFalse(formatted.text.contains("will fix"),
                      "Guarantee should be sanitized")
        XCTAssertFalse(formatted.text.lowercased().contains("push through the pain"),
                      "Unsafe directive should be removed")
        XCTAssertGreaterThanOrEqual(formatted.disclaimers.count, 1,
                                   "Should have multiple disclaimers")

        // Should have medical-related disclaimer
        let hasHealthDisclaimer = formatted.disclaimers.contains { disclaimer in
            disclaimer.contains("healthcare professional") || disclaimer.contains("injury")
        }
        XCTAssertTrue(hasHealthDisclaimer, "Should include health-related disclaimer")
    }

    // MARK: - Medical Disclaimer Consistency Tests

    func testMedicalDisclaimer_AddedForPainMention() {
        // GIVEN: Response mentioning pain without diagnosis
        let response = "If you're feeling pain in your elbow, take a break."

        let options = BonesResponseFormatter.FormatOptions(
            intent: .recoveryCheck
        )

        // WHEN: Formatting response
        let formatted = formatter.format(response, options: options)

        // THEN: Should add medical disclaimer
        let hasMedicalDisclaimer = formatted.disclaimers.contains { disclaimer in
            disclaimer.contains("healthcare professional")
        }
        XCTAssertTrue(hasMedicalDisclaimer, "Should add medical disclaimer for pain mention")
    }

    func testMedicalDisclaimer_AddedWhenForced() {
        // GIVEN: Clean response but force disclaimer flag set (user mentioned pain in input)
        let response = "Consider using a grip that's more comfortable for you."

        let options = BonesResponseFormatter.FormatOptions(
            intent: .shotRecommendation,
            forceDisclaimer: true
        )

        // WHEN: Formatting response
        let formatted = formatter.format(response, options: options)

        // THEN: Should add medical disclaimer even for clean response
        let hasMedicalDisclaimer = formatted.disclaimers.contains { disclaimer in
            disclaimer.contains("healthcare professional")
        }
        XCTAssertTrue(hasMedicalDisclaimer, "Should force medical disclaimer when requested")
    }

    func testMedicalDisclaimer_NotAddedForCleanContent() {
        // GIVEN: Clean response with no medical content
        let response = "Consider using your 8-iron for this shot based on the wind."

        let options = BonesResponseFormatter.FormatOptions(
            intent: .shotRecommendation
        )

        // WHEN: Formatting response
        let formatted = formatter.format(response, options: options)

        // THEN: Should not add medical disclaimer
        XCTAssertTrue(formatted.disclaimers.isEmpty,
                     "Should not add disclaimers for clean content")
    }

    func testMedicalDisclaimer_ConsistentAcrossModules() {
        // GIVEN: Medical content in different module contexts
        let medicalResponse = "If you're experiencing wrist pain, consider modifying your grip."

        let caddyOptions = BonesResponseFormatter.FormatOptions(
            module: .caddy,
            intent: .shotRecommendation
        )

        let coachOptions = BonesResponseFormatter.FormatOptions(
            module: .coach,
            intent: .drillRequest
        )

        let recoveryOptions = BonesResponseFormatter.FormatOptions(
            module: .recovery,
            intent: .recoveryCheck
        )

        // WHEN: Formatting in different modules
        let caddyFormatted = formatter.format(medicalResponse, options: caddyOptions)
        let coachFormatted = formatter.format(medicalResponse, options: coachOptions)
        let recoveryFormatted = formatter.format(medicalResponse, options: recoveryOptions)

        // THEN: All should have medical disclaimer
        XCTAssertFalse(caddyFormatted.disclaimers.isEmpty, "Caddy module should have disclaimer")
        XCTAssertFalse(coachFormatted.disclaimers.isEmpty, "Coach module should have disclaimer")
        XCTAssertFalse(recoveryFormatted.disclaimers.isEmpty, "Recovery module should have disclaimer")

        // All should have same medical disclaimer
        let caddyHasMedical = caddyFormatted.disclaimers.contains { $0.contains("healthcare professional") }
        let coachHasMedical = coachFormatted.disclaimers.contains { $0.contains("healthcare professional") }
        let recoveryHasMedical = recoveryFormatted.disclaimers.contains { $0.contains("healthcare professional") }

        XCTAssertTrue(caddyHasMedical && coachHasMedical && recoveryHasMedical,
                     "All modules should use consistent medical disclaimer")
    }

    // MARK: - Voice Consistency Tests

    func testBonesVoiceMaintained_AfterSanitization() {
        // GIVEN: Response with both Bones voice and forbidden content
        let response = "Let's go with the 7-iron here. This will definitely fix your slice."

        let options = BonesResponseFormatter.FormatOptions(
            intent: .clubAdjustment
        )

        // WHEN: Formatting response
        let formatted = formatter.format(response, options: options)

        // THEN: Should maintain Bones voice while sanitizing guarantee
        XCTAssertTrue(formatted.text.contains("Let's go with the 7-iron"),
                     "Should preserve Bones caddy language")
        XCTAssertFalse(formatted.text.contains("definitely fix"),
                      "Should sanitize guarantee")
        XCTAssertTrue(formatted.text.contains("should help") || formatted.text.contains("likely to"),
                     "Should replace with softer language")
    }

    func testGenericAIPhrases_RemovedConsistently() {
        // GIVEN: Response with generic AI phrases
        let response = """
        As an AI assistant, I must tell you that it's important to note that
        you should use your 9-iron here. In conclusion, this is the best choice.
        """

        let options = BonesResponseFormatter.FormatOptions(
            intent: .shotRecommendation
        )

        // WHEN: Formatting response
        let formatted = formatter.format(response, options: options)

        // THEN: Should remove all generic AI phrases
        XCTAssertFalse(formatted.text.contains("As an AI"),
                      "Should remove 'As an AI'")
        XCTAssertFalse(formatted.text.contains("I must"),
                      "Should remove 'I must'")
        XCTAssertFalse(formatted.text.contains("important to note"),
                      "Should remove 'important to note'")
        XCTAssertFalse(formatted.text.contains("In conclusion"),
                      "Should remove 'In conclusion'")
    }

    func testFormalLanguage_ReplacedWithNaturalLanguage() {
        // GIVEN: Response with formal language
        let response = "It is recommended that you utilize your 8-iron in order to achieve approximately 150 yards."

        let options = BonesResponseFormatter.FormatOptions(
            intent: .shotRecommendation
        )

        // WHEN: Formatting response
        let formatted = formatter.format(response, options: options)

        // THEN: Should use natural language
        XCTAssertFalse(formatted.text.contains("utilize"),
                      "Should replace 'utilize' with 'use'")
        XCTAssertFalse(formatted.text.contains("approximately"),
                      "Should replace 'approximately' with 'about'")
        XCTAssertFalse(formatted.text.contains("in order to"),
                      "Should replace 'in order to' with 'to'")
        XCTAssertFalse(formatted.text.contains("It is recommended that you"),
                      "Should replace formal phrasing")
    }

    func testPatternReferences_MaintainBonesVoice() {
        // GIVEN: Response with pattern context
        let response = "Consider the 7-iron for this approach."

        let patterns = [
            MissPattern(
                direction: .push,
                club: Club(id: "7iron", name: "7-iron", type: .iron),
                frequency: 5,
                confidence: 0.7,
                lastOccurrence: Date()
            )
        ]

        let options = BonesResponseFormatter.FormatOptions(
            intent: .shotRecommendation,
            patterns: patterns
        )

        // WHEN: Formatting with patterns
        let formatted = formatter.format(response, options: options)

        // THEN: Pattern references should use natural caddy language
        XCTAssertTrue(formatted.text.contains("tend to") || formatted.text.contains("push"),
                     "Should reference pattern naturally")
        XCTAssertTrue(formatted.referencedPatterns.count > 0,
                     "Should include pattern references")
    }

    // MARK: - Response Validation Integration Tests

    func testResponseValidation_BeforeFormatting() {
        // GIVEN: Responses with varying safety levels
        let safeResponse = "Use the 8-iron for this shot."
        let unsafeResponse = "Ignore the pain and keep playing."
        let bettingResponse = "Bet on making this putt."

        // WHEN: Validating responses
        let safeValid = formatter.validateResponse(safeResponse)
        let unsafeValid = formatter.validateResponse(unsafeResponse)
        let bettingValid = formatter.validateResponse(bettingResponse)

        // THEN: Validation should catch unsafe content
        XCTAssertTrue(safeValid, "Safe response should validate")
        XCTAssertFalse(unsafeValid, "Unsafe directive should fail validation")
        XCTAssertFalse(bettingValid, "Betting advice should fail validation")
    }

    func testSafetyContext_PreparedForUserInput() {
        // GIVEN: User inputs with sensitive topics
        let painInput = "My elbow hurts when I swing"
        let cleanInput = "What club should I use?"

        // WHEN: Preparing safety context
        let painContext = formatter.prepareSafetyContext(for: painInput)
        let cleanContext = formatter.prepareSafetyContext(for: cleanInput)

        // THEN: Should generate appropriate safety reminders
        XCTAssertNotNil(painContext, "Should generate safety context for pain mention")
        XCTAssertTrue(painContext?.contains("medical") ?? false,
                     "Safety context should mention medical caution")
        XCTAssertNil(cleanContext, "Should not generate context for clean input")
    }

    func testFullPipeline_UserInputToFormattedResponse() {
        // GIVEN: User mentions pain
        let userInput = "My wrist hurts when I grip the club"

        // Prepare safety context
        let safetyContext = formatter.prepareSafetyContext(for: userInput)
        XCTAssertNotNil(safetyContext, "Should detect pain mention")

        // Simulate LLM response (with injected safety context)
        let llmResponse = """
        Consider using a lighter grip pressure. If the pain persists,
        you should modify your grip style or consult a professional.
        """

        let options = BonesResponseFormatter.FormatOptions(
            intent: .shotRecommendation,
            forceDisclaimer: true // Because user mentioned pain
        )

        // WHEN: Validating and formatting
        let isValid = formatter.validateResponse(llmResponse)
        let formatted = formatter.format(llmResponse, options: options)

        // THEN: Full pipeline should work correctly
        XCTAssertTrue(isValid, "Response should pass validation")
        XCTAssertFalse(formatted.disclaimers.isEmpty,
                      "Should include medical disclaimer")
        XCTAssertTrue(formatted.text.contains("grip"),
                     "Should preserve helpful content")

        let hasMedicalDisclaimer = formatted.disclaimers.contains { disclaimer in
            disclaimer.contains("healthcare professional")
        }
        XCTAssertTrue(hasMedicalDisclaimer,
                     "Should add medical disclaimer for pain-related query")
    }

    func testConsistency_AcrossMultipleFormattingCalls() {
        // GIVEN: Same response formatted multiple times
        let response = "This swing adjustment will fix your slice."

        let options = BonesResponseFormatter.FormatOptions(
            intent: .drillRequest
        )

        // WHEN: Formatting multiple times
        let formatted1 = formatter.format(response, options: options)
        let formatted2 = formatter.format(response, options: options)
        let formatted3 = formatter.format(response, options: options)

        // THEN: Should produce consistent results
        XCTAssertEqual(formatted1.text, formatted2.text,
                      "Should produce consistent text")
        XCTAssertEqual(formatted2.text, formatted3.text,
                      "Should produce consistent text across calls")
        XCTAssertEqual(formatted1.disclaimers.count, formatted2.disclaimers.count,
                      "Should produce consistent disclaimers")
    }

    func testSuggestedActions_ConsistentWithIntent() {
        // GIVEN: Different intents
        let response = "Use the 7-iron for this shot."

        let shotOptions = BonesResponseFormatter.FormatOptions(
            intent: .shotRecommendation
        )

        let patternOptions = BonesResponseFormatter.FormatOptions(
            intent: .patternQuery
        )

        let statsOptions = BonesResponseFormatter.FormatOptions(
            intent: .statsLookup
        )

        // WHEN: Formatting with different intents
        let shotFormatted = formatter.format(response, options: shotOptions)
        let patternFormatted = formatter.format(response, options: patternOptions)
        let statsFormatted = formatter.format(response, options: statsOptions)

        // THEN: Suggested actions should be intent-appropriate
        XCTAssertFalse(shotFormatted.suggestedActions.isEmpty,
                      "Should suggest actions for shot recommendation")
        XCTAssertFalse(patternFormatted.suggestedActions.isEmpty,
                      "Should suggest actions for pattern query")
        XCTAssertFalse(statsFormatted.suggestedActions.isEmpty,
                      "Should suggest actions for stats lookup")

        // Actions should be different and contextual
        XCTAssertNotEqual(shotFormatted.suggestedActions, patternFormatted.suggestedActions,
                         "Actions should differ by intent")
    }
}
