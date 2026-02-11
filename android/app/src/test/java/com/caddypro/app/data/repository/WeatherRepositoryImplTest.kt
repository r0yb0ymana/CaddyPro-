package com.caddypro.app.data.repository

import com.caddypro.app.data.local.dao.WeatherDao
import com.caddypro.app.data.local.entities.WeatherEntity
import com.caddypro.app.data.remote.weather.MainData
import com.caddypro.app.data.remote.weather.WeatherApiResponse
import com.caddypro.app.data.remote.weather.WeatherApiService
import com.caddypro.app.data.remote.weather.WeatherCondition
import com.caddypro.app.data.remote.weather.WindData
import com.caddypro.app.domain.model.WeatherData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for WeatherRepositoryImpl
 *
 * AC15: Cached weather shown immediately
 * AC16: Stale data warning when cache older than 30 min
 * AC17: Base distances shown when no cache and no network
 * AC18: Weather cache persists across app restarts via Room
 */
class WeatherRepositoryImplTest {

    private lateinit var repository: WeatherRepositoryImpl
    private lateinit var weatherDao: WeatherDao
    private lateinit var weatherApiService: WeatherApiService

    private val testLat = -37.8136
    private val testLon = 144.9631

    private val freshCachedWeather = WeatherEntity(
        latitude = testLat,
        longitude = testLon,
        temperatureC = 22.0,
        windSpeedKmh = 15.0,
        windDirectionDeg = 180,
        humidity = 65,
        conditionCode = 802,
        conditionDescription = "scattered clouds",
        feelsLikeC = 21.0,
        pressureHpa = 1015,
        fetchedAt = System.currentTimeMillis() // fresh
    )

    private val staleCachedWeather = freshCachedWeather.copy(
        fetchedAt = System.currentTimeMillis() - 15 * 60 * 1000L // 15 min old (stale for refresh, not for display)
    )

    private val apiResponse = WeatherApiResponse(
        main = MainData(
            temp = 25.0,
            humidity = 60,
            pressure = 1013,
            feelsLike = 24.0
        ),
        wind = WindData(speed = 5.0, deg = 270),
        weather = listOf(WeatherCondition(id = 800, description = "clear sky"))
    )

    @Before
    fun setup() {
        weatherDao = mockk(relaxed = true)
        weatherApiService = mockk(relaxed = true)
        repository = WeatherRepositoryImpl(weatherDao, weatherApiService)
    }

    // AC15: Return cached data immediately when fresh

    @Test
    fun `getWeather returns fresh cached data without API call`() = runTest {
        coEvery { weatherDao.getLatestWeather() } returns freshCachedWeather

        val result = repository.getWeather(testLat, testLon)

        assertTrue("Should succeed", result.isSuccess)
        assertEquals(22.0, result.getOrNull()!!.temperatureC, 0.01)
        coVerify(exactly = 0) { weatherApiService.getCurrentWeather(any(), any()) }
    }

    // AC2: Refresh when stale

    @Test
    fun `getWeather fetches from API when cache is stale`() = runTest {
        coEvery { weatherDao.getLatestWeather() } returns staleCachedWeather
        coEvery { weatherApiService.getCurrentWeather(testLat, testLon) } returns apiResponse

        val result = repository.getWeather(testLat, testLon)

        assertTrue("Should succeed", result.isSuccess)
        assertEquals(25.0, result.getOrNull()!!.temperatureC, 0.01)
        coVerify { weatherApiService.getCurrentWeather(testLat, testLon) }
        coVerify { weatherDao.insertWeather(any()) }
    }

    // AC17: Fall back to cache when API fails

    @Test
    fun `getWeather returns stale cache when API fails`() = runTest {
        coEvery { weatherDao.getLatestWeather() } returns staleCachedWeather
        coEvery { weatherApiService.getCurrentWeather(any(), any()) } throws Exception("Network error")

        val result = repository.getWeather(testLat, testLon)

        assertTrue("Should succeed with cached data", result.isSuccess)
        assertEquals(22.0, result.getOrNull()!!.temperatureC, 0.01)
    }

    @Test
    fun `getWeather returns failure when no cache and API fails`() = runTest {
        coEvery { weatherDao.getLatestWeather() } returns null
        coEvery { weatherApiService.getCurrentWeather(any(), any()) } throws Exception("Network error")

        val result = repository.getWeather(testLat, testLon)

        assertTrue("Should fail without cache or API", result.isFailure)
    }

    @Test
    fun `getWeather fetches from API when no cache exists`() = runTest {
        coEvery { weatherDao.getLatestWeather() } returns null
        coEvery { weatherApiService.getCurrentWeather(testLat, testLon) } returns apiResponse

        val result = repository.getWeather(testLat, testLon)

        assertTrue("Should succeed from API", result.isSuccess)
        assertEquals(25.0, result.getOrNull()!!.temperatureC, 0.01)
        // Wind speed converted from m/s to km/h: 5.0 * 3.6 = 18.0
        assertEquals(18.0, result.getOrNull()!!.windSpeedKmh, 0.01)
    }

    // AC18: Cache persists

    @Test
    fun `getCachedWeather returns cached data`() = runTest {
        coEvery { weatherDao.getLatestWeather() } returns freshCachedWeather

        val result = repository.getCachedWeather()

        assertNotNull("Cached weather should not be null", result)
        assertEquals(22.0, result!!.temperatureC, 0.01)
    }

    @Test
    fun `getCachedWeather returns null when no cache`() = runTest {
        coEvery { weatherDao.getLatestWeather() } returns null

        val result = repository.getCachedWeather()

        assertNull("Should return null when no cache", result)
    }

    // refreshWeather

    @Test
    fun `refreshWeather always calls API`() = runTest {
        coEvery { weatherApiService.getCurrentWeather(testLat, testLon) } returns apiResponse

        val result = repository.refreshWeather(testLat, testLon)

        assertTrue("Should succeed", result.isSuccess)
        coVerify { weatherApiService.getCurrentWeather(testLat, testLon) }
        coVerify { weatherDao.insertWeather(any()) }
    }

    @Test
    fun `refreshWeather returns failure when API fails`() = runTest {
        coEvery { weatherApiService.getCurrentWeather(any(), any()) } throws Exception("Timeout")

        val result = repository.refreshWeather(testLat, testLon)

        assertTrue("Should fail", result.isFailure)
    }

    // Cleanup

    @Test
    fun `getWeather cleans up old entries after successful fetch`() = runTest {
        coEvery { weatherDao.getLatestWeather() } returns null
        coEvery { weatherApiService.getCurrentWeather(testLat, testLon) } returns apiResponse

        repository.getWeather(testLat, testLon)

        coVerify { weatherDao.deleteOldEntries(any()) }
    }
}
