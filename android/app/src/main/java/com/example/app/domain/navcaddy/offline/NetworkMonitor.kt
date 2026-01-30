package caddypro.domain.navcaddy.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors network connectivity state using ConnectivityManager.
 *
 * Provides a Flow of connectivity status that emits true when connected,
 * false when disconnected. Uses Android's NetworkCallback API for efficient
 * monitoring without polling.
 *
 * Spec reference: navcaddy-engine.md C6 (Offline behavior)
 * Plan reference: navcaddy-engine-plan.md Task 24
 */
@Singleton
class NetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    /**
     * Flow of network connectivity status.
     *
     * Emits true when network is available and has internet capability,
     * false otherwise. Only emits when state changes (distinctUntilChanged).
     *
     * @return Flow<Boolean> where true = online, false = offline
     */
    val isOnline: Flow<Boolean> = callbackFlow {
        // Send initial state
        trySend(getCurrentConnectivityState())

        // Register network callback
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            // Track active networks
            private val networks = mutableSetOf<Network>()

            override fun onAvailable(network: Network) {
                networks.add(network)
                trySend(true)
            }

            override fun onLost(network: Network) {
                networks.remove(network)
                // Only send false if no networks remain
                if (networks.isEmpty()) {
                    trySend(false)
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                // Check if network has internet capability
                val hasInternet = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_INTERNET
                )
                val hasValidated = networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_VALIDATED
                )

                if (hasInternet && hasValidated) {
                    networks.add(network)
                    trySend(true)
                } else {
                    networks.remove(network)
                    if (networks.isEmpty()) {
                        trySend(false)
                    }
                }
            }
        }

        // Create network request for all transports
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        // Register callback
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Clean up when Flow is cancelled
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged()

    /**
     * Get current connectivity state synchronously.
     *
     * Useful for checking network state outside of Flow collection.
     *
     * @return true if device has validated internet connection, false otherwise
     */
    fun isCurrentlyOnline(): Boolean {
        return getCurrentConnectivityState()
    }

    /**
     * Check if device is currently offline.
     *
     * @return true if device has no validated internet connection
     */
    fun isCurrentlyOffline(): Boolean {
        return !isCurrentlyOnline()
    }

    /**
     * Get current connectivity state by checking active network.
     */
    private fun getCurrentConnectivityState(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /**
     * Get network type for analytics or debugging.
     *
     * @return NetworkType enum indicating current network connection type
     */
    fun getNetworkType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
            else -> NetworkType.OTHER
        }
    }

    /**
     * Network connection type.
     */
    enum class NetworkType {
        NONE,
        WIFI,
        CELLULAR,
        ETHERNET,
        OTHER
    }
}
