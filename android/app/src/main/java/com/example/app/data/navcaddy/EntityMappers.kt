package caddypro.data.navcaddy

import caddypro.data.navcaddy.entities.BagClubEntity
import caddypro.data.navcaddy.entities.BagProfileEntity
import caddypro.data.navcaddy.entities.ConversationTurnEntity
import caddypro.data.navcaddy.entities.DistanceAuditEntity
import caddypro.data.navcaddy.entities.MissPatternEntity
import caddypro.data.navcaddy.entities.SessionEntity
import caddypro.data.navcaddy.entities.ShotEntity
import com.example.app.domain.navcaddy.models.BagProfile
import com.example.app.domain.navcaddy.models.Club
import com.example.app.domain.navcaddy.models.DistanceAuditEntry
import com.example.app.domain.navcaddy.models.MissBias
import com.example.app.domain.navcaddy.models.MissBiasDirection
import com.example.app.domain.navcaddy.models.MissType
import caddypro.domain.navcaddy.models.ClubType
import caddypro.domain.navcaddy.models.ConversationTurn
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.MissPattern
import caddypro.domain.navcaddy.models.PressureContext
import caddypro.domain.navcaddy.models.Role
import caddypro.domain.navcaddy.models.SessionContext
import caddypro.domain.navcaddy.models.Shot
import java.util.UUID

/**
 * Extension functions for mapping between Room entities and domain models.
 *
 * These mappers handle the conversion between database representations
 * (flattened, string-based) and domain models (typed, structured).
 */

// ============================================================================
// Shot Mappings
// ============================================================================

/**
 * Convert ShotEntity to domain Shot model.
 */
fun ShotEntity.toDomain(): Shot {
    return Shot(
        id = id,
        timestamp = timestamp,
        club = Club(
            id = clubId,
            name = clubName,
            type = ClubType.valueOf(clubType),
            estimatedCarry = 0 // Not stored in ShotEntity
        ),
        missDirection = missDirection?.let { MissDirection.valueOf(it) },
        lie = Lie.valueOf(lie),
        pressureContext = PressureContext(
            isUserTagged = isUserTaggedPressure,
            isInferred = isInferredPressure,
            scoringContext = scoringContext
        ),
        holeNumber = holeNumber,
        notes = notes
    )
}

/**
 * Convert domain Shot to ShotEntity.
 */
fun Shot.toEntity(): ShotEntity {
    return ShotEntity(
        id = id,
        timestamp = timestamp,
        clubId = club.id,
        clubName = club.name,
        clubType = club.type.name,
        missDirection = missDirection?.name,
        lie = lie.name,
        isUserTaggedPressure = pressureContext.isUserTagged,
        isInferredPressure = pressureContext.isInferred,
        scoringContext = pressureContext.scoringContext,
        holeNumber = holeNumber,
        notes = notes
    )
}

// ============================================================================
// MissPattern Mappings
// ============================================================================

/**
 * Convert MissPatternEntity to domain MissPattern model.
 */
fun MissPatternEntity.toDomain(): MissPattern {
    return MissPattern(
        id = id,
        direction = MissDirection.valueOf(direction),
        club = if (clubId != null && clubName != null && clubType != null) {
            Club(
                id = clubId,
                name = clubName,
                type = ClubType.valueOf(clubType),
                estimatedCarry = 0 // Not stored in MissPatternEntity
            )
        } else {
            null
        },
        frequency = frequency,
        confidence = confidence,
        pressureContext = if (isUserTaggedPressure != null || isInferredPressure != null) {
            PressureContext(
                isUserTagged = isUserTaggedPressure ?: false,
                isInferred = isInferredPressure ?: false,
                scoringContext = scoringContext
            )
        } else {
            null
        },
        lastOccurrence = lastOccurrence
    )
}

/**
 * Convert domain MissPattern to MissPatternEntity.
 */
fun MissPattern.toEntity(): MissPatternEntity {
    return MissPatternEntity(
        id = id,
        direction = direction.name,
        clubId = club?.id,
        clubName = club?.name,
        clubType = club?.type?.name,
        frequency = frequency,
        confidence = confidence,
        isUserTaggedPressure = pressureContext?.isUserTagged,
        isInferredPressure = pressureContext?.isInferred,
        scoringContext = pressureContext?.scoringContext,
        lastOccurrence = lastOccurrence
    )
}

// ============================================================================
// Session Mappings
// ============================================================================

/**
 * Convert SessionEntity to domain SessionContext model.
 *
 * Note: Conversation history is loaded separately via SessionDao.
 */
fun SessionEntity.toDomain(conversationHistory: List<ConversationTurn> = emptyList()): SessionContext {
    return SessionContext(
        currentRound = null, // Round loaded separately if roundId is not null
        currentHole = currentHole,
        lastShot = null, // Last shot loaded separately if needed
        lastRecommendation = lastRecommendation,
        conversationHistory = conversationHistory
    )
}

/**
 * Convert domain SessionContext to SessionEntity.
 *
 * Note: Conversation history is persisted separately via SessionDao.
 */
fun SessionContext.toEntity(): SessionEntity {
    return SessionEntity(
        id = "current",
        roundId = currentRound?.id,
        currentHole = currentHole,
        lastRecommendation = lastRecommendation,
        updatedAt = System.currentTimeMillis()
    )
}

// ============================================================================
// ConversationTurn Mappings
// ============================================================================

/**
 * Convert ConversationTurnEntity to domain ConversationTurn model.
 */
fun ConversationTurnEntity.toDomain(): ConversationTurn {
    return ConversationTurn(
        role = Role.valueOf(role),
        content = content,
        timestamp = timestamp
    )
}

/**
 * Convert domain ConversationTurn to ConversationTurnEntity.
 */
fun ConversationTurn.toEntity(sessionId: String = "current"): ConversationTurnEntity {
    return ConversationTurnEntity(
        sessionId = sessionId,
        role = role.name,
        content = content,
        timestamp = timestamp
    )
}

// ============================================================================
// BagProfile Mappings
// ============================================================================

/**
 * Convert BagProfileEntity to domain BagProfile model.
 */
fun BagProfileEntity.toDomain(): BagProfile {
    return BagProfile(
        id = id,
        name = name,
        isActive = isActive,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Convert domain BagProfile to BagProfileEntity.
 */
fun BagProfile.toEntity(): BagProfileEntity {
    return BagProfileEntity(
        id = id,
        name = name,
        isActive = isActive,
        isArchived = isArchived,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// ============================================================================
// BagClub Mappings
// ============================================================================

/**
 * Convert BagClubEntity to domain Club model.
 */
fun BagClubEntity.toDomain(): Club {
    return Club(
        id = clubId,
        name = name,
        type = ClubType.valueOf(type),
        loft = loft,
        estimatedCarry = estimatedCarry,
        inferredCarry = inferredCarry,
        inferredConfidence = inferredConfidence,
        missBias = if (missBiasDirection != null) {
            MissBias(
                dominantDirection = MissBiasDirection.valueOf(missBiasDirection),
                missType = missBiasType?.let { MissType.valueOf(it) },
                isUserDefined = missBiasIsUserDefined ?: false,
                confidence = missBiasConfidence ?: 0.0,
                lastUpdated = missBiasLastUpdated ?: System.currentTimeMillis()
            )
        } else {
            null
        },
        shaft = shaft,
        flex = flex,
        notes = notes
    )
}

/**
 * Convert domain Club to BagClubEntity.
 *
 * @param bagId The bag this club belongs to
 * @param position The position of this club in the bag
 */
fun Club.toEntity(bagId: String, position: Int): BagClubEntity {
    return BagClubEntity(
        id = UUID.randomUUID().toString(),
        bagId = bagId,
        clubId = id,
        position = position,
        name = name,
        type = type.name,
        loft = loft,
        estimatedCarry = estimatedCarry,
        inferredCarry = inferredCarry,
        inferredConfidence = inferredConfidence,
        missBiasDirection = missBias?.dominantDirection?.name,
        missBiasType = missBias?.missType?.name,
        missBiasIsUserDefined = missBias?.isUserDefined,
        missBiasConfidence = missBias?.confidence,
        missBiasLastUpdated = missBias?.lastUpdated,
        shaft = shaft,
        flex = flex,
        notes = notes
    )
}

// ============================================================================
// DistanceAudit Mappings
// ============================================================================

/**
 * Convert DistanceAuditEntity to domain DistanceAuditEntry model.
 */
fun DistanceAuditEntity.toDomain(): DistanceAuditEntry {
    return DistanceAuditEntry(
        id = id,
        clubId = clubId,
        oldEstimated = oldEstimated,
        newEstimated = newEstimated,
        inferredValue = inferredValue,
        confidence = confidence,
        reason = reason,
        timestamp = timestamp,
        wasAccepted = wasAccepted
    )
}

/**
 * Convert domain DistanceAuditEntry to DistanceAuditEntity.
 */
fun DistanceAuditEntry.toEntity(): DistanceAuditEntity {
    return DistanceAuditEntity(
        id = id,
        clubId = clubId,
        oldEstimated = oldEstimated,
        newEstimated = newEstimated,
        inferredValue = inferredValue,
        confidence = confidence,
        reason = reason,
        timestamp = timestamp,
        wasAccepted = wasAccepted
    )
}
