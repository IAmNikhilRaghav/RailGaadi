package com.railgaadi.app.model

enum class AlarmTriggerUnit(val label: String, val shortLabel: String) {
    KILOMETERS("Distance (km before station)", "km"),
    MINUTES("Time (minutes before arrival)", "min")
}

enum class AlarmTone(val id: String, val displayName: String, val emoji: String) {
    RAILWAY_CHIME("chime", "IR Station Chime", "🔔"),
    GENTLE_ALARM("gentle", "Gentle Wake Bell", "✨"),
    LOUD_HORN("horn", "Locomotive Horn Alert", "⚡"),
    RADAR_PULSE("radar", "Radar Telemetry Beacon", "📡")
}

data class DestinationAlarm(
    val trainNumber: String,
    val trainName: String,
    val stationCode: String,
    val stationName: String,
    val stationLat: Double = 0.0,
    val stationLon: Double = 0.0,
    val triggerDistanceKm: Double = 10.0, // 5, 10, 15, 20, 25 km
    val triggerMinutesBefore: Int = 15,    // 10, 15, 20, 30 min
    val triggerUnit: AlarmTriggerUnit = AlarmTriggerUnit.KILOMETERS,
    val voiceAnnouncement: Boolean = true,
    val vibrate: Boolean = true,
    val tone: AlarmTone = AlarmTone.RAILWAY_CHIME,
    val isArmed: Boolean = true,
    val isRinging: Boolean = false,
    val isSnoozed: Boolean = false,
    val distanceRemainingKm: Double? = null,
    val minutesRemaining: Int? = null
)
