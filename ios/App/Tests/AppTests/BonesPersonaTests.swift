import XCTest
@testable import App

/// Tests for the BonesPersona system prompt and voice characteristics.
///
/// Spec R4: Bones Persona Layer
/// - Validates system prompt content
/// - Tests voice characteristics
/// - Verifies module-specific context
/// - Tests pattern inclusion logic
@MainActor
final class BonesPersonaTests: XCTestCase {

    // MARK: - System Prompt Tests

    func testSystemPromptContainsKeyGuidelines() {
        let prompt = BonesPersona.systemPrompt

        // Voice characteristics
        XCTAssertTrue(prompt.contains("Bones"))
        XCTAssertTrue(prompt.contains("professional"))
        XCTAssertTrue(prompt.contains("caddy"))
        XCTAssertTrue(prompt.contains("warm"))

        // Content guidelines
        XCTAssertTrue(prompt.contains("tactical"))
        XCTAssertTrue(prompt.contains("context-aware"))
        XCTAssertTrue(prompt.contains("uncertainty"))

        // Safety guardrails
        XCTAssertTrue(prompt.contains("medical"))
        XCTAssertTrue(prompt.contains("guarantees"))
        XCTAssertTrue(prompt.contains("betting"))
    }

    func testSystemPromptAvoidsGenericPhrases() {
        let prompt = BonesPersona.systemPrompt

        // Should not contain generic assistant phrases
        XCTAssertFalse(prompt.contains("I'm just an AI"))
        XCTAssertFalse(prompt.contains("However, I must"))
    }

    func testShortReminderContainsEssentials() {
        let reminder = BonesPersona.shortReminder

        XCTAssertTrue(reminder.contains("Bones"))
        XCTAssertTrue(reminder.contains("caddy"))
        XCTAssertTrue(reminder.contains("uncertainty"))
        XCTAssertTrue(reminder.contains("medical"))

        // Should be significantly shorter than full prompt
        XCTAssertLessThan(reminder.count, BonesPersona.systemPrompt.count / 3)
    }

    // MARK: - Voice Characteristics Tests

    func testNaturalCaddyPhrasesAreDefined() {
        let phrases = BonesPersona.VoiceCharacteristics.naturalCaddyPhrases

        XCTAssertFalse(phrases.isEmpty)
        XCTAssertTrue(phrases.contains("Let's go with"))
        XCTAssertTrue(phrases.contains("That's your miss"))
        XCTAssertTrue(phrases.contains("You tend to"))
    }

    func testAvoidPhrasesAreDefined() {
        let phrases = BonesPersona.VoiceCharacteristics.avoidPhrases

        XCTAssertFalse(phrases.isEmpty)
        XCTAssertTrue(phrases.contains("As an AI"))
        XCTAssertTrue(phrases.contains("However, I must"))
        XCTAssertTrue(phrases.contains("In conclusion"))
    }

    func testMaxLengthIsDefined() {
        let maxLength = BonesPersona.VoiceCharacteristics.maxLength

        XCTAssertGreaterThan(maxLength, 0)
        XCTAssertLessThanOrEqual(maxLength, 1000) // Reasonable upper bound
    }

    func testMinSubstantiveLengthIsDefined() {
        let minLength = BonesPersona.VoiceCharacteristics.minSubstantiveLength

        XCTAssertGreaterThan(minLength, 0)
        XCTAssertLessThan(
            minLength,
            BonesPersona.VoiceCharacteristics.maxLength
        )
    }

    // MARK: - Module Context Tests

    func testCaddyModuleContext() {
        let context = BonesPersona.contextForModule(.caddy)

        XCTAssertTrue(context.contains("shot") || context.contains("club"))
        XCTAssertTrue(context.contains("pattern"))
    }

    func testCoachModuleContext() {
        let context = BonesPersona.contextForModule(.coach)

        XCTAssertTrue(context.contains("drill") || context.contains("improvement"))
        XCTAssertTrue(context.contains("suggestion"))
    }

    func testRecoveryModuleContext() {
        let context = BonesPersona.contextForModule(.recovery)

        XCTAssertTrue(context.contains("wellness") || context.contains("recovery"))
        XCTAssertTrue(context.contains("disclaimer"))
    }

    func testSettingsModuleContext() {
        let context = BonesPersona.contextForModule(.settings)

        XCTAssertTrue(context.contains("configure") || context.contains("app"))
        XCTAssertTrue(context.contains("concise"))
    }

    // MARK: - Pattern Inclusion Tests

    func testShotRecommendationIncludesPatterns() {
        let shouldInclude = BonesPersona.shouldIncludePatterns(
            for: .shotRecommendation,
            hasPatterns: true
        )

        XCTAssertTrue(shouldInclude)
    }

    func testClubAdjustmentIncludesPatterns() {
        let shouldInclude = BonesPersona.shouldIncludePatterns(
            for: .clubAdjustment,
            hasPatterns: true
        )

        XCTAssertTrue(shouldInclude)
    }

    func testPatternQueryIncludesPatterns() {
        let shouldInclude = BonesPersona.shouldIncludePatterns(
            for: .patternQuery,
            hasPatterns: true
        )

        XCTAssertTrue(shouldInclude)
    }

    func testRecoveryCheckIncludesPatterns() {
        let shouldInclude = BonesPersona.shouldIncludePatterns(
            for: .recoveryCheck,
            hasPatterns: true
        )

        XCTAssertTrue(shouldInclude)
    }

    func testDrillRequestIncludesPatterns() {
        let shouldInclude = BonesPersona.shouldIncludePatterns(
            for: .drillRequest,
            hasPatterns: true
        )

        XCTAssertTrue(shouldInclude)
    }

    func testNoPatternInclusionWithoutPatterns() {
        // Even pattern-relevant intents should not include if no patterns exist
        let shouldInclude = BonesPersona.shouldIncludePatterns(
            for: .shotRecommendation,
            hasPatterns: false
        )

        XCTAssertFalse(shouldInclude)
    }

    func testScoreEntryDoesNotIncludePatterns() {
        let shouldInclude = BonesPersona.shouldIncludePatterns(
            for: .scoreEntry,
            hasPatterns: true
        )

        XCTAssertFalse(shouldInclude)
    }

    func testHelpRequestDoesNotIncludePatterns() {
        let shouldInclude = BonesPersona.shouldIncludePatterns(
            for: .helpRequest,
            hasPatterns: true
        )

        XCTAssertFalse(shouldInclude)
    }

    func testSettingsChangeDoesNotIncludePatterns() {
        let shouldInclude = BonesPersona.shouldIncludePatterns(
            for: .settingsChange,
            hasPatterns: true
        )

        XCTAssertFalse(shouldInclude)
    }

    // MARK: - Integration Tests

    func testSystemPromptLength() {
        let prompt = BonesPersona.systemPrompt

        // Should be substantial but not excessive
        XCTAssertGreaterThan(prompt.count, 500)
        XCTAssertLessThan(prompt.count, 3000)
    }

    func testAllModulesHaveContext() {
        for module in [Module.caddy, .coach, .recovery, .settings] {
            let context = BonesPersona.contextForModule(module)
            XCTAssertFalse(context.isEmpty)
            XCTAssertGreaterThan(context.count, 20)
        }
    }
}
