package caddypro.domain.navcaddy.context

import caddypro.domain.navcaddy.models.ConversationTurn
import caddypro.domain.navcaddy.models.Role
import caddypro.domain.navcaddy.models.Round
import caddypro.domain.navcaddy.models.SessionContext
import caddypro.domain.navcaddy.models.Shot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages session context with reactive state updates.
 *
 * Tracks the current round state, hole information, last shot, last recommendation,
 * and conversation history. Provides a StateFlow for reactive UI updates and
 * integrates with the intent classification pipeline for context-aware responses.
 *
 * Session persists during app lifecycle but clears on app restart.
 *
 * Spec reference: navcaddy-engine.md R6 (Session Context)
 * Plan reference: navcaddy-engine-plan.md Task 15
 */
@Singleton
class SessionContextManager @Inject constructor() {

    private val conversationHistory = ConversationHistory()
    private var currentRoundState: RoundState? = null

    private val _context = MutableStateFlow(SessionContext.empty)

    /**
     * Current session context as a reactive StateFlow.
     *
     * UI and business logic can observe this for context updates.
     */
    val context: StateFlow<SessionContext> = _context.asStateFlow()

    /**
     * Update the current round information.
     *
     * Typically called when starting a new round.
     *
     * @param roundId Unique identifier for the round
     * @param courseName Name of the golf course
     * @param startingHole Starting hole number (default: 1)
     * @param startingPar Par for the starting hole (default: 4)
     */
    fun updateRound(
        roundId: String,
        courseName: String,
        startingHole: Int = 1,
        startingPar: Int = 4
    ) {
        require(roundId.isNotBlank()) { "Round ID cannot be blank" }
        require(courseName.isNotBlank()) { "Course name cannot be blank" }

        currentRoundState = RoundState(
            roundId = roundId,
            courseName = courseName,
            currentHole = startingHole,
            currentPar = startingPar
        )

        updateContextState()
    }

    /**
     * Update the current hole information.
     *
     * Called when moving to a new hole.
     *
     * @param holeNumber Hole number (1-18)
     * @param par Par for the hole
     */
    fun updateHole(holeNumber: Int, par: Int) {
        require(holeNumber in 1..18) { "Hole number must be between 1 and 18" }
        require(par in 3..5) { "Par must be between 3 and 5" }

        currentRoundState = currentRoundState?.copy(
            currentHole = holeNumber,
            currentPar = par
        )

        updateContextState()
    }

    /**
     * Update course conditions.
     *
     * @param conditions Current course conditions
     */
    fun updateConditions(conditions: CourseConditions) {
        currentRoundState = currentRoundState?.copy(
            conditions = conditions
        )

        updateContextState()
    }

    /**
     * Update the total score and holes completed.
     *
     * @param totalScore Running total score
     * @param holesCompleted Number of holes completed
     */
    fun updateScore(totalScore: Int, holesCompleted: Int) {
        require(totalScore >= 0) { "Total score must be non-negative" }
        require(holesCompleted in 0..18) { "Holes completed must be between 0 and 18" }

        currentRoundState = currentRoundState?.copy(
            totalScore = totalScore,
            holesCompleted = holesCompleted
        )

        updateContextState()
    }

    /**
     * Record a shot in the session context.
     *
     * Updates the lastShot field and can be used for pattern analysis.
     *
     * @param shot The shot to record
     */
    fun recordShot(shot: Shot) {
        _context.update { currentContext ->
            currentContext.copy(lastShot = shot)
        }
    }

    /**
     * Record a recommendation given to the user.
     *
     * Useful for follow-up queries like "What if the wind picks up?"
     *
     * @param recommendation The recommendation text
     */
    fun recordRecommendation(recommendation: String) {
        require(recommendation.isNotBlank()) { "Recommendation cannot be blank" }

        _context.update { currentContext ->
            currentContext.copy(lastRecommendation = recommendation)
        }
    }

    /**
     * Add a conversation turn to the history.
     *
     * Both user input and assistant response should be recorded separately.
     *
     * @param userInput The user's message
     * @param assistantResponse The assistant's response
     */
    fun addConversationTurn(userInput: String, assistantResponse: String) {
        require(userInput.isNotBlank()) { "User input cannot be blank" }
        require(assistantResponse.isNotBlank()) { "Assistant response cannot be blank" }

        val timestamp = System.currentTimeMillis()

        // Add user turn
        conversationHistory.addTurn(
            ConversationTurn(
                role = Role.USER,
                content = userInput,
                timestamp = timestamp
            )
        )

        // Add assistant turn
        conversationHistory.addTurn(
            ConversationTurn(
                role = Role.ASSISTANT,
                content = assistantResponse,
                timestamp = timestamp + 1 // Slightly later timestamp
            )
        )

        updateContextState()
    }

    /**
     * Add a single conversation turn.
     *
     * Use this when recording turns individually (e.g., during streaming responses).
     *
     * @param role The role (USER or ASSISTANT)
     * @param content The message content
     */
    fun addTurn(role: Role, content: String) {
        require(content.isNotBlank()) { "Content cannot be blank" }

        conversationHistory.addTurn(
            ConversationTurn(
                role = role,
                content = content,
                timestamp = System.currentTimeMillis()
            )
        )

        updateContextState()
    }

    /**
     * Clear the entire session context.
     *
     * Resets round state, conversation history, and all tracked information.
     * Typically called when starting a new session or on explicit user action.
     */
    fun clearSession() {
        currentRoundState = null
        conversationHistory.clear()
        _context.value = SessionContext.empty
    }

    /**
     * Clear only the conversation history.
     *
     * Keeps round and shot information but resets the dialogue.
     */
    fun clearConversationHistory() {
        conversationHistory.clear()
        updateContextState()
    }

    /**
     * Get the current round state.
     *
     * @return Current round state or null if no active round
     */
    fun getCurrentRoundState(): RoundState? = currentRoundState

    /**
     * Get the current context snapshot.
     *
     * Useful for synchronous access without StateFlow collection.
     *
     * @return Current session context
     */
    fun getCurrentContext(): SessionContext = _context.value

    /**
     * Get recent conversation turns.
     *
     * @param count Number of recent turns to retrieve
     * @return List of recent conversation turns
     */
    fun getRecentConversation(count: Int): List<ConversationTurn> {
        return conversationHistory.getRecent(count)
    }

    /**
     * Check if there is an active round.
     */
    fun hasActiveRound(): Boolean = currentRoundState != null

    /**
     * Check if conversation history exists.
     */
    fun hasConversationHistory(): Boolean = !conversationHistory.isEmpty()

    /**
     * Update the StateFlow with current state.
     */
    private fun updateContextState() {
        val round = currentRoundState?.let { state ->
            Round(
                id = state.roundId,
                startTime = System.currentTimeMillis(), // This should ideally be tracked separately
                courseName = state.courseName
            )
        }

        _context.update { currentContext ->
            SessionContext(
                currentRound = round,
                currentHole = currentRoundState?.currentHole,
                lastShot = currentContext.lastShot,
                lastRecommendation = currentContext.lastRecommendation,
                conversationHistory = conversationHistory.getAll()
            )
        }
    }
}
