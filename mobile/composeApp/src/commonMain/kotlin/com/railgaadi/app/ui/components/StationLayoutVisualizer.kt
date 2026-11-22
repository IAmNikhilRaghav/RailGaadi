package com.railgaadi.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.railgaadi.app.model.*
import com.railgaadi.app.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// StationLayoutVisualizer — Full Helicopter Aerial Yard & Coach Visualizer
// 100% Native Compose UI — Zero Canvas drawText / Zero Skia HarfBuzz crashes
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StationLayoutVisualizer(
    stationCode: String,
    stationName: String,
    targetPlatform: Int,
    targetCoachCode: String? = null,
    trainNumber: String? = null,
    trainName: String? = null,
    trainType: String? = null,
    isDark: Boolean = false,
    onClose: () -> Unit
) {
    // ── Station yard layout ──────────────────────────────────────────────────
    val layout = remember(stationCode, stationName, targetPlatform) {
        JunctionDataService.getLayoutForStation(
            stationCode = stationCode,
            stationName = stationName,
            platformCountHint = targetPlatform.coerceAtLeast(3)
        )
    }

    // ── Entry Gate state ─────────────────────────────────────────────────────
    var selectedGateNumber by remember { mutableStateOf(1) }
    val selectedEntry = remember(layout, selectedGateNumber) {
        if (selectedGateNumber == 1) layout.entries.firstOrNull()
        else layout.entries.getOrNull(1) ?: layout.entries.firstOrNull()
    }

    // ── Train category state ─────────────────────────────────────────────────
    val initialCategory = remember(trainName, trainType) {
        TrainRakeDataService.detectCategory(trainName ?: "", trainType ?: "")
    }

    var selectedCategory by remember(initialCategory) {
        mutableStateOf(initialCategory)
    }

    val isDefaultCategory = selectedCategory == initialCategory

    val rake = remember(selectedCategory, trainNumber, trainName, trainType, isDefaultCategory) {
        TrainRakeDataService.getRakeForCategory(
            category = selectedCategory,
            userCoachCode = targetCoachCode,
            trainNumber = if (isDefaultCategory) trainNumber else "",
            trainName = if (isDefaultCategory) trainName else selectedCategory.displayName,
            trainType = if (isDefaultCategory) trainType else selectedCategory.displayName
        )
    }

    // Currently inspected coach (defaults to target coach, or first coach)
    var inspectedCoachCode by remember(rake, targetCoachCode) {
        val matching = rake.find { it.code.equals(targetCoachCode, ignoreCase = true) }
        mutableStateOf(matching?.code ?: rake.getOrNull(3)?.code ?: rake.firstOrNull()?.code ?: "B4")
    }

    val inspectedCoach = remember(rake, inspectedCoachCode) {
        rake.find { it.code == inspectedCoachCode } ?: rake.firstOrNull()
    }
    val inspectedCoachIdx = remember(rake, inspectedCoach) {
        rake.indexOf(inspectedCoach).coerceAtLeast(0)
    }

    // ── Train horizontal drag offset ─────────────────────────────────────────
    var trainOffsetPx by remember { mutableStateOf(-280f) }
    var hasAutoCentered by remember { mutableStateOf(false) }

    // ── Smart PNR Pulse Animation ────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    // ── Navigation Route ─────────────────────────────────────────────────────
    val route = remember(layout, selectedEntry?.id, targetPlatform, targetCoachCode) {
        JunctionDataService.calculateNavigationRoute(
            layout = layout,
            fromEntryId = selectedEntry?.id,
            targetPlatform = targetPlatform,
            targetCoachCode = targetCoachCode,
            preferAccessible = true
        )
    }

    // Center train on a specific coach
    fun centerOnCoach(code: String) {
        val targetIdx = rake.indexOfFirst { it.code.equals(code, ignoreCase = true) }.takeIf { it >= 0 } ?: 0
        var distDp = 40f
        for (i in 0 until targetIdx) {
            distDp += rake[i].widthDp + 6f
        }
        val targetCoachW = rake.getOrNull(targetIdx)?.widthDp ?: 80f
        val targetCenterDp = distDp + (targetCoachW / 2f)
        val desiredOffset = (150f - targetCenterDp) * 2.5f
        val maxPanLeft = -((rake.size * 90f) - 260f).coerceAtLeast(0f)
        trainOffsetPx = desiredOffset.coerceIn(maxPanLeft, 40f)
    }

    // Auto-center on user coach once layout is displayed
    LaunchedEffect(targetCoachCode, rake) {
        if (!hasAutoCentered) {
            val userCode = targetCoachCode ?: "B4"
            centerOnCoach(userCode)
            hasAutoCentered = true
        }
    }

    // ── Colors ───────────────────────────────────────────────────────────────
    val bgCol       = Color(0xFF080C16)
    val headerBg    = Color(0xFF0F172A)
    val cardBg      = Color(0xFF0F172A)
    val textPrimary = Color(0xFFF8FAFC)
    val textMuted   = Color(0xFF94A3B8)
    val railBlue    = Color(0xFF0284C7)
    val railBlueGlow= Color(0xFF38BDF8)
    val railGreen   = Color(0xFF10B981)
    val railGreenGlow = Color(0xFF34D399)
    val dividerCol  = Color(0xFF1E293B)

    Surface(
        color = bgCol,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {

            // ── 1. CLEAN HEADER (Station Title + Subtitle) ───────────────────
            Surface(
                color = headerBg,
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${layout.stationName.uppercase()} (${layout.stationCode}) • PF $targetPlatform",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = buildString {
                                if (isDefaultCategory && !trainNumber.isNullOrBlank()) append("$trainNumber • ")
                                append(if (isDefaultCategory) (trainName ?: selectedCategory.displayName) else selectedCategory.displayName)
                                append(" • ${rake.size} Coaches • WAP-7 Loco")
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Close (✕) Button
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }
                }
            }

            // ── 2. TRAIN CATEGORY SCROLLER BAR ──────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0B1120))
            ) {
                items(TrainCategory.values().toList()) { cat ->
                    val isSel = cat == selectedCategory
                    Surface(
                        color = if (isSel) railBlue else Color(0xFF1E293B),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isSel) railBlueGlow else Color(0xFF334155)),
                        modifier = Modifier.clickable {
                            selectedCategory = cat
                            hasAutoCentered = false
                            centerOnCoach(inspectedCoachCode)
                        }
                    ) {
                        Text(
                            text = "${cat.emoji} ${cat.displayName}",
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) Color.White else textMuted,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            HorizontalDivider(color = dividerCol, thickness = 1.dp)

            // ── 3. LIVE CONTROLS STRIP (Entry Gate Toggle + Snap to Coach) ───
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Gate 1 / Gate 2 Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Entry:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textMuted)
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(1.dp)
                    ) {
                        Row {
                            Surface(
                                color = if (selectedGateNumber == 1) railBlue else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable { selectedGateNumber = 1 }
                            ) {
                                Text(
                                    "Gate 1 (City)",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedGateNumber == 1) Color.White else textMuted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = if (selectedGateNumber == 2) railBlue else Color.Transparent,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable { selectedGateNumber = 2 }
                            ) {
                                Text(
                                    "Gate 2 (Cantt)",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedGateNumber == 2) Color.White else textMuted,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Snap to Coach Button
                val targetCode = targetCoachCode ?: "B4"
                Surface(
                    color = railGreen.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, railGreen),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.clickable {
                        inspectedCoachCode = targetCode
                        centerOnCoach(targetCode)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🎯", fontSize = 11.sp)
                        Text(
                            text = "Snap to $targetCode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = railGreenGlow
                        )
                    }
                }
            }

            HorizontalDivider(color = dividerCol, thickness = 1.dp)

            // ── 4. UNIFIED HELICOPTER YARD VIEWPORT ───────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(Color(0xFF090E1A))
            ) {
                // Yard Elements Column
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 1. Gate 1 Concourse (City Side)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Text(
                                text = "🏛️ MAIN CONCOURSE • GATE 1 (CITY SIDE) • PARKING & TICKETS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "📍 MAIN ENTRY POINT",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }

                    // 2. Platform 1 (City Face)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .background(Color(0xFF334155), RoundedCornerShape(4.dp))
                            .border(width = 2.dp, color = Color(0xFFFACC15), shape = RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PLATFORM 1 (CITY FACE)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                    }

                    // 3. Track 1 & 2 Ballast Bed
                    TrackBallastView(heightDp = 24.dp)

                    // 4. Platform 3 (Target Island Platform)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .background(Color(0xFF0C4A6E), RoundedCornerShape(6.dp))
                            .border(width = 1.5.dp, color = Color(0xFF0284C7), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "🚉 PLATFORM 3 (DEPARTURE TRACK)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    // 5. Track 3 Ballast Bed + HORIZONTALLY DRAGGABLE TRAIN RAKE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                    ) {
                        // Background rails and gravel
                        TrackBallastView(heightDp = 88.dp)

                        // Train Rake Row
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                                .pointerInput(rake) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val maxPanLeft = -((rake.size * 88f) - 240f).coerceAtLeast(0f)
                                        trainOffsetPx = (trainOffsetPx + dragAmount.x).coerceIn(maxPanLeft, 40f)
                                    }
                                }
                                .graphicsLayer { translationX = trainOffsetPx },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.width(30.dp))

                            rake.forEachIndexed { idx, coach ->
                                val isUser = coach.code.equals(targetCoachCode ?: "B4", ignoreCase = true)
                                val isInspected = coach.code.equals(inspectedCoachCode, ignoreCase = true)

                                if (idx > 0) {
                                    // Coupler between coaches
                                    Box(
                                        modifier = Modifier
                                            .width(6.dp)
                                            .height(5.dp)
                                            .background(Color(0xFF475569), RoundedCornerShape(1.dp))
                                    )
                                }

                                CoachVisualCard(
                                    coach = coach,
                                    isUser = isUser,
                                    isInspected = isInspected,
                                    pulse = pulse,
                                    onClick = { inspectedCoachCode = coach.code }
                                )
                            }

                            Spacer(modifier = Modifier.width(50.dp))
                        }
                    }

                    // 6. Platform 4 / 5 (Island)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .background(Color(0xFF334155), RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "PLATFORM 4 / 5",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    // 7. Track 4 Ballast Bed
                    TrackBallastView(heightDp = 20.dp)

                    // 8. Gate 2 Concourse (Cantt / Colony Side)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 3.dp)
                            .background(Color(0xFF1E293B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Column {
                            Text(
                                text = "🚗 SECONDARY ENTRY • GATE 2 (CANTT / COLONY SIDE)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "📍 REAR STATION ENTRY",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    }
                }

                // ── OVERLAID ELEVATED FOOT-OVER-BRIDGES ─────────────────────────
                // Central FOB (Lift + Escalator) at 42% horizontal position
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(32.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = 150.dp)
                        .background(Color(0xFF0369A1).copy(alpha = 0.88f))
                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.90f))
                ) {
                    // Glass Lift Icon at Platform 3 Landing
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.Center)
                            .background(Color(0xFF0F172A), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🛗", fontSize = 11.sp)
                    }
                }

                // North Overbridge (Stairs) at 80% horizontal position
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(26.dp)
                        .align(Alignment.CenterStart)
                        .offset(x = 300.dp)
                        .background(Color(0xFF475569).copy(alpha = 0.82f))
                        .border(1.dp, Color(0xFF94A3B8).copy(alpha = 0.70f))
                )

                // Floating Drag Instruction Hint HUD
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .background(Color(0xFF0F172A).copy(alpha = 0.88f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        "↔ Swipe / Drag horizontally to inspect train along platform",
                        fontSize = 10.sp,
                        color = textMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── 5. WAYFINDING & COACH INSPECTION SHEET ────────────────────────
            Surface(
                color = cardBg,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, dividerCol),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Coach row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isUser = inspectedCoach?.code.equals(targetCoachCode, ignoreCase = true)

                        // Large Coach Badge Box
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isUser) railGreen else Color(inspectedCoach?.bodyColor ?: 0xFF0284C7))
                                .border(1.dp, if (isUser) railGreenGlow else Color.White.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = inspectedCoach?.code ?: "B4",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isUser) Color(0xFF022C22) else Color.White
                            )
                        }

                        // Coach details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Coach ${inspectedCoach?.code ?: "B4"} • ${TrainRakeDataService.classDisplayName(inspectedCoach?.classType ?: "3A")}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Position: Coach ${inspectedCoachIdx + 1} of ${rake.size} in rake • Direct Platform 3 Access",
                                fontSize = 11.sp,
                                color = textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isUser) {
                                Text(
                                    text = "🎯 YOUR RESERVED COACH (SMART PNR SYNCED)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = railGreenGlow
                                )
                            }
                        }

                        // Walk info
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "~${route.estimatedMinutes} MIN",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = railBlueGlow
                            )
                            Text(
                                text = "CLOSEST BRIDGE",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = railGreenGlow
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * High-performance pure vector ballast track renderer (zero text, zero font shaper calls)
 */
@Composable
private fun TrackBallastView(heightDp: Dp) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp)
    ) {
        val w = size.width
        val h = size.height

        // Crushed dark gravel bed
        drawRect(color = Color(0xFF111827), size = size)

        // Sleepers (Cross ties)
        var x = 0f
        while (x < w) {
            drawLine(
                color = Color(0xFF1F2937),
                start = Offset(x, 2f),
                end = Offset(x, h - 2f),
                strokeWidth = 3f
            )
            x += 16f
        }

        // Steel Rails (Pair)
        val r1 = h * 0.32f
        val r2 = h * 0.68f
        drawLine(Color(0xFF64748B), Offset(0f, r1), Offset(w, r1), strokeWidth = 2.5f)
        drawLine(Color(0xFF64748B), Offset(0f, r2), Offset(w, r2), strokeWidth = 2.5f)
    }
}

/**
 * Individual Train Coach Card rendered with pure Compose components
 */
@Composable
private fun CoachVisualCard(
    coach: RakeCoach,
    isUser: Boolean,
    isInspected: Boolean,
    pulse: Float,
    onClick: () -> Unit
) {
    val coachW = (coach.widthDp * 0.95f).dp
    val coachH = 46.dp
    val bodyCol = Color(coach.bodyColor)
    val classTag = TrainRakeDataService.classShortTag(coach.classType)
    val badgeBgCol = Color(TrainRakeDataService.classBadgeColor(coach.classType))

    val r = ((coach.bodyColor shr 16) and 0xFF) / 255f
    val g = ((coach.bodyColor shr 8) and 0xFF) / 255f
    val b = (coach.bodyColor and 0xFF) / 255f
    val isLightBody = (0.299f * r + 0.587f * g + 0.114f * b) > 0.65f
    val coachCodeColor = if (isUser) Color.White else if (isLightBody) Color(0xFF0F172A) else Color(0xFFF8FAFC)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        // Floating Beacon Pin above user's coach
        if (isUser) {
            Box(
                modifier = Modifier
                    .background(Color(0xFF10B981), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "🎯 YOUR COACH",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF022C22),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
        } else {
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Coach Body
        Box(
            modifier = Modifier
                .width(coachW)
                .height(coachH)
                .clip(RoundedCornerShape(if (coach.isAeroNose) 10.dp else 5.dp))
                .background(bodyCol)
                .then(
                    if (isUser) {
                        Modifier.border(
                            width = (2f + 1.5f * pulse).dp,
                            color = Color(0xFF10B981).copy(alpha = 0.7f + 0.3f * pulse),
                            shape = RoundedCornerShape(5.dp)
                        )
                    } else if (isInspected) {
                        Modifier.border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(5.dp))
                    } else {
                        Modifier.border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(5.dp))
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Coach Code (e.g. B4, A1, GEN)
                Text(
                    text = coach.code,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Black,
                    color = coachCodeColor,
                    maxLines = 1
                )

                // Class Tag Badge (e.g. [3A], [2A], [SL])
                Box(
                    modifier = Modifier
                        .background(badgeBgCol, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = classTag,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
