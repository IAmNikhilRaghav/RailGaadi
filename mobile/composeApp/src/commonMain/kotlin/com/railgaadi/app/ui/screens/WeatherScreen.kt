package com.railgaadi.app.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.model.AqiCategory
import com.railgaadi.app.model.Landmark
import com.railgaadi.app.model.StationWeatherData
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.*
import kotlin.math.roundToInt

@Composable
fun WeatherScreen() {
    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) DarkBackground else LightBackground
    val surface = if (isDark) DarkSurface else LightSurface
    val textPrimary = if (isDark) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val border = if (isDark) DarkBorder else LightBorder

    val uriHandler = LocalUriHandler.current
    val weatherData = TrainTrackerState.weatherData
    val liveStatus = TrainTrackerState.liveStatus
    val landmarks = TrainTrackerState.nearbyLandmarks
    val isLoading = TrainTrackerState.isLoadingWeather
    val activeTrain = TrainTrackerState.activeTrain

    // Auto-refresh weather when screen is opened and train is active
    LaunchedEffect(activeTrain.number) {
        if (activeTrain.number.isNotBlank()) {
            TrainTrackerState.refreshWeather()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // ── Top Actions (clean, modern, no redundant titles) ──
        if (activeTrain.number.isNotBlank()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RailBluePrimary.copy(alpha = 0.1f))
                            .clickable { TrainTrackerState.refreshWeather() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔄", fontSize = 11.sp)
                            Text("Refresh", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)
                        }
                    }
                }
            }
        }

        // ── No Train State ───────────────────────────────────────
        if (activeTrain.number.isBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(32.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⛅", fontSize = 48.sp)
                        Text("No Train Selected", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text(
                            "Search and track a train to see live weather,\nAQI and nearby landmarks along your route.",
                            fontSize = 12.sp, color = textSecondary, textAlign = TextAlign.Center
                        )
                    }
                }
            }
            return@LazyColumn
        }

        // ── Loading shimmer ──────────────────────────────────────
        if (isLoading && weatherData == null) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = RailBluePrimary,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp)
                            )
                            Text("Fetching live weather & AQI…", fontSize = 12.sp, color = textSecondary)
                        }
                    }
                }
            }
            return@LazyColumn
        }

        val w = weatherData

        if (w != null) {
            // ── Fog / Smog Advisory Banner ───────────────────────
            if (w.isFogAdvisory || w.isSmogAdvisory) {
                item {
                    val advisoryMsg = when {
                        w.isFogAdvisory && w.isSmogAdvisory ->
                            "⚠️  Dense Fog + High Smog (AQI ${w.aqiValue ?: ""}) detected. Trains on this section may operate under fog-induced speed restrictions."
                        w.isFogAdvisory ->
                            "⚠️  Low Visibility: ${"%.1f".format(w.visibilityKm)} km. Fog-induced speed restrictions may apply on this section."
                        else ->
                            "⚠️  High Smog Alert: AQI ${w.aqiValue ?: ""} (${w.aqiCategory.label}). Avoid outdoor exposure at station stops."
                    }
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = RailAmberWarning.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("⚠️", fontSize = 20.sp)
                            Text(
                                advisoryMsg,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RailAmberWarning,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // ── Live Weather Hero Card ───────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🌡️ Live Weather", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    Text("● LIVE", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = RailGreenOnTime)
                                }
                                Text(w.stationName, fontSize = 11.sp, color = textSecondary)
                            }
                            Text(w.conditionIcon, fontSize = 36.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            Text(
                                "${w.temperature.roundToInt()}°C",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Text(w.condition, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
                                Text("Feels like ${w.apparentTemperature.roundToInt()}°C", fontSize = 11.sp, color = textSecondary)
                            }
                        }

                        HorizontalDivider(color = border.copy(alpha = 0.5f), thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            WeatherMetricChip("💧", "Humidity", "${w.humidityPercent}%", textPrimary, textSecondary)
                            WeatherMetricChip("💨", "Wind", "${w.windKmh.roundToInt()} km/h", textPrimary, textSecondary)
                            WeatherMetricChip("👁️", "Visibility", "${"%.1f".format(w.visibilityKm)} km", textPrimary, textSecondary)
                            WeatherMetricChip("🌧️", "Rain", "${w.rainProbability}%", textPrimary, textSecondary)
                        }

                        Text(
                            "Source: Open-Meteo (ECMWF) · Updated live",
                            fontSize = 9.sp, color = textSecondary.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // ── Live AQI Card ────────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💨 Air Quality Index", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("● LIVE", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = RailGreenOnTime)
                            }
                            if (w.aqiValue != null) {
                                AqiBadge(w.aqiCategory)
                            }
                        }

                        if (w.aqiValue != null) {
                            // Big AQI number
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "${w.aqiValue}",
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(w.aqiCategory.colorHex)
                                )
                                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                                    Text("National AQI (CPCB / SAFAR)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
                                    Text(
                                        "${w.aqiCategory.emoji} ${w.aqiCategory.label}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(w.aqiCategory.colorHex)
                                    )
                                }
                            }

                            // AQI scale bar (CPCB Standard: 0-50, 51-100, 101-200, 201-300, 301-400, 401-500)
                            AqiScaleBar(w.aqiValue ?: 0, isDark)

                            HorizontalDivider(color = border.copy(alpha = 0.5f), thickness = 0.5.dp)

                            // Pollutant breakdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                if (w.pm25 != null) PollutantChip("PM2.5", "${"%.1f".format(w.pm25)} µg", textPrimary, textSecondary)
                                if (w.pm10 != null) PollutantChip("PM10", "${"%.0f".format(w.pm10)} µg", textPrimary, textSecondary)
                                if (w.no2 != null) PollutantChip("NO₂", "${"%.1f".format(w.no2)} µg", textPrimary, textSecondary)
                                if (w.so2 != null) PollutantChip("SO₂", "${"%.1f".format(w.so2)} µg", textPrimary, textSecondary)
                            }

                            Text(
                                "Source: CPCB / SAFAR Standard · Open-Meteo Sensor Telemetry",
                                fontSize = 9.sp, color = textSecondary.copy(alpha = 0.6f)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("AQI data unavailable for this location", fontSize = 12.sp, color = textSecondary)
                            }
                        }
                    }
                }
            }
        }

        // ── Nearby Landmarks & Monuments ─────────────────────────
        if (landmarks.isNotEmpty()) {
            item {
                Text(
                    "🏛️ Nearby Landmarks & Monuments",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                )
            }

            items(landmarks) { landmark ->
                LandmarkCard(
                    landmark = landmark,
                    isDark = isDark,
                    onGoogleMaps = {
                        val url = "https://www.google.com/maps/search/?api=1&query=${landmark.lat},${landmark.lon}"
                        uriHandler.openUri(url)
                    }
                )
            }
        } else if (w != null && !isLoading) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("🗺️", fontSize = 32.sp)
                        Text(
                            "No Landmarks Nearby",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary
                        )
                        Text(
                            "No major landmarks found within 60 km of the current station.",
                            fontSize = 11.sp,
                            color = if (isDark) DarkTextSecondary else LightTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ── Supporting Composables ────────────────────────────────────────

@Composable
private fun WeatherMetricChip(
    icon: String,
    label: String,
    value: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(icon, fontSize = 18.sp)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text(label, fontSize = 9.sp, color = textSecondary)
    }
}

@Composable
private fun PollutantChip(label: String, value: String, textPrimary: Color, textSecondary: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text(label, fontSize = 9.sp, color = textSecondary)
    }
}

@Composable
private fun AqiBadge(category: AqiCategory) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(Color(category.colorHex).copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            "${category.emoji} ${category.label}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(category.colorHex)
        )
    }
}

@Composable
private fun AqiScaleBar(aqi: Int, isDark: Boolean) {
    val segments = listOf(
        50 to Color(0xFF16A34A),   // 0-50 Good
        100 to Color(0xFF65A30D),  // 51-100 Satisfactory
        200 to Color(0xFFD97706),  // 101-200 Moderate
        300 to Color(0xFFDC2626),  // 201-300 Poor
        400 to Color(0xFF7C3AED),  // 301-400 Very Poor
        500 to Color(0xFF92400E)   // 401-500 Severe
    )
    val maxAqi = 500
    val clampedAqi = aqi.coerceIn(0, maxAqi)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            segments.forEachIndexed { i, (limit, color) ->
                val prevLimit = if (i == 0) 0 else segments[i - 1].first
                val segWeight = (limit - prevLimit).toFloat() / maxAqi
                Box(
                    modifier = Modifier
                        .weight(segWeight)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }
        // Indicator dot
        val indicatorFraction = clampedAqi.toFloat() / maxAqi
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.weight(indicatorFraction.coerceAtLeast(0.01f)))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isDark) DarkTextPrimary else LightTextPrimary)
            )
            Spacer(modifier = Modifier.weight((1f - indicatorFraction).coerceAtLeast(0.01f)))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("0", "50", "100", "200", "300", "400", "500").forEach { label ->
                Text(label, fontSize = 8.sp, color = if (isDark) DarkTextSecondary else LightTextSecondary)
            }
        }
    }
}

@Composable
private fun LandmarkCard(
    landmark: Landmark,
    isDark: Boolean,
    onGoogleMaps: () -> Unit
) {
    val surface = if (isDark) DarkSurface else LightSurface
    val textPrimary = if (isDark) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDark) DarkTextSecondary else LightTextSecondary

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Category icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(RailBluePrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(landmark.categoryIcon, fontSize = 20.sp)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        landmark.name,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(RailBluePrimary.copy(alpha = 0.1f))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            "${"%.1f".format(landmark.distanceKm)} km",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RailBluePrimary
                        )
                    }
                }

                Text(
                    landmark.description,
                    fontSize = 11.sp,
                    color = textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isDark) DarkBackground else Color(0xFFF1F5F9))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            landmark.category.replaceFirstChar { it.uppercase() },
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )
                    }

                    // Google Maps redirect button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RailBluePrimary)
                            .clickable { onGoogleMaps() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("↗", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            Text("Open Maps", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
