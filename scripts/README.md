# Automation Scripts

Helper scripts for the claude-in-the-loop mobile development workflow.

## Available Scripts

### `new-spec.sh`
Create a new feature spec from template.

```bash
./scripts/new-spec.sh <feature-name>
```

**Example:**
```bash
./scripts/new-spec.sh user-authentication
# Creates specs/user-authentication.md from template
```

**What it does:**
- Copies `specs/feature-template.md` to `specs/<feature-name>.md`
- Replaces the `<name>` placeholder with your feature name
- Provides next steps for spec refinement

---

### `validate-spec.sh`
Validate that a generic spec is complete and ready for planning.

```bash
./scripts/validate-spec.sh <spec-file>
```

**Example:**
```bash
./scripts/validate-spec.sh specs/user-authentication.md
```

**What it checks:**
- All required sections are present
- No unreplaced placeholders remain
- Acceptance criteria are defined
- Functional requirements exist (warning if missing)

**Exit codes:**
- `0` - Validation passed
- `1` - Validation failed

---

### `validate-mobile-spec.sh`
Validate that a mobile-specific spec is complete with platform requirements.

```bash
./scripts/validate-mobile-spec.sh <spec-file>
```

**Example:**
```bash
./scripts/validate-mobile-spec.sh specs/login-screen.md
```

**What it checks (in addition to basic validation):**
- Android-specific content present
- iOS-specific content present
- Accessibility requirements mentioned
- Given-When-Then format used
- Data model section present
- API contracts documented (if API used)
- Touch target requirements

**Exit codes:**
- `0` - Validation passed (may include warnings)
- `1` - Validation failed

---

## Making Scripts Executable

```bash
chmod +x scripts/*.sh
```

## Integration with Workflow

These scripts complement the Claude Code slash commands:

```
./scripts/new-spec.sh my-feature
                ↓
        claude /spec specs/my-feature.md
                ↓
  ./scripts/validate-mobile-spec.sh specs/my-feature.md
                ↓
        claude /plan specs/my-feature.md
                ↓
claude /implement specs/my-feature-plan.md
```

## Mobile-Specific Workflow

For mobile features, use the mobile validation script:

```bash
# Create spec
./scripts/new-spec.sh login-screen

# Edit using mobile template sections
# Add Android and iOS specific requirements

# Validate with mobile-specific checks
./scripts/validate-mobile-spec.sh specs/login-screen.md

# If validation passes, continue with planning
claude /plan specs/login-screen.md
```

## Adding New Scripts

When adding automation scripts:

1. Use bash for portability
2. Include usage help (`-h` or no arguments)
3. Validate inputs and provide clear error messages
4. Exit with appropriate codes (0 = success, 1+ = error)
5. Document in this README
6. Make executable: `chmod +x scripts/your-script.sh`

## Build Scripts

For convenience, you can run platform builds:

```bash
# Android
cd android
./gradlew assembleDevDebug        # Debug APK
./gradlew testDevDebugUnitTest    # Unit tests
./gradlew lintDevDebug            # Lint check

# iOS
cd ios/App
swift build                       # Build package
swift test                        # Run tests
```

## Headless Automation

For CI/CD integration, you can use Claude Code in headless mode:

```bash
# Generate plan from spec
claude -p "Generate implementation plan from specs/my-feature.md" \
  --output-format stream-json > plan-output.json

# Run tests for acceptance criteria
claude -p "Run tests for acceptance criteria in specs/my-feature.md" \
  --output-format stream-json > test-results.json
```

See [CLAUDE.md](../CLAUDE.md) for more on headless patterns.
