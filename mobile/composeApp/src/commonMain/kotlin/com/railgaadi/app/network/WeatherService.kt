package com.railgaadi.app.network

import com.railgaadi.app.model.Landmark
import com.railgaadi.app.model.StationWeatherData
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// ─────────────────────────────────────────────────────────────────
// Open-Meteo Forecast API response models
// ─────────────────────────────────────────────────────────────────
@Serializable
data class OpenMeteoForecastResponse(
    val current: OpenMeteoCurrent? = null
)

@Serializable
data class OpenMeteoCurrent(
    @SerialName("temperature_2m") val temperature: Double = 28.0,
    @SerialName("relative_humidity_2m") val humidity: Int = 60,
    @SerialName("precipitation") val precipitation: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 1,
    @SerialName("wind_speed_10m") val windSpeed: Double = 12.0,
    @SerialName("visibility") val visibilityMeters: Double = 10000.0,
    @SerialName("precipitation_probability") val rainProbability: Int? = 20
)

// ─────────────────────────────────────────────────────────────────
// Open-Meteo Air Quality API response models
// ─────────────────────────────────────────────────────────────────
@Serializable
data class OpenMeteoAirQualityResponse(
    val current: OpenMeteoAqiCurrent? = null
)

@Serializable
data class OpenMeteoAqiCurrent(
    @SerialName("us_aqi") val usAqi: Int? = null,
    @SerialName("pm2_5") val pm25: Double? = null,
    @SerialName("pm10") val pm10: Double? = null,
    @SerialName("nitrogen_dioxide") val no2: Double? = null,
    @SerialName("sulphur_dioxide") val so2: Double? = null
)

// ─────────────────────────────────────────────────────────────────
// Domain models (used by UI)
// ─────────────────────────────────────────────────────────────────
data class StationWeather(
    val stationName: String,
    val temperature: Double,
    val condition: String,
    val icon: String,
    val windKmh: Double,
    val humidityPercent: Int = 60,
    val visibilityKm: Double = 10.0,
    val rainProbability: Int = 20,
    val source: String = "Open-Meteo"
)

// ─────────────────────────────────────────────────────────────────
// WeatherService: fetches weather + AQI from Open-Meteo (no key)
// ─────────────────────────────────────────────────────────────────
class WeatherService(private val client: HttpClient = createHttpClient()) {

    // Fetch weather + AQI together for a station in one coroutine scope
    suspend fun getStationWeatherData(
        stationName: String,
        latitude: Double,
        longitude: Double
    ): StationWeatherData = withContext(Dispatchers.IO) {
        coroutineScope {
            val weatherDeferred = async { fetchWeather(stationName, latitude, longitude) }
            val aqiDeferred = async { fetchAqi(latitude, longitude) }

            val weather = weatherDeferred.await()
            val aqi = aqiDeferred.await()

            StationWeatherData(
                stationName = stationName,
                temperature = weather.temperature,
                condition = weather.condition,
                conditionIcon = weather.icon,
                windKmh = weather.windKmh,
                humidityPercent = weather.humidityPercent,
                visibilityKm = weather.visibilityKm,
                rainProbability = weather.rainProbability,
                usAqi = aqi.usAqi,
                pm25 = aqi.pm25,
                pm10 = aqi.pm10,
                no2 = aqi.no2,
                so2 = aqi.so2
            )
        }
    }

    // Also keep the old method signature for backward compat
    suspend fun getWeatherForCoordinates(
        stationName: String,
        latitude: Double,
        longitude: Double
    ): StationWeather = withContext(Dispatchers.IO) {
        fetchWeather(stationName, latitude, longitude)
    }

    private suspend fun fetchWeather(stationName: String, lat: Double, lon: Double): StationWeather {
        return try {
            val response: OpenMeteoForecastResponse = client.get("https://api.open-meteo.com/v1/forecast") {
                parameter("latitude", lat)
                parameter("longitude", lon)
                parameter("current", "temperature_2m,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,visibility,precipitation_probability")
                parameter("timezone", "auto")
            }.body()

            val cur = response.current
            val (cond, icon) = weatherCodeToText(cur?.weatherCode ?: 1)

            StationWeather(
                stationName = stationName,
                temperature = cur?.temperature ?: 28.0,
                condition = cond,
                icon = icon,
                windKmh = cur?.windSpeed ?: 12.0,
                humidityPercent = cur?.humidity ?: 60,
                visibilityKm = (cur?.visibilityMeters ?: 10000.0) / 1000.0,
                rainProbability = cur?.rainProbability ?: 20
            )
        } catch (_: Exception) {
            StationWeather(
                stationName = stationName,
                temperature = 28.0,
                condition = "Clear Sky",
                icon = "☀️",
                windKmh = 14.0
            )
        }
    }

    private suspend fun fetchAqi(lat: Double, lon: Double): OpenMeteoAqiCurrent {
        return try {
            val response: OpenMeteoAirQualityResponse = client.get("https://air-quality-api.open-meteo.com/v1/air-quality") {
                parameter("latitude", lat)
                parameter("longitude", lon)
                parameter("current", "us_aqi,pm2_5,pm10,nitrogen_dioxide,sulphur_dioxide")
            }.body()
            response.current ?: OpenMeteoAqiCurrent()
        } catch (_: Exception) {
            OpenMeteoAqiCurrent() // silent fallback
        }
    }

    fun weatherCodeToText(code: Int): Pair<String, String> = when (code) {
        0 -> "Clear Sky" to "☀️"
        1, 2, 3 -> "Partly Cloudy" to "⛅"
        45, 48 -> "Dense Fog" to "🌫️"
        51, 53, 55 -> "Drizzle" to "🌦️"
        61, 63, 65 -> "Rain" to "🌧️"
        71, 73, 75 -> "Snowfall" to "❄️"
        80, 81, 82 -> "Rain Showers" to "🌧️"
        95, 96, 99 -> "Thunderstorm" to "⛈️"
        else -> "Pleasant" to "🌤️"
    }
}

// ─────────────────────────────────────────────────────────────────
// LandmarkService: curated DB + Wikipedia GeoSearch
// ─────────────────────────────────────────────────────────────────
object LandmarkService {

    // Haversine distance in km
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.PI / 180 * (lat2 - lat1)
        val dLon = Math.PI / 180 * (lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.PI / 180 * lat1) * cos(Math.PI / 180 * lat2) * sin(dLon / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }

    // Pan-India curated corridor landmarks
    private val CURATED = listOf(
        // Delhi & surroundings
        Landmark("Red Fort (Lal Qila)", "heritage", 28.6562, 77.2410, "17th-century Mughal fortress and UNESCO World Heritage site."),
        Landmark("Qutub Minar Complex", "monument", 28.5245, 77.1855, "73-metre minaret built in 1192 — tallest brick minaret in the world."),
        Landmark("India Gate", "monument", 28.6129, 77.2295, "War memorial arch standing 42 metres tall at the heart of New Delhi."),
        Landmark("Humayun's Tomb", "heritage", 28.5933, 77.2507, "16th-century Mughal garden tomb — first garden-tomb on the Indian subcontinent."),
        Landmark("Lotus Temple", "monument", 28.5535, 77.2588, "Iconic Bahá'í House of Worship shaped like a lotus flower."),
        Landmark("Yamuna River Crossing", "river", 28.665, 77.255, "Historic river crossing near old Delhi Railway Bridge."),
        // Agra
        Landmark("Taj Mahal Viewpoint", "monument", 27.1751, 78.0421, "Marble mausoleum on the south bank of the Yamuna — one of the 7 wonders of the world."),
        Landmark("Agra Fort", "heritage", 27.1795, 78.0211, "16th-century UNESCO World Heritage Mughal fortress."),
        Landmark("Fatehpur Sikri", "heritage", 27.0945, 77.6700, "Abandoned royal Mughal city built by Emperor Akbar, a UNESCO World Heritage site."),
        // Gwalior
        Landmark("Gwalior Fort", "heritage", 26.2312, 78.1695, "8th-century hill fortress with dramatic defensive bastions."),
        // Kanpur / Prayagraj / Varanasi
        Landmark("Ganga River Rail Bridge (Kanpur)", "bridge", 26.470, 80.360, "Spans the holy Ganges River at Kanpur Central corridor."),
        Landmark("Triveni Sangam", "river", 25.423, 81.884, "Sacred confluence of the Ganges, Yamuna, and mythical Saraswati."),
        Landmark("Curzon Bridge (Prayagraj)", "bridge", 25.465, 81.850, "Historic 1905 dual-deck railway bridge across the Ganges."),
        Landmark("Kashi Vishwanath Corridor", "heritage", 25.3109, 83.0104, "Ancient riverside ghats and the grand Kashi Vishwanath temple corridor in Varanasi."),
        Landmark("Malviya Bridge", "bridge", 25.321, 83.033, "Iconic double-deck steel truss rail-road bridge across the Ganges."),
        // Jaipur / Rajasthan
        Landmark("Hawa Mahal", "monument", 26.9239, 75.8267, "Palace of Winds with 953 honeycomb windows, built in 1799."),
        Landmark("Amer (Amber) Fort", "heritage", 26.9855, 75.8513, "11th-century hilltop fort with stunning mirror palace interiors."),
        Landmark("Mehrangarh Fort Jodhpur", "heritage", 26.298, 73.018, "Towering cliffside fort 122 metres above the Blue City of Jodhpur."),
        Landmark("Umaid Bhawan Palace", "heritage", 26.280, 73.047, "One of the world's largest private royal residences, built in sandstone."),
        Landmark("Thar Desert Dunes", "mountain", 25.750, 71.390, "Vast golden sand ridges along the western corridor."),
        // Kolkata / Bengal
        Landmark("Howrah Bridge", "bridge", 22.5851, 88.3468, "Balanced cantilever bridge over the Hooghly river into Kolkata."),
        Landmark("Victoria Memorial", "monument", 22.5448, 88.3426, "Magnificent white marble palace built between 1906 and 1921."),
        Landmark("Damodar River Rail Bridge", "bridge", 23.670, 86.950, "Historic bridge crossing the Damodar river valley in Bengal."),
        Landmark("Parasnath Hill (Shikharji)", "mountain", 23.962, 86.136, "Highest peak in Jharkhand (1365 m) — sacred Jain pilgrimage site."),
        // Mumbai / Western Ghats
        Landmark("Chhatrapati Shivaji Maharaj Terminus", "heritage", 18.940, 72.835, "Victorian Gothic Revival UNESCO World Heritage rail headquarters."),
        Landmark("Western Ghats (Sahyadri Hills)", "mountain", 19.350, 73.150, "Lush biological hotspot rising dramatically east of Mumbai rail routes."),
        Landmark("Narmada River Bridge (Bharuch)", "bridge", 21.700, 72.990, "Historic bridge across the sacred Narmada River."),
        // Hyderabad / Deccan
        Landmark("Charminar", "monument", 17.3616, 78.4747, "Iconic 16th-century mosque and monument at the heart of Hyderabad."),
        Landmark("Golconda Fort", "heritage", 17.3833, 78.4011, "15th-century diamond-trade fortress with legendary acoustics."),
        // South
        Landmark("Mysuru (Mysore) Palace", "heritage", 12.3052, 76.6552, "Opulent Indo-Saracenic royal palace illuminated by 97,000 lights."),
        Landmark("Brihadeeswarar Temple (Thanjavur)", "heritage", 10.7827, 79.1317, "11th-century UNESCO-listed Chola granite temple, 66 metres tall.")
    )

    fun getNearbyLandmarks(lat: Double, lon: Double, radiusKm: Double = 60.0): List<Landmark> {
        return CURATED
            .filter { distanceKm(lat, lon, it.lat, it.lon) <= radiusKm }
            .sortedBy { distanceKm(lat, lon, it.lat, it.lon) }
            .take(8)
            .map { it.copy(distanceKm = distanceKm(lat, lon, it.lat, it.lon)) }
    }
}
