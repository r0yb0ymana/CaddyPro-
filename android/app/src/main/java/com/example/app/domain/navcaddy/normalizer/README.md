# Input Normalizer

## Overview

The Input Normalizer preprocesses user input before intent classification. It handles:

1. **Profanity filtering** - Replaces offensive words with asterisks
2. **Golf slang expansion** - Expands abbreviations (7i → 7-iron, PW → pitching wedge)
3. **Number normalization** - Converts spoken numbers to digits ("one fifty" → "150")
4. **Language detection** - Simple English detection (MVP)
5. **Whitespace cleanup** - Removes extra spaces and trims

## Spec Reference

- **Spec**: `specs/navcaddy-engine.md` R2
- **Plan**: `specs/navcaddy-engine-plan.md` Task 7
- **Acceptance Criteria**: A1, A2

## Architecture

```
InputNormalizer
├── normalize(input: String): NormalizationResult
│   ├── isEnglish() - Language detection
│   ├── filterProfanity() - Remove offensive words
│   ├── normalizeCompoundNumbers() - "one fifty" → "150"
│   ├── expandGolfSlang() - "7i" → "7-iron"
│   ├── normalizeNumberWords() - "fifty" → "50"
│   └── cleanWhitespace() - Cleanup
│
GolfSlangDictionary (object)
├── clubAbbreviations - 7i, PW, SW, etc.
├── commonTerms - "stick" → "club", etc.
├── numberWords - "one" → "1", "fifty" → "50"
└── compoundNumbers - "one fifty" → "150"
```

## Usage

### Basic Usage

```kotlin
val normalizer = InputNormalizer()
val result = normalizer.normalize("My 7i feels long today")

println(result.normalizedInput)  // "My 7-iron feels long today"
println(result.wasModified)      // true
println(result.modifications)    // [Modification(SLANG, "7i", "7-iron")]
```

### Integration with IntentClassifier

```kotlin
class IntentClassifier(
    private val llmClient: LLMClient,
    private val normalizer: InputNormalizer = InputNormalizer()
) {
    suspend fun classify(input: String, context: SessionContext? = null): ClassificationResult {
        // Normalize input before classification
        val normalizationResult = normalizer.normalize(input)
        val normalizedInput = normalizationResult.normalizedInput

        // Use normalized input for LLM classification
        val llmResponse = llmClient.classify(normalizedInput, context)
        // ...
    }
}
```

## Examples

### Golf Slang Expansion

```kotlin
normalizer.normalize("My 7i feels long")
// → "My 7-iron feels long"

normalizer.normalize("Use PW or SW")
// → "Use pitching wedge or sand wedge"

normalizer.normalize("Hit the 3w")
// → "Hit the 3-wood"

normalizer.normalize("Try my big stick")
// → "Try my driver"
```

### Number Normalization

```kotlin
normalizer.normalize("I have one fifty to the green")
// → "I have 150 to the green"

normalizer.normalize("Use the seven iron")
// → "Use the 7-iron"

normalizer.normalize("twenty five yards")
// → "25 yards"

normalizer.normalize("one hundred seventy")
// → "170"
```

### Profanity Filtering

```kotlin
normalizer.normalize("This shot is shit")
// → "This **** is ****"

normalizer.normalize("Damn that was bad")
// → "**** that was bad"

// Preserves intent while filtering
normalizer.normalize("I keep slicing this damn club")
// → "I keep slicing this **** club"
```

### Complex Input

```kotlin
normalizer.normalize("My 7i is one fifty to the damn green")
// → "My 7-iron is 150 to the **** green"

normalizer.normalize("pw from the dance floor")
// → "pitching wedge from the green"

normalizer.normalize("Should I use 3w or 5w for this one seventy shot")
// → "Should I use 3-wood or 5-wood for this 170 shot"
```

## Golf Slang Dictionary

### Club Abbreviations

| Abbreviation | Expansion |
|--------------|-----------|
| 3i, 4i, 5i, 6i, 7i, 8i, 9i | 3-iron, 4-iron, etc. |
| PW | pitching wedge |
| GW | gap wedge |
| AW | approach wedge |
| SW | sand wedge |
| LW | lob wedge |
| 3w, 5w, 7w | 3-wood, 5-wood, 7-wood |
| D | driver |
| 2h, 3h, 4h, 5h | 2-hybrid, 3-hybrid, etc. |

### Common Terms

| Slang | Expansion |
|-------|-----------|
| stick, sticks | club, clubs |
| dance floor, the dance floor | green |
| big stick, big dog | driver |
| flat stick | putter |
| tin cup | hole |
| fairway metal | fairway wood |

### Number Words

| Word | Digit |
|------|-------|
| one, two, three, ... | 1, 2, 3, ... |
| ten, eleven, ... nineteen | 10, 11, ... 19 |
| twenty, thirty, ... ninety | 20, 30, ... 90 |
| hundred | 100 |

### Compound Numbers

| Phrase | Number |
|--------|--------|
| one fifty, one sixty, one seventy, one eighty | 150, 160, 170, 180 |
| one hundred fifty | 150 |
| twenty one, twenty two, ... | 21, 22, ... |
| seven iron, eight iron | 7-iron, 8-iron |
| three wood, five wood | 3-wood, 5-wood |

## NormalizationResult

```kotlin
data class NormalizationResult(
    val normalizedInput: String,      // Processed text
    val originalInput: String,         // Original text
    val wasModified: Boolean,          // True if any changes made
    val modifications: List<Modification>  // All modifications applied
)
```

## Modification Types

```kotlin
enum class ModificationType {
    SLANG,      // Golf slang expanded
    NUMBER,     // Number normalized
    PROFANITY,  // Profanity filtered
    OTHER       // Other normalization
}

data class Modification(
    val type: ModificationType,
    val original: String,
    val replacement: String
)
```

## Edge Cases

### Already Normalized Input
```kotlin
normalizer.normalize("What club should I use for 150 yards?")
// → No modifications, returns unchanged
```

### Empty/Blank Input
```kotlin
normalizer.normalize("")
// → Returns unchanged with wasModified = false

normalizer.normalize("   ")
// → Returns empty string after trim
```

### Mixed Input
```kotlin
normalizer.normalize("  My  7i   feels   long  ")
// → "My 7-iron feels long" (slang + whitespace cleanup)
```

### Special Characters
```kotlin
normalizer.normalize("My 7i? PW! What club...")
// → "My 7-iron? pitching wedge! What club..."
```

### Partial Words (Not Filtered)
```kotlin
normalizer.normalize("I need assistance")
// → "I need assistance" (ass in assistance not filtered)
```

## Testing

Comprehensive test coverage includes:

- **Golf slang expansion** - All club types and common terms
- **Number normalization** - Spoken to digits, compound numbers
- **Profanity filtering** - Whole word matching, intent preservation
- **Edge cases** - Empty input, already normalized, mixed modifications
- **Integration** - Realistic user input scenarios

Run tests:
```bash
./gradlew testDevDebugUnitTest --tests "caddypro.domain.navcaddy.normalizer.*"
```

## Performance

The normalizer uses regex-based pattern matching with:

- **Word boundary matching** - Prevents false positives
- **Ordered processing** - Compound patterns before single words
- **Case-insensitive** - Handles user input variations
- **Single pass** - Each normalization step runs once

Typical input (10-50 words) processes in < 1ms.

## Language Support

Currently English only. Non-English input:
- Proceeds through pipeline (may fail at classification)
- Future: Return error or translate

## Privacy

- **No logging** - User input not logged
- **Profanity filtering** - Removes offensive content
- **No storage** - Normalization is stateless

## Future Enhancements

1. **Multi-language support** - Spanish, etc.
2. **Custom dictionaries** - User-defined slang
3. **Regional variations** - UK vs US terminology
4. **Machine learning** - Learn user patterns
5. **Context-aware** - Use session context for normalization

## Files

- `InputNormalizer.kt` - Main normalizer implementation
- `GolfSlangDictionary.kt` - Golf terminology mappings
- `NormalizationResult.kt` - Result data class
- `Modification.kt` - Modification data class
- `ModificationType.kt` - Modification type enum
- `InputNormalizerTest.kt` - Comprehensive unit tests
- `GolfSlangDictionaryTest.kt` - Dictionary validation tests

## Dependencies

- **Kotlin stdlib** - Regex, collections, string operations
- **JUnit** - Unit testing
- No external dependencies required

## Maintenance

### Adding New Golf Slang

1. Add to `GolfSlangDictionary.kt`
2. Add test case to verify expansion
3. Document in this README

### Adding New Number Patterns

1. Add to `compoundNumbers` list (order by specificity)
2. Add test case to verify conversion
3. Consider conflicts with existing patterns

### Updating Profanity List

1. Update `profanityWords` set in `InputNormalizer.kt`
2. Keep minimal to avoid false positives
3. Use whole-word matching only
