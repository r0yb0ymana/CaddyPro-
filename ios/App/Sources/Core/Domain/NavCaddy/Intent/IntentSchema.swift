import Foundation

/// Schema definition for an intent type.
/// Defines required/optional entities and default routing.
struct IntentSchema: Hashable, Identifiable {
    var id: IntentType { intentType }

    let intentType: IntentType
    let displayName: String
    let description: String
    let requiredEntities: Set<EntityType>
    let optionalEntities: Set<EntityType>
    let defaultRoutingTarget: RoutingTarget?
    let requiresNavigation: Bool
    let examplePhrases: [String]

    init(
        intentType: IntentType,
        displayName: String,
        description: String,
        requiredEntities: Set<EntityType> = [],
        optionalEntities: Set<EntityType> = [],
        defaultRoutingTarget: RoutingTarget? = nil,
        requiresNavigation: Bool = true,
        examplePhrases: [String] = []
    ) {
        self.intentType = intentType
        self.displayName = displayName
        self.description = description
        self.requiredEntities = requiredEntities
        self.optionalEntities = optionalEntities
        self.defaultRoutingTarget = defaultRoutingTarget
        self.requiresNavigation = requiresNavigation
        self.examplePhrases = examplePhrases
    }
}

/// Types of entities that can be extracted from user input.
enum EntityType: String, CaseIterable, Hashable {
    case club
    case yardage
    case lie
    case wind
    case fatigue
    case pain
    case scoreContext
    case holeNumber
    case drillType
    case statType
    case courseName
    case equipmentType
    case settingKey
    case feedbackText
}
