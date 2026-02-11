package com.caddypro.app.data.repository

import com.caddypro.app.data.local.dao.WeatherDao
import com.caddypro.app.data.local.mappers.toDomain
import com.caddypro.app.data.local.mappers.toEntity
import com.caddypro.app.data.remote.weather.WeatherApiService
import com.caddypro.app.domain.model.WeatherData
import com.caddypro.app.domain.repository.WeatherRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Weather repository implementation
 *
 * Strategy: Return cached data immediately, refresh from API when stale.
 * AC15: Cached weather shown immediately
 * AC16: Stale data warning when cache older than 30 min
 * AC18: Weather cache persists across app restarts via Room
 */
@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val weatherDao: WeatherDao,
    private val weatherApiService: WeatherApiService
) : WeatherRepository {

    override suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherData> {
        val cached = weatherDao.getLatestWeather()

        // If cache is fresh enough (< 10 min), return it
        if (cached != null) {
            val age = System.currentTimeMillis() - cached.fetchedAt
            if (age < WeatherData.REFRESH_INTERVAL_MS) {
                return Result.success(cached.toDomain())
            }
        }

        // Try to fetch fresh data
        return try {
            val response = weatherApiService.getCurrentWeather(latitude, longitude)
            val entity = response.toEntity(latitude, longitude)
            weatherDao.insertWeather(entity)
            // Clean up old entries (older than 24 hours)
            weatherDao.deleteOldEntries(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            // If API fails, return cached data if available
            if (cached != null) {
                Result.success(cached.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getCachedWeather(): WeatherData? {
        return weatherDao.getLatestWeather()?.toDomain()
    }

    override suspend fun refreshWeather(latitude: Double, longitude: Double): Result<WeatherData> {
        return try {
            val response = weatherApiService.getCurrentWeather(latitude, longitude)
            val entity = response.toEntity(latitude, longitude)
            weatherDao.insertWeather(entity)
            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
