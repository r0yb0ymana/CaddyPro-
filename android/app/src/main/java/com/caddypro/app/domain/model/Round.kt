package com.caddypro.app.domain.model

import java.util.UUID

/**
 * Round domain model
 */
data class Round(
    val id: String = UUID.randomUUID().toString(),
    val profileId: String,
    val courseName: String,
    val holesPlayed: Int = 18,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null,
    val isActive: Boolean = true,
    val totalShots: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
