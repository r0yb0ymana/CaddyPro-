# GitHub Workflows & Fastlane Guide

This repository uses **Fastlane** triggered by GitHub Actions for automated building and deployment.

## Overview

- **Version Management**: `release-please` automatically manages the CHANGELOG and version numbers.
- **Android Release**: `.github/workflows/android-release.yml` -> Runs `fastlane deploy`
- **iOS Release**: `.github/workflows/ios-release.yml` -> Runs `fastlane release`

Both workflows trigger on tags matching `v*` (e.g., `v1.0.0`).

## Release Process (Release Please)

1. **Develop**: Merge feature PRs into `main`. This triggers Beta builds.
2. **Accumulate**: `release-please` automatically maintains a "Release PR" that accumulates changes and bumps versions in `build.gradle.kts` and `project.yml`.
3. **Release**: When you are ready to cut a production release, **merge the Release PR**.
4. **Deploy**: Merging the PR creates a GitHub Release and a git tag (e.g. `v1.1.0`), which triggers the Android and iOS release workflows.

## Prerequisites

### Android Requirements
- Google Play Console account with app created
- Release keystore file
- Google Cloud service account with Google Play Developer API access

### iOS Requirements
- Apple Developer account
- App Store Connect app created
- Distribution certificate (.p12)
- Distribution provisioning profile
- App Store Connect API key

## Secrets Setup

Add these secrets to your GitHub repository:

### Android Secrets
| Secret Name | Description |
|------------|-------------|
| `ANDROID_KEYSTORE_BASE64` | Base64 encoded keystore file |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | Key alias |
| `ANDROID_KEY_PASSWORD` | Key password |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Content of service account JSON file |
| `ANDROID_PACKAGE_NAME` | Your app package name (e.g. com.example.app) |

### iOS Secrets
| Secret Name | Description |
|------------|-------------|
| `IOS_CERTIFICATES_P12` | Base64 encoded .p12 certificate |
| `IOS_CERTIFICATES_PASSWORD` | Password for the .p12 file |
| `IOS_PROVISIONING_PROFILE_BASE64` | Base64 encoded .mobileprovision file |
| `APP_STORE_CONNECT_API_KEY_ID` | API Key ID from App Store Connect |
| `APP_STORE_CONNECT_API_ISSUER_ID` | Issuer ID from App Store Connect |
| `APP_STORE_CONNECT_API_KEY_BASE64` | Base64 encoded .p8 API key file |
| `IOS_BUNDLE_ID` | Your app bundle ID |
| `IOS_APPLE_ID` | Your Apple ID email |
| `IOS_ITC_TEAM_ID` | App Store Connect Team ID |
| `IOS_TEAM_ID` | Apple Developer Team ID |

## Fastlane Configuration

### Android (`android/fastlane/Fastfile`)
- `lane :test` - Runs unit tests
- `lane :beta` - Deploys to Firebase App Distribution
- `lane :deploy` - Deploys to Google Play (default: **internal** track)
- `lane :promote` - Promotes a build (e.g., from internal to production)

### iOS (`ios/fastlane/Fastfile`)
- `lane :test` - Runs unit tests
- `lane :beta` - Deploys to TestFlight
- `lane :release` - Deploys to App Store

## How to Release

1. Review the automated "Release PR" created by `release-please`.
2. Merge the PR.
3. GitHub Actions will:
   - Create a GitHub Release.
   - Tag the commit (e.g., `v1.1.0`).
   - Trigger `android-release.yml` (Internal Track).
   - Trigger `ios-release.yml` (TestFlight External).

## Local Usage

You can also run Fastlane locally:

```bash
# Android
cd android
fastlane deploy

# iOS
cd ios
fastlane release
```
