# Quick Start: Live Caddy Mode Testing

This guide helps you quickly run all tests for Live Caddy Mode acceptance criteria verification.

---

## Prerequisites

- Android Studio installed
- Android SDK configured
- Gradle configured (automatically handled by Android Studio)
- Physical device or emulator for E2E tests (optional)

---

## 1. Run Unit Tests (< 1 minute)

**What it tests**: Domain logic, calculations, business rules

```bash
cd c:\dev\CaddyPro--main\android
.\gradlew testDevDebugUnitTest
```

**Expected Output**:
```
BUILD SUCCESSFUL in 42s
75 tests completed, 0 failed, 0 skipped
```

**Coverage**:
- Weather calculations (A1)
- Readiness adjustments (A2)
- Hazard filtering (A3)
- Shot persistence (A4)

---

## 2. Run Integration Tests (< 30 seconds)

**What it tests**: ViewModel flows, state management, component integration

```bash
.\gradlew testDevDebugUnitTest --tests "*IntegrationTest"
```

**Expected Output**:
```
BUILD SUCCESSFUL in 18s
20 tests completed, 0 failed, 0 skipped
```

**Coverage**:
- Complete round flow
- Shot logging with latency tracking
- Offline state transitions
- Error recovery

---

## 3. Run E2E Tests (< 5 minutes)

**What it tests**: Complete user flows on device

**Prerequisites**:
- Connect Android device via USB (or start emulator)
- Enable USB debugging on device
- Grant location permissions

```bash
.\gradlew connectedDevDebugAndroidTest --tests "LiveCaddyE2ETest"
```

**Expected Output**:
```
BUILD SUCCESSFUL in 4m 32s
15 tests completed, 0 failed, 0 skipped
```

**Coverage**:
- Weather HUD render time (A1)
- Readiness strategy adjustment (A2)
- Hazard-aware landing zones (A3)
- Shot logger speed and persistence (A4)

**Note**: E2E tests require:
- Test course data in database
- Valid weather API key (or mock enabled)
- Location permissions granted

---

## 4. Run Specific Acceptance Criterion Tests

### Test A1: Weather HUD

```bash
.\gradlew connectedDevDebugAndroidTest --tests "*acceptanceCriteria_A1*"
```

Tests:
- `acceptanceCriteria_A1_weatherHudRendersWithin2Seconds`
- `acceptanceCriteria_A1_conditionsAdjustmentIsCalculated`

### Test A2: Readiness Strategy

```bash
.\gradlew connectedDevDebugAndroidTest --tests "*acceptanceCriteria_A2*"
```

Tests:
- `acceptanceCriteria_A2_lowReadinessIncreasesConservatism`
- `acceptanceCriteria_A2_readinessExplanationIsShown`

### Test A3: Hazard-Aware Zones

```bash
.\gradlew connectedDevDebugAndroidTest --tests "*acceptanceCriteria_A3*"
```

Tests:
- `acceptanceCriteria_A3_dangerZonesAreHighlighted`
- `acceptanceCriteria_A3_landingZoneAvoidsSlice`
- `acceptanceCriteria_A3_landingZoneAvoidsHook`
- `acceptanceCriteria_A3_riskCalloutsAreLimited`

### Test A4: Shot Logger

```bash
.\gradlew connectedDevDebugAndroidTest --tests "*acceptanceCriteria_A4*"
```

Tests:
- `acceptanceCriteria_A4_shotLoggingCompletesUnder1Second`
- `acceptanceCriteria_A4_offlineShotsAreQueued`
- `acceptanceCriteria_A4_shotsAutoSyncWhenOnline`
- `acceptanceCriteria_A4_shotsPersistAfterAppRestart`
- `acceptanceCriteria_A4_hapticFeedbackOnShotLogged`

---

## 5. Run Performance Benchmarks

```bash
.\gradlew connectedDevDebugAndroidTest --tests "*benchmark*"
```

Tests:
- `benchmark_shotLoggingAverageLatency` (10 iterations)

**Expected Results**:
- Average latency: < 2000ms
- P95 latency: < 2500ms

---

## 6. View Test Results

### Console Output
Test results are printed to console in real-time.

### HTML Report
After running tests, view detailed HTML report:

```bash
# Unit tests
start android\app\build\reports\tests\testDevDebugUnitTest\index.html

# E2E tests
start android\app\build\reports\androidTests\connected\index.html
```

### Coverage Report (Optional)
Generate code coverage report:

```bash
.\gradlew testDevDebugUnitTestCoverage
start android\app\build\reports\coverage\html\index.html
```

---

## 7. Manual Testing

For tests that require manual verification, follow:
`c:\dev\CaddyPro--main\docs\testing\live-caddy-e2e-test-plan.md`

**Manual tests include**:
- Actual haptic feedback sensation
- Outdoor GPS accuracy
- Real weather API integration
- Performance on various devices

---

## Troubleshooting

### Issue: Gradle fails to start
**Solution**: Ensure Android Studio is installed and ANDROID_HOME is set

### Issue: E2E tests fail with "No devices found"
**Solution**: Connect device via USB or start emulator

### Issue: E2E tests fail with "Location permission denied"
**Solution**: Manually grant location permission in device settings

### Issue: Weather API tests fail
**Solution**: Ensure valid API key in `local.properties`:
```
OPEN_WEATHER_API_KEY=your_key_here
```

### Issue: Tests are slow
**Solution**:
- Use physical device instead of emulator
- Increase Gradle heap size in `gradle.properties`:
  ```
  org.gradle.jvmargs=-Xmx4096m
  ```

---

## Expected Execution Times

| Test Suite | Duration | Tests |
|-----------|----------|-------|
| Unit Tests | 30-45s | 75+ |
| Integration Tests | 15-20s | 20+ |
| E2E Tests | 3-5 min | 15 |
| **Total** | **5-7 min** | **110+** |

---

## Quick Verification Checklist

Run these commands to verify all tests pass:

```bash
cd c:\dev\CaddyPro--main\android

# 1. Unit tests
.\gradlew testDevDebugUnitTest
# Expected: BUILD SUCCESSFUL, 75+ tests passed

# 2. Integration tests
.\gradlew testDevDebugUnitTest --tests "*IntegrationTest"
# Expected: BUILD SUCCESSFUL, 20+ tests passed

# 3. E2E tests (requires device)
.\gradlew connectedDevDebugAndroidTest --tests "LiveCaddyE2ETest"
# Expected: BUILD SUCCESSFUL, 15 tests passed
```

✅ If all three commands succeed, all acceptance criteria are verified!

---

## CI/CD Integration

Add to `.github/workflows/android-ci.yml`:

```yaml
- name: Run Unit Tests
  run: ./gradlew testDevDebugUnitTest

- name: Run Integration Tests
  run: ./gradlew testDevDebugUnitTest --tests "*IntegrationTest"

- name: Upload Test Results
  uses: actions/upload-artifact@v3
  with:
    name: test-results
    path: android/app/build/reports/tests/
```

---

## Next Steps

After all tests pass:
1. Review verification report: `TASK_28_VERIFICATION_REPORT.md`
2. Execute manual test plan: `docs/testing/live-caddy-e2e-test-plan.md`
3. Deploy to staging environment
4. Conduct field testing

---

**Need Help?**

- Full test plan: `docs/testing/live-caddy-e2e-test-plan.md`
- Verification report: `TASK_28_VERIFICATION_REPORT.md`
- Spec reference: `specs/live-caddy-mode.md`
