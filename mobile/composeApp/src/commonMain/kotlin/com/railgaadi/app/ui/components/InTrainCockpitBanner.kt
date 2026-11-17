package com.railgaadi.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.*

@Composable
fun InTrainCockpitBanner(
    modifier: Modifier = Modifier,
    onOpenAlarmModal: (() -> Unit)? = null
) {
    val activeTrain = TrainTrackerState.activeTrain
    if (activeTrain.number.isBlank()) return

    val isDark = TrainTrackerState.isDarkMode
    val isInside = TrainTrackerState.isInsideTrain
    val liveLoc = TrainTrackerState.currentGpsLocation
    val locText = TrainTrackerState.inTrainLocationText
    val nextHaltText = TrainTrackerState.inTrainNextHaltText
    val estArrivalMins = TrainTrackerState.inTrainEstArrivalMinutes
    val remainingKm = TrainTrackerState.inTrainRemainingKm
    val activeAlarm = TrainTrackerState.activeAlarm

    // Pulsing green beacon animation for live GPS
    val infiniteTransition = rememberInfiniteTransition(label = "gpsPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isInside) {
                if (isDark) Color(0xFF0F231D) else Color(0xFFF0FDF4)
            } else {
                if (isDark) DarkSurface else LightSurface
            }
        ),
        border = BorderStroke(
            1.2.dp,
            if (isInside) {
                if (isDark) Color(0xFF10B981).copy(alpha = 0.5f) else Color(0xFF86EFAC)
            } else {
                if (isDark) DarkBorder else LightBorder
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isInside) 4.dp else 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Inside Train status + Switch Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isInside) Color(0xFF10B981).copy(alpha = 0.18f)
                                else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isInside) "🚆" else "🧭", fontSize = 16.sp)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isInside) "Inside ${activeTrain.number}" else "Are you inside this train?",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isInside) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981).copy(alpha = pulseAlpha))
                                )
                            }
                        }

                        Text(
                            text = if (isInside) "🛰️ Live Satellite Telemetry Active"
                            else "Turn ON for zero-lag speed & exact station detection",
                            fontSize = 11.sp,
                            color = if (isInside) Color(0xFF10B981)
                            else if (isDark) DarkTextSecondary else LightTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Interactive Toggle Switch
                Switch(
                    checked = isInside,
                    onCheckedChange = { checked ->
                        TrainTrackerState.toggleInsideTrainMode(checked)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF10B981),
                        uncheckedThumbColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                        uncheckedTrackColor = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                    ),
                    modifier = Modifier.padding(start = 6.dp)
                )
            }

            // Expanded Cockpit Body when Inside Train Mode is ACTIVE
            AnimatedVisibility(
                visible = isInside,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(
                        color = if (isDark) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFBBF7D0),
                        thickness = 0.8.dp
                    )

                    // 1. Telemetry Strip: Speedometer + GPS Accuracy + ETA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Live Speedometer
                        val speedKmh = liveLoc?.speedKmh?.toInt() ?: TrainTrackerState.liveStatus?.speedKmh ?: 0
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFFDCFCE7))
                                .border(0.8.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$speedKmh km/h",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFF34D399) else Color(0xFF047857)
                                )
                                Text(
                                    text = "Physical Speed",
                                    fontSize = 10.sp,
                                    color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF065F46)
                                )
                            }
                        }

                        // Next Halt Distance
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                .border(0.8.dp, if (isDark) DarkBorder else LightBorder, RoundedCornerShape(12.dp))
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (remainingKm > 0) "${remainingKm.toInt().coerceAtLeast(1)} km" else "Arrived",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RailBluePrimary
                                )
                                Text(
                                    text = "To Next Halt",
                                    fontSize = 10.sp,
                                    color = if (isDark) DarkTextSecondary else LightTextSecondary
                                )
                            }
                        }

                        // ETA Minutes
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                .border(0.8.dp, if (isDark) DarkBorder else LightBorder, RoundedCornerShape(12.dp))
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (estArrivalMins > 0) "~${estArrivalMins}m" else "< 1m",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFFF59E0B) else Color(0xFFD97706)
                                )
                                Text(
                                    text = "Est. Arrival",
                                    fontSize = 10.sp,
                                    color = if (isDark) DarkTextSecondary else LightTextSecondary
                                )
                            }
                        }
                    }

                    // 2. Exact Physical Location Callout
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC))
                            .border(0.5.dp, if (isDark) DarkBorder else LightBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📍", fontSize = 12.sp)
                            Text(
                                text = "Current Position:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) DarkTextSecondary else LightTextSecondary
                            )
                            Text(
                                text = locText.ifBlank { "Detecting physical train segment..." },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎯", fontSize = 12.sp)
                            Text(
                                text = "Next Scheduled Halt:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDark) DarkTextSecondary else LightTextSecondary
                            )
                            Text(
                                text = nextHaltText.ifBlank { "Syncing route schedule..." },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RailBluePrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 3. Quick Wake-up Alarm Integration
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeAlarm?.isArmed == true) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🔔", fontSize = 13.sp)
                                Text(
                                    text = "Alarm set for ${activeAlarm.stationName} (${activeAlarm.triggerDistanceKm.toInt()}km before)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⏰", fontSize = 12.sp)
                                Text(
                                    text = "Want a wake-up call before your stop?",
                                    fontSize = 11.sp,
                                    color = if (isDark) DarkTextSecondary else LightTextSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RailBluePrimary)
                                    .clickable {
                                        onOpenAlarmModal?.invoke() ?: TrainTrackerState.openAlarmSetup()
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "Set Alarm",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // 4. Community Crowdsource Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📡", fontSize = 10.sp)
                        Text(
                            text = "Contributing zero-lag live radar telemetry • 100% Free & Private",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFF6EE7B7) else Color(0xFF059669),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

