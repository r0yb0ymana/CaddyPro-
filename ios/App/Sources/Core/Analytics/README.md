# NavCaddy Analytics - iOS

Event tracking and observability for the NavCaddy intent pipeline.

## Overview

This package provides:
- **Event logging** with session correlation
- **Latency tracking** at each pipeline stage
- **PII redaction** for privacy-safe logs
- **Debug trace view** for QA builds
- **Protocol-based design** for pluggable implementations

## Quick Start

### Basic Usage

```swift
// Get analytics from dependency container
let analytics = dependencies.analytics

// Generate session ID for correlation
let sessionId = UUID().uuidString

// Log an event
analytics.log(.inputReceived(InputReceivedEvent(
    timestamp: Date(),
    inputType: .text,
    sessionId: sessionId
)))

// Track latency
analytics.trackLatency(
    operation: "intent_classification",
    latencyMs: 1234,
    sessionId: sessionId
)
```

### Automatic Latency Tracking

```swift
// Track execution time automatically
let result = await analytics.track(
    operation: "llm_classification",
    sessionId: sessionId
) {
    // Your async operation
    return await classifier.classify(input)
}
// Latency is automatically measured and logged
```

## Event Types

### 1. InputReceived
User input received (text or voice).

```swift
analytics.log(.inputReceived(InputReceivedEvent(
    timestamp: Date(),
    inputType: .text, // or .voice
    sessionId: sessionId
)))
```

### 2. IntentClassified
Intent classification completed by LLM.

```swift
analytics.log(.intentClassified(IntentClassifiedEvent(
    intent: "club_adjustment",
    confidence: 0.92,
    latencyMs: 1234,
    sessionId: sessionId
)))
```

### 3. RouteExecuted
Navigation triggered to a module/screen.

```swift
analytics.log(.routeExecuted(RouteExecutedEvent(
    module: "CADDY",
    screen: "club_selection",
    latencyMs: 45,
    sessionId: sessionId
)))
```

### 4. ErrorOccurred
Error in the pipeline.

```swift
analytics.log(.errorOccurred(ErrorOccurredEvent(
    errorType: .network,
    message: "Connection failed",
    isRecoverable: true,
    sessionId: sessionId
)))
```

**Error Types**:
- `.network` - Network connectivity issues
- `.timeout` - Request timeout
- `.transcription` - Voice transcription failed
- `.classification` - Intent classification failed
- `.routing` - Navigation routing failed
- `.unknown` - Unknown error

### 5. VoiceTranscription
Voice transcription completed.

```swift
analytics.log(.voiceTranscription(VoiceTranscriptionEvent(
    latencyMs: 856,
    wordCount: 7,
    sessionId: sessionId
)))
```

## PII Redaction

All logged text is automatically redacted for PII:

```swift
let text = "Email me at john@example.com"
let redacted = PIIRedactor.redact(text)
// Result: "Email me at [REDACTED_EMAIL]"
```

**Redacted PII Types**:
- Email addresses: `[REDACTED_EMAIL]`
- Phone numbers: `[REDACTED_PHONE]`
- Credit cards: `[REDACTED_CARD]`
- SSNs: `[REDACTED_SSN]`

**Selective Redaction**:
```swift
PIIRedactor.redact(text, types: [.email, .phone])
```

## Debug Trace View

View real-time events and latency breakdown in QA builds:

```swift
#if DEBUG
@State private var showTrace = false

// In your view
.sheet(isPresented: $showTrace) {
    IntentTraceView(
        analytics: dependencies.analytics as! ConsoleAnalytics,
        sessionId: sessionId
    )
}
#endif
```

**Features**:
- Real-time event list
- Latency breakdown (P50, P95, mean)
- Visual latency bars (color-coded)
- Session info
- Clear events button

## Implementations

### ConsoleAnalytics (DEBUG)
In-memory analytics with console logging.

```swift
let analytics = ConsoleAnalytics(
    enablePIIRedaction: true,
    verbose: true
)
```

**Features**:
- Stores events in memory
- Logs to console
- Latency statistics (P50, P95)
- PII redaction

**Usage**: Automatically used in DEBUG builds via DependencyContainer.

### NoOpAnalytics (Release)
Zero-overhead no-op implementation.

```swift
let analytics = NoOpAnalytics()
```

**Features**:
- No storage
- No logging
- Zero overhead
- Maximum privacy

**Usage**: Automatically used in release builds via DependencyContainer.

## Architecture

### Protocol-Based Design

```swift
protocol NavCaddyAnalytics {
    func log(_ event: AnalyticsEvent)
    func trackLatency(operation: String, latencyMs: Int, sessionId: String)
    func getEvents(for sessionId: String) -> [AnalyticsEvent]
    func clearEvents()
}
```

This allows:
- Easy testing with mock implementations
- Pluggable backends (console, remote, etc.)
- No-op implementation for production

### Session Correlation

All events include a `sessionId` for correlation:

```swift
let sessionId = UUID().uuidString

// All events in this session share the same ID
analytics.log(.inputReceived(..., sessionId: sessionId))
analytics.log(.intentClassified(..., sessionId: sessionId))
analytics.log(.routeExecuted(..., sessionId: sessionId))

// Retrieve all events for this session
let events = analytics.getEvents(for: sessionId)
```

## Integration with ConversationViewModel

The ConversationViewModel automatically tracks:
1. **Input received** (text/voice)
2. **Intent classification** latency
3. **Routing execution** latency
4. **Total pipeline** latency
5. **Errors** at each stage

Example from ConversationViewModel:

```swift
// Track input
analytics.log(.inputReceived(InputReceivedEvent(
    timestamp: Date(),
    inputType: .text,
    sessionId: sessionId
)))

// Measure classification
let startTime = Date()
let result = await classifier.classify(input, context)
let latency = Int(Date().timeIntervalSince(startTime) * 1000)

analytics.trackLatency(
    operation: "intent_classification",
    latencyMs: latency,
    sessionId: sessionId
)

analytics.log(.intentClassified(IntentClassifiedEvent(
    intent: intent.intentType.rawValue,
    confidence: intent.confidence,
    latencyMs: latency,
    sessionId: sessionId
)))
```

## Latency Statistics

ConsoleAnalytics automatically calculates:

```swift
if let stats = analytics.getLatencyStats(for: "intent_classification") {
    print("Count: \(stats.count)")
    print("Mean: \(stats.mean)ms")
    print("P50: \(stats.p50)ms")
    print("P95: \(stats.p95)ms")
    print("Min: \(stats.min)ms")
    print("Max: \(stats.max)ms")
}
```

**Common Operations**:
- `voice_transcription` - Voice to text latency
- `intent_classification` - LLM classification latency
- `routing` - Navigation routing latency
- `total_pipeline` - End-to-end latency

## Privacy & Compliance

### Maximum Privacy (Production)
- NoOpAnalytics in release builds
- No events logged
- No data stored
- Zero overhead

### Debug Privacy
- PII redaction enabled by default
- No raw voice audio stored
- Session IDs (not user IDs)
- In-memory only (no persistence)

### Compliance
- R8: Observability requirements ✅
- R9: Safety & compliance ✅
- Q7: No audit logging (resolved) ✅

## Testing

### Unit Tests

```swift
@MainActor
func testEventLogging() {
    let analytics = ConsoleAnalytics()
    let sessionId = UUID().uuidString

    analytics.log(.inputReceived(InputReceivedEvent(
        timestamp: Date(),
        inputType: .text,
        sessionId: sessionId
    )))

    let events = analytics.getEvents(for: sessionId)
    XCTAssertEqual(events.count, 1)
}
```

### Mock Analytics

```swift
class MockAnalytics: NavCaddyAnalytics {
    var loggedEvents: [AnalyticsEvent] = []

    func log(_ event: AnalyticsEvent) {
        loggedEvents.append(event)
    }

    // Implement other protocol methods
}
```

## Best Practices

### 1. Always Use Session IDs
```swift
// Generate once per conversation session
let sessionId = UUID().uuidString

// Use consistently across all events
analytics.log(.inputReceived(..., sessionId: sessionId))
```

### 2. Measure Latency at Key Stages
```swift
let startTime = Date()
// ... operation ...
let latency = Int(Date().timeIntervalSince(startTime) * 1000)
analytics.trackLatency(operation: "operation_name", latencyMs: latency, sessionId: sessionId)
```

### 3. Log Errors with Context
```swift
catch {
    analytics.log(.errorOccurred(ErrorOccurredEvent(
        errorType: .network,
        message: error.localizedDescription,
        isRecoverable: true,
        sessionId: sessionId
    )))
}
```

### 4. Use Automatic Tracking Helper
```swift
let result = await analytics.track(operation: "operation", sessionId: sessionId) {
    await doAsyncWork()
}
```

### 5. Clear Events Periodically (Debug)
```swift
// Clear after each test or debugging session
analytics.clearEvents()
```

## Troubleshooting

### Issue: Events not appearing in trace view
- Ensure you're running a DEBUG build
- Check that analytics is ConsoleAnalytics (not NoOpAnalytics)
- Verify sessionId matches between logging and viewing

### Issue: PII not redacted
- PII redaction is automatic in ConsoleAnalytics
- Check enablePIIRedaction parameter
- Verify regex patterns match your PII format

### Issue: Latency stats returning nil
- Ensure you've tracked at least one latency sample
- Check operation name matches exactly
- Verify analytics is ConsoleAnalytics

### Issue: Memory usage growing
- Call clearEvents() periodically
- ConsoleAnalytics is meant for short debugging sessions
- Consider limiting events stored (future enhancement)

## Future Enhancements

Potential improvements (not required for MVP):
- Remote analytics backend
- Persistent event storage
- Export logs to file
- Real-time monitoring dashboard
- ML-based PII detection
- Performance budgets with alerts

---

For more details, see:
- `TASK22_IMPLEMENTATION_SUMMARY.md` - Full implementation details
- `specs/navcaddy-engine.md` - R8 requirements
- `specs/navcaddy-engine-plan.md` - Task 22 plan
