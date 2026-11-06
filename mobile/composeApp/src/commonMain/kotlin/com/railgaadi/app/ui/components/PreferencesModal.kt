package com.railgaadi.app.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.RailBluePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesModal(
    isDarkMode: Boolean,
    onDarkModeChanged: (Boolean) -> Unit,
    onOpenLanguageModal: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            // Header Row: Preferences + Close Button (✕)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Preferences",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Card 1: Dark Theme
            PreferenceActionCard(
                icon = "☀️",
                iconBg = Color(0xFFFEF3C7),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Card 2: Trip Delay Alerts
            PreferenceActionCard(
                icon = "🔔",
                iconBg = Color(0xFFF1F5F9),
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

            Spacer(modifier = Modifier.height(12.dp))

            // Card 3: Select Language / भाषा चुनें
            PreferenceActionCard(
                icon = "🌐",
                iconBg = Color(0xFFDCFCE7),
                title = "Select Language / भाषा चुनें",
                subtitle = "${I18n.languages.find { it.code == I18n.currentLanguage }?.native ?: "English"} (${I18n.languages.find { it.code == I18n.currentLanguage }?.label ?: "English"})",
                onClick = onOpenLanguageModal,
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .background(RailBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(I18n.currentPill, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RailBluePrimary)
                        }
                        Text("›", fontSize = 20.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Card 4: Saved Trains & Routes
            PreferenceActionCard(
                icon = "❤️",
                iconBg = Color(0xFFFFE4E6),
                title = "Saved Trains & Routes",
                subtitle = "Quick access to pinned trains (${TrainTrackerState.savedTrains.size})",
                onClick = {},
                trailing = {
                    Text("›", fontSize = 20.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Card 5: Share Live Status
            PreferenceActionCard(
                icon = "📤",
                iconBg = Color(0xFFF3E8FF),
                title = "Share Live Status",
                subtitle = "Send live train link via WhatsApp",
                onClick = {},
                trailing = {
                    Text("›", fontSize = 20.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card 6: 🗺️ MAP STYLE Selector
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("🗺️", fontSize = 15.sp)
                        Text(
                            text = "MAP STYLE",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val styles = listOf(
                            Triple("light", "Light", "☀️"),
                            Triple("dark", "Dark", "🌙"),
                            Triple("satellite", "Satellite", "🛰️"),
                            Triple("terrain", "Terrain", "🏔️")
                        )

                        styles.forEach { (id, label, icon) ->
                            val isSelected = TrainTrackerState.currentMapStyle == id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (isSelected) RailBluePrimary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) RailBluePrimary else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { TrainTrackerState.currentMapStyle = id }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(icon, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) RailBluePrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun PreferenceActionCard(
    icon: String,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
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
                        .size(40.dp)
                        .background(iconBg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(icon, fontSize = 18.sp)
                }

                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            trailing()
        }
    }
}
