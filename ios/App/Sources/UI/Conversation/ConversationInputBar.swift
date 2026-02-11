import SwiftUI

/**
 * Input bar for the conversation screen.
 *
 * Features:
 * - TextField for text input
 * - Mic button (toggles to Send when text present)
 * - Animated state transitions
 * - Keyboard handling
 *
 * Spec reference: navcaddy-engine.md R1, R7
 * Plan reference: navcaddy-engine-plan.md Task 19
 */
struct ConversationInputBar: View {
    let value: String
    let onValueChange: (String) -> Void
    let onSend: () -> Void
    let onMicClick: () -> Void
    let isVoiceActive: Bool
    let enabled: Bool

    var body: some View {
        VStack(spacing: 0) {
            Divider()

            HStack(alignment: .bottom, spacing: 8) {
                // Text input field
                TextField(
                    isVoiceActive ? "Listening..." : "Ask Bones anything...",
                    text: Binding(
                        get: { value },
                        set: { onValueChange($0) }
                    ),
                    axis: .vertical
                )
                .textFieldStyle(.roundedBorder)
                .lineLimit(1...4)
                .disabled(!enabled || isVoiceActive)
                .submitLabel(.send)
                .onSubmit {
                    if !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        onSend()
                    }
                }
                .accessibilityLabel("Message input")
                .accessibilityHint("Enter your question for Bones")

                // Mic/Send button with animated transition
                ZStack {
                    if !value.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                        // Send button
                        Button(action: onSend) {
                            Image(systemName: "arrow.up.circle.fill")
                                .font(.system(size: 32))
                                .foregroundStyle(.white)
                                .background(
                                    Circle()
                                        .fill(Color.accentColor)
                                        .frame(width: 32, height: 32)
                                )
                        }
                        .disabled(!enabled)
                        .accessibilityLabel("Send message")
                        .transition(.scale.combined(with: .opacity))
                    } else {
                        // Mic button
                        Button(action: onMicClick) {
                            Image(systemName: "mic.fill")
                                .font(.system(size: 20))
                                .foregroundStyle(isVoiceActive ? .white : .primary)
                                .frame(width: 44, height: 44)
                                .background(
                                    Circle()
                                        .fill(isVoiceActive ? Color.red : Color(.tertiarySystemBackground))
                                )
                        }
                        .disabled(!enabled)
                        .accessibilityLabel(isVoiceActive ? "Stop voice input" : "Start voice input")
                        .transition(.scale.combined(with: .opacity))
                    }
                }
                .frame(width: 44, height: 44)
                .animation(.spring(response: 0.3, dampingFraction: 0.7), value: value.isEmpty)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
        }
        .background(Color(.systemBackground))
        .shadow(color: .black.opacity(0.05), radius: 8, y: -2)
    }
}

// MARK: - Previews

#Preview("Empty Input") {
    ConversationInputBar(
        value: "",
        onValueChange: { _ in },
        onSend: {},
        onMicClick: {},
        isVoiceActive: false,
        enabled: true
    )
    .previewLayout(.sizeThatFits)
}

#Preview("With Text") {
    ConversationInputBar(
        value: "What club should I use?",
        onValueChange: { _ in },
        onSend: {},
        onMicClick: {},
        isVoiceActive: false,
        enabled: true
    )
    .previewLayout(.sizeThatFits)
}

#Preview("Voice Active") {
    ConversationInputBar(
        value: "",
        onValueChange: { _ in },
        onSend: {},
        onMicClick: {},
        isVoiceActive: true,
        enabled: true
    )
    .previewLayout(.sizeThatFits)
}

#Preview("Disabled") {
    ConversationInputBar(
        value: "",
        onValueChange: { _ in },
        onSend: {},
        onMicClick: {},
        isVoiceActive: false,
        enabled: false
    )
    .previewLayout(.sizeThatFits)
}
