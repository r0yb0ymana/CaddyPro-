import XCTest
@testable import App

final class IntentClassifierTests: XCTestCase {
    var classifier: IntentClassifier!
    var mockClient: MockLLMClient!

    override func setUp() {
        super.setUp()
        mockClient = MockLLMClient()
        classifier = IntentClassifier(llmClient: mockClient)
    }

    override func tearDown() {
        classifier = nil
        mockClient = nil
        super.tearDown()
    }

    // MARK: - High Confidence (Route) Tests

    func testHighConfidenceClubAdjustment_Routes() async {
        // Given: High confidence response for club adjustment
        mockClient.mockResponse = """
        {
          "intent": "club_adjustment",
          "confidence": 0.85,
          "entities": {
            "club": "7-iron",
            "yardage": null,
            "lie": null,
            "wind": null,
            "fatigue": null,
            "pain": null,
            "scoreContext": null,
            "holeNumber": null
          },
          "userGoal": "User wants to adjust 7-iron distance"
        }
        """

        // When: Classifying the input
        let result = await classifier.classify(input: "My 7-iron feels long today", context: nil)

        // Then: Should route directly
        switch result {
        case .route(let intent, let target):
            XCTAssertEqual(intent.intentType, .clubAdjustment)
            XCTAssertEqual(intent.confidence, 0.85)
            XCTAssertEqual(intent.entities.club?.name, "7-iron")
            XCTAssertEqual(target.module, .caddy)
            XCTAssertEqual(target.screen, "ClubAdjustmentScreen")
        default:
            XCTFail("Expected route result, got \(result)")
        }
    }

    func testHighConfidenceRecoveryCheck_Routes() async {
        // Given: High confidence response for recovery check
        mockClient.mockResponse = """
        {
          "intent": "recovery_check",
          "confidence": 0.92,
          "entities": {
            "club": null,
            "yardage": null,
            "lie": null,
            "wind": null,
            "fatigue": null,
            "pain": null,
            "scoreContext": null,
            "holeNumber": null
          },
          "userGoal": "Check recovery status"
        }
        """

        // When: Classifying the input
        let result = await classifier.classify(input: "How's my recovery looking?", context: nil)

        // Then: Should route to recovery module
        switch result {
        case .route(let intent, let target):
            XCTAssertEqual(intent.intentType, .recoveryCheck)
            XCTAssertEqual(target.module, .recovery)
        default:
            XCTFail("Expected route result")
        }
    }

    func testHighConfidenceShotRecommendation_WithYardage_Routes() async {
        // Given: High confidence with yardage entity
        mockClient.mockResponse = """
        {
          "intent": "shot_recommendation",
          "confidence": 0.88,
          "entities": {
            "club": null,
            "yardage": 150,
            "lie": "fairway",
            "wind": "light breeze",
            "fatigue": null,
            "pain": null,
            "scoreContext": null,
            "holeNumber": null
          },
          "userGoal": "Get shot advice for 150 yards"
        }
        """

        // When: Classifying the input
        let result = await classifier.classify(input: "What club for 150 from the fairway?", context: nil)

        // Then: Should route with all entities extracted
        switch result {
        case .route(let intent, _):
            XCTAssertEqual(intent.intentType, .shotRecommendation)
            XCTAssertEqual(intent.entities.yardage, 150)
            XCTAssertEqual(intent.entities.lie, .fairway)
            XCTAssertEqual(intent.entities.wind, "light breeze")
        default:
            XCTFail("Expected route result")
        }
    }

    // MARK: - Medium Confidence (Confirm) Tests

    func testMediumConfidence_AsksForConfirmation() async {
        // Given: Medium confidence response
        mockClient.mockResponse = """
        {
          "intent": "pattern_query",
          "confidence": 0.65,
          "entities": {
            "club": "driver",
            "yardage": null,
            "lie": null,
            "wind": null,
            "fatigue": null,
            "pain": null,
            "scoreContext": null,
            "holeNumber": null
          },
          "userGoal": "Ask about miss pattern"
        }
        """

        // When: Classifying the input
        let result = await classifier.classify(input: "It feels off today", context: nil)

        // Then: Should return confirmation request
        switch result {
        case .confirm(let intent, let message):
            XCTAssertEqual(intent.intentType, .patternQuery)
            XCTAssertEqual(intent.confidence, 0.65)
            XCTAssertFalse(message.isEmpty)
            XCTAssertTrue(message.contains("Did you want to"))
        default:
            XCTFail("Expected confirm result")
        }
    }

    func testMediumConfidence_ScoreEntry_Confirmation() async {
        // Given: Medium confidence for score entry
        mockClient.mockResponse = """
        {
          "intent": "score_entry",
          "confidence": 0.58,
          "entities": {
            "club": null,
            "yardage": null,
            "lie": null,
            "wind": null,
            "fatigue": null,
            "pain": null,
            "scoreContext": null,
            "holeNumber": 5
          },
          "userGoal": "Enter score"
        }
        """

        // When: Classifying the input
        let result = await classifier.classify(input: "I got a 5", context: nil)

        // Then: Should confirm before routing
        switch result {
        case .confirm(let intent, _):
            XCTAssertEqual(intent.intentType, .scoreEntry)
            XCTAssertEqual(intent.entities.holeNumber, 5)
        default:
            XCTFail("Expected confirm result")
        }
    }

    // MARK: - Low Confidence (Clarify) Tests

    func testLowConfidence_RequestsClarification() async {
        // Given: Low confidence response
        mockClient.mockResponse = """
        {
          "intent": "help_request",
          "confidence": 0.35,
          "entities": {
            "club": null,
            "yardage": null,
            "lie": null,
            "wind": null,
            "fatigue": null,
            "pain": null,
            "scoreContext": null,
            "holeNumber": null
          },
          "userGoal": null
        }
        """

        // When: Classifying ambiguous input
        let result = await classifier.classify(input: "hmm", context: nil)

        // Then: Should request clarification with suggestions
        switch result {
        case .clarify(let suggestions, let message):
            XCTAssertFalse(suggestions.isEmpty)
            XCTAssertTrue(suggestions.count <= 3)
            XCTAssertFalse(message.isEmpty)
        default:
            XCTFail("Expected clarify result")
        }
    }

    func testLowConfidence_WithContext_ProvidesSuggestions() async {
        // Given: Low confidence with active round context
        mockClient.mockResponse = """
        {
          "intent": "help_request",
          "confidence": 0.20,
          "entities": {
            "club": null,
            "yardage": null,
            "lie": null,
            "wind": null,
            "fatigue": null,
            "pain": null,
            "scoreContext": null,
            "holeNumber": null
          },
          "userGoal": null
        }
        """

        let context = SessionContext(
            currentRound: Round(id: "test-round", courseName: "Test Course", startTime: Date()),
            currentHole: 5
        )

        // When: Classifying with context
        let result = await classifier.classify(input: "uh", context: context)

        // Then: Should provide context-appropriate suggestions
        switch result {
        case .clarify(let suggestions, _):
            // With active round, should suggest round-related intents
            XCTAssertTrue(suggestions.contains(.shotRecommendation) ||
                         suggestions.contains(.scoreEntry) ||
                         suggestions.contains(.patternQuery))
        default:
            XCTFail("Expected clarify result")
        }
    }

    // MARK: - Error Handling Tests

    func testNetworkError_ReturnsError() async {
        // Given: LLM client that throws network error
        mockClient.shouldThrowError = .networkError("Connection failed")

        // When: Classifying input
        let result = await classifier.classify(input: "test", context: nil)

        // Then: Should return error result
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

    func testTimeout_ReturnsError() async {
        // Given: LLM client that throws timeout
        mockClient.shouldThrowError = .timeout

        // When: Classifying input
        let result = await classifier.classify(input: "test", context: nil)

        // Then: Should return timeout error
        switch result {
        case .error(let error):
            XCTAssertTrue(error is LLMError)
            if case LLMError.timeout = error {
                // Success
            } else {
                XCTFail("Expected timeout error")
            }
        default:
            XCTFail("Expected error result")
        }
    }

    func testInvalidJSON_RequestsClarification() async {
        // Given: Invalid JSON response
        mockClient.mockResponse = "{ invalid json }"

        // When: Classifying input
        let result = await classifier.classify(input: "test", context: nil)

        // Then: Should fall back to clarification
        switch result {
        case .clarify:
            // Expected fallback behavior
            break
        default:
            XCTFail("Expected clarify result for invalid JSON")
        }
    }

    func testUnknownIntent_FallsBackToHelp() async {
        // Given: Response with unknown intent type
        mockClient.mockResponse = """
        {
          "intent": "unknown_intent_type",
          "confidence": 0.80,
          "entities": {
            "club": null,
            "yardage": null,
            "lie": null,
            "wind": null,
            "fatigue": null,
            "pain": null,
            "scoreContext": null,
            "holeNumber": null
          },
          "userGoal": null
        }
        """

        // When: Classifying input
        let result = await classifier.classify(input: "test", context: nil)

        // Then: Should fall back to help intent with low confidence
        switch result {
        case .clarify:
            // Falls back to clarification due to very low confidence
            break
        default:
            XCTFail("Expected clarify result for unknown intent")
        }
    }

    // MARK: - Entity Validation Tests

    func testMissingRequiredEntity_AsksForConfirmation() async {
        // Given: High confidence but missing required entity (club for club_adjustment)
        mockClient.mockResponse = """
        {
          "intent": "club_adjustment",
          "confidence": 0.85,
          "entities": {
            "club": null,
            "yardage": null,
            "lie": null,
            "wind": null,
            "fatigue": null,
            "pain": null,
            "scoreContext": null,
            "holeNumber": null
          },
          "userGoal": "Adjust club distance"
        }
        """

        // When: Classifying input
        let result = await classifier.classify(input: "Adjust my club", context: nil)

        // Then: Should ask for missing entity
        switch result {
        case .confirm(let intent, let message):
            XCTAssertEqual(intent.intentType, .clubAdjustment)
            XCTAssertTrue(message.contains("club"))
        default:
            XCTFail("Expected confirm result asking for club")
        }
    }

    // MARK: - Club Parsing Tests

    func testClubParsing_Driver() async {
        mockClient.mockResponse = """
        {
          "intent": "club_adjustment",
          "confidence": 0.90,
          "entities": {
            "club": "driver",
            "yardage": null,
            "lie": null,
            "wind": null,
            "fatigue": null,
            "pain": null,
            "scoreContext": null,
            "holeNumber": null
          },
          "userGoal": null
        }
        """

        let result = await classifier.classify(input: "Driver feels off", context: nil)

        if case .route(let intent, _) = result {
            XCTAssertEqual(intent.entities.club?.type, .driver)
            XCTAssertEqual(intent.entities.club?.name, "Driver")
        } else {
            XCTFail("Expected route result")
        }
    }

    func testClubParsing_IronWithNumber() async {
        mockClient.mockResponse = """
        {
          "intent": "pattern_query",
          "confidence": 0.88,
          "entities": {
            "club": "7-iron",
            "yardage": null,
            "lie": null,
            "wind": null,
            "fatigue": null,
            "pain": null,
            "scoreContext": null,
            "holeNumber": null
          },
          "userGoal": null
        }
        """

        let result = await classifier.classify(input: "What's my miss with 7-iron?", context: nil)

        if case .route(let intent, _) = result {
            XCTAssertEqual(intent.entities.club?.type, .iron)
            XCTAssertEqual(intent.entities.club?.name, "7-iron")
        } else {
            XCTFail("Expected route result")
        }
    }

    func testClubParsing_Wedges() async {
        let wedgeTests: [(input: String, expectedName: String)] = [
            ("pitching wedge", "Pitching Wedge"),
            ("PW", "Pitching Wedge"),
            ("gap wedge", "Gap Wedge"),
            ("sand wedge", "Sand Wedge"),
            ("lob wedge", "Lob Wedge")
        ]

        for test in wedgeTests {
            mockClient.mockResponse = """
            {
              "intent": "club_adjustment",
              "confidence": 0.85,
              "entities": {
                "club": "\(test.input)",
                "yardage": null,
                "lie": null,
                "wind": null,
                "fatigue": null,
                "pain": null,
                "scoreContext": null,
                "holeNumber": null
              },
              "userGoal": null
            }
            """

            let result = await classifier.classify(input: "test", context: nil)

            if case .route(let intent, _) = result {
                XCTAssertEqual(intent.entities.club?.type, .wedge)
                XCTAssertEqual(intent.entities.club?.name, test.expectedName)
            } else {
                XCTFail("Expected route result for \(test.input)")
            }
        }
    }

    // MARK: - Context Tests

    func testClassificationWithContext_IncludesHistory() async {
        // Given: Context with conversation history
        let turn1 = ConversationTurn(
            role: .user,
            content: "What club for 150?",
            timestamp: Date()
        )
        let turn2 = ConversationTurn(
            role: .assistant,
            content: "I'd suggest a 7-iron.",
            timestamp: Date()
        )
        let context = SessionContext(conversationHistory: [turn1, turn2])

        mockClient.mockResponse = """
        {
          "intent": "shot_recommendation",
          "confidence": 0.90,
          "entities": {
            "club": "6-iron",
            "yardage": null,
            "lie": null,
            "wind": "10mph headwind",
            "fatigue": null,
            "pain": null,
            "scoreContext": null,
            "holeNumber": null
          },
          "userGoal": "Adjust for wind"
        }
        """

        // When: Classifying follow-up with context
        let result = await classifier.classify(input: "What about if the wind picks up?", context: context)

        // Then: Should use context for better classification
        if case .route(let intent, _) = result {
            XCTAssertEqual(intent.intentType, .shotRecommendation)
            XCTAssertEqual(intent.entities.wind, "10mph headwind")
        } else {
            XCTFail("Expected route result")
        }
    }
}

// MARK: - Mock LLM Client

class MockLLMClient: LLMClient {
    var mockResponse: String = ""
    var shouldThrowError: LLMError?
    var lastInput: String?
    var lastContext: SessionContext?

    func classify(input: String, context: SessionContext?) async throws -> LLMResponse {
        lastInput = input
        lastContext = context

        if let error = shouldThrowError {
            throw error
        }

        return LLMResponse(
            rawResponse: mockResponse,
            latencyMs: 100,
            model: "mock-model"
        )
    }
}
