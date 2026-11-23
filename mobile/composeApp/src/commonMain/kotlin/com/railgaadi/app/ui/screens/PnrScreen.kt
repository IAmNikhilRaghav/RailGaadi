package com.railgaadi.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.network.PnrData
import com.railgaadi.app.network.PnrPassenger
import com.railgaadi.app.network.RailRadarService
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.*
import com.railgaadi.app.util.PlatformDate
import com.railgaadi.app.util.PlatformShare
import com.railgaadi.app.util.clearFocusOnTap
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PnrScreen() {
    val isDark = TrainTrackerState.isDarkMode
    val background = if (isDark) DarkBackground else LightBackground
    val surface = if (isDark) DarkSurface else LightSurface
    val textPrimary = if (isDark) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val borderColor = if (isDark) DarkBorder else LightBorder

    // ── State ──────────────────────────────────────────────────────────────────
    var selectedSection by remember { mutableStateOf("pnr") }
    var pnrInput by remember { mutableStateOf("") }
    var pnrResult by remember { mutableStateOf<PnrData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var showCopiedSnack by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val service = remember { RailRadarService() }
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val recentPnrs = TrainTrackerState.recentPnrs

    // ── PNR lookup ─────────────────────────────────────────────────────────────
    fun checkPnr(pnr: String = pnrInput) {
        val trimmed = pnr.trim()
        if (trimmed.length != 10) { errorMsg = I18n.t("pnr_invalid"); return }
        errorMsg = null
        hasSearched = false
        isLoading = true
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        coroutineScope.launch {
            val result = service.getPnrStatus(trimmed)
            isLoading = false
            hasSearched = true
            if (result.success == true && result.data != null) {
                pnrResult = result.data
                errorMsg = null
                // ── Save to history ──
                val d = result.data
                TrainTrackerState.addPnrHistory(
                    TrainTrackerState.PnrHistoryEntry(
                        pnrNumber    = trimmed,
                        trainName    = d.trainName ?: "",
                        trainNumber  = d.trainNumber ?: "",
                        source       = d.sourceStation ?: "",
                        destination  = d.destinationStation ?: "",
                        journeyDate  = d.dateOfJourney ?: "",
                        lastCheckedAt = PlatformDate.nowMillis()
                    )
                )
            } else {
                pnrResult = null
                errorMsg = result.message ?: I18n.t("pnr_not_found")
            }
        }
    }

    // ── Build shareable text ───────────────────────────────────────────────────
    fun buildShareText(d: PnrData): String {
        val sb = StringBuilder()
        sb.appendLine("🎟️ ${I18n.t("pnr_share_title")}")
        sb.appendLine("PNR: ${d.pnrNumber ?: pnrInput}")
        sb.appendLine("🚆 ${d.trainNumber} · ${d.trainName}")
        sb.appendLine("🗓 ${d.dateOfJourney}")
        sb.appendLine("📍 ${d.sourceStation} → ${d.destinationStation}")
        sb.appendLine("🎫 Class: ${d.travelClass} | Boarding: ${d.boardingStation}")
        sb.appendLine("Chart: ${d.chartStatus}")
        sb.appendLine()
        d.passengers?.forEachIndexed { idx, p ->
            sb.appendLine("Passenger ${idx + 1}: ${p.currentStatus} | Coach ${p.coach} Berth ${p.berth}")
        }
        sb.appendLine()
        sb.appendLine("Checked via RailGaadi 🚂")
        return sb.toString()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clearFocusOnTap(focusManager, keyboard)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(background),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

        // ── Header ─────────────────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
                Text(
                    text = I18n.t("pnr_title"),
                    fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                    color = textPrimary, letterSpacing = (-0.5).sp
                )
                Spacer(Modifier.height(4.dp))
                Text(text = I18n.t("pnr_subtitle"), fontSize = 14.sp, color = textSecondary)
            }
        }

        // ── Segmented Nav ──────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth().padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(surface).border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PnrNavPill("🎟️", "PNR Status", selectedSection == "pnr", isDark,
                    { selectedSection = "pnr" }, Modifier.weight(1f))
                PnrNavPill("🎫", "Tickets", selectedSection == "tickets", isDark,
                    { selectedSection = "tickets" }, Modifier.weight(1f))
                PnrNavPill("🚃", "Bookings", selectedSection == "bookings", isDark,
                    { selectedSection = "bookings" }, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
        }

        // ══════════════════════════════════════════════════════════════════════
        // PNR STATUS SECTION
        // ══════════════════════════════════════════════════════════════════════
        if (selectedSection == "pnr") {

            // ── Input Card ────────────────────────────────────────────────────
            item {
                PnrGlassCard(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clearFocusOnTap(focusManager, keyboard),
                    surface,
                    borderColor
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "🎟️  ${I18n.t("pnr_input_label")}",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = pnrInput,
                            onValueChange = { v ->
                                if (v.length <= 10 && v.all { it.isDigit() }) {
                                    pnrInput = v; errorMsg = null
                                }
                            },
                            placeholder = {
                                Text("e.g. 2341567890", color = textSecondary.copy(alpha = 0.5f), fontSize = 16.sp)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { checkPnr() }),
                            isError = errorMsg != null,
                            supportingText = errorMsg?.let { msg -> { Text(msg, color = Color(0xFFEF4444), fontSize = 12.sp) } },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RailBluePrimary, unfocusedBorderColor = borderColor,
                                focusedTextColor = textPrimary, unfocusedTextColor = textPrimary, cursorColor = RailBluePrimary
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 3.sp, color = textPrimary, fontFamily = FontFamily.Monospace
                            ),
                            trailingIcon = if (pnrInput.isNotEmpty()) {
                                { IconButton(onClick = { pnrInput = ""; errorMsg = null; pnrResult = null; hasSearched = false }) {
                                    Text("✕", fontSize = 14.sp, color = textSecondary)
                                }}
                            } else null
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { checkPnr() }, enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RailBluePrimary, disabledContainerColor = RailBluePrimary.copy(0.5f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                if (isLoading) I18n.t("pnr_loading") else I18n.t("pnr_check_btn"),
                                fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Result Card ───────────────────────────────────────────────────
            if (hasSearched && pnrResult != null) {
                val d = pnrResult!!
                item {
                    PnrResultCard(
                        data = d,
                        isDark = isDark,
                        surface = surface,
                        borderColor = borderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        pnrInput = pnrInput,
                        onShare = {
                            // Platform clipboard via share text
                            val text = buildShareText(d)
                            PlatformShare.shareText(text)
                        },
                        onCopyPnr = {
                            PlatformShare.copyToClipboard(d.pnrNumber ?: pnrInput)
                            showCopiedSnack = true
                            coroutineScope.launch {
                                kotlinx.coroutines.delay(2000)
                                showCopiedSnack = false
                            }
                        },
                        showCopiedSnack = showCopiedSnack
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Not Found ─────────────────────────────────────────────────────
            if (hasSearched && pnrResult == null && !isLoading) {
                item {
                    PnrGlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), surface, borderColor) {
                        Column(
                            Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎫", fontSize = 48.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(I18n.t("pnr_not_found"), fontWeight = FontWeight.Bold, fontSize = 18.sp,
                                color = textPrimary, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(6.dp))
                            Text(errorMsg ?: I18n.t("pnr_not_found_sub"),
                                fontSize = 13.sp, color = textSecondary, textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }

            // ── Idle hint ─────────────────────────────────────────────────────
            if (!hasSearched && !isLoading) {
                item {
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🚂", fontSize = 52.sp)
                        Spacer(Modifier.height(10.dp))
                        Text("Enter your 10-digit PNR number\nto check ticket & seat status",
                            fontSize = 14.sp, color = textSecondary,
                            textAlign = TextAlign.Center, lineHeight = 20.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("PNR is printed on your ticket or booking confirmation",
                            fontSize = 12.sp, color = textSecondary.copy(0.6f),
                            textAlign = TextAlign.Center, lineHeight = 18.sp)
                    }
                }
            }

            // ── PNR History ───────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🕐  ${I18n.t("pnr_history_title")}",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary
                    )
                    if (recentPnrs.isNotEmpty()) {
                        Text(
                            "Clear",
                            fontSize = 12.sp, color = Color(0xFFEF4444),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { TrainTrackerState.clearPnrHistory() }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (recentPnrs.isEmpty()) {
                item {
                    PnrGlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), surface, borderColor) {
                        Column(
                            Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📋", fontSize = 32.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(I18n.t("pnr_history_empty"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                color = textPrimary, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(4.dp))
                            Text(I18n.t("pnr_history_empty_sub"), fontSize = 12.sp, color = textSecondary,
                                textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            } else {
                items(recentPnrs) { entry ->
                    PnrHistoryRow(
                        entry = entry,
                        isDark = isDark,
                        surface = surface,
                        borderColor = borderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onTap = {
                            pnrInput = entry.pnrNumber
                            checkPnr(entry.pnrNumber)
                        },
                        onDelete = { TrainTrackerState.removePnrHistory(entry.pnrNumber) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // ══════════════════════════════════════════════════════════════════════
        // TICKETS / BOOKINGS — COMING SOON
        // ══════════════════════════════════════════════════════════════════════
        if (selectedSection == "tickets" || selectedSection == "bookings") {
            val sectionLabel = if (selectedSection == "tickets") "Ticket Booking" else "Train Bookings"
            val sectionIcon  = if (selectedSection == "tickets") "🎫" else "🚃"
            val sectionDesc  = if (selectedSection == "tickets")
                "Reserve & book train tickets directly from RailGaadi.\nCurrently in development."
            else
                "Manage and view your train bookings in one place.\nCurrently in development."

            item {
                PnrGlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), surface, borderColor) {
                    Column(
                        Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(sectionIcon, fontSize = 52.sp)
                        Spacer(Modifier.height(14.dp))
                        Text(sectionLabel, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier.background(Color(0xFFF59E0B).copy(0.15f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Coming Soon", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFF59E0B))
                        }
                        Spacer(Modifier.height(14.dp))
                        Text(sectionDesc, fontSize = 14.sp, color = textSecondary,
                            textAlign = TextAlign.Center, lineHeight = 20.sp)
                        Spacer(Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = {},
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, RailBluePrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("🔔  Notify Me", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = RailBluePrimary)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
}

// ─── Glassmorphic Card ─────────────────────────────────────────────────────────
@Composable
private fun PnrGlassCard(
    modifier: Modifier = Modifier,
    surface: Color,
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp))
            .background(surface).border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        content = content
    )
}

// ─── Segmented Nav Pill ────────────────────────────────────────────────────────
@Composable
private fun PnrNavPill(
    icon: String, label: String, isSelected: Boolean,
    isDark: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val bg = if (isSelected) RailBluePrimary else Color.Transparent
    val fg = if (isSelected) Color.White else (if (isDark) DarkTextSecondary else LightTextSecondary)
    Box(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(bg)
            .clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = fg, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

// ─── Status Badge ──────────────────────────────────────────────────────────────
@Composable
private fun StatusBadge(status: String?) {
    if (status.isNullOrBlank()) return
    val upper = status.uppercase()
    val (bg, fg, label) = when {
        upper.contains("CONFIRM") -> Triple(Color(0xFF16A34A).copy(0.15f), Color(0xFF16A34A), "✅  CONFIRMED")
        upper.contains("RAC")     -> Triple(Color(0xFFF59E0B).copy(0.15f), Color(0xFFF59E0B), "⚠️  RAC")
        upper.contains("WL") || upper.contains("WAIT") ->
            Triple(Color(0xFFEF4444).copy(0.15f), Color(0xFFEF4444), "🔴  WAITLIST")
        else -> Triple(Color(0xFF64748B).copy(0.12f), Color(0xFF64748B), status)
    }
    Box(Modifier.background(bg, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = fg)
    }
}

// ─── WL Refund Prediction Card ─────────────────────────────────────────────────
@Composable
private fun WlRefundPredictionCard(
    passengers: List<PnrPassenger>,
    isDark: Boolean,
    surface: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    val wlPassengers = passengers.filter {
        val s = (it.currentStatus ?: it.bookingStatus ?: "").uppercase()
        s.contains("WL") || s.contains("WAIT")
    }
    if (wlPassengers.isEmpty()) return

    // Extract WL number from status like "WL/45" or "GNWL/12/8"
    fun extractWlNum(status: String?): Int {
        if (status.isNullOrBlank()) return 99
        val digits = Regex("\\d+").findAll(status).toList()
        return digits.lastOrNull()?.value?.toIntOrNull() ?: 99
    }

    val minWl = wlPassengers.minOf { extractWlNum(it.currentStatus ?: it.bookingStatus) }

    val (confidence, label, color, barFill) = when {
        minWl <= 10 -> listOf("HIGH",   I18n.t("pnr_wl_refund_high"),   Color(0xFF16A34A), 0.85f)
        minWl <= 30 -> listOf("MEDIUM", I18n.t("pnr_wl_refund_medium"), Color(0xFFF59E0B), 0.50f)
        else        -> listOf("LOW",    I18n.t("pnr_wl_refund_low"),    Color(0xFFEF4444), 0.20f)
    }
    val fillColor = color as Color
    val fill = barFill as Float

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(fillColor.copy(alpha = 0.08f))
            .border(1.dp, fillColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📊  ${I18n.t("pnr_wl_refund_title")}",
                fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textPrimary)
            Box(
                Modifier.background(fillColor.copy(0.15f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(confidence as String, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = fillColor)
            }
        }
        Spacer(Modifier.height(8.dp))

        // Progress bar
        Box(
            Modifier.fillMaxWidth().height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (isDark) Color.White.copy(0.1f) else Color.Black.copy(0.08f))
        ) {
            Box(
                Modifier.fillMaxWidth(fill).fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(fillColor.copy(0.7f), fillColor)))
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(label as String, fontSize = 12.sp, color = fillColor, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Current WL: $minWl  •  ${I18n.t("pnr_refund_note")}",
            fontSize = 11.sp, color = textSecondary, lineHeight = 15.sp
        )
    }
}

// ─── PNR Result Card ───────────────────────────────────────────────────────────
@Composable
private fun PnrResultCard(
    data: PnrData,
    isDark: Boolean,
    surface: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    pnrInput: String,
    onShare: () -> Unit,
    onCopyPnr: () -> Unit,
    showCopiedSnack: Boolean
) {
    PnrGlassCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp), surface, borderColor) {
        Column(Modifier.padding(20.dp)) {

            // ── Train banner ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier.background(RailBluePrimary, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(data.trainNumber ?: "—", fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold, color = Color.White,
                                fontFamily = FontFamily.Monospace)
                        }
                        Text(data.trainName ?: "", fontWeight = FontWeight.Bold,
                            fontSize = 15.sp, color = textPrimary, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("${data.sourceStation ?: "—"} → ${data.destinationStation ?: "—"}",
                        fontSize = 13.sp, color = textSecondary)
                    if (!data.dateOfJourney.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text("🗓  ${data.dateOfJourney}", fontSize = 12.sp, color = textSecondary.copy(0.8f))
                    }
                }
                // Chart status
                val chartPrepared = data.chartStatus?.contains("PREPARED", ignoreCase = true) == true
                        && data.chartStatus?.contains("NOT", ignoreCase = true) == false
                Box(
                    Modifier.background(
                        if (chartPrepared) Color(0xFF16A34A).copy(0.12f) else Color(0xFFF59E0B).copy(0.12f),
                        RoundedCornerShape(8.dp)
                    ).padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        if (chartPrepared) "✅ ${I18n.t("pnr_chart_prepared")}" else "⏳ ${I18n.t("pnr_chart_not_prepared")}",
                        fontSize = 11.sp, fontWeight = FontWeight.Bold,
                        color = if (chartPrepared) Color(0xFF16A34A) else Color(0xFFF59E0B)
                    )
                }
            }

            // ── Info chips row ────────────────────────────────────────────────
            if (!data.travelClass.isNullOrBlank() || !data.boardingStation.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (!data.travelClass.isNullOrBlank()) {
                        InfoChip(I18n.t("pnr_class"), data.travelClass, textSecondary, textPrimary)
                    }
                    if (!data.boardingStation.isNullOrBlank()) {
                        InfoChip("Boarding", data.boardingStation, textSecondary, textPrimary)
                    }
                    if (!data.reservedUpto.isNullOrBlank() && data.reservedUpto != data.destinationStation) {
                        InfoChip("Reserved Upto", data.reservedUpto, textSecondary, textPrimary)
                    }
                }
            }

            // ── Action buttons: Share + Copy PNR ─────────────────────────────
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                // Share button
                OutlinedButton(
                    onClick = onShare,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, RailBluePrimary),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text("↑  ${I18n.t("pnr_share_btn")}", fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold, color = RailBluePrimary, maxLines = 1)
                }
                // Copy PNR button
                Button(
                    onClick = onCopyPnr,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (showCopiedSnack) Color(0xFF16A34A) else RailBluePrimary.copy(0.12f)
                    ),
                    modifier = Modifier.weight(1f).height(40.dp)
                ) {
                    Text(
                        if (showCopiedSnack) "✅ ${I18n.t("pnr_copied")}" else "📋 ${I18n.t("pnr_copy_btn")}",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = if (showCopiedSnack) Color.White else RailBluePrimary, maxLines = 1
                    )
                }
            }

            HorizontalDivider(color = borderColor, modifier = Modifier.padding(vertical = 14.dp))

            // ── Passengers ────────────────────────────────────────────────────
            Text("👥  Passengers", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textPrimary)
            Spacer(Modifier.height(10.dp))

            val passengers = data.passengers ?: emptyList()
            if (passengers.isEmpty()) {
                Text("No passenger details available.", fontSize = 13.sp, color = textSecondary)
            } else {
                // Header
                Row(
                    Modifier.fillMaxWidth()
                        .background(
                            if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.03f),
                            RoundedCornerShape(8.dp)
                        ).padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TableHeader("#", Modifier.weight(0.5f), textSecondary)
                    TableHeader(I18n.t("pnr_booking_status"), Modifier.weight(2f), textSecondary)
                    TableHeader("Current", Modifier.weight(2f), textSecondary)
                    TableHeader(I18n.t("pnr_coach"), Modifier.weight(1f), textSecondary)
                    TableHeader(I18n.t("pnr_berth"), Modifier.weight(1f), textSecondary)
                }
                Spacer(Modifier.height(4.dp))
                passengers.forEach { pax -> PassengerRow(pax, isDark, textPrimary, textSecondary) }

                // ── WL Refund Prediction (shown if any passenger is WL) ───────
                val hasWl = passengers.any {
                    val s = (it.currentStatus ?: it.bookingStatus ?: "").uppercase()
                    s.contains("WL") || s.contains("WAIT")
                }
                if (hasWl) {
                    Spacer(Modifier.height(16.dp))
                    WlRefundPredictionCard(passengers, isDark, surface, borderColor, textPrimary, textSecondary)
                }
            }

            HorizontalDivider(color = borderColor, modifier = Modifier.padding(vertical = 14.dp))
            Text(
                "ℹ️  Data sourced from Indian Railways via RailGaadi API",
                fontSize = 11.sp, color = textSecondary.copy(0.6f),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ─── PNR History Row ───────────────────────────────────────────────────────────
@Composable
private fun PnrHistoryRow(
    entry: TrainTrackerState.PnrHistoryEntry,
    isDark: Boolean,
    surface: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(surface).border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onTap).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // PNR chip
        Box(
            Modifier.background(RailBluePrimary.copy(0.12f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(entry.pnrNumber, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                color = RailBluePrimary, fontFamily = FontFamily.Monospace)
        }
        Column(Modifier.weight(1f)) {
            Text(
                "${entry.trainNumber.ifBlank { "—" }} · ${entry.trainName.ifBlank { "Unknown Train" }}",
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = textPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            if (entry.source.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text("${entry.source} → ${entry.destination}  ${entry.journeyDate}",
                    fontSize = 11.sp, color = textSecondary, maxLines = 1)
            }
        }
        // Re-check arrow
        Box(
            Modifier.size(32.dp).clip(CircleShape)
                .background(RailBluePrimary.copy(0.10f)),
            contentAlignment = Alignment.Center
        ) { Text("↻", fontSize = 16.sp, color = RailBluePrimary) }
        // Delete
        Box(
            Modifier.size(32.dp).clip(CircleShape)
                .background(Color(0xFFEF4444).copy(0.10f))
                .clickable(onClick = onDelete),
            contentAlignment = Alignment.Center
        ) { Text("✕", fontSize = 12.sp, color = Color(0xFFEF4444)) }
    }
}

// ─── Helpers ───────────────────────────────────────────────────────────────────
@Composable
private fun InfoChip(label: String, value: String, textSecondary: Color, textPrimary: Color) {
    Column {
        Text(label, fontSize = 10.sp, color = textSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier, color: Color) {
    Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color,
        modifier = modifier, maxLines = 1)
}

@Composable
private fun PassengerRow(
    pax: PnrPassenger, isDark: Boolean, textPrimary: Color, textSecondary: Color
) {
    val curr = pax.currentStatus?.uppercase() ?: ""
    val statusColor = when {
        curr.contains("CONFIRM") -> Color(0xFF16A34A)
        curr.contains("RAC")     -> Color(0xFFF59E0B)
        curr.contains("WL") || curr.contains("WAIT") -> Color(0xFFEF4444)
        else -> textSecondary
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${pax.number ?: "—"}", fontSize = 13.sp, color = textPrimary, modifier = Modifier.weight(0.5f))
        Text(pax.bookingStatus ?: "—", fontSize = 12.sp, color = textSecondary, modifier = Modifier.weight(2f), maxLines = 1)
        Text(pax.currentStatus ?: "—", fontSize = 12.sp, fontWeight = FontWeight.Bold,
            color = statusColor, modifier = Modifier.weight(2f), maxLines = 1)
        Text(pax.coach ?: "—", fontSize = 12.sp, color = textPrimary, modifier = Modifier.weight(1f), maxLines = 1)
        Text(pax.berth ?: "—", fontSize = 12.sp, color = textPrimary, modifier = Modifier.weight(1f), maxLines = 1)
    }
    HorizontalDivider(color = if (isDark) Color.White.copy(0.05f) else Color.Black.copy(0.04f))
}
