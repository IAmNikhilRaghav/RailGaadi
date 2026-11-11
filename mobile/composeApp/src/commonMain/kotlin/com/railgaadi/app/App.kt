package com.railgaadi.app

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.model.TabItem
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.*
import com.railgaadi.app.ui.components.PreferencesDrawer
import com.railgaadi.app.ui.screens.*

// ─── Shared scroll-collapse state exposed to child screens ───────────────────
object BarsCollapseState {
    var barsVisible by mutableStateOf(true)
}

/** iOS-style animation spec — snappy spring-like ease */
private val barAnimSpec: AnimationSpec<Dp> = tween(durationMillis = 260, easing = FastOutSlowInEasing)
private val alphaAnimSpec: AnimationSpec<Float> = tween(durationMillis = 220, easing = FastOutSlowInEasing)

// ─── Constants ────────────────────────────────────────────────────────────────
// These are the CONTENT heights of the bars (excluding system insets).
// The bars themselves grow to cover system insets too, but content is padded by these values.
private val TOP_BAR_CONTENT_HEIGHT = 54.dp
private val BOTTOM_BAR_CONTENT_HEIGHT = 62.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val isDarkMode = TrainTrackerState.isDarkMode
    var selectedTab by remember { mutableStateOf(TabItem.LIVE_MAP) }
    var showLanguageModal by remember { mutableStateOf(false) }
    var showPreferencesDrawer by remember { mutableStateOf(false) }

    // Reset bars to visible whenever the user switches tabs
    LaunchedEffect(selectedTab) {
        BarsCollapseState.barsVisible = true
    }

    // ── Scroll-driven collapse ─────────────────────────────────────────────
    val barsVisible = BarsCollapseState.barsVisible

    // NestedScrollConnection: intercepts scroll events from any child LazyColumn/Scrollable
    val scrollConnection = remember {
        object : NestedScrollConnection {
            private var scrollAccum = 0f
            private val THRESHOLD = 24f   // px before toggling — avoids flicker on tiny scrolls

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                scrollAccum += available.y
                when {
                    scrollAccum < -THRESHOLD -> { // scrolling DOWN → hide bars
                        scrollAccum = 0f
                        if (BarsCollapseState.barsVisible) BarsCollapseState.barsVisible = false
                    }
                    scrollAccum > THRESHOLD -> {  // scrolling UP → show bars
                        scrollAccum = 0f
                        if (!BarsCollapseState.barsVisible) BarsCollapseState.barsVisible = true
                    }
                }
                return Offset.Zero  // don't consume — pass through to child
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                scrollAccum = 0f
                // Always reveal bars when scrolling comes to rest at the top
                if (available.y > 100f) BarsCollapseState.barsVisible = true
                return super.onPostFling(consumed, available)
            }
        }
    }

    val topBarContentHeight by animateDpAsState(
        targetValue = if (barsVisible) TOP_BAR_CONTENT_HEIGHT else 0.dp,
        animationSpec = barAnimSpec,
        label = "topBarHeight"
    )
    val bottomBarContentHeight by animateDpAsState(
        targetValue = if (barsVisible) BOTTOM_BAR_CONTENT_HEIGHT else 0.dp,
        animationSpec = barAnimSpec,
        label = "bottomBarHeight"
    )
    val barsAlpha by animateFloatAsState(
        targetValue = if (barsVisible) 1f else 0f,
        animationSpec = alphaAnimSpec,
        label = "barsAlpha"
    )

    val surfaceGlass = if (isDarkMode)
        Color(0xFF0D0D0F).copy(alpha = 0.88f)
    else
        Color(0xFFFAFAFC).copy(alpha = 0.92f)

    val borderColor = if (isDarkMode)
        Color.White.copy(alpha = 0.08f)
    else
        Color.Black.copy(alpha = 0.07f)

    RailGaadiTheme(darkTheme = isDarkMode) {
        Box(modifier = Modifier.fillMaxSize()) {

            // ── CONTENT — always covers full screen, padded so bars don't overlap ──
            // statusBarsPadding + navigationBarsPadding always applied to prevent
            // system UI overlap. topBarContentHeight/bottomBarContentHeight add
            // EXTRA space for the floating glass bars when they're visible.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(top = topBarContentHeight, bottom = bottomBarContentHeight)
                    .nestedScroll(scrollConnection)
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
            }

            // ── TOP BAR — glass overlay, includes status bar inset ─────────────
            // Uses WindowInsetsSides.Top so it covers exactly: statusBar + content height
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.TopCenter)
                    .alpha(barsAlpha)
                    .drawBehind {
                        drawRect(color = surfaceGlass)
                        // Bottom separator line
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 0.8f
                        )
                    }
            ) {
                // Status bar inset + content row
                Column {
                    // Fill the status bar area with glass (transparent spacer with inset padding)
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsTopHeight(WindowInsets.statusBars)
                    )
                    // Actual header row content
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(TOP_BAR_CONTENT_HEIGHT)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Logo + Name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(RailBluePrimary, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                MinimalistTrainLogo()
                            }
                            Text(
                                "RailGaadi",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                letterSpacing = (-0.5).sp
                            )
                        }

                        // Action pills
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            // Language pill
                            Box(
                                modifier = Modifier
                                    .height(30.dp)
                                    .background(RailBluePrimary.copy(alpha = 0.11f), RoundedCornerShape(15.dp))
                                    .border(0.8.dp, RailBluePrimary.copy(alpha = 0.25f), RoundedCornerShape(15.dp))
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

                            // Theme toggle
                            GlassCircleButton(
                                isDarkMode = isDarkMode,
                                borderColor = borderColor,
                                onClick = { TrainTrackerState.isDarkMode = !TrainTrackerState.isDarkMode }
                            ) {
                                Text(if (isDarkMode) "☀️" else "🌙", fontSize = 15.sp)
                            }

                            // Menu
                            GlassCircleButton(isDarkMode = isDarkMode, borderColor = borderColor, onClick = { showPreferencesDrawer = true }) {
                                Text("☰", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color(0xFF0F172A))
                            }
                        }
                    }
                }
            }

            // ── BOTTOM TAB BAR — glass overlay, includes nav bar inset ────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .align(Alignment.BottomCenter)
                    .alpha(barsAlpha)
                    .drawBehind {
                        drawRect(color = surfaceGlass)
                        // Top separator line
                        drawLine(
                            color = borderColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 0.8f
                        )
                    }
            ) {
                Column {
                    // Tab items row — fixed 62dp, no system insets in here
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BOTTOM_BAR_CONTENT_HEIGHT)
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabItem.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            val tabAlpha by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0.50f,
                                animationSpec = tween(200),
                                label = "tabAlpha"
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                                    ) { selectedTab = tab },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                // Active indicator bar above icon
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height(3.dp)
                                        .background(
                                            color = if (isSelected) RailBluePrimary else Color.Transparent,
                                            shape = RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp)
                                        )
                                )

                                Spacer(modifier = Modifier.height(5.dp))

                                // Icon
                                Text(
                                    text = tab.icon,
                                    fontSize = 21.sp,
                                    modifier = Modifier.alpha(tabAlpha)
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Label — always visible, never clipped
                                Text(
                                    text = I18n.t(tab.i18nKey),
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) RailBluePrimary else (if (isDarkMode) Color(0xFF8E8E93) else Color(0xFF6B7280)),
                                    letterSpacing = 0.sp,
                                    maxLines = 1,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Navigation bar inset space (home indicator on iPhone)
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                    )
                }
            }

            // ── OVERLAYS ──────────────────────────────────────────────────────
            // Right-Side Preferences Drawer
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

/** Reusable frosted-glass circular icon button */
@Composable
fun GlassCircleButton(
    isDarkMode: Boolean,
    borderColor: Color,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val bg = if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f)
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(bg, CircleShape)
            .border(0.8.dp, borderColor, CircleShape)
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
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
