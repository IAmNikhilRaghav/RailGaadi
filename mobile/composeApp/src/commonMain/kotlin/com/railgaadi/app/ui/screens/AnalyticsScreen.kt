package com.railgaadi.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
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
import com.railgaadi.app.ui.components.LiveAutoRefreshPill
import com.railgaadi.app.ui.components.MetricBox
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class TimelineViewMode {
    WIMT, TABLE
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

    // Search dialog state to change or pick any train
    var showTrainSearchModal by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Train>>(emptyList()) }
    var isSearchingTrains by remember { mutableStateOf(false) }

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
            val foundIdx = stations.indexOfFirst {
                it.name.contains(stnName, ignoreCase = true) ||
                (stnName.contains(it.code, ignoreCase = true) && it.code.isNotBlank())
            }
            if (foundIdx >= 0) foundIdx
            else {
                val pct = liveStatus?.progressPercent ?: 0f
                ((stations.size - 1) * pct).toInt().coerceIn(0, stations.size - 1)
            }
        }
    }

    val curStation = stations.getOrNull(curIdx)
    val nextStnIdx = if (curIdx + 1 < stations.size) curIdx + 1 else null
    val nextStation = nextStnIdx?.let { stations.getOrNull(it) }

    val kmToNext = remember(curStation, nextStation) {
        if (curStation != null && nextStation != null && nextStation.distanceKm > curStation.distanceKm) {
            ((nextStation.distanceKm - curStation.distanceKm) * 10).toInt() / 10.0
        } else null
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
                        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else LightSurface),
                        border = BorderStroke(1.dp, if (isDark) DarkBorder else RailBluePrimary.copy(alpha = 0.25f)),
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
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else LightSurface),
                                    border = BorderStroke(1.dp, if (isDark) DarkBorder else LightBorder),
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
                contentPadding = PaddingValues(bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // -----------------------------------------------------
                // 1. ACTIVE TRAIN HEADER
                // -----------------------------------------------------
                item {
                    Card(
                        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else LightSurface),
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
                            .padding(horizontal = 16.dp)
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
                                                Brush.linearGradient(listOf(RailBluePrimary, RailBlueDark)),
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
                                            text = nextStation?.name ?: "Terminus Approaching",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDark) DarkTextPrimary else LightTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                if (kmToNext != null) {
                                    Box(
                                        modifier = Modifier
                                            .background(RailBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "$kmToNext km away",
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Delay Status Pill
                                val (statusText, statusBg, statusColor) = when {
                                    delay == 0 -> Triple("● On Time", RailGreenOnTime.copy(alpha = 0.12f), RailGreenOnTime)
                                    delay > 0 -> Triple("● +${delay}m Late", RailRedDelayed.copy(alpha = 0.12f), RailRedDelayed)
                                    else -> Triple("● ${-delay}m Early", RailGreenOnTime.copy(alpha = 0.12f), RailGreenOnTime)
                                }

                                Box(
                                    modifier = Modifier
                                        .background(statusBg, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(statusText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                }

                                // Total Distance
                                if (totalKm > 0) {
                                    Box(
                                        modifier = Modifier
                                            .background(if (isDark) DarkBackground else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text("📏 $totalKm km total", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = if (isDark) DarkTextPrimary else LightTextPrimary)
                                    }
                                }

                                // Current Speed
                                Box(
                                    modifier = Modifier
                                        .background(if (isDark) DarkBackground else Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text("⚡ ${liveStatus?.speedKmh ?: 0} km/h", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)
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
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header with Title & View Switcher
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Full Journey Timeline",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) DarkTextPrimary else LightTextPrimary
                            )

                            // Switcher: Overview (WIMT) vs Description (Table)
                            Row(
                                modifier = Modifier
                                    .background(
                                        if (isDark) DarkSurface else Color(0xFFE2E8F0),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { viewMode = TimelineViewMode.WIMT }
                                        .background(
                                            if (viewMode == TimelineViewMode.WIMT) RailBluePrimary else Color.Transparent
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Overview",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (viewMode == TimelineViewMode.WIMT) Color.White else if (isDark) DarkTextSecondary else LightTextSecondary
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { viewMode = TimelineViewMode.TABLE }
                                        .background(
                                            if (viewMode == TimelineViewMode.TABLE) RailBluePrimary else Color.Transparent
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Table",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (viewMode == TimelineViewMode.TABLE) Color.White else if (isDark) DarkTextSecondary else LightTextSecondary
                                    )
                                }
                            }
                        }

                        // =============================================
                        // VIEW 1: WHERE IS MY TRAIN (WIMT) VERTICAL TRACK
                        // =============================================
                        if (viewMode == TimelineViewMode.WIMT) {
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
                                            isDark = isDark
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
                                            isDark = isDark
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
                                                    // LEFT: Live Speed & Telemetry Column
                                                    Column(
                                                        modifier = Modifier.width(62.dp),
                                                        horizontalAlignment = Alignment.Start,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = "LIVE",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = RailGreenOnTime
                                                        )
                                                        val curSpeed = liveStatus?.speedKmh ?: 85
                                                        Text(
                                                            text = "${curSpeed} km/h",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = RailBluePrimary
                                                        )
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
                                                            .padding(vertical = 4.dp)
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(10.dp),
                                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Row(
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    modifier = Modifier.weight(1f, fill = false)
                                                                ) {
                                                                    Text(
                                                                        text = "🚆 Crossing ${activePassThrough?.name ?: liveStatus?.currentStation ?: "Pass-through"}",
                                                                        fontSize = 12.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = RailBluePrimary,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )
                                                                }
                                                                Text(
                                                                    text = if (TrainTrackerState.isAutoRefreshing) "● UPDATING…" else "● LIVE · ${TrainTrackerState.autoRefreshCountdown}s",
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = if (TrainTrackerState.isAutoRefreshing) RailBluePrimary else RailGreenOnTime
                                                                )
                                                            }

                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                val delay = liveStatus?.delayMinutes ?: 0
                                                                Text(
                                                                    text = if (delay == 0) "Running On Time" else "Delayed +${delay}m",
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    color = if (delay == 0) RailGreenOnTime else RailRedDelayed
                                                                )

                                                                // Interactive Expand Pill Button
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(20.dp))
                                                                        .background(RailBluePrimary.copy(alpha = 0.15f))
                                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                                ) {
                                                                    Row(
                                                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
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
                                                                        "Hide $ptCount pass-through station${if (ptCount > 1) "s" else ""}" + if (isTrainInThisSegment) " (● Live inside)" else ""
                                                                    else
                                                                        "$ptCount pass-through station${if (ptCount > 1) "s" else ""}",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = if (isTrainInThisSegment) FontWeight.Bold else FontWeight.Medium,
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
                                                        isDark = isDark
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
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
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else LightSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
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
                                Column {
                                    Text(
                                        text = "Speed Velocity Profile",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (isDark) DarkTextPrimary else LightTextPrimary
                                    )
                                    Text(
                                        text = "Continuous speed telemetry & acceleration zones",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .background(RailBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when {
                                            currentSpeed >= 100 -> "🚀 High-Speed Express"
                                            currentSpeed >= 60 -> "⚡ Normal Cruising"
                                            currentSpeed > 0 -> "⚠️ Deceleration Zone"
                                            else -> "🛑 Platform Halt"
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RailBluePrimary
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MetricBox(
                                    title = "Live Speed",
                                    value = "$currentSpeed km/h",
                                    icon = "⚡",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBox(
                                    title = "Top Speed",
                                    value = "$topSpeed km/h",
                                    icon = "🚀",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricBox(
                                    title = "Avg Speed",
                                    value = "$avgSpeed km/h",
                                    icon = "⏱️",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // -----------------------------------------------------
                // 5. SCHEDULE VARIANCE & PUNCTUALITY
                // -----------------------------------------------------
                item {
                    val delay = liveStatus?.delayMinutes ?: 0
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else LightSurface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Schedule Variance & Punctuality",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) DarkTextPrimary else LightTextPrimary
                            )
                            Text(
                                text = "Station-by-station adherence to Indian Railways timetable",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            val punctualityPercent = if (delay <= 0) 96 else (96 - (delay * 0.8).toInt()).coerceIn(40, 99)

                            LinearProgressIndicator(
                                progress = { punctualityPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp)),
                                color = if (delay <= 0) RailGreenOnTime else RailAmberWarning,
                                trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$punctualityPercent% Journey on schedule",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (delay <= 0) RailGreenOnTime else RailAmberWarning
                                )

                                Text(
                                    text = if (delay <= 0) "Running on schedule" else "Delayed by $delay mins",
                                    fontSize = 11.sp,
                                    color = Color.Gray
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
    }
}

/**
 * Concentric pulsing sonar rings + train badge
 */
@Composable
private fun LiveTrainPulseNode(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "trainPulse")

    val outerRingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerScale"
    )
    val outerRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerAlpha"
    )

    val innerRingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerScale"
    )
    val innerRingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerAlpha"
    )

    val badgeScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badgeScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(36.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .scale(outerRingScale)
                .clip(CircleShape)
                .background(RailBluePrimary.copy(alpha = outerRingAlpha))
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(innerRingScale)
                .clip(CircleShape)
                .background(RailBluePrimary.copy(alpha = innerRingAlpha))
        )
        Box(
            modifier = Modifier
                .size(24.dp)
                .scale(badgeScale)
                .clip(CircleShape)
                .background(RailBluePrimary)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("🚆", fontSize = 11.sp)
        }
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
    isDark: Boolean
) {
    val isHaltStop = station.haltMinutes > 0  // true = scheduled halt; false = pass-through

    val trackDoneColor = RailBluePrimary
    val trackFutureColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val passThroughColor = RailBluePrimary.copy(alpha = 0.28f) // light translucent for non-halt nodes

    val kmRemaining = if (isUpcoming && station.distanceKm > curTrainDistKm) {
        ((station.distanceKm - curTrainDistKm) * 10).toInt() / 10.0
    } else null

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
                if (isCurrent && delayMinutes > 0) {
                    Text(
                        text = "+${delayMinutes}m",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RailRedDelayed
                    )
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
                                color = RailBluePrimary
                            )
                        }
                        if (station.distanceKm > 0) {
                            Text(
                                text = "• ${station.distanceKm.toInt()} km",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                        if (station.haltMinutes > 0) {
                            Text(
                                text = "• Halt ${station.haltMinutes}m",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        // Pass-through: show distance + "No halt" label
                        if (station.distanceKm > 0) {
                            Text(
                                text = "${station.distanceKm.toInt()} km",
                                fontSize = 9.sp,
                                color = Color.Gray.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = "· No halt",
                            fontSize = 9.sp,
                            color = Color.Gray.copy(alpha = 0.5f)
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
                                Text(
                                    text = "At ${station.name}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RailBluePrimary
                                )
                                Text(
                                    text = if (TrainTrackerState.isAutoRefreshing) "● UPDATING…" else "● LIVE · ${TrainTrackerState.autoRefreshCountdown}s",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (TrainTrackerState.isAutoRefreshing) RailBluePrimary else RailGreenOnTime
                                )
                            }
                            Text(
                                text = if (delayMinutes == 0) "Running on time • ${speedKmh} km/h"
                                else "Delayed by $delayMinutes mins • ${speedKmh} km/h",
                                fontSize = 10.sp,
                                color = if (delayMinutes == 0) RailGreenOnTime else RailRedDelayed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Upcoming distance remaining tag
                if (isUpcoming && kmRemaining != null) {
                    Text(
                        text = "📍 $kmRemaining km remaining",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = RailBluePrimary
                    )
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
                if (isCurrent && delayMinutes > 0) {
                    Text(
                        text = "+${delayMinutes}m",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = RailRedDelayed
                    )
                }
            }
            // pass-through: right column stays empty
        }
    }
}
