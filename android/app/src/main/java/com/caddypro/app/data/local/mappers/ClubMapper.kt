package com.caddypro.app.data.local.mappers

import com.caddypro.app.data.local.entities.ClubEntity
import com.caddypro.app.domain.model.Club

/**
 * Mapper between ClubEntity and Club domain model
 */
fun ClubEntity.toDomain(): Club {
    return Club(
        id = id,
        bagId = bagId,
        name = name,
        type = type,
        loft = loft,
        carryDistance = carryDistance,
        totalDistance = totalDistance,
        missBias = missBias,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        synced = synced
    )
}

fun Club.toEntity(): ClubEntity {
    return ClubEntity(
        id = id,
        bagId = bagId,
        name = name,
        type = type,
        loft = loft,
        carryDistance = carryDistance,
        totalDistance = totalDistance,
        missBias = missBias,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
        synced = synced
    )
}
