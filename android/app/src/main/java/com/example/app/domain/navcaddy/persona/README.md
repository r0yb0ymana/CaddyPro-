# Bones Persona Layer

The Bones Persona Layer provides a consistent "Friendly Expert" tour-caddy voice across all NavCaddy conversational interactions.

## Overview

This package contains three main components:

1. **BonesPersona** - System prompt and voice characteristics
2. **PersonaGuardrails** - Forbidden pattern detection and disclaimers
3. **BonesResponseFormatter** - Response post-processing and formatting

## Quick Start

### Basic Response Formatting

```kotlin
@Inject
lateinit var formatter: BonesResponseFormatter

// Format a simple response
val result = formatter.format("Take your 7-iron for this shot.")
displayMessage(result.formattedResponse)
```

### With Pattern References

```kotlin
// Get relevant patterns from the store
val patterns = missPatternStore.getPatterns(clubId = "7-iron")

// Format with patterns
val result = formatter.format(
    rawResponse = llmResponse,
    relevantPatterns = patterns,
    includePatternReferences = true
)

// Display with patterns and any disclaimers
displayMessage(result.formattedResponse)
```

### System Prompt for LLM

```kotlin
// Get the Bones system prompt
val systemPrompt = BonesPersona.getSystemPrompt()

// Use with your LLM client
val llmRequest = LLMRequest(
    systemPrompt = systemPrompt,
    userMessage = userInput,
    context = sessionContext
)
```

### Clarification Formatting

```kotlin
// Low confidence intent - ask for clarification
val clarification = formatter.formatClarification(
    ambiguousInput = "it feels off",
    suggestedIntents = listOf(
        "Club adjustment",
        "Pattern query",
        "Shot recommendation"
    )
)

displayMessage(clarification)
```

### Error Formatting

```kotlin
// Format errors in Bones voice
val errorMessage = when (error) {
    is NetworkException -> formatter.formatError(ErrorType.NETWORK_ERROR)
    is TimeoutException -> formatter.formatError(ErrorType.TIMEOUT)
    is UnknownIntentException -> formatter.formatError(ErrorType.UNKNOWN_INTENT)
    else -> formatter.formatError(ErrorType.SERVICE_UNAVAILABLE)
}

displayError(errorMessage)
```

## Persona Characteristics

The Bones persona is defined as:

- **Tone**: Warm but professional
- **Verbosity**: Balanced (not ultra-terse, not verbose)
- **Domain**: Golf caddy language (natural, no cringe roleplay)
- **Personality**: Friendly expert
- **Uncertainty**: Explicit signaling when data is limited

## Guardrails

The persona enforces several safety rules:

### Medical Advice
Responses discussing pain, injury, or physical conditions automatically include:
> *Note: This is general information only. For pain, injury concerns, or persistent physical issues, please consult with a qualified medical professional or physical therapist.*

### Swing Technique
Responses providing specific swing instruction automatically include:
> *Note: This is general guidance. For personalized swing instruction, consider working with a certified golf professional.*

### Betting
Responses discussing betting or gambling automatically include:
> *Note: CaddyPro does not provide betting or gambling advice. Please bet responsibly and within your means.*

### Guarantees
Responses making absolute claims automatically include:
> *Note: Results may vary. These are suggestions based on patterns, not guaranteed outcomes.*

## Pattern References

When relevant miss patterns exist, they are automatically formatted and appended:

```
**Based on your recent patterns:**
- push (frequently with driver under pressure, 85% confidence)
- slice (occasionally with 7-iron, 72% confidence)
```

Pattern references:
- Show top 3 patterns by confidence
- Filter out low-confidence patterns (<60%)
- Include club information when available
- Include pressure context when present
- Show natural frequency descriptions (frequently/occasionally/sometimes)
- Display confidence percentages for transparency

## Voice Polishing

The formatter automatically polishes responses to sound more natural:

| Formal | Natural |
|--------|---------|
| utilize | use |
| approximately | about |
| in order to | to |
| it is recommended that you | I'd recommend |
| you should consider | consider |
| it would be beneficial to | it'll help to |

## Integration Example

Full pipeline integration:

```kotlin
@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val llmClient: LLMClient,
    private val missPatternStore: MissPatternStore,
    private val formatter: BonesResponseFormatter
) : ViewModel() {

    suspend fun processUserInput(input: String) {
        // Get system prompt
        val systemPrompt = BonesPersona.getSystemPrompt()

        // Get relevant patterns
        val patterns = missPatternStore.getPatterns()

        // Call LLM
        val llmResponse = llmClient.generateResponse(
            systemPrompt = systemPrompt,
            userMessage = input,
            context = sessionContext
        )

        // Format response with guardrails and patterns
        val result = formatter.format(
            rawResponse = llmResponse,
            relevantPatterns = patterns,
            includePatternReferences = true
        )

        // Display formatted response
        _messages.emit(Message.Assistant(result.formattedResponse))
    }
}
```

## Testing

All components have comprehensive unit tests:

```bash
# Run all persona tests
./gradlew testDevDebugUnitTest --tests "caddypro.domain.navcaddy.persona.*"

# Run specific component tests
./gradlew testDevDebugUnitTest --tests "caddypro.domain.navcaddy.persona.BonesPersonaTest"
./gradlew testDevDebugUnitTest --tests "caddypro.domain.navcaddy.persona.PersonaGuardrailsTest"
./gradlew testDevDebugUnitTest --tests "caddypro.domain.navcaddy.persona.BonesResponseFormatterTest"
```

## Advanced Usage

### Manual Guardrail Checking

```kotlin
// Check a response manually
val result = PersonaGuardrails.checkResponse(response)

if (result.needsDisclaimer) {
    val disclaimer = PersonaGuardrails.getDisclaimer(result.disclaimerType!!)
    // Add disclaimer manually
}
```

### Confidence Qualifiers

```kotlin
// Add qualifier for uncertain responses
val qualifier = formatter.formatConfidenceQualifier(confidence = 0.6)

if (qualifier != null) {
    val qualifiedResponse = qualifier + response
}
```

### Voice Polishing

```kotlin
// Polish a response manually
val polished = formatter.applyVoicePolish(response)

// Remove generic language
val cleaned = formatter.removeGenericLanguage(response)
```

## Spec Reference

- **Spec**: specs/navcaddy-engine.md R4 (Bones Persona Layer)
- **Plan**: specs/navcaddy-engine-plan.md Task 16
- **Persona Rules**: specs/navcaddy-engine.md Q6 (Friendly Expert)

## Dependencies

- `caddypro.domain.navcaddy.models.MissPattern`
- `caddypro.domain.navcaddy.models.MissDirection`
- `caddypro.domain.navcaddy.models.Club`
- `caddypro.domain.navcaddy.models.PressureContext`
- `javax.inject.Inject`

## Architecture

```
persona/
├── BonesPersona.kt                 # System prompt and characteristics
├── PersonaGuardrails.kt            # Pattern detection and disclaimers
├── BonesResponseFormatter.kt       # Response post-processing
└── README.md                       # This file
```

## Support

For questions or issues with the persona layer, refer to:
- Implementation summary: `/TASK16_IMPLEMENTATION_SUMMARY.md`
- File manifest: `/TASK16_FILE_MANIFEST.txt`
- Spec: `/specs/navcaddy-engine.md`
