package com.railgaadi.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.network.PnrData
import com.railgaadi.app.network.PnrPassenger
import com.railgaadi.app.network.RailRadarService
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PnrScreen() {
    val isDarkMode = TrainTrackerState.isDarkMode
    val background = if (isDarkMode) DarkBackground else LightBackground
    val surface = if (isDarkMode) DarkSurface else LightSurface
    val textPrimary = if (isDarkMode) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDarkMode) DarkTextSecondary else LightTextSecondary
    val borderColor = if (isDarkMode) DarkBorder else LightBorder

    var selectedSection by remember { mutableStateOf("pnr") }
    var pnrInput by remember { mutableStateOf("") }
    var pnrResult by remember { mutableStateOf<PnrData?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }
    var showComingSoonFor by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val service = remember { RailRadarService() }

    fun checkPnr() {
        if (pnrInput.trim().length != 10) {
            errorMsg = I18n.t("pnr_invalid")
            return
        }
        errorMsg = null
        hasSearched = false
        isLoading = true
        coroutineScope.launch {
            val result = service.getPnrStatus(pnrInput.trim())
            isLoading = false
            hasSearched = true
            if (result.success == true && result.data != null) {
                pnrResult = result.data
                errorMsg = null
            } else {
                pnrResult = null
                errorMsg = result.message ?: I18n.t("pnr_not_found")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // ── Screen Header ────────────────────────────────────────────────────
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text(
                    text = I18n.t("pnr_title"),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = I18n.t("pnr_subtitle"),
                    fontSize = 14.sp,
                    color = textSecondary
                )
            }
        }

        // ── Segmented Navigation ──────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(surface)
                    .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PnrNavPill(
                    icon = "🎟️",
                    label = "PNR Status",
                    isSelected = selectedSection == "pnr",
                    isDarkMode = isDarkMode,
                    onClick = { selectedSection = "pnr" },
                    modifier = Modifier.weight(1f)
                )
                PnrNavPill(
                    icon = "🎫",
                    label = "Tickets",
                    isSelected = selectedSection == "tickets",
                    isDarkMode = isDarkMode,
                    onClick = { showComingSoonFor = "Ticket Booking"; selectedSection = "tickets" },
                    modifier = Modifier.weight(1f)
                )
                PnrNavPill(
                    icon = "🚃",
                    label = "Bookings",
                    isSelected = selectedSection == "bookings",
                    isDarkMode = isDarkMode,
                    onClick = { showComingSoonFor = "Bookings"; selectedSection = "bookings" },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── PNR Section ───────────────────────────────────────────────────────
        if (selectedSection == "pnr") {
            // Input Card
            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    isDarkMode = isDarkMode,
                    surface = surface,
                    borderColor = borderColor
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "🎟️  ${I18n.t("pnr_input_label")}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = pnrInput,
                            onValueChange = { v ->
                                if (v.length <= 10 && v.all { it.isDigit() }) {
                                    pnrInput = v
                                    errorMsg = null
                                }
                            },
                            placeholder = {
                                Text(
                                    text = "e.g. 2341567890",
                                    color = textSecondary.copy(alpha = 0.6f),
                                    fontSize = 16.sp
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = errorMsg != null,
                            supportingText = errorMsg?.let { msg ->
                                { Text(msg, color = Color(0xFFEF4444), fontSize = 12.sp) }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RailBluePrimary,
                                unfocusedBorderColor = borderColor,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                cursorColor = RailBluePrimary
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = textPrimary
                            )
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = { checkPnr() },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RailBluePrimary,
                                disabledContainerColor = RailBluePrimary.copy(alpha = 0.5f)
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = if (isLoading) I18n.t("pnr_loading") else I18n.t("pnr_check_btn"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Result Card
            if (hasSearched && pnrResult != null) {
                val data = pnrResult!!
                item {
                    PnrResultCard(
                        data = data,
                        isDarkMode = isDarkMode,
                        surface = surface,
                        borderColor = borderColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Not Found State
            if (hasSearched && pnrResult == null && !isLoading) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        isDarkMode = isDarkMode,
                        surface = surface,
                        borderColor = borderColor
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("🎫", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = I18n.t("pnr_not_found"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = textPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = errorMsg ?: I18n.t("pnr_not_found_sub"),
                                fontSize = 13.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Idle state hint
            if (!hasSearched && !isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🚂", fontSize = 52.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Enter your 10-digit PNR number\nto check ticket & seat status",
                            fontSize = 14.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PNR is printed on your ticket or booking confirmation",
                            fontSize = 12.sp,
                            color = textSecondary.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }

        // ── Tickets / Bookings Coming Soon ────────────────────────────────────
        if (selectedSection == "tickets" || selectedSection == "bookings") {
            val sectionLabel = if (selectedSection == "tickets") "Ticket Booking" else "Train Bookings"
            val sectionIcon = if (selectedSection == "tickets") "🎫" else "🚃"
            val sectionDesc = if (selectedSection == "tickets")
                "Reserve & book train tickets directly from RailGaadi.\nCurrently in development."
            else
                "Manage and view your train bookings in one place.\nCurrently in development."

            item {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    isDarkMode = isDarkMode,
                    surface = surface,
                    borderColor = borderColor
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(sectionIcon, fontSize = 52.sp)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = sectionLabel,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = textPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Coming Soon",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFF59E0B)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = sectionDesc,
                            fontSize = 14.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = { /* Notify me - UI only */ },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, RailBluePrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🔔  Notify Me",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = RailBluePrimary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ─── Glassmorphic Card ────────────────────────────────────────────────────────
@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean,
    surface: Color,
    borderColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(surface)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp)),
        content = content
    )
}

// ─── Segmented Nav Pill ───────────────────────────────────────────────────────
@Composable
private fun PnrNavPill(
    icon: String,
    label: String,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) RailBluePrimary else Color.Transparent
    val textColor = if (isSelected) Color.White else (if (isDarkMode) DarkTextSecondary else LightTextSecondary)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// ─── Status Badge ─────────────────────────────────────────────────────────────
@Composable
private fun StatusBadge(status: String?) {
    if (status.isNullOrBlank()) return
    val upper = status.uppercase()
    val (bg, fg, label) = when {
        upper.contains("CONFIRM") -> Triple(Color(0xFF16A34A).copy(alpha = 0.15f), Color(0xFF16A34A), "✅  CONFIRMED")
        upper.contains("RAC")     -> Triple(Color(0xFFF59E0B).copy(alpha = 0.15f), Color(0xFFF59E0B), "⚠️  RAC")
        upper.contains("WL") || upper.contains("WAIT") -> Triple(Color(0xFFEF4444).copy(alpha = 0.15f), Color(0xFFEF4444), "🔴  WAITLIST")
        else -> Triple(Color(0xFF64748B).copy(alpha = 0.12f), Color(0xFF64748B), status)
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = fg)
    }
}

// ─── PNR Result Card ──────────────────────────────────────────────────────────
@Composable
private fun PnrResultCard(
    data: PnrData,
    isDarkMode: Boolean,
    surface: Color,
    borderColor: Color,
    textPrimary: Color,
    textSecondary: Color
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        isDarkMode = isDarkMode,
        surface = surface,
        borderColor = borderColor
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Train info banner
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${data.trainNumber ?: "—"} · ${data.trainName ?: ""}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${data.sourceStation ?: "—"} → ${data.destinationStation ?: "—"}",
                        fontSize = 13.sp,
                        color = textSecondary
                    )
                    if (!data.dateOfJourney.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "🗓  ${data.dateOfJourney}",
                            fontSize = 12.sp,
                            color = textSecondary.copy(alpha = 0.8f)
                        )
                    }
                }
                // Chart status badge
                val chartPrepared = data.chartStatus?.contains("PREPARED", ignoreCase = true) == true
                        && data.chartStatus?.contains("NOT", ignoreCase = true) == false
                Box(
                    modifier = Modifier
                        .background(
                            if (chartPrepared) Color(0xFF16A34A).copy(alpha = 0.12f)
                            else Color(0xFFF59E0B).copy(alpha = 0.12f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (chartPrepared) "✅ Chart Ready" else "⏳ Chart Pending",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (chartPrepared) Color(0xFF16A34A) else Color(0xFFF59E0B)
                    )
                }
            }

            // Travel class / boarding / quota info
            if (!data.travelClass.isNullOrBlank() || !data.boardingStation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!data.travelClass.isNullOrBlank()) {
                        InfoChip(label = I18n.t("pnr_class"), value = data.travelClass, textSecondary = textSecondary, textPrimary = textPrimary)
                    }
                    if (!data.boardingStation.isNullOrBlank()) {
                        InfoChip(label = "Boarding", value = data.boardingStation, textSecondary = textSecondary, textPrimary = textPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = borderColor)
            Spacer(modifier = Modifier.height(16.dp))

            // Passengers header
            Text(
                text = "👥  Passengers",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = textPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            val passengers = data.passengers ?: emptyList()
            if (passengers.isEmpty()) {
                Text(
                    text = "No passenger details available.",
                    fontSize = 13.sp,
                    color = textSecondary
                )
            } else {
                // Header row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TableHeader("#", Modifier.weight(0.5f), textSecondary)
                    TableHeader(I18n.t("pnr_booking_status"), Modifier.weight(2f), textSecondary)
                    TableHeader("Current", Modifier.weight(2f), textSecondary)
                    TableHeader(I18n.t("pnr_coach"), Modifier.weight(1f), textSecondary)
                    TableHeader(I18n.t("pnr_berth"), Modifier.weight(1f), textSecondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                passengers.forEach { pax ->
                    PassengerRow(pax = pax, isDarkMode = isDarkMode, textPrimary = textPrimary, textSecondary = textSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = borderColor)
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "ℹ️  Data sourced from Indian Railways via RailGaadi API",
                fontSize = 11.sp,
                color = textSecondary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, textSecondary: Color, textPrimary: Color) {
    Column {
        Text(label, fontSize = 10.sp, color = textSecondary)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
    }
}

@Composable
private fun TableHeader(text: String, modifier: Modifier, color: Color) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = modifier,
        maxLines = 1
    )
}

@Composable
private fun PassengerRow(
    pax: PnrPassenger,
    isDarkMode: Boolean,
    textPrimary: Color,
    textSecondary: Color
) {
    val currentStatus = pax.currentStatus?.uppercase() ?: ""
    val statusColor = when {
        currentStatus.contains("CONFIRM") -> Color(0xFF16A34A)
        currentStatus.contains("RAC")     -> Color(0xFFF59E0B)
        currentStatus.contains("WL") || currentStatus.contains("WAIT") -> Color(0xFFEF4444)
        else -> textSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${pax.number ?: "—"}",
            fontSize = 13.sp,
            color = textPrimary,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            text = pax.bookingStatus ?: "—",
            fontSize = 12.sp,
            color = textSecondary,
            modifier = Modifier.weight(2f),
            maxLines = 1
        )
        Text(
            text = pax.currentStatus ?: "—",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = statusColor,
            modifier = Modifier.weight(2f),
            maxLines = 1
        )
        Text(
            text = pax.coach ?: "—",
            fontSize = 12.sp,
            color = textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = pax.berth ?: "—",
            fontSize = 12.sp,
            color = textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
    }
    HorizontalDivider(color = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f))
}
