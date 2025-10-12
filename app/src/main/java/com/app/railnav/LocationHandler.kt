package com.app.railnav

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import org.osmdroid.util.GeoPoint

class LocationHandler(
    private val context: Context,
    private val onLocationReceived: (GeoPoint) -> Unit,
    private val onPermissionDenied: () -> Unit
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var locationListener: LocationListener? = null

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            onPermissionDenied()
            return
        }

        try {
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    onLocationReceived(geoPoint)
                }

                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }

            // Try GPS first, then network
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    5000L, // 5 seconds
                    5f,    // 5 meters
                    locationListener!!
                )
            }

            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000L,
                    5f,
                    locationListener!!
                )
            }

            // Get last known location immediately
            val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            lastLocation?.let {
                onLocationReceived(GeoPoint(it.latitude, it.longitude))
            }
        } catch (e: SecurityException) {
            onPermissionDenied()
        }
    }

    fun stopLocationUpdates() {
        try {
            locationListener?.let {
                locationManager.removeUpdates(it)
            }
        } catch (e: SecurityException) {
            // Ignore
        }
    }
}