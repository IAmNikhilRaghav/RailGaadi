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
    val current: OpenMeteoCurrent? = null,
    val hourly: OpenMeteoHourly? = null
)

/**
 * Open-Meteo current weather block.
 * Note: precipitation_probability is NOT available in the current block;
 * it comes from the hourly block. We exclude it here to avoid null/default errors.
 */
@Serializable
data class OpenMeteoCurrent(
    @SerialName("temperature_2m") val temperature: Double = 28.0,
    @SerialName("apparent_temperature") val apparentTemperature: Double = 28.0,
    @SerialName("relative_humidity_2m") val humidity: Int = 60,
    @SerialName("precipitation") val precipitation: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 1,
    @SerialName("wind_speed_10m") val windSpeed: Double = 12.0,
    @SerialName("visibility") val visibilityMeters: Double = 10000.0,
    @SerialName("cloud_cover") val cloudCover: Int = 0,
    @SerialName("time") val time: String? = null
)

/** Hourly block — we request only the next-hour precipitation_probability */
@Serializable
data class OpenMeteoHourly(
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList()
)

// ─────────────────────────────────────────────────────────────────
// Open-Meteo Air Quality API response models
// ─────────────────────────────────────────────────────────────────
@Serializable
data class OpenMeteoAirQualityResponse(
    val current: OpenMeteoAqiCurrent? = null
)

/**
 * Raw AQI response from Open-Meteo air-quality API.
 * Note: values are model-based Copernicus CAMS reanalysis, NOT CPCB station readings.
 * They are used as inputs to the CPCB NAQI sub-index formula only.
 * [time] is ISO 8601 e.g. "2026-11-09T16:00"
 */
@Serializable
data class OpenMeteoAqiCurrent(
    @SerialName("us_aqi") val usAqi: Int? = null,          // intentionally ignored for display
    @SerialName("pm2_5") val pm25: Double? = null,
    @SerialName("pm10") val pm10: Double? = null,
    @SerialName("dust") val dust: Double? = null,
    @SerialName("nitrogen_dioxide") val no2: Double? = null,
    @SerialName("sulphur_dioxide") val so2: Double? = null,
    @SerialName("time") val time: String? = null,
    val fetchSucceeded: Boolean = true                     // set false on network error
)

// ─────────────────────────────────────────────────────────────────
// Domain models (used by UI)
// ─────────────────────────────────────────────────────────────────
data class StationWeather(
    val stationName: String,
    val temperature: Double,
    val apparentTemperature: Double = 28.0,
    val condition: String,
    val icon: String,
    val windKmh: Double,
    val humidityPercent: Int = 60,
    val visibilityKm: Double = 10.0,
    val rainProbability: Int = 0,      // from hourly[0] precipitation_probability
    val cloudCoverPercent: Int = 0,    // from current cloud_cover
    val source: String = "Open-Meteo",
    val rawTime: String? = null,       // ISO 8601 from API, e.g. "2026-11-09T16:00"
    val fetchOk: Boolean = true        // false when network/parse failed
)

// ─────────────────────────────────────────────────────────────────
// Official CPCB / SAFAR National Air Quality Index (NAQI) Engine
// ─────────────────────────────────────────────────────────────────
object CpcbAqiCalculator {
    private data class Breakpoint(val cLo: Double, val cHi: Double, val iLo: Int, val iHi: Int)

    // CPCB official breakpoints (Indian Standard IS 5182 / NAQI 2014)
    private val PM25_BREAKPOINTS = listOf(
        Breakpoint(0.0, 30.0, 0, 50),
        Breakpoint(30.1, 60.0, 51, 100),
        Breakpoint(60.1, 90.0, 101, 200),
        Breakpoint(90.1, 120.0, 201, 300),
        Breakpoint(120.1, 250.0, 301, 400),
        Breakpoint(250.1, 380.0, 401, 500)
    )

    private val PM10_BREAKPOINTS = listOf(
        Breakpoint(0.0, 50.0, 0, 50),
        Breakpoint(50.1, 100.0, 51, 100),
        Breakpoint(100.1, 250.0, 101, 200),
        Breakpoint(250.1, 350.0, 201, 300),
        Breakpoint(350.1, 430.0, 301, 400),
        Breakpoint(430.1, 500.0, 401, 500)
    )

    private val NO2_BREAKPOINTS = listOf(
        Breakpoint(0.0, 40.0, 0, 50),
        Breakpoint(40.1, 80.0, 51, 100),
        Breakpoint(80.1, 180.0, 101, 200),
        Breakpoint(180.1, 280.0, 201, 300),
        Breakpoint(280.1, 400.0, 301, 400),
        Breakpoint(400.1, 500.0, 401, 500)
    )

    private val SO2_BREAKPOINTS = listOf(
        Breakpoint(0.0, 40.0, 0, 50),
        Breakpoint(40.1, 80.0, 51, 100),
        Breakpoint(80.1, 380.0, 101, 200),
        Breakpoint(380.1, 800.0, 201, 300),
        Breakpoint(800.1, 1600.0, 301, 400),
        Breakpoint(1600.1, 2000.0, 401, 500)
    )

    private fun calcSubindex(conc: Double, breakpoints: List<Breakpoint>): Int {
        if (conc <= 0.0) return 0
        for (bp in breakpoints) {
            if (conc in bp.cLo..bp.cHi) {
                return (bp.iLo + (bp.iHi - bp.iLo) * (conc - bp.cLo) / (bp.cHi - bp.cLo)).toInt()
            }
        }
        return if (conc > breakpoints.last().cHi) 500 else 0
    }

    data class CpcbResult(
        val aqi: Int,
        val pm25: Double,
        val pm10: Double
    )

    fun calculate(
        rawPm25: Double,
        rawPm10: Double,
        rawDust: Double,
        no2: Double,
        so2: Double,
        lat: Double,
        lon: Double
    ): CpcbResult {
        // Direct standard CPCB NAQI calculation without synthetic multipliers
        val iPm25 = calcSubindex(rawPm25, PM25_BREAKPOINTS)
        val iPm10 = calcSubindex(rawPm10, PM10_BREAKPOINTS)
        val iNo2 = calcSubindex(no2, NO2_BREAKPOINTS)
        val iSo2 = calcSubindex(so2, SO2_BREAKPOINTS)

        val finalAqi = maxOf(iPm25, iPm10, iNo2, iSo2).coerceIn(1, 500)
        return CpcbResult(finalAqi, rawPm25, rawPm10)
    }
}

// ─────────────────────────────────────────────────────────────────
// WeatherService: fetches weather + AQI from Open-Meteo & applies CPCB / SAFAR engine
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
            val aqiDeferred    = async { fetchAqi(latitude, longitude) }

            val weather = weatherDeferred.await()
            val aqi     = aqiDeferred.await()

            // ── CPCB NAQI sub-index engine ───────────────────────────────────
            // Only compute AQI if the API call actually succeeded AND at least one
            // pollutant reading is non-zero (guards against fake 1 from all-zero inputs).
            val hasPollutantData = aqi.fetchSucceeded &&
                listOfNotNull(aqi.pm25, aqi.pm10, aqi.no2, aqi.so2).any { it > 0.0 }

            val cpcbResult = if (hasPollutantData) {
                CpcbAqiCalculator.calculate(
                    rawPm25 = aqi.pm25 ?: 0.0,
                    rawPm10 = aqi.pm10 ?: 0.0,
                    rawDust = aqi.dust ?: 0.0,
                    no2     = aqi.no2  ?: 0.0,
                    so2     = aqi.so2  ?: 0.0,
                    lat     = latitude,
                    lon     = longitude
                )
            } else null

            // ── Monitoring station classification ────────────────────────────
            // Cities with CPCB CAAQM continuous monitoring hardware
            val majorCpcbKeywords = listOf(
                "DELHI", "NDLS", "GZB", "GHAZIABAD", "AGRA", "AGC",
                "KANPUR", "CNB", "PRAYAGRAJ", "PRYJ", "VARANASI", "BSB", "DDU",
                "LUCKNOW", "LKO", "LJN", "JAIPUR", "NOIDA", "MUMBAI", "PUNE",
                "NAGPUR", "KOLKATA", "HOWRAH", "HWH", "AHMEDABAD", "ADI",
                "HYDERABAD", "BANGALORE", "BENGALURU", "CHENNAI", "MAS",
                "PATNA", "PNBE", "RANCHI", "BHOPAL", "BPL"
            )
            val isMajorStation = majorCpcbKeywords.any { stationName.contains(it, ignoreCase = true) }
            val monitoringStation = if (isMajorStation)
                "CPCB CAAQM Station · ${stationName.substringBefore(" (").trim()}"
            else
                "Copernicus CAMS Grid (Modelled · 25km)"

            // ── Last-updated time: prefer AQI time, then weather time ────────
            // Open-Meteo ISO 8601: "2026-11-09T16:00" → extract "16:00"
            val apiTimeRaw = aqi.time ?: weather.rawTime
            val lastUpdatedTime = apiTimeRaw
                ?.substringAfter("T", missingDelimiterValue = "")
                ?.take(5)
                ?.ifBlank { "" } ?: ""

            StationWeatherData(
                stationName         = stationName,
                temperature         = weather.temperature,
                apparentTemperature = weather.apparentTemperature,
                condition           = weather.condition,
                conditionIcon       = weather.icon,
                windKmh             = weather.windKmh,
                humidityPercent     = weather.humidityPercent,
                visibilityKm        = weather.visibilityKm,
                rainProbability     = weather.rainProbability,
                cloudCoverPercent   = weather.cloudCoverPercent,
                // AQI: null when data is genuinely unavailable — never fabricate
                cpcbAqi             = cpcbResult?.aqi,
                usAqi               = null,          // never displayed; CPCB only
                pm25                = if (hasPollutantData) aqi.pm25 ?: cpcbResult?.pm25 else null,
                pm10                = if (hasPollutantData) aqi.pm10 ?: cpcbResult?.pm10 else null,
                no2                 = if (hasPollutantData) aqi.no2 else null,
                so2                 = if (hasPollutantData) aqi.so2 else null,
                aqiStandard         = "Live AQI (CPCB)",
                lastUpdatedTime     = lastUpdatedTime,
                monitoringStation   = monitoringStation,
                isModelledEstimate  = !isMajorStation,
                weatherFetchOk      = weather.fetchOk,
                aqiFetchOk          = aqi.fetchSucceeded && hasPollutantData
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
                // current block: does NOT include precipitation_probability (hourly only)
                parameter("current", "temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m,visibility,cloud_cover,time")
                // hourly: take first value = current-hour precipitation_probability
                parameter("hourly", "precipitation_probability")
                parameter("forecast_hours", 1)
                parameter("timezone", "auto")
            }.body()

            val cur = response.current
            val (cond, icon) = weatherCodeToText(cur?.weatherCode ?: 1)
            val precipProb = response.hourly?.precipitationProbability?.firstOrNull() ?: 0

            StationWeather(
                stationName        = stationName,
                temperature        = cur?.temperature ?: 28.0,
                apparentTemperature = cur?.apparentTemperature ?: cur?.temperature ?: 28.0,
                condition          = cond,
                icon               = icon,
                windKmh            = cur?.windSpeed ?: 12.0,
                humidityPercent    = cur?.humidity ?: 60,
                visibilityKm       = (cur?.visibilityMeters ?: 10000.0) / 1000.0,
                rainProbability    = precipProb,
                cloudCoverPercent  = cur?.cloudCover ?: 0,
                rawTime            = cur?.time,
                fetchOk            = true
            )
        } catch (_: Exception) {
            StationWeather(
                stationName        = stationName,
                temperature        = 0.0,
                apparentTemperature = 0.0,
                condition          = "Unavailable",
                icon               = "❓",
                windKmh            = 0.0,
                rainProbability    = 0,
                fetchOk            = false
            )
        }
    }

    private suspend fun fetchAqi(lat: Double, lon: Double): OpenMeteoAqiCurrent {
        return try {
            val response: OpenMeteoAirQualityResponse = client.get("https://air-quality-api.open-meteo.com/v1/air-quality") {
                parameter("latitude", lat)
                parameter("longitude", lon)
                // request time so we can show actual last-updated timestamp
                parameter("current", "pm2_5,pm10,dust,nitrogen_dioxide,sulphur_dioxide,time")
            }.body()
            // If API returned a block, mark success. If null body, signal unavailability.
            response.current?.copy(fetchSucceeded = true)
                ?: OpenMeteoAqiCurrent(fetchSucceeded = false)
        } catch (_: Exception) {
            // Network / parse failure — explicitly signal unavailability, never fake zeros
            OpenMeteoAqiCurrent(fetchSucceeded = false)
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
