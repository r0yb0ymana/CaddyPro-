package caddypro.data.navcaddy

import caddypro.data.navcaddy.entities.ConversationTurnEntity
import caddypro.data.navcaddy.entities.MissPatternEntity
import caddypro.data.navcaddy.entities.SessionEntity
import caddypro.data.navcaddy.entities.ShotEntity
import caddypro.domain.navcaddy.models.Club
import caddypro.domain.navcaddy.models.ClubType
import caddypro.domain.navcaddy.models.ConversationTurn
import caddypro.domain.navcaddy.models.Lie
import caddypro.domain.navcaddy.models.MissDirection
import caddypro.domain.navcaddy.models.MissPattern
import caddypro.domain.navcaddy.models.PressureContext
import caddypro.domain.navcaddy.models.Role
import caddypro.domain.navcaddy.models.SessionContext
import caddypro.domain.navcaddy.models.Shot

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
            type = ClubType.valueOf(clubType)
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
                type = ClubType.valueOf(clubType)
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
