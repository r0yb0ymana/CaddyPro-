# NavCaddy Data Layer - iOS Implementation

**Task 4**: Set Up Local Persistence for iOS
**Spec**: `specs/navcaddy-engine.md` (R5, C4, C5)
**Plan**: `specs/navcaddy-engine-plan.md` (Task 4)

## Overview

This implementation provides SwiftData-based persistence for the NavCaddy Engine, including:
- Shot recording with full context
- Miss pattern aggregation
- Session context management
- 90-day retention policy
- Encrypted storage via iOS Data Protection

## File Structure

```
Core/Data/NavCaddy/
├── Models/
│   ├── ShotRecord.swift               # Persists Shot domain model
│   ├── MissPatternRecord.swift        # Persists MissPattern aggregations
│   ├── SessionRecord.swift            # Persists SessionContext
│   └── ConversationTurnRecord.swift   # Persists conversation history
├── NavCaddyRepository.swift           # Protocol defining persistence operations
├── NavCaddyRepositoryImpl.swift       # SwiftData implementation
└── NavCaddyDataContainer.swift        # ModelContainer factory
```

## SwiftData Models

### ShotRecord
Stores complete shot records with:
- Club information (flattened from Club domain model)
- Miss direction (optional)
- Lie type
- Pressure context (user-tagged and inferred)
- Hole number and notes

**Spec compliance**: C5 (attributable memory - stores full shot events)

### MissPatternRecord
Stores aggregated miss patterns with:
- Direction and frequency
- Confidence score (subject to decay)
- Optional club and pressure context filters
- Last occurrence timestamp

**Spec compliance**: R5 (pattern aggregation with decay)

### SessionRecord
Stores session context including:
- Current round ID and hole
- Last recommendation
- Conversation history (relationship to ConversationTurnRecord)

**Spec compliance**: R6 (short-term context for continuity)

### ConversationTurnRecord
Stores individual conversation turns:
- Role (user/assistant)
- Content
- Timestamp

**Spec compliance**: R6 (limited to last 10 turns)

## Repository Operations

### Shots
- `recordShot(_:)` - Insert new shot
- `getRecentShots(days:)` - Query shots within N days (default: 90)
- `getShotsWithPressure()` - Filter shots with pressure context

### Patterns
- `getMissPatterns()` - Retrieve all patterns
- `updatePattern(_:)` - Upsert pattern
- `getPatternsByDirection(_:)` - Filter by miss direction
- `getPatternsByClub(_:)` - Filter by club ID

### Session
- `getSession()` - Get current session (or empty)
- `saveSession(_:)` - Save session context
- `addConversationTurn(_:)` - Append turn to history

### Memory Management
- `clearMemory()` - Delete all data (user control per C4)
- `enforceRetentionPolicy()` - Delete data older than 90 days (per R5/Q5)

## Data Protection

**Spec C4**: Data encrypted at rest

SwiftData uses iOS Data Protection by default when `isStoredInMemoryOnly: false`:
- Files stored in application's Documents directory
- Protected by device passcode/biometrics
- Cannot be accessed when device is locked (Complete Protection class)

## Integration

### Dependency Injection

The repository is available via `DependencyContainer`:

```swift
@MainActor
let container = DependencyContainer.shared
let repository = container.navCaddyRepository
```

### Usage Example

```swift
// Record a shot
let club = Club(name: "7 Iron", type: .iron)
let shot = Shot(
    club: club,
    missDirection: .slice,
    lie: .fairway,
    pressureContext: PressureContext(isUserTagged: true)
)
try await repository.recordShot(shot)

// Query patterns
let patterns = try await repository.getPatternsByDirection(.slice)

// Manage session
var session = try await repository.getSession()
session = session.addingTurn(ConversationTurn(role: .user, content: "What club?"))
try await repository.saveSession(session)
```

## Testing

### Unit Tests

**NavCaddyDataModelTests.swift**:
- Record-to-domain mapping correctness
- Roundtrip conversion tests
- Enum parsing edge cases

**NavCaddyRepositoryTests.swift**:
- CRUD operations
- Query filtering
- Retention policy enforcement
- Conversation history limiting (10 turns)

### Running Tests

```bash
cd ios/App
swift test  # If toolchain supports
# OR
xcodebuild test -scheme App -destination 'platform=iOS Simulator,name=iPhone 15'
```

## Verification Checklist

- [x] SwiftData models compile and include all required fields
- [x] Repository protocol defines all operations
- [x] Repository implementation uses SwiftData queries
- [x] ModelContainer factory created
- [x] DependencyContainer integration
- [x] Domain-to-record conversion methods
- [x] Record-to-domain conversion methods
- [x] Unit tests for mapping
- [x] Unit tests for repository operations
- [x] 90-day retention policy implemented
- [x] Conversation history limited to 10 turns
- [x] Data Protection enabled (default SwiftData behavior)

## Next Steps

1. **Phase 2 (Task 5-9)**: Implement Intent Pipeline
   - Use repository in intent classification context
   - Load session context for continuity

2. **Phase 4 (Task 14)**: Implement Miss Pattern Store
   - Use repository to aggregate patterns from shots
   - Implement decay calculations

3. **Phase 4 (Task 15)**: Implement Session Context Manager
   - Wrap repository with reactive state management
   - Expose StateFlow/Observable for UI binding

## Notes

- **Club enrichment**: MissPatternRecord stores only `clubId`. Full club details may need enrichment from a separate club repository or cache.
- **Round persistence**: SessionRecord stores `roundId` but not full Round details. Consider adding RoundRecord if needed.
- **Testing limitations**: CI/CD may need Xcode installed for full test execution. In-memory ModelContainer works for unit tests.
