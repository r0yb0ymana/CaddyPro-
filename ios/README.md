# iOS App

Native iOS application built with Swift and SwiftUI.

## Tech Stack

- **Language:** Swift 5.9+
- **UI:** SwiftUI with iOS 17+
- **Architecture:** MVVM with Clean Architecture
- **Async:** Swift Concurrency (async/await)
- **Data:** Combine for reactive streams
- **Networking:** URLSession with async/await
- **Storage:** UserDefaults, Keychain, SwiftData

## Project Structure

```
ios/App/
├── Sources/
│   ├── App/               # App entry point, state
│   ├── Features/          # Feature modules
│   ├── Core/
│   │   ├── DI/           # Dependency injection
│   │   ├── Data/         # Data layer
│   │   ├── Domain/       # Domain models, use cases
│   │   └── Network/      # API client
│   └── UI/
│       ├── Theme/        # App theme, colors, styles
│       ├── Components/   # Reusable views
│       ├── Screens/      # Feature screens
│       └── Navigation/   # Navigation setup
├── Tests/
│   └── AppTests/         # Unit tests
├── UITests/              # UI tests
└── Package.swift         # Swift Package manifest
```

## Getting Started

### Prerequisites

- Xcode 15.0 or later
- iOS 17.0+ deployment target
- macOS Sonoma 14.0+ (for development)

### Setup

1. Clone the repository
2. Open `ios/App/` in Xcode (via Package.swift or create .xcodeproj)
3. Select your development team for signing
4. Build and run on simulator or device

### Creating Xcode Project

If you prefer a traditional Xcode project:

```bash
cd ios/App
# Create project from Package.swift
xcodegen generate  # If using XcodeGen

# Or create manually in Xcode:
# File > New > Project > iOS > App
# Then add existing files from Sources/
```

### Build Commands

```bash
# Build
xcodebuild -scheme App -destination 'platform=iOS Simulator,name=iPhone 15'

# Run tests
xcodebuild test -scheme App -destination 'platform=iOS Simulator,name=iPhone 15'

# Build for release
xcodebuild archive -scheme App -archivePath build/App.xcarchive
```

## Configuration

### Environment Configuration

API endpoints are configured via build settings:

```swift
#if DEBUG
let baseURL = "https://api.dev.example.com"
#else
let baseURL = "https://api.example.com"
#endif
```

For multiple environments, use Xcode configurations:
- Debug
- Staging
- Release

### Code Signing

Configure signing in Xcode:
1. Select the project in navigator
2. Select target > Signing & Capabilities
3. Select your team and bundle identifier

For CI/CD, use environment variables and provisioning profiles.

## Testing

### Unit Tests

Located in `Tests/AppTests/`:
- Test ViewModels
- Test Use Cases
- Test Data transformations

### UI Tests

Located in `UITests/`:
- Test user flows
- Test accessibility
- Snapshot tests (with swift-snapshot-testing)

### Running Tests

```bash
# Unit tests
swift test

# Or via Xcode
Cmd+U

# Specific test
swift test --filter ExampleTests
```

## Code Quality

### SwiftLint

Add SwiftLint to your project:

```yaml
# .swiftlint.yml
disabled_rules:
  - trailing_whitespace
  
opt_in_rules:
  - empty_count
  - force_unwrapping
  
line_length: 120
```

Run:
```bash
swiftlint
swiftlint --fix
```

### SwiftFormat

```bash
swiftformat . --config .swiftformat
```

## Architecture

### MVVM Pattern

```
View (SwiftUI) 
    ↓ observes
ViewModel (@Observable / @MainActor)
    ↓ calls
UseCase (Business Logic)
    ↓ calls
Repository (Data Access)
    ↓ calls
DataSource (API / Database)
```

### Example Feature Structure

```
Features/Login/
├── LoginView.swift         # SwiftUI View
├── LoginViewModel.swift    # @Observable view model
├── LoginUseCase.swift      # Business logic
└── LoginModels.swift       # Feature-specific models
```

## Release Process

1. Update version in Xcode project settings
2. Create a git tag: `git tag v1.0.0`
3. Push tag: `git push origin v1.0.0`
4. GitHub Actions will build and deploy to App Store Connect

## Resources

- [SwiftUI Documentation](https://developer.apple.com/documentation/swiftui)
- [Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines)
- [Swift Concurrency](https://docs.swift.org/swift-book/LanguageGuide/Concurrency.html)
- [App Store Guidelines](https://developer.apple.com/app-store/review/guidelines/)
