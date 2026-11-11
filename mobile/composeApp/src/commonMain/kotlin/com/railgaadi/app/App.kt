package com.railgaadi.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.model.TabItem
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.*
import com.railgaadi.app.ui.components.PreferencesDrawer
import com.railgaadi.app.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val isDarkMode = TrainTrackerState.isDarkMode
    var selectedTab by remember { mutableStateOf(TabItem.LIVE_MAP) }
    var showLanguageModal by remember { mutableStateOf(false) }
    var showPreferencesDrawer by remember { mutableStateOf(false) }

    RailGaadiTheme(darkTheme = isDarkMode) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Compact Minimalist Train Logo
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(RailBluePrimary, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                MinimalistTrainLogo()
                            }

                            Text(
                                "RailGaadi",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = (-0.3).sp
                            )
                        }
                    },
                    actions = {
                        // 1. Language Pill Button (e.g. 🌐 EN) - compact iOS pill
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .background(
                                    RailBluePrimary.copy(alpha = 0.10f),
                                    RoundedCornerShape(16.dp)
                                )
                                .border(1.dp, RailBluePrimary.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
                                .clickable { showLanguageModal = true }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🌐 ${I18n.currentPill}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = RailBluePrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // 2. Circular Theme Toggle Button (🌙 / ☀️) - centered iOS frosted circle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f), CircleShape)
                                .clickable { TrainTrackerState.isDarkMode = !TrainTrackerState.isDarkMode },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(if (isDarkMode) "☀️" else "🌙", fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // 3. Circular Hamburger Menu Button (☰) - centered iOS frosted circle
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.14f), CircleShape)
                                .clickable { showPreferencesDrawer = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("☰", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Spacer(modifier = Modifier.width(6.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.height(58.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    TabItem.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Text(tab.icon, fontSize = 16.sp) },
                            label = {
                                Text(
                                    I18n.t(tab.i18nKey),
                                    fontSize = 10.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RailBluePrimary,
                                selectedTextColor = RailBluePrimary,
                                indicatorColor = RailBluePrimary.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    TabItem.LIVE_MAP -> LiveMapScreen()
                    TabItem.ROUTES -> RoutesScreen(onTrackTrain = { train, boardingStationText ->
                        TrainTrackerState.selectTrain(train, boardingStationText)
                        selectedTab = TabItem.LIVE_MAP
                    })
                    TabItem.ANALYTICS -> AnalyticsScreen(onViewLiveMap = {
                        selectedTab = TabItem.LIVE_MAP
                    })
                    TabItem.WEATHER -> WeatherScreen()
                    TabItem.STATION -> StationBoardScreen(onTrackTrain = { train ->
                        TrainTrackerState.selectTrain(train)
                        selectedTab = TabItem.LIVE_MAP
                    })
                }

                // Right-Side Preferences Drawer (Opens smoothly from right edge)
                PreferencesDrawer(
                    isOpen = showPreferencesDrawer,
                    isDarkMode = isDarkMode,
                    onDarkModeChanged = { TrainTrackerState.isDarkMode = it },
                    onOpenLanguageModal = {
                        showPreferencesDrawer = false
                        showLanguageModal = true
                    },
                    onDismiss = { showPreferencesDrawer = false }
                )

                // Language Selector BottomSheet
                if (showLanguageModal) {
                    ModalBottomSheet(
                        onDismissRequest = { showLanguageModal = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = I18n.t("select_language"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)
                            ) {
                                items(I18n.languages) { lang ->
                                    val isSelected = I18n.currentLanguage == lang.code
                                    Card(
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) RailBluePrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(RailBluePrimary)) else null,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                I18n.currentLanguage = lang.code
                                                showLanguageModal = false
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(lang.native, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text(lang.label, fontSize = 11.sp, color = Color.Gray)
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(Color.White.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(lang.pill, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = RailBluePrimary)
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MinimalistTrainLogo() {
    Canvas(modifier = Modifier.size(22.dp)) {
        val color = Color.White
        // Train Body
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.16f, size.height * 0.08f),
            size = Size(size.width * 0.68f, size.height * 0.74f),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(width = 2.2f)
        )
        // Windshield
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.26f, size.height * 0.18f),
            size = Size(size.width * 0.48f, size.height * 0.28f),
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 1.8f)
        )
        // Two Headlights
        drawCircle(color = color, radius = 2.2f, center = Offset(size.width * 0.34f, size.height * 0.66f))
        drawCircle(color = color, radius = 2.2f, center = Offset(size.width * 0.66f, size.height * 0.66f))
        // Rails
        drawLine(
            color,
            Offset(size.width * 0.1f, size.height * 0.94f),
            Offset(size.width * 0.9f, size.height * 0.94f),
            strokeWidth = 2f,
            cap = StrokeCap.Round
        )
    }
}
