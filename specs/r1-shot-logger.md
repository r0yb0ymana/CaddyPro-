# Spec: Shot Logger (R1)

**Status:** Draft
**Priority:** P0 (Core feature - on-course tracking)
**Estimated Effort:** 3-4 sessions (2-4hr blocks)
**Depends On:** Player Profile + Bag (needs club list for selection)

## Problem Statement

Golfers need a fast, minimal-friction way to log shots during a round. Current shot trackers require too many taps and distract from play. CaddyPro's Shot Logger prioritizes speed: one tap for club, one tap for shot type, auto-capture location. The data feeds into strategy recommendations and personal stats.

## User Stories

1. As a golfer, I want to start a round quickly so I'm not fumbling with my phone on the first tee.
2. As a golfer, I want to log a shot in 2-3 taps so it doesn't slow down play.
3. As a golfer, I want my club list ready so I can tap the club I just used.
4. As a golfer, I want shots saved offline so I can log in areas with no signal.
5. As a golfer, I want to undo my last shot in case I logged it wrong.
6. As a golfer, I want to see my shots for the current hole so I know my count.
7. As a golfer, I want to end a round and see a basic summary.

## Screens

### 1. Round Start Screen
**Route:** `/round/start`
**Trigger:** "Start Round" button on home screen

| Field | Type | Required | Default |
|-------|------|----------|---------|
| Course Name | Text input | Yes | Last played course or empty |
| Number of Holes | Toggle | Yes | 18 |
| Active Bag | Read-only | Auto | Current active bag |
| Tee Time | Auto | Auto | Current time |

### 2. Shot Logger Screen (Active Round)
**Route:** `/round/{roundId}/log`
**When:** During an active round (primary screen)

#### Header
- Hole number (large, prominent): "Hole 7"
- Par for hole (if available): "Par 4"
- Shot count for current hole: "Shot 3"
- Round shot total

#### Club Quick-Select
- Horizontal scrollable strip of clubs from active bag
- Most recently used club pre-highlighted
- Grouped by type with visual separators
- Club name + carry distance shown
- One tap to select

#### Shot Type Selector
- Row of shot type buttons: TEE, FAIRWAY, APPROACH, CHIP, PUTT, PENALTY
- Auto-suggested based on shot number (shot 1 = TEE, high shot # = PUTT)
- One tap to select

#### Log Shot Button
- Large, prominent button: "Log Shot"
- Requires: club selected + shot type selected
- Auto-captures GPS location as shot start position
- Haptic feedback on tap
- Brief confirmation animation

#### Action Bar
- Undo last shot (with confirmation)
- Next hole button
- Hole overview (mini list of shots this hole)

### 3. Hole Summary Card
**Type:** Expandable card on Shot Logger screen
**Trigger:** Tap "Shots" indicator or auto-expand between holes

- List of shots for current hole
- Each shot: #, club used, shot type, time
- Editable: tap to change club or shot type
- Deletable: swipe to remove

### 4. Round Summary Screen
**Route:** `/round/{roundId}/summary`
**When:** After ending a round

| Section | Content |
|---------|---------|
| Overview | Total shots, holes played, duration |
| Per-Hole | Shots per hole in a simple grid |
| Club Usage | Which clubs were used, frequency |
| Shot Types | Breakdown by shot type |

## Data Models

### Round (Room + Supabase)
```
Round {
    id: UUID (PK)
    profileId: UUID (FK)
    courseName: String
    holesPlayed: Int (9 or 18)
    startedAt: Instant
    endedAt: Instant?
    isActive: Boolean
    totalShots: Int
    createdAt: Instant
    updatedAt: Instant
    synced: Boolean
}
```

### Shot (Room + Supabase)
```
Shot {
    id: UUID (PK)
    roundId: UUID (FK)
    holeNumber: Int (1-18)
    shotNumber: Int (per hole)
    clubId: UUID (FK -> Club)
    clubName: String (denormalized for offline display)
    shotType: Enum (TEE, FAIRWAY, APPROACH, CHIP, PUTT, PENALTY)
    startLatitude: Double?
    startLongitude: Double?
    endLatitude: Double?
    endLongitude: Double?
    timestamp: Instant
    createdAt: Instant
    updatedAt: Instant
    synced: Boolean
}
```

## Shot Logging Flow

```
1. User on Shot Logger screen (hole N)
2. Tap club from quick-select strip → club highlighted
3. Tap shot type (auto-suggested) → type selected
4. Tap "Log Shot" → shot saved to Room
   - GPS location captured as start position
   - Shot number auto-incremented
   - Haptic confirmation
   - Club strip scrolls back to recently used
   - Sync queued via WorkManager
5. Repeat for next shot
6. Tap "Next Hole" → hole increments, shot counter resets
7. Tap "End Round" → navigate to Round Summary
```

## Auto-Suggestions

| Shot Number | Suggested Type | Rationale |
|-------------|---------------|-----------|
| 1 | TEE | First shot is always from tee |
| 2 (par 3) | PUTT | Second shot on par 3 likely a putt |
| 2 (par 4/5) | FAIRWAY | Second shot likely from fairway |
| 3 (par 4) | APPROACH/PUTT | Getting close to green |
| High (any) | PUTT | Late shots are usually putts |

For MVP, use simple rules based on shot number. No par data in R1 (no course data integration yet).

Simplified MVP rules:
- Shot 1: TEE
- Shot 2-3: FAIRWAY
- Shot 4+: PUTT
- User always overrides with one tap

## Offline Behavior

- All shot logging works fully offline (Room-first)
- GPS location captured from device (no network needed)
- Shots queued for Supabase sync via WorkManager
- Round can be started, played, and completed entirely offline
- Sync happens when connectivity returns

## Acceptance Criteria

### Round Management
- [ ] AC1: User can start a round with course name and hole count (9/18)
- [ ] AC2: Active bag auto-selected and shown (read-only)
- [ ] AC3: Only one active round at a time
- [ ] AC4: Round can be ended from the Shot Logger screen
- [ ] AC5: Round summary shows total shots, club usage, shot type breakdown

### Shot Logging
- [ ] AC6: Club quick-select shows all clubs from active bag
- [ ] AC7: Shot type auto-suggested based on shot number
- [ ] AC8: "Log Shot" requires both club and shot type selected
- [ ] AC9: Shot number auto-increments per hole
- [ ] AC10: GPS location captured automatically on shot log (if permission granted)
- [ ] AC11: Haptic feedback on shot logged
- [ ] AC12: Undo last shot with confirmation dialog

### Hole Navigation
- [ ] AC13: "Next Hole" advances hole number and resets shot counter
- [ ] AC14: Hole number displayed prominently (large text)
- [ ] AC15: Shot count for current hole visible at all times
- [ ] AC16: Cannot advance past hole 18 (or 9 for 9-hole round)

### Hole Summary
- [ ] AC17: Expandable shot list for current hole
- [ ] AC18: Each shot shows: number, club, type, time
- [ ] AC19: Shots editable (change club or type) via tap
- [ ] AC20: Shots deletable via swipe (with shot renumbering)

### Offline
- [ ] AC21: All shot logging works with no network
- [ ] AC22: Shots persist after app kill (Room)
- [ ] AC23: Shots sync to Supabase when online
- [ ] AC24: Active round survives app restart

### Theme Compliance
- [ ] AC25: All screens use CaddyPro dark theme
- [ ] AC26: Shot count and hole number in JetBrains Mono
- [ ] AC27: Touch targets minimum 48dp (especially club strip and Log Shot button)
- [ ] AC28: Performance Lime for "Log Shot" primary action
- [ ] AC29: Minimal animations (outdoor distraction reduction)

## Task Breakdown

### Task 1: Round Data Layer (2hr)
- Round + Shot Room entities and DAOs
- RoundRepository: create, end, get active round
- ShotRepository: CRUD operations, auto-numbering
- Supabase DTOs and sync integration
- Unit tests for repository logic

### Task 2: Round Start Screen (1hr)
- RoundStartScreen composable
- RoundStartViewModel
- Course name input, hole count toggle
- Active bag display (read-only)
- Navigation to Shot Logger on start
- Unit tests for round creation validation

### Task 3: Shot Logger Screen (3hr)
- ShotLoggerScreen composable (primary screen)
- ShotLoggerViewModel: manages active round state
- Club quick-select horizontal strip
- Shot type selector with auto-suggestions
- Log Shot button with haptic feedback
- Undo last shot with confirmation
- Next Hole navigation
- Header with hole number, shot count
- Unit tests for shot logging flow

### Task 4: Hole Summary (2hr)
- HoleSummaryCard expandable composable
- Shot list with edit/delete capability
- Shot renumbering on delete
- Edit shot sheet (change club or type)
- Unit tests for shot editing

### Task 5: Round Summary Screen (2hr)
- RoundSummaryScreen composable
- Per-hole shot grid
- Club usage stats
- Shot type breakdown
- RoundSummaryViewModel
- Unit tests for summary calculations

### Task 6: Polish + Review (2hr)
- Verify all acceptance criteria
- Theme compliance audit
- Offline scenarios testing
- Haptic feedback verification
- Edge cases (empty bag, location denied, mid-round app kill)

## Dependencies

- Player Profile + Bag complete (club data exists in Room)
- Active bag has clubs (empty bag should show warning)
- Location permission for GPS tracking
- Vibrator service for haptic feedback

## Out of Scope

- Shot distance calculation (needs end position, R2 with Hole Map)
- Par tracking / scoring (no course data in R1)
- Shot shape/trajectory recording
- Photo/video of shots
- Live leaderboard / multiplayer rounds
- Detailed statistics / trends (R2)
- Voice shot logging (R2 NavCaddy)

## Open Questions

1. **End position capture:** For MVP, we only capture start position (where the golfer is standing when they log). End position requires logging from the ball's landing spot on the next shot, which adds complexity. **Decision: R1 captures start position only.** End position comes in R2 with Hole Map integration.
2. **Par data:** Without course data integration, we can't show par or calculate score relative to par. **Decision: R1 shows raw shot counts only.** Par integration comes with Hole Map in R2.
