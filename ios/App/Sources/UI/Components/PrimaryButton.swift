import SwiftUI

/// A styled primary button
struct PrimaryButton: View {
    let title: String
    let isLoading: Bool
    let action: () -> Void
    
    init(_ title: String, isLoading: Bool = false, action: @escaping () -> Void) {
        self.title = title
        self.isLoading = isLoading
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: AppTheme.Spacing.xs) {
                if isLoading {
                    ProgressView()
                        .tint(.white)
                }
                Text(title)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, AppTheme.Spacing.sm)
        }
        .buttonStyle(.borderedProminent)
        .disabled(isLoading)
    }
}

#Preview {
    VStack(spacing: 16) {
        PrimaryButton("Submit", action: {})
        PrimaryButton("Loading...", isLoading: true, action: {})
    }
    .padding()
}
