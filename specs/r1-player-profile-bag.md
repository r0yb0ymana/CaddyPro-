# Spec: Player Profile + Bag Management (R1)

**Status:** Draft
**Priority:** P0 (Foundation - blocks all other R1 features)
**Estimated Effort:** 3-4 sessions (2-4hr blocks)

## Problem Statement

Every R1 feature depends on the player's club data. Forecaster HUD needs carry distances to calculate adjustments. Shot Logger needs the club list for quick selection. Hole Map needs miss bias for recommendations. Without a bag profile, nothing works.

## User Stories

1. As a golfer, I want to create my player profile so the app knows my basic info.
2. As a golfer, I want to add my clubs with carry/total distances so the app gives me accurate yardages.
3. As a golfer, I want to set my miss bias per club so strategy recommendations account for my tendencies.
4. As a golfer, I want to manage multiple bag setups (tournament vs casual) so I get accurate data for each context.
5. As a golfer, I want my bag data available offline so it works on the course with no signal.

## Screens

### 1. Profile Setup Screen
**Route:** `/profile/setup`
**When:** First launch + accessible from settings

| Field | Type | Required | Default |
|-------|------|----------|---------|
| Display Name | Text | Yes | From Google account |
| Handicap Index | Number (0.0-54.0) | No | None |
| Preferred Units | Toggle | Yes | Metric (AU default) |
| Home Course | Search/Select | No | None |

### 2. Bag List Screen
**Route:** `/profile/bags`

- List of bag profiles (name, club count, active badge)
- One bag marked as "Active" at all times
- Tap bag to view/edit clubs
- FAB to create new bag
- Swipe to delete (with confirmation)
- Default bag created on first launch: "My Bag"

### 3. Club Editor Screen
**Route:** `/profile/bags/{bagId}/clubs`

- List of clubs in the bag, grouped by type (Driver, Woods, Hybrids, Irons, Wedges, Putter)
- Each club shows: name, carry distance, total distance, miss bias icon
- FAB to add club
- Tap to edit, swipe to remove
- "Quick Add" option: pre-populated templates (standard 14-club set)

### 4. Club Detail Sheet
**Type:** Bottom sheet modal
**Trigger:** Add or edit club

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| Club Name | Text | Yes | Max 20 chars |
| Club Type | Dropdown | Yes | DRIVER, WOOD, HYBRID, IRON, WEDGE, PUTTER |
| Loft | Number | No | 1-64 degrees |
| Carry Distance | Number | Yes | 1-400 yards/metres |
| Total Distance | Number | Yes | >= carry distance |
| Miss Bias | Selector | Yes | STRAIGHT, SLICE, HOOK, PUSH, PULL |

**Miss Bias Selector:** Visual diagram showing ball flight paths. Tap to select. Default: STRAIGHT.

## Data Models

### PlayerProfile (Room + Supabase)
```
PlayerProfile {
    id: UUID (PK)
    supabaseUserId: String
    displayName: String
    handicapIndex: Float?
    preferredUnits: Enum (METRIC, IMPERIAL)
    homeCourseId: UUID?
    createdAt: Instant
    updatedAt: Instant
    synced: Boolean
}
```

### Bag (Room + Supabase)
```
Bag {
    id: UUID (PK)
    profileId: UUID (FK)
    name: String
    isActive: Boolean
    createdAt: Instant
    updatedAt: Instant
    synced: Boolean
}
```

### Club (Room + Supabase)
```
Club {
    id: UUID (PK)
    bagId: UUID (FK)
    name: String
    type: Enum (DRIVER, WOOD, HYBRID, IRON, WEDGE, PUTTER)
    loft: Float?
    carryDistance: Int
    totalDistance: Int
    missBias: Enum (STRAIGHT, SLICE, HOOK, PUSH, PULL)
    sortOrder: Int
    createdAt: Instant
    updatedAt: Instant
    synced: Boolean
}
```

## Sync Strategy

- **Write:** Room first (immediate), queue Supabase sync via WorkManager
- **Read:** Always from Room
- **Conflict resolution:** Last-write-wins using `updatedAt` timestamp
- **Initial load:** Pull from Supabase on first login, merge with any local data
- **Offline indicator:** Subtle icon when unsynced changes exist

## Acceptance Criteria

### Profile Setup
- [ ] AC1: User completes profile setup on first launch
- [ ] AC2: Profile persists in Room after app kill
- [ ] AC3: Profile syncs to Supabase when online
- [ ] AC4: Handicap accepts 0.0-54.0 with one decimal place
- [ ] AC5: Units toggle switches all distance displays app-wide

### Bag Management
- [ ] AC6: Default "My Bag" created on first launch
- [ ] AC7: Only one bag is active at a time (toggling activates one, deactivates previous)
- [ ] AC8: Deleting the active bag promotes the next bag to active
- [ ] AC9: At least one bag must exist (prevent deleting last bag)
- [ ] AC10: Bag list updates in real-time when clubs are added/removed

### Club Management
- [ ] AC11: Quick Add populates a standard 14-club set with typical distances
- [ ] AC12: Carry distance must be less than or equal to total distance (validation)
- [ ] AC13: Club type determines sort order within the bag (Driver first, Putter last)
- [ ] AC14: Miss bias selector shows visual ball flight diagram
- [ ] AC15: Maximum 14 clubs per bag (tournament rules) with warning at 14, hard block at 15

### Offline
- [ ] AC16: All CRUD operations work with no network
- [ ] AC17: Unsynced changes show subtle indicator
- [ ] AC18: Sync completes automatically when connectivity returns
- [ ] AC19: Conflicting edits resolve via last-write-wins

### Theme Compliance
- [ ] AC20: All screens use CaddyPro dark theme (Matte Charcoal background)
- [ ] AC21: Performance Lime used only for primary action buttons
- [ ] AC22: Distance values rendered in JetBrains Mono
- [ ] AC23: Touch targets minimum 48dp
- [ ] AC24: Inter font for all UI text

## Task Breakdown

### Task 1: Project Setup + Theme (2hr)
- Initialize Android project with Compose + Hilt
- Implement CaddyPro theme (colors, typography, spacing tokens)
- Configure Room database with initial schema
- Configure Supabase client
- Set up navigation graph shell

### Task 2: Profile Setup Screen (2hr)
- ProfileSetupScreen composable
- ProfileViewModel with Room persistence
- First-launch detection + navigation
- Unit tests for profile validation

### Task 3: Bag Management (3hr)
- BagListScreen composable
- BagViewModel with CRUD operations
- Default bag creation on first launch
- Active bag toggle logic
- Swipe-to-delete with confirmation
- Unit tests for bag business rules

### Task 4: Club Editor + Detail Sheet (3hr)
- ClubListScreen composable (grouped by type)
- ClubDetailSheet bottom sheet
- Miss bias visual selector component
- Quick Add 14-club template
- Validation (carry <= total, max 14 clubs)
- Unit tests for club validation

### Task 5: Sync Layer (2hr)
- Supabase table setup (profiles, bags, clubs)
- Repository pattern: Room-first, Supabase-sync
- WorkManager sync job
- Conflict resolution (last-write-wins)
- Offline indicator component
- Integration tests for sync flow

### Task 6: Polish + Review (2hr)
- Verify all acceptance criteria
- Theme compliance audit
- Accessibility pass (touch targets, contrast, dynamic text)
- Edge cases (empty states, error states, no network)

## Dependencies

- Supabase project created with auth configured (Google + Email)
- Mapbox API key (not needed for this spec, but project setup should include it)
- OpenWeatherMap API key (same)

## Out of Scope

- Importing clubs from other apps
- Club distance inference from shot data
- Club photos or brand/model selection
- Sharing bag profiles with other users

## Open Questions

None. Ready for implementation.
