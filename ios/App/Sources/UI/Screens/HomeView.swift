import SwiftUI

struct HomeView: View {
    var body: some View {
        VStack(spacing: 16) {
            Text("Welcome to Your App")
                .font(.title)
                .fontWeight(.bold)
            
            Text("Start building your features!")
                .font(.body)
                .foregroundStyle(.secondary)
        }
        .navigationTitle("Home")
    }
}

#Preview {
    NavigationStack {
        HomeView()
    }
}
