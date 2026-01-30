import SwiftUI

/**
 * Message view components for displaying different message types.
 *
 * Spec reference: navcaddy-engine.md R1, R4, R7
 * Plan reference: navcaddy-engine-plan.md Task 19
 */

// MARK: - Message Item Router

/// Routes to the appropriate message view based on message type.
struct MessageView: View {
    let message: ConversationMessage
    let onAction: (ConversationAction) -> Void

    var body: some View {
        switch message {
        case .user(let userMessage):
            UserMessageView(message: userMessage)
        case .assistant(let assistantMessage):
            AssistantMessageView(message: assistantMessage)
        case .clarification(let clarificationMessage):
            ClarificationView(
                message: clarificationMessage,
                onSuggestionClick: { suggestion in
                    onAction(.selectSuggestion(
                        intentType: suggestion.intentType,
                        label: suggestion.label
                    ))
                }
            )
        case .error(let errorMessage):
            ErrorMessageView(
                message: errorMessage,
                onRetry: errorMessage.isRecoverable ? {
                    onAction(.retry)
                } : nil
            )
        }
    }
}

// MARK: - User Message View

/// User message bubble (right-aligned, primary color).
struct UserMessageView: View {
    let message: UserMessage

    var body: some View {
        HStack {
            Spacer()
            Text(message.text)
                .font(.body)
                .foregroundStyle(.white)
                .padding(12)
                .background(Color.accentColor)
                .clipShape(
                    .rect(
                        topLeadingRadius: 16,
                        bottomLeadingRadius: 16,
                        bottomTrailingRadius: 4,
                        topTrailingRadius: 16
                    )
                )
                .frame(maxWidth: 280, alignment: .trailing)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("You said: \(message.text)")
    }
}

// MARK: - Assistant Message View

/// Assistant message bubble (left-aligned, with Bones branding).
struct AssistantMessageView: View {
    let message: AssistantMessage

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                // Bones branding label
                Text("Bones")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundStyle(.secondary)
                    .padding(.leading, 4)

                // Message content
                Text(message.text)
                    .font(.body)
                    .foregroundStyle(.primary)
                    .padding(12)
                    .background(Color(.secondarySystemBackground))
                    .clipShape(
                        .rect(
                            topLeadingRadius: 4,
                            bottomLeadingRadius: 16,
                            bottomTrailingRadius: 16,
                            topTrailingRadius: 16
                        )
                    )

                // Pattern references indicator
                if message.patternReferencesCount > 0 {
                    Text("Based on \(message.patternReferencesCount) pattern\(message.patternReferencesCount > 1 ? "s" : "")")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .padding(.leading, 4)
                }
            }
            .frame(maxWidth: 280, alignment: .leading)

            Spacer()
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Bones says: \(message.text)")
    }
}

// MARK: - Clarification View

/// Clarification message with suggestion chips.
struct ClarificationView: View {
    let message: ClarificationMessage
    let onSuggestionClick: (ClarificationSuggestion) -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                // Bones branding label
                Text("Bones")
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundStyle(.secondary)
                    .padding(.leading, 4)

                // Clarification card
                VStack(alignment: .leading, spacing: 12) {
                    // Icon and message
                    HStack(alignment: .top, spacing: 8) {
                        Image(systemName: "info.circle.fill")
                            .foregroundStyle(.blue)
                            .accessibilityHidden(true)

                        Text(message.message)
                            .font(.body)
                            .foregroundStyle(.primary)
                    }

                    // Suggestion chips
                    SuggestionChipsView(
                        suggestions: message.suggestions,
                        onSuggestionClick: onSuggestionClick
                    )
                }
                .padding(12)
                .background(Color(.tertiarySystemBackground))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .frame(maxWidth: 320, alignment: .leading)

            Spacer()
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Clarification: \(message.message)")
    }
}

// MARK: - Error Message View

/// Error message view with optional retry button.
struct ErrorMessageView: View {
    let message: ErrorMessage
    let onRetry: (() -> Void)?

    var body: some View {
        HStack {
            HStack(alignment: .top, spacing: 8) {
                Image(systemName: "exclamationmark.triangle.fill")
                    .foregroundStyle(.red)
                    .accessibilityHidden(true)

                VStack(alignment: .leading, spacing: 4) {
                    Text(message.message)
                        .font(.body)
                        .foregroundStyle(.primary)

                    if message.isRecoverable, onRetry != nil {
                        Button("Tap to retry") {
                            onRetry?()
                        }
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundStyle(.red)
                    }
                }
            }
            .padding(12)
            .background(Color.red.opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .frame(maxWidth: 280, alignment: .leading)

            Spacer()
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("Error: \(message.message)")
    }
}

// MARK: - Previews

#Preview("User Message") {
    MessageView(
        message: .user(UserMessage(text: "What club should I use for 150 yards?")),
        onAction: { _ in }
    )
    .padding()
    .previewLayout(.sizeThatFits)
}

#Preview("Assistant Message") {
    MessageView(
        message: .assistant(AssistantMessage(
            text: "For 150 yards, I'd recommend your 7-iron. The conditions look good for it.",
            patternReferencesCount: 2
        )),
        onAction: { _ in }
    )
    .padding()
    .previewLayout(.sizeThatFits)
}

#Preview("Clarification Message") {
    MessageView(
        message: .clarification(ClarificationMessage(
            message: "What would you like help with?",
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
                )
            ]
        )),
        onAction: { _ in }
    )
    .padding()
    .previewLayout(.sizeThatFits)
}

#Preview("Error Message") {
    MessageView(
        message: .error(ErrorMessage(
            message: "Network error. Please check your connection.",
            isRecoverable: true
        )),
        onAction: { _ in }
    )
    .padding()
    .previewLayout(.sizeThatFits)
}
