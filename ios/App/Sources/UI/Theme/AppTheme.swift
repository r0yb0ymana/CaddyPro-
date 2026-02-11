import SwiftUI

/// App-wide theme configuration
enum AppTheme {
    // MARK: - Colors
    enum Colors {
        static let primary = Color.accentColor
        static let secondary = Color.secondary
        static let background = Color(.systemBackground)
        static let surface = Color(.secondarySystemBackground)
        static let error = Color.red
        static let success = Color.green
        static let warning = Color.orange
    }
    
    // MARK: - Spacing
    enum Spacing {
        static let xxs: CGFloat = 4
        static let xs: CGFloat = 8
        static let sm: CGFloat = 12
        static let md: CGFloat = 16
        static let lg: CGFloat = 24
        static let xl: CGFloat = 32
        static let xxl: CGFloat = 48
    }
    
    // MARK: - Corner Radius
    enum CornerRadius {
        static let small: CGFloat = 8
        static let medium: CGFloat = 12
        static let large: CGFloat = 16
        static let full: CGFloat = 9999
    }
    
    // MARK: - Shadows
    enum Shadows {
        static let small = ShadowStyle(color: .black.opacity(0.1), radius: 4, x: 0, y: 2)
        static let medium = ShadowStyle(color: .black.opacity(0.15), radius: 8, x: 0, y: 4)
        static let large = ShadowStyle(color: .black.opacity(0.2), radius: 16, x: 0, y: 8)
    }
}

// MARK: - Shadow Style
struct ShadowStyle {
    let color: Color
    let radius: CGFloat
    let x: CGFloat
    let y: CGFloat
}

// MARK: - View Extensions
extension View {
    func cardStyle() -> some View {
        self
            .padding(AppTheme.Spacing.md)
            .background(AppTheme.Colors.surface)
            .clipShape(RoundedRectangle(cornerRadius: AppTheme.CornerRadius.medium))
    }
    
    func primaryButtonStyle() -> some View {
        self
            .font(.headline)
            .foregroundStyle(.white)
            .padding(.horizontal, AppTheme.Spacing.lg)
            .padding(.vertical, AppTheme.Spacing.sm)
            .background(AppTheme.Colors.primary)
            .clipShape(RoundedRectangle(cornerRadius: AppTheme.CornerRadius.medium))
    }
}
