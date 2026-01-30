# Live Caddy Mode - End-to-End Test Plan

**Feature**: Live Caddy Mode
**Spec Reference**: specs/live-caddy-mode.md
**Plan Reference**: specs/live-caddy-mode-plan.md Task 28
**Date**: 2026-01-31

## Purpose

This document provides manual test procedures to verify all acceptance criteria (A1-A4) for Live Caddy Mode. Each acceptance criterion is tested end-to-end in a realistic on-course scenario.

---

## A1: Weather HUD and Adjustment

**GIVEN**: User has location enabled during an active round
**WHEN**: User opens Live Caddy HUD
**THEN**: Wind/temperature/humidity render within 2 seconds and the strategy engine applies a conditions adjustment to carry recommendations.

### Test Steps

1. **Prerequisites**
   - Install app on test device
   - Enable location permissions in device settings
   - Ensure network connectivity (for weather API)
   - Start a new round from Home screen

2. **Execute Test**
   - Open app and navigate to "Start Round"
   - Enter course name (e.g., "Pebble Beach")
   - Tap "Start Round"
   - Start timer when Live Caddy screen begins loading
   - Observe weather HUD section

3. **Verify Weather Rendering**
   - [ ] Weather HUD appears within 2 seconds of screen load
   - [ ] Wind speed is displayed (e.g., "10 m/s")
   - [ ] Wind direction is displayed (e.g., "N" or "180°")
   - [ ] Temperature is displayed (e.g., "72°F" or "22°C")
   - [ ] Humidity is displayed (e.g., "65%")

4. **Verify Conditions Adjustment**
   - Tap to expand weather HUD (if collapsed)
   - [ ] Conditions chip shows carry adjustment (e.g., "-5m" or "+3m")
   - [ ] Tap on conditions chip
   - [ ] Adjustment reason is human-readable (e.g., "Cold air and 5 m/s headwind")
   - [ ] Reason mentions specific factors (temperature, wind, direction)

### Expected Results

- [x] **Performance**: Weather HUD visible within 2 seconds
- [x] **Data Completeness**: All weather fields populated with valid data
- [x] **Calculation**: Conditions adjustment is calculated and displayed
- [x] **Transparency**: Adjustment reason explains the calculation

### Pass Criteria

All checkboxes must be checked. Weather HUD must render in under 2 seconds on mid-range device (Pixel 5 or equivalent).

---

## A2: Readiness Impacts Strategy

**GIVEN**: Wearable sync reports low readiness (below configured threshold)
**WHEN**: User asks "Where should I aim?"
**THEN**: Bones recommends more conservative targets with larger safety margins and explains the risk reduction briefly.

### Test Steps

1. **Setup Low Readiness**
   - Navigate to Settings > Live Caddy > Readiness
   - Set manual readiness to 35 (low) using slider
   - Save settings
   - Return to Live Caddy screen

2. **View Strategy with Low Readiness**
   - On Live Caddy screen, tap "Show Hole Strategy"
   - Observe PinSeeker map
   - Note the safety margin value (should be larger than normal)

3. **Test with High Readiness for Comparison**
   - Return to Settings
   - Set manual readiness to 85 (high)
   - Return to Live Caddy screen
   - Tap "Show Hole Strategy"
   - Note the safety margin value (should be smaller)

4. **Verify Readiness Adjustment**
   - Set readiness back to 35 (low)
   - View PinSeeker map again
   - [ ] Safety margin with readiness 35 is approximately 2x margin with readiness 85
   - [ ] Landing zone visual is more conservative (further from hazards)

5. **Test Voice Query (Optional if voice is implemented)**
   - Tap microphone icon
   - Say: "Where should I aim?"
   - Listen to Bones response
   - [ ] Response mentions conservative play
   - [ ] Response references readiness or fatigue
   - [ ] Recommended aim point is safe/conservative

### Expected Results

- [x] **Safety Margin Scaling**: Low readiness increases safety margin significantly
- [x] **Visual Feedback**: Landing zone indicator reflects conservative strategy
- [x] **Voice Response**: Bones explains risk reduction in persona-appropriate language
- [x] **Transparency**: User understands why conservative recommendation is given

### Pass Criteria

Safety margin with readiness=35 must be at least 1.5x larger than with readiness=85. Voice response (if tested) must mention conservatism/safety.

---

## A3: Hazard-Aware Landing Zone

**GIVEN**: Hole hazards include OB right and water long
**WHEN**: User requests "PinSeeker summary"
**THEN**: App highlights danger zones and recommends a landing zone that avoids the user's dominant miss and typical distance error.

### Test Steps

1. **Setup Test Hole with Hazards**
   - Use test course data with known hazards:
     - Hole 1: Par 4, 360m
     - Hazards: OB right (200-300m), Water long (350m+)
   - Start round on this hole

2. **Setup Dominant Miss Pattern**
   - Navigate to Settings > Miss Patterns (or use test data)
   - Set dominant miss to SLICE (right)
   - Confidence: 80%

3. **View PinSeeker Strategy**
   - On Live Caddy screen, tap "Show Hole Strategy"
   - Observe PinSeeker map

4. **Verify Danger Zones**
   - [ ] Danger zones list is visible
   - [ ] "OB right" is listed as danger zone
   - [ ] "Water long" is listed as danger zone
   - [ ] Hazards are visually highlighted on map (if visual map implemented)

5. **Verify Landing Zone Personalization**
   - [ ] Recommended landing zone aims LEFT of center (away from slice)
   - [ ] Landing zone is SHORT of water hazard (conservative distance)
   - [ ] Visual cue indicates aim direction (e.g., "Aim left of center")

6. **Verify Risk Callouts**
   - [ ] Risk callout mentions "Right miss brings OB into play"
   - [ ] Risk callout mentions dominant miss (slice) if applicable
   - [ ] Risk callouts are prioritized (OB > Water > Bunkers)
   - [ ] Maximum 3 risk callouts displayed

7. **Test with Different Miss Pattern**
   - Change dominant miss to HOOK (left)
   - View PinSeeker map again
   - [ ] Landing zone now aims RIGHT of center (away from hook)
   - [ ] Risk callouts adjust to reflect hook hazards

### Expected Results

- [x] **Hazard Detection**: All relevant hazards are identified and listed
- [x] **Miss Pattern Filtering**: Only hazards affecting dominant miss are prioritized
- [x] **Landing Zone Adjustment**: Aim point compensates for miss tendency
- [x] **Risk Communication**: Clear callouts explain hazards and why they matter

### Pass Criteria

Landing zone must shift based on dominant miss (left for slice, right for hook). Risk callouts must reference the specific hazards that affect the user's miss pattern.

---

## A4: Shot Logger Speed and Persistence

**GIVEN**: User is mid-round with poor reception
**WHEN**: User logs a shot
**THEN**: The shot is saved locally with haptic confirmation and appears in the round timeline; it syncs automatically when connection returns.

### Test Steps

1. **Test Online Shot Logging First**
   - Ensure device has network connectivity
   - On Live Caddy screen, tap FAB (floating action button) to log shot
   - Start timer
   - Select club (e.g., "7I")
   - Select result (e.g., "FAIRWAY")
   - Stop timer
   - [ ] Total time from FAB tap to confirmation < 1 second (target)
   - [ ] Haptic feedback fires on confirmation
   - [ ] Toast message shows shot details ("7I - FAIRWAY")
   - [ ] Shot logger closes automatically

2. **Test Offline Shot Logging**
   - Enable airplane mode on device
   - Wait 5 seconds for offline detection
   - [ ] Connectivity banner appears showing "Offline"
   - Tap FAB to log shot
   - Select club (e.g., "Driver")
   - Select result (e.g., "ROUGH")
   - [ ] Shot saves locally within 1 second
   - [ ] Haptic feedback fires (offline doesn't block this)
   - [ ] Toast confirms shot: "Driver - ROUGH (will sync when online)"
   - [ ] Connectivity banner updates: "1 shot queued"

3. **Log Multiple Shots Offline**
   - Log second shot: "5I" - "GREEN"
   - [ ] Connectivity banner updates: "2 shots queued"
   - Log third shot: "PW" - "BUNKER"
   - [ ] Connectivity banner updates: "3 shots queued"

4. **Test Sync on Reconnection**
   - Disable airplane mode
   - Wait for network reconnection
   - [ ] Connectivity banner shows "Syncing..."
   - [ ] After sync completes, banner shows "3 shots synced"
   - [ ] Banner disappears after a few seconds
   - [ ] Pending shot count returns to 0

5. **Verify Shot Persistence**
   - Navigate away from Live Caddy screen
   - Kill app (swipe away from recent apps)
   - Reopen app
   - Navigate to Live Caddy screen
   - [ ] All logged shots are visible in round timeline
   - [ ] Shot details are correct (club, lie, timestamp)

6. **Performance Benchmark**
   - Perform shot logging flow 10 times
   - Measure average time from FAB tap to confirmation
   - [ ] Average latency < 1 second
   - [ ] P95 latency < 1.5 seconds
   - [ ] No dropped shots or errors

### Expected Results

- [x] **Speed**: Shot logging completes in under 1 second
- [x] **Haptic Feedback**: Tactile confirmation on each shot
- [x] **Offline-First**: Shots save locally even without network
- [x] **Queue Management**: Pending shots tracked and displayed
- [x] **Auto-Sync**: Shots sync automatically when online
- [x] **Persistence**: Shots survive app restart

### Pass Criteria

Shot logging must complete in under 1 second (P50) and under 1.5 seconds (P95). Offline shots must queue and sync automatically. Haptic feedback must fire on all devices that support it.

---

## Test Environment

### Devices
- **Primary**: Google Pixel 5 (Android 13)
- **Secondary**: Samsung Galaxy S21 (Android 14)
- **Low-End**: Moto G Power (Android 12)

### Network Conditions
- **Fast WiFi**: 50+ Mbps
- **4G LTE**: 10-20 Mbps
- **Poor Reception**: Airplane mode (offline)

### Location
- Outdoor testing preferred (for realistic GPS signal)
- Indoor testing acceptable with mock location

### Test Data
- Course: "Test Course 1" with 3 holes
- Hole 1: Par 4, 360m, hazards: OB right, water long
- Hole 2: Par 3, 150m, hazards: bunkers front
- Hole 3: Par 5, 500m, hazards: trees left, water right

---

## Acceptance Criteria Summary

| Criterion | Status | Notes |
|-----------|--------|-------|
| A1: Weather HUD (<2s) | ⬜ PENDING | Test on target devices |
| A2: Readiness Strategy | ⬜ PENDING | Verify margin scaling |
| A3: Hazard-Aware Zones | ⬜ PENDING | Test with slice/hook patterns |
| A4: Shot Logger Speed | ⬜ PENDING | Verify <1s logging, offline sync |

---

## Notes for Tester

1. **Weather API**: Requires valid API key in build config. If weather fails to load, check API quota and key validity.

2. **Mock Data**: For testing without real course data, use the sample holes included in the app's test fixtures.

3. **Readiness Testing**: Manual readiness entry is sufficient for A2 testing. Wearable sync can be tested separately if devices are available.

4. **Performance**: Shot logging speed is critical for A4. Use a stopwatch or screen recording to verify < 1 second latency.

5. **Offline Testing**: Ensure airplane mode is enabled for at least 5 seconds before logging shots to properly trigger offline mode.

6. **Haptic Feedback**: Verify on physical devices. Emulators may not support haptic feedback properly.

---

## Sign-Off

**Tester Name**: _____________________
**Date**: _____________________
**Device Tested**: _____________________
**Test Result**: ⬜ PASS  ⬜ FAIL  ⬜ BLOCKED

**Notes**:
