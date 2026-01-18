import XCTest
@testable import App

/// Integration tests for SessionContextManager with conversation flow.
///
/// Tests interaction between SessionContextManager, ConversationHistory, and ContextInjector.
///
/// Spec reference: navcaddy-engine.md R6, A4, A5
/// Task reference: navcaddy-engine-plan.md Task 17
///
/// Tests verify:
/// - Follow-up query handling uses conversation history
/// - Context injection formats correctly for LLM
/// - Session state persists across interactions
/// - Clear wipes all context components
@MainActor
final class SessionContextIntegrationTests: XCTestCase {
    var contextManager: SessionContextManager!
    var conversationHistory: ConversationHistory!
    var contextInjector: ContextInjector!

    override func setUp() {
        super.setUp()
        conversationHistory = ConversationHistory()
        contextInjector = ContextInjector()
        contextManager = SessionContextManager(
            conversationHistory: conversationHistory,
            contextInjector: contextInjector
        )
    }

    override func tearDown() {
        contextManager = nil
        contextInjector = nil
        conversationHistory = nil
        super.tearDown()
    }

    // MARK: - Follow-Up Query Handling Tests

    func testFollowUpQuery_UsesConversationHistory() {
        // GIVEN: Initial conversation about club selection
        contextManager.addConversationTurn(
            userInput: "What club should I use from 150 yards?",
            assistantResponse: "I'd recommend the 7-iron for 150 yards."
        )

        // WHEN: User asks follow-up without context
        contextManager.addConversationTurn(
            userInput: "What if there's wind?",
            assistantResponse: "With wind, consider the 6-iron instead."
        )

        // THEN: Context should include both turns
        let context = contextManager.getCurrentContext()
        XCTAssertEqual(context.conversationHistory.count, 4,
                      "Should have 4 turns (2 user + 2 assistant)")

        // Last user input should be accessible
        let lastInput = contextManager.getLastUserInput()
        XCTAssertEqual(lastInput, "What if there's wind?")

        // Recent conversation should include context
        let recent = contextManager.getRecentConversation(count: 5)
        XCTAssertEqual(recent.count, 4, "Should return all 4 turns")
    }

    func testFollowUpQuery_ReferencesLastRecommendation() {
        // GIVEN: Recommendation recorded in session
        let club = Club(id: "7iron", name: "7-iron", type: .iron)
        let shot = Shot(
            timestamp: Date(),
            club: club,
            missDirection: nil,
            lie: .fairway
        )
        contextManager.recordShot(shot)
        contextManager.recordRecommendation("Use the 8-iron for this approach shot.")

        // WHEN: Building context prompt
        let contextPrompt = contextManager.buildContextPrompt()

        // THEN: Should include last recommendation
        XCTAssertTrue(contextPrompt.contains("Last Recommendation"),
                     "Context should include recommendation section")
        XCTAssertTrue(contextPrompt.contains("8-iron"),
                     "Context should include recommended club")
    }

    func testFollowUpQuery_AccessesLastShot() {
        // GIVEN: Shot recorded in session
        let club = Club(id: "pw", name: "Pitching Wedge", type: .wedge)
        let shot = Shot(
            timestamp: Date(),
            club: club,
            missDirection: .short,
            lie: .rough,
            pressureContext: PressureContext(isUnderPressure: true, isUserTagged: true)
        )
        contextManager.recordShot(shot)

        // WHEN: Getting context
        let context = contextManager.getCurrentContext()

        // THEN: Last shot should be available
        XCTAssertNotNil(context.lastShot, "Context should have last shot")
        XCTAssertEqual(context.lastShot?.club.name, "Pitching Wedge")
        XCTAssertEqual(context.lastShot?.missDirection, .short)
        XCTAssertTrue(context.lastShot?.pressureContext.isUnderPressure ?? false,
                     "Should preserve pressure context")
    }

    func testMultipleFollowUps_MaintainFullContext() {
        // GIVEN: Series of related questions
        let conversations = [
            ("What club for 175 yards?", "Try the 6-iron."),
            ("What about from the rough?", "From rough, go with the 5-iron."),
            ("And if it's uphill?", "Uphill rough, definitely the 5-iron, maybe even 4-iron.")
        ]

        // WHEN: Adding all conversations
        for (user, assistant) in conversations {
            contextManager.addConversationTurn(
                userInput: user,
                assistantResponse: assistant
            )
        }

        // THEN: All context should be preserved
        let context = contextManager.getCurrentContext()
        XCTAssertEqual(context.conversationHistory.count, 6,
                      "Should have all 6 turns (3 exchanges)")

        // Most recent should be accessible
        let lastInput = contextManager.getLastUserInput()
        XCTAssertEqual(lastInput, "And if it's uphill?")

        // Context prompt should include all turns
        let prompt = contextManager.buildContextPrompt()
        XCTAssertTrue(prompt.contains("175 yards"), "Should include first question")
        XCTAssertTrue(prompt.contains("rough"), "Should include second question")
        XCTAssertTrue(prompt.contains("uphill"), "Should include third question")
    }

    // MARK: - Context Injection Format Tests

    func testContextPrompt_FormatsRoundInformation() {
        // GIVEN: Active round
        contextManager.updateRound(
            roundId: "round-123",
            courseName: "Pebble Beach"
        )
        contextManager.updateHole(holeNumber: 7, par: 4)

        // WHEN: Building context prompt
        let prompt = contextManager.buildContextPrompt()

        // THEN: Should include formatted round info
        XCTAssertTrue(prompt.contains("Current Round"),
                     "Should have round section header")
        XCTAssertTrue(prompt.contains("Pebble Beach"),
                     "Should include course name")
        XCTAssertTrue(prompt.contains("round-123"),
                     "Should include round ID")
        XCTAssertTrue(prompt.contains("Current Hole"),
                     "Should have hole section header")
        XCTAssertTrue(prompt.contains("7"),
                     "Should include hole number")
    }

    func testContextPrompt_FormatsShotDetails() {
        // GIVEN: Shot with full details
        let club = Club(id: "9iron", name: "9-iron", type: .iron)
        let shot = Shot(
            timestamp: Date(),
            club: club,
            missDirection: .pull,
            lie: .fairway,
            pressureContext: PressureContext(isUnderPressure: true, isUserTagged: true),
            holeNumber: 5,
            notes: "Into the wind"
        )
        contextManager.recordShot(shot)

        // WHEN: Building context prompt
        let prompt = contextManager.buildContextPrompt()

        // THEN: Should format all shot details
        XCTAssertTrue(prompt.contains("Last Shot"),
                     "Should have shot section header")
        XCTAssertTrue(prompt.contains("9-iron"),
                     "Should include club name")
        XCTAssertTrue(prompt.contains("fairway"),
                     "Should include lie")
        XCTAssertTrue(prompt.contains("pull"),
                     "Should include miss direction")
        XCTAssertTrue(prompt.contains("Pressure: Yes"),
                     "Should indicate pressure")
        XCTAssertTrue(prompt.contains("Into the wind"),
                     "Should include notes")
    }

    func testContextPrompt_FormatsConversationHistory() {
        // GIVEN: Conversation history
        contextManager.addConversationTurn(
            userInput: "How far is my 7-iron?",
            assistantResponse: "Your 7-iron typically goes about 165 yards."
        )

        // WHEN: Building context prompt
        let prompt = contextManager.buildContextPrompt()

        // THEN: Should format conversation with roles
        XCTAssertTrue(prompt.contains("Recent Conversation"),
                     "Should have conversation section header")
        XCTAssertTrue(prompt.contains("User: How far is my 7-iron?"),
                     "Should label user input")
        XCTAssertTrue(prompt.contains("Assistant: Your 7-iron typically goes about 165 yards."),
                     "Should label assistant response")
    }

    func testContextPrompt_HandlesEmptyState() {
        // GIVEN: Empty context
        contextManager.clearSession()

        // WHEN: Building context prompt
        let prompt = contextManager.buildContextPrompt()

        // THEN: Should generate minimal valid prompt
        XCTAssertTrue(prompt.contains("Session Context"),
                     "Should have header")
        // Should not have section headers for missing data
        XCTAssertFalse(prompt.contains("Current Round"),
                      "Should not show empty round section")
        XCTAssertFalse(prompt.contains("Last Shot"),
                      "Should not show empty shot section")
    }

    func testContextPrompt_LimitsHistoryToRecent() {
        // GIVEN: More than 10 conversation turns
        for i in 1...12 {
            contextManager.addConversationTurn(
                userInput: "Question \(i)",
                assistantResponse: "Answer \(i)"
            )
        }

        // WHEN: Building context prompt
        let prompt = contextManager.buildContextPrompt()
        let context = contextManager.getCurrentContext()

        // THEN: Should only include last 10 turns
        XCTAssertEqual(context.conversationHistory.count, 10,
                      "Should limit to 10 turns")
        XCTAssertFalse(prompt.contains("Question 1"),
                      "Should not include oldest turn")
        XCTAssertFalse(prompt.contains("Question 2"),
                      "Should not include second oldest turn")
        XCTAssertTrue(prompt.contains("Question 11"),
                     "Should include recent turn")
        XCTAssertTrue(prompt.contains("Question 12"),
                     "Should include most recent turn")
    }

    // MARK: - Session State Persistence Tests

    func testSessionState_PersistsAcrossMultipleOperations() {
        // GIVEN: Session with multiple state updates
        contextManager.updateRound(roundId: "round-456", courseName: "Augusta National")
        contextManager.updateHole(holeNumber: 12, par: 3)

        let club = Club(id: "6iron", name: "6-iron", type: .iron)
        let shot = Shot(timestamp: Date(), club: club, missDirection: nil, lie: .tee)
        contextManager.recordShot(shot)

        contextManager.recordRecommendation("Aim for the middle of the green.")

        contextManager.addConversationTurn(
            userInput: "Where should I aim?",
            assistantResponse: "Aim for the middle of the green."
        )

        // WHEN: Getting context multiple times
        let context1 = contextManager.getCurrentContext()
        let context2 = contextManager.getCurrentContext()

        // THEN: State should be consistent
        XCTAssertEqual(context1.currentRound?.courseName, context2.currentRound?.courseName)
        XCTAssertEqual(context1.currentHole, context2.currentHole)
        XCTAssertEqual(context1.lastShot?.club.id, context2.lastShot?.club.id)
        XCTAssertEqual(context1.lastRecommendation, context2.lastRecommendation)
        XCTAssertEqual(context1.conversationHistory.count, context2.conversationHistory.count)
    }

    func testSessionState_UpdatesIncrementally() {
        // GIVEN: Initial empty state
        var context = contextManager.getCurrentContext()
        XCTAssertNil(context.currentRound, "Should start with no round")

        // WHEN: Updating round
        contextManager.updateRound(roundId: "round-1", courseName: "Pine Valley")
        context = contextManager.getCurrentContext()

        // THEN: Round should be set, others still empty
        XCTAssertNotNil(context.currentRound)
        XCTAssertNil(context.currentHole)
        XCTAssertNil(context.lastShot)

        // WHEN: Updating hole
        contextManager.updateHole(holeNumber: 3, par: 4)
        context = contextManager.getCurrentContext()

        // THEN: Hole should be set, round preserved
        XCTAssertNotNil(context.currentRound)
        XCTAssertEqual(context.currentHole, 3)
        XCTAssertNil(context.lastShot)

        // WHEN: Recording shot
        let club = Club(id: "driver", name: "Driver", type: .driver)
        let shot = Shot(timestamp: Date(), club: club, missDirection: nil, lie: .tee)
        contextManager.recordShot(shot)
        context = contextManager.getCurrentContext()

        // THEN: All state preserved
        XCTAssertNotNil(context.currentRound)
        XCTAssertEqual(context.currentHole, 3)
        XCTAssertNotNil(context.lastShot)
    }

    func testSessionState_PreservesAcrossConversations() {
        // GIVEN: Session with round and shot
        contextManager.updateRound(roundId: "round-789", courseName: "St Andrews")
        let club = Club(id: "3wood", name: "3-wood", type: .wood)
        let shot = Shot(timestamp: Date(), club: club, missDirection: .slice, lie: .fairway)
        contextManager.recordShot(shot)

        // WHEN: Having multiple conversations
        for i in 1...5 {
            contextManager.addConversationTurn(
                userInput: "Question \(i)",
                assistantResponse: "Answer \(i)"
            )
        }

        // THEN: Round and shot should still be present
        let context = contextManager.getCurrentContext()
        XCTAssertEqual(context.currentRound?.courseName, "St Andrews")
        XCTAssertEqual(context.lastShot?.club.name, "3-wood")
        XCTAssertEqual(context.conversationHistory.count, 10) // 5 exchanges = 10 turns
    }

    // MARK: - Clear Session Tests

    func testClearSession_WipesAllContext() {
        // GIVEN: Session with full context
        contextManager.updateRound(roundId: "round-999", courseName: "Oakmont")
        contextManager.updateHole(holeNumber: 18, par: 4)

        let club = Club(id: "pw", name: "Pitching Wedge", type: .wedge)
        let shot = Shot(timestamp: Date(), club: club, missDirection: .short, lie: .rough)
        contextManager.recordShot(shot)

        contextManager.recordRecommendation("Take the safe line to the middle of the green.")

        contextManager.addConversationTurn(
            userInput: "What's my play here?",
            assistantResponse: "Take the safe line to the middle of the green."
        )

        // Verify context exists
        var context = contextManager.getCurrentContext()
        XCTAssertNotNil(context.currentRound)
        XCTAssertNotNil(context.currentHole)
        XCTAssertNotNil(context.lastShot)
        XCTAssertNotNil(context.lastRecommendation)
        XCTAssertFalse(context.conversationHistory.isEmpty)

        // WHEN: Clearing session
        contextManager.clearSession()

        // THEN: All context should be wiped
        context = contextManager.getCurrentContext()
        XCTAssertNil(context.currentRound, "Round should be cleared")
        XCTAssertNil(context.currentHole, "Hole should be cleared")
        XCTAssertNil(context.lastShot, "Last shot should be cleared")
        XCTAssertNil(context.lastRecommendation, "Recommendation should be cleared")
        XCTAssertTrue(context.conversationHistory.isEmpty, "History should be cleared")
    }

    func testClearSession_WipesConversationHistory() {
        // GIVEN: Conversation history
        for i in 1...5 {
            contextManager.addConversationTurn(
                userInput: "User message \(i)",
                assistantResponse: "Assistant response \(i)"
            )
        }

        XCTAssertEqual(contextManager.getRecentConversation().count, 10,
                      "Should have conversation history")

        // WHEN: Clearing session
        contextManager.clearSession()

        // THEN: History should be empty
        XCTAssertTrue(contextManager.getRecentConversation().isEmpty,
                     "Conversation history should be cleared")
        XCTAssertNil(contextManager.getLastUserInput(),
                    "Last user input should be nil")
    }

    func testClearSession_AllowsNewSession() {
        // GIVEN: Cleared session
        contextManager.updateRound(roundId: "old-round", courseName: "Old Course")
        contextManager.clearSession()

        // WHEN: Starting new session
        contextManager.updateRound(roundId: "new-round", courseName: "New Course")
        contextManager.updateHole(holeNumber: 1, par: 4)

        let club = Club(id: "driver", name: "Driver", type: .driver)
        let shot = Shot(timestamp: Date(), club: club, missDirection: nil, lie: .tee)
        contextManager.recordShot(shot)

        // THEN: New context should be independent
        let context = contextManager.getCurrentContext()
        XCTAssertEqual(context.currentRound?.id, "new-round")
        XCTAssertEqual(context.currentRound?.courseName, "New Course")
        XCTAssertEqual(context.currentHole, 1)
        XCTAssertEqual(context.lastShot?.club.name, "Driver")
        XCTAssertTrue(context.conversationHistory.isEmpty,
                     "Should have fresh conversation history")
    }

    // MARK: - Context Integration Tests

    func testFullConversationFlow_WithAllComponents() {
        // GIVEN: Complete conversation scenario
        // 1. Start round
        contextManager.updateRound(roundId: "round-complete", courseName: "Cypress Point")
        contextManager.updateHole(holeNumber: 16, par: 4)

        // 2. User asks for club recommendation
        contextManager.addConversationTurn(
            userInput: "What club from 180 yards?",
            assistantResponse: "I'd go with the 5-iron for 180 yards."
        )

        // 3. Record shot with recommendation
        let club = Club(id: "5iron", name: "5-iron", type: .iron)
        let shot = Shot(
            timestamp: Date(),
            club: club,
            missDirection: nil,
            lie: .fairway
        )
        contextManager.recordShot(shot)
        contextManager.recordRecommendation("Use the 5-iron for 180 yards.")

        // 4. User asks follow-up
        contextManager.addConversationTurn(
            userInput: "What if I hit it fat?",
            assistantResponse: "If you're worried about hitting it fat, consider the 4-iron for safety."
        )

        // WHEN: Building complete context
        let context = contextManager.getCurrentContext()
        let prompt = contextManager.buildContextPrompt()

        // THEN: Context should include all elements
        XCTAssertEqual(context.currentRound?.courseName, "Cypress Point")
        XCTAssertEqual(context.currentHole, 16)
        XCTAssertEqual(context.lastShot?.club.name, "5-iron")
        XCTAssertEqual(context.lastRecommendation, "Use the 5-iron for 180 yards.")
        XCTAssertEqual(context.conversationHistory.count, 4)

        // Prompt should be comprehensive
        XCTAssertTrue(prompt.contains("Cypress Point"))
        XCTAssertTrue(prompt.contains("16"))
        XCTAssertTrue(prompt.contains("5-iron"))
        XCTAssertTrue(prompt.contains("180 yards"))
        XCTAssertTrue(prompt.contains("hit it fat"))
    }

    func testContextManager_WithConversationHistoryComponent() {
        // GIVEN: Direct interaction with conversation history component
        let turn1 = ConversationTurn(role: .user, content: "Test question", timestamp: Date())
        conversationHistory.addTurn(turn1)

        let turn2 = ConversationTurn(role: .assistant, content: "Test answer", timestamp: Date())
        conversationHistory.addTurn(turn2)

        // WHEN: Context manager uses same history
        contextManager.addConversationTurn(
            userInput: "Second question",
            assistantResponse: "Second answer"
        )

        // THEN: Should have combined history
        let allTurns = conversationHistory.getAll()
        XCTAssertEqual(allTurns.count, 4,
                      "Should combine pre-existing and new turns")
    }

    func testContextInjector_IntegratesWithManager() {
        // GIVEN: Context with various elements
        contextManager.updateRound(roundId: "test-round", courseName: "Test Course")
        contextManager.updateHole(holeNumber: 9, par: 5)

        // WHEN: Using injector directly vs through manager
        let managerPrompt = contextManager.buildContextPrompt()
        let directPrompt = contextInjector.buildContextPrompt(from: contextManager.getCurrentContext())

        // THEN: Should produce same result
        XCTAssertEqual(managerPrompt, directPrompt,
                      "Manager should delegate to injector correctly")
    }
}
