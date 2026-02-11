package com.caddypro.app.data.remote.dto

import com.caddypro.app.data.local.entities.BagEntity
import com.caddypro.app.data.local.entities.ClubEntity
import com.caddypro.app.data.local.entities.PlayerProfileEntity
import com.caddypro.app.data.local.entities.PreferredUnits
import com.caddypro.app.domain.model.ClubType
import com.caddypro.app.domain.model.MissBias

// Profile mappers

fun PlayerProfileEntity.toDto(): ProfileDto = ProfileDto(
    id = id,
    supabaseUserId = supabaseUserId,
    displayName = displayName,
    handicapIndex = handicapIndex,
    preferredUnits = preferredUnits.name,
    homeCourseId = homeCourseId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ProfileDto.toEntity(): PlayerProfileEntity = PlayerProfileEntity(
    id = id,
    supabaseUserId = supabaseUserId,
    displayName = displayName,
    handicapIndex = handicapIndex,
    preferredUnits = try { PreferredUnits.valueOf(preferredUnits) } catch (_: Exception) { PreferredUnits.METRIC },
    homeCourseId = homeCourseId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    synced = true
)

// Bag mappers

fun BagEntity.toDto(): BagDto = BagDto(
    id = id,
    profileId = profileId,
    name = name,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun BagDto.toEntity(): BagEntity = BagEntity(
    id = id,
    profileId = profileId,
    name = name,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt,
    synced = true
)

// Club mappers

fun ClubEntity.toDto(): ClubDto = ClubDto(
    id = id,
    bagId = bagId,
    name = name,
    type = type.name,
    loft = loft,
    carryDistance = carryDistance,
    totalDistance = totalDistance,
    missBias = missBias.name,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun ClubDto.toEntity(): ClubEntity = ClubEntity(
    id = id,
    bagId = bagId,
    name = name,
    type = try { ClubType.valueOf(type) } catch (_: Exception) { ClubType.IRON },
    loft = loft,
    carryDistance = carryDistance,
    totalDistance = totalDistance,
    missBias = try { MissBias.valueOf(missBias) } catch (_: Exception) { MissBias.STRAIGHT },
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    synced = true
)
