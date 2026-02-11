package com.caddypro.app.data.local.mappers

import com.caddypro.app.data.local.entities.RoundEntity
import com.caddypro.app.domain.model.Round

fun RoundEntity.toDomain(): Round {
    return Round(
        id = id,
        profileId = profileId,
        courseName = courseName,
        holesPlayed = holesPlayed,
        startedAt = startedAt,
        endedAt = endedAt,
        isActive = isActive,
        totalShots = totalShots,
        createdAt = createdAt,
        updatedAt = updatedAt,
        synced = synced
    )
}

fun Round.toEntity(): RoundEntity {
    return RoundEntity(
        id = id,
        profileId = profileId,
        courseName = courseName,
        holesPlayed = holesPlayed,
        startedAt = startedAt,
        endedAt = endedAt,
        isActive = isActive,
        totalShots = totalShots,
        createdAt = createdAt,
        updatedAt = updatedAt,
        synced = synced
    )
}
