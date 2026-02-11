package caddypro.domain.navcaddy.routing

/**
 * Fake implementation of PrerequisiteChecker for testing.
 *
 * Allows tests to configure which prerequisites are satisfied.
 */
class FakePrerequisiteChecker : PrerequisiteChecker {
    private val satisfiedPrerequisites = mutableSetOf<Prerequisite>()

    /**
     * Mark a prerequisite as satisfied for testing.
     */
    fun satisfy(prerequisite: Prerequisite) {
        satisfiedPrerequisites.add(prerequisite)
    }

    /**
     * Mark a prerequisite as not satisfied for testing.
     */
    fun unsatisfy(prerequisite: Prerequisite) {
        satisfiedPrerequisites.remove(prerequisite)
    }

    /**
     * Mark all prerequisites as satisfied.
     */
    fun satisfyAll() {
        satisfiedPrerequisites.addAll(Prerequisite.entries)
    }

    /**
     * Clear all satisfied prerequisites.
     */
    fun reset() {
        satisfiedPrerequisites.clear()
    }

    override suspend fun check(prerequisite: Prerequisite): Boolean {
        return satisfiedPrerequisites.contains(prerequisite)
    }
}
