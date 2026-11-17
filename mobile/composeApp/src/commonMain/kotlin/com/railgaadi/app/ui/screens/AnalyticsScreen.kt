package com.railgaadi.app.ui.screens

import kotlin.math.roundToInt
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.model.Station
import com.railgaadi.app.model.Train
import com.railgaadi.app.network.RailRadarService
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.*
import com.railgaadi.app.ui.components.CoachPositionVisualizer
import com.railgaadi.app.ui.components.LiveAutoRefreshPill
import com.railgaadi.app.ui.components.MetricBox
import com.railgaadi.app.ui.components.VectorCoachIcon
import com.railgaadi.app.ui.components.DestinationAlarmModal
import com.railgaadi.app.ui.components.AlarmRingingDialog
import com.railgaadi.app.ui.components.InTrainCockpitBanner
import com.railgaadi.app.util.formatDelayDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TimelineViewMode {
    WIMT, TABLE, COACH
}

@Composable
fun AnalyticsScreen(
    onViewLiveMap: () -> Unit = {}
) {
    val isDark = TrainTrackerState.isDarkMode
    val scope = rememberCoroutineScope()
    val service = remember { RailRadarService() }

    val activeTrain = TrainTrackerState.activeTrain
    val liveStatus = TrainTrackerState.liveStatus
    val isLoading = TrainTrackerState.isLoadingStatus

    // View switcher: Overview (WIMT vertical track) vs Description (Table timetable)
    var viewMode by remember { mutableStateOf(TimelineViewMode.WIMT) }

    // Glassmorphic translucent styling
    val cardSurface = if (isDark) Color(0xFF1E293B).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.88f)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)

    // Search dialog state to change or pick any train
    var showTrainSearchModal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Train>>(emptyList()) }
    var isSearchingTrains by remember { mutableStateOf(false) }

    // Floating Inside-the-Train banner state with 7-second auto-collapse timer (Where Is My Train style)
    var isInsideTrainBannerVisible by remember { mutableStateOf(false) }
    var autoCollapseJob by remember { mutableStateOf<Job?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val triggerInsideTrainBanner: () -> Unit = {
        isInsideTrainBannerVisible = true
        autoCollapseJob?.cancel()
        autoCollapseJob = coroutineScope.launch {
            delay(7000)
            isInsideTrainBannerVisible = false
        }
    }

    // If active train is set, ensure liveStatus is fetched and start live auto-refresh (10s)
    LaunchedEffect(activeTrain.number) {
        if (activeTrain.number.isNotBlank()) {
            if (liveStatus == null) {
                TrainTrackerState.refreshLiveStatus()
            }
            TrainTrackerState.startAutoRefresh(10)
        }
    }

    // Debounced train search for modal
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length >= 2) {
            delay(200)
            isSearchingTrains = true
            searchResults = service.searchTrains(q)
            isSearchingTrains = false
        } else {
            searchResults = emptyList()
        }
    }

    val stations = liveStatus?.stations ?: emptyList()

    // Determine current train station index along route
    val curIdx = remember(stations, liveStatus?.currentStation, liveStatus?.progressPercent) {
        if (stations.isEmpty()) 0
        else {
            val stnName = liveStatus?.currentStation ?: ""
            val foundIdx = TrainTrackerState.findStationIndex(stnName, stations)
            if (foundIdx >= 0) foundIdx
            else {
                val pct = liveStatus?.progressPercent ?: 0f
                ((stations.size - 1) * pct).toInt().coerceIn(0, stations.size - 1)
            }
        }
    }

    val curStation = stations.getOrNull(curIdx)

    // Immediate next station ahead of train (whether pass-through or halt)
    val upcomingStation = run {
        if (stations.isEmpty()) null
        else {
            stations.drop(curIdx + 1).firstOrNull { it.distanceKm > (curStation?.distanceKm ?: 0.0) }
                ?: stations.getOrNull(curIdx + 1)
                ?: stations.drop(curIdx + 1).firstOrNull()
        }
    }

    // Find the NEXT scheduled HALT station (haltMinutes > 0), skipping pass-throughs
    // This matches the "Next Station Halt:" label in the UI
    val nextStation = run {
        val candidates = stations.drop(curIdx + 1)
        // Prefer a halt station; if none found fall back to any next station
        candidates.firstOrNull { it.haltMinutes > 0 }
            ?: candidates.firstOrNull()
    }

    // Observe live odometer offset so Compose re-evaluates remaining distance in real time every second
    val liveOdometer = TrainTrackerState.liveDistanceOffsetKm

    val kmToUpcoming = upcomingStation?.let {
        TrainTrackerState.getLiveRemainingKmToStation(it, curStation)
    }

    val kmToNext = nextStation?.let {
        TrainTrackerState.getLiveRemainingKmToStation(it, curStation)
    }

    val progressPercent = remember(liveStatus?.progressPercent, curIdx, stations.size) {
        if (liveStatus?.progressPercent != null && liveStatus.progressPercent > 0f) {
            (liveStatus.progressPercent * 100).toInt()
        } else if (stations.isNotEmpty()) {
            ((curIdx.toFloat() / (stations.size - 1).coerceAtLeast(1)) * 100).toInt()
        } else 0
    }

    val totalKm = stations.lastOrNull()?.distanceKm ?: 0.0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBackground else LightBackground)
    ) {
        if (activeTrain.number.isBlank()) {
            // =========================================================
            // EMPTY STATE: Pick or Search a train dynamically
            // =========================================================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("📊", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Live Journey Analytics",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) DarkTextPrimary else LightTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Track any train across India to view full timetable, vertical journey timeline, live speed, and station events.",
                        fontSize = 13.sp,
                        color = if (isDark) DarkTextSecondary else LightTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                // Dynamic Marquee Ticker Search Bar (Mobile & responsive)
                item {
                    val infiniteTransition = rememberInfiniteTransition(label = "analytics_search_marquee")
                    val marqueeProgress by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 7500, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "analytics_marquee_progress"
                    )

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardSurface),
                        border = BorderStroke(1.dp, cardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .clickable { showTrainSearchModal = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🔍", fontSize = 16.sp)

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
                                val defaultTextWidthPx = remember(density) { with(density) { 260.dp.toPx() } }
                                val effectiveTextWidth = if (textWidthPx > 0f) textWidthPx else defaultTextWidthPx
                                val currentX = boxWidthPx - marqueeProgress * (boxWidthPx + effectiveTextWidth)

                                Text(
                                    text = "Search train name or number across Indian Railways...",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    softWrap = false,
                                    onTextLayout = { textLayoutResult ->
                                        textWidthPx = textLayoutResult.size.width.toFloat()
                                    },
                                    modifier = Modifier.graphicsLayer {
                                        translationX = currentX
                                    }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(RailBluePrimary, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Search", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // Quick Picks from Recently Searched Trains
                val recent = TrainTrackerState.recentTrains
                if (recent.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Recently Searched Trains:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) DarkTextPrimary else LightTextPrimary
                            )

                            recent.take(3).forEach { train ->
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = cardSurface),
                                    border = BorderStroke(1.dp, cardBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            TrainTrackerState.selectTrain(train)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(RailBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = train.number,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = RailBluePrimary
                                                )
                                            }

                                            Column {
                                                Text(
                                                    text = train.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                                                )
                                                Text(
                                                    text = "${train.origin.substringBefore(" (")} ➔ ${train.destination.substringBefore(" (")}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Track →",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RailBluePrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // =========================================================
            // ACTIVE TRAIN LIVE ANALYTICS & JOURNEY TIMELINE
            // =========================================================
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // -----------------------------------------------------
                // 1. ACTIVE TRAIN HEADER
                // -----------------------------------------------------
                item {
                    Card(
                        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = cardSurface),
                        border = BorderStroke(1.dp, cardBorder),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Top row: Auto-refresh countdown indicator + action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Live Auto-Refresh Ticker Badge: ● LIVE · 10s
                                LiveAutoRefreshPill(
                                    countdown = TrainTrackerState.autoRefreshCountdown,
                                    isRefreshing = TrainTrackerState.isAutoRefreshing,
                                    onForceRefresh = { TrainTrackerState.triggerManualRefresh() }
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Refresh button
                                    IconButton(
                                        onClick = { TrainTrackerState.triggerManualRefresh() },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(RailBluePrimary.copy(alpha = 0.12f), CircleShape)
                                    ) {
                                        Text(if (TrainTrackerState.isAutoRefreshing) "⏳" else "🔄", fontSize = 14.sp)
                                    }

                                    // Change train button
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { showTrainSearchModal = true }
                                            .background(if (isDark) DarkBackground else Color(0xFFF1F5F9))
                                            .border(1.dp, if (isDark) DarkBorder else LightBorder, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Text("Change ▾", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RailBluePrimary)
                                    }
                                }
                            }

                            // Active Train Hero Card
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            listOf(RailBluePrimary.copy(alpha = 0.08f), Color(0xFF3B82F6).copy(alpha = 0.05f))
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(1.dp, RailBluePrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(RailBluePrimary, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = activeTrain.number,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeTrain.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) DarkTextPrimary else LightTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${activeTrain.origin.substringBefore(" (")} ➔ ${activeTrain.destination.substringBefore(" (")}",
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // View on Live Map Pill
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onViewLiveMap() }
                                    .background(RailBluePrimary)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("Map 🗺️", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }

                // -----------------------------------------------------
                // 2. DYNAMIC NEXT HALT & JOURNEY PROGRESS BANNER
                // -----------------------------------------------------
                item {
                    val delay = liveStatus?.delayMinutes ?: 0
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else LightSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Primary Line: Next Station and remaining distance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .background(
                                                if (isDark) RailBluePrimary.copy(alpha = 0.20f) else Color(0xFFE0F2FE),
                                                CircleShape
                                            )
                                            .border(
                                                1.dp,
                                                RailBluePrimary.copy(alpha = 0.30f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("📍", fontSize = 18.sp)
                                    }

                                    Column {
                                        Text(
                                            text = "Next Station Halt:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = nextStation?.name ?: "Next Station Halt",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDark) DarkTextPrimary else LightTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (kmToNext != null) {
                                    val displayKm = kmToNext.toInt().coerceAtLeast(1)
                                    Box(
                                        modifier = Modifier
                                            .background(RailBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (kmToNext <= 0.0) "Arrived" else "$displayKm km away",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = RailBluePrimary
                                        )
                                    }
                                }
                            }

                            // Progress Bar
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$progressPercent% Completed",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) DarkTextSecondary else LightTextSecondary
                                    )
                                    Text(
                                        text = "${curIdx + 1}/${stations.size} Stations",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Gray
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { progressPercent / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = RailBluePrimary,
                                    trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                )
                            }

                            // Summary Badges: Station Count, Delay, Distance
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Delay Status Pill
                                val (statusText, statusBg, statusColor) = when {
                                    delay == 0 -> Triple("● On Time", RailGreenOnTime.copy(alpha = 0.12f), RailGreenOnTime)
                                    delay > 0 -> Triple("● +${formatDelayDuration(delay)} Late", RailRedDelayed.copy(alpha = 0.12f), RailRedDelayed)
                                    else -> Triple("● ${formatDelayDuration(-delay)} Early", RailGreenOnTime.copy(alpha = 0.12f), RailGreenOnTime)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(statusBg, RoundedCornerShape(8.dp))
                                        .border(0.5.dp, statusColor.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 6.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor, maxLines = 1)
                                }

                                // Total Distance
                                if (totalKm > 0) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(if (isDark) DarkBackground else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                            .border(0.5.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 6.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$totalKm km total", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (isDark) DarkTextPrimary else LightTextPrimary, maxLines = 1)
                                    }
                                }
                            }

                            // ── Phase 4: Destination Station Wake-Up Alarm Capsule ──
                            val activeAlarm = TrainTrackerState.activeAlarm
                            val isAlarmArmed = activeAlarm?.isArmed == true

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isAlarmArmed) {
                                        RailGreenOnTime.copy(alpha = if (isDark) 0.16f else 0.10f)
                                    } else {
                                        if (isDark) Color(0xFF1E293B).copy(alpha = 0.50f) else Color(0xFFF8FAFC)
                                    }
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isAlarmArmed) RailGreenOnTime.copy(alpha = 0.45f)
                                    else (if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f))
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { TrainTrackerState.openAlarmSetup() }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    if (isAlarmArmed) RailGreenOnTime.copy(alpha = 0.20f)
                                                    else RailBluePrimary.copy(alpha = 0.12f),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(if (isAlarmArmed) "🔔" else "⏰", fontSize = 16.sp)
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            if (isAlarmArmed) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "Alarm: ${activeAlarm.stationName}",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isDark) DarkTextPrimary else LightTextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.weight(1f, fill = false)
                                                    )
                                                    if (activeAlarm.isSnoozed) {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(RailAmberWarning.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                "SNOOZED",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = RailAmberWarning,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .background(RailGreenOnTime.copy(alpha = 0.20f), RoundedCornerShape(6.dp))
                                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                "ARMED",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = RailGreenOnTime,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                    }
                                                }
                                                val distInfo = activeAlarm.distanceRemainingKm?.let { "$it km away" } ?: "${activeAlarm.triggerDistanceKm.toInt()} km before"
                                                val snoozeSecs = TrainTrackerState.snoozeRemainingSeconds
                                                val snoozeText = if (snoozeSecs > 0) {
                                                    val m = snoozeSecs / 60
                                                    val s = snoozeSecs % 60
                                                    "Alarm snoozed • Rings in ${if (m > 0) "${m}m ${s}s" else "${s}s"}"
                                                } else {
                                                    "Alarm snoozed • Ringing shortly"
                                                }
                                                Text(
                                                    text = if (activeAlarm.isSnoozed) snoozeText else "Rings $distInfo • Geofence active",
                                                    fontSize = 11.sp,
                                                    color = if (isDark) DarkTextSecondary else LightTextSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else {
                                                Text(
                                                    text = "Destination Wake-Up Alarm",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                                                )
                                                Text(
                                                    text = "GPS alerts wake you up before your station",
                                                    fontSize = 11.sp,
                                                    color = if (isDark) DarkTextSecondary else LightTextSecondary
                                                )
                                            }
                                        }
                                    }

                                    if (isAlarmArmed) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(RailRedDelayed.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                                .clickable { TrainTrackerState.disarmAlarm() }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "✕ Off",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = RailRedDelayed,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .background(RailBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text("Set →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // -----------------------------------------------------
                // 3. FULL JOURNEY TIMELINE & VIEW SWITCHER
                // -----------------------------------------------------
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Full-Width iOS Segmented Control: Track | Timetable | Coach
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isDark) Color(0xFF1E293B).copy(alpha = 0.85f) else Color(0xFFF1F5F9),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. TRACK TAB
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        if (viewMode == TimelineViewMode.WIMT) RailBluePrimary else Color.Transparent
                                    )
                                    .clickable { viewMode = TimelineViewMode.WIMT },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Track",
                                    fontSize = 12.sp,
                                    fontWeight = if (viewMode == TimelineViewMode.WIMT) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (viewMode == TimelineViewMode.WIMT) Color.White else if (isDark) DarkTextSecondary else LightTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // 2. TIMETABLE TAB
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        if (viewMode == TimelineViewMode.TABLE) RailBluePrimary else Color.Transparent
                                    )
                                    .clickable { viewMode = TimelineViewMode.TABLE },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Timetable",
                                    fontSize = 12.sp,
                                    fontWeight = if (viewMode == TimelineViewMode.TABLE) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (viewMode == TimelineViewMode.TABLE) Color.White else if (isDark) DarkTextSecondary else LightTextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // 3. COACH TAB
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(
                                        if (viewMode == TimelineViewMode.COACH) RailBluePrimary else Color.Transparent
                                    )
                                    .clickable { viewMode = TimelineViewMode.COACH },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    VectorCoachIcon(
                                        isActive = viewMode == TimelineViewMode.COACH,
                                        iconSize = 13.dp
                                    )
                                    Text(
                                        text = "Coach",
                                        fontSize = 12.sp,
                                        fontWeight = if (viewMode == TimelineViewMode.COACH) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (viewMode == TimelineViewMode.COACH) Color.White else if (isDark) DarkTextSecondary else LightTextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // =============================================
                        // VIEWS: TRACK (WIMT), TABLE (TIMETABLE), COACH POSITION
                        // =============================================
                        when (viewMode) {
                            TimelineViewMode.WIMT -> {
                                Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else LightSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Column Header: Arrival | Station / Day 1 | Departure
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Arrival",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray,
                                            modifier = Modifier.width(62.dp),
                                            textAlign = TextAlign.Start
                                        )

                                        Text(
                                            text = "Day 1 · Stations & Stops",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center
                                        )

                                        Text(
                                            text = "Departure",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray,
                                            modifier = Modifier.width(62.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }

                                    HorizontalDivider(
                                        color = if (isDark) DarkBorder.copy(alpha = 0.5f) else LightBorder.copy(alpha = 0.5f),
                                        thickness = 0.5.dp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    // ---------------------------------------------------------------
                                    // Build collapsed segments:
                                    //   Each "segment" = one halt stop + the pass-through stations
                                    //   that immediately follow it (until the next halt/end).
                                    //   Pass-through stations are collapsed by default; tap the pill
                                    //   between two halt stops to expand/collapse that segment.
                                    // ---------------------------------------------------------------
                                    data class StationSegment(
                                        val haltStation: Station,
                                        val haltIndex: Int,          // index in `stations`
                                        val passThroughs: List<Pair<Int, Station>> // (origIdx, station)
                                    )

                                    // Build segments by iterating through the flat station list
                                    val segments = mutableListOf<StationSegment>()
                                    var pendingPassThroughs = mutableListOf<Pair<Int, Station>>()
                                    var lastHaltIdx = -1
                                    var lastHaltStn: Station? = null

                                    stations.forEachIndexed { idx, stn ->
                                        if (stn.haltMinutes > 0) {
                                            if (lastHaltStn != null) {
                                                segments.add(StationSegment(lastHaltStn!!, lastHaltIdx, pendingPassThroughs.toList()))
                                            }
                                            pendingPassThroughs = mutableListOf()
                                            lastHaltIdx = idx
                                            lastHaltStn = stn
                                        } else {
                                            pendingPassThroughs.add(Pair(idx, stn))
                                        }
                                    }
                                    // Add final segment (may or may not have trailing pass-throughs)
                                    if (lastHaltStn != null) {
                                        segments.add(StationSegment(lastHaltStn!!, lastHaltIdx, pendingPassThroughs.toList()))
                                    }

                                    // Track which segment indices are expanded (empty = all collapsed)
                                    val expandedSegments = remember { mutableStateOf(setOf<Int>()) }

                                    // Handle pure pass-through stations before first halt stop
                                    val leadingPassThroughs = stations.takeWhile { it.haltMinutes == 0 }
                                    leadingPassThroughs.forEachIndexed { idx, stn ->
                                        val isPassed = idx < curIdx
                                        val isCurrent = idx == curIdx
                                        WimtStationTrackRow(
                                            station = stn, index = idx,
                                            isPassed = isPassed, isCurrent = isCurrent,
                                            isUpcoming = idx > curIdx,
                                            isFirst = idx == 0, isLast = false,
                                            curTrainDistKm = curStation?.distanceKm ?: 0.0,
                                            delayMinutes = liveStatus?.delayMinutes ?: 0,
                                            speedKmh = liveStatus?.speedKmh ?: 0,
                                            isDark = isDark,
                                            isEnRoute = isCurrent && (liveStatus?.status?.contains("AT-STATION", ignoreCase = true) != true)
                                        )
                                    }

                                    // Render each segment
                                    segments.forEachIndexed { segIdx, seg ->
                                        val haltIdx = seg.haltIndex
                                        val isPassed = haltIdx < curIdx
                                        val isCurrent = haltIdx == curIdx
                                        val isFirst = haltIdx == 0 && leadingPassThroughs.isEmpty()
                                        val isLast = haltIdx == stations.size - 1 && seg.passThroughs.isEmpty()
                                        val isExpanded = expandedSegments.value.contains(segIdx)

                                        // Render the halt station row
                                        WimtStationTrackRow(
                                            station = seg.haltStation,
                                            index = haltIdx,
                                            isPassed = isPassed,
                                            isCurrent = isCurrent,
                                            isUpcoming = haltIdx > curIdx,
                                            isFirst = isFirst,
                                            isLast = isLast && seg.passThroughs.isEmpty(),
                                            curTrainDistKm = curStation?.distanceKm ?: 0.0,
                                            delayMinutes = liveStatus?.delayMinutes ?: 0,
                                            speedKmh = liveStatus?.speedKmh ?: 0,
                                            isDark = isDark,
                                            isEnRoute = isCurrent && (liveStatus?.status?.contains("AT-STATION", ignoreCase = true) != true)
                                        )

                                        // If there are pass-through stations after this halt stop, show collapse pill / active train bar
                                        if (seg.passThroughs.isNotEmpty()) {
                                            val ptCount = seg.passThroughs.size
                                            val trackDone = RailBluePrimary
                                            val trackFuture = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                                            val segPassed = seg.passThroughs.all { (ptIdx, _) -> ptIdx < curIdx }
                                            val nextHaltIdx = if (segIdx + 1 < segments.size) segments[segIdx + 1].haltIndex else stations.size
                                            val isTrainInThisSegment = curIdx > seg.haltIndex && curIdx < nextHaltIdx
                                            val activePassThrough = seg.passThroughs.find { (ptIdx, _) -> ptIdx == curIdx }?.second ?: curStation

                                            if (isTrainInThisSegment && !isExpanded) {
                                                // ─────────────────────────────────────────────────────────────
                                                // ACTIVE TRAIN ON PLAIN BAR (Pass-Through Corridor)
                                                // ─────────────────────────────────────────────────────────────
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(IntrinsicSize.Min),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // LEFT: Scheduled / Estimated Time Column (WIMT style — no speed shown)
                                                    Column(
                                                        modifier = Modifier.width(62.dp),
                                                        horizontalAlignment = Alignment.Start,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        val timeDisplay = nextStation?.scheduledArr?.takeIf { it.isNotBlank() }
                                                            ?: curStation?.scheduledDep?.takeIf { it.isNotBlank() }
                                                            ?: ""
                                                        if (timeDisplay.isNotBlank()) {
                                                            Text(
                                                                text = timeDisplay,
                                                                fontSize = 13.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (isDark) DarkTextPrimary else LightTextPrimary
                                                            )
                                                        }
                                                    }

                                                    // CENTER: Active Track Spine with Pulsing Train Node
                                                    Column(
                                                        modifier = Modifier
                                                            .width(28.dp)
                                                            .fillMaxHeight(),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(3.dp)
                                                                .weight(1f)
                                                                .background(trackDone)
                                                        )
                                                        LiveTrainPulseNode()
                                                        Box(
                                                            modifier = Modifier
                                                                .width(3.dp)
                                                                .weight(1f)
                                                                .background(trackFuture)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(8.dp))

                                                    // RIGHT: Interactive Active Train Corridor Status Card
                                                    Card(
                                                        shape = RoundedCornerShape(12.dp),
                                                        colors = CardDefaults.cardColors(
                                                            containerColor = RailBluePrimary.copy(alpha = if (isDark) 0.16f else 0.08f)
                                                        ),
                                                        border = BorderStroke(1.dp, RailBluePrimary.copy(alpha = 0.45f)),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable {
                                                                expandedSegments.value = expandedSegments.value + segIdx
                                                            }
                                                            .padding(vertical = 2.dp)
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                                        ) {
                                                            val cardOdometer = TrainTrackerState.liveDistanceOffsetKm
                                                            val nextHaltStation = segments.getOrNull(segIdx + 1)?.haltStation
                                                            val targetStop = nextHaltStation ?: nextStation ?: upcomingStation
                                                            val curStn = curStation ?: stations.getOrNull(curIdx)
                                                            val stopDist = targetStop?.let { TrainTrackerState.getLiveRemainingKmToStation(it, curStn) }
                                                                ?: kmToNext
                                                                ?: kmToUpcoming
                                                                ?: if (targetStop != null && curStn != null && targetStop.distanceKm > curStn.distanceKm) {
                                                                    (targetStop.distanceKm - curStn.distanceKm).coerceAtLeast(0.0)
                                                                } else null

                                                            val approachingHeader = when {
                                                                stopDist != null && stopDist <= 0.3 -> "Arrived at ${targetStop?.name ?: "Station"}"
                                                                stopDist != null && stopDist <= 8.0 -> {
                                                                    val dist = stopDist.roundToInt().coerceAtLeast(1)
                                                                    "$dist km to ${targetStop?.name ?: "Next Station"}"
                                                                }
                                                                else -> {
                                                                    val cur = curStation ?: stations.getOrNull(curIdx)
                                                                    val name = cur?.name ?: ""
                                                                    if (name.isNotBlank()) {
                                                                        if (cur?.haltMinutes == 0) "Crossing $name"
                                                                        else "Departed $name"
                                                                    } else {
                                                                        "En Route"
                                                                    }
                                                                }
                                                            }

                                                            // Line 1: Header + Compact stations expand button
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = approachingHeader,
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = RailBluePrimary,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    modifier = Modifier.weight(1f, fill = false)
                                                                )

                                                                if (ptCount > 0) {
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .clip(RoundedCornerShape(20.dp))
                                                                            .background(RailBluePrimary.copy(alpha = 0.15f))
                                                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                                                    ) {
                                                                        Row(
                                                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            Text("▼", fontSize = 8.sp, color = RailBluePrimary)
                                                                            Text(
                                                                                text = "$ptCount stations",
                                                                                fontSize = 10.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = RailBluePrimary
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            // Line 2: Full width delay text (appears in one line without truncating)
                                                            val delay = liveStatus?.delayMinutes ?: 0
                                                            val delayText = when {
                                                                delay == 0 -> "On Time"
                                                                delay > 0 -> "Delayed by ${formatDelayDuration(delay)}"
                                                                else -> "${formatDelayDuration(-delay)} Early"
                                                            }
                                                            val passText = if (activePassThrough != null && activePassThrough.name.isNotBlank()) " • Passing ${activePassThrough.name}" else ""
                                                            Text(
                                                                text = "((•)) $delayText$passText",
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = if (delay <= 0) RailGreenOnTime else RailRedDelayed,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                // Standard Pass-Through Collapse/Expand Pill Row
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(IntrinsicSize.Min),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // LEFT: empty time column
                                                    Spacer(modifier = Modifier.width(62.dp))

                                                    // CENTER: spine + pill
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Track spine column
                                                        Column(
                                                            modifier = Modifier
                                                                .width(28.dp)
                                                                .fillMaxHeight(),
                                                            horizontalAlignment = Alignment.CenterHorizontally
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .width(3.dp)
                                                                    .fillMaxHeight()
                                                                    .background(if (segPassed || isTrainInThisSegment) trackDone else trackFuture)
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.width(8.dp))

                                                        // Collapse/Expand pill
                                                        Box(
                                                            modifier = Modifier
                                                                .padding(vertical = 6.dp)
                                                                .clip(RoundedCornerShape(50.dp))
                                                                .background(
                                                                    if (isTrainInThisSegment)
                                                                        RailBluePrimary.copy(alpha = 0.18f)
                                                                    else if (isDark)
                                                                        RailBluePrimary.copy(alpha = 0.12f)
                                                                    else
                                                                        RailBluePrimary.copy(alpha = 0.08f)
                                                                )
                                                                .border(
                                                                    if (isTrainInThisSegment) 1.dp else 0.dp,
                                                                    if (isTrainInThisSegment) RailBluePrimary else Color.Transparent,
                                                                    RoundedCornerShape(50.dp)
                                                                )
                                                                .clickable {
                                                                    expandedSegments.value = if (isExpanded)
                                                                        expandedSegments.value - segIdx
                                                                    else
                                                                        expandedSegments.value + segIdx
                                                                }
                                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                                        ) {
                                                            Row(
                                                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text(
                                                                    text = if (isExpanded) "▲" else "▼",
                                                                    fontSize = 8.sp,
                                                                    color = RailBluePrimary.copy(alpha = 0.7f)
                                                                )
                                                                Text(
                                                                    text = if (isExpanded)
                                                                        "Hide $ptCount pass-through station${if (ptCount > 1) "s" else ""}"
                                                                    else
                                                                        "$ptCount pass-through station${if (ptCount > 1) "s" else ""}",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Medium,
                                                                    color = RailBluePrimary.copy(alpha = 0.85f)
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // RIGHT: empty departure column
                                                    Spacer(modifier = Modifier.width(62.dp))
                                                }
                                            }

                                            // Expanded pass-through station rows
                                            if (isExpanded) {
                                                seg.passThroughs.forEachIndexed { ptRelIdx, (ptIdx, ptStn) ->
                                                    val ptPassed = ptIdx < curIdx
                                                    val ptCurrent = ptIdx == curIdx
                                                    val ptLast = ptIdx == stations.size - 1
                                                    WimtStationTrackRow(
                                                        station = ptStn,
                                                        index = ptIdx,
                                                        isPassed = ptPassed,
                                                        isCurrent = ptCurrent,
                                                        isUpcoming = ptIdx > curIdx,
                                                        isFirst = false,
                                                        isLast = ptLast,
                                                        curTrainDistKm = curStation?.distanceKm ?: 0.0,
                                                        delayMinutes = liveStatus?.delayMinutes ?: 0,
                                                        speedKmh = liveStatus?.speedKmh ?: 0,
                                                        isDark = isDark,
                                                        isEnRoute = ptCurrent && (liveStatus?.status?.contains("AT-STATION", ignoreCase = true) != true)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            }
                            TimelineViewMode.TABLE -> {
                                // =============================================
                                // VIEW 2: TABLE VIEW TIMETABLE
                                // =============================================
                                Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else LightSurface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    // Table Headers
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isDark) DarkBackground else Color(0xFFF1F5F9),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("#", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(22.dp))
                                        Text("Station", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f))
                                        Text("Arr", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(46.dp), textAlign = TextAlign.Center)
                                        Text("Dep", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(46.dp), textAlign = TextAlign.Center)
                                        Text("PF", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                                        Text("Dist", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
                                    }

                                    stations.forEachIndexed { idx, stn ->
                                        val isCurrent = idx == curIdx
                                        val isPassed = idx < curIdx
                                        val isHaltStop = stn.haltMinutes > 0

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    when {
                                                        isCurrent -> RailBluePrimary.copy(alpha = 0.1f)
                                                        !isHaltStop -> if (isDark) Color(0xFF1E293B).copy(alpha = 0.4f) else Color(0xFFF1F5F9).copy(alpha = 0.5f)
                                                        else -> Color.Transparent
                                                    },
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = if (isHaltStop) 8.dp else 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${idx + 1}",
                                                fontSize = if (isHaltStop) 11.sp else 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isHaltStop) Color.Gray else Color.Gray.copy(alpha = 0.5f),
                                                modifier = Modifier.width(22.dp)
                                            )

                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = stn.name,
                                                        fontSize = if (isHaltStop) 12.sp else 10.sp,
                                                        fontWeight = if (isCurrent) FontWeight.ExtraBold else if (isHaltStop) FontWeight.SemiBold else FontWeight.Normal,
                                                        color = if (isCurrent) RailBluePrimary
                                                                else if (!isHaltStop) Color.Gray.copy(alpha = 0.65f)
                                                                else if (isDark) DarkTextPrimary else LightTextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (isCurrent) {
                                                        Text("🟢", fontSize = 8.sp)
                                                    }
                                                }
                                                Text(
                                                    text = "${stn.code}${if (isHaltStop) " · ${stn.haltMinutes}m halt" else " · Pass"}",
                                                    fontSize = if (isHaltStop) 10.sp else 9.sp,
                                                    color = if (isHaltStop) Color.Gray else Color.Gray.copy(alpha = 0.5f)
                                                )
                                            }

                                            Text(
                                                text = if (isHaltStop) stn.scheduledArr.takeIf { it != "—" } ?: "START" else "—",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (!isHaltStop) Color.Gray.copy(alpha = 0.4f) else if (isPassed) Color.Gray else if (isDark) DarkTextPrimary else LightTextPrimary,
                                                modifier = Modifier.width(46.dp),
                                                textAlign = TextAlign.Center
                                            )

                                            Text(
                                                text = if (isHaltStop) stn.scheduledDep.takeIf { it != "—" } ?: "END" else "—",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (!isHaltStop) Color.Gray.copy(alpha = 0.4f) else if (isPassed) Color.Gray else if (isDark) DarkTextPrimary else LightTextPrimary,
                                                modifier = Modifier.width(46.dp),
                                                textAlign = TextAlign.Center
                                            )

                                            Text(
                                                text = if (isHaltStop && stn.platform.isNotBlank())
                                                    if (stn.platform.startsWith("PF")) stn.platform.removePrefix("PF") else stn.platform
                                                else "—",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isHaltStop) RailBluePrimary else Color.Gray.copy(alpha = 0.4f),
                                                modifier = Modifier.width(30.dp),
                                                textAlign = TextAlign.Center
                                            )

                                            Text(
                                                text = "${stn.distanceKm.toInt()}km",
                                                fontSize = 10.sp,
                                                color = if (isHaltStop) Color.Gray else Color.Gray.copy(alpha = 0.5f),
                                                modifier = Modifier.width(44.dp),
                                                textAlign = TextAlign.End
                                            )
                                        }

                                        if (idx < stations.size - 1) {
                                            HorizontalDivider(
                                                color = if (isDark) DarkBorder.copy(alpha = 0.3f) else LightBorder.copy(alpha = 0.3f),
                                                thickness = 0.5.dp
                                            )
                                        }
                                    }
                                }
                            }
                            }
                            TimelineViewMode.COACH -> {
                                // =============================================
                                // VIEW 3: INDIAN RAILWAYS COACH POSITION & RAKE
                                // =============================================
                                CoachPositionVisualizer(
                                    train = activeTrain,
                                    isDark = isDark
                                )
                            }
                        }
                    }
                }

                // -----------------------------------------------------
                // 4. LIVE SPEED TELEMETRY & VELOCITY PROFILE
                // -----------------------------------------------------
                item {
                    val currentSpeed = liveStatus?.speedKmh ?: 0
                    val topSpeed = when {
                        activeTrain.name.contains("Vande Bharat", ignoreCase = true) -> 160
                        activeTrain.name.contains("Rajdhani", ignoreCase = true) || activeTrain.name.contains("Shatabdi", ignoreCase = true) -> 130
                        else -> 110
                    }
                    val avgSpeed = (topSpeed * 0.68).toInt()

                    Card(
                        shape = RoundedCornerShape(0.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else LightSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Journey Route Profile",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricBox(
                                    title = "Total Route",
                                    value = "${totalKm.toInt()} km",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBox(
                                    title = "Halts",
                                    value = "${stations.count { it.haltMinutes > 0 }} Stns",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBox(
                                    title = "Total Stops",
                                    value = "${stations.size} Stns",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // =============================================================
        // MODAL: TRAIN SEARCH DIALOG
        // =============================================================
        if (showTrainSearchModal) {
            AlertDialog(
                onDismissRequest = {
                    showTrainSearchModal = false
                    searchQuery = ""
                },
                title = {
                    Text(
                        text = "Track Any Train Pan-India",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search train (e.g. 12404, Vande Bharat)...", fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isSearchingTrains) {
                            Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = RailBluePrimary)
                            }
                        } else if (searchResults.isNotEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(searchResults) { train ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                TrainTrackerState.selectTrain(train)
                                                showTrainSearchModal = false
                                                searchQuery = ""
                                            }
                                            .padding(vertical = 8.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${train.number} - ${train.name}",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${train.origin.substringBefore(" (")} ➔ ${train.destination.substringBefore(" (")}",
                                                fontSize = 11.sp,
                                                color = Color.Gray
                                            )
                                        }
                                        Text("Select →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)
                                    }
                                    HorizontalDivider(thickness = 0.5.dp)
                                }
                            }
                        } else {
                            Text(
                                text = "Enter 2+ characters to search live trains.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showTrainSearchModal = false
                        searchQuery = ""
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ── Phase 6 Master Hero: Floating Action Controls (Where Is My Train style) ──
        if (activeTrain.number.isNotBlank()) {
            val targetStop = upcomingStation ?: nextStation
            val targetDistKm = targetStop?.let { TrainTrackerState.getLiveRemainingKmToStation(it, curStation) }
                ?: kmToUpcoming
                ?: kmToNext

            // 1. Expandable "Are you inside this train?" Banner
            // Initiates / expands horizontally from the right (where the button is)
            AnimatedVisibility(
                visible = isInsideTrainBannerVisible,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 80.dp)
                    .padding(end = 16.dp, start = 16.dp)
            ) {
                WimtInsideTrainBanner(
                    activeTrain = activeTrain,
                    isDark = isDark,
                    onSwitchChanged = { checked ->
                        TrainTrackerState.toggleInsideTrainMode(checked)
                        triggerInsideTrainBanner()
                    },
                    onDismiss = {
                        isInsideTrainBannerVisible = false
                        autoCollapseJob?.cancel()
                    }
                )
            }

            // 2. Floating Action Train+Person Button (pinned on bottom right)
            // Replaces the '+' icon with "person sitting inside little train", initiates banner from right
            if (!isInsideTrainBannerVisible) {
                WimtFloatingTrainPersonButton(
                    isInside = TrainTrackerState.isInsideTrain,
                    isDark = isDark,
                    onClick = { triggerInsideTrainBanner() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 80.dp)
                )
            }

            // 3. Collapsible Floating Refresh Button (pinned at bottom-right, auto-collapses after 7s inactivity)
            CollapsibleFloatingRefreshButton(
                isRefreshing = TrainTrackerState.isAutoRefreshing,
                isDark = isDark,
                onRefresh = { TrainTrackerState.triggerManualRefresh() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 20.dp)
            )
        }

        // Phase 4: Destination Alarm Configuration BottomSheet
        if (TrainTrackerState.showAlarmSetupModal) {
            DestinationAlarmModal(
                onDismiss = { TrainTrackerState.closeAlarmSetup() }
            )
        }
    }
}

/**
 * Minimal Person-Inside-Train Vector Icon.
 * Highly recognizable minimal representation of a train carriage with a passenger sitting inside.
 */
@Composable
private fun PersonInsideTrainIcon(
    isInside: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bodyColor = if (isInside) Color(0xFF10B981) else RailBluePrimary
    val windowBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val personColor = if (isInside) Color(0xFF059669) else Color(0xFF1E293B)
    val wheelColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val headlightColor = Color(0xFFFBBF24)

    Canvas(modifier = modifier.size(width = 28.dp, height = 22.dp)) {
        val w = size.width
        val h = size.height

        // 1. Train Carriage Body (curved front on the right)
        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(x = 1.dp.toPx(), y = 2.5.dp.toPx()),
            size = Size(width = w - 3.dp.toPx(), height = h - 6.5.dp.toPx()),
            cornerRadius = CornerRadius(4.5.dp.toPx(), 4.5.dp.toPx())
        )

        // 2. Aerodynamic Front Windshield / Headlight on the right
        drawCircle(
            color = headlightColor,
            center = Offset(x = w - 4.dp.toPx(), y = h - 7.dp.toPx()),
            radius = 1.8.dp.toPx()
        )

        // 3. Passenger Window
        val windowLeft = 4.dp.toPx()
        val windowTop = 4.5.dp.toPx()
        val windowW = w - 11.dp.toPx()
        val windowH = 8.5.dp.toPx()

        drawRoundRect(
            color = windowBg,
            topLeft = Offset(x = windowLeft, y = windowTop),
            size = Size(width = windowW, height = windowH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )

        // 4. Person sitting inside looking out the window
        // Head
        val personCenterX = windowLeft + windowW * 0.45f
        val headCenterY = windowTop + 3.0.dp.toPx()
        drawCircle(
            color = personColor,
            center = Offset(x = personCenterX, y = headCenterY),
            radius = 2.0.dp.toPx()
        )

        // Torso / Shoulders
        drawRoundRect(
            color = personColor,
            topLeft = Offset(x = personCenterX - 3.0.dp.toPx(), y = headCenterY + 2.0.dp.toPx()),
            size = Size(width = 6.0.dp.toPx(), height = 3.5.dp.toPx()),
            cornerRadius = CornerRadius(1.2.dp.toPx(), 1.2.dp.toPx())
        )

        // 5. Train Wheels
        val wheelY = h - 2.2.dp.toPx()
        drawCircle(
            color = wheelColor,
            center = Offset(x = 6.dp.toPx(), y = wheelY),
            radius = 1.6.dp.toPx()
        )
        drawCircle(
            color = wheelColor,
            center = Offset(x = w - 8.dp.toPx(), y = wheelY),
            radius = 1.6.dp.toPx()
        )
    }
}

/**
 * Collapsible Floating Refresh Button (Where Is My Train style).
 * Sits at bottom-right.
 * Features:
 * - Extremely minimal: purely the circular refresh icon (like the one at the top), with NO extra arrows.
 * - Auto-collapses after 7 seconds of inactivity into the right display edge.
 * - Tapping while collapsed expands it and triggers a live refresh, resetting the 7-second timer.
 */
@Composable
private fun CollapsibleFloatingRefreshButton(
    isRefreshing: Boolean,
    isDark: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isCollapsed by remember { mutableStateOf(false) }
    var autoCollapseTimerJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun resetInactivityTimer() {
        autoCollapseTimerJob?.cancel()
        autoCollapseTimerJob = scope.launch {
            delay(7000)
            isCollapsed = true
        }
    }

    // Auto-collapse after 7 seconds of inactivity
    LaunchedEffect(Unit) {
        resetInactivityTimer()
    }

    val offsetX by animateDpAsState(
        targetValue = if (isCollapsed) 24.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "refreshButtonOffset"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .offset(x = offsetX)
            .size(46.dp)
            .clip(CircleShape)
            .background(
                color = if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE),
                shape = CircleShape
            )
            .border(1.2.dp, RailBluePrimary.copy(alpha = 0.35f), CircleShape)
            .clickable {
                if (isCollapsed) isCollapsed = false
                resetInactivityTimer()
                onRefresh()
            }
    ) {
        Text(
            text = if (isRefreshing) "⏳" else "🔄",
            fontSize = 19.sp
        )
    }
}

/**
 * Floating Train+Person Button pinned on bottom right (Where Is My Train style).
 * Displays a minimal recognizable "person inside train" logo.
 * Tapping opens the Inside-the-Train banner, expanding from the right.
 */
@Composable
private fun WimtFloatingTrainPersonButton(
    isInside: Boolean,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isInside) {
            if (isDark) Color(0xFF065F46) else Color(0xFFDCFCE7)
        } else {
            if (isDark) Color(0xFF1E293B) else Color(0xFFE0F2FE)
        },
        shadowElevation = 6.dp,
        border = BorderStroke(
            1.2.dp,
            if (isInside) Color(0xFF10B981) else RailBluePrimary.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .size(48.dp)
            .clickable { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            PersonInsideTrainIcon(isInside = isInside, isDark = isDark)

            if (isInside) {
                // Active satellite beacon dot at top right
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981))
                )
            }
        }
    }
}

/**
 * Expandable "Are you inside this train?" Banner.
 * Initiates / expands from the right, and automatically collapses after 7 seconds.
 */
@Composable
private fun WimtInsideTrainBanner(
    activeTrain: Train,
    isDark: Boolean,
    onSwitchChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val isInside = TrainTrackerState.isInsideTrain

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInside) {
                if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)
            } else {
                if (isDark) Color(0xFF1E293B) else Color.White
            }
        ),
        border = BorderStroke(
            1.5.dp,
            if (isInside) Color(0xFF10B981)
            else if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFF93C5FD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            if (isInside) Color(0xFF10B981).copy(alpha = 0.20f)
                            else if (isDark) Color(0xFF334155) else Color(0xFFDBEAFE)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    PersonInsideTrainIcon(isInside = isInside, isDark = isDark)
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    val headerText = if (isInside) {
                        when (TrainTrackerState.activeTrackingProvider) {
                            "Trilateration" -> "Inside ${activeTrain.number} (Trilateration)"
                            "Cell Tower" -> "Inside ${activeTrain.number} (Cell Tower)"
                            "GPS" -> "Inside ${activeTrain.number} (GPS)"
                            else -> "Inside ${activeTrain.number}"
                        }
                    } else "Are you inside this train?"

                    Text(
                        text = headerText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isInside) (if (isDark) Color(0xFF34D399) else Color(0xFF065F46))
                        else if (isDark) DarkTextPrimary else LightTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val subText = if (isInside) {
                        when (TrainTrackerState.activeTrackingProvider) {
                            "Trilateration" -> "Multi-Tower Trilateration active (±80m) • Auto-collapses in 7s"
                            "Cell Tower" -> "Offline Cell Tower tracking active • Auto-collapses in 7s"
                            "GPS" -> "Zero-lag satellite GPS tracking active • Auto-collapses in 7s"
                            else -> "Zero-lag live tracking active • Auto-collapses in 7s"
                        }
                    } else {
                        "Turn ON for zero-lag GPS & Multi-Tower tracking"
                    }
                    Text(
                        text = subText,
                        fontSize = 10.sp,
                        color = if (isInside) (if (isDark) Color(0xFF6EE7B7) else Color(0xFF047857))
                        else if (isDark) DarkTextSecondary else LightTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = isInside,
                    onCheckedChange = onSwitchChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF10B981),
                        uncheckedThumbColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        uncheckedTrackColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                    )
                )

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Text("✕", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

/**
 * Concentric pulsing sonar rings + train badge anchored rock-solid to the track
 */
@Composable
private fun LiveTrainPulseNode(
    modifier: Modifier = Modifier,
    isRunning: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "trainPulse")

    val outerRingScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerScale"
    )
    val outerRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerAlpha"
    )

    val innerRingScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.22f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerScale"
    )
    val innerRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerAlpha"
    )

    val trainScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "trainScale"
    )

    // Lighter App Theme Blue palette (Sky/Cyan theme blue)
    val lightThemeBlue = Color(0xFF38BDF8)
    val primaryThemeBlue = RailBluePrimary // 0xFF0284C7

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(34.dp)
    ) {
        // Outermost radar pulse ring (lighter app theme blue)
        Box(
            modifier = Modifier
                .size(30.dp)
                .scale(outerRingScale)
                .clip(CircleShape)
                .background(lightThemeBlue.copy(alpha = outerRingAlpha))
        )
        // Inner radar pulse ring (app theme blue)
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(innerRingScale)
                .clip(CircleShape)
                .background(primaryThemeBlue.copy(alpha = innerRingAlpha))
        )
        // Train badge in app's theme blue gradient, brightly popping with white border
        Box(
            modifier = Modifier
                .size(23.dp)
                .scale(trainScale)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7))
                    )
                )
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🚆", fontSize = 12.sp)
        }
    }
}

/**
 * Adds delay minutes to a time string (e.g. "15:55" + 50 -> "16:45")
 */
private fun addMinutesToTimeString(timeStr: String, minutes: Int): String? {
    if (timeStr == "—" || timeStr == "START" || timeStr == "END" || !timeStr.contains(":")) return null
    return try {
        val parts = timeStr.trim().split(":")
        val h = parts[0].toInt()
        val m = parts[1].take(2).toInt()
        var totalMinutes = h * 60 + m + minutes
        while (totalMinutes < 0) totalMinutes += 1440
        totalMinutes %= 1440
        val newH = totalMinutes / 60
        val newM = totalMinutes % 60
        "${newH.toString().padStart(2, '0')}:${newM.toString().padStart(2, '0')}"
    } catch (_: Exception) {
        null
    }
}

/**
 * Individual Station Node & Vertical Spine Component in Where Is My Train (WIMT) Format
 */
@Composable
private fun WimtStationTrackRow(
    station: Station,
    index: Int,
    isPassed: Boolean,
    isCurrent: Boolean,
    isUpcoming: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    curTrainDistKm: Double,
    delayMinutes: Int,
    speedKmh: Int,
    isDark: Boolean,
    isEnRoute: Boolean = false  // true = train departed this station and is now between stops
) {
    val isHaltStop = station.haltMinutes > 0  // true = scheduled halt; false = pass-through

    val trackDoneColor = RailBluePrimary
    val trackFutureColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val passThroughColor = RailBluePrimary.copy(alpha = 0.28f) // light translucent for non-halt nodes

    // Read live odometer state so Compose recomposes every station row in real time each second
    val liveOffset = TrainTrackerState.liveDistanceOffsetKm

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // -------------------------------------------------------------
        // LEFT COLUMN: Scheduled & Actual Arrival Time
        // (hide times for pass-through stations – they have no dwell)
        // -------------------------------------------------------------
        Column(
            modifier = Modifier.width(62.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center
        ) {
            if (isHaltStop) {
                val arrTime = station.scheduledArr.takeIf { it != "—" } ?: "START"
                Text(
                    text = arrTime,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isPassed) Color.Gray else if (isDark) DarkTextPrimary else LightTextPrimary
                )
                // Projected / Actual Live Arrival Time (Where Is My Train style)
                val liveArrTime = addMinutesToTimeString(arrTime, delayMinutes)
                if (liveArrTime != null && liveArrTime != arrTime) {
                    Text(
                        text = liveArrTime,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (delayMinutes > 0) RailRedDelayed else RailGreenOnTime
                    )
                }

                // Subtle Alarm bell icon for upcoming stations
                if (!isPassed) {
                    val isStationArmed = TrainTrackerState.activeAlarm?.let {
                        it.isArmed && (it.stationCode.equals(station.code, ignoreCase = true) || it.stationName.contains(station.name, ignoreCase = true))
                    } ?: false

                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable {
                                TrainTrackerState.openAlarmSetup(station)
                            }
                    ) {
                        Text(
                            text = if (isStationArmed) "🔔" else "⏰",
                            fontSize = 12.sp,
                            modifier = Modifier.alpha(if (isStationArmed) 1f else 0.45f)
                        )
                    }
                }
            }
            // pass-through: left column stays empty
        }

        // -------------------------------------------------------------
        // CENTER COLUMN: Track Spine + Station Node + Station Details
        // -------------------------------------------------------------
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vertical Track Spine & Station Marker Node
            Column(
                modifier = Modifier
                    .width(28.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top segment line
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .weight(1f)
                        .background(
                            if (isFirst) Color.Transparent
                            else if (isPassed || isCurrent) trackDoneColor
                            else trackFutureColor
                        )
                )

                // Station Node Icon
                if (isCurrent) {
                    LiveTrainPulseNode()
                } else if (isHaltStop) {
                    // ● Halt station: dark blue filled circle (passed = solid, future = slightly lighter)
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isPassed) trackDoneColor else RailBluePrimary)
                    )
                } else {
                    // ○ Pass-through station: smaller translucent hollow ring
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isDark) DarkSurface else LightSurface)
                            .border(1.5.dp, passThroughColor, CircleShape)
                    )
                }

                // Bottom segment line
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .weight(1f)
                        .background(
                            if (isLast) Color.Transparent
                            else if (isPassed) trackDoneColor
                            else trackFutureColor
                        )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Station Name, Details & Speech-Bubble Card
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = if (isHaltStop) 8.dp else 3.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = station.name,
                    fontSize = if (isHaltStop) 13.sp else 11.sp,
                    fontWeight = if (isCurrent) FontWeight.ExtraBold else if (isHaltStop) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) RailBluePrimary
                            else if (!isHaltStop) Color.Gray.copy(alpha = 0.75f)
                            else if (isDark) DarkTextPrimary else LightTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Sub-info row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isHaltStop) {
                        if (station.platform.isNotBlank()) {
                            Text(
                                text = if (station.platform.startsWith("PF")) station.platform else "PF ${station.platform}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = RailBluePrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        if (station.distanceKm > 0) {
                            Text(
                                text = "• ${station.distanceKm.toInt()} km",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        if (station.haltMinutes > 0) {
                            Text(
                                text = "• Halt ${station.haltMinutes}m",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        // Pass-through: show distance + "No halt" label
                        if (station.distanceKm > 0) {
                            Text(
                                text = "${station.distanceKm.toInt()} km",
                                fontSize = 9.sp,
                                color = Color.Gray.copy(alpha = 0.6f),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        Text(
                            text = "· No halt",
                            fontSize = 9.sp,
                            color = Color.Gray.copy(alpha = 0.5f),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                                // If this is the current live station, show floating Live Speech-Bubble
                if (isCurrent) {
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = RailBluePrimary.copy(alpha = 0.08f)
                        ),
                        border = BorderStroke(1.dp, RailBluePrimary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val bubbleTitle = when {
                                    station.haltMinutes == 0 -> "Crossing ${station.name}"
                                    isEnRoute -> "Departed ${station.name}"
                                    else -> "At ${station.name}"
                                }
                                Text(
                                    text = bubbleTitle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RailBluePrimary
                                )
                            }
                            val curDelayText = when {
                                delayMinutes == 0 -> "Running on time"
                                delayMinutes > 0 -> "Delayed by ${formatDelayDuration(delayMinutes)}"
                                else -> "${formatDelayDuration(-delayMinutes)} early"
                            }
                            Text(
                                text = curDelayText,
                                fontSize = 10.sp,
                                color = if (delayMinutes <= 0) RailGreenOnTime else RailRedDelayed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // -------------------------------------------------------------
        // RIGHT COLUMN: Departure Time (halt stations only)
        // -------------------------------------------------------------
        Column(
            modifier = Modifier.width(62.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            if (isHaltStop) {
                val depTime = station.scheduledDep.takeIf { it != "—" } ?: "END"
                Text(
                    text = depTime,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isPassed) Color.Gray else if (isDark) DarkTextPrimary else LightTextPrimary
                )
                // Projected / Actual Live Departure Time (Where Is My Train style)
                val liveDepTime = addMinutesToTimeString(depTime, delayMinutes)
                if (liveDepTime != null && liveDepTime != depTime) {
                    Text(
                        text = liveDepTime,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (delayMinutes > 0) RailRedDelayed else RailGreenOnTime
                    )
                } else if (isCurrent && delayMinutes > 0) {
                    Text(
                        text = "+${formatDelayDuration(delayMinutes)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RailRedDelayed
                    )
                }

                // Subtle Alarm bell icon for upcoming stations
                if (!isPassed) {
                    val isStationArmed = TrainTrackerState.activeAlarm?.let {
                        it.isArmed && (it.stationCode.equals(station.code, ignoreCase = true) || it.stationName.contains(station.name, ignoreCase = true))
                    } ?: false

                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable {
                                TrainTrackerState.openAlarmSetup(station)
                            }
                    ) {
                        Text(
                            text = if (isStationArmed) "🔔" else "⏰",
                            fontSize = 12.sp,
                            modifier = Modifier.alpha(if (isStationArmed) 1f else 0.45f)
                        )
                    }
                }
            }
            // pass-through: right column stays empty
        }
    }
}
