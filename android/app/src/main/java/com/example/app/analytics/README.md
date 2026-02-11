# Analytics Package

Event logging and observability for the NavCaddy engine.

## Overview

This package provides comprehensive analytics tracking for the NavCaddy conversational navigation system. It tracks user interactions, system performance, and errors while maintaining strict privacy standards.

## Components

### AnalyticsEvent.kt
Event schema with 7 event types:
- `InputReceived` - User input (text/voice)
- `IntentClassified` - Intent classification results
- `RouteExecuted` - Navigation routing
- `ErrorOccurred` - System errors
- `VoiceTranscription` - Voice input results
- `ClarificationRequested` - Low-confidence scenarios
- `SuggestionSelected` - User selections

All events include:
- `timestamp: Long` - Event time
- `sessionId: String` - Session identifier

### NavCaddyAnalytics.kt
Singleton analytics service providing:
- Session management
- Latency tracking
- Event streaming
- PII redaction
- Console logging (DEBUG only)

**Injection:**
```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val analytics: NavCaddyAnalytics
) : ViewModel()
```

**Usage:**
```kotlin
// Start session
analytics.startSession()

// Track input
analytics.logInputReceived(
    inputType = AnalyticsEvent.InputReceived.InputType.TEXT,
    input = userInput
)

// Track with latency
analytics.startLatencyTracking("classification")
val result = classifier.classify(input)
val latency = analytics.stopLatencyTracking("classification")

analytics.logIntentClassified(
    intent = result.intent.name,
    confidence = result.confidence,
    latencyMs = latency,
    wasSuccessful = true
)

// Track errors
analytics.logError(
    errorType = AnalyticsEvent.ErrorOccurred.ErrorType.NETWORK_ERROR,
    message = "Connection failed",
    isRecoverable = true,
    throwable = exception
)
```

### PIIRedactor.kt
Utility for PII detection and redaction:
- Email addresses
- Phone numbers (US/international)
- Credit cards
- SSNs
- Street addresses
- Names (optional)

**Usage:**
```kotlin
// Redact text
val redacted = PIIRedactor.redact(userInput)

// Redact map values
val redactedParams = PIIRedactor.redactMap(parameters)

// Check for PII
if (PIIRedactor.containsPII(text)) {
    // Handle sensitive data
}
```

### IntentTraceView.kt
Debug UI for QA builds (only visible when `BuildConfig.DEBUG == true`):
- Floating bug icon button
- Bottom sheet with event list
- Visual latency indicators
- Color-coded event types

**Usage:**
```kotlin
@Composable
fun MyScreen(viewModel: MyViewModel) {
    Box {
        // Main content
        MainContent()

        // Debug overlay (only in DEBUG builds)
        IntentTraceView(
            analytics = viewModel.getAnalytics(),
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

## Privacy & Compliance

### PII Protection
All potentially sensitive data is redacted before logging:
- User input (emails, phones, names)
- Error messages
- Route parameters

### Data Minimization
Only essential metadata is logged:
- Input length (not full text)
- Operation latencies
- Success/failure indicators
- Session IDs (short-lived)

### User Control
- Session IDs are ephemeral (cleared with conversation)
- No persistent storage by default
- Debug view only in QA builds

## Performance

Minimal overhead:
- Event logging: ~1ms per event
- PII redaction: ~5-10ms when needed
- Latency tracking: <1ms
- Total pipeline overhead: ~10-15ms per interaction

## Integration

### ConversationViewModel Example
```kotlin
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val analytics: NavCaddyAnalytics,
    // ... other dependencies
) : ViewModel() {

    init {
        analytics.startSession()
    }

    private fun processUserInput(input: String) {
        viewModelScope.launch {
            // Log input
            analytics.logInputReceived(
                inputType = AnalyticsEvent.InputReceived.InputType.TEXT,
                input = input
            )

            // Track classification latency
            analytics.startLatencyTracking("classification")
            val result = classifier.classify(input)
            val classificationLatency = analytics.stopLatencyTracking("classification")

            // Log classification
            analytics.logIntentClassified(
                intent = result.intent.name,
                confidence = result.confidence,
                latencyMs = classificationLatency,
                wasSuccessful = true
            )

            // Track routing latency
            analytics.startLatencyTracking("routing")
            val route = orchestrator.route(result)
            val routingLatency = analytics.stopLatencyTracking("routing")

            // Log routing
            analytics.logRouteExecuted(
                module = route.module.name,
                screen = route.screen,
                latencyMs = routingLatency,
                parameters = route.parameters
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        analytics.clearTracking()
    }
}
```

## Event Stream

The `eventStream` is a `SharedFlow` with:
- Replay buffer: 50 events
- Extra buffer capacity: 100 events

**Collecting Events:**
```kotlin
viewModelScope.launch {
    analytics.eventStream.collect { event ->
        when (event) {
            is AnalyticsEvent.ErrorOccurred -> handleError(event)
            is AnalyticsEvent.IntentClassified -> trackMetric(event)
            // ... handle other events
        }
    }
}
```

## Extending with Backend Integration

To add analytics backends (Firebase, Amplitude, etc.):

```kotlin
// In NavCaddyAnalytics.kt
private fun logEvent(event: AnalyticsEvent) {
    // Existing: Stream and console
    _eventStream.tryEmit(event)
    if (BuildConfig.DEBUG) {
        logToConsole(event)
    }

    // Add: Backend integration
    sendToBackend(event)
}

private fun sendToBackend(event: AnalyticsEvent) {
    when (BuildConfig.ENVIRONMENT) {
        "production" -> {
            firebaseAnalytics.logEvent(event.toFirebaseEvent())
            amplitude.track(event.toAmplitudeEvent())
        }
        "staging" -> {
            customBackend.sendEvent(event)
        }
        "development" -> {
            // Local only
        }
    }
}
```

## Testing

### Unit Tests
- `PIIRedactorTest`: 18 tests for redaction patterns
- `NavCaddyAnalyticsTest`: 16 tests for analytics service

### Manual Testing
1. Run app in debug mode
2. Open conversation screen
3. Tap bug icon (bottom-right)
4. Verify events appear in trace panel
5. Check latency bars and colors
6. Verify PII is redacted

### Test Cases
```kotlin
// Test PII redaction
@Test
fun `redact email addresses`() {
    val input = "Contact me at john@example.com"
    val redacted = PIIRedactor.redact(input)
    assertEquals("Contact me at [EMAIL_REDACTED]", redacted)
}

// Test event emission
@Test
fun `logInputReceived emits event to stream`() = runTest {
    analytics.eventStream.test {
        analytics.logInputReceived(
            inputType = AnalyticsEvent.InputReceived.InputType.TEXT,
            input = "test"
        )
        val event = awaitItem() as AnalyticsEvent.InputReceived
        assertEquals(4, event.inputLength)
    }
}
```

## Debug Trace View

### Features
- Toggle with floating button
- Last 20 events (reversed)
- Session ID display
- Event details by type
- Visual latency bars

### Latency Colors
- Green: < 1s (fast)
- Yellow: 1-2s (acceptable)
- Orange: 2-3s (slow)
- Red: > 3s (very slow)

### Event Colors
- Green: InputReceived
- Blue: IntentClassified
- Purple: RouteExecuted
- Red: ErrorOccurred
- Orange: VoiceTranscription
- Yellow: ClarificationRequested
- Cyan: SuggestionSelected

## Best Practices

1. **Always redact PII**
   ```kotlin
   // Good
   analytics.logInputReceived(TEXT, input)  // Automatically redacted

   // Bad
   println("User input: $input")  // Raw input in logs
   ```

2. **Track latencies**
   ```kotlin
   // Good
   analytics.startLatencyTracking("operation")
   doOperation()
   val latency = analytics.stopLatencyTracking("operation")

   // Bad
   val start = System.currentTimeMillis()
   doOperation()
   val latency = System.currentTimeMillis() - start  // Not tracked
   ```

3. **Use try-finally for cleanup**
   ```kotlin
   try {
       analytics.startLatencyTracking("operation")
       doOperation()
   } finally {
       analytics.stopLatencyTracking("operation")
   }
   ```

4. **Log errors with context**
   ```kotlin
   // Good
   analytics.logError(
       errorType = NETWORK_ERROR,
       message = "Failed to connect",
       isRecoverable = true,
       throwable = exception
   )

   // Bad
   analytics.logError(UNKNOWN, "Error", false)
   ```

## Common Patterns

### Pattern 1: Track full pipeline
```kotlin
suspend fun processRequest(input: String) {
    // Input
    analytics.logInputReceived(TEXT, input)

    // Classification
    analytics.startLatencyTracking("classification")
    val intent = classifier.classify(input)
    analytics.logIntentClassified(
        intent = intent.name,
        confidence = intent.confidence,
        latencyMs = analytics.stopLatencyTracking("classification"),
        wasSuccessful = true
    )

    // Routing
    analytics.startLatencyTracking("routing")
    val route = orchestrator.route(intent)
    analytics.logRouteExecuted(
        module = route.module,
        screen = route.screen,
        latencyMs = analytics.stopLatencyTracking("routing")
    )
}
```

### Pattern 2: Handle errors gracefully
```kotlin
try {
    analytics.startLatencyTracking("operation")
    val result = doOperation()
    analytics.logSuccess(result)
} catch (e: Exception) {
    analytics.logError(
        errorType = mapException(e),
        message = e.message ?: "Unknown error",
        isRecoverable = e is RecoverableException,
        throwable = e
    )
} finally {
    analytics.stopLatencyTracking("operation")
    analytics.clearTracking()
}
```

### Pattern 3: Session lifecycle
```kotlin
class ConversationViewModel @Inject constructor(
    private val analytics: NavCaddyAnalytics
) : ViewModel() {

    init {
        analytics.startSession()
    }

    fun clearConversation() {
        // Start new session for new conversation
        analytics.startSession()
    }

    override fun onCleared() {
        super.onCleared()
        analytics.clearTracking()
    }
}
```

## Troubleshooting

### Events not appearing in trace view
1. Check `BuildConfig.DEBUG == true`
2. Verify analytics is injected
3. Check trace view is rendered
4. Look for errors in console

### PII not being redacted
1. Verify using `PIIRedactor.redact()` before logging
2. Check regex patterns in `PIIRedactor.kt`
3. Test with `PIIRedactorTest`

### High latencies
1. Check trace view latency bars
2. Identify slow operations (red bars)
3. Add more granular tracking
4. Profile with Android Profiler

### Memory leaks
1. Call `clearTracking()` in `onCleared()`
2. Check event stream subscribers
3. Use `viewModelScope` for collection

## Migration Guide

### From manual logging
```kotlin
// Before
println("User input: $input")
val start = System.currentTimeMillis()
val result = classifier.classify(input)
println("Classification took ${System.currentTimeMillis() - start}ms")

// After
analytics.logInputReceived(TEXT, input)
analytics.startLatencyTracking("classification")
val result = classifier.classify(input)
analytics.logIntentClassified(
    intent = result.intent.name,
    confidence = result.confidence,
    latencyMs = analytics.stopLatencyTracking("classification"),
    wasSuccessful = true
)
```

### Adding new event types
1. Add to `AnalyticsEvent.kt` sealed interface
2. Add logging method to `NavCaddyAnalytics.kt`
3. Add UI handling to `IntentTraceView.kt`
4. Add tests to `NavCaddyAnalyticsTest.kt`

## References

- Spec: `specs/navcaddy-engine.md` R8
- Plan: `specs/navcaddy-engine-plan.md` Task 22
- Tests: `app/src/test/java/com/example/app/analytics/`
- Integration: `ConversationViewModel.kt`

## Support

For questions or issues:
1. Check test files for examples
2. Review implementation summary: `TASK_22_IMPLEMENTATION_SUMMARY.md`
3. Check architecture diagram: `TASK_22_ARCHITECTURE.md`
