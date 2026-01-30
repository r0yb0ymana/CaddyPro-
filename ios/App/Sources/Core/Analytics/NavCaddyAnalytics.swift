import Foundation

/**
 * Protocol for NavCaddy analytics and observability.
 *
 * Provides event tracking for the NavCaddy pipeline with:
 * - Session correlation via session IDs
 * - Latency tracking at each stage
 * - PII-safe logging
 * - Pluggable implementations (console, remote, etc.)
 *
 * Spec reference: navcaddy-engine.md R8
 * Plan reference: navcaddy-engine-plan.md Task 22
 */
protocol NavCaddyAnalytics {
    /// Log an analytics event
    /// - Parameter event: Event to log
    func log(_ event: AnalyticsEvent)

    /// Track latency for a specific operation
    /// - Parameters:
    ///   - operation: Name of the operation
    ///   - latencyMs: Latency in milliseconds
    ///   - sessionId: Session ID for correlation
    func trackLatency(operation: String, latencyMs: Int, sessionId: String)

    /// Get all events for a session (for debugging)
    /// - Parameter sessionId: Session ID
    /// - Returns: All events for the session
    func getEvents(for sessionId: String) -> [AnalyticsEvent]

    /// Clear all events (for testing/debugging)
    func clearEvents()
}

// MARK: - Console Implementation

/**
 * Console-based analytics implementation for development/QA.
 *
 * Logs events to console with PII redaction.
 * Stores events in memory for debugging.
 */
@MainActor
final class ConsoleAnalytics: NavCaddyAnalytics {
    // MARK: - Storage

    private var events: [AnalyticsEvent] = []
    private var latencies: [String: [Int]] = [:] // operation -> latencies

    // MARK: - Configuration

    private let enablePIIRedaction: Bool
    private let verbose: Bool

    // MARK: - Initialization

    init(enablePIIRedaction: Bool = true, verbose: Bool = true) {
        self.enablePIIRedaction = enablePIIRedaction
        self.verbose = verbose
    }

    // MARK: - NavCaddyAnalytics

    func log(_ event: AnalyticsEvent) {
        // Store event
        events.append(event)

        // Log to console
        if verbose {
            logToConsole(event)
        }
    }

    func trackLatency(operation: String, latencyMs: Int, sessionId: String) {
        // Store latency
        latencies[operation, default: []].append(latencyMs)

        // Log to console
        if verbose {
            print("[Analytics] \(operation): \(latencyMs)ms (session: \(sessionId))")
        }
    }

    func getEvents(for sessionId: String) -> [AnalyticsEvent] {
        events.filter { $0.sessionId == sessionId }
    }

    func clearEvents() {
        events.removeAll()
        latencies.removeAll()
    }

    // MARK: - Latency Statistics

    /// Get latency statistics for an operation
    func getLatencyStats(for operation: String) -> LatencyStats? {
        guard let values = latencies[operation], !values.isEmpty else {
            return nil
        }

        let sorted = values.sorted()
        let count = values.count

        return LatencyStats(
            count: count,
            min: sorted.first ?? 0,
            max: sorted.last ?? 0,
            mean: values.reduce(0, +) / count,
            p50: sorted[count / 2],
            p95: sorted[min(Int(Double(count) * 0.95), count - 1)]
        )
    }

    // MARK: - Private Helpers

    private func logToConsole(_ event: AnalyticsEvent) {
        let timestamp = ISO8601DateFormatter().string(from: event.timestamp)
        print("[Analytics] [\(timestamp)] \(event.name)")

        // Log properties with optional PII redaction
        for (key, value) in event.properties {
            let valueString = String(describing: value)
            let safeValue = enablePIIRedaction ? PIIRedactor.redact(valueString) : valueString
            print("  \(key): \(safeValue)")
        }
    }
}

// MARK: - Latency Statistics

/// Statistics for operation latencies
struct LatencyStats {
    let count: Int
    let min: Int
    let max: Int
    let mean: Int
    let p50: Int
    let p95: Int
}

// MARK: - No-Op Implementation

/**
 * No-op analytics implementation for production or when analytics is disabled.
 */
final class NoOpAnalytics: NavCaddyAnalytics {
    func log(_ event: AnalyticsEvent) {
        // No-op
    }

    func trackLatency(operation: String, latencyMs: Int, sessionId: String) {
        // No-op
    }

    func getEvents(for sessionId: String) -> [AnalyticsEvent] {
        return []
    }

    func clearEvents() {
        // No-op
    }
}

// MARK: - Helper Extensions

extension NavCaddyAnalytics {
    /// Track the execution time of an operation
    /// - Parameters:
    ///   - operation: Name of the operation
    ///   - sessionId: Session ID for correlation
    ///   - block: Async block to execute and measure
    /// - Returns: Result of the block
    func track<T>(
        operation: String,
        sessionId: String,
        _ block: () async throws -> T
    ) async rethrows -> T {
        let startTime = Date()
        defer {
            let latencyMs = Int(Date().timeIntervalSince(startTime) * 1000)
            trackLatency(operation: operation, latencyMs: latencyMs, sessionId: sessionId)
        }
        return try await block()
    }
}
