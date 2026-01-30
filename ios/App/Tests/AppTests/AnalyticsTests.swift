import XCTest
@testable import App

/**
 * Unit tests for NavCaddy Analytics (Task 22)
 *
 * Tests:
 * - Event logging and schema
 * - PII redaction
 * - Latency tracking
 * - Session correlation
 * - Console analytics implementation
 *
 * Spec reference: navcaddy-engine.md R8
 * Plan reference: navcaddy-engine-plan.md Task 22
 */
@MainActor
final class AnalyticsTests: XCTestCase {
    // MARK: - Event Logging Tests

    func testInputReceivedEvent() {
        // Given
        let analytics = ConsoleAnalytics()
        let sessionId = UUID().uuidString

        // When
        analytics.log(.inputReceived(InputReceivedEvent(
            timestamp: Date(),
            inputType: .text,
            sessionId: sessionId
        )))

        // Then
        let events = analytics.getEvents(for: sessionId)
        XCTAssertEqual(events.count, 1)
        XCTAssertEqual(events.first?.name, "input_received")
        XCTAssertEqual(events.first?.sessionId, sessionId)
    }

    func testIntentClassifiedEvent() {
        // Given
        let analytics = ConsoleAnalytics()
        let sessionId = UUID().uuidString

        // When
        analytics.log(.intentClassified(IntentClassifiedEvent(
            intent: "club_adjustment",
            confidence: 0.92,
            latencyMs: 1234,
            sessionId: sessionId
        )))

        // Then
        let events = analytics.getEvents(for: sessionId)
        XCTAssertEqual(events.count, 1)
        XCTAssertEqual(events.first?.name, "intent_classified")

        if case .intentClassified(let event) = events.first {
            XCTAssertEqual(event.intent, "club_adjustment")
            XCTAssertEqual(event.confidence, 0.92, accuracy: 0.01)
            XCTAssertEqual(event.latencyMs, 1234)
        } else {
            XCTFail("Expected intentClassified event")
        }
    }

    func testRouteExecutedEvent() {
        // Given
        let analytics = ConsoleAnalytics()
        let sessionId = UUID().uuidString

        // When
        analytics.log(.routeExecuted(RouteExecutedEvent(
            module: "CADDY",
            screen: "club_selection",
            latencyMs: 45,
            sessionId: sessionId
        )))

        // Then
        let events = analytics.getEvents(for: sessionId)
        XCTAssertEqual(events.count, 1)

        if case .routeExecuted(let event) = events.first {
            XCTAssertEqual(event.module, "CADDY")
            XCTAssertEqual(event.screen, "club_selection")
            XCTAssertEqual(event.latencyMs, 45)
        } else {
            XCTFail("Expected routeExecuted event")
        }
    }

    func testErrorOccurredEvent() {
        // Given
        let analytics = ConsoleAnalytics()
        let sessionId = UUID().uuidString

        // When
        analytics.log(.errorOccurred(ErrorOccurredEvent(
            errorType: .network,
            message: "Connection failed",
            isRecoverable: true,
            sessionId: sessionId
        )))

        // Then
        let events = analytics.getEvents(for: sessionId)
        XCTAssertEqual(events.count, 1)

        if case .errorOccurred(let event) = events.first {
            XCTAssertEqual(event.errorType, .network)
            XCTAssertEqual(event.message, "Connection failed")
            XCTAssertTrue(event.isRecoverable)
        } else {
            XCTFail("Expected errorOccurred event")
        }
    }

    func testVoiceTranscriptionEvent() {
        // Given
        let analytics = ConsoleAnalytics()
        let sessionId = UUID().uuidString

        // When
        analytics.log(.voiceTranscription(VoiceTranscriptionEvent(
            latencyMs: 856,
            wordCount: 7,
            sessionId: sessionId
        )))

        // Then
        let events = analytics.getEvents(for: sessionId)
        XCTAssertEqual(events.count, 1)

        if case .voiceTranscription(let event) = events.first {
            XCTAssertEqual(event.latencyMs, 856)
            XCTAssertEqual(event.wordCount, 7)
        } else {
            XCTFail("Expected voiceTranscription event")
        }
    }

    // MARK: - Session Correlation Tests

    func testSessionCorrelation() {
        // Given
        let analytics = ConsoleAnalytics()
        let session1 = UUID().uuidString
        let session2 = UUID().uuidString

        // When
        analytics.log(.inputReceived(InputReceivedEvent(
            timestamp: Date(),
            inputType: .text,
            sessionId: session1
        )))

        analytics.log(.intentClassified(IntentClassifiedEvent(
            intent: "club_adjustment",
            confidence: 0.85,
            latencyMs: 1000,
            sessionId: session1
        )))

        analytics.log(.inputReceived(InputReceivedEvent(
            timestamp: Date(),
            inputType: .voice,
            sessionId: session2
        )))

        // Then
        let session1Events = analytics.getEvents(for: session1)
        let session2Events = analytics.getEvents(for: session2)

        XCTAssertEqual(session1Events.count, 2)
        XCTAssertEqual(session2Events.count, 1)
    }

    // MARK: - Latency Tracking Tests

    func testLatencyTracking() {
        // Given
        let analytics = ConsoleAnalytics()
        let sessionId = UUID().uuidString

        // When
        analytics.trackLatency(operation: "intent_classification", latencyMs: 1200, sessionId: sessionId)
        analytics.trackLatency(operation: "intent_classification", latencyMs: 1500, sessionId: sessionId)
        analytics.trackLatency(operation: "intent_classification", latencyMs: 900, sessionId: sessionId)

        // Then
        let stats = analytics.getLatencyStats(for: "intent_classification")
        XCTAssertNotNil(stats)
        XCTAssertEqual(stats?.count, 3)
        XCTAssertEqual(stats?.min, 900)
        XCTAssertEqual(stats?.max, 1500)
        XCTAssertEqual(stats?.mean, 1200)
    }

    func testLatencyStatsPercentiles() {
        // Given
        let analytics = ConsoleAnalytics()
        let sessionId = UUID().uuidString

        // When - add 100 samples with known distribution
        for i in 1...100 {
            analytics.trackLatency(operation: "test_op", latencyMs: i * 10, sessionId: sessionId)
        }

        // Then
        let stats = analytics.getLatencyStats(for: "test_op")
        XCTAssertNotNil(stats)
        XCTAssertEqual(stats?.count, 100)
        XCTAssertEqual(stats?.p50, 500) // 50th percentile
        XCTAssertEqual(stats?.p95, 950) // 95th percentile
    }

    func testClearEvents() {
        // Given
        let analytics = ConsoleAnalytics()
        let sessionId = UUID().uuidString

        analytics.log(.inputReceived(InputReceivedEvent(
            timestamp: Date(),
            inputType: .text,
            sessionId: sessionId
        )))

        analytics.trackLatency(operation: "test", latencyMs: 100, sessionId: sessionId)

        // When
        analytics.clearEvents()

        // Then
        let events = analytics.getEvents(for: sessionId)
        let stats = analytics.getLatencyStats(for: "test")

        XCTAssertEqual(events.count, 0)
        XCTAssertNil(stats)
    }

    // MARK: - Event Properties Tests

    func testEventPropertiesSerialization() {
        // Given
        let sessionId = UUID().uuidString
        let event = AnalyticsEvent.intentClassified(IntentClassifiedEvent(
            intent: "shot_recommendation",
            confidence: 0.87,
            latencyMs: 1456,
            sessionId: sessionId
        ))

        // When
        let properties = event.properties

        // Then
        XCTAssertNotNil(properties["timestamp"])
        XCTAssertEqual(properties["session_id"] as? String, sessionId)
        XCTAssertEqual(properties["intent"] as? String, "shot_recommendation")
        XCTAssertEqual(properties["confidence"] as? Double, 0.87)
        XCTAssertEqual(properties["latency_ms"] as? Int, 1456)
    }

    // MARK: - No-Op Analytics Tests

    func testNoOpAnalytics() {
        // Given
        let analytics = NoOpAnalytics()
        let sessionId = UUID().uuidString

        // When
        analytics.log(.inputReceived(InputReceivedEvent(
            timestamp: Date(),
            inputType: .text,
            sessionId: sessionId
        )))

        analytics.trackLatency(operation: "test", latencyMs: 100, sessionId: sessionId)

        // Then - no-op should not store anything
        let events = analytics.getEvents(for: sessionId)
        XCTAssertEqual(events.count, 0)
    }

    // MARK: - Async Tracking Helper Tests

    func testAsyncTrackingHelper() async {
        // Given
        let analytics = ConsoleAnalytics()
        let sessionId = UUID().uuidString

        // When
        let result = await analytics.track(operation: "async_op", sessionId: sessionId) {
            try? await Task.sleep(nanoseconds: 100_000_000) // 100ms
            return "success"
        }

        // Then
        XCTAssertEqual(result, "success")

        let stats = analytics.getLatencyStats(for: "async_op")
        XCTAssertNotNil(stats)
        XCTAssertEqual(stats?.count, 1)
        // Latency should be approximately 100ms (with some tolerance)
        XCTAssertGreaterThanOrEqual(stats?.min ?? 0, 90)
        XCTAssertLessThanOrEqual(stats?.max ?? 0, 200)
    }
}

// MARK: - PII Redaction Tests

final class PIIRedactorTests: XCTestCase {
    func testEmailRedaction() {
        // Given
        let text = "Contact me at john.doe@example.com for details"

        // When
        let redacted = PIIRedactor.redact(text)

        // Then
        XCTAssertEqual(redacted, "Contact me at [REDACTED_EMAIL] for details")
        XCTAssertFalse(redacted.contains("john.doe"))
        XCTAssertFalse(redacted.contains("example.com"))
    }

    func testPhoneNumberRedaction() {
        // Given
        let text1 = "Call me at (555) 123-4567"
        let text2 = "My number is 555-123-4567"
        let text3 = "International: +1-555-123-4567"

        // When
        let redacted1 = PIIRedactor.redact(text1)
        let redacted2 = PIIRedactor.redact(text2)
        let redacted3 = PIIRedactor.redact(text3)

        // Then
        XCTAssertTrue(redacted1.contains("[REDACTED_PHONE]"))
        XCTAssertTrue(redacted2.contains("[REDACTED_PHONE]"))
        XCTAssertTrue(redacted3.contains("[REDACTED_PHONE]"))
        XCTAssertFalse(redacted1.contains("555"))
    }

    func testCreditCardRedaction() {
        // Given
        let text = "My card is 4532-1234-5678-9012"

        // When
        let redacted = PIIRedactor.redact(text)

        // Then
        XCTAssertTrue(redacted.contains("[REDACTED_CARD]"))
        XCTAssertFalse(redacted.contains("4532"))
    }

    func testSSNRedaction() {
        // Given
        let text = "SSN: 123-45-6789"

        // When
        let redacted = PIIRedactor.redact(text)

        // Then
        XCTAssertTrue(redacted.contains("[REDACTED_SSN]"))
        XCTAssertFalse(redacted.contains("123-45"))
    }

    func testMultiplePIITypesRedaction() {
        // Given
        let text = "Email john@example.com, phone 555-123-4567, card 1234-5678-9012-3456"

        // When
        let redacted = PIIRedactor.redact(text)

        // Then
        XCTAssertTrue(redacted.contains("[REDACTED_EMAIL]"))
        XCTAssertTrue(redacted.contains("[REDACTED_PHONE]"))
        XCTAssertTrue(redacted.contains("[REDACTED_CARD]"))
        XCTAssertFalse(redacted.contains("john@example.com"))
        XCTAssertFalse(redacted.contains("555-123-4567"))
        XCTAssertFalse(redacted.contains("1234-5678"))
    }

    func testSelectiveRedaction() {
        // Given
        let text = "Email john@example.com, phone 555-123-4567"

        // When
        let redacted = PIIRedactor.redact(text, types: [.email])

        // Then
        XCTAssertTrue(redacted.contains("[REDACTED_EMAIL]"))
        XCTAssertTrue(redacted.contains("555-123-4567")) // Phone not redacted
    }

    func testGolfContentPreserved() {
        // Given
        let text = "I hit my 7-iron 150 yards with a slight fade"

        // When
        let redacted = PIIRedactor.redact(text)

        // Then - should not be modified (no PII detected)
        XCTAssertEqual(redacted, text)
    }

    func testNoFalsePositives() {
        // Given
        let text = "Score was 72-68-71 for a total of 211"

        // When
        let redacted = PIIRedactor.redact(text)

        // Then - golf scores should not be redacted
        XCTAssertEqual(redacted, text)
    }
}
