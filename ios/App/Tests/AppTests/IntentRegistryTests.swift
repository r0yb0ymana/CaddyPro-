import XCTest
@testable import App

/// Tests for IntentRegistry and IntentSchema.
///
/// Verifies:
/// - All 15 MVP intents are registered
/// - Schema metadata is complete
/// - Routing targets are correctly configured
/// - Entity validation works correctly
/// - Query methods return expected results
final class IntentRegistryTests: XCTestCase {

    // MARK: - Registration Tests

    func testAllIntentsAreRegistered() {
        // GIVEN: All 15 MVP intent types
        let expectedIntents: [IntentType] = [
            .clubAdjustment, .recoveryCheck, .shotRecommendation, .scoreEntry, .patternQuery,
            .drillRequest, .weatherCheck, .statsLookup, .roundStart, .roundEnd,
            .equipmentInfo, .courseInfo, .settingsChange, .helpRequest, .feedback
        ]

        // WHEN: Getting all schemas
        let schemas = IntentRegistry.getAllSchemas()

        // THEN: All intents are registered
        XCTAssertEqual(schemas.count, 15, "Should have exactly 15 registered intents")

        for intentType in expectedIntents {
            let schema = IntentRegistry.getSchema(for: intentType)
            XCTAssertEqual(schema.intentType, intentType, "Schema should exist for \(intentType)")
        }
    }

    func testGetSchemaReturnsCorrectSchema() {
        // GIVEN: A specific intent type
        let intentType = IntentType.clubAdjustment

        // WHEN: Getting schema
        let schema = IntentRegistry.getSchema(for: intentType)

        // THEN: Schema matches
        XCTAssertEqual(schema.intentType, .clubAdjustment)
        XCTAssertEqual(schema.displayName, "Club Adjustment")
        XCTAssertFalse(schema.description.isEmpty)
        XCTAssertTrue(schema.requiredEntities.contains(.club))
    }

    // MARK: - Schema Metadata Tests

    func testAllSchemasHaveDisplayNames() {
        // GIVEN: All schemas
        let schemas = IntentRegistry.getAllSchemas()

        // THEN: Each has a non-empty display name
        for schema in schemas {
            XCTAssertFalse(
                schema.displayName.isEmpty,
                "\(schema.intentType) should have a display name"
            )
        }
    }

    func testAllSchemasHaveDescriptions() {
        // GIVEN: All schemas
        let schemas = IntentRegistry.getAllSchemas()

        // THEN: Each has a non-empty description
        for schema in schemas {
            XCTAssertFalse(
                schema.description.isEmpty,
                "\(schema.intentType) should have a description"
            )
        }
    }

    func testAllSchemasHaveExamplePhrases() {
        // GIVEN: All schemas
        let schemas = IntentRegistry.getAllSchemas()

        // THEN: Each has at least one example phrase
        for schema in schemas {
            XCTAssertFalse(
                schema.examplePhrases.isEmpty,
                "\(schema.intentType) should have example phrases"
            )
            XCTAssertGreaterThanOrEqual(
                schema.examplePhrases.count,
                3,
                "\(schema.intentType) should have at least 3 example phrases"
            )
        }
    }

    // MARK: - Routing Target Tests

    func testClubAdjustmentRoutingTarget() {
        // GIVEN: Club adjustment intent
        let schema = IntentRegistry.getSchema(for: .clubAdjustment)

        // THEN: Routes to correct module and screen
        XCTAssertNotNil(schema.defaultRoutingTarget)
        XCTAssertEqual(schema.defaultRoutingTarget?.module, .caddy)
        XCTAssertEqual(schema.defaultRoutingTarget?.screen, "ClubAdjustmentScreen")
        XCTAssertTrue(schema.requiresNavigation)
    }

    func testRecoveryCheckRoutingTarget() {
        // GIVEN: Recovery check intent
        let schema = IntentRegistry.getSchema(for: .recoveryCheck)

        // THEN: Routes to recovery module
        XCTAssertNotNil(schema.defaultRoutingTarget)
        XCTAssertEqual(schema.defaultRoutingTarget?.module, .recovery)
        XCTAssertEqual(schema.defaultRoutingTarget?.screen, "RecoveryOverviewScreen")
        XCTAssertTrue(schema.requiresNavigation)
    }

    func testDrillRequestRoutingTarget() {
        // GIVEN: Drill request intent
        let schema = IntentRegistry.getSchema(for: .drillRequest)

        // THEN: Routes to coach module
        XCTAssertNotNil(schema.defaultRoutingTarget)
        XCTAssertEqual(schema.defaultRoutingTarget?.module, .coach)
        XCTAssertEqual(schema.defaultRoutingTarget?.screen, "DrillScreen")
        XCTAssertTrue(schema.requiresNavigation)
    }

    func testEquipmentInfoRoutingTarget() {
        // GIVEN: Equipment info intent
        let schema = IntentRegistry.getSchema(for: .equipmentInfo)

        // THEN: Routes to settings module
        XCTAssertNotNil(schema.defaultRoutingTarget)
        XCTAssertEqual(schema.defaultRoutingTarget?.module, .settings)
        XCTAssertEqual(schema.defaultRoutingTarget?.screen, "EquipmentScreen")
        XCTAssertTrue(schema.requiresNavigation)
    }

    // MARK: - No-Navigation Intents Tests

    func testGetNoNavigationIntents() {
        // WHEN: Getting no-navigation intents
        let noNavIntents = IntentRegistry.getNoNavigationIntents()

        // THEN: Only patternQuery and helpRequest should be included
        XCTAssertEqual(noNavIntents.count, 2)

        let intentTypes = noNavIntents.map { $0.intentType }
        XCTAssertTrue(intentTypes.contains(.patternQuery))
        XCTAssertTrue(intentTypes.contains(.helpRequest))
    }

    func testPatternQueryDoesNotRequireNavigation() {
        // GIVEN: Pattern query intent
        let schema = IntentRegistry.getSchema(for: .patternQuery)

        // THEN: Does not require navigation
        XCTAssertFalse(schema.requiresNavigation)
        XCTAssertNil(schema.defaultRoutingTarget)
    }

    func testHelpRequestDoesNotRequireNavigation() {
        // GIVEN: Help request intent
        let schema = IntentRegistry.getSchema(for: .helpRequest)

        // THEN: Does not require navigation
        XCTAssertFalse(schema.requiresNavigation)
        XCTAssertNil(schema.defaultRoutingTarget)
    }

    // MARK: - Module Query Tests

    func testGetIntentsForCaddyModule() {
        // WHEN: Getting intents for caddy module
        let caddyIntents = IntentRegistry.getIntents(for: .caddy)

        // THEN: Contains correct intents
        let intentTypes = caddyIntents.map { $0.intentType }
        XCTAssertTrue(intentTypes.contains(.clubAdjustment))
        XCTAssertTrue(intentTypes.contains(.shotRecommendation))
        XCTAssertTrue(intentTypes.contains(.scoreEntry))
        XCTAssertTrue(intentTypes.contains(.weatherCheck))
        XCTAssertTrue(intentTypes.contains(.statsLookup))
        XCTAssertTrue(intentTypes.contains(.roundStart))
        XCTAssertTrue(intentTypes.contains(.roundEnd))
        XCTAssertTrue(intentTypes.contains(.courseInfo))
    }

    func testGetIntentsForCoachModule() {
        // WHEN: Getting intents for coach module
        let coachIntents = IntentRegistry.getIntents(for: .coach)

        // THEN: Contains drill request
        XCTAssertEqual(coachIntents.count, 1)
        XCTAssertEqual(coachIntents.first?.intentType, .drillRequest)
    }

    func testGetIntentsForRecoveryModule() {
        // WHEN: Getting intents for recovery module
        let recoveryIntents = IntentRegistry.getIntents(for: .recovery)

        // THEN: Contains recovery check
        XCTAssertEqual(recoveryIntents.count, 1)
        XCTAssertEqual(recoveryIntents.first?.intentType, .recoveryCheck)
    }

    func testGetIntentsForSettingsModule() {
        // WHEN: Getting intents for settings module
        let settingsIntents = IntentRegistry.getIntents(for: .settings)

        // THEN: Contains equipment info, settings change, and feedback
        let intentTypes = settingsIntents.map { $0.intentType }
        XCTAssertTrue(intentTypes.contains(.equipmentInfo))
        XCTAssertTrue(intentTypes.contains(.settingsChange))
        XCTAssertTrue(intentTypes.contains(.feedback))
    }

    // MARK: - Entity Validation Tests

    func testValidateEntities_WithAllRequiredEntities_ReturnsValid() {
        // GIVEN: Club adjustment intent and entities with required club
        let club = Club(name: "7-iron", type: .iron)
        let entities = ExtractedEntities(club: club)

        // WHEN: Validating entities
        let result = IntentRegistry.validateEntities(for: .clubAdjustment, entities: entities)

        // THEN: Validation succeeds
        XCTAssertEqual(result, .valid)
    }

    func testValidateEntities_MissingRequiredEntity_ReturnsMissingEntities() {
        // GIVEN: Club adjustment intent but no club entity
        let entities = ExtractedEntities()

        // WHEN: Validating entities
        let result = IntentRegistry.validateEntities(for: .clubAdjustment, entities: entities)

        // THEN: Validation fails with missing club
        if case .missingEntities(let missing) = result {
            XCTAssertTrue(missing.contains(.club))
        } else {
            XCTFail("Expected missingEntities result")
        }
    }

    func testValidateEntities_IntentWithNoRequiredEntities_ReturnsValid() {
        // GIVEN: Recovery check intent (no required entities) and empty entities
        let entities = ExtractedEntities()

        // WHEN: Validating entities
        let result = IntentRegistry.validateEntities(for: .recoveryCheck, entities: entities)

        // THEN: Validation succeeds
        XCTAssertEqual(result, .valid)
    }

    func testValidateEntities_WithOptionalEntities_ReturnsValid() {
        // GIVEN: Shot recommendation with optional entities
        let club = Club(name: "Driver", type: .driver)
        let entities = ExtractedEntities(
            club: club,
            yardage: 250,
            lie: .tee,
            wind: "10mph headwind"
        )

        // WHEN: Validating entities
        let result = IntentRegistry.validateEntities(for: .shotRecommendation, entities: entities)

        // THEN: Validation succeeds
        XCTAssertEqual(result, .valid)
    }

    // MARK: - Required/Optional Entity Tests

    func testClubAdjustmentRequiredEntities() {
        // GIVEN: Club adjustment schema
        let schema = IntentRegistry.getSchema(for: .clubAdjustment)

        // THEN: Requires club
        XCTAssertTrue(schema.requiredEntities.contains(.club))
        XCTAssertTrue(schema.optionalEntities.contains(.yardage))
    }

    func testRecoveryCheckOptionalEntities() {
        // GIVEN: Recovery check schema
        let schema = IntentRegistry.getSchema(for: .recoveryCheck)

        // THEN: No required entities, fatigue and pain are optional
        XCTAssertTrue(schema.requiredEntities.isEmpty)
        XCTAssertTrue(schema.optionalEntities.contains(.fatigue))
        XCTAssertTrue(schema.optionalEntities.contains(.pain))
    }

    func testShotRecommendationOptionalEntities() {
        // GIVEN: Shot recommendation schema
        let schema = IntentRegistry.getSchema(for: .shotRecommendation)

        // THEN: No required entities, multiple optional
        XCTAssertTrue(schema.requiredEntities.isEmpty)
        XCTAssertTrue(schema.optionalEntities.contains(.club))
        XCTAssertTrue(schema.optionalEntities.contains(.yardage))
        XCTAssertTrue(schema.optionalEntities.contains(.lie))
        XCTAssertTrue(schema.optionalEntities.contains(.wind))
    }

    // MARK: - IntentSchema Identifiable Tests

    func testIntentSchemaIdentifiable() {
        // GIVEN: A schema
        let schema = IntentRegistry.getSchema(for: .clubAdjustment)

        // THEN: ID matches intent type
        XCTAssertEqual(schema.id, .clubAdjustment)
    }

    // MARK: - IntentSchema Hashable Tests

    func testIntentSchemaHashable() {
        // GIVEN: Two schemas with same intent type
        let schema1 = IntentRegistry.getSchema(for: .clubAdjustment)
        let schema2 = IntentRegistry.getSchema(for: .clubAdjustment)

        // THEN: They are equal
        XCTAssertEqual(schema1, schema2)
    }

    func testIntentSchemasInSet() {
        // GIVEN: Multiple schemas
        let schema1 = IntentRegistry.getSchema(for: .clubAdjustment)
        let schema2 = IntentRegistry.getSchema(for: .recoveryCheck)
        let schema3 = IntentRegistry.getSchema(for: .clubAdjustment) // duplicate

        // WHEN: Adding to a set
        let schemaSet: Set<IntentSchema> = [schema1, schema2, schema3]

        // THEN: Duplicates are removed
        XCTAssertEqual(schemaSet.count, 2)
    }

    // MARK: - ValidationResult Tests

    func testValidationResultEquality() {
        // GIVEN: Two valid results
        let result1 = ValidationResult.valid
        let result2 = ValidationResult.valid

        // THEN: They are equal
        XCTAssertEqual(result1, result2)
    }

    func testValidationResultMissingEntitiesEquality() {
        // GIVEN: Two missing entities results with same entities
        let result1 = ValidationResult.missingEntities([.club, .yardage])
        let result2 = ValidationResult.missingEntities([.club, .yardage])

        // THEN: They are equal
        XCTAssertEqual(result1, result2)
    }

    func testValidationResultInequality() {
        // GIVEN: Valid and missing results
        let result1 = ValidationResult.valid
        let result2 = ValidationResult.missingEntities([.club])

        // THEN: They are not equal
        XCTAssertNotEqual(result1, result2)
    }

    // MARK: - ExtractedEntities Validation Tests

    func testExtractedEntities_ClampsInvalidFatigue() {
        // GIVEN: Fatigue value below valid range
        let tooLow = ExtractedEntities(fatigue: 0)
        // THEN: Clamped to minimum (1)
        XCTAssertEqual(tooLow.fatigue, 1)

        // GIVEN: Fatigue value above valid range
        let tooHigh = ExtractedEntities(fatigue: 11)
        // THEN: Clamped to maximum (10)
        XCTAssertEqual(tooHigh.fatigue, 10)

        // GIVEN: Valid fatigue value
        let valid = ExtractedEntities(fatigue: 5)
        // THEN: Preserved as-is
        XCTAssertEqual(valid.fatigue, 5)
    }

    func testExtractedEntities_RejectsInvalidYardage() {
        // GIVEN: Negative yardage
        let negative = ExtractedEntities(yardage: -10)
        // THEN: Rejected (set to nil)
        XCTAssertNil(negative.yardage)

        // GIVEN: Zero yardage
        let zero = ExtractedEntities(yardage: 0)
        // THEN: Rejected (set to nil)
        XCTAssertNil(zero.yardage)

        // GIVEN: Valid yardage
        let valid = ExtractedEntities(yardage: 150)
        // THEN: Preserved
        XCTAssertEqual(valid.yardage, 150)
    }

    func testExtractedEntities_RejectsInvalidHoleNumber() {
        // GIVEN: Hole number 0
        let tooLow = ExtractedEntities(holeNumber: 0)
        // THEN: Rejected (set to nil)
        XCTAssertNil(tooLow.holeNumber)

        // GIVEN: Hole number 19
        let tooHigh = ExtractedEntities(holeNumber: 19)
        // THEN: Rejected (set to nil)
        XCTAssertNil(tooHigh.holeNumber)

        // GIVEN: Valid hole number
        let valid = ExtractedEntities(holeNumber: 9)
        // THEN: Preserved
        XCTAssertEqual(valid.holeNumber, 9)
    }

    func testExtractedEntities_BoundaryValues() {
        // GIVEN: Boundary values for all validated fields
        let entities = ExtractedEntities(
            yardage: 1,        // Minimum valid
            fatigue: 1,        // Minimum valid
            holeNumber: 18     // Maximum valid
        )

        // THEN: All values preserved
        XCTAssertEqual(entities.yardage, 1)
        XCTAssertEqual(entities.fatigue, 1)
        XCTAssertEqual(entities.holeNumber, 18)
    }
}
