package caddypro.domain.navcaddy.context

import caddypro.domain.navcaddy.models.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for ContextInjector.
 *
 * Spec reference: navcaddy-engine.md R6, navcaddy-engine-plan.md Task 15
 */
class ContextInjectorTest {

    @Test
    fun `buildContextPrompt returns empty string for empty context`() {
        val context = SessionContext.empty
        val prompt = ContextInjector.buildContextPrompt(context)

        assertEquals("", prompt)
    }

    @Test
    fun `buildContextPrompt includes round information`() {
        val context = SessionContext(
            currentRound = Round(
                id = "round-123",
                startTime = 1234567890L,
                courseName = "Pebble Beach"
            )
        )

        val prompt = ContextInjector.buildContextPrompt(context)

        assertTrue(prompt.contains("Round Information"))
        assertTrue(prompt.contains("Pebble Beach"))
        assertTrue(prompt.contains("round-123"))
    }

    @Test
    fun `buildContextPrompt includes current hole`() {
        val context = SessionContext(
            currentHole = 7
        )

        val prompt = ContextInjector.buildContextPrompt(context)

        assertTrue(prompt.contains("Current Position"))
        assertTrue(prompt.contains("Hole: 7"))
    }

    @Test
    fun `buildContextPrompt includes last shot details`() {
        val shot = Shot(
            id = "shot-1",
            timestamp = System.currentTimeMillis(),
            club = Club(name = "7-iron", type = ClubType.IRON, estimatedCarry = 150),
            missDirection = MissDirection.PUSH_RIGHT,
            lie = Lie.FAIRWAY,
            pressureContext = PressureContext.PRESSURE
        )

        val context = SessionContext(lastShot = shot)
        val prompt = ContextInjector.buildContextPrompt(context)

        assertTrue(prompt.contains("Last Shot"))
        assertTrue(prompt.contains("7-iron"))
        assertTrue(prompt.contains("PUSH_RIGHT"))
        assertTrue(prompt.contains("FAIRWAY"))
        assertTrue(prompt.contains("PRESSURE"))
    }

    @Test
    fun `buildContextPrompt includes last recommendation`() {
        val context = SessionContext(
            lastRecommendation = "Take one more club due to headwind"
        )

        val prompt = ContextInjector.buildContextPrompt(context)

        assertTrue(prompt.contains("Last Recommendation"))
        assertTrue(prompt.contains("Take one more club due to headwind"))
    }

    @Test
    fun `buildContextPrompt includes conversation history`() {
        val history = listOf(
            ConversationTurn(Role.USER, "What club for 150 yards?", System.currentTimeMillis()),
            ConversationTurn(Role.ASSISTANT, "I recommend your 7-iron", System.currentTimeMillis())
        )

        val context = SessionContext(conversationHistory = history)
        val prompt = ContextInjector.buildContextPrompt(context)

        assertTrue(prompt.contains("Recent Conversation"))
        assertTrue(prompt.contains("User: What club for 150 yards?"))
        assertTrue(prompt.contains("Assistant: I recommend your 7-iron"))
    }

    @Test
    fun `buildContextPrompt combines all context elements`() {
        val shot = Shot(
            id = "shot-1",
            timestamp = System.currentTimeMillis(),
            club = Club(name = "PW", type = ClubType.WEDGE, estimatedCarry = 110),
            lie = Lie.ROUGH,
            pressureContext = PressureContext.NORMAL
        )

        val history = listOf(
            ConversationTurn(Role.USER, "Tough lie here", System.currentTimeMillis())
        )

        val context = SessionContext(
            currentRound = Round(
                id = "round-1",
                startTime = System.currentTimeMillis(),
                courseName = "Augusta National"
            ),
            currentHole = 12,
            lastShot = shot,
            lastRecommendation = "Play it safe to the fat part of the green",
            conversationHistory = history
        )

        val prompt = ContextInjector.buildContextPrompt(context)

        // Verify all sections present
        assertTrue(prompt.contains("Round Information"))
        assertTrue(prompt.contains("Current Position"))
        assertTrue(prompt.contains("Last Shot"))
        assertTrue(prompt.contains("Last Recommendation"))
        assertTrue(prompt.contains("Recent Conversation"))
    }

    @Test
    fun `buildContextSummary returns no active session for empty context`() {
        val context = SessionContext.empty
        val summary = ContextInjector.buildContextSummary(context)

        assertEquals("No active session", summary)
    }

    @Test
    fun `buildContextSummary includes course and hole`() {
        val context = SessionContext(
            currentRound = Round(
                id = "round-1",
                startTime = System.currentTimeMillis(),
                courseName = "Pebble Beach"
            ),
            currentHole = 7
        )

        val summary = ContextInjector.buildContextSummary(context)

        assertTrue(summary.contains("Pebble Beach"))
        assertTrue(summary.contains("Hole 7"))
    }

    @Test
    fun `buildContextSummary includes last club`() {
        val shot = Shot(
            id = "shot-1",
            timestamp = System.currentTimeMillis(),
            club = Club(name = "Driver", type = ClubType.DRIVER, estimatedCarry = 250),
            lie = Lie.TEE,
            pressureContext = PressureContext.NORMAL
        )

        val context = SessionContext(lastShot = shot)
        val summary = ContextInjector.buildContextSummary(context)

        assertTrue(summary.contains("Last: Driver"))
    }

    @Test
    fun `extractContextHints returns empty map for empty context`() {
        val context = SessionContext.empty
        val hints = ContextInjector.extractContextHints(context)

        assertTrue(hints.isEmpty())
    }

    @Test
    fun `extractContextHints includes last shot details`() {
        val shot = Shot(
            id = "shot-1",
            timestamp = System.currentTimeMillis(),
            club = Club(name = "5-iron", type = ClubType.IRON, estimatedCarry = 170),
            missDirection = MissDirection.SLICE,
            lie = Lie.ROUGH,
            pressureContext = PressureContext.NORMAL
        )

        val context = SessionContext(lastShot = shot)
        val hints = ContextInjector.extractContextHints(context)

        assertEquals("5-iron", hints["last_club"])
        assertEquals("SLICE", hints["last_miss"])
        assertEquals("ROUGH", hints["last_lie"])
    }

    @Test
    fun `extractContextHints includes round information`() {
        val context = SessionContext(
            currentRound = Round(
                id = "round-1",
                startTime = System.currentTimeMillis(),
                courseName = "St Andrews"
            ),
            currentHole = 18
        )

        val hints = ContextInjector.extractContextHints(context)

        assertEquals("18", hints["current_hole"])
        assertEquals("St Andrews", hints["course"])
    }

    @Test
    fun `extractContextHints includes last recommendation`() {
        val context = SessionContext(
            lastRecommendation = "Favor the right side of the green"
        )

        val hints = ContextInjector.extractContextHints(context)

        assertEquals("Favor the right side of the green", hints["last_recommendation"])
    }

    @Test
    fun `buildFollowUpContext returns empty for no history`() {
        val context = SessionContext.empty
        val followUpContext = ContextInjector.buildFollowUpContext(context)

        assertEquals("", followUpContext)
    }

    @Test
    fun `buildFollowUpContext includes last exchange`() {
        val history = listOf(
            ConversationTurn(Role.USER, "What club for 175?", System.currentTimeMillis()),
            ConversationTurn(Role.ASSISTANT, "Take your 6-iron", System.currentTimeMillis() + 1)
        )

        val context = SessionContext(conversationHistory = history)
        val followUpContext = ContextInjector.buildFollowUpContext(context)

        assertTrue(followUpContext.contains("Last exchange:"))
        assertTrue(followUpContext.contains("User: What club for 175?"))
        assertTrue(followUpContext.contains("Assistant: Take your 6-iron"))
    }

    @Test
    fun `buildFollowUpContext returns empty when missing user input`() {
        val history = listOf(
            ConversationTurn(Role.ASSISTANT, "Welcome!", System.currentTimeMillis())
        )

        val context = SessionContext(conversationHistory = history)
        val followUpContext = ContextInjector.buildFollowUpContext(context)

        assertEquals("", followUpContext)
    }

    @Test
    fun `buildFollowUpContext returns empty when missing assistant response`() {
        val history = listOf(
            ConversationTurn(Role.USER, "Hello", System.currentTimeMillis())
        )

        val context = SessionContext(conversationHistory = history)
        val followUpContext = ContextInjector.buildFollowUpContext(context)

        assertEquals("", followUpContext)
    }

    @Test
    fun `buildFollowUpContext uses most recent exchange`() {
        val history = listOf(
            ConversationTurn(Role.USER, "First question", System.currentTimeMillis()),
            ConversationTurn(Role.ASSISTANT, "First answer", System.currentTimeMillis() + 1),
            ConversationTurn(Role.USER, "Second question", System.currentTimeMillis() + 2),
            ConversationTurn(Role.ASSISTANT, "Second answer", System.currentTimeMillis() + 3)
        )

        val context = SessionContext(conversationHistory = history)
        val followUpContext = ContextInjector.buildFollowUpContext(context)

        assertTrue(followUpContext.contains("Second question"))
        assertTrue(followUpContext.contains("Second answer"))
        assertFalse(followUpContext.contains("First question"))
    }
}
