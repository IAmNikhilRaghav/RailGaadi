package com.railgaadi.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.model.Station
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.*
import com.railgaadi.app.ui.components.MetricBox
import com.railgaadi.app.ui.components.NativeMapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMapScreen() {
    var isNavigationMode by remember { mutableStateOf(true) }
    var isNavigationPaused by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var is3DMode by remember { mutableStateOf(false) }
    var followTrain by remember { mutableStateOf(false) }
    var compassBearing by remember { mutableStateOf(0f) }

    // Fullscreen Map & Collapsible Tools State (Web App Parity)
    var isFullscreen by remember { mutableStateOf(false) }
    var isNormalToolsExpanded by remember { mutableStateOf(false) }
    var isFullscreenToolsCollapsed by remember { mutableStateOf(false) }
    var initializerDragX by remember { mutableStateOf(0f) }
    var initializerDragY by remember { mutableStateOf(0f) }

    // Route Preview & Google Maps Redirection State
    var showRoutePreviewSheet by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current
    val userLat = 27.8974
    val userLon = 78.0880

    val activeTrain = TrainTrackerState.activeTrain
    val liveStatus = TrainTrackerState.liveStatus

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
        // 1. FULLSCREEN NATIVE MAP (Syncs with live status, active train, and map style)
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
            mapStyle = TrainTrackerState.currentMapStyle,
            onStationClick = { station ->
                selectedStation = station
                showStationSheet = true
            },
            onNavigateToStation = { station ->
                selectedStation = station
                showRoutePreviewSheet = true
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

        // 2. TOP INTERACTIVE SEARCH BAR (Hidden when in Fullscreen mode or Navigation mode)
        AnimatedVisibility(
            visible = !isNavigationMode && !isFullscreen,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
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
                                        trainNo,
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
        }

        // NAVIGATION MODE: Top Turn Banner (Emerald Green when active, Warm Amber when paused)
        if (isNavigationMode) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNavigationPaused) Color(0xFFB45309) else Color(0xFF044E42)
                ),
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
                        Text(
                            if (isNavigationPaused) "⏸" else navIcon,
                            fontSize = 24.sp,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Column {
                        Text(
                            text = if (isNavigationPaused) "Navigation Paused" else navInstruction,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (isNavigationPaused) "Tap Resume in bottom bar to continue guidance" else "Toward ${selectedStation.name} • Pan-India Route",
                            fontSize = 12.sp,
                            color = if (isNavigationPaused) Color(0xFFFED7AA) else Color(0xFF99F6E4)
                        )
                    }
                }
            }
        }

        // 3. NORMAL VIEW MAP CONTROLS (Top-Right: Three-dots toggle button with Fullscreen button directly beneath; collapsible tools expand under three-dots)
        AnimatedVisibility(
            visible = !isFullscreen,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = if (isNavigationMode) 95.dp else 116.dp, end = 12.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Button: Three-dots toggle button
                MinimalMapToolButton(
                    isActive = isNormalToolsExpanded,
                    onClick = { isNormalToolsExpanded = !isNormalToolsExpanded }
                ) {
                    VectorThreeDotsIcon(isExpanded = isNormalToolsExpanded)
                }

                // Collapsible tool stack (expands vertically when three-dots button is tapped)
                AnimatedVisibility(
                    visible = isNormalToolsExpanded,
                    enter = fadeIn(animationSpec = tween(160)) + expandVertically(),
                    exit = fadeOut(animationSpec = tween(160)) + shrinkVertically()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Dynamic Compass
                        MinimalMapToolButton(onClick = { compassBearing = 0f }) {
                            Text("🧭", fontSize = 18.sp, modifier = Modifier.rotate(-compassBearing))
                        }

                        // 3D Perspective Toggle
                        MinimalMapToolButton(
                            label = if (is3DMode) "2D" else "3D",
                            isActive = is3DMode,
                            onClick = { is3DMode = !is3DMode }
                        )

                        // Follow Train Camera Toggle
                        MinimalMapToolButton(
                            isActive = followTrain,
                            onClick = { followTrain = !followTrain }
                        ) {
                            VectorCameraFollowIcon(followTrain)
                        }

                        // GPS Find Nearest Station / Route Preview
                        MinimalMapToolButton(
                            onClick = {
                                selectedStation = trainStations.first()
                                showRoutePreviewSheet = true
                            }
                        ) {
                            VectorGpsIcon()
                        }

                        if (isNavigationMode) {
                            // Voice Mute Toggle
                            MinimalMapToolButton(
                                label = if (isMuted) "🔇" else "🔊",
                                onClick = { isMuted = !isMuted }
                            )

                            // Alternative Routes
                            MinimalMapToolButton(
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
                }

                // Fullscreen button directly beneath three-dots / tools
                MinimalMapToolButton(
                    onClick = {
                        isFullscreen = true
                        isFullscreenToolsCollapsed = false
                    }
                ) {
                    VectorFullscreenIcon(isFullscreen = false)
                }
            }
        }

        // 4. FULLSCREEN MODE MAP CONTROLS & BLACK INITIALIZER
        AnimatedVisibility(
            visible = isFullscreen,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Black Little Initializer Tab (Top-right, draggable anywhere so user enjoys unblocked full map view)
                AnimatedVisibility(
                    visible = isFullscreenToolsCollapsed,
                    enter = fadeIn(animationSpec = tween(150)) + slideInHorizontally(initialOffsetX = { it }),
                    exit = fadeOut(animationSpec = tween(150)) + slideOutHorizontally(targetOffsetX = { it }),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 18.dp, end = 12.dp)
                        .offset { IntOffset(initializerDragX.roundToInt(), initializerDragY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                initializerDragX += dragAmount.x
                                initializerDragY += dragAmount.y
                            }
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .shadow(8.dp, RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
                            .clickable { isFullscreenToolsCollapsed = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "‹",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Fullscreen Floating Tools Stack (revealed by clicking black initializer, collapsible via top › button)
                AnimatedVisibility(
                    visible = !isFullscreenToolsCollapsed,
                    enter = fadeIn(animationSpec = tween(180)) + slideInHorizontally(initialOffsetX = { it / 2 }),
                    exit = fadeOut(animationSpec = tween(180)) + slideOutHorizontally(targetOffsetX = { it / 2 }),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 18.dp, end = 12.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Collapse into black initializer tab
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .shadow(4.dp, RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .clickable { isFullscreenToolsCollapsed = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("›", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        // Exit Fullscreen Mode button
                        MinimalMapToolButton(
                            isActive = true,
                            onClick = {
                                isFullscreen = false
                                isFullscreenToolsCollapsed = false
                            }
                        ) {
                            VectorFullscreenIcon(isFullscreen = true)
                        }

                        // Compass
                        MinimalMapToolButton(onClick = { compassBearing = 0f }) {
                            Text("🧭", fontSize = 18.sp, modifier = Modifier.rotate(-compassBearing))
                        }

                        // 3D Perspective
                        MinimalMapToolButton(
                            label = if (is3DMode) "2D" else "3D",
                            isActive = is3DMode,
                            onClick = { is3DMode = !is3DMode }
                        )

                        // Follow Train Camera
                        MinimalMapToolButton(
                            isActive = followTrain,
                            onClick = { followTrain = !followTrain }
                        ) {
                            VectorCameraFollowIcon(followTrain)
                        }

                        // GPS Station / Route Preview
                        MinimalMapToolButton(
                            onClick = {
                                selectedStation = trainStations.first()
                                showRoutePreviewSheet = true
                            }
                        ) {
                            VectorGpsIcon()
                        }

                        if (isNavigationMode) {
                            // Voice Mute Toggle
                            MinimalMapToolButton(
                                label = if (isMuted) "🔇" else "🔊",
                                onClick = { isMuted = !isMuted }
                            )

                            // Alternative Routes
                            MinimalMapToolButton(
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
                }
            }
        }

        // 5. BOTTOM OVERLAYS (Hidden when in Fullscreen mode for immersive viewing)
        AnimatedVisibility(
            visible = !isFullscreen,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            if (!isNavigationMode) {
                // NORMAL MODE: Hero Train Telemetry Card
                Card(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier.fillMaxWidth()
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

                        // Action Button: Preview & Start Turn-by-Turn Navigation
                        Button(
                            onClick = { showRoutePreviewSheet = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF044E42))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🧭", fontSize = 16.sp)
                                Text(
                                    "Directions to ${selectedStation.name}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                // NAVIGATION MODE: Bottom Sheet with Pause & Stop controls
                Card(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                    modifier = Modifier.fillMaxWidth()
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
                                    text = if (isNavigationPaused) "Paused" else "$navDurationMin min",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isNavigationPaused) Color(0xFFB45309) else Color(0xFF044E42)
                                )
                                Text(
                                    text = "$navDistanceKm • $navEtaTime",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Gray
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Voice mute toggle
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFFF1F5F9), CircleShape)
                                        .clickable { isMuted = !isMuted },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (isMuted) "🔇" else "🔊", fontSize = 16.sp)
                                }

                                // Navigation Pause / Resume Button
                                Button(
                                    onClick = { isNavigationPaused = !isNavigationPaused },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isNavigationPaused) Color(0xFF044E42) else Color(0xFFF1F5F9),
                                        contentColor = if (isNavigationPaused) Color.White else Color(0xFF0F172A)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        if (isNavigationPaused) "▶ Resume" else "⏸ Pause",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                // Navigation Stop / Exit Button (Web App Parity: #fee2e2 / #dc2626)
                                Button(
                                    onClick = {
                                        isNavigationMode = false
                                        isNavigationPaused = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2)),
                                    border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("✕ Stop", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFDC2626))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. STATION POPUP MODAL
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
                            showStationSheet = false
                            showRoutePreviewSheet = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("🧭 View Routes & Navigate", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }

        // 7. ROUTE PREVIEW & GOOGLE MAPS REDIRECTION MODAL (Web App Parity)
        if (showRoutePreviewSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRoutePreviewSheet = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Header Row: 🧭 ROUTES TO <Station Name> + Dismiss ✕
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(Color(0xFFEDE9FE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🧭", fontSize = 20.sp)
                            }
                            Column {
                                Text(
                                    "ROUTES TO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    selectedStation.name,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(onClick = { showRoutePreviewSheet = false }) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFF1F5F9), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✕", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Route Card: ⚡ Fastest (Purple theme matching Web App)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAF5FF)),
                        border = BorderStroke(1.5.dp, Color(0xFF7C3AED)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("⚡", fontSize = 16.sp)
                                    Text(
                                        "Fastest",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    "$navDurationMin min",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF6D28D9)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Recommended • $navDistanceKm",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Start in-app navigation button
                            Button(
                                onClick = {
                                    isNavigationMode = true
                                    isNavigationPaused = false
                                    showRoutePreviewSheet = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.align(Alignment.End),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    "▶ Start (Fastest)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Actions Row: Open Google Maps & Cancel
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                val gmapsUrl = "https://www.google.com/maps/dir/?api=1&origin=$userLat,$userLon&destination=${selectedStation.latitude},${selectedStation.longitude}&travelmode=driving"
                                uriHandler.openUri(gmapsUrl)
                                showRoutePreviewSheet = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("↗", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text(
                                    "Open Google Maps",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Button(
                            onClick = { showRoutePreviewSheet = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(46.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text("✕ Cancel", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF475569))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun MinimalMapToolButton(
    modifier: Modifier = Modifier,
    label: String? = null,
    isActive: Boolean = false,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .shadow(3.dp, RoundedCornerShape(11.dp))
            .background(if (isActive) RailBluePrimary else MaterialTheme.colorScheme.surface, RoundedCornerShape(11.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(11.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (content != null) {
            content()
        } else if (label != null) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   Minimalist Vector Map Icons
   ───────────────────────────────────────────────────────────── */

@Composable
fun VectorThreeDotsIcon(isExpanded: Boolean) {
    if (isExpanded) {
        Canvas(modifier = Modifier.size(16.dp)) {
            val stroke = 2.2f
            val color = Color.White
            val pad = size.width * 0.22f
            drawLine(color, Offset(pad, pad), Offset(size.width - pad, size.height - pad), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(size.width - pad, pad), Offset(pad, size.height - pad), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    } else {
        Canvas(modifier = Modifier.size(16.dp)) {
            val dotRadius = 1.8.dp.toPx()
            val color = Color(0xFF0F172A)
            drawCircle(color = color, radius = dotRadius, center = Offset(size.width / 2, size.height * 0.22f))
            drawCircle(color = color, radius = dotRadius, center = Offset(size.width / 2, size.height * 0.50f))
            drawCircle(color = color, radius = dotRadius, center = Offset(size.width / 2, size.height * 0.78f))
        }
    }
}

@Composable
fun VectorFullscreenIcon(isFullscreen: Boolean) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val color = if (isFullscreen) Color.White else Color(0xFF0F172A)
        val stroke = 1.8f
        val arm = size.width * 0.32f

        if (!isFullscreen) {
            // Expand corners
            drawLine(color, Offset(0f, 0f), Offset(arm, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(0f, 0f), Offset(0f, arm), strokeWidth = stroke, cap = StrokeCap.Round)

            drawLine(color, Offset(size.width, 0f), Offset(size.width - arm, 0f), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(size.width, 0f), Offset(size.width, arm), strokeWidth = stroke, cap = StrokeCap.Round)

            drawLine(color, Offset(0f, size.height), Offset(arm, size.height), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(0f, size.height), Offset(0f, size.height - arm), strokeWidth = stroke, cap = StrokeCap.Round)

            drawLine(color, Offset(size.width, size.height), Offset(size.width - arm, size.height), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(size.width, size.height), Offset(size.width, size.height - arm), strokeWidth = stroke, cap = StrokeCap.Round)
        } else {
            // Collapse corners
            drawLine(color, Offset(arm, 0f), Offset(arm, arm), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(0f, arm), Offset(arm, arm), strokeWidth = stroke, cap = StrokeCap.Round)

            drawLine(color, Offset(size.width - arm, 0f), Offset(size.width - arm, arm), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(size.width, arm), Offset(size.width - arm, arm), strokeWidth = stroke, cap = StrokeCap.Round)

            drawLine(color, Offset(0f, size.height - arm), Offset(arm, size.height - arm), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(arm, size.height), Offset(arm, size.height - arm), strokeWidth = stroke, cap = StrokeCap.Round)

            drawLine(color, Offset(size.width, size.height - arm), Offset(size.width - arm, size.height - arm), strokeWidth = stroke, cap = StrokeCap.Round)
            drawLine(color, Offset(size.width - arm, size.height), Offset(size.width - arm, size.height - arm), strokeWidth = stroke, cap = StrokeCap.Round)
        }
    }
}

@Composable
fun VectorCameraFollowIcon(isActive: Boolean) {
    Canvas(modifier = Modifier.size(17.dp)) {
        val color = if (isActive) Color.White else Color(0xFF0F172A)
        drawCircle(color = color, radius = size.width * 0.4f, center = center, style = Stroke(width = 1.5f))
        drawCircle(color = color, radius = 2.5f, center = center)
        drawLine(color, Offset(center.x, 0f), Offset(center.x, size.height * 0.22f), strokeWidth = 1.4f)
        drawLine(color, Offset(center.x, size.height * 0.78f), Offset(center.x, size.height), strokeWidth = 1.4f)
        drawLine(color, Offset(0f, center.y), Offset(size.width * 0.22f, center.y), strokeWidth = 1.4f)
        drawLine(color, Offset(size.width * 0.78f, center.y), Offset(size.width, center.y), strokeWidth = 1.4f)
    }
}

@Composable
fun VectorGpsIcon() {
    Canvas(modifier = Modifier.size(17.dp)) {
        val color = Color(0xFF0F172A)
        drawCircle(color = color, radius = size.width * 0.28f, center = center, style = Stroke(width = 1.6f))
        drawCircle(color = color, radius = 2f, center = center)
        drawLine(color, Offset(center.x, 0f), Offset(center.x, size.height * 0.25f), strokeWidth = 1.5f)
        drawLine(color, Offset(center.x, size.height * 0.75f), Offset(center.x, size.height), strokeWidth = 1.5f)
        drawLine(color, Offset(0f, center.y), Offset(size.width * 0.25f, center.y), strokeWidth = 1.5f)
        drawLine(color, Offset(size.width * 0.75f, center.y), Offset(size.width, center.y), strokeWidth = 1.5f)
    }
}
