package app.tanh.toolsftw.location

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import app.tanh.toolsftw.sensor.CompassLocationData
import java.util.concurrent.TimeUnit

class TrueNorthProvider(private val context: Context) {
    private val locationManager = context.getSystemService(LocationManager::class.java)
    private var cachedData: CompassLocationData? = null
    private var cachedAtElapsedRealtime = 0L
    private var currentLocationRequest: CancellationSignal? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (currentLocationRequest != null || !hasLocationPermission(context)) return
        val provider = preferredProvider() ?: return
        val cancellationSignal = CancellationSignal()
        currentLocationRequest = cancellationSignal
        runCatching {
            locationManager.getCurrentLocation(provider, cancellationSignal, context.mainExecutor) { location ->
                if (currentLocationRequest == cancellationSignal) {
                    currentLocationRequest = null
                }
                location?.let(::updateCachedLocation)
            }
        }.onFailure {
            if (currentLocationRequest == cancellationSignal) {
                currentLocationRequest = null
            }
        }
    }

    fun stop() {
        currentLocationRequest?.cancel()
        currentLocationRequest = null
    }

    fun locationData(): CompassLocationData {
        val now = SystemClock.elapsedRealtime()
        cachedData?.takeIf {
            val cacheMillis =
                if (it.coordinates == null) EMPTY_LOCATION_CACHE_MILLIS else LOCATION_CACHE_MILLIS
            now - cachedAtElapsedRealtime < cacheMillis
        }?.let {
            return it
        }
        val location = latestKnownLocation()
        val data = location?.toCompassLocationData() ?: CompassLocationData()
        cachedData = data
        cachedAtElapsedRealtime = now
        return data
    }

    private fun latestKnownLocation(): Location? {
        if (!hasLocationPermission(context)) return null
        return locationManager
            .getProviders(true)
            .mapNotNull(::lastKnownLocation)
            .filterNot(::isStale)
            .maxByOrNull(Location::getElapsedRealtimeNanos)
    }

    private fun preferredProvider(): String? {
        val enabledProviders = locationManager.getProviders(true).toSet()
        return PROVIDER_PRIORITY.firstOrNull(enabledProviders::contains) ?: enabledProviders.firstOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(provider: String): Location? =
        runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()

    private fun updateCachedLocation(location: Location) {
        if (isStale(location)) return
        cachedData = location.toCompassLocationData()
        cachedAtElapsedRealtime = SystemClock.elapsedRealtime()
    }

    private fun Location.toCompassLocationData(): CompassLocationData =
        CompassLocationData(
            declinationDegrees =
                GeomagneticField(
                    latitude.toFloat(),
                    longitude.toFloat(),
                    altitude.toFloat(),
                    System.currentTimeMillis(),
                ).declination,
            aslMeters =
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                        hasMslAltitude()
                ) {
                    mslAltitudeMeters
                } else {
                    null
                },
            coordinates = latitude to longitude,
        )

    private fun isStale(location: Location): Boolean =
        TimeUnit.NANOSECONDS.toMillis(
            SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        ) > MAX_LOCATION_AGE_MILLIS

    private companion object {
        const val EMPTY_LOCATION_CACHE_MILLIS = 5_000L
        const val LOCATION_CACHE_MILLIS = 60_000L
        const val MAX_LOCATION_AGE_MILLIS = 10 * 60_000L
        val PROVIDER_PRIORITY =
            listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )
    }
}
