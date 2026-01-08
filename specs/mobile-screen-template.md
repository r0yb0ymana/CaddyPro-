# Feature: <Screen Name>

> Brief description of this screen's purpose

## 1. Problem Statement

What user problem does this screen solve? Why is it needed?

## 2. Goals

- G1: Primary goal
- G2: Secondary goal

## 3. Non-Goals

- NG1: What this screen explicitly doesn't do
- NG2: Features deferred to future work

## 4. Functional Requirements

### R1: Screen Layout

#### Android (Jetpack Compose)
- [ ] Material 3 components used
- [ ] Follows Material Design guidelines
- [ ] Responsive to screen sizes
- [ ] Handles configuration changes (rotation)
- [ ] Edge-to-edge display support

#### iOS (SwiftUI)
- [ ] Native SwiftUI components
- [ ] Follows Human Interface Guidelines
- [ ] Adapts to all iPhone sizes
- [ ] Safe area handling
- [ ] Dynamic Type support

### R2: Navigation
- Entry point(s): How user reaches this screen
- Exit point(s): Where user can navigate from here
- Back button behavior
- Deep link support (if applicable)

### R3: Data Loading
- Data source (API, local DB, etc.)
- Loading state handling
- Empty state handling
- Error state handling
- Pull-to-refresh (if applicable)

### R4: User Interactions
- Primary action(s)
- Secondary action(s)
- Gestures supported
- Keyboard handling

### R5: State Management
- ViewModel state properties
- State transitions
- Side effects (API calls, navigation)

## 5. Acceptance Criteria

### Display

#### A1: Initial load
- GIVEN: User navigates to this screen
- WHEN: Screen loads
- THEN: [Expected initial state]

#### A2: Loading state
- GIVEN: Data is being fetched
- WHEN: Screen is displayed
- THEN: Loading indicator is shown
- AND: User cannot interact with placeholder content

#### A3: Empty state
- GIVEN: No data available
- WHEN: Screen loads
- THEN: Empty state message is shown
- AND: Call-to-action is provided (if applicable)

#### A4: Error state
- GIVEN: Data fetch fails
- WHEN: Error occurs
- THEN: Error message is displayed
- AND: Retry option is available

### User Actions

#### A5: Primary action
- GIVEN: [Precondition]
- WHEN: User performs [action]
- THEN: [Expected result]

#### A6: Navigation
- GIVEN: User is on this screen
- WHEN: User taps back or navigates away
- THEN: [Navigation behavior]

### Accessibility

#### A7: Screen reader support
- GIVEN: VoiceOver/TalkBack enabled
- WHEN: User navigates screen
- THEN: All content is accessible
- AND: Reading order is logical

#### A8: Dynamic Type (iOS) / Font Scaling (Android)
- GIVEN: User has increased text size
- WHEN: Screen is displayed
- THEN: Text scales appropriately
- AND: Layout remains usable

## 6. Constraints & Invariants

### C1: Performance
- Screen renders within X ms
- Scrolling maintains 60fps
- Memory usage < X MB

### C2: Platform Guidelines
- Android: Material 3
- iOS: Human Interface Guidelines

### C3: API Dependencies
```json
// Endpoint(s) used by this screen
// GET/POST /api/...
```

## 7. Open Questions

- Q1: [Question needing clarification]

## 8. Data Model

### ScreenState
```kotlin
// Android
data class <Screen>State(
    val isLoading: Boolean = false,
    val error: String? = null,
    val data: <DataType>? = null
)
```

```swift
// iOS
struct <Screen>State {
    var isLoading: Bool = false
    var error: String?
    var data: <DataType>?
}
```

## 9. UI Preview

[Include wireframe or mockup reference if available]

## 10. Testing Strategy

### Unit Tests
- ViewModel state changes
- Data transformation
- Validation logic

### UI Tests
- Screen renders correctly
- User interactions work
- Navigation functions
- Accessibility compliance
