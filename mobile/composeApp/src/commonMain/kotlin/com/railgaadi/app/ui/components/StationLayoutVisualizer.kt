package com.railgaadi.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.railgaadi.app.model.*
import com.railgaadi.app.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// StationLayoutVisualizer — Unified helicopter yard navigator + draggable rake
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
    // ── Layout data ──────────────────────────────────────────────────────────
    val layout = remember(stationCode, stationName, targetPlatform) {
        JunctionDataService.getLayoutForStation(
            stationCode = stationCode,
            stationName = stationName,
            platformCountHint = targetPlatform.coerceAtLeast(3)
        )
    }

    // ── State ────────────────────────────────────────────────────────────────
    var selectedEntryId by remember(layout) {
        mutableStateOf(layout.entries.firstOrNull()?.id)
    }

    var selectedCategory by remember {
        mutableStateOf(
            TrainRakeDataService.detectCategory(trainName ?: "", trainType ?: "")
        )
    }

    val rake by remember(selectedCategory) {
        derivedStateOf { TrainRakeDataService.getRakeForCategory(selectedCategory, targetCoachCode) }
    }

    // Train horizontal drag offset (in pixels, clamped inside canvas)
    var trainOffsetPx by remember { mutableStateOf(0f) }
    var canvasWidthPx by remember { mutableStateOf(1000f) }

    // Auto-snap to user coach on first open if PNR coach is provided
    var hasAutoSnapped by remember { mutableStateOf(false) }

    // ── Pulse animation for user coach green glow ────────────────────────────
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.55f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )

    // ── Route ────────────────────────────────────────────────────────────────
    val route = remember(layout, selectedEntryId, targetPlatform, targetCoachCode) {
        JunctionDataService.calculateNavigationRoute(
            layout = layout,
            fromEntryId = selectedEntryId,
            targetPlatform = targetPlatform,
            targetCoachCode = targetCoachCode,
            preferAccessible = true
        )
    }

    // ── Colors ───────────────────────────────────────────────────────────────
    val bgCol       = if (isDark) Color(0xFF0F172A)   else Color(0xFFF1F5F9)
    val cardBg      = if (isDark) Color(0xFF1E293B)   else Color(0xFFFFFFFF)
    val headerBg    = if (isDark) Color(0xFF1E293B)   else Color(0xFFFFFFFF)
    val textPrimary = if (isDark) Color(0xFFF8FAFC)   else Color(0xFF0F172A)
    val textMuted   = if (isDark) Color(0xFF94A3B8)   else Color(0xFF64748B)
    val divider     = if (isDark) Color(0xFF334155)   else Color(0xFFE2E8F0)
    val chipSel     = Color(0xFF0EA5E9)
    val chipUnsel   = if (isDark) Color(0xFF334155)   else Color(0xFFE2E8F0)
    val greenAura   = Color(0xFF22C55E)

    // ── User coach info ───────────────────────────────────────────────────────
    val userCoach   = rake.firstOrNull { it.code == targetCoachCode }
    val userCoachIdx = rake.indexOfFirst { it.code == targetCoachCode }

    // ── Snap to coach function ───────────────────────────────────────────────
    fun snapToUserCoach() {
        if (userCoachIdx < 0) return
        // Calculate x center of the user coach in the rake
        val coachStartX = rake.take(userCoachIdx).sumOf { (it.widthDp + 4f).toDouble() }.toFloat()
        val coachCenterX = coachStartX + rake[userCoachIdx].widthDp / 2f
        // We want coachCenterX + trainOffsetPx == canvasWidthPx / 2
        // So trainOffsetPx = canvasWidthPx/2 - coachCenterX (in dp units; will be converted in canvas)
        // We use density-independent calculation at canvas level; here we store in dp
        trainOffsetPx = (canvasWidthPx / 2f) - coachStartX * 2.75f // scaled for canvas
    }

    // Auto-snap on first open
    LaunchedEffect(rake, canvasWidthPx) {
        if (!hasAutoSnapped && userCoachIdx >= 0 && canvasWidthPx > 100f) {
            snapToUserCoach()
            hasAutoSnapped = true
        }
    }

    // ── Root surface ─────────────────────────────────────────────────────────
    Surface(
        color = bgCol,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 500.dp, max = 720.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── HEADER ─────────────────────────────────────────────────────────
            Surface(
                color = headerBg,
                shadowElevation = 2.dp,
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
                        // Station name + code chip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = layout.stationName.uppercase(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                color = if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = layout.stationCode,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0284C7),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            // Platform badge
                            Surface(
                                color = Color(0xFF0EA5E9),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "PF $targetPlatform",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        // Train subtitle
                        if (!trainName.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = buildString {
                                    if (!trainNumber.isNullOrBlank()) append("$trainNumber • ")
                                    append(trainName)
                                    val coachCount = rake.count { it.classType !in listOf("LOCO","EOG","CAB","PANTRY") }
                                    append(" • $coachCount LHB Coaches")
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Close button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                            .clickable { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✕", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    }
                }
            }

            // ── CATEGORY CHIP SCROLL ROW ────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(TrainCategory.values().toList()) { cat ->
                    val isSelected = cat == selectedCategory
                    Surface(
                        color = if (isSelected) chipSel else chipUnsel,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.clickable { selectedCategory = cat; trainOffsetPx = 0f; hasAutoSnapped = false }
                    ) {
                        Text(
                            text = "${cat.emoji} ${cat.displayName}",
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else textMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            HorizontalDivider(color = divider, thickness = 0.5.dp)

            // ── ENTRY GATE SELECTOR + SNAP BUTTON ─────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Gate selector pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Entry:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textMuted
                    )
                    layout.entries.take(3).forEach { entry ->
                        val isSel = entry.id == selectedEntryId
                        Surface(
                            color = if (isSel) chipSel else chipUnsel,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.clickable { selectedEntryId = entry.id }
                        ) {
                            Text(
                                text = entry.name,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else textMuted,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // Snap to user coach button
                if (!targetCoachCode.isNullOrBlank()) {
                    Surface(
                        color = greenAura,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.clickable { snapToUserCoach() }
                    ) {
                        Text(
                            text = "🎯 Snap to $targetCoachCode",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = divider, thickness = 0.5.dp)

            // ── UNIFIED CANVAS (Helicopter Platform + Draggable Rake) ───────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(if (isDark) Color(0xFF0B1120) else Color(0xFF1A2744))
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            trainOffsetPx = (trainOffsetPx + dragAmount).coerceIn(
                                -canvasWidthPx * 1.5f, canvasWidthPx * 0.8f
                            )
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    canvasWidthPx = size.width

                    drawUnifiedYardCanvas(
                        layout = layout,
                        targetPlatform = targetPlatform,
                        rake = rake,
                        userCoachCode = targetCoachCode,
                        trainOffsetPx = trainOffsetPx,
                        pulse = pulse,
                        isDark = isDark,
                        selectedEntryId = selectedEntryId,
                        route = route
                    )
                }

                // Swipe hint label (bottom of canvas, fades if user has dragged)
                val hintAlpha = if (abs(trainOffsetPx) > 60f) 0f else 0.7f
                if (hintAlpha > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "⟺  Swipe / Drag horizontally to inspect train",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = hintAlpha),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── COACH INFO CARD ─────────────────────────────────────────────────
            if (userCoach != null) {
                Surface(
                    color = cardBg,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Green coach badge
                        Surface(
                            color = greenAura,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = userCoach.code,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        // Coach class + position
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = TrainRakeDataService.classDisplayName(userCoach.classType),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Coach ${userCoachIdx + 1} of ${rake.size} in rake",
                                fontSize = 11.sp,
                                color = textMuted
                            )
                        }

                        // Walk info
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "~${route.estimatedMinutes} min walk",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0EA5E9)
                            )
                            Text(
                                text = route.recommendedBridge?.name ?: "Central FOB",
                                fontSize = 10.sp,
                                color = textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                // No PNR coach — show route summary
                Surface(
                    color = cardBg,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🚶", fontSize = 20.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Platform $targetPlatform",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = route.fromLabel,
                                fontSize = 11.sp,
                                color = textMuted
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${route.totalDistanceMeters}m",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0EA5E9)
                            )
                            Text(
                                text = "~${route.estimatedMinutes} min",
                                fontSize = 10.sp,
                                color = textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Canvas drawing function — the heart of the unified view
// ─────────────────────────────────────────────────────────────────────────────

private fun DrawScope.drawUnifiedYardCanvas(
    layout: JunctionLayout,
    targetPlatform: Int,
    rake: List<RakeCoach>,
    userCoachCode: String?,
    trainOffsetPx: Float,
    pulse: Float,
    isDark: Boolean,
    selectedEntryId: String?,
    route: NavigationRoute
) {
    val W = size.width
    val H = size.height

    // ── Layout geometry ──────────────────────────────────────────────────────
    val concourseH   = 36f                       // top gate bar
    val bottomBarH   = 32f                       // bottom gate bar
    val yardH        = H - concourseH - bottomBarH
    val numPlatforms = layout.totalPlatforms.coerceIn(2, 8)
    val trackZoneH   = yardH / (numPlatforms + 1f)
    val platformH    = trackZoneH * 0.38f
    val trackH       = trackZoneH * 0.28f
    val ballastH     = trackZoneH * 0.34f

    // ── Color palette ────────────────────────────────────────────────────────
    val yardBg        = Color(0xFF101828)
    val ballastColor  = Color(0xFF2D3748)
    val sleeperColor  = Color(0xFF4A3728)
    val railColor     = Color(0xFF718096)
    val pfSlab        = Color(0xFF2D3A52)
    val pfTarget      = Color(0xFF1E3A6E)
    val pfEdge        = Color(0xFF4A90D9)
    val tacticStripe  = Color(0xFFFFD700)
    val canopyColor   = Color(0xFF1E293B).copy(alpha = 0.85f)
    val fobBeam       = Color(0xFF4B5563)
    val fobGlass      = Color(0xFF60A5FA).copy(alpha = 0.5f)
    val walkDot       = Color(0xFF38BDF8)
    val greenGlow     = Color(0xFF22C55E)
    val whiteText     = Color(0xFFF1F5F9)
    val mutedText     = Color(0xFF94A3B8)
    val entryConcourse= Color(0xFF1A2235)

    // ── 1. BACKGROUND ────────────────────────────────────────────────────────
    drawRect(color = yardBg, size = size)

    // ── 2. TOP CONCOURSE (Gate 1) ────────────────────────────────────────────
    val selectedEntry = layout.entries.firstOrNull { it.id == selectedEntryId }
        ?: layout.entries.firstOrNull()
    drawRect(color = entryConcourse, size = Size(W, concourseH))
    // Label — rendered as a simple rect+text approximation using clipping
    drawRect(
        color = Color(0xFF0284C7).copy(alpha = 0.18f),
        topLeft = Offset(0f, 0f),
        size = Size(W, concourseH)
    )

    // ── 3. PLATFORMS + TRACKS + TRAIN RAKE ──────────────────────────────────
    var userCoachXCenter = -1f   // track for beacon pin
    var userCoachY = 0f

    // Calculate total rake pixel width
    val coachGap = 4f
    val totalRakeWidthPx = rake.sumOf { (it.widthDp * 2.75f + coachGap).toDouble() }.toFloat()

    // Accumulate train x as we iterate coaches
    var rakeStartX = trainOffsetPx + 20f   // 20px left margin

    for (pIdx in 0 until numPlatforms) {
        val pfNum = pIdx + 1
        val isTarget = pfNum == targetPlatform

        val zoneTop  = concourseH + pIdx * trackZoneH
        val pfTop    = zoneTop + ballastH / 2f
        val pfBot    = pfTop + platformH
        val trackTop = pfBot
        val trackBot = trackTop + trackH

        // ── Ballast bed ────────────────────────────────────────────────────
        drawRect(
            color = ballastColor,
            topLeft = Offset(0f, zoneTop),
            size = Size(W, trackZoneH)
        )

        // Cross-tie sleepers
        val sleeperSpacing = 18f
        var sx = 0f
        while (sx < W) {
            drawRect(
                color = sleeperColor,
                topLeft = Offset(sx, trackTop - 3f),
                size = Size(10f, trackH + 6f),
                alpha = 0.7f
            )
            sx += sleeperSpacing
        }

        // Rails (two lines)
        val railY1 = trackTop + trackH * 0.25f
        val railY2 = trackTop + trackH * 0.72f
        drawLine(railColor, Offset(0f, railY1), Offset(W, railY1), strokeWidth = 2f)
        drawLine(railColor, Offset(0f, railY2), Offset(W, railY2), strokeWidth = 2f)

        // ── Platform slab ──────────────────────────────────────────────────
        val pfColor = if (isTarget) pfTarget else pfSlab
        drawRoundRect(
            color = pfColor,
            topLeft = Offset(0f, pfTop),
            size = Size(W, platformH),
            cornerRadius = CornerRadius(3f, 3f)
        )

        // Platform edge highlight
        drawLine(
            color = if (isTarget) pfEdge else Color(0xFF475569),
            start = Offset(0f, pfBot),
            end = Offset(W, pfBot),
            strokeWidth = if (isTarget) 2.5f else 1.5f
        )

        // Tactile yellow safety stripes (edge of platform)
        val stripeW = 10f
        for (i in 0..6) {
            val stripeX = i * (W / 7f)
            drawRect(
                color = tacticStripe,
                topLeft = Offset(stripeX, pfBot - stripeW),
                size = Size(W / 14f, stripeW),
                alpha = 0.75f
            )
        }

        // Platform shelter canopy (semi-transparent roof over middle 70%)
        val canopyL = W * 0.1f
        val canopyR = W * 0.9f
        drawRect(
            color = canopyColor,
            topLeft = Offset(canopyL, pfTop),
            size = Size(canopyR - canopyL, platformH * 0.45f)
        )

        // ── Train rake on target platform track ────────────────────────────
        if (isTarget) {
            val trainY = trackTop + 2f
            val trainH = trackH - 4f

            var drawX = rakeStartX
            rake.forEachIndexed { idx, coach ->
                val coachW = coach.widthDp * 2.75f
                val bodyColor = Color(coach.bodyColor)
                val roofColor = Color(coach.roofColor)
                val stripeColor = coach.stripeColor?.let { Color(it) }
                val isUserCoach = coach.code == userCoachCode

                // Coach body
                drawRoundRect(
                    color = bodyColor,
                    topLeft = Offset(drawX, trainY),
                    size = Size(coachW, trainH),
                    cornerRadius = CornerRadius(if (coach.isAeroNose) 10f else 4f, 4f)
                )

                // Roof panel
                drawRoundRect(
                    color = roofColor,
                    topLeft = Offset(drawX, trainY),
                    size = Size(coachW, trainH * 0.3f),
                    cornerRadius = CornerRadius(if (coach.isAeroNose) 10f else 4f, 2f)
                )

                // Colour stripe
                if (stripeColor != null) {
                    drawRect(
                        color = stripeColor,
                        topLeft = Offset(drawX, trainY + trainH * 0.3f),
                        size = Size(coachW, trainH * 0.12f)
                    )
                }

                // Roof vent dots
                if (coach.classType !in listOf("LOCO", "CAB", "EOG")) {
                    val ventY = trainY + trainH * 0.12f
                    for (v in 0..2) {
                        drawCircle(
                            color = Color(0xFF374151),
                            radius = 3f,
                            center = Offset(drawX + coachW * (0.25f + v * 0.25f), ventY)
                        )
                    }
                }

                // Loco/cab pantograph
                if (coach.isAeroNose || coach.classType == "LOCO") {
                    val pantoX = drawX + coachW / 2f
                    val pantoY = trainY
                    // Simple pantograph lines
                    drawLine(Color(0xFF9CA3AF), Offset(pantoX - 10f, pantoY), Offset(pantoX, pantoY - 12f), strokeWidth = 2f)
                    drawLine(Color(0xFF9CA3AF), Offset(pantoX + 10f, pantoY), Offset(pantoX, pantoY - 12f), strokeWidth = 2f)
                    drawLine(Color(0xFF9CA3AF), Offset(pantoX - 10f, pantoY - 12f), Offset(pantoX + 10f, pantoY - 12f), strokeWidth = 2f)
                }

                // Coupler between coaches
                if (idx < rake.size - 1) {
                    drawRect(
                        color = Color(0xFF374151),
                        topLeft = Offset(drawX + coachW, trainY + trainH * 0.42f),
                        size = Size(coachGap, trainH * 0.16f)
                    )
                }

                // Green aura for user's coach
                if (isUserCoach) {
                    userCoachXCenter = drawX + coachW / 2f
                    userCoachY = trainY + trainH / 2f

                    // Pulsing shadow layer
                    val auraAlpha = 0.18f + 0.22f * pulse
                    val auraExpand = 6f + 8f * pulse
                    drawRoundRect(
                        color = greenGlow.copy(alpha = auraAlpha),
                        topLeft = Offset(drawX - auraExpand, trainY - auraExpand),
                        size = Size(coachW + auraExpand * 2f, trainH + auraExpand * 2f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    // Hard green outline
                    drawRoundRect(
                        color = greenGlow,
                        topLeft = Offset(drawX - 2f, trainY - 2f),
                        size = Size(coachW + 4f, trainH + 4f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 2.5f)
                    )
                }

                drawX += coachW + coachGap
            }
        }
    }

    // ── 4. FOOT OVER BRIDGES (drawn AFTER train — floats on top) ────────────
    layout.bridges.forEach { bridge ->
        val bridgeX = W * bridge.relativePosition
        val bridgeTop = concourseH
        val bridgeBot = H - bottomBarH
        val beamW = 20f

        // Drop shadow
        drawRect(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(bridgeX - beamW / 2f + 4f, bridgeTop + 4f),
            size = Size(beamW, bridgeBot - bridgeTop)
        )

        // FOB beam
        drawRect(
            color = fobBeam,
            topLeft = Offset(bridgeX - beamW / 2f, bridgeTop),
            size = Size(beamW, bridgeBot - bridgeTop)
        )

        // Glass panels on each platform crossing
        for (pIdx in 0 until layout.totalPlatforms) {
            val zoneTop = concourseH + pIdx * trackZoneH
            val pfTop   = zoneTop + ballastH / 2f
            val pfBot   = pfTop + platformH
            drawRect(
                color = fobGlass,
                topLeft = Offset(bridgeX - beamW / 2f, pfTop),
                size = Size(beamW, pfBot - pfTop)
            )
        }

        // Elevator/lift cabin at top
        if (bridge.hasLift || bridge.hasEscalator) {
            drawRoundRect(
                color = Color(0xFF1D4ED8).copy(alpha = 0.8f),
                topLeft = Offset(bridgeX - 14f, bridgeTop + 2f),
                size = Size(28f, 22f),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }

    // ── 5. DOTTED WALKING PATH ───────────────────────────────────────────────
    if (route.recommendedBridge != null) {
        val bridgeX = W * route.recommendedBridge.relativePosition
        val entryY  = concourseH - 2f
        // Vertical drop from gate to bridge on target platform
        val pfIdx = (targetPlatform - 1).coerceIn(0, numPlatforms - 1)
        val zoneTop = concourseH + pfIdx * trackZoneH
        val pfBot   = zoneTop + ballastH / 2f + platformH

        // Horizontal from edge to bridge
        drawLine(
            color = walkDot,
            start = Offset(if (selectedEntry?.nearestPlatform ?: 1 > targetPlatform / 2) 0f else W, entryY),
            end = Offset(bridgeX, entryY),
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
            alpha = 0.8f
        )
        // Vertical down bridge
        drawLine(
            color = walkDot,
            start = Offset(bridgeX, entryY),
            end = Offset(bridgeX, pfBot + 4f),
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
            alpha = 0.8f
        )
        // Along platform edge to coach
        if (userCoachXCenter > 0f) {
            drawLine(
                color = walkDot,
                start = Offset(bridgeX, pfBot + 4f),
                end = Offset(userCoachXCenter, pfBot + 4f),
                strokeWidth = 2.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f),
                alpha = 0.8f
            )
        }
    }

    // ── 6. "🎯 YOUR COACH" BEACON PIN (always last — never clipped) ──────────
    if (userCoachXCenter > 0f) {
        val pinX = userCoachXCenter
        val pinY = userCoachY

        // Outer pulse ring
        drawCircle(
            color = greenGlow.copy(alpha = 0.2f + 0.3f * pulse),
            radius = 20f + 8f * pulse,
            center = Offset(pinX, pinY)
        )
        // Inner ring
        drawCircle(
            color = greenGlow.copy(alpha = 0.6f),
            radius = 12f,
            center = Offset(pinX, pinY)
        )
        // Pin dot
        drawCircle(
            color = Color.White,
            radius = 5f,
            center = Offset(pinX, pinY)
        )
        // Green crosshair lines
        drawLine(greenGlow, Offset(pinX - 18f, pinY), Offset(pinX - 7f, pinY), strokeWidth = 2f)
        drawLine(greenGlow, Offset(pinX + 7f, pinY), Offset(pinX + 18f, pinY), strokeWidth = 2f)
        drawLine(greenGlow, Offset(pinX, pinY - 18f), Offset(pinX, pinY - 7f), strokeWidth = 2f)
        drawLine(greenGlow, Offset(pinX, pinY + 7f), Offset(pinX, pinY + 18f), strokeWidth = 2f)
    }

    // ── 7. BOTTOM CONCOURSE (Gate 2 / Secondary entry) ───────────────────────
    val secondaryEntry = layout.entries.getOrNull(1)
    drawRect(
        color = entryConcourse,
        topLeft = Offset(0f, H - bottomBarH),
        size = Size(W, bottomBarH)
    )
    drawRect(
        color = Color(0xFFDC2626).copy(alpha = 0.12f),
        topLeft = Offset(0f, H - bottomBarH),
        size = Size(W, bottomBarH)
    )
}
