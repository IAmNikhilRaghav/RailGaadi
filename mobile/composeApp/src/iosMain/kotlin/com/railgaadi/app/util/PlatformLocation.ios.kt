package com.railgaadi.app.util

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBestForNavigation
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.Foundation.timeIntervalSince1970
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
actual object PlatformLocation {
    private var locationManager: CLLocationManager? = null
    private var locationDelegate: LocationDelegate? = null
    var lastLocation: GeoLocation? = null
    private var isListening = false

    private class LocationDelegate(private val callback: (GeoLocation) -> Unit) : NSObject(), CLLocationManagerDelegateProtocol {
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val loc = didUpdateLocations.lastOrNull() as? CLLocation ?: return
            val (lat, lon) = loc.coordinate.useContents { latitude to longitude }
            val speedKmh = if (loc.speed > 0.0) (loc.speed * 3.6) else 0.0
            val geo = GeoLocation(
                latitude = lat,
                longitude = lon,
                speedKmh = speedKmh,
                accuracyMeters = loc.horizontalAccuracy.toFloat(),
                altitudeMeters = loc.altitude,
                timestamp = (loc.timestamp.timeIntervalSince1970 * 1000).toLong()
            )
            lastLocation = geo
            callback(geo)
        }
    }

    actual fun hasPermission(): Boolean {
        val status = CLLocationManager.authorizationStatus()
        return status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways
    }

    actual fun startLocationUpdates(onLocation: (GeoLocation) -> Unit) {
        if (isListening) return
        try {
            val lm = CLLocationManager()
            locationManager = lm
            lm.desiredAccuracy = kCLLocationAccuracyBestForNavigation
            lm.distanceFilter = 5.0 // updates every 5 meters

            val delegate = LocationDelegate(onLocation)
            locationDelegate = delegate
            lm.delegate = delegate

            if (!hasPermission()) {
                lm.requestWhenInUseAuthorization()
            }
            lm.startUpdatingLocation()
            isListening = true
        } catch (e: Throwable) {
            println("PlatformLocation iOS start error: ${e.message}")
        }
    }

    actual fun stopLocationUpdates() {
        if (!isListening) return
        try {
            locationManager?.stopUpdatingLocation()
            locationManager?.delegate = null
            locationDelegate = null
            isListening = false
        } catch (_: Throwable) {}
    }

    actual fun getLastLocation(): GeoLocation? = lastLocation
}
