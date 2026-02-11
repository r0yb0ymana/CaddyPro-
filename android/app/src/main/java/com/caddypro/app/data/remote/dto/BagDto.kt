package com.caddypro.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase DTO for bags table
 */
@Serializable
data class BagDto(
    val id: String,
    @SerialName("profile_id") val profileId: String,
    val name: String,
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)
