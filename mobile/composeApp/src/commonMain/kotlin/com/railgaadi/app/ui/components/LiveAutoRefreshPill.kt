package com.railgaadi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.theme.RailBluePrimary
import com.railgaadi.app.theme.RailGreenOnTime

@Composable
fun LiveAutoRefreshPill(
    countdown: Int,
    isRefreshing: Boolean,
    onForceRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = if (isRefreshing) RailBluePrimary else RailGreenOnTime
    val text = if (isRefreshing) "● REFRESHING…" else "● LIVE · ${countdown}s"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .background(activeColor.copy(alpha = 0.12f))
            .clickable { onForceRefresh() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(activeColor)
            )
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = activeColor
            )
        }
    }
}

