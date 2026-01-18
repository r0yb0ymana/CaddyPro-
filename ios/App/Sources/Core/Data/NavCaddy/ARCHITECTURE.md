# NavCaddy Data Layer Architecture

## Layer Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  (Views, ViewModels - Phase 5)                              │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Use Case Layer                            │
│  (Intent Classifier, Pattern Aggregator - Phase 2, 4)       │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Domain Layer                              │
│  Shot, MissPattern, SessionContext, ConversationTurn        │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                 Repository Protocol                          │
│              NavCaddyRepository                              │
│  • recordShot(_:)                                           │
│  • getRecentShots(days:)                                    │
│  • getShotsWithPressure()                                   │
│  • getMissPatterns()                                        │
│  • updatePattern(_:)                                        │
│  • getSession()                                             │
│  • saveSession(_:)                                          │
│  • clearMemory()                                            │
│  • enforceRetentionPolicy()                                 │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│            Repository Implementation                         │
│          NavCaddyRepositoryImpl                             │
│  • FetchDescriptor queries                                  │
│  • Predicate-based filtering                               │
│  • 90-day retention logic                                   │
│  • 10-turn conversation limit                               │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                  SwiftData Models                            │
│  @Model classes with persistence metadata                   │
│  • ShotRecord                                               │
│  • MissPatternRecord                                        │
│  • SessionRecord                                            │
│  • ConversationTurnRecord                                   │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                   ModelContainer                             │
│  • Schema definition                                        │
│  • Storage configuration                                    │
│  • Encryption (iOS Data Protection)                         │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Persistent Storage                        │
│  Documents/default.store (encrypted)                        │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow

### Recording a Shot

```
User Action (UI)
    │
    ▼
ViewModel
    │
    ▼
Domain: Shot
    │
    ▼
Repository.recordShot(shot)
    │
    ▼
ShotRecord(from: shot)  ← Domain-to-Record conversion
    │
    ▼
ModelContext.insert(record)
    │
    ▼
ModelContext.save()
    │
    ▼
Persistent Storage (encrypted)
```

### Querying Patterns

```
Use Case Request
    │
    ▼
Repository.getPatternsByDirection(.slice)
    │
    ▼
FetchDescriptor<MissPatternRecord>(
    predicate: #Predicate { $0.direction == "slice" }
)
    │
    ▼
ModelContext.fetch(descriptor)
    │
    ▼
[MissPatternRecord]
    │
    ▼
records.compactMap { $0.toDomain() }  ← Record-to-Domain conversion
    │
    ▼
[MissPattern]
    │
    ▼
Use Case (Pattern Aggregator)
```

## Conversion Strategy

### Why Separate Record and Domain Models?

**SwiftData Models** (Records):
- Optimized for persistence (flat structure, raw values)
- @Model macro requirements (no protocols, limited types)
- Storage-specific concerns (unique attributes, relationships)

**Domain Models**:
- Rich business logic (methods, computed properties)
- Platform-agnostic (can be shared with Android via KMP if desired)
- Composable (nested structs, enums)

### Conversion Pattern

```swift
// Domain → Record
extension ShotRecord {
    convenience init(from shot: Shot) {
        self.init(
            id: shot.id,
            clubId: shot.club.id,              // Flatten Club
            clubName: shot.club.name,
            clubType: shot.club.type.rawValue, // Enum to String
            ...
        )
    }
}

// Record → Domain
extension ShotRecord {
    func toDomain() -> Shot? {
        guard let clubType = ClubType(rawValue: clubType) else {
            return nil  // Invalid data
        }

        let club = Club(id: clubId, name: clubName, type: clubType)
        return Shot(id: id, club: club, ...)
    }
}
```

## Relationship Mapping

### SessionRecord ← ConversationTurnRecord

```swift
@Model
final class SessionRecord {
    @Relationship(deleteRule: .cascade)
    var conversationTurns: [ConversationTurnRecord] = []
}

@Model
final class ConversationTurnRecord {
    var session: SessionRecord?  // Inverse relationship
}
```

**Benefits**:
- Automatic cleanup when session deleted
- Efficient querying
- Type-safe relationships

## Query Patterns

### Date-Based Filtering (Retention Policy)

```swift
let cutoffDate = Calendar.current.date(
    byAdding: .day,
    value: -90,
    to: Date()
)!

let descriptor = FetchDescriptor<ShotRecord>(
    predicate: #Predicate { $0.timestamp > cutoffDate }
)
```

### Boolean Filtering (Pressure Context)

```swift
let descriptor = FetchDescriptor<ShotRecord>(
    predicate: #Predicate {
        $0.isUserTaggedPressure == true ||
        $0.isInferredPressure == true
    }
)
```

### String Filtering (Direction, Club)

```swift
let descriptor = FetchDescriptor<MissPatternRecord>(
    predicate: #Predicate { $0.direction == "slice" }
)
```

## Memory Management

### Retention Policy (90 Days)

```
                    Today
                      │
         ┌────────────┼────────────┐
         │            │            │
    [Old Data]    90 days      [Recent Data]
         │            │            │
         ▼            ▼            ▼
     Deleted      Cutoff        Retained
```

**Implementation**:
```swift
func enforceRetentionPolicy() async throws {
    let cutoffDate = Date() - 90.days

    // Delete old shots
    let oldShots = fetch(ShotRecord where timestamp < cutoffDate)
    oldShots.forEach { modelContext.delete($0) }

    // Delete old patterns
    let oldPatterns = fetch(MissPatternRecord where lastOccurrence < cutoffDate)
    oldPatterns.forEach { modelContext.delete($0) }

    modelContext.save()
}
```

### Conversation History Limit (10 Turns)

```
New turn arrives
    │
    ▼
Append to history
    │
    ▼
History.count > 10?
    │
    ├─ YES → Keep last 10 (Array.suffix(10))
    │
    └─ NO → Keep all
    │
    ▼
Save to SessionRecord
```

## Encryption

### iOS Data Protection

```
┌─────────────────────────────────────┐
│      Application Sandbox            │
│                                     │
│  Documents/                         │
│    └── default.store  ← SwiftData  │
│         │                           │
│         ▼                           │
│    [Encrypted by iOS]               │
│         │                           │
│         ▼                           │
│    File System                      │
│         │                           │
│         ▼                           │
│    [Protected by Passcode/FaceID]  │
└─────────────────────────────────────┘
```

**Protection Level**: Complete Protection
- Data inaccessible when device locked
- Requires device unlock to read
- Automatic via SwiftData + iOS APIs

## Dependency Injection

```
DependencyContainer (Singleton)
    │
    ├── navCaddyModelContainer: ModelContainer
    │       └── Created via NavCaddyDataContainer.create()
    │
    └── navCaddyRepository: NavCaddyRepository
            └── NavCaddyRepositoryImpl(modelContainer)
```

**Usage in ViewModels**:
```swift
@Observable
final class FeatureViewModel {
    private let repository: NavCaddyRepository

    init(dependencies: DependencyContainer = .shared) {
        self.repository = dependencies.navCaddyRepository
    }
}
```

## Testing Strategy

### Unit Tests (In-Memory)

```swift
override func setUp() async throws {
    // Create in-memory container
    modelContainer = try NavCaddyDataContainer.create(inMemory: true)
    repository = NavCaddyRepositoryImpl(modelContainer: modelContainer)
}
```

**Benefits**:
- Fast (no disk I/O)
- Isolated (no cross-test pollution)
- Deterministic

### Integration Tests (Persistent)

```swift
let container = try NavCaddyDataContainer.create(inMemory: false)
let repository = NavCaddyRepositoryImpl(modelContainer: container)

// Write data
try await repository.recordShot(shot)

// Kill app, restart

// Read data (should survive)
let shots = try await repository.getRecentShots()
XCTAssertEqual(shots.count, 1)
```

## Performance Characteristics

| Operation | Complexity | Notes |
|-----------|-----------|-------|
| recordShot | O(1) | Single insert |
| getRecentShots | O(n log n) | Index scan + sort |
| getShotsWithPressure | O(n) | Boolean filter |
| updatePattern | O(1) | Unique ID lookup |
| getPatternsByDirection | O(n) | String filter |
| clearMemory | O(n) | Batch delete |
| enforceRetentionPolicy | O(n) | Date filter + delete |

**Optimization Opportunities**:
- Add composite indexes for common queries
- Batch inserts for bulk shot recording
- Background queue for retention policy enforcement

## Schema Evolution

### Adding Fields (Backward Compatible)

```swift
@Model
final class ShotRecord {
    var id: String
    var timestamp: Date
    // ... existing fields ...

    // New field (optional to maintain compatibility)
    var windSpeed: Double?
}
```

### Renaming Fields (Migration Required)

```swift
// Use SwiftData migration API or
// Implement custom migration logic
```

## Error Handling

```swift
do {
    try await repository.recordShot(shot)
} catch let error as NSError {
    // SwiftData errors
    switch error.code {
    case NSManagedObjectConstraintMergeError:
        // Unique constraint violation
    case NSPersistentStoreTimeoutError:
        // Database locked
    default:
        // Other persistence errors
    }
}
```

## Future Enhancements

1. **Background Context**: For long-running operations (pattern aggregation)
2. **CloudKit Sync**: Optional iCloud backup
3. **Export/Import**: For data portability (spec C4 - user control)
4. **Compression**: For old data before deletion
5. **Indexing**: Optimize frequently-queried fields

---

**Architecture Status**: Production-ready for Phase 2 integration
