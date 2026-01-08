import SwiftUI

struct AppTextField: View {
    let label: String
    @Binding var text: String
    var error: String?
    var keyboardType: UIKeyboardType = .default
    var isSecure: Bool = false
    
    var body: some View {
        VStack(alignment: .leading, spacing: AppTheme.Spacing.xxs) {
            if isSecure {
                SecureField(label, text: $text)
                    .textFieldStyle(.roundedBorder)
            } else {
                TextField(label, text: $text)
                    .keyboardType(keyboardType)
                    .textFieldStyle(.roundedBorder)
            }
            
            if let error {
                Text(error)
                    .font(.caption)
                    .foregroundStyle(AppTheme.Colors.error)
            }
        }
    }
}
