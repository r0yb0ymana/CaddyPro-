# NavCaddy Routing Package

This package implements Task 10 (Routing Orchestrator) of the NavCaddy Engine.

**Spec Reference**: `specs/navcaddy-engine.md` R3, `specs/navcaddy-engine-plan.md` Task 10

## Overview

The Routing Orchestrator is responsible for taking classification results from the Intent Classifier and determining the appropriate routing action. It validates prerequisites, handles no-navigation intents, and returns structured routing decisions.

## Architecture

```
ClassificationResult → RoutingOrchestrator → RoutingResult
                              ↓
                     PrerequisiteChecker
```

## Components

### 1. RoutingResult (sealed class)

Four variants representing routing decisions:

#### Navigate
Direct navigation to a target screen. Used for high-confidence intents with all prerequisites satisfied.

```kotlin
RoutingResult.Navigate(
    target = RoutingTarget(Module.CADDY, "club_adjustment", mapOf("club" to "7-iron")),
    intent = parsedIntent
)
```

#### NoNavigation
Answer without screen change. Used for query/help intents that don't require navigation.

```kotlin
RoutingResult.NoNavigation(
    intent = parsedIntent,
    response = "Let me check your miss patterns..."
)
```

#### PrerequisiteMissing
Required data is missing. User must provide it before intent can be fulfilled.

```kotlin
RoutingResult.PrerequisiteMissing(
    intent = parsedIntent,
    missing = listOf(Prerequisite.RECOVERY_DATA),
    message = "I don't have any recovery data yet..."
)
```

#### ConfirmationRequired
Mid-confidence intent needs user confirmation before routing.

```kotlin
RoutingResult.ConfirmationRequired(
    intent = parsedIntent,
    message = "Did you want to check the weather forecast?"
)
```

### 2. Prerequisite (enum)

Defines data prerequisites that certain intents require:

- **RECOVERY_DATA**: Required for `RECOVERY_CHECK` intent
  - User must have logged recovery data (sleep, HRV, readiness)

- **ROUND_ACTIVE**: Required for `SCORE_ENTRY`, `ROUND_END` intents
  - User must have an active round in progress

- **BAG_CONFIGURED**: Required for `CLUB_ADJUSTMENT`, `SHOT_RECOMMENDATION` intents
  - User must have configured club distances and bag contents

- **COURSE_SELECTED**: Required for `COURSE_INFO` intent
  - User must have selected the course they're playing

### 3. PrerequisiteChecker (interface)

Interface for checking if prerequisites are satisfied:

```kotlin
interface PrerequisiteChecker {
    suspend fun check(prerequisite: Prerequisite): Boolean
    suspend fun checkAll(prerequisites: List<Prerequisite>): List<Prerequisite>
}
```

**Implementation Note**: The actual implementation will query repositories and session context to verify data exists. For now, this is an interface that will be implemented in the data layer.

### 4. RoutingOrchestrator (service)

Main orchestration service that processes classification results:

```kotlin
class RoutingOrchestrator @Inject constructor(
    private val prerequisiteChecker: PrerequisiteChecker
) {
    suspend fun route(classificationResult: ClassificationResult): RoutingResult
}
```

## Routing Logic Flow

1. **Route Classification (high confidence >= 0.75)**:
   - Check if intent is no-navigation type → Return `NoNavigation`
   - Get required prerequisites for intent type
   - Validate prerequisites → If missing, return `PrerequisiteMissing`
   - All checks passed → Return `Navigate`

2. **Confirm Classification (mid confidence 0.50-0.74)**:
   - Return `ConfirmationRequired` with confirmation message

3. **Clarify Classification (low confidence < 0.50)**:
   - Return `NoNavigation` with clarification message
   - (Clarification UI handled by conversation layer)

4. **Error Classification**:
   - Return `NoNavigation` with error message

## Intent Type → Prerequisite Mapping

| Intent Type | Required Prerequisites |
|-------------|----------------------|
| RECOVERY_CHECK | RECOVERY_DATA |
| SCORE_ENTRY | ROUND_ACTIVE |
| ROUND_END | ROUND_ACTIVE |
| CLUB_ADJUSTMENT | BAG_CONFIGURED |
| SHOT_RECOMMENDATION | BAG_CONFIGURED |
| COURSE_INFO | COURSE_SELECTED |
| All others | None |

## No-Navigation Intents

These intents are answered inline without navigation:
- **PATTERN_QUERY**: Query historical miss patterns
- **HELP_REQUEST**: Request help or instructions
- **FEEDBACK**: Provide feedback about the app

## Usage Example

```kotlin
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val intentClassifier: IntentClassifier,
    private val routingOrchestrator: RoutingOrchestrator
) : ViewModel() {

    suspend fun processUserInput(input: String) {
        // Step 1: Classify intent
        val classification = intentClassifier.classify(input)

        // Step 2: Determine routing
        val routingResult = routingOrchestrator.route(classification)

        // Step 3: Handle result
        when (routingResult) {
            is RoutingResult.Navigate -> {
                navigateToTarget(routingResult.target)
            }
            is RoutingResult.NoNavigation -> {
                displayResponse(routingResult.response)
            }
            is RoutingResult.PrerequisiteMissing -> {
                displayPrerequisitePrompt(routingResult.message)
            }
            is RoutingResult.ConfirmationRequired -> {
                showConfirmationDialog(routingResult.message)
            }
        }
    }
}
```

## Testing

Unit tests verify:
- ✅ High-confidence intents route correctly
- ✅ Prerequisites are validated before routing
- ✅ No-navigation intents return responses
- ✅ Mid-confidence intents trigger confirmation
- ✅ Missing prerequisites return appropriate messages
- ✅ All intent types have correct prerequisite mappings

See `RoutingOrchestratorTest.kt` for complete test coverage.

## Next Steps

1. **Task 11**: Implement deep link navigation for Android (NavController integration)
2. **Data Layer**: Implement `PrerequisiteChecker` with actual repository queries
3. **UI Layer**: Build conversation UI that handles all `RoutingResult` variants
4. **Integration**: Wire up complete flow from user input → classification → routing → navigation

## Files

```
routing/
├── RoutingResult.kt              # Sealed class with 4 routing variants
├── Prerequisite.kt               # Enum of data prerequisites
├── PrerequisiteChecker.kt        # Interface for prerequisite validation
├── RoutingOrchestrator.kt        # Main orchestration service
└── README.md                     # This file
```

## Dependencies

- `domain/navcaddy/classifier`: ClassificationResult
- `domain/navcaddy/models`: ParsedIntent, RoutingTarget, IntentType, Module
- Hilt: Dependency injection

## Acceptance Criteria (from Plan)

- [x] Intents route to correct modules
- [x] Prerequisites are validated before navigation
- [x] Missing data triggers appropriate prompts
- [x] No-navigation intents handled correctly
- [x] All intent types have prerequisite mappings defined
- [x] Unit tests provide comprehensive coverage

## Notes

- **Clean Architecture**: Orchestrator is pure domain logic, no UI or framework dependencies
- **Testability**: All dependencies injected, fully mockable
- **Error Handling**: All edge cases handled with user-friendly messages
- **Extensibility**: Easy to add new prerequisites or intent mappings
