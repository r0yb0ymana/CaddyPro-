import XCTest
@testable import App

/// Tests for ContextInjector.
///
/// Verifies context prompt formatting for LLM injection.
final class ContextInjectorTests: XCTestCase {

    var injector: ContextInjector!

    override func setUp() {
        injector = ContextInjector()
    }

    override func tearDown() {
        injector = nil
    }

    // MARK: - Empty Context

    func testBuildContextPromptWithEmptyContext() {
        // Given
        let context = SessionContext.empty

        // When
        let prompt = injector.buildContextPrompt(from: context)

        // Then
        XCTAssertTrue(prompt.contains("# Session Context"))
        // Should not crash with empty context
        XCTAssertFalse(prompt.contains("Current Round"))
        XCTAssertFalse(prompt.contains("Current Hole"))
    }

    // MARK: - Round Information

    func testBuildContextPromptWithRound() {
        // Given
        let round = Round(
            id: "round-123",
            startTime: Date(),
            courseName: "Pebble Beach",
            scores: [1: 4, 2: 5, 3: 3]
        )
        let context = SessionContext(currentRound: round)

        // When
        let prompt = injector.buildContextPrompt(from: context)

        // Then
        XCTAssertTrue(prompt.contains("## Current Round"))
        XCTAssertTrue(prompt.contains("Course: Pebble Beach"))
        XCTAssertTrue(prompt.contains("Round ID: round-123"))
        XCTAssertTrue(prompt.contains("Score: 12 after 3 holes"))
    }

    func testBuildContextPromptWithRoundNoCourseName() {
        // Given
        let round = Round(
            id: "round-456",
            startTime: Date(),
            courseName: nil,
            scores: [:]
        )
        let context = SessionContext(currentRound: round)

        // When
        let prompt = injector.buildContextPrompt(from: context)

        // Then
        XCTAssertTrue(prompt.contains("## Current Round"))
        XCTAssertTrue(prompt.contains("Round ID: round-456"))
        XCTAssertFalse(prompt.contains("Course:"))
    }

    // MARK: - Hole Information

    func testBuildContextPromptWithHole() {
        // Given
        let context = SessionContext(currentHole: 7)

        // When
        let prompt = injector.buildContextPrompt(from: context)

        // Then
        XCTAssertTrue(prompt.contains("## Current Hole"))
        XCTAssertTrue(prompt.contains("Hole: 7"))
    }

    // MARK: - Last Shot

    func testBuildContextPromptWithLastShot() {
        // Given
        let shot = Shot(
            club: Club(name: "7-iron", type: .iron),
            missDirection: .sliceRight,
            lie: .fairway,
            pressureContext: PressureContext(isUnderPressure: true),
            holeNumber: 5,
            notes: "Rushed the swing"
        )
        let context = SessionContext(lastShot: shot)

        // When
        let prompt = injector.buildContextPrompt(from: context)

        // Then
        XCTAssertTrue(prompt.contains("## Last Shot"))
        XCTAssertTrue(prompt.contains("Club: 7-iron"))
        XCTAssertTrue(prompt.contains("Lie: fairway"))
        XCTAssertTrue(prompt.contains("Miss: slice right"))
        XCTAssertTrue(prompt.contains("Pressure: Yes"))
        XCTAssertTrue(prompt.contains("Notes: Rushed the swing"))
    }

    func testBuildContextPromptWithShotNoMiss() {
        // Given
        let shot = Shot(
            club: Club(name: "Driver", type: .driver),
            missDirection: nil,
            lie: .tee,
            pressureContext: PressureContext(isUnderPressure: false)
        )
        let context = SessionContext(lastShot: shot)

        // When
        let prompt = injector.buildContextPrompt(from: context)

        // Then
        XCTAssertTrue(prompt.contains("## Last Shot"))
        XCTAssertTrue(prompt.contains("Club: Driver"))
        XCTAssertTrue(prompt.contains("Lie: tee"))
        XCTAssertFalse(prompt.contains("Miss:"))
        XCTAssertFalse(prompt.contains("Pressure:"))
    }

    func testMissDirectionFormatting() {
        // Test all miss direction formats
        let missDirections: [(MissDirection, String)] = [
            (.straight, "straight"),
            (.pushRight, "push right"),
            (.pullLeft, "pull left"),
            (.sliceRight, "slice right"),
            (.hookLeft, "hook left"),
            (.short, "short"),
            (.long, "long")
        ]

        for (direction, expectedString) in missDirections {
            // Given
            let shot = Shot(
                club: Club(name: "8-iron", type: .iron),
                missDirection: direction,
                lie: .fairway
            )
            let context = SessionContext(lastShot: shot)

            // When
            let prompt = injector.buildContextPrompt(from: context)

            // Then
            XCTAssertTrue(
                prompt.contains("Miss: \(expectedString)"),
                "Expected '\(expectedString)' for \(direction)"
            )
        }
    }

    // MARK: - Last Recommendation

    func testBuildContextPromptWithRecommendation() {
        // Given
        let recommendation = "Hit a soft 9-iron to the center of the green"
        let context = SessionContext(lastRecommendation: recommendation)

        // When
        let prompt = injector.buildContextPrompt(from: context)

        // Then
        XCTAssertTrue(prompt.contains("## Last Recommendation"))
        XCTAssertTrue(prompt.contains(recommendation))
    }

    // MARK: - Conversation History

    func testBuildContextPromptWithConversationHistory() {
        // Given
        let turns = [
            ConversationTurn(role: .user, content: "What club should I use?"),
            ConversationTurn(role: .assistant, content: "Use your 7-iron"),
            ConversationTurn(role: .user, content: "What about the wind?"),
            ConversationTurn(role: .assistant, content: "Add a club for the headwind")
        ]
        let context = SessionContext(conversationHistory: turns)

        // When
        let prompt = injector.buildContextPrompt(from: context)

        // Then
        XCTAssertTrue(prompt.contains("## Recent Conversation"))
        XCTAssertTrue(prompt.contains("User: What club should I use?"))
        XCTAssertTrue(prompt.contains("Assistant: Use your 7-iron"))
        XCTAssertTrue(prompt.contains("User: What about the wind?"))
        XCTAssertTrue(prompt.contains("Assistant: Add a club for the headwind"))
    }

    // MARK: - Full Context

    func testBuildContextPromptWithFullContext() {
        // Given
        let round = Round(
            id: "round-789",
            startTime: Date(),
            courseName: "Augusta National",
            scores: [1: 4, 2: 3]
        )

        let shot = Shot(
            club: Club(name: "9-iron", type: .iron),
            missDirection: .short,
            lie: .fairway,
            pressureContext: PressureContext(isUnderPressure: false),
            holeNumber: 3
        )

        let turns = [
            ConversationTurn(role: .user, content: "What's the yardage?"),
            ConversationTurn(role: .assistant, content: "155 yards to the pin")
        ]

        let context = SessionContext(
            currentRound: round,
            currentHole: 3,
            lastShot: shot,
            lastRecommendation: "Hit a smooth 9-iron",
            conversationHistory: turns
        )

        // When
        let prompt = injector.buildContextPrompt(from: context)

        // Then
        // Round info
        XCTAssertTrue(prompt.contains("Augusta National"))
        XCTAssertTrue(prompt.contains("Score: 7 after 2 holes"))

        // Hole info
        XCTAssertTrue(prompt.contains("Hole: 3"))

        // Shot info
        XCTAssertTrue(prompt.contains("Club: 9-iron"))
        XCTAssertTrue(prompt.contains("Miss: short"))

        // Recommendation
        XCTAssertTrue(prompt.contains("Hit a smooth 9-iron"))

        // Conversation
        XCTAssertTrue(prompt.contains("What's the yardage?"))
        XCTAssertTrue(prompt.contains("155 yards to the pin"))
    }

    // MARK: - Prompt Structure

    func testPromptStructureIsConsistent() {
        // Given
        let round = Round(id: "r1", courseName: "Test Course")
        let context = SessionContext(
            currentRound: round,
            currentHole: 5,
            lastRecommendation: "Test recommendation"
        )

        // When
        let prompt = injector.buildContextPrompt(from: context)

        // Then - Verify sections appear in expected order
        let roundIndex = prompt.range(of: "## Current Round")?.lowerBound
        let holeIndex = prompt.range(of: "## Current Hole")?.lowerBound
        let recommendationIndex = prompt.range(of: "## Last Recommendation")?.lowerBound

        XCTAssertNotNil(roundIndex)
        XCTAssertNotNil(holeIndex)
        XCTAssertNotNil(recommendationIndex)

        // Round should come before hole
        XCTAssertTrue(roundIndex! < holeIndex!)
        // Hole should come before recommendation
        XCTAssertTrue(holeIndex! < recommendationIndex!)
    }
}
