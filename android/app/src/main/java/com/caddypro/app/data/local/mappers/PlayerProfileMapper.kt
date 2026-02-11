package com.caddypro.app.data.local.mappers

import com.caddypro.app.data.local.entities.PlayerProfileEntity
import com.caddypro.app.domain.model.PlayerProfile

/**
 * Mapper functions to convert between PlayerProfile domain model and entity
 */

fun PlayerProfileEntity.toDomain(): PlayerProfile {
    return PlayerProfile(
        id = id,
        supabaseUserId = supabaseUserId,
        displayName = displayName,
        handicapIndex = handicapIndex,
        preferredUnits = preferredUnits,
        homeCourseId = homeCourseId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        synced = synced
    )
}

fun PlayerProfile.toEntity(): PlayerProfileEntity {
    return PlayerProfileEntity(
        id = id,
        supabaseUserId = supabaseUserId,
        displayName = displayName,
        handicapIndex = handicapIndex,
        preferredUnits = preferredUnits,
        homeCourseId = homeCourseId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        synced = synced
    )
}
