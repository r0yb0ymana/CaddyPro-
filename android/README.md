# Android App

Native Android application built with Kotlin and Jetpack Compose.

## Tech Stack

- **Language:** Kotlin 2.1
- **UI:** Jetpack Compose with Material 3
- **Architecture:** MVVM with Clean Architecture
- **DI:** Hilt
- **Navigation:** Navigation Compose
- **Network:** Retrofit + OkHttp
- **Database:** Room
- **Async:** Kotlin Coroutines + Flow
- **Image Loading:** Coil

## Project Structure

```
app/src/main/java/com/example/app/
├── di/                 # Dependency injection modules
├── data/
│   ├── local/         # Room database, DAOs
│   ├── remote/        # Retrofit services, DTOs
│   └── repository/    # Repository implementations
├── domain/
│   ├── model/         # Domain models
│   └── usecase/       # Business logic use cases
└── ui/
    ├── theme/         # Material theme configuration
    ├── components/    # Reusable composables
    ├── screens/       # Feature screens
    └── navigation/    # Navigation setup
```

## Build Variants

### Environments (Flavors)
- **dev** - Development environment
- **staging** - Staging/QA environment
- **prod** - Production environment

### Build Types
- **debug** - Debug build with logging
- **release** - Minified release build

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.2.1) or later
- JDK 17
- Android SDK 35

### Setup

1. Clone the repository
2. Open the `android/` directory in Android Studio
3. Sync Gradle files
4. Run the app on an emulator or device

### Build Commands

```bash
# Build debug APK
./gradlew assembleDevDebug

# Build release AAB
./gradlew bundleProdRelease

# Run unit tests
./gradlew testDevDebugUnitTest

# Run instrumented tests
./gradlew connectedDevDebugAndroidTest

# Run lint checks
./gradlew lintDevDebug

# Generate test coverage report
./gradlew jacocoTestReport
```

## Configuration

### API Configuration

Set the API base URL in `app/build.gradle.kts`:

```kotlin
buildTypes {
    debug {
        buildConfigField("String", "API_BASE_URL", "\"https://api.dev.example.com/\"")
    }
    release {
        buildConfigField("String", "API_BASE_URL", "\"https://api.example.com/\"")
    }
}
```

### Signing Configuration

For release builds, configure signing via environment variables or `local.properties`:

```properties
# local.properties (do not commit!)
RELEASE_STORE_FILE=path/to/keystore.jks
RELEASE_STORE_PASSWORD=yourpassword
RELEASE_KEY_ALIAS=youralias
RELEASE_KEY_PASSWORD=yourkeypassword
```

## Testing

### Unit Tests

Located in `app/src/test/`:
- Test ViewModels with MockK
- Test Use Cases in isolation
- Test Repository with fake data sources

### Instrumented Tests

Located in `app/src/androidTest/`:
- Compose UI tests
- Integration tests with Hilt
- Database tests with Room

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# All tests
./gradlew test connectedAndroidTest
```

## Code Quality

### Lint

```bash
./gradlew lint
```

### Ktlint (if configured)

```bash
./gradlew ktlintCheck
./gradlew ktlintFormat
```

## Release Process

1. Update version in `app/build.gradle.kts`
2. Create a git tag: `git tag v1.0.0`
3. Push tag: `git push origin v1.0.0`
4. GitHub Actions will build and deploy to Google Play

## Resources

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material 3](https://m3.material.io/)
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [Hilt](https://dagger.dev/hilt/)
