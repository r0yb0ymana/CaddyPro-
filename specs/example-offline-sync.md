# Feature: Offline Data Synchronization

> Example mobile spec demonstrating offline-first architecture and data sync patterns

## 1. Problem Statement

Users need to access and modify data when they have poor or no network connectivity. Currently, the app requires an internet connection for all operations, leading to a poor user experience in areas with unreliable connectivity.

## 2. Goals

- G1: Allow users to view cached data when offline
- G2: Enable data modifications while offline (create, update, delete)
- G3: Automatically sync changes when connectivity is restored
- G4: Resolve conflicts between local and server changes
- G5: Provide clear feedback about sync status to users
- G6: Minimize data usage by syncing only changes (delta sync)

## 3. Non-Goals

- NG1: Real-time collaboration (multiple users editing same item)
- NG2: Large file sync (images, videos) - separate spec
- NG3: Selective sync (choosing what to cache) - future enhancement
- NG4: Cross-device sync without server (peer-to-peer)
- NG5: Background sync when app is killed - future enhancement

## 4. Functional Requirements

### R1: Local Database

#### Android
- Use Room database for local storage
- Store all entities with sync metadata
- Support migrations for schema changes

#### iOS
- Use SwiftData or Core Data for local storage
- Store all entities with sync metadata
- Support lightweight migrations

### R2: Sync Metadata Schema
Each synced entity must include:
```
- id: UUID (client-generated)
- server_id: String? (assigned by server after first sync)
- created_at: Timestamp
- updated_at: Timestamp
- sync_status: Enum (synced, pending_create, pending_update, pending_delete)
- last_synced_at: Timestamp?
- version: Integer (for conflict detection)
```

### R3: Connectivity Monitoring
- Monitor network state changes in real-time
- Detect: No connection, WiFi, Cellular
- Trigger sync when connection becomes available
- Provide connection state to UI layer

### R4: Sync Queue
- Queue all local changes when offline
- Process queue in order when online
- Retry failed sync operations with exponential backoff
- Maximum 5 retry attempts before marking as failed
- Allow manual retry of failed syncs

### R5: Conflict Resolution
- Detect conflicts via version numbers
- Default strategy: Server wins (last-write-wins)
- Option for user resolution on critical data
- Log all conflicts for debugging

### R6: Sync Operations

#### Pull (Server → Local)
- Fetch changes since last sync timestamp
- Apply server changes to local database
- Handle deleted items (soft delete with tombstones)
- Update last_synced_at after successful pull

#### Push (Local → Server)
- Send pending local changes to server
- Update sync_status after server confirms
- Map server_id to local id for new items
- Remove deleted items from local after server confirms

### R7: UI Indicators
- Show sync status in app bar or status area
- States: Synced, Syncing, Offline, Sync Error
- Show last sync time
- Pull-to-refresh triggers manual sync
- Badge count for pending changes

### R8: Data Freshness
- Cache expiration: 24 hours for most data
- Critical data synced every 5 minutes when online
- Show "Last updated X ago" on list screens
- Warn user when viewing stale data (> 24 hours)

## 5. Acceptance Criteria

### Offline Reading

#### A1: View cached data offline
- GIVEN: User has previously loaded items while online
- WHEN: Device goes offline
- THEN: User can still view all cached items
- AND: UI indicates offline status

#### A2: Empty state for uncached data
- GIVEN: User has never synced a section
- WHEN: User navigates to that section offline
- THEN: Empty state shows "No cached data. Connect to sync."
- AND: Retry button is disabled

### Offline Writing

#### A3: Create item offline
- GIVEN: Device is offline
- WHEN: User creates a new item
- THEN: Item is saved to local database
- AND: Item appears in list immediately
- AND: Item shows "pending sync" indicator
- AND: sync_status is pending_create

#### A4: Update item offline
- GIVEN: Device is offline
- WHEN: User edits an existing item
- THEN: Changes are saved locally
- AND: Item shows "pending sync" indicator
- AND: sync_status is pending_update
- AND: version is incremented locally

#### A5: Delete item offline
- GIVEN: Device is offline
- WHEN: User deletes an item
- THEN: Item is removed from UI
- AND: Item is marked pending_delete in database
- AND: Item syncs deletion when online

### Automatic Sync

#### A6: Sync on connectivity restored
- GIVEN: Device was offline with pending changes
- WHEN: Network connectivity is restored
- THEN: Sync begins automatically within 5 seconds
- AND: UI shows "Syncing..." indicator
- AND: Pending changes are pushed to server

#### A7: Background sync while using app
- GIVEN: User is using app with network connection
- WHEN: 5 minutes pass since last sync
- THEN: Background sync pulls latest data
- AND: UI updates without interrupting user
- AND: No loading overlay shown

#### A8: Pull-to-refresh sync
- GIVEN: User is on a list screen
- WHEN: User pulls down to refresh
- THEN: Full sync is triggered
- AND: Pull-to-refresh indicator shows progress
- AND: List updates with new data

### Conflict Resolution

#### A9: Server wins on conflict
- GIVEN: Item A was modified locally (v2) and on server (v2)
- WHEN: Sync detects version conflict
- THEN: Server version is applied locally
- AND: Local changes are discarded
- AND: Conflict is logged for debugging

#### A10: Conflict notification for critical data
- GIVEN: Critical item modified locally and on server
- WHEN: Conflict is detected
- THEN: User is notified of conflict
- AND: User can choose local or server version
- AND: Choice is applied and synced

### Error Handling

#### A11: Retry on network error
- GIVEN: Sync fails due to network error
- WHEN: Error occurs
- THEN: Retry is attempted after 5 seconds
- AND: Exponential backoff: 5s, 10s, 20s, 40s, 80s
- AND: After 5 failures, item marked as failed

#### A12: Failed sync indicator
- GIVEN: Sync has failed after all retries
- WHEN: User views item or list
- THEN: Failed item shows error indicator
- AND: User can tap to retry manually
- AND: Error message explains the issue

#### A13: Server error handling
- GIVEN: Server returns 500 error during sync
- WHEN: Push fails
- THEN: Changes remain in local queue
- AND: Sync retries with backoff
- AND: User is not blocked from using app

### Performance

#### A14: Initial sync performance
- GIVEN: User has no cached data
- WHEN: First sync is triggered
- THEN: First 20 items load within 2 seconds
- AND: Remaining items load in background
- AND: User can interact with loaded items

#### A15: Incremental sync performance
- GIVEN: User has existing cached data
- WHEN: Sync fetches changes
- THEN: Only changed items since last sync are fetched
- AND: Sync completes within 5 seconds for < 100 changes

### UI Feedback

#### A16: Sync status visibility
- GIVEN: User is on main screen
- WHEN: Any sync state
- THEN: Current sync status is visible (icon or text)
- AND: Tapping status shows detailed sync info

#### A17: Pending changes count
- GIVEN: User has pending offline changes
- WHEN: Viewing list or detail screen
- THEN: Badge shows count of pending items
- AND: Badge updates as items sync

#### A18: Last sync time
- GIVEN: User is on list screen
- WHEN: Data is cached
- THEN: "Last synced X minutes ago" is visible
- AND: Time updates dynamically

## 6. Constraints & Invariants

### C1: Data Integrity
- Local database must be ACID compliant
- Sync operations are idempotent
- No data loss during sync failures
- Tombstones retained for 30 days

### C2: API Contract

#### Pull Changes
```json
// GET /api/sync/changes?since={timestamp}
// Response 200
{
  "items": [
    {
      "id": "string",
      "type": "item_type",
      "action": "create|update|delete",
      "data": { ... },
      "version": 5,
      "updated_at": "ISO8601"
    }
  ],
  "sync_token": "string",
  "has_more": false
}
```

#### Push Changes
```json
// POST /api/sync/push
// Request
{
  "changes": [
    {
      "client_id": "uuid",
      "type": "item_type",
      "action": "create|update|delete",
      "data": { ... },
      "base_version": 4
    }
  ]
}

// Response 200
{
  "results": [
    {
      "client_id": "uuid",
      "server_id": "string",
      "status": "success|conflict|error",
      "version": 5,
      "server_data": { ... }  // if conflict
    }
  ]
}
```

### C3: Storage Limits
- Maximum local cache: 100MB
- Prune oldest synced items when limit reached
- Never prune pending changes
- Warn user at 80% capacity

### C4: Battery Efficiency
- Batch sync operations
- Use WorkManager (Android) / BGTaskScheduler (iOS)
- Respect battery saver modes
- No sync when battery < 15%

### C5: Security
- Encrypt local database
- Secure sync tokens in keychain/keystore
- Clear local data on logout
- No sensitive data in logs

## 7. Open Questions

- Q1: Should we support selective sync (user chooses what to cache)?
  - **Decision:** Not in MVP. All data is synced for simplicity.

- Q2: How to handle sync during poor connectivity (high latency)?
  - **Decision:** Use timeouts (30s) and retry. Show "slow connection" warning.

- Q3: Should we sync when on cellular data?
  - **Decision:** Yes, but respect user's data saver settings if enabled.

## 8. Data Model

### SyncStatus Enum
```kotlin
// Android
enum class SyncStatus {
    SYNCED,
    PENDING_CREATE,
    PENDING_UPDATE,
    PENDING_DELETE,
    SYNC_FAILED
}
```

```swift
// iOS
enum SyncStatus: String, Codable {
    case synced
    case pendingCreate
    case pendingUpdate
    case pendingDelete
    case syncFailed
}
```

### Base Syncable Entity
```kotlin
// Android - Room Entity
@Entity
abstract class SyncableEntity {
    @PrimaryKey
    var id: String = UUID.randomUUID().toString()
    var serverId: String? = null
    var createdAt: Long = System.currentTimeMillis()
    var updatedAt: Long = System.currentTimeMillis()
    var syncStatus: SyncStatus = SyncStatus.PENDING_CREATE
    var lastSyncedAt: Long? = null
    var version: Int = 1
}
```

```swift
// iOS - SwiftData Model
@Model
class SyncableEntity {
    var id: UUID = UUID()
    var serverId: String?
    var createdAt: Date = Date()
    var updatedAt: Date = Date()
    var syncStatus: SyncStatus = .pendingCreate
    var lastSyncedAt: Date?
    var version: Int = 1
}
```

## 9. Testing Strategy

### Unit Tests
- Sync status transitions
- Conflict detection logic
- Retry/backoff logic
- Data transformation

### Integration Tests
- Full sync cycle with mock server
- Offline → Online transition
- Conflict resolution scenarios
- Error recovery

### E2E Tests
- Create item offline, sync, verify on server
- Server update reflected locally after sync
- Conflict resolution UI flow
