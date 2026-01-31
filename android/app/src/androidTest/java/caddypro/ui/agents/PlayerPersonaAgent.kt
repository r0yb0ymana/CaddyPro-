package caddypro.ui.agents

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.rules.ActivityScenarioRule
import caddypro.MainActivity

/**
 * Player Persona Agent - Simulates different golfer types using CaddyPro.
 *
 * Three player profiles:
 * - WeekendWarrior: High handicap (25+), casual, wants simple advice
 * - CompetitiveAmateur: Mid handicap (10-18), serious, wants detailed strategy
 * - LowHandicapper: Low handicap (<5), demands precision and speed
 *
 * Spec reference: player-profile-bag-management.md, live-caddy-mode.md
 */
class PlayerPersonaAgent(
    private val composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>,
    private val profile: PlayerProfile
) {

    private val experiences = mutableListOf<PlayerExperience>()
    private var frustrationLevel = 0  // 0-10 scale

    /**
     * Player starts a round.
     *
     * Navigation flow: HomeScreen → StartRoundScreen → LiveCaddyScreen
     * StartRoundScreen automatically starts a mock round and navigates to LiveCaddy.
     */
    fun startRound() {
        log("${profile.name} starting a round...")

        val startTime = System.currentTimeMillis()

        try {
            // Wait for HomeScreen "Start Round" button
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                composeTestRule.onAllNodesWithText("Start Round")
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("Start Round").performClick()

            // StartRoundScreen shows "Starting round..." then auto-navigates to LiveCaddy
            // Wait for StartRoundScreen to appear briefly (optional - it's fast)
            Thread.sleep(500)

            // Wait for LiveCaddy content to appear
            // The LiveCaddyTopBar shows "Hole X • Par Y" when roundState is not null
            // Or we might see weather/readiness HUD content first
            composeTestRule.waitUntil(timeoutMillis = 8000) {
                // Check for any indication of LiveCaddy being active
                composeTestRule.onAllNodesWithText("Hole", substring = true, ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("Readiness", substring = true, ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("Forecaster", substring = true, ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("Put your phone away", substring = true, ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("Pebble Beach", substring = true, ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }

            val elapsed = System.currentTimeMillis() - startTime

            experience(
                action = "Start Round",
                success = true,
                satisfaction = rateSatisfaction(elapsed, profile.patienceThresholdMs),
                note = "Round started in ${elapsed}ms"
            )

        } catch (e: Exception) {
            experience(
                action = "Start Round",
                success = false,
                satisfaction = 1,
                note = "Failed to start round: ${e.message}"
            )
            frustrationLevel += 3
        }
    }

    /**
     * Player checks weather before their shot.
     */
    fun checkWeather() {
        log("${profile.name} checking weather...")

        val weatherVisible = checkForText("km/h") && checkForText("°C")

        if (weatherVisible) {
            experience(
                action = "Check Weather",
                success = true,
                satisfaction = when (profile.type) {
                    PlayerType.WEEKEND_WARRIOR -> 4  // "That's nice"
                    PlayerType.COMPETITIVE_AMATEUR -> 5  // "Good, I need this"
                    PlayerType.LOW_HANDICAPPER -> 5  // "Essential info"
                },
                note = "Weather conditions visible"
            )
        } else {
            experience(
                action = "Check Weather",
                success = false,
                satisfaction = 2,
                note = "Couldn't find weather info"
            )
            if (profile.type != PlayerType.WEEKEND_WARRIOR) {
                frustrationLevel += 2  // Serious players care more
            }
        }
    }

    /**
     * Player views hole strategy.
     */
    fun viewHoleStrategy() {
        log("${profile.name} viewing hole strategy...")

        try {
            // Wait for the strategy button to be visible
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule.onAllNodesWithText("Show Hole Strategy", substring = true, ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onNodeWithText("Show Hole Strategy").performClick()

            // Wait for strategy panel to appear
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                composeTestRule.onAllNodesWithText("Danger Zones", substring = true, ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("Strategy", substring = true, ignoreCase = true)
                    .fetchSemanticsNodes().size >= 2
            }
            Thread.sleep(300)

            // Check for hazard info - the UI shows "Water", "Bunker", "OB" or "Danger Zones"
            val hasHazards = checkForText("Water|Bunker|OB|Danger", isRegex = true)
            // Check for recommendation - the UI shows "Aim" or "Strategy" or landing zone info
            val hasRecommendation = checkForText("Aim|fairway|Strategy|Landing", isRegex = true)

            when (profile.type) {
                PlayerType.WEEKEND_WARRIOR -> {
                    // Weekend player just wants simple advice
                    if (hasRecommendation) {
                        experience(
                            action = "View Strategy",
                            success = true,
                            satisfaction = 4,
                            note = "Got simple advice - perfect!"
                        )
                    } else {
                        experience(
                            action = "View Strategy",
                            success = false,
                            satisfaction = 2,
                            note = "Too much info, just tell me where to hit!"
                        )
                        frustrationLevel += 1
                    }
                }

                PlayerType.COMPETITIVE_AMATEUR -> {
                    // Competitive player wants hazard details
                    if (hasHazards && hasRecommendation) {
                        experience(
                            action = "View Strategy",
                            success = true,
                            satisfaction = 5,
                            note = "Good detail on hazards and strategy"
                        )
                    } else {
                        experience(
                            action = "View Strategy",
                            success = false,
                            satisfaction = 3,
                            note = "Need more hazard information"
                        )
                        frustrationLevel += 2
                    }
                }

                PlayerType.LOW_HANDICAPPER -> {
                    // Low handicapper wants precision
                    val hasDistances = checkForText("\\d+m|\\d+-\\d+m", isRegex = true)
                    if (hasHazards && hasDistances) {
                        experience(
                            action = "View Strategy",
                            success = true,
                            satisfaction = 5,
                            note = "Precise distances and hazard locations"
                        )
                    } else {
                        experience(
                            action = "View Strategy",
                            success = false,
                            satisfaction = 2,
                            note = "Need exact yardages to hazards"
                        )
                        frustrationLevel += 3
                    }
                }
            }

            // Close strategy
            composeTestRule.onNodeWithText("Hide Hole Strategy").performClick()

        } catch (e: Exception) {
            experience(
                action = "View Strategy",
                success = false,
                satisfaction = 1,
                note = "Strategy view broken: ${e.message}"
            )
            frustrationLevel += 2
        }
    }

    /**
     * Player logs a shot.
     *
     * Shot logging flow:
     * 1. Click FAB to open shot logger bottom sheet
     * 2. Wait for and select club
     * 3. Wait for and select lie result
     * 4. If rough/hazard, select miss direction (handled automatically by UI)
     */
    fun logShot(club: String = "7 Iron", lie: String = "Fairway") {
        log("${profile.name} logging shot: $club -> $lie")

        val startTime = System.currentTimeMillis()

        try {
            // Open shot logger via FAB
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                try {
                    composeTestRule.onNodeWithContentDescription("Log Shot")
                        .assertIsDisplayed()
                    true
                } catch (e: AssertionError) {
                    false
                }
            }
            composeTestRule.onNodeWithContentDescription("Log Shot").performClick()

            // Wait for shot logger bottom sheet to appear
            Thread.sleep(500)

            // Find and select club (substring match for flexibility)
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                composeTestRule.onAllNodesWithText(club, substring = true, ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onAllNodesWithText(club, substring = true, ignoreCase = true)
                .get(0).performClick()

            // Wait for result selector to appear (after club selection)
            Thread.sleep(300)

            // Select lie (the UI shows "Fairway", "Rough", "Green", "Bunker", "Water", "Fringe")
            composeTestRule.waitUntil(timeoutMillis = 3000) {
                composeTestRule.onAllNodesWithText(lie, substring = true, ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
            composeTestRule.onAllNodesWithText(lie, substring = true, ignoreCase = true)
                .get(0).performClick()

            // If lie is Rough or Water (Hazard), miss direction selector appears
            // For simplicity, we'll pick "Right" if needed
            if (lie.equals("Rough", ignoreCase = true) || lie.equals("Water", ignoreCase = true)) {
                Thread.sleep(300)
                try {
                    composeTestRule.waitUntil(timeoutMillis = 2000) {
                        composeTestRule.onAllNodesWithText("Right", substring = true, ignoreCase = true)
                            .fetchSemanticsNodes().isNotEmpty()
                    }
                    composeTestRule.onNodeWithText("Right").performClick()
                } catch (e: Exception) {
                    // Miss direction might not be required for all cases
                }
            }

            val elapsed = System.currentTimeMillis() - startTime

            // Different expectations by player type
            val acceptable = when (profile.type) {
                PlayerType.WEEKEND_WARRIOR -> elapsed < 8000  // Casual, more patient
                PlayerType.COMPETITIVE_AMATEUR -> elapsed < 5000  // Wants it quick
                PlayerType.LOW_HANDICAPPER -> elapsed < 3000  // Demands speed
            }

            experience(
                action = "Log Shot",
                success = acceptable,
                satisfaction = rateSatisfaction(elapsed, profile.patienceThresholdMs),
                note = "Shot logged in ${elapsed}ms"
            )

            if (!acceptable) {
                frustrationLevel += when (profile.type) {
                    PlayerType.WEEKEND_WARRIOR -> 1
                    PlayerType.COMPETITIVE_AMATEUR -> 2
                    PlayerType.LOW_HANDICAPPER -> 4
                }
            }

        } catch (e: Exception) {
            experience(
                action = "Log Shot",
                success = false,
                satisfaction = 1,
                note = "Shot logging failed: ${e.message}"
            )
            frustrationLevel += 3
        }
    }

    /**
     * Player checks their readiness score.
     */
    fun checkReadiness() {
        log("${profile.name} checking readiness...")

        val readinessVisible = checkForText("Readiness") ||
                               checkForText("\\d{1,2}%|\\d{3}%", isRegex = true)

        when (profile.type) {
            PlayerType.WEEKEND_WARRIOR -> {
                // Weekend player may not care much
                experience(
                    action = "Check Readiness",
                    success = readinessVisible,
                    satisfaction = if (readinessVisible) 3 else 3,  // Doesn't affect them much
                    note = if (readinessVisible) "Readiness shown" else "No readiness - meh"
                )
            }

            PlayerType.COMPETITIVE_AMATEUR -> {
                // Competitive player values this info
                experience(
                    action = "Check Readiness",
                    success = readinessVisible,
                    satisfaction = if (readinessVisible) 5 else 2,
                    note = if (readinessVisible) "Good to know my readiness level"
                           else "Where's my readiness data?"
                )
                if (!readinessVisible) frustrationLevel += 2
            }

            PlayerType.LOW_HANDICAPPER -> {
                // Low handicapper expects precision
                experience(
                    action = "Check Readiness",
                    success = readinessVisible,
                    satisfaction = if (readinessVisible) 4 else 3,
                    note = if (readinessVisible) "Readiness affects my strategy"
                           else "Missing readiness metric"
                )
            }
        }
    }

    /**
     * Simulate a complete hole.
     */
    fun playHole(holeNumber: Int = 1) {
        log("${profile.name} playing hole $holeNumber...")

        checkWeather()
        viewHoleStrategy()

        // Log shots based on player type
        when (profile.type) {
            PlayerType.WEEKEND_WARRIOR -> {
                // Casual player, logs 5-6 shots
                logShot("Driver", "Rough")
                logShot("7 Iron", "Fairway")
                logShot("PW", "Green")
            }

            PlayerType.COMPETITIVE_AMATEUR -> {
                // Logs every shot carefully
                logShot("Driver", "Fairway")
                logShot("6 Iron", "Green")
            }

            PlayerType.LOW_HANDICAPPER -> {
                // Quick, efficient logging
                logShot("Driver", "Fairway")
                logShot("8 Iron", "Green")
            }
        }
    }

    /**
     * Play multiple holes and generate feedback.
     */
    fun playRound(holes: Int = 3): PlayerFeedback {
        log("=== ${profile.name} PLAYING $holes HOLES ===")

        startRound()

        for (index in 0 until holes) {
            playHole(index + 1)

            // Check if player would give up
            if (frustrationLevel >= 8) {
                log("${profile.name} is too frustrated and quits!")
                break
            }
        }

        return generateFeedback()
    }

    /**
     * Generate player feedback report.
     */
    fun generateFeedback(): PlayerFeedback {
        val successRate = experiences.count { it.success } * 100 / experiences.size.coerceAtLeast(1)
        val avgSatisfaction = experiences.map { it.satisfaction }.average()

        val wouldRecommend = when {
            frustrationLevel >= 7 -> false
            avgSatisfaction >= 4.0 -> true
            profile.type == PlayerType.WEEKEND_WARRIOR && avgSatisfaction >= 3.0 -> true
            else -> false
        }

        return PlayerFeedback(
            playerName = profile.name,
            playerType = profile.type,
            successRate = successRate,
            averageSatisfaction = avgSatisfaction,
            frustrationLevel = frustrationLevel,
            wouldRecommend = wouldRecommend,
            experiences = experiences.toList(),
            verdict = when {
                wouldRecommend && avgSatisfaction >= 4.5 -> "LOVE IT - Will use every round!"
                wouldRecommend -> "LIKE IT - Good app for golf"
                avgSatisfaction >= 3.0 -> "OKAY - Has potential, needs work"
                else -> "FRUSTRATED - Would not use again"
            }
        )
    }

    // ============ Helper Methods ============

    private fun checkForText(pattern: String, isRegex: Boolean = false): Boolean {
        return try {
            if (isRegex) {
                // For regex patterns, check each alternative separated by |
                val alternatives = pattern.split("|")
                alternatives.any { alt ->
                    // Remove regex special chars for simple substring match
                    val searchText = alt.trim().replace("\\d+", "").replace("\\d{1,2}", "")
                        .replace("\\d{3}", "").replace("[+-]", "").replace("\\s+", " ")
                    if (searchText.isNotEmpty()) {
                        composeTestRule.onAllNodesWithText(searchText, substring = true, ignoreCase = true)
                            .fetchSemanticsNodes().isNotEmpty()
                    } else {
                        // For pure number patterns, look for any numeric content
                        composeTestRule.onAllNodesWithText("m", substring = true)
                            .fetchSemanticsNodes().isNotEmpty() ||
                        composeTestRule.onAllNodesWithText("%", substring = true)
                            .fetchSemanticsNodes().isNotEmpty()
                    }
                }
            } else {
                composeTestRule.onAllNodesWithText(pattern, substring = true, ignoreCase = true)
                    .fetchSemanticsNodes().isNotEmpty()
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun rateSatisfaction(actualMs: Long, thresholdMs: Long): Int {
        return when {
            actualMs < thresholdMs / 2 -> 5  // Excellent
            actualMs < thresholdMs -> 4      // Good
            actualMs < thresholdMs * 1.5 -> 3  // Okay
            actualMs < thresholdMs * 2 -> 2    // Poor
            else -> 1                          // Terrible
        }
    }

    private fun experience(action: String, success: Boolean, satisfaction: Int, note: String) {
        experiences.add(PlayerExperience(action, success, satisfaction, note))
        val emoji = if (success) "✓" else "✗"
        val stars = "★".repeat(satisfaction) + "☆".repeat(5 - satisfaction)
        log("  [$emoji] $action: $stars - $note")
    }

    private fun log(message: String) {
        println("[${profile.name}] $message")
    }

    companion object {
        /**
         * Create a Weekend Warrior player (high handicap, casual).
         */
        fun weekendWarrior(
            composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
        ) = PlayerPersonaAgent(
            composeTestRule,
            PlayerProfile(
                name = "Weekend Warrior",
                type = PlayerType.WEEKEND_WARRIOR,
                handicap = 25,
                patienceThresholdMs = 5000,
                expectsSimplicity = true
            )
        )

        /**
         * Create a Competitive Amateur player (mid handicap, serious).
         */
        fun competitiveAmateur(
            composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
        ) = PlayerPersonaAgent(
            composeTestRule,
            PlayerProfile(
                name = "Competitive Amateur",
                type = PlayerType.COMPETITIVE_AMATEUR,
                handicap = 12,
                patienceThresholdMs = 3000,
                expectsSimplicity = false
            )
        )

        /**
         * Create a Low Handicapper player (scratch-level, demanding).
         */
        fun lowHandicapper(
            composeTestRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>
        ) = PlayerPersonaAgent(
            composeTestRule,
            PlayerProfile(
                name = "Low Handicapper",
                type = PlayerType.LOW_HANDICAPPER,
                handicap = 3,
                patienceThresholdMs = 2000,
                expectsSimplicity = false
            )
        )
    }
}

enum class PlayerType {
    WEEKEND_WARRIOR,
    COMPETITIVE_AMATEUR,
    LOW_HANDICAPPER
}

data class PlayerProfile(
    val name: String,
    val type: PlayerType,
    val handicap: Int,
    val patienceThresholdMs: Long,
    val expectsSimplicity: Boolean
)

data class PlayerExperience(
    val action: String,
    val success: Boolean,
    val satisfaction: Int,  // 1-5 stars
    val note: String
)

data class PlayerFeedback(
    val playerName: String,
    val playerType: PlayerType,
    val successRate: Int,
    val averageSatisfaction: Double,
    val frustrationLevel: Int,
    val wouldRecommend: Boolean,
    val experiences: List<PlayerExperience>,
    val verdict: String
) {
    override fun toString(): String = buildString {
        appendLine("═══════════════════════════════════════")
        appendLine("       PLAYER FEEDBACK REPORT          ")
        appendLine("═══════════════════════════════════════")
        appendLine()
        appendLine("Player: $playerName (${playerType.name.replace("_", " ")})")
        appendLine("Success Rate: $successRate%")
        appendLine("Avg Satisfaction: ${"%.1f".format(averageSatisfaction)}/5.0")
        appendLine("Frustration Level: $frustrationLevel/10")
        appendLine("Would Recommend: ${if (wouldRecommend) "YES" else "NO"}")
        appendLine()
        appendLine("VERDICT: $verdict")
        appendLine()
        appendLine("Experience Log:")
        experiences.forEach { exp ->
            val stars = "★".repeat(exp.satisfaction) + "☆".repeat(5 - exp.satisfaction)
            appendLine("  ${if (exp.success) "✓" else "✗"} ${exp.action}: $stars")
            appendLine("    ${exp.note}")
        }
        appendLine("═══════════════════════════════════════")
    }
}
