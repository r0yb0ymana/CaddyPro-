package com.caddypro.app.domain.repository

import com.caddypro.app.domain.model.CourseOverlayBundle

/**
 * Repository interface for course overlay data (Overpass API + Room cache)
 */
interface CourseDataRepository {
    suspend fun getCourseData(lat: Double, lon: Double): Result<CourseOverlayBundle>
    suspend fun getCachedCourseData(lat: Double, lon: Double): CourseOverlayBundle?
}
