package com.caddypro.app.data.repository

import com.caddypro.app.data.local.dao.CourseOverlayDao
import com.caddypro.app.data.local.entities.CourseOverlayEntity
import com.caddypro.app.data.remote.overpass.OverpassApiService
import com.caddypro.app.data.remote.overpass.OverpassParser
import com.caddypro.app.domain.model.CourseOverlayBundle
import com.caddypro.app.domain.model.GreenData
import com.caddypro.app.domain.model.HazardData
import com.caddypro.app.domain.model.HazardType
import com.caddypro.app.domain.model.LatLngPoint
import com.caddypro.app.domain.repository.CourseDataRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Course data repository implementation
 *
 * AC19: Overpass API data cached in Room for 7 days
 * AC20: Cache-first strategy
 * AC21: Graceful degradation on API failure
 */
@Singleton
class CourseDataRepositoryImpl @Inject constructor(
    private val courseOverlayDao: CourseOverlayDao,
    private val overpassApiService: OverpassApiService,
    private val overpassParser: OverpassParser
) : CourseDataRepository {

    companion object {
        private const val CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
    }

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getCourseData(lat: Double, lon: Double): Result<CourseOverlayBundle> {
        // Check cache first
        val cached = courseOverlayDao.getCachedNear(lat, lon)
        if (cached != null && !isStale(cached.fetchedAt)) {
            return Result.success(deserializeBundle(cached))
        }

        // Fetch from Overpass API
        return try {
            val response = overpassApiService.getGolfFeatures(lat, lon)
            val bundle = overpassParser.parse(response)

            // Cache the result
            val entity = serializeBundle(lat, lon, bundle)
            courseOverlayDao.insert(entity)

            // Cleanup old entries
            courseOverlayDao.deleteOldEntries(
                System.currentTimeMillis() - CACHE_TTL_MS * 2
            )

            Result.success(bundle)
        } catch (e: Exception) {
            // Fallback to stale cache if available
            if (cached != null) {
                Result.success(deserializeBundle(cached))
            } else {
                // Graceful degradation: return empty bundle (satellite only)
                Result.success(CourseOverlayBundle())
            }
        }
    }

    override suspend fun getCachedCourseData(lat: Double, lon: Double): CourseOverlayBundle? {
        val cached = courseOverlayDao.getCachedNear(lat, lon) ?: return null
        return deserializeBundle(cached)
    }

    private fun isStale(fetchedAt: Long): Boolean {
        return System.currentTimeMillis() - fetchedAt > CACHE_TTL_MS
    }

    private fun serializeBundle(lat: Double, lon: Double, bundle: CourseOverlayBundle): CourseOverlayEntity {
        val cacheData = CacheData(
            greens = bundle.greens.map { green ->
                CacheGreen(green.id, green.latitude, green.longitude, green.holeNumber)
            },
            hazards = bundle.hazards.map { hazard ->
                CacheHazard(
                    hazard.id, hazard.type.name, hazard.name,
                    hazard.coordinates.map { CacheCoord(it.latitude, it.longitude) }
                )
            }
        )

        return CourseOverlayEntity(
            id = UUID.randomUUID().toString(),
            centerLat = lat,
            centerLon = lon,
            courseName = bundle.courseName,
            dataJson = json.encodeToString(cacheData)
        )
    }

    private fun deserializeBundle(entity: CourseOverlayEntity): CourseOverlayBundle {
        return try {
            val cacheData = json.decodeFromString<CacheData>(entity.dataJson)
            CourseOverlayBundle(
                greens = cacheData.greens.map { green ->
                    GreenData(green.id, green.lat, green.lon, green.holeNumber)
                },
                hazards = cacheData.hazards.map { hazard ->
                    HazardData(
                        hazard.id,
                        HazardType.valueOf(hazard.type),
                        hazard.name,
                        hazard.coordinates.map { LatLngPoint(it.lat, it.lon) }
                    )
                },
                courseName = entity.courseName,
                fetchedAt = entity.fetchedAt
            )
        } catch (e: Exception) {
            CourseOverlayBundle()
        }
    }

}

@Serializable
internal data class CacheData(
    val greens: List<CacheGreen>,
    val hazards: List<CacheHazard>
)

@Serializable
internal data class CacheGreen(
    val id: String,
    val lat: Double,
    val lon: Double,
    val holeNumber: Int? = null
)

@Serializable
internal data class CacheHazard(
    val id: String,
    val type: String,
    val name: String? = null,
    val coordinates: List<CacheCoord>
)

@Serializable
internal data class CacheCoord(
    val lat: Double,
    val lon: Double
)
