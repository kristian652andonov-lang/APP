package com.aura.dashboard.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.aura.dashboard.core.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

interface ConnectivityObserver {
    /** True when the device has a network connection with internet capability. */
    val isOnline: StateFlow<Boolean>
}

@Singleton
class AndroidConnectivityObserver @Inject constructor(
    @ApplicationContext context: Context,
    @ApplicationScope scope: CoroutineScope,
) : ConnectivityObserver {

    private val manager = context.getSystemService(ConnectivityManager::class.java)

    private fun currentlyOnline(): Boolean {
        val cm = manager ?: return true
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override val isOnline: StateFlow<Boolean> = callbackFlow {
        val cm = manager
        if (cm == null) {
            trySend(true)
            awaitClose { }
            return@callbackFlow
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(currentlyOnline())
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
            }

            override fun onUnavailable() {
                trySend(false)
            }
        }
        trySend(currentlyOnline())
        runCatching { cm.registerDefaultNetworkCallback(callback) }
        awaitClose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }
        .conflate()
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), currentlyOnline())
}
