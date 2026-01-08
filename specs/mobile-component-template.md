# Component: <Component Name>

> Brief description of this reusable UI component

## 1. Purpose

What UI problem does this component solve? Where will it be used?

## 2. Goals

- G1: Be reusable across multiple screens
- G2: Follow platform design guidelines
- G3: Support accessibility requirements

## 3. Non-Goals

- NG1: Complex business logic (keep it presentation-only)
- NG2: Network calls or data fetching

## 4. Component API

### Android (Jetpack Compose)

```kotlin
@Composable
fun <ComponentName>(
    // Required parameters
    title: String,
    onClick: () -> Unit,
    
    // Optional parameters with defaults
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
)
```

### iOS (SwiftUI)

```swift
struct <ComponentName>: View {
    // Required parameters
    let title: String
    let action: () -> Void
    
    // Optional parameters with defaults
    var isEnabled: Bool = true
    var isLoading: Bool = false
}
```

## 5. Visual States

### Default State
- Normal appearance
- Interactive

### Disabled State
- Reduced opacity (60%)
- Non-interactive

### Loading State
- Shows progress indicator
- Non-interactive

### Pressed/Highlighted State
- Visual feedback on touch
- Platform-appropriate animation

### Error State (if applicable)
- Error styling (red border, icon)
- Error message display

## 6. Acceptance Criteria

### Appearance

#### A1: Default rendering
- GIVEN: Component with required props
- WHEN: Rendered
- THEN: Displays title and default styling

#### A2: Platform styling
- GIVEN: Component rendered on Android
- WHEN: Displayed
- THEN: Uses Material 3 styling
- ---
- GIVEN: Component rendered on iOS
- WHEN: Displayed
- THEN: Uses SwiftUI native styling

### Interaction

#### A3: Click/tap handling
- GIVEN: Component is enabled
- WHEN: User taps component
- THEN: onClick/action callback is invoked

#### A4: Disabled state
- GIVEN: enabled = false
- WHEN: User attempts to tap
- THEN: No callback is invoked
- AND: Visual appears disabled

#### A5: Loading state
- GIVEN: isLoading = true
- WHEN: Displayed
- THEN: Shows loading indicator
- AND: User interaction is blocked

### Accessibility

#### A6: Screen reader
- GIVEN: VoiceOver/TalkBack enabled
- WHEN: Component is focused
- THEN: Announces purpose and state

#### A7: Touch target
- GIVEN: Component rendered
- WHEN: Measuring touch area
- THEN: Minimum 44x44pt (iOS) / 48x48dp (Android)

## 7. Constraints

### C1: Performance
- Recomposition/redraw < 16ms
- No unnecessary state updates

### C2: Customization
- Accepts Modifier (Android) for layout customization
- Does not expose internal styling

### C3: Theming
- Respects app theme (colors, typography)
- Supports dark mode

## 8. Usage Examples

### Android

```kotlin
// Basic usage
<ComponentName>(
    title = "Submit",
    onClick = { viewModel.onSubmit() }
)

// With loading state
<ComponentName>(
    title = "Submit",
    onClick = { viewModel.onSubmit() },
    isLoading = uiState.isLoading,
    enabled = !uiState.isLoading
)

// Custom modifier
<ComponentName>(
    title = "Submit",
    onClick = { },
    modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
)
```

### iOS

```swift
// Basic usage
<ComponentName>(title: "Submit") {
    viewModel.onSubmit()
}

// With loading state
<ComponentName>(
    title: "Submit",
    action: viewModel.onSubmit,
    isLoading: viewModel.isLoading,
    isEnabled: !viewModel.isLoading
)

// In a form
Form {
    <ComponentName>(title: "Submit") {
        viewModel.onSubmit()
    }
}
```

## 9. Testing Strategy

### Unit/Snapshot Tests
- Render in each state
- Compare against baseline
- Test with different themes

### Interaction Tests
- Click/tap triggers callback
- Disabled prevents callback
- Loading prevents callback

### Accessibility Tests
- Screen reader announces correctly
- Touch targets meet minimum size
- Focus order is correct
