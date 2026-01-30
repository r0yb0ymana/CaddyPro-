# Forecaster HUD Components

This directory contains the UI components for the Live Caddy Mode Forecaster HUD, providing real-time weather information optimized for outdoor golf course visibility.

## Overview

The Forecaster HUD displays current weather conditions and their impact on ball flight, helping golfers make informed club selection and strategy decisions during a round.

**Spec reference**: `specs/live-caddy-mode.md` R2 (Forecaster HUD)
**Plan reference**: `specs/live-caddy-mode-plan.md` Task 14
**Acceptance criteria**: A1 (Weather HUD renders within 2 seconds)

## Components

### ForecasterHud.kt

Main container component for the weather HUD.

**Features:**
- Compact view (always visible): wind speed/direction + temperature/humidity
- Expandable details view: conditions adjustment with carry modifier percentage
- Loading state with progress indicator
- High contrast Material 3 design with semi-transparent card
- Animated expansion/collapse transitions
- Null weather handling

**Usage:**
```kotlin
ForecasterHud(
    weather = weatherData,
    isExpanded = isExpanded,
    onToggleExpanded = { expanded -> /* handle toggle */ },
    targetBearing = 0, // Optional: shot direction for adjustment
    baseCarryMeters = 150 // Optional: expected carry for adjustment
)
```

**Preview Composables:**
- `PreviewForecasterHudLoading` - Loading state
- `PreviewForecasterHudCompact` - Collapsed view
- `PreviewForecasterHudExpanded` - Expanded with conditions adjustment
- `PreviewForecasterHudColdHeadwind` - Hurting conditions example
- `PreviewForecasterHudHotTailwind` - Helping conditions example

### WindIndicator.kt

Displays wind speed and direction with visual arrow indicator.

**Features:**
- Wind speed in mph (converted from m/s)
- Directional arrow pointing in wind direction
- Compass direction label (N, NE, E, SE, S, SW, W, NW)
- Large typography (20sp) for outdoor visibility
- Bold arrow graphics with rotation

**Conversion:**
- Wind speed: meters/second to mph (1 m/s = 2.237 mph)
- Wind direction: meteorological convention (from direction, 0° = North)

**Usage:**
```kotlin
WindIndicator(
    speedMps = 5.5,
    degrees = 270 // Wind from West
)
```

**Preview Composables:**
- `PreviewWindIndicatorNorth` - 0° (North wind)
- `PreviewWindIndicatorEast` - 90° (East wind)
- `PreviewWindIndicatorSouthwest` - 225° (SW wind)
- `PreviewWindIndicatorLight` - Light wind (1.5 m/s)
- `PreviewWindIndicatorStrong` - Strong wind (12 m/s)

### TemperatureIndicator.kt

Displays temperature and humidity with icon indicators.

**Features:**
- Temperature in Fahrenheit (converted from Celsius)
- Humidity percentage
- Icons for visual recognition (thermometer and water drop)
- Large typography (20sp) for outdoor visibility
- Right-aligned layout

**Conversion:**
- Temperature: Celsius to Fahrenheit (F = C × 9/5 + 32)

**Usage:**
```kotlin
TemperatureIndicator(
    celsius = 22.0,
    humidity = 65
)
```

**Preview Composables:**
- `PreviewTemperatureIndicatorWarm` - 28°C (warm weather)
- `PreviewTemperatureIndicatorCold` - 5°C (cold weather)
- `PreviewTemperatureIndicatorHotHumid` - 35°C, 85% (hot and humid)
- `PreviewTemperatureIndicatorMild` - 20°C (mild weather)
- `PreviewTemperatureIndicatorFreezing` - 0°C (freezing)

### ConditionsChip.kt

Shows conditions adjustment as a percentage with explanation.

**Features:**
- Percentage adjustment to carry distance (+5%, -8%, etc.)
- Visual indicator (up/down arrow) for direction
- Human-readable explanation of factors
- Color-coded chip (green for helping, red for hurting)
- Large typography (16sp) for outdoor visibility

**Color Scheme:**
- Green (#4CAF50): Helping conditions (tailwind, warm air)
- Red (#F44336): Hurting conditions (headwind, cold air)
- Gray: Neutral conditions

**Usage:**
```kotlin
ConditionsChip(
    carryModifier = 1.05, // 5% more carry
    reason = "Tailwind (+3 m/s) and warm air (+5°C above standard)"
)
```

**Preview Composables:**
- `PreviewConditionsChipHelping` - +5% carry (helping)
- `PreviewConditionsChipHurting` - -8% carry (hurting)
- `PreviewConditionsChipNeutral` - 0% (standard conditions)
- `PreviewConditionsChipStrongHelping` - +12% (strong tailwind)
- `PreviewConditionsChipModerateHurting` - -5% (light headwind)

## Design Principles

### Outdoor Visibility (Spec C2)

All components follow outdoor visibility requirements:

1. **Large Typography**
   - Primary values: 20sp minimum (temperature, wind speed)
   - Secondary values: 14sp minimum (humidity, direction)
   - Bold font weights for critical information

2. **High Contrast**
   - Material 3 color scheme with strong contrast ratios
   - Semi-transparent card background (92% opacity)
   - Distinct colors for semantic meaning (green/red for conditions)

3. **Minimal Fine Detail**
   - Simple, bold icons
   - Clear directional indicators
   - No gradients or complex visual effects

4. **Touch Targets**
   - 48dp minimum (per Material 3 guidelines)
   - Card-level interaction for expansion
   - Large clickable areas

### Material 3 Design

Components use Material 3 design system:

- `MaterialTheme.colorScheme` for consistent theming
- `MaterialTheme.typography` for text styles
- `Card` with elevation and alpha for depth
- `AssistChip` for informational displays
- `AnimatedVisibility` for smooth transitions

### Performance

Per acceptance criteria A1 (Weather HUD renders within 2 seconds):

- Lightweight Composables with minimal recomposition
- Efficient Canvas drawing for wind arrow
- Simple calculations (conversions, percentages)
- Loading indicator while fetching data

## Testing

### Unit Tests

`ForecasterHudTest.kt` provides comprehensive unit tests:

1. **Wind Direction Conversion**
   - Cardinal directions (N, E, S, W)
   - Intercardinal directions (NE, SE, SW, NW)
   - Boundary cases (22.5°, 67.5°, etc.)
   - Wraparound at 360°

2. **Temperature Conversion**
   - Standard values (0°C, 15°C, 20°C)
   - Extreme values (-40°C, 50°C)
   - Freezing point and below

3. **Wind Speed Conversion**
   - Calm to hurricane force
   - Rounding accuracy

4. **Conditions Adjustment**
   - Helping conditions (positive percentages)
   - Hurting conditions (negative percentages)
   - Neutral conditions (0%)

5. **WeatherData Integration**
   - Tailwind scenarios
   - Headwind scenarios
   - Crosswind scenarios

6. **Outdoor Visibility**
   - Minimum font size verification
   - Color contrast validation

### Compose UI Tests

Future work (requires Compose testing framework):

- Verify layout rendering
- Test expansion/collapse animations
- Validate touch target sizes
- Check color contrast ratios
- Test with TalkBack for accessibility

## Integration

### With LiveCaddyViewModel

The ForecasterHud integrates with LiveCaddyViewModel:

```kotlin
@Composable
fun LiveCaddyScreen(
    viewModel: LiveCaddyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ForecasterHud(
        weather = state.weather,
        isExpanded = state.isWeatherHudExpanded,
        onToggleExpanded = { expanded ->
            viewModel.onAction(LiveCaddyAction.ToggleWeatherHud(expanded))
        },
        targetBearing = state.holeStrategy?.targetBearing,
        baseCarryMeters = state.selectedClub?.estimatedCarry
    )
}
```

### With WeatherData

The components consume `WeatherData` domain model:

```kotlin
data class WeatherData(
    val windSpeedMps: Double,
    val windDegrees: Int,
    val temperatureCelsius: Double,
    val humidity: Int,
    val timestamp: Long,
    val location: Location
)
```

Conversions are handled within components:
- Wind speed: m/s → mph
- Temperature: Celsius → Fahrenheit
- Direction: degrees → compass labels

### With ConditionsCalculator

For expanded view, components use `WeatherData.conditionsAdjustment()`:

```kotlin
val adjustment = weather.conditionsAdjustment(
    targetBearing = 0,
    baseCarryMeters = 150
)

ConditionsChip(
    carryModifier = adjustment.carryModifier,
    reason = adjustment.reason
)
```

## Accessibility

Components support accessibility features:

1. **Content Descriptions**
   - Icons have descriptive labels
   - Arrow direction is announced

2. **Semantic Colors**
   - Not color-only (icons + text + arrows)
   - High contrast for visibility

3. **Dynamic Text Sizing**
   - Uses Material 3 typography scale
   - Scales with system font size settings

4. **Touch Targets**
   - Minimum 48dp per guidelines
   - Card-level interaction area

## Future Enhancements

Potential improvements beyond MVP:

1. **Unit Preferences**
   - User setting for mph vs. m/s
   - Fahrenheit vs. Celsius toggle

2. **Historical Weather**
   - Show weather trend (improving/worsening)
   - Forecast for later holes

3. **Detailed Breakdown**
   - Separate wind component display (headwind/crosswind)
   - Air density factor visualization

4. **Haptic Feedback**
   - Vibration on expansion/collapse
   - Alert on significant condition changes

5. **Refresh Controls**
   - Pull-to-refresh weather data
   - Auto-refresh interval
   - Manual refresh button

## References

- **Spec**: `specs/live-caddy-mode.md` sections R2, C2, A1
- **Plan**: `specs/live-caddy-mode-plan.md` Task 14
- **Domain Models**: `domain/caddy/models/WeatherData.kt`
- **State Management**: `ui/caddy/LiveCaddyState.kt`
- **Tests**: `test/.../ui/caddy/components/ForecasterHudTest.kt`
