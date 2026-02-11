import XCTest
@testable import App

/// Integration tests for the complete Intent Pipeline.
///
/// Tests the full flow: Input → Normalizer → Classifier → Result
///
/// Spec reference: navcaddy-engine.md R2, A1, A3
/// Task reference: navcaddy-engine-plan.md Task 9
///
/// Tests verify:
/// - A1: High-confidence routing with entity extraction
/// - A3: Low-confidence clarification with suggestions
/// - Full pipeline integration with normalization
/// - Threshold boundary behavior
/// - Edge cases and error handling
final class IntentPipelineIntegrationTests: XCTestCase {
    var classifier: IntentClassifier!
    var mockLLMClient: MockIntegrationLLMClient!

    override func setUp() {
        super.setUp()
        mockLLMClient = MockIntegrationLLMClient()
        classifier = IntentClassifier(llmClient: mockLLMClient)
    }

    override func tearDown() {
        classifier = nil
        mockLLMClient = nil
        super.tearDown()
    }

    // MARK: - A1: High Confidence Routing Tests

    func testHighConfidenceClubAdjustment_FullPipeline() async {
        // GIVEN: User says "My 7i feels long today"
        // This tests: normalization (7i → 7-iron) + classification + routing
        let input = "My 7i feels long today"

        mockLLMClient.setupResponse(
            confidence: 0.85,
            intent: .clubAdjustment,
            club: "7-iron"
        )

        // WHEN: Running through full pipeline
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should route to ClubAdjustmentScreen with normalized club
        switch result {
        case .route(let intent, let target):
            XCTAssertEqual(intent.intentType, .clubAdjustment)
            XCTAssertEqual(intent.confidence, 0.85)
            XCTAssertEqual(intent.entities.club?.name, "7-iron")
            XCTAssertEqual(target.module, .caddy)
            XCTAssertEqual(target.screen, "ClubAdjustmentScreen")

            // Verify normalizer was applied (input contains "7i" but classifier got "7-iron")
            XCTAssertEqual(mockLLMClient.lastClassificationInput?.contains("7-iron"), true)
        default:
            XCTFail("Expected route result, got \(result)")
        }
    }

    func testHighConfidenceRecoveryCheck_NoEntities() async {
        // GIVEN: User asks "How's my recovery looking?"
        let input = "How's my recovery looking?"

        mockLLMClient.setupResponse(
            confidence: 0.92,
            intent: .recoveryCheck
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should route to recovery module
        switch result {
        case .route(let intent, let target):
            XCTAssertEqual(intent.intentType, .recoveryCheck)
            XCTAssertEqual(intent.confidence, 0.92)
            XCTAssertEqual(target.module, .recovery)
            XCTAssertEqual(target.screen, "RecoveryOverviewScreen")
        default:
            XCTFail("Expected route result")
        }
    }

    func testHighConfidenceShotRecommendation_MultipleEntities() async {
        // GIVEN: Complex input with multiple entities
        let input = "What club from one fifty in the fairway with a breeze?"

        mockLLMClient.setupResponse(
            confidence: 0.88,
            intent: .shotRecommendation,
            yardage: 150,
            lie: "fairway",
            wind: "light breeze"
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should route with all entities extracted
        switch result {
        case .route(let intent, let target):
            XCTAssertEqual(intent.intentType, .shotRecommendation)
            XCTAssertEqual(intent.entities.yardage, 150)
            XCTAssertEqual(intent.entities.lie, .fairway)
            XCTAssertEqual(intent.entities.wind, "light breeze")
            XCTAssertEqual(target.module, .caddy)

            // Verify normalization: "one fifty" → "150"
            XCTAssertTrue(mockLLMClient.lastClassificationInput?.contains("150") ?? false)
        default:
            XCTFail("Expected route result")
        }
    }

    // MARK: - A3: Low Confidence Clarification Tests

    func testLowConfidence_RequestsClarification() async {
        // GIVEN: Ambiguous input "It feels off today"
        let input = "It feels off today"

        mockLLMClient.setupResponse(
            confidence: 0.35,
            intent: .helpRequest
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should return clarification with suggestions
        switch result {
        case .clarify(let response):
            XCTAssertFalse(response.message.isEmpty)
            XCTAssertFalse(response.suggestions.isEmpty)
            XCTAssertTrue(response.suggestions.count <= 3)
            XCTAssertEqual(response.originalInput, input)

            // Should suggest relevant intents
            let intentTypes = response.suggestions.map { $0.intentType }
            XCTAssertTrue(
                intentTypes.contains(.clubAdjustment) ||
                intentTypes.contains(.recoveryCheck) ||
                intentTypes.contains(.patternQuery)
            )
        default:
            XCTFail("Expected clarify result, got \(result)")
        }
    }

    func testLowConfidence_WithProfanity_StillClarifies() async {
        // GIVEN: Input with profanity that gets filtered
        let input = "damn it feels off"

        mockLLMClient.setupResponse(
            confidence: 0.30,
            intent: .helpRequest
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should clarify, and profanity should be filtered in classification input
        switch result {
        case .clarify(let response):
            XCTAssertFalse(response.suggestions.isEmpty)

            // Verify profanity was filtered before LLM classification
            XCTAssertTrue(mockLLMClient.lastClassificationInput?.contains("****") ?? false)
            XCTAssertFalse(mockLLMClient.lastClassificationInput?.contains("damn") ?? true)
        default:
            XCTFail("Expected clarify result")
        }
    }

    func testLowConfidence_EmptyInput_ReturnsError() async {
        // GIVEN: Empty input
        let input = ""

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should return clarification with default suggestions
        switch result {
        case .clarify(let response):
            XCTAssertFalse(response.suggestions.isEmpty)
        default:
            XCTFail("Expected clarify result for empty input")
        }
    }

    // MARK: - Medium Confidence Confirmation Tests

    func testMediumConfidence_AsksForConfirmation() async {
        // GIVEN: Input with medium confidence
        let input = "Check my recovery"

        mockLLMClient.setupResponse(
            confidence: 0.65,
            intent: .recoveryCheck
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should return confirmation request
        switch result {
        case .confirm(let intent, let message):
            XCTAssertEqual(intent.intentType, .recoveryCheck)
            XCTAssertEqual(intent.confidence, 0.65)
            XCTAssertFalse(message.isEmpty)
            XCTAssertTrue(message.contains("Did you want to"))
        default:
            XCTFail("Expected confirm result")
        }
    }

    func testMediumConfidenceWithMissingEntity_AsksForClarification() async {
        // GIVEN: Club adjustment with missing club entity
        let input = "Adjust my club"

        mockLLMClient.setupResponse(
            confidence: 0.80, // High confidence but missing required entity
            intent: .clubAdjustment
            // No club specified
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should confirm and ask for missing entity
        switch result {
        case .confirm(let intent, let message):
            XCTAssertEqual(intent.intentType, .clubAdjustment)
            XCTAssertTrue(message.contains("club"))
        default:
            XCTFail("Expected confirm result for missing entity")
        }
    }

    // MARK: - Threshold Boundary Tests

    func testThresholdBoundary_ExactlyRoute() async {
        // GIVEN: Confidence = 0.75 (route threshold)
        let input = "My 7-iron feels long"

        mockLLMClient.setupResponse(
            confidence: 0.75,
            intent: .clubAdjustment,
            club: "7-iron"
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should route
        switch result {
        case .route:
            // Success - routes at exactly 0.75
            break
        default:
            XCTFail("Expected route at confidence 0.75")
        }
    }

    func testThresholdBoundary_JustBelowRoute() async {
        // GIVEN: Confidence = 0.74 (just below route threshold)
        let input = "My 7-iron feels long"

        mockLLMClient.setupResponse(
            confidence: 0.74,
            intent: .clubAdjustment,
            club: "7-iron"
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should confirm
        switch result {
        case .confirm(let intent, _):
            XCTAssertEqual(intent.confidence, 0.74)
        default:
            XCTFail("Expected confirm at confidence 0.74")
        }
    }

    func testThresholdBoundary_ExactlyConfirm() async {
        // GIVEN: Confidence = 0.50 (lower bound of confirm threshold)
        let input = "Check my recovery"

        mockLLMClient.setupResponse(
            confidence: 0.50,
            intent: .recoveryCheck
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should confirm
        switch result {
        case .confirm(let intent, _):
            XCTAssertEqual(intent.confidence, 0.50)
        default:
            XCTFail("Expected confirm at confidence 0.50")
        }
    }

    func testThresholdBoundary_JustBelowConfirm() async {
        // GIVEN: Confidence = 0.49 (just below confirm threshold)
        let input = "It feels off"

        mockLLMClient.setupResponse(
            confidence: 0.49,
            intent: .helpRequest
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should clarify
        switch result {
        case .clarify:
            // Success - clarifies below 0.50
            break
        default:
            XCTFail("Expected clarify at confidence 0.49")
        }
    }

    // MARK: - Normalization Integration Tests

    func testNormalization_GolfSlang_BeforeClassification() async {
        // GIVEN: Input with multiple golf slang terms
        let input = "My pw to the dance floor"

        mockLLMClient.setupResponse(
            confidence: 0.85,
            intent: .clubAdjustment,
            club: "pitching wedge"
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should route, and normalizer should have expanded slang
        switch result {
        case .route(let intent, _):
            XCTAssertEqual(intent.intentType, .clubAdjustment)

            // Verify normalization: "pw" → "pitching wedge", "dance floor" → "green"
            let classifiedInput = mockLLMClient.lastClassificationInput ?? ""
            XCTAssertTrue(classifiedInput.contains("pitching wedge"))
            XCTAssertTrue(classifiedInput.contains("green"))
            XCTAssertFalse(classifiedInput.contains("pw"))
            XCTAssertFalse(classifiedInput.contains("dance floor"))
        default:
            XCTFail("Expected route result")
        }
    }

    func testNormalization_SpokenNumbers_ToDigits() async {
        // GIVEN: Input with spoken numbers
        let input = "seven iron from one fifty"

        mockLLMClient.setupResponse(
            confidence: 0.88,
            intent: .shotRecommendation,
            club: "7-iron",
            yardage: 150
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should route with normalized numbers
        switch result {
        case .route(let intent, _):
            XCTAssertEqual(intent.entities.yardage, 150)

            // Verify normalization: "seven iron" → "7-iron", "one fifty" → "150"
            let classifiedInput = mockLLMClient.lastClassificationInput ?? ""
            XCTAssertTrue(classifiedInput.contains("7-iron"))
            XCTAssertTrue(classifiedInput.contains("150"))
        default:
            XCTFail("Expected route result")
        }
    }

    func testNormalization_MultipleTransformations() async {
        // GIVEN: Complex input requiring multiple normalizations
        let input = "  My 7i  from  one fifty feels  long today  "

        mockLLMClient.setupResponse(
            confidence: 0.85,
            intent: .clubAdjustment,
            club: "7-iron"
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should apply all normalizations: trim, club abbrev, numbers, spacing
        switch result {
        case .route:
            let classifiedInput = mockLLMClient.lastClassificationInput ?? ""

            // Verify all normalizations
            XCTAssertTrue(classifiedInput.contains("7-iron"))
            XCTAssertTrue(classifiedInput.contains("150"))
            XCTAssertFalse(classifiedInput.hasPrefix(" ")) // No leading space
            XCTAssertFalse(classifiedInput.hasSuffix(" ")) // No trailing space
            XCTAssertFalse(classifiedInput.contains("  ")) // No double spaces
        default:
            XCTFail("Expected route result")
        }
    }

    func testNormalization_Profanity_FilteredBeforeClassification() async {
        // GIVEN: Input with profanity
        let input = "My damn 7i is long today"

        mockLLMClient.setupResponse(
            confidence: 0.85,
            intent: .clubAdjustment,
            club: "7-iron"
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Profanity filtered, intent still detected
        switch result {
        case .route(let intent, _):
            XCTAssertEqual(intent.intentType, .clubAdjustment)

            // Verify profanity was filtered
            let classifiedInput = mockLLMClient.lastClassificationInput ?? ""
            XCTAssertTrue(classifiedInput.contains("****"))
            XCTAssertFalse(classifiedInput.contains("damn"))

            // But intent still detected correctly
            XCTAssertEqual(intent.entities.club?.name, "7-iron")
        default:
            XCTFail("Expected route result")
        }
    }

    // MARK: - Edge Case Tests

    func testEdgeCase_BlankInput_ReturnsClarification() async {
        // GIVEN: Blank input
        let input = "   \n\t  "

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should return clarification
        switch result {
        case .clarify(let response):
            XCTAssertFalse(response.suggestions.isEmpty)
        default:
            XCTFail("Expected clarify result for blank input")
        }
    }

    func testEdgeCase_VeryLongInput_HandlesGracefully() async {
        // GIVEN: Very long input
        let input = String(repeating: "My 7-iron feels long today. ", count: 50)

        mockLLMClient.setupResponse(
            confidence: 0.85,
            intent: .clubAdjustment,
            club: "7-iron"
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should handle gracefully
        switch result {
        case .route(let intent, _):
            XCTAssertEqual(intent.intentType, .clubAdjustment)
        default:
            XCTFail("Expected route result")
        }
    }

    func testEdgeCase_SpecialCharacters_HandlesGracefully() async {
        // GIVEN: Input with special characters
        let input = "My 7-iron!!! feels... long???"

        mockLLMClient.setupResponse(
            confidence: 0.85,
            intent: .clubAdjustment,
            club: "7-iron"
        )

        // WHEN: Classifying
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should normalize punctuation and classify
        switch result {
        case .route(let intent, _):
            XCTAssertEqual(intent.intentType, .clubAdjustment)

            // Verify punctuation normalized
            let classifiedInput = mockLLMClient.lastClassificationInput ?? ""
            XCTAssertFalse(classifiedInput.contains("!!!"))
            XCTAssertFalse(classifiedInput.contains("..."))
            XCTAssertFalse(classifiedInput.contains("???"))
        default:
            XCTFail("Expected route result")
        }
    }

    // MARK: - Error Handling Tests

    func testError_NetworkFailure_ReturnsError() async {
        // GIVEN: LLM client that throws network error
        mockLLMClient.shouldThrowError = .networkError("Connection failed")

        // WHEN: Classifying
        let result = await classifier.classify(input: "test", context: nil)

        // THEN: Should return error result
        switch result {
        case .error(let error):
            if case LLMError.networkError(let message) = error {
                XCTAssertEqual(message, "Connection failed")
            } else {
                XCTFail("Expected network error")
            }
        default:
            XCTFail("Expected error result")
        }
    }

    func testError_Timeout_ReturnsError() async {
        // GIVEN: LLM client that times out
        mockLLMClient.shouldThrowError = .timeout

        // WHEN: Classifying
        let result = await classifier.classify(input: "test", context: nil)

        // THEN: Should return timeout error
        switch result {
        case .error(let error):
            if case LLMError.timeout = error {
                // Success
            } else {
                XCTFail("Expected timeout error")
            }
        default:
            XCTFail("Expected error result")
        }
    }

    func testError_InvalidJSON_FallsBackToClarification() async {
        // GIVEN: LLM returns invalid JSON
        mockLLMClient.mockInvalidJSON = true

        // WHEN: Classifying
        let result = await classifier.classify(input: "test", context: nil)

        // THEN: Should fall back to clarification
        switch result {
        case .clarify:
            // Expected fallback
            break
        default:
            XCTFail("Expected clarify result for invalid JSON")
        }
    }

    // MARK: - Context-Aware Tests

    func testContext_ActiveRound_InfluencesSuggestions() async {
        // GIVEN: Low confidence with active round context
        let input = "what now?"
        let context = SessionContext(
            currentRound: Round(id: "test", courseName: "Test Course", startTime: Date()),
            currentHole: 5
        )

        mockLLMClient.setupResponse(
            confidence: 0.30,
            intent: .helpRequest
        )

        // WHEN: Classifying with context
        let result = await classifier.classify(input: input, context: context)

        // THEN: Should provide round-related suggestions
        switch result {
        case .clarify(let response):
            let intentTypes = response.suggestions.map { $0.intentType }

            // Should suggest round-related intents
            XCTAssertTrue(
                intentTypes.contains(.shotRecommendation) ||
                intentTypes.contains(.scoreEntry) ||
                intentTypes.contains(.patternQuery)
            )
        default:
            XCTFail("Expected clarify result")
        }
    }

    // MARK: - Real-World Scenario Tests

    func testRealWorldScenario_ClubFeelsLong() async {
        // GIVEN: Real user input from spec example
        let input = "My 7i feels long today"

        mockLLMClient.setupResponse(
            confidence: 0.85,
            intent: .clubAdjustment,
            club: "7-iron"
        )

        // WHEN: Processing through pipeline
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should route to ClubAdjustmentScreen with club=7-iron
        switch result {
        case .route(let intent, let target):
            XCTAssertEqual(intent.intentType, .clubAdjustment)
            XCTAssertEqual(intent.entities.club?.name, "7-iron")
            XCTAssertEqual(target.screen, "ClubAdjustmentScreen")
            XCTAssertGreaterThanOrEqual(intent.confidence, 0.75)
        default:
            XCTFail("Expected route result")
        }
    }

    func testRealWorldScenario_AmbiguousFeelsOff() async {
        // GIVEN: Real ambiguous input from spec
        let input = "It feels off today"

        mockLLMClient.setupResponse(
            confidence: 0.35,
            intent: .helpRequest
        )

        // WHEN: Processing through pipeline
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should clarify with 3 suggestions
        switch result {
        case .clarify(let response):
            XCTAssertFalse(response.message.isEmpty)
            XCTAssertTrue(response.suggestions.count <= 3)
            XCTAssertTrue(response.suggestions.count >= 1)

            // Should suggest relevant intents
            let intentTypes = response.suggestions.map { $0.intentType }
            XCTAssertTrue(
                intentTypes.contains(.clubAdjustment) ||
                intentTypes.contains(.recoveryCheck) ||
                intentTypes.contains(.patternQuery)
            )
        default:
            XCTFail("Expected clarify result")
        }
    }

    func testRealWorldScenario_MediumConfidenceNeedsConfirmation() async {
        // GIVEN: Input with medium confidence
        let input = "Check my recovery"

        mockLLMClient.setupResponse(
            confidence: 0.65,
            intent: .recoveryCheck
        )

        // WHEN: Processing through pipeline
        let result = await classifier.classify(input: input, context: nil)

        // THEN: Should ask for confirmation before routing
        switch result {
        case .confirm(let intent, let message):
            XCTAssertEqual(intent.intentType, .recoveryCheck)
            XCTAssertGreaterThanOrEqual(intent.confidence, 0.50)
            XCTAssertLessThan(intent.confidence, 0.75)
            XCTAssertTrue(message.contains("Did you want to"))
        default:
            XCTFail("Expected confirm result")
        }
    }
}

// MARK: - Mock LLM Client for Integration Tests

/// Mock LLM client that returns predictable responses based on setup.
class MockIntegrationLLMClient: LLMClient {
    var lastClassificationInput: String?
    var lastContext: SessionContext?

    // Configuration
    var shouldThrowError: LLMError?
    var mockInvalidJSON = false

    // Response configuration
    private var responseConfidence: Double = 0.85
    private var responseIntent: IntentType = .helpRequest
    private var responseClub: String?
    private var responseYardage: Int?
    private var responseLie: String?
    private var responseWind: String?

    func setupResponse(
        confidence: Double,
        intent: IntentType,
        club: String? = nil,
        yardage: Int? = nil,
        lie: String? = nil,
        wind: String? = nil
    ) {
        self.responseConfidence = confidence
        self.responseIntent = intent
        self.responseClub = club
        self.responseYardage = yardage
        self.responseLie = lie
        self.responseWind = wind
    }

    func classify(input: String, context: SessionContext?) async throws -> LLMResponse {
        lastClassificationInput = input
        lastContext = context

        if let error = shouldThrowError {
            throw error
        }

        if mockInvalidJSON {
            return LLMResponse(
                rawResponse: "{ invalid json }",
                latencyMs: 100,
                model: "mock-model"
            )
        }

        // Build JSON response
        let json = """
        {
          "intent": "\(responseIntent.rawValue)",
          "confidence": \(responseConfidence),
          "entities": {
            "club": \(responseClub.map { "\"\($0)\"" } ?? "null"),
            "yardage": \(responseYardage.map { "\($0)" } ?? "null"),
            "lie": \(responseLie.map { "\"\($0)\"" } ?? "null"),
            "wind": \(responseWind.map { "\"\($0)\"" } ?? "null"),
            "fatigue": null,
            "pain": null,
            "scoreContext": null,
            "holeNumber": null
          },
          "userGoal": "Mock user goal"
        }
        """

        return LLMResponse(
            rawResponse: json,
            latencyMs: 100,
            model: "mock-model"
        )
    }
}
