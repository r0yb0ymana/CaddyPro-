# Player Profile & Bag Management - Implementation Plan

**Spec**: `specs/player-profile-bag-management.md`
**Generated**: 2026-01-19
**Status**: READY - Q1-Q2 resolved, Q3-Q4 deferred to post-MVP

---

## Summary

This plan breaks down the Player Profile & Bag Management feature into 22 sequential tasks across 5 phases. The feature provides a "Digital Locker" for golfers to manage multiple bag profiles, track club distances, and store miss biases that feed into NavCaddy, Live Caddy, and Coach Mode.

**Architecture approach**: Extends existing patterns (Room/SwiftData, Repository, MVVM) with new domain models for bags, clubs, and distance tracking. Reactive propagation ensures all modules reflect active bag changes.

---

## Prerequisites & Blockers

**Spec clarifications needed before full implementation**:

| Blocker | Spec Reference | Impact |
|---------|---------------|--------|
| ~~Material distance difference undefined~~ | Q1 | ~~Blocks Task 9-10~~ RESOLVED: 5% |
| ~~Minimum sample size undefined~~ | Q2 | ~~Blocks Task 9-10~~ RESOLVED: 20 shots |
| Wind/temperature normalization unclear | Q3 | Blocks Task 10 (advanced inference) |
| Altitude profile support undefined | Q4 | Blocks Task 10 (optional enhancement) |

**Recommendation**: Resolve Q1-Q2 before starting Phase 3. Q3-Q4 can be deferred to post-MVP.

**Proposed Defaults (if spec not updated)**:
- Q1: Material difference = 5% of estimated carry
- Q2: Minimum sample size = 20 shots per club
- Q3: MVP skips normalization; track raw values with "conditions varied" disclaimer
- Q4: Defer altitude support to future release

**Note: All measurements use metric units (meters, Celsius, kilograms)**

---

## Total Tasks: 22

| Phase | Tasks | Focus |
|-------|-------|-------|
| Phase 1: Foundation | 1-5 | Domain models, data layer setup |
| Phase 2: Bag Management | 6-9 | CRUD operations, switching, UI |
| Phase 3: Distance Calibration | 10-13 | Inference, suggestions, audit trail |
| Phase 4: Miss Bias | 14-17 | Manual input, auto-suggestion, strategy integration |
| Phase 5: Integration & Polish | 18-22 | Propagation, testing, edge cases |

---

## Phase 1: Foundation (Tasks 1-5)

### Task 1: Validate Spec Completeness

**Spec reference**: Section 7 (Open Questions)
**Acceptance criteria**: Prerequisite for all
**Dependencies**: None

#### Implementation steps:
1. Review all open questions (Q1-Q4) with stakeholders
2. Document decisions in spec amendments
3. Update spec file with resolved questions
4. Mark remaining blockers for deferred tasks

#### Verification:
- [ ] Q1 (material difference) resolved with concrete values
- [ ] Q2 (minimum sample size) has specific threshold
- [ ] Q3 (normalization) has MVP approach documented
- [ ] Q4 (altitude) marked as deferred or has scope

**Status**: ✅ COMPLETE - All questions resolved (Q1: 5%, Q2: 20 shots, Q3-Q4 deferred)

---

### Task 2: Define Domain Models (Shared)

**Spec reference**: R1, R2
**Acceptance criteria**: Foundation for A1-A3
**Dependencies**: Task 1 (partial - can proceed with core models)

#### Implementation steps:

**Android** (`domain/navcaddy/models/`):
1. Create `BagProfile` data class:
   - id: String (UUID)
   - name: String
   - isActive: Boolean
   - isArchived: Boolean
   - createdAt: Instant
   - updatedAt: Instant

2. Extend `Club` data class with new fields:
   - estimatedCarry: Int (meters)
   - inferredCarry: Int? (nullable until calculated)
   - inferredConfidence: Float? (0.0-1.0)
   - missBias: MissBias?
   - shaft: String?
   - flex: String?
   - notes: String?

3. Create `MissBias` data class:
   - dominantDirection: MissDirection
   - missType: MissType? (optional detailed type)
   - isUserDefined: Boolean
   - confidence: Float
   - lastUpdated: Instant

4. Create `MissType` enum:
   - SLICE, HOOK, PUSH, PULL, FAT, THIN

5. Create `DistanceAuditEntry` data class:
   - id: String
   - clubId: String
   - oldEstimated: Int
   - newEstimated: Int
   - inferredValue: Int?
   - confidence: Float?
   - reason: String
   - timestamp: Instant
   - wasAccepted: Boolean

**iOS** (`Core/Domain/NavCaddy/`):
- Mirror all Kotlin models with Swift conventions
- Use Codable for serialization
- Use Identifiable where appropriate

#### Verification:
- [x] All models compile on both platforms
- [x] Models are Codable/Serializable for persistence
- [x] Enums match across platforms (MissBiasDirection, MissType)
- [ ] Unit tests validate serialization roundtrip (deferred to Task 19)

**Status**: ✅ COMPLETE

---

### Task 3: Set Up Persistence - Android

**Spec reference**: R1, R2, R3
**Acceptance criteria**: Data integrity (C2)
**Dependencies**: Task 2

#### Implementation steps:
1. Create Room entities in `data/navcaddy/entities/`:
   - `BagProfileEntity` with all fields
   - `BagClubEntity` (join table: bagId + clubId + position + club data)
   - `DistanceAuditEntity` for audit trail

2. Update `NavCaddyDatabase`:
   - Add new entities to database annotation
   - Increment version, add migration

3. Create DAOs in `data/navcaddy/dao/`:
   - `BagProfileDao`: CRUD + getActiveBag + switchActiveBag
   - `BagClubDao`: getClubsForBag, updateClubDistance, updateMissBias
   - `DistanceAuditDao`: insertAudit, getAuditHistory

4. Create mappers in `EntityMappers.kt`:
   - `BagProfileEntity.toDomain()` / `BagProfile.toEntity()`
   - `BagClubEntity.toDomain()` / extension functions

5. Update `NavCaddyDataModule.kt`:
   - Provide new DAOs via Hilt

#### Verification:
- [x] Database migrates successfully (destructive for dev, migration needed for prod)
- [x] CRUD operations work for all entities
- [x] Foreign key constraints enforced (CASCADE delete tested)
- [x] Instrumented tests pass (24 tests covering all DAOs)

**Status**: ✅ COMPLETE

**Notes**:
- Uses `fallbackToDestructiveMigration()` for development
- Production deployment requires proper Migration(1, 2) implementation
- Entity mappers handle nested MissBias reconstruction

---

### Task 4: Set Up Persistence - iOS

**Spec reference**: R1, R2, R3
**Acceptance criteria**: Data integrity (C2)
**Dependencies**: Task 2

#### Implementation steps:
1. Create SwiftData models in `Core/Data/NavCaddy/Models/`:
   - `BagProfileRecord` with @Model annotation
   - `BagClubRecord` for club-bag association
   - `DistanceAuditRecord` for audit trail

2. Update `NavCaddyDataContainer.swift`:
   - Add new model types to schema

3. Extend `NavCaddyRepository.swift` protocol:
   - Add bag profile methods
   - Add club distance methods
   - Add audit trail methods

4. Implement in `NavCaddyRepositoryImpl.swift`:
   - Bag CRUD operations
   - Club updates with bag context
   - Audit entry recording

#### Verification:
- [x] SwiftData models persist correctly
- [x] Queries return expected results
- [x] Data survives app termination
- [x] Unit tests validate repository (27 tests in BagProfileRepositoryTests.swift)

**Status**: ✅ COMPLETE

**Notes**:
- SwiftData @Relationship with cascade delete for BagProfile -> BagClub
- @MainActor annotation for repository (ModelContext requires main thread)
- Domain mappers handle nested MissBias reconstruction

---

### Task 5: Extend Repository Interface

**Spec reference**: R1, R5
**Acceptance criteria**: A1 (multi-bag switching foundation)
**Dependencies**: Tasks 3, 4

#### Implementation steps:

**Android** - Extend `NavCaddyRepository.kt`:
```kotlin
// Bag Profile Operations
suspend fun createBag(profile: BagProfile): BagProfile
suspend fun updateBag(profile: BagProfile)
suspend fun archiveBag(bagId: String)
suspend fun duplicateBag(bagId: String, newName: String): BagProfile
fun getActiveBag(): Flow<BagProfile?>
fun getAllBags(): Flow<List<BagProfile>>
suspend fun switchActiveBag(bagId: String)

// Club Operations (bag-scoped)
fun getClubsForBag(bagId: String): Flow<List<Club>>
suspend fun updateClubDistance(bagId: String, clubId: String, estimated: Int)
suspend fun updateClubMissBias(bagId: String, clubId: String, bias: MissBias)
suspend fun addClubToBag(bagId: String, club: Club, position: Int)
suspend fun removeClubFromBag(bagId: String, clubId: String)

// Audit Trail
suspend fun recordDistanceAudit(entry: DistanceAuditEntry)
fun getAuditHistory(clubId: String): Flow<List<DistanceAuditEntry>>
```

**iOS** - Mirror in `NavCaddyRepository.swift` protocol with async/await

#### Verification:
- [ ] Repository compiles on both platforms
- [ ] Active bag flow emits on changes
- [ ] Switching bag updates all subscribers
- [ ] Integration tests verify propagation

---

## Phase 2: Bag Management (Tasks 6-9)

### Task 6: Implement Bag Management ViewModel - Android

**Spec reference**: R1, G1
**Acceptance criteria**: A1 (multi-bag switching)
**Dependencies**: Task 5

#### Implementation steps:
1. Create `BagManagementViewModel.kt`:
   - Inject `NavCaddyRepository` via Hilt
   - Expose `activeBag: StateFlow<BagProfile?>`
   - Expose `allBags: StateFlow<List<BagProfile>>`
   - Expose `uiState: StateFlow<BagManagementState>`

2. Create `BagManagementState.kt`:
   - sealed class with Loading, Success, Error states
   - Include `isRoundActive: Boolean` for switch confirmation

3. Implement actions:
   - `createBag(name: String)`
   - `switchBag(bagId: String)`
   - `duplicateBag(bagId: String)`
   - `renameBag(bagId: String, newName: String)`
   - `archiveBag(bagId: String)`

4. Handle round-active confirmation:
   - Check `SessionContextManager.hasActiveRound()`
   - Set `showConfirmationDialog` state if round active

#### Verification:
- [ ] ViewModel emits correct states
- [ ] Switching bag updates active bag flow
- [ ] Confirmation required when round active
- [ ] Unit tests cover all actions

---

### Task 7: Implement Bag Management ViewModel - iOS

**Spec reference**: R1, G1
**Acceptance criteria**: A1 (multi-bag switching)
**Dependencies**: Task 5

#### Implementation steps:
1. Create `BagManagementViewModel.swift`:
   - Use `@Observable` macro
   - `@MainActor` for UI safety
   - Inject dependencies via DependencyContainer

2. Create state properties:
   - `activeBag: BagProfile?`
   - `allBags: [BagProfile]`
   - `isLoading: Bool`
   - `error: Error?`
   - `showSwitchConfirmation: Bool`

3. Implement async actions matching Android

4. Handle round-active check via SessionContextManager

#### Verification:
- [ ] Observable updates trigger view refresh
- [ ] Async operations complete correctly
- [ ] Error handling works
- [ ] Unit tests pass

---

### Task 8: Build Bag Management UI - Android

**Spec reference**: R1, G4, C1, C4
**Acceptance criteria**: A1
**Dependencies**: Task 6

#### Implementation steps:
1. Create `BagManagementScreen.kt`:
   - List of bags with active indicator
   - FAB for creating new bag
   - Swipe actions: rename, duplicate, archive
   - Material 3 styling

2. Create `BagListItem.kt`:
   - Club count, last updated
   - Active badge
   - Tap to switch, long-press for options

3. Create `CreateBagDialog.kt`:
   - Name input with validation
   - Option to duplicate from existing

4. Create `SwitchBagConfirmationDialog.kt`:
   - Warning about active round
   - Confirm/Cancel actions

5. Add navigation route in `AppNavigation.kt`:
   - `NavCaddyDestination.BagManagement`

#### Verification:
- [ ] UI renders correctly
- [ ] Touch targets >= 48dp (C4)
- [ ] Accessibility labels present
- [ ] Preview composables work

---

### Task 9: Build Bag Management UI - iOS

**Spec reference**: R1, G4, C1, C4
**Acceptance criteria**: A1
**Dependencies**: Task 7

#### Implementation steps:
1. Create `BagManagementView.swift`:
   - List with swipe actions
   - Toolbar with add button
   - HIG-compliant styling

2. Create `BagRowView.swift`:
   - Club count, status indicators
   - Tap/swipe gestures

3. Create confirmation alerts using `.alert()` modifier

4. Add to navigation coordinator

#### Verification:
- [ ] UI matches HIG
- [ ] VoiceOver accessible
- [ ] iPad layout adapts correctly
- [ ] Preview works

---

## Phase 3: Distance Calibration (Tasks 10-13)

### Task 10: Implement Distance Inference Logic

**Spec reference**: R3, G2
**Acceptance criteria**: A2 (distance update suggestion)
**Dependencies**: Task 1 (Q1, Q2 resolved), Task 5

#### Implementation steps:
1. Create `DistanceInferenceService.kt` (Android):
   - Calculate inferred carry from shot history
   - Apply recency weighting (similar to pattern decay)
   - Filter by minimum sample size (Q2 value)
   - Calculate confidence score

2. Create algorithm:
   ```
   For each club:
   1. Get shots from last 90 days
   2. Filter shots with known carry outcome
   3. Apply decay weighting (14-day half-life)
   4. Calculate weighted average carry
   5. Calculate confidence = min(shots/minSample, 1.0) * decay_factor
   6. Compare to estimated: diff > threshold (Q1) AND confidence > 0.7
   7. If significant, create suggestion
   ```

3. Create `DistanceSuggestion` data class:
   - clubId, clubName
   - currentEstimated, suggestedValue
   - confidence, sampleSize
   - reason: String

4. Mirror implementation for iOS

#### Verification:
- [ ] Algorithm produces correct averages
- [ ] Confidence scales with sample size
- [ ] Suggestions only trigger above threshold
- [ ] Edge cases handled (no data, single shot)

---

### Task 11: Implement Suggestion Rate Limiting

**Spec reference**: R6
**Acceptance criteria**: Non-blocking prompts
**Dependencies**: Task 10

#### Implementation steps:
1. Create `SuggestionRateLimiter.kt`:
   - Track last suggestion time per club
   - Minimum 7 days between suggestions for same club
   - Maximum 2 suggestions per session
   - Store timestamps in SharedPreferences/UserDefaults

2. Create `SuggestionManager.kt`:
   - Coordinates inference + rate limiting
   - Exposes `pendingSuggestions: Flow<List<DistanceSuggestion>>`
   - Handles accept/reject/remind-later actions

3. Implement remind-later:
   - Snooze for 3 days
   - Track snooze count (max 3 then auto-dismiss)

#### Verification:
- [ ] Rate limiting prevents spam
- [ ] Remind-later delays suggestion
- [ ] Session limit respected
- [ ] Unit tests cover all paths

---

### Task 12: Build Distance Suggestion UI - Android

**Spec reference**: R3, R6, G4
**Acceptance criteria**: A2
**Dependencies**: Tasks 10, 11

#### Implementation steps:
1. Create `DistanceSuggestionCard.kt`:
   - Non-modal card at bottom of BagEdit screen
   - Shows: club name, current vs suggested, confidence
   - Actions: Accept, Decline, Remind Later

2. Integrate with `BagEditScreen`:
   - Observe `pendingSuggestions` flow
   - Show dismissible cards

3. Handle accept action:
   - Update club distance via repository
   - Record audit entry with reason
   - Show confirmation snackbar

4. Handle decline:
   - Dismiss suggestion
   - Don't show again for this value

#### Verification:
- [ ] Card appears when suggestion available
- [ ] Accept updates distance and records audit
- [ ] Decline dismisses without update
- [ ] Remind-later delays reappearance

---

### Task 13: Build Distance Suggestion UI - iOS

**Spec reference**: R3, R6, G4
**Acceptance criteria**: A2
**Dependencies**: Tasks 10, 11

#### Implementation steps:
1. Create `DistanceSuggestionBanner.swift`:
   - Floating banner or inline card
   - HIG-compliant styling
   - Swipe to dismiss

2. Integrate with bag editing views

3. Handle all actions with haptic feedback

#### Verification:
- [ ] UI follows HIG
- [ ] Interactions feel native
- [ ] Audit trail works

---

## Phase 4: Miss Bias (Tasks 14-17)

### Task 14: Implement Miss Bias Aggregation

**Spec reference**: R4
**Acceptance criteria**: A3 (miss bias influences strategy)
**Dependencies**: Task 2

#### Implementation steps:
1. Create `MissBiasAggregator.kt`:
   - Reuse existing `MissPatternAggregator` logic
   - Filter patterns by specific club
   - Identify dominant direction
   - Calculate confidence

2. Create per-club bias calculation:
   ```
   For each club:
   1. Get shots with this club
   2. Group by miss direction
   3. Calculate frequency percentages
   4. If any direction > 40%: that's dominant
   5. If no clear dominant (all < 40%): STRAIGHT
   6. Confidence = frequency * sample_factor
   ```

3. Create `MissBiasSuggestion` for auto-suggest mode

#### Verification:
- [ ] Aggregation matches expected patterns
- [ ] Club-specific filtering works
- [ ] Confidence calculation correct
- [ ] Unit tests cover edge cases

---

### Task 15: Build Club Edit Screen - Android

**Spec reference**: R2, R4, G4, C4
**Acceptance criteria**: A3
**Dependencies**: Tasks 6, 14

#### Implementation steps:
1. Create `ClubEditScreen.kt`:
   - Club info section (name, type, loft, shaft, flex, notes)
   - Distance section (estimated carry, inferred display)
   - Miss bias section (direction picker, miss type picker)
   - Large touch targets, minimal typing

2. Create `DirectionPicker.kt`:
   - Visual compass-style picker
   - Options: Left, Straight, Right
   - Shows current selection prominently

3. Create `MissTypePicker.kt`:
   - Optional detail selector
   - Chips: Slice, Hook, Push, Pull, Fat, Thin

4. Save changes on back navigation

#### Verification:
- [ ] All fields editable
- [ ] Pickers accessible (48dp targets)
- [ ] Changes persist correctly
- [ ] Validation prevents invalid data

---

### Task 16: Build Club Edit Screen - iOS

**Spec reference**: R2, R4, G4, C4
**Acceptance criteria**: A3
**Dependencies**: Tasks 7, 14

#### Implementation steps:
1. Create `ClubEditView.swift`:
   - Form-based layout
   - Pickers for direction/type
   - Number input for distances

2. Use native iOS components:
   - Segmented control for direction
   - Picker for miss type
   - TextField with number keyboard

#### Verification:
- [ ] Form follows HIG
- [ ] Keyboard handling correct
- [ ] Data saves on dismiss

---

### Task 17: Integrate Miss Bias with Strategy

**Spec reference**: R4, R5
**Acceptance criteria**: A3 (miss bias influences strategy)
**Dependencies**: Tasks 14, 15, 16

#### Implementation steps:
1. Update `SessionContextManager`:
   - Include active bag's club biases in context
   - Provide `getClubMissBias(clubId): MissBias?`

2. Update NavCaddy persona responses:
   - When recommending clubs, consider miss bias
   - Example: "Your driver tends right, so aim left on this hole"

3. Create integration points for future modules:
   - PinSeeker: safety margin adjustments
   - Live Caddy: target line suggestions

4. Document integration API for future modules

#### Verification:
- [ ] Context includes miss bias data
- [ ] NavCaddy recommendations reflect bias
- [ ] API documented for future use

---

## Phase 5: Integration & Polish (Tasks 18-22)

### Task 18: Implement Data Propagation

**Spec reference**: R5, G5
**Acceptance criteria**: A1 (changes propagate globally)
**Dependencies**: Tasks 5, 6, 7

#### Implementation steps:
1. Create `BagChangeNotifier.kt`:
   - Singleton that broadcasts bag changes
   - Uses SharedFlow for multi-subscriber support

2. Subscribe in all dependent modules:
   - SessionContextManager
   - MissPatternAggregator (recompute on bag change)
   - DistanceInferenceService (recompute)

3. Verify instant propagation:
   - Change bag → immediate UI updates everywhere
   - No stale data in any module

#### Verification:
- [ ] Switching bag updates all modules
- [ ] Distance changes reflect immediately
- [ ] Miss bias changes propagate to context
- [ ] No race conditions or stale reads

---

### Task 19: Write Unit Tests - Domain Layer

**Spec reference**: All
**Acceptance criteria**: All
**Dependencies**: Tasks 2, 10, 14

#### Implementation steps:
1. Test `BagProfile` operations:
   - Create, duplicate, archive
   - Active bag switching

2. Test `DistanceInferenceService`:
   - Average calculation
   - Confidence scoring
   - Threshold comparison

3. Test `MissBiasAggregator`:
   - Dominant direction calculation
   - Club-specific filtering

4. Test `SuggestionRateLimiter`:
   - Rate limiting logic
   - Snooze functionality

#### Verification:
- [ ] >80% code coverage on domain layer
- [ ] All edge cases covered
- [ ] Tests run in CI

---

### Task 20: Write Unit Tests - Data Layer

**Spec reference**: C2
**Acceptance criteria**: Data integrity
**Dependencies**: Tasks 3, 4

#### Implementation steps:
1. Test DAOs (Android):
   - CRUD operations
   - Query correctness
   - Foreign key constraints

2. Test Repository (both platforms):
   - Active bag retrieval
   - Club updates
   - Audit trail recording

3. Test Mappers:
   - Entity ↔ Domain roundtrip
   - Null handling

#### Verification:
- [ ] All DAO methods tested
- [ ] Repository integration tested
- [ ] Data integrity maintained

---

### Task 21: Write Integration Tests

**Spec reference**: A1, A2, A3
**Acceptance criteria**: All acceptance criteria
**Dependencies**: Tasks 18, 19, 20

#### Implementation steps:
1. Test A1 (multi-bag switching):
   - Create two bags
   - Switch between them
   - Verify global state updates

2. Test A2 (distance suggestion):
   - Log sufficient shots
   - Verify suggestion appears
   - Accept and verify audit trail

3. Test A3 (miss bias strategy):
   - Set driver bias to "right"
   - Request recommendation
   - Verify bias-aware response

#### Verification:
- [ ] All acceptance criteria have passing tests
- [ ] End-to-end flow works
- [ ] Tests run in CI

---

### Task 22: Handle Edge Cases & Polish

**Spec reference**: C2, C3, C4
**Acceptance criteria**: All
**Dependencies**: All previous tasks

#### Implementation steps:
1. Handle empty bag:
   - Prompt to add clubs
   - Prevent activation without clubs

2. Handle deleted club references:
   - Shots referencing deleted clubs
   - Graceful degradation

3. Handle first-time user:
   - Default bag creation
   - Onboarding for distance setup

4. Optimize performance:
   - Lazy loading for audit history
   - Pagination for long club lists

5. Add analytics events:
   - Bag created, switched, archived
   - Distance updated (manual vs accepted suggestion)
   - Miss bias set

#### Verification:
- [ ] Empty states handled gracefully
- [ ] No crashes on edge cases
- [ ] Performance acceptable on low-end devices
- [ ] Analytics events fire correctly

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| Spec Q1-Q2 not resolved | High | High | Proceed with proposed defaults, flag for review |
| Distance inference inaccuracy | Medium | High | Conservative thresholds, user confirmation required |
| Propagation race conditions | Medium | Medium | Single source of truth via repository Flow |
| UI complexity on mobile | Medium | Medium | Iterative design, user testing |
| Data migration issues | Low | High | Thorough testing, versioned migrations |
| Performance with large shot history | Low | Medium | Pagination, background processing |

---

## Open Questions

| Question | Spec Ref | Proposed Default | Status |
|----------|----------|------------------|--------|
| Q1: Material distance difference | R3 | 5% of estimated carry | RESOLVED |
| Q2: Minimum sample size | R3 | 20 shots/club | RESOLVED |
| Q3: Wind/temp normalization | R3 | Skip for MVP | DEFERRED |
| Q4: Altitude profiles | R3 | Skip for MVP | DEFERRED |

---

## Next Steps

1. **Resolve Q1-Q2** with stakeholders (blocks Phase 3)
2. **Begin Task 1** (spec validation)
3. **Proceed with Phase 1** (Tasks 2-5) in parallel
4. **Review plan** after Phase 1 completion
