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
    val apparentTemperature: Double = 28.0,
    val condition: String = "Clear Sky",
    val conditionIcon: String = "☀️",
    val windKmh: Double = 12.0,
    val humidityPercent: Int = 60,
    val visibilityKm: Double = 10.0,
    val rainProbability: Int = 0,       // from hourly precipitation_probability (not current)
    val cloudCoverPercent: Int = 0,     // from current cloud_cover
    val cpcbAqi: Int? = null,
    val usAqi: Int? = null,
    val pm25: Double? = null,
    val pm10: Double? = null,
    val no2: Double? = null,
    val so2: Double? = null,
    val co: Double? = null,             // CO in mg/m³ (CPCB 8-hr standard)
    val o3: Double? = null,             // O3 in µg/m³ (CPCB 8-hr standard)
    val dominantPollutant: String? = null,
    val aqiStandard: String = "Live AQI (CPCB)",
    val lastUpdatedTime: String = "",
    val monitoringStation: String = "Copernicus CAMS Grid (Modelled · 25km)",
    val statePollutionBoard: String = "CPCB",
    val isModelledEstimate: Boolean = true,
    val isDirectStation: Boolean = false,
    val nearestStationDistanceKm: Double? = null,
    val isStaleData: Boolean = false,
    val weatherFetchOk: Boolean = true,  // false when weather API call failed
    val aqiFetchOk: Boolean = true       // false when AQI API call failed / no data
) {
    // CPCB NAQI: use max(sub-index) — cpcbAqi is preferred over usAqi
    val aqiValue: Int? get() = cpcbAqi ?: usAqi

    // Official Indian CPCB / CAQM 6-tier classification
    val aqiCategory: AqiCategory get() = when {
        aqiValue == null    -> AqiCategory.UNKNOWN
        aqiValue!! <= 50    -> AqiCategory.GOOD          // 0-50   Green
        aqiValue!! <= 100   -> AqiCategory.SATISFACTORY  // 51-100 Parrot Green
        aqiValue!! <= 200   -> AqiCategory.MODERATE      // 101-200 Yellow  ← AQI 175 = Moderate
        aqiValue!! <= 300   -> AqiCategory.POOR          // 201-300 Orange
        aqiValue!! <= 400   -> AqiCategory.VERY_POOR     // 301-400 Red
        else                -> AqiCategory.SEVERE        // 401-500 Maroon
    }
    val isFogAdvisory: Boolean get() = visibilityKm < 1.5 || condition.contains("Fog", ignoreCase = true)
    val isSmogAdvisory: Boolean get() = (aqiValue ?: 0) > 200
}

// Official Indian CPCB / CAQM 6-tier AQI classification
enum class AqiCategory(val label: String, val emoji: String, val colorHex: Long) {
    GOOD("Good",           "🟢", 0xFF16A34A),  // Green
    SATISFACTORY("Satisfactory", "🟢", 0xFF84CC16),  // Parrot Green
    MODERATE("Moderate",   "🟡", 0xFFEAB308),  // Yellow   ← AQI 101-200
    POOR("Poor",           "🟠", 0xFFF97316),  // Orange   ← AQI 201-300
    VERY_POOR("Very Poor", "🔴", 0xFFEF4444),  // Red      ← AQI 301-400
    SEVERE("Severe",       "🟤", 0xFF7F1D1D),  // Maroon   ← AQI 401-500
    UNKNOWN("—",           "⚪", 0xFF94A3B8)
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

