# Development Workflow

This repository follows **Trunk-Based Development**.

## Core Principles

1. **Main is Source of Truth**: The `main` branch is always deployable.
2. **Short-Lived Branches**: Feature branches should be merged into `main` quickly (e.g., daily).
3. **Automated Testing**: CI must pass before merging.
4. **Automated Deployment**: Merging to `main` triggers beta deployment.

## The Loop

### 1. Create Feature Branch
Start from `main` and create a short-lived feature branch.

```bash
git checkout main
git pull
git checkout -b feature/my-feature
```

### 2. Implement & Verify
Follow the spec-driven implementation loop:

```bash
./scripts/scaffold-feature.sh MyFeature
claude /spec specs/my-feature.md
claude /plan specs/my-feature.md
claude /implement specs/my-feature-plan.md
```

### 3. Open Pull Request
When verification passes locally:

```bash
git push origin feature/my-feature
```

Open a PR to `main`. CI will run:
- Lint checks
- Unit tests
- Build verification

### 4. Merge to Main
Once approved, squash and merge into `main`.

**Triggers:**
- `android-beta.yml`: Deploys to Firebase App Distribution
- `ios-beta.yml`: Deploys to TestFlight

### 5. Release
When ready to ship to production, create a tag on `main`.

```bash
git tag v1.0.0
git push origin v1.0.0
```

**Triggers:**
- `android-release.yml`: Deploys to Google Play (Internal Track)
- `ios-release.yml`: Deploys to App Store Connect (External Beta)

### 6. Promote (Android Only)
To move from Internal -> Production on Android:

Go to GitHub Actions -> `Android Promote to Production` -> Run workflow.
