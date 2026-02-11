# Routing Orchestrator Flow Diagram

## High-Level Architecture

```
┌─────────────────┐
│   User Input    │
│ "Check my 7i"   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ IntentClassifier│ (Task 6)
│  + Normalizer   │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────┐
│   ClassificationResult      │
│  ┌─────────────────────┐   │
│  │ Route (conf≥0.75)   │   │
│  │ Confirm (0.50-0.74) │   │
│  │ Clarify (<0.50)     │   │
│  │ Error               │   │
│  └─────────────────────┘   │
└─────────┬───────────────────┘
          │
          ▼
┌──────────────────────────────┐
│   RoutingOrchestrator        │ ◄── TASK 10
│   (This Implementation)      │
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────────┐
│      RoutingResult           │
│  ┌──────────────────────┐   │
│  │ Navigate             │   │
│  │ NoNavigation         │   │
│  │ PrerequisiteMissing  │   │
│  │ ConfirmationRequired │   │
│  └──────────────────────┘   │
└──────────┬───────────────────┘
           │
           ▼
┌──────────────────────────────┐
│   ConversationViewModel      │ (Future - Task 18)
│   + UI Handlers              │
└──────────────────────────────┘
```

## Detailed Routing Decision Flow

```
                    ClassificationResult
                            │
        ┌───────────────────┼───────────────────┬──────────┐
        │                   │                   │          │
        ▼                   ▼                   ▼          ▼
    Route(*)            Confirm            Clarify      Error
        │                   │                   │          │
        │                   │                   │          │
        │                   ▼                   ▼          │
        │          ConfirmationRequired    NoNavigation    │
        │                                       │          │
        │                                       └──────────┘
        ▼                                              │
  Is No-Nav Intent?                                    │
  (PATTERN_QUERY,                                      │
   HELP_REQUEST,                                       │
   FEEDBACK)                                           │
        │                                              │
    Yes │ No                                           │
        │   │                                          │
        ▼   │                                          │
   NoNavigation                                        │
        │   │                                          │
        └───┼──────────────────────────────────────────┘
            │                                          │
            ▼                                          │
    Get Required Prerequisites                         │
            │                                          │
            ▼                                          │
    Prerequisites = []?                                │
            │                                          │
        Yes │ No                                       │
            │   │                                      │
            │   ▼                                      │
            │ PrerequisiteChecker.checkAll()           │
            │   │                                      │
            │   ▼                                      │
            │ Missing Prerequisites?                   │
            │   │                                      │
            │ Yes│ No                                  │
            │   │  │                                   │
            │   ▼  │                                   │
            │ PrerequisiteMissing                      │
            │      │                                   │
            └──────┼───────────────────────────────────┘
                   │
                   ▼
               Navigate
```

## Intent Type → Routing Decision Matrix

```
┌──────────────────────┬─────────────────┬────────────┬──────────────┐
│   Intent Type        │ Prerequisites   │ Nav Type   │ Result       │
├──────────────────────┼─────────────────┼────────────┼──────────────┤
│ RECOVERY_CHECK       │ RECOVERY_DATA   │ Screen     │ Nav or Pre   │
│ SCORE_ENTRY          │ ROUND_ACTIVE    │ Screen     │ Nav or Pre   │
│ ROUND_END            │ ROUND_ACTIVE    │ Screen     │ Nav or Pre   │
│ CLUB_ADJUSTMENT      │ BAG_CONFIGURED  │ Screen     │ Nav or Pre   │
│ SHOT_RECOMMENDATION  │ BAG_CONFIGURED  │ Screen     │ Nav or Pre   │
│ COURSE_INFO          │ COURSE_SELECTED │ Screen     │ Nav or Pre   │
├──────────────────────┼─────────────────┼────────────┼──────────────┤
│ PATTERN_QUERY        │ None            │ Inline     │ NoNav        │
│ HELP_REQUEST         │ None            │ Inline     │ NoNav        │
│ FEEDBACK             │ None            │ Inline     │ NoNav        │
├──────────────────────┼─────────────────┼────────────┼──────────────┤
│ DRILL_REQUEST        │ None            │ Screen     │ Navigate     │
│ WEATHER_CHECK        │ None            │ Screen     │ Navigate     │
│ STATS_LOOKUP         │ None            │ Screen     │ Navigate     │
│ ROUND_START          │ None            │ Screen     │ Navigate     │
│ EQUIPMENT_INFO       │ None            │ Screen     │ Navigate     │
│ SETTINGS_CHANGE      │ None            │ Screen     │ Navigate     │
└──────────────────────┴─────────────────┴────────────┴──────────────┘

Legend:
  Nav = Navigate result (route to screen)
  Pre = PrerequisiteMissing result (need data)
  NoNav = NoNavigation result (inline answer)
```

## Prerequisite Validation Flow

```
Intent with Prerequisites
         │
         ▼
┌──────────────────────┐
│ Get Required Prereqs │
│ Based on Intent Type │
└─────────┬────────────┘
          │
          ▼
┌──────────────────────┐
│ PrerequisiteChecker  │
│   .checkAll()        │
└─────────┬────────────┘
          │
          ▼
    Missing Any?
          │
     Yes  │  No
          │   │
          ▼   ▼
  ┌──────────────┐   ┌──────────┐
  │ Prerequisite │   │ Navigate │
  │   Missing    │   │  to      │
  │              │   │ Target   │
  └──────────────┘   └──────────┘
```

## Example Scenarios

### Scenario 1: High Confidence + Prerequisites Met

```
User: "What's my 7-iron yardage?"
  │
  ▼ IntentClassifier
ClassificationResult.Route
  intentType: CLUB_ADJUSTMENT
  confidence: 0.92
  │
  ▼ RoutingOrchestrator
Check prerequisites: [BAG_CONFIGURED]
PrerequisiteChecker.check(BAG_CONFIGURED) → true
  │
  ▼
RoutingResult.Navigate
  target: RoutingTarget(
    module = CADDY,
    screen = "club_adjustment",
    parameters = {"club": "7-iron"}
  )
```

### Scenario 2: High Confidence + Prerequisites Missing

```
User: "How's my recovery?"
  │
  ▼ IntentClassifier
ClassificationResult.Route
  intentType: RECOVERY_CHECK
  confidence: 0.88
  │
  ▼ RoutingOrchestrator
Check prerequisites: [RECOVERY_DATA]
PrerequisiteChecker.check(RECOVERY_DATA) → false
  │
  ▼
RoutingResult.PrerequisiteMissing
  missing: [RECOVERY_DATA]
  message: "I don't have any recovery data yet.
            Log your sleep, HRV, or readiness
            score first..."
```

### Scenario 3: No-Navigation Intent

```
User: "Show me my miss patterns"
  │
  ▼ IntentClassifier
ClassificationResult.Route
  intentType: PATTERN_QUERY
  confidence: 0.91
  │
  ▼ RoutingOrchestrator
Check: isNoNavigationIntent(PATTERN_QUERY) → true
  │
  ▼
RoutingResult.NoNavigation
  response: "Let me check your miss patterns.
             Based on your recent shots..."
```

### Scenario 4: Mid-Confidence Intent

```
User: "weather"
  │
  ▼ IntentClassifier
ClassificationResult.Confirm
  intentType: WEATHER_CHECK
  confidence: 0.65
  │
  ▼ RoutingOrchestrator
  │
  ▼
RoutingResult.ConfirmationRequired
  message: "Did you want to check the
            weather forecast?"
```

## Component Interactions

```
┌─────────────────────────────────────────────────────────┐
│                   RoutingOrchestrator                   │
│                                                         │
│  ┌───────────────────────────────────────────────────┐ │
│  │ route(ClassificationResult)                       │ │
│  │   │                                               │ │
│  │   ├─► handleRouteClassification()                 │ │
│  │   │     │                                         │ │
│  │   │     ├─► isNoNavigationIntent()                │ │
│  │   │     ├─► getRequiredPrerequisites()            │ │
│  │   │     ├─► prerequisiteChecker.checkAll() ──────┼─┼─►
│  │   │     ├─► generatePrerequisiteMessage()        │ │
│  │   │     └─► generateNoNavigationResponse()       │ │
│  │   │                                               │ │
│  │   ├─► ConfirmationRequired (mid-conf)            │ │
│  │   ├─► NoNavigation (clarify/error)               │ │
│  │   └─► return RoutingResult                       │ │
│  └───────────────────────────────────────────────────┘ │
│                                                         │
│  Dependencies:                                          │
│  └─► PrerequisiteChecker (injected)                    │
└─────────────────────────────────────────────────────────┘

        │                                     │
        │ RoutingResult                       │ Prerequisites check
        ▼                                     ▼
┌──────────────────┐              ┌──────────────────────┐
│  UI/ViewModel    │              │ PrerequisiteChecker  │
│  (Future Task)   │              │  Implementation      │
└──────────────────┘              │  (Future Task)       │
                                  │                      │
                                  │ Queries:             │
                                  │ • RecoveryRepository │
                                  │ • SessionContext     │
                                  │ • UserProfile        │
                                  └──────────────────────┘
```

## State Transitions

```
   ┌─────────────────────────────────────────┐
   │                                         │
   │  User submits input                     │
   │         │                               │
   │         ▼                               │
   │  [CLASSIFYING]                          │
   │         │                               │
   │         ▼                               │
   │  Classification complete                │
   │         │                               │
   │         ▼                               │
   │  [ROUTING]  ◄── RoutingOrchestrator     │
   │         │                               │
   │         ├──────┬──────┬──────┬──────┐   │
   │         ▼      ▼      ▼      ▼      ▼   │
   │      [NAV] [NO_NAV] [PRE] [CONF] [ERR] │
   │         │      │      │      │      │   │
   │         │      │      │      │      │   │
   │   (navigate) (show) (prompt) (ask) (err)│
   └─────────────────────────────────────────┘

Legend:
  NAV = Navigate to screen
  NO_NAV = Show inline response
  PRE = Prompt for prerequisite data
  CONF = Ask for confirmation
  ERR = Show error
```

## Testing Strategy Diagram

```
┌────────────────────────────────────────────────────┐
│          RoutingOrchestratorTest                   │
│                                                    │
│  Mock: PrerequisiteChecker                         │
│    └─► configure: satisfied / not satisfied        │
│                                                    │
│  Test Cases:                                       │
│  ┌──────────────────────────────────────────────┐ │
│  │ 1. Route + No Prerequisites → Navigate      │ │
│  │ 2. Route + Satisfied Prereqs → Navigate     │ │
│  │ 3. Route + Missing RECOVERY_DATA → PreMiss  │ │
│  │ 4. Route + Missing ROUND_ACTIVE → PreMiss   │ │
│  │ 5. Route + Missing BAG_CONFIG → PreMiss     │ │
│  │ 6. Route + Missing COURSE_SEL → PreMiss     │ │
│  │ 7. Route + PATTERN_QUERY → NoNavigation     │ │
│  │ 8. Route + HELP_REQUEST → NoNavigation      │ │
│  │ 9. Route + FEEDBACK → NoNavigation          │ │
│  │ 10. Confirm → ConfirmationRequired          │ │
│  │ 11. Clarify → NoNavigation                  │ │
│  │ 12. Error → NoNavigation                    │ │
│  └──────────────────────────────────────────────┘ │
│                                                    │
│  Assertions:                                       │
│  • Correct RoutingResult type                      │
│  • Correct prerequisite list                       │
│  • User-friendly messages                          │
│  • PrerequisiteChecker called correctly            │
└────────────────────────────────────────────────────┘
```

## Future Integration Points

```
┌──────────────────────────────────────────────────────┐
│                 CURRENT (Task 10)                    │
│                                                      │
│  IntentClassifier → RoutingOrchestrator → Result    │
│                           ▲                          │
│                           │                          │
│                  PrerequisiteChecker (interface)     │
└──────────────────────────┬───────────────────────────┘
                           │
┌──────────────────────────┴───────────────────────────┐
│                  FUTURE (Tasks 11-18)                │
│                                                      │
│  Task 11: DeepLinkBuilder                           │
│    • Convert RoutingTarget → NavController route    │
│    • Execute navigation with parameters             │
│                                                      │
│  Data Layer: PrerequisiteChecker Implementation     │
│    • Query RecoveryRepository                       │
│    • Check SessionContext                           │
│    • Validate UserProfile                           │
│                                                      │
│  Task 18: ConversationViewModel                     │
│    • Process RoutingResults                         │
│    • Handle UI for all result types                 │
│    • Coordinate with NavigationCoordinator          │
│                                                      │
│  UI Components:                                     │
│    • PrerequisitePromptDialog                       │
│    • ConfirmationDialog                             │
│    • InlineResponseView                             │
└──────────────────────────────────────────────────────┘
```
