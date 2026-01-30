package caddypro.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for PIIRedactor.
 *
 * Spec reference: navcaddy-engine.md R8, R9
 * Plan reference: navcaddy-engine-plan.md Task 22
 */
class PIIRedactorTest {

    @Test
    fun `redact email addresses`() {
        val input = "Contact me at john.doe@example.com for details"
        val redacted = PIIRedactor.redact(input)
        assertEquals("Contact me at [EMAIL_REDACTED] for details", redacted)
    }

    @Test
    fun `redact multiple emails`() {
        val input = "Send to user@test.com and admin@example.org"
        val redacted = PIIRedactor.redact(input)
        assertEquals("Send to [EMAIL_REDACTED] and [EMAIL_REDACTED]", redacted)
    }

    @Test
    fun `redact phone numbers - US format`() {
        val input = "Call me at 555-123-4567"
        val redacted = PIIRedactor.redact(input)
        assertTrue(redacted.contains("[PHONE_REDACTED]"))
    }

    @Test
    fun `redact phone numbers - parentheses format`() {
        val input = "My number is (555) 123-4567"
        val redacted = PIIRedactor.redact(input)
        assertTrue(redacted.contains("[PHONE_REDACTED]"))
    }

    @Test
    fun `redact potential names`() {
        val input = "I played with John Smith yesterday"
        val redacted = PIIRedactor.redact(input, redactNames = true)
        assertTrue(redacted.contains("[NAME_REDACTED]"))
    }

    @Test
    fun `do not redact names when disabled`() {
        val input = "I played with John Smith yesterday"
        val redacted = PIIRedactor.redact(input, redactNames = false)
        assertEquals(input, redacted)
    }

    @Test
    fun `redact credit card numbers`() {
        val input = "Card number 4532-1234-5678-9010"
        val redacted = PIIRedactor.redact(input)
        assertEquals("Card number [CC_REDACTED]", redacted)
    }

    @Test
    fun `redact SSN`() {
        val input = "SSN: 123-45-6789"
        val redacted = PIIRedactor.redact(input)
        assertEquals("SSN: [SSN_REDACTED]", redacted)
    }

    @Test
    fun `redact street addresses`() {
        val input = "I live at 123 Main Street"
        val redacted = PIIRedactor.redact(input)
        assertEquals("I live at [ADDRESS_REDACTED]", redacted)
    }

    @Test
    fun `preserve golf terminology`() {
        val input = "I'm 150 yards out with my 7-iron"
        val redacted = PIIRedactor.redact(input, redactNames = false)
        assertEquals(input, redacted)
    }

    @Test
    fun `redact multiple PII types`() {
        val input = "Contact John Smith at john@example.com or 555-1234"
        val redacted = PIIRedactor.redact(input, redactNames = true)
        assertTrue(redacted.contains("[NAME_REDACTED]"))
        assertTrue(redacted.contains("[EMAIL_REDACTED]"))
        assertTrue(redacted.contains("[PHONE_REDACTED]"))
    }

    @Test
    fun `redactMap preserves keys and redacts values`() {
        val input = mapOf(
            "email" to "user@example.com",
            "club" to "7-iron",
            "phone" to "555-1234"
        )
        val redacted = PIIRedactor.redactMap(input)
        assertEquals("[EMAIL_REDACTED]", redacted["email"])
        assertEquals("7-iron", redacted["club"])
        assertTrue(redacted["phone"]?.contains("[PHONE_REDACTED]") == true)
    }

    @Test
    fun `containsPII detects email`() {
        assertTrue(PIIRedactor.containsPII("Contact me at user@example.com"))
    }

    @Test
    fun `containsPII detects phone`() {
        assertTrue(PIIRedactor.containsPII("Call 555-1234"))
    }

    @Test
    fun `containsPII returns false for clean text`() {
        assertFalse(PIIRedactor.containsPII("I hit my 7-iron 150 yards"))
    }

    @Test
    fun `empty string returns empty`() {
        assertEquals("", PIIRedactor.redact(""))
    }

    @Test
    fun `text without PII unchanged`() {
        val input = "The weather is nice today"
        val redacted = PIIRedactor.redact(input, redactNames = false)
        assertEquals(input, redacted)
    }
}
