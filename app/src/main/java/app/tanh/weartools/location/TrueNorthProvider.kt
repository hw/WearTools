package app.tanh.weartools.location

import android.annotation.SuppressLint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import app.tanh.weartools.sensor.CompassLocationData

class TrueNorthProvider(private val context: Context) {
    private val locationManager = context.getSystemService(LocationManager::class.java)
    private var cachedData: CompassLocationData? = null
    private var cachedAtElapsedRealtime = 0L

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
        val data =
            if (location == null) {
                CompassLocationData()
            } else {
                CompassLocationData(
                    declinationDegrees =
                        GeomagneticField(
                            location.latitude.toFloat(),
                            location.longitude.toFloat(),
                            location.altitude.toFloat(),
                            System.currentTimeMillis(),
                        ).declination,
                    aslMeters =
                        if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                                location.hasMslAltitude()
                        ) {
                            location.mslAltitudeMeters
                        } else {
                            null
                        },
                    coordinates = location.latitude to location.longitude,
                )
            }
        cachedData = data
        cachedAtElapsedRealtime = now
        return data
    }

    private fun latestKnownLocation(): Location? {
        if (!hasLocationPermission()) return null
        return locationManager
            .getProviders(true)
            .mapNotNull(::lastKnownLocation)
            .maxByOrNull(Location::getTime)
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(provider: String): Location? =
        runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private companion object {
        const val EMPTY_LOCATION_CACHE_MILLIS = 5_000L
        const val LOCATION_CACHE_MILLIS = 60_000L
    }
}
