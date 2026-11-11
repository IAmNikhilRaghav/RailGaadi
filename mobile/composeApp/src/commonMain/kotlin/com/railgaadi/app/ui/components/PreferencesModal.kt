package com.railgaadi.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.RailBluePrimary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PreferencesDrawer(
    isOpen: Boolean,
    isDarkMode: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    onOpenLanguageModal: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 1. Semi-transparent Scrim Backdrop (Tap to close)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss
                )
        )

        // 2. Right-Side Sliding Drawer
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(150)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing)
            ) + fadeOut(animationSpec = tween(150)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .clickable(interactionSource = interactionSource, indication = null) {},
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    // Header: Preferences + Minimal Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Preferences",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.3).sp
                        )

                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "✕",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // 1. Dark Theme
                    CleanPreferenceTile(
                        icon = { VectorThemeIcon(isDarkMode) },
                        title = "Dark Theme",
                        subtitle = "Comfortable viewing at night",
                        trailing = {
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = onDarkModeChanged,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = RailBluePrimary
                                )
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Trip Delay Alerts
                    CleanPreferenceTile(
                        icon = { VectorBellIcon() },
                        title = "Trip Delay Alerts",
                        subtitle = "Live notifications on delay",
                        trailing = {
                            Switch(
                                checked = TrainTrackerState.tripDelayAlerts,
                                onCheckedChange = { TrainTrackerState.tripDelayAlerts = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = RailBluePrimary
                                )
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Select Language
                    CleanPreferenceTile(
                        icon = { VectorGlobeIcon() },
                        title = "Select Language / भाषा",
                        subtitle = "${I18n.languages.find { it.code == I18n.currentLanguage }?.native ?: "English"} (${I18n.languages.find { it.code == I18n.currentLanguage }?.label ?: "English"})",
                        onClick = onOpenLanguageModal,
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .background(RailBluePrimary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        I18n.currentPill,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = RailBluePrimary
                                    )
                                }
                                Text("›", fontSize = 18.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Saved Trains & Routes
                    CleanPreferenceTile(
                        icon = { VectorBookmarkIcon() },
                        title = "Saved Trains & Routes",
                        subtitle = "Quick access to pinned trains (${TrainTrackerState.savedTrains.size})",
                        onClick = {},
                        trailing = {
                            Text("›", fontSize = 18.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5. Share Live Status
                    CleanPreferenceTile(
                        icon = { VectorShareIcon() },
                        title = "Share Live Status",
                        subtitle = "Send journey status link",
                        onClick = {},
                        trailing = {
                            Text("›", fontSize = 18.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 6. MAP STYLE (Minimalist Cards, zero cartoonish icons)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                RoundedCornerShape(14.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "MAP STYLE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val styles = listOf(
                                "light" to "Light",
                                "dark" to "Dark",
                                "satellite" to "Satellite",
                                "terrain" to "Terrain"
                            )

                            styles.forEach { (id, label) ->
                                val isSelected = TrainTrackerState.currentMapStyle == id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) RailBluePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .border(
                                            width = if (isSelected) 1.8.dp else 1.dp,
                                            color = if (isSelected) RailBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { TrainTrackerState.currentMapStyle = id }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        VectorMapStyleIcon(id, isSelected)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = label,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) RailBluePrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }
        }
    }
}

@Composable
fun CleanPreferenceTile(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            trailing()
        }
    }
}

/* ─────────────────────────────────────────────────────────────
   Minimalist Vector Icons (Resolution-independent, 0 cartoonish emojis)
   ───────────────────────────────────────────────────────────── */

@Composable
fun VectorThemeIcon(isDark: Boolean) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val color = Color(0xFF2563EB)
        if (isDark) {
            // Minimalist Moon Crescent
            val path = Path().apply {
                moveTo(size.width * 0.7f, size.height * 0.15f)
                cubicTo(
                    size.width * 0.25f, size.height * 0.25f,
                    size.width * 0.25f, size.height * 0.75f,
                    size.width * 0.7f, size.height * 0.85f
                )
                cubicTo(
                    size.width * 0.45f, size.height * 0.75f,
                    size.width * 0.45f, size.height * 0.25f,
                    size.width * 0.7f, size.height * 0.15f
                )
                close()
            }
            drawPath(path, color = color)
        } else {
            // Minimalist Sun
            drawCircle(color = color, radius = size.width * 0.25f, center = center, style = Stroke(width = 1.8f))
            val rayLen = size.width * 0.12f
            for (i in 0 until 8) {
                val angle = i * PI / 4.0
                val startX = (center.x + cos(angle) * (size.width * 0.32f)).toFloat()
                val startY = (center.y + sin(angle) * (size.height * 0.32f)).toFloat()
                val endX = (center.x + cos(angle) * (size.width * 0.32f + rayLen)).toFloat()
                val endY = (center.y + sin(angle) * (size.height * 0.32f + rayLen)).toFloat()
                drawLine(color, Offset(startX, startY), Offset(endX, endY), strokeWidth = 1.6f, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
fun VectorBellIcon() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val color = Color(0xFF0F172A)
        val path = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.15f)
            lineTo(size.width * 0.75f, size.height * 0.65f)
            lineTo(size.width * 0.85f, size.height * 0.75f)
            lineTo(size.width * 0.15f, size.height * 0.75f)
            lineTo(size.width * 0.25f, size.height * 0.65f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 1.6f, cap = StrokeCap.Round))
        drawCircle(color = color, radius = 2f, center = Offset(size.width * 0.5f, size.height * 0.88f))
    }
}

@Composable
fun VectorGlobeIcon() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val color = Color(0xFF0F172A)
        drawCircle(color = color, radius = size.width * 0.42f, center = center, style = Stroke(width = 1.6f))
        drawLine(color, Offset(size.width * 0.08f, center.y), Offset(size.width * 0.92f, center.y), strokeWidth = 1.4f)
        drawOval(
            color = color,
            topLeft = Offset(size.width * 0.28f, size.height * 0.08f),
            size = Size(size.width * 0.44f, size.height * 0.84f),
            style = Stroke(width = 1.4f)
        )
    }
}

@Composable
fun VectorBookmarkIcon() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val color = Color(0xFF0F172A)
        val path = Path().apply {
            moveTo(size.width * 0.25f, size.height * 0.15f)
            lineTo(size.width * 0.75f, size.height * 0.15f)
            lineTo(size.width * 0.75f, size.height * 0.85f)
            lineTo(size.width * 0.5f, size.height * 0.65f)
            lineTo(size.width * 0.25f, size.height * 0.85f)
            close()
        }
        drawPath(path, color = color, style = Stroke(width = 1.6f, cap = StrokeCap.Round))
    }
}

@Composable
fun VectorShareIcon() {
    Canvas(modifier = Modifier.size(18.dp)) {
        val color = Color(0xFF0F172A)
        val r = 2.4f
        val p1 = Offset(size.width * 0.75f, size.height * 0.25f)
        val p2 = Offset(size.width * 0.25f, size.height * 0.5f)
        val p3 = Offset(size.width * 0.75f, size.height * 0.75f)

        drawLine(color, p2, p1, strokeWidth = 1.5f)
        drawLine(color, p2, p3, strokeWidth = 1.5f)
        drawCircle(color, r, p1)
        drawCircle(color, r, p2)
        drawCircle(color, r, p3)
    }
}

@Composable
fun VectorMapStyleIcon(style: String, isSelected: Boolean) {
    val activeColor = if (isSelected) RailBluePrimary else Color(0xFF475569)

    Canvas(modifier = Modifier.size(20.dp)) {
        when (style) {
            "light" -> {
                drawCircle(color = activeColor, radius = size.width * 0.28f, style = Stroke(width = 1.5f))
            }
            "dark" -> {
                val path = Path().apply {
                    moveTo(size.width * 0.65f, size.height * 0.2f)
                    cubicTo(size.width * 0.2f, size.height * 0.3f, size.width * 0.2f, size.height * 0.7f, size.width * 0.65f, size.height * 0.8f)
                    cubicTo(size.width * 0.4f, size.height * 0.7f, size.width * 0.4f, size.height * 0.3f, size.width * 0.65f, size.height * 0.2f)
                    close()
                }
                drawPath(path, color = activeColor)
            }
            "satellite" -> {
                drawCircle(color = activeColor, radius = size.width * 0.38f, style = Stroke(width = 1.5f))
                drawLine(activeColor, Offset(size.width * 0.12f, center.y), Offset(size.width * 0.88f, center.y), strokeWidth = 1.2f)
                drawLine(activeColor, Offset(center.x, size.height * 0.12f), Offset(center.x, size.height * 0.88f), strokeWidth = 1.2f)
            }
            "terrain" -> {
                val path = Path().apply {
                    moveTo(size.width * 0.1f, size.height * 0.8f)
                    lineTo(size.width * 0.45f, size.height * 0.25f)
                    lineTo(size.width * 0.65f, size.height * 0.55f)
                    lineTo(size.width * 0.78f, size.height * 0.38f)
                    lineTo(size.width * 0.95f, size.height * 0.8f)
                    close()
                }
                drawPath(path, color = activeColor, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
            }
        }
    }
}
