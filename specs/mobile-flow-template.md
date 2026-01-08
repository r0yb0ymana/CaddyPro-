# Feature: <Flow Name>

> Multi-screen user flow (e.g., onboarding, checkout, registration)

## 1. Problem Statement

What user journey problem does this flow solve?

## 2. Goals

- G1: Guide user through [process]
- G2: Collect necessary information
- G3: Provide clear progress indication
- G4: Handle errors gracefully

## 3. Non-Goals

- NG1: Alternative paths not in this flow
- NG2: Features for future iterations

## 4. Flow Overview

```
┌─────────────────┐
│   Screen 1      │
│   [Entry Point] │
└────────┬────────┘
         │
         ▼
┌─────────────────┐     ┌─────────────┐
│   Screen 2      │────▶│   Error     │
│   [Step Name]   │     │   Handling  │
└────────┬────────┘     └─────────────┘
         │
         ▼
┌─────────────────┐
│   Screen 3      │
│   [Completion]  │
└─────────────────┘
```

## 5. Screens in Flow

### Screen 1: [Name]
- **Purpose:** Entry point, initial information
- **Inputs:** [What user provides]
- **Validation:** [Rules applied]
- **Next:** Screen 2 on success

### Screen 2: [Name]
- **Purpose:** [What this step accomplishes]
- **Inputs:** [What user provides]
- **Validation:** [Rules applied]
- **Next:** Screen 3 on success
- **Back:** Returns to Screen 1

### Screen 3: [Name]
- **Purpose:** Completion/confirmation
- **Inputs:** None (or final confirmation)
- **Actions:** [What happens here]
- **Exit:** Navigate to main app

## 6. Functional Requirements

### R1: Progress Indication
- Show current step (e.g., "Step 2 of 3")
- Visual progress bar or dots
- Users know how much is remaining

### R2: Navigation
- Back button returns to previous step
- Progress is preserved when going back
- Swipe back gesture (iOS) / predictive back (Android)
- Confirm before abandoning flow with unsaved data

### R3: Data Persistence
- Save progress between steps
- Survive app backgrounding
- Resume from last completed step
- Clear on successful completion or explicit cancel

### R4: Validation
- Validate each step before proceeding
- Show inline errors
- Disable "Next" until valid

### R5: Error Handling
- Network errors: Show retry option
- Validation errors: Show inline
- Server errors: Show error screen with support contact

## 7. Acceptance Criteria

### Flow Navigation

#### A1: Start flow
- GIVEN: User initiates flow
- WHEN: First screen loads
- THEN: Progress shows "Step 1 of N"
- AND: Back exits flow (with confirmation if data entered)

#### A2: Progress between steps
- GIVEN: User completes current step
- WHEN: User taps "Next" or "Continue"
- THEN: Next screen loads
- AND: Progress indicator updates

#### A3: Navigate back
- GIVEN: User is on step 2+
- WHEN: User taps back
- THEN: Previous screen loads
- AND: Previously entered data is preserved

#### A4: Abandon flow
- GIVEN: User has entered data
- WHEN: User attempts to exit flow
- THEN: Confirmation dialog appears
- AND: User can choose to continue or discard

### Data Handling

#### A5: Persist across sessions
- GIVEN: User completes step 1 and backgrounds app
- WHEN: User returns to app
- THEN: User resumes at step 2
- AND: Step 1 data is preserved

#### A6: Clear on completion
- GIVEN: User completes entire flow
- WHEN: Flow ends
- THEN: All temporary flow data is cleared

### Validation

#### A7: Step validation
- GIVEN: Current step has required fields
- WHEN: Fields are empty or invalid
- THEN: "Next" button is disabled
- AND: Error messages shown on attempt

#### A8: Cross-step validation
- GIVEN: Step 3 depends on data from step 1
- WHEN: User reaches step 3
- THEN: Previous data is validated
- AND: User returns to relevant step if invalid

### Error Scenarios

#### A9: Network failure during submission
- GIVEN: Final step submits data to server
- WHEN: Network request fails
- THEN: Error message is shown
- AND: "Retry" option is available
- AND: User data is not lost

#### A10: Server rejection
- GIVEN: Server rejects submitted data
- WHEN: Error response received
- THEN: User is shown specific error
- AND: Directed to correct step to fix

## 8. Constraints

### C1: State Management
- Flow state separate from global app state
- Use navigation arguments or dedicated state holder
- Survive process death

### C2: Deep Linking
- Each step has unique route
- Can deep link to specific step (with validation)
- Example: `app://flow/{step_number}`

### C3: Analytics
- Track flow start
- Track each step completion
- Track abandonment (which step)
- Track completion time

## 9. Data Model

### FlowState
```kotlin
// Android
data class <Flow>State(
    val currentStep: Int = 1,
    val totalSteps: Int = 3,
    val step1Data: Step1Data? = null,
    val step2Data: Step2Data? = null,
    val isSubmitting: Boolean = false,
    val error: String? = null
)
```

```swift
// iOS
struct <Flow>State {
    var currentStep: Int = 1
    let totalSteps: Int = 3
    var step1Data: Step1Data?
    var step2Data: Step2Data?
    var isSubmitting: Bool = false
    var error: String?
}
```

## 10. Screen Specifications

For each screen in the flow, create a detailed spec using `mobile-screen-template.md`:

- [ ] `specs/<flow>-step1-<name>.md`
- [ ] `specs/<flow>-step2-<name>.md`
- [ ] `specs/<flow>-step3-<name>.md`

## 11. Testing Strategy

### Unit Tests
- Flow state management
- Validation logic per step
- Data transformation

### Integration Tests
- Full flow completion
- Back navigation preserves data
- Error recovery

### E2E Tests
- Happy path (complete flow)
- Abandonment handling
- Deep link entry points
- Resume after backgrounding
