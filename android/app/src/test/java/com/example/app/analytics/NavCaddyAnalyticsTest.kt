package caddypro.analytics

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NavCaddyAnalytics.
 *
 * Spec reference: navcaddy-engine.md R8
 * Plan reference: navcaddy-engine-plan.md Task 22
 */
class NavCaddyAnalyticsTest {

    private lateinit var analytics: NavCaddyAnalytics

    @Before
    fun setup() {
        analytics = NavCaddyAnalytics()
    }

    @Test
    fun `startSession generates unique session ID`() {
        val sessionId1 = analytics.getSessionId()
        analytics.startSession()
        val sessionId2 = analytics.getSessionId()

        assertNotEquals(sessionId1, sessionId2)
    }

    @Test
    fun `logInputReceived emits event to stream`() = runTest {
        analytics.eventStream.test {
            analytics.logInputReceived(
                inputType = AnalyticsEvent.InputReceived.InputType.TEXT,
                input = "test input"
            )

            val event = awaitItem() as AnalyticsEvent.InputReceived
            assertEquals(AnalyticsEvent.InputReceived.InputType.TEXT, event.inputType)
            assertEquals(10, event.inputLength)
            assertEquals(analytics.getSessionId(), event.sessionId)
        }
    }

    @Test
    fun `logIntentClassified captures latency and confidence`() = runTest {
        analytics.eventStream.test {
            analytics.logIntentClassified(
                intent = "CLUB_ADJUSTMENT",
                confidence = 0.85f,
                latencyMs = 500L,
                wasSuccessful = true
            )

            val event = awaitItem() as AnalyticsEvent.IntentClassified
            assertEquals("CLUB_ADJUSTMENT", event.intent)
            assertEquals(0.85f, event.confidence)
            assertEquals(500L, event.latencyMs)
            assertTrue(event.wasSuccessful)
        }
    }

    @Test
    fun `logRouteExecuted redacts PII from parameters`() = runTest {
        analytics.eventStream.test {
            analytics.logRouteExecuted(
                module = "CADDY",
                screen = "ClubSelection",
                latencyMs = 100L,
                parameters = mapOf(
                    "club" to "7-iron",
                    "email" to "user@example.com"
                )
            )

            val event = awaitItem() as AnalyticsEvent.RouteExecuted
            assertEquals("CADDY", event.module)
            assertEquals("ClubSelection", event.screen)
            assertEquals("7-iron", event.parameters["club"])
            assertEquals("[EMAIL_REDACTED]", event.parameters["email"])
        }
    }

    @Test
    fun `logError captures error type and message`() = runTest {
        analytics.eventStream.test {
            analytics.logError(
                errorType = AnalyticsEvent.ErrorOccurred.ErrorType.NETWORK_ERROR,
                message = "Connection failed",
                isRecoverable = true
            )

            val event = awaitItem() as AnalyticsEvent.ErrorOccurred
            assertEquals(AnalyticsEvent.ErrorOccurred.ErrorType.NETWORK_ERROR, event.errorType)
            assertEquals("Connection failed", event.message)
            assertTrue(event.isRecoverable)
        }
    }

    @Test
    fun `logVoiceTranscription calculates word count`() = runTest {
        analytics.eventStream.test {
            analytics.logVoiceTranscription(
                latencyMs = 1200L,
                transcription = "How far is my seven iron",
                wasSuccessful = true
            )

            val event = awaitItem() as AnalyticsEvent.VoiceTranscription
            assertEquals(1200L, event.latencyMs)
            assertEquals(5, event.wordCount)
            assertTrue(event.wasSuccessful)
        }
    }

    @Test
    fun `logClarificationRequested redacts PII from input`() = runTest {
        analytics.eventStream.test {
            analytics.logClarificationRequested(
                originalInput = "Contact me at john@example.com",
                confidence = 0.45f,
                suggestionsCount = 3
            )

            val event = awaitItem() as AnalyticsEvent.ClarificationRequested
            assertTrue(event.originalInput.contains("[EMAIL_REDACTED]"))
            assertEquals(0.45f, event.confidence)
            assertEquals(3, event.suggestionsCount)
        }
    }

    @Test
    fun `logSuggestionSelected tracks selection`() = runTest {
        analytics.eventStream.test {
            analytics.logSuggestionSelected(
                intentType = "SHOT_RECOMMENDATION",
                suggestionIndex = 1
            )

            val event = awaitItem() as AnalyticsEvent.SuggestionSelected
            assertEquals("SHOT_RECOMMENDATION", event.intentType)
            assertEquals(1, event.suggestionIndex)
        }
    }

    @Test
    fun `latency tracking starts and stops correctly`() {
        analytics.startLatencyTracking("test_operation")
        Thread.sleep(100) // Wait for some time
        val elapsed = analytics.stopLatencyTracking("test_operation")

        assertTrue("Elapsed time should be at least 100ms", elapsed >= 100)
    }

    @Test
    fun `latency tracking returns zero for unknown operation`() {
        val elapsed = analytics.stopLatencyTracking("unknown_operation")
        assertEquals(0L, elapsed)
    }

    @Test
    fun `getElapsedTime returns time without stopping`() {
        analytics.startLatencyTracking("test_operation")
        Thread.sleep(50)
        val elapsed1 = analytics.getElapsedTime("test_operation")
        Thread.sleep(50)
        val elapsed2 = analytics.getElapsedTime("test_operation")

        assertTrue("Elapsed time should increase", elapsed2 > elapsed1)
    }

    @Test
    fun `clearTracking removes all operations`() {
        analytics.startLatencyTracking("op1")
        analytics.startLatencyTracking("op2")
        analytics.clearTracking()

        assertEquals(0L, analytics.getElapsedTime("op1"))
        assertEquals(0L, analytics.getElapsedTime("op2"))
    }

    @Test
    fun `eventStream replays recent events`() = runTest {
        // Log multiple events
        repeat(5) { index ->
            analytics.logInputReceived(
                inputType = AnalyticsEvent.InputReceived.InputType.TEXT,
                input = "test $index"
            )
        }

        // New subscriber should receive replayed events
        analytics.eventStream.test {
            val events = mutableListOf<AnalyticsEvent>()
            // Collect available items
            while (!isEmpty) {
                events.add(awaitItem())
            }

            assertTrue("Should have replayed events", events.size >= 5)
        }
    }

    @Test
    fun `session ID persists across multiple events`() = runTest {
        val sessionId = analytics.getSessionId()

        analytics.eventStream.test {
            analytics.logInputReceived(
                inputType = AnalyticsEvent.InputReceived.InputType.TEXT,
                input = "test"
            )
            analytics.logIntentClassified(
                intent = "TEST",
                confidence = 0.9f,
                latencyMs = 100L,
                wasSuccessful = true
            )

            val event1 = awaitItem()
            val event2 = awaitItem()

            assertEquals(sessionId, event1.sessionId)
            assertEquals(sessionId, event2.sessionId)
        }
    }

    // Task 21: Shot logger analytics tests

    @Test
    fun `logShotLoggerOpened emits event to stream`() = runTest {
        analytics.eventStream.test {
            analytics.logShotLoggerOpened()

            val event = awaitItem() as AnalyticsEvent.ShotLoggerOpened
            assertEquals(analytics.getSessionId(), event.sessionId)
        }
    }

    @Test
    fun `logClubSelected captures latency`() = runTest {
        analytics.eventStream.test {
            analytics.logClubSelected(clubType = "DRIVER", latencyMs = 500L)

            val event = awaitItem() as AnalyticsEvent.ClubSelected
            assertEquals("DRIVER", event.clubType)
            assertEquals(500L, event.latencyMs)
            assertEquals(analytics.getSessionId(), event.sessionId)
        }
    }

    @Test
    fun `logShotLogged captures total latency`() = runTest {
        analytics.eventStream.test {
            analytics.logShotLogged(
                clubType = "IRON_7",
                lie = "FAIRWAY",
                totalLatencyMs = 1500L
            )

            val event = awaitItem() as AnalyticsEvent.ShotLogged
            assertEquals("IRON_7", event.clubType)
            assertEquals("FAIRWAY", event.lie)
            assertEquals(1500L, event.totalLatencyMs)
            assertEquals(analytics.getSessionId(), event.sessionId)
        }
    }

    @Test
    fun `logShotSynced captures sync latency`() = runTest {
        analytics.eventStream.test {
            analytics.logShotSynced(shotId = "shot-123", latencyMs = 300L)

            val event = awaitItem() as AnalyticsEvent.ShotSynced
            assertEquals("shot-123", event.shotId)
            assertEquals(300L, event.latencyMs)
            assertEquals(analytics.getSessionId(), event.sessionId)
        }
    }
}
