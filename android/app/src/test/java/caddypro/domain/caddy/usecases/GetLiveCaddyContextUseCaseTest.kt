package caddypro.domain.caddy.usecases

import caddypro.data.caddy.repository.ReadinessRepository
import caddypro.data.caddy.repository.WeatherRepository
import caddypro.domain.caddy.models.Location
import caddypro.domain.caddy.models.MetricScore
import caddypro.domain.caddy.models.ReadinessBreakdown
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.ReadinessSource
import caddypro.domain.caddy.models.WeatherData
import caddypro.domain.caddy.services.PinSeekerEngine
import caddypro.domain.navcaddy.context.CourseConditions
import caddypro.domain.navcaddy.context.RoundState
import caddypro.domain.navcaddy.context.SessionContextManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for GetLiveCaddyContextUseCase.
 *
 * Validates:
 * - Successful context aggregation with all data sources
 * - Graceful degradation when weather unavailable
 * - Default readiness when no scores exist
 * - Failure when no active round
 *
 * Spec reference: live-caddy-mode.md R1, R2, R3, R4
 * Plan reference: live-caddy-mode-plan.md Task 9
 */
class GetLiveCaddyContextUseCaseTest {

    private lateinit var sessionContextManager: SessionContextManager
    private lateinit var weatherRepository: WeatherRepository
    private lateinit var readinessRepository: ReadinessRepository
    private lateinit var pinSeekerEngine: PinSeekerEngine
    private lateinit var useCase: GetLiveCaddyContextUseCase

    private val testRoundState = RoundState(
        roundId = "round-123",
        courseName = "Pebble Beach",
        currentHole = 7,
        currentPar = 4,
        totalScore = 35,
        holesCompleted = 6,
        conditions = CourseConditions(
            weather = "sunny",
            windSpeed = 10,
            windDirection = "NW",
            temperature = 72
        )
    )

    private val testLocation = Location(
        latitude = 36.5674,
        longitude = -121.9500
    )

    private val testWeatherData = WeatherData(
        windSpeedMps = 5.0,
        windDegrees = 315,
        temperatureCelsius = 22.0,
        humidity = 60,
        timestamp = System.currentTimeMillis(),
        location = testLocation
    )

    private val testReadinessScore = ReadinessScore(
        overall = 75,
        breakdown = ReadinessBreakdown(
            hrv = MetricScore(value = 80.0, weight = 0.4),
            sleepQuality = MetricScore(value = 70.0, weight = 0.4),
            stressLevel = MetricScore(value = 75.0, weight = 0.2)
        ),
        timestamp = System.currentTimeMillis(),
        source = ReadinessSource.WEARABLE_SYNC
    )

    @Before
    fun setup() {
        sessionContextManager = mockk()
        weatherRepository = mockk()
        readinessRepository = mockk()
        pinSeekerEngine = mockk()

        useCase = GetLiveCaddyContextUseCase(
            sessionContextManager = sessionContextManager,
            weatherRepository = weatherRepository,
            readinessRepository = readinessRepository,
            pinSeekerEngine = pinSeekerEngine
        )
    }

    /**
     * Test: Successful context aggregation with all data available.
     *
     * Validates:
     * - Round state is retrieved
     * - Weather is fetched successfully
     * - Readiness score is retrieved
     * - Context contains all data
     */
    @Test
    fun `invoke with all data available returns complete context`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns testRoundState
        coEvery { weatherRepository.getCurrentWeather(any()) } returns Result.success(testWeatherData)
        coEvery { readinessRepository.getMostRecent() } returns testReadinessScore

        // When
        val result = useCase()

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val context = result.getOrNull()
        assertNotNull("Context should not be null", context)
        assertEquals("Round state should match", testRoundState, context?.roundState)
        assertNotNull("Weather should be present", context?.weather)
        assertEquals(testWeatherData, context?.weather)
        assertEquals("Readiness should match", testReadinessScore, context?.readiness)
        verify(exactly = 1) { sessionContextManager.getCurrentRoundState() }
    }

    /**
     * Test: Weather unavailable gracefully degrades.
     *
     * Validates:
     * - Weather failure doesn't fail entire context
     * - Context is returned with null weather
     * - Other data is still present
     */
    @Test
    fun `invoke with weather unavailable returns context with null weather`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns testRoundState
        coEvery { weatherRepository.getCurrentWeather(any()) } returns Result.failure(Exception("Network error"))
        coEvery { readinessRepository.getMostRecent() } returns testReadinessScore

        // When
        val result = useCase()

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val context = result.getOrNull()
        assertNotNull("Context should not be null", context)
        assertNull("Weather should be null when unavailable", context?.weather)
        assertEquals("Readiness should still be present", testReadinessScore, context?.readiness)
    }

    /**
     * Test: No readiness data uses default.
     *
     * Validates:
     * - When no readiness exists, default is used
     * - Default has overall score of 70
     * - Default source is MANUAL_ENTRY
     */
    @Test
    fun `invoke with no readiness data uses default score`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns testRoundState
        coEvery { weatherRepository.getCurrentWeather(any()) } returns Result.success(testWeatherData)
        coEvery { readinessRepository.getMostRecent() } returns null

        // When
        val result = useCase()

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val context = result.getOrNull()
        assertNotNull("Context should not be null", context)
        assertEquals("Default readiness should be 70", 70, context?.readiness?.overall)
        assertEquals("Default source should be MANUAL_ENTRY", ReadinessSource.MANUAL_ENTRY, context?.readiness?.source)
    }

    /**
     * Test: No active round returns failure.
     *
     * Validates:
     * - When no active round exists, failure is returned
     * - Error message indicates no active round
     * - No repository calls are made
     */
    @Test
    fun `invoke with no active round returns failure`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns null

        // When
        val result = useCase()

        // Then
        assertTrue("Result should be failure", result.isFailure)
        val exception = result.exceptionOrNull()
        assertNotNull("Exception should not be null", exception)
        assertEquals("Error message should indicate no active round", "No active round", exception?.message)
    }

    /**
     * Test: Hole strategy is null when course data unavailable.
     *
     * Validates:
     * - Context is still successful with null strategy
     * - This is expected for MVP (per Task 9 TODO comment)
     */
    @Test
    fun `invoke returns null hole strategy when course data unavailable`() = runTest {
        // Given
        every { sessionContextManager.getCurrentRoundState() } returns testRoundState
        coEvery { weatherRepository.getCurrentWeather(any()) } returns Result.success(testWeatherData)
        coEvery { readinessRepository.getMostRecent() } returns testReadinessScore

        // When
        val result = useCase()

        // Then
        assertTrue("Result should be successful", result.isSuccess)
        val context = result.getOrNull()
        assertNull("Hole strategy should be null (course data not yet integrated)", context?.holeStrategy)
    }
}
