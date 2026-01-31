package caddypro.ui.agents

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import caddypro.MainActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

/**
 * Persona-based testing for CaddyPro.
 *
 * Runs automated tests simulating:
 * - A golf coach evaluating the app's strategy and recommendations
 * - Different player types (weekend, competitive, low handicap) using the app
 *
 * This provides behavioral testing that goes beyond unit tests to verify
 * the app works well for its intended audience.
 *
 * Run with: ./gradlew connectedDevDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=caddypro.ui.agents.PersonaAgentTest
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@LargeTest
class PersonaAgentTest {

    private val hiltRule = HiltAndroidRule(this)
    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: RuleChain = RuleChain
        .outerRule(hiltRule)
        .around(composeRule)

    // Access composeTestRule for backwards compatibility
    val composeTestRule get() = composeRule

    // ============================================================
    // COACH EVALUATION TESTS
    // ============================================================

    @Test
    fun coachEvaluatesApp_fullSuite() {
        val coach = CoachPersonaAgent(composeTestRule)
        val report = coach.runFullEvaluation()

        println(report)

        // Coach should approve with at least 60% pass rate for MVP
        assert(report.passRate >= 60) {
            "Coach evaluation failed: ${report.passRate}% pass rate. " +
            "Concerns: ${report.concerns.joinToString()}"
        }
    }

    @Test
    fun coachEvaluatesStrategyForSlicer() {
        val coach = CoachPersonaAgent(composeTestRule)
        coach.evaluateStrategyForSlicer()
        val report = coach.generateReport()

        println(report)

        // Strategy should show pattern awareness, hazards, and recommendations
        // At minimum 50% of checks should pass for MVP
        assert(report.passRate >= 50) {
            "Strategy incomplete: ${report.passRate}% pass rate. Concerns: ${report.concerns.joinToString()}"
        }
    }

    @Test
    fun coachEvaluatesWeatherIntegration() {
        val coach = CoachPersonaAgent(composeTestRule)
        coach.evaluateWeatherAdjustments()
        val report = coach.generateReport()

        println(report)

        assert(report.strengths.any { it.contains("Weather", ignoreCase = true) }) {
            "Weather integration needs improvement"
        }
    }

    // ============================================================
    // WEEKEND WARRIOR TESTS
    // ============================================================

    @Test
    fun weekendWarriorPlaysRound_shouldBeSatisfied() {
        val player = PlayerPersonaAgent.weekendWarrior(composeTestRule)
        val feedback = player.playRound(holes = 2)

        println(feedback)

        // Weekend warrior should not get too frustrated
        assert(feedback.frustrationLevel < 6) {
            "Weekend Warrior got too frustrated: ${feedback.frustrationLevel}/10"
        }

        // Should have positive experience overall
        assert(feedback.averageSatisfaction >= 3.0) {
            "Weekend Warrior not satisfied: ${feedback.averageSatisfaction}/5.0"
        }
    }

    @Test
    fun weekendWarriorLogsShots_shouldBeSimple() {
        val player = PlayerPersonaAgent.weekendWarrior(composeTestRule)

        player.startRound()
        player.logShot("Driver", "Rough")
        player.logShot("7 Iron", "Fairway")

        val feedback = player.generateFeedback()
        println(feedback)

        // Shot logging should be satisfactory
        val loggingExperiences = feedback.experiences.filter { it.action == "Log Shot" }
        val avgLoggingSatisfaction = loggingExperiences.map { it.satisfaction }.average()

        assert(avgLoggingSatisfaction >= 3.0) {
            "Shot logging too complex for weekend warrior: $avgLoggingSatisfaction/5.0"
        }
    }

    // ============================================================
    // COMPETITIVE AMATEUR TESTS
    // ============================================================

    @Test
    fun competitiveAmateurPlaysRound_shouldValueStrategy() {
        val player = PlayerPersonaAgent.competitiveAmateur(composeTestRule)
        val feedback = player.playRound(holes = 2)

        println(feedback)

        // Competitive amateur expects detailed strategy
        val strategyExperience = feedback.experiences.find { it.action == "View Strategy" }
        assert(strategyExperience?.success == true) {
            "Competitive amateur needs better strategy info"
        }

        // Should recommend the app if strategy is good
        assert(feedback.wouldRecommend || feedback.averageSatisfaction >= 3.5) {
            "Competitive amateur would not recommend: ${feedback.verdict}"
        }
    }

    @Test
    fun competitiveAmateurChecksHazards_shouldSeeDetails() {
        val player = PlayerPersonaAgent.competitiveAmateur(composeTestRule)

        player.startRound()
        player.viewHoleStrategy()

        val feedback = player.generateFeedback()
        println(feedback)

        // Should see hazard details
        val strategyExp = feedback.experiences.find { it.action == "View Strategy" }
        assert(strategyExp?.satisfaction ?: 0 >= 4) {
            "Competitive amateur needs more hazard detail: ${strategyExp?.note}"
        }
    }

    // ============================================================
    // LOW HANDICAPPER TESTS
    // ============================================================

    @Test
    fun lowHandicapperPlaysRound_demandsPrecision() {
        val player = PlayerPersonaAgent.lowHandicapper(composeTestRule)
        val feedback = player.playRound(holes = 2)

        println(feedback)

        // Low handicapper is demanding - but shouldn't be frustrated by MVP
        assert(feedback.frustrationLevel < 8) {
            "Low handicapper too frustrated with app: ${feedback.frustrationLevel}/10"
        }
    }

    @Test
    fun lowHandicapperLogsShots_speedMatters() {
        val player = PlayerPersonaAgent.lowHandicapper(composeTestRule)

        player.startRound()

        // Log multiple shots quickly
        repeat(3) {
            player.logShot("8 Iron", "Green")
        }

        val feedback = player.generateFeedback()
        println(feedback)

        // Speed is critical for low handicappers
        val loggingExperiences = feedback.experiences.filter { it.action == "Log Shot" }
        val successRate = loggingExperiences.count { it.success } * 100 / loggingExperiences.size

        assert(successRate >= 66) {
            "Shot logging too slow for low handicapper: $successRate% success rate"
        }
    }

    // ============================================================
    // MULTI-PERSONA COMPARISON TEST
    // ============================================================

    @Test
    fun allPersonasTest_compareExperiences() {
        println("\n" + "=".repeat(60))
        println("MULTI-PERSONA COMPARISON TEST")
        println("=".repeat(60) + "\n")

        // Test each persona
        val weekendResult = runPersonaTest { PlayerPersonaAgent.weekendWarrior(composeTestRule) }
        composeTestRule.activityRule.scenario.recreate()

        val competitiveResult = runPersonaTest { PlayerPersonaAgent.competitiveAmateur(composeTestRule) }
        composeTestRule.activityRule.scenario.recreate()

        val lowHandicapResult = runPersonaTest { PlayerPersonaAgent.lowHandicapper(composeTestRule) }

        // Print comparison
        println("\n" + "=".repeat(60))
        println("COMPARISON SUMMARY")
        println("=".repeat(60))
        println()
        println("Player Type          | Satisfaction | Frustration | Would Recommend")
        println("-".repeat(60))
        println("Weekend Warrior      |    ${formatSat(weekendResult.averageSatisfaction)}    |     ${weekendResult.frustrationLevel}/10     |      ${if (weekendResult.wouldRecommend) "YES" else "NO"}")
        println("Competitive Amateur  |    ${formatSat(competitiveResult.averageSatisfaction)}    |     ${competitiveResult.frustrationLevel}/10     |      ${if (competitiveResult.wouldRecommend) "YES" else "NO"}")
        println("Low Handicapper      |    ${formatSat(lowHandicapResult.averageSatisfaction)}    |     ${lowHandicapResult.frustrationLevel}/10     |      ${if (lowHandicapResult.wouldRecommend) "YES" else "NO"}")
        println("=".repeat(60))

        // At least 2 of 3 personas should be satisfied
        val satisfied = listOf(weekendResult, competitiveResult, lowHandicapResult)
            .count { it.wouldRecommend }

        assert(satisfied >= 2) {
            "Only $satisfied/3 personas would recommend the app"
        }
    }

    private fun runPersonaTest(createPlayer: () -> PlayerPersonaAgent): PlayerFeedback {
        val player = createPlayer()
        return player.playRound(holes = 1)
    }

    private fun formatSat(satisfaction: Double): String {
        return "${"%.1f".format(satisfaction)}/5"
    }
}
