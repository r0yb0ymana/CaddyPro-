package caddypro.domain.navcaddy.context

import caddypro.domain.navcaddy.models.ConversationTurn
import caddypro.domain.navcaddy.models.Role
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ConversationHistory.
 *
 * Spec reference: navcaddy-engine.md R6, navcaddy-engine-plan.md Task 15
 */
class ConversationHistoryTest {

    private lateinit var history: ConversationHistory

    @Before
    fun setUp() {
        history = ConversationHistory(maxSize = 5)
    }

    @Test
    fun `addTurn adds turn to history`() {
        val turn = createTurn(Role.USER, "Test message")
        history.addTurn(turn)

        assertEquals(1, history.size())
        assertEquals(turn, history.getAll().first())
    }

    @Test
    fun `addTurn maintains max size limit`() {
        // Add more turns than max size
        repeat(10) { index ->
            history.addTurn(createTurn(Role.USER, "Message $index"))
        }

        assertEquals(5, history.size())
        // Should keep the most recent 5
        assertEquals("Message 5", history.getAll()[0].content)
        assertEquals("Message 9", history.getAll()[4].content)
    }

    @Test
    fun `getRecent returns specified number of turns`() {
        repeat(5) { index ->
            history.addTurn(createTurn(Role.USER, "Message $index"))
        }

        val recent = history.getRecent(3)
        assertEquals(3, recent.size)
        assertEquals("Message 2", recent[0].content)
        assertEquals("Message 4", recent[2].content)
    }

    @Test
    fun `getRecent with count greater than size returns all turns`() {
        repeat(3) { index ->
            history.addTurn(createTurn(Role.USER, "Message $index"))
        }

        val recent = history.getRecent(10)
        assertEquals(3, recent.size)
    }

    @Test
    fun `getLastUserInput returns most recent user message`() {
        history.addTurn(createTurn(Role.USER, "User 1"))
        history.addTurn(createTurn(Role.ASSISTANT, "Assistant 1"))
        history.addTurn(createTurn(Role.USER, "User 2"))

        assertEquals("User 2", history.getLastUserInput())
    }

    @Test
    fun `getLastUserInput returns null when no user messages`() {
        history.addTurn(createTurn(Role.ASSISTANT, "Assistant only"))

        assertNull(history.getLastUserInput())
    }

    @Test
    fun `getLastAssistantResponse returns most recent assistant message`() {
        history.addTurn(createTurn(Role.ASSISTANT, "Assistant 1"))
        history.addTurn(createTurn(Role.USER, "User 1"))
        history.addTurn(createTurn(Role.ASSISTANT, "Assistant 2"))

        assertEquals("Assistant 2", history.getLastAssistantResponse())
    }

    @Test
    fun `getLastAssistantResponse returns null when no assistant messages`() {
        history.addTurn(createTurn(Role.USER, "User only"))

        assertNull(history.getLastAssistantResponse())
    }

    @Test
    fun `getRecentByRole filters by role`() {
        history.addTurn(createTurn(Role.USER, "User 1"))
        history.addTurn(createTurn(Role.ASSISTANT, "Assistant 1"))
        history.addTurn(createTurn(Role.USER, "User 2"))
        history.addTurn(createTurn(Role.ASSISTANT, "Assistant 2"))

        val userTurns = history.getRecentByRole(Role.USER, 10)
        assertEquals(2, userTurns.size)
        assertEquals("User 1", userTurns[0].content)
        assertEquals("User 2", userTurns[1].content)

        val assistantTurns = history.getRecentByRole(Role.ASSISTANT, 10)
        assertEquals(2, assistantTurns.size)
        assertEquals("Assistant 1", assistantTurns[0].content)
        assertEquals("Assistant 2", assistantTurns[1].content)
    }

    @Test
    fun `clear removes all turns`() {
        repeat(5) { index ->
            history.addTurn(createTurn(Role.USER, "Message $index"))
        }

        assertEquals(5, history.size())

        history.clear()

        assertEquals(0, history.size())
        assertTrue(history.isEmpty())
    }

    @Test
    fun `isEmpty returns true for new history`() {
        assertTrue(history.isEmpty())
    }

    @Test
    fun `isEmpty returns false when turns exist`() {
        history.addTurn(createTurn(Role.USER, "Test"))
        assertFalse(history.isEmpty())
    }

    @Test
    fun `circular buffer behavior - FIFO`() {
        val maxSize = 3
        val smallHistory = ConversationHistory(maxSize = maxSize)

        // Add 5 turns to buffer of size 3
        smallHistory.addTurn(createTurn(Role.USER, "First"))
        smallHistory.addTurn(createTurn(Role.USER, "Second"))
        smallHistory.addTurn(createTurn(Role.USER, "Third"))
        smallHistory.addTurn(createTurn(Role.USER, "Fourth"))
        smallHistory.addTurn(createTurn(Role.USER, "Fifth"))

        val all = smallHistory.getAll()
        assertEquals(3, all.size)
        assertEquals("Third", all[0].content)
        assertEquals("Fourth", all[1].content)
        assertEquals("Fifth", all[2].content)
    }

    @Test
    fun `thread safety - concurrent additions`() {
        val threads = (1..10).map { index ->
            Thread {
                repeat(100) {
                    history.addTurn(createTurn(Role.USER, "Thread $index Message $it"))
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Should only have maxSize turns, no exceptions thrown
        assertEquals(5, history.size())
    }

    @Test
    fun `default max size is 10`() {
        val defaultHistory = ConversationHistory()

        repeat(15) { index ->
            defaultHistory.addTurn(createTurn(Role.USER, "Message $index"))
        }

        assertEquals(10, defaultHistory.size())
    }

    // Helper function to create conversation turns
    private fun createTurn(role: Role, content: String): ConversationTurn {
        return ConversationTurn(
            role = role,
            content = content,
            timestamp = System.currentTimeMillis()
        )
    }
}
