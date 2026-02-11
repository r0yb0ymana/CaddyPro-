# Coach Mode - Implementation Plan

**Spec**: `specs/coach-mode.md`
**Generated**: 2026-01-08
**Status**: DRAFT - Pending spec clarifications

---

## Summary

This plan breaks down Coach Mode into 28 tasks across 6 phases. The feature provides post-round diagnostics, swing video analysis, statistical insights, and personalized practice planning with calendar export.

**Key Components**:
- **SwingSensei**: Computer vision for swing video analysis
- **Shot Surgeon**: Statistical autopsy of round data
- **TempoSetter**: Practice plan scheduler with .ics export
- **RangeMaster**: Dynamic drill library

**Architecture approach**: Platform-specific implementations (Kotlin/Android, Swift/iOS) following existing MVVM + Clean Architecture patterns. Depends on shared domain models (potentially from NavCaddy Engine).

---

## Prerequisites & Blockers

**Spec clarifications needed**:

| Blocker | Spec Reference | Impact |
|---------|---------------|--------|
| Strokes gained benchmark source | Q1 | Blocks Task 9-11 (Shot Surgeon) |
| Minimum video quality thresholds | Q2 | Blocks Task 5-7 (SwingSensei) |
| Multi-angle fusion scope | Q3 | Affects Task 6 complexity |
| Drill versioning/localization | Q4 | Affects Task 18-19 (RangeMaster) |

**Dependencies on other features**:
- Requires round/shot data persistence (may overlap with NavCaddy Engine Phase 1)
- Requires miss pattern data (R4 references "miss store")

---

## Total Tasks: 28

| Phase | Tasks | Focus |
|-------|-------|-------|
| Phase 1: Foundation | 1-4 | Domain models, data layer, shared infrastructure |
| Phase 2: SwingSensei | 5-8 | Video upload, CV analysis, One-Fix output |
| Phase 3: Shot Surgeon | 9-13 | Statistical analysis, problem clubs, strokes gained |
| Phase 4: TempoSetter | 14-17 | Practice planning, .ics export |
| Phase 5: RangeMaster | 18-21 | Drill library, personalization |
| Phase 6: UI & Integration | 22-28 | Coach Mode screens, persona, testing |

---

## Phase 1: Foundation (Tasks 1-4)

### Task 1: Validate Spec Completeness

**Spec reference**: Section 7 (Open Questions)
**Acceptance criteria**: Prerequisite for all
**Dependencies**: None

#### Implementation steps:
1. Review all open questions (Q1-Q4) with stakeholders
2. Document benchmark data source for strokes gained (Q1)
3. Define video quality thresholds (resolution, lighting, frame rate) (Q2)
4. Confirm multi-angle scope for v1 (Q3)
5. Define drill versioning strategy (Q4)

#### Verification:
- [ ] Q1 resolved with specific benchmark dataset reference
- [ ] Q2 has concrete quality thresholds documented
- [ ] Q3 scope confirmed (single-angle MVP recommended)
- [ ] Q4 versioning strategy documented

---

### Task 2: Define Coach Mode Domain Models

**Spec reference**: R1, R2, R3, R4
**Acceptance criteria**: Foundation for A1-A4
**Dependencies**: Task 1, NavCaddy Engine Task 2 (shared models)

#### Implementation steps:

**Android** (`caddypro/domain/models/coach/`):
```kotlin
// SwingSensei models
data class SwingVideo(
    val id: String,
    val uri: Uri,
    val angle: SwingAngle,
    val uploadedAt: Instant,
    val quality: VideoQuality
)

enum class SwingAngle { FRONT_ON, DOWN_THE_LINE }

data class VideoQuality(
    val resolution: Resolution,
    val frameRate: Int,
    val lightingScore: Float,
    val meetsThreshold: Boolean
)

data class SwingAnalysis(
    val videoId: String,
    val keyFrames: List<KeyFrame>,
    val detectedFaults: List<DetectedFault>,
    val oneFix: OneFix?,
    val confidence: Float,
    val needsBetterVideo: Boolean
)

data class KeyFrame(
    val position: SwingPosition,
    val timestamp: Long,
    val frameUri: Uri?
)

enum class SwingPosition { ADDRESS, TOP, IMPACT, FOLLOW_THROUGH }

data class DetectedFault(
    val type: FaultType,
    val confidence: Float,
    val description: String
)

enum class FaultType {
    EARLY_EXTENSION,
    OVER_THE_TOP,
    CASTING,
    CHICKEN_WING,
    SWAY,
    SLIDE,
    FLIP,
    HANGING_BACK
}

data class OneFix(
    val priorityCue: String,
    val drill: Drill,
    val successLooksLike: String
)

// Shot Surgeon models
data class RoundAnalysis(
    val roundIds: List<String>,
    val problemClubs: List<ProblemClub>,
    val strokesGained: StrokesGainedBreakdown,
    val topContributors: List<ScoringContributor>,
    val primaryFocusArea: FocusArea,
    val dataSufficiency: DataSufficiency
)

data class ProblemClub(
    val club: Club,
    val penaltyRate: Float,
    val dispersionYards: Float,
    val strokesLost: Float
)

data class StrokesGainedBreakdown(
    val offTheTee: Float,
    val approach: Float,
    val aroundTheGreen: Float,
    val putting: Float,
    val total: Float,
    val benchmark: Benchmark
)

enum class Benchmark { SCRATCH, PLUS_5, PLUS_10, PLUS_15, PLUS_20 }

data class ScoringContributor(
    val category: String,
    val strokesImpact: Float,
    val description: String
)

data class FocusArea(
    val name: String,
    val rationale: String,
    val suggestedDrills: List<Drill>
)

data class DataSufficiency(
    val roundCount: Int,
    val shotCount: Int,
    val isSufficient: Boolean,
    val message: String?
)

// TempoSetter models
data class PracticePreferences(
    val availableDays: List<DayOfWeek>,
    val sessionLengthMinutes: Int,
    val accessContext: Set<PracticeContext>
)

enum class PracticeContext { RANGE, NETS, PUTTING_GREEN, GYM, HOME }

data class WeeklyPlan(
    val id: String,
    val weekOf: LocalDate,
    val sessions: List<PracticeSession>,
    val focusArea: FocusArea,
    val generatedAt: Instant
)

data class PracticeSession(
    val dayOfWeek: DayOfWeek,
    val drills: List<ScheduledDrill>,
    val totalMinutes: Int
)

data class ScheduledDrill(
    val drill: Drill,
    val durationMinutes: Int,
    val objective: String,
    val progressionLevel: Int
)

// RangeMaster models
data class Drill(
    val id: String,
    val title: String,
    val category: DrillCategory,
    val setup: String,
    val reps: String,
    val scoringMethod: String,
    val commonMistakes: List<String>,
    val targetsFaults: List<FaultType>,
    val targetsMisses: List<MissDirection>,
    val difficultyLevel: Int
)

enum class DrillCategory {
    FULL_SWING,
    SHORT_GAME,
    PUTTING,
    ALIGNMENT,
    TEMPO,
    STRENGTH
}
```

**iOS** (`Core/Domain/Models/Coach/`):
- Mirror all Kotlin models with Swift conventions

#### Verification:
- [ ] All models compile on both platforms
- [ ] Models support Codable/Serializable
- [ ] Enums align across platforms
- [ ] Unit tests validate serialization

---

### Task 3: Set Up Coach Mode Data Layer - Android

**Spec reference**: R2, R3, R4, C3
**Acceptance criteria**: A2, A3, A4
**Dependencies**: Task 2

#### Implementation steps:
1. Create Room entities for swing analysis, round analysis, practice plans
2. Create DAOs with queries for:
   - Recent swing analyses by video
   - Round analyses with date ranges
   - Practice plans by week
   - Drill catalog access
3. Add video file management (secure storage, deletion)
4. Add Hilt module for Coach Mode repositories

```kotlin
@Entity(tableName = "swing_analyses")
data class SwingAnalysisEntity(
    @PrimaryKey val id: String,
    val videoId: String,
    val keyFramesJson: String,
    val detectedFaultsJson: String,
    val oneFixJson: String?,
    val confidence: Float,
    val needsBetterVideo: Boolean,
    val createdAt: Long
)

@Entity(tableName = "round_analyses")
data class RoundAnalysisEntity(
    @PrimaryKey val id: String,
    val roundIdsJson: String,
    val problemClubsJson: String,
    val strokesGainedJson: String,
    val topContributorsJson: String,
    val primaryFocusAreaJson: String,
    val dataSufficiencyJson: String,
    val createdAt: Long
)

@Entity(tableName = "drills")
data class DrillEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val setup: String,
    val reps: String,
    val scoringMethod: String,
    val commonMistakesJson: String,
    val targetsFaultsJson: String,
    val targetsMissesJson: String,
    val difficultyLevel: Int,
    val version: Int
)
```

#### Verification:
- [ ] Database creates with all tables
- [ ] CRUD operations work for all entities
- [ ] Video files stored securely
- [ ] User can delete videos and analyses (C3)

---

### Task 4: Set Up Coach Mode Data Layer - iOS

**Spec reference**: R2, R3, R4, C3
**Acceptance criteria**: A2, A3, A4
**Dependencies**: Task 2

#### Implementation steps:
1. Add SwiftData models for coach mode entities
2. Create repository protocols and implementations
3. Add secure video file management
4. Configure ModelContainer extension

#### Verification:
- [ ] SwiftData models persist correctly
- [ ] Video deletion cascades to analyses
- [ ] Repository queries return expected results

---

## Phase 2: SwingSensei (Tasks 5-8)

### Task 5: Implement Video Upload & Quality Check - Android

**Spec reference**: R1, C3
**Acceptance criteria**: A1 (quality gating)
**Dependencies**: Task 3, Task 1 (needs Q2 resolved)

#### Implementation steps:
1. Create video picker integration (MediaStore/Photos)
2. Implement quality analyzer:
   - Resolution check (minimum 720p)
   - Frame rate check (minimum 30fps)
   - Lighting score estimation
3. Create upload flow with progress
4. Store video securely with encryption

```kotlin
class VideoQualityAnalyzer @Inject constructor() {

    suspend fun analyze(uri: Uri): VideoQuality {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)

        val width = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toInt() ?: 0
        val height = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toInt() ?: 0
        val frameRate = retriever.extractMetadata(METADATA_KEY_CAPTURE_FRAMERATE)?.toInt() ?: 30

        val lightingScore = estimateLighting(retriever)

        return VideoQuality(
            resolution = Resolution(width, height),
            frameRate = frameRate,
            lightingScore = lightingScore,
            meetsThreshold = width >= 1280 && height >= 720 &&
                            frameRate >= 30 && lightingScore >= 0.6f
        )
    }

    private suspend fun estimateLighting(retriever: MediaMetadataRetriever): Float {
        // Sample frames and analyze brightness/contrast
        val sampleFrame = retriever.getFrameAtTime(0)
        return analyzeBrightness(sampleFrame)
    }
}
```

#### Verification:
- [ ] Video picker launches and returns selection
- [ ] Quality analysis runs within 2 seconds
- [ ] Low-quality videos flagged correctly
- [ ] Videos stored with encryption

---

### Task 6: Implement Swing Analysis Service

**Spec reference**: R1
**Acceptance criteria**: A1
**Dependencies**: Task 5, Task 1 (needs Q2, Q3 resolved)

#### Implementation steps:
1. Create key frame extractor (address, top, impact, follow-through)
2. Implement fault detection model integration:
   - Option A: On-device ML (TFLite/CoreML)
   - Option B: Cloud API (if latency acceptable)
3. Calculate confidence scores per fault
4. Apply confidence gating (threshold from Q2)

```kotlin
interface SwingAnalyzer {
    suspend fun analyze(video: SwingVideo): SwingAnalysisResult
}

class SwingAnalyzerImpl @Inject constructor(
    private val keyFrameExtractor: KeyFrameExtractor,
    private val faultDetector: FaultDetector,
    private val config: SwingAnalysisConfig
) : SwingAnalyzer {

    override suspend fun analyze(video: SwingVideo): SwingAnalysisResult {
        // 1. Extract key frames
        val keyFrames = keyFrameExtractor.extract(video)

        // 2. Detect faults with confidence
        val detectedFaults = faultDetector.detect(keyFrames, video.angle)
            .filter { it.confidence >= config.faultMinConfidence }

        // 3. Check overall confidence
        val overallConfidence = calculateOverallConfidence(detectedFaults, keyFrames)

        return if (overallConfidence >= config.analysisMinConfidence) {
            SwingAnalysisResult.Success(
                keyFrames = keyFrames,
                faults = detectedFaults,
                confidence = overallConfidence
            )
        } else {
            SwingAnalysisResult.NeedsBetterVideo(
                reason = determineLowConfidenceReason(video, keyFrames),
                suggestion = "Try recording from ${suggestBetterAngle(video.angle)} with better lighting"
            )
        }
    }
}
```

#### Verification:
- [ ] Key frames extracted at correct swing positions
- [ ] Faults detected with confidence scores
- [ ] Low-confidence triggers "needs better video"
- [ ] Analysis completes within 10 seconds

---

### Task 7: Implement One-Fix Generator

**Spec reference**: R1
**Acceptance criteria**: A1
**Dependencies**: Task 6

#### Implementation steps:
1. Create fault prioritization logic (highest impact fault)
2. Map faults to priority cues
3. Map faults to recommended drills
4. Generate "success looks like" description

```kotlin
class OneFixGenerator @Inject constructor(
    private val drillRepository: DrillRepository,
    private val faultPrioritizer: FaultPrioritizer
) {

    suspend fun generate(analysis: SwingAnalysis): OneFix? {
        if (analysis.needsBetterVideo) return null

        val priorityFault = faultPrioritizer.selectPrimary(analysis.detectedFaults)
            ?: return null

        val drill = drillRepository.getDrillForFault(priorityFault.type)
            ?: return null

        return OneFix(
            priorityCue = getPriorityCue(priorityFault),
            drill = drill,
            successLooksLike = getSuccessDescription(priorityFault)
        )
    }

    private fun getPriorityCue(fault: DetectedFault): String {
        return when (fault.type) {
            FaultType.EARLY_EXTENSION -> "Feel like you're sitting into a chair through impact"
            FaultType.OVER_THE_TOP -> "Start the downswing with your lower body, not your shoulders"
            FaultType.CASTING -> "Maintain the angle between your lead arm and club into impact"
            // ... other faults
        }
    }

    private fun getSuccessDescription(fault: DetectedFault): String {
        return when (fault.type) {
            FaultType.EARLY_EXTENSION -> "Hips stay back, spine angle maintained at impact"
            FaultType.OVER_THE_TOP -> "Club drops inside on downswing, path is neutral or in-to-out"
            // ... other faults
        }
    }
}
```

#### Verification:
- [ ] Exactly one fix generated per analysis
- [ ] Fix includes cue, drill, and success description
- [ ] No fix generated when confidence too low
- [ ] Drill matches the detected fault

---

### Task 8: Implement SwingSensei iOS

**Spec reference**: R1
**Acceptance criteria**: A1
**Dependencies**: Task 4

#### Implementation steps:
1. Create video picker with PHPickerViewController
2. Implement VideoQualityAnalyzer using AVFoundation
3. Port SwingAnalyzer using CoreML or shared cloud API
4. Port OneFixGenerator logic

#### Verification:
- [ ] Video selection works via Photos picker
- [ ] Quality analysis matches Android thresholds
- [ ] Fault detection produces consistent results
- [ ] One-Fix output matches Android format

---

## Phase 3: Shot Surgeon (Tasks 9-13)

### Task 9: Implement Strokes Gained Calculator

**Spec reference**: R2
**Acceptance criteria**: A2
**Dependencies**: Task 2, Task 1 (needs Q1 resolved - benchmark source)

#### Implementation steps:
1. Define strokes gained lookup tables (from benchmark source)
2. Create calculator for each category:
   - Off the tee
   - Approach
   - Around the green
   - Putting
3. Handle edge cases (penalties, drops, unusual situations)

```kotlin
class StrokesGainedCalculator @Inject constructor(
    private val benchmarkData: BenchmarkDataSource
) {

    fun calculate(
        shots: List<Shot>,
        benchmark: Benchmark
    ): StrokesGainedBreakdown {
        val baselineExpected = benchmarkData.getBaseline(benchmark)

        val offTheTee = calculateCategory(
            shots.filter { it.isTeeShotPar4Or5() },
            baselineExpected.offTheTee
        )

        val approach = calculateCategory(
            shots.filter { it.isApproachShot() },
            baselineExpected.approach
        )

        val aroundTheGreen = calculateCategory(
            shots.filter { it.isAroundTheGreen() },
            baselineExpected.aroundTheGreen
        )

        val putting = calculateCategory(
            shots.filter { it.isPutt() },
            baselineExpected.putting
        )

        return StrokesGainedBreakdown(
            offTheTee = offTheTee,
            approach = approach,
            aroundTheGreen = aroundTheGreen,
            putting = putting,
            total = offTheTee + approach + aroundTheGreen + putting,
            benchmark = benchmark
        )
    }

    private fun calculateCategory(shots: List<Shot>, baseline: BaselineData): Float {
        // Calculate strokes gained using expected strokes from distance
        return shots.sumOf { shot ->
            val expectedBefore = baseline.expectedStrokes(shot.distanceTo)
            val expectedAfter = baseline.expectedStrokes(shot.resultDistance)
            val actual = 1.0 // One stroke taken
            (expectedBefore - expectedAfter - actual)
        }.toFloat()
    }
}
```

#### Verification:
- [ ] Strokes gained matches manual calculation for test data
- [ ] All four categories calculated correctly
- [ ] Penalties handled appropriately
- [ ] Benchmark selection works

---

### Task 10: Implement Problem Club Analyzer

**Spec reference**: R2
**Acceptance criteria**: A2
**Dependencies**: Task 9

#### Implementation steps:
1. Calculate penalty rate per club
2. Calculate dispersion (standard deviation of miss distance)
3. Calculate strokes lost per club
4. Rank and return worst performers

```kotlin
class ProblemClubAnalyzer @Inject constructor(
    private val strokesGainedCalculator: StrokesGainedCalculator
) {

    fun analyze(shots: List<Shot>, benchmark: Benchmark): List<ProblemClub> {
        return shots
            .groupBy { it.club }
            .map { (club, clubShots) ->
                ProblemClub(
                    club = club,
                    penaltyRate = calculatePenaltyRate(clubShots),
                    dispersionYards = calculateDispersion(clubShots),
                    strokesLost = calculateStrokesLost(clubShots, benchmark)
                )
            }
            .filter { it.strokesLost > 0 } // Only problem clubs
            .sortedByDescending { it.strokesLost }
            .take(5)
    }

    private fun calculatePenaltyRate(shots: List<Shot>): Float {
        val penalties = shots.count { it.resultedInPenalty }
        return penalties.toFloat() / shots.size
    }

    private fun calculateDispersion(shots: List<Shot>): Float {
        val distances = shots.mapNotNull { it.offlineDistance }
        if (distances.size < 2) return 0f
        return standardDeviation(distances)
    }
}
```

#### Verification:
- [ ] Penalty rate calculation correct
- [ ] Dispersion calculation matches expected
- [ ] Clubs ranked by strokes lost
- [ ] Handles clubs with few shots gracefully

---

### Task 11: Implement Round Analyzer Service

**Spec reference**: R2, C2
**Acceptance criteria**: A2
**Dependencies**: Tasks 9, 10

#### Implementation steps:
1. Create data sufficiency checker (minimum rounds/shots)
2. Aggregate analyses across rounds
3. Identify top 3 scoring contributors
4. Select primary focus area
5. Apply confidence gating (C2)

```kotlin
class RoundAnalyzerService @Inject constructor(
    private val roundRepository: RoundRepository,
    private val strokesGainedCalculator: StrokesGainedCalculator,
    private val problemClubAnalyzer: ProblemClubAnalyzer
) {

    suspend fun analyzeRounds(
        roundCount: Int = 10,
        benchmark: Benchmark = Benchmark.SCRATCH
    ): RoundAnalysis {
        val rounds = roundRepository.getRecentRounds(roundCount)
        val allShots = rounds.flatMap { it.shots }

        // Check data sufficiency
        val sufficiency = checkSufficiency(rounds, allShots)
        if (!sufficiency.isSufficient) {
            return RoundAnalysis(
                roundIds = rounds.map { it.id },
                problemClubs = emptyList(),
                strokesGained = StrokesGainedBreakdown.empty(benchmark),
                topContributors = emptyList(),
                primaryFocusArea = FocusArea.needsMoreData(),
                dataSufficiency = sufficiency
            )
        }

        val strokesGained = strokesGainedCalculator.calculate(allShots, benchmark)
        val problemClubs = problemClubAnalyzer.analyze(allShots, benchmark)
        val contributors = identifyContributors(strokesGained, problemClubs)
        val focusArea = selectFocusArea(contributors, problemClubs)

        return RoundAnalysis(
            roundIds = rounds.map { it.id },
            problemClubs = problemClubs,
            strokesGained = strokesGained,
            topContributors = contributors.take(3),
            primaryFocusArea = focusArea,
            dataSufficiency = sufficiency
        )
    }

    private fun checkSufficiency(rounds: List<Round>, shots: List<Shot>): DataSufficiency {
        val minRounds = 5
        val minShots = 200

        return DataSufficiency(
            roundCount = rounds.size,
            shotCount = shots.size,
            isSufficient = rounds.size >= minRounds && shots.size >= minShots,
            message = when {
                rounds.size < minRounds -> "Need at least $minRounds rounds for reliable analysis"
                shots.size < minShots -> "Need more shot data for statistical confidence"
                else -> null
            }
        )
    }
}
```

#### Verification:
- [ ] Minimum 5 rounds required (A2)
- [ ] Top 3 contributors identified
- [ ] Primary focus matches largest strokes-lost driver
- [ ] Insufficient data handled gracefully (C2)

---

### Task 12: Implement Shot Surgeon iOS

**Spec reference**: R2
**Acceptance criteria**: A2
**Dependencies**: Task 4

#### Implementation steps:
1. Port StrokesGainedCalculator
2. Port ProblemClubAnalyzer
3. Port RoundAnalyzerService
4. Ensure parity with Android calculations

#### Verification:
- [ ] Calculations match Android for same test data
- [ ] Data sufficiency checks consistent
- [ ] Focus area selection matches

---

### Task 13: Write Shot Surgeon Tests

**Spec reference**: R2, A2
**Acceptance criteria**: A2
**Dependencies**: Tasks 9-12

#### Implementation steps:
1. Unit tests for strokes gained calculation
2. Unit tests for problem club analysis
3. Integration tests for round analyzer
4. Test data sufficiency edge cases
5. iOS XCTest equivalents

```kotlin
@Test
fun `strokes gained calculation matches expected`() {
    val shots = listOf(
        // Approach from 150 yards, ends 10 feet from hole
        Shot(distanceTo = 150, resultDistance = 10, type = APPROACH),
        // Putt from 10 feet, makes it
        Shot(distanceTo = 10, resultDistance = 0, type = PUTT)
    )

    val result = calculator.calculate(shots, Benchmark.SCRATCH)

    // Expected values based on benchmark data
    assertEquals(0.15f, result.approach, 0.05f)
    assertEquals(0.20f, result.putting, 0.05f)
}

@Test
fun `insufficient data returns needs more data state`() = runTest {
    // Only 3 rounds
    whenever(roundRepository.getRecentRounds(any())).thenReturn(
        listOf(round1, round2, round3)
    )

    val result = service.analyzeRounds()

    assertFalse(result.dataSufficiency.isSufficient)
    assertEquals("Need at least 5 rounds for reliable analysis", result.dataSufficiency.message)
}
```

#### Verification:
- [ ] All calculation edge cases covered
- [ ] Data sufficiency tests pass
- [ ] Cross-platform consistency verified
- [ ] Tests run in CI

---

## Phase 4: TempoSetter (Tasks 14-17)

### Task 14: Implement Practice Plan Generator

**Spec reference**: R3
**Acceptance criteria**: A3, A4
**Dependencies**: Tasks 11, 18 (needs drill library)

#### Implementation steps:
1. Create plan generation algorithm
2. Distribute drills across available days
3. Apply progressive overload rules
4. Respect session length constraints

```kotlin
class PracticePlanGenerator @Inject constructor(
    private val drillRepository: DrillRepository,
    private val progressionEngine: ProgressionEngine
) {

    suspend fun generate(
        preferences: PracticePreferences,
        focusArea: FocusArea,
        currentLevel: Int = 1
    ): WeeklyPlan {
        // Get drills matching focus area
        val availableDrills = drillRepository.getDrillsForFocusArea(focusArea)
            .filter { it.difficultyLevel <= currentLevel + 1 }

        // Build sessions for each available day
        val sessions = preferences.availableDays.map { day ->
            buildSession(
                day = day,
                duration = preferences.sessionLengthMinutes,
                drills = availableDrills,
                context = preferences.accessContext,
                level = currentLevel
            )
        }

        return WeeklyPlan(
            id = UUID.randomUUID().toString(),
            weekOf = LocalDate.now().with(DayOfWeek.MONDAY),
            sessions = sessions,
            focusArea = focusArea,
            generatedAt = Instant.now()
        )
    }

    private fun buildSession(
        day: DayOfWeek,
        duration: Int,
        drills: List<Drill>,
        context: Set<PracticeContext>,
        level: Int
    ): PracticeSession {
        val scheduledDrills = mutableListOf<ScheduledDrill>()
        var remainingMinutes = duration

        // Filter drills by available context
        val contextDrills = drills.filter { drill ->
            drill.requiredContext.any { it in context }
        }

        // Fill session with drills
        for (drill in contextDrills.shuffled()) {
            if (remainingMinutes < 10) break

            val drillDuration = minOf(drill.suggestedDuration, remainingMinutes)
            scheduledDrills.add(
                ScheduledDrill(
                    drill = drill,
                    durationMinutes = drillDuration,
                    objective = progressionEngine.getObjective(drill, level),
                    progressionLevel = level
                )
            )
            remainingMinutes -= drillDuration
        }

        return PracticeSession(
            dayOfWeek = day,
            drills = scheduledDrills,
            totalMinutes = duration - remainingMinutes
        )
    }
}
```

#### Verification:
- [ ] Plan respects available days
- [ ] Session lengths match preference
- [ ] Drills match focus area
- [ ] Context constraints respected (range vs home)

---

### Task 15: Implement .ics Export

**Spec reference**: R3, C4
**Acceptance criteria**: A3
**Dependencies**: Task 14

#### Implementation steps:
1. Create ICS file generator following RFC 5545
2. Generate VEVENT for each session
3. Include drill titles and objectives in description
4. Handle timezone correctly
5. Provide share/export mechanism

```kotlin
class IcsExporter @Inject constructor() {

    fun export(plan: WeeklyPlan): String {
        return buildString {
            appendLine("BEGIN:VCALENDAR")
            appendLine("VERSION:2.0")
            appendLine("PRODID:-//CaddyPro//Coach Mode//EN")
            appendLine("CALSCALE:GREGORIAN")
            appendLine("METHOD:PUBLISH")

            plan.sessions.forEach { session ->
                val date = plan.weekOf.with(session.dayOfWeek)
                appendLine(generateEvent(session, date, plan.focusArea))
            }

            appendLine("END:VCALENDAR")
        }
    }

    private fun generateEvent(
        session: PracticeSession,
        date: LocalDate,
        focusArea: FocusArea
    ): String {
        val startTime = LocalTime.of(18, 0) // Default 6pm, could be configurable
        val endTime = startTime.plusMinutes(session.totalMinutes.toLong())

        val description = buildDescription(session)

        return buildString {
            appendLine("BEGIN:VEVENT")
            appendLine("UID:${UUID.randomUUID()}@caddypro.com")
            appendLine("DTSTAMP:${formatDateTime(Instant.now())}")
            appendLine("DTSTART:${formatDateTime(date.atTime(startTime))}")
            appendLine("DTEND:${formatDateTime(date.atTime(endTime))}")
            appendLine("SUMMARY:Golf Practice - ${focusArea.name}")
            appendLine("DESCRIPTION:${escapeIcs(description)}")
            appendLine("END:VEVENT")
        }
    }

    private fun buildDescription(session: PracticeSession): String {
        return session.drills.joinToString("\\n\\n") { scheduled ->
            "${scheduled.drill.title} (${scheduled.durationMinutes}min)\\n" +
            "Objective: ${scheduled.objective}"
        }
    }

    private fun escapeIcs(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace(",", "\\,")
            .replace(";", "\\;")
            .replace("\n", "\\n")
    }
}
```

#### Verification:
- [ ] .ics file validates against RFC 5545
- [ ] Imports successfully into Apple Calendar
- [ ] Imports successfully into Google Calendar
- [ ] Events have correct dates, times, durations
- [ ] Drill details appear in event description

---

### Task 16: Implement Plan Regeneration

**Spec reference**: R3
**Acceptance criteria**: A3
**Dependencies**: Task 14

#### Implementation steps:
1. Store user preferences persistently
2. Allow preference updates
3. Regenerate plan on preference change
4. Preserve progression level across regenerations

```kotlin
class PracticePlanManager @Inject constructor(
    private val generator: PracticePlanGenerator,
    private val planRepository: PlanRepository,
    private val preferencesRepository: PreferencesRepository
) {

    suspend fun regeneratePlan(
        newPreferences: PracticePreferences? = null
    ): WeeklyPlan {
        // Update preferences if provided
        newPreferences?.let { preferencesRepository.save(it) }

        val preferences = preferencesRepository.get()
        val currentPlan = planRepository.getCurrentPlan()
        val focusArea = currentPlan?.focusArea ?: getDefaultFocusArea()
        val currentLevel = currentPlan?.sessions
            ?.flatMap { it.drills }
            ?.maxOfOrNull { it.progressionLevel } ?: 1

        val newPlan = generator.generate(preferences, focusArea, currentLevel)
        planRepository.save(newPlan)

        return newPlan
    }
}
```

#### Verification:
- [ ] Preferences persist across sessions
- [ ] Plan regenerates when preferences change
- [ ] Progression level preserved
- [ ] Old plans archived (not deleted)

---

### Task 17: Implement TempoSetter iOS

**Spec reference**: R3
**Acceptance criteria**: A3
**Dependencies**: Task 4

#### Implementation steps:
1. Port PracticePlanGenerator
2. Port IcsExporter
3. Implement iOS share sheet for .ics files
4. Port PracticePlanManager

#### Verification:
- [ ] Plan generation matches Android
- [ ] .ics exports via share sheet
- [ ] Calendar import works on iOS
- [ ] Preference persistence works

---

## Phase 5: RangeMaster (Tasks 18-21)

### Task 18: Create Drill Catalog Schema

**Spec reference**: R4
**Acceptance criteria**: A4
**Dependencies**: Task 2, Task 1 (needs Q4 resolved)

#### Implementation steps:
1. Define drill data format (JSON/YAML)
2. Create initial drill catalog (20-30 drills)
3. Tag drills with fault types and miss patterns
4. Include versioning metadata
5. Support localization structure

```json
{
  "drills": [
    {
      "id": "alignment-stick-path",
      "version": 1,
      "title": "Alignment Stick Path Drill",
      "category": "FULL_SWING",
      "setup": "Place two alignment sticks on the ground...",
      "reps": "10 swings at 50% speed, 10 at 75%, 10 at full",
      "scoringMethod": "Count swings where club passes inside the stick",
      "commonMistakes": [
        "Starting too fast",
        "Stick placed too close to ball"
      ],
      "targetsFaults": ["OVER_THE_TOP", "CASTING"],
      "targetsMisses": ["SLICE", "PULL"],
      "difficultyLevel": 1,
      "requiredContext": ["RANGE"],
      "suggestedDurationMinutes": 15,
      "successCriteria": "8/10 swings pass inside the stick"
    }
  ]
}
```

#### Verification:
- [ ] Schema validates all drill entries
- [ ] All drills have required fields
- [ ] Fault/miss mappings are complete
- [ ] Version numbers assigned

---

### Task 19: Implement Drill Repository

**Spec reference**: R4
**Acceptance criteria**: A4
**Dependencies**: Task 18, Tasks 3-4

#### Implementation steps:
1. Parse and load drill catalog
2. Index by fault type, miss direction, category
3. Support filtering and searching
4. Handle catalog updates

```kotlin
class DrillRepositoryImpl @Inject constructor(
    private val drillDao: DrillDao,
    private val catalogLoader: DrillCatalogLoader
) : DrillRepository {

    override suspend fun getDrillsForFault(fault: FaultType): List<Drill> {
        return drillDao.getByFaultType(fault.name)
            .map { it.toDomain() }
    }

    override suspend fun getDrillsForMiss(miss: MissDirection): List<Drill> {
        return drillDao.getByMissType(miss.name)
            .map { it.toDomain() }
    }

    override suspend fun getDrillsForFocusArea(focusArea: FocusArea): List<Drill> {
        // Combine faults and misses from focus area
        val faultDrills = focusArea.suggestedFaults
            .flatMap { getDrillsForFault(it) }
        val missDrills = focusArea.suggestedMisses
            .flatMap { getDrillsForMiss(it) }

        return (faultDrills + missDrills).distinctBy { it.id }
    }

    override suspend fun refreshCatalog() {
        val catalog = catalogLoader.load()
        drillDao.insertAll(catalog.map { it.toEntity() })
    }
}
```

#### Verification:
- [ ] All drills loadable from catalog
- [ ] Filtering by fault works
- [ ] Filtering by miss works
- [ ] Catalog refresh updates database

---

### Task 20: Implement Drill Personalization Engine

**Spec reference**: R4, R5
**Acceptance criteria**: A4
**Dependencies**: Task 19, NavCaddy Engine Task 14 (miss pattern store)

#### Implementation steps:
1. Integrate with miss pattern store
2. Rank drills by relevance to user's patterns
3. Prioritize high-impact fixes
4. Add measurable targets based on user data

```kotlin
class DrillPersonalizationEngine @Inject constructor(
    private val drillRepository: DrillRepository,
    private val missPatternAggregator: MissPatternAggregator,
    private val roundAnalyzer: RoundAnalyzerService
) {

    suspend fun getPersonalizedDrills(limit: Int = 10): List<PersonalizedDrill> {
        // Get user's miss patterns
        val missPatterns = missPatternAggregator.getActivePatterns()

        // Get latest round analysis for strokes lost data
        val analysis = roundAnalyzer.analyzeRounds()

        // Score drills based on relevance
        val allDrills = drillRepository.getAll()

        return allDrills
            .map { drill ->
                val relevanceScore = calculateRelevance(drill, missPatterns, analysis)
                val target = generateTarget(drill, missPatterns)

                PersonalizedDrill(
                    drill = drill,
                    relevanceScore = relevanceScore,
                    personalizedTarget = target,
                    rationale = generateRationale(drill, missPatterns)
                )
            }
            .sortedByDescending { it.relevanceScore }
            .take(limit)
    }

    private fun calculateRelevance(
        drill: Drill,
        patterns: List<MissPattern>,
        analysis: RoundAnalysis
    ): Float {
        var score = 0f

        // Score based on miss pattern matches
        patterns.forEach { pattern ->
            if (pattern.direction in drill.targetsMisses) {
                score += pattern.frequency * pattern.confidence
            }
        }

        // Boost if drill addresses problem clubs
        analysis.problemClubs.forEach { problemClub ->
            if (drill.appliesToClubType(problemClub.club.type)) {
                score += problemClub.strokesLost
            }
        }

        return score
    }

    private fun generateTarget(drill: Drill, patterns: List<MissPattern>): String {
        // Generate measurable target based on user's data
        val primaryMiss = patterns.maxByOrNull { it.frequency }

        return when (primaryMiss?.direction) {
            MissDirection.SLICE, MissDirection.PUSH ->
                "Reduce right-side misses by 30% in next 5 rounds"
            MissDirection.HOOK, MissDirection.PULL ->
                "Reduce left-side misses by 30% in next 5 rounds"
            else -> drill.successCriteria
        }
    }
}

data class PersonalizedDrill(
    val drill: Drill,
    val relevanceScore: Float,
    val personalizedTarget: String,
    val rationale: String
)
```

#### Verification:
- [ ] Drills ranked by user relevance
- [ ] OB-right pattern prioritizes alignment drills (A4)
- [ ] Targets are measurable and personalized
- [ ] Rationale explains why drill was selected

---

### Task 21: Implement RangeMaster iOS

**Spec reference**: R4
**Acceptance criteria**: A4
**Dependencies**: Task 4

#### Implementation steps:
1. Port DrillRepository
2. Port DrillPersonalizationEngine
3. Ensure parity with Android recommendations

#### Verification:
- [ ] Drill loading works
- [ ] Personalization matches Android
- [ ] Same patterns produce same recommendations

---

## Phase 6: UI & Integration (Tasks 22-28)

### Task 22: Build Coach Mode Navigation - Android

**Spec reference**: C1
**Acceptance criteria**: All
**Dependencies**: Phase 2-5

#### Implementation steps:
1. Add Coach Mode to main navigation
2. Create Coach Mode hub screen
3. Add navigation to SwingSensei, Shot Surgeon, TempoSetter, RangeMaster
4. Implement bottom navigation or tabs

```kotlin
sealed class CoachScreen(val route: String) {
    object Hub : CoachScreen("coach")
    object SwingSensei : CoachScreen("coach/swing-sensei")
    object ShotSurgeon : CoachScreen("coach/shot-surgeon")
    object TempoSetter : CoachScreen("coach/tempo-setter")
    object RangeMaster : CoachScreen("coach/range-master")
    object DrillDetail : CoachScreen("coach/drill/{drillId}")
}
```

#### Verification:
- [ ] Navigation between all Coach screens works
- [ ] Back navigation correct
- [ ] Deep links work from NavCaddy Engine

---

### Task 23: Build SwingSensei Screen - Android

**Spec reference**: R1, C1
**Acceptance criteria**: A1
**Dependencies**: Tasks 5-7, Task 22

#### Implementation steps:
1. Create video upload UI with camera/gallery options
2. Show quality check results
3. Display analysis progress
4. Show One-Fix result or "need better video" prompt
5. Link to recommended drill

#### Verification:
- [ ] Video selection works
- [ ] Quality feedback shown
- [ ] Analysis results display correctly
- [ ] One-Fix is prominent and actionable

---

### Task 24: Build Shot Surgeon Screen - Android

**Spec reference**: R2, C1
**Acceptance criteria**: A2
**Dependencies**: Tasks 9-11, Task 22

#### Implementation steps:
1. Create round selector (last N rounds)
2. Display strokes gained breakdown
3. Show problem clubs list
4. Display top 3 contributors
5. Highlight primary focus area
6. Handle "needs more data" state

#### Verification:
- [ ] Round selection works
- [ ] Strokes gained visualized clearly
- [ ] Problem clubs ranked
- [ ] Focus area is actionable
- [ ] Insufficient data handled gracefully

---

### Task 25: Build TempoSetter Screen - Android

**Spec reference**: R3, C1
**Acceptance criteria**: A3
**Dependencies**: Tasks 14-16, Task 22

#### Implementation steps:
1. Create preference input UI (days, duration, context)
2. Display weekly plan with drill schedule
3. Show drill details on tap
4. Add .ics export button
5. Add regenerate plan action

#### Verification:
- [ ] Preferences input intuitive
- [ ] Plan displays all sessions
- [ ] Drill details accessible
- [ ] Export triggers share sheet
- [ ] Regenerate updates plan

---

### Task 26: Build RangeMaster Screen - Android

**Spec reference**: R4, C1
**Acceptance criteria**: A4
**Dependencies**: Tasks 18-20, Task 22

#### Implementation steps:
1. Create personalized drill list
2. Show relevance rationale
3. Display drill detail with all fields
4. Show personalized targets
5. Add drill filtering/search

#### Verification:
- [ ] Personalized drills shown first
- [ ] Rationale explains selection
- [ ] All drill details visible
- [ ] Targets are measurable

---

### Task 27: Build Coach Mode Screens - iOS

**Spec reference**: R1-R4, C1
**Acceptance criteria**: A1-A4
**Dependencies**: Tasks 8, 12, 17, 21

#### Implementation steps:
1. Create CoachView hub
2. Port SwingSenseiView
3. Port ShotSurgeonView
4. Port TempoSetterView
5. Port RangeMasterView
6. Follow HIG patterns

#### Verification:
- [ ] All screens match iOS HIG
- [ ] Navigation patterns correct
- [ ] Functionality parity with Android
- [ ] Accessibility labels present

---

### Task 28: Write Integration Tests

**Spec reference**: A1-A4
**Acceptance criteria**: A1-A4
**Dependencies**: All previous tasks

#### Implementation steps:
1. End-to-end test: video upload → One-Fix output (A1)
2. End-to-end test: rounds → Shot Surgeon analysis (A2)
3. End-to-end test: preferences → .ics export (A3)
4. End-to-end test: miss patterns → personalized drills (A4)
5. Cross-feature test: Shot Surgeon → TempoSetter integration

```kotlin
@Test
fun `video analysis produces one-fix when quality sufficient`() = runTest {
    // Upload test video meeting quality thresholds
    val video = uploadTestVideo("swing_front_on_720p.mp4")

    // Run analysis
    val result = swingSenseiViewModel.analyzeVideo(video.id)

    // Verify One-Fix output
    assertTrue(result is AnalysisResult.Success)
    val oneFix = (result as AnalysisResult.Success).oneFix
    assertNotNull(oneFix)
    assertNotNull(oneFix.priorityCue)
    assertNotNull(oneFix.drill)
    assertNotNull(oneFix.successLooksLike)
}

@Test
fun `ics export imports into calendar`() = runTest {
    // Generate plan
    val preferences = PracticePreferences(
        availableDays = listOf(MONDAY, WEDNESDAY, FRIDAY),
        sessionLengthMinutes = 45,
        accessContext = setOf(RANGE, PUTTING_GREEN)
    )
    val plan = tempoSetterViewModel.generatePlan(preferences)

    // Export .ics
    val icsContent = icsExporter.export(plan)

    // Validate format
    assertTrue(icsContent.startsWith("BEGIN:VCALENDAR"))
    assertTrue(icsContent.contains("VERSION:2.0"))
    assertEquals(3, icsContent.count { it.contains("BEGIN:VEVENT") })
}
```

#### Verification:
- [ ] All acceptance criteria have passing tests
- [ ] Cross-feature integration verified
- [ ] Tests run in CI
- [ ] iOS equivalents pass

---

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| CV model accuracy insufficient | High | High | Confidence gating, request better video, human review for edge cases |
| Strokes gained benchmark data unavailable | Medium | High | Use PGA Tour public data, allow custom benchmarks |
| Video processing too slow | Medium | Medium | On-device ML, progress indicators, background processing |
| .ics compatibility issues | Low | Medium | Test with multiple calendar apps, follow RFC strictly |
| Drill catalog too small | Medium | Medium | Start with 30 high-quality drills, add based on user feedback |
| Cross-platform calculation drift | Medium | High | Shared test data, automated parity tests |

---

## Open Questions (Blocking)

1. **Q1 (Benchmark Source)**: What data source for strokes gained baselines?
   - Options: PGA Tour ShotLink, proprietary, user-configurable

2. **Q2 (Video Quality)**: What are exact thresholds?
   - Proposed: 720p minimum, 30fps, lighting score ≥ 0.6

3. **Q3 (Multi-angle)**: Single angle for v1?
   - Recommendation: Single angle MVP, multi-angle v2

4. **Q4 (Drill Versioning)**: How to handle updates?
   - Proposed: Version number per drill, migration on catalog update

---

## Dependencies on Other Features

- **NavCaddy Engine**: Task 14 (miss pattern store) needed for drill personalization
- **Shared Domain Models**: Task 2 (shot, round, club models) may overlap

---

## Next Steps

1. Resolve blocking open questions (Q1-Q2 critical)
2. Confirm NavCaddy Engine dependency timeline
3. Begin Task 1 (spec validation)
4. Proceed with Phase 1 foundation
