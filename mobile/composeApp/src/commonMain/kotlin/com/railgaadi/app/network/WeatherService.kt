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
import com.railgaadi.app.util.formatDecimals
import kotlin.math.PI
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
    @SerialName("dew_point_2m") val dewPoint: Double? = null,
    @SerialName("precipitation") val precipitation: Double = 0.0,
    @SerialName("weather_code") val weatherCode: Int = 1,
    @SerialName("wind_speed_10m") val windSpeed: Double = 12.0,
    @SerialName("wind_direction_10m") val windDirection: Int = 0,
    @SerialName("wind_gusts_10m") val windGusts: Double = 0.0,
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
 * MUST remain a pure @Serializable class — no non-JSON fields.
 * [time] is auto-included by Open-Meteo (ISO 8601 "2026-11-09T16:00").
 */
@Serializable
data class OpenMeteoAqiCurrent(
    @SerialName("pm2_5") val pm25: Double? = null,
    @SerialName("pm10") val pm10: Double? = null,
    @SerialName("dust") val dust: Double? = null,
    @SerialName("nitrogen_dioxide") val no2: Double? = null,
    @SerialName("sulphur_dioxide") val so2: Double? = null,
    @SerialName("carbon_monoxide") val co: Double? = null,
    @SerialName("ozone") val o3: Double? = null,
    @SerialName("time") val time: String? = null
)

/**
 * Non-serializable wrapper that carries both the parsed AQI data
 * and a network-fetch status flag. Keeps @Serializable classes clean.
 */
data class AqiFetchResult(
    val data: OpenMeteoAqiCurrent,
    val fetchSucceeded: Boolean
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
    val windDirectionDeg: Int = 0,
    val windGustsKmh: Double = 0.0,
    val dewPointC: Double = 15.0,
    val precipitationMm: Double = 0.0,
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

    // CPCB official breakpoints (Indian Standard IS 5182 / NAQI 2014) - Contiguous scale
    private val PM25_BREAKPOINTS = listOf(
        Breakpoint(0.0, 30.0, 0, 50),
        Breakpoint(30.0, 60.0, 51, 100),
        Breakpoint(60.0, 90.0, 101, 200),
        Breakpoint(90.0, 120.0, 201, 300),
        Breakpoint(120.0, 250.0, 301, 400),
        Breakpoint(250.0, 380.0, 401, 500)
    )

    private val PM10_BREAKPOINTS = listOf(
        Breakpoint(0.0, 50.0, 0, 50),
        Breakpoint(50.0, 100.0, 51, 100),
        Breakpoint(100.0, 250.0, 101, 200),
        Breakpoint(250.0, 350.0, 201, 300),
        Breakpoint(350.0, 430.0, 301, 400),
        Breakpoint(430.0, 500.0, 401, 500)
    )

    private val NO2_BREAKPOINTS = listOf(
        Breakpoint(0.0, 40.0, 0, 50),
        Breakpoint(40.0, 80.0, 51, 100),
        Breakpoint(80.0, 180.0, 101, 200),
        Breakpoint(180.0, 280.0, 201, 300),
        Breakpoint(280.0, 400.0, 301, 400),
        Breakpoint(400.0, 500.0, 401, 500)
    )

    private val SO2_BREAKPOINTS = listOf(
        Breakpoint(0.0, 40.0, 0, 50),
        Breakpoint(40.0, 80.0, 51, 100),
        Breakpoint(80.0, 380.0, 101, 200),
        Breakpoint(380.0, 800.0, 201, 300),
        Breakpoint(800.0, 1600.0, 301, 400),
        Breakpoint(1600.0, 2000.0, 401, 500)
    )

    // CO: CPCB standard in mg/m³ (8-hour averaging period)
    private val CO_BREAKPOINTS = listOf(
        Breakpoint(0.0, 1.0, 0, 50),
        Breakpoint(1.0, 2.0, 51, 100),
        Breakpoint(2.0, 10.0, 101, 200),
        Breakpoint(10.0, 17.0, 201, 300),
        Breakpoint(17.0, 34.0, 301, 400),
        Breakpoint(34.0, 50.0, 401, 500)
    )

    // O3: CPCB standard in µg/m³ (8-hour averaging period)
    private val O3_BREAKPOINTS = listOf(
        Breakpoint(0.0, 50.0, 0, 50),
        Breakpoint(50.0, 100.0, 51, 100),
        Breakpoint(100.0, 168.0, 101, 200),
        Breakpoint(168.0, 208.0, 201, 300),
        Breakpoint(208.0, 748.0, 301, 400),
        Breakpoint(748.0, 1000.0, 401, 500)
    )

    private fun calcSubindex(conc: Double, breakpoints: List<Breakpoint>): Int {
        if (conc <= 0.0) return 0
        for (bp in breakpoints) {
            if (conc >= bp.cLo && conc <= bp.cHi) {
                val ratio = (conc - bp.cLo) / (bp.cHi - bp.cLo)
                return (bp.iLo + (bp.iHi - bp.iLo) * ratio).toInt().coerceIn(1, 500)
            }
        }
        return if (conc > breakpoints.last().cHi) 500 else 0
    }

    data class CpcbResult(
        val aqi: Int,
        val dominantPollutant: String,
        val pm25: Double,
        val pm10: Double,
        val no2: Double,
        val so2: Double,
        val coMg: Double,
        val o3: Double
    )

    fun calculate(
        rawPm25: Double,
        rawPm10: Double,
        rawDust: Double,
        no2: Double,
        so2: Double,
        coUg: Double,
        o3: Double,
        lat: Double,
        lon: Double
    ): CpcbResult {
        val coMg = coUg / 1000.0 // Convert Open-Meteo µg/m³ to CPCB mg/m³ for CO

        val iPm25 = calcSubindex(rawPm25, PM25_BREAKPOINTS)
        val iPm10 = calcSubindex(rawPm10, PM10_BREAKPOINTS)
        val iNo2  = calcSubindex(no2, NO2_BREAKPOINTS)
        val iSo2  = calcSubindex(so2, SO2_BREAKPOINTS)
        val iCo   = calcSubindex(coMg, CO_BREAKPOINTS)
        val iO3   = calcSubindex(o3, O3_BREAKPOINTS)

        val subIndices = listOf(
            "PM2.5" to iPm25,
            "PM10"  to iPm10,
            "NO₂"   to iNo2,
            "SO₂"   to iSo2,
            "CO"    to iCo,
            "O₃"    to iO3
        )

        val dominant = subIndices.maxByOrNull { it.second } ?: ("PM2.5" to 0)
        val finalAqi = dominant.second.coerceIn(1, 500)

        return CpcbResult(
            aqi = finalAqi,
            dominantPollutant = dominant.first,
            pm25 = rawPm25,
            pm10 = rawPm10,
            no2 = no2,
            so2 = so2,
            coMg = coMg,
            o3 = o3
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Verified CPCB CAAQM Continuous Ambient Air Quality Monitoring Network
// ─────────────────────────────────────────────────────────────────
object CpcbMonitoringDirectory {
    data class CaaqmStation(
        val name: String,
        val city: String,
        val lat: Double,
        val lon: Double,
        val board: String = "CPCB"
    )

    val STATIONS = listOf(
        // Delhi NCR (DPCC / CPCB)
        CaaqmStation("Mandir Marg CAAQM", "New Delhi", 28.6360, 77.2150, "DPCC / CPCB"),
        CaaqmStation("Anand Vihar CAAQM", "Delhi", 28.6470, 77.3150, "DPCC / CPCB"),
        CaaqmStation("ITO CAAQM", "Delhi", 28.6310, 77.2490, "DPCC / CPCB"),
        CaaqmStation("Vasundhara CAAQM", "Ghaziabad", 28.6650, 77.3600, "UPPCB / CPCB"),
        CaaqmStation("Sanjay Nagar CAAQM", "Ghaziabad", 28.6920, 77.4530, "UPPCB / CPCB"),
        CaaqmStation("Sector 62 CAAQM", "Noida", 28.6250, 77.3650, "UPPCB / CPCB"),
        // Maharashtra (MPCB / SAFAR)
        CaaqmStation("Mazgaon CAAQM", "Mumbai", 18.9660, 72.8420, "SAFAR / MPCB"),
        CaaqmStation("Worli CAAQM", "Mumbai", 19.0160, 72.8170, "MPCB"),
        CaaqmStation("Bandra Kurla CAAQM", "Mumbai", 19.0620, 72.8650, "MPCB"),
        CaaqmStation("Colaba CAAQM", "Mumbai", 18.9067, 72.8147, "MPCB"),
        CaaqmStation("Shivajinagar CAAQM", "Pune", 18.5300, 73.8500, "MPCB / SAFAR"),
        CaaqmStation("Civil Lines CAAQM", "Nagpur", 21.1500, 79.0800, "MPCB"),
        // Gujarat (GPCB / CPCB)
        CaaqmStation("Alkapuri CAAQM", "Vadodara", 22.3150, 73.1700, "GPCB"),
        CaaqmStation("Dandia Bazar CAAQM", "Vadodara", 22.2985, 73.1950, "GPCB"),
        CaaqmStation("Aji Dam / Kasturba Rd CAAQM", "Rajkot", 22.2850, 70.8010, "GPCB"),
        CaaqmStation("Maninagar CAAQM", "Ahmedabad", 22.9970, 72.6030, "GPCB"),
        CaaqmStation("Chandkheda CAAQM", "Ahmedabad", 23.1120, 72.5850, "GPCB"),
        CaaqmStation("Central Zone CAAQM", "Surat", 21.1950, 72.8300, "GPCB"),
        // Uttar Pradesh (UPPCB / CPCB)
        CaaqmStation("Mathura Refinery CAAQM", "Mathura", 27.4920, 77.6730, "UPPCB"),
        CaaqmStation("Sanjay Palace CAAQM", "Agra", 27.1990, 78.0030, "UPPCB"),
        CaaqmStation("Shastripuram CAAQM", "Agra", 27.2100, 77.9400, "UPPCB"),
        CaaqmStation("Vibhuti Nagar CAAQM", "Firozabad", 27.1500, 78.3950, "UPPCB"),
        CaaqmStation("Civil Lines CAAQM", "Aligarh", 27.9100, 78.0700, "UPPCB"),
        CaaqmStation("Nehru Nagar CAAQM", "Kanpur", 26.4710, 80.3180, "UPPCB"),
        CaaqmStation("Kidwai Nagar CAAQM", "Kanpur", 26.4350, 80.3340, "UPPCB"),
        CaaqmStation("Civil Lines CAAQM", "Prayagraj", 25.4520, 81.8340, "UPPCB"),
        CaaqmStation("Jhunsi CAAQM", "Prayagraj", 25.4350, 81.9050, "UPPCB"),
        CaaqmStation("BHU Varanasi CAAQM", "Varanasi", 25.2677, 82.9913, "UPPCB"),
        CaaqmStation("Ardhali Bazar CAAQM", "Varanasi", 25.3450, 82.9810, "UPPCB"),
        CaaqmStation("Lalbagh CAAQM", "Lucknow", 26.8467, 80.9462, "UPPCB"),
        CaaqmStation("Gomti Nagar CAAQM", "Lucknow", 26.8520, 81.0020, "UPPCB"),
        CaaqmStation("Civil Lines CAAQM", "Bareilly", 28.3670, 79.4300, "UPPCB"),
        CaaqmStation("Transport Nagar CAAQM", "Moradabad", 28.8250, 78.7700, "UPPCB"),
        CaaqmStation("Mughalsarai CAAQM", "Pt. Deen Dayal Upadhyaya", 25.2818, 83.1189, "UPPCB"),
        // Madhya Pradesh (MPPCB / CPCB)
        CaaqmStation("Bundelkhand Univ CAAQM", "Jhansi", 25.4500, 78.5600, "MPPCB"),
        CaaqmStation("Phoolbagh CAAQM", "Gwalior", 26.2120, 78.1770, "MPPCB"),
        CaaqmStation("City Center CAAQM", "Gwalior", 26.2000, 78.1900, "MPPCB"),
        CaaqmStation("Paryavaran Parisar CAAQM", "Bhopal", 23.2300, 77.4300, "MPPCB"),
        CaaqmStation("T.T. Nagar CAAQM", "Bhopal", 23.2350, 77.4010, "MPPCB"),
        CaaqmStation("Chhoti Gwaltoli CAAQM", "Indore", 22.7190, 75.8710, "MPPCB"),
        CaaqmStation("Mahanagar CAAQM", "Jabalpur", 23.1670, 79.9520, "MPPCB"),
        CaaqmStation("Mahakal CAAQM", "Ujjain", 23.1820, 75.7770, "MPPCB"),
        // Rajasthan (RSPCB / CPCB)
        CaaqmStation("Adarsh Nagar CAAQM", "Jaipur", 26.9030, 75.8260, "RSPCB"),
        CaaqmStation("Police Commissionerate CAAQM", "Jaipur", 26.9180, 75.8010, "RSPCB"),
        CaaqmStation("Civil Lines CAAQM", "Ajmer", 26.4710, 74.6390, "RSPCB"),
        CaaqmStation("Collectorate CAAQM", "Jodhpur", 26.2890, 73.0250, "RSPCB"),
        CaaqmStation("Shrinath Puram CAAQM", "Kota", 25.1480, 75.8260, "RSPCB"),
        // Bihar, Jharkhand, Bengal (BSPCB / JSPCB / WBPCB)
        CaaqmStation("Muradpur CAAQM", "Patna", 25.6180, 85.1550, "BSPCB"),
        CaaqmStation("IGIMS CAAQM", "Patna", 25.6120, 85.0890, "BSPCB"),
        CaaqmStation("Doranda CAAQM", "Ranchi", 23.3370, 85.3210, "JSPCB"),
        CaaqmStation("Dhanbad CAAQM", "Dhanbad", 23.7950, 86.4300, "JSPCB"),
        CaaqmStation("Victoria Memorial CAAQM", "Kolkata", 22.5448, 88.3426, "WBPCB"),
        CaaqmStation("Rabindra Bharati CAAQM", "Kolkata", 22.5850, 88.3750, "WBPCB"),
        CaaqmStation("Belur CAAQM", "Howrah", 22.6320, 88.3540, "WBPCB"),
        // South (KSPCB / TNPCB / TSPCB)
        CaaqmStation("BTM Layout CAAQM", "Bengaluru", 12.9150, 77.6100, "KSPCB"),
        CaaqmStation("Alandur Bus Depot CAAQM", "Chennai", 13.0030, 80.2010, "TNPCB"),
        CaaqmStation("Bollaram Industrial CAAQM", "Hyderabad", 17.5500, 78.3300, "TSPCB")
    )

    data class ProximityResult(
        val station: CaaqmStation,
        val distanceKm: Double,
        val isDirect: Boolean,
        val displayName: String
    )

    fun findNearest(lat: Double, lon: Double): ProximityResult {
        var minD = Double.MAX_VALUE
        var closest = STATIONS.first()
        for (s in STATIONS) {
            val d = LandmarkService.distanceKm(lat, lon, s.lat, s.lon)
            if (d < minD) {
                minD = d
                closest = s
            }
        }
        val isDirect = minD <= 12.0
        val label = if (isDirect) {
            "${closest.board}: ${closest.name} (${closest.city}) · Direct ${minD.formatDecimals(1)} km"
        } else if (minD <= 80.0) {
            "Nearest Station: ${closest.name} (${closest.board}) · ${minD.formatDecimals(1)} km away"
        } else {
            "Copernicus CAMS Surface Grid (25km Modelled Reanalysis)"
        }
        return ProximityResult(closest, minD, isDirect, label)
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
            val aqiResult = aqiDeferred.await()
            val aqi = aqiResult.data
            val aqiFetchSucceeded = aqiResult.fetchSucceeded

            // ── CPCB NAQI sub-index engine ───────────────────────────────────
            // Only compute AQI if the API call actually succeeded AND at least one
            // pollutant reading is non-zero (guards against fake 1 from all-zero inputs).
            val hasPollutantData = aqiFetchSucceeded &&
                listOfNotNull(aqi.pm25, aqi.pm10, aqi.no2, aqi.so2).any { it > 0.0 }

            val cpcbResult = if (hasPollutantData) {
                CpcbAqiCalculator.calculate(
                    rawPm25 = aqi.pm25 ?: 0.0,
                    rawPm10 = aqi.pm10 ?: 0.0,
                    rawDust = aqi.dust ?: 0.0,
                    no2     = aqi.no2  ?: 0.0,
                    so2     = aqi.so2  ?: 0.0,
                    coUg    = aqi.co   ?: 0.0,
                    o3      = aqi.o3   ?: 0.0,
                    lat     = latitude,
                    lon     = longitude
                )
            } else null

            // ── Verified CPCB Proximity & Monitoring Attribution ────────────
            val proximity = CpcbMonitoringDirectory.findNearest(latitude, longitude)
            val monitoringStation = proximity.displayName
            val isDirectStation = proximity.isDirect
            val stationDist = proximity.distanceKm

            // ── Last-updated time & Observation Timestamp in IST ────────────
            // Open-Meteo ISO 8601: "2026-11-09T22:30" → extract "22:30"
            val apiTimeRaw = aqi.time ?: weather.rawTime
            val lastUpdatedTime = apiTimeRaw
                ?.substringAfter("T", missingDelimiterValue = "")
                ?.take(5)
                ?.ifBlank { "" } ?: ""

            StationWeatherData(
                stationName              = stationName,
                temperature              = weather.temperature,
                apparentTemperature      = weather.apparentTemperature,
                condition                = weather.condition,
                conditionIcon            = weather.icon,
                windKmh                  = weather.windKmh,
                windDirectionDeg         = weather.windDirectionDeg,
                windGustsKmh             = weather.windGustsKmh,
                dewPointC                = weather.dewPointC,
                precipitationMm          = weather.precipitationMm,
                humidityPercent          = weather.humidityPercent,
                visibilityKm             = weather.visibilityKm,
                rainProbability          = weather.rainProbability,
                cloudCoverPercent        = weather.cloudCoverPercent,
                // AQI: null when data is genuinely unavailable — never fabricate
                cpcbAqi                  = cpcbResult?.aqi,
                usAqi                    = null,          // never displayed; CPCB only
                pm25                     = if (hasPollutantData) aqi.pm25 ?: cpcbResult?.pm25 else null,
                pm10                     = if (hasPollutantData) aqi.pm10 ?: cpcbResult?.pm10 else null,
                no2                      = if (hasPollutantData) aqi.no2 else null,
                so2                      = if (hasPollutantData) aqi.so2 else null,
                co                       = if (hasPollutantData) cpcbResult?.coMg else null,
                o3                       = if (hasPollutantData) aqi.o3 else null,
                dominantPollutant        = cpcbResult?.dominantPollutant,
                aqiStandard              = "CPCB NAQI (India)",
                lastUpdatedTime          = lastUpdatedTime,
                monitoringStation        = monitoringStation,
                statePollutionBoard      = proximity.station.board,
                isModelledEstimate       = !isDirectStation,
                isDirectStation          = isDirectStation,
                nearestStationDistanceKm = stationDist,
                isStaleData              = false,
                weatherFetchOk           = weather.fetchOk,
                aqiFetchOk               = aqiFetchSucceeded && hasPollutantData
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
                parameter("current", "temperature_2m,apparent_temperature,relative_humidity_2m,dew_point_2m,precipitation,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m,visibility,cloud_cover")
                parameter("hourly", "precipitation_probability")
                parameter("forecast_hours", 1)
                parameter("timezone", "Asia/Kolkata")
            }.body()

            val cur = response.current
            val (cond, icon) = weatherCodeToText(cur?.weatherCode ?: 1)
            val precipProb = response.hourly?.precipitationProbability?.firstOrNull() ?: 0

            val temp = cur?.temperature ?: 28.0
            val rh = (cur?.humidity ?: 60).coerceIn(1, 100)
            val calculatedDewPoint = cur?.dewPoint ?: run {
                val a = 17.625
                val b = 243.04
                val alpha = kotlin.math.ln(rh / 100.0) + (a * temp) / (b + temp)
                (b * alpha) / (a - alpha)
            }

            StationWeather(
                stationName         = stationName,
                temperature         = temp,
                apparentTemperature = cur?.apparentTemperature ?: temp,
                condition           = cond,
                icon                = icon,
                windKmh             = cur?.windSpeed ?: 12.0,
                windDirectionDeg    = cur?.windDirection ?: 0,
                windGustsKmh        = cur?.windGusts ?: (cur?.windSpeed ?: 12.0),
                dewPointC           = calculatedDewPoint,
                precipitationMm     = cur?.precipitation ?: 0.0,
                humidityPercent     = rh,
                visibilityKm        = (cur?.visibilityMeters ?: 10000.0) / 1000.0,
                rainProbability     = precipProb,
                cloudCoverPercent   = cur?.cloudCover ?: 0,
                rawTime             = cur?.time,
                fetchOk             = true
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

    private suspend fun fetchAqi(lat: Double, lon: Double): AqiFetchResult {
        return try {
            val response: OpenMeteoAirQualityResponse = client.get("https://air-quality-api.open-meteo.com/v1/air-quality") {
                parameter("latitude", lat)
                parameter("longitude", lon)
                // CPCB NAQI standard pollutants + IST timezone
                parameter("current", "pm2_5,pm10,dust,nitrogen_dioxide,sulphur_dioxide,carbon_monoxide,ozone")
                parameter("timezone", "Asia/Kolkata")
            }.body()
            val current = response.current
            if (current != null) {
                AqiFetchResult(data = current, fetchSucceeded = true)
            } else {
                AqiFetchResult(data = OpenMeteoAqiCurrent(), fetchSucceeded = false)
            }
        } catch (_: Exception) {
            // Network / parse failure — signal unavailability; never compute AQI from zeros
            AqiFetchResult(data = OpenMeteoAqiCurrent(), fetchSucceeded = false)
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
        val dLat = PI / 180 * (lat2 - lat1)
        val dLon = PI / 180 * (lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(PI / 180 * lat1) * cos(PI / 180 * lat2) * sin(dLon / 2).pow(2)
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
