package com.caddypro.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase DTO for clubs table
 */
@Serializable
data class ClubDto(
    val id: String,
    @SerialName("bag_id") val bagId: String,
    val name: String,
    val type: String,
    val loft: Float? = null,
    @SerialName("carry_distance") val carryDistance: Int,
    @SerialName("total_distance") val totalDistance: Int,
    @SerialName("miss_bias") val missBias: String = "STRAIGHT",
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)
