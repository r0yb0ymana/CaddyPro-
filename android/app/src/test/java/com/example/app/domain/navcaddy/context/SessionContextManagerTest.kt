package caddypro.domain.navcaddy.context

import app.cash.turbine.test
import caddypro.domain.navcaddy.models.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SessionContextManager.
 *
 * Uses Turbine for testing StateFlow emissions.
 *
 * Spec reference: navcaddy-engine.md R6, navcaddy-engine-plan.md Task 15
 */
class SessionContextManagerTest {

    private lateinit var manager: SessionContextManager

    @Before
    fun setUp() {
        manager = SessionContextManager()
    }

    @Test
    fun `initial context is empty`() = runTest {
        manager.context.test {
            val initialContext = awaitItem()
            assertEquals(SessionContext.empty, initialContext)
        }
    }

    @Test
    fun `updateRound updates context with round information`() = runTest {
        manager.context.test {
            skipItems(1) // Skip initial empty

            manager.updateRound(
                roundId = "round-123",
                courseName = "Pebble Beach",
                startingHole = 1,
                startingPar = 4
            )

            val updatedContext = awaitItem()
            assertEquals("round-123", updatedContext.currentRound?.id)
            assertEquals("Pebble Beach", updatedContext.currentRound?.courseName)
            assertEquals(1, updatedContext.currentHole)
        }
    }

    @Test
    fun `updateRound validates input`() {
        assertThrows(IllegalArgumentException::class.java) {
            manager.updateRound(roundId = "", courseName = "Test")
        }

        assertThrows(IllegalArgumentException::class.java) {
            manager.updateRound(roundId = "test", courseName = "")
        }
    }

    @Test
    fun `updateHole updates current hole`() = runTest {
        manager.updateRound("round-1", "Test Course")

        manager.context.test {
            skipItems(2) // Skip initial and round update

            manager.updateHole(7, 4)

            val updatedContext = awaitItem()
            assertEquals(7, updatedContext.currentHole)
        }
    }

    @Test
    fun `updateHole validates hole number`() {
        manager.updateRound("round-1", "Test Course")

        assertThrows(IllegalArgumentException::class.java) {
            manager.updateHole(0, 4)
        }

        assertThrows(IllegalArgumentException::class.java) {
            manager.updateHole(19, 4)
        }
    }

    @Test
    fun `updateHole validates par`() {
        manager.updateRound("round-1", "Test Course")

        assertThrows(IllegalArgumentException::class.java) {
            manager.updateHole(1, 2)
        }

        assertThrows(IllegalArgumentException::class.java) {
            manager.updateHole(1, 6)
        }
    }

    @Test
    fun `updateConditions updates round state`() {
        manager.updateRound("round-1", "Test Course")

        val conditions = CourseConditions(
            weather = "Sunny",
            windSpeed = 10,
            windDirection = "NW"
        )

        manager.updateConditions(conditions)

        val roundState = manager.getCurrentRoundState()
        assertNotNull(roundState)
        assertEquals("Sunny", roundState?.conditions?.weather)
        assertEquals(10, roundState?.conditions?.windSpeed)
    }

    @Test
    fun `updateScore updates total score and holes completed`() {
        manager.updateRound("round-1", "Test Course")

        manager.updateScore(totalScore = 42, holesCompleted = 9)

        val roundState = manager.getCurrentRoundState()
        assertEquals(42, roundState?.totalScore)
        assertEquals(9, roundState?.holesCompleted)
    }

    @Test
    fun `updateScore validates input`() {
        manager.updateRound("round-1", "Test Course")

        assertThrows(IllegalArgumentException::class.java) {
            manager.updateScore(totalScore = -1, holesCompleted = 9)
        }

        assertThrows(IllegalArgumentException::class.java) {
            manager.updateScore(totalScore = 42, holesCompleted = 19)
        }
    }

    @Test
    fun `recordShot updates last shot in context`() = runTest {
        val shot = Shot(
            id = "shot-1",
            timestamp = System.currentTimeMillis(),
            club = Club(name = "7-iron", type = ClubType.IRON, estimatedCarry = 150),
            lie = Lie.FAIRWAY,
            pressureContext = PressureContext.NORMAL
        )

        manager.context.test {
            skipItems(1) // Skip initial

            manager.recordShot(shot)

            val updatedContext = awaitItem()
            assertEquals(shot, updatedContext.lastShot)
        }
    }

    @Test
    fun `recordRecommendation updates last recommendation`() = runTest {
        val recommendation = "Take one more club due to wind"

        manager.context.test {
            skipItems(1) // Skip initial

            manager.recordRecommendation(recommendation)

            val updatedContext = awaitItem()
            assertEquals(recommendation, updatedContext.lastRecommendation)
        }
    }

    @Test
    fun `recordRecommendation validates input`() {
        assertThrows(IllegalArgumentException::class.java) {
            manager.recordRecommendation("")
        }

        assertThrows(IllegalArgumentException::class.java) {
            manager.recordRecommendation("   ")
        }
    }

    @Test
    fun `addConversationTurn adds both user and assistant turns`() = runTest {
        manager.context.test {
            skipItems(1) // Skip initial

            manager.addConversationTurn(
                userInput = "What club for 150 yards?",
                assistantResponse = "I recommend your 7-iron"
            )

            val updatedContext = awaitItem()
            assertEquals(2, updatedContext.conversationHistory.size)

            val userTurn = updatedContext.conversationHistory[0]
            assertEquals(Role.USER, userTurn.role)
            assertEquals("What club for 150 yards?", userTurn.content)

            val assistantTurn = updatedContext.conversationHistory[1]
            assertEquals(Role.ASSISTANT, assistantTurn.role)
            assertEquals("I recommend your 7-iron", assistantTurn.content)
        }
    }

    @Test
    fun `addConversationTurn validates input`() {
        assertThrows(IllegalArgumentException::class.java) {
            manager.addConversationTurn("", "response")
        }

        assertThrows(IllegalArgumentException::class.java) {
            manager.addConversationTurn("input", "")
        }
    }

    @Test
    fun `addTurn adds single turn`() = runTest {
        manager.context.test {
            skipItems(1) // Skip initial

            manager.addTurn(Role.USER, "Test message")

            val updatedContext = awaitItem()
            assertEquals(1, updatedContext.conversationHistory.size)
            assertEquals(Role.USER, updatedContext.conversationHistory[0].role)
            assertEquals("Test message", updatedContext.conversationHistory[0].content)
        }
    }

    @Test
    fun `conversation history maintains max size of 10`() = runTest {
        // Add 15 conversation turns (30 individual turns)
        repeat(15) { index ->
            manager.addConversationTurn(
                userInput = "User message $index",
                assistantResponse = "Assistant response $index"
            )
        }

        val context = manager.getCurrentContext()
        // Should only keep last 10 turns
        assertEquals(10, context.conversationHistory.size)
    }

    @Test
    fun `clearSession resets all context`() = runTest {
        // Setup context
        manager.updateRound("round-1", "Test Course")
        manager.updateHole(5, 4)
        manager.recordRecommendation("Test recommendation")
        manager.addConversationTurn("User", "Assistant")

        manager.context.test {
            skipItems(4) // Skip all previous updates

            manager.clearSession()

            val clearedContext = awaitItem()
            assertEquals(SessionContext.empty, clearedContext)
            assertNull(manager.getCurrentRoundState())
            assertFalse(manager.hasActiveRound())
            assertFalse(manager.hasConversationHistory())
        }
    }

    @Test
    fun `clearConversationHistory keeps round info`() = runTest {
        manager.updateRound("round-1", "Test Course")
        manager.addConversationTurn("User", "Assistant")

        manager.context.test {
            skipItems(2) // Skip initial updates

            manager.clearConversationHistory()

            val updatedContext = awaitItem()
            assertNotNull(updatedContext.currentRound)
            assertTrue(updatedContext.conversationHistory.isEmpty())
        }
    }

    @Test
    fun `getCurrentContext returns current snapshot`() {
        manager.updateRound("round-1", "Test Course")

        val context = manager.getCurrentContext()
        assertNotNull(context.currentRound)
        assertEquals("round-1", context.currentRound?.id)
    }

    @Test
    fun `getRecentConversation returns specified number of turns`() {
        repeat(5) { index ->
            manager.addConversationTurn(
                userInput = "User $index",
                assistantResponse = "Assistant $index"
            )
        }

        val recent = manager.getRecentConversation(4)
        assertEquals(4, recent.size)
    }

    @Test
    fun `hasActiveRound returns true when round exists`() {
        assertFalse(manager.hasActiveRound())

        manager.updateRound("round-1", "Test Course")

        assertTrue(manager.hasActiveRound())
    }

    @Test
    fun `hasConversationHistory returns true when history exists`() {
        assertFalse(manager.hasConversationHistory())

        manager.addTurn(Role.USER, "Test")

        assertTrue(manager.hasConversationHistory())
    }

    @Test
    fun `multiple rapid updates maintain consistency`() = runTest {
        manager.context.test {
            skipItems(1) // Skip initial

            // Rapid updates
            manager.updateRound("round-1", "Course 1")
            manager.updateHole(3, 4)
            manager.recordRecommendation("Recommendation 1")
            manager.addTurn(Role.USER, "Message 1")

            // Wait for all updates to settle
            skipItems(3) // Skip intermediate updates

            val finalContext = awaitItem()

            // Verify final state
            assertEquals("round-1", finalContext.currentRound?.id)
            assertEquals(3, finalContext.currentHole)
            assertEquals("Recommendation 1", finalContext.lastRecommendation)
            assertEquals(1, finalContext.conversationHistory.size)
        }
    }

    @Test
    fun `context updates are thread-safe`() = runTest {
        val threads = (1..10).map { index ->
            Thread {
                repeat(10) {
                    manager.addTurn(Role.USER, "Thread $index Message $it")
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Should have conversation history without crashes
        val context = manager.getCurrentContext()
        assertEquals(10, context.conversationHistory.size) // Limited to max size
    }

    @Test
    fun `getCurrentRoundState returns null when no round`() {
        assertNull(manager.getCurrentRoundState())
    }

    @Test
    fun `getCurrentRoundState returns round state when round active`() {
        manager.updateRound("round-1", "Pebble Beach", startingHole = 5, startingPar = 3)

        val roundState = manager.getCurrentRoundState()
        assertNotNull(roundState)
        assertEquals("round-1", roundState?.roundId)
        assertEquals("Pebble Beach", roundState?.courseName)
        assertEquals(5, roundState?.currentHole)
        assertEquals(3, roundState?.currentPar)
    }

    @Test
    fun `full workflow integration test`() = runTest {
        manager.context.test {
            skipItems(1) // Skip initial

            // Start a round
            manager.updateRound("round-123", "Augusta National")
            val context1 = awaitItem()
            assertEquals("Augusta National", context1.currentRound?.courseName)

            // Move to hole 12
            manager.updateHole(12, 3)
            val context2 = awaitItem()
            assertEquals(12, context2.currentHole)

            // Update conditions
            manager.updateConditions(
                CourseConditions(weather = "Windy", windSpeed = 15, windDirection = "SW")
            )
            val context3 = awaitItem()
            // Context should still have round and hole
            assertEquals("Augusta National", context3.currentRound?.courseName)
            assertEquals(12, context3.currentHole)

            // Record a shot
            val shot = Shot(
                id = "shot-1",
                timestamp = System.currentTimeMillis(),
                club = Club(name = "9-iron", type = ClubType.IRON, estimatedCarry = 135),
                lie = Lie.FAIRWAY,
                pressureContext = PressureContext.PRESSURE
            )
            manager.recordShot(shot)
            val context4 = awaitItem()
            assertEquals("9-iron", context4.lastShot?.club?.name)

            // Record recommendation
            manager.recordRecommendation("Aim for center of green")
            val context5 = awaitItem()
            assertEquals("Aim for center of green", context5.lastRecommendation)

            // Add conversation
            manager.addConversationTurn("What if wind picks up?", "Add 10 yards")
            val context6 = awaitItem()
            assertEquals(2, context6.conversationHistory.size)

            // Verify all context is present
            assertNotNull(context6.currentRound)
            assertNotNull(context6.currentHole)
            assertNotNull(context6.lastShot)
            assertNotNull(context6.lastRecommendation)
            assertFalse(context6.conversationHistory.isEmpty())
        }
    }
}
