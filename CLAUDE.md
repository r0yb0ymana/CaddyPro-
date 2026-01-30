# Claude-in-the-Loop Mobile Development Guide

This repository is a template for **spec-driven, multi-agent development** of native Android and iOS applications using Claude Code.

## Core Philosophy

**Specs are the source of truth. Claude executes the loop. Humans stay in control.**

- No implementation without a spec
- No "looks good" without acceptance criteria
- Specs evolve when reality disagrees

## Repository Structure

```
claude-in-the-loop/
├── CLAUDE.md              # This file - repository context for Claude
├── README.md              # Project overview
├── .gitignore            # Mobile + common ignores
├── .mcp.json.template    # MCP server configuration
│
├── android/              # Native Android app (Kotlin + Compose)
│   ├── app/
│   │   ├── src/main/java/com/example/app/
│   │   │   ├── di/       # Hilt dependency injection
│   │   │   ├── data/     # Data layer (Room, Retrofit)
│   │   │   ├── domain/   # Business logic
│   │   │   └── ui/       # Composables and ViewModels
│   │   └── build.gradle.kts
│   ├── gradle/
│   └── build.gradle.kts
│
├── ios/                  # Native iOS app (Swift + SwiftUI)
│   └── App/
│       ├── Sources/
│       │   ├── App/      # App entry point
│       │   ├── Core/     # DI, networking, data
│       │   └── UI/       # Views and ViewModels
│       ├── Tests/
│       └── Package.swift
│
├── .github/workflows/    # CI/CD pipelines
│   ├── android-ci.yml    # Android PR checks
│   ├── android-beta.yml  # Firebase App Distribution
│   ├── android-release.yml # Google Play release
│   ├── ios-ci.yml        # iOS PR checks
│   ├── ios-beta.yml      # TestFlight beta
│   └── ios-release.yml   # App Store release
│
├── specs/                # Feature specifications
│   ├── feature-template.md        # Generic feature template
│   ├── mobile-screen-template.md  # Screen/view template
│   ├── mobile-component-template.md # Reusable component template
│   ├── mobile-flow-template.md    # Multi-screen flow template
│   ├── example-login-screen.md    # Mobile example
│   └── example-offline-sync.md    # Mobile example
│
├── .claude/
│   ├── agents/           # Role-specific agent definitions
│   │   ├── engineer.md         # Generic implementation
│   │   ├── android-engineer.md # Android specialist
│   │   ├── ios-engineer.md     # iOS specialist
│   │   ├── tester.md           # Generic testing
│   │   ├── mobile-tester.md    # Mobile testing
│   │   ├── reviewer.md         # Generic review
│   │   └── mobile-reviewer.md  # Mobile review
│   │
│   └── commands/         # Custom slash commands
│       ├── spec.md       # Refine specifications
│       ├── plan.md       # Generate task lists
│       ├── implement.md  # Execute loop
│       └── review.md     # Code review
│
├── prompts/              # Workflow phase prompts
├── scripts/              # Automation helpers
└── docs/                 # Documentation
```

## Mobile Development Patterns

### Android (Kotlin + Jetpack Compose)

#### Architecture
- **MVVM** with Clean Architecture layers
- **Hilt** for dependency injection
- **Navigation Compose** for navigation
- **Room** for local database
- **Retrofit** for networking
- **Coroutines + Flow** for async

#### File Structure
```
ui/screens/feature/
├── FeatureScreen.kt      # Composable entry point
├── FeatureViewModel.kt   # State management
├── FeatureState.kt       # UI state data class
└── FeatureAction.kt      # User actions sealed class
```

#### Code Patterns
```kotlin
// ViewModel with Hilt
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val useCase: FeatureUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(FeatureState())
    val uiState: StateFlow<FeatureState> = _uiState.asStateFlow()
}

// Composable Screen
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // UI implementation
}
```

#### Testing
```kotlin
// Unit test with MockK and Turbine
@Test
fun `when action then state updates`() = runTest {
    val viewModel = FeatureViewModel(mockUseCase)
    viewModel.uiState.test {
        // assertions
    }
}
```

### iOS (Swift + SwiftUI)

#### Architecture
- **MVVM** with Clean Architecture layers
- **Manual DI** or Environment injection
- **NavigationStack** for navigation
- **SwiftData** or Core Data for persistence
- **URLSession** with async/await for networking
- **Combine** where needed

#### File Structure
```
UI/Screens/Feature/
├── FeatureView.swift      # SwiftUI view
├── FeatureViewModel.swift # @Observable view model
└── FeatureModels.swift    # State and action types
```

#### Code Patterns
```swift
// ViewModel with @Observable (iOS 17+)
@Observable
@MainActor
final class FeatureViewModel {
    private(set) var state = FeatureState()
    private let useCase: FeatureUseCase
    
    func handle(_ action: FeatureAction) {
        Task { /* handle action */ }
    }
}

// SwiftUI View
struct FeatureView: View {
    @State private var viewModel = FeatureViewModel()
    
    var body: some View {
        // UI implementation
    }
}
```

#### Testing
```swift
// Unit test
func testActionUpdatesState() async {
    let viewModel = FeatureViewModel(useCase: mockUseCase)
    await viewModel.handle(.someAction)
    XCTAssertEqual(viewModel.state.someValue, expected)
}
```

## Common Workflows

### Starting a New Feature

```bash
# 1. Create a spec from template
./scripts/new-spec.sh feature-name

# 2. Refine the spec
claude /spec specs/feature-name.md

# 3. Generate plan
claude /plan specs/feature-name.md

# 4. Implement (choose platform agent)
claude /implement specs/feature-name-plan.md
```

### Mobile-Specific Commands

When implementing mobile features:
```
# For Android implementation
"Use the android-engineer agent to implement Task 1"

# For iOS implementation
"Use the ios-engineer agent to implement Task 1"

# For testing
"Use the mobile-tester agent to verify acceptance criteria"

# For review
"Use the mobile-reviewer agent to review the implementation"
```

## Custom Slash Commands

- `/spec <file>` - Refine a feature specification
- `/plan <file>` - Generate implementation task list
- `/implement <file>` - Execute the implementation loop
- `/review <spec> [files]` - Spec-based code review

## Development Conventions

### Spec-Driven Development Rules

1. **Always start with a spec** - Use mobile templates for UI features
2. **Platform parity** - Specify requirements for both platforms
3. **One task at a time** - Complete cycle before moving forward
4. **Verify against spec** - Tests must validate acceptance criteria
5. **Stop on ambiguity** - Clarify specs, don't invent requirements

### Git Workflow

- **Branch naming**: `feature/spec-name` or `fix/issue-description`
- **Commit messages**: Reference the spec and task number
- **PR requirements**: Link to spec and verification results

### Testing Standards

- Every acceptance criterion must have a test
- Tests should fail before implementation (TDD)
- Test on both platforms when spec requires parity
- Include accessibility tests

### Code Style

#### Android
- Follow Kotlin coding conventions
- Use ktlint for formatting
- Compose functions are PascalCase
- State classes are immutable data classes

#### iOS
- Follow Swift API Design Guidelines
- Use SwiftLint/SwiftFormat
- Use @MainActor for UI-related code
- Prefer value types (structs) for state

## Build Commands

### Android
```bash
# Debug build
cd android && ./gradlew assembleDevDebug

# Run tests
./gradlew testDevDebugUnitTest

# Lint check
./gradlew lintDevDebug
```

### iOS
```bash
# Build
cd ios/App && swift build

# Run tests
swift test

# Build for simulator
xcodebuild -scheme App -destination 'platform=iOS Simulator,name=iPhone 15'
```

## Environment Configuration

### Android Flavors
- `dev` - Development environment
- `staging` - QA/staging environment  
- `prod` - Production environment

### iOS Configurations
- `Debug` - Development with logging
- `Release` - Production optimized

### API Base URLs
Configured per environment in build files:
- Android: `app/build.gradle.kts` buildConfigField
- iOS: Compile-time #if DEBUG checks

## Multi-Agent Pattern

For mobile development, use specialized agents:

1. **android-engineer** / **ios-engineer** - Platform-specific implementation
2. **mobile-tester** - Cross-platform test validation
3. **mobile-reviewer** - Platform compliance review

Each agent has specific permissions and domain knowledge.

## Common Commands

```bash
# Android
cd android
./gradlew assembleDevDebug      # Build debug APK
./gradlew testDevDebugUnitTest  # Run unit tests
./gradlew connectedAndroidTest  # Run instrumented tests
./gradlew lintDevDebug          # Run lint

# iOS
cd ios/App
swift build                     # Build package
swift test                      # Run tests
xcodebuild test -scheme App ... # Run UI tests
```

## Context Management Tips

- Use `/clear` between features to reset context
- Reference specs explicitly: "Read specs/login-screen.md"
- Use screenshots for UI verification
- Use platform-specific agents for implementation

## Troubleshooting

- **Claude ignoring platform patterns?** → Specify which agent to use
- **Commands not working?** → Verify `.claude/commands/` exists
- **Build failures?** → Check Android SDK / Xcode versions
- **Tests failing on CI?** → Verify CI has same SDK versions

## Resources

- [Claude Code Best Practices](https://www.anthropic.com/engineering/claude-code-best-practices)
- [Android Developers](https://developer.android.com/)
- [Apple Developer](https://developer.apple.com/)
- [Material 3](https://m3.material.io/)
- [Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/)

---

**Remember**: Specs define success. Claude implements. Tests verify. Humans approve.
