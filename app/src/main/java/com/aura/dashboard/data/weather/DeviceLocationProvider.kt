package com.aura.dashboard.data.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import android.os.CancellationSignal
import com.aura.dashboard.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Approximate device location for weather, via the platform LocationManager (no Play Services). */
@Singleton
class DeviceLocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val manager: LocationManager? = context.getSystemService(LocationManager::class.java)
    private val executor = Executors.newSingleThreadExecutor()

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** A fresh coarse fix if possible, otherwise the most recent known one. */
    suspend fun currentLocation(): Location? {
        val lm = manager ?: return null
        if (!hasPermission()) return null
        val fresh = providers(lm).firstNotNullOfOrNull { provider ->
            withTimeoutOrNull(LOCATION_TIMEOUT_MS) { requestCurrent(lm, provider) }
        }
        return fresh ?: lastKnown(lm)
    }

    /** City name for coordinates, using the platform geocoder when the device provides one. */
    suspend fun placeName(latitude: Double, longitude: Double): Pair<String, String?>? = withContext(ioDispatcher) {
        if (!Geocoder.isPresent()) return@withContext null
        runCatching {
            @Suppress("DEPRECATION")
            val address = Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 1)?.firstOrNull()
            address?.let { (it.locality ?: it.subAdminArea ?: it.adminArea ?: return@let null) to it.countryCode }
        }.getOrNull()
    }

    private fun providers(lm: LocationManager): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
        add(LocationManager.GPS_PROVIDER)
    }.filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

    private suspend fun requestCurrent(lm: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val signal = CancellationSignal()
            continuation.invokeOnCancellation { signal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(lm, provider, signal, executor) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            } catch (error: SecurityException) {
                // Permission revoked mid-request, or GPS needing fine location on old versions.
                if (continuation.isActive) continuation.resume(null)
            } catch (error: IllegalArgumentException) {
                // The provider disappeared between the check and the request.
                if (continuation.isActive) continuation.resume(null)
            }
        }

    private fun lastKnown(lm: LocationManager): Location? = providers(lm)
        .mapNotNull { provider ->
            try {
                lm.getLastKnownLocation(provider)
            } catch (error: SecurityException) {
                null
            } catch (error: IllegalArgumentException) {
                null
            }
        }
        .maxByOrNull { it.time }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 10_000L
    }
}
