package caddypro.data.caddy.repository

import caddypro.data.caddy.local.dao.ReadinessScoreDao
import caddypro.data.caddy.local.entities.ReadinessScoreEntity
import caddypro.domain.caddy.models.MetricScore
import caddypro.domain.caddy.models.ReadinessBreakdown
import caddypro.domain.caddy.models.ReadinessScore
import caddypro.domain.caddy.models.ReadinessSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ReadinessRepositoryImpl.
 *
 * Validates:
 * - Save and retrieve readiness scores
 * - Entity-to-domain mapping
 * - History retrieval with date range
 * - Null handling when no scores exist
 *
 * Spec reference: live-caddy-mode.md R3 (BodyCaddy)
 * Plan reference: live-caddy-mode-plan.md Task 10
 */
class ReadinessRepositoryImplTest {

    private lateinit var readinessScoreDao: ReadinessScoreDao
    private lateinit var repository: ReadinessRepositoryImpl

    private val testTimestamp = System.currentTimeMillis()

    private val testReadinessScore = ReadinessScore(
        overall = 75,
        breakdown = ReadinessBreakdown(
            hrv = MetricScore(value = 80.0, weight = 0.4),
            sleepQuality = MetricScore(value = 70.0, weight = 0.4),
            stressLevel = MetricScore(value = 75.0, weight = 0.2)
        ),
        timestamp = testTimestamp,
        source = ReadinessSource.WEARABLE_SYNC
    )

    private val testEntity = ReadinessScoreEntity(
        timestamp = testTimestamp,
        overall = 75,
        hrvScore = 80.0,
        sleepScore = 70.0,
        stressScore = 75.0,
        source = "WEARABLE_SYNC"
    )

    @Before
    fun setup() {
        readinessScoreDao = mockk()
        repository = ReadinessRepositoryImpl(readinessScoreDao)
    }

    /**
     * Test: Save readiness score persists to database.
     *
     * Validates:
     * - Domain model is converted to entity
     * - DAO insert is called
     */
    @Test
    fun `saveReadiness persists score to database`() = runTest {
        // Given
        coEvery { readinessScoreDao.insert(any()) } returns Unit

        // When
        repository.saveReadiness(testReadinessScore)

        // Then
        coVerify(exactly = 1) {
            readinessScoreDao.insert(
                withArg { entity ->
                    assertEquals(testTimestamp, entity.timestamp)
                    assertEquals(75, entity.overall)
                    assertEquals(80.0, entity.hrvScore ?: 0.0, 0.01)
                    assertEquals(70.0, entity.sleepScore ?: 0.0, 0.01)
                    assertEquals(75.0, entity.stressScore ?: 0.0, 0.01)
                    assertEquals("WEARABLE_SYNC", entity.source)
                }
            )
        }
    }

    /**
     * Test: Get most recent score returns latest.
     *
     * Validates:
     * - DAO query is called
     * - Entity is mapped to domain
     * - Weights are correctly assigned
     */
    @Test
    fun `getMostRecent returns latest score`() = runTest {
        // Given
        coEvery { readinessScoreDao.getMostRecent() } returns testEntity

        // When
        val result = repository.getMostRecent()

        // Then
        assertNotNull("Result should not be null", result)
        assertEquals(75, result?.overall)
        assertEquals(testTimestamp, result?.timestamp)
        assertEquals(ReadinessSource.WEARABLE_SYNC, result?.source)

        // Verify breakdown
        assertNotNull("HRV should not be null", result?.breakdown?.hrv)
        assertEquals(80.0, result?.breakdown?.hrv?.value ?: 0.0, 0.01)
        assertEquals(0.4, result?.breakdown?.hrv?.weight ?: 0.0, 0.01)

        assertNotNull("Sleep should not be null", result?.breakdown?.sleepQuality)
        assertEquals(70.0, result?.breakdown?.sleepQuality?.value ?: 0.0, 0.01)
        assertEquals(0.4, result?.breakdown?.sleepQuality?.weight ?: 0.0, 0.01)

        assertNotNull("Stress should not be null", result?.breakdown?.stressLevel)
        assertEquals(75.0, result?.breakdown?.stressLevel?.value ?: 0.0, 0.01)
        assertEquals(0.2, result?.breakdown?.stressLevel?.weight ?: 0.0, 0.01)
    }

    /**
     * Test: Get most recent returns null when no scores exist.
     */
    @Test
    fun `getMostRecent returns null when no scores exist`() = runTest {
        // Given
        coEvery { readinessScoreDao.getMostRecent() } returns null

        // When
        val result = repository.getMostRecent()

        // Then
        assertNull("Result should be null", result)
    }

    /**
     * Test: Get history returns scores in range.
     *
     * Validates:
     * - DAO is called with correct date range
     * - Results are ordered descending (most recent first)
     * - Entities are mapped to domain
     */
    @Test
    fun `getHistory returns scores in date range descending`() = runTest {
        // Given
        val now = System.currentTimeMillis()
        val twoDaysAgo = now - (2 * 24 * 60 * 60 * 1000L)
        val oneDayAgo = now - (1 * 24 * 60 * 60 * 1000L)

        val entities = listOf(
            testEntity.copy(timestamp = twoDaysAgo, overall = 60),
            testEntity.copy(timestamp = oneDayAgo, overall = 70),
            testEntity.copy(timestamp = now, overall = 80)
        )

        coEvery {
            readinessScoreDao.getScoresInRange(any(), any())
        } returns entities

        // When
        val result = repository.getHistory(days = 7)

        // Then
        assertEquals("Should return 3 scores", 3, result.size)
        // Repository reverses the list (DAO returns ascending, we want descending)
        assertEquals("First should be most recent (80)", 80, result[0].overall)
        assertEquals("Second should be one day ago (70)", 70, result[1].overall)
        assertEquals("Third should be two days ago (60)", 60, result[2].overall)

        coVerify(exactly = 1) {
            readinessScoreDao.getScoresInRange(
                start = withArg { it <= now },
                end = withArg { it == now || it >= now }
            )
        }
    }

    /**
     * Test: Get history with no data returns empty list.
     */
    @Test
    fun `getHistory with no data returns empty list`() = runTest {
        // Given
        coEvery { readinessScoreDao.getScoresInRange(any(), any()) } returns emptyList()

        // When
        val result = repository.getHistory(days = 30)

        // Then
        assertEquals("Should return empty list", 0, result.size)
    }

    /**
     * Test: Save manual entry score.
     *
     * Validates:
     * - Manual entry source is persisted correctly
     */
    @Test
    fun `saveReadiness with manual entry persists source correctly`() = runTest {
        // Given
        val manualScore = testReadinessScore.copy(
            source = ReadinessSource.MANUAL_ENTRY
        )
        coEvery { readinessScoreDao.insert(any()) } returns Unit

        // When
        repository.saveReadiness(manualScore)

        // Then
        coVerify {
            readinessScoreDao.insert(
                withArg { entity ->
                    assertEquals("MANUAL_ENTRY", entity.source)
                }
            )
        }
    }

    /**
     * Test: Save score with missing metrics (null values).
     *
     * Validates:
     * - Null metrics are handled correctly
     * - Only available metrics are persisted
     */
    @Test
    fun `saveReadiness with null metrics persists correctly`() = runTest {
        // Given
        val partialScore = ReadinessScore(
            overall = 65,
            breakdown = ReadinessBreakdown(
                hrv = MetricScore(value = 70.0, weight = 0.4),
                sleepQuality = null,  // Missing sleep data
                stressLevel = null    // Missing stress data
            ),
            timestamp = testTimestamp,
            source = ReadinessSource.WEARABLE_SYNC
        )
        coEvery { readinessScoreDao.insert(any()) } returns Unit

        // When
        repository.saveReadiness(partialScore)

        // Then
        coVerify {
            readinessScoreDao.insert(
                withArg { entity ->
                    assertEquals(65, entity.overall)
                    assertEquals(70.0, entity.hrvScore ?: 0.0, 0.01)
                    assertNull("Sleep score should be null", entity.sleepScore)
                    assertNull("Stress score should be null", entity.stressScore)
                }
            )
        }
    }

    /**
     * Test: Retrieve score with null metrics maps correctly.
     *
     * Validates:
     * - Entity with null values maps to domain with null MetricScores
     */
    @Test
    fun `getMostRecent with null metrics maps to domain correctly`() = runTest {
        // Given
        val partialEntity = testEntity.copy(
            sleepScore = null,
            stressScore = null
        )
        coEvery { readinessScoreDao.getMostRecent() } returns partialEntity

        // When
        val result = repository.getMostRecent()

        // Then
        assertNotNull("Result should not be null", result)
        assertNotNull("HRV should be present", result?.breakdown?.hrv)
        assertNull("Sleep should be null", result?.breakdown?.sleepQuality)
        assertNull("Stress should be null", result?.breakdown?.stressLevel)
    }
}
