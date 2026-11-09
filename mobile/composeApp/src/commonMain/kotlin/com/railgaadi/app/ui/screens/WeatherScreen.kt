package com.railgaadi.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.railgaadi.app.ui.components.LiveAutoRefreshPill
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
            TrainTrackerState.refreshWeather(silent = false)
            TrainTrackerState.startAutoRefresh(10)
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiveAutoRefreshPill(
                        countdown = TrainTrackerState.autoRefreshCountdown,
                        isRefreshing = TrainTrackerState.isAutoRefreshing,
                        onForceRefresh = { TrainTrackerState.triggerManualRefresh() }
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RailBluePrimary.copy(alpha = 0.1f))
                            .clickable { TrainTrackerState.triggerManualRefresh() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (TrainTrackerState.isAutoRefreshing) "⏳" else "🔄", fontSize = 11.sp)
                            Text("Refresh", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)
                        }
                    }
                }
            }

            // ── Route Corridor Station Selector ───────────────────
            val routeStations = liveStatus?.stations?.filter { it.haltMinutes > 0 || it.name.contains(liveStatus?.currentStation ?: "___", ignoreCase = true) } ?: emptyList()
            if (routeStations.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Corridor Stations (Tap to view local Weather & AQI)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isLiveActive = TrainTrackerState.selectedWeatherStationName == null
                            item {
                                val liveStnName = liveStatus?.currentStation?.substringBefore(" (") ?: "Current"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isLiveActive) RailGreenOnTime else surface)
                                        .border(1.dp, if (isLiveActive) RailGreenOnTime else border, RoundedCornerShape(20.dp))
                                        .clickable { TrainTrackerState.selectWeatherStation(null) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "● Live: $liveStnName",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLiveActive) Color.White else textPrimary
                                    )
                                }
                            }

                            items(routeStations.filter { !it.name.contains(liveStatus?.currentStation?.substringBefore(" (") ?: "___", ignoreCase = true) }) { stn ->
                                val isSelected = TrainTrackerState.selectedWeatherStationName?.equals(stn.name, ignoreCase = true) == true
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isSelected) RailBluePrimary else surface)
                                        .border(1.dp, if (isSelected) RailBluePrimary else border, RoundedCornerShape(20.dp))
                                        .clickable { TrainTrackerState.selectWeatherStation(stn.name) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = stn.name,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else textPrimary
                                    )
                                }
                            }
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
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🌡️ Live Weather", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                    Text(
                                        text = when {
                                            TrainTrackerState.isAutoRefreshing -> "● UPDATING…"
                                            w.lastUpdatedTime.isNotBlank()     -> "● Updated ${w.lastUpdatedTime}"
                                            else                               -> "● LIVE · ${TrainTrackerState.autoRefreshCountdown}s"
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (TrainTrackerState.isAutoRefreshing) RailBluePrimary else RailGreenOnTime
                                    )
                                }
                                Text(w.stationName, fontSize = 11.sp, color = textSecondary)
                            }
                            Text(w.conditionIcon, fontSize = 36.sp)
                        }

                        if (!w.weatherFetchOk) {
                            // Weather fetch failed — show honest error, not fake data
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "⚠️ Weather data temporarily unavailable. Tap Refresh to retry.",
                                    fontSize = 11.sp,
                                    color = RailAmberWarning,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 15.sp
                                )
                            }
                        } else {
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
                                // Rain probability comes from hourly block; cloud cover from current
                                WeatherMetricChip("🌧️", "Rain", "${w.rainProbability}%", textPrimary, textSecondary)
                            }
                        }

                        Text(
                            if (w.lastUpdatedTime.isNotBlank())
                                "Source: Open-Meteo (ECMWF) · Updated ${w.lastUpdatedTime}"
                            else
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
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("💨 Air Quality Index", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text(
                                    text = when {
                                        TrainTrackerState.isAutoRefreshing -> "● UPDATING…"
                                        w.lastUpdatedTime.isNotBlank()     -> "● Updated ${w.lastUpdatedTime}"
                                        else                               -> "● LIVE · ${TrainTrackerState.autoRefreshCountdown}s"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (TrainTrackerState.isAutoRefreshing) RailBluePrimary else RailGreenOnTime
                                )
                            }
                            if (w.aqiValue != null) {
                                AqiBadge(w.aqiCategory)
                            }
                        }

                        if (!w.aqiFetchOk || w.aqiValue == null) {
                            // AQI unavailable — be explicit, never show fabricated data
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("AQI data unavailable for this location", fontSize = 12.sp, color = textSecondary)
                                    Text(
                                        "No CPCB station data or modelled estimate available.",
                                        fontSize = 9.sp,
                                        color = textSecondary.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
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
                                    Text(w.aqiStandard, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = textSecondary)
                                    Text(
                                        "${w.aqiCategory.emoji} ${w.aqiCategory.label}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(w.aqiCategory.colorHex)
                                    )
                                }
                            }

                            // AQI scale bar (CPCB 6-tier: 0-50, 51-100, 101-200, 201-300, 301-400, 401-500)
                            AqiScaleBar(w.aqiValue ?: 0, isDark)

                            HorizontalDivider(color = border.copy(alpha = 0.5f), thickness = 0.5.dp)

                            // Pollutant breakdown
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                if (w.pm25 != null) PollutantChip("PM2.5", "${"%.1f".format(w.pm25)} µg/m³", textPrimary, textSecondary)
                                if (w.pm10 != null) PollutantChip("PM10", "${"%.0f".format(w.pm10)} µg/m³", textPrimary, textSecondary)
                                if (w.no2 != null) PollutantChip("NO₂", "${"%.1f".format(w.no2)} µg/m³", textPrimary, textSecondary)
                                if (w.so2 != null) PollutantChip("SO₂", "${"%.1f".format(w.so2)} µg/m³", textPrimary, textSecondary)
                            }

                            // Source + monitoring station transparency row
                            Text(
                                "Source: CPCB NAQI Standard · Copernicus CAMS via Open-Meteo",
                                fontSize = 9.sp, color = textSecondary.copy(alpha = 0.6f)
                            )

                            // Monitoring station type tag + last-updated time
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val tagColor = if (w.isDirectStation) RailGreenOnTime else RailAmberWarning
                                Box(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(tagColor.copy(alpha = 0.08f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = w.monitoringStation,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = tagColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                if (w.lastUpdatedTime.isNotBlank()) {
                                    Text(
                                        text = "Observed ${w.lastUpdatedTime}",
                                        fontSize = 8.sp,
                                        color = textSecondary.copy(alpha = 0.65f)
                                    )
                                }
                            }

                            HorizontalDivider(color = border.copy(alpha = 0.5f), thickness = 0.5.dp)

                            // ── Collapsible Health Guidance, Travel Safety & Citizen Action ──
                            AqiAdvisoryCollapsible(
                                aqi = w.aqiValue ?: 0,
                                category = w.aqiCategory,
                                isDark = isDark,
                                textPrimary = textPrimary,
                                textSecondary = textSecondary,
                                border = border
                            )
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
        50  to Color(0xFF16A34A),   // 0-50   Good          → Green
        100 to Color(0xFF84CC16),  // 51-100 Satisfactory  → Parrot Green
        200 to Color(0xFFEAB308),  // 101-200 Moderate     → Yellow
        300 to Color(0xFFF97316),  // 201-300 Poor         → Orange
        400 to Color(0xFFEF4444),  // 301-400 Very Poor    → Red
        500 to Color(0xFF7F1D1D)   // 401-500 Severe       → Maroon
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

// ─────────────────────────────────────────────────────────────────
// Collapsible Health Guidance & Citizen Action Component
// ─────────────────────────────────────────────────────────────────

@Composable
private fun AqiAdvisoryCollapsible(
    aqi: Int,
    category: AqiCategory,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    border: Color
) {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Health, 1: Protection, 2: Green Action

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color(0xFFF8FAFC))
            .border(1.dp, border.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
    ) {
        // Toggle Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(category.colorHex).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isExpanded) "🌱" else "🛡️", fontSize = 15.sp)
                }
                Column {
                    Text(
                        text = "Health Advisory & Clean Air Action",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = if (isExpanded) "Tap to collapse" else "Consequences, travel protection & citizen tips",
                        fontSize = 10.sp,
                        color = textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(RailBluePrimary.copy(alpha = 0.1f))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = if (isExpanded) "Hide ▲" else "Guide ▼",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = RailBluePrimary
                )
            }
        }

        // Expanded Content with Animation
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HorizontalDivider(color = border.copy(alpha = 0.4f), thickness = 0.5.dp)

                // Category Selector: Health Risks | Travel Care | Clean City
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "🩺 Health Risks",
                        "🛡️ Travel Care",
                        "🌱 Clean City"
                    ).forEachIndexed { idx, title ->
                        val isSelected = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) RailBluePrimary else if (isDark) DarkSurface else Color.White
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) RailBluePrimary else border.copy(alpha = 0.6f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedTab = idx }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Tab Content View
                when (selectedTab) {
                    0 -> HealthConsequencesTab(aqi, category, textPrimary, textSecondary)
                    1 -> TravelProtectionTab(textPrimary, textSecondary)
                    2 -> CitizenInitiativesTab(textPrimary, textSecondary)
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun HealthConsequencesTab(
    aqi: Int,
    category: AqiCategory,
    textPrimary: Color,
    textSecondary: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val impactSummary = when {
            aqi > 400 -> "Hazardous: Severe respiratory and cardiovascular strain across healthy population; emergency for lung/heart patients."
            aqi > 300 -> "Very Poor: Triggers respiratory illness on prolonged exposure. Significant risk for asthma, bronchitis, or heart conditions."
            aqi > 200 -> "Poor: Causes breathing discomfort, throat burning, and eye stinging on prolonged outdoor exposure."
            aqi > 100 -> "Moderate: Breathing discomfort for sensitive individuals, children, and elderly travelers on extended platform stays."
            else -> "Good / Satisfactory: Air quality is safe for normal outdoor breathing."
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(category.colorHex).copy(alpha = 0.12f))
                .padding(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(category.emoji, fontSize = 16.sp)
                Column {
                    Text(
                        "Current Impact: ${category.label} (AQI $aqi)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(category.colorHex)
                    )
                    Text(
                        impactSummary,
                        fontSize = 10.sp,
                        color = textPrimary,
                        lineHeight = 14.sp
                    )
                }
            }
        }

        AdvisoryItem("🫁 Lungs & Airway", "Fine PM2.5 penetrates deep into alveoli and bloodstream, reducing oxygen uptake and causing persistent coughing or wheezing.", textPrimary, textSecondary)
        AdvisoryItem("👁️ Eyes & Throat", "Nitrogen dioxide (NO₂) and ozone cause stinging eyes, dry throat, and sinus congestion along rail corridors.", textPrimary, textSecondary)
        AdvisoryItem("👶 High-Risk Groups", "Children, elderly travelers, pregnant women, and asthmatics suffer 3x higher vulnerability during railway transit in smog.", textPrimary, textSecondary)
    }
}

@Composable
private fun TravelProtectionTab(
    textPrimary: Color,
    textSecondary: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdvisoryItem("😷 Wear N95 Mask", "Cloth masks don't filter PM2.5. Wear a certified N95/FFP2 mask when waiting on open platforms or at carriage doors.", textPrimary, textSecondary)
        AdvisoryItem("🪟 Pull Down Windows", "In Sleeper and General coaches, keep glass shutters down while crossing major industrial corridors and congested rail yards.", textPrimary, textSecondary)
        AdvisoryItem("💧 Regular Warm Hydration", "Sipping warm water or tea helps clear toxic particles trapped in throat mucous membranes and prevents dehydration.", textPrimary, textSecondary)
        AdvisoryItem("👁️ Saline Eye / Nasal Relief", "Carry sterile lubricating eye drops and saline nasal spray to quickly rinse smog particulate residue at station halts.", textPrimary, textSecondary)
        AdvisoryItem("🚶 Minimize Platform Strolls", "Avoid brisk walking or heavy exercise during long halts; stay inside shaded, ventilated waiting halls or AC coaches.", textPrimary, textSecondary)
    }
}

@Composable
private fun CitizenInitiativesTab(
    textPrimary: Color,
    textSecondary: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AdvisoryItem("🚫 Zero Open Burning", "Never burn dried leaves, plastic, or domestic waste. Report illegal garbage or farm fires on the official CPCB Sameer App.", textPrimary, textSecondary)
        AdvisoryItem("🚆 Champion Public Transit", "Traveling by Indian Railways and Metro rather than solo cars prevents kilograms of carbon and particulate emissions per trip.", textPrimary, textSecondary)
        AdvisoryItem("🌿 Plant Native Air Purifiers", "Grow native trees (Peepal, Neem, Banyan) in your society and keep indoor plants (Snake Plant, Areca Palm, Tulsi) at home.", textPrimary, textSecondary)
        AdvisoryItem("🚗 Vehicle PUC & Carpooling", "Keep your vehicle's PUC certification valid, maintain correct tyre pressure, and turn off your engine at railway crossings.", textPrimary, textSecondary)
        AdvisoryItem("💧 Dust Suppression", "Sprinkle water on loose soil or unpaved paths around your home/shop to prevent ambient dust from kicking up into the air.", textPrimary, textSecondary)
    }
}

@Composable
private fun AdvisoryItem(
    title: String,
    desc: String,
    textPrimary: Color,
    textSecondary: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(RailBluePrimary)
        )
        Column {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Text(desc, fontSize = 10.sp, color = textSecondary, lineHeight = 14.sp)
        }
    }
}
