# Feature: live-caddy-mode

## 1. Problem statement
During a round, golfers need fast, low-friction decision support. Current apps focus on scorekeeping and post-round stats, not real-time strategy. Users also face:
- Changing weather impacting carry and shot shape.
- Fatigue affecting timing and dispersion.
- Course hazards that punish a golfer’s dominant miss.

We need an on-course mode that merges play-as conditions, readiness, course hazard modeling, and the user’s miss bias to recommend survivable targets.

## 2. Goals
- G1: Deliver on-course strategy with minimal screen time (voice-first + fast taps).
- G2: Provide a Forecaster HUD that accounts for weather effects on ball flight.
- G3: Use biometric readiness to adjust risk tolerance in recommendations.
- G4: Provide a hole “danger zone” summary and recommended landing zones tailored to handicap and miss bias.
- G5: Enable a real-time shot logger that is quick enough to use mid-round and feeds downstream analytics.

## 3. Non-goals
- NG1: Real-time 3D ball tracking or launch monitor replacement.
- NG2: Guaranteed yardage accuracy without validated distance sources.
- NG3: Full course mapping creation tools (assume course data is available via provider/import).

## 4. Functional requirements
- R1: Live Round Context
  - Start/Resume/End a round.
  - Maintain current hole, tee position, and shot count context.
  - Persist state if app is backgrounded (PWA safe state restoration).

- R2: Forecaster HUD (Weather Integration)
  - Fetch real-time weather at geolocation:
    - wind speed/direction
    - temperature
    - humidity
  - Display a compact HUD optimized for outdoor visibility.
  - Compute a “conditions adjustment” signal used by strategy suggestions:
    - air density proxy and its effect on carry (simplified model is acceptable for MVP)
    - wind component (head/tail/cross) relative to target line

- R3: BodyCaddy (Wearable Integration)
  - Sync supported wearable metrics (as available per platform):
    - HRV, sleep, stress (or equivalents)
  - Compute Readiness Score (0–100) with transparent contributors.
  - Strategy adjustment rules:
    - lower readiness increases conservatism (bigger safety margins, fewer hero lines)
  - Provide user override (manual readiness) when wearables are unavailable.

- R4: PinSeeker AI Map (Hole Strategy Summary)
  - Input: hole geometry + hazards + pin position (if available) + user handicap + miss bias.
  - Output:
    - danger zones (OB, water, bunkers, high-penalty rough)
    - recommended landing zone and target line
    - risk callouts (“right miss brings water into play”)
  - Must personalize based on:
    - handicap (e.g., 9)
    - club distances and dispersion (from profile)
    - dominant miss (from miss store)

- R5: Voice-first Interaction (Bones)
  - Support queries like:
    - “What’s the play off the tee?”
    - “Club up or down?”
    - “Where’s the bailout?”
  - Route to the relevant Live Caddy screens and respond in Bones persona.

- R6: Real-Time Shot Logger
  - One-second logging flow:
    - select club (tap)
    - select result (fairway/rough/bunker/water/OB/green/short/long/left/right)
  - Haptic confirmation on save.
  - Offline-first queueing: logs stored locally and synced when online.
  - Logging writes to:
    - round history
    - miss store signals (with controlled inference rules)

- R7: Safety and Distraction Controls
  - Provide “low distraction mode”:
    - large touch targets
    - reduced animations
    - optional auto-lock prevention only while actively using the HUD (platform permitting)
  - Explicitly discourage phone use during active swing sequence (UX copy).

## 5. Acceptance criteria
- A1: Weather HUD and adjustment
  - GIVEN: User has location enabled during an active round
  - WHEN: User opens Live Caddy HUD
  - THEN: Wind/temperature/humidity render within 2 seconds and the strategy engine applies a conditions adjustment to carry recommendations.

- A2: Readiness impacts strategy
  - GIVEN: Wearable sync reports low readiness (below configured threshold)
  - WHEN: User asks “Where should I aim?”
  - THEN: Bones recommends more conservative targets with larger safety margins and explains the risk reduction briefly.

- A3: Hazard-aware landing zone
  - GIVEN: Hole hazards include OB right and water long
  - WHEN: User requests “PinSeeker summary”
  - THEN: App highlights danger zones and recommends a landing zone that avoids the user’s dominant miss and typical distance error.

- A4: Shot logger speed and persistence
  - GIVEN: User is mid-round with poor reception
  - WHEN: User logs a shot
  - THEN: The shot is saved locally with haptic confirmation and appears in the round timeline; it syncs automatically when connection returns.

## 6. Constraints & invariants
- C1:
  - Android: Material 3
  - iOS: Human Interface Guidelines
- C2: Outdoor visibility
  - High contrast, large typography, minimal fine detail.
- C3: Offline-first
  - Core HUD, logging, and cached course data function without connectivity.
- C4: Geolocation privacy
  - Location used only for round context and weather; user can disable and still use manual entry.

## 7. Open questions
- Q1: Weather provider and API limits?
- Q2: Course data source and hazard geometry format?
- Q3: Readiness algorithm: fixed weights vs personalized calibration?
- Q4: Minimum logging taxonomy required for downstream stats?
