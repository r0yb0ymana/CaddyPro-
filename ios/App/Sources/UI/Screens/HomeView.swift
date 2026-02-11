import SwiftUI

struct HomeView: View {
    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "figure.golf")
                .font(.system(size: 80))
                .foregroundStyle(.green)

            Text("CaddyPro+")
                .font(.largeTitle)
                .fontWeight(.bold)

            Text("Your AI Golf Caddy")
                .font(.title3)
                .foregroundStyle(.secondary)

            Spacer()

            NavigationLink(destination: ConversationView()) {
                Label("Talk to Bones", systemImage: "message.fill")
                    .font(.headline)
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(.green)
                    .foregroundStyle(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
        .navigationTitle("Home")
    }
}

#Preview {
    NavigationStack {
        HomeView()
    }
}
