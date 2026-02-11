package com.caddypro.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase DTO for player_profiles table
 */
@Serializable
data class ProfileDto(
    val id: String,
    @SerialName("supabase_user_id") val supabaseUserId: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("handicap_index") val handicapIndex: Float? = null,
    @SerialName("preferred_units") val preferredUnits: String = "METRIC",
    @SerialName("home_course_id") val homeCourseId: String? = null,
    @SerialName("created_at") val createdAt: Long,
    @SerialName("updated_at") val updatedAt: Long
)
