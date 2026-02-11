# CaddyPro

A real-time on-course golf assistant for savvy golfers. CaddyPro provides live strategy recommendations that account for weather, fatigue, course hazards, and personal shot tendencies.

## Project Status

**Current Phase:** R1 MVP Development
**Active Task:** Player Profile + Bag Management

## Tech Stack

### Android App
- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **DI:** Hilt
- **Local DB:** Room (offline-first)
- **Async:** Coroutines + Flow
- **Navigation:** Navigation Compose

### Backend
- **Database:** Supabase (PostgreSQL + PostGIS)
- **Auth:** Supabase Auth (Google Sign-In + Email/Password)
- **Storage:** Supabase Storage

## Project Structure

```
caddypro/
├── CLAUDE.md                 # Development guide and conventions
├── README.md                 # This file
├── specs/                    # Feature specifications
│   └── r1-player-profile-bag.md
├── android/                  # Native Android app
│   ├── app/
│   │   ├── src/main/java/com/caddypro/app/
│   │   │   ├── di/           # Hilt dependency injection
│   │   │   ├── data/         # Data layer (Room + Supabase)
│   │   │   ├── domain/       # Domain models and use cases
│   │   │   └── ui/           # UI layer (Compose screens)
│   │   └── build.gradle.kts
│   └── build.gradle.kts
└── .gitignore
```

## Setup

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Gradle 8.2+

### Configuration

1. Clone the repository
2. Open the `android/` directory in Android Studio
3. Create a `secrets.properties` file in the `android/app/` directory:

```properties
SUPABASE_URL=your-supabase-url
SUPABASE_ANON_KEY=your-supabase-anon-key
OPENWEATHER_API_KEY=your-openweather-api-key
MAPBOX_ACCESS_TOKEN=your-mapbox-token
```

4. Update `android/app/build.gradle.kts` to read from `secrets.properties`
5. Sync Gradle and build the project

### Building

```bash
cd android

# Debug build
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Run lint
./gradlew lintDebug
```

## Development Workflow

This project follows a **spec-driven, multi-agent development** approach using Claude Code:

1. All features start with a spec in `specs/`
2. One task at a time, following the task breakdown in each spec
3. Tests validate acceptance criteria
4. Human approval before moving to the next task

See [CLAUDE.md](CLAUDE.md) for detailed development conventions.

## Task 1 Completion Status ✓

### Completed Items
- [x] Android project structure created
- [x] Gradle configuration with all dependencies
- [x] CaddyPro dark theme implemented (colors, typography)
- [x] Room database with PlayerProfile, Bag, Club entities
- [x] Supabase client configuration
- [x] Navigation graph shell with route definitions
- [x] Hilt dependency injection setup

### Next Steps
Proceed to Task 2: Profile Setup Screen (see `specs/r1-player-profile-bag.md`)

## License

Proprietary - All rights reserved

## Target Market

- **Region:** Australia (Melbourne courses for QA)
- **Audience:** Experienced golfers looking for real-time strategic guidance
- **Competitive Edge:** Strategy layer on top of shot tracking, not another scorekeeper
