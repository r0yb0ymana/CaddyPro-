import Foundation
import Network
import Observation

/**
 * Monitors network connectivity using NWPathMonitor.
 *
 * Features:
 * - Real-time network status updates
 * - Debounced notifications to avoid flapping
 * - Observable state for reactive UI updates
 * - Battery-efficient monitoring
 *
 * Spec reference: navcaddy-engine.md C6 (Offline behavior)
 * Plan reference: navcaddy-engine-plan.md Task 24
 */
@Observable
@MainActor
final class NetworkMonitor {
    // MARK: - State

    /// Current network connectivity status.
    private(set) var isConnected: Bool = true

    /// Whether we've had at least one connectivity update.
    private(set) var hasReceivedUpdate: Bool = false

    /// Connection type (wifi, cellular, etc.)
    private(set) var connectionType: ConnectionType = .unknown

    // MARK: - Private Properties

    /// Network path monitor.
    private let monitor: NWPathMonitor

    /// Dispatch queue for monitoring.
    private let monitorQueue = DispatchQueue(label: "com.caddypro.networkmonitor")

    /// Debounce timer to avoid flapping.
    private var debounceTask: Task<Void, Never>?

    /// Debounce delay in milliseconds.
    private let debounceDelayMs: UInt64 = 500

    // MARK: - Initialization

    init() {
        self.monitor = NWPathMonitor()
    }

    deinit {
        monitor.cancel()
    }

    // MARK: - Public Interface

    /// Start monitoring network connectivity.
    func startMonitoring() {
        monitor.pathUpdateHandler = { [weak self] path in
            Task { @MainActor [weak self] in
                self?.handlePathUpdate(path)
            }
        }

        monitor.start(queue: monitorQueue)
    }

    /// Stop monitoring network connectivity.
    func stopMonitoring() {
        monitor.cancel()
        debounceTask?.cancel()
    }

    // MARK: - Private Implementation

    /// Handle network path update.
    private func handlePathUpdate(_ path: NWPath) {
        // Cancel any pending debounce
        debounceTask?.cancel()

        // Debounce the update to avoid flapping
        debounceTask = Task { @MainActor in
            // Wait for debounce delay
            try? await Task.sleep(nanoseconds: debounceDelayMs * 1_000_000)

            guard !Task.isCancelled else { return }

            // Update state
            let wasConnected = isConnected
            isConnected = path.status == .satisfied
            hasReceivedUpdate = true

            // Determine connection type
            if path.usesInterfaceType(.wifi) {
                connectionType = .wifi
            } else if path.usesInterfaceType(.cellular) {
                connectionType = .cellular
            } else if path.usesInterfaceType(.wiredEthernet) {
                connectionType = .ethernet
            } else if path.status == .satisfied {
                connectionType = .other
            } else {
                connectionType = .none
            }

            // Log state change
            if wasConnected != isConnected {
                print("[NetworkMonitor] Connectivity changed: \(isConnected ? "Connected" : "Disconnected") (\(connectionType))")
            }
        }
    }
}

// MARK: - Connection Type

/// Type of network connection.
enum ConnectionType: String {
    case wifi = "WiFi"
    case cellular = "Cellular"
    case ethernet = "Ethernet"
    case other = "Other"
    case none = "None"
    case unknown = "Unknown"
}

// MARK: - Network Status Snapshot

extension NetworkMonitor {
    /// Get current network status snapshot.
    var status: NetworkStatus {
        NetworkStatus(
            isConnected: isConnected,
            connectionType: connectionType,
            hasReceivedUpdate: hasReceivedUpdate
        )
    }
}

/// Snapshot of network status at a point in time.
struct NetworkStatus: Equatable {
    let isConnected: Bool
    let connectionType: ConnectionType
    let hasReceivedUpdate: Bool

    /// Whether we're definitely offline (have update and not connected).
    var isDefinitelyOffline: Bool {
        hasReceivedUpdate && !isConnected
    }

    /// Whether we're definitely online (have update and connected).
    var isDefinitelyOnline: Bool {
        hasReceivedUpdate && isConnected
    }

    /// Whether status is still unknown (no update received).
    var isUnknown: Bool {
        !hasReceivedUpdate
    }
}
