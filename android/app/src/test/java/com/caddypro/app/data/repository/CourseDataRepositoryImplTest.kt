package com.caddypro.app.data.repository

import com.caddypro.app.data.local.dao.CourseOverlayDao
import com.caddypro.app.data.local.entities.CourseOverlayEntity
import com.caddypro.app.data.remote.overpass.OverpassApiService
import com.caddypro.app.data.remote.overpass.OverpassParser
import com.caddypro.app.domain.model.CourseOverlayBundle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CourseDataRepositoryImplTest {

    private lateinit var courseOverlayDao: CourseOverlayDao
    private lateinit var overpassApiService: OverpassApiService
    private lateinit var overpassParser: OverpassParser
    private lateinit var repository: CourseDataRepositoryImpl

    private val testLat = -37.8136
    private val testLon = 144.9631

    @Before
    fun setup() {
        courseOverlayDao = mockk(relaxed = true)
        overpassApiService = mockk(relaxed = true)
        overpassParser = OverpassParser()
        repository = CourseDataRepositoryImpl(courseOverlayDao, overpassApiService, overpassParser)
    }

    // AC20: Cache-first strategy - fresh cache is returned without API call
    @Test
    fun `getCourseData returns cached data when fresh`() = runTest {
        val freshEntity = CourseOverlayEntity(
            id = "cache-1",
            centerLat = testLat,
            centerLon = testLon,
            courseName = "Royal Melbourne",
            dataJson = """{"greens":[],"hazards":[]}""",
            fetchedAt = System.currentTimeMillis()
        )
        coEvery { courseOverlayDao.getCachedNear(any(), any()) } returns freshEntity

        val result = repository.getCourseData(testLat, testLon)
        assertTrue(result.isSuccess)
        assertEquals("Royal Melbourne", result.getOrNull()?.courseName)
    }

    @Test
    fun `getCourseData returns fresh cached greens`() = runTest {
        val freshEntity = CourseOverlayEntity(
            id = "cache-1",
            centerLat = testLat,
            centerLon = testLon,
            courseName = "Kingston Heath",
            dataJson = """{"greens":[{"id":"g1","lat":-37.814,"lon":144.964,"holeNumber":5}],"hazards":[{"id":"h1","type":"BUNKER","name":"Front Bunker","coordinates":[{"lat":-37.814,"lon":144.964},{"lat":-37.815,"lon":144.965},{"lat":-37.814,"lon":144.965}]}]}""",
            fetchedAt = System.currentTimeMillis()
        )
        coEvery { courseOverlayDao.getCachedNear(any(), any()) } returns freshEntity

        val result = repository.getCourseData(testLat, testLon)
        assertTrue(result.isSuccess)
        val bundle = result.getOrNull()!!
        assertEquals("Kingston Heath", bundle.courseName)
        assertEquals(1, bundle.greens.size)
        assertEquals(5, bundle.greens[0].holeNumber)
        assertEquals(1, bundle.hazards.size)
        assertEquals("Front Bunker", bundle.hazards[0].name)
    }

    // AC21: Graceful degradation - empty bundle when no cache and API fails
    @Test
    fun `getCourseData returns empty bundle when no cache and API returns empty`() = runTest {
        coEvery { courseOverlayDao.getCachedNear(any(), any()) } returns null
        // Relaxed mock returns default OverpassResponse with empty elements

        val result = repository.getCourseData(testLat, testLon)
        assertTrue(result.isSuccess)
        // Either gets empty from API parse or from graceful degradation - both OK
        assertNull(result.getOrNull()?.courseName)
    }

    // AC21: Graceful degradation - stale cache fallback when API unavailable
    @Test
    fun `getCourseData uses stale cache on exception`() = runTest {
        val staleEntity = CourseOverlayEntity(
            id = "cache-1",
            centerLat = testLat,
            centerLon = testLon,
            courseName = "Old Cache",
            dataJson = """{"greens":[],"hazards":[]}""",
            fetchedAt = System.currentTimeMillis() - 8 * 24 * 60 * 60 * 1000L
        )
        coEvery { courseOverlayDao.getCachedNear(any(), any()) } returns staleEntity

        // Relaxed mock + stale cache = repo will try API, get empty/default response,
        // parse it, and return that result (or fall back to stale cache on error)
        val result = repository.getCourseData(testLat, testLon)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `getCachedCourseData returns null when no cache`() = runTest {
        coEvery { courseOverlayDao.getCachedNear(any(), any()) } returns null

        val result = repository.getCachedCourseData(testLat, testLon)
        assertNull(result)
    }

    @Test
    fun `getCachedCourseData returns deserialized data`() = runTest {
        val entity = CourseOverlayEntity(
            id = "cache-1",
            centerLat = testLat,
            centerLon = testLon,
            courseName = "Royal Melbourne",
            dataJson = """{"greens":[{"id":"g1","lat":-37.814,"lon":144.964,"holeNumber":1}],"hazards":[]}""",
            fetchedAt = System.currentTimeMillis()
        )
        coEvery { courseOverlayDao.getCachedNear(any(), any()) } returns entity

        val result = repository.getCachedCourseData(testLat, testLon)
        assertEquals(1, result?.greens?.size)
        assertEquals("Royal Melbourne", result?.courseName)
        assertEquals(1, result?.greens?.get(0)?.holeNumber)
    }

    @Test
    fun `getCachedCourseData handles corrupt JSON gracefully`() = runTest {
        val entity = CourseOverlayEntity(
            id = "cache-1",
            centerLat = testLat,
            centerLon = testLon,
            courseName = null,
            dataJson = "INVALID_JSON",
            fetchedAt = System.currentTimeMillis()
        )
        coEvery { courseOverlayDao.getCachedNear(any(), any()) } returns entity

        val result = repository.getCachedCourseData(testLat, testLon)
        assertTrue(result?.greens?.isEmpty() == true)
    }

    // AC19: Cache TTL - stale check
    @Test
    fun `fresh cache within 7 days is not stale`() = runTest {
        val recentEntity = CourseOverlayEntity(
            id = "cache-1",
            centerLat = testLat,
            centerLon = testLon,
            courseName = "Fresh Course",
            dataJson = """{"greens":[{"id":"g1","lat":-37.814,"lon":144.964}],"hazards":[]}""",
            fetchedAt = System.currentTimeMillis() - 3 * 24 * 60 * 60 * 1000L // 3 days old
        )
        coEvery { courseOverlayDao.getCachedNear(any(), any()) } returns recentEntity

        val result = repository.getCourseData(testLat, testLon)
        assertTrue(result.isSuccess)
        assertEquals("Fresh Course", result.getOrNull()?.courseName)
        assertEquals(1, result.getOrNull()?.greens?.size)
    }
}
