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
data class StationBoardTrain(
    val number: String,
    val name: String,
    val type: String = "Superfast Express",
    val origin: String,
    val destination: String,
    val scheduledArr: String? = null,
    val scheduledDep: String? = null,
    val actualArr: String? = null,
    val actualDep: String? = null,
    val platform: String = "1",
    val delayMinutes: Int = 0,
    val haltMinutes: Int = 2
) {
    fun toTrain(): Train = Train(
        number = number,
        name = name,
        type = type,
        origin = origin,
        destination = destination,
        departureTime = scheduledDep ?: scheduledArr ?: "12:00",
        arrivalTime = scheduledArr ?: scheduledDep ?: "12:00",
        platform = platform
    )
}

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

@Serializable
data class StationWeatherData(
    val stationName: String = "",
    val temperature: Double = 28.0,
    val condition: String = "Clear Sky",
    val conditionIcon: String = "☀️",
    val windKmh: Double = 12.0,
    val humidityPercent: Int = 60,
    val visibilityKm: Double = 10.0,
    val rainProbability: Int = 20,
    val usAqi: Int? = null,
    val pm25: Double? = null,
    val pm10: Double? = null,
    val no2: Double? = null,
    val so2: Double? = null
) {
    val aqiCategory: AqiCategory get() = when {
        usAqi == null -> AqiCategory.UNKNOWN
        usAqi <= 50 -> AqiCategory.GOOD
        usAqi <= 100 -> AqiCategory.SATISFACTORY
        usAqi <= 150 -> AqiCategory.MODERATE
        usAqi <= 200 -> AqiCategory.POOR
        usAqi <= 300 -> AqiCategory.VERY_POOR
        else -> AqiCategory.SEVERE
    }
    val isFogAdvisory: Boolean get() = visibilityKm < 1.5 || condition.contains("Fog", ignoreCase = true)
    val isSmogAdvisory: Boolean get() = (usAqi ?: 0) > 250
}

enum class AqiCategory(val label: String, val emoji: String, val colorHex: Long) {
    GOOD("Good", "🟢", 0xFF16A34A),
    SATISFACTORY("Satisfactory", "🟡", 0xFF65A30D),
    MODERATE("Moderate", "🟠", 0xFFD97706),
    POOR("Poor", "🔴", 0xFFDC2626),
    VERY_POOR("Very Poor", "🟣", 0xFF7C3AED),
    SEVERE("Severe", "🟤", 0xFF92400E),
    UNKNOWN("—", "⚪", 0xFF94A3B8)
}

@Serializable
data class Landmark(
    val name: String,
    val category: String, // "monument", "heritage", "bridge", "river", "mountain"
    val lat: Double,
    val lon: Double,
    val description: String,
    val distanceKm: Double = 0.0
) {
    val categoryIcon: String get() = when (category) {
        "monument" -> "🏛️"
        "heritage" -> "🏯"
        "bridge" -> "🌉"
        "river" -> "🌊"
        "mountain" -> "⛰️"
        "temple" -> "⛩️"
        "park" -> "🌿"
        else -> "📍"
    }
}

enum class TabItem(val id: String, val icon: String, val i18nKey: String) {
    LIVE_MAP("map", "🗺️", "tab_live_map"),
    ROUTES("routes", "🚆", "tab_routes"),
    ANALYTICS("analytics", "📈", "tab_analytics"),
    WEATHER("weather", "⛅", "tab_weather"),
    STATION("station", "🚉", "tab_station")
}

