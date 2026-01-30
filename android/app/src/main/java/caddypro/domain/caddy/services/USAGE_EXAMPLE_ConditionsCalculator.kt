package caddypro.domain.caddy.services

import caddypro.domain.caddy.models.Location
import caddypro.domain.caddy.models.WeatherData

/**
 * Usage examples for ConditionsCalculator.
 *
 * This file demonstrates how to use the ConditionsCalculator service
 * in production code, typically from a ViewModel or use case.
 *
 * NOT INTENDED FOR PRODUCTION - This is a reference/example file only.
 */

// Example 1: Inject in a ViewModel
/*
@HiltViewModel
class HoleStrategyViewModel @Inject constructor(
    private val conditionsCalculator: ConditionsCalculator,
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HoleStrategyState())
    val uiState: StateFlow<HoleStrategyState> = _uiState.asStateFlow()

    fun calculateAdjustedCarry(
        club: Club,
        targetBearing: Int
    ) {
        viewModelScope.launch {
            // Fetch current weather
            val weather = weatherRepository.getCurrentWeather()

            if (weather != null) {
                // Calculate adjustment
                val adjustment = conditionsCalculator.calculateAdjustment(
                    weather = weather,
                    targetBearing = targetBearing,
                    baseCarryMeters = club.averageCarryMeters
                )

                // Apply to club carry
                val adjustedCarry = (club.averageCarryMeters * adjustment.carryModifier).toInt()

                // Update UI state
                _uiState.update { state ->
                    state.copy(
                        recommendedClub = club,
                        baseCarry = club.averageCarryMeters,
                        adjustedCarry = adjustedCarry,
                        adjustmentReason = adjustment.reason,
                        weather = weather
                    )
                }
            }
        }
    }
}
*/

// Example 2: Use in a domain use case
/*
@Singleton
class CalculateClubRecommendationUseCase @Inject constructor(
    private val conditionsCalculator: ConditionsCalculator
) {

    suspend operator fun invoke(
        weather: WeatherData,
        targetDistanceMeters: Int,
        targetBearing: Int,
        availableClubs: List<Club>
    ): ClubRecommendation {
        // For each club, calculate adjusted carry
        val clubsWithAdjustments = availableClubs.map { club ->
            val adjustment = conditionsCalculator.calculateAdjustment(
                weather = weather,
                targetBearing = targetBearing,
                baseCarryMeters = club.averageCarryMeters
            )

            val adjustedCarry = (club.averageCarryMeters * adjustment.carryModifier).toInt()

            ClubWithAdjustment(
                club = club,
                baseCarry = club.averageCarryMeters,
                adjustedCarry = adjustedCarry,
                modifier = adjustment.carryModifier,
                reason = adjustment.reason
            )
        }

        // Find club closest to target distance
        val recommended = clubsWithAdjustments.minByOrNull { clubAdj ->
            kotlin.math.abs(clubAdj.adjustedCarry - targetDistanceMeters)
        }

        return ClubRecommendation(
            club = recommended!!.club,
            adjustedCarry = recommended.adjustedCarry,
            reason = recommended.reason
        )
    }
}
*/

// Example 3: Convenience method on WeatherData (already implemented)
/*
fun demonstrateConvenienceMethod() {
    val weather = WeatherData(
        windSpeedMps = 5.0,
        windDegrees = 180,
        temperatureCelsius = 20.0,
        humidity = 50,
        timestamp = System.currentTimeMillis(),
        location = Location(37.7749, -122.4194)
    )

    // Option A: Use WeatherData convenience method (creates calculator internally)
    val adjustment1 = weather.conditionsAdjustment(
        targetBearing = 0,
        baseCarryMeters = 100
    )

    // Option B: Inject calculator and use directly (PREFERRED in production)
    val calculator = ConditionsCalculator() // In reality, this is injected
    val adjustment2 = calculator.calculateAdjustment(
        weather = weather,
        targetBearing = 0,
        baseCarryMeters = 100
    )

    // Both produce same result, but Option B is better for:
    // - Testability (can mock calculator)
    // - Performance (reuses singleton instance)
    // - Dependency injection best practices
}
*/

// Example 4: Use in Forecaster HUD (future Task 13+)
/*
@Composable
fun ForecasterHUD(
    weather: WeatherData,
    targetBearing: Int,
    selectedClub: Club,
    viewModel: ForecasterViewModel = hiltViewModel()
) {
    val adjustment by remember(weather, targetBearing, selectedClub) {
        derivedStateOf {
            viewModel.calculateAdjustment(weather, targetBearing, selectedClub.averageCarryMeters)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Weather Conditions",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Wind display
            Row {
                Icon(Icons.Default.Air, contentDescription = "Wind")
                Text("${weather.windSpeedMps.toInt()} m/s from ${weather.windDegrees}°")
            }

            // Temperature display
            Row {
                Icon(Icons.Default.Thermostat, contentDescription = "Temperature")
                Text("${weather.temperatureCelsius.toInt()}°C")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Divider()

            Spacer(modifier = Modifier.height(8.dp))

            // Carry adjustment
            Text(
                text = "Carry Adjustment",
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = adjustment.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = if (adjustment.carryModifier < 1.0) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        }
    }
}
*/

// Example 5: Test usage (see ConditionsCalculatorTest.kt for full examples)
/*
class SomeFeatureTest {

    private lateinit var conditionsCalculator: ConditionsCalculator

    @Before
    fun setup() {
        conditionsCalculator = ConditionsCalculator()
    }

    @Test
    fun `calculate carry adjustment for specific scenario`() {
        // Given
        val weather = WeatherData(
            windSpeedMps = 5.0,
            windDegrees = 0,
            temperatureCelsius = 15.0,
            humidity = 50,
            timestamp = System.currentTimeMillis(),
            location = Location(37.7749, -122.4194)
        )

        // When
        val adjustment = conditionsCalculator.calculateAdjustment(
            weather = weather,
            targetBearing = 0,
            baseCarryMeters = 100
        )

        // Then
        assertEquals(1.05, adjustment.carryModifier, 0.01)
        assertTrue(adjustment.reason.contains("tailwind"))
    }
}
*/

// Example 6: Real-world workflow in a round
/*
suspend fun handleShotPreparation(
    currentHole: CourseHole,
    position: Location,
    targetBearing: Int,
    weatherRepository: WeatherRepository,
    clubRepository: ClubRepository,
    conditionsCalculator: ConditionsCalculator
) {
    // Step 1: Fetch current weather
    val weather = weatherRepository.getCurrentWeather() ?: return

    // Step 2: Get player's clubs
    val clubs = clubRepository.getPlayerClubs()

    // Step 3: Calculate distance to target
    val distanceToPin = currentHole.calculateDistanceToPin(position)

    // Step 4: For each club, calculate adjusted carry
    val recommendations = clubs.map { club ->
        val adjustment = conditionsCalculator.calculateAdjustment(
            weather = weather,
            targetBearing = targetBearing,
            baseCarryMeters = club.averageCarryMeters
        )

        ClubRecommendation(
            club = club,
            baseCarry = club.averageCarryMeters,
            adjustedCarry = (club.averageCarryMeters * adjustment.carryModifier).toInt(),
            distanceToTarget = distanceToPin,
            adjustment = adjustment
        )
    }

    // Step 5: Sort by closest to target
    val bestMatch = recommendations.minByOrNull { rec ->
        kotlin.math.abs(rec.adjustedCarry - distanceToPin)
    }

    // Step 6: Present to user
    println("Recommended club: ${bestMatch?.club?.name}")
    println("Adjusted carry: ${bestMatch?.adjustedCarry}m")
    println("Reason: ${bestMatch?.adjustment?.reason}")
}
*/

/**
 * Key points for using ConditionsCalculator:
 *
 * 1. **Injection**: Always inject via constructor, never create instances manually
 * 2. **Thread-safety**: The calculator is stateless and thread-safe
 * 3. **Performance**: All calculations are synchronous and fast (< 1ms)
 * 4. **Validation**: Input validation is built-in, check for IllegalArgumentException
 * 5. **Weather freshness**: Always use recent weather data (< 15 minutes old)
 * 6. **Bearing accuracy**: Ensure target bearing is accurate for best results
 * 7. **Base carry**: Use player's actual average carry, not theoretical max
 * 8. **Rounding**: Adjust carry distances are in whole meters
 *
 * Common pitfalls:
 * - Using stale weather data
 * - Incorrect bearing (using "from" instead of "to" direction)
 * - Applying adjustments multiple times (compounding)
 * - Not handling null weather (offline scenario)
 */
