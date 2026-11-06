package com.railgaadi.app.model

import kotlinx.serialization.Serializable

@Serializable
data class SupportedLanguage(
    val code: String,
    val label: String,
    val native: String,
    val pill: String,
    val flag: String = "🇮🇳"
)

@Serializable
data class Train(
    val number: String,
    val name: String,
    val type: String = "Superfast Express",
    val origin: String,
    val destination: String,
    val departureTime: String,
    val arrivalTime: String,
    val durationMinutes: Int = 0,
    val platform: String = "1",
    val runsOnDays: List<Boolean> = List(7) { true }
)

@Serializable
data class Station(
    val code: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val scheduledDep: String = "—",
    val scheduledArr: String = "—",
    val distanceKm: Double = 0.0,
    val platform: String = "1",
    val haltMinutes: Int = 2
)

@Serializable
data class LiveStatus(
    val trainNumber: String,
    val trainName: String,
    val status: String, // "ON TIME", "DELAYED", "EARLY", "ARRIVED"
    val delayMinutes: Int = 0,
    val speedKmh: Int = 0,
    val currentStation: String = "",
    val nextStation: String = "",
    val progressPercent: Float = 0f,
    val stations: List<Station> = emptyList(),
    val coordinates: List<Pair<Double, Double>> = emptyList() // [lon, lat]
)

enum class TabItem(val id: String, val icon: String, val i18nKey: String) {
    LIVE_MAP("map", "🗺️", "tab_live_map"),
    ROUTES("routes", "🚆", "tab_routes"),
    ANALYTICS("analytics", "📈", "tab_analytics"),
    WEATHER("weather", "⛅", "tab_weather"),
    STATION("station", "🚉", "tab_station")
}

