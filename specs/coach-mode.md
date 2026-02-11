# Feature: coach-mode

## 1. Problem statement
Golfers improve when they understand:
- What is failing (pattern identification)
- Why it is failing (mechanics or decision errors)
- What to do next (a focused practice plan)

Most apps either dump stats without coaching, or give generic tips without tying to the golfer’s data. We need post-round diagnostics that produce one clear priority, backed by evidence, and a practical plan.

## 2. Goals
- G1: Provide post-round “diagnostics” that translate stats into actionable focus.
- G2: Offer swing video analysis that identifies likely technical faults and outputs a single high-leverage fix.
- G3: Identify problem clubs and stroke gains/losses vs a benchmark.
- G4: Generate a weekly practice plan that schedules drills and exports to calendar (.ics).
- G5: Personalize drills dynamically based on the user’s round-killing miss patterns.

## 3. Non-goals
- NG1: Replacing an in-person coach.
- NG2: Medical or injury diagnosis from video.
- NG3: Perfect biomechanical modeling. MVP uses high-signal heuristics and confidence gating.

## 4. Functional requirements
- R1: SwingSensei (Computer Vision)
  - Upload swing videos (front-on and/or down-the-line).
  - Extract key frames and timing markers (address, top, impact, follow-through).
  - Detect common faults with confidence scores (e.g., early extension, over-the-top).
  - Output a “One-Fix” recommendation:
    - single priority cue
    - one drill
    - what success looks like
  - If confidence is low, request another angle or better lighting instead of guessing.

- R2: Shot Surgeon (Statistical Autopsy)
  - Analyze last N rounds (default: 10) including penalties and miss outcomes.
  - Identify:
    - Problem Clubs: highest penalty rate, worst dispersion, biggest strokes lost
    - Clinical Gains: areas gaining/losing strokes vs benchmark (scratch as default)
  - Output:
    - top 3 contributors to scoring issues
    - one primary focus area for next practice cycle
  - Require data sufficiency checks (minimum rounds/shots) before strong claims.

- R3: TempoSetter Scheduler (Practice Plan + .ics Export)
  - User selects:
    - available days
    - session length
    - access context (range, nets, putting green, gym)
  - System generates:
    - weekly plan with drill titles, duration, objective
    - progressive overload rules (increase difficulty, not volume, when targets met)
  - Export as .ics compatible with Apple/Google calendars.
  - Plan must be regenerable when user schedule changes.

- R4: RangeMaster Drills Library
  - Curated drill catalog with:
    - setup instructions
    - reps/sets
    - scoring method
    - common mistakes
  - Drill selection is dynamic:
    - driven by miss store and Shot Surgeon findings
    - prioritizes drills that address the highest-impact failure

- R5: Coaching Voice and Persona
  - Coach Mode uses a more “instructor” stance but remains in Bones identity:
    - still direct and tactical
    - more technical detail allowed than Live Caddy
  - Must separate:
    - “What happened” (stats)
    - “Likely cause” (mechanics hypothesis)
    - “Next action” (drill plan)

## 5. Acceptance criteria
- A1: One-Fix output with confidence gating
  - GIVEN: User uploads a down-the-line swing video meeting minimum quality thresholds
  - WHEN: Analysis completes
  - THEN: App identifies the top fault with confidence and outputs exactly one primary fix plus one drill; if confidence is below threshold, it requests a better video/angle.

- A2: Statistical autopsy correctness and prioritization
  - GIVEN: User has ≥ 5 rounds logged with club and penalty outcomes
  - WHEN: User runs Shot Surgeon for last 10 rounds
  - THEN: App surfaces problem clubs and the top 3 scoring contributors, and selects one focus area that matches the largest strokes-lost driver.

- A3: Calendar export
  - GIVEN: User selects 3 practice days and 45 minutes per session
  - WHEN: User generates a plan and exports .ics
  - THEN: The .ics imports successfully into Apple Calendar and Google Calendar with correct titles, dates, and durations.

- A4: Drill personalization
  - GIVEN: User’s last rounds show repeated OB right with driver/3-wood
  - WHEN: Plan is generated
  - THEN: RangeMaster prioritizes alignment/path drills and includes measurable targets tied to reducing right-miss penalties.

## 6. Constraints & invariants
- C1:
  - Android: Material 3
  - iOS: Human Interface Guidelines
- C2: Data sufficiency and honesty
  - System must not claim causality without sufficient evidence.
  - Use confidence scoring and “needs more data” states.
- C3: Privacy
  - Videos stored securely; user can delete videos and derived analysis.
- C4: Export format compatibility
  - .ics must validate against standard calendar importers.

## 7. Open questions
- Q1: What benchmark set defines “scratch” baselines (strokes gained model source)?
- Q2: What minimum video quality thresholds are enforced?
- Q3: Do we support multi-angle fusion in v1 or single-angle only?
- Q4: How are drills versioned and localized?
