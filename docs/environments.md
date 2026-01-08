# Environment Configuration Guide

This document describes how to configure different environments (development, staging, production) for Android and iOS applications.

## Overview

Both platforms support multiple build configurations for different environments:

| Environment | Purpose | API Server | Debug Features |
|-------------|---------|------------|----------------|
| Development | Local development | `api.dev.example.com` | Full logging, debug tools |
| Staging | QA/Testing | `api.staging.example.com` | Logging, test tools |
| Production | Live users | `api.example.com` | Minimal logging, no debug |

## Android Configuration

### Build Variants

Android uses a combination of **build types** and **product flavors**:

```
Build Variants = Product Flavors × Build Types

dev + debug     = devDebug
dev + release   = devRelease
staging + debug = stagingDebug
staging + release = stagingRelease
prod + debug    = prodDebug
prod + release  = prodRelease
```

### app/build.gradle.kts

```kotlin
android {
    // Build types (debug/release configurations)
    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    
    // Product flavors (environment configurations)
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            
            buildConfigField("String", "API_BASE_URL", "\"https://api.dev.example.com/\"")
            buildConfigField("String", "ENVIRONMENT", "\"development\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
        
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            
            buildConfigField("String", "API_BASE_URL", "\"https://api.staging.example.com/\"")
            buildConfigField("String", "ENVIRONMENT", "\"staging\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        }
        
        create("prod") {
            dimension = "environment"
            // No suffix for production
            
            buildConfigField("String", "API_BASE_URL", "\"https://api.example.com/\"")
            buildConfigField("String", "ENVIRONMENT", "\"production\"")
            buildConfigField("Boolean", "ENABLE_LOGGING", "false")
        }
    }
    
    buildFeatures {
        buildConfig = true
    }
}
```

### Using Build Config

```kotlin
// In code
val baseUrl = BuildConfig.API_BASE_URL
val environment = BuildConfig.ENVIRONMENT
val isLoggingEnabled = BuildConfig.ENABLE_LOGGING

// Configure logging
if (BuildConfig.ENABLE_LOGGING) {
    Timber.plant(Timber.DebugTree())
}

// Configure API client
val okHttpClient = OkHttpClient.Builder()
    .apply {
        if (BuildConfig.DEBUG || BuildConfig.ENABLE_LOGGING) {
            addInterceptor(HttpLoggingInterceptor())
        }
    }
    .build()
```

### Environment-Specific Resources

Create resource directories for each flavor:

```
app/src/
├── main/           # Shared resources
├── dev/
│   └── res/
│       └── values/
│           └── strings.xml  # Dev-specific strings
├── staging/
│   └── res/
│       └── values/
│           └── strings.xml  # Staging-specific strings
└── prod/
    └── res/
        └── values/
            └── strings.xml  # Prod-specific strings
```

### Google Services (Firebase)

Place environment-specific `google-services.json`:

```
app/src/
├── dev/google-services.json
├── staging/google-services.json
└── prod/google-services.json
```

### Build Commands

```bash
# Development
./gradlew assembleDevDebug

# Staging (for QA)
./gradlew assembleStagingRelease

# Production
./gradlew assembleProdRelease
./gradlew bundleProdRelease  # For Play Store (AAB)
```

## iOS Configuration

### Build Configurations

iOS uses Xcode configurations combined with schemes:

1. **Configurations**: Debug, Release (and custom like Staging)
2. **Schemes**: Define which configuration to use

### xcconfig Files

Create configuration files:

```
ios/App/
├── Configurations/
│   ├── Base.xcconfig
│   ├── Debug.xcconfig
│   ├── Staging.xcconfig
│   └── Release.xcconfig
```

**Base.xcconfig:**
```
// Shared settings
PRODUCT_NAME = MyApp
SWIFT_VERSION = 5.9
IPHONEOS_DEPLOYMENT_TARGET = 17.0
```

**Debug.xcconfig:**
```
#include "Base.xcconfig"

API_BASE_URL = https:/$()/api.dev.example.com
ENVIRONMENT = development
ENABLE_LOGGING = YES

PRODUCT_BUNDLE_IDENTIFIER = com.example.app.dev
ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon-Dev
SWIFT_ACTIVE_COMPILATION_CONDITIONS = DEBUG DEV
```

**Staging.xcconfig:**
```
#include "Base.xcconfig"

API_BASE_URL = https:/$()/api.staging.example.com
ENVIRONMENT = staging
ENABLE_LOGGING = YES

PRODUCT_BUNDLE_IDENTIFIER = com.example.app.staging
ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon-Staging
SWIFT_ACTIVE_COMPILATION_CONDITIONS = STAGING
```

**Release.xcconfig:**
```
#include "Base.xcconfig"

API_BASE_URL = https:/$()/api.example.com
ENVIRONMENT = production
ENABLE_LOGGING = NO

PRODUCT_BUNDLE_IDENTIFIER = com.example.app
ASSETCATALOG_COMPILER_APPICON_NAME = AppIcon
SWIFT_ACTIVE_COMPILATION_CONDITIONS = RELEASE
```

### Info.plist Configuration

Add custom keys to Info.plist:

```xml
<key>API_BASE_URL</key>
<string>$(API_BASE_URL)</string>
<key>ENVIRONMENT</key>
<string>$(ENVIRONMENT)</string>
<key>ENABLE_LOGGING</key>
<string>$(ENABLE_LOGGING)</string>
```

### Swift Environment Access

```swift
enum Environment {
    static let apiBaseURL: URL = {
        guard let urlString = Bundle.main.object(forInfoDictionaryKey: "API_BASE_URL") as? String,
              let url = URL(string: urlString) else {
            fatalError("API_BASE_URL not configured")
        }
        return url
    }()
    
    static let name: String = {
        Bundle.main.object(forInfoDictionaryKey: "ENVIRONMENT") as? String ?? "unknown"
    }()
    
    static let isLoggingEnabled: Bool = {
        Bundle.main.object(forInfoDictionaryKey: "ENABLE_LOGGING") as? String == "YES"
    }()
    
    static var isDevelopment: Bool {
        #if DEV
        return true
        #else
        return false
        #endif
    }
    
    static var isProduction: Bool {
        #if RELEASE
        return true
        #else
        return false
        #endif
    }
}
```

### Compile-Time Configuration

For simpler cases, use compiler flags:

```swift
struct Config {
    static let apiBaseURL: URL = {
        #if DEV
        return URL(string: "https://api.dev.example.com")!
        #elseif STAGING
        return URL(string: "https://api.staging.example.com")!
        #else
        return URL(string: "https://api.example.com")!
        #endif
    }()
    
    static let isLoggingEnabled: Bool = {
        #if DEBUG || STAGING
        return true
        #else
        return false
        #endif
    }()
}
```

### Schemes

Create schemes for each environment:
1. `App-Dev` → Debug configuration
2. `App-Staging` → Staging configuration
3. `App-Release` → Release configuration

### Build Commands

```bash
# Development
xcodebuild -scheme "App-Dev" -configuration Debug

# Staging
xcodebuild -scheme "App-Staging" -configuration Staging

# Production
xcodebuild -scheme "App-Release" -configuration Release
```

## Secrets Management

### Never Commit Secrets

Add to `.gitignore`:
```
# Secrets
.env
.env.*
**/google-services.json
**/GoogleService-Info.plist
**/secrets.properties
```

### Android Secrets

Use `local.properties` or environment variables:

```properties
# local.properties (not committed)
API_KEY_DEV=your_dev_key
API_KEY_STAGING=your_staging_key
API_KEY_PROD=your_prod_key
```

```kotlin
// build.gradle.kts
val localProperties = Properties()
localProperties.load(project.rootProject.file("local.properties").inputStream())

productFlavors {
    create("dev") {
        buildConfigField("String", "API_KEY", "\"${localProperties["API_KEY_DEV"]}\"")
    }
}
```

### iOS Secrets

Use xcconfig with gitignored secrets file:

```
// Secrets.xcconfig (not committed)
API_KEY = your_api_key_here

// Debug.xcconfig
#include "Secrets.xcconfig"
```

Or use environment variables in CI:

```bash
# In CI script
echo "API_KEY = $API_KEY_SECRET" > ios/App/Configurations/Secrets.xcconfig
```

## Feature Flags

### Implementation

```kotlin
// Android
object FeatureFlags {
    val newOnboarding: Boolean
        get() = when (BuildConfig.ENVIRONMENT) {
            "development" -> true
            "staging" -> true
            "production" -> RemoteConfig.getBoolean("new_onboarding")
            else -> false
        }
}
```

```swift
// iOS
enum FeatureFlags {
    static var newOnboarding: Bool {
        switch Environment.name {
        case "development", "staging":
            return true
        case "production":
            return RemoteConfig.shared.bool(forKey: "new_onboarding")
        default:
            return false
        }
    }
}
```

## CI/CD Environment Configuration

### GitHub Actions Secrets

Configure secrets per environment:

```yaml
# .github/workflows/android-release.yml
jobs:
  release:
    environment: production  # Use production secrets
    steps:
      - name: Build
        env:
          API_KEY: ${{ secrets.API_KEY_PROD }}
```

### Environment-Specific Workflows

```yaml
# For staging
on:
  push:
    branches: [develop]

jobs:
  deploy-staging:
    environment: staging
    # ...

# For production
on:
  push:
    tags: ['v*']

jobs:
  deploy-production:
    environment: production
    # ...
```

## Testing Different Environments

### Android

```kotlin
@Test
fun testDevEnvironment() {
    // Use dev build variant
    assertEquals("development", BuildConfig.ENVIRONMENT)
    assertTrue(BuildConfig.ENABLE_LOGGING)
}
```

### iOS

```swift
func testDevEnvironment() {
    #if DEV
    XCTAssertEqual(Environment.name, "development")
    XCTAssertTrue(Environment.isLoggingEnabled)
    #endif
}
```

## Checklist

### Before Release

- [ ] Verify correct API endpoints per environment
- [ ] Check logging is disabled in production
- [ ] Ensure debug tools are not included in production
- [ ] Verify app icons differentiate environments
- [ ] Test all environments in CI

### Per Environment

- [ ] API base URL configured
- [ ] Analytics configured (different properties)
- [ ] Crash reporting configured
- [ ] Push notifications configured
- [ ] Deep links configured
- [ ] App signing configured

## Resources

- [Android Build Variants](https://developer.android.com/studio/build/build-variants)
- [Xcode Build Configurations](https://developer.apple.com/documentation/xcode/customizing-the-build-phases-of-a-target)
- [GitHub Environments](https://docs.github.com/en/actions/deployment/targeting-different-environments/using-environments-for-deployment)
