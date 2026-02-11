package caddypro.domain.navcaddy.routing

/**
 * Interface for checking if prerequisites are satisfied.
 *
 * Implementations will check the current app state (repositories, session context)
 * to determine if required data exists.
 *
 * Spec reference: navcaddy-engine.md R3
 */
interface PrerequisiteChecker {
    /**
     * Check if a specific prerequisite is satisfied.
     *
     * @param prerequisite The prerequisite to check
     * @return true if prerequisite is satisfied, false otherwise
     */
    suspend fun check(prerequisite: Prerequisite): Boolean

    /**
     * Check multiple prerequisites at once.
     *
     * @param prerequisites List of prerequisites to check
     * @return List of prerequisites that are NOT satisfied
     */
    suspend fun checkAll(prerequisites: List<Prerequisite>): List<Prerequisite> {
        return prerequisites.filter { !check(it) }
    }
}
