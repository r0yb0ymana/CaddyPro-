import SwiftUI

/// A reusable error view with retry action
struct ErrorView: View {
    let error: Error
    var retryAction: (() -> Void)?
    
    var body: some View {
        VStack(spacing: AppTheme.Spacing.md) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 48))
                .foregroundStyle(AppTheme.Colors.error)
            
            Text("Something went wrong")
                .font(.headline)
            
            Text(error.localizedDescription)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, AppTheme.Spacing.lg)
            
            if let retryAction {
                Button("Try Again", action: retryAction)
                    .primaryButtonStyle()
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppTheme.Colors.background)
    }
}

#Preview {
    ErrorView(
        error: NSError(domain: "test", code: -1, userInfo: [NSLocalizedDescriptionKey: "Failed to load data"]),
        retryAction: {}
    )
}
