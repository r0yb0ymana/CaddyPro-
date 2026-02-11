import SwiftUI

/**
 * Horizontal scrolling row of tappable suggestion chips.
 *
 * Used in clarification messages to present intent suggestions.
 * Chips wrap automatically if they exceed the view width.
 *
 * Spec reference: navcaddy-engine.md A3
 * Plan reference: navcaddy-engine-plan.md Task 19
 */
struct SuggestionChipsView: View {
    let suggestions: [ClarificationSuggestion]
    let onSuggestionClick: (ClarificationSuggestion) -> Void

    var body: some View {
        FlowLayout(spacing: 8) {
            ForEach(suggestions) { suggestion in
                SuggestionChip(
                    suggestion: suggestion,
                    onClick: { onSuggestionClick(suggestion) }
                )
            }
        }
    }
}

/**
 * Individual suggestion chip.
 */
private struct SuggestionChip: View {
    let suggestion: ClarificationSuggestion
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            Text(suggestion.label)
                .font(.subheadline)
                .fontWeight(.medium)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
        }
        .buttonStyle(.bordered)
        .controlSize(.small)
        .accessibilityLabel(suggestion.label)
        .accessibilityHint(suggestion.description)
    }
}

/**
 * Simple flow layout that wraps content to multiple rows.
 *
 * Similar to FlowRow in Compose - arranges items horizontally and wraps to new rows.
 */
private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let result = FlowResult(
            in: proposal.replacingUnspecifiedDimensions().width,
            subviews: subviews,
            spacing: spacing
        )
        return result.size
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let result = FlowResult(
            in: bounds.width,
            subviews: subviews,
            spacing: spacing
        )
        for (index, subview) in subviews.enumerated() {
            let position = result.positions[index]
            subview.place(
                at: CGPoint(x: bounds.minX + position.x, y: bounds.minY + position.y),
                proposal: .unspecified
            )
        }
    }

    struct FlowResult {
        var size: CGSize = .zero
        var positions: [CGPoint] = []

        init(in maxWidth: CGFloat, subviews: Subviews, spacing: CGFloat) {
            var currentX: CGFloat = 0
            var currentY: CGFloat = 0
            var lineHeight: CGFloat = 0

            for subview in subviews {
                let size = subview.sizeThatFits(.unspecified)

                if currentX + size.width > maxWidth && currentX > 0 {
                    // Move to next line
                    currentX = 0
                    currentY += lineHeight + spacing
                    lineHeight = 0
                }

                positions.append(CGPoint(x: currentX, y: currentY))
                currentX += size.width + spacing
                lineHeight = max(lineHeight, size.height)
            }

            self.size = CGSize(
                width: maxWidth,
                height: currentY + lineHeight
            )
        }
    }
}

// MARK: - Previews

#Preview("Suggestion Chips") {
    SuggestionChipsView(
        suggestions: [
            ClarificationSuggestion(
                intentType: .clubAdjustment,
                label: "Adjust Club",
                description: "Adjust club distances"
            ),
            ClarificationSuggestion(
                intentType: .recoveryCheck,
                label: "Check Recovery",
                description: "View recovery status"
            ),
            ClarificationSuggestion(
                intentType: .shotRecommendation,
                label: "Shot Advice",
                description: "Get shot recommendations"
            )
        ],
        onSuggestionClick: { _ in }
    )
    .padding()
    .previewLayout(.sizeThatFits)
}
