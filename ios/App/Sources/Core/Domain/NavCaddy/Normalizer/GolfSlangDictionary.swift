import Foundation

/// Dictionary of golf slang terms and their normalized forms.
///
/// Spec R2: Input normalization - golf slang handling.
enum GolfSlangDictionary {

    // MARK: - Club Abbreviations

    /// Maps club abbreviations to full names.
    static let clubAbbreviations: [String: String] = [
        // Irons
        "1i": "1-iron",
        "2i": "2-iron",
        "3i": "3-iron",
        "4i": "4-iron",
        "5i": "5-iron",
        "6i": "6-iron",
        "7i": "7-iron",
        "8i": "8-iron",
        "9i": "9-iron",

        // Wedges
        "pw": "pitching wedge",
        "gw": "gap wedge",
        "aw": "approach wedge",
        "sw": "sand wedge",
        "lw": "lob wedge",

        // Woods
        "d": "driver",
        "dr": "driver",
        "1w": "driver",
        "2w": "2-wood",
        "3w": "3-wood",
        "4w": "4-wood",
        "5w": "5-wood",
        "7w": "7-wood",

        // Hybrids
        "3h": "3-hybrid",
        "4h": "4-hybrid",
        "5h": "5-hybrid",
        "6h": "6-hybrid",
        "7h": "7-hybrid"
    ]

    // MARK: - Common Golf Terms

    /// Maps slang terms to standard golf terminology.
    static let commonTerms: [String: String] = [
        "stick": "club",
        "sticks": "clubs",
        "the dance floor": "green",
        "dance floor": "green",
        "the short stuff": "fairway",
        "short stuff": "fairway",
        "the big stick": "driver",
        "big stick": "driver",
        "flatstick": "putter",
        "flat stick": "putter",
        "texas wedge": "putter",
        "worm burner": "low shot",
        "skull": "thin shot",
        "chunk": "fat shot",
        "duff": "fat shot",
        "shank": "toe shot",
        "banana ball": "slice",
        "duck hook": "severe hook",
        "snap hook": "severe hook",
        "pull": "pull shot",
        "push": "push shot",
        "fade": "controlled slice",
        "draw": "controlled hook",
        "dead straight": "straight",
        "pin high": "correct distance",
        "pin": "flagstick",
        "flag": "flagstick",
        "jar": "hole",
        "cup": "hole",
        "gimmie": "very short putt",
        "gimme": "very short putt",
        "birdie": "one under par",
        "eagle": "two under par",
        "albatross": "three under par",
        "bogey": "one over par",
        "double": "double bogey",
        "triple": "triple bogey",
        "snowman": "score of 8",
        "fried egg": "buried lie in sand",
        "beach": "sand bunker",
        "cabbage": "thick rough",
        "heavy rough": "thick rough",
        "up and down": "chip and one putt",
        "pitch and putt": "short course",
        "links": "seaside course",
        "track": "golf course",
        "layout": "golf course",
        "turn": "after 9 holes",
        "front nine": "holes 1-9",
        "back nine": "holes 10-18",
        "card": "scorecard",
        "tee it up": "start round",
        "first tee": "starting hole"
    ]

    // MARK: - Number Words

    /// Maps written numbers to digits.
    static let numberWords: [String: String] = [
        // Basic digits
        "zero": "0",
        "one": "1",
        "two": "2",
        "three": "3",
        "four": "4",
        "five": "5",
        "six": "6",
        "seven": "7",
        "eight": "8",
        "nine": "9",

        // Teens
        "ten": "10",
        "eleven": "11",
        "twelve": "12",
        "thirteen": "13",
        "fourteen": "14",
        "fifteen": "15",
        "sixteen": "16",
        "seventeen": "17",
        "eighteen": "18",
        "nineteen": "19",

        // Tens
        "twenty": "20",
        "thirty": "30",
        "forty": "40",
        "fifty": "50",
        "sixty": "60",
        "seventy": "70",
        "eighty": "80",
        "ninety": "90",

        // Hundreds
        "hundred": "100",
        "one hundred": "100",
        "two hundred": "200",
        "three hundred": "300"
    ]

    // MARK: - Composite Number Patterns

    /// Patterns for composite numbers like "one fifty" -> "150".
    static let compositePatterns: [(pattern: NSRegularExpression, transform: (String) -> String)] = [
        // Pattern: "one fifty" -> "150"
        (
            pattern: try! NSRegularExpression(pattern: #"\b(one|two|three) (ten|twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)\b"#, options: .caseInsensitive),
            transform: { match in
                let components = match.lowercased().split(separator: " ").map(String.init)
                guard components.count == 2 else { return match }

                let hundreds: [String: String] = ["one": "1", "two": "2", "three": "3"]
                let tens: [String: String] = ["ten": "10", "twenty": "20", "thirty": "30", "forty": "40", "fifty": "50", "sixty": "60", "seventy": "70", "eighty": "80", "ninety": "90"]

                guard let h = hundreds[components[0]], let t = tens[components[1]] else { return match }
                return h + t
            }
        ),

        // Pattern: "twenty three" -> "23"
        (
            pattern: try! NSRegularExpression(pattern: #"\b(twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)[\s-](one|two|three|four|five|six|seven|eight|nine)\b"#, options: .caseInsensitive),
            transform: { match in
                let normalized = match.lowercased().replacingOccurrences(of: "-", with: " ")
                let components = normalized.split(separator: " ").map(String.init)
                guard components.count == 2 else { return match }

                let tens: [String: String] = ["twenty": "2", "thirty": "3", "forty": "4", "fifty": "5", "sixty": "6", "seventy": "7", "eighty": "8", "ninety": "9"]
                let ones: [String: String] = ["one": "1", "two": "2", "three": "3", "four": "4", "five": "5", "six": "6", "seven": "7", "eight": "8", "nine": "9"]

                guard let t = tens[components[0]], let o = ones[components[1]] else { return match }
                return t + o
            }
        ),

        // Pattern: "seven iron" -> "7-iron"
        (
            pattern: try! NSRegularExpression(pattern: #"\b(one|two|three|four|five|six|seven|eight|nine)\s+iron\b"#, options: .caseInsensitive),
            transform: { match in
                let components = match.lowercased().split(separator: " ").map(String.init)
                guard components.count == 2 else { return match }

                let numbers: [String: String] = ["one": "1", "two": "2", "three": "3", "four": "4", "five": "5", "six": "6", "seven": "7", "eight": "8", "nine": "9"]

                guard let num = numbers[components[0]] else { return match }
                return num + "-iron"
            }
        ),

        // Pattern: "three wood" -> "3-wood"
        (
            pattern: try! NSRegularExpression(pattern: #"\b(one|two|three|four|five|six|seven)\s+wood\b"#, options: .caseInsensitive),
            transform: { match in
                let components = match.lowercased().split(separator: " ").map(String.init)
                guard components.count == 2 else { return match }

                let numbers: [String: String] = ["one": "1", "two": "2", "three": "3", "four": "4", "five": "5", "six": "6", "seven": "7"]

                guard let num = numbers[components[0]] else { return match }
                return num + "-wood"
            }
        ),

        // Pattern: "four hybrid" -> "4-hybrid"
        (
            pattern: try! NSRegularExpression(pattern: #"\b(one|two|three|four|five|six|seven)\s+hybrid\b"#, options: .caseInsensitive),
            transform: { match in
                let components = match.lowercased().split(separator: " ").map(String.init)
                guard components.count == 2 else { return match }

                let numbers: [String: String] = ["one": "1", "two": "2", "three": "3", "four": "4", "five": "5", "six": "6", "seven": "7"]

                guard let num = numbers[components[0]] else { return match }
                return num + "-hybrid"
            }
        )
    ]

    // MARK: - Profanity Filter

    /// Common profanity patterns (replace with asterisks or remove).
    /// Note: This is a minimal set for demonstration. A production app should use
    /// a comprehensive profanity filter service or library.
    static let profanityPatterns: [String] = [
        "damn",
        "hell",
        "crap",
        "shit",
        "fuck",
        "ass",
        "bitch",
        "bastard",
        "piss"
    ]

    /// Create regex pattern for profanity filtering.
    static func profanityRegex() -> NSRegularExpression? {
        let pattern = profanityPatterns
            .map { NSRegularExpression.escapedPattern(for: $0) }
            .joined(separator: "|")

        let fullPattern = #"\b(\#(pattern))\w*\b"#
        return try? NSRegularExpression(pattern: fullPattern, options: .caseInsensitive)
    }
}
