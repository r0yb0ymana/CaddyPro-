import Foundation

/// Service for classifying user intents using LLM.
///
/// Uses InputNormalizer to preprocess input, then LLMClient to get classification,
/// validates entities with IntentRegistry, and returns appropriate ClassificationResult
/// based on confidence thresholds.
///
/// Spec R2: Intent classification pipeline with confidence-based routing.
/// Spec A1: Correct routing on high-confidence intent.
/// Spec A3: Clarification on low-confidence intent.
actor IntentClassifier {
    private let llmClient: LLMClient
    private let normalizer: InputNormalizer
    private let clarificationHandler: ClarificationHandler

    init(
        llmClient: LLMClient,
        normalizer: InputNormalizer = InputNormalizer(),
        clarificationHandler: ClarificationHandler = ClarificationHandler()
    ) {
        self.llmClient = llmClient
        self.normalizer = normalizer
        self.clarificationHandler = clarificationHandler
    }

    /// Classifies user input and determines the appropriate action.
    ///
    /// - Parameters:
    ///   - input: The user's input text (raw, unnormalized)
    ///   - context: Optional session context for conversation continuity
    /// - Returns: Classification result with routing decision
    func classify(input: String, context: SessionContext?) async -> ClassificationResult {
        // Step 1: Normalize input
        let normalizationResult = normalizer.normalize(input)
        let normalizedInput = normalizationResult.normalizedInput

        // Step 2: Language detection (basic check)
        if !normalizer.isEnglish(normalizedInput) {
            let clarificationResponse = clarificationHandler.generateClarification(
                for: input,
                parsedIntent: nil,
                context: context
            )
            return .clarify(response: clarificationResponse)
        }

        do {
            // Step 3: Get LLM classification using normalized input
            let llmResponse = try await llmClient.classify(input: normalizedInput, context: context)

            // Step 4: Parse the JSON response
            guard let parsedIntent = try? parseResponse(llmResponse.rawResponse) else {
                // If parsing fails, return clarification
                let clarificationResponse = clarificationHandler.generateClarification(
                    for: input,
                    parsedIntent: nil,
                    context: context
                )
                return .clarify(response: clarificationResponse)
            }

            // Step 5: Validate entities against schema
            let validationResult = IntentRegistry.validateEntities(
                for: parsedIntent.intentType,
                entities: parsedIntent.entities
            )

            // Step 6: Get routing target
            let schema = IntentRegistry.getSchema(for: parsedIntent.intentType)
            let routingTarget = parsedIntent.routingTarget ?? schema.defaultRoutingTarget

            // Step 7: Determine action based on confidence threshold
            let action = ConfidenceThresholds.action(for: parsedIntent.confidence)

            switch action {
            case .route:
                // High confidence: route directly
                if case .missingEntities(let missing) = validationResult {
                    // Missing required entities - ask for confirmation with details
                    return .confirm(
                        intent: parsedIntent,
                        message: buildMissingEntitiesMessage(
                            intentType: parsedIntent.intentType,
                            missingEntities: missing
                        )
                    )
                } else if let target = routingTarget {
                    return .route(intent: parsedIntent, target: target)
                } else {
                    // No routing target (pure answer intent)
                    // Return as confirmation since we need to generate a response
                    return .confirm(
                        intent: parsedIntent,
                        message: "I'll help you with that."
                    )
                }

            case .confirm:
                // Medium confidence: ask for confirmation
                return .confirm(
                    intent: parsedIntent,
                    message: buildConfirmationMessage(intent: parsedIntent)
                )

            case .clarify:
                // Low confidence: request clarification
                let clarificationResponse = clarificationHandler.generateClarification(
                    for: input,
                    parsedIntent: parsedIntent,
                    context: context
                )
                return .clarify(response: clarificationResponse)
            }

        } catch let error as LLMError {
            return .error(error)
        } catch {
            return .error(LLMError.unknown(error.localizedDescription))
        }
    }

    // MARK: - Response Parsing

    private func parseResponse(_ jsonString: String) throws -> ParsedIntent {
        guard let jsonData = jsonString.data(using: .utf8) else {
            throw LLMError.parseError("Failed to convert response to data")
        }

        let decoder = JSONDecoder()
        decoder.keyDecodingStrategy = .convertFromSnakeCase

        do {
            let response = try decoder.decode(LLMClassificationResponse.self, from: jsonData)
            return response.toParsedIntent()
        } catch {
            throw LLMError.parseError("JSON decode failed: \(error.localizedDescription)")
        }
    }

    // MARK: - Message Building

    private func buildConfirmationMessage(intent: ParsedIntent) -> String {
        var message = "Did you want to "

        switch intent.intentType {
        case .clubAdjustment:
            if let club = intent.entities.club {
                message += "adjust your \(club.name)?"
            } else {
                message += "adjust a club distance?"
            }
        case .recoveryCheck:
            message += "check your recovery status?"
        case .shotRecommendation:
            if let yardage = intent.entities.yardage {
                message += "get advice for a \(yardage) yard shot?"
            } else {
                message += "get shot advice?"
            }
        case .scoreEntry:
            message += "enter your score?"
        case .patternQuery:
            message += "check your miss patterns?"
        case .drillRequest:
            message += "get a practice drill?"
        case .weatherCheck:
            message += "check the weather?"
        case .statsLookup:
            message += "look up your stats?"
        case .roundStart:
            message += "start a new round?"
        case .roundEnd:
            message += "end your round?"
        case .equipmentInfo:
            message += "check your equipment?"
        case .courseInfo:
            message += "get course information?"
        case .settingsChange:
            message += "change a setting?"
        case .helpRequest:
            message += "get help?"
        case .feedback:
            message += "provide feedback?"
        }

        return message
    }

    private func buildMissingEntitiesMessage(
        intentType: IntentType,
        missingEntities: [EntityType]
    ) -> String {
        let entityNames = missingEntities.map { entityType in
            switch entityType {
            case .club: return "club"
            case .yardage: return "yardage"
            case .lie: return "lie"
            case .wind: return "wind conditions"
            case .fatigue: return "fatigue level"
            case .pain: return "pain details"
            case .scoreContext: return "score context"
            case .holeNumber: return "hole number"
            case .drillType: return "drill type"
            case .statType: return "statistic type"
            case .courseName: return "course name"
            case .equipmentType: return "equipment type"
            case .settingKey: return "setting name"
            case .feedbackText: return "feedback"
            }
        }

        if entityNames.count == 1 {
            return "Which \(entityNames[0])?"
        } else {
            let allButLast = entityNames.dropLast().joined(separator: ", ")
            let last = entityNames.last!
            return "I need the \(allButLast) and \(last)."
        }
    }
}

// MARK: - LLM Response Model

/// Internal model for parsing LLM JSON responses.
private struct LLMClassificationResponse: Decodable {
    let intent: String
    let confidence: Double
    let entities: LLMEntities
    let userGoal: String?

    func toParsedIntent() -> ParsedIntent {
        guard let intentType = IntentType(rawValue: intent) else {
            // Fallback to help if intent type is unknown
            return ParsedIntent(
                intentType: .helpRequest,
                confidence: 0.1,
                userGoal: userGoal
            )
        }

        let extractedEntities = entities.toExtractedEntities()
        let schema = IntentRegistry.getSchema(for: intentType)

        return ParsedIntent(
            intentType: intentType,
            confidence: confidence,
            entities: extractedEntities,
            userGoal: userGoal,
            routingTarget: schema.defaultRoutingTarget
        )
    }
}

/// Internal model for parsing entity JSON.
private struct LLMEntities: Decodable {
    let club: String?
    let yardage: Int?
    let lie: String?
    let wind: String?
    let fatigue: Int?
    let pain: String?
    let scoreContext: String?
    let holeNumber: Int?

    func toExtractedEntities() -> ExtractedEntities {
        let clubEntity = club.flatMap { parseClub($0) }
        let lieEntity = lie.flatMap { Lie(rawValue: $0.lowercased()) }

        return ExtractedEntities(
            club: clubEntity,
            yardage: yardage,
            lie: lieEntity,
            wind: wind,
            fatigue: fatigue,
            pain: pain,
            scoreContext: scoreContext,
            holeNumber: holeNumber
        )
    }

    /// Parse club string into Club model.
    private func parseClub(_ clubString: String) -> Club? {
        let normalized = clubString.lowercased()
            .replacingOccurrences(of: "-", with: "")
            .replacingOccurrences(of: " ", with: "")

        var clubType: ClubType
        var clubName: String

        // Try to parse common club names
        if normalized.contains("driver") || normalized == "dr" {
            clubType = .driver
            clubName = "Driver"
        } else if normalized.contains("wood") {
            clubType = .wood
            let number = normalized.first(where: { $0.isNumber }).flatMap { Int(String($0)) } ?? 3
            clubName = "\(number)-wood"
        } else if normalized.contains("hybrid") || normalized.contains("rescue") {
            clubType = .hybrid
            let number = normalized.first(where: { $0.isNumber }).flatMap { Int(String($0)) } ?? 3
            clubName = "\(number)-hybrid"
        } else if normalized.contains("iron") {
            clubType = .iron
            guard let number = normalized.first(where: { $0.isNumber }).flatMap({ Int(String($0)) }),
                  (1...9).contains(number) else {
                return nil
            }
            clubName = "\(number)-iron"
        } else if normalized.contains("wedge") {
            clubType = .wedge
            if normalized.contains("pitching") || normalized == "pw" {
                clubName = "Pitching Wedge"
            } else if normalized.contains("gap") || normalized == "gw" || normalized == "aw" {
                clubName = "Gap Wedge"
            } else if normalized.contains("sand") || normalized == "sw" {
                clubName = "Sand Wedge"
            } else if normalized.contains("lob") || normalized == "lw" {
                clubName = "Lob Wedge"
            } else {
                clubName = "Wedge"
            }
        } else if normalized.contains("putter") {
            clubType = .putter
            clubName = "Putter"
        } else {
            // Try to extract just a number (assume iron)
            if let number = Int(normalized), (1...9).contains(number) {
                clubType = .iron
                clubName = "\(number)-iron"
            } else {
                return nil
            }
        }

        return Club(name: clubName, type: clubType)
    }
}
