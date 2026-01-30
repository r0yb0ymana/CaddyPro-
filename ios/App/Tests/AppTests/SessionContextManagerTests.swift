import XCTest
@testable import App

/// Tests for SessionContextManager.
///
/// Verifies context tracking, conversation history, and LLM context injection.
///
/// Spec R6: Session Context
/// - Context updates correctly
/// - Follow-up queries use context
/// - Session persists during app lifecycle
/// - Clear wipes all context
@MainActor
final class SessionContextManagerTests: XCTestCase {

    var manager: SessionContextManager!

    override func setUp() async throws {
        manager = SessionContextManager()
    }

    override func tearDown() async throws {
        manager = nil
    }

    // MARK: - Round Management Tests

    func testUpdateRound() {
        // Given
        let roundId = "round-123"
        let courseName = "Pebble Beach"

        // When
        manager.updateRound(roundId: roundId, courseName: courseName)

        // Then
        XCTAssertNotNil(manager.context.currentRound)
        XCTAssertEqual(manager.context.currentRound?.id, roundId)
        XCTAssertEqual(manager.context.currentRound?.courseName, courseName)
    }

    func testUpdateHole() {
        // Given
        let holeNumber = 7
        let par = 4

        // When
        manager.updateHole(holeNumber: holeNumber, par: par)

        // Then
        XCTAssertEqual(manager.context.currentHole, holeNumber)
    }

    func testUpdateHoleWithInvalidNumber() {
        // Given
        let invalidHole = 25

        // When
        manager.updateHole(holeNumber: invalidHole, par: 4)

        // Then - should not update
        XCTAssertNil(manager.context.currentHole)
    }

    func testUpdateHolePreservesRound() {
        // Given
        manager.updateRound(roundId: "round-1", courseName: "Augusta")
        let roundBefore = manager.context.currentRound

        // When
        manager.updateHole(holeNumber: 1, par: 4)

        // Then
        XCTAssertEqual(manager.context.currentRound?.id, roundBefore?.id)
        XCTAssertEqual(manager.context.currentHole, 1)
    }

    // MARK: - Shot Tracking Tests

    func testRecordShot() {
        // Given
        let shot = Shot(
            club: Club(name: "7-iron", type: .iron),
            missDirection: .sliceRight,
            lie: .fairway,
            pressureContext: PressureContext(isUnderPressure: true),
            holeNumber: 5
        )

        // When
        manager.recordShot(shot)

        // Then
        XCTAssertNotNil(manager.context.lastShot)
        XCTAssertEqual(manager.context.lastShot?.id, shot.id)
        XCTAssertEqual(manager.context.lastShot?.club.name, "7-iron")
        XCTAssertEqual(manager.context.lastShot?.missDirection, .sliceRight)
    }

    func testRecordShotPreservesContext() {
        // Given
        manager.updateRound(roundId: "round-1", courseName: "St Andrews")
        manager.updateHole(holeNumber: 3, par: 4)

        let shot = Shot(
            club: Club(name: "Driver", type: .driver),
            lie: .tee
        )

        // When
        manager.recordShot(shot)

        // Then
        XCTAssertNotNil(manager.context.currentRound)
        XCTAssertEqual(manager.context.currentHole, 3)
        XCTAssertNotNil(manager.context.lastShot)
    }

    // MARK: - Recommendation Tracking Tests

    func testRecordRecommendation() {
        // Given
        let recommendation = "Hit a 7-iron with a slight draw to avoid the bunker"

        // When
        manager.recordRecommendation(recommendation)

        // Then
        XCTAssertEqual(manager.context.lastRecommendation, recommendation)
    }

    func testRecordRecommendationPreservesShot() {
        // Given
        let shot = Shot(
            club: Club(name: "9-iron", type: .iron),
            lie: .fairway
        )
        manager.recordShot(shot)

        // When
        manager.recordRecommendation("Play it safe")

        // Then
        XCTAssertNotNil(manager.context.lastShot)
        XCTAssertEqual(manager.context.lastShot?.club.name, "9-iron")
        XCTAssertEqual(manager.context.lastRecommendation, "Play it safe")
    }

    // MARK: - Conversation Management Tests

    func testAddConversationTurn() {
        // Given
        let userInput = "What's the play here?"
        let assistantResponse = "Hit a 7-iron to the center of the green"

        // When
        manager.addConversationTurn(userInput: userInput, assistantResponse: assistantResponse)

        // Then
        XCTAssertEqual(manager.context.conversationHistory.count, 2)
        XCTAssertEqual(manager.context.conversationHistory[0].role, .user)
        XCTAssertEqual(manager.context.conversationHistory[0].content, userInput)
        XCTAssertEqual(manager.context.conversationHistory[1].role, .assistant)
        XCTAssertEqual(manager.context.conversationHistory[1].content, assistantResponse)
    }

    func testConversationHistoryLimitedToTenTurns() {
        // Given - Add 15 conversation exchanges (30 turns)
        for i in 1...15 {
            manager.addConversationTurn(
                userInput: "User message \(i)",
                assistantResponse: "Assistant response \(i)"
            )
        }

        // Then - Should only have last 10 turns (5 exchanges)
        XCTAssertEqual(manager.context.conversationHistory.count, 10)

        // Verify oldest turns were removed
        XCTAssertEqual(manager.context.conversationHistory[0].content, "User message 11")
        XCTAssertEqual(manager.context.conversationHistory.last?.content, "Assistant response 15")
    }

    func testGetLastUserInput() {
        // Given
        manager.addConversationTurn(
            userInput: "What club should I use?",
            assistantResponse: "Use your 8-iron"
        )
        manager.addConversationTurn(
            userInput: "What about wind?",
            assistantResponse: "Add a club for the headwind"
        )

        // When
        let lastInput = manager.getLastUserInput()

        // Then
        XCTAssertEqual(lastInput, "What about wind?")
    }

    func testGetRecentConversation() {
        // Given
        for i in 1...10 {
            manager.addConversationTurn(
                userInput: "User \(i)",
                assistantResponse: "Assistant \(i)"
            )
        }

        // When
        let recent = manager.getRecentConversation(count: 4)

        // Then
        XCTAssertEqual(recent.count, 4)
        XCTAssertEqual(recent[0].content, "Assistant 9")
        XCTAssertEqual(recent[3].content, "Assistant 10")
    }

    // MARK: - Session Clearing Tests

    func testClearSession() {
        // Given
        manager.updateRound(roundId: "round-1", courseName: "Augusta")
        manager.updateHole(holeNumber: 5, par: 4)
        manager.recordShot(Shot(
            club: Club(name: "Driver", type: .driver),
            lie: .tee
        ))
        manager.recordRecommendation("Play it safe")
        manager.addConversationTurn(
            userInput: "What's the play?",
            assistantResponse: "Hit driver"
        )

        // When
        manager.clearSession()

        // Then
        XCTAssertNil(manager.context.currentRound)
        XCTAssertNil(manager.context.currentHole)
        XCTAssertNil(manager.context.lastShot)
        XCTAssertNil(manager.context.lastRecommendation)
        XCTAssertTrue(manager.context.conversationHistory.isEmpty)
    }

    // MARK: - Context Retrieval Tests

    func testGetCurrentContext() {
        // Given
        manager.updateRound(roundId: "round-1", courseName: "Pebble Beach")
        manager.updateHole(holeNumber: 7, par: 3)

        // When
        let context = manager.getCurrentContext()

        // Then
        XCTAssertNotNil(context.currentRound)
        XCTAssertEqual(context.currentRound?.courseName, "Pebble Beach")
        XCTAssertEqual(context.currentHole, 7)
    }

    func testBuildContextPrompt() {
        // Given
        manager.updateRound(roundId: "round-1", courseName: "Augusta National")
        manager.updateHole(holeNumber: 12, par: 3)
        manager.recordShot(Shot(
            club: Club(name: "9-iron", type: .iron),
            missDirection: .short,
            lie: .fairway,
            pressureContext: PressureContext(isUnderPressure: false)
        ))
        manager.recordRecommendation("Hit a soft 8-iron to the center")
        manager.addConversationTurn(
            userInput: "What's the yardage?",
            assistantResponse: "155 yards"
        )

        // When
        let prompt = manager.buildContextPrompt()

        // Then
        XCTAssertTrue(prompt.contains("Augusta National"))
        XCTAssertTrue(prompt.contains("Hole: 12"))
        XCTAssertTrue(prompt.contains("9-iron"))
        XCTAssertTrue(prompt.contains("Hit a soft 8-iron to the center"))
        XCTAssertTrue(prompt.contains("What's the yardage?"))
        XCTAssertTrue(prompt.contains("155 yards"))
    }

    func testBuildContextPromptWithEmptyContext() {
        // When
        let prompt = manager.buildContextPrompt()

        // Then
        XCTAssertTrue(prompt.contains("# Session Context"))
        // Should not crash with empty context
    }

    // MARK: - Integration Tests

    func testFullSessionWorkflow() {
        // Given - Start a round
        manager.updateRound(roundId: "round-123", courseName: "Torrey Pines")
        manager.updateHole(holeNumber: 1, par: 4)

        // When - Play first hole
        manager.addConversationTurn(
            userInput: "What's the play off the tee?",
            assistantResponse: "Hit driver to the right side of the fairway"
        )

        manager.recordShot(Shot(
            club: Club(name: "Driver", type: .driver),
            missDirection: .sliceRight,
            lie: .tee,
            pressureContext: PressureContext(isUnderPressure: false),
            holeNumber: 1
        ))

        manager.addConversationTurn(
            userInput: "I sliced it into the rough",
            assistantResponse: "Take a 7-iron and punch out to the fairway"
        )

        manager.recordRecommendation("Punch out with 7-iron")

        // Then - Context should contain full history
        let context = manager.getCurrentContext()
        XCTAssertNotNil(context.currentRound)
        XCTAssertEqual(context.currentHole, 1)
        XCTAssertNotNil(context.lastShot)
        XCTAssertEqual(context.lastShot?.missDirection, .sliceRight)
        XCTAssertEqual(context.lastRecommendation, "Punch out with 7-iron")
        XCTAssertEqual(context.conversationHistory.count, 4)

        // Verify prompt includes all context
        let prompt = manager.buildContextPrompt()
        XCTAssertTrue(prompt.contains("Torrey Pines"))
        XCTAssertTrue(prompt.contains("slice right"))
        XCTAssertTrue(prompt.contains("Punch out with 7-iron"))
    }

    func testFollowUpQueryContext() {
        // Given - Establish context with a recommendation
        manager.updateHole(holeNumber: 5, par: 4)
        manager.recordRecommendation("Hit a soft 9-iron to avoid going long")
        manager.addConversationTurn(
            userInput: "What club should I hit?",
            assistantResponse: "Hit a soft 9-iron to avoid going long"
        )

        // When - User asks follow-up
        manager.addConversationTurn(
            userInput: "What about if the wind picks up?",
            assistantResponse: "In that case, hit a full 9-iron or even an 8-iron"
        )

        // Then - Context maintains conversation flow
        XCTAssertEqual(manager.context.conversationHistory.count, 4)
        XCTAssertEqual(manager.getLastUserInput(), "What about if the wind picks up?")

        let prompt = manager.buildContextPrompt()
        XCTAssertTrue(prompt.contains("What club should I hit?"))
        XCTAssertTrue(prompt.contains("What about if the wind picks up?"))
    }
}
