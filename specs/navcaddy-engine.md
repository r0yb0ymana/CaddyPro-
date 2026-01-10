Feature: navcaddy-engine

1. Problem statement

Traditional app navigation forces golfers to:

Hunt through menus during time-sensitive moments (on-course, between shots, post-round).

Translate real-world intent into UI steps (“I’m missing right” → which screen?).

Lose context across modules (swing tendencies, fatigue, recovery, equipment, weather).

We need an intent-driven Conversational OS that:

Accepts unstructured voice/text input.

Infers user intent reliably.

Routes the user to the correct module and state.

Responds in a consistent “Bones” Tour-caddy persona.

Uses contextual memory (historical misses, patterns) to tailor advice.

2. Goals

G1: Replace menu-first navigation with intent-first routing for all primary user journeys.

G2: Parse voice/text into a structured intent + entities + confidence score.

G3: Route users to the correct module and deep-link state with minimal friction.

G4: Maintain a consistent “Bones” persona across modules and sessions.

G5: Persist and apply contextual memory of user tendencies (e.g., pressure slice) to adjust advice.

G6: Provide deterministic fallbacks when intent confidence is low (clarify, confirm, or show suggestions).

3. Non-goals

NG1: Building full swing detection from sensors (beyond using available app inputs).

NG2: Replacing all UI with chat (UI remains for browsing, editing, dashboards).

NG3: Real-time coaching claims requiring regulated/medical validation (recovery insights remain informational).

NG4: Fully offline conversational intelligence (offline mode uses limited local intents only).

4. Functional requirements

R1: Unified Conversational Entry

Single entry point for voice/text available globally (persistent mic/chat button).

Supports interrupt-and-resume (user can ask follow-ups without losing state).

R2: Intent Processing Pipeline

Input normalization: language detect, profanity filtering, golf slang handling.

Intent classification and entity extraction using Gemini 3 Flash.

Output schema (minimum):

intent_id

confidence (0–1)

entities (club, yardage, lie, wind, fatigue, pain, score context, etc.)

user_goal (optional)

routing_target (module + screen + parameters)

response_style (Bones persona tone settings)

Must support ambiguous utterances and request clarification.

R3: Routing Orchestrator

Maps intent_id → module route + required parameters.

Validates prerequisites (e.g., “How’s my recovery?” requires at least one recovery datapoint or user prompt to input).

Executes navigation as a deep link:

Module name

Screen name

State payload (prefilled filters, selected club, recommended drill, etc.)

Supports “no-navigation” intents (pure answer, quick tip, confirmation question).

R4: Bones Persona Layer

Consistent voice and style rules:

Tactical, concise, context-aware.

Avoids generic LLM filler.

Uses golf-caddy language with restraint (no cringe roleplay).

Persona must remain stable across modules and not drift into other roles.

Persona guardrails:

No unsafe medical directives.

No absolute guarantees (“this will fix your slice”).

Clear uncertainty signaling when data is missing.

R5: Contextual Memory: Miss Pattern Store

Track “misses” and situational tags:

direction (push/pull, slice/hook)

pressure context (leading, trailing, tournament mode)

club

lie/wind conditions (if known)

timestamps and frequency

Update rules:

Append new events

Aggregate into patterns (rolling windows)

Decay old patterns (avoid permanent anchoring)

Retrieval rules:

When intent relates to shot strategy or swing tendency, fetch relevant patterns and feed into response/routing.

R6: Session Context

Short-term context: current round, hole, last shot, last recommendation.

Must support follow-ups like:

“What about if the wind picks up?”

“Switch to 6-iron then.”

R7: Multi-Modal Input Handling

Voice-to-text transcription (platform-native or chosen provider).

Text input with quick suggestions (“Common intents” chips).

Error handling: transcription fails, LLM timeout, low confidence.

R8: Observability and Debugging

Log events (privacy-safe):

raw input (redacted as needed)

inferred intent

confidence

route chosen

clarification prompts

latency breakdown (ASR, LLM, routing)

Developer “Intent Trace” view for QA builds.

R9: Safety and Compliance

PII handling: minimize collection, encrypt at rest, secure transport.

Clear user controls:

delete memory

export data (optional)

disable personalization

Recovery-related responses must include informational disclaimers in UX copy (not chatty).

5. Acceptance criteria

A1: Correct routing on high-confidence intent

GIVEN: User is on any screen

WHEN: User says “My 7-iron feels long today”

THEN: System identifies club_adjustment (or equivalent) intent with confidence ≥ threshold and routes to the club/yardage adjustment workflow with club=7i preselected.

A2: Correct module selection for wellness intent

GIVEN: User has recovery data available

WHEN: User asks “How’s my recovery looking?”

THEN: System routes to Recovery module overview and returns a Bones-style summary grounded in available data.

A3: Clarification on low-confidence intent

GIVEN: User input is ambiguous (“It feels off today”)

WHEN: Intent confidence < threshold

THEN: System asks 1–2 targeted clarification questions and presents 3 suggested intents as selectable chips.

A4: Memory influences advice

GIVEN: User history indicates “slice under pressure” pattern with recent occurrences

WHEN: User says “Big tee shot, what’s the play?”

THEN: System references the pattern and recommends a conservative line/club/shot shape that reduces slice risk, with a brief rationale.

A5: Persona consistency across modules

GIVEN: User moves between Caddy, Coach, Recovery modules

WHEN: User asks comparable questions in different contexts

THEN: Response tone remains consistent with Bones persona rules and avoids generic assistant phrasing.

A6: Failure-mode resilience

GIVEN: LLM request times out

WHEN: User submits an intent

THEN: System falls back to local intent suggestions and does not crash or dead-end the user.

6. Constraints & invariants

C1: Platform UX guidelines

Android: Material 3

iOS: Human Interface Guidelines

C2: Latency budgets (target)

Voice end-to-response: ≤ 2.5s P50, ≤ 4.5s P95

Text end-to-response: ≤ 1.5s P50, ≤ 3.0s P95

C3: Deterministic routing

Routing decisions must be reproducible from the same (input + context + memory snapshot) in QA builds.

C4: Privacy and security

Encrypt memory store at rest.

No storing raw voice audio by default.

Provide user-facing controls to wipe memory.

C5: Memory correctness

Memory must be attributable (store event sources) to avoid hallucinated “history”.

Decay/aging rules prevent stale patterns dominating advice.

C6: Offline behavior

If offline: disable LLM calls and provide a limited local-intent menu + last-known context only.

7. Open questions (RESOLVED)

Q1: What are the initial supported intents and their priority order (MVP list)?
**RESOLVED**: Full 15+ intents for MVP:
- Core: club_adjustment, recovery_check, shot_recommendation, score_entry, pattern_query
- Extended: drill_request, weather_check, stats_lookup, round_start, round_end
- Additional: equipment_info, course_info, settings_change, help_request, feedback

Q2: What confidence thresholds trigger: route, confirm, or clarify?
**RESOLVED**: Balanced thresholds:
- Route: >= 0.75
- Confirm: 0.50 - 0.74
- Clarify: < 0.50

Q3: Where does "miss" data originate (manual tagging, shot tracking, inferred from scoring, integrations)?
**RESOLVED**: Manual only for MVP - user manually tags misses via UI.

Q4: How is "pressure" defined and detected (user toggle, scoring context, tournament mode)?
**RESOLVED**: Both combined - user toggle + automatic inference from scoring context.

Q5: What is the memory retention policy (time window, decay half-life, user-configurable)?
**RESOLVED**: 90-day retention window with 14-day decay half-life.

Q6: What are the exact persona rules (forbidden phrases, max verbosity, tone calibration)?
**RESOLVED**: Friendly Expert persona - warm but professional, slightly longer responses OK, fewer forbidden phrases. Avoid medical claims and guarantees.

Q7: Do you require audit logs for LLM prompts/responses for support, and how do you redact them safely?
**RESOLVED**: No logging - maximum privacy approach, no LLM audit logs stored.

If you paste the next spec input, I’ll translate it into the same structure and keep the interface contracts consistent (intent schema, routing rules, memory rules).
