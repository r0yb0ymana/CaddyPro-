import Foundation

/// Represents the result of intent classification from user input.
///
/// Spec R2: Output schema includes intent_id, confidence, entities, routing_target.
struct ParsedIntent: Codable, Hashable, Identifiable {
    let id: String
    let intentType: IntentType

    /// Confidence score (0-1) for the classification
    let confidence: Double

    let entities: ExtractedEntities
    let userGoal: String?
    let routingTarget: RoutingTarget?

    init(
        id: String = UUID().uuidString,
        intentType: IntentType,
        confidence: Double,
        entities: ExtractedEntities = ExtractedEntities(),
        userGoal: String? = nil,
        routingTarget: RoutingTarget? = nil
    ) {
        self.id = id
        self.intentType = intentType
        self.confidence = min(max(confidence, 0.0), 1.0) // Clamp to [0, 1]
        self.entities = entities
        self.userGoal = userGoal
        self.routingTarget = routingTarget
    }
}
