# Feature: technical-specifications

## 1. Problem statement
The product requires:
- A mobile-first web experience that works on course, in poor reception.
- Voice-first interaction and geolocation support.
- Multiple AI workloads with different latency and quality requirements.
- A consistent UI style optimized for outdoor readability.

We need a concrete technical baseline to support implementation and testing.

## 2. Goals
- G1: Ship a PWA that supports offline-first behavior for core flows.
- G2: Provide a consistent, high-contrast mobile UI aligned to glassmorphic styling.
- G3: Use model tiering to optimize cost/latency:
  - fast routing and dialogue vs deep analysis vs image/video analysis.
- G4: Provide browser capability integration (geolocation, speech).
- G5: Establish performance, reliability, and security baselines.

## 3. Non-goals
- NG1: Native-only implementation (this spec targets web/PWA first).
- NG2: Full device sensor fusion beyond standard web APIs.
- NG3: Supporting outdated browsers that lack PWA/Speech APIs.

## 4. Functional requirements
- R1: Frontend stack
  - React 19.
  - Tailwind CSS with mobile-first breakpoints.
  - Glassmorphic UI style with explicit contrast requirements for outdoor use.

- R2: PWA and Offline-first
  - Service Worker for:
    - caching app shell
    - caching last-known course/hole data
    - caching drill library metadata
  - Offline queue for:
    - shot logs
    - profile edits (if allowed) or defer with clear UX
  - Conflict resolution strategy for queued writes (last-write-wins with audit for critical fields).

- R3: AI model usage policy
  - gemini-3-flash-preview:
    - intent routing
    - live caddy dialogue
  - gemini-3-pro-preview:
    - deep statistical analysis (Shot Surgeon)
    - multi-round summaries
  - gemini-3-pro-image-preview:
    - swing video/frame analysis
  - Must include:
    - timeouts
    - retries with backoff
    - circuit-breaker behavior
    - cost telemetry per call type

- R4: Browser capabilities
  - Geolocation API:
    - used for on-course positioning and weather fetch
    - permission prompts handled with rationale screens
  - Web Speech API:
    - voice input and optional readout
    - fall back to text entry if unsupported

- R5: API and security baseline
  - Auth: (placeholder) must support secure token storage and refresh.
  - Encrypt sensitive data at rest server-side.
  - Rate limit AI endpoints and protect against prompt injection:
    - strict schema outputs
    - whitelist routing targets
    - sanitize user-provided content before using in system prompts

- R6: Observability
  - Client metrics:
    - page load, time-to-interactive
    - offline events
  - AI metrics:
    - latency, failure rate, token usage/cost proxies
  - Logging must avoid storing raw audio and minimize PII.

## 5. Acceptance criteria
- A1: PWA offline round logging
  - GIVEN: Device loses connectivity on course
  - WHEN: User logs shots
  - THEN: Logs are stored locally and sync when connectivity returns without data loss.

- A2: Model tiering behavior
  - GIVEN: User runs a post-round autopsy over 10 rounds
  - WHEN: Analysis is executed
  - THEN: The system uses gemini-3-pro-preview (not flash) and returns within the configured timeout or fails gracefully with retry options.

- A3: Capability fallback
  - GIVEN: Web Speech API is unsupported
  - WHEN: User attempts voice input
  - THEN: UI falls back to text input and still supports intent routing.

## 6. Constraints & invariants
- C1:
  - Android: Material 3
  - iOS: Human Interface Guidelines
- C2: Performance
  - LCP targets and memory constraints appropriate for mid-range phones.
- C3: Reliability
  - Offline-first must not corrupt round state.
- C4: Security
  - All AI routing targets must be validated server-side (no client-trusted routing).

## 7. Open questions
- Q1: Backend stack and hosting target (Edge vs serverful)?
- Q2: Auth provider and SSO requirements?
- Q3: Course data provider and licensing constraints?
- Q4: Which browsers are officially supported for Speech API features?
