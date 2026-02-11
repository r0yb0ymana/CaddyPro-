package com.caddypro.app.data.local.mappers

import com.caddypro.app.data.local.entities.ShotEntity
import com.caddypro.app.domain.model.Shot

fun ShotEntity.toDomain(): Shot {
    return Shot(
        id = id,
        roundId = roundId,
        holeNumber = holeNumber,
        shotNumber = shotNumber,
        clubId = clubId,
        clubName = clubName,
        shotType = shotType,
        startLatitude = startLatitude,
        startLongitude = startLongitude,
        endLatitude = endLatitude,
        endLongitude = endLongitude,
        timestamp = timestamp,
        createdAt = createdAt,
        updatedAt = updatedAt,
        synced = synced
    )
}

fun Shot.toEntity(): ShotEntity {
    return ShotEntity(
        id = id,
        roundId = roundId,
        holeNumber = holeNumber,
        shotNumber = shotNumber,
        clubId = clubId,
        clubName = clubName,
        shotType = shotType,
        startLatitude = startLatitude,
        startLongitude = startLongitude,
        endLatitude = endLatitude,
        endLongitude = endLongitude,
        timestamp = timestamp,
        createdAt = createdAt,
        updatedAt = updatedAt,
        synced = synced
    )
}
