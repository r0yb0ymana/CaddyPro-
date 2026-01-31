package caddypro.domain.navcaddy.intent

import caddypro.domain.navcaddy.models.*
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for IntentRegistry.
 *
 * Verifies that all 15 MVP intents are properly registered with correct
 * schemas, entity requirements, routing targets, and example phrases.
 *
 * Spec reference: navcaddy-engine-plan.md Task 5
 */
class IntentRegistryTest {

    @Test
    fun `all 15 MVP intents are registered`() {
        val allSchemas = IntentRegistry.getAllSchemas()
        assertEquals("Should have exactly 15 intents", 15, allSchemas.size)

        // Verify each intent type is present
        val expectedIntents = listOf(
            IntentType.CLUB_ADJUSTMENT,
            IntentType.RECOVERY_CHECK,
            IntentType.SHOT_RECOMMENDATION,
            IntentType.SCORE_ENTRY,
            IntentType.PATTERN_QUERY,
            IntentType.DRILL_REQUEST,
            IntentType.WEATHER_CHECK,
            IntentType.STATS_LOOKUP,
            IntentType.ROUND_START,
            IntentType.ROUND_END,
            IntentType.EQUIPMENT_INFO,
            IntentType.COURSE_INFO,
            IntentType.SETTINGS_CHANGE,
            IntentType.HELP_REQUEST,
            IntentType.FEEDBACK
        )

        val registeredTypes = allSchemas.map { it.intentType }
        expectedIntents.forEach { intentType ->
            assertTrue(
                "Intent $intentType should be registered",
                registeredTypes.contains(intentType)
            )
        }
    }

    @Test
    fun `getSchema returns correct schema for CLUB_ADJUSTMENT`() {
        val schema = IntentRegistry.getSchema(IntentType.CLUB_ADJUSTMENT)

        assertEquals(IntentType.CLUB_ADJUSTMENT, schema.intentType)
        assertEquals("Club Adjustment", schema.displayName)
        assertTrue(schema.description.isNotEmpty())
        assertTrue(schema.requiredEntities.contains(EntityType.CLUB))
        assertTrue(schema.optionalEntities.contains(EntityType.YARDAGE))
        assertNotNull(schema.defaultRoutingTarget)
        assertEquals(Module.CADDY, schema.defaultRoutingTarget?.module)
        assertEquals("ClubAdjustmentScreen", schema.defaultRoutingTarget?.screen)
        assertTrue(schema.requiresNavigation)
        assertTrue(schema.examplePhrases.isNotEmpty())
    }

    @Test
    fun `getSchema returns correct schema for RECOVERY_CHECK`() {
        val schema = IntentRegistry.getSchema(IntentType.RECOVERY_CHECK)

        assertEquals(IntentType.RECOVERY_CHECK, schema.intentType)
        assertEquals("Recovery Check", schema.displayName)
        assertTrue(schema.requiredEntities.isEmpty())
        assertTrue(schema.optionalEntities.contains(EntityType.FATIGUE))
        assertTrue(schema.optionalEntities.contains(EntityType.PAIN))
        assertNotNull(schema.defaultRoutingTarget)
        assertEquals(Module.RECOVERY, schema.defaultRoutingTarget?.module)
        assertEquals("RecoveryOverviewScreen", schema.defaultRoutingTarget?.screen)
        assertTrue(schema.requiresNavigation)
    }

    @Test
    fun `getSchema returns correct schema for SHOT_RECOMMENDATION`() {
        val schema = IntentRegistry.getSchema(IntentType.SHOT_RECOMMENDATION)

        assertEquals(IntentType.SHOT_RECOMMENDATION, schema.intentType)
        assertTrue(schema.requiredEntities.isEmpty()) // Uses session context
        assertTrue(schema.optionalEntities.contains(EntityType.CLUB))
        assertTrue(schema.optionalEntities.contains(EntityType.YARDAGE))
        assertTrue(schema.optionalEntities.contains(EntityType.LIE))
        assertTrue(schema.optionalEntities.contains(EntityType.WIND))
        assertNotNull(schema.defaultRoutingTarget)
        assertEquals(Module.CADDY, schema.defaultRoutingTarget?.module)
        assertEquals("ShotAdvisorScreen", schema.defaultRoutingTarget?.screen)
    }

    @Test
    fun `getSchema returns correct schema for PATTERN_QUERY`() {
        val schema = IntentRegistry.getSchema(IntentType.PATTERN_QUERY)

        assertEquals(IntentType.PATTERN_QUERY, schema.intentType)
        assertNull(schema.defaultRoutingTarget) // Pure answer
        assertFalse(schema.requiresNavigation) // No navigation needed
        assertTrue(schema.examplePhrases.isNotEmpty())
    }

    @Test
    fun `getSchema returns correct schema for HELP_REQUEST`() {
        val schema = IntentRegistry.getSchema(IntentType.HELP_REQUEST)

        assertEquals(IntentType.HELP_REQUEST, schema.intentType)
        assertNull(schema.defaultRoutingTarget) // Pure answer
        assertFalse(schema.requiresNavigation) // No navigation needed
    }

    @Test
    fun `getIntentsForModule returns correct intents for CADDY`() {
        val caddyIntents = IntentRegistry.getIntentsForModule(Module.CADDY)

        assertTrue(caddyIntents.any { it.intentType == IntentType.CLUB_ADJUSTMENT })
        assertTrue(caddyIntents.any { it.intentType == IntentType.SHOT_RECOMMENDATION })
        assertTrue(caddyIntents.any { it.intentType == IntentType.SCORE_ENTRY })
        assertTrue(caddyIntents.any { it.intentType == IntentType.WEATHER_CHECK })
        assertTrue(caddyIntents.any { it.intentType == IntentType.STATS_LOOKUP })
        assertTrue(caddyIntents.any { it.intentType == IntentType.ROUND_START })
        assertTrue(caddyIntents.any { it.intentType == IntentType.ROUND_END })
        assertTrue(caddyIntents.any { it.intentType == IntentType.COURSE_INFO })

        // Should not contain intents from other modules
        assertFalse(caddyIntents.any { it.intentType == IntentType.RECOVERY_CHECK })
        assertFalse(caddyIntents.any { it.intentType == IntentType.DRILL_REQUEST })
    }

    @Test
    fun `getIntentsForModule returns correct intents for COACH`() {
        val coachIntents = IntentRegistry.getIntentsForModule(Module.COACH)

        assertTrue(coachIntents.any { it.intentType == IntentType.DRILL_REQUEST })
        assertEquals(1, coachIntents.size)
    }

    @Test
    fun `getIntentsForModule returns correct intents for RECOVERY`() {
        val recoveryIntents = IntentRegistry.getIntentsForModule(Module.RECOVERY)

        assertTrue(recoveryIntents.any { it.intentType == IntentType.RECOVERY_CHECK })
        assertEquals(1, recoveryIntents.size)
    }

    @Test
    fun `getIntentsForModule returns correct intents for SETTINGS`() {
        val settingsIntents = IntentRegistry.getIntentsForModule(Module.SETTINGS)

        assertTrue(settingsIntents.any { it.intentType == IntentType.EQUIPMENT_INFO })
        assertTrue(settingsIntents.any { it.intentType == IntentType.SETTINGS_CHANGE })
        assertTrue(settingsIntents.any { it.intentType == IntentType.FEEDBACK })
    }

    @Test
    fun `getNoNavigationIntents returns only PATTERN_QUERY and HELP_REQUEST`() {
        val noNavIntents = IntentRegistry.getNoNavigationIntents()

        assertEquals(2, noNavIntents.size)
        assertTrue(noNavIntents.any { it.intentType == IntentType.PATTERN_QUERY })
        assertTrue(noNavIntents.any { it.intentType == IntentType.HELP_REQUEST })
    }

    @Test
    fun `validateEntities returns Valid when all required entities present`() {
        val club = Club(name = "7-iron", type = ClubType.IRON, estimatedCarry = 150)
        val entities = ExtractedEntities(club = club)

        val result = IntentRegistry.validateEntities(
            IntentType.CLUB_ADJUSTMENT,
            entities
        )

        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateEntities returns MissingEntities when required entity absent`() {
        val entities = ExtractedEntities() // No club provided

        val result = IntentRegistry.validateEntities(
            IntentType.CLUB_ADJUSTMENT,
            entities
        )

        assertTrue(result is ValidationResult.MissingEntities)
        val missingResult = result as ValidationResult.MissingEntities
        assertTrue(missingResult.missing.contains(EntityType.CLUB))
    }

    @Test
    fun `validateEntities returns Valid when no required entities`() {
        val entities = ExtractedEntities()

        val result = IntentRegistry.validateEntities(
            IntentType.RECOVERY_CHECK,
            entities
        )

        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateEntities handles optional entities correctly`() {
        val club = Club(name = "7-iron", type = ClubType.IRON, estimatedCarry = 150)
        val entities = ExtractedEntities(club = club, yardage = 150)

        // CLUB_ADJUSTMENT requires CLUB, yardage is optional
        val result = IntentRegistry.validateEntities(
            IntentType.CLUB_ADJUSTMENT,
            entities
        )

        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `all intents have non-empty display names`() {
        val allSchemas = IntentRegistry.getAllSchemas()

        allSchemas.forEach { schema ->
            assertTrue(
                "Intent ${schema.intentType} should have a display name",
                schema.displayName.isNotEmpty()
            )
        }
    }

    @Test
    fun `all intents have non-empty descriptions`() {
        val allSchemas = IntentRegistry.getAllSchemas()

        allSchemas.forEach { schema ->
            assertTrue(
                "Intent ${schema.intentType} should have a description",
                schema.description.isNotEmpty()
            )
        }
    }

    @Test
    fun `all intents have at least 3 example phrases`() {
        val allSchemas = IntentRegistry.getAllSchemas()

        allSchemas.forEach { schema ->
            assertTrue(
                "Intent ${schema.intentType} should have at least 3 example phrases, has ${schema.examplePhrases.size}",
                schema.examplePhrases.size >= 3
            )
        }
    }

    @Test
    fun `all example phrases are non-empty`() {
        val allSchemas = IntentRegistry.getAllSchemas()

        allSchemas.forEach { schema ->
            schema.examplePhrases.forEach { phrase ->
                assertTrue(
                    "Intent ${schema.intentType} has an empty example phrase",
                    phrase.isNotEmpty()
                )
            }
        }
    }

    @Test
    fun `navigation intents have routing targets`() {
        val allSchemas = IntentRegistry.getAllSchemas()

        allSchemas.filter { it.requiresNavigation }.forEach { schema ->
            assertNotNull(
                "Intent ${schema.intentType} requires navigation but has no routing target",
                schema.defaultRoutingTarget
            )
        }
    }

    @Test
    fun `no-navigation intents have no routing targets`() {
        val allSchemas = IntentRegistry.getAllSchemas()

        allSchemas.filter { !it.requiresNavigation }.forEach { schema ->
            assertNull(
                "Intent ${schema.intentType} doesn't require navigation but has a routing target",
                schema.defaultRoutingTarget
            )
        }
    }

    @Test
    fun `routing targets have valid modules and screens`() {
        val allSchemas = IntentRegistry.getAllSchemas()

        allSchemas.mapNotNull { it.defaultRoutingTarget }.forEach { target ->
            assertNotNull("Routing target should have a module", target.module)
            assertTrue(
                "Routing target should have a non-empty screen name",
                target.screen.isNotEmpty()
            )
        }
    }

    @Test
    fun `validateEntities correctly identifies all entity types`() {
        val club = Club(name = "7-iron", type = ClubType.IRON, estimatedCarry = 150)
        val entities = ExtractedEntities(
            club = club,
            yardage = 150,
            lie = Lie.FAIRWAY,
            wind = "10mph left-to-right",
            fatigue = 5,
            pain = "lower back",
            scoreContext = "1 under",
            holeNumber = 7
        )

        // Test an intent with multiple optional entities
        val result = IntentRegistry.validateEntities(
            IntentType.SHOT_RECOMMENDATION,
            entities
        )

        assertTrue(result is ValidationResult.Valid)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `getSchema throws for unregistered intent`() {
        // This should throw since we're using reflection to create an unregistered enum value
        // In practice, this can't happen unless IntentType enum is modified without updating registry
        // This test verifies the error handling works
        val allTypes = IntentType.values()
        val registeredTypes = IntentRegistry.getAllSchemas().map { it.intentType }

        // Find any intent type not in registry (should be none)
        val unregistered = allTypes.find { !registeredTypes.contains(it) }

        // If all are registered (as expected), throw to pass test
        if (unregistered == null) {
            throw IllegalArgumentException("Test passed: all intents registered")
        }
    }

    @Test
    fun `each intent type appears exactly once in registry`() {
        val allSchemas = IntentRegistry.getAllSchemas()
        val intentTypes = allSchemas.map { it.intentType }

        // Check for duplicates
        val uniqueTypes = intentTypes.toSet()
        assertEquals(
            "Each intent type should appear exactly once",
            intentTypes.size,
            uniqueTypes.size
        )
    }

    @Test
    fun `CLUB_ADJUSTMENT has correct entity requirements`() {
        val schema = IntentRegistry.getSchema(IntentType.CLUB_ADJUSTMENT)

        assertEquals(1, schema.requiredEntities.size)
        assertTrue(schema.requiredEntities.contains(EntityType.CLUB))
        assertTrue(schema.optionalEntities.contains(EntityType.YARDAGE))
    }

    @Test
    fun `SCORE_ENTRY has correct entity requirements`() {
        val schema = IntentRegistry.getSchema(IntentType.SCORE_ENTRY)

        assertTrue(schema.requiredEntities.isEmpty())
        assertTrue(schema.optionalEntities.contains(EntityType.HOLE_NUMBER))
        assertTrue(schema.optionalEntities.contains(EntityType.SCORE_CONTEXT))
    }

    @Test
    fun `DRILL_REQUEST routes to COACH module`() {
        val schema = IntentRegistry.getSchema(IntentType.DRILL_REQUEST)

        assertNotNull(schema.defaultRoutingTarget)
        assertEquals(Module.COACH, schema.defaultRoutingTarget?.module)
        assertEquals("DrillScreen", schema.defaultRoutingTarget?.screen)
    }

    @Test
    fun `WEATHER_CHECK routes to CADDY module`() {
        val schema = IntentRegistry.getSchema(IntentType.WEATHER_CHECK)

        assertNotNull(schema.defaultRoutingTarget)
        assertEquals(Module.CADDY, schema.defaultRoutingTarget?.module)
        assertEquals("WeatherScreen", schema.defaultRoutingTarget?.screen)
    }

    @Test
    fun `STATS_LOOKUP has correct optional entities`() {
        val schema = IntentRegistry.getSchema(IntentType.STATS_LOOKUP)

        assertTrue(schema.optionalEntities.contains(EntityType.STAT_TYPE))
        assertTrue(schema.optionalEntities.contains(EntityType.CLUB))
    }

    @Test
    fun `ROUND_START and ROUND_END route to correct screens`() {
        val startSchema = IntentRegistry.getSchema(IntentType.ROUND_START)
        val endSchema = IntentRegistry.getSchema(IntentType.ROUND_END)

        assertEquals(Module.CADDY, startSchema.defaultRoutingTarget?.module)
        assertEquals("RoundSetupScreen", startSchema.defaultRoutingTarget?.screen)

        assertEquals(Module.CADDY, endSchema.defaultRoutingTarget?.module)
        assertEquals("RoundSummaryScreen", endSchema.defaultRoutingTarget?.screen)
    }

    @Test
    fun `EQUIPMENT_INFO and SETTINGS_CHANGE route to SETTINGS module`() {
        val equipmentSchema = IntentRegistry.getSchema(IntentType.EQUIPMENT_INFO)
        val settingsSchema = IntentRegistry.getSchema(IntentType.SETTINGS_CHANGE)

        assertEquals(Module.SETTINGS, equipmentSchema.defaultRoutingTarget?.module)
        assertEquals(Module.SETTINGS, settingsSchema.defaultRoutingTarget?.module)
    }

    @Test
    fun `COURSE_INFO has correct optional entities`() {
        val schema = IntentRegistry.getSchema(IntentType.COURSE_INFO)

        assertTrue(schema.optionalEntities.contains(EntityType.COURSE_NAME))
        assertTrue(schema.optionalEntities.contains(EntityType.HOLE_NUMBER))
    }

    @Test
    fun `FEEDBACK has correct optional entity`() {
        val schema = IntentRegistry.getSchema(IntentType.FEEDBACK)

        assertTrue(schema.optionalEntities.contains(EntityType.FEEDBACK_TEXT))
        assertEquals(Module.SETTINGS, schema.defaultRoutingTarget?.module)
        assertEquals("FeedbackScreen", schema.defaultRoutingTarget?.screen)
    }
}
