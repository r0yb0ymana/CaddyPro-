import Foundation

/// Gemini-based implementation of LLMClient.
///
/// Builds prompts for intent classification, includes system prompt with
/// intent list and entity schemas, and parses JSON responses.
///
/// Spec R2: Intent classification using Gemini 3 Flash.
final class GeminiClient: LLMClient {
    private let apiKey: String
    private let model: String
    private let timeoutSeconds: TimeInterval

    init(
        apiKey: String = "",
        model: String = "gemini-3-flash",
        timeoutSeconds: TimeInterval = 3.0
    ) {
        self.apiKey = apiKey
        self.model = model
        self.timeoutSeconds = timeoutSeconds
    }

    func classify(input: String, context: SessionContext?) async throws -> LLMResponse {
        // Validate input
        guard !input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw LLMError.invalidInput("Input cannot be empty")
        }

        let startTime = Date()

        // Build the prompt
        let systemPrompt = buildSystemPrompt()
        let userPrompt = buildUserPrompt(input: input, context: context)

        // Make API request (placeholder - actual implementation would call Gemini API)
        let response = try await makeAPIRequest(
            systemPrompt: systemPrompt,
            userPrompt: userPrompt
        )

        let latencyMs = Int(Date().timeIntervalSince(startTime) * 1000)

        return LLMResponse(
            rawResponse: response,
            latencyMs: latencyMs,
            model: model,
            timestamp: startTime
        )
    }

    // MARK: - Prompt Building

    private func buildSystemPrompt() -> String {
        """
        You are an intent classification system for a golf app called CaddyPro.
        Your job is to classify user input into one of the supported intents and extract relevant entities.

        ## Supported Intents

        \(buildIntentList())

        ## Entity Types

        - club: Golf club (e.g., "7-iron", "driver", "PW")
        - yardage: Distance in yards (e.g., 150, 175)
        - lie: Ball position (e.g., "fairway", "rough", "bunker")
        - wind: Wind description (e.g., "10mph headwind", "light breeze")
        - fatigue: Fatigue level 1-10
        - pain: Pain description
        - scoreContext: Scoring situation (e.g., "leading", "trailing")
        - holeNumber: Hole number 1-18
        - drillType: Type of practice drill
        - statType: Type of statistic
        - courseName: Name of golf course
        - equipmentType: Type of equipment
        - settingKey: Setting name
        - feedbackText: User feedback text

        ## Response Format

        Respond ONLY with a JSON object in this exact format:
        {
          "intent": "<intent_type>",
          "confidence": <0.0-1.0>,
          "entities": {
            "club": "<club_name>",
            "yardage": <number>,
            ...
          },
          "userGoal": "<optional brief description of user's goal>"
        }

        ## Classification Rules

        1. Choose the single most likely intent
        2. Set confidence based on clarity and context:
           - 0.90-1.0: Very clear, unambiguous
           - 0.75-0.89: Clear with good context
           - 0.50-0.74: Somewhat clear, needs confirmation
           - 0.0-0.49: Ambiguous, needs clarification
        3. Extract all relevant entities
        4. Use null for missing entities
        5. Be conservative with confidence scores
        """
    }

    private func buildIntentList() -> String {
        let schemas = IntentRegistry.getAllSchemas()
        return schemas.map { schema in
            "- \(schema.intentType.rawValue): \(schema.description)"
        }.joined(separator: "\n")
    }

    private func buildUserPrompt(input: String, context: SessionContext?) -> String {
        var prompt = "Classify this input: \"\(input)\""

        if let context = context, !context.conversationHistory.isEmpty {
            prompt += "\n\nConversation context:\n"
            let recentTurns = context.conversationHistory.suffix(3)
            for turn in recentTurns {
                let roleLabel = turn.role == .user ? "User" : "System"
                prompt += "\(roleLabel): \(turn.content)\n"
            }
        }

        if let hole = context?.currentHole {
            prompt += "\n\nCurrent hole: \(hole)"
        }

        return prompt
    }

    // MARK: - API Request

    private func makeAPIRequest(systemPrompt: String, userPrompt: String) async throws -> String {
        // TODO: Implement actual Gemini API call
        // For now, this is a placeholder that will be replaced with actual API integration

        // Simulate network delay
        try await Task.sleep(nanoseconds: UInt64(0.5 * 1_000_000_000))

        // Return a mock response for testing
        // In production, this would make an actual HTTP request to Gemini API
        throw LLMError.unknown("Gemini API integration not yet implemented")
    }
}

// MARK: - Intent List Extension

private extension GeminiClient {
    /// Generates example JSON for documentation
    static func exampleResponse() -> String {
        """
        {
          "intent": "club_adjustment",
          "confidence": 0.85,
          "entities": {
            "club": "7-iron",
            "yardage": null
          },
          "userGoal": "User wants to adjust 7-iron distance based on feel"
        }
        """
    }
}
