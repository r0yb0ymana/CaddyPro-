package caddypro.domain.navcaddy.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NetworkMonitor.
 *
 * Tests network connectivity monitoring and state detection.
 *
 * Spec reference: navcaddy-engine.md C6 (Offline behavior)
 * Plan reference: navcaddy-engine-plan.md Task 24
 */
class NetworkMonitorTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var network: Network
    private lateinit var networkCapabilities: NetworkCapabilities

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        connectivityManager = mockk(relaxed = true)
        network = mockk(relaxed = true)
        networkCapabilities = mockk(relaxed = true)

        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivityManager
    }

    @Test
    fun `isCurrentlyOnline returns true when network has internet capability`() {
        // Given: Active network with internet and validated capabilities
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every {
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } returns true
        every {
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } returns true

        // When
        val networkMonitor = NetworkMonitor(context)
        val isOnline = networkMonitor.isCurrentlyOnline()

        // Then
        assertTrue("Should be online when network has internet capability", isOnline)
    }

    @Test
    fun `isCurrentlyOnline returns false when no active network`() {
        // Given: No active network
        every { connectivityManager.activeNetwork } returns null

        // When
        val networkMonitor = NetworkMonitor(context)
        val isOnline = networkMonitor.isCurrentlyOnline()

        // Then
        assertFalse("Should be offline when no active network", isOnline)
    }

    @Test
    fun `isCurrentlyOnline returns false when network capabilities are null`() {
        // Given: Active network but no capabilities
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns null

        // When
        val networkMonitor = NetworkMonitor(context)
        val isOnline = networkMonitor.isCurrentlyOnline()

        // Then
        assertFalse("Should be offline when network capabilities are null", isOnline)
    }

    @Test
    fun `isCurrentlyOnline returns false when network lacks internet capability`() {
        // Given: Active network without internet capability
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every {
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } returns false
        every {
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } returns true

        // When
        val networkMonitor = NetworkMonitor(context)
        val isOnline = networkMonitor.isCurrentlyOnline()

        // Then
        assertFalse("Should be offline when network lacks internet capability", isOnline)
    }

    @Test
    fun `isCurrentlyOnline returns false when network is not validated`() {
        // Given: Active network with internet but not validated
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every {
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } returns true
        every {
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } returns false

        // When
        val networkMonitor = NetworkMonitor(context)
        val isOnline = networkMonitor.isCurrentlyOnline()

        // Then
        assertFalse("Should be offline when network is not validated", isOnline)
    }

    @Test
    fun `isCurrentlyOffline returns true when offline`() {
        // Given: No active network
        every { connectivityManager.activeNetwork } returns null

        // When
        val networkMonitor = NetworkMonitor(context)
        val isOffline = networkMonitor.isCurrentlyOffline()

        // Then
        assertTrue("Should return true when offline", isOffline)
    }

    @Test
    fun `isCurrentlyOffline returns false when online`() {
        // Given: Active network with internet capability
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every {
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } returns true
        every {
            networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } returns true

        // When
        val networkMonitor = NetworkMonitor(context)
        val isOffline = networkMonitor.isCurrentlyOffline()

        // Then
        assertFalse("Should return false when online", isOffline)
    }

    @Test
    fun `getNetworkType returns WIFI when connected to WiFi`() {
        // Given: WiFi network
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } returns true

        // When
        val networkMonitor = NetworkMonitor(context)
        val networkType = networkMonitor.getNetworkType()

        // Then
        assertTrue("Should return WIFI type", networkType == NetworkMonitor.NetworkType.WIFI)
    }

    @Test
    fun `getNetworkType returns CELLULAR when connected to cellular`() {
        // Given: Cellular network
        every { connectivityManager.activeNetwork } returns network
        every { connectivityManager.getNetworkCapabilities(network) } returns networkCapabilities
        every {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } returns false
        every {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        } returns true

        // When
        val networkMonitor = NetworkMonitor(context)
        val networkType = networkMonitor.getNetworkType()

        // Then
        assertTrue("Should return CELLULAR type", networkType == NetworkMonitor.NetworkType.CELLULAR)
    }

    @Test
    fun `getNetworkType returns NONE when no active network`() {
        // Given: No active network
        every { connectivityManager.activeNetwork } returns null

        // When
        val networkMonitor = NetworkMonitor(context)
        val networkType = networkMonitor.getNetworkType()

        // Then
        assertTrue("Should return NONE type", networkType == NetworkMonitor.NetworkType.NONE)
    }
}
