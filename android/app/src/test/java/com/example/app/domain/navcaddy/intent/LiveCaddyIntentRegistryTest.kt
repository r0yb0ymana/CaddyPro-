package caddypro.domain.navcaddy.intent

import caddypro.domain.navcaddy.models.IntentType
import caddypro.domain.navcaddy.models.Module
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Live Caddy intent registration in IntentRegistry.
 *
 * Verifies that all Live Caddy intents (SHOT_RECOMMENDATION, BAILOUT_QUERY,
 * WEATHER_CHECK, READINESS_CHECK, CLUB_ADJUSTMENT) are properly registered
 * with correct routing targets, parameters, and example phrases.
 *
 * Spec reference: live-caddy-mode.md R5, live-caddy-mode-plan.md Task 24
 */
class LiveCaddyIntentRegistryTest {

    @Test
    fun `SHOT_RECOMMENDATION is registered with correct schema`() {
        val schema = IntentRegistry.getSchema(IntentType.SHOT_RECOMMENDATION)

        assertEquals("Shot Recommendation", schema.displayName)
        assertEquals("Get shot advice based on current situation", schema.description)
        assertTrue(schema.requiresNavigation)
        assertNotNull(schema.defaultRoutingTarget)
        assertEquals(Module.CADDY, schema.defaultRoutingTarget?.module)
        assertEquals("LiveCaddyScreen", schema.defaultRoutingTarget?.screen)
        assertTrue(schema.defaultRoutingTarget?.parameters?.containsKey("expandStrategy") == true)
        assertEquals(true, schema.defaultRoutingTarget?.parameters?.get("expandStrategy"))
    }

    @Test
    fun `SHOT_RECOMMENDATION has correct example phrases`() {
        val schema = IntentRegistry.getSchema(IntentType.SHOT_RECOMMENDATION)

        assertTrue(schema.examplePhrases.isNotEmpty())
        assertTrue(schema.examplePhrases.contains("What's the play off the tee?"))
        assertTrue(schema.examplePhrases.any { it.contains("club", ignoreCase = true) })
        assertTrue(schema.examplePhrases.any { it.contains("wind", ignoreCase = true) })
    }

    @Test
    fun `SHOT_RECOMMENDATION has correct entity requirements`() {
        val schema = IntentRegistry.getSchema(IntentType.SHOT_RECOMMENDATION)

        assertTrue(schema.requiredEntities.isEmpty()) // Context from active round
        assertTrue(schema.optionalEntities.isNotEmpty())
    }

    @Test
    fun `BAILOUT_QUERY is registered with correct schema`() {
        val schema = IntentRegistry.getSchema(IntentType.BAILOUT_QUERY)

        assertEquals("Bailout Query", schema.displayName)
        assertEquals("Ask where to aim for safe miss or bailout area", schema.description)
        assertTrue(schema.requiresNavigation)
        assertNotNull(schema.defaultRoutingTarget)
        assertEquals(Module.CADDY, schema.defaultRoutingTarget?.module)
        assertEquals("LiveCaddyScreen", schema.defaultRoutingTarget?.screen)
    }

    @Test
    fun `BAILOUT_QUERY has correct routing parameters`() {
        val schema = IntentRegistry.getSchema(IntentType.BAILOUT_QUERY)

        val params = schema.defaultRoutingTarget?.parameters
        assertNotNull(params)
        assertTrue(params?.containsKey("expandStrategy") == true)
        assertTrue(params?.containsKey("highlightBailout") == true)
        assertEquals(true, params?.get("expandStrategy"))
        assertEquals(true, params?.get("highlightBailout"))
    }

    @Test
    fun `BAILOUT_QUERY has correct example phrases`() {
        val schema = IntentRegistry.getSchema(IntentType.BAILOUT_QUERY)

        assertTrue(schema.examplePhrases.isNotEmpty())
        assertTrue(schema.examplePhrases.contains("Where's the bailout?"))
        assertTrue(schema.examplePhrases.contains("Where should I miss?"))
        assertTrue(schema.examplePhrases.contains("What's the safe play?"))
    }

    @Test
    fun `WEATHER_CHECK routes to LiveCaddyScreen`() {
        val schema = IntentRegistry.getSchema(IntentType.WEATHER_CHECK)

        assertEquals("Weather Check", schema.displayName)
        assertEquals(Module.CADDY, schema.defaultRoutingTarget?.module)
        assertEquals("LiveCaddyScreen", schema.defaultRoutingTarget?.screen)
        assertTrue(schema.defaultRoutingTarget?.parameters?.containsKey("expandWeather") == true)
        assertEquals(true, schema.defaultRoutingTarget?.parameters?.get("expandWeather"))
    }

    @Test
    fun `WEATHER_CHECK has wind-related example phrases`() {
        val schema = IntentRegistry.getSchema(IntentType.WEATHER_CHECK)

        assertTrue(schema.examplePhrases.any { it.contains("wind", ignoreCase = true) })
        assertTrue(schema.examplePhrases.any { it.contains("weather", ignoreCase = true) })
        assertTrue(schema.examplePhrases.contains("How's the wind?"))
    }

    @Test
    fun `READINESS_CHECK is registered with correct schema`() {
        val schema = IntentRegistry.getSchema(IntentType.READINESS_CHECK)

        assertEquals("Readiness Check", schema.displayName)
        assertEquals("Check readiness score and how it affects strategy", schema.description)
        assertTrue(schema.requiresNavigation)
        assertNotNull(schema.defaultRoutingTarget)
        assertEquals(Module.CADDY, schema.defaultRoutingTarget?.module)
        assertEquals("LiveCaddyScreen", schema.defaultRoutingTarget?.screen)
    }

    @Test
    fun `READINESS_CHECK has expandReadiness parameter`() {
        val schema = IntentRegistry.getSchema(IntentType.READINESS_CHECK)

        val params = schema.defaultRoutingTarget?.parameters
        assertNotNull(params)
        assertTrue(params?.containsKey("expandReadiness") == true)
        assertEquals(true, params?.get("expandReadiness"))
    }

    @Test
    fun `READINESS_CHECK has feeling-related example phrases`() {
        val schema = IntentRegistry.getSchema(IntentType.READINESS_CHECK)

        assertTrue(schema.examplePhrases.isNotEmpty())
        assertTrue(schema.examplePhrases.contains("How am I feeling?"))
        assertTrue(schema.examplePhrases.contains("What's my readiness?"))
        assertTrue(schema.examplePhrases.any { it.contains("ready", ignoreCase = true) })
    }

    @Test
    fun `all Live Caddy intents route to CADDY module`() {
        val liveCaddyIntents = listOf(
            IntentType.SHOT_RECOMMENDATION,
            IntentType.BAILOUT_QUERY,
            IntentType.WEATHER_CHECK,
            IntentType.READINESS_CHECK
        )

        liveCaddyIntents.forEach { intentType ->
            val schema = IntentRegistry.getSchema(intentType)
            assertEquals(
                "Intent $intentType should route to CADDY module",
                Module.CADDY,
                schema.defaultRoutingTarget?.module
            )
        }
    }

    @Test
    fun `all Live Caddy intents route to LiveCaddyScreen`() {
        val liveCaddyIntents = listOf(
            IntentType.SHOT_RECOMMENDATION,
            IntentType.BAILOUT_QUERY,
            IntentType.WEATHER_CHECK,
            IntentType.READINESS_CHECK
        )

        liveCaddyIntents.forEach { intentType ->
            val schema = IntentRegistry.getSchema(intentType)
            assertEquals(
                "Intent $intentType should route to LiveCaddyScreen",
                "LiveCaddyScreen",
                schema.defaultRoutingTarget?.screen
            )
        }
    }

    @Test
    fun `all Live Caddy intents require navigation`() {
        val liveCaddyIntents = listOf(
            IntentType.SHOT_RECOMMENDATION,
            IntentType.BAILOUT_QUERY,
            IntentType.WEATHER_CHECK,
            IntentType.READINESS_CHECK
        )

        liveCaddyIntents.forEach { intentType ->
            val schema = IntentRegistry.getSchema(intentType)
            assertTrue(
                "Intent $intentType should require navigation",
                schema.requiresNavigation
            )
        }
    }

    @Test
    fun `Live Caddy intents have unique routing parameters`() {
        val shotRec = IntentRegistry.getSchema(IntentType.SHOT_RECOMMENDATION)
        val bailout = IntentRegistry.getSchema(IntentType.BAILOUT_QUERY)
        val weather = IntentRegistry.getSchema(IntentType.WEATHER_CHECK)
        val readiness = IntentRegistry.getSchema(IntentType.READINESS_CHECK)

        // Each intent has different parameter combinations
        assertTrue(shotRec.defaultRoutingTarget?.parameters?.containsKey("expandStrategy") == true)
        assertFalse(shotRec.defaultRoutingTarget?.parameters?.containsKey("highlightBailout") == true)

        assertTrue(bailout.defaultRoutingTarget?.parameters?.containsKey("expandStrategy") == true)
        assertTrue(bailout.defaultRoutingTarget?.parameters?.containsKey("highlightBailout") == true)

        assertTrue(weather.defaultRoutingTarget?.parameters?.containsKey("expandWeather") == true)
        assertFalse(weather.defaultRoutingTarget?.parameters?.containsKey("expandStrategy") == true)

        assertTrue(readiness.defaultRoutingTarget?.parameters?.containsKey("expandReadiness") == true)
        assertFalse(readiness.defaultRoutingTarget?.parameters?.containsKey("expandStrategy") == true)
    }

    @Test
    fun `getIntentsForModule returns Live Caddy intents for CADDY module`() {
        val caddyIntents = IntentRegistry.getIntentsForModule(Module.CADDY)

        val liveCaddyIntentTypes = caddyIntents.map { it.intentType }
        assertTrue(liveCaddyIntentTypes.contains(IntentType.SHOT_RECOMMENDATION))
        assertTrue(liveCaddyIntentTypes.contains(IntentType.BAILOUT_QUERY))
        assertTrue(liveCaddyIntentTypes.contains(IntentType.WEATHER_CHECK))
        assertTrue(liveCaddyIntentTypes.contains(IntentType.READINESS_CHECK))
    }

    @Test
    fun `Live Caddy intents do not require entities by default`() {
        // Live Caddy intents should work with context from active round
        val liveCaddyIntents = listOf(
            IntentType.SHOT_RECOMMENDATION,
            IntentType.BAILOUT_QUERY,
            IntentType.WEATHER_CHECK,
            IntentType.READINESS_CHECK
        )

        liveCaddyIntents.forEach { intentType ->
            val schema = IntentRegistry.getSchema(intentType)
            assertTrue(
                "Intent $intentType should not require entities (uses round context)",
                schema.requiredEntities.isEmpty()
            )
        }
    }

    @Test
    fun `CLUB_ADJUSTMENT maintains backward compatibility`() {
        // Ensure CLUB_ADJUSTMENT still works as before (not routed to LiveCaddyScreen)
        val schema = IntentRegistry.getSchema(IntentType.CLUB_ADJUSTMENT)

        assertEquals("Club Adjustment", schema.displayName)
        assertEquals(Module.CADDY, schema.defaultRoutingTarget?.module)
        assertEquals("ClubAdjustmentScreen", schema.defaultRoutingTarget?.screen)
        assertNotEquals("LiveCaddyScreen", schema.defaultRoutingTarget?.screen)
    }

    @Test
    fun `RECOVERY_CHECK maintains backward compatibility`() {
        // Ensure RECOVERY_CHECK still routes to Recovery module, not LiveCaddyScreen
        val schema = IntentRegistry.getSchema(IntentType.RECOVERY_CHECK)

        assertEquals("Recovery Check", schema.displayName)
        assertEquals(Module.RECOVERY, schema.defaultRoutingTarget?.module)
        assertEquals("RecoveryOverviewScreen", schema.defaultRoutingTarget?.screen)
        assertNotEquals("LiveCaddyScreen", schema.defaultRoutingTarget?.screen)
    }

    @Test
    fun `all Live Caddy intents have non-empty example phrases`() {
        val liveCaddyIntents = listOf(
            IntentType.SHOT_RECOMMENDATION,
            IntentType.BAILOUT_QUERY,
            IntentType.WEATHER_CHECK,
            IntentType.READINESS_CHECK
        )

        liveCaddyIntents.forEach { intentType ->
            val schema = IntentRegistry.getSchema(intentType)
            assertTrue(
                "Intent $intentType should have example phrases",
                schema.examplePhrases.isNotEmpty()
            )
            assertTrue(
                "Intent $intentType should have at least 3 example phrases",
                schema.examplePhrases.size >= 3
            )
        }
    }

    @Test
    fun `routing parameters are Boolean type`() {
        val schemas = listOf(
            IntentRegistry.getSchema(IntentType.SHOT_RECOMMENDATION),
            IntentRegistry.getSchema(IntentType.BAILOUT_QUERY),
            IntentRegistry.getSchema(IntentType.WEATHER_CHECK),
            IntentRegistry.getSchema(IntentType.READINESS_CHECK)
        )

        schemas.forEach { schema ->
            schema.defaultRoutingTarget?.parameters?.forEach { (key, value) ->
                assertTrue(
                    "Parameter $key in ${schema.intentType} should be Boolean",
                    value is Boolean
                )
            }
        }
    }

    @Test
    fun `Live Caddy intent validation works correctly`() {
        // All Live Caddy intents have no required entities, so validation should always pass
        val liveCaddyIntents = listOf(
            IntentType.SHOT_RECOMMENDATION,
            IntentType.BAILOUT_QUERY,
            IntentType.WEATHER_CHECK,
            IntentType.READINESS_CHECK
        )

        val emptyEntities = caddypro.domain.navcaddy.models.ExtractedEntities()

        liveCaddyIntents.forEach { intentType ->
            val result = IntentRegistry.validateEntities(intentType, emptyEntities)
            assertTrue(
                "Intent $intentType should validate with empty entities",
                result is ValidationResult.Valid
            )
        }
    }
}
