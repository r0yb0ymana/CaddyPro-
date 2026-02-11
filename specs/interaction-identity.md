# Feature: interaction-identity

## 1. Problem statement
On-course interaction fails when it demands attention. Golfers need:
- Voice-first control while the phone stays in pocket or cart.
- Confirmation feedback without staring at the screen.
- System-driven micro-suggestions that improve data quality over time without annoying the user.

We need a consistent interaction identity that prioritizes speed, low distraction, and trust.

## 2. Goals
- G1: Voice-first as the primary control path (Omni Button).
- G2: Tactile confirmation for critical quick actions (shot logging).
- G3: Smart micro-suggestions that improve profile accuracy (carry updates, miss bias prompts).
- G4: Keep cognitive load low with predictable interaction patterns and minimal steps.
- G5: Ensure accessibility and outdoor usability.

## 3. Non-goals
- NG1: Full conversational replacement of all settings and editing screens.
- NG2: Always-on microphone listening (push-to-talk only, unless user opts in explicitly).
- NG3: Frequent “coaching nags” that interrupt the round.

## 4. Functional requirements
- R1: Omni Button (Voice-first Control)
  - Persistent control accessible globally.
  - Modes:
    - tap to talk (voice)
    - type (text)
  - Provides immediate state cues:
    - listening
    - thinking
    - responding
  - Supports “route + speak” behavior (navigate while responding).

- R2: Audio Output
  - Optional readout of key recommendations.
  - User controls:
    - mute
    - voice speed (if supported)
    - “brief vs detailed” setting

- R3: Haptics (Tactile Feedback)
  - Haptic confirmation for:
    - shot saved
    - undo available
  - Haptic error pattern for:
    - failed save (queued offline)
  - Must be subtle and consistent.

- R4: Smart Suggestions (Micro-settings)
  - Suggest updates based on behavior:
    - carry distance drift
    - recurring miss bias by club
    - readiness threshold personalization prompts
  - Constraints:
    - rate limit (max N per week)
    - never interrupt a swing sequence (respect “in-round focus” states)
    - provide “don’t ask again” per suggestion type
  - Suggestions must be explainable:
    - show the evidence summary (“last 12 3W logs show +10y carry vs saved value”).

- R5: Low-distraction UI patterns
  - Large touch targets in on-course screens.
  - Minimal typing; defaults and quick toggles.
  - High contrast and sunlight-readable typography.

- R6: Accessibility
  - Support dynamic text sizing.
  - Ensure sufficient contrast ratios.
  - Voice input alternative to fine motor interactions.

## 5. Acceptance criteria
- A1: Voice-first navigation
  - GIVEN: User is on any screen
  - WHEN: User uses Omni Button and says “Show me my recovery”
  - THEN: App routes to Recovery module and reads out a brief summary if audio output is enabled.

- A2: Haptic confirmation for logging
  - GIVEN: User logs a shot via quick tap flow
  - WHEN: Save completes (local or queued)
  - THEN: A haptic confirmation fires and the UI reflects saved state without additional steps.

- A3: Smart suggestion rate limiting
  - GIVEN: User behavior triggers multiple carry update suggestions
  - WHEN: Suggestions exceed configured weekly cap
  - THEN: System suppresses additional prompts and queues them for later review in Settings.

## 6. Constraints & invariants
- C1:
  - Android: Material 3
  - iOS: Human Interface Guidelines
- C2: Distraction minimization
  - No modal interruptions during “active shot” states.
- C3: Consent
  - Voice and wearable features require explicit permissions and clear rationale screens.

## 7. Open questions
- Q1: Do we support wake-word in v1 (default: no)?
- Q2: What is the weekly cap for micro-suggestions?
- Q3: What is the definition of “active shot” state for suppression rules?
- Q4: How do we expose suggestion history and evidence in Settings?
