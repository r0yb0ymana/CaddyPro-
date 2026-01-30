# Implementation Plan: Live Caddy Mode

**Feature**: Live Caddy Mode
**Spec**: `specs/live-caddy-mode.md`
**Status**: Ready for implementation
**Created**: 2026-01-30
**Estimated tasks**: 28

---

## Executive Summary

Live Caddy Mode provides on-course strategy with minimal screen time through:
- **Forecaster HUD**: Real-time weather integration with conditions adjustment
- **BodyCaddy**: Wearable-based readiness scoring that adjusts risk tolerance
- **PinSeeker AI Map**: Hazard-aware landing zones personalized to player's miss bias
- **Real-Time Shot Logger**: One-second logging flow with offline-first queueing
- **Voice-first interaction**: Bones persona responses to queries like "What's the play?"

The codebase has **solid foundations** with domain models, repository patterns, Room database, Hilt DI, StateFlow-based state management, and analytics infrastructure already in place.

---

## Architectural Readiness

| Component | Status | Notes |
|-----------|--------|-------|
| Domain models (Round, Shot, Club, MissPattern) | ✅ Ready | Well-defined with 163 Kotlin files |
| Data persistence (Room database) | ✅ Ready | 7 tables with strategic indexes |
| Repository pattern | ✅ Ready | 25+ methods in NavCaddyRepository |
| State management (StateFlow) | ✅ Ready | ConversationViewModel is excellent reference |
| DI (Hilt) | ✅ Ready | Modular with @Singleton core services |
| Voice support | ✅ Ready | VoiceInputManager, analytics tracking |
| Offline support | ✅ Ready | NetworkMonitor, offline handlers |
| Analytics | ✅ Ready | Event tracking, PII redaction |
| Theme/Design | ✅ Ready | Material 3 with component library |
| Weather API | ⚠️ TBD | Needs Retrofit service + provider selection |
| Wearable integration | ⚠️ TBD | Platform-specific services needed |
| HUD UI components | ⚠️ TBD | Need custom Compose components |

---

## Task Breakdown

Total: **28 tasks** organized in 6 phases

### Phase 1: Foundation & API Integration (Tasks 1-5)
### Phase 2: Domain Services & Strategy Engine (Tasks 6-12)
### Phase 3: UI Components & HUD (Tasks 13-18)
### Phase 4: Shot Logger & Offline Sync (Tasks 19-22)
### Phase 5: Voice Integration & Navigation (Tasks 23-25)
### Phase 6: Testing & Verification (Tasks 26-28)

---

## PHASE 1: FOUNDATION & API INTEGRATION

### Task 1: Validate Spec Completeness & Resolve Open Questions

**Spec reference**: Section 7 (Open questions Q1-Q4)
**Acceptance criteria**: N/A (meta-task)
**Dependencies**: None

#### Implementation steps:
1. Review spec open questions:
   - Q1: Weather provider and API limits → **Decision needed**: OpenWeatherMap, WeatherAPI.com, or NOAA?
   - Q2: Course data source → **Decision needed**: Use placeholder hazard data for MVP or integrate with provider?
   - Q3: Readiness algorithm → **Recommendation**: Use fixed weights for MVP (HRV: 40%, Sleep: 40%, Stress: 20%)
   - Q4: Minimum logging taxonomy → **Confirmed**: Club + result (fairway/rough/bunker/water/OB/green + directional modifiers) per spec R6

2. Document decisions in this plan
3. Flag any blockers requiring user input

#### Verification:
- [ ] All Q1-Q4 questions have documented decisions or are marked as "use placeholder for MVP"
- [ ] No spec ambiguities block implementation start

#### Decisions Made:
- **Q1 Weather Provider**: Use **OpenWeatherMap** (free tier: 60 calls/min, 1M calls/month)
  - Endpoint: Current Weather Data API
  - Fields: wind_speed, wind_deg, temp, humidity
  - Fallback: Manual weather entry if API unavailable
- **Q2 Course Data**: Use **placeholder hazard data** for MVP (hardcoded sample holes)
  - Future: Integrate with GolfNow, GHIN, or manual import
- **Q3 Readiness Algorithm**: **Fixed weights** for MVP
  - HRV: 40%, Sleep Quality: 40%, Stress Level: 20%
  - Normalize to 0-100 scale
  - User override always available
- **Q4 Logging Taxonomy**: Use spec R6 exactly
  - Club selection (tap from bag profile)
  - Result: primary (fairway/rough/bunker/water/OB/green) + modifier (short/long/left/right)
  - Writes to Shot entity with lie and optional miss direction

---

### Task 2: Create Weather Data Models & API Contract

**Spec reference**: R2 (Forecaster HUD)
**Acceptance criteria**: A1 (Weather HUD renders within 2 seconds)
**Dependencies**: Task 1 (weather provider decision)

#### Implementation steps:
1. Create `domain/caddy/models/WeatherData.kt`:
   ```kotlin
   data class WeatherData(
       val windSpeedMps: Double,      // meters per second
       val windDegrees: Int,           // 0-360, meteorological (from direction)
       val temperatureCelsius: Double,
       val humidity: Int,              // percentage
       val timestamp: Long,            // epoch millis
       val location: Location
   ) {
       fun windComponent(targetBearing: Int): WindComponent
       fun airDensityProxy(): Double
       fun conditionsAdjustment(): ConditionsAdjustment
   }

   data class WindComponent(
       val headwind: Double,   // positive = helping, negative = hurting
       val crosswind: Double   // positive = right-to-left, negative = left-to-right
   )

   data class ConditionsAdjustment(
       val carryModifier: Double,  // e.g., 0.95 = 5% less carry
       val reason: String           // e.g., "Cold air (+10°C) and headwind (-5 m/s)"
   )
   ```

2. Create `data/caddy/remote/WeatherApiService.kt` (Retrofit interface):
   ```kotlin
   interface WeatherApiService {
       @GET("weather")
       suspend fun getCurrentWeather(
           @Query("lat") latitude: Double,
           @Query("lon") longitude: Double,
           @Query("appid") apiKey: String,
           @Query("units") units: String = "metric"
       ): WeatherApiResponse
   }
   ```

3. Create `data/caddy/remote/WeatherApiResponse.kt` (DTO):
   ```kotlin
   data class WeatherApiResponse(
       val wind: Wind,
       val main: Main,
       val dt: Long
   ) {
       data class Wind(val speed: Double, val deg: Int)
       data class Main(val temp: Double, val humidity: Int)
   }
   ```

4. Add mapping extension: `WeatherApiResponse.toDomain(location: Location): WeatherData`

#### Verification:
- [ ] WeatherData model compiles with required fields
- [ ] WeatherApiService interface uses suspend functions
- [ ] DTO → Domain mapping preserves all required fields
- [ ] windComponent() and conditionsAdjustment() methods have placeholder implementations (refined in Task 7)

---

### Task 3: Create Readiness & Wearable Data Models

**Spec reference**: R3 (BodyCaddy)
**Acceptance criteria**: A2 (Readiness impacts strategy)
**Dependencies**: Task 1 (readiness algorithm decision)

#### Implementation steps:
1. Create `domain/caddy/models/ReadinessData.kt`:
   ```kotlin
   data class ReadinessScore(
       val overall: Int,               // 0-100
       val breakdown: ReadinessBreakdown,
       val timestamp: Long,
       val source: ReadinessSource
   ) {
       fun isLow(threshold: Int = 60): Boolean = overall < threshold
       fun adjustmentFactor(): Double  // 0.5-1.0 (low readiness = 0.5)
   }

   data class ReadinessBreakdown(
       val hrv: MetricScore?,          // null if unavailable
       val sleepQuality: MetricScore?,
       val stressLevel: MetricScore?
   )

   data class MetricScore(
       val value: Double,      // 0-100 normalized
       val weight: Double      // 0.0-1.0 contribution
   )

   enum class ReadinessSource {
       WEARABLE_SYNC,  // From Apple Health / Google Fit
       MANUAL_ENTRY    // User override
   }
   ```

2. Create `domain/caddy/models/WearableMetrics.kt`:
   ```kotlin
   data class WearableMetrics(
       val hrvMs: Double?,             // HRV in milliseconds
       val sleepMinutes: Int?,         // Sleep duration
       val sleepQualityScore: Double?, // 0-100 if available
       val stressScore: Double?,       // 0-100 (0=low stress)
       val timestamp: Long
   )
   ```

3. Add Room entity `data/caddy/local/entity/ReadinessScoreEntity.kt`:
   ```kotlin
   @Entity(tableName = "readiness_scores")
   data class ReadinessScoreEntity(
       @PrimaryKey val timestamp: Long,
       val overall: Int,
       val hrvScore: Double?,
       val sleepScore: Double?,
       val stressScore: Double?,
       val source: String  // "WEARABLE_SYNC" or "MANUAL_ENTRY"
   )
   ```

4. Create DAO with `@Dao interface ReadinessScoreDao` with:
   - `suspend fun insert(score: ReadinessScoreEntity)`
   - `suspend fun getMostRecent(): ReadinessScoreEntity?`
   - `suspend fun getScoresInRange(start: Long, end: Long): List<ReadinessScoreEntity>`

#### Verification:
- [ ] ReadinessScore model compiles with breakdown structure
- [ ] ReadinessScoreEntity table schema is valid
- [ ] DAO methods use suspend for async operations
- [ ] adjustmentFactor() returns reasonable range (0.5-1.0)

---

### Task 4: Create Hole Strategy & Hazard Models

**Spec reference**: R4 (PinSeeker AI Map)
**Acceptance criteria**: A3 (Hazard-aware landing zone)
**Dependencies**: None

#### Implementation steps:
1. Create `domain/caddy/models/HoleStrategy.kt`:
   ```kotlin
   data class HoleStrategy(
       val holeNumber: Int,
       val dangerZones: List<HazardZone>,
       val recommendedLandingZone: LandingZone,
       val riskCallouts: List<String>,
       val personalizedFor: PersonalizationContext
   )

   data class HazardZone(
       val type: HazardType,
       val location: HazardLocation,
       val penaltyStrokes: Double,        // Expected penalty
       val affectedMisses: List<MissDirection>  // e.g., [SLICE] if right-side hazard
   )

   enum class HazardType {
       WATER, OB, BUNKER, PENALTY_ROUGH, TREES
   }

   data class HazardLocation(
       val side: String,           // "right", "left", "center", "long", "short"
       val distanceFromTee: IntRange  // e.g., 220..260 meters
   )

   data class LandingZone(
       val targetLine: Int,        // Bearing in degrees
       val idealDistance: Int,     // Meters from tee
       val safetyMargin: Int,      // Meters (increases with low readiness)
       val visualCue: String       // e.g., "Aim 10 yards left of fairway bunker"
   )

   data class PersonalizationContext(
       val handicap: Int,
       val dominantMiss: MissDirection,
       val clubDistances: Map<String, Int>,  // Club name → carry distance
       val readinessScore: Int
   )
   ```

2. Create `domain/caddy/models/CourseHole.kt` (placeholder for MVP):
   ```kotlin
   data class CourseHole(
       val number: Int,
       val par: Int,
       val lengthMeters: Int,
       val hazards: List<HazardZone>,
       val pinPosition: PinPosition?
   )

   data class PinPosition(
       val front: Boolean,      // Front/middle/back thirds
       val middle: Boolean,
       val back: Boolean,
       val left: Boolean,       // Left/center/right thirds
       val center: Boolean,
       val right: Boolean
   )
   ```

3. Add placeholder course data: `data/caddy/local/PlaceholderCourseData.kt` with 3 sample holes

#### Verification:
- [ ] HoleStrategy model includes all required fields per spec R4
- [ ] HazardZone model supports personalization based on miss direction
- [ ] LandingZone includes safetyMargin for readiness adjustment
- [ ] PlaceholderCourseData provides 3 varied examples (par 3, par 4 with water, par 5 with OB)

---

### Task 5: Set Up Weather API Integration (Retrofit Service)

**Spec reference**: R2 (Forecaster HUD)
**Acceptance criteria**: A1 (Weather HUD renders within 2 seconds)
**Dependencies**: Task 2 (weather models)

#### Implementation steps:
1. Add OpenWeatherMap API key to `build.gradle.kts`:
   ```kotlin
   buildTypes {
       debug {
           buildConfigField("String", "WEATHER_API_KEY", "\"${project.findProperty("WEATHER_API_KEY") ?: ""}\"")
       }
   }
   ```

2. Update `di/AppModule.kt` to provide WeatherApiService:
   ```kotlin
   @Provides
   @Singleton
   fun provideWeatherApiService(okHttpClient: OkHttpClient): WeatherApiService {
       return Retrofit.Builder()
           .baseUrl("https://api.openweathermap.org/data/2.5/")
           .client(okHttpClient)
           .addConverterFactory(GsonConverterFactory.create())
           .build()
           .create(WeatherApiService::class.java)
   }
   ```

3. Create `data/caddy/repository/WeatherRepository.kt`:
   ```kotlin
   interface WeatherRepository {
       suspend fun getCurrentWeather(location: Location): Result<WeatherData>
       suspend fun getCachedWeather(): WeatherData?
   }
   ```

4. Create `data/caddy/repository/WeatherRepositoryImpl.kt` with:
   - 5-minute cache (WeatherData stored in memory with timestamp)
   - Error handling for network failures → fallback to cache or manual entry
   - Location permission check

5. Create Hilt module `di/CaddyDataModule.kt`:
   ```kotlin
   @Module
   @InstallIn(SingletonComponent::class)
   abstract class CaddyDataModule {
       @Binds
       abstract fun bindWeatherRepository(impl: WeatherRepositoryImpl): WeatherRepository
   }
   ```

#### Verification:
- [ ] WeatherApiService is provided via Hilt
- [ ] WeatherRepository implements caching with 5-minute expiry
- [ ] Network errors return cached data or Result.failure()
- [ ] Build config includes WEATHER_API_KEY placeholder

---

## PHASE 2: DOMAIN SERVICES & STRATEGY ENGINE

### Task 6: Implement Readiness Calculation Service

**Spec reference**: R3 (BodyCaddy)
**Acceptance criteria**: A2 (Readiness impacts strategy)
**Dependencies**: Task 3 (readiness models)

#### Implementation steps:
1. Create `domain/caddy/services/ReadinessCalculator.kt`:
   ```kotlin
   @Singleton
   class ReadinessCalculator @Inject constructor() {
       fun calculateReadiness(metrics: WearableMetrics): ReadinessScore {
           // Normalize each metric to 0-100
           val hrvScore = normalizeHRV(metrics.hrvMs)
           val sleepScore = normalizeSleep(metrics.sleepMinutes, metrics.sleepQualityScore)
           val stressScore = normalizeStress(metrics.stressScore)

           // Weighted average (HRV: 40%, Sleep: 40%, Stress: 20%)
           val overall = (
               (hrvScore?.value ?: 50.0) * 0.4 +
               (sleepScore?.value ?: 50.0) * 0.4 +
               (stressScore?.value ?: 50.0) * 0.2
           ).toInt()

           return ReadinessScore(
               overall = overall,
               breakdown = ReadinessBreakdown(hrvScore, sleepScore, stressScore),
               timestamp = System.currentTimeMillis(),
               source = ReadinessSource.WEARABLE_SYNC
           )
       }

       private fun normalizeHRV(hrvMs: Double?): MetricScore?
       private fun normalizeSleep(minutes: Int?, quality: Double?): MetricScore?
       private fun normalizeStress(stress: Double?): MetricScore?
   }
   ```

2. Create `domain/caddy/services/WearableSyncService.kt` (interface):
   ```kotlin
   interface WearableSyncService {
       suspend fun syncMetrics(): Result<WearableMetrics>
       suspend fun isWearableAvailable(): Boolean
   }
   ```

3. Create stub implementation `data/caddy/wearable/StubWearableSyncService.kt`:
   ```kotlin
   @Singleton
   class StubWearableSyncService @Inject constructor() : WearableSyncService {
       override suspend fun syncMetrics(): Result<WearableMetrics> {
           // Return placeholder metrics for MVP
           return Result.success(WearableMetrics(
               hrvMs = 45.0,
               sleepMinutes = 420,
               sleepQualityScore = 75.0,
               stressScore = 30.0,
               timestamp = System.currentTimeMillis()
           ))
       }

       override suspend fun isWearableAvailable() = false  // MVP: always manual entry
   }
   ```

4. Bind in `di/CaddyDomainModule.kt`

#### Verification:
- [ ] ReadinessCalculator produces scores in 0-100 range
- [ ] Missing metrics default to 50 (neutral)
- [ ] adjustmentFactor() in ReadinessScore maps correctly (60+ → 1.0, <40 → 0.5, linear between)
- [ ] StubWearableSyncService returns plausible data

---

### Task 7: Implement Weather Conditions Adjustment Calculator

**Spec reference**: R2 (Forecaster HUD)
**Acceptance criteria**: A1 (conditions adjustment applied to carry)
**Dependencies**: Task 2 (weather models), Task 5 (weather API)

#### Implementation steps:
1. Create `domain/caddy/services/ConditionsCalculator.kt`:
   ```kotlin
   @Singleton
   class ConditionsCalculator @Inject constructor() {

       fun calculateAdjustment(
           weather: WeatherData,
           targetBearing: Int,
           baseCarryMeters: Int
       ): ConditionsAdjustment {
           val airDensity = airDensityFactor(weather.temperatureCelsius, weather.humidity)
           val wind = weather.windComponent(targetBearing)

           // Simplified model (acceptable for MVP per spec)
           val tempEffect = temperatureCarryEffect(weather.temperatureCelsius)
           val windEffect = windCarryEffect(wind.headwind)

           val totalModifier = airDensity * tempEffect * windEffect
           val adjustedCarry = (baseCarryMeters * totalModifier).toInt()

           return ConditionsAdjustment(
               carryModifier = totalModifier,
               reason = buildReasonString(weather, wind, adjustedCarry - baseCarryMeters)
           )
       }

       private fun airDensityFactor(tempC: Double, humidity: Int): Double {
           // Simplified: 1.0 at 15°C, increases at cold temps
           return 1.0 + ((15.0 - tempC) * 0.002)
       }

       private fun temperatureCarryEffect(tempC: Double): Double {
           // Cold air = denser = less carry (simplified)
           return 1.0 - ((15.0 - tempC) * 0.005)
       }

       private fun windCarryEffect(headwindMps: Double): Double {
           // Headwind (negative) reduces carry, tailwind (positive) increases
           return 1.0 + (headwindMps * 0.01)
       }

       private fun buildReasonString(weather: WeatherData, wind: WindComponent, carryDiff: Int): String
   }

   fun WeatherData.windComponent(targetBearing: Int): WindComponent {
       val windRad = Math.toRadians(windDegrees.toDouble())
       val targetRad = Math.toRadians(targetBearing.toDouble())
       val relativeAngle = windRad - targetRad

       val headwind = windSpeedMps * cos(relativeAngle)
       val crosswind = windSpeedMps * sin(relativeAngle)

       return WindComponent(headwind, crosswind)
   }
   ```

2. Add unit tests in `test/.../ConditionsCalculatorTest.kt`:
   - Test headwind reduces carry
   - Test cold temperature reduces carry
   - Test tailwind increases carry

#### Verification:
- [ ] Headwind of 5 m/s reduces carry by ~5%
- [ ] Temperature at 0°C reduces carry by ~7.5% vs 15°C
- [ ] Tailwind of 5 m/s increases carry by ~5%
- [ ] Reason string is human-readable (e.g., "Cold (+5°C) and headwind (-3 m/s): -8m carry")

---

### Task 8: Implement PinSeeker Strategy Engine

**Spec reference**: R4 (PinSeeker AI Map)
**Acceptance criteria**: A3 (Hazard-aware landing zone)
**Dependencies**: Task 4 (hole models), Task 3 (readiness models)

#### Implementation steps:
1. Create `domain/caddy/services/PinSeekerEngine.kt`:
   ```kotlin
   @Singleton
   class PinSeekerEngine @Inject constructor(
       private val navCaddyRepository: NavCaddyRepository
   ) {

       suspend fun computeStrategy(
           hole: CourseHole,
           handicap: Int,
           readinessScore: ReadinessScore
       ): HoleStrategy {
           // Get dominant miss bias from repository
           val missPatterns = navCaddyRepository.getMissPatterns()
           val dominantMiss = missPatterns
               .maxByOrNull { it.decayedConfidence(System.currentTimeMillis()) }
               ?.direction ?: MissDirection.STRAIGHT

           // Get club distances from active bag
           val activeBag = navCaddyRepository.getActiveBagProfile()
           val clubDistances = activeBag?.clubs?.associate { it.name to it.estimatedCarryMeters } ?: emptyMap()

           // Identify danger zones for this player's miss
           val personalizedHazards = hole.hazards.filter { hazard ->
               hazard.affectedMisses.contains(dominantMiss)
           }

           // Compute safe landing zone
           val safetyMargin = computeSafetyMargin(readinessScore, handicap)
           val landingZone = computeLandingZone(hole, personalizedHazards, safetyMargin, clubDistances)

           // Generate risk callouts
           val riskCallouts = generateRiskCallouts(dominantMiss, personalizedHazards)

           return HoleStrategy(
               holeNumber = hole.number,
               dangerZones = personalizedHazards,
               recommendedLandingZone = landingZone,
               riskCallouts = riskCallouts,
               personalizedFor = PersonalizationContext(
                   handicap = handicap,
                   dominantMiss = dominantMiss,
                   clubDistances = clubDistances,
                   readinessScore = readinessScore.overall
               )
           )
       }

       private fun computeSafetyMargin(readiness: ReadinessScore, handicap: Int): Int {
           // Low readiness → bigger margins
           val readinessFactor = readiness.adjustmentFactor()  // 0.5-1.0
           val handicapMargin = handicap * 2  // ~18m for 9 handicap
           return (handicapMargin / readinessFactor).toInt()
       }

       private fun computeLandingZone(
           hole: CourseHole,
           hazards: List<HazardZone>,
           safetyMargin: Int,
           clubDistances: Map<String, Int>
       ): LandingZone

       private fun generateRiskCallouts(
           dominantMiss: MissDirection,
           hazards: List<HazardZone>
       ): List<String>
   }
   ```

2. Add unit tests for strategy personalization:
   - Test low readiness increases safety margin
   - Test dominant slice avoids right-side hazards
   - Test higher handicap gets more conservative targets

#### Verification:
- [ ] Low readiness (score <40) doubles safety margin
- [ ] Dominant slice prioritizes avoiding right-side hazards
- [ ] Strategy includes 1-3 risk callouts
- [ ] Landing zone visual cue is actionable (e.g., "Aim at left edge of fairway")

---

### Task 9: Create Live Caddy Use Cases

**Spec reference**: R1-R6 (all functional requirements)
**Acceptance criteria**: A1-A4 (indirect - use cases coordinate services)
**Dependencies**: Tasks 6, 7, 8 (domain services)

#### Implementation steps:
1. Create `domain/caddy/usecases/GetLiveCaddyContextUseCase.kt`:
   ```kotlin
   @Singleton
   class GetLiveCaddyContextUseCase @Inject constructor(
       private val sessionContextManager: SessionContextManager,
       private val weatherRepository: WeatherRepository,
       private val readinessRepository: ReadinessRepository,
       private val pinSeekerEngine: PinSeekerEngine
   ) {
       suspend operator fun invoke(): Result<LiveCaddyContext> {
           val session = sessionContextManager.getSession().firstOrNull()
               ?: return Result.failure(Exception("No active round"))

           val weather = weatherRepository.getCurrentWeather(session.location)
               .getOrNull()

           val readiness = readinessRepository.getMostRecent()
               ?: ReadinessScore.default()

           val currentHole = getCurrentHole(session.roundState)
           val strategy = currentHole?.let { hole ->
               pinSeekerEngine.computeStrategy(hole, session.handicap, readiness)
           }

           return Result.success(LiveCaddyContext(
               roundState = session.roundState,
               weather = weather,
               readiness = readiness,
               holeStrategy = strategy
           ))
       }
   }

   data class LiveCaddyContext(
       val roundState: RoundState,
       val weather: WeatherData?,
       val readiness: ReadinessScore,
       val holeStrategy: HoleStrategy?
   )
   ```

2. Create `domain/caddy/usecases/LogShotUseCase.kt`:
   ```kotlin
   @Singleton
   class LogShotUseCase @Inject constructor(
       private val navCaddyRepository: NavCaddyRepository,
       private val sessionContextManager: SessionContextManager,
       private val analytics: NavCaddyAnalytics
   ) {
       suspend operator fun invoke(
           club: Club,
           result: ShotResult
       ): Result<Unit> {
           val session = sessionContextManager.getSession().firstOrNull()
               ?: return Result.failure(Exception("No active round"))

           val shot = Shot(
               clubId = club.id,
               clubName = club.name,
               clubType = club.type,
               lie = result.lie,
               missDirection = result.missDirection,
               timestamp = System.currentTimeMillis(),
               holeNumber = session.roundState.currentHole,
               pressureContext = null  // Inferred by downstream analytics
           )

           navCaddyRepository.recordShot(shot)

           analytics.trackEvent(AnalyticsEvent.ShotLogged(
               clubType = club.type.name,
               lie = result.lie.name,
               latencyMs = 0  // Instant logging
           ))

           return Result.success(Unit)
       }
   }

   data class ShotResult(
       val lie: Lie,
       val missDirection: MissDirection?
   )
   ```

3. Create `domain/caddy/usecases/StartRoundUseCase.kt`, `EndRoundUseCase.kt`, `UpdateReadinessUseCase.kt`

#### Verification:
- [ ] GetLiveCaddyContextUseCase aggregates all required data
- [ ] LogShotUseCase writes to repository and analytics
- [ ] Use cases handle missing session gracefully
- [ ] All use cases are @Singleton and Hilt-injectable

---

### Task 10: Create Readiness Repository

**Spec reference**: R3 (BodyCaddy)
**Acceptance criteria**: A2 (Readiness impacts strategy)
**Dependencies**: Task 3 (readiness models), Task 6 (readiness calculator)

#### Implementation steps:
1. Update `data/caddy/NavCaddyDatabase.kt` to include ReadinessScoreEntity (v3):
   ```kotlin
   @Database(
       entities = [
           // ... existing entities
           ReadinessScoreEntity::class
       ],
       version = 3,
       exportSchema = true
   )
   ```

2. Add migration from v2 → v3:
   ```kotlin
   val MIGRATION_2_3 = object : Migration(2, 3) {
       override fun migrate(database: SupportSQLiteDatabase) {
           database.execSQL("""
               CREATE TABLE IF NOT EXISTS readiness_scores (
                   timestamp INTEGER PRIMARY KEY NOT NULL,
                   overall INTEGER NOT NULL,
                   hrvScore REAL,
                   sleepScore REAL,
                   stressScore REAL,
                   source TEXT NOT NULL
               )
           """)
       }
   }
   ```

3. Create `data/caddy/repository/ReadinessRepository.kt`:
   ```kotlin
   interface ReadinessRepository {
       suspend fun saveReadiness(score: ReadinessScore)
       suspend fun getMostRecent(): ReadinessScore?
       suspend fun getHistory(days: Int): List<ReadinessScore>
   }
   ```

4. Implement `data/caddy/repository/ReadinessRepositoryImpl.kt` with DAO + mapping

5. Bind in `di/CaddyDataModule.kt`

#### Verification:
- [ ] Database migration v2→v3 runs successfully
- [ ] ReadinessRepository saves and retrieves scores
- [ ] getMostRecent() returns null if no data
- [ ] Mapping preserves all fields (overall, breakdown, source)

---

### Task 11: Create Round Management Use Cases

**Spec reference**: R1 (Live Round Context)
**Acceptance criteria**: A4 (persistence during backgrounding)
**Dependencies**: Existing SessionContextManager

#### Implementation steps:
1. Create `domain/caddy/usecases/StartRoundUseCase.kt`:
   ```kotlin
   @Singleton
   class StartRoundUseCase @Inject constructor(
       private val sessionContextManager: SessionContextManager,
       private val navCaddyRepository: NavCaddyRepository,
       private val analytics: NavCaddyAnalytics
   ) {
       suspend operator fun invoke(
           courseName: String,
           teePosition: String,
           initialLocation: Location
       ): Result<RoundState> {
           val round = Round(
               startTime = System.currentTimeMillis(),
               courseName = courseName,
               scores = IntArray(18) { 0 }  // Initialize with zeros
           )

           val roundState = RoundState(
               round = round,
               currentHole = 1,
               currentPar = 4,  // Default, update from course data
               totalScore = 0,
               holesCompleted = 0,
               courseConditions = CourseConditions(
                   weather = null,  // Fetched separately
                   windSpeed = 0.0,
                   windDirection = 0,
                   temperature = 20.0
               )
           )

           sessionContextManager.updateRound(roundState)

           analytics.trackEvent(AnalyticsEvent.RoundStarted(
               courseName = courseName,
               timestamp = round.startTime
           ))

           return Result.success(roundState)
       }
   }
   ```

2. Create `domain/caddy/usecases/EndRoundUseCase.kt`:
   - Saves final round to repository
   - Clears session context
   - Triggers analytics event

3. Create `domain/caddy/usecases/AdvanceHoleUseCase.kt`:
   - Updates currentHole in RoundState
   - Clears hole-specific context
   - Persists via SessionContextManager

#### Verification:
- [ ] StartRoundUseCase creates valid RoundState
- [ ] EndRoundUseCase persists round and clears session
- [ ] AdvanceHoleUseCase increments hole and updates par
- [ ] Session survives app backgrounding (SessionContextManager already handles persistence)

---

### Task 12: Add Low Distraction Mode Settings

**Spec reference**: R7 (Safety and Distraction Controls)
**Acceptance criteria**: N/A (UX requirement, no specific AC)
**Dependencies**: None

#### Implementation steps:
1. Create `domain/caddy/models/LiveCaddySettings.kt`:
   ```kotlin
   data class LiveCaddySettings(
       val lowDistractionMode: Boolean = false,
       val autoLockPrevention: Boolean = false,  // Android: FLAG_KEEP_SCREEN_ON
       val largeTouchTargets: Boolean = false,   // Min 48dp per Material 3
       val reducedAnimations: Boolean = false,
       val hapticFeedback: Boolean = true
   )
   ```

2. Create `data/caddy/local/LiveCaddySettingsDataStore.kt`:
   ```kotlin
   @Singleton
   class LiveCaddySettingsDataStore @Inject constructor(
       private val dataStore: DataStore<Preferences>
   ) {
       suspend fun saveSettings(settings: LiveCaddySettings)
       fun getSettings(): Flow<LiveCaddySettings>
   }
   ```

3. Add DataStore preference keys:
   ```kotlin
   private object PreferencesKeys {
       val LOW_DISTRACTION_MODE = booleanPreferencesKey("low_distraction_mode")
       val AUTO_LOCK_PREVENTION = booleanPreferencesKey("auto_lock_prevention")
       val LARGE_TOUCH_TARGETS = booleanPreferencesKey("large_touch_targets")
       val REDUCED_ANIMATIONS = booleanPreferencesKey("reduced_animations")
       val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
   }
   ```

4. Provide in `di/CaddyDataModule.kt`

#### Verification:
- [ ] Settings persist across app restarts
- [ ] Default values match spec (haptics on, others off)
- [ ] Flow-based settings updates work reactively
- [ ] DataStore uses correct scope (application-level)

---

## PHASE 3: UI COMPONENTS & HUD

### Task 13: Create LiveCaddyViewModel & State

**Spec reference**: R1-R7 (all requirements)
**Acceptance criteria**: A1-A4 (all)
**Dependencies**: Task 9 (use cases), Task 12 (settings)

#### Implementation steps:
1. Create `ui/caddy/LiveCaddyState.kt`:
   ```kotlin
   data class LiveCaddyState(
       val roundState: RoundState? = null,
       val weather: WeatherData? = null,
       val readiness: ReadinessScore = ReadinessScore.default(),
       val holeStrategy: HoleStrategy? = null,
       val isLoading: Boolean = true,
       val error: String? = null,
       val settings: LiveCaddySettings = LiveCaddySettings(),

       // Shot logger state
       val selectedClub: Club? = null,
       val isShotLoggerVisible: Boolean = false,
       val lastShotConfirmed: Boolean = false,

       // HUD visibility
       val isWeatherHudExpanded: Boolean = false,
       val isReadinessDetailsVisible: Boolean = false,
       val isStrategyMapVisible: Boolean = false
   )

   sealed interface LiveCaddyAction {
       data object LoadContext : LiveCaddyAction
       data object RefreshWeather : LiveCaddyAction
       data object RefreshReadiness : LiveCaddyAction

       data class SelectClub(val club: Club) : LiveCaddyAction
       data class LogShot(val result: ShotResult) : LiveCaddyAction
       data object DismissShotLogger : LiveCaddyAction

       data class AdvanceHole(val holeNumber: Int) : LiveCaddyAction
       data object EndRound : LiveCaddyAction

       data class ToggleWeatherHud(val expanded: Boolean) : LiveCaddyAction
       data class ToggleReadinessDetails(val visible: Boolean) : LiveCaddyAction
       data class ToggleStrategyMap(val visible: Boolean) : LiveCaddyAction

       data class UpdateSettings(val settings: LiveCaddySettings) : LiveCaddyAction
   }
   ```

2. Create `ui/caddy/LiveCaddyViewModel.kt`:
   ```kotlin
   @HiltViewModel
   class LiveCaddyViewModel @Inject constructor(
       private val getLiveCaddyContext: GetLiveCaddyContextUseCase,
       private val logShot: LogShotUseCase,
       private val endRound: EndRoundUseCase,
       private val settingsDataStore: LiveCaddySettingsDataStore,
       private val analytics: NavCaddyAnalytics
   ) : ViewModel() {

       private val _uiState = MutableStateFlow(LiveCaddyState())
       val uiState: StateFlow<LiveCaddyState> = _uiState.asStateFlow()

       init {
           loadSettings()
           loadContext()
       }

       fun onAction(action: LiveCaddyAction) {
           when (action) {
               LiveCaddyAction.LoadContext -> loadContext()
               LiveCaddyAction.RefreshWeather -> refreshWeather()
               is LiveCaddyAction.LogShot -> logShotAction(action.result)
               // ... handle all actions
           }
       }

       private fun loadContext() {
           viewModelScope.launch {
               _uiState.update { it.copy(isLoading = true) }

               getLiveCaddyContext()
                   .onSuccess { context ->
                       _uiState.update {
                           it.copy(
                               roundState = context.roundState,
                               weather = context.weather,
                               readiness = context.readiness,
                               holeStrategy = context.holeStrategy,
                               isLoading = false,
                               error = null
                           )
                       }
                   }
                   .onFailure { e ->
                       _uiState.update {
                           it.copy(isLoading = false, error = e.message)
                       }
                   }
           }
       }

       private fun logShotAction(result: ShotResult) {
           // Implementation in next step
       }
   }
   ```

3. Add state update methods for all actions

#### Verification:
- [ ] LiveCaddyState includes all required fields
- [ ] LiveCaddyViewModel loads context on init
- [ ] State updates preserve immutability
- [ ] All actions have handlers
- [ ] Settings are loaded reactively from DataStore

---

### Task 14: Create Forecaster HUD Component

**Spec reference**: R2 (Forecaster HUD)
**Acceptance criteria**: A1 (Weather HUD renders within 2 seconds)
**Dependencies**: Task 13 (ViewModel)

#### Implementation steps:
1. Create `ui/caddy/components/ForecasterHud.kt`:
   ```kotlin
   @Composable
   fun ForecasterHud(
       weather: WeatherData?,
       isExpanded: Boolean,
       onToggleExpanded: (Boolean) -> Unit,
       modifier: Modifier = Modifier
   ) {
       Card(
           modifier = modifier
               .fillMaxWidth()
               .clickable { onToggleExpanded(!isExpanded) },
           colors = CardDefaults.cardColors(
               containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
           )
       ) {
           if (weather == null) {
               LoadingWeatherIndicator()
           } else {
               WeatherContent(weather, isExpanded)
           }
       }
   }

   @Composable
   private fun WeatherContent(weather: WeatherData, isExpanded: Boolean) {
       Column(modifier = Modifier.padding(16.dp)) {
           // Compact view (always visible)
           Row(
               modifier = Modifier.fillMaxWidth(),
               horizontalArrangement = Arrangement.SpaceBetween
           ) {
               WindIndicator(
                   speedMps = weather.windSpeedMps,
                   degrees = weather.windDegrees
               )

               TemperatureIndicator(
                   celsius = weather.temperatureCelsius,
                   humidity = weather.humidity
               )

               ConditionsChip(
                   adjustment = weather.conditionsAdjustment()
               )
           }

           // Expanded details
           AnimatedVisibility(visible = isExpanded) {
               Column(modifier = Modifier.padding(top = 8.dp)) {
                   Text(
                       text = "Conditions Impact",
                       style = MaterialTheme.typography.labelMedium
                   )
                   Text(
                       text = weather.conditionsAdjustment().reason,
                       style = MaterialTheme.typography.bodySmall
                   )
               }
           }
       }
   }

   @Composable
   private fun WindIndicator(speedMps: Double, degrees: Int) {
       Row(verticalAlignment = Alignment.CenterVertically) {
           Icon(
               imageVector = Icons.Default.Air,  // Wind icon
               contentDescription = "Wind",
               modifier = Modifier.rotate(degrees.toFloat())
           )
           Spacer(modifier = Modifier.width(4.dp))
           Text(
               text = "${speedMps.toInt()} m/s",
               style = MaterialTheme.typography.bodyLarge,
               fontWeight = FontWeight.Bold
           )
       }
   }
   ```

2. Create high-contrast outdoor theme variant in `ui/theme/OutdoorTheme.kt`:
   - Increase contrast ratios for sunlight visibility
   - Larger minimum font sizes (16sp → 18sp)
   - Thicker borders and outlines

3. Add preview composables for light/dark/outdoor themes

#### Verification:
- [ ] ForecasterHud renders within 2 seconds (measured via analytics)
- [ ] Wind direction arrow rotates correctly (0° = North)
- [ ] Conditions chip shows carry adjustment (e.g., "-5m")
- [ ] Expanded view shows human-readable reason
- [ ] High contrast mode meets WCAG AAA for outdoor visibility

---

### Task 15: Create Readiness Indicator Component

**Spec reference**: R3 (BodyCaddy)
**Acceptance criteria**: A2 (Readiness impacts strategy)
**Dependencies**: Task 13 (ViewModel)

#### implementation steps:
1. Create `ui/caddy/components/ReadinessIndicator.kt`:
   ```kotlin
   @Composable
   fun ReadinessIndicator(
       readiness: ReadinessScore,
       showDetails: Boolean,
       onToggleDetails: (Boolean) -> Unit,
       modifier: Modifier = Modifier
   ) {
       Card(
           modifier = modifier.clickable { onToggleDetails(!showDetails) },
           colors = CardDefaults.cardColors(
               containerColor = readinessColor(readiness.overall)
           )
       ) {
           Column(
               modifier = Modifier.padding(12.dp),
               horizontalAlignment = Alignment.CenterHorizontally
           ) {
               // Circular progress indicator
               Box(contentAlignment = Alignment.Center) {
                   CircularProgressIndicator(
                       progress = readiness.overall / 100f,
                       modifier = Modifier.size(64.dp),
                       strokeWidth = 6.dp,
                       color = MaterialTheme.colorScheme.primary
                   )
                   Text(
                       text = "${readiness.overall}",
                       style = MaterialTheme.typography.headlineMedium,
                       fontWeight = FontWeight.Bold
                   )
               }

               Text(
                   text = "Readiness",
                   style = MaterialTheme.typography.labelSmall,
                   modifier = Modifier.padding(top = 4.dp)
               )

               // Breakdown details
               AnimatedVisibility(visible = showDetails) {
                   ReadinessBreakdown(readiness.breakdown)
               }
           }
       }
   }

   @Composable
   private fun ReadinessBreakdown(breakdown: ReadinessBreakdown) {
       Column(modifier = Modifier.padding(top = 8.dp)) {
           breakdown.hrv?.let { MetricRow("HRV", it) }
           breakdown.sleepQuality?.let { MetricRow("Sleep", it) }
           breakdown.stressLevel?.let { MetricRow("Stress", it) }
       }
   }

   @Composable
   private fun MetricRow(label: String, metric: MetricScore) {
       Row(
           modifier = Modifier.fillMaxWidth(),
           horizontalArrangement = Arrangement.SpaceBetween
       ) {
           Text(text = label, style = MaterialTheme.typography.bodySmall)
           Text(
               text = "${metric.value.toInt()}/100",
               style = MaterialTheme.typography.bodySmall,
               fontWeight = FontWeight.Medium
           )
       }
   }

   private fun readinessColor(score: Int): Color {
       return when {
           score >= 70 -> Color(0xFF4CAF50)  // Green
           score >= 40 -> Color(0xFFFFA726)  // Orange
           else -> Color(0xFFEF5350)          // Red
       }
   }
   ```

2. Add manual readiness override dialog:
   ```kotlin
   @Composable
   fun ManualReadinessDialog(
       currentScore: Int,
       onDismiss: () -> Unit,
       onConfirm: (Int) -> Unit
   ) {
       var sliderValue by remember { mutableStateOf(currentScore.toFloat()) }

       AlertDialog(
           onDismissRequest = onDismiss,
           title = { Text("Set Readiness Manually") },
           text = {
               Column {
                   Text("Override wearable data if unavailable")
                   Slider(
                       value = sliderValue,
                       onValueChange = { sliderValue = it },
                       valueRange = 0f..100f,
                       steps = 19  // 5-point increments
                   )
                   Text("Score: ${sliderValue.toInt()}/100")
               }
           },
           confirmButton = {
               TextButton(onClick = { onConfirm(sliderValue.toInt()) }) {
                   Text("Confirm")
               }
           },
           dismissButton = {
               TextButton(onClick = onDismiss) {
                   Text("Cancel")
               }
           }
       )
   }
   ```

#### Verification:
- [ ] Readiness indicator shows score 0-100 with color coding
- [ ] Breakdown shows HRV, Sleep, Stress with weights
- [ ] Manual override dialog allows 0-100 selection
- [ ] Low readiness (<60) displays warning color
- [ ] Transparency of contributors is clear (per spec R3)

---

### Task 16: Create PinSeeker Strategy Map Component

**Spec reference**: R4 (PinSeeker AI Map)
**Acceptance criteria**: A3 (Hazard-aware landing zone)
**Dependencies**: Task 13 (ViewModel), Task 8 (strategy engine)

#### Implementation steps:
1. Create `ui/caddy/components/PinSeekerMap.kt`:
   ```kotlin
   @Composable
   fun PinSeekerMap(
       strategy: HoleStrategy?,
       modifier: Modifier = Modifier
   ) {
       if (strategy == null) {
           EmptyStrategyPlaceholder(modifier)
           return
       }

       Column(modifier = modifier.padding(16.dp)) {
           // Hole header
           HoleHeader(
               holeNumber = strategy.holeNumber,
               personalizedFor = strategy.personalizedFor
           )

           Spacer(modifier = Modifier.height(16.dp))

           // Simplified overhead view (MVP: no course rendering, just zones)
           HazardZonesList(strategy.dangerZones)

           Spacer(modifier = Modifier.height(12.dp))

           // Recommended landing zone
           LandingZoneCard(strategy.recommendedLandingZone)

           Spacer(modifier = Modifier.height(12.dp))

           // Risk callouts
           RiskCallouts(strategy.riskCallouts)
       }
   }

   @Composable
   private fun HazardZonesList(zones: List<HazardZone>) {
       Card(
           colors = CardDefaults.cardColors(
               containerColor = MaterialTheme.colorScheme.errorContainer
           )
       ) {
           Column(modifier = Modifier.padding(12.dp)) {
               Text(
                   text = "Danger Zones",
                   style = MaterialTheme.typography.titleSmall,
                   fontWeight = FontWeight.Bold
               )

               zones.forEach { zone ->
                   HazardRow(zone)
               }
           }
       }
   }

   @Composable
   private fun HazardRow(zone: HazardZone) {
       Row(
           modifier = Modifier
               .fillMaxWidth()
               .padding(vertical = 4.dp),
           horizontalArrangement = Arrangement.SpaceBetween
       ) {
           Row {
               Icon(
                   imageVector = hazardIcon(zone.type),
                   contentDescription = zone.type.name,
                   tint = MaterialTheme.colorScheme.error
               )
               Spacer(modifier = Modifier.width(8.dp))
               Text(
                   text = "${zone.type.name} ${zone.location.side}",
                   style = MaterialTheme.typography.bodyMedium
               )
           }

           Text(
               text = "${zone.location.distanceFromTee.first}-${zone.location.distanceFromTee.last}m",
               style = MaterialTheme.typography.bodySmall
           )
       }
   }

   @Composable
   private fun LandingZoneCard(zone: LandingZone) {
       Card(
           colors = CardDefaults.cardColors(
               containerColor = MaterialTheme.colorScheme.primaryContainer
           )
       ) {
           Column(modifier = Modifier.padding(12.dp)) {
               Text(
                   text = "Recommended Target",
                   style = MaterialTheme.typography.titleSmall,
                   fontWeight = FontWeight.Bold
               )

               Text(
                   text = zone.visualCue,
                   style = MaterialTheme.typography.bodyLarge,
                   modifier = Modifier.padding(vertical = 8.dp)
               )

               Row(
                   modifier = Modifier.fillMaxWidth(),
                   horizontalArrangement = Arrangement.SpaceBetween
               ) {
                   Text(
                       text = "Distance: ${zone.idealDistance}m",
                       style = MaterialTheme.typography.bodySmall
                   )
                   Text(
                       text = "Margin: ±${zone.safetyMargin}m",
                       style = MaterialTheme.typography.bodySmall
                   )
               }
           }
       }
   }

   @Composable
   private fun RiskCallouts(callouts: List<String>) {
       callouts.forEach { callout ->
           Row(
               modifier = Modifier.padding(vertical = 4.dp),
               verticalAlignment = Alignment.CenterVertically
           ) {
               Icon(
                   imageVector = Icons.Default.Warning,
                   contentDescription = "Risk",
                   tint = MaterialTheme.colorScheme.error,
                   modifier = Modifier.size(16.dp)
               )
               Spacer(modifier = Modifier.width(8.dp))
               Text(
                   text = callout,
                   style = MaterialTheme.typography.bodySmall,
                   color = MaterialTheme.colorScheme.onSurface
               )
           }
       }
   }
   ```

2. Add hazard icon mapping:
   ```kotlin
   private fun hazardIcon(type: HazardType): ImageVector {
       return when (type) {
           HazardType.WATER -> Icons.Default.Water
           HazardType.OB -> Icons.Default.Block
           HazardType.BUNKER -> Icons.Default.Terrain
           HazardType.PENALTY_ROUGH -> Icons.Default.Grass
           HazardType.TREES -> Icons.Default.Park
       }
   }
   ```

#### Verification:
- [ ] Hazard zones list shows all danger zones
- [ ] Landing zone card displays visual cue prominently
- [ ] Risk callouts reference player's dominant miss (e.g., "Right miss brings water into play")
- [ ] Personalization context is visible (handicap, miss bias)
- [ ] Large touch targets per spec R7 (min 48dp)

---

### Task 17: Create Shot Logger Component

**Spec reference**: R6 (Real-Time Shot Logger)
**Acceptance criteria**: A4 (Shot logger speed and persistence)
**Dependencies**: Task 13 (ViewModel)

#### Implementation steps:
1. Create `ui/caddy/components/ShotLogger.kt`:
   ```kotlin
   @Composable
   fun ShotLogger(
       clubs: List<Club>,
       selectedClub: Club?,
       onClubSelected: (Club) -> Unit,
       onShotLogged: (ShotResult) -> Unit,
       modifier: Modifier = Modifier
   ) {
       Column(modifier = modifier.padding(16.dp)) {
           Text(
               text = "Log Shot",
               style = MaterialTheme.typography.titleLarge,
               fontWeight = FontWeight.Bold
           )

           Spacer(modifier = Modifier.height(16.dp))

           // Step 1: Select club (grid layout, large targets)
           Text(
               text = "1. Select Club",
               style = MaterialTheme.typography.titleSmall
           )

           LazyVerticalGrid(
               columns = GridCells.Fixed(4),
               modifier = Modifier.padding(vertical = 8.dp),
               horizontalArrangement = Arrangement.spacedBy(8.dp),
               verticalArrangement = Arrangement.spacedBy(8.dp)
           ) {
               items(clubs) { club ->
                   ClubChip(
                       club = club,
                       isSelected = club == selectedClub,
                       onClick = { onClubSelected(club) }
                   )
               }
           }

           Spacer(modifier = Modifier.height(16.dp))

           // Step 2: Select result (only visible if club selected)
           AnimatedVisibility(visible = selectedClub != null) {
               Column {
                   Text(
                       text = "2. Select Result",
                       style = MaterialTheme.typography.titleSmall
                   )

                   ShotResultGrid(
                       onResultSelected = { result ->
                           onShotLogged(result)
                       }
                   )
               }
           }
       }
   }

   @Composable
   private fun ClubChip(
       club: Club,
       isSelected: Boolean,
       onClick: () -> Unit
   ) {
       FilterChip(
           selected = isSelected,
           onClick = onClick,
           label = {
               Text(
                   text = club.name,
                   style = MaterialTheme.typography.bodyLarge,
                   fontWeight = FontWeight.Bold
               )
           },
           modifier = Modifier
               .height(56.dp)  // Large touch target
               .fillMaxWidth()
       )
   }

   @Composable
   private fun ShotResultGrid(onResultSelected: (ShotResult) -> Unit) {
       val results = listOf(
           ShotResult(Lie.FAIRWAY, MissDirection.STRAIGHT),
           ShotResult(Lie.ROUGH, MissDirection.PUSH),
           ShotResult(Lie.ROUGH, MissDirection.PULL),
           ShotResult(Lie.BUNKER, null),
           ShotResult(Lie.HAZARD, null),  // Water
           ShotResult(Lie.GREEN, MissDirection.STRAIGHT)
           // Add more variations with long/short modifiers
       )

       LazyVerticalGrid(
           columns = GridCells.Fixed(3),
           modifier = Modifier.padding(vertical = 8.dp),
           horizontalArrangement = Arrangement.spacedBy(8.dp),
           verticalArrangement = Arrangement.spacedBy(8.dp)
       ) {
           items(results) { result ->
               ResultButton(
                   result = result,
                   onClick = { onResultSelected(result) }
               )
           }
       }
   }

   @Composable
   private fun ResultButton(result: ShotResult, onClick: () -> Unit) {
       Button(
           onClick = onClick,
           modifier = Modifier
               .height(64.dp)  // Extra large for one-second tap
               .fillMaxWidth(),
           colors = ButtonDefaults.buttonColors(
               containerColor = lieColor(result.lie)
           )
       ) {
           Column(horizontalAlignment = Alignment.CenterHorizontally) {
               Text(
                   text = result.lie.name,
                   style = MaterialTheme.typography.labelSmall,
                   fontWeight = FontWeight.Bold
               )
               result.missDirection?.let {
                   Text(
                       text = it.name,
                       style = MaterialTheme.typography.labelSmall
                   )
               }
           }
       }
   }

   private fun lieColor(lie: Lie): Color {
       return when (lie) {
           Lie.FAIRWAY -> Color(0xFF4CAF50)
           Lie.GREEN -> Color(0xFF2196F3)
           Lie.ROUGH -> Color(0xFFFFA726)
           Lie.BUNKER -> Color(0xFF8D6E63)
           Lie.HAZARD -> Color(0xFFEF5350)
           else -> Color.Gray
       }
   }
   ```

2. Add haptic feedback to shot logging:
   ```kotlin
   @Composable
   fun rememberHapticFeedback(): () -> Unit {
       val view = LocalView.current
       return remember {
           {
               view.performHapticFeedback(
                   HapticFeedbackConstants.CONFIRM,
                   HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
               )
           }
       }
   }
   ```

3. Update LiveCaddyViewModel to trigger haptics on shot log success

#### Verification:
- [ ] Club selection grid has min 48dp touch targets
- [ ] Shot result buttons are 64dp (extra large for speed)
- [ ] Haptic feedback fires on shot save (per spec R6)
- [ ] Flow takes <2 seconds (club tap → result tap → haptic confirmation)
- [ ] Color coding is intuitive (green=good, red=bad)

---

### Task 18: Create Live Caddy Screen (Main HUD)

**Spec reference**: R1-R7 (all)
**Acceptance criteria**: A1-A4 (all)
**Dependencies**: Tasks 13-17 (ViewModel + all components)

#### Implementation steps:
1. Create `ui/caddy/LiveCaddyScreen.kt`:
   ```kotlin
   @Composable
   fun LiveCaddyScreen(
       viewModel: LiveCaddyViewModel = hiltViewModel(),
       onNavigateBack: () -> Unit
   ) {
       val state by viewModel.uiState.collectAsStateWithLifecycle()
       val hapticFeedback = rememberHapticFeedback()

       // Apply window flags for low distraction mode
       if (state.settings.autoLockPrevention) {
           KeepScreenOn()
       }

       Scaffold(
           topBar = {
               LiveCaddyTopBar(
                   roundState = state.roundState,
                   onNavigateBack = onNavigateBack,
                   onEndRound = { viewModel.onAction(LiveCaddyAction.EndRound) }
               )
           },
           floatingActionButton = {
               if (!state.isShotLoggerVisible) {
                   FloatingActionButton(
                       onClick = {
                           viewModel.onAction(LiveCaddyAction.ToggleShotLogger(true))
                       }
                   ) {
                       Icon(Icons.Default.Add, "Log Shot")
                   }
               }
           }
       ) { paddingValues ->
           if (state.isLoading) {
               LoadingView(modifier = Modifier.fillMaxSize())
           } else if (state.error != null) {
               ErrorView(
                   message = state.error ?: "Unknown error",
                   onRetry = { viewModel.onAction(LiveCaddyAction.LoadContext) }
               )
           } else {
               LiveCaddyContent(
                   state = state,
                   onAction = { action ->
                       viewModel.onAction(action)
                       if (action is LiveCaddyAction.LogShot) {
                           hapticFeedback()
                       }
                   },
                   modifier = Modifier.padding(paddingValues)
               )
           }
       }
   }

   @Composable
   private fun LiveCaddyContent(
       state: LiveCaddyState,
       onAction: (LiveCaddyAction) -> Unit,
       modifier: Modifier = Modifier
   ) {
       Column(
           modifier = modifier
               .fillMaxSize()
               .verticalScroll(rememberScrollState())
               .padding(16.dp)
       ) {
           // Forecaster HUD (always on top)
           ForecasterHud(
               weather = state.weather,
               isExpanded = state.isWeatherHudExpanded,
               onToggleExpanded = { onAction(LiveCaddyAction.ToggleWeatherHud(it)) }
           )

           Spacer(modifier = Modifier.height(16.dp))

           // Readiness indicator
           ReadinessIndicator(
               readiness = state.readiness,
               showDetails = state.isReadinessDetailsVisible,
               onToggleDetails = { onAction(LiveCaddyAction.ToggleReadinessDetails(it)) },
               modifier = Modifier.align(Alignment.CenterHorizontally)
           )

           Spacer(modifier = Modifier.height(16.dp))

           // PinSeeker strategy map
           if (state.isStrategyMapVisible) {
               PinSeekerMap(strategy = state.holeStrategy)
               Spacer(modifier = Modifier.height(16.dp))
           } else {
               TextButton(
                   onClick = { onAction(LiveCaddyAction.ToggleStrategyMap(true)) }
               ) {
                   Text("Show Hole Strategy")
               }
           }

           // Shot logger (modal bottom sheet)
           if (state.isShotLoggerVisible) {
               ModalBottomSheet(
                   onDismissRequest = { onAction(LiveCaddyAction.DismissShotLogger) }
               ) {
                   state.roundState?.let { roundState ->
                       val activeBag = /* get from state or repository */
                       ShotLogger(
                           clubs = activeBag?.clubs ?: emptyList(),
                           selectedClub = state.selectedClub,
                           onClubSelected = { club ->
                               onAction(LiveCaddyAction.SelectClub(club))
                           },
                           onShotLogged = { result ->
                               onAction(LiveCaddyAction.LogShot(result))
                           }
                       )
                   }
               }
           }
       }
   }

   @Composable
   private fun KeepScreenOn() {
       val context = LocalContext.current
       DisposableEffect(Unit) {
           val window = (context as? Activity)?.window
           window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

           onDispose {
               window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
           }
       }
   }
   ```

2. Add UX copy for distraction warning (per spec R7):
   ```kotlin
   @Composable
   private fun DistractionWarningBanner() {
       Card(
           colors = CardDefaults.cardColors(
               containerColor = MaterialTheme.colorScheme.tertiaryContainer
           ),
           modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
       ) {
           Row(
               modifier = Modifier.padding(12.dp),
               verticalAlignment = Alignment.CenterVertically
           ) {
               Icon(
                   imageVector = Icons.Default.Info,
                   contentDescription = "Info",
                   tint = MaterialTheme.colorScheme.tertiary
               )
               Spacer(modifier = Modifier.width(8.dp))
               Text(
                   text = "Keep phone away during swing. Review strategy between shots.",
                   style = MaterialTheme.typography.bodySmall
               )
           }
       }
   }
   ```

#### Verification:
- [ ] Screen loads within 2 seconds (per A1)
- [ ] All HUD components render correctly
- [ ] FAB opens shot logger modal
- [ ] Shot logging flow completes in <2 seconds
- [ ] Auto-lock prevention activates when setting enabled
- [ ] Distraction warning is visible on first use
- [ ] Offline mode degrades gracefully (shows cached data)

---

## PHASE 4: SHOT LOGGER & OFFLINE SYNC

### Task 19: Implement Offline-First Shot Logging

**Spec reference**: R6 (Real-Time Shot Logger)
**Acceptance criteria**: A4 (Shot logger speed and persistence)
**Dependencies**: Task 9 (LogShotUseCase)

#### Implementation steps:
1. Create `data/caddy/local/entity/PendingShotEntity.kt`:
   ```kotlin
   @Entity(tableName = "pending_shots")
   data class PendingShotEntity(
       @PrimaryKey(autoGenerate = true) val id: Long = 0,
       val clubId: String,
       val clubName: String,
       val clubType: String,
       val lie: String,
       val missDirection: String?,
       val timestamp: Long,
       val holeNumber: Int?,
       val syncStatus: String  // "PENDING", "SYNCING", "SYNCED", "FAILED"
   )
   ```

2. Create DAO:
   ```kotlin
   @Dao
   interface PendingShotDao {
       @Insert
       suspend fun insert(shot: PendingShotEntity): Long

       @Query("SELECT * FROM pending_shots WHERE syncStatus = 'PENDING' ORDER BY timestamp ASC")
       suspend fun getPendingShots(): List<PendingShotEntity>

       @Query("UPDATE pending_shots SET syncStatus = :status WHERE id = :shotId")
       suspend fun updateSyncStatus(shotId: Long, status: String)

       @Query("DELETE FROM pending_shots WHERE id = :shotId")
       suspend fun delete(shotId: Long)
   }
   ```

3. Create `domain/caddy/services/ShotSyncService.kt`:
   ```kotlin
   @Singleton
   class ShotSyncService @Inject constructor(
       private val pendingShotDao: PendingShotDao,
       private val navCaddyRepository: NavCaddyRepository,
       private val networkMonitor: NetworkMonitor,
       private val analytics: NavCaddyAnalytics
   ) {
       fun startAutoSync() {
           viewModelScope.launch {
               networkMonitor.isOnline.collectLatest { isOnline ->
                   if (isOnline) {
                       syncPendingShots()
                   }
               }
           }
       }

       suspend fun syncPendingShots() {
           val pending = pendingShotDao.getPendingShots()

           pending.forEach { shotEntity ->
               pendingShotDao.updateSyncStatus(shotEntity.id, "SYNCING")

               try {
                   val shot = shotEntity.toDomain()
                   navCaddyRepository.recordShot(shot)

                   pendingShotDao.delete(shotEntity.id)

                   analytics.trackEvent(AnalyticsEvent.ShotSynced(
                       shotId = shotEntity.id.toString(),
                       latencyMs = System.currentTimeMillis() - shotEntity.timestamp
                   ))
               } catch (e: Exception) {
                   pendingShotDao.updateSyncStatus(shotEntity.id, "FAILED")
               }
           }
       }
   }
   ```

4. Update LogShotUseCase to queue shots locally first:
   ```kotlin
   suspend operator fun invoke(club: Club, result: ShotResult): Result<Unit> {
       // Save locally immediately
       val pendingShot = PendingShotEntity(
           clubId = club.id,
           clubName = club.name,
           clubType = club.type.name,
           lie = result.lie.name,
           missDirection = result.missDirection?.name,
           timestamp = System.currentTimeMillis(),
           holeNumber = sessionContextManager.getCurrentHole(),
           syncStatus = "PENDING"
       )

       pendingShotDao.insert(pendingShot)

       // Attempt immediate sync if online
       if (networkMonitor.isOnline.value) {
           shotSyncService.syncPendingShots()
       }

       return Result.success(Unit)
   }
   ```

#### Verification:
- [ ] Shots save locally instantly (<100ms)
- [ ] Pending shots queue when offline
- [ ] Auto-sync triggers when network returns
- [ ] Sync retries failed shots
- [ ] Analytics tracks sync latency
- [ ] No data loss even if app crashes before sync

---

### Task 20: Add Network Connectivity Handling

**Spec reference**: C3 (Offline-first), R6 (offline queueing)
**Acceptance criteria**: A4 (shot logger with poor reception)
**Dependencies**: Task 19 (offline shot logging)

#### Implementation steps:
1. Create `ui/caddy/components/ConnectivityBanner.kt`:
   ```kotlin
   @Composable
   fun ConnectivityBanner(
       isOnline: Boolean,
       pendingShotsCount: Int,
       modifier: Modifier = Modifier
   ) {
       AnimatedVisibility(
           visible = !isOnline || pendingShotsCount > 0,
           modifier = modifier
       ) {
           Card(
               colors = CardDefaults.cardColors(
                   containerColor = if (isOnline) {
                       MaterialTheme.colorScheme.tertiaryContainer
                   } else {
                       MaterialTheme.colorScheme.errorContainer
                   }
               ),
               modifier = Modifier.fillMaxWidth()
           ) {
               Row(
                   modifier = Modifier.padding(12.dp),
                   verticalAlignment = Alignment.CenterVertically
               ) {
                   Icon(
                       imageVector = if (isOnline) {
                           Icons.Default.CloudUpload
                       } else {
                           Icons.Default.CloudOff
                       },
                       contentDescription = null
                   )

                   Spacer(modifier = Modifier.width(8.dp))

                   Text(
                       text = if (isOnline) {
                           "Syncing $pendingShotsCount shots..."
                       } else {
                           "Offline: $pendingShotsCount shots queued"
                       },
                       style = MaterialTheme.typography.bodySmall
                   )
               }
           }
       }
   }
   ```

2. Update LiveCaddyViewModel to track connectivity:
   ```kotlin
   init {
       viewModelScope.launch {
           networkMonitor.isOnline.collect { isOnline ->
               _uiState.update { it.copy(isOffline = !isOnline) }

               if (isOnline) {
                   shotSyncService.syncPendingShots()
               }
           }
       }
   }
   ```

3. Add pending shots count to state:
   ```kotlin
   data class LiveCaddyState(
       // ... existing fields
       val isOffline: Boolean = false,
       val pendingShotsCount: Int = 0
   )
   ```

#### Verification:
- [ ] Connectivity banner shows when offline
- [ ] Banner displays pending shots count
- [ ] Banner auto-hides when all shots synced
- [ ] Offline mode still allows shot logging
- [ ] Weather HUD shows cached data when offline

---

### Task 21: Add Shot Logger Analytics & Latency Tracking

**Spec reference**: R6 (one-second logging flow)
**Acceptance criteria**: A4 (speed requirement)
**Dependencies**: Task 17 (shot logger component)

#### Implementation steps:
1. Add analytics events to AnalyticsEvent.kt:
   ```kotlin
   sealed class AnalyticsEvent {
       // ... existing events

       data class ShotLoggerOpened(val timestamp: Long) : AnalyticsEvent()
       data class ClubSelected(val clubType: String, val latencyMs: Long) : AnalyticsEvent()
       data class ShotLogged(
           val clubType: String,
           val lie: String,
           val totalLatencyMs: Long  // From FAB tap to shot saved
       ) : AnalyticsEvent()
       data class ShotSynced(val shotId: String, val latencyMs: Long) : AnalyticsEvent()
   }
   ```

2. Update LiveCaddyViewModel to track timing:
   ```kotlin
   private var shotLoggerOpenedAt: Long = 0
   private var clubSelectedAt: Long = 0

   fun onAction(action: LiveCaddyAction) {
       when (action) {
           LiveCaddyAction.ToggleShotLogger(true) -> {
               shotLoggerOpenedAt = System.currentTimeMillis()
               analytics.trackEvent(AnalyticsEvent.ShotLoggerOpened(shotLoggerOpenedAt))
               _uiState.update { it.copy(isShotLoggerVisible = true) }
           }

           is LiveCaddyAction.SelectClub -> {
               clubSelectedAt = System.currentTimeMillis()
               analytics.trackEvent(AnalyticsEvent.ClubSelected(
                   clubType = action.club.type.name,
                   latencyMs = clubSelectedAt - shotLoggerOpenedAt
               ))
               _uiState.update { it.copy(selectedClub = action.club) }
           }

           is LiveCaddyAction.LogShot -> {
               val totalLatency = System.currentTimeMillis() - shotLoggerOpenedAt

               analytics.trackEvent(AnalyticsEvent.ShotLogged(
                   clubType = _uiState.value.selectedClub?.type?.name ?: "UNKNOWN",
                   lie = action.result.lie.name,
                   totalLatencyMs = totalLatency
               ))

               viewModelScope.launch {
                   logShot(
                       _uiState.value.selectedClub ?: return@launch,
                       action.result
                   )

                   _uiState.update {
                       it.copy(
                           isShotLoggerVisible = false,
                           selectedClub = null,
                           lastShotConfirmed = true
                       )
                   }
               }
           }
       }
   }
   ```

3. Add performance monitoring in debug builds:
   ```kotlin
   if (BuildConfig.DEBUG) {
       if (totalLatency > 2000) {
           Log.w("ShotLogger", "Shot logging exceeded 2s target: ${totalLatency}ms")
       }
   }
   ```

#### Verification:
- [ ] Analytics tracks shot logger open timestamp
- [ ] Analytics tracks club selection latency
- [ ] Analytics tracks total shot logging latency
- [ ] Average latency <2 seconds (monitored in analytics dashboard)
- [ ] Debug logs warn if latency exceeds target

---

### Task 22: Add Haptic Feedback & Confirmation UI

**Spec reference**: R6 (haptic confirmation on save), R7 (haptic feedback setting)
**Acceptance criteria**: A4 (haptic confirmation)
**Dependencies**: Task 17 (shot logger), Task 21 (analytics)

#### Implementation steps:
1. Create `ui/caddy/components/ShotConfirmationToast.kt`:
   ```kotlin
   @Composable
   fun ShotConfirmationToast(
       visible: Boolean,
       shotDetails: String,
       onDismiss: () -> Unit
   ) {
       AnimatedVisibility(
           visible = visible,
           enter = slideInVertically { it } + fadeIn(),
           exit = slideOutVertically { it } + fadeOut()
       ) {
           Snackbar(
               modifier = Modifier.padding(16.dp),
               action = {
                   TextButton(onClick = onDismiss) {
                       Text("Dismiss")
                   }
               }
           ) {
               Row(verticalAlignment = Alignment.CenterVertically) {
                   Icon(
                       imageVector = Icons.Default.CheckCircle,
                       contentDescription = "Success",
                       tint = Color.Green
                   )
                   Spacer(modifier = Modifier.width(8.dp))
                   Text("Shot logged: $shotDetails")
               }
           }
       }

       // Auto-dismiss after 2 seconds
       LaunchedEffect(visible) {
           if (visible) {
               delay(2000)
               onDismiss()
           }
       }
   }
   ```

2. Update LiveCaddyScreen to show confirmation:
   ```kotlin
   Box(modifier = Modifier.fillMaxSize()) {
       LiveCaddyContent(/* ... */)

       ShotConfirmationToast(
           visible = state.lastShotConfirmed,
           shotDetails = state.selectedClub?.let { "${it.name} → ${state.lastShotResult?.lie?.name}" } ?: "",
           onDismiss = { viewModel.onAction(LiveCaddyAction.DismissShotConfirmation) }
       )
   }
   ```

3. Respect haptic feedback setting:
   ```kotlin
   @Composable
   fun rememberHapticFeedback(enabled: Boolean): () -> Unit {
       val view = LocalView.current
       return remember(enabled) {
           if (enabled) {
               {
                   view.performHapticFeedback(
                       HapticFeedbackConstants.CONFIRM,
                       HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                   )
               }
           } else {
               {} // No-op if disabled
           }
       }
   }

   // Usage in LiveCaddyScreen
   val hapticFeedback = rememberHapticFeedback(enabled = state.settings.hapticFeedback)
   ```

#### Verification:
- [ ] Haptic feedback fires when shot is saved
- [ ] Confirmation toast appears with shot details
- [ ] Toast auto-dismisses after 2 seconds
- [ ] Haptic feedback respects settings toggle
- [ ] Feedback is strong enough to be noticeable outdoors

---

## PHASE 5: VOICE INTEGRATION & NAVIGATION

### Task 23: Add Live Caddy Routes to Navigation

**Spec reference**: R5 (Voice-first interaction), R1 (navigation to Live Caddy)
**Acceptance criteria**: A2 (voice queries route to Live Caddy)
**Dependencies**: Task 18 (LiveCaddyScreen)

#### Implementation steps:
1. Update `ui/navigation/Screen.kt`:
   ```kotlin
   sealed class Screen(val route: String) {
       // ... existing screens
       data object LiveCaddy : Screen("live_caddy")
       data object StartRound : Screen("start_round")
       data object EndRoundSummary : Screen("end_round/{roundId}")
   }
   ```

2. Update `ui/navigation/AppNavigation.kt`:
   ```kotlin
   @Composable
   fun AppNavigation(navController: NavHostController) {
       NavHost(
           navController = navController,
           startDestination = Screen.Home.route
       ) {
           // ... existing routes

           composable(Screen.LiveCaddy.route) {
               LiveCaddyScreen(
                   onNavigateBack = { navController.popBackStack() }
               )
           }

           composable(Screen.StartRound.route) {
               StartRoundScreen(
                   onRoundStarted = {
                       navController.navigate(Screen.LiveCaddy.route) {
                           popUpTo(Screen.Home.route)
                       }
                   }
               )
           }
       }
   }
   ```

3. Update NavCaddyNavigatorImpl to include Live Caddy routes:
   ```kotlin
   override fun navigate(destination: Destination) {
       when (destination) {
           is Destination.LiveCaddy -> {
               navController.navigate(Screen.LiveCaddy.route)
           }
           is Destination.ShotRecommendation -> {
               navController.navigate(Screen.LiveCaddy.route) {
                   // Deep link to strategy map expanded
               }
           }
           // ... existing destinations
       }
   }
   ```

#### Verification:
- [ ] LiveCaddy route is reachable from home
- [ ] StartRound navigates to LiveCaddy on success
- [ ] Back navigation from LiveCaddy works correctly
- [ ] Deep links to specific sections work (e.g., strategy map)

---

### Task 24: Integrate Voice Queries with Live Caddy Routing

**Spec reference**: R5 (Voice-first interaction)
**Acceptance criteria**: A2 (voice queries route to Live Caddy)
**Dependencies**: Task 23 (navigation), existing VoiceInputManager

#### Implementation steps:
1. Update IntentType enum with Live Caddy intents:
   ```kotlin
   enum class IntentType {
       // ... existing intents
       SHOT_RECOMMENDATION,     // "What's the play off the tee?"
       CLUB_ADJUSTMENT,         // "Club up or down?"
       BAILOUT_QUERY,           // "Where's the bailout?"
       WEATHER_CHECK,           // "How's the wind?"
       READINESS_CHECK          // "How am I feeling?"
   }
   ```

2. Update IntentClassifier to recognize Live Caddy queries:
   ```kotlin
   suspend fun classify(input: String, context: SessionContext): ClassificationResult {
       // ... existing classification logic

       return when {
           input.contains("play", ignoreCase = true) ||
           input.contains("strategy", ignoreCase = true) -> {
               ClassificationResult.Route(
                   intent = IntentType.SHOT_RECOMMENDATION,
                   module = Module.CADDY,
                   destination = Destination.LiveCaddy(expandStrategy = true)
               )
           }

           input.contains("club up", ignoreCase = true) ||
           input.contains("club down", ignoreCase = true) -> {
               ClassificationResult.Route(
                   intent = IntentType.CLUB_ADJUSTMENT,
                   module = Module.CADDY,
                   destination = Destination.LiveCaddy(expandWeather = true)
               )
           }

           input.contains("bailout", ignoreCase = true) -> {
               ClassificationResult.Route(
                   intent = IntentType.BAILOUT_QUERY,
                   module = Module.CADDY,
                   destination = Destination.LiveCaddy(expandStrategy = true)
               )
           }

           // ... existing patterns
       }
   }
   ```

3. Create Bones persona response templates:
   ```kotlin
   object BonesResponses {
       fun shotRecommendation(strategy: HoleStrategy, weather: WeatherData): String {
           return buildString {
               append("Alright boss, here's the play: ")
               append(strategy.recommendedLandingZone.visualCue)
               append(". ")

               if (strategy.riskCallouts.isNotEmpty()) {
                   append(strategy.riskCallouts.first())
                   append(". ")
               }

               if (weather.conditionsAdjustment().carryModifier < 0.98) {
                   append("Wind's gonna knock it down a bit, so maybe take one more club.")
               }
           }
       }

       fun clubAdjustment(weather: WeatherData, baseCarry: Int): String {
           val adjustment = weather.conditionsAdjustment()
           return when {
               adjustment.carryModifier < 0.95 -> "Club up, boss. ${adjustment.reason}"
               adjustment.carryModifier > 1.05 -> "Club down. ${adjustment.reason}"
               else -> "Stick with your number. Conditions are neutral."
           }
       }

       fun bailoutLocation(strategy: HoleStrategy): String {
           val safeSide = strategy.dangerZones
               .groupBy { it.location.side }
               .minByOrNull { it.value.sumOf { zone -> zone.penaltyStrokes } }
               ?.key ?: "center"

           return "If you miss, miss $safeSide. That's the bailout."
       }
   }
   ```

4. Update ConversationViewModel to generate Bones responses:
   ```kotlin
   private suspend fun handleLiveCaddyIntent(intent: IntentType) {
       val context = getLiveCaddyContext().getOrNull() ?: return

       val response = when (intent) {
           IntentType.SHOT_RECOMMENDATION -> {
               context.holeStrategy?.let { strategy ->
                   context.weather?.let { weather ->
                       BonesResponses.shotRecommendation(strategy, weather)
                   }
               } ?: "Need to load the hole first, boss."
           }

           IntentType.CLUB_ADJUSTMENT -> {
               context.weather?.let { weather ->
                   BonesResponses.clubAdjustment(weather, 150)  // Use player's average carry
               } ?: "Can't get a read on the wind right now."
           }

           IntentType.BAILOUT_QUERY -> {
               context.holeStrategy?.let { strategy ->
                   BonesResponses.bailoutLocation(strategy)
               } ?: "Let me pull up the hole info first."
           }

           else -> "Not sure what you need, boss. Try asking about the play or the wind."
       }

       _uiState.update {
           it.copy(
               messages = it.messages + ConversationMessage.Assistant(response)
           )
       }
   }
   ```

#### Verification:
- [ ] "What's the play off the tee?" routes to LiveCaddy with strategy expanded
- [ ] "Club up or down?" provides Bones-style weather adjustment advice
- [ ] "Where's the bailout?" identifies safe miss zones
- [ ] Bones persona tone is consistent (casual, confident, Tour caddy style)
- [ ] Voice queries work from any screen (route to LiveCaddy)

---

### Task 25: Add Live Caddy Entry Points to Home Screen

**Spec reference**: R1 (Start/Resume/End a round)
**Acceptance criteria**: N/A (UX convenience)
**Dependencies**: Task 23 (navigation)

#### Implementation steps:
1. Update HomeScreen to show Live Caddy CTA:
   ```kotlin
   @Composable
   fun HomeScreen(
       onNavigateToLiveCaddy: () -> Unit,
       onStartRound: () -> Unit,
       sessionContextManager: SessionContextManager = hiltViewModel()
   ) {
       val currentSession by sessionContextManager.getSession().collectAsState(initial = null)

       Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
           // Hero section
           if (currentSession?.roundState != null) {
               ResumeRoundCard(
                   roundState = currentSession.roundState,
                   onResume = onNavigateToLiveCaddy
               )
           } else {
               StartRoundCard(onStartRound = onStartRound)
           }

           Spacer(modifier = Modifier.height(24.dp))

           // Quick actions
           QuickActionGrid(
               onNavigateToLiveCaddy = onNavigateToLiveCaddy,
               hasActiveRound = currentSession?.roundState != null
           )
       }
   }

   @Composable
   private fun ResumeRoundCard(
       roundState: RoundState,
       onResume: () -> Unit
   ) {
       Card(
           modifier = Modifier.fillMaxWidth(),
           colors = CardDefaults.cardColors(
               containerColor = MaterialTheme.colorScheme.primaryContainer
           )
       ) {
           Column(modifier = Modifier.padding(16.dp)) {
               Text(
                   text = "Round in Progress",
                   style = MaterialTheme.typography.titleLarge,
                   fontWeight = FontWeight.Bold
               )

               Text(
                   text = roundState.round.courseName,
                   style = MaterialTheme.typography.bodyLarge
               )

               Text(
                   text = "Hole ${roundState.currentHole} of 18 • ${roundState.totalScore} total",
                   style = MaterialTheme.typography.bodyMedium
               )

               Spacer(modifier = Modifier.height(12.dp))

               Button(
                   onClick = onResume,
                   modifier = Modifier.fillMaxWidth()
               ) {
                   Text("Resume Round")
               }
           }
       }
   }
   ```

2. Add quick action for "Check Weather" and "View Strategy":
   ```kotlin
   @Composable
   private fun QuickActionGrid(
       onNavigateToLiveCaddy: () -> Unit,
       hasActiveRound: Boolean
   ) {
       LazyVerticalGrid(
           columns = GridCells.Fixed(2),
           horizontalArrangement = Arrangement.spacedBy(12.dp),
           verticalArrangement = Arrangement.spacedBy(12.dp)
       ) {
           if (hasActiveRound) {
               item {
                   QuickActionCard(
                       icon = Icons.Default.Cloud,
                       label = "Weather",
                       onClick = { onNavigateToLiveCaddy() }
                   )
               }

               item {
                   QuickActionCard(
                       icon = Icons.Default.Map,
                       label = "Strategy",
                       onClick = { onNavigateToLiveCaddy() }
                   )
               }
           }
       }
   }
   ```

#### Verification:
- [ ] Home screen shows "Resume Round" if active round exists
- [ ] Resume Round navigates to LiveCaddyScreen with context restored
- [ ] Start Round button navigates to round setup flow
- [ ] Quick actions are only visible when round is active
- [ ] Round details (course, hole, score) display correctly

---

## PHASE 6: TESTING & VERIFICATION

### Task 26: Write Unit Tests for Domain Services

**Spec reference**: All functional requirements
**Acceptance criteria**: All acceptance criteria
**Dependencies**: Tasks 6-9 (all domain services)

#### Implementation steps:
1. Create test file `test/.../ReadinessCalculatorTest.kt`:
   ```kotlin
   @Test
   fun `calculateReadiness with all metrics returns weighted average`() {
       val metrics = WearableMetrics(
           hrvMs = 50.0,      // 100/100 normalized
           sleepMinutes = 480, // 100/100 (8 hours)
           sleepQualityScore = 80.0,
           stressScore = 20.0, // 80/100 (low stress is good)
           timestamp = System.currentTimeMillis()
       )

       val readiness = calculator.calculateReadiness(metrics)

       // (100 * 0.4) + (80 * 0.4) + (80 * 0.2) = 88
       assertEquals(88, readiness.overall)
       assertTrue(readiness.breakdown.hrv!!.value > 90)
   }

   @Test
   fun `calculateReadiness with missing metrics defaults to 50`() {
       val metrics = WearableMetrics(
           hrvMs = null,
           sleepMinutes = null,
           sleepQualityScore = null,
           stressScore = null,
           timestamp = System.currentTimeMillis()
       )

       val readiness = calculator.calculateReadiness(metrics)

       assertEquals(50, readiness.overall)  // All defaults
   }
   ```

2. Create `test/.../ConditionsCalculatorTest.kt`:
   ```kotlin
   @Test
   fun `headwind reduces carry distance`() {
       val weather = WeatherData(
           windSpeedMps = 5.0,
           windDegrees = 0,    // North
           temperatureCelsius = 15.0,
           humidity = 50,
           timestamp = System.currentTimeMillis(),
           location = Location(0.0, 0.0)
       )

       val adjustment = calculator.calculateAdjustment(
           weather = weather,
           targetBearing = 0,  // Also North (headwind)
           baseCarryMeters = 150
       )

       assertTrue(adjustment.carryModifier < 1.0)  // Less than 100%
       assertTrue(adjustment.reason.contains("headwind"))
   }
   ```

3. Create `test/.../PinSeekerEngineTest.kt`:
   ```kotlin
   @Test
   fun `computeStrategy avoids hazards matching dominant miss`() = runTest {
       val hole = CourseHole(
           number = 1,
           par = 4,
           lengthMeters = 350,
           hazards = listOf(
               HazardZone(
                   type = HazardType.WATER,
                   location = HazardLocation("right", 200..250),
                   penaltyStrokes = 1.0,
                   affectedMisses = listOf(MissDirection.SLICE, MissDirection.PUSH)
               )
           ),
           pinPosition = null
       )

       val readiness = ReadinessScore(
           overall = 70,
           breakdown = ReadinessBreakdown(null, null, null),
           timestamp = System.currentTimeMillis(),
           source = ReadinessSource.MANUAL_ENTRY
       )

       // Mock repository to return slice as dominant miss
       coEvery { navCaddyRepository.getMissPatterns() } returns listOf(
           MissPattern(
               direction = MissDirection.SLICE,
               clubId = null,
               frequency = 10,
               confidence = 0.8,
               lastOccurrence = System.currentTimeMillis()
           )
       )

       val strategy = pinSeekerEngine.computeStrategy(hole, handicap = 9, readiness)

       // Should recommend aiming left to avoid right-side water
       assertTrue(strategy.riskCallouts.any { it.contains("right") })
       assertTrue(strategy.dangerZones.isNotEmpty())
   }
   ```

4. Write tests for all use cases (StartRoundUseCase, LogShotUseCase, etc.)

#### Verification:
- [ ] All domain service tests pass
- [ ] Edge cases covered (null inputs, extreme values)
- [ ] Test coverage >80% for domain layer
- [ ] Tests run in <5 seconds

---

### Task 27: Write Integration Tests for LiveCaddyViewModel

**Spec reference**: All functional requirements
**Acceptance criteria**: A1-A4
**Dependencies**: Task 13 (ViewModel), Task 26 (unit tests)

#### Implementation steps:
1. Create `test/.../LiveCaddyViewModelTest.kt`:
   ```kotlin
   @HiltAndroidTest
   class LiveCaddyViewModelTest {

       @get:Rule
       val hiltRule = HiltAndroidRule(this)

       @Inject
       lateinit var getLiveCaddyContext: GetLiveCaddyContextUseCase

       @Inject
       lateinit var logShot: LogShotUseCase

       private lateinit var viewModel: LiveCaddyViewModel

       @Before
       fun setup() {
           hiltRule.inject()
           viewModel = LiveCaddyViewModel(
               getLiveCaddyContext,
               logShot,
               // ... other dependencies
           )
       }

       @Test
       fun `init loads context successfully`() = runTest {
           viewModel.uiState.test {
               val initialState = awaitItem()
               assertTrue(initialState.isLoading)

               val loadedState = awaitItem()
               assertFalse(loadedState.isLoading)
               assertNotNull(loadedState.roundState)
           }
       }

       @Test
       fun `LogShot action saves shot and triggers haptic`() = runTest {
           val club = Club(/* ... */)
           val result = ShotResult(Lie.FAIRWAY, MissDirection.STRAIGHT)

           viewModel.onAction(LiveCaddyAction.SelectClub(club))
           viewModel.onAction(LiveCaddyAction.LogShot(result))

           viewModel.uiState.test {
               val state = awaitItem()
               assertTrue(state.lastShotConfirmed)
               assertNull(state.selectedClub)
           }

           // Verify shot was saved via repository
           coVerify { logShot(club, result) }
       }

       @Test
       fun `weather refresh updates state`() = runTest {
           viewModel.onAction(LiveCaddyAction.RefreshWeather)

           viewModel.uiState.test {
               val state = awaitItem()
               assertNotNull(state.weather)
           }
       }
   }
   ```

2. Create UI tests for LiveCaddyScreen:
   ```kotlin
   @Test
   fun liveCaddyScreen_displaysWeatherHud() {
       composeTestRule.setContent {
           AppTheme {
               LiveCaddyScreen(
                   viewModel = fakeViewModel,
                   onNavigateBack = {}
               )
           }
       }

       composeTestRule.onNodeWithText("Forecaster").assertExists()
       composeTestRule.onNodeWithContentDescription("Wind").assertExists()
   }

   @Test
   fun shotLogger_completesInUnder2Seconds() {
       val startTime = System.currentTimeMillis()

       composeTestRule.setContent {
           LiveCaddyScreen(viewModel = fakeViewModel, onNavigateBack = {})
       }

       // Tap FAB
       composeTestRule.onNodeWithContentDescription("Log Shot").performClick()

       // Select club
       composeTestRule.onNodeWithText("7I").performClick()

       // Select result
       composeTestRule.onNodeWithText("FAIRWAY").performClick()

       val endTime = System.currentTimeMillis()

       assertTrue((endTime - startTime) < 2000)
   }
   ```

#### Verification:
- [ ] ViewModel tests pass with Hilt injection
- [ ] State updates work correctly for all actions
- [ ] Shot logging integration test confirms <2s latency
- [ ] UI tests verify all components render
- [ ] Tests use Turbine for Flow testing

---

### Task 28: End-to-End Verification Against Acceptance Criteria

**Spec reference**: Section 5 (Acceptance criteria A1-A4)
**Acceptance criteria**: A1-A4 (meta-verification)
**Dependencies**: All previous tasks

#### Implementation steps:
1. Create manual test plan `docs/testing/live-caddy-e2e-test-plan.md`:
   ```markdown
   # Live Caddy Mode - End-to-End Test Plan

   ## A1: Weather HUD and adjustment
   **GIVEN**: User has location enabled during an active round
   **WHEN**: User opens Live Caddy HUD
   **THEN**: Wind/temperature/humidity render within 2 seconds and the strategy engine applies a conditions adjustment to carry recommendations.

   ### Test Steps:
   1. Enable location permissions
   2. Start a new round
   3. Navigate to Live Caddy screen
   4. Start timer when screen loads
   5. Verify weather HUD renders within 2 seconds
   6. Verify wind speed, direction, temperature, humidity displayed
   7. Verify conditions chip shows carry adjustment (e.g., "-5m")
   8. Tap expanded view → verify adjustment reason is shown

   ### Expected Results:
   - [ ] Weather HUD visible within 2 seconds
   - [ ] All weather fields populated
   - [ ] Conditions adjustment calculated and displayed
   - [ ] Adjustment reason is human-readable

   ---

   ## A2: Readiness impacts strategy
   **GIVEN**: Wearable sync reports low readiness (below configured threshold)
   **WHEN**: User asks "Where should I aim?"
   **THEN**: Bones recommends more conservative targets with larger safety margins and explains the risk reduction briefly.

   ### Test Steps:
   1. Set manual readiness to 35 (low)
   2. View PinSeeker strategy map
   3. Verify safety margin is larger than baseline
   4. Use voice: "Where should I aim?"
   5. Verify Bones response mentions conservative play

   ### Expected Results:
   - [ ] Safety margin increases with low readiness
   - [ ] Bones response includes risk reduction explanation
   - [ ] Landing zone is more conservative than with high readiness

   ---

   ## A3: Hazard-aware landing zone
   **GIVEN**: Hole hazards include OB right and water long
   **WHEN**: User requests "PinSeeker summary"
   **THEN**: App highlights danger zones and recommends a landing zone that avoids the user's dominant miss and typical distance error.

   ### Test Steps:
   1. Load hole with OB right and water long hazards
   2. Set dominant miss to SLICE (right)
   3. View PinSeeker map
   4. Verify danger zones highlighted
   5. Verify recommended landing zone avoids right side
   6. Verify risk callouts mention right-side hazard

   ### Expected Results:
   - [ ] Danger zones listed (OB right, water long)
   - [ ] Landing zone aims left of center (away from slice)
   - [ ] Risk callout: "Right miss brings OB into play"
   - [ ] Personalization context shows slice as dominant miss

   ---

   ## A4: Shot logger speed and persistence
   **GIVEN**: User is mid-round with poor reception
   **WHEN**: User logs a shot
   **THEN**: The shot is saved locally with haptic confirmation and appears in the round timeline; it syncs automatically when connection returns.

   ### Test Steps:
   1. Enable airplane mode (offline)
   2. Tap FAB to open shot logger
   3. Select club (e.g., 7I)
   4. Select result (e.g., FAIRWAY)
   5. Verify haptic feedback fires
   6. Verify confirmation toast appears
   7. Verify shot appears in pending queue
   8. Disable airplane mode
   9. Verify shot auto-syncs
   10. Verify sync confirmation

   ### Expected Results:
   - [ ] Shot saves locally within 1 second
   - [ ] Haptic feedback is noticeable
   - [ ] Confirmation toast shows shot details
   - [ ] Connectivity banner shows "1 shot queued" when offline
   - [ ] Shot syncs automatically when online
   - [ ] Pending queue clears after sync
   ```

2. Create automated E2E tests using Espresso/Compose UI Testing:
   ```kotlin
   @Test
   fun acceptanceCriteria_A1_weatherHudRendersWithin2Seconds() {
       // Measure time from screen load to weather HUD visible
       val scenario = launchActivityScenario<MainActivity>()

       val startTime = System.currentTimeMillis()

       composeTestRule.waitUntil(timeoutMillis = 2000) {
           composeTestRule.onAllNodesWithText("Forecaster")
               .fetchSemanticsNodes().isNotEmpty()
       }

       val renderTime = System.currentTimeMillis() - startTime

       assertTrue("Weather HUD rendered in ${renderTime}ms", renderTime < 2000)

       // Verify all fields present
       composeTestRule.onNodeWithContentDescription("Wind").assertExists()
       composeTestRule.onNodeWithText(Regex("\\d+ m/s")).assertExists()
       composeTestRule.onNodeWithText(Regex("\\d+°C")).assertExists()
   }
   ```

3. Create performance benchmark:
   ```kotlin
   @Test
   fun benchmark_shotLoggingLatency() {
       val iterations = 10
       val latencies = mutableListOf<Long>()

       repeat(iterations) {
           val startTime = System.currentTimeMillis()

           // Perform shot logging flow
           composeTestRule.onNodeWithContentDescription("Log Shot").performClick()
           composeTestRule.onNodeWithText("7I").performClick()
           composeTestRule.onNodeWithText("FAIRWAY").performClick()

           val endTime = System.currentTimeMillis()
           latencies.add(endTime - startTime)
       }

       val averageLatency = latencies.average()
       val p95Latency = latencies.sorted()[iterations * 95 / 100]

       println("Shot logging latency - Avg: ${averageLatency}ms, P95: ${p95Latency}ms")

       assertTrue("Average latency under 2s", averageLatency < 2000)
       assertTrue("P95 latency under 2.5s", p95Latency < 2500)
   }
   ```

4. Run full test suite and document results:
   ```bash
   cd android
   ./gradlew testDevDebugUnitTest      # Unit tests
   ./gradlew connectedDevDebugAndroidTest  # Integration tests
   ```

#### Verification:
- [ ] A1 verified: Weather HUD renders <2s with adjustment
- [ ] A2 verified: Low readiness increases safety margins
- [ ] A3 verified: Strategy avoids dominant miss hazards
- [ ] A4 verified: Shot logger works offline with haptic confirmation
- [ ] All automated tests pass
- [ ] Performance benchmarks meet targets
- [ ] Manual test plan executed and documented

---

## Risk Assessment

### High Risks
1. **Weather API rate limits**: OpenWeatherMap free tier allows 60 calls/min
   - **Mitigation**: Implement 5-minute caching, fallback to manual weather entry

2. **Wearable integration complexity**: Platform-specific APIs (Apple Health, Google Fit)
   - **Mitigation**: Use stub implementation for MVP, add real integration in Phase 2

3. **Course data availability**: No guaranteed source for hazard geometry
   - **Mitigation**: Use placeholder data for MVP, manual import option

4. **Shot logger latency on low-end devices**: 2-second target may be tight
   - **Mitigation**: Profile on low-end devices, optimize rendering, use lazy loading

### Medium Risks
1. **Location permission denials**: Users may not grant location access
   - **Mitigation**: Provide manual course/weather entry as fallback

2. **Offline mode complexity**: Sync conflicts if user edits on multiple devices
   - **Mitigation**: Last-write-wins for MVP, eventual consistency acceptable

3. **Battery drain from location + network**: On-course use is battery-intensive
   - **Mitigation**: Implement low-power location mode, cache aggressively

### Low Risks
1. **Voice recognition accuracy outdoors**: Wind noise may affect transcription
   - **Mitigation**: Provide tap-based alternatives, improve voice UX in future

2. **Outdoor visibility**: Sunlight readability concerns
   - **Mitigation**: High-contrast theme variant, OLED-friendly colors

---

## Open Questions (Remaining)

### Q1: Weather Provider (RESOLVED)
**Decision**: Use OpenWeatherMap free tier with 5-minute caching

### Q2: Course Data Source (RESOLVED FOR MVP)
**Decision**: Placeholder data for MVP (3 sample holes), manual import option, integrate with provider in Phase 2

### Q3: Readiness Algorithm (RESOLVED)
**Decision**: Fixed weights (HRV: 40%, Sleep: 40%, Stress: 20%), user override always available

### Q4: Logging Taxonomy (RESOLVED)
**Decision**: Use spec R6 exactly (club + lie + optional miss direction)

### New Open Questions:
- **Q5**: Should we support Apple Watch / Wear OS complications for quick shot logging?
  - **Recommendation**: Defer to Phase 2 (wearable integration)

- **Q6**: How to handle multi-player rounds (group scoring)?
  - **Recommendation**: Out of scope for Live Caddy MVP, focus on single-player experience

- **Q7**: Should we integrate with golf GPS apps for course data?
  - **Recommendation**: Investigate GolfNow, GHIN, or SwingU APIs in Phase 2

---

## Implementation Timeline Estimate

| Phase | Tasks | Estimated Effort | Dependencies |
|-------|-------|-----------------|--------------|
| Phase 1: Foundation & API | 1-5 | 2-3 days | None |
| Phase 2: Domain Services | 6-12 | 3-4 days | Phase 1 |
| Phase 3: UI Components | 13-18 | 4-5 days | Phase 2 |
| Phase 4: Shot Logger & Offline | 19-22 | 2-3 days | Phase 3 |
| Phase 5: Voice & Navigation | 23-25 | 1-2 days | Phase 4 |
| Phase 6: Testing & Verification | 26-28 | 2-3 days | All phases |
| **Total** | **28 tasks** | **14-20 days** | Sequential |

**Note**: Timeline assumes single developer, full-time work. Parallelization possible for UI (Phase 3) and domain services (Phase 2).

---

## Next Steps

1. **Get user approval** on this plan
2. **Resolve remaining open questions** (Q5-Q7 if needed)
3. **Set up OpenWeatherMap API key** in build config
4. **Begin Task 1**: Validate spec and document final decisions
5. **Use `/implement` command** to execute tasks sequentially with android-engineer agent

---

## Success Metrics

Post-implementation, track:
- **Weather HUD render time**: Target <2s, measure via analytics
- **Shot logger latency**: Target <2s, P95 <2.5s
- **Offline sync success rate**: Target >95%
- **User engagement**: % of rounds using Live Caddy vs manual scoring
- **Voice query accuracy**: % of intents correctly classified

---

**Plan Status**: ✅ READY FOR IMPLEMENTATION
**Total Tasks**: 28
**Estimated Duration**: 14-20 days
**Blockers**: None (all open questions resolved for MVP)
