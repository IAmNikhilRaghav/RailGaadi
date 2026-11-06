package com.railgaadi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.theme.DarkBackground

@Composable
fun MetricBox(
    title: String,
    value: String,
    icon: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground
    val bgColor = if (isDark) Color(0xFF334155).copy(alpha = 0.6f) else Color(0xFFF1F5F9)
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val labelColor = if (isDark) Color(0xFF94A3B8) else Color.Gray

    Box(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$icon $value",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = textColor
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = labelColor
            )
        }
    }
}
