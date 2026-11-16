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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.*
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.model.Station
import com.railgaadi.app.model.StationDataService
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.*
import com.railgaadi.app.ui.components.MetricBox
import com.railgaadi.app.ui.components.NativeMapView
import com.railgaadi.app.util.PlatformSpeech

// Pan-India Real-time Navigation Telemetry Calculator
fun calculateNavigationTelemetry(
    uLat: Double,
    uLon: Double,
    dLat: Double,
    dLon: Double,
    destStationName: String,
    altRouteIndex: Int = 0
): Triple<String, Int, String> {
    val destLat = if (dLat != 0.0) dLat else 28.6139
    val destLon = if (dLon != 0.0) dLon else 77.2090

    val dLatRad = (destLat - uLat) * (PI / 180.0)
    val dLonRad = (destLon - uLon) * (PI / 180.0)
    val a = sin(dLatRad / 2.0).pow(2) +
            cos(uLat * (PI / 180.0)) * cos(destLat * (PI / 180.0)) *
            sin(dLonRad / 2.0).pow(2)
    val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    val aerialKm = 6371.0 * c

    // Road curvature factor for Indian road networks (Fastest vs Alternative)
    val roadMultiplier = if (altRouteIndex == 1) 1.38 else 1.25
    val roadKm = (aerialKm * roadMultiplier).coerceAtLeast(0.4)

    // Realistic speeds across Indian roads & highways
    val speedKmh = when {
        roadKm < 8.0 -> 28.0
        roadKm < 45.0 -> 42.0
        roadKm < 150.0 -> 55.0
        else -> 70.0
    }

    val durMin = ((roadKm / speedKmh) * 60.0).roundToInt().coerceAtLeast(2)

    val distStr = when {
        roadKm < 1.0 -> "${(roadKm * 1000).roundToInt()} m"
        roadKm < 10.0 -> "${(roadKm * 10).roundToInt() / 10.0} km"
        else -> "${roadKm.roundToInt()} km"
    }

    // Direction instruction based on bearing
    val y = sin(dLonRad) * cos(destLat * (PI / 180.0))
    val x = cos(uLat * (PI / 180.0)) * sin(destLat * (PI / 180.0)) -
            sin(uLat * (PI / 180.0)) * cos(destLat * (PI / 180.0)) * cos(dLonRad)
    val bearing = (atan2(y, x) * 180.0 / PI + 360.0) % 360.0

    val compassDir = when (bearing) {
        in 22.5..67.5 -> "Northeast"
        in 67.5..112.5 -> "East"
        in 112.5..157.5 -> "Southeast"
        in 157.5..202.5 -> "South"
        in 202.5..247.5 -> "Southwest"
        in 247.5..292.5 -> "West"
        in 292.5..337.5 -> "Northwest"
        else -> "North"
    }

    val instruction = "Head $compassDir toward $destStationName"
    return Triple(distStr, durMin, instruction)
}

fun formatEtaDuration(minutes: Int): String {
    val hrs = minutes / 60
    val mins = minutes % 60
    return if (hrs > 0) "${hrs}h ${mins}m" else "$mins min"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveMapScreen() {
    var isNavigationMode by remember { mutableStateOf(false) }
    var isNavigationPaused by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var is3DMode by remember { mutableStateOf(false) }
    var followTrain by remember { mutableStateOf(false) }
    var compassBearing by remember { mutableStateOf(0f) }

    // Modern Spacious UI States: Interactive Peek Sheet & Floating Search Capsule
    var isBottomSheetExpanded by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    // Dynamic Black Initializer & Auto-Collapsing Map Controls
    var isFullscreen by remember { mutableStateOf(false) }
    var isNormalToolsExpanded by remember { mutableStateOf(true) }   // expanded by default
    var isFullscreenToolsExpanded by remember { mutableStateOf(true) } // expanded by default
    var interactionTick by remember { mutableStateOf(0) }
    var initializerDragX by remember { mutableStateOf(0f) }
    var initializerDragY by remember { mutableStateOf(0f) }

    // Fullscreen only: auto-collapse to black initializer after 3.5s inactivity
    LaunchedEffect(isFullscreenToolsExpanded, interactionTick) {
        if (isFullscreen && isFullscreenToolsExpanded) {
            delay(3500)
            isFullscreenToolsExpanded = false
        }
    }

    // Reset fullscreen tools when entering fullscreen
    LaunchedEffect(isFullscreen) {
        if (isFullscreen) {
            isFullscreenToolsExpanded = true
            interactionTick++
        }
    }

    fun pingInteraction() {
        interactionTick++
    }

    // Route Preview & Google Maps Redirection State
    var showRoutePreviewSheet by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current
    val userLat = 27.8974
    val userLon = 78.0880

    val activeTrain = TrainTrackerState.activeTrain
    val liveStatus = TrainTrackerState.liveStatus

    val trainStations = remember(liveStatus, TrainTrackerState.userBoardingStation) {
        val baseList = liveStatus?.stations?.takeIf { it.isNotEmpty() } ?: listOf(
            Station("LGNH", "Lalgarh Jn", 28.0559, 73.2819, scheduledDep = "10:30", scheduledArr = "—", platform = "1", haltMinutes = 0),
            Station("BKN", "Bikaner Jn", 28.0180, 73.3168, scheduledDep = "10:55", scheduledArr = "10:45", platform = "2", haltMinutes = 10),
            Station("JP", "Jaipur Jn", 26.9196, 75.7878, scheduledDep = "15:20", scheduledArr = "15:10", platform = "4", haltMinutes = 10),
            Station("AF", "Agra Fort", 27.1767, 78.0081, scheduledDep = "19:25", scheduledArr = "19:15", platform = "1", haltMinutes = 10),
            Station("ALJN", "Aligarh Jn", 27.8974, 78.0880, scheduledDep = "20:15", scheduledArr = "20:10", platform = "2", haltMinutes = 5),
            Station("CNB", "Kanpur Central", 26.4547, 80.3507, scheduledDep = "00:45", scheduledArr = "00:35", platform = "7", haltMinutes = 10),
            Station("PRYJ", "Prayagraj Jn", 25.4358, 81.8463, scheduledDep = "—", scheduledArr = "04:45", platform = "2", haltMinutes = 0)
        )

        val boarding = TrainTrackerState.userBoardingStation
        if (boarding != null && baseList.none { it.code.equals(boarding.code, ignoreCase = true) }) {
            listOf(boarding) + baseList
        } else {
            baseList
        }
    }

    // Navigation Target: The station exactly where the user wants to onboard and start their journey
    var selectedStation by remember(TrainTrackerState.userBoardingStation, trainStations) {
        val target = TrainTrackerState.userBoardingStation
        val matched = trainStations.find { it.code.equals(target?.code, ignoreCase = true) }
        mutableStateOf(matched ?: target ?: trainStations.firstOrNull() ?: Station("NDLS", "New Delhi", 28.6139, 77.2090))
    }

    // Sync when tracking a new train or when user changes boarding station
    LaunchedEffect(TrainTrackerState.userBoardingStation) {
        val target = TrainTrackerState.userBoardingStation
        if (target != null) {
            val matched = trainStations.find { it.code.equals(target.code, ignoreCase = true) }
            selectedStation = matched ?: target
        }
    }

    LaunchedEffect(activeTrain.number) {
        if (activeTrain.number.isNotBlank()) {
            TrainTrackerState.startAutoRefresh(10)
        }
        isNavigationMode = false
        val target = TrainTrackerState.userBoardingStation
        if (target != null) {
            val matched = trainStations.find { it.code.equals(target.code, ignoreCase = true) }
            selectedStation = matched ?: target
        } else if (trainStations.isNotEmpty()) {
            selectedStation = trainStations.first()
        }
    }

    LaunchedEffect(trainStations) {
        val target = TrainTrackerState.userBoardingStation
        if (target != null) {
            val matched = trainStations.find { it.code.equals(target.code, ignoreCase = true) }
            selectedStation = matched ?: target
        } else if (trainStations.isNotEmpty()) {
            selectedStation = trainStations.first()
        }
    }
    var showStationSheet by remember { mutableStateOf(false) }

    // Navigation telemetry (dynamically updated in real time for any station pan India)
    var navDistanceKm by remember { mutableStateOf("0.5 km") }
    var navDurationMin by remember { mutableStateOf(3) }
    var navEtaTime by remember { mutableStateOf("3 min") }
    var navInstruction by remember { mutableStateOf("Head toward station") }
    var navIcon by remember { mutableStateOf("⬆") }
    var selectedAlternativeRoute by remember { mutableStateOf(0) }

    // Real-time calculation whenever selectedStation or alternative route changes
    LaunchedEffect(selectedStation, selectedAlternativeRoute) {
        val dLat = if (selectedStation.latitude != 0.0) selectedStation.latitude else StationDataService.getCoordinates(selectedStation.code).first
        val dLon = if (selectedStation.longitude != 0.0) selectedStation.longitude else StationDataService.getCoordinates(selectedStation.code).second
        val (dist, dur, instr) = calculateNavigationTelemetry(userLat, userLon, dLat, dLon, selectedStation.name, selectedAlternativeRoute)
        navDistanceKm = dist
        navDurationMin = dur
        navInstruction = instr
        navEtaTime = formatEtaDuration(dur)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // 1. FULLSCREEN NATIVE MAP (Syncs with live status, active train, and map style)
        NativeMapView(
            modifier = Modifier.fillMaxSize(),
            isNavigationMode = isNavigationMode,
            userLat = userLat,
            userLon = userLon,
            destLat = if (selectedStation.latitude != 0.0) selectedStation.latitude else StationDataService.getCoordinates(selectedStation.code).first,
            destLon = if (selectedStation.longitude != 0.0) selectedStation.longitude else StationDataService.getCoordinates(selectedStation.code).second,
            stationName = selectedStation.name,
            trainNumber = activeTrain.number,
            trainProgress = liveStatus?.progressPercent ?: 0.65f,
            stations = trainStations,
            is3DMode = is3DMode,
            followTrain = followTrain,
            mapStyle = TrainTrackerState.currentMapStyle,
            onStationClick = { station ->
                val resolved = if (station.latitude != 0.0 && station.longitude != 0.0) station else {
                    val (lat, lon) = StationDataService.getCoordinates(station.code)
                    station.copy(latitude = lat, longitude = lon)
                }
                selectedStation = resolved
                showStationSheet = true
            },
            onNavigateToStation = { station ->
                val resolved = if (station.latitude != 0.0 && station.longitude != 0.0) station else {
                    val (lat, lon) = StationDataService.getCoordinates(station.code)
                    station.copy(latitude = lat, longitude = lon)
                }
                selectedStation = resolved
                TrainTrackerState.userBoardingStation = resolved
                val (dist, dur, instr) = calculateNavigationTelemetry(userLat, userLon, resolved.latitude, resolved.longitude, resolved.name, selectedAlternativeRoute)
                navDistanceKm = dist
                navDurationMin = dur
                navInstruction = instr
                navEtaTime = formatEtaDuration(dur)
                showRoutePreviewSheet = true
                showStationSheet = false
                if (!isMuted) {
                    PlatformSpeech.speak("Route to ${resolved.name}. Estimated travel time $dur minutes. $instr")
                }
            },
            onVoiceInstruction = { instruction ->
                if (!isMuted && instruction.isNotBlank()) {
                    PlatformSpeech.speak(instruction)
                }
            },
            onCompassRotationChanged = { bearing ->
                compassBearing = bearing
            },
            onNavStatsUpdated = { distM, durS, instruction, icon ->
                val km = distM / 1000.0
                navDistanceKm = if (km < 1.0) "${distM.roundToInt()} m" else "${(km * 10).roundToInt() / 10.0} km"
                navDurationMin = (durS / 60.0).roundToInt().coerceAtLeast(1)
                val changed = navInstruction != instruction && instruction.isNotBlank()
                navInstruction = instruction
                navIcon = icon
                navEtaTime = formatEtaDuration(navDurationMin)
                if (changed && !isMuted && isNavigationMode) {
                    PlatformSpeech.speak(instruction)
                }
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
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                val isDark = TrainTrackerState.isDarkMode
                if (!isSearchActive) {
                    // Sleek Floating Capsule (Takes only 48dp, 80% map visible!)
                    val hasActiveTrain = TrainTrackerState.activeTrain.number.isNotBlank()

                    // Continuous right-to-left marquee animation
                    val infiniteTransition = rememberInfiniteTransition(label = "search_marquee")
                    val progress by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 7000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "marquee_progress"
                    )

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clickable { isSearchActive = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 16.dp, end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 1. DYNAMIC TICKER / MARQUEE: starts at right corner, leaves at left end
                            BoxWithConstraints(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clipToBounds(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val boxWidthPx = constraints.maxWidth.toFloat()
                                var textWidthPx by remember { mutableStateOf(0f) }
                                val density = LocalDensity.current
                                val defaultTextWidthPx = remember(density) { with(density) { 240.dp.toPx() } }
                                val effectiveTextWidth = if (textWidthPx > 0f) textWidthPx else defaultTextWidthPx
                                // Starts exactly at the right corner (boxWidthPx) and travels to completely leave left edge (-effectiveTextWidth)
                                val currentX = boxWidthPx - progress * (boxWidthPx + effectiveTextWidth)

                                if (hasActiveTrain) {
                                    Text(
                                        text = TrainTrackerState.searchQuery,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        fontWeight = FontWeight.SemiBold,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "Search by Train name and No.",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        fontWeight = FontWeight.Normal,
                                        softWrap = false,
                                        onTextLayout = { textLayoutResult ->
                                            textWidthPx = textLayoutResult.size.width.toFloat()
                                        },
                                        modifier = Modifier.graphicsLayer {
                                            translationX = currentX
                                        }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // 2. RIGHT CORNER: Train number badge (if active) + Search lens logo
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (hasActiveTrain) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(RailBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = activeTrain.number,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RailBluePrimary
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                                                .clickable {
                                                    TrainTrackerState.clearActiveTrain()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("✕", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // Search lens logo placed at the right corner
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(RailBluePrimary.copy(alpha = 0.08f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🔍", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                } else {
                    // Expanded Interactive Search Card (Only visible when user taps into search!)
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
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
                                    onValueChange = {
                                        if (it.isEmpty()) {
                                            TrainTrackerState.clearActiveTrain()
                                        } else {
                                            TrainTrackerState.onQueryChanged(it)
                                        }
                                    },
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

                                if (TrainTrackerState.searchQuery.isNotEmpty() || TrainTrackerState.activeTrain.number.isNotBlank()) {
                                    IconButton(onClick = { TrainTrackerState.clearActiveTrain() }) {
                                        Text("✕", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        isSearchActive = false
                                        TrainTrackerState.closeSearchDropdown()
                                        if (TrainTrackerState.searchQuery.isBlank()) {
                                            TrainTrackerState.clearActiveTrain()
                                        }
                                    }
                                ) {
                                    Text("Done", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)
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
                                            .clickable {
                                                TrainTrackerState.selectByNumber(trainNo)
                                                isSearchActive = false
                                                TrainTrackerState.closeSearchDropdown()
                                            }
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
                            text = if (isNavigationPaused) "Tap Resume in bottom bar to continue guidance" else "Toward ${selectedStation.name} • Boarding Station Route",
                            fontSize = 12.sp,
                            color = if (isNavigationPaused) Color(0xFFFED7AA) else Color(0xFF99F6E4)
                        )
                    }
                }
            }
        }

        val toolsTopPadding = if (isNavigationMode) 95.dp else if (!isFullscreen) (if (isSearchActive) 175.dp else 70.dp) else 18.dp

        // 3. NORMAL MAP VIEW: 3-Dots Button + Tools (always visible until user collapses)
        if (!isFullscreen) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = toolsTopPadding, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 3-Dots toggle button (web-app parity: top-right of map)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(3.dp, RoundedCornerShape(11.dp))
                        .background(
                            if (isNormalToolsExpanded) Color(0xFF2563EB).copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(11.dp)
                        )
                        .border(
                            1.5.dp,
                            if (isNormalToolsExpanded) Color(0xFF2563EB).copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            RoundedCornerShape(11.dp)
                        )
                        .clickable { isNormalToolsExpanded = !isNormalToolsExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    VectorThreeDotsIcon(isExpanded = isNormalToolsExpanded)
                }

                // Fullscreen button — always visible directly below 3-dots
                MinimalMapToolButton(
                    isActive = false,
                    onClick = { isFullscreen = true }
                ) {
                    VectorFullscreenIcon(isFullscreen = false)
                }

                // Collapsible tools under 3-dots (expanded by default)
                AnimatedVisibility(
                    visible = isNormalToolsExpanded,
                    enter = fadeIn(animationSpec = tween(160)) + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut(animationSpec = tween(160)) + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Precision Compass Needle
                        MinimalMapToolButton(onClick = { compassBearing = 0f }) {
                            VectorCompassNeedleIcon(bearing = compassBearing)
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
                            VectorCameraFollowIcon(isActive = followTrain)
                        }

                        // GPS Find Nearest Station
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
                                isActive = !isMuted,
                                onClick = {
                                    isMuted = !isMuted
                                    if (!isMuted) {
                                        val toSay = if (navInstruction.isNotBlank()) navInstruction else "Navigation voice guidance enabled"
                                        PlatformSpeech.speak(toSay)
                                    } else {
                                        PlatformSpeech.stop()
                                    }
                                }
                            ) {
                                VectorSoundIcon(isMuted = isMuted)
                            }

                            // Alternative Routes Toggle
                            MinimalMapToolButton(
                                isActive = selectedAlternativeRoute == 1,
                                onClick = {
                                    selectedAlternativeRoute = if (selectedAlternativeRoute == 0) 1 else 0
                                    val dLat = if (selectedStation.latitude != 0.0) selectedStation.latitude else StationDataService.getCoordinates(selectedStation.code).first
                                    val dLon = if (selectedStation.longitude != 0.0) selectedStation.longitude else StationDataService.getCoordinates(selectedStation.code).second
                                    val (dist, dur, instr) = calculateNavigationTelemetry(userLat, userLon, dLat, dLon, selectedStation.name, selectedAlternativeRoute)
                                    navDistanceKm = dist
                                    navDurationMin = dur
                                    navInstruction = instr
                                    navEtaTime = formatEtaDuration(dur)
                                }
                            ) {
                                VectorRouteSplitIcon()
                            }
                        }
                    }
                }
            }
        }

        // 4. FULLSCREEN VIEW: Black Draggable Initializer + 3.5s Auto-Collapse Tools
        if (isFullscreen) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 12.dp)
                    .offset { IntOffset(initializerDragX.roundToInt(), initializerDragY.roundToInt()) }
            ) {
                // Black initializer tab (shown when tools auto-collapsed)
                AnimatedVisibility(
                    visible = !isFullscreenToolsExpanded,
                    enter = fadeIn(animationSpec = tween(150)) + slideInHorizontally(initialOffsetX = { it }),
                    exit = fadeOut(animationSpec = tween(150)) + slideOutHorizontally(targetOffsetX = { it }),
                    modifier = Modifier.pointerInput(Unit) {
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
                            .clickable {
                                isFullscreenToolsExpanded = true
                                pingInteraction()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("‹", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Fullscreen tools (auto-collapse after 3.5s inactivity)
                AnimatedVisibility(
                    visible = isFullscreenToolsExpanded,
                    enter = fadeIn(animationSpec = tween(160)) + slideInHorizontally(initialOffsetX = { it / 2 }),
                    exit = fadeOut(animationSpec = tween(160)) + slideOutHorizontally(targetOffsetX = { it / 2 })
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Exit Fullscreen button at top
                        MinimalMapToolButton(
                            isActive = true,
                            onClick = {
                                isFullscreen = false
                                pingInteraction()
                            }
                        ) {
                            VectorFullscreenIcon(isFullscreen = true)
                        }

                        // Collapse tools → black initializer
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .shadow(4.dp, RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
                                .clickable {
                                    isFullscreenToolsExpanded = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("›", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        // Compass
                        MinimalMapToolButton(onClick = {
                            compassBearing = 0f
                            pingInteraction()
                        }) {
                            VectorCompassNeedleIcon(bearing = compassBearing)
                        }

                        // 3D
                        MinimalMapToolButton(
                            label = if (is3DMode) "2D" else "3D",
                            isActive = is3DMode,
                            onClick = { is3DMode = !is3DMode; pingInteraction() }
                        )

                        // Follow
                        MinimalMapToolButton(
                            isActive = followTrain,
                            onClick = { followTrain = !followTrain; pingInteraction() }
                        ) {
                            VectorCameraFollowIcon(isActive = followTrain)
                        }

                        // GPS
                        MinimalMapToolButton(onClick = {
                            selectedStation = trainStations.first()
                            showRoutePreviewSheet = true
                            isFullscreenToolsExpanded = false
                        }) {
                            VectorGpsIcon()
                        }

                        if (isNavigationMode) {
                            MinimalMapToolButton(
                                isActive = !isMuted,
                                onClick = {
                                    isMuted = !isMuted
                                    if (!isMuted) {
                                        val toSay = if (navInstruction.isNotBlank()) navInstruction else "Navigation voice guidance enabled"
                                        PlatformSpeech.speak(toSay)
                                    } else {
                                        PlatformSpeech.stop()
                                    }
                                    pingInteraction()
                                }
                            ) {
                                VectorSoundIcon(isMuted = isMuted)
                            }

                            MinimalMapToolButton(
                                isActive = selectedAlternativeRoute == 1,
                                onClick = {
                                    selectedAlternativeRoute = if (selectedAlternativeRoute == 0) 1 else 0
                                    val dLat = if (selectedStation.latitude != 0.0) selectedStation.latitude else StationDataService.getCoordinates(selectedStation.code).first
                                    val dLon = if (selectedStation.longitude != 0.0) selectedStation.longitude else StationDataService.getCoordinates(selectedStation.code).second
                                    val (dist, dur, instr) = calculateNavigationTelemetry(userLat, userLon, dLat, dLon, selectedStation.name, selectedAlternativeRoute)
                                    navDistanceKm = dist
                                    navDurationMin = dur
                                    navInstruction = instr
                                    navEtaTime = formatEtaDuration(dur)
                                    pingInteraction()
                                }
                            ) {
                                VectorRouteSplitIcon()
                            }
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
                val hasActiveTrain = activeTrain.number.isNotBlank()
                // NORMAL MODE: Modern Interactive Peek Bottom Sheet (Spacious & Minimalist)
                Card(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .clickable {
                            if (hasActiveTrain) isBottomSheetExpanded = !isBottomSheetExpanded
                            else isSearchActive = true
                        }
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // Drag Handle Pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(36.dp)
                                .height(4.dp)
                                .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (!hasActiveTrain) {
                            // No train selected yet — prompt user to search
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🚆", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Search for a Train", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Tap above to track any train live", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        } else {
                        // Header Row (Compact Peek Header)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        activeTrain.number,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    activeTrain.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val isDelayed = (liveStatus?.delayMinutes ?: 0) > 0
                                Box(
                                    modifier = Modifier
                                        .background(if (isDelayed) Color(0xFFFEF3C7) else Color(0xFFDCFCE7), RoundedCornerShape(20.dp))
                                        .border(1.dp, if (isDelayed) Color(0xFFFDE68A) else Color(0xFFBBF7D0), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 9.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (isDelayed) "${liveStatus?.delayMinutes}m LATE" else I18n.t("status_on_time"),
                                        color = if (isDelayed) Color(0xFFB45309) else RailGreenOnTime,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                // Interactive Expand / Collapse Indicator
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(Color(0xFFF1F5F9), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (isBottomSheetExpanded) "▼" else "▲",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        } // end hasActiveTrain

                        // Expanded Content: Telemetry Metrics, Progress Bar, and Action Button
                        if (isBottomSheetExpanded && hasActiveTrain) {
                            Spacer(modifier = Modifier.height(14.dp))

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
                                modifier = Modifier.fillMaxWidth().height(46.dp),
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
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
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

                            Text("Source Station: ${selectedStation.name}", fontSize = 11.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = if (isNavigationPaused) "Paused" else formatEtaDuration(navDurationMin),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isNavigationPaused) Color(0xFFB45309) else Color(0xFF044E42),
                                    maxLines = 1
                                )
                                Text(
                                    text = "$navDistanceKm • $navEtaTime",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Gray,
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Voice mute toggle
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFF1F5F9), CircleShape)
                                        .clickable {
                                            isMuted = !isMuted
                                            if (!isMuted) {
                                                val toSay = if (navInstruction.isNotBlank()) navInstruction else "Navigation voice guidance enabled"
                                                PlatformSpeech.speak(toSay)
                                            } else {
                                                PlatformSpeech.stop()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(if (isMuted) "🔇" else "🔊", fontSize = 15.sp)
                                }

                                // Navigation Pause / Resume Button
                                Button(
                                    onClick = { isNavigationPaused = !isNavigationPaused },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isNavigationPaused) Color(0xFF044E42) else Color(0xFFF1F5F9),
                                        contentColor = if (isNavigationPaused) Color.White else Color(0xFF0F172A)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        if (isNavigationPaused) "▶ Resume" else "⏸ Pause",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        softWrap = false
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
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        "✕ Stop",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color(0xFFDC2626),
                                        maxLines = 1,
                                        softWrap = false
                                    )
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
                        Text("🧭 Directions to ${selectedStation.name} (Start Journey)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                    // Header Row: 🧭 DIRECTIONS TO SOURCE / BOARDING STATION + Dismiss ✕
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
                                    "DIRECTIONS TO YOUR ONBOARDING STATION",
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

                    // Quick Boarding Station Selector (Default: User's Search Origin Station)
                    if (trainStations.size > 1) {
                        Text(
                            "Boarding Station (Where you start your journey):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        ) {
                            items(trainStations) { stn ->
                                val isSelected = selectedStation.code == stn.code
                                val isOrigin = trainStations.firstOrNull()?.code == stn.code
                                val isBoarding = stn.code.equals(TrainTrackerState.userBoardingStation?.code, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) Color(0xFF6D28D9) else Color(0xFFF1F5F9),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable {
                                            val resolved = if (stn.latitude != 0.0 && stn.longitude != 0.0) stn else {
                                                val (lat, lon) = StationDataService.getCoordinates(stn.code)
                                                stn.copy(latitude = lat, longitude = lon)
                                            }
                                            selectedStation = resolved
                                            TrainTrackerState.userBoardingStation = resolved
                                            val (dist, dur, instr) = calculateNavigationTelemetry(userLat, userLon, resolved.latitude, resolved.longitude, resolved.name, selectedAlternativeRoute)
                                            navDistanceKm = dist
                                            navDurationMin = dur
                                            navInstruction = instr
                                            navEtaTime = formatEtaDuration(dur)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${stn.name}${if (isBoarding) " (Boarding)" else if (isOrigin) " (Origin)" else ""}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF334155)
                                    )
                                }
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
                                        if (selectedAlternativeRoute == 1) "Alternative" else "Fastest",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                                Text(
                                    formatEtaDuration(navDurationMin),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF6D28D9)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${if (selectedAlternativeRoute == 1) "Alternative Route" else "Recommended"} • $navDistanceKm",
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
                                    if (!isMuted) {
                                        val announcement = if (navInstruction.isNotBlank()) {
                                            "Navigation started to ${selectedStation.name}. $navInstruction"
                                        } else {
                                            "Navigation started to ${selectedStation.name}. Follow the highlighted route."
                                        }
                                        PlatformSpeech.speak(announcement)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6D28D9)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.align(Alignment.End),
                                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    "▶ Start (${if (selectedAlternativeRoute == 1) "Alternative" else "Fastest"})",
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
                                val destLat = if (selectedStation.latitude != 0.0) selectedStation.latitude else StationDataService.getCoordinates(selectedStation.code).first
                                val destLon = if (selectedStation.longitude != 0.0) selectedStation.longitude else StationDataService.getCoordinates(selectedStation.code).second
                                val gmapsUrl = "https://www.google.com/maps/dir/?api=1&origin=$userLat,$userLon&destination=$destLat,$destLon&travelmode=driving"
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
            .shadow(4.dp, RoundedCornerShape(13.dp))
            .background(
                if (isActive) Color(0xFF2563EB).copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                RoundedCornerShape(13.dp)
            )
            .border(
                1.dp,
                if (isActive) Color(0xFF2563EB).copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                RoundedCornerShape(13.dp)
            )
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
                color = if (isActive) Color(0xFF1D4ED8) else MaterialTheme.colorScheme.onSurface
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
        // Show X (close indicator) in blue when expanded
        Canvas(modifier = Modifier.size(16.dp)) {
            val stroke = 2.2f
            val color = Color(0xFF1D4ED8)
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

@Composable
fun VectorCompassNeedleIcon(bearing: Float) {
    Canvas(modifier = Modifier.size(20.dp).rotate(-bearing)) {
        val cx = size.width / 2
        val cy = size.height / 2
        val needleW = 3.5.dp.toPx()

        // North needle (crisp crimson red)
        val northPath = Path().apply {
            moveTo(cx, 2.dp.toPx())
            lineTo(cx + needleW, cy)
            lineTo(cx - needleW, cy)
            close()
        }
        drawPath(northPath, Color(0xFFEF4444))

        // South needle (subtle slate)
        val southPath = Path().apply {
            moveTo(cx, size.height - 2.dp.toPx())
            lineTo(cx + needleW, cy)
            lineTo(cx - needleW, cy)
            close()
        }
        drawPath(southPath, Color(0xFF64748B))

        // Center pivot
        drawCircle(Color.White, radius = 2.dp.toPx(), center = Offset(cx, cy))
        drawCircle(Color(0xFF0F172A), radius = 1.2.dp.toPx(), center = Offset(cx, cy))
    }
}

@Composable
fun VectorSoundIcon(isMuted: Boolean) {
    Canvas(modifier = Modifier.size(17.dp)) {
        val color = if (isMuted) Color(0xFF94A3B8) else Color(0xFF0F172A)
        val stroke = 1.6f

        // Speaker cone
        val bodyPath = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.35f)
            lineTo(size.width * 0.35f, size.height * 0.35f)
            lineTo(size.width * 0.60f, size.height * 0.18f)
            lineTo(size.width * 0.60f, size.height * 0.82f)
            lineTo(size.width * 0.35f, size.height * 0.65f)
            lineTo(size.width * 0.15f, size.height * 0.65f)
            close()
        }
        drawPath(bodyPath, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))

        if (isMuted) {
            drawLine(Color(0xFFEF4444), Offset(size.width * 0.72f, size.height * 0.32f), Offset(size.width * 0.92f, size.height * 0.68f), strokeWidth = 1.8f, cap = StrokeCap.Round)
        } else {
            drawArc(
                color = color,
                startAngle = -45f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(size.width * 0.45f, size.height * 0.25f),
                size = Size(size.width * 0.45f, size.height * 0.50f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun VectorRouteSplitIcon() {
    Canvas(modifier = Modifier.size(17.dp)) {
        val color = Color(0xFF0F172A)
        val stroke = 1.8f
        // Stem
        drawLine(color, Offset(size.width * 0.5f, size.height * 0.85f), Offset(size.width * 0.5f, size.height * 0.50f), strokeWidth = stroke, cap = StrokeCap.Round)
        // Left path
        val leftBranch = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.50f)
            cubicTo(size.width * 0.5f, size.height * 0.35f, size.width * 0.25f, size.height * 0.35f, size.width * 0.25f, size.height * 0.20f)
        }
        drawPath(leftBranch, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        // Right path
        val rightBranch = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.50f)
            cubicTo(size.width * 0.5f, size.height * 0.35f, size.width * 0.75f, size.height * 0.35f, size.width * 0.75f, size.height * 0.20f)
        }
        drawPath(rightBranch, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        // Arrow heads
        drawLine(color, Offset(size.width * 0.15f, size.height * 0.28f), Offset(size.width * 0.25f, size.height * 0.16f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.35f, size.height * 0.28f), Offset(size.width * 0.25f, size.height * 0.16f), strokeWidth = stroke, cap = StrokeCap.Round)

        drawLine(color, Offset(size.width * 0.65f, size.height * 0.28f), Offset(size.width * 0.75f, size.height * 0.16f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(size.width * 0.85f, size.height * 0.28f), Offset(size.width * 0.75f, size.height * 0.16f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}
