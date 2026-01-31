package caddypro.domain.navcaddy.integration

import caddypro.domain.navcaddy.context.ContextInjector
import caddypro.domain.navcaddy.context.CourseConditions
import caddypro.domain.navcaddy.context.SessionContextManager
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.ClubType
import caddypro.domain.navcaddy.models.ConversationTurn
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.PressureContext
import caddypro.domain.navcaddy.models.Role
import caddypro.domain.navcaddy.models.Shot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

/**
 * Integration tests for session context management.
 *
 * Tests the interaction between SessionContextManager and ContextInjector
 * to verify conversation history, context persistence, and context injection
 * for LLM prompts.
 *
 * Task: Task 17 - Write Memory & Persona Integration Tests
 * Spec reference: navcaddy-engine.md R6
 */
class SessionContextIntegrationTest {

    private lateinit var sessionManager: SessionContextManager
    private lateinit var contextInjector: ContextInjector

    private val testClub = Club(
        id = "7-iron-id",
        name = "7-iron",
        type = ClubType.IRON,
        estimatedCarry = 150
    )

    @Before
    fun setup() {
        sessionManager = SessionContextManager()
        contextInjector = ContextInjector
    }

    // ========================================================================
    // Follow-up Query Handling Tests
    // ========================================================================

    @Test
    fun `follow-up queries use conversation history`() = runTest {
        // Set up initial conversation
        sessionManager.addConversationTurn(
            userInput = "What club should I use for 150 yards?",
            assistantResponse = "I'd recommend your 7-iron for that distance."
        )

        // Add follow-up
        sessionManager.addConversationTurn(
            userInput = "What if there's wind?",
            assistantResponse = "With wind, consider bumping up to a 6-iron."
        )

        val context = sessionManager.getCurrentContext()

        // Verify conversation history is tracked
        assertEquals("Should have 4 turns (2 user + 2 assistant)", 4, context.conversationHistory.size)

        // Verify turns are in order
        assertEquals(Role.USER, context.conversationHistory[0].role)
        assertEquals(Role.ASSISTANT, context.conversationHistory[1].role)
        assertEquals(Role.USER, context.conversationHistory[2].role)
        assertEquals(Role.ASSISTANT, context.conversationHistory[3].role)

        // Verify content
        assertTrue(
            "First user query should be about club",
            context.conversationHistory[0].content.contains("150 yards")
        )
        assertTrue(
            "Follow-up should reference wind",
            context.conversationHistory[2].content.contains("wind")
        )
    }

    @Test
    fun `recent conversation retrieved for context injection`() = runTest {
        // Add multiple conversation turns
        repeat(15) { index ->
            sessionManager.addConversationTurn(
                userInput = "User message $index",
                assistantResponse = "Assistant response $index"
            )
        }

        // Get recent conversation (last 10 turns = 5 exchanges)
        val recentTurns = sessionManager.getRecentConversation(10)

        assertEquals("Should return last 10 turns", 10, recentTurns.size)

        // Verify most recent turns are included
        assertTrue(
            "Should include most recent user input",
            recentTurns.any { it.content.contains("User message 14") }
        )
        assertFalse(
            "Should not include very old turns",
            recentTurns.any { it.content.contains("User message 0") }
        )
    }

    @Test
    fun `conversation history limits to recent turns`() = runTest {
        // Add many conversation turns
        repeat(20) { index ->
            sessionManager.addTurn(Role.USER, "Message $index")
        }

        val recentTurns = sessionManager.getRecentConversation(5)

        assertEquals("Should limit to requested count", 5, recentTurns.size)
        assertTrue(
            "Should include most recent messages",
            recentTurns.last().content.contains("Message 19")
        )
    }

    @Test
    fun `context injector formats conversation for LLM`() = runTest {
        // Set up conversation
        sessionManager.addConversationTurn(
            userInput = "What club for 150 yards?",
            assistantResponse = "7-iron should work."
        )

        sessionManager.addConversationTurn(
            userInput = "What about from the rough?",
            assistantResponse = "From rough, maybe go with 6-iron for extra distance."
        )

        val context = sessionManager.getCurrentContext()
        val promptContext = contextInjector.buildContextPrompt(context)

        // Verify context is included in prompt
        assertTrue(
            "Prompt should include conversation history",
            promptContext.contains("7-iron")
        )
        assertTrue(
            "Prompt should reference rough question",
            promptContext.contains("rough")
        )
    }

    // ========================================================================
    // Context Injection Format Tests
    // ========================================================================

    @Test
    fun `context injection includes round information`() = runTest {
        // Set up round context
        sessionManager.updateRound(
            roundId = "round-123",
            courseName = "Pebble Beach",
            startingHole = 1,
            startingPar = 4
        )

        sessionManager.updateHole(holeNumber = 7, par = 4)

        val context = sessionManager.getCurrentContext()
        val promptContext = contextInjector.buildContextPrompt(context)

        // Verify round info is in context
        assertTrue(
            "Context should include course name",
            promptContext.contains("Pebble Beach", ignoreCase = true)
        )
        assertTrue(
            "Context should include current hole",
            promptContext.contains("hole", ignoreCase = true) && promptContext.contains("7")
        )
    }

    @Test
    fun `context injection includes last shot information`() = runTest {
        val lastShot = Shot(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            club = testClub,
            missDirection = MissDirection.SLICE,
            lie = Lie.FAIRWAY,
            pressureContext = PressureContext(isUserTagged = false, isInferred = false)
        )

        sessionManager.recordShot(lastShot)

        val context = sessionManager.getCurrentContext()
        val promptContext = contextInjector.buildContextPrompt(context)

        // Verify last shot info is in context
        assertTrue(
            "Context should reference last shot",
            promptContext.contains("Last Shot", ignoreCase = true) ||
            promptContext.contains("7-iron", ignoreCase = true)
        )
    }

    @Test
    fun `context injection includes last recommendation`() = runTest {
        val recommendation = "Use your 7-iron and aim slightly left of the flag."
        sessionManager.recordRecommendation(recommendation)

        val context = sessionManager.getCurrentContext()
        val promptContext = contextInjector.buildContextPrompt(context)

        // Verify recommendation is referenced
        assertTrue(
            "Context should include last recommendation",
            promptContext.contains("7-iron") || promptContext.contains("left")
        )
    }

    @Test
    fun `context injection formats correctly for LLM`() = runTest {
        // Set up comprehensive context
        sessionManager.updateRound(
            roundId = "round-1",
            courseName = "Augusta National",
            startingHole = 1,
            startingPar = 4
        )

        sessionManager.addConversationTurn(
            userInput = "What's my best club here?",
            assistantResponse = "Based on distance, I'd suggest your 8-iron."
        )

        val context = sessionManager.getCurrentContext()
        val promptContext = contextInjector.buildContextPrompt(context)

        // Verify structure
        assertFalse("Context should not be empty", promptContext.isEmpty())
        assertTrue("Context should include course info", promptContext.contains("Augusta National"))

        // Verify formatting doesn't break prompt structure
        val lines = promptContext.lines()
        assertTrue("Should have multiple lines of context", lines.size > 1)
    }

    @Test
    fun `follow-up context extraction works correctly`() = runTest {
        sessionManager.addConversationTurn(
            userInput = "What club for 150 yards?",
            assistantResponse = "7-iron should work."
        )

        val context = sessionManager.getCurrentContext()
        val followUpContext = contextInjector.buildFollowUpContext(context)

        assertTrue("Follow-up context should include last exchange", followUpContext.contains("7-iron"))
        assertTrue("Follow-up context should have user input", followUpContext.contains("150 yards"))
    }

    // ========================================================================
    // Session State Persistence Tests
    // ========================================================================

    @Test
    fun `session state persists across interactions`() = runTest {
        // Initial setup
        sessionManager.updateRound(
            roundId = "round-1",
            courseName = "St Andrews",
            startingHole = 1
        )

        sessionManager.updateHole(5, 4)
        sessionManager.updateScore(totalScore = 22, holesCompleted = 4)

        // Simulate interaction
        sessionManager.addConversationTurn(
            userInput = "How am I doing?",
            assistantResponse = "You're playing well, 4 over through 4 holes."
        )

        // Verify state persists
        val context = sessionManager.getCurrentContext()
        assertNotNull("Round should be set", context.currentRound)
        assertEquals("Hole should be 5", 5, context.currentHole)
        assertTrue("History should be saved", context.conversationHistory.isNotEmpty())

        // Additional interaction
        sessionManager.addConversationTurn(
            userInput = "What club for this hole?",
            assistantResponse = "Try your driver."
        )

        val updatedContext = sessionManager.getCurrentContext()
        assertEquals("Should still be on hole 5", 5, updatedContext.currentHole)
        assertEquals("Should have 4 conversation turns", 4, updatedContext.conversationHistory.size)
    }

    @Test
    fun `session state accessible via flow`() = runTest {
        sessionManager.updateRound(
            roundId = "round-1",
            courseName = "Pebble Beach",
            startingHole = 1
        )

        // Access via StateFlow
        val initialContext = sessionManager.context.first()
        assertNotNull("Context should be available via flow", initialContext.currentRound)
        assertEquals("Course name should match", "Pebble Beach", initialContext.currentRound?.courseName)

        // Update and verify flow updates
        sessionManager.updateHole(2, 5)

        val updatedContext = sessionManager.context.first()
        assertEquals("Hole should be updated in flow", 2, updatedContext.currentHole)
    }

    @Test
    fun `session state tracks score progression`() = runTest {
        sessionManager.updateRound(
            roundId = "round-1",
            courseName = "Test Course",
            startingHole = 1
        )

        // Progress through holes
        sessionManager.updateScore(totalScore = 4, holesCompleted = 1)
        var roundState = sessionManager.getCurrentRoundState()
        assertEquals("Score after hole 1", 4, roundState?.totalScore)

        sessionManager.updateScore(totalScore = 9, holesCompleted = 2)
        roundState = sessionManager.getCurrentRoundState()
        assertEquals("Score after hole 2", 9, roundState?.totalScore)
        assertEquals("Holes completed", 2, roundState?.holesCompleted)
    }

    @Test
    fun `session state tracks course conditions`() = runTest {
        sessionManager.updateRound(
            roundId = "round-1",
            courseName = "Test Course",
            startingHole = 1
        )

        val conditions = CourseConditions(
            windSpeed = 15,
            windDirection = "NW",
            temperature = 72,
            weather = "Clear"
        )

        sessionManager.updateConditions(conditions)

        val roundState = sessionManager.getCurrentRoundState()
        assertNotNull("Conditions should be set", roundState?.conditions)
        assertEquals("Wind speed should match", 15, roundState?.conditions?.windSpeed)
        assertEquals("Wind direction should match", "NW", roundState?.conditions?.windDirection)
    }

    // ========================================================================
    // Clear Context Tests
    // ========================================================================

    @Test
    fun `clear session wipes all context components`() = runTest {
        // Set up full context
        sessionManager.updateRound(
            roundId = "round-1",
            courseName = "Test Course",
            startingHole = 1
        )

        sessionManager.addConversationTurn(
            userInput = "Test query",
            assistantResponse = "Test response"
        )

        sessionManager.recordRecommendation("Test recommendation")

        val shot = Shot(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            club = testClub,
            missDirection = MissDirection.SLICE,
            lie = Lie.FAIRWAY,
            pressureContext = PressureContext()
        )
        sessionManager.recordShot(shot)

        // Verify context exists
        var context = sessionManager.getCurrentContext()
        assertNotNull("Round should be set", context.currentRound)
        assertTrue("History should exist", context.conversationHistory.isNotEmpty())
        assertNotNull("Last shot should be set", context.lastShot)
        assertNotNull("Last recommendation should be set", context.lastRecommendation)

        // Clear session
        sessionManager.clearSession()

        // Verify everything is cleared
        context = sessionManager.getCurrentContext()
        assertNull("Round should be cleared", context.currentRound)
        assertNull("Current hole should be cleared", context.currentHole)
        assertTrue("History should be empty", context.conversationHistory.isEmpty())
        assertNull("Last shot should be cleared", context.lastShot)
        assertNull("Last recommendation should be cleared", context.lastRecommendation)

        // Verify round state is also cleared
        assertNull("Round state should be null", sessionManager.getCurrentRoundState())
        assertFalse("Should not have active round", sessionManager.hasActiveRound())
        assertFalse("Should not have conversation history", sessionManager.hasConversationHistory())
    }

    @Test
    fun `clear conversation history preserves round state`() = runTest {
        // Set up context
        sessionManager.updateRound(
            roundId = "round-1",
            courseName = "Test Course",
            startingHole = 1
        )

        sessionManager.updateHole(5, 4)

        sessionManager.addConversationTurn(
            userInput = "Test query",
            assistantResponse = "Test response"
        )

        // Clear only conversation
        sessionManager.clearConversationHistory()

        // Verify round state preserved
        val context = sessionManager.getCurrentContext()
        assertNotNull("Round should still be set", context.currentRound)
        assertEquals("Hole should still be 5", 5, context.currentHole)
        assertTrue("History should be empty", context.conversationHistory.isEmpty())
    }

    @Test
    fun `context injector handles empty context gracefully`() = runTest {
        val emptyContext = sessionManager.getCurrentContext()

        val promptContext = contextInjector.buildContextPrompt(emptyContext)

        // Should return empty string for empty context
        assertTrue("Prompt should be empty for empty context", promptContext.isEmpty())
    }

    // ========================================================================
    // Context Integration Tests
    // ========================================================================

    @Test
    fun `full conversation flow maintains context continuity`() = runTest {
        // Start round
        sessionManager.updateRound(
            roundId = "round-1",
            courseName = "Pebble Beach",
            startingHole = 1,
            startingPar = 4
        )

        // First interaction
        sessionManager.addConversationTurn(
            userInput = "What club for 150 yards?",
            assistantResponse = "Your 7-iron should be perfect."
        )

        var context = sessionManager.getCurrentContext()
        var prompt = contextInjector.buildContextPrompt(context)
        assertTrue("Context should reference 7-iron", prompt.contains("7-iron"))

        // Second interaction (follow-up)
        sessionManager.addConversationTurn(
            userInput = "What if there's wind?",
            assistantResponse = "With wind, go with your 6-iron."
        )

        // Record the shot
        val shot = Shot(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            club = Club(id = "6-iron-id", name = "6-iron", type = ClubType.IRON, estimatedCarry = 160),
            missDirection = MissDirection.SLICE,
            lie = Lie.FAIRWAY,
            pressureContext = PressureContext()
        )
        sessionManager.recordShot(shot)

        // Third interaction (referencing result)
        context = sessionManager.getCurrentContext()
        prompt = contextInjector.buildContextPrompt(context)

        // Verify full context is available
        assertTrue("Should reference conversation history", prompt.contains("6-iron") || prompt.contains("wind"))
        assertTrue("Should include last shot", prompt.contains("Last Shot") || prompt.contains("SLICE"))
    }

    @Test
    fun `context supports multi-hole tracking`() = runTest {
        sessionManager.updateRound(
            roundId = "round-1",
            courseName = "Augusta National",
            startingHole = 1,
            startingPar = 4
        )

        // Play through multiple holes
        for (hole in 1..5) {
            sessionManager.updateHole(hole, 4)
            sessionManager.addConversationTurn(
                userInput = "What's the play on hole $hole?",
                assistantResponse = "Recommendation for hole $hole"
            )
        }

        val context = sessionManager.getCurrentContext()

        // Verify current hole
        assertEquals("Should be on hole 5", 5, context.currentHole)

        // Verify history includes all holes
        assertEquals("Should have 10 turns (5 holes × 2 turns)", 10, context.conversationHistory.size)

        // Get recent conversation for context
        val recentTurns = sessionManager.getRecentConversation(6)
        assertTrue(
            "Recent turns should include hole 5",
            recentTurns.any { it.content.contains("hole 5") }
        )
    }

    @Test
    fun `session state helpers work correctly`() = runTest {
        // Initially no active round
        assertFalse("Should not have active round", sessionManager.hasActiveRound())
        assertFalse("Should not have conversation history", sessionManager.hasConversationHistory())

        // Start round
        sessionManager.updateRound("round-1", "Test Course")
        assertTrue("Should have active round", sessionManager.hasActiveRound())

        // Add conversation
        sessionManager.addTurn(Role.USER, "Test message")
        assertTrue("Should have conversation history", sessionManager.hasConversationHistory())

        // Clear
        sessionManager.clearSession()
        assertFalse("Should not have active round after clear", sessionManager.hasActiveRound())
        assertFalse("Should not have conversation history after clear", sessionManager.hasConversationHistory())
    }

    @Test
    fun `context hints extracted correctly`() = runTest {
        val shot = Shot(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            club = testClub,
            missDirection = MissDirection.SLICE,
            lie = Lie.FAIRWAY,
            pressureContext = PressureContext()
        )

        sessionManager.recordShot(shot)
        sessionManager.updateRound("round-1", "Pebble Beach")
        sessionManager.updateHole(7, 4)
        sessionManager.recordRecommendation("Aim left")

        val context = sessionManager.getCurrentContext()
        val hints = contextInjector.extractContextHints(context)

        assertEquals("Should extract last club", "7-iron", hints["last_club"])
        assertEquals("Should extract last miss", "SLICE", hints["last_miss"])
        assertEquals("Should extract course", "Pebble Beach", hints["course"])
        assertEquals("Should extract current hole", "7", hints["current_hole"])
        assertEquals("Should extract recommendation", "Aim left", hints["last_recommendation"])
    }

    @Test
    fun `context summary formatted correctly`() = runTest {
        sessionManager.updateRound("round-1", "Pebble Beach")
        sessionManager.updateHole(7, 4)

        val shot = Shot(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            club = testClub,
            missDirection = MissDirection.SLICE,
            lie = Lie.FAIRWAY,
            pressureContext = PressureContext()
        )
        sessionManager.recordShot(shot)

        val context = sessionManager.getCurrentContext()
        val summary = contextInjector.buildContextSummary(context)

        assertTrue("Summary should include course", summary.contains("Pebble Beach"))
        assertTrue("Summary should include hole", summary.contains("Hole 7"))
        assertTrue("Summary should include last club", summary.contains("7-iron"))
    }
}
