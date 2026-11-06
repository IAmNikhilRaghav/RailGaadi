package com.railgaadi.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.model.Station
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.*
import com.railgaadi.app.ui.components.MetricBox
import com.railgaadi.app.ui.components.NativeMapView

@Composable
fun LiveMapScreen() {
    var isNavigationMode by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var is3DMode by remember { mutableStateOf(false) }
    var followTrain by remember { mutableStateOf(false) }
    var compassBearing by remember { mutableStateOf(0f) }

    val activeTrain = TrainTrackerState.activeTrain
    val liveStatus = TrainTrackerState.liveStatus

    // Station list dynamically obtained from liveStatus
    val trainStations = remember(liveStatus) {
        liveStatus?.stations?.takeIf { it.isNotEmpty() } ?: listOf(
            Station("LGNH", "Lalgarh Jn", 28.0559, 73.2819, scheduledDep = "10:30", scheduledArr = "—", platform = "1", haltMinutes = 0),
            Station("BKN", "Bikaner Jn", 28.0180, 73.3168, scheduledDep = "10:55", scheduledArr = "10:45", platform = "2", haltMinutes = 10),
            Station("JP", "Jaipur Jn", 26.9196, 75.7878, scheduledDep = "15:20", scheduledArr = "15:10", platform = "4", haltMinutes = 10),
            Station("AF", "Agra Fort", 27.1767, 78.0081, scheduledDep = "19:25", scheduledArr = "19:15", platform = "1", haltMinutes = 10),
            Station("ALJN", "Aligarh Jn", 27.8974, 78.0880, scheduledDep = "20:15", scheduledArr = "20:10", platform = "2", haltMinutes = 5),
            Station("CNB", "Kanpur Central", 26.4547, 80.3507, scheduledDep = "00:45", scheduledArr = "00:35", platform = "7", haltMinutes = 10),
            Station("PRYJ", "Prayagraj Jn", 25.4358, 81.8463, scheduledDep = "—", scheduledArr = "04:45", platform = "2", haltMinutes = 0)
        )
    }

    var selectedStation by remember(trainStations) {
        mutableStateOf(trainStations.getOrNull(1) ?: trainStations.first())
    }
    var showStationSheet by remember { mutableStateOf(false) }

    // Navigation telemetry
    var navDistanceKm by remember { mutableStateOf("13 km") }
    var navDurationMin by remember { mutableStateOf(25) }
    var navEtaTime by remember { mutableStateOf("6:04 PM") }
    var navInstruction by remember { mutableStateOf("Head northwest") }
    var navIcon by remember { mutableStateOf("⬆") }
    var selectedAlternativeRoute by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 1. FULLSCREEN NATIVE MAP (Syncs with live status and active train)
        NativeMapView(
            modifier = Modifier.fillMaxSize(),
            isNavigationMode = isNavigationMode,
            userLat = 27.8974,
            userLon = 78.0880,
            destLat = selectedStation.latitude,
            destLon = selectedStation.longitude,
            stationName = selectedStation.name,
            trainNumber = activeTrain.number,
            trainProgress = liveStatus?.progressPercent ?: 0.65f,
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

        // 2. TOP INTERACTIVE SEARCH BAR & DROPDOWN (Phase 2 Feature)
        if (!isNavigationMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Interactive Search Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔍", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                            TextField(
                                value = TrainTrackerState.searchQuery,
                                onValueChange = { TrainTrackerState.onQueryChanged(it) },
                                placeholder = {
                                    Text(
                                        I18n.t("search_placeholder"),
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier.weight(1f)
                            )

                            if (TrainTrackerState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { TrainTrackerState.onQueryChanged("") }) {
                                    Text("✕", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { isNavigationMode = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF044E42)),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("🧭 Go", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        // Recent Searches Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            items(TrainTrackerState.recentSearches) { trainNo ->
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (activeTrain.number == trainNo) RailBluePrimary.copy(alpha = 0.15f) else Color(0xFFF1F5F9),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { TrainTrackerState.selectByNumber(trainNo) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "🚆 $trainNo",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeTrain.number == trainNo) RailBluePrimary else Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }

                // Live Search Dropdown Suggestions
                AnimatedVisibility(
                    visible = TrainTrackerState.isSearchDropdownOpen && TrainTrackerState.searchResults.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .heightIn(max = 260.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(TrainTrackerState.searchResults) { train ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { TrainTrackerState.selectTrain(train) }
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                train.number,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp,
                                                color = RailBluePrimary
                                            )
                                            Text(
                                                train.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Text(
                                            "${train.origin} → ${train.destination}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(train.type, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFE2E8F0))
                            }
                        }
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

            FloatingRoundButton(
                label = if (is3DMode) "2D" else "3D",
                isPill = true,
                isActive = is3DMode,
                onClick = { is3DMode = !is3DMode }
            )

            FloatingRoundButton(
                label = "🎥",
                isActive = followTrain,
                onClick = { followTrain = !followTrain }
            )

            FloatingRoundButton(
                label = "🎯",
                onClick = {
                    selectedStation = trainStations.first()
                    isNavigationMode = true
                }
            )

            if (isNavigationMode) {
                FloatingRoundButton(
                    label = if (isMuted) "🔇" else "🔊",
                    onClick = { isMuted = !isMuted }
                )

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

        // 4. BOTTOM HERO TELEMETRY CARD (Updates dynamically with activeTrain & liveStatus)
        if (!isNavigationMode) {
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
                                Text(activeTrain.number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(
                                activeTrain.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        val isDelayed = (liveStatus?.delayMinutes ?: 0) > 0
                        Box(
                            modifier = Modifier
                                .background(if (isDelayed) Color(0xFFFEF3C7) else Color(0xFFDCFCE7), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                if (isDelayed) "${liveStatus?.delayMinutes}m LATE" else I18n.t("status_on_time"),
                                color = if (isDelayed) Color(0xFFB45309) else RailGreenOnTime,
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
                        MetricBox(title = I18n.t("speed"), value = "${liveStatus?.speedKmh ?: 112} km/h", icon = "⚡")
                        MetricBox(title = I18n.t("platform"), value = "PF ${selectedStation.platform}", icon = "🚉")
                        MetricBox(title = "Delay", value = "${liveStatus?.delayMinutes ?: 0} min", icon = "⏱️")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Progress Bar
                    LinearProgressIndicator(
                        progress = { liveStatus?.progressPercent ?: 0.68f },
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
            // NAVIGATION MODE: Bottom Sheet
            Card(
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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

        // 5. STATION POPUP MODAL
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
