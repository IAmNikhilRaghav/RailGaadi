package com.railgaadi.app.util

data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double = 0.0,
    val accuracyMeters: Float = 0f,
    val altitudeMeters: Double = 0.0,
    val timestamp: Long = 0L
)

expect object PlatformLocation {
    fun startLocationUpdates(onLocation: (GeoLocation) -> Unit)
    fun stopLocationUpdates()
    fun getLastLocation(): GeoLocation?
    fun hasPermission(): Boolean
}
