package caddypro.ui.caddy

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                    roundId = 1L,
                    courseId = 1L,
                    currentHole = 7,
                    currentPar = 3,
                    shotsOnCurrentHole = 1,
                    totalScore = 8
                ),
                weather = WeatherData(
                    location = Location(37.7749, -122.4194),
                    windSpeed = 15.0,
                    windDirection = 225.0,
                    temperature = 68.0,
                    humidity = 65,
                    timestamp = System.currentTimeMillis()
                ),
                readiness = ReadinessScore(
                    overall = 75,
                    breakdown = ReadinessBreakdown(
                        hrv = MetricScore(value = 80, trend = 0),
                        sleepQuality = MetricScore(value = 70, trend = -5),
                        stressLevel = MetricScore(value = 75, trend = 2)
                    ),
                    timestamp = System.currentTimeMillis(),
                    source = ReadinessSource.WEARABLE_SYNC
                ),
                holeStrategy = HoleStrategy(
                    holeNumber = 7,
                    par = 3,
                    hazardZones = listOf(
                        HazardZone(
                            type = HazardType.WATER,
                            location = HazardLocation.FRONT_RIGHT,
                            description = "Water hazard guarding front right of green"
                        ),
                        HazardZone(
                            type = HazardType.BUNKER,
                            location = HazardLocation.LEFT,
                            description = "Deep bunker left of green"
                        )
                    ),
                    recommendedLandingZone = LandingZone(
                        targetYardage = 155,
                        safetyMargin = 10,
                        aimPoint = "Center of green"
                    ),
                    riskCallouts = listOf(
                        "Right miss brings water into play",
                        "Wind is cross-left to right - aim 5 yards left"
                    ),
                    personalizationContext = PersonalizationContext(
                        handicap = 9,
                        dominantMiss = MissDirection.RIGHT,
                        readinessAdjustment = "Normal strategy (readiness 75/100)"
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
                    roundId = 1L,
                    courseId = 1L,
                    currentHole = 14,
                    currentPar = 5,
                    shotsOnCurrentHole = 2,
                    totalScore = 15
                ),
                weather = WeatherData(
                    location = Location(37.7749, -122.4194),
                    windSpeed = 8.0,
                    windDirection = 90.0,
                    temperature = 75.0,
                    humidity = 50,
                    timestamp = System.currentTimeMillis()
                ),
                readiness = ReadinessScore(
                    overall = 85,
                    breakdown = ReadinessBreakdown(
                        hrv = MetricScore(value = 85, trend = 3),
                        sleepQuality = MetricScore(value = 88, trend = 5),
                        stressLevel = MetricScore(value = 82, trend = 0)
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
                    roundId = 1L,
                    courseId = 1L,
                    currentHole = 1,
                    currentPar = 4,
                    shotsOnCurrentHole = 0,
                    totalScore = 0
                ),
                weather = WeatherData(
                    location = Location(37.7749, -122.4194),
                    windSpeed = 20.0,
                    windDirection = 180.0,
                    temperature = 65.0,
                    humidity = 70,
                    timestamp = System.currentTimeMillis()
                ),
                readiness = ReadinessScore(
                    overall = 60,
                    breakdown = ReadinessBreakdown(
                        hrv = MetricScore(value = 55, trend = -10),
                        sleepQuality = MetricScore(value = 62, trend = -8),
                        stressLevel = MetricScore(value = 63, trend = 5)
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
