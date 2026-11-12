package com.railgaadi.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.*

@Composable
fun AlarmRingingDialog() {
    val alarm = TrainTrackerState.activeAlarm ?: return
    val isDark = TrainTrackerState.isDarkMode

    // Pulsing radar animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Dialog(
        onDismissRequest = { /* Modal: require explicit dismiss or snooze */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFFFFFFF)
            ),
            border = BorderStroke(1.5.dp, RailBluePrimary.copy(alpha = 0.40f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pulsing Icon Target
                Box(
                    modifier = Modifier.size(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer pulsating wave
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(RailBluePrimary.copy(alpha = pulseAlpha))
                    )

                    // Inner glowing circle
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(RailBluePrimary, RailBlueDark)
                                )
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏰", fontSize = 34.sp)
                    }
                }

                // Alert Heading
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RailAmberWarning.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⚡ WAKE UP PASSENGER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RailAmberWarning,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Approaching Destination!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) DarkTextPrimary else LightTextPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                // Station Highlight Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) DarkSurface else Color(0xFFF1F5F9)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = alarm.stationName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RailBluePrimary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Station Code: ${alarm.stationCode}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) DarkTextSecondary else LightTextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Distance / Telemetry
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            alarm.distanceRemainingKm?.let { dist ->
                                Text(
                                    text = "📍 $dist km away",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = RailGreenOnTime
                                )
                            }
                            alarm.minutesRemaining?.let { mins ->
                                Text(
                                    text = "•  ⏱ ~${mins}m remaining",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "⚠️ Please collect your luggage, check under your berth, and prepare to deboard safely.",
                    fontSize = 11.sp,
                    color = if (isDark) DarkTextSecondary else LightTextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                // Dismiss & Snooze Action Buttons
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Big Wake Up & Dismiss button
                    Button(
                        onClick = { TrainTrackerState.dismissAlarm() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RailGreenOnTime
                        )
                    ) {
                        Text(
                            "✅ WAKE UP & DISMISS",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp,
                            color = Color.White
                        )
                    }

                    // Snooze button
                    OutlinedButton(
                        onClick = { TrainTrackerState.snoozeAlarm(5) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f))
                    ) {
                        Text(
                            "⏰ Snooze 5 Minutes",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary
                        )
                    }
                }
            }
        }
    }
}
