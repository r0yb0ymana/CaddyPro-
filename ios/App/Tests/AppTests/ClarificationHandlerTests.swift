import XCTest
@testable import App

/// Unit tests for ClarificationHandler.
///
/// Validates clarification generation for ambiguous user inputs.
/// Spec reference: navcaddy-engine.md A3, navcaddy-engine-plan.md Task 8
final class ClarificationHandlerTests: XCTestCase {
    var handler: ClarificationHandler!

    override func setUp() {
        super.setUp()
        handler = ClarificationHandler()
    }

    override func tearDown() {
        handler = nil
        super.tearDown()
    }

    // MARK: - Basic Clarification Generation Tests

    func testAmbiguousInput_GeneratesClarification() {
        // Given: Ambiguous input with no clear intent
        let input = "It feels off today"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: Should return valid clarification
        XCTAssertFalse(response.message.isEmpty)
        XCTAssertFalse(response.suggestions.isEmpty)
        XCTAssertEqual(response.originalInput, input)
    }

    func testVagueInput_GeneratesClarification() {
        // Given: Very vague input
        let input = "help"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: Should ask user to clarify
        XCTAssertFalse(response.message.isEmpty)
        XCTAssertTrue(response.suggestions.count <= 3)
        XCTAssertTrue(response.suggestions.count >= 1)
    }

    func testShortInput_GeneratesAppropriateMessage() {
        // Given: Very short ambiguous input
        let input = "hmm"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: Should ask for more information
        XCTAssertTrue(response.message.contains("not sure") ||
                     response.message.contains("tell me more"))
    }

    // MARK: - Suggestion Limit Tests

    func testSuggestions_MaxThree() {
        // Given: Any ambiguous input
        let input = "What should I do?"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: Should have at most 3 suggestions
        XCTAssertTrue(response.suggestions.count <= 3)
        XCTAssertTrue(response.suggestions.count >= 1)
    }

    func testSuggestions_HaveRequiredFields() {
        // Given: Ambiguous input
        let input = "I need help with my game"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: Each suggestion should have all required fields
        for suggestion in response.suggestions {
            XCTAssertFalse(suggestion.label.isEmpty)
            XCTAssertFalse(suggestion.description.isEmpty)
            XCTAssertNotNil(suggestion.intentType)
        }
    }

    // MARK: - Contextual Relevance Tests

    func testFeelWords_SuggestsRelevantIntents() {
        // Given: Input with "feel" keyword
        let input = "It feels off today"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: Should suggest relevant intents like club adjustment or pattern query
        let suggestedTypes = response.suggestions.map { $0.intentType }
        XCTAssertTrue(
            suggestedTypes.contains(.clubAdjustment) ||
            suggestedTypes.contains(.patternQuery) ||
            suggestedTypes.contains(.recoveryCheck)
        )
    }

    func testHelpKeyword_SuggestsAppropriateIntents() {
        // Given: Input requesting help with game
        let input = "Help me with my game"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: Should suggest coaching or practice-related intents
        let suggestedTypes = response.suggestions.map { $0.intentType }
        XCTAssertTrue(
            suggestedTypes.contains(.drillRequest) ||
            suggestedTypes.contains(.shotRecommendation) ||
            suggestedTypes.contains(.statsLookup)
        )
    }

    func testContextWithActiveRound_SuggestsRoundRelatedIntents() {
        // Given: Context with active round
        let context = SessionContext(
            currentRound: Round(id: "test-round", courseName: "Test Course", startTime: Date()),
            currentHole: 5
        )
        let input = "What should I do?"

        // When: Generating clarification with context
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: context
        )

        // Then: Should prioritize round-related intents
        let suggestedTypes = response.suggestions.map { $0.intentType }
        XCTAssertTrue(
            suggestedTypes.contains(.shotRecommendation) ||
            suggestedTypes.contains(.scoreEntry) ||
            suggestedTypes.contains(.patternQuery) ||
            suggestedTypes.contains(.weatherCheck)
        )
    }

    func testContextWithoutRound_SuggestsGeneralIntents() {
        // Given: Context without active round
        let context = SessionContext()
        let input = "What should I do?"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: context
        )

        // Then: Should suggest general intents like round start
        let suggestedTypes = response.suggestions.map { $0.intentType }
        XCTAssertTrue(
            suggestedTypes.contains(.roundStart) ||
            suggestedTypes.contains(.recoveryCheck) ||
            suggestedTypes.contains(.statsLookup)
        )
    }

    // MARK: - Keyword-Based Suggestion Tests

    func testClubKeywords_SuggestsClubRelatedIntents() {
        // Given: Input with club-related keywords
        let inputs = [
            "my 7-iron",
            "driver feels long",
            "club distance is off"
        ]

        for input in inputs {
            // When: Generating clarification
            let response = handler.generateClarification(
                for: input,
                parsedIntent: nil,
                context: nil
            )

            // Then: Should suggest club adjustment
            let suggestedTypes = response.suggestions.map { $0.intentType }
            XCTAssertTrue(
                suggestedTypes.contains(.clubAdjustment),
                "Failed for input: \(input)"
            )
        }
    }

    func testRecoveryKeywords_SuggestsRecoveryIntent() {
        // Given: Input with recovery-related keywords
        let inputs = [
            "I'm tired",
            "feeling sore",
            "am I ready to play?",
            "check my recovery"
        ]

        for input in inputs {
            // When: Generating clarification
            let response = handler.generateClarification(
                for: input,
                parsedIntent: nil,
                context: nil
            )

            // Then: Should suggest recovery check
            let suggestedTypes = response.suggestions.map { $0.intentType }
            XCTAssertTrue(
                suggestedTypes.contains(.recoveryCheck),
                "Failed for input: \(input)"
            )
        }
    }

    func testShotKeywords_SuggestsShotRecommendation() {
        // Given: Input with shot-related keywords
        let inputs = [
            "what's the play?",
            "what club should I hit?",
            "150 yards out"
        ]

        for input in inputs {
            // When: Generating clarification
            let response = handler.generateClarification(
                for: input,
                parsedIntent: nil,
                context: nil
            )

            // Then: Should suggest shot recommendation
            let suggestedTypes = response.suggestions.map { $0.intentType }
            XCTAssertTrue(
                suggestedTypes.contains(.shotRecommendation),
                "Failed for input: \(input)"
            )
        }
    }

    func testPatternKeywords_SuggestsPatternQuery() {
        // Given: Input with pattern-related keywords
        let inputs = [
            "my miss pattern",
            "do I slice?",
            "what's my tendency?"
        ]

        for input in inputs {
            // When: Generating clarification
            let response = handler.generateClarification(
                for: input,
                parsedIntent: nil,
                context: nil
            )

            // Then: Should suggest pattern query
            let suggestedTypes = response.suggestions.map { $0.intentType }
            XCTAssertTrue(
                suggestedTypes.contains(.patternQuery),
                "Failed for input: \(input)"
            )
        }
    }

    func testScoreKeywords_SuggestsScoreEntry() {
        // Given: Input with score-related keywords
        let inputs = [
            "I got a 5",
            "enter my score",
            "log a par"
        ]

        for input in inputs {
            // When: Generating clarification
            let response = handler.generateClarification(
                for: input,
                parsedIntent: nil,
                context: nil
            )

            // Then: Should suggest score entry
            let suggestedTypes = response.suggestions.map { $0.intentType }
            XCTAssertTrue(
                suggestedTypes.contains(.scoreEntry),
                "Failed for input: \(input)"
            )
        }
    }

    // MARK: - Message Generation Tests

    func testMessage_ContainsQuestionFormat() {
        // Given: Any ambiguous input
        let input = "something is wrong"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: Message should be a question or prompt
        XCTAssertTrue(
            response.message.contains("?") ||
            response.message.contains("Did you want to") ||
            response.message.contains("Could you clarify")
        )
    }

    func testMessage_IsUserFriendly() {
        // Given: Ambiguous input
        let input = "I need something"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: Message should be friendly and not technical
        let lowerMessage = response.message.lowercased()
        XCTAssertFalse(lowerMessage.contains("error"))
        XCTAssertFalse(lowerMessage.contains("failed"))
        XCTAssertFalse(lowerMessage.contains("invalid"))
    }

    // MARK: - Parsed Intent Boost Tests

    func testParsedIntent_BoostsRelevance() {
        // Given: Low confidence parsed intent
        let parsedIntent = ParsedIntent(
            intentType: .clubAdjustment,
            confidence: 0.35
        )
        let input = "something feels off"

        // When: Generating clarification with parsed intent
        let response = handler.generateClarification(
            for: input,
            parsedIntent: parsedIntent,
            context: nil
        )

        // Then: Should include the parsed intent as one of the suggestions
        let suggestedTypes = response.suggestions.map { $0.intentType }
        XCTAssertTrue(suggestedTypes.contains(.clubAdjustment))
    }

    // MARK: - Edge Cases

    func testEmptyInput_StillGeneratesSuggestions() {
        // Given: Empty or near-empty input
        let input = ""

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: Should still provide suggestions
        XCTAssertFalse(response.suggestions.isEmpty)
    }

    func testSpecialCharacters_HandledGracefully() {
        // Given: Input with special characters
        let input = "!@#$%"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: Should not crash and provide default suggestions
        XCTAssertFalse(response.suggestions.isEmpty)
        XCTAssertFalse(response.message.isEmpty)
    }

    // MARK: - Example Scenarios from Spec

    func testScenario_ItFeelsOffToday() {
        // Given: Spec example "It feels off today"
        let input = "It feels off today"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: Should suggest clubAdjustment, patternQuery, or recoveryCheck
        let suggestedTypes = response.suggestions.map { $0.intentType }
        XCTAssertTrue(suggestedTypes.contains(.clubAdjustment) ||
                     suggestedTypes.contains(.patternQuery) ||
                     suggestedTypes.contains(.recoveryCheck))
        XCTAssertTrue(response.suggestions.count <= 3)
    }

    func testScenario_HelpMeWithMyGame() {
        // Given: Spec example "Help me with my game"
        let input = "Help me with my game"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: Should suggest shotRecommendation, drillRequest, or statsLookup
        let suggestedTypes = response.suggestions.map { $0.intentType }
        XCTAssertTrue(suggestedTypes.contains(.shotRecommendation) ||
                     suggestedTypes.contains(.drillRequest) ||
                     suggestedTypes.contains(.statsLookup))
    }

    func testScenario_WhatShouldIDo() {
        // Given: Spec example "What should I do?"
        let input = "What should I do?"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: Should suggest shotRecommendation, helpRequest, or weatherCheck
        let suggestedTypes = response.suggestions.map { $0.intentType }
        XCTAssertTrue(suggestedTypes.contains(.shotRecommendation) ||
                     suggestedTypes.contains(.helpRequest) ||
                     suggestedTypes.contains(.weatherCheck))
        XCTAssertEqual(response.originalInput, input)
    }

    // MARK: - Chip Label Tests

    func testChipLabels_AreUserFriendly() {
        // Given: Any input that generates suggestions
        let input = "I need help"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: All chip labels should be concise and actionable
        for suggestion in response.suggestions {
            XCTAssertTrue(suggestion.label.count < 30, "Label too long: \(suggestion.label)")
            XCTAssertFalse(suggestion.label.contains("_"))
            XCTAssertFalse(suggestion.label.contains("Request"))
        }
    }

    func testChipLabels_AreUnique() {
        // Given: Any input
        let input = "help"

        // When: Generating clarification
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // Then: All chip labels should be unique
        let labels = response.suggestions.map { $0.label }
        let uniqueLabels = Set(labels)
        XCTAssertEqual(labels.count, uniqueLabels.count)
    }

    // MARK: - Integration with Classification Result

    func testClarificationResponse_CanBeUsedInClassificationResult() {
        // Given: A clarification response
        let input = "unclear"
        let response = handler.generateClarification(
            for: input,
            parsedIntent: nil,
            context: nil
        )

        // When: Creating a classification result
        let result = ClassificationResult.clarify(response: response)

        // Then: Should be properly structured
        switch result {
        case .clarify(let clarificationResponse):
            XCTAssertEqual(clarificationResponse.originalInput, input)
            XCTAssertFalse(clarificationResponse.suggestions.isEmpty)
        default:
            XCTFail("Expected clarify result")
        }
    }
}
