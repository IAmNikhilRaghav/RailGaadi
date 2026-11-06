package com.railgaadi.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.model.Station
import com.railgaadi.app.theme.*
import com.railgaadi.app.ui.components.NativeMapView

@Composable
fun LiveMapScreen() {
    var isNavigationMode by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var is3DMode by remember { mutableStateOf(false) }
    var followTrain by remember { mutableStateOf(false) }
    var compassBearing by remember { mutableStateOf(0f) }

    // Selected Station for directions or details
    var selectedStation by remember {
        mutableStateOf(
            Station("ALJN", "Aligarh Junction", 27.8974, 78.0880, scheduledDep = "12:45", scheduledArr = "12:40", platform = "2", haltMinutes = 5)
        )
    }
    var showStationSheet by remember { mutableStateOf(false) }

    // Dynamic Navigation Telemetry
    var navDistanceKm by remember { mutableStateOf("13 km") }
    var navDurationMin by remember { mutableStateOf(25) }
    var navEtaTime by remember { mutableStateOf("6:04 PM") }
    var navInstruction by remember { mutableStateOf("Head northwest") }
    var navIcon by remember { mutableStateOf("⬆") }
    var selectedAlternativeRoute by remember { mutableStateOf(0) }

    // Station list along Prayagraj Express route
    val trainStations = remember {
        listOf(
            Station("LGNH", "Lalgarh Jn", 28.0559, 73.2819, scheduledDep = "10:30", scheduledArr = "—", platform = "1", haltMinutes = 0),
            Station("BKN", "Bikaner Jn", 28.0180, 73.3168, scheduledDep = "10:55", scheduledArr = "10:45", platform = "2", haltMinutes = 10),
            Station("JP", "Jaipur Jn", 26.9196, 75.7878, scheduledDep = "15:20", scheduledArr = "15:10", platform = "4", haltMinutes = 10),
            Station("BTE", "Bharatpur Jn", 27.2152, 77.4930, scheduledDep = "18:15", scheduledArr = "18:10", platform = "3", haltMinutes = 5),
            Station("AF", "Agra Fort", 27.1767, 78.0081, scheduledDep = "19:25", scheduledArr = "19:15", platform = "1", haltMinutes = 10),
            Station("TDL", "Tundla Jn", 27.2064, 78.2415, scheduledDep = "20:40", scheduledArr = "20:35", platform = "5", haltMinutes = 5),
            Station("CNB", "Kanpur Central", 26.4547, 80.3507, scheduledDep = "00:45", scheduledArr = "00:35", platform = "7", haltMinutes = 10),
            Station("PRYJ", "Prayagraj Jn", 25.4358, 81.8463, scheduledDep = "—", scheduledArr = "04:45", platform = "2", haltMinutes = 0)
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 1. FULLSCREEN NATIVE MAP
        NativeMapView(
            modifier = Modifier.fillMaxSize(),
            isNavigationMode = isNavigationMode,
            userLat = 27.8974,
            userLon = 78.0880,
            destLat = selectedStation.latitude,
            destLon = selectedStation.longitude,
            stationName = selectedStation.name,
            trainNumber = "12404",
            trainProgress = 0.68f,
            stations = trainStations,
            is3DMode = is3DMode,
            followTrain = followTrain,
            onStationClick = { station ->
                selectedStation = station
                showStationSheet = true
            },
            onNavigateToStation = { station ->
                selectedStation = station
                isNavigationMode = true
                showStationSheet = false
            },
            onCompassRotationChanged = { bearing ->
                compassBearing = bearing
            },
            onNavStatsUpdated = { distM, durS, instruction, icon ->
                val km = distM / 1000.0
                navDistanceKm = "${(km * 10).toInt() / 10.0} km"
                navDurationMin = (durS / 60.0).toInt().coerceAtLeast(1)
                navInstruction = instruction
                navIcon = icon
            }
        )

        // 2. TOP OVERLAYS
        if (!isNavigationMode) {
            // NORMAL MODE: Top Search & Track Header Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🚆 12404 - Prayagraj SF",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Next: ${selectedStation.name} (${selectedStation.code})",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Button(
                        onClick = { isNavigationMode = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF044E42)),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("🧭 Directions", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        } else {
            // NAVIGATION MODE: Google Maps-style Top Emerald Turn-by-Turn Direction Banner
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF044E42)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Turn Arrow Indicator
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color.White.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(navIcon, fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }

                    Column {
                        Text(
                            text = navInstruction,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Toward ${selectedStation.name} • Pan-India Route",
                            fontSize = 12.sp,
                            color = Color(0xFF99F6E4)
                        )
                    }
                }
            }
        }

        // 3. RIGHT FLOATING TOOLS (3D Perspective, Compass, Rotate, GPS Nearest Station, Follow Train)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Compass with dynamic heading rotation
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .shadow(4.dp, CircleShape)
                    .background(Color.White, CircleShape)
                    .clickable { compassBearing = 0f },
                contentAlignment = Alignment.Center
            ) {
                Text("🧭", fontSize = 20.sp, modifier = Modifier.rotate(-compassBearing))
            }

            // 3D Perspective Toggle
            FloatingRoundButton(
                label = if (is3DMode) "2D" else "3D",
                isPill = true,
                isActive = is3DMode,
                onClick = { is3DMode = !is3DMode }
            )

            // Follow Train Camera Toggle
            FloatingRoundButton(
                label = "🎥",
                isActive = followTrain,
                onClick = { followTrain = !followTrain }
            )

            // GPS Find Nearest Station
            FloatingRoundButton(
                label = "🎯",
                onClick = {
                    selectedStation = trainStations.first()
                    isNavigationMode = true
                }
            )

            if (isNavigationMode) {
                // Voice Mute Toggle
                FloatingRoundButton(
                    label = if (isMuted) "🔇" else "🔊",
                    onClick = { isMuted = !isMuted }
                )

                // Alternative Routes Switcher
                FloatingRoundButton(
                    label = "🔀",
                    onClick = {
                        selectedAlternativeRoute = if (selectedAlternativeRoute == 0) 1 else 0
                        if (selectedAlternativeRoute == 1) {
                            navDistanceKm = "15.4 km"
                            navDurationMin = 29
                        } else {
                            navDistanceKm = "13 km"
                            navDurationMin = 25
                        }
                    }
                )
            }
        }

        // 4. BOTTOM OVERLAYS
        if (!isNavigationMode) {
            // NORMAL MODE: Hero Train Telemetry Card
            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("12404", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(
                                "Prayagraj SF Express",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(Color(0xFFDCFCE7), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                I18n.t("status_on_time"),
                                color = RailGreenOnTime,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBox(title = I18n.t("speed"), value = "112 km/h", icon = "⚡")
                        MetricBox(title = I18n.t("platform"), value = "PF ${selectedStation.platform}", icon = "🚉")
                        MetricBox(title = "Halt", value = "${selectedStation.haltMinutes}m", icon = "⏱️")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { 0.68f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = RailBluePrimary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Button: Launch Turn-by-Turn Directions
                    Button(
                        onClick = { isNavigationMode = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF044E42))
                    ) {
                        Text(
                            "📍 Directions to ${selectedStation.name}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // NAVIGATION MODE: Google Maps-style Bottom Sheet
            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Re-centre Pill Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { compassBearing = 0f },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("▲ Re-centre", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Text("Target: ${selectedStation.name}", fontSize = 11.sp, color = Color.Gray)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Duration & ETA
                        Column {
                            Text(
                                text = "$navDurationMin min",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFB45309)
                            )
                            Text(
                                text = "$navDistanceKm • $navEtaTime",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Gray
                            )
                        }

                        // Right action buttons: Voice & Exit Button
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(Color(0xFFF1F5F9), CircleShape)
                                    .clickable { isMuted = !isMuted },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (isMuted) "🔇" else "🔊", fontSize = 18.sp)
                            }

                            Button(
                                onClick = { isNavigationMode = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text("Exit", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // 5. STATION POPUP MODAL (When tapping any station along the route)
        if (showStationSheet) {
            ModalBottomSheet(
                onDismissRequest = { showStationSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedStation.name,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Station Code: ${selectedStation.code}",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                        Box(
                            modifier = Modifier
                                .background(RailBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("PF ${selectedStation.platform}", color = RailBluePrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBox("Scheduled Arr", selectedStation.scheduledArr, "⏱️")
                        MetricBox("Scheduled Dep", selectedStation.scheduledDep, "⏱️")
                        MetricBox("Halt Time", "${selectedStation.haltMinutes} min", "🛑")
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            isNavigationMode = true
                            showStationSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF044E42)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("🧭 Navigate to this Station", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun FloatingRoundButton(
    label: String,
    isActive: Boolean = false,
    isPill: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .shadow(4.dp, CircleShape)
            .background(if (isActive) RailBluePrimary else Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = if (isPill) 13.sp else 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) Color.White else Color(0xFF0F172A)
        )
    }
}
