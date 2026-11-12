package com.railgaadi.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat

actual object PlatformLocation {
    private var appContext: Context? = null
    private var locationManager: LocationManager? = null
    private var locationCallback: ((GeoLocation) -> Unit)? = null
    private var lastLocation: GeoLocation? = null
    private var isListening = false

    private val listener = object : LocationListener {
        override fun onLocationChanged(loc: Location) {
            val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6).coerceAtLeast(0.0) else 0.0
            val geo = GeoLocation(
                latitude = loc.latitude,
                longitude = loc.longitude,
                speedKmh = speedKmh,
                accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else 0f,
                altitudeMeters = if (loc.hasAltitude()) loc.altitude else 0.0,
                timestamp = loc.time
            )
            lastLocation = geo
            locationCallback?.invoke(geo)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    actual fun hasPermission(): Boolean {
        val ctx = appContext ?: return false
        val fine = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    actual fun startLocationUpdates(onLocation: (GeoLocation) -> Unit) {
        val lm = locationManager ?: return
        if (!hasPermission()) return

        locationCallback = onLocation
        if (isListening) return

        try {
            // First send last known location if available
            val lastGps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val best = when {
                lastGps != null && lastNet != null -> if (lastGps.time > lastNet.time) lastGps else lastNet
                lastGps != null -> lastGps
                else -> lastNet
            }
            best?.let { loc ->
                val speedKmh = if (loc.hasSpeed()) (loc.speed * 3.6).coerceAtLeast(0.0) else 0.0
                val geo = GeoLocation(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    speedKmh = speedKmh,
                    accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else 0f,
                    altitudeMeters = if (loc.hasAltitude()) loc.altitude else 0.0,
                    timestamp = loc.time
                )
                lastLocation = geo
                onLocation(geo)
            }

            // Register for satellite GPS updates (minTime: 1000ms = 1 sec, minDistance: 5 meters)
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 5f, listener)
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 10f, listener)
            }
            isListening = true
        } catch (e: Throwable) {
            println("PlatformLocation Android start error: ${e.message}")
        }
    }

    actual fun stopLocationUpdates() {
        if (!isListening) return
        try {
            locationManager?.removeUpdates(listener)
            isListening = false
            locationCallback = null
        } catch (_: Throwable) {}
    }

    actual fun getLastLocation(): GeoLocation? = lastLocation
}
