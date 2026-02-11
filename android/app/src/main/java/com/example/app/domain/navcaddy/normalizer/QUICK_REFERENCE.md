# Input Normalizer - Quick Reference

## One-Line Summary
Preprocesses user input by expanding golf slang, normalizing numbers, and filtering profanity before intent classification.

## Basic Usage

```kotlin
val normalizer = InputNormalizer()
val result = normalizer.normalize("My 7i feels long")
// result.normalizedInput = "My 7-iron feels long"
```

## Common Transformations

| Input | Output |
|-------|--------|
| `My 7i feels long` | `My 7-iron feels long` |
| `Use pw or sw` | `Use pitching wedge or sand wedge` |
| `one fifty to the green` | `150 to the green` |
| `seven iron` | `7-iron` |
| `damn shot` | `**** shot` |
| `My 3w or 5w?` | `My 3-wood or 5-wood?` |

## Golf Slang Dictionary

### Clubs
- **Irons**: `7i` → `7-iron`
- **Wedges**: `pw` → `pitching wedge`, `sw` → `sand wedge`, `lw` → `lob wedge`
- **Woods**: `3w` → `3-wood`, `5w` → `5-wood`
- **Driver**: `d` → `driver`
- **Hybrids**: `3h` → `3-hybrid`

### Terms
- `stick` → `club`
- `dance floor` → `green`
- `big stick` → `driver`

### Numbers
- `one fifty` → `150`
- `seven iron` → `7-iron`
- `twenty five` → `25`
- `fifty` → `50`

## Integration

```kotlin
class IntentClassifier(
    private val normalizer: InputNormalizer = InputNormalizer()
) {
    suspend fun classify(input: String): ClassificationResult {
        val normalized = normalizer.normalize(input).normalizedInput
        // Use normalized input for classification
    }
}
```

## Testing

```bash
./gradlew testDevDebugUnitTest --tests "caddypro.domain.navcaddy.normalizer.*"
```

## Key Files

- `InputNormalizer.kt` - Main implementation (229 lines)
- `GolfSlangDictionary.kt` - All mappings (183 lines)
- `NormalizationResult.kt` - Result data class
- `InputNormalizerTest.kt` - 60+ tests (416 lines)

## Performance

- **Latency**: < 1ms for typical input
- **No external dependencies**
- **Thread-safe**: Stateless, reusable

## See Also

- `README.md` - Full documentation
- `InputNormalizerTest.kt` - Usage examples
- `specs/navcaddy-engine.md` R2 - Spec reference
