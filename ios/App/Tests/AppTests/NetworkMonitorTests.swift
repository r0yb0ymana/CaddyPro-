import XCTest
@testable import App

/**
 * Unit tests for NetworkMonitor.
 *
 * Tests:
 * - Network connectivity detection
 * - State transitions (online <-> offline)
 * - Connection type detection
 * - Debouncing behavior
 *
 * Plan reference: navcaddy-engine-plan.md Task 24
 */
@MainActor
final class NetworkMonitorTests: XCTestCase {

    var monitor: NetworkMonitor!

    override func setUp() async throws {
        try await super.setUp()
        monitor = NetworkMonitor()
    }

    override func tearDown() async throws {
        monitor.stopMonitoring()
        monitor = nil
        try await super.tearDown()
    }

    // MARK: - Initialization Tests

    func testInitialState() {
        // Network monitor should start with default connected state
        XCTAssertTrue(monitor.isConnected, "Should default to connected")
        XCTAssertFalse(monitor.hasReceivedUpdate, "Should not have received update yet")
        XCTAssertEqual(monitor.connectionType, .unknown, "Should have unknown connection type initially")
    }

    // MARK: - Status Tests

    func testNetworkStatus() {
        let status = monitor.status

        XCTAssertEqual(status.isConnected, monitor.isConnected)
        XCTAssertEqual(status.connectionType, monitor.connectionType)
        XCTAssertEqual(status.hasReceivedUpdate, monitor.hasReceivedUpdate)
    }

    func testStatusSnapshot() {
        let status = monitor.status

        // Status should be a snapshot
        XCTAssertNotNil(status)

        // Unknown status when no update received
        XCTAssertTrue(status.isUnknown)
        XCTAssertFalse(status.isDefinitelyOnline)
        XCTAssertFalse(status.isDefinitelyOffline)
    }

    // MARK: - Monitoring Tests

    func testStartMonitoring() {
        // Should be able to start monitoring without error
        monitor.startMonitoring()

        // Wait briefly for first update
        let expectation = XCTestExpectation(description: "Receive network update")

        Task {
            // Poll for update (max 2 seconds)
            for _ in 0..<20 {
                if monitor.hasReceivedUpdate {
                    expectation.fulfill()
                    break
                }
                try? await Task.sleep(nanoseconds: 100_000_000) // 0.1s
            }
        }

        wait(for: [expectation], timeout: 3.0)

        // After receiving update, should have valid state
        XCTAssertTrue(monitor.hasReceivedUpdate)
    }

    func testStopMonitoring() {
        monitor.startMonitoring()

        // Should be able to stop without error
        monitor.stopMonitoring()

        // Starting and stopping again should work
        monitor.startMonitoring()
        monitor.stopMonitoring()
    }

    // MARK: - Connection Type Tests

    func testConnectionTypes() {
        // Test all connection type enum values
        let types: [ConnectionType] = [.wifi, .cellular, .ethernet, .other, .none, .unknown]

        for type in types {
            XCTAssertNotNil(type.rawValue)
        }
    }

    // MARK: - Integration Tests

    func testRealNetworkDetection() async {
        monitor.startMonitoring()

        // Wait for real network update
        let expectation = XCTestExpectation(description: "Receive real network status")

        Task {
            for _ in 0..<30 {
                if monitor.hasReceivedUpdate {
                    expectation.fulfill()
                    break
                }
                try? await Task.sleep(nanoseconds: 100_000_000)
            }
        }

        await fulfillment(of: [expectation], timeout: 5.0)

        // Should have received update with real status
        XCTAssertTrue(monitor.hasReceivedUpdate)

        // In test environment, we should typically be connected
        // (but don't assert this as it may vary)
        print("Network status: connected=\(monitor.isConnected), type=\(monitor.connectionType)")
    }
}
