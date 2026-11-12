package com.railgaadi.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.model.CoachClass
import com.railgaadi.app.model.CoachDataService
import com.railgaadi.app.model.CoachPosition
import com.railgaadi.app.model.SeatBerth
import com.railgaadi.app.model.Train
import com.railgaadi.app.theme.DarkBorder
import com.railgaadi.app.theme.DarkTextPrimary
import com.railgaadi.app.theme.DarkTextSecondary
import com.railgaadi.app.theme.LightBorder
import com.railgaadi.app.theme.LightTextPrimary
import com.railgaadi.app.theme.LightTextSecondary
import com.railgaadi.app.theme.RailBlueDark
import com.railgaadi.app.theme.RailBluePrimary
import com.railgaadi.app.theme.RailGreenOnTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachPositionVisualizer(
    train: Train,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val rake = remember(train.number, train.name, train.type) {
        CoachDataService.getRakeComposition(train.number, train.name, train.type)
    }

    var selectedCoach by remember { mutableStateOf<CoachPosition?>(null) }
    var highlightedCoachIndex by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val lazyListState = rememberLazyListState()

    // Filter coaches by category
    val filteredCoaches = remember(rake.coaches, selectedFilter) {
        when (selectedFilter) {
            "AC" -> rake.coaches.filter { it.isAirConditioned && !it.isLocomotive }
            "Sleeper" -> rake.coaches.filter { it.coachClass == CoachClass.SLEEPER }
            "General" -> rake.coaches.filter { it.coachClass == CoachClass.GENERAL_UNRESERVED || it.coachClass == CoachClass.LUGGAGE_BRAKE_VAN }
            "Pantry" -> rake.coaches.filter { it.coachClass == CoachClass.PANTRY_CAR }
            else -> rake.coaches
        }
    }

    // Scroll directly to a searched coach code (e.g. "B4", "E1", "S2") OR seat number (e.g. "45", "12")
    fun scrollToCoachOrSeat(query: String) {
        val clean = query.trim().uppercase()
        if (clean.isBlank()) return

        // 1. Direct coach code match (e.g. "B4", "S2", "GS1", "C2")
        val matchIndex = rake.coaches.indexOfFirst { it.coachCode.equals(clean, ignoreCase = true) }
        if (matchIndex >= 0) {
            highlightedCoachIndex = matchIndex
            coroutineScope.launch {
                lazyListState.animateScrollToItem((matchIndex - 1).coerceAtLeast(0))
            }
            return
        }

        // 2. Numeric seat / berth search (e.g. "42", "72", "15")
        val seatNum = clean.toIntOrNull()
        if (seatNum != null && seatNum in 1..80) {
            val candidateIndex = rake.coaches.indexOfFirst {
                !it.isLocomotive && it.coachClass != CoachClass.PANTRY_CAR &&
                it.coachClass != CoachClass.LUGGAGE_BRAKE_VAN &&
                seatNum <= it.totalSeats
            }
            if (candidateIndex >= 0) {
                highlightedCoachIndex = candidateIndex
                coroutineScope.launch {
                    lazyListState.animateScrollToItem((candidateIndex - 1).coerceAtLeast(0))
                }
            }
        }
    }

    val cardBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.88f)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ── 1. RAKE SUMMARY HEADER CARD ──────────────────────────────────────
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Title on left, Coaches count & Engine pill on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Train Composition",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) DarkTextPrimary else LightTextPrimary
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(RailBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .border(0.5.dp, RailBluePrimary.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${rake.totalCoaches} Coaches",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RailBluePrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    if (rake.trainType.contains("Vande Bharat", ignoreCase = true)) Color(0xFF1E3A8A).copy(alpha = 0.15f)
                                    else Color(0xFFDC2626).copy(alpha = 0.12f),
                                    RoundedCornerShape(8.dp)
                                )
                                .border(
                                    0.5.dp,
                                    if (rake.trainType.contains("Vande Bharat", ignoreCase = true)) RailBluePrimary.copy(alpha = 0.3f)
                                    else Color(0xFFDC2626).copy(alpha = 0.25f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (rake.trainType.contains("Vande Bharat", ignoreCase = true)) "⚡ EMU 160" else "⚡ ${rake.engineType.split(" ").firstOrNull() ?: "WAP-7"}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (rake.trainType.contains("Vande Bharat", ignoreCase = true)) RailBluePrimary else Color(0xFFDC2626)
                            )
                        }
                    }
                }

                // 2. Train Number & Name
                Text(
                    text = "${rake.trainNumber} • ${rake.trainName}",
                    fontSize = 12.sp,
                    color = if (isDark) DarkTextSecondary else LightTextSecondary,
                    maxLines = 1
                )

                // 3. Locomotive & Direction
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🚂 ${rake.engineType}",
                        fontSize = 11.sp,
                        color = if (isDark) DarkTextSecondary else LightTextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Front: Engine → Rear: Guard",
                        fontSize = 10.5.sp,
                        color = RailGreenOnTime,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── 2. QUICK COACH OR SEAT SEARCH BAR (iOS Glass Capsule) ─────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isDark) Color(0xFF1E293B).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.90f),
                    RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (searchQuery.isNotBlank()) RailBluePrimary
                           else if (isDark) Color.White.copy(alpha = 0.12f)
                           else Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "🔍",
                    fontSize = 13.sp,
                    color = if (searchQuery.isNotBlank()) RailBluePrimary else Color.Gray
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search coach or seat (e.g. B4, S1, 42)",
                            fontSize = 12.5.sp,
                            color = if (isDark) DarkTextSecondary.copy(alpha = 0.7f) else LightTextSecondary.copy(alpha = 0.7f)
                        )
                    }

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            if (it.isNotBlank()) scrollToCoachOrSeat(it)
                            else highlightedCoachIndex = null
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary
                        ),
                        cursorBrush = SolidColor(RailBluePrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (searchQuery.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            .clickable {
                                searchQuery = ""
                                highlightedCoachIndex = null
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Category filter chips: All, AC, Sleeper, General, Pantry
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val filters = listOf("All", "AC", "Sleeper", "General", "Pantry")
            items(filters.size) { idx ->
                val f = filters[idx]
                val isSelected = selectedFilter == f
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) RailBluePrimary else cardBg)
                        .border(
                            0.8.dp,
                            if (isSelected) RailBluePrimary else cardBorder,
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { selectedFilter = f }
                        .padding(horizontal = 13.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = f,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else if (isDark) DarkTextPrimary else LightTextPrimary
                    )
                }
            }
        }

        // ── 3. HORIZONTAL PANORAMIC TRAIN RAKE VISUALIZER ────────────────────
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            border = BorderStroke(1.dp, cardBorder),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Rake Layout (Tap coach for berth map)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DarkTextSecondary else LightTextSecondary
                    )
                    Text(
                        text = "◀ Swipe track ▶",
                        fontSize = 10.5.sp,
                        color = RailBluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Horizontal scrolling train rake with tracks underneath
                Box(modifier = Modifier.fillMaxWidth()) {
                    // Steel Rails Drawn Beneath
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 6.dp)
                    ) {
                        // Rail steel track line
                        drawLine(
                            color = Color(0xFF64748B),
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }

                    LazyRow(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        itemsIndexed(filteredCoaches) { index, coach ->
                            val isHighlighted = highlightedCoachIndex == rake.coaches.indexOf(coach)
                            CoachBoxCard(
                                coach = coach,
                                isDark = isDark,
                                isHighlighted = isHighlighted,
                                onClick = {
                                    if (coach.totalSeats > 0) {
                                        selectedCoach = coach
                                    }
                                }
                            )

                            // Coupler link between coaches
                            if (index < filteredCoaches.size - 1) {
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(4.dp)
                                        .background(Color(0xFF475569), RoundedCornerShape(1.dp))
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── 4. INTERACTIVE SEAT & BERTH MAP MODAL BOTTOM SHEET ────────────────────
    selectedCoach?.let { coach ->
        ModalBottomSheet(
            onDismissRequest = { selectedCoach = null },
            containerColor = if (isDark) Color(0xFF0F172A) else Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            CoachSeatMapSheetContent(coach = coach, isDark = isDark, onClose = { selectedCoach = null })
        }
    }
}

/**
 * Individual Coach Card in the Horizontal Train View
 */
@Composable
private fun CoachBoxCard(
    coach: CoachPosition,
    isDark: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else if (isHighlighted) 1.05f else 1.0f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "coach_scale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "highlight_glow")
    val highlightAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "highlight_alpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        // Coach Compartment Body
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            border = BorderStroke(
                width = if (isHighlighted) 2.5.dp else 1.dp,
                color = if (isHighlighted) RailBluePrimary.copy(alpha = highlightAlpha)
                else if (isDark) Color.White.copy(alpha = 0.15f)
                else Color.Black.copy(alpha = 0.12f)
            ),
            shadowElevation = if (isHighlighted) 8.dp else 2.dp,
            modifier = Modifier
                .width(76.dp)
                .height(86.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Livery Top Stripe (Color coded by train / class)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .background(coach.primaryColor)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Coach Code (e.g. "B3", "E1", "ENG", "SLR")
                    Text(
                        text = coach.coachCode,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        color = if (coach.isLocomotive) Color(0xFFDC2626) else RailBluePrimary,
                        textAlign = TextAlign.Center
                    )

                    // Windows illustration
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(width = 12.dp, height = 9.dp)
                                    .background(
                                        if (coach.isAirConditioned) Color(0xFF38BDF8).copy(alpha = 0.5f)
                                        else Color(0xFF94A3B8).copy(alpha = 0.4f),
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }

                    // Coach Class Pill
                    Box(
                        modifier = Modifier
                            .background(
                                coach.secondaryColor.copy(alpha = 0.20f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = coach.coachClass.shortName,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF0F172A),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Undercarriage Bogie Wheels (◯───◯)
        Row(
            modifier = Modifier.width(64.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color(0xFF334155), CircleShape)
            )
            Box(
                modifier = Modifier
                    .height(2.dp)
                    .weight(1f)
                    .background(Color(0xFF475569))
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color(0xFF334155), CircleShape)
            )
        }
    }
}

/**
 * Seat & Berth Map Sheet Content with Indian Railways Standard Layout
 */
@Composable
private fun CoachSeatMapSheetContent(
    coach: CoachPosition,
    isDark: Boolean,
    onClose: () -> Unit
) {
    val seats = remember(coach) { CoachDataService.generateSeatLayout(coach) }
    var seatFilterQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sheet Header: Coach Info & Close
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Coach ${coach.coachCode} • ${coach.displayName}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                )
                Text(
                    text = "${coach.totalSeats} Total Berths/Seats • ${if (coach.isAirConditioned) "Air-Conditioned" else "Non-AC"}",
                    fontSize = 12.sp,
                    color = if (isDark) DarkTextSecondary else LightTextSecondary
                )
            }

            IconButton(onClick = onClose) {
                Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Amenities Pills: Charging, Toilets, Exits
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AmenityPill("⚡ 220V Sockets", isDark)
            AmenityPill("🚻 4 Bio-Toilets", isDark)
            AmenityPill("🚪 Emergency Exit", isDark)
        }

        // Search Seat Number (e.g. "Find seat 43") - iOS Glass Capsule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isDark) Color(0xFF1E293B).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.90f),
                    RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (seatFilterQuery.isNotBlank()) RailBluePrimary
                           else if (isDark) Color.White.copy(alpha = 0.12f)
                           else Color.Black.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💺",
                    fontSize = 14.sp
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (seatFilterQuery.isEmpty()) {
                        Text(
                            text = "Find seat or berth (e.g. 24, 45)...",
                            fontSize = 12.5.sp,
                            color = if (isDark) DarkTextSecondary.copy(alpha = 0.7f) else LightTextSecondary.copy(alpha = 0.7f)
                        )
                    }

                    BasicTextField(
                        value = seatFilterQuery,
                        onValueChange = { seatFilterQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary
                        ),
                        cursorBrush = SolidColor(RailBluePrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (seatFilterQuery.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            .clickable { seatFilterQuery = "" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Berth Type Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendItem("Lower", Color(0xFF10B981))
            LegendItem("Middle", Color(0xFF3B82F6))
            LegendItem("Upper", Color(0xFF8B5CF6))
            LegendItem("Side", Color(0xFFF59E0B))
            LegendItem("Window", RailBluePrimary)
        }

        // Interactive Seat Grid (Scrollable)
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 340.dp)
        ) {
            items(seats) { seat ->
                val isSearched = seatFilterQuery.isNotBlank() && seat.seatNumber.toString() == seatFilterQuery.trim()
                val berthColor = when {
                    seat.berthType.contains("Lower", ignoreCase = true) -> Color(0xFF10B981)
                    seat.berthType.contains("Middle", ignoreCase = true) -> Color(0xFF3B82F6)
                    seat.berthType.contains("Upper", ignoreCase = true) -> Color(0xFF8B5CF6)
                    seat.berthType.contains("Side", ignoreCase = true) -> Color(0xFFF59E0B)
                    else -> RailBluePrimary
                }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSearched) RailBluePrimary.copy(alpha = 0.25f)
                        else if (isDark) Color(0xFF1E293B)
                        else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(
                        width = if (isSearched) 2.dp else 1.dp,
                        color = if (isSearched) RailBluePrimary else berthColor.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${seat.seatNumber}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = seat.berthType.substringBefore(" "),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = berthColor,
                            maxLines = 1
                        )
                        if (seat.isWindow) {
                            Text(
                                text = "🪟 Window",
                                fontSize = 8.sp,
                                color = RailBluePrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun AmenityPill(text: String, isDark: Boolean) {
    Box(
        modifier = Modifier
            .background(
                if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) DarkTextSecondary else LightTextSecondary
        )
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
    }
}

/**
 * Minimalist Vector Coach Icon for the Analytics Tab Switcher
 */
@Composable
fun VectorCoachIcon(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp
) {
    val activeColor = if (isActive) Color.White else Color(0xFF64748B)

    Canvas(modifier = modifier.size(iconSize)) {
        // Coach Body
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(size.width * 0.12f, size.height * 0.20f),
            size = Size(size.width * 0.76f, size.height * 0.52f),
            cornerRadius = CornerRadius(3f, 3f),
            style = Stroke(width = 1.6f)
        )

        // Panoramic Windows
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(size.width * 0.22f, size.height * 0.32f),
            size = Size(size.width * 0.22f, size.height * 0.24f),
            cornerRadius = CornerRadius(2f, 2f),
            style = Stroke(width = 1.2f)
        )
        drawRoundRect(
            color = activeColor,
            topLeft = Offset(size.width * 0.54f, size.height * 0.32f),
            size = Size(size.width * 0.22f, size.height * 0.24f),
            cornerRadius = CornerRadius(2f, 2f),
            style = Stroke(width = 1.2f)
        )

        // Coupler hooks at both ends
        drawLine(
            color = activeColor,
            start = Offset(0f, size.height * 0.46f),
            end = Offset(size.width * 0.12f, size.height * 0.46f),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = activeColor,
            start = Offset(size.width * 0.88f, size.height * 0.46f),
            end = Offset(size.width, size.height * 0.46f),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round
        )

        // Wheels
        drawCircle(color = activeColor, radius = 1.8f, center = Offset(size.width * 0.28f, size.height * 0.82f))
        drawCircle(color = activeColor, radius = 1.8f, center = Offset(size.width * 0.72f, size.height * 0.82f))
        drawLine(
            color = activeColor,
            start = Offset(size.width * 0.28f, size.height * 0.82f),
            end = Offset(size.width * 0.72f, size.height * 0.82f),
            strokeWidth = 1.2f
        )
    }
}
