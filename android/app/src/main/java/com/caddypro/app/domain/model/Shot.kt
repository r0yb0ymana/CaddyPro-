package com.caddypro.app.domain.model

import java.util.UUID

/**
 * Shot domain model
 */
data class Shot(
    val id: String = UUID.randomUUID().toString(),
    val roundId: String,
    val holeNumber: Int,
    val shotNumber: Int,
    val clubId: String,
    val clubName: String,
    val shotType: ShotType,
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
