import Foundation

/// Demo script to verify InputNormalizer functionality.
/// This can be run in a playground or as a standalone script.
///
/// Usage: swift NormalizerDemo.swift
func runNormalizerDemo() {
    let normalizer = InputNormalizer()

    print("=== Input Normalizer Demo ===\n")

    let testCases = [
        "My 7i feels long today",
        "Hit the PW from one fifty",
        "How's my recovery looking?",
        "seven iron from the dance floor",
        "sw from the beach",
        "damn that 7i from one fifty missed",
        "three wood off the tee",
        "My 7i from one fifty to the green"
    ]

    for testCase in testCases {
        let result = normalizer.normalize(testCase)
        print("Original:   \(testCase)")
        print("Normalized: \(result.normalizedInput)")
        print("Modified:   \(result.wasModified)")
        if !result.modifications.isEmpty {
            print("Changes:")
            for mod in result.modifications {
                print("  - [\(mod.type)] \"\(mod.original)\" → \"\(mod.replacement)\"")
            }
        }
        print()
    }

    // Test language detection
    print("=== Language Detection ===\n")
    let languageTests = [
        "My 7-iron feels long",
        "Bonjour comment allez-vous",
        "Hola como estas"
    ]

    for test in languageTests {
        let isEnglish = normalizer.isEnglish(test)
        print("\"\(test)\" → English: \(isEnglish)")
    }
    print()

    print("=== Demo Complete ===")
}

// Uncomment to run demo
// runNormalizerDemo()
