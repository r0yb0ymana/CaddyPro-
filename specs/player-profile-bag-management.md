# Feature: player-profile-bag-management

## 1. Problem statement
Live strategy and coaching are only as good as the golfer’s underlying profile:
- Multiple bag setups exist (tournament vs sim vs travel).
- Distances drift over time due to fitness, technique, and conditions.
- Miss tendencies differ by club and context.

We need a “Digital Locker” that stores and maintains accurate bag specs, distances, and miss biases that feed NavCaddy, Live Caddy, and Coach Mode.

## 2. Goals
- G1: Support multiple bag profiles with clear activation and switching.
- G2: Maintain club distance calibration: estimated vs inferred, with suggested updates.
- G3: Store per-club miss bias that influences live strategy recommendations.
- G4: Provide a clean editing experience optimized for mobile.
- G5: Ensure profile changes propagate consistently to all modules.

## 3. Non-goals
- NG1: Full equipment marketplace or e-commerce.
- NG2: Launch monitor level distance modeling.
- NG3: Automatic miss inference without user consent or sufficient data.

## 4. Functional requirements
- R1: Bag Profiles
  - Create, duplicate, rename, archive bag profiles.
  - Support different club counts (14-club tournament bag vs half bag).
  - One active bag at a time; switching prompts confirmation if a round is active.

- R2: Club Inventory Model
  - For each club:
    - label (e.g., “7i”, “3W”)
    - loft (optional)
    - shaft/flex (optional)
    - notes (optional)
  - Required fields for strategy:
    - carry distance (estimated + inferred)
    - dispersion/miss bias (left/right/straight + miss type)

- R3: Distance Calibration
  - Track:
    - Estimated Carry: user-input expected carry
    - Inferred Carry: computed from logged outcomes + conditions (if available)
  - Suggest updates when inference is statistically significant:
    - minimum sample size threshold per club
    - confidence score and recentness weighting
  - Provide “accept”, “reject”, “remind later” actions.
  - Maintain audit trail:
    - old value, new value, timestamp, reason (“recent short bias”)

- R4: Miss Bias Mapping
  - Manual input per club:
    - dominant direction: left/right/straight
    - miss type: slice/hook/push/pull/fat/thin (optional)
  - Automatic suggestion (optional):
    - derived from shot logger distribution
    - requires user confirmation before applying
  - Miss biases feed:
    - PinSeeker strategy safety margins
    - Bones recommendations and club selection

- R5: Data Propagation
  - Changes to active bag instantly update:
    - Live Caddy distances and recommended clubs
    - Coach Mode analysis mappings
    - NavCaddy memory interpretation (“came up short with 7i”)

- R6: Smart Suggestions (Micro-settings)
  - Trigger rule examples:
    - “3W inferred carry has increased by X yards over last Y shots”
  - UI:
    - non-blocking prompt
    - quick accept/decline
  - Must be rate-limited to avoid nagging.

## 5. Acceptance criteria
- A1: Multi-bag switching
  - GIVEN: User has “Tournament Bag” and “Simulator Bag”
  - WHEN: User switches active bag outside an active round
  - THEN: App updates distances/miss biases globally and confirms the active bag change.

- A2: Distance update suggestion
  - GIVEN: User logs sufficient shots with 3-wood and inferred carry differs materially from estimated carry
  - WHEN: Confidence exceeds threshold
  - THEN: App prompts to update carry distance and, if accepted, persists the change with an audit entry.

- A3: Miss bias influences strategy
  - GIVEN: User sets driver miss bias to “right”
  - WHEN: User requests tee-shot strategy on a hole with OB right
  - THEN: PinSeeker and Bones recommendations avoid right exposure with a conservative line/club.

## 6. Constraints & invariants
- C1:
  - Android: Material 3
  - iOS: Human Interface Guidelines
- C2: Data integrity
  - Active bag must always have valid distance entries for clubs used in strategy.
- C3: Suggestion thresholds
  - No auto-overwrites of user-entered values; user confirmation is mandatory.
- C4: UX constraints
  - Mobile-first editing: large touch targets, minimal typing, sensible defaults.

## 7. Open questions
- Q1: What constitutes “material” distance difference (absolute yards vs %)?
- Q2: What is the minimum sample size per club for inferred carry updates?
- Q3: How do we normalize inferred carry for wind/temperature in MVP?
- Q4: Do we support different distances per course altitude profile?
