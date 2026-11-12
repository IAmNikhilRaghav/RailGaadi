package com.railgaadi.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.model.AlarmTone
import com.railgaadi.app.model.AlarmTriggerUnit
import com.railgaadi.app.model.Station
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationAlarmModal(
    onDismiss: () -> Unit
) {
    val isDark = TrainTrackerState.isDarkMode
    val liveStatus = TrainTrackerState.liveStatus
    val stations = liveStatus?.stations ?: emptyList()
    val existingAlarm = TrainTrackerState.activeAlarm

    // Default station: initial target or last station (terminus)
    var selectedStation by remember {
        mutableStateOf(
            TrainTrackerState.targetAlarmStation
                ?: stations.lastOrNull()
                ?: Station(code = "—", name = "Destination", latitude = 0.0, longitude = 0.0)
        )
    }

    var triggerUnit by remember {
        mutableStateOf(existingAlarm?.triggerUnit ?: AlarmTriggerUnit.KILOMETERS)
    }
    var triggerDistanceKm by remember {
        mutableStateOf(existingAlarm?.triggerDistanceKm ?: 10.0)
    }
    var isManualDistance by remember {
        mutableStateOf(existingAlarm?.triggerDistanceKm?.let { it !in listOf(5.0, 10.0, 15.0, 20.0, 25.0) } ?: false)
    }
    var manualDistanceInput by remember {
        mutableStateOf(if (isManualDistance) triggerDistanceKm.toInt().toString() else "")
    }

    var triggerMinutesBefore by remember {
        mutableStateOf(existingAlarm?.triggerMinutesBefore ?: 15)
    }
    var isManualMinutes by remember {
        mutableStateOf(existingAlarm?.triggerMinutesBefore?.let { it !in listOf(10, 15, 20, 30) } ?: false)
    }
    var manualMinutesInput by remember {
        mutableStateOf(if (isManualMinutes) triggerMinutesBefore.toString() else "")
    }

    var voiceAnnouncement by remember {
        mutableStateOf(existingAlarm?.voiceAnnouncement ?: true)
    }
    var vibrate by remember {
        mutableStateOf(existingAlarm?.vibrate ?: true)
    }
    var selectedTone by remember {
        mutableStateOf(existingAlarm?.tone ?: AlarmTone.RAILWAY_CHIME)
    }
    var isStationPickerOpen by remember { mutableStateOf(false) }

    val bgSurface = if (isDark) DarkSurface else LightSurface
    val textPrimary = if (isDark) DarkTextPrimary else LightTextPrimary
    val textSecondary = if (isDark) DarkTextSecondary else LightTextSecondary
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bgSurface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(RailBluePrimary.copy(alpha = 0.14f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⏰", fontSize = 19.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Destination Wake-Up Alarm",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "GPS geofence alerts you even in sleep mode",
                            fontSize = 12.sp,
                            color = textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (existingAlarm?.isArmed == true) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .wrapContentWidth()
                            .background(RailGreenOnTime.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, RailGreenOnTime.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "● ARMED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RailGreenOnTime,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 1. Station Selector
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "TARGET DESTINATION STATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    letterSpacing = 0.5.sp
                )

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) DarkBackground else Color(0xFFF1F5F9)
                    ),
                    border = BorderStroke(1.dp, cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isStationPickerOpen = !isStationPickerOpen }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("🚉", fontSize = 20.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = selectedStation.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Code: ${selectedStation.code} • Platform ${selectedStation.platform.ifBlank { "—" }}",
                                    fontSize = 12.sp,
                                    color = textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = if (isStationPickerOpen) "▲ Change" else "▼ Change",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = RailBluePrimary,
                            maxLines = 1
                        )
                    }
                }

                // Dropdown of stations along route if opened
                if (isStationPickerOpen && stations.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkBackground else Color.White),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(stations) { stn ->
                                val isCur = stn.code == selectedStation.code
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedStation = stn
                                            isStationPickerOpen = false
                                        }
                                        .background(if (isCur) RailBluePrimary.copy(alpha = 0.12f) else Color.Transparent)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(stn.name, fontSize = 13.sp, fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal, color = textPrimary)
                                        Text("${stn.code} • ${stn.distanceKm} km", fontSize = 11.sp, color = textSecondary)
                                    }
                                    if (isCur) {
                                        Text("✓", color = RailBluePrimary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Trigger Type & Value Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "WHEN TO WAKE YOU UP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondary,
                        letterSpacing = 0.5.sp
                    )

                    // Distance vs Time Segment Switch
                    Row(
                        modifier = Modifier
                            .background(if (isDark) DarkBackground else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (triggerUnit == AlarmTriggerUnit.KILOMETERS) RailBluePrimary else Color.Transparent)
                                .clickable { triggerUnit = AlarmTriggerUnit.KILOMETERS }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "By Distance",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (triggerUnit == AlarmTriggerUnit.KILOMETERS) Color.White else textSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (triggerUnit == AlarmTriggerUnit.MINUTES) RailBluePrimary else Color.Transparent)
                                .clickable { triggerUnit = AlarmTriggerUnit.MINUTES }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "By Time",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (triggerUnit == AlarmTriggerUnit.MINUTES) Color.White else textSecondary
                            )
                        }
                    }
                }

                // Options Chips & Manual Entry Mode
                if (triggerUnit == AlarmTriggerUnit.KILOMETERS) {
                    val distances = listOf(5.0, 10.0, 15.0, 20.0, 25.0)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        distances.forEach { dist ->
                            val isSel = !isManualDistance && triggerDistanceKm == dist
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) RailBluePrimary else (if (isDark) DarkBackground else Color(0xFFF1F5F9)))
                                    .border(1.dp, if (isSel) RailBluePrimary else cardBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        isManualDistance = false
                                        triggerDistanceKm = dist
                                        manualDistanceInput = ""
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${dist.toInt()} km",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else textPrimary
                                )
                            }
                        }
                    }

                    // Manual Distance Entry Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isManualDistance) RailBluePrimary.copy(alpha = 0.08f)
                                else (if (isDark) DarkBackground else Color(0xFFF8FAFC))
                            )
                            .border(
                                width = if (isManualDistance) 1.5.dp else 1.dp,
                                color = if (isManualDistance) RailBluePrimary else cardBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { isManualDistance = true }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("✏️", fontSize = 16.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Distance",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isManualDistance) RailBluePrimary else textPrimary
                                )
                                Text(
                                    text = "Enter any distance (1 - 999 km)",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                        }

                        MinimalNumberInput(
                            value = manualDistanceInput,
                            onValueChange = { digits ->
                                manualDistanceInput = digits
                                isManualDistance = true
                                val parsed = digits.toDoubleOrNull()
                                if (parsed != null && parsed > 0) {
                                    triggerDistanceKm = parsed
                                }
                            },
                            placeholder = "0",
                            suffix = "km",
                            isActive = isManualDistance,
                            onActivate = { isManualDistance = true },
                            isDark = isDark,
                            cardBorder = cardBorder
                        )
                    }

                    Text(
                        "Alarms automatically as soon as the train is ${triggerDistanceKm.toInt()} km away from ${selectedStation.name}",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                } else {
                    val minutes = listOf(10, 15, 20, 30)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        minutes.forEach { min ->
                            val isSel = !isManualMinutes && triggerMinutesBefore == min
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) RailBluePrimary else (if (isDark) DarkBackground else Color(0xFFF1F5F9)))
                                    .border(1.dp, if (isSel) RailBluePrimary else cardBorder, RoundedCornerShape(10.dp))
                                    .clickable {
                                        isManualMinutes = false
                                        triggerMinutesBefore = min
                                        manualMinutesInput = ""
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$min min",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) Color.White else textPrimary
                                )
                            }
                        }
                    }

                    // Manual Time Entry Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isManualMinutes) RailBluePrimary.copy(alpha = 0.08f)
                                else (if (isDark) DarkBackground else Color(0xFFF8FAFC))
                            )
                            .border(
                                width = if (isManualMinutes) 1.5.dp else 1.dp,
                                color = if (isManualMinutes) RailBluePrimary else cardBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { isManualMinutes = true }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("⏱️", fontSize = 16.sp)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Custom Time",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isManualMinutes) RailBluePrimary else textPrimary
                                )
                                Text(
                                    text = "Enter minutes before arrival",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                        }

                        MinimalNumberInput(
                            value = manualMinutesInput,
                            onValueChange = { digits ->
                                manualMinutesInput = digits
                                isManualMinutes = true
                                val parsed = digits.toIntOrNull()
                                if (parsed != null && parsed > 0) {
                                    triggerMinutesBefore = parsed
                                }
                            },
                            placeholder = "0",
                            suffix = "min",
                            isActive = isManualMinutes,
                            onActivate = { isManualMinutes = true },
                            isDark = isDark,
                            cardBorder = cardBorder
                        )
                    }

                    Text(
                        "Alarms estimated $triggerMinutesBefore minutes before the train pulls into ${selectedStation.name}",
                        fontSize = 11.sp,
                        color = textSecondary
                    )
                }
            }

            // 3. Ringtone & Audio Toggles
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "ALERT SOUND & TONES",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary,
                    letterSpacing = 0.5.sp
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(AlarmTone.entries) { tone ->
                        val isSel = selectedTone == tone
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) RailBluePrimary.copy(alpha = 0.15f) else (if (isDark) DarkBackground else Color(0xFFF1F5F9)))
                                .border(1.dp, if (isSel) RailBluePrimary else cardBorder, RoundedCornerShape(12.dp))
                                .clickable { selectedTone = tone }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(tone.emoji, fontSize = 14.sp)
                                Text(
                                    tone.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) RailBluePrimary else textPrimary
                                )
                            }
                        }
                    }
                }

                // Voice & Vibration Switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🗣️", fontSize = 16.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Station Voice Announcement", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("IR-style speaker announcement", fontSize = 11.sp, color = textSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = voiceAnnouncement,
                        onCheckedChange = { voiceAnnouncement = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RailBluePrimary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("📳", fontSize = 16.sp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Continuous Vibration", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("Haptic pulses while phone is in pocket", fontSize = 11.sp, color = textSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = vibrate,
                        onCheckedChange = { vibrate = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = RailBluePrimary)
                    )
                }
            }

            // 4. Test Alarm & Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Test Sound Button
                OutlinedButton(
                    onClick = {
                        TrainTrackerState.testAlarmSound(selectedTone, voiceAnnouncement, vibrate)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RailBluePrimary.copy(alpha = 0.45f))
                ) {
                    Text("🔊 Test Sound", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)
                }

                // Arm / Update Alarm Button
                Button(
                    onClick = {
                        val effectiveDistance = if (isManualDistance && manualDistanceInput.isNotBlank()) {
                            manualDistanceInput.toDoubleOrNull()?.takeIf { it > 0 } ?: triggerDistanceKm
                        } else {
                            triggerDistanceKm
                        }
                        val effectiveMinutes = if (isManualMinutes && manualMinutesInput.isNotBlank()) {
                            manualMinutesInput.toIntOrNull()?.takeIf { it > 0 } ?: triggerMinutesBefore
                        } else {
                            triggerMinutesBefore
                        }
                        TrainTrackerState.armDestinationAlarm(
                            station = selectedStation,
                            triggerDistanceKm = effectiveDistance,
                            triggerMinutesBefore = effectiveMinutes,
                            triggerUnit = triggerUnit,
                            voiceAnnouncement = voiceAnnouncement,
                            vibrate = vibrate,
                            tone = selectedTone
                        )
                    },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RailBluePrimary)
                ) {
                    Text(
                        if (existingAlarm?.isArmed == true) "Update Alarm" else "Arm Wake-Up Alarm",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Disarm Button (if already armed)
            if (existingAlarm?.isArmed == true) {
                TextButton(
                    onClick = { TrainTrackerState.disarmAlarm() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text("✕ Turn Off Wake-Up Alarm", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = RailRedDelayed)
                }
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun MinimalNumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    suffix: String,
    isActive: Boolean,
    onActivate: () -> Unit,
    isDark: Boolean,
    cardBorder: Color,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDark) DarkSurface else Color.White)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) RailBluePrimary else cardBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onActivate() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = { raw ->
                onActivate()
                val digits = raw.filter { it.isDigit() }
                if (digits.length <= 3) {
                    onValueChange(digits)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { keyboardController?.hide() }
            ),
            textStyle = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) DarkTextPrimary else LightTextPrimary,
                textAlign = TextAlign.Center
            ),
            cursorBrush = SolidColor(RailBluePrimary),
            modifier = Modifier.width(46.dp),
            decorationBox = { innerTextField ->
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) DarkTextSecondary.copy(alpha = 0.5f) else LightTextSecondary.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                    innerTextField()
                }
            }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = suffix,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) RailBluePrimary else (if (isDark) DarkTextSecondary else LightTextSecondary)
        )
    }
}
