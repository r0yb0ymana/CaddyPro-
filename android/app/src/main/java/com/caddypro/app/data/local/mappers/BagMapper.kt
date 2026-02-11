package com.caddypro.app.data.local.mappers

import com.caddypro.app.data.local.entities.BagEntity
import com.caddypro.app.domain.model.Bag

/**
 * Mapper functions to convert between Bag domain model and entity
 */

fun BagEntity.toDomain(clubCount: Int = 0): Bag {
    return Bag(
        id = id,
        profileId = profileId,
        name = name,
        isActive = isActive,
        clubCount = clubCount,
        createdAt = createdAt,
        updatedAt = updatedAt,
        synced = synced
    )
}

fun Bag.toEntity(): BagEntity {
    return BagEntity(
        id = id,
        profileId = profileId,
        name = name,
        isActive = isActive,
        createdAt = createdAt,
        updatedAt = updatedAt,
        synced = synced
    )
}
