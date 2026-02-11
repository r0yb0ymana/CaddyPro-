package caddypro.domain.navcaddy.persona

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for BonesPersona.
 *
 * Validates system prompt generation and persona characteristics.
 *
 * Spec reference: navcaddy-engine.md R4, navcaddy-engine-plan.md Task 16
 */
class BonesPersonaTest {

    @Test
    fun `persona name is Bones`() {
        assertEquals("Bones", BonesPersona.PERSONA_NAME)
    }

    @Test
    fun `system prompt contains persona introduction`() {
        val prompt = BonesPersona.getSystemPrompt()

        assertTrue(prompt.contains("You are Bones"))
        assertTrue(prompt.contains("professional golf caddy"))
    }

    @Test
    fun `system prompt includes core responsibilities`() {
        val prompt = BonesPersona.getSystemPrompt()

        // Check for key responsibilities
        assertTrue(prompt.contains("Club selection"))
        assertTrue(prompt.contains("Shot strategy"))
        assertTrue(prompt.contains("swing patterns"))
        assertTrue(prompt.contains("Recovery"))
    }

    @Test
    fun `system prompt includes voice characteristics`() {
        val prompt = BonesPersona.getSystemPrompt()

        assertTrue(prompt.contains("Tactical and context-aware"))
        assertTrue(prompt.contains("Warm but professional"))
        assertTrue(prompt.contains("golf-caddy language"))
    }

    @Test
    fun `system prompt includes medical disclaimer constraint`() {
        val prompt = BonesPersona.getSystemPrompt()

        assertTrue(prompt.contains("Never provide medical advice") || prompt.contains("medical"))
        assertTrue(prompt.contains("disclaimer"))
    }

    @Test
    fun `system prompt forbids guarantees`() {
        val prompt = BonesPersona.getSystemPrompt()

        assertTrue(prompt.contains("Never guarantee") || prompt.contains("guarantee"))
    }

    @Test
    fun `system prompt forbids betting advice`() {
        val prompt = BonesPersona.getSystemPrompt()

        assertTrue(prompt.contains("betting") || prompt.contains("gambling"))
    }

    @Test
    fun `system prompt includes uncertainty guidance`() {
        val prompt = BonesPersona.getSystemPrompt()

        assertTrue(prompt.contains("uncertainty") || prompt.contains("limited"))
    }

    @Test
    fun `system prompt emphasizes no swing technique without disclaimer`() {
        val prompt = BonesPersona.getSystemPrompt()

        assertTrue(prompt.contains("swing technique") && prompt.contains("disclaimer"))
    }

    @Test
    fun `characteristics list is not empty`() {
        val characteristics = BonesPersona.getCharacteristics()

        assertTrue(characteristics.isNotEmpty())
        assertTrue(characteristics.size >= 4)
    }

    @Test
    fun `characteristics include tactical and context-aware`() {
        val characteristics = BonesPersona.getCharacteristics()

        assertTrue(characteristics.any { it.contains("Tactical", ignoreCase = true) })
        assertTrue(characteristics.any { it.contains("context-aware", ignoreCase = true) })
    }

    @Test
    fun `description is concise and informative`() {
        val description = BonesPersona.getDescription()

        assertNotNull(description)
        assertTrue(description.isNotBlank())
        assertTrue(description.contains("caddy", ignoreCase = true))
        assertTrue(description.length < 200) // Should be concise
    }

    @Test
    fun `tone guidelines include all required keys`() {
        val guidelines = BonesPersona.getToneGuidelines()

        assertTrue(guidelines.containsKey("formality"))
        assertTrue(guidelines.containsKey("verbosity"))
        assertTrue(guidelines.containsKey("personality"))
        assertTrue(guidelines.containsKey("domain"))
        assertTrue(guidelines.containsKey("uncertainty"))
    }

    @Test
    fun `tone guidelines specify warm professional formality`() {
        val guidelines = BonesPersona.getToneGuidelines()

        assertEquals("warm_professional", guidelines["formality"])
    }

    @Test
    fun `tone guidelines specify balanced verbosity`() {
        val guidelines = BonesPersona.getToneGuidelines()

        assertEquals("balanced", guidelines["verbosity"])
    }

    @Test
    fun `tone guidelines specify friendly expert personality`() {
        val guidelines = BonesPersona.getToneGuidelines()

        assertEquals("friendly_expert", guidelines["personality"])
    }

    @Test
    fun `tone guidelines specify golf caddy domain`() {
        val guidelines = BonesPersona.getToneGuidelines()

        assertEquals("golf_caddy", guidelines["domain"])
    }

    @Test
    fun `tone guidelines require explicit uncertainty signaling`() {
        val guidelines = BonesPersona.getToneGuidelines()

        assertEquals("explicit", guidelines["uncertainty"])
    }

    @Test
    fun `system prompt is substantial and complete`() {
        val prompt = BonesPersona.getSystemPrompt()

        // Should be detailed enough to guide LLM behavior
        assertTrue(prompt.length > 500)

        // Should have multiple sections
        val sections = prompt.split("\n\n")
        assertTrue(sections.size >= 4)
    }

    @Test
    fun `system prompt ends with reminder about role`() {
        val prompt = BonesPersona.getSystemPrompt()

        // Should reinforce the role at the end
        assertTrue(prompt.contains("Remember"))
        assertTrue(prompt.contains("helpful expert"))
    }

    @Test
    fun `persona characteristics avoid cringe roleplay`() {
        val prompt = BonesPersona.getSystemPrompt()

        // Should mention restraint in caddy language
        assertTrue(prompt.contains("restrained") || prompt.contains("natural"))
        assertTrue(prompt.contains("no forced roleplay") || prompt.contains("no cringe"))
    }

    @Test
    fun `system prompt emphasizes contextual information usage`() {
        val prompt = BonesPersona.getSystemPrompt()

        assertTrue(prompt.contains("Reference user patterns") || prompt.contains("contextual information"))
        assertTrue(prompt.contains("session"))
    }
}
