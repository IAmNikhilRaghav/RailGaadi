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

            // ── 4. INTERACTIVE 2D STATION SCHEMATIC CANVAS ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDark) Color(0xFF090D16) else Color(0xFFF1F5F9))
                    .border(1.dp, if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            ) {
                Station2DSchematicCanvas(
                    layout = layout,
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
 * 2D Canvas rendering the complete railway station platform grid, tracks,
 * Foot Over Bridges with escalator icons, and the train rake with the target coach pin!
 */
@Composable
private fun Station2DSchematicCanvas(
    layout: JunctionLayout,
    targetPlatform: Int,
    targetCoachCode: String?,
    route: NavigationRoute,
    isDark: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_pin")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val totalPfs = layout.totalPlatforms.coerceAtLeast(1)
        val horizontalMargin = 42.dp.toPx()
        val verticalMargin = 22.dp.toPx()
        val availableWidth = size.width - (horizontalMargin * 2)
        val availableHeight = size.height - (verticalMargin * 2)

        val pfSpacing = if (totalPfs > 1) availableHeight / (totalPfs - 1) else availableHeight / 2

        // 1. Draw Railway Track ballast lines between platforms
        for (i in 0 until totalPfs - 1) {
            val trackY = verticalMargin + (i * pfSpacing) + (pfSpacing / 2)
            // Double iron rails
            drawLine(
                color = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                start = Offset(horizontalMargin, trackY - 2.dp.toPx()),
                end = Offset(size.width - horizontalMargin, trackY - 2.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )
            drawLine(
                color = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                start = Offset(horizontalMargin, trackY + 2.dp.toPx()),
                end = Offset(size.width - horizontalMargin, trackY + 2.dp.toPx()),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // 2. Draw Platform Surfaces
        for (i in 0 until totalPfs) {
            val pfNumber = i + 1
            val pfY = verticalMargin + (i * pfSpacing)
            val isTarget = (pfNumber == targetPlatform)

            val pfColor = when {
                isTarget -> if (isDark) Color(0xFF0369A1) else Color(0xFF38BDF8)
                isDark -> Color(0xFF1E293B)
                else -> Color.White
            }
            val pfBorder = when {
                isTarget -> Color(0xFF0284C7)
                isDark -> Color(0xFF334155)
                else -> Color(0xFFCBD5E1)
            }

            // Platform rect
            val rectHeight = 10.dp.toPx()
            drawRoundRect(
                color = pfColor,
                topLeft = Offset(horizontalMargin, pfY - (rectHeight / 2)),
                size = Size(availableWidth, rectHeight),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
            drawRoundRect(
                color = pfBorder,
                topLeft = Offset(horizontalMargin, pfY - (rectHeight / 2)),
                size = Size(availableWidth, rectHeight),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                style = Stroke(width = if (isTarget) 1.5.dp.toPx() else 1.dp.toPx())
            )

            // Platform Number Badge on Left edge
            drawCircle(
                color = if (isTarget) Color(0xFF0284C7) else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                radius = 7.dp.toPx(),
                center = Offset(horizontalMargin - 14.dp.toPx(), pfY)
            )

            // Platform Number Badge on Right edge
            drawCircle(
                color = if (isTarget) Color(0xFF0284C7) else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                radius = 7.dp.toPx(),
                center = Offset(size.width - horizontalMargin + 14.dp.toPx(), pfY)
            )
        }

        // 3. Draw Foot Over Bridges (FOBs) traversing vertically across platforms
        layout.bridges.forEach { fob ->
            val fobX = horizontalMargin + (availableWidth * fob.relativePosition)
            val isBridgeRecommended = (fob.id == route.recommendedBridge?.id)

            val minPf = fob.connectedPlatforms.minOrNull() ?: 1
            val maxPf = fob.connectedPlatforms.maxOrNull() ?: totalPfs
            val startY = verticalMargin + ((minPf - 1) * pfSpacing) - 6.dp.toPx()
            val endY = verticalMargin + ((maxPf - 1) * pfSpacing) + 6.dp.toPx()

            // Bridge beam shadow / background
            drawRoundRect(
                color = if (isBridgeRecommended) {
                    if (isDark) Color(0xFF0284C7).copy(alpha = 0.85f) else Color(0xFF0284C7)
                } else {
                    if (isDark) Color(0xFF475569) else Color(0xFF94A3B8)
                },
                topLeft = Offset(fobX - 4.dp.toPx(), startY),
                size = Size(8.dp.toPx(), endY - startY),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )

            // Lift / Escalator indicator circle on bridge
            if (fob.hasLift || fob.hasEscalator) {
                drawCircle(
                    color = Color(0xFF10B981),
                    radius = 4.dp.toPx(),
                    center = Offset(fobX, startY - 6.dp.toPx())
                )
            }
        }

        // 4. Target Coach Stopping Pin on Target Platform
        val targetPfIndex = (targetPlatform - 1).coerceIn(0, totalPfs - 1)
        val targetPfY = verticalMargin + (targetPfIndex * pfSpacing)
        val coachX = horizontalMargin + (availableWidth * route.coachStopRelativeOffset)

        // Pulsing target halo
        drawCircle(
            color = Color(0xFFF59E0B).copy(alpha = pulseAlpha * 0.4f),
            radius = 16.dp.toPx(),
            center = Offset(coachX, targetPfY)
        )
        // Solid pin dot
        drawCircle(
            color = Color(0xFFF59E0B),
            radius = 6.dp.toPx(),
            center = Offset(coachX, targetPfY)
        )
        drawCircle(
            color = Color.White,
            radius = 2.5.dp.toPx(),
            center = Offset(coachX, targetPfY)
        )

        // 5. Draw Walking Navigation Path (Dotted Line from Entry/FOB to Coach)
        route.recommendedBridge?.let { fob ->
            val fobX = horizontalMargin + (availableWidth * fob.relativePosition)
            val path = Path().apply {
                moveTo(fobX, targetPfY)
                lineTo(coachX, targetPfY)
            }
            drawPath(
                path = path,
                color = Color(0xFFF59E0B),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}
