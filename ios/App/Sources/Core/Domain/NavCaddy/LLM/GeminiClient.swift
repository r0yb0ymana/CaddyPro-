import Foundation

/// Gemini-based implementation of LLMClient.
///
/// Builds prompts for intent classification, includes system prompt with
/// intent list and entity schemas, and parses JSON responses.
///
/// Spec R2: Intent classification using Gemini Flash.
final class GeminiClient: LLMClient {
    private let apiKey: String
    private let model: String
    private let timeoutSeconds: TimeInterval

    init(
        apiKey: String = "",
        model: String = "gemini-2.0-flash-lite",
        timeoutSeconds: TimeInterval = 10.0
    ) {
        self.apiKey = apiKey
        self.model = model
        self.timeoutSeconds = timeoutSeconds
    }

    // MARK: - Direct Chat

    /// Sends a conversational message to Gemini as Bones the caddy.
    /// Returns a plain text response (not JSON classification).
    func chat(message: String, history: [(role: String, content: String)]) async throws -> String {
        guard !apiKey.isEmpty, apiKey != "PLACEHOLDER_API_KEY" else {
            throw LLMError.authenticationError
        }

        let urlString = "https://generativelanguage.googleapis.com/v1beta/models/\(model):generateContent?key=\(apiKey)"
        guard let url = URL(string: urlString) else {
            throw LLMError.unknown("Invalid API URL")
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = timeoutSeconds

        let systemPrompt = """
        You are Bones, a friendly and knowledgeable digital golf caddy in the CaddyPro+ app. \
        You speak in a warm, conversational tone — like a real caddy walking alongside the golfer.

        You help with:
        - Club selection and shot recommendations
        - Reading course conditions (wind, lie, elevation)
        - Strategy and course management
        - Swing tips and practice drills
        - Score tracking advice
        - Equipment recommendations
        - General golf knowledge

        Keep responses concise (2-4 sentences) and practical. Use golf terminology naturally. \
        Don't say "As an AI" — you're Bones, a caddy. Be confident but not pushy with advice. \
        If someone mentions pain or injury, suggest they consult a medical professional.
        """

        var contents: [[String: Any]] = []
        for turn in history.suffix(10) {
            contents.append([
                "role": turn.role,
                "parts": [["text": turn.content]]
            ])
        }
        contents.append([
            "role": "user",
            "parts": [["text": message]]
        ])

        let body: [String: Any] = [
            "contents": contents,
            "systemInstruction": [
                "parts": [["text": systemPrompt]]
            ],
            "generationConfig": [
                "temperature": 0.7,
                "maxOutputTokens": 512
            ]
        ]

        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        // Retry up to 2 times on rate limit (429)
        var lastError: Error?
        for attempt in 0..<3 {
            if attempt > 0 {
                // Wait before retrying (3s, then 6s)
                try await Task.sleep(nanoseconds: UInt64(attempt) * 3_000_000_000)
            }

            let (data, response): (Data, URLResponse)
            do {
                (data, response) = try await URLSession.shared.data(for: request)
            } catch let error as URLError where error.code == .timedOut {
                throw LLMError.timeout
            } catch {
                throw LLMError.networkError(error.localizedDescription)
            }

            guard let httpResponse = response as? HTTPURLResponse else {
                throw LLMError.networkError("Invalid response")
            }

            switch httpResponse.statusCode {
            case 200:
                guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
                      let candidates = json["candidates"] as? [[String: Any]],
                      let firstCandidate = candidates.first,
                      let content = firstCandidate["content"] as? [String: Any],
                      let parts = content["parts"] as? [[String: Any]],
                      let firstPart = parts.first,
                      let text = firstPart["text"] as? String else {
                    throw LLMError.parseError("Failed to extract text from Gemini response")
                }
                return text
            case 401, 403:
                throw LLMError.authenticationError
            case 429:
                lastError = LLMError.rateLimitExceeded
                continue // retry
            default:
                let errorBody = String(data: data, encoding: .utf8) ?? "Unknown error"
                throw LLMError.networkError("HTTP \(httpResponse.statusCode): \(errorBody)")
            }
        }

        throw lastError ?? LLMError.rateLimitExceeded
    }

    // MARK: - Intent Classification

    func classify(input: String, context: SessionContext?) async throws -> LLMResponse {
        // Validate input
        guard !input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw LLMError.invalidInput("Input cannot be empty")
        }

        guard !apiKey.isEmpty, apiKey != "PLACEHOLDER_API_KEY" else {
            throw LLMError.authenticationError
        }

        let startTime = Date()

        // Build the prompt
        let systemPrompt = buildSystemPrompt()
        let userPrompt = buildUserPrompt(input: input, context: context)

        // Make API request
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
        let urlString = "https://generativelanguage.googleapis.com/v1beta/models/\(model):generateContent?key=\(apiKey)"

        guard let url = URL(string: urlString) else {
            throw LLMError.unknown("Invalid API URL")
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.timeoutInterval = timeoutSeconds

        let body: [String: Any] = [
            "contents": [
                [
                    "role": "user",
                    "parts": [["text": userPrompt]]
                ]
            ],
            "systemInstruction": [
                "parts": [["text": systemPrompt]]
            ],
            "generationConfig": [
                "responseMimeType": "application/json",
                "temperature": 0.1,
                "maxOutputTokens": 256
            ]
        ]

        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch let error as URLError where error.code == .timedOut {
            throw LLMError.timeout
        } catch {
            throw LLMError.networkError(error.localizedDescription)
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw LLMError.networkError("Invalid response")
        }

        switch httpResponse.statusCode {
        case 200:
            break
        case 401, 403:
            throw LLMError.authenticationError
        case 429:
            throw LLMError.rateLimitExceeded
        default:
            let errorBody = String(data: data, encoding: .utf8) ?? "Unknown error"
            throw LLMError.networkError("HTTP \(httpResponse.statusCode): \(errorBody)")
        }

        // Parse the Gemini response to extract the text content
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let candidates = json["candidates"] as? [[String: Any]],
              let firstCandidate = candidates.first,
              let content = firstCandidate["content"] as? [String: Any],
              let parts = content["parts"] as? [[String: Any]],
              let firstPart = parts.first,
              let text = firstPart["text"] as? String else {
            throw LLMError.parseError("Failed to extract text from Gemini response")
        }

        return text
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
