package com.railgaadi.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.model.*
import com.railgaadi.app.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationLayoutVisualizer(
    stationCode: String,
    stationName: String,
    targetPlatform: Int,
    targetCoachCode: String? = null,
    trainNumber: String? = null,
    trainName: String? = null,
    isDark: Boolean = false,
    onClose: () -> Unit
) {
    val layout = remember(stationCode, stationName, targetPlatform) {
        JunctionDataService.getLayoutForStation(
            stationCode = stationCode,
            stationName = stationName,
            platformCountHint = targetPlatform.coerceAtLeast(3)
        )
    }

    var selectedEntryId by remember(layout) {
        mutableStateOf(layout.entries.firstOrNull()?.id)
    }
    var preferAccessible by remember { mutableStateOf(true) }

    val route = remember(layout, selectedEntryId, targetPlatform, targetCoachCode, preferAccessible) {
        JunctionDataService.calculateNavigationRoute(
            layout = layout,
            fromEntryId = selectedEntryId,
            targetPlatform = targetPlatform,
            targetCoachCode = targetCoachCode,
            preferAccessible = preferAccessible
        )
    }

    // Colors
    val bgCol = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textMuted = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = bgCol),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE2E8F0)),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 680.dp)
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── 1. HEADER BAR ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🧭", fontSize = 16.sp)
                        Text(
                            text = layout.stationName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = textPrimary
                        )
                        Box(
                            modifier = Modifier
                                .background(if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE), RoundedCornerShape(5.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(layout.stationCode, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)
                        }
                    }
                    Text(
                        text = "Junction Platform & Bridge Navigator",
                        fontSize = 11.sp,
                        color = textMuted,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                }
            }

            HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))

            // ── 2. TARGET HERO BANNER ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isDark) Color(0xFF0369A1).copy(alpha = 0.20f) else Color(0xFFF0F9FF),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(1.dp, if (isDark) Color(0xFF0284C7).copy(alpha = 0.40f) else Color(0xFFBAE6FD), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Platform $targetPlatform",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                        )
                        if (!targetCoachCode.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFD97706), RoundedCornerShape(5.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Coach $targetCoachCode",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    if (!trainName.isNullOrBlank()) {
                        Text(
                            text = "${trainNumber ?: ""} $trainName".trim(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Estimated walk badge
                Box(
                    modifier = Modifier
                        .background(if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "🚶 ${route.totalDistanceMeters}m • ~${route.estimatedMinutes} min",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                    )
                }
            }

            // ── 3. ENTRY & ACCESSIBILITY CONTROLS ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Entry side chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(layout.entries) { entry ->
                        val isSelected = entry.id == selectedEntryId
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) RailBluePrimary
                                    else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) RailBluePrimary
                                    else if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFCBD5E1),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedEntryId = entry.id }
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = entry.name,
                                fontSize = 10.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else textPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Luggage / Accessibility Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (preferAccessible) if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE)
                            else if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                        )
                        .border(
                            1.dp,
                            if (preferAccessible) RailBluePrimary else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { preferAccessible = !preferAccessible }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (preferAccessible) "🛗 Lifts/Escalator" else "🪜 Any Bridge",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (preferAccessible) RailBluePrimary else textMuted
                    )
                }
            }

            // ── 4. HELICOPTER DRONE AERIAL YARD VIEW ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("🚁", fontSize = 13.sp)
                    Text(
                        text = "Helicopter Yard View",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .background(if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Alt 250m • Bird's Eye",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF7DD3FC) else Color(0xFF0369A1)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0xFF090D16) else Color(0xFF0F172A))
                    .border(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFF334155), RoundedCornerShape(14.dp))
            ) {
                Station2DSchematicCanvas(
                    layout = layout,
                    selectedEntryId = selectedEntryId,
                    targetPlatform = targetPlatform,
                    targetCoachCode = targetCoachCode,
                    route = route,
                    isDark = isDark
                )
            }

            // ── 5. STEP-BY-STEP TURN DIRECTIONS ──
            Text(
                text = "Turn-by-Turn Path to Platform $targetPlatform",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary,
                modifier = Modifier.padding(top = 2.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(route.steps) { step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(cardBg, RoundedCornerShape(10.dp))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Icon capsule
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(
                                    when {
                                        step.isEscalatorOrLift -> if (isDark) Color(0xFF0C4A6E) else Color(0xFFE0F2FE)
                                        step.isFobCrossing -> if (isDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                                        else -> if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(step.icon, fontSize = 13.sp)
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = step.instruction,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = step.subDetail,
                                fontSize = 10.sp,
                                color = textMuted,
                                lineHeight = 13.sp
                            )
                        }

                        Text(
                            text = "${step.distanceMeters}m",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helicopter / Drone Bird's-Eye Canvas rendering the entire railway station yard:
 * - Separated concrete platform slabs with yellow tactile warning stripes
 * - Realistic crushed gravel track ballast beds with sleepers and steel rails
 * - Parked train rake alongside target platform with exact coach bay alignment
 * - Elevated Foot-Over-Bridges casting realistic overhead drop shadows
 * - Dotted walking navigation path from entry gate across FOB to target coach door
 */
@Composable
private fun Station2DSchematicCanvas(
    layout: JunctionLayout,
    selectedEntryId: String?,
    targetPlatform: Int,
    targetCoachCode: String?,
    route: NavigationRoute,
    isDark: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_pin")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val totalPfs = layout.totalPlatforms.coerceAtLeast(2)
        val horizontalMargin = 38.dp.toPx()
        val topConcourseHeight = 24.dp.toPx()
        val bottomConcourseHeight = 20.dp.toPx()
        val yardHeight = size.height - topConcourseHeight - bottomConcourseHeight
        val yardWidth = size.width - (horizontalMargin * 2)

        val pfSpacing = yardHeight / totalPfs.toFloat()
        val platformHeight = (pfSpacing * 0.44f).coerceIn(12.dp.toPx(), 22.dp.toPx())

        // ── 0. TOP CONCOURSE (GATE 1 CITY SIDE) ──
        drawRoundRect(
            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
            topLeft = Offset(horizontalMargin, 4.dp.toPx()),
            size = Size(yardWidth, topConcourseHeight - 6.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        // ── 1. RAIL TRACK BALLAST BEDS & SLEEPERS ──
        for (i in 0 until totalPfs) {
            val pfCenterY = topConcourseHeight + (i * pfSpacing) + (pfSpacing / 2)
            val ballastY = pfCenterY - (pfSpacing * 0.28f)
            val ballastHeight = pfSpacing * 0.56f

            // Crushed gravel dark ballast bed
            drawRect(
                color = if (isDark) Color(0xFF0D1322) else Color(0xFF1E293B),
                topLeft = Offset(horizontalMargin - 8.dp.toPx(), ballastY),
                size = Size(yardWidth + 16.dp.toPx(), ballastHeight)
            )

            // Sleepers (Cross ties)
            val sleeperSpacing = 11.dp.toPx()
            var sx = horizontalMargin - 6.dp.toPx()
            val sleeperColor = if (isDark) Color(0xFF1F293D) else Color(0xFF334155)
            while (sx <= horizontalMargin + yardWidth + 6.dp.toPx()) {
                drawLine(
                    color = sleeperColor,
                    start = Offset(sx, ballastY + 2.dp.toPx()),
                    end = Offset(sx, ballastY + ballastHeight - 2.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
                sx += sleeperSpacing
            }

            // Pair of shiny steel rails
            val rail1Y = ballastY + (ballastHeight * 0.32f)
            val rail2Y = ballastY + (ballastHeight * 0.68f)
            val railColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
            drawLine(
                color = railColor,
                start = Offset(horizontalMargin - 8.dp.toPx(), rail1Y),
                end = Offset(horizontalMargin + yardWidth + 8.dp.toPx(), rail1Y),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = railColor,
                start = Offset(horizontalMargin - 8.dp.toPx(), rail2Y),
                end = Offset(horizontalMargin + yardWidth + 8.dp.toPx(), rail2Y),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // ── 2. SEPARATED PLATFORM SLABS WITH TACTILE STRIPES ──
        for (i in 0 until totalPfs) {
            val pfNum = i + 1
            val pfCenterY = topConcourseHeight + (i * pfSpacing) + (pfSpacing / 2)
            val isTarget = (pfNum == targetPlatform)

            val slabTop = pfCenterY - (platformHeight / 2)

            // Soft drop shadow beneath platform slab (Proves elevation over ballast!)
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.45f),
                topLeft = Offset(horizontalMargin, slabTop + 3.dp.toPx()),
                size = Size(yardWidth, platformHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Concrete platform surface
            val platformSurfaceColor = when {
                isTarget -> if (isDark) Color(0xFF0C4A6E) else Color(0xFF0284C7)
                isDark -> Color(0xFF334155)
                else -> Color(0xFFE2E8F0)
            }
            drawRoundRect(
                color = platformSurfaceColor,
                topLeft = Offset(horizontalMargin, slabTop),
                size = Size(yardWidth, platformHeight),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )

            // High-visibility Yellow Tactile Safety Warning Stripe on top and bottom platform edges
            val tactileColor = Color(0xFFFACC15)
            drawRect(
                color = tactileColor,
                topLeft = Offset(horizontalMargin, slabTop),
                size = Size(yardWidth, 1.5.dp.toPx())
            )
            drawRect(
                color = tactileColor,
                topLeft = Offset(horizontalMargin, slabTop + platformHeight - 1.5.dp.toPx()),
                size = Size(yardWidth, 1.5.dp.toPx())
            )

            // Central shelter canopy strip
            drawRoundRect(
                color = if (isTarget) Color(0xFF0284C7) else (if (isDark) Color(0xFF1E293B) else Color(0xFFCBD5E1)),
                topLeft = Offset(horizontalMargin + 30.dp.toPx(), slabTop + (platformHeight * 0.2f)),
                size = Size(yardWidth - 60.dp.toPx(), platformHeight * 0.6f),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // Platform indicator badge on left & right
            val badgeBg = if (isTarget) Color(0xFF0284C7) else (if (isDark) Color(0xFF1E293B) else Color(0xFF94A3B8))
            drawCircle(
                color = badgeBg,
                radius = 7.dp.toPx(),
                center = Offset(horizontalMargin - 14.dp.toPx(), pfCenterY)
            )
            drawCircle(
                color = badgeBg,
                radius = 7.dp.toPx(),
                center = Offset(horizontalMargin + yardWidth + 14.dp.toPx(), pfCenterY)
            )
        }

        // ── 3. PARKED TRAIN RAKE BESIDE TARGET PLATFORM ──
        val targetIdx = (targetPlatform - 1).coerceIn(0, totalPfs - 1)
        val targetPfCenterY = topConcourseHeight + (targetIdx * pfSpacing) + (pfSpacing / 2)
        val targetTrackY = targetPfCenterY + (platformHeight / 2) + 6.dp.toPx()

        val coachWidth = 26.dp.toPx()
        val coachHeight = 7.dp.toPx()
        val coachGap = 2.dp.toPx()
        val trainStartX = horizontalMargin + (yardWidth * 0.12f)
        val coachCount = 9

        for (c in 0 until coachCount) {
            val cx = trainStartX + (c * (coachWidth + coachGap))
            val isLoco = (c == 0)
            val isTargetCoach = (c == 4) // Middle coach alignment

            // Coach shadow
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(cx + 1.dp.toPx(), targetTrackY + 1.5.dp.toPx()),
                size = Size(coachWidth, coachHeight)
            )

            // Coach body
            val coachColor = when {
                isLoco -> Color(0xFFDC2626) // Red Loco
                isTargetCoach -> Color(0xFF10B981) // Green Target Coach
                else -> Color(0xFF0369A1) // Rail Blue ICF/LHB Coach
            }
            drawRoundRect(
                color = coachColor,
                topLeft = Offset(cx, targetTrackY),
                size = Size(coachWidth, coachHeight),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )

            // Coach roof ventilation strip
            drawRect(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(cx + 2.dp.toPx(), targetTrackY + 1.5.dp.toPx()),
                size = Size(coachWidth - 4.dp.toPx(), 1.5.dp.toPx())
            )
        }

        // ── 4. ELEVATED FOOT-OVER-BRIDGES (FOBs) WITH DEEP DROP SHADOWS ──
        val bridgeYStart = topConcourseHeight - 2.dp.toPx()
        val bridgeYEnd = size.height - bottomConcourseHeight + 2.dp.toPx()

        layout.bridges.forEach { fob ->
            val fobX = horizontalMargin + (yardWidth * fob.relativePosition)
            val isRecommended = (fob.id == route.recommendedBridge?.id)
            val bridgeBeamWidth = 10.dp.toPx()

            // Directional Drop Shadow across all platforms and tracks
            // This is the #1 visual cue that creates the authentic helicopter overhead perspective!
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.55f),
                topLeft = Offset(fobX - (bridgeBeamWidth / 2) + 6.dp.toPx(), bridgeYStart + 4.dp.toPx()),
                size = Size(bridgeBeamWidth, bridgeYEnd - bridgeYStart),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // Elevated Bridge Deck
            val bridgeColor = when {
                isRecommended -> Color(0xFF0284C7)
                isDark -> Color(0xFF475569)
                else -> Color(0xFF64748B)
            }
            drawRoundRect(
                color = bridgeColor,
                topLeft = Offset(fobX - (bridgeBeamWidth / 2), bridgeYStart),
                size = Size(bridgeBeamWidth, bridgeYEnd - bridgeYStart),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )

            // Bridge Handrail lines
            val railCol = if (isRecommended) Color(0xFF38BDF8) else Color(0xFF94A3B8)
            drawLine(
                color = railCol,
                start = Offset(fobX - (bridgeBeamWidth / 2), bridgeYStart),
                end = Offset(fobX - (bridgeBeamWidth / 2), bridgeYEnd),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = railCol,
                start = Offset(fobX + (bridgeBeamWidth / 2), bridgeYStart),
                end = Offset(fobX + (bridgeBeamWidth / 2), bridgeYEnd),
                strokeWidth = 1.dp.toPx()
            )

            // Glass Elevator & Escalator towers on bridge landings
            if (fob.hasLift || fob.hasEscalator) {
                for (p in 0 until totalPfs) {
                    val pCenterY = topConcourseHeight + (p * pfSpacing) + (pfSpacing / 2)
                    drawCircle(
                        color = Color(0xFF10B981),
                        radius = 3.5.dp.toPx(),
                        center = Offset(fobX + (bridgeBeamWidth / 2) + 4.dp.toPx(), pCenterY)
                    )
                }
            }
        }

        // ── 5. HELICOPTER TARGET CROSSHAIR & COACH PIN ──
        val targetCoachX = trainStartX + (4 * (coachWidth + coachGap)) + (coachWidth / 2)
        val pinY = targetPfCenterY

        // Pulsing target halo
        drawCircle(
            color = Color(0xFF10B981).copy(alpha = pulseAlpha * 0.45f),
            radius = 18.dp.toPx(),
            center = Offset(targetCoachX, pinY)
        )
        // Solid pin dot
        drawCircle(
            color = Color(0xFF10B981),
            radius = 5.dp.toPx(),
            center = Offset(targetCoachX, pinY)
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = Offset(targetCoachX, pinY)
        )

        // Helicopter Crosshairs
        drawLine(
            color = Color(0xFF34D399),
            start = Offset(targetCoachX - 16.dp.toPx(), pinY),
            end = Offset(targetCoachX + 16.dp.toPx(), pinY),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color(0xFF34D399),
            start = Offset(targetCoachX, pinY - 16.dp.toPx()),
            end = Offset(targetCoachX, pinY + 16.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )

        // ── 6. DOTTED WALKING ROUTE GUIDANCE ──
        route.recommendedBridge?.let { fob ->
            val fobX = horizontalMargin + (yardWidth * fob.relativePosition)
            val isSecondary = (layout.entries.find { it.id == selectedEntryId }?.nearestPlatform ?: 1) > 1
            val startY = if (isSecondary) {
                size.height - bottomConcourseHeight
            } else {
                topConcourseHeight / 2
            }

            val path = Path().apply {
                moveTo(horizontalMargin + 30.dp.toPx(), startY)
                lineTo(fobX, startY)
                lineTo(fobX, pinY)
                lineTo(targetCoachX, pinY)
            }

            drawPath(
                path = path,
                color = Color(0xFF38BDF8),
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                    cap = StrokeCap.Round
                )
            )

            // Starting Gate Pin
            drawCircle(
                color = Color(0xFFF59E0B),
                radius = 4.dp.toPx(),
                center = Offset(horizontalMargin + 30.dp.toPx(), startY)
            )
        }

        // ── 7. BOTTOM CONCOURSE (GATE 2 CANTT SIDE) ──
        drawRoundRect(
            color = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
            topLeft = Offset(horizontalMargin, size.height - bottomConcourseHeight + 4.dp.toPx()),
            size = Size(yardWidth, bottomConcourseHeight - 8.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )
    }
}
