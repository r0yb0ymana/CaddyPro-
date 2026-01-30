import Foundation

/// Registry of all supported intents with their schemas.
/// Provides lookup and query capabilities.
enum IntentRegistry {
    private static let schemas: [IntentType: IntentSchema] = buildSchemas()

    /// Get schema for a specific intent type.
    static func getSchema(for intentType: IntentType) -> IntentSchema {
        guard let schema = schemas[intentType] else {
            fatalError("Unknown intent type: \(intentType)")
        }
        return schema
    }

    /// Get all registered schemas.
    static func getAllSchemas() -> [IntentSchema] {
        Array(schemas.values)
    }

    /// Find intents that can be routed to a specific module.
    static func getIntents(for module: Module) -> [IntentSchema] {
        schemas.values.filter { $0.defaultRoutingTarget?.module == module }
    }

    /// Find intents that don't require navigation (pure answers).
    static func getNoNavigationIntents() -> [IntentSchema] {
        schemas.values.filter { !$0.requiresNavigation }
    }

    /// Validate if extracted entities satisfy intent requirements.
    static func validateEntities(for intentType: IntentType, entities: ExtractedEntities) -> ValidationResult {
        let schema = getSchema(for: intentType)
        let missingRequired = schema.requiredEntities.filter { entityType in
            !hasEntity(entities, entityType: entityType)
        }
        if missingRequired.isEmpty {
            return .valid
        } else {
            return .missingEntities(Array(missingRequired))
        }
    }

    private static func hasEntity(_ entities: ExtractedEntities, entityType: EntityType) -> Bool {
        switch entityType {
        case .club: return entities.club != nil
        case .yardage: return entities.yardage != nil
        case .lie: return entities.lie != nil
        case .wind: return entities.wind != nil
        case .fatigue: return entities.fatigue != nil
        case .pain: return entities.pain != nil
        case .scoreContext: return entities.scoreContext != nil
        case .holeNumber: return entities.holeNumber != nil
        // For new entity types not in ExtractedEntities, return true
        default: return true
        }
    }

    private static func buildSchemas() -> [IntentType: IntentSchema] {
        let schemas: [IntentSchema] = [
            // MARK: - Core Intents

            IntentSchema(
                intentType: .clubAdjustment,
                displayName: "Club Adjustment",
                description: "Adjust club distances or settings based on feel and performance",
                requiredEntities: [.club],
                optionalEntities: [.yardage],
                defaultRoutingTarget: RoutingTarget(
                    module: .caddy,
                    screen: "ClubAdjustmentScreen"
                ),
                requiresNavigation: true,
                examplePhrases: [
                    "My 7-iron feels long today",
                    "Update my pitching wedge distance",
                    "The driver is going about 10 yards shorter"
                ]
            ),

            IntentSchema(
                intentType: .recoveryCheck,
                displayName: "Recovery Check",
                description: "Check current recovery status and readiness",
                requiredEntities: [],
                optionalEntities: [.fatigue, .pain],
                defaultRoutingTarget: RoutingTarget(
                    module: .recovery,
                    screen: "RecoveryOverviewScreen"
                ),
                requiresNavigation: true,
                examplePhrases: [
                    "How's my recovery looking?",
                    "Am I ready to play?",
                    "Check my readiness score"
                ]
            ),

            IntentSchema(
                intentType: .shotRecommendation,
                displayName: "Shot Recommendation",
                description: "Get advice for the current shot",
                requiredEntities: [],
                optionalEntities: [.club, .yardage, .lie, .wind],
                defaultRoutingTarget: RoutingTarget(
                    module: .caddy,
                    screen: "ShotAdvisorScreen"
                ),
                requiresNavigation: true,
                examplePhrases: [
                    "What's the play here?",
                    "What club should I hit from 150?",
                    "How do I handle this lie?"
                ]
            ),

            IntentSchema(
                intentType: .scoreEntry,
                displayName: "Score Entry",
                description: "Enter a score for the current or specified hole",
                requiredEntities: [],
                optionalEntities: [.holeNumber, .scoreContext],
                defaultRoutingTarget: RoutingTarget(
                    module: .caddy,
                    screen: "ScoreEntryScreen"
                ),
                requiresNavigation: true,
                examplePhrases: [
                    "Log a 5 for this hole",
                    "I made par",
                    "Enter my score"
                ]
            ),

            IntentSchema(
                intentType: .patternQuery,
                displayName: "Pattern Query",
                description: "Ask about miss patterns and tendencies",
                requiredEntities: [],
                optionalEntities: [.club, .lie],
                defaultRoutingTarget: nil,
                requiresNavigation: false,
                examplePhrases: [
                    "What's my miss with driver?",
                    "Do I slice under pressure?",
                    "Show my pattern with 7-iron"
                ]
            ),

            // MARK: - Extended Intents

            IntentSchema(
                intentType: .drillRequest,
                displayName: "Drill Request",
                description: "Request a practice drill or training exercise",
                requiredEntities: [],
                optionalEntities: [.club, .drillType],
                defaultRoutingTarget: RoutingTarget(
                    module: .coach,
                    screen: "DrillScreen"
                ),
                requiresNavigation: true,
                examplePhrases: [
                    "Give me a drill for my slice",
                    "What should I practice today?",
                    "I need a putting drill"
                ]
            ),

            IntentSchema(
                intentType: .weatherCheck,
                displayName: "Weather Check",
                description: "Check current weather conditions and forecast",
                requiredEntities: [],
                optionalEntities: [],
                defaultRoutingTarget: RoutingTarget(
                    module: .caddy,
                    screen: "WeatherScreen"
                ),
                requiresNavigation: true,
                examplePhrases: [
                    "What's the weather like?",
                    "Check the wind",
                    "Is it going to rain?"
                ]
            ),

            IntentSchema(
                intentType: .statsLookup,
                displayName: "Stats Lookup",
                description: "Look up performance statistics",
                requiredEntities: [],
                optionalEntities: [.statType, .club],
                defaultRoutingTarget: RoutingTarget(
                    module: .caddy,
                    screen: "StatsScreen"
                ),
                requiresNavigation: true,
                examplePhrases: [
                    "Show my driving stats",
                    "What's my fairway hit percentage?",
                    "How am I putting?"
                ]
            ),

            IntentSchema(
                intentType: .roundStart,
                displayName: "Start Round",
                description: "Start a new round of golf",
                requiredEntities: [],
                optionalEntities: [.courseName],
                defaultRoutingTarget: RoutingTarget(
                    module: .caddy,
                    screen: "RoundSetupScreen"
                ),
                requiresNavigation: true,
                examplePhrases: [
                    "Start a new round",
                    "Begin round at Pebble Beach",
                    "Let's tee off"
                ]
            ),

            IntentSchema(
                intentType: .roundEnd,
                displayName: "End Round",
                description: "End the current round and view summary",
                requiredEntities: [],
                optionalEntities: [],
                defaultRoutingTarget: RoutingTarget(
                    module: .caddy,
                    screen: "RoundSummaryScreen"
                ),
                requiresNavigation: true,
                examplePhrases: [
                    "Finish the round",
                    "End my round",
                    "Show round summary"
                ]
            ),

            // MARK: - Additional Intents

            IntentSchema(
                intentType: .equipmentInfo,
                displayName: "Equipment Info",
                description: "Get information about golf equipment",
                requiredEntities: [],
                optionalEntities: [.equipmentType, .club],
                defaultRoutingTarget: RoutingTarget(
                    module: .settings,
                    screen: "EquipmentScreen"
                ),
                requiresNavigation: true,
                examplePhrases: [
                    "Show my bag setup",
                    "What clubs do I have?",
                    "Tell me about my driver"
                ]
            ),

            IntentSchema(
                intentType: .courseInfo,
                displayName: "Course Info",
                description: "Get information about the course or specific hole",
                requiredEntities: [],
                optionalEntities: [.courseName, .holeNumber],
                defaultRoutingTarget: RoutingTarget(
                    module: .caddy,
                    screen: "CourseInfoScreen"
                ),
                requiresNavigation: true,
                examplePhrases: [
                    "Tell me about this hole",
                    "What's the yardage on hole 12?",
                    "Show course overview"
                ]
            ),

            IntentSchema(
                intentType: .settingsChange,
                displayName: "Settings Change",
                description: "Change an app setting or preference",
                requiredEntities: [],
                optionalEntities: [.settingKey],
                defaultRoutingTarget: RoutingTarget(
                    module: .settings,
                    screen: "SettingsScreen"
                ),
                requiresNavigation: true,
                examplePhrases: [
                    "Change my units to metric",
                    "Turn off notifications",
                    "Update my preferences"
                ]
            ),

            IntentSchema(
                intentType: .helpRequest,
                displayName: "Help Request",
                description: "Get help with using the app",
                requiredEntities: [],
                optionalEntities: [],
                defaultRoutingTarget: nil,
                requiresNavigation: false,
                examplePhrases: [
                    "How do I use this?",
                    "Help me understand",
                    "What can you do?"
                ]
            ),

            IntentSchema(
                intentType: .feedback,
                displayName: "Feedback",
                description: "Provide feedback about the app",
                requiredEntities: [],
                optionalEntities: [.feedbackText],
                defaultRoutingTarget: RoutingTarget(
                    module: .settings,
                    screen: "FeedbackScreen"
                ),
                requiresNavigation: true,
                examplePhrases: [
                    "I have feedback",
                    "Report a bug",
                    "Send feedback about the shot tracker"
                ]
            )
        ]

        return Dictionary(uniqueKeysWithValues: schemas.map { ($0.intentType, $0) })
    }
}

enum ValidationResult: Equatable {
    case valid
    case missingEntities([EntityType])
}
