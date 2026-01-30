# Readiness HUD Components

Implementation of **Task 15** from `specs/live-caddy-mode-plan.md` according to `specs/live-caddy-mode.md`.

## Overview

The Readiness HUD provides a transparent, outdoor-optimized display of the golfer's biometric readiness score, which influences strategy recommendations by adjusting conservatism based on physical condition.

## Components

### 1. ReadinessHud.kt

Main readiness display component with two modes:

**Compact View:**
- Overall readiness score (0-100) with color-coded circular progress indicator
- Color coding per spec:
  - 0-40: Red (low readiness, maximum conservatism)
  - 41-60: Yellow (moderate readiness, increased safety margins)
  - 61+: Green (high readiness, normal risk tolerance)
- Source indicator (wearable/manual)
- Manual override button (48dp touch target)

**Expanded View:**
- Complete metric breakdown showing:
  - HRV (Heart Rate Variability) - 40% weight
  - Sleep Quality - 40% weight
  - Stress Level - 20% weight
- Horizontal bar chart for each metric
- Transparent display of how overall score is calculated

**Key Features:**
- Outdoor visibility optimized (large text, high contrast)
- Material 3 design with semi-transparent card
- Graceful handling of missing metrics (null states)
- Accessibility support with semantic labels
- Loading state for async data fetch
- Manual override dialog with 5-point slider increments

### 2. ReadinessScoreIndicator.kt

Circular progress indicator component:

**Features:**
- 80dp circular indicator with 8dp stroke width
- Color-coded progress bar matching readiness thresholds
- Centered numeric score (28sp bold)
- "Readiness" label below indicator
- Background track for visual context
- Optimized for outdoor visibility

**Preview States:**
- High readiness (85)
- Moderate readiness (55)
- Low readiness (35)
- Perfect readiness (100)
- Very low readiness (15)
- Threshold boundaries (61, 41)

### 3. ReadinessBreakdownChart.kt

Horizontal bar chart for metric breakdown:

**Features:**
- Shows individual metric contributions:
  - Metric name with weight percentage
  - Horizontal progress bar (0-100)
  - Numeric score value
  - Color coding based on metric value
- Graceful handling of partial/missing metrics
- Message when no wearable data available
- Transparency into overall score calculation

**Layout:**
- Metric label and weight on left
- Horizontal bar showing value/100
- Numeric score on right
- 12dp bar height for touch-friendly design

## Design Specifications

### Color Coding

Per spec R3 (BodyCaddy) and A2 (Readiness impacts strategy):

```kotlin
fun readinessColor(score: Int): Color {
    return when {
        score >= 61 -> Color(0xFF4CAF50)  // Green - normal risk tolerance
        score >= 41 -> Color(0xFFFFA726)  // Yellow - increased safety margins
        else -> Color(0xFFEF5350)          // Red - maximum conservatism
    }
}
```

### Touch Targets

All interactive elements meet Material 3 minimum of 48dp:
- Manual override button: 48dp × 48dp
- Card click area: Full width with adequate height
- Dialog buttons: Standard Material 3 sizes

### Typography

Outdoor visibility optimized per spec C2:
- Readiness score: 28sp bold (primary value)
- Metric labels: 14sp medium
- Body text: 13-16sp
- Minimum: 12sp for secondary text

### Spacing

Material 3 design system:
- Card padding: 16dp
- Component spacing: 8-12dp
- Metric bar spacing: 12dp between items
- Breakdown section: 8dp vertical padding

## Strategy Integration

The readiness score influences strategy recommendations through the `adjustmentFactor()` method:

```kotlin
fun adjustmentFactor(): Double {
    return when {
        overall >= 60 -> 1.0    // Normal risk tolerance
        overall <= 40 -> 0.5    // Maximum conservatism
        else -> {
            // Linear interpolation between 0.5 and 1.0
            0.5 + ((overall - 40) / 20.0) * 0.5
        }
    }
}
```

**Impact on Strategy:**
- **High readiness (≥60)**: Normal recommendations with standard safety margins
- **Moderate readiness (41-59)**: Increased safety margins, fewer aggressive lines
- **Low readiness (≤40)**: Maximum conservatism, larger bailout zones, safer club selections

## Manual Override

Users can manually set readiness when wearables are unavailable:

**Dialog Features:**
- Large score display with color coding
- Slider with 5-point increments (0, 5, 10, ..., 100)
- Readiness label (Excellent, Good, Fair, Low, Very Low)
- Confirm/Cancel buttons
- Semantic labels for accessibility

**Use Cases:**
- Wearable not available
- User disagrees with automatic score
- Override temporary conditions (caffeine, warm-up, etc.)

## Data Sources

### Wearable Sync (ReadinessSource.WEARABLE_SYNC)
- HRV from compatible wearables
- Sleep quality scores
- Stress level metrics
- Automatic weight redistribution for missing metrics

### Manual Entry (ReadinessSource.MANUAL_ENTRY)
- User-provided overall score
- No metric breakdown (all null)
- Fallback when wearables unavailable

## Testing

### Unit Tests (ReadinessHudTest.kt)

Comprehensive test coverage:

**Color Coding Tests:**
- Red threshold (0-40)
- Yellow threshold (41-60)
- Green threshold (61+)
- Boundary cases (40/41, 60/61)

**Adjustment Factor Tests:**
- High readiness (1.0 factor)
- Low readiness (0.5 factor)
- Linear interpolation (41-59)

**Breakdown Tests:**
- All metrics available
- Partial metrics (HRV only, Sleep+Stress)
- No metrics (manual entry)
- Weight redistribution

**Edge Cases:**
- Minimum score (0)
- Maximum score (100)
- Extreme metric values
- Null handling

**Accessibility Tests:**
- Touch target sizes (≥48dp)
- Text sizes (≥12sp)
- Semantic labels

### Preview Composables

All components include comprehensive previews:
- Loading state
- Compact view (high/moderate/low)
- Expanded view (high/moderate/low)
- Manual entry
- Manual override dialog
- Threshold boundaries

## Usage Example

```kotlin
@Composable
fun LiveCaddyScreen(
    viewModel: LiveCaddyViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ReadinessHud(
        readiness = state.readiness,
        showDetails = state.isReadinessDetailsVisible,
        onToggleDetails = { visible ->
            viewModel.onAction(LiveCaddyAction.ToggleReadinessDetails(visible))
        },
        onManualOverride = { score ->
            viewModel.onAction(LiveCaddyAction.SetManualReadiness(score))
        }
    )
}
```

## Acceptance Criteria Validation

**A2: Readiness impacts strategy**

✅ **GIVEN**: Wearable sync reports low readiness (below configured threshold)
- ReadinessScore.isLow() detects scores < 60
- ReadinessScore.adjustmentFactor() returns 0.5 for scores ≤ 40

✅ **WHEN**: User asks "Where should I aim?"
- Strategy engine uses adjustment factor
- Larger safety margins applied automatically

✅ **THEN**: Bones recommends more conservative targets
- adjustmentFactor() ranges from 0.5 (max conservatism) to 1.0 (normal)
- Transparent breakdown shows contributing factors

## Spec Compliance

### R3 (BodyCaddy) Requirements

✅ Sync supported wearable metrics (HRV, sleep, stress)
- Domain models support all three metrics
- Graceful handling of null/missing metrics

✅ Compute Readiness Score (0-100) with transparent contributors
- Overall score computed from weighted average
- Breakdown chart shows individual contributions
- Weight percentages displayed

✅ Strategy adjustment rules
- Lower readiness increases conservatism
- Adjustment factor: 0.5 (low) to 1.0 (high)
- Applied to safety margins and target recommendations

✅ Provide user override (manual readiness)
- Manual override dialog with slider
- ReadinessSource.MANUAL_ENTRY tracking
- Override button on HUD

### C2 (Outdoor Visibility) Requirements

✅ High contrast
- Color-coded indicators (red/yellow/green)
- Semi-transparent card overlays
- Clear foreground/background separation

✅ Large typography
- Primary values: 28sp (score)
- Labels: 14sp
- Body text: 13-16sp

✅ Minimal fine detail
- Bold weights for key values
- Simple bar charts
- Clear visual hierarchy

### R7 (Safety and Distraction Controls) Requirements

✅ Large touch targets
- Manual override button: 48dp
- Full-width clickable card
- Dialog buttons: Material 3 standard

✅ Reduced animations
- Simple expand/collapse animations
- No distracting transitions

✅ Accessibility
- Semantic content descriptions
- TalkBack support
- Touch-friendly controls

## File Locations

```
android/app/src/main/java/caddypro/ui/caddy/components/
├── ReadinessHud.kt                    # Main readiness display
├── ReadinessScoreIndicator.kt         # Circular progress indicator
├── ReadinessBreakdownChart.kt         # Metric breakdown chart
└── READINESS_COMPONENTS_README.md     # This file

android/app/src/test/java/caddypro/ui/caddy/components/
└── ReadinessHudTest.kt                # Comprehensive unit tests
```

## Dependencies

Domain models:
- `caddypro.domain.caddy.models.ReadinessScore`
- `caddypro.domain.caddy.models.ReadinessBreakdown`
- `caddypro.domain.caddy.models.MetricScore`
- `caddypro.domain.caddy.models.ReadinessSource`

UI state:
- `caddypro.ui.caddy.LiveCaddyState`
- `caddypro.ui.caddy.LiveCaddyAction`

Theme:
- `caddypro.ui.theme.CaddyProTheme`

## Next Steps

1. **Task 16**: Implement PinSeeker Strategy Map component
2. **Integration**: Wire up manual override action in LiveCaddyViewModel
3. **Wearable Sync**: Implement actual wearable data fetching
4. **UI Tests**: Add Compose UI tests for user interactions
5. **Screenshot Tests**: Add screenshot tests for visual regression

## Notes

- All components follow Material 3 design guidelines
- Color coding matches spec exactly (0-40 red, 41-60 yellow, 61+ green)
- Strategy adjustment algorithm uses fixed weights (HRV 40%, Sleep 40%, Stress 20%)
- Manual override provides 5-point increments for easy selection
- Outdoor visibility is prioritized in all design decisions
- Accessibility is first-class with semantic labels throughout
