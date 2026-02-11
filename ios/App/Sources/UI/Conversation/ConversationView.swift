import SwiftUI

/**
 * Main conversation screen view.
 *
 * Features:
 * - ScrollView with message list
 * - Auto-scroll to bottom on new messages
 * - ConversationInputBar at bottom
 * - Loading indicator during processing
 * - Empty state when no messages
 * - Menu for clearing conversation
 *
 * Spec reference: navcaddy-engine.md R1, R7
 * Plan reference: navcaddy-engine-plan.md Task 19
 */
struct ConversationView: View {
    @State private var viewModel: ConversationViewModel

    init(dependencies: DependencyContainer = .shared) {
        _viewModel = State(initialValue: ConversationViewModel(dependencies: dependencies))
    }

    var body: some View {
        ConversationContent(
            state: viewModel.state,
            onAction: viewModel.handle
        )
    }
}

/// Stateless content view for conversation screen.
private struct ConversationContent: View {
    let state: ConversationState
    let onAction: (ConversationAction) -> Void

    var body: some View {
        // Message list with keyboard-aware input bar
        ScrollViewReader { scrollProxy in
            ScrollView {
                if state.messages.isEmpty {
                    // Empty state
                    EmptyConversationState()
                } else {
                    // Message list
                    LazyVStack(spacing: 16) {
                        ForEach(state.messages) { message in
                            MessageView(
                                message: message,
                                onAction: onAction
                            )
                            .id(message.id)
                        }

                        // Loading indicator at bottom
                        if state.isLoading {
                            HStack {
                                Spacer()
                                ProgressView()
                                    .progressViewStyle(.circular)
                                    .padding(.vertical, 8)
                                Spacer()
                            }
                            .id("loading")
                        }
                    }
                    .padding(16)
                }
            }
            .scrollDismissesKeyboard(.interactively)
            .onChange(of: state.messages.count) { _, _ in
                // Auto-scroll to bottom when new messages arrive
                withAnimation {
                    if let lastMessage = state.messages.last {
                        scrollProxy.scrollTo(lastMessage.id, anchor: .bottom)
                    } else if state.isLoading {
                        scrollProxy.scrollTo("loading", anchor: .bottom)
                    }
                }
            }
            .onChange(of: state.isLoading) { _, isLoading in
                // Scroll to loading indicator when it appears
                if isLoading {
                    withAnimation {
                        scrollProxy.scrollTo("loading", anchor: .bottom)
                    }
                }
            }
        }
        // Input bar at bottom with keyboard avoidance
        .safeAreaInset(edge: .bottom, spacing: 0) {
            ConversationInputBar(
                value: state.currentInput,
                onValueChange: { onAction(.updateInput($0)) },
                onSend: { onAction(.sendMessage(state.currentInput)) },
                onMicClick: {
                    if state.isVoiceInputActive {
                        onAction(.stopVoiceInput)
                    } else {
                        onAction(.startVoiceInput)
                    }
                },
                isVoiceActive: state.isVoiceInputActive,
                enabled: !state.isLoading
            )
        }
        .navigationTitle("Bones")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button("Clear conversation", role: .destructive) {
                        onAction(.clearConversation)
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .accessibilityLabel("More options")
                }
            }
        }
    }
}

/// Empty state when no messages are present.
private struct EmptyConversationState: View {
    var body: some View {
        VStack(spacing: 16) {
            Spacer()

            Image(systemName: "bubble.left.and.bubble.right")
                .font(.system(size: 60))
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)

            Text("Start a conversation with Bones")
                .font(.title2)
                .fontWeight(.semibold)
                .multilineTextAlignment(.center)
                .foregroundStyle(.primary)

            Text("Ask about club selection, check your recovery, enter scores, or get coaching tips.")
                .font(.body)
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
                .padding(.horizontal, 32)

            Spacer()
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(32)
        .accessibilityElement(children: .combine)
        .accessibilityLabel("Start a conversation with Bones. Ask about club selection, check your recovery, enter scores, or get coaching tips.")
    }
}

// MARK: - Previews

#Preview("Empty State") {
    NavigationStack {
        ConversationContent(
            state: ConversationState(),
            onAction: { _ in }
        )
    }
}

#Preview("With Messages") {
    NavigationStack {
        ConversationContent(
            state: ConversationState(
                messages: [
                    .assistant(AssistantMessage(
                        text: "Hi! I'm Bones, your digital caddy. How can I help?"
                    )),
                    .user(UserMessage(
                        text: "What club should I use for 150 yards?"
                    )),
                    .assistant(AssistantMessage(
                        text: "For 150 yards, I'd recommend your 7-iron. The conditions look good for it.",
                        patternReferencesCount: 1
                    ))
                ]
            ),
            onAction: { _ in }
        )
    }
}

#Preview("Loading State") {
    NavigationStack {
        ConversationContent(
            state: ConversationState(
                messages: [
                    .assistant(AssistantMessage(
                        text: "Hi! I'm Bones, your digital caddy. How can I help?"
                    )),
                    .user(UserMessage(
                        text: "What club should I use for 150 yards?"
                    ))
                ],
                isLoading: true
            ),
            onAction: { _ in }
        )
    }
}

#Preview("Clarification") {
    NavigationStack {
        ConversationContent(
            state: ConversationState(
                messages: [
                    .assistant(AssistantMessage(
                        text: "Hi! I'm Bones, your digital caddy. How can I help?"
                    )),
                    .user(UserMessage(
                        text: "It feels off today"
                    )),
                    .clarification(ClarificationMessage(
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
                            ),
                            ClarificationSuggestion(
                                intentType: .patternQuery,
                                label: "View Patterns",
                                description: "Check your miss patterns"
                            )
                        ]
                    ))
                ]
            ),
            onAction: { _ in }
        )
    }
}

#Preview("Error State") {
    NavigationStack {
        ConversationContent(
            state: ConversationState(
                messages: [
                    .assistant(AssistantMessage(
                        text: "Hi! I'm Bones, your digital caddy. How can I help?"
                    )),
                    .user(UserMessage(
                        text: "What club should I use?"
                    )),
                    .error(ErrorMessage(
                        message: "Network error. Please check your connection and try again.",
                        isRecoverable: true
                    ))
                ]
            ),
            onAction: { _ in }
        )
    }
}
