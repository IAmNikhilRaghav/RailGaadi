package com.railgaadi.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.state.TrainTrackerState

private val LiveGreen = Color(0xFF10B981)
private val GpsBlue = Color(0xFF0284C7)
private val GpsAmber = Color(0xFFD97706)

@Composable
fun LiveAutoRefreshPill(
    countdown: Int = 0,
    isRefreshing: Boolean = false,
    onForceRefresh: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val isOfflineGps = TrainTrackerState.isOfflineModeEnabled || TrainTrackerState.isGpsTrackingActive
    val gpsSpeed = TrainTrackerState.currentGpsLocation?.speedKmh?.toInt() ?: 0

    // Elegant pulsing animation for the live status dot
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    val activeColor = if (isOfflineGps) GpsBlue else LiveGreen
    val labelText = if (isOfflineGps) {
        if (gpsSpeed > 5) "🛰️ GPS $gpsSpeed km/h" else "🛰️ GPS MODE"
    } else {
        "LIVE"
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(activeColor.copy(alpha = 0.12f))
            .border(0.8.dp, activeColor.copy(alpha = 0.28f), CircleShape)
            .clickable {
                if (isOfflineGps) {
                    TrainTrackerState.disableOfflineGpsMode()
                } else {
                    onForceRefresh()
                }
            }
            .padding(horizontal = 9.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.5.dp)
                    .clip(CircleShape)
                    .background(activeColor)
                    .alpha(dotAlpha)
            )
            Text(
                text = labelText,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = activeColor,
                letterSpacing = 0.6.sp
            )
        }
    }
}
