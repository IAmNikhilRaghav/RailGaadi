package com.railgaadi.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.railgaadi.app.model.*
import com.railgaadi.app.theme.*
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────────────────────
// StationLayoutVisualizer — Full Helicopter Aerial Yard & Coach Visualizer
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
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
    val textMeasurer = rememberTextMeasurer()

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
    var selectedCategory by remember {
        mutableStateOf(
            TrainRakeDataService.detectCategory(trainName ?: "", trainType ?: "")
        )
    }

    val rake = remember(selectedCategory) {
        TrainRakeDataService.getRakeForCategory(selectedCategory, targetCoachCode)
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
    var trainOffsetPx by remember { mutableStateOf(-320f) }
    var canvasWidthPx by remember { mutableStateOf(900f) }
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

    // Function to center train on a specific coach at Central FOB (X = canvasWidthPx * 0.42f)
    fun centerOnCoach(code: String) {
        val targetIdx = rake.indexOfFirst { it.code == code }.takeIf { it >= 0 } ?: 0
        var distPx = 40f
        for (i in 0 until targetIdx) {
            distPx += (rake[i].widthDp * 2.2f) + 12f
        }
        val targetCoachW = (rake.getOrNull(targetIdx)?.widthDp ?: 84f) * 2.2f
        val bridgeX = canvasWidthPx * 0.42f
        trainOffsetPx = bridgeX - (distPx + targetCoachW / 2f)
    }

    // Auto-center on user coach once layout is measured
    LaunchedEffect(canvasWidthPx, targetCoachCode, rake) {
        if (!hasAutoCentered && canvasWidthPx > 200f) {
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
            .heightIn(min = 540.dp, max = 740.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

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
                                if (!trainNumber.isNullOrBlank()) append("$trainNumber • ")
                                append(trainName ?: selectedCategory.displayName)
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

            // ── 4. UNIFIED HELICOPTER CANVAS (Yard + Draggable Train Rake) ───
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFF090E1A))
                    .pointerInput(rake, trainOffsetPx) {
                        // Drag gesture to scroll train horizontally
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            trainOffsetPx += dragAmount.x
                        }
                    }
                    .pointerInput(rake, trainOffsetPx) {
                        // Tap gesture to select and inspect tapped coach
                        detectTapGestures { tapOffset ->
                            val trackY = 280f
                            val coachH = 46f
                            if (tapOffset.y in (trackY - 20f)..(trackY + coachH + 20f)) {
                                var currX = trainOffsetPx + 40f
                                for (coach in rake) {
                                    val cW = coach.widthDp * 2.2f
                                    if (tapOffset.x in currX..(currX + cW)) {
                                        inspectedCoachCode = coach.code
                                        break
                                    }
                                    currX += cW + 12f
                                }
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    canvasWidthPx = size.width

                    drawActualUnifiedYardCanvas(
                        textMeasurer = textMeasurer,
                        layout = layout,
                        targetPlatform = targetPlatform,
                        rake = rake,
                        userCoachCode = targetCoachCode ?: "B4",
                        inspectedCoachCode = inspectedCoachCode,
                        trainOffsetPx = trainOffsetPx,
                        pulse = pulse,
                        selectedGate = selectedGateNumber,
                        route = route
                    )
                }

                // Floating Drag Hint HUD
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
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

            // ── 5. WAYFINDING & COACH INSPECTION SHEET (Actual Visual Card) ───
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Coach ${inspectedCoach?.code ?: "B4"} • ${TrainRakeDataService.classDisplayName(inspectedCoach?.classType ?: "3A")}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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

// ─────────────────────────────────────────────────────────────────────────────
// Actual Unified Yard Canvas Renderer (Matches the Approved HTML Visual)
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawActualUnifiedYardCanvas(
    textMeasurer: TextMeasurer,
    layout: JunctionLayout,
    targetPlatform: Int,
    rake: List<RakeCoach>,
    userCoachCode: String,
    inspectedCoachCode: String,
    trainOffsetPx: Float,
    pulse: Float,
    selectedGate: Int,
    route: NavigationRoute
) {
    val W = size.width
    val H = size.height

    // ── Fixed Station Bridge Coordinates ─────────────────────────────────────
    val centralFobX = W * 0.42f
    val centralFobW = 36f
    val northBridgeX = W * 0.82f
    val northBridgeW = 30f

    // ── 1. BACKGROUND & FIXED YARD INFRASTRUCTURE ─────────────────────────────
    drawRect(color = Color(0xFF090E1A), size = size)

    // ── Gate 1: Main Concourse (City Side - Top) ─────────────────────────────
    val gate1H = 50f
    drawRect(color = Color(0xFF1E293B), topLeft = Offset(16f, 12f), size = Size(W - 32f, gate1H))
    drawText(
        textMeasurer = textMeasurer,
        text = "🏛️ MAIN CONCOURSE • GATE 1 (CITY SIDE) • PARKING & TICKETS",
        topLeft = Offset(26f, 18f),
        style = TextStyle(color = Color(0xFFF8FAFC), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
    )
    drawText(
        textMeasurer = textMeasurer,
        text = "📍 MAIN ENTRY POINT",
        topLeft = Offset(26f, 38f),
        style = TextStyle(color = Color(0xFF38BDF8), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
    )

    // ── Platform 1 (City Face) ───────────────────────────────────────────────
    val pf1Y = 74f
    val pf1H = 38f
    drawRect(color = Color(0xFF334155), topLeft = Offset(16f, pf1Y), size = Size(W - 32f, pf1H))
    // Yellow tactile safety line at bottom edge
    drawRect(color = Color(0xFFFACC15), topLeft = Offset(16f, pf1Y + pf1H - 3f), size = Size(W - 32f, 3f))
    drawText(
        textMeasurer = textMeasurer,
        text = "PLATFORM 1 (CITY FACE)",
        topLeft = Offset(26f, pf1Y + 12f),
        style = TextStyle(color = Color(0xFFE2E8F0), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    )

    // ── Track 1 & 2 Ballast Bed ──────────────────────────────────────────────
    drawBallastTrackBed(y = pf1Y + pf1H, h = 58f, canvasW = W)

    // ── Platform 2 / 3 (Target Island Platform) ──────────────────────────────
    val pf3Y = pf1Y + pf1H + 58f
    val pf3H = 54f
    // Drop shadow
    drawRect(color = Color.Black.copy(alpha = 0.5f), topLeft = Offset(16f, pf3Y + 4f), size = Size(W - 32f, pf3H))
    // Vibrant Blue Target Platform slab
    drawRect(color = Color(0xFF0C4A6E), topLeft = Offset(16f, pf3Y), size = Size(W - 32f, pf3H))
    // Yellow tactile lines on both edges of island platform
    drawRect(color = Color(0xFFFACC15), topLeft = Offset(16f, pf3Y), size = Size(W - 32f, 3f))
    drawRect(color = Color(0xFFFACC15), topLeft = Offset(16f, pf3Y + pf3H - 3f), size = Size(W - 32f, 3f))

    // Platform Shelter & Departure Track Sign
    drawRoundRect(
        color = Color(0xFF0284C7),
        topLeft = Offset(36f, pf3Y + 10f),
        size = Size(W - 72f, 34f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawText(
        textMeasurer = textMeasurer,
        text = "🚉 PLATFORM 3 (DEPARTURE TRACK)",
        topLeft = Offset(48f, pf3Y + 18f),
        style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
    )

    // ── Track 3 Ballast Bed (Where our train is parked!) ─────────────────────
    val track3Y = pf3Y + pf3H
    val track3H = 76f
    drawBallastTrackBed(y = track3Y, h = track3H, canvasW = W)

    // ── Platform 4 / 5 (Island) ──────────────────────────────────────────────
    val pf4Y = track3Y + track3H
    val pf4H = 44f
    drawRect(color = Color(0xFF334155), topLeft = Offset(16f, pf4Y), size = Size(W - 32f, pf4H))
    drawRect(color = Color(0xFFFACC15), topLeft = Offset(16f, pf4Y), size = Size(W - 32f, 3f))
    drawRect(color = Color(0xFFFACC15), topLeft = Offset(16f, pf4Y + pf4H - 3f), size = Size(W - 32f, 3f))
    drawText(
        textMeasurer = textMeasurer,
        text = "PLATFORM 4 / 5",
        topLeft = Offset(26f, pf4Y + 14f),
        style = TextStyle(color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    )

    // ── Track 4 Ballast Bed ──────────────────────────────────────────────────
    val track4Y = pf4Y + pf4H
    drawBallastTrackBed(y = track4Y, h = 52f, canvasW = W)

    // ── Gate 2: Secondary Entry (Cantt / Colony - Bottom) ────────────────────
    val gate2Y = H - 46f
    drawRect(color = Color(0xFF1E293B), topLeft = Offset(16f, gate2Y), size = Size(W - 32f, 42f))
    drawText(
        textMeasurer = textMeasurer,
        text = "🚗 SECONDARY ENTRY • GATE 2 (CANTT / COLONY SIDE)",
        topLeft = Offset(26f, gate2Y + 6f),
        style = TextStyle(color = Color(0xFFF8FAFC), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    )
    drawText(
        textMeasurer = textMeasurer,
        text = "📍 REAR STATION ENTRY",
        topLeft = Offset(26f, gate2Y + 24f),
        style = TextStyle(color = Color(0xFF38BDF8), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    )

    // ── 2. DYNAMIC HORIZONTAL TRAIN RAKE (DRAGGABLE ALONG PLATFORM 3) ────────
    val trainCenterY = track3Y + (track3H / 2f)
    val coachH = 38f
    val coachY = trainCenterY - (coachH / 2f)
    var curX = trainOffsetPx + 40f
    var userCoachScreenX = -1f
    var userCoachScreenY = -1f

    rake.forEachIndexed { idx, coach ->
        val coachW = coach.widthDp * 2.2f
        val isUser = coach.code.equals(userCoachCode, ignoreCase = true)
        val isInspected = coach.code.equals(inspectedCoachCode, ignoreCase = true)

        // Coupler between coaches
        if (idx > 0) {
            drawRect(
                color = Color(0xFF334155),
                topLeft = Offset(curX - 8f, trainCenterY - 3f),
                size = Size(8f, 6f)
            )
        }

        // ── SMART PNR HIGHLIGHT: LIVE PULSING GREEN GLOW SHADOW ──
        if (isUser) {
            userCoachScreenX = curX + coachW / 2f
            userCoachScreenY = coachY
            val auraPulse = pulse * 8f

            // Multi-layer glowing green shadow aura
            drawRoundRect(
                color = Color(0xFF10B981).copy(alpha = 0.25f + 0.2f * pulse),
                topLeft = Offset(curX - 8f - auraPulse / 2f, coachY - 8f - auraPulse / 2f),
                size = Size(coachW + 16f + auraPulse, coachH + 16f + auraPulse),
                cornerRadius = CornerRadius(12f, 12f)
            )
            drawRoundRect(
                color = Color(0xFF10B981).copy(alpha = 0.45f),
                topLeft = Offset(curX - 3f, coachY - 3f),
                size = Size(coachW + 6f, coachH + 6f),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }

        // Selection ring for inspected coach (if different from user)
        if (isInspected && !isUser) {
            drawRoundRect(
                color = Color(0xFF38BDF8).copy(alpha = 0.4f),
                topLeft = Offset(curX - 3f, coachY - 3f),
                size = Size(coachW + 6f, coachH + 6f),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 2f)
            )
        }

        // Coach Drop Shadow
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.6f),
            topLeft = Offset(curX + 3f, coachY + 4f),
            size = Size(coachW, coachH),
            cornerRadius = CornerRadius(6f, 6f)
        )

        // Coach Body Surface
        val bodyCol = Color(coach.bodyColor)
        drawRoundRect(
            color = bodyCol,
            topLeft = Offset(curX, coachY),
            size = Size(coachW, coachH),
            cornerRadius = CornerRadius(if (coach.isAeroNose) 12f else 6f, 6f)
        )

        // Livery Stripe (top & bottom)
        val stripeCol = coach.stripeColor?.let { Color(it) }
        if (stripeCol != null) {
            drawRect(
                color = stripeCol,
                topLeft = Offset(curX + 2f, coachY + 4f),
                size = Size(coachW - 4f, 4f)
            )
            drawRect(
                color = stripeCol,
                topLeft = Offset(curX + 2f, coachY + coachH - 8f),
                size = Size(coachW - 4f, 4f)
            )
        }

        // Roof Details (AC vent center corridor)
        val roofCol = Color(coach.roofColor)
        drawRect(
            color = roofCol,
            topLeft = Offset(curX + 8f, coachY + (coachH / 2f) - 4f),
            size = Size(coachW - 16f, 8f)
        )

        // Pantograph on Locomotives
        if (coach.classType == "LOCO" || coach.classType == "CAB") {
            val pantoX = curX + 16f
            val pantoY = coachY + (coachH / 2f) - 6f
            drawRect(
                color = Color(0xFFF59E0B),
                topLeft = Offset(pantoX, pantoY),
                size = Size(16f, 12f),
                style = Stroke(width = 1.5f)
            )
        }

        // Passenger Doors / Steps facing Platform 3
        drawRect(
            color = Color(0xFFFACC15),
            topLeft = Offset(curX + 4f, coachY),
            size = Size(8f, 3f)
        )
        drawRect(
            color = Color(0xFFFACC15),
            topLeft = Offset(curX + coachW - 12f, coachY),
            size = Size(8f, 3f)
        )

        // ── 1. COACH CODE TEXT (e.g. B4, A1, S3, GEN, WAP-7) ──
        val codeColor = if (isUser) Color.White else Color(0xFFF8FAFC)
        val codeText = coach.code
        val codeMeas = textMeasurer.measure(
            text = codeText,
            style = TextStyle(fontSize = 11.5.sp, fontWeight = FontWeight.Black)
        )
        drawText(
            textMeasurer = textMeasurer,
            text = codeText,
            topLeft = Offset(curX + (coachW - codeMeas.size.width) / 2f, coachY + 4f),
            style = TextStyle(color = codeColor, fontSize = 11.5.sp, fontWeight = FontWeight.Black)
        )

        // ── 2. COACH CLASS BADGE PILL (e.g. [3A], [SL], [GEN], [2A], [1A]) ──
        val classTag = TrainRakeDataService.classShortTag(coach.classType)
        val badgeBgCol = Color(TrainRakeDataService.classBadgeColor(coach.classType))
        val tagMeas = textMeasurer.measure(
            text = classTag,
            style = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Black)
        )
        val badgeW = tagMeas.size.width + 10f
        val badgeH = 14f
        val badgeX = curX + (coachW - badgeW) / 2f
        val badgeY = coachY + coachH - 18f

        drawRoundRect(
            color = badgeBgCol,
            topLeft = Offset(badgeX, badgeY),
            size = Size(badgeW, badgeH),
            cornerRadius = CornerRadius(4f, 4f)
        )
        drawText(
            textMeasurer = textMeasurer,
            text = classTag,
            topLeft = Offset(badgeX + 5f, badgeY + 1f),
            style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
        )

        curX += coachW + 12f
    }

    // ── 3. ELEVATED FOOT-OVER-BRIDGES (Rendered ON TOP of train!) ───────────
    val fobY1 = 36f
    val fobY2 = H - 24f

    // Central FOB (🛗 Lift + Escalator)
    // Deep Drop Shadow
    drawRect(
        color = Color.Black.copy(alpha = 0.65f),
        topLeft = Offset(centralFobX + 12f, fobY1 + 10f),
        size = Size(centralFobW, fobY2 - fobY1)
    )
    // Bridge Deck Beam
    drawRect(
        color = Color(0xFF0369A1),
        topLeft = Offset(centralFobX, fobY1),
        size = Size(centralFobW, fobY2 - fobY1)
    )
    // Handrails
    drawRect(
        color = Color(0xFF38BDF8),
        topLeft = Offset(centralFobX, fobY1),
        size = Size(centralFobW, fobY2 - fobY1),
        style = Stroke(width = 2f)
    )
    // Center Dashed Walkway Stripe
    drawLine(
        color = Color.White.copy(alpha = 0.45f),
        start = Offset(centralFobX + centralFobW / 2f, fobY1),
        end = Offset(centralFobX + centralFobW / 2f, fobY2),
        strokeWidth = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
    )
    // Glass Lift / Escalator Tower at Platform 3 Landing
    drawRoundRect(
        color = Color(0xFF0F172A),
        topLeft = Offset(centralFobX + centralFobW + 2f, pf3Y + 14f),
        size = Size(26f, 26f),
        cornerRadius = CornerRadius(4f, 4f)
    )
    drawRoundRect(
        color = Color(0xFF38BDF8),
        topLeft = Offset(centralFobX + centralFobW + 2f, pf3Y + 14f),
        size = Size(26f, 26f),
        cornerRadius = CornerRadius(4f, 4f),
        style = Stroke(width = 1.5f)
    )
    drawText(
        textMeasurer = textMeasurer,
        text = "🛗",
        topLeft = Offset(centralFobX + centralFobW + 7f, pf3Y + 18f),
        style = TextStyle(fontSize = 12.sp)
    )

    // North Overbridge (Stairs)
    // Drop Shadow
    drawRect(
        color = Color.Black.copy(alpha = 0.65f),
        topLeft = Offset(northBridgeX + 10f, fobY1 + 10f),
        size = Size(northBridgeW, fobY2 - fobY1)
    )
    // Bridge Deck
    drawRect(
        color = Color(0xFF475569),
        topLeft = Offset(northBridgeX, fobY1),
        size = Size(northBridgeW, fobY2 - fobY1)
    )
    drawRect(
        color = Color(0xFF94A3B8),
        topLeft = Offset(northBridgeX, fobY1),
        size = Size(northBridgeW, fobY2 - fobY1),
        style = Stroke(width = 1.5f)
    )
    drawLine(
        color = Color.White.copy(alpha = 0.35f),
        start = Offset(northBridgeX + northBridgeW / 2f, fobY1),
        end = Offset(northBridgeX + northBridgeW / 2f, fobY2),
        strokeWidth = 1.5f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
    )

    // ── 4. PASSENGER DOTTED WALKING ROUTE (Cyan Dashed Line) ─────────────────
    val entryStartY = if (selectedGate == 1) 36f else H - 40f
    val liftLandY = pf3Y + pf3H - 8f

    // 1. From Gate entrance along the bridge
    drawLine(
        color = Color(0xFF38BDF8),
        start = Offset(centralFobX + centralFobW / 2f, entryStartY),
        end = Offset(centralFobX + centralFobW / 2f, liftLandY),
        strokeWidth = 3f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
    )
    // 2. Along Platform 3 right to the door of user's coach
    if (userCoachScreenX > 0f) {
        drawLine(
            color = Color(0xFF38BDF8),
            start = Offset(centralFobX + centralFobW / 2f, liftLandY),
            end = Offset(userCoachScreenX, liftLandY),
            strokeWidth = 3f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
        )
    }

    // ── 5. FLOATING 3D BEACON PIN: "🎯 YOUR COACH" (Rendered on top!) ────────
    if (userCoachScreenX > 0f) {
        val pinX = userCoachScreenX
        val pinY = userCoachScreenY - 32f

        // Green Beacon Pill Background
        val beaconW = 96f
        val beaconH = 22f
        drawRoundRect(
            color = Color(0xFF10B981),
            topLeft = Offset(pinX - beaconW / 2f, pinY - beaconH / 2f),
            size = Size(beaconW, beaconH),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // Downward Triangle Pointer
        val trianglePath = Path().apply {
            moveTo(pinX - 6f, pinY + beaconH / 2f - 1f)
            lineTo(pinX + 6f, pinY + beaconH / 2f - 1f)
            lineTo(pinX, pinY + beaconH / 2f + 8f)
            close()
        }
        drawPath(trianglePath, color = Color(0xFF10B981))

        // Beacon Text
        drawText(
            textMeasurer = textMeasurer,
            text = "🎯 YOUR COACH",
            topLeft = Offset(pinX - 42f, pinY - 7f),
            style = TextStyle(color = Color(0xFF022C22), fontSize = 9.5.sp, fontWeight = FontWeight.Black)
        )
    }
}

// ── Helper to draw realistic track ballast beds with sleepers and rails ───────
private fun DrawScope.drawBallastTrackBed(y: Float, h: Float, canvasW: Float) {
    // Crushed gravel ballast
    drawRect(color = Color(0xFF111827), topLeft = Offset(0f, y), size = Size(canvasW, h))

    // Wooden / Concrete Sleepers
    var x = 0f
    while (x < canvasW) {
        drawLine(
            color = Color(0xFF1F2937),
            start = Offset(x, y + 4f),
            end = Offset(x, y + h - 4f),
            strokeWidth = 3f
        )
        x += 16f
    }

    // Steel Rails (Pair)
    val r1 = y + h * 0.30f
    val r2 = y + h * 0.70f
    drawLine(Color(0xFF64748B), Offset(0f, r1), Offset(canvasW, r1), strokeWidth = 2.5f)
    drawLine(Color(0xFF64748B), Offset(0f, r2), Offset(canvasW, r2), strokeWidth = 2.5f)
}
