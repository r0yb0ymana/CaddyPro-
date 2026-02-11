# CaddyPro - Claude-in-the-Loop Development Guide

This repository uses **spec-driven, multi-agent development** for the CaddyPro native Android application using Claude Code.

## Core Philosophy

**Specs are the source of truth. Claude executes the loop. Humans stay in control.**

- No implementation without a spec
- No "looks good" without acceptance criteria
- Specs evolve when reality disagrees

## Product Context

CaddyPro is a real-time on-course golf assistant for savvy golfers. It provides live strategy recommendations that account for weather, fatigue, course hazards, and personal shot tendencies.

**Target Market:** Experienced golfers (Australia-only at launch, Melbourne courses for QA)
**Competitive Position:** Strategy layer on top of shot tracking. Not another scorekeeper.
**Key Differentiator:** Real-time carry adjustments + hazard-aware recommendations personalized to the player's miss bias and bag profile.

## MVP Scope (R1)

### In Scope
| Feature | Description |
|---------|-------------|
| Player Profile + Bag | Manual club entry with distances and miss bias per club |
| Shot Logger | Offline-first, minimal-tap shot tracking during rounds |
| Forecaster HUD | Real-time weather + carry distance adjustments per club |
| Basic Hole Map | Satellite imagery + OSM hazard overlays, green-center target |

### Explicitly Out of R1
- Voice/NavCaddy (R2+)
- Wearable/BodyCaddy (R2+)
- Coach Mode (R2+)
- Course Editor / Tier 2 data (R2)
- iOS (post-MVP)
- Dynamic pin positions

## Tech Stack

### Android App
| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DI | Hilt |
| Local DB | Room (offline-first) |
| Async | Coroutines + Flow |
| Maps | Mapbox SDK (dark theme styling control) |
| Navigation | Navigation Compose |

### Backend
| Service | Technology |
|---------|-----------|
| Database | Supabase (PostgreSQL + PostGIS) |
| Auth | Supabase Auth (Google Sign-In + Email/Password) |
| Storage | Supabase Storage |
| Geospatial | PostGIS for course data queries |

### External APIs
| API | Purpose |
|-----|---------|
| OpenWeatherMap | Wind, temp, humidity for Forecaster HUD |
| Overpass API | OSM golf course data (fairways, greens, bunkers, water) |
| Mapbox | Satellite tile layers + map rendering |

### Course Data Strategy
- **Tier 1 (MVP):** OSM data via Overpass API + Mapbox satellite tiles
- **Tier 2 (R2):** User-contributed course editor on satellite imagery
- **Tier 3 (Future):** CV-based auto-detection from satellite imagery
- Green-center as default target (no pin positions in MVP)
- "Flag incorrect data" button seeds Tier 2 contributions
- Graceful degradation: courses without OSM data show satellite-only view

## Repository Structure

```
caddypro/
├── CLAUDE.md                    # This file
├── README.md                    # Project overview
├── .gitignore                   # Android + common ignores
│
├── android/                     # Native Android app
│   ├── app/
│   │   ├── src/main/java/com/caddypro/app/
│   │   │   ├── di/              # Hilt modules
│   │   │   ├── data/
│   │   │   │   ├── local/       # Room DAOs, entities
│   │   │   │   ├── remote/      # Supabase client, API services
│   │   │   │   └── repository/  # Repository implementations
│   │   │   ├── domain/
│   │   │   │   ├── model/       # Domain models
│   │   │   │   ├── usecase/     # Business logic
│   │   │   │   └── repository/  # Repository interfaces
│   │   │   └── ui/
│   │   │       ├── theme/       # CaddyPro dark theme
│   │   │       ├── components/  # Shared composables
│   │   │       ├── profile/     # Player Profile + Bag
│   │   │       ├── shotlogger/  # Shot Logger
│   │   │       ├── forecaster/  # Forecaster HUD
│   │   │       └── holemap/     # Hole Map
│   │   ├── src/test/            # Unit tests
│   │   ├── src/androidTest/     # Instrumented tests
│   │   └── build.gradle.kts
│   ├── gradle/
│   └── build.gradle.kts
│
├── specs/                       # Feature specifications
│   ├── templates/
│   │   ├── feature-template.md
│   │   ├── screen-template.md
│   │   └── component-template.md
│   ├── r1-player-profile-bag.md
│   ├── r1-shot-logger.md
│   ├── r1-forecaster-hud.md
│   └── r1-hole-map.md
│
├── .claude/
│   ├── agents/
│   │   ├── android-engineer.md
│   │   ├── mobile-tester.md
│   │   └── mobile-reviewer.md
│   └── commands/
│       ├── spec.md
│       ├── plan.md
│       ├── implement.md
│       └── review.md
│
├── .github/workflows/
│   ├── android-ci.yml
│   └── android-beta.yml
│
├── scripts/
└── docs/
    └── architecture.md
```

## Architecture Patterns

### MVVM + Clean Architecture

```
UI Layer (Compose) → ViewModel → UseCase → Repository → DataSource
                                                ├── Room (local)
                                                └── Supabase (remote)
```

### File Structure Per Feature
```
ui/feature/
├── FeatureScreen.kt          # Composable entry point
├── FeatureViewModel.kt       # State management via Hilt
├── FeatureState.kt           # Immutable UI state data class
└── FeatureAction.kt          # User actions sealed class
```

### Code Patterns
```kotlin
// ViewModel
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val useCase: FeatureUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(FeatureState())
    val uiState: StateFlow<FeatureState> = _uiState.asStateFlow()
}

// Screen
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
}
```

### Offline-First Pattern
```
User Action → Room (write local) → UI updates immediately
           → WorkManager (queue sync) → Supabase (when online)
           ← Conflict resolution: last-write-wins with timestamp
```

## Domain-Specific Rules

### Shot Data Model
```
Shot {
    id: UUID
    roundId: UUID
    holeNumber: Int (1-18)
    shotNumber: Int
    clubUsed: ClubId
    startLocation: LatLng
    endLocation: LatLng?
    shotType: Enum (TEE, FAIRWAY, APPROACH, CHIP, PUTT, PENALTY)
    timestamp: Instant
    synced: Boolean
}
```

### Bag Profile Model
```
Club {
    id: UUID
    name: String (e.g., "7 Iron")
    type: Enum (DRIVER, WOOD, HYBRID, IRON, WEDGE, PUTTER)
    loft: Float?
    carryDistance: Int (yards)
    totalDistance: Int (yards)
    missBias: Enum (STRAIGHT, SLICE, HOOK, PUSH, PULL, NONE)
    isActive: Boolean
}
```

### Carry Distance Adjustment Formula
```
adjustedCarry = baseCarry
    * temperatureFactor(tempC)    // ~1yd per 5C from 20C baseline
    * altitudeFactor(altM)        // ~2% per 300m above sea level
    * windFactor(windSpeed, windDir, shotDir)  // headwind/tailwind
    * humidityFactor(humidity)    // minimal effect, ~1yd
```

### Course Data Schema (PostGIS)
```sql
courses (id, name, location GEOGRAPHY, osm_id, par, holes_count)
holes (id, course_id, number, par, handicap_index, green_center GEOGRAPHY)
hazards (id, hole_id, type ENUM, geometry GEOGRAPHY, name)
-- types: BUNKER, WATER, OB, TREES, ROUGH
```

## Brand Standards

### Color Palette
| Token | Hex | Usage |
|-------|-----|-------|
| Performance Lime | #19A849 | Primary actions only (sparingly) |
| Matte Charcoal | #1A1A1A | Backgrounds |
| Carbon Grey | #4A4A4A | Borders, secondary elements |
| Titanium White | #F5F5F5 | Primary text, icons |
| Surface Elevated | #242424 | Cards, sheets |
| Surface Dim | #141414 | Dimmed backgrounds |
| Border Subtle | #3A3A3A | Subtle borders |
| Danger Zone | #CC3333 | Hazard overlays, errors |
| Warning | #FFA726 | Caution states |
| Info | #5C9CE6 | Informational states |

### Typography
- **UI Text:** Inter (all headings, body, labels)
- **Data Displays:** JetBrains Mono (distances, scores, wind, temperature)
- Display Large: 57sp Black | Headline Medium: 28sp SemiBold
- Body Large: 16sp Normal | Label Large: 14sp Medium
- Data Large: 48sp (distance to pin) | Data Medium: 32sp (wind)

### Interaction Rules
- Touch targets: minimum 48dp
- Haptic feedback on: shot saved, undo, errors
- Animations: minimal (outdoor distraction reduction)
- Minimize typing: defaults and quick toggles

### Accessibility
- Dynamic text sizing support
- High contrast ratios (outdoor readability)
- Material 3 compliance

## Development Conventions

### Solo Dev Workflow
- **Task size:** 2-4 hour blocks maximum
- **One feature at a time:** Complete spec cycle before moving on
- **Branch per spec task:** `feature/r1-profile-bag-task-1`
- **Commit messages:** Reference spec + task number

### Spec-Driven Rules
1. Always start with a spec
2. One task at a time, complete cycle before next
3. Verify against acceptance criteria
4. Stop on ambiguity, clarify specs first
5. Tests must validate acceptance criteria

### Testing Standards
- Every acceptance criterion has a test
- Unit tests with MockK + Turbine
- Offline scenarios tested explicitly
- Compose UI tests for critical flows

### Build Commands
```bash
cd android

# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run lint
./gradlew lintDebug

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Feature Build Order

| Order | Feature | Dependency |
|-------|---------|-----------|
| 1 | Player Profile + Bag | None (foundation) |
| 2 | Forecaster HUD | Bag (needs club distances) |
| 3 | Shot Logger | Bag (needs club selection) |
| 4 | Basic Hole Map | Course data pipeline |

Profile + Bag ships first because every other feature depends on club data.

## Context Management

- Use `/clear` between features to reset context
- Reference specs explicitly: "Read specs/r1-player-profile-bag.md"
- Use platform-specific agents for implementation
- Keep tasks atomic for solo dev workflow

---

**Remember**: Specs define success. Claude implements. Tests verify. Human approves.
