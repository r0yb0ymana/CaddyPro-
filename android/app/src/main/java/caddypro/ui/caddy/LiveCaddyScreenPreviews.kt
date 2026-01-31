package caddypro.ui.caddy

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import caddypro.domain.caddy.models.HazardLocation
import caddypro.domain.caddy.models.HazardType
import caddypro.domain.caddy.models.HazardZone
import caddypro.domain.caddy.models.HoleStrategy
import caddypro.domain.caddy.models.LandingZone
import caddypro.domain.caddy.models.Location
import caddypro.domain.caddy.models.MetricScore
import caddypro.domain.caddy.models.PersonalizationContext
import caddypro.domain.caddy.models.ReadinessBreakdown
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.ReadinessSource
import caddypro.domain.caddy.models.WeatherData
import caddypro.domain.navcaddy.context.RoundState
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.ui.theme.CaddyProTheme

/**
 * Comprehensive preview composables for LiveCaddyScreen.
 *
 * Demonstrates all screen states and component combinations to aid in UI development.
 *
 * Spec reference: live-caddy-mode.md R1-R7
 * Plan reference: live-caddy-mode-plan.md Task 18
 */

@Preview(name = "Full Screen - Strategy Visible", showBackground = true, heightDp = 800)
@Composable
private fun LiveCaddyScreenFullPreview() {
    CaddyProTheme {
        LiveCaddyContent(
            state = LiveCaddyState(
                roundState = RoundState(
                    roundId = "round-1",
                    courseName = "Pebble Beach",
                    currentHole = 7,
                    currentPar = 3,
                    totalScore = 8
                ),
                weather = WeatherData(
                    location = Location(37.7749, -122.4194),
                    windSpeedMps = 6.7,
                    windDegrees = 225,
                    temperatureCelsius = 20.0,
                    humidity = 65,
                    timestamp = System.currentTimeMillis()
                ),
                readiness = ReadinessScore(
                    overall = 75,
                    breakdown = ReadinessBreakdown(
                        hrv = MetricScore(value = 80.0, weight = 0.4),
                        sleepQuality = MetricScore(value = 70.0, weight = 0.4),
                        stressLevel = MetricScore(value = 75.0, weight = 0.2)
                    ),
                    timestamp = System.currentTimeMillis(),
                    source = ReadinessSource.WEARABLE_SYNC
                ),
                holeStrategy = HoleStrategy(
                    holeNumber = 7,
                    dangerZones = listOf(
                        HazardZone(
                            type = HazardType.WATER,
                            location = HazardLocation(
                                side = "right",
                                distanceFromTee = 130..160
                            ),
                            penaltyStrokes = 1.0,
                            affectedMisses = listOf(MissDirection.SLICE)
                        ),
                        HazardZone(
                            type = HazardType.BUNKER,
                            location = HazardLocation(
                                side = "left",
                                distanceFromTee = 140..155
                            ),
                            penaltyStrokes = 0.5,
                            affectedMisses = listOf(MissDirection.HOOK)
                        )
                    ),
                    recommendedLandingZone = LandingZone(
                        targetLine = 180,
                        idealDistance = 142,
                        safetyMargin = 10,
                        visualCue = "Center of green"
                    ),
                    riskCallouts = listOf(
                        "Right miss brings water into play",
                        "Wind is cross-left to right - aim 5 yards left"
                    ),
                    personalizedFor = PersonalizationContext(
                        handicap = 9,
                        dominantMiss = MissDirection.SLICE,
                        clubDistances = mapOf("7i" to 140, "8i" to 130, "PW" to 110),
                        readinessScore = 75
                    )
                ),
                isLoading = false,
                error = null,
                isStrategyMapVisible = true,
                isWeatherHudExpanded = false,
                isReadinessDetailsVisible = false
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Full Screen - Strategy Hidden", showBackground = true, heightDp = 600)
@Composable
private fun LiveCaddyScreenStrategyHiddenPreview() {
    CaddyProTheme {
        LiveCaddyContent(
            state = LiveCaddyState(
                roundState = RoundState(
                    roundId = "round-2",
                    courseName = "Augusta National",
                    currentHole = 14,
                    currentPar = 5,
                    totalScore = 15
                ),
                weather = WeatherData(
                    location = Location(37.7749, -122.4194),
                    windSpeedMps = 3.6,
                    windDegrees = 90,
                    temperatureCelsius = 24.0,
                    humidity = 50,
                    timestamp = System.currentTimeMillis()
                ),
                readiness = ReadinessScore(
                    overall = 85,
                    breakdown = ReadinessBreakdown(
                        hrv = MetricScore(value = 85.0, weight = 0.4),
                        sleepQuality = MetricScore(value = 88.0, weight = 0.4),
                        stressLevel = MetricScore(value = 82.0, weight = 0.2)
                    ),
                    timestamp = System.currentTimeMillis(),
                    source = ReadinessSource.WEARABLE_SYNC
                ),
                isLoading = false,
                error = null,
                isStrategyMapVisible = false,
                isWeatherHudExpanded = false,
                isReadinessDetailsVisible = false
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Full Screen - Expanded HUDs", showBackground = true, heightDp = 800)
@Composable
private fun LiveCaddyScreenExpandedHudsPreview() {
    CaddyProTheme {
        LiveCaddyContent(
            state = LiveCaddyState(
                roundState = RoundState(
                    roundId = "round-3",
                    courseName = "St Andrews",
                    currentHole = 1,
                    currentPar = 4,
                    totalScore = 0
                ),
                weather = WeatherData(
                    location = Location(37.7749, -122.4194),
                    windSpeedMps = 8.9,
                    windDegrees = 180,
                    temperatureCelsius = 18.0,
                    humidity = 70,
                    timestamp = System.currentTimeMillis()
                ),
                readiness = ReadinessScore(
                    overall = 60,
                    breakdown = ReadinessBreakdown(
                        hrv = MetricScore(value = 55.0, weight = 0.4),
                        sleepQuality = MetricScore(value = 62.0, weight = 0.4),
                        stressLevel = MetricScore(value = 63.0, weight = 0.2)
                    ),
                    timestamp = System.currentTimeMillis(),
                    source = ReadinessSource.WEARABLE_SYNC
                ),
                isLoading = false,
                error = null,
                isStrategyMapVisible = false,
                isWeatherHudExpanded = true,
                isReadinessDetailsVisible = true
            ),
            onAction = {}
        )
    }
}

@Preview(name = "Error State", showBackground = true, heightDp = 400)
@Composable
private fun LiveCaddyScreenErrorPreview() {
    CaddyProTheme {
        LiveCaddyContent(
            state = LiveCaddyState(
                isLoading = false,
                error = "Failed to load weather data. Check your internet connection."
            ),
            onAction = {}
        )
    }
}
