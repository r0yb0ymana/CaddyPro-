import XCTest
@testable import App

/// Tests for ConversationHistory circular buffer.
///
/// Verifies fixed-size buffer behavior and conversation retrieval.
final class ConversationHistoryTests: XCTestCase {

    var history: ConversationHistory!

    override func setUp() {
        history = ConversationHistory()
    }

    override func tearDown() {
        history = nil
    }

    // MARK: - Basic Operations

    func testAddTurn() {
        // Given
        let turn = ConversationTurn(
            role: .user,
            content: "Hello"
        )

        // When
        history.addTurn(turn)

        // Then
        XCTAssertEqual(history.count(), 1)
        XCTAssertFalse(history.isEmpty())
    }

    func testGetAll() {
        // Given
        history.addTurn(ConversationTurn(role: .user, content: "First"))
        history.addTurn(ConversationTurn(role: .assistant, content: "Second"))
        history.addTurn(ConversationTurn(role: .user, content: "Third"))

        // When
        let all = history.getAll()

        // Then
        XCTAssertEqual(all.count, 3)
        XCTAssertEqual(all[0].content, "First")
        XCTAssertEqual(all[1].content, "Second")
        XCTAssertEqual(all[2].content, "Third")
    }

    func testClear() {
        // Given
        history.addTurn(ConversationTurn(role: .user, content: "Test"))
        history.addTurn(ConversationTurn(role: .assistant, content: "Response"))

        // When
        history.clear()

        // Then
        XCTAssertEqual(history.count(), 0)
        XCTAssertTrue(history.isEmpty())
    }

    // MARK: - Circular Buffer Behavior

    func testCircularBufferPrunesOldest() {
        // Given - Add 15 turns (exceeds maxTurns of 10)
        for i in 1...15 {
            history.addTurn(ConversationTurn(
                role: i % 2 == 0 ? .assistant : .user,
                content: "Message \(i)"
            ))
        }

        // Then - Should only have 10 most recent
        XCTAssertEqual(history.count(), 10)

        let all = history.getAll()
        XCTAssertEqual(all.first?.content, "Message 6")
        XCTAssertEqual(all.last?.content, "Message 15")
    }

    func testCircularBufferAtExactCapacity() {
        // Given - Add exactly 10 turns
        for i in 1...10 {
            history.addTurn(ConversationTurn(
                role: .user,
                content: "Turn \(i)"
            ))
        }

        // Then - All 10 should be retained
        XCTAssertEqual(history.count(), 10)

        // When - Add one more
        history.addTurn(ConversationTurn(role: .user, content: "Turn 11"))

        // Then - Oldest should be removed
        XCTAssertEqual(history.count(), 10)
        let all = history.getAll()
        XCTAssertEqual(all.first?.content, "Turn 2")
        XCTAssertEqual(all.last?.content, "Turn 11")
    }

    // MARK: - Retrieval

    func testGetRecent() {
        // Given
        for i in 1...10 {
            history.addTurn(ConversationTurn(
                role: .user,
                content: "Message \(i)"
            ))
        }

        // When
        let recent = history.getRecent(count: 3)

        // Then
        XCTAssertEqual(recent.count, 3)
        XCTAssertEqual(recent[0].content, "Message 8")
        XCTAssertEqual(recent[1].content, "Message 9")
        XCTAssertEqual(recent[2].content, "Message 10")
    }

    func testGetRecentWithFewerThanRequested() {
        // Given
        history.addTurn(ConversationTurn(role: .user, content: "Only one"))

        // When
        let recent = history.getRecent(count: 5)

        // Then
        XCTAssertEqual(recent.count, 1)
        XCTAssertEqual(recent[0].content, "Only one")
    }

    func testGetRecentWithEmptyHistory() {
        // When
        let recent = history.getRecent(count: 5)

        // Then
        XCTAssertEqual(recent.count, 0)
    }

    func testGetLastUserInput() {
        // Given
        history.addTurn(ConversationTurn(role: .user, content: "First user"))
        history.addTurn(ConversationTurn(role: .assistant, content: "Response"))
        history.addTurn(ConversationTurn(role: .user, content: "Second user"))
        history.addTurn(ConversationTurn(role: .assistant, content: "Another response"))

        // When
        let lastUserInput = history.getLastUserInput()

        // Then
        XCTAssertEqual(lastUserInput, "Second user")
    }

    func testGetLastUserInputWithNoUserTurns() {
        // Given
        history.addTurn(ConversationTurn(role: .assistant, content: "Only assistant"))

        // When
        let lastUserInput = history.getLastUserInput()

        // Then
        XCTAssertNil(lastUserInput)
    }

    func testGetLastAssistantResponse() {
        // Given
        history.addTurn(ConversationTurn(role: .user, content: "User"))
        history.addTurn(ConversationTurn(role: .assistant, content: "First response"))
        history.addTurn(ConversationTurn(role: .user, content: "Another user"))
        history.addTurn(ConversationTurn(role: .assistant, content: "Second response"))

        // When
        let lastResponse = history.getLastAssistantResponse()

        // Then
        XCTAssertEqual(lastResponse, "Second response")
    }

    func testGetLastAssistantResponseWithNoAssistantTurns() {
        // Given
        history.addTurn(ConversationTurn(role: .user, content: "Only user"))

        // When
        let lastResponse = history.getLastAssistantResponse()

        // Then
        XCTAssertNil(lastResponse)
    }

    // MARK: - Custom Max Turns

    func testCustomMaxTurns() {
        // Given
        let customHistory = ConversationHistory(maxTurns: 5)

        // When
        for i in 1...10 {
            customHistory.addTurn(ConversationTurn(
                role: .user,
                content: "Message \(i)"
            ))
        }

        // Then
        XCTAssertEqual(customHistory.count(), 5)
        let all = customHistory.getAll()
        XCTAssertEqual(all.first?.content, "Message 6")
        XCTAssertEqual(all.last?.content, "Message 10")
    }
}
