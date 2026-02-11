import SwiftUI

/**
 * Debug trace view for NavCaddy intent pipeline.
 *
 * Shows recent events with latency breakdown for QA/development builds.
 * Only available when DEBUG flag is set.
 *
 * Spec reference: navcaddy-engine.md R8
 * Plan reference: navcaddy-engine-plan.md Task 22
 */
#if DEBUG
struct IntentTraceView: View {
    // MARK: - Properties

    private let analytics: ConsoleAnalytics
    private let sessionId: String

    @State private var events: [AnalyticsEvent] = []
    @State private var latencyStats: [String: LatencyStats] = [:]

    // MARK: - Initialization

    init(analytics: ConsoleAnalytics, sessionId: String) {
        self.analytics = analytics
        self.sessionId = sessionId
    }

    // MARK: - Body

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    // Session Info
                    sessionInfoSection

                    // Latency Breakdown
                    if !latencyStats.isEmpty {
                        latencyBreakdownSection
                    }

                    // Recent Events
                    eventsSection
                }
                .padding()
            }
            .navigationTitle("Intent Trace")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Clear") {
                        analytics.clearEvents()
                        refreshData()
                    }
                }
            }
            .onAppear {
                refreshData()
            }
        }
    }

    // MARK: - Sections

    private var sessionInfoSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Session")
                .font(.headline)
            Text(sessionId)
                .font(.caption)
                .fontDesign(.monospaced)
                .foregroundStyle(.secondary)
            Text("\(events.count) events")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }

    private var latencyBreakdownSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Latency Breakdown")
                .font(.headline)

            ForEach(Array(latencyStats.keys.sorted()), id: \.self) { operation in
                if let stats = latencyStats[operation] {
                    LatencyStatsRow(operation: operation, stats: stats)
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }

    private var eventsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Recent Events")
                .font(.headline)

            if events.isEmpty {
                Text("No events yet")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding()
            } else {
                ForEach(Array(events.enumerated()), id: \.offset) { index, event in
                    EventRow(event: event, index: index)
                }
            }
        }
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }

    // MARK: - Helpers

    private func refreshData() {
        events = analytics.getEvents(for: sessionId)

        // Calculate latency stats
        latencyStats = [
            "voice_transcription": analytics.getLatencyStats(for: "voice_transcription"),
            "intent_classification": analytics.getLatencyStats(for: "intent_classification"),
            "routing": analytics.getLatencyStats(for: "routing"),
        ].compactMapValues { $0 }
    }
}

// MARK: - Supporting Views

private struct LatencyStatsRow: View {
    let operation: String
    let stats: LatencyStats

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(operation.replacingOccurrences(of: "_", with: " ").capitalized)
                .font(.subheadline)
                .fontWeight(.medium)

            HStack(spacing: 16) {
                StatItem(label: "Mean", value: "\(stats.mean)ms")
                StatItem(label: "P50", value: "\(stats.p50)ms")
                StatItem(label: "P95", value: "\(stats.p95)ms")
            }

            // Visual bar for P95 (max 5000ms scale)
            LatencyBar(latency: stats.p95, max: 5000)
        }
        .padding(.vertical, 4)
    }
}

private struct StatItem: View {
    let label: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.caption)
                .fontDesign(.monospaced)
        }
    }
}

private struct LatencyBar: View {
    let latency: Int
    let max: Int

    private var percentage: Double {
        min(Double(latency) / Double(max), 1.0)
    }

    private var color: Color {
        switch latency {
        case 0..<1500:
            return .green
        case 1500..<3000:
            return .yellow
        default:
            return .red
        }
    }

    var body: some View {
        GeometryReader { geometry in
            ZStack(alignment: .leading) {
                Rectangle()
                    .fill(Color(.systemGray5))

                Rectangle()
                    .fill(color)
                    .frame(width: geometry.size.width * percentage)
            }
        }
        .frame(height: 4)
        .cornerRadius(2)
    }
}

private struct EventRow: View {
    let event: AnalyticsEvent
    let index: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(event.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                Spacer()
                Text(timeString)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            ForEach(Array(event.properties.keys.sorted()), id: \.self) { key in
                if let value = event.properties[key], key != "timestamp" && key != "session_id" {
                    HStack {
                        Text(key)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                        Spacer()
                        Text(String(describing: value))
                            .font(.caption)
                            .fontDesign(.monospaced)
                    }
                }
            }
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(6)
    }

    private var timeString: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss.SSS"
        return formatter.string(from: event.timestamp)
    }
}

// MARK: - Preview

#Preview {
    let analytics = ConsoleAnalytics()
    let sessionId = UUID().uuidString

    // Add sample events
    analytics.log(.inputReceived(InputReceivedEvent(
        timestamp: Date(),
        inputType: .text,
        sessionId: sessionId
    )))

    analytics.trackLatency(operation: "intent_classification", latencyMs: 1234, sessionId: sessionId)

    analytics.log(.intentClassified(IntentClassifiedEvent(
        intent: "club_adjustment",
        confidence: 0.92,
        latencyMs: 1234,
        sessionId: sessionId
    )))

    analytics.trackLatency(operation: "routing", latencyMs: 45, sessionId: sessionId)

    analytics.log(.routeExecuted(RouteExecutedEvent(
        module: "CADDY",
        screen: "club_selection",
        latencyMs: 45,
        sessionId: sessionId
    )))

    return IntentTraceView(analytics: analytics, sessionId: sessionId)
}
#endif
