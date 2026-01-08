# claude-in-the-loop

> Spec-driven native mobile development — powered by Claude Code.

`claude-in-the-loop` is a production-ready template for building **native Android and iOS applications** using:
- **Spec-driven development (SDD)** - Requirements as the single source of truth
- **Multi-agent collaboration** - Platform-specific engineers, testers, and reviewers
- **Iterative execution loops** - Plan → implement → verify cycles with continuous validation

**Specs are the source of truth. Claude executes the loop. Humans stay in control.**

Built following [Claude Code best practices](https://www.anthropic.com/engineering/claude-code-best-practices) for optimal performance and team collaboration.

## Why Native?

This template focuses on **native development** (Kotlin/Compose for Android, Swift/SwiftUI for iOS) because:

- ✅ Full platform capabilities without abstractions
- ✅ Best performance and user experience
- ✅ Independent release cycles per platform
- ✅ Platform-specific optimizations
- ✅ No cross-platform framework dependencies

## Core Principles

- ✅ No implementation without a spec
- ✅ No "looks good" without acceptance criteria
- ✅ Specs evolve when reality disagrees
- ✅ Test-driven development by default
- ✅ Small, verifiable commits
- ✅ Platform-appropriate patterns

## Quick Start

### 1. Fork & Setup
```bash
# Clone this repository
git clone https://github.com/yourusername/claude-in-the-loop
cd claude-in-the-loop

# Run the interactive setup script
# This configures your project name, package IDs, and environment
./setup.sh
```

### 2. Create Your First Spec

```bash
./scripts/new-spec.sh my-feature
```

### 3. Start Claude Code

```bash
claude
```

### 4. Run the Workflow

```bash
# 1. Scaffold the feature files
./scripts/scaffold-feature.sh MyFeature

# 2. Refine specification
claude
> /spec specs/my-feature.md

# 3. Generate implementation plan
> /plan specs/my-feature.md

# 4. Execute with verification
> /implement specs/my-feature-plan.md
```

**New to this workflow?** Start with the [Getting Started Guide](docs/getting-started.md).

## Repository Structure

```
claude-in-the-loop/
├── CLAUDE.md                 # Repository context (auto-loaded by Claude)
├── README.md                 # This file
│
├── android/                  # Native Android app
│   ├── app/
│   │   └── src/main/java/com/example/app/
│   │       ├── di/          # Hilt dependency injection
│   │       ├── data/        # Repositories, Room, Retrofit
│   │       ├── domain/      # Use cases, models
│   │       └── ui/          # Compose screens, ViewModels
│   ├── gradle/
│   └── build.gradle.kts
│
├── ios/                      # Native iOS app
│   └── App/
│       ├── Sources/
│       │   ├── App/         # Entry point
│       │   ├── Core/        # DI, networking, data
│       │   └── UI/          # SwiftUI views, ViewModels
│       └── Package.swift
│
├── .github/workflows/        # CI/CD pipelines
│   ├── android-ci.yml       # Android PR checks
│   ├── android-beta.yml     # Firebase App Distribution
│   ├── android-release.yml  # Google Play release
│   ├── ios-ci.yml           # iOS PR checks
│   ├── ios-beta.yml         # TestFlight beta
│   └── ios-release.yml      # App Store release
│
├── specs/                    # Feature specifications
│   ├── feature-template.md          # Generic template
│   ├── mobile-screen-template.md    # Screen spec template
│   ├── mobile-component-template.md # Component template
│   ├── mobile-flow-template.md      # Multi-screen flow template
│   ├── example-login-screen.md      # Mobile example
│   └── example-offline-sync.md      # Mobile example
│
├── .claude/
│   ├── agents/               # Role-specific agents
│   │   ├── android-engineer.md  # Android implementation
│   │   ├── ios-engineer.md      # iOS implementation
│   │   ├── mobile-tester.md     # Mobile testing
│   │   └── mobile-reviewer.md   # Mobile review
│   │
│   └── commands/             # Custom slash commands
│       ├── spec.md           # /spec - Refine specifications
│       ├── plan.md           # /plan - Generate task lists
│       ├── implement.md      # /implement - Execute loop
│       └── review.md         # /review - Code review
│
├── scripts/                  # Automation helpers
└── docs/                     # Documentation
    ├── getting-started.md
    ├── mobile-architecture.md
    ├── design-system.md
    ├── accessibility.md
    ├── environments.md
    └── github-workflows.md
```

## Tech Stack

### Android
- **Language:** Kotlin 2.1
- **UI:** Jetpack Compose with Material 3
- **Architecture:** MVVM + Clean Architecture
- **DI:** Hilt
- **Async:** Coroutines + Flow
- **Network:** Retrofit + OkHttp
- **Database:** Room

### iOS
- **Language:** Swift 5.9+
- **UI:** SwiftUI (iOS 17+)
- **Architecture:** MVVM + Clean Architecture
- **Async:** Swift Concurrency
- **Network:** URLSession + async/await
- **Database:** SwiftData

## The Workflow

### 1. Spec Phase

Create a clear, testable specification:

```bash
./scripts/new-spec.sh user-auth

claude
> /spec specs/user-auth.md
```

Claude will help you define:
- Problem statement
- Platform-specific requirements
- Acceptance criteria (testable!)
- Constraints and invariants

### 2. Plan Phase

Generate an implementation plan:

```bash
> /plan specs/user-auth.md
```

Claude will:
- Explore your codebase
- Create tasks for both platforms
- Map tasks to acceptance criteria
- Generate `specs/user-auth-plan.md`

### 3. Implement Phase

Execute with platform-specific agents:

```bash
> /implement specs/user-auth-plan.md
```

For each task:
- **Android/iOS Engineer** → Implements per platform guidelines
- **Mobile Tester** → Validates acceptance criteria
- **Mobile Reviewer** → Checks platform compliance

### 4. Ship

```bash
# Run tests
cd android && ./gradlew test
cd ios/App && swift test

# Create release
git tag v1.0.0
git push origin v1.0.0
# CI/CD handles the rest!
```

## Key Features

### Multi-Agent Verification

Specialized agents for quality:

- **android-engineer** - Kotlin/Compose implementation
- **ios-engineer** - Swift/SwiftUI implementation
- **mobile-tester** - Platform test validation
- **mobile-reviewer** - Platform compliance checks

### Mobile-Specific Templates

Templates designed for mobile:

- **Screen Template** - Single screen specs with platform sections
- **Component Template** - Reusable UI component specs
- **Flow Template** - Multi-screen user flows

### Complete CI/CD

Six workflows included:

| Workflow | Trigger | Purpose |
|----------|---------|---------|
| `android-ci.yml` | PR | Tests, lint, build |
| `android-beta.yml` | `main` push | Firebase App Distribution |
| `android-release.yml` | v* tags | Google Play Store |
| `ios-ci.yml` | PR | Tests, lint, build |
| `ios-beta.yml` | `main` push | TestFlight |
| `ios-release.yml` | v* tags | App Store |

## Documentation

- **[Getting Started](docs/getting-started.md)** - First feature walkthrough
- **[Mobile Architecture](docs/mobile-architecture.md)** - MVVM patterns
- **[Design System](docs/design-system.md)** - UI components
- **[Accessibility](docs/accessibility.md)** - A11y requirements
- **[Environments](docs/environments.md)** - Dev/staging/prod setup
- **[GitHub Workflows](docs/github-workflows.md)** - CI/CD configuration

## Examples

See `specs/` for complete examples:

- **[example-login-screen.md](specs/example-login-screen.md)** - Full mobile screen spec with validation, accessibility, platform sections
- **[example-offline-sync.md](specs/example-offline-sync.md)** - Complex feature with data sync patterns

## Customization

### 1. Update Package Names

```kotlin
// android/app/build.gradle.kts
namespace = "com.yourcompany.yourapp"
applicationId = "com.yourcompany.yourapp"
```

```swift
// ios/App - Update bundle identifier in Xcode
```

### 2. Configure API Endpoints

```kotlin
// android/app/build.gradle.kts
buildConfigField("String", "API_BASE_URL", "\"https://your-api.com/\"")
```

```swift
// ios/App/Sources/Core/Network/APIClient.swift
private var baseURL: URL { URL(string: "https://your-api.com")! }
```

### 3. Add Your Design System

Update theme files:
- `android/app/src/main/java/com/example/app/ui/theme/`
- `ios/App/Sources/UI/Theme/`

### 4. Configure CI/CD Secrets

See [GitHub Workflows Guide](docs/github-workflows.md) for required secrets.

## Best Practices

### Write Mobile-Specific Acceptance Criteria

**Bad:**
```markdown
- The login screen should work
```

**Good:**
```markdown
- A3: Email validation
  - GIVEN: Email field contains "notanemail"
  - WHEN: User leaves field or submits
  - THEN: Error message "Please enter a valid email" appears
  - AND: Field shows error styling (red border)
  - AND: (iOS) VoiceOver announces "Error: Please enter a valid email"
  - AND: (Android) TalkBack announces error state
```

### Include Platform Sections

```markdown
### R1: Login Button

#### Android
- Material 3 FilledButton
- Minimum height 48dp
- Full width with 16dp horizontal margin

#### iOS
- SwiftUI Button with .borderedProminent style
- Minimum height 44pt
- Full width with 16pt horizontal padding
```

### Use Platform-Specific Agents

```
> Use the android-engineer agent to implement Task 1 for Android
> Use the ios-engineer agent to implement Task 1 for iOS
```

## Requirements

- **Android Studio** Ladybug (2024.2.1) or later
- **Xcode** 15.0 or later
- **Claude Code** installed
- **Git** for version control

## Contributing

This is a template repository. Fork and customize!

Improvements welcome:
- Additional mobile specs
- Platform-specific patterns
- CI/CD enhancements
- Documentation improvements

## Resources

- [Claude Code Best Practices](https://www.anthropic.com/engineering/claude-code-best-practices)
- [Android Developers](https://developer.android.com/)
- [Apple Developer](https://developer.apple.com/)
- [Material 3](https://m3.material.io/)
- [Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/)

## License

MIT - Fork, customize, and build great mobile apps!

---

**Ready to build?** → [docs/getting-started.md](docs/getting-started.md)
