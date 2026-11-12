package com.railgaadi.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.model.StationDataService
import com.railgaadi.app.model.StationInfo
import com.railgaadi.app.model.Train
import com.railgaadi.app.network.RailRadarService
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.*
import com.railgaadi.app.util.PlatformDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// -------------------------------------------------------------
// Cross-Platform Lightweight Travel Date Model & Helper
// -------------------------------------------------------------
data class RouteTravelDate(
    val dayNumber: Int,
    val monthNumber: Int,
    val monthName: String,
    val year: Int,
    val dayOfWeek: String,
    val dayOfWeekFull: String,
    val dayOfWeekIndex: Int,
    val isYesterday: Boolean = false,
    val isToday: Boolean = false,
    val isTomorrow: Boolean = false
) {
    val displayLabel: String
        get() = when {
            isToday -> "Today"
            isTomorrow -> "Tomorrow"
            isYesterday -> "Yesterday"
            else -> "$dayOfWeek, $dayNumber $monthName"
        }

    val shortLabel: String
        get() = when {
            isToday -> "Today, $dayNumber $monthName"
            isTomorrow -> "Tomorrow, $dayNumber $monthName"
            isYesterday -> "Yesterday, $dayNumber $monthName"
            else -> "$dayOfWeek, $dayNumber $monthName"
        }

    val fullFormatted: String
        get() = "$dayOfWeekFull, $dayNumber $monthName $year"
}

fun generateTravelDates(): List<RouteTravelDate> {
    val (todayDay, todayMonth, todayYear) = PlatformDate.getToday()
    val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    // leap-year aware days per month
    fun daysInMonth(m: Int, y: Int): Int {
        if (m == 2) return if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 29 else 28
        return intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)[m]
    }
    // Tomohiko Sakamoto – returns 0=Sun … 6=Sat
    fun dayOfWeek(d: Int, m: Int, y: Int): Int {
        val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
        val yr = if (m < 3) y - 1 else y
        return (yr + yr/4 - yr/100 + yr/400 + t[m - 1] + d) % 7
    }
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val dayFullNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    // Yesterday calculation:
    var yDay = todayDay - 1
    var yMonth = todayMonth
    var yYear = todayYear
    if (yDay < 1) {
        yMonth -= 1
        if (yMonth < 1) {
            yMonth = 12
            yYear -= 1
        }
        yDay = daysInMonth(yMonth, yYear)
    }

    val list = mutableListOf<RouteTravelDate>()
    var curDay = yDay
    var curMonth = yMonth
    var curYear = yYear

    for (i in 0 until 120) {
        val dow = dayOfWeek(curDay, curMonth, curYear)
        list.add(
            RouteTravelDate(
                dayNumber = curDay,
                monthNumber = curMonth,
                monthName = monthNames[curMonth],
                year = curYear,
                dayOfWeek = dayNames[dow],
                dayOfWeekFull = dayFullNames[dow],
                dayOfWeekIndex = dow,
                isYesterday = (i == 0),
                isToday = (i == 1),
                isTomorrow = (i == 2)
            )
        )
        curDay++
        if (curDay > daysInMonth(curMonth, curYear)) {
            curDay = 1
            curMonth++
            if (curMonth > 12) { curMonth = 1; curYear++ }
        }
    }
    return list
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(onTrackTrain: (Train, String) -> Unit = { _, _ -> }) {
    val isDark = TrainTrackerState.isDarkMode
    val service = remember { RailRadarService() }
    val coroutineScope = rememberCoroutineScope()

    var fromTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var toTextFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    val fromStationQuery = fromTextFieldValue.text
    val toStationQuery = toTextFieldValue.text

    val fromFocusRequester = remember { FocusRequester() }
    val toFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    var swapRotation by remember { mutableStateOf(0f) }
    val animatedSwapRotation by animateFloatAsState(
        targetValue = swapRotation,
        animationSpec = tween(320, easing = FastOutSlowInEasing),
        label = "swap_rot"
    )

    var isFromDropdownOpen by remember { mutableStateOf(false) }
    var isToDropdownOpen by remember { mutableStateOf(false) }

    // Travel Dates & Calendar — dynamically generated starting from real device date
    val travelDates = remember { generateTravelDates() }
    var selectedTravelDate by remember(travelDates) { mutableStateOf(travelDates.firstOrNull { it.isToday } ?: travelDates[0]) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    // Show bottom sheet style: "Yesterday / Today / Tomorrow / Choose from Calendar"
    var showDateQuickPicker by remember { mutableStateOf(false) }

    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Train>>(emptyList()) }
    var hasSearchedOnce by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun triggerSearch(fromText: String, toText: String) {
        val cleanFrom = fromText.trim()
        val cleanTo = toText.trim()

        if (cleanFrom.isEmpty() || cleanTo.isEmpty()) {
            validationError = "Please enter both From and To stations"
            return
        }

        validationError = null
        val fCode = StationDataService.resolveStationCode(cleanFrom)
        val tCode = StationDataService.resolveStationCode(cleanTo)

        coroutineScope.launch {
            isSearching = true
            val results = service.searchTrainsBetweenStations(fCode, tCode)
            searchResults = results
            hasSearchedOnce = true
            isSearching = false
        }
    }

    fun selectFromStation(stationText: String) {
        fromTextFieldValue = TextFieldValue(
            text = stationText,
            selection = TextRange(stationText.length)
        )
        isFromDropdownOpen = false
        validationError = null
        toFocusRequester.requestFocus()
    }

    fun selectToStation(stationText: String) {
        toTextFieldValue = TextFieldValue(
            text = stationText,
            selection = TextRange(stationText.length)
        )
        isToDropdownOpen = false
        validationError = null
        keyboardController?.hide()
        if (fromTextFieldValue.text.isNotBlank()) {
            triggerSearch(fromTextFieldValue.text, stationText)
        }
    }

    var liveFromSuggestions by remember { mutableStateOf<List<StationInfo>>(emptyList()) }
    var liveToSuggestions by remember { mutableStateOf<List<StationInfo>>(emptyList()) }

    LaunchedEffect(fromStationQuery) {
        val q = fromStationQuery.trim()
        if (q.length >= 2 && !q.contains("(")) {
            delay(150)
            val r = service.searchStations(q)
            if (r.isNotEmpty()) liveFromSuggestions = r
        } else {
            liveFromSuggestions = emptyList()
        }
    }

    LaunchedEffect(toStationQuery) {
        val q = toStationQuery.trim()
        if (q.length >= 2 && !q.contains("(")) {
            delay(150)
            val r = service.searchStations(q)
            if (r.isNotEmpty()) liveToSuggestions = r
        } else {
            liveToSuggestions = emptyList()
        }
    }

    val fromSuggestions = remember(fromStationQuery, liveFromSuggestions) {
        if (fromStationQuery.isBlank()) emptyList()
        else (liveFromSuggestions + StationDataService.searchStations(fromStationQuery))
            .distinctBy { it.code.uppercase() }.take(15)
    }

    val toSuggestions = remember(toStationQuery, liveToSuggestions) {
        if (toStationQuery.isBlank()) emptyList()
        else (liveToSuggestions + StationDataService.searchStations(toStationQuery))
            .distinctBy { it.code.uppercase() }.take(15)
    }

    val filteredTrains = remember(searchResults, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") searchResults
        else searchResults.filter {
            it.type.contains(selectedCategoryFilter, ignoreCase = true) ||
            it.name.contains(selectedCategoryFilter, ignoreCase = true)
        }
    }

    // "Where is my train" style quick date picker bottom sheet
    if (showDateQuickPicker) {
        val departureName = fromStationQuery.substringBefore(" (").ifBlank { "origin station" }
        ModalBottomSheet(
            onDismissRequest = { showDateQuickPicker = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Choose the date",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "for departure from $departureName",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                HorizontalDivider(color = Color(0xFFF1F5F9))

                val yesterdayDate = travelDates.firstOrNull { it.isYesterday }
                val todayDate = travelDates.firstOrNull { it.isToday } ?: travelDates[0]
                val tomorrowDate = travelDates.firstOrNull { it.isTomorrow } ?: travelDates[1]

                val options = listOfNotNull(
                    yesterdayDate?.let { "Yesterday" to it },
                    "Today" to todayDate,
                    "Tomorrow" to tomorrowDate
                )

                options.forEach { (label, date) ->
                    val isChosen = (selectedTravelDate.dayNumber == date.dayNumber && selectedTravelDate.monthNumber == date.monthNumber && selectedTravelDate.year == date.year)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTravelDate = date
                                showDateQuickPicker = false
                            }
                            .padding(vertical = 15.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = label,
                            fontSize = 16.sp,
                            fontWeight = if (isChosen) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isChosen) RailBluePrimary else Color(0xFF1E3A8A)
                        )
                    }
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                }

                // "Choose from Calendar" row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDateQuickPicker = false
                            showCalendarDialog = true
                        }
                        .padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose from Calendar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = RailBluePrimary
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Cancel button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDateQuickPicker = false }
                        .padding(vertical = 15.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RailBluePrimary
                    )
                }
            }
        }
    }

    // Full Calendar Bottom Sheet (Where is my Train style)
    if (showCalendarDialog) {
        CalendarPickerBottomSheet(
            currentSelected = selectedTravelDate,
            departureStation = fromStationQuery,
            allDates = travelDates,
            onDismiss = { showCalendarDialog = false },
            onDateSelected = { date ->
                selectedTravelDate = date
                showCalendarDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ─────────────────────────────────────────────────────
        // 1. COMPACT Station-to-Station Search Card (Glassmorphic)
        // ─────────────────────────────────────────────────────
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.60f) else Color.White.copy(alpha = 0.88f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Search Trains",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // FROM input
                    Column {
                        OutlinedTextField(
                            value = fromTextFieldValue,
                            onValueChange = {
                                fromTextFieldValue = it
                                isFromDropdownOpen = true
                                isToDropdownOpen = false
                                validationError = null
                            },
                            label = { Text("From", fontSize = 12.sp) },
                            placeholder = { Text("Station name or code", color = Color.Gray, fontSize = 12.sp) },
                            trailingIcon = {
                                if (fromTextFieldValue.text.isNotEmpty()) {
                                    IconButton(onClick = {
                                        fromTextFieldValue = TextFieldValue("", selection = TextRange(0))
                                        isFromDropdownOpen = false
                                    }) {
                                        Text("✕", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Next,
                                capitalization = KeyboardCapitalization.Words
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    val bestStation = fromSuggestions.firstOrNull()
                                    val stationText = if (bestStation != null) {
                                        "${bestStation.name} (${bestStation.code})"
                                    } else {
                                        fromTextFieldValue.text.trim()
                                    }
                                    if (stationText.isNotBlank()) {
                                        selectFromStation(stationText)
                                    } else {
                                        toFocusRequester.requestFocus()
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(fromFocusRequester),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )

                        AnimatedVisibility(
                            visible = isFromDropdownOpen && fromSuggestions.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 3.dp).heightIn(max = 180.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                    items(fromSuggestions) { stn ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectFromStation("${stn.name} (${stn.code})")
                                                }
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(stn.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                                if (stn.city.isNotEmpty()) Text("${stn.city}, ${stn.state}", fontSize = 10.sp, color = Color.Gray)
                                            }
                                            Box(
                                                modifier = Modifier.background(Color(0xFFE2E8F0), RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(stn.code, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = RailBluePrimary)
                                            }
                                        }
                                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                                    }
                                }
                            }
                        }
                    }

                    // Swap button between From / To – clean connecting divider with zero overlap
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            thickness = 1.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .graphicsLayer { rotationZ = animatedSwapRotation }
                                .clip(CircleShape)
                                .background(RailBluePrimary.copy(alpha = 0.12f))
                                .border(1.dp, RailBluePrimary.copy(alpha = 0.30f), CircleShape)
                                .clickable {
                                    swapRotation += 180f
                                    val temp = fromTextFieldValue
                                    fromTextFieldValue = TextFieldValue(toTextFieldValue.text, selection = TextRange(toTextFieldValue.text.length))
                                    toTextFieldValue = TextFieldValue(temp.text, selection = TextRange(temp.text.length))
                                    isFromDropdownOpen = false
                                    isToDropdownOpen = false
                                    if (fromTextFieldValue.text.isNotBlank() && toTextFieldValue.text.isNotBlank()) {
                                        triggerSearch(fromTextFieldValue.text, toTextFieldValue.text)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("⇅", fontSize = 16.sp, color = RailBluePrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                            thickness = 1.dp
                        )
                    }

                    // TO input
                    Column {
                        OutlinedTextField(
                            value = toTextFieldValue,
                            onValueChange = {
                                toTextFieldValue = it
                                isToDropdownOpen = true
                                isFromDropdownOpen = false
                                validationError = null
                            },
                            label = { Text("To", fontSize = 12.sp) },
                            placeholder = { Text("Station name or code", color = Color.Gray, fontSize = 12.sp) },
                            trailingIcon = {
                                if (toTextFieldValue.text.isNotEmpty()) {
                                    IconButton(onClick = {
                                        toTextFieldValue = TextFieldValue("", selection = TextRange(0))
                                        isToDropdownOpen = false
                                    }) {
                                        Text("✕", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Search,
                                capitalization = KeyboardCapitalization.Words
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    val bestStation = toSuggestions.firstOrNull()
                                    val stationText = if (bestStation != null) {
                                        "${bestStation.name} (${bestStation.code})"
                                    } else {
                                        toTextFieldValue.text.trim()
                                    }
                                    if (stationText.isNotBlank()) {
                                        selectToStation(stationText)
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(toFocusRequester),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            textStyle = LocalTextStyle.current.copy(fontSize = 13.sp)
                        )

                        AnimatedVisibility(
                            visible = isToDropdownOpen && toSuggestions.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                                modifier = Modifier.fillMaxWidth().padding(top = 3.dp).heightIn(max = 180.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                    items(toSuggestions) { stn ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectToStation("${stn.name} (${stn.code})")
                                                }
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(stn.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                                if (stn.city.isNotEmpty()) Text("${stn.city}, ${stn.state}", fontSize = 10.sp, color = Color.Gray)
                                            }
                                            Box(
                                                modifier = Modifier.background(Color(0xFFE2E8F0), RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(stn.code, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = RailBluePrimary)
                                            }
                                        }
                                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isSearching) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = RailBluePrimary,
                            trackColor = RailBluePrimary.copy(alpha = 0.12f)
                        )
                    }

                    if (validationError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = validationError ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // ── DATE PICKER – Where Is My Train style compact pill ──
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE2EDF9), RoundedCornerShape(8.dp))
                                .clickable { showDateQuickPicker = true }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = selectedTravelDate.displayLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E3A8A)
                                )
                                Text(
                                    text = "▾",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E3A8A)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────
        // 2. RECENTLY SEARCHED TRAINS – max 3, with Red Bin in header & ✕ per row
        // ─────────────────────────────────────────────────────
        val recentTrains = TrainTrackerState.recentTrains
        if (recentTrains.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🕒", fontSize = 13.sp)
                            Text(
                                "Recently Searched Trains",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        // Red bin logo at the right corner (clears recent history)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFEE2E2).copy(alpha = 0.85f))
                                .border(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.5f), CircleShape)
                                .clickable {
                                    TrainTrackerState.clearRecentTrains()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "🗑️",
                                fontSize = 13.sp
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recentTrains.take(3).forEach { train ->
                            SwipeToDeleteRecentTrainItem(
                                train = train,
                                isDark = isDark,
                                onTrackTrain = { onTrackTrain(train, train.origin) },
                                onDelete = { TrainTrackerState.removeRecentTrain(train.number) }
                            )
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────
        // 3. CATEGORY FILTERS
        // ─────────────────────────────────────────────────────
        if (searchResults.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        "${I18n.t("rs_filter_by")} Category",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val categories = listOf("All", "Vande Bharat", "Rajdhani", "Shatabdi", "Superfast", "Express", "Passenger")
                        items(categories) { cat ->
                            RouteCategoryChip(
                                category = cat,
                                isSelected = selectedCategoryFilter == cat,
                                isDark = isDark,
                                onClick = { selectedCategoryFilter = cat }
                            )
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────
        // 4. RESULTS HEADER
        // ─────────────────────────────────────────────────────
        if (hasSearchedOnce && searchResults.isNotEmpty()) {
            item {
                val fCode = StationDataService.resolveStationCode(fromStationQuery)
                val tCode = StationDataService.resolveStationCode(toStationQuery)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isSearching) "Searching trains…" else "${filteredTrains.size} Trains Found ($fCode → $tCode)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        selectedTravelDate.shortLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RailBluePrimary
                    )
                }
            }
        }

        // ─────────────────────────────────────────────────────
        // 5. WELCOME STATE (no search yet)
        // ─────────────────────────────────────────────────────
        if (!hasSearchedOnce) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🗺️", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Plan Your Train Journey", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            "Search from 8,690+ Indian Railway stations. Pick a date and track trains live.",
                            fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            FeaturePill(icon = "⚡", label = "Live GPS", modifier = Modifier.weight(1f))
                            FeaturePill(icon = "⏱️", label = "Delays", modifier = Modifier.weight(1f))
                            FeaturePill(icon = "📅", label = "Calendar", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────
        // 6. TRAIN RESULTS
        // ─────────────────────────────────────────────────────
        if (filteredTrains.isEmpty() && !isSearching && hasSearchedOnce) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🚆", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No Direct Trains Found", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Try junction codes like MTJ, JP, NDLS, AGC, or MMCT.",
                            fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 15.sp
                        )
                    }
                }
            }
        } else if (hasSearchedOnce) {
            items(filteredTrains) { train ->
                TrainCardItem(
                    train = train,
                    selectedDate = selectedTravelDate,
                    onTrackClick = { onTrackTrain(train, fromStationQuery) }
                )
            }
        }
    }
}

@Composable
fun FeaturePill(icon: String, label: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.40f), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(icon, fontSize = 12.sp)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Where is my Train style full grid Calendar Bottom Sheet
// ─────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarPickerBottomSheet(
    currentSelected: RouteTravelDate,
    departureStation: String,
    allDates: List<RouteTravelDate>,
    onDismiss: () -> Unit,
    onDateSelected: (RouteTravelDate) -> Unit
) {
    var viewingMonth by remember { mutableStateOf(currentSelected.monthNumber) }
    var viewingYear by remember { mutableStateOf(currentSelected.year) }

    val monthFullNames = listOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    // leap-year aware
    fun daysInMonth(m: Int, y: Int): Int {
        if (m == 2) return if (y % 4 == 0 && (y % 100 != 0 || y % 400 == 0)) 29 else 28
        return intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)[m]
    }
    // Compute first day of week (0=Sun..6=Sat) using Tomohiko Sakamoto's algorithm
    fun firstDayOffset(month: Int, year: Int): Int {
        val t = intArrayOf(0, 3, 2, 5, 0, 3, 5, 1, 4, 6, 2, 4)
        val y = if (month < 3) year - 1 else year
        return (y + y/4 - y/100 + y/400 + t[month - 1] + 1) % 7
    }

    val daysInViewingMonth = daysInMonth(viewingMonth, viewingYear)
    val firstOffset = firstDayOffset(viewingMonth, viewingYear)
    val monthLabel = "${monthFullNames[viewingMonth]} $viewingYear"
    val stationClean = departureStation.substringBefore(" (").ifBlank { "origin station" }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
        ) {
            // Header: "Choose the date"
            Text(
                text = "Choose the date",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "for departure from $stationClean",
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 12.dp)
            )

            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Month navigation header with ‹ MonthYear ▾ › arrows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Month/year with dropdown arrow
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = monthLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("▾", fontSize = 12.sp, color = Color.Gray)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val (todayDay, todayMonth, todayYear) = PlatformDate.getToday()
                    val canGoBack = !(viewingYear < todayYear || (viewingYear == todayYear && viewingMonth <= todayMonth))
                    Text(
                        text = "‹",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canGoBack) MaterialTheme.colorScheme.onSurface else Color(0xFFCBD5E1),
                        modifier = Modifier
                            .clickable(enabled = canGoBack) {
                                if (viewingMonth == 1) { viewingMonth = 12; viewingYear-- }
                                else viewingMonth--
                            }
                            .padding(horizontal = 6.dp)
                    )

                    val maxDate = allDates.lastOrNull()
                    val canGoForward = maxDate != null && (viewingYear < maxDate.year || (viewingYear == maxDate.year && viewingMonth < maxDate.monthNumber))
                    Text(
                        text = "›",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (canGoForward) MaterialTheme.colorScheme.onSurface else Color(0xFFCBD5E1),
                        modifier = Modifier
                            .clickable(enabled = canGoForward) {
                                if (viewingMonth == 12) { viewingMonth = 1; viewingYear++ }
                                else viewingMonth++
                            }
                            .padding(horizontal = 6.dp)
                    )
                }
            }

            // Day-of-week headers: S M T W T F S
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
                    Text(
                        text = d,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.width(38.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Calendar grid
            val (todayDay, todayMonth, todayYear) = PlatformDate.getToday()
            val rows = (firstOffset + daysInViewingMonth + 6) / 7

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - firstOffset + 1
                            if (cellIndex < firstOffset || dayNum > daysInViewingMonth) {
                                Box(modifier = Modifier.size(38.dp))
                            } else {
                                val isPast = (viewingYear < todayYear) ||
                                    (viewingYear == todayYear && viewingMonth < todayMonth) ||
                                    (viewingYear == todayYear && viewingMonth == todayMonth && dayNum < (todayDay - 1))
                                val isSelected = (currentSelected.dayNumber == dayNum && currentSelected.monthNumber == viewingMonth && currentSelected.year == viewingYear)
                                val isToday = (dayNum == todayDay && viewingMonth == todayMonth && viewingYear == todayYear)

                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            when {
                                                isSelected -> RailBluePrimary
                                                else -> Color.Transparent
                                            },
                                            CircleShape
                                        )
                                        .border(
                                            width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                            color = if (isToday && !isSelected) RailBluePrimary else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable(enabled = !isPast) {
                                            val matched = allDates.find {
                                                it.dayNumber == dayNum && it.monthNumber == viewingMonth && it.year == viewingYear
                                            } ?: run {
                                                val dOfWeek = (firstOffset + dayNum - 1) % 7
                                                val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                                                val dayFullNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                                                RouteTravelDate(
                                                    dayNumber = dayNum,
                                                    monthNumber = viewingMonth,
                                                    monthName = monthFullNames[viewingMonth].take(3),
                                                    year = viewingYear,
                                                    dayOfWeek = dayNames[dOfWeek],
                                                    dayOfWeekFull = dayFullNames[dOfWeek],
                                                    dayOfWeekIndex = dOfWeek,
                                                    isYesterday = (dayNum == todayDay - 1 && viewingMonth == todayMonth && viewingYear == todayYear),
                                                    isToday = (dayNum == todayDay && viewingMonth == todayMonth && viewingYear == todayYear),
                                                    isTomorrow = (dayNum == todayDay + 1 && viewingMonth == todayMonth && viewingYear == todayYear)
                                                )
                                            }
                                            onDateSelected(matched)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            isSelected -> Color.White
                                            isPast -> Color(0xFFCBD5E1)
                                            isToday -> RailBluePrimary
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))

            // Cancel button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismiss() }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RailBluePrimary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// Interactive Glassmorphic Category Chip with Touch/Hover Response
// ─────────────────────────────────────────────────────────────────
@Composable
private fun RouteCategoryChip(
    category: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        animationSpec = tween(110, easing = FastOutSlowInEasing),
        label = "chip_scale"
    )

    val bg = when {
        isSelected -> RailBluePrimary
        isDark -> Color(0xFF1E293B).copy(alpha = 0.65f)
        else -> Color.White.copy(alpha = 0.85f)
    }
    val borderCol = when {
        isSelected -> RailBluePrimary
        isDark -> Color.White.copy(alpha = 0.12f)
        else -> Color.Black.copy(alpha = 0.09f)
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(0.8.dp, borderCol, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = category,
            fontSize = 11.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else if (isDark) DarkTextPrimary else LightTextPrimary
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// Train Card Item with Date Specific Running Verification & Glassmorphism
// ─────────────────────────────────────────────────────────────────
@Composable
fun TrainCardItem(
    train: Train,
    selectedDate: RouteTravelDate,
    onTrackClick: () -> Unit
) {
    val isDark = TrainTrackerState.isDarkMode
    val dayIndex = selectedDate.dayOfWeekIndex
    val runsOnSelectedDate = train.runsOnDays.getOrElse(dayIndex) { true }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "card_scale"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B).copy(alpha = 0.65f) else Color.White.copy(alpha = 0.88f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onTrackClick() }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Train number, name, category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .background(if (isDark) Color(0xFF0F172A) else Color(0xFF0F172A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(train.number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Text(
                        train.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val typeBg = if (isDark) {
                    when {
                        train.type.contains("Vande Bharat", ignoreCase = true) -> Color(0xFF1E3A8A).copy(alpha = 0.4f)
                        train.type.contains("Rajdhani", ignoreCase = true) -> Color(0xFF7F1D1D).copy(alpha = 0.4f)
                        train.type.contains("Shatabdi", ignoreCase = true) -> Color(0xFF14532D).copy(alpha = 0.4f)
                        train.type.contains("Superfast", ignoreCase = true) -> Color(0xFF581C87).copy(alpha = 0.4f)
                        else -> Color(0xFF334155).copy(alpha = 0.4f)
                    }
                } else {
                    when {
                        train.type.contains("Vande Bharat", ignoreCase = true) -> Color(0xFFEFF6FF)
                        train.type.contains("Rajdhani", ignoreCase = true) -> Color(0xFFFEF2F2)
                        train.type.contains("Shatabdi", ignoreCase = true) -> Color(0xFFF0FDF4)
                        train.type.contains("Superfast", ignoreCase = true) -> Color(0xFFFAF5FF)
                        else -> Color(0xFFF8FAFC)
                    }
                }

                val typeTextColor = if (isDark) {
                    when {
                        train.type.contains("Vande Bharat", ignoreCase = true) -> Color(0xFF93C5FD)
                        train.type.contains("Rajdhani", ignoreCase = true) -> Color(0xFFFCA5A5)
                        train.type.contains("Shatabdi", ignoreCase = true) -> Color(0xFF86EFAC)
                        train.type.contains("Superfast", ignoreCase = true) -> Color(0xFFD8B4FE)
                        else -> Color(0xFFCBD5E1)
                    }
                } else {
                    when {
                        train.type.contains("Vande Bharat", ignoreCase = true) -> Color(0xFF1D4ED8)
                        train.type.contains("Rajdhani", ignoreCase = true) -> Color(0xFFB91C1C)
                        train.type.contains("Shatabdi", ignoreCase = true) -> Color(0xFF15803D)
                        train.type.contains("Superfast", ignoreCase = true) -> Color(0xFF7C3AED)
                        else -> Color(0xFF475569)
                    }
                }

                Box(
                    modifier = Modifier
                        .background(typeBg, RoundedCornerShape(7.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        train.type,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        color = typeTextColor
                    )
                }
            }

            // Date-specific run badge
            val runBadgeBg = if (isDark) {
                if (runsOnSelectedDate) Color(0xFF065F46).copy(alpha = 0.40f) else Color(0xFF991B1B).copy(alpha = 0.35f)
            } else {
                if (runsOnSelectedDate) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
            }
            val runBadgeColor = if (isDark) {
                if (runsOnSelectedDate) Color(0xFF34D399) else Color(0xFFF87171)
            } else {
                if (runsOnSelectedDate) RailGreenOnTime else Color(0xFFDC2626)
            }

            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .background(runBadgeBg, RoundedCornerShape(5.dp))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    if (runsOnSelectedDate) "✓ Runs on ${selectedDate.dayOfWeek}, ${selectedDate.dayNumber} ${selectedDate.monthName}"
                    else "✕ Doesn't run on ${selectedDate.dayOfWeek}, ${selectedDate.dayNumber} ${selectedDate.monthName}",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = runBadgeColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timings
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(train.departureTime, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(train.origin.substringBefore(" ("), fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val hrs = train.durationMinutes / 60; val mins = train.durationMinutes % 60
                    val dur = if (hrs > 0) "${hrs}h ${mins}m" else if (mins > 0) "${mins}m" else "Direct"
                    Text(dur, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(5.dp).background(RailBluePrimary, CircleShape))
                        HorizontalDivider(modifier = Modifier.width(50.dp), thickness = 2.dp, color = RailBluePrimary)
                        Box(modifier = Modifier.size(5.dp).background(RailBluePrimary, CircleShape))
                    }
                    Text(if (train.platform.isNotEmpty()) "PF ${train.platform}" else "Daily", fontSize = 9.sp, color = Color.Gray)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(train.arrivalTime, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(train.destination.substringBefore(" ("), fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Running days + Track button
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                // Day circles — centered in their space with Apple glass styling
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val letters = listOf("S", "M", "T", "W", "T", "F", "S")
                    val runsList = if (train.runsOnDays.size == 7) train.runsOnDays else List(7) { true }
                    runsList.forEachIndexed { idx, runs ->
                        val isTravelDay = (idx == dayIndex)
                        val circleBg = when {
                            isTravelDay && runs -> RailBluePrimary
                            runs -> RailBluePrimary.copy(alpha = 0.85f)
                            isTravelDay && !runs -> Color(0xFFEF4444).copy(alpha = 0.16f)
                            else -> if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                        }
                        val circleBorderColor = when {
                            isTravelDay && runs -> Color(0xFF38BDF8)
                            isTravelDay && !runs -> Color(0xFFEF4444).copy(alpha = 0.70f)
                            else -> if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
                        }
                        val textColor = when {
                            runs -> Color.White
                            isTravelDay && !runs -> Color(0xFFEF4444)
                            else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                        }

                        Box(
                            modifier = Modifier
                                .size(25.dp)
                                .clip(CircleShape)
                                .background(circleBg, CircleShape)
                                .border(
                                    width = if (isTravelDay) 1.5.dp else 0.5.dp,
                                    color = circleBorderColor,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letters[idx],
                                fontSize = 10.sp,
                                fontWeight = if (isTravelDay) FontWeight.ExtraBold else FontWeight.Bold,
                                color = textColor,
                                textAlign = TextAlign.Center,
                                lineHeight = 10.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = onTrackClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RailBluePrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("🚆 Track Live", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun SwipeToDeleteRecentTrainItem(
    train: Train,
    isDark: Boolean,
    onTrackTrain: () -> Unit,
    onDelete: () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "swipe_offset"
    )
    val density = LocalDensity.current
    val deleteThresholdPx = with(density) { 76.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(14.dp))
    ) {
        // iOS-style Red Delete background revealed on right-to-left swipe
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFFEF4444))
                .clickable { onDelete() }
                .padding(end = 20.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🗑️", fontSize = 16.sp)
                Text(
                    text = "Delete",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        // Foreground card with swipe gesture
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(animatedOffsetX.roundToInt(), 0) }
                .pointerInput(train.number) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -deleteThresholdPx * 1.8f) {
                                onDelete()
                            } else if (offsetX < -deleteThresholdPx / 2f) {
                                offsetX = -deleteThresholdPx
                            } else {
                                offsetX = 0f
                            }
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(-deleteThresholdPx * 2.2f, 0f)
                        }
                    )
                }
                .clickable {
                    if (offsetX < -5f) {
                        offsetX = 0f
                    } else {
                        onTrackTrain()
                    }
                },
            shape = RoundedCornerShape(14.dp),
            color = if (isDark) Color(0xFF1E293B) else Color.White,
            border = BorderStroke(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
            ),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .background(RailBluePrimary.copy(alpha = 0.10f), RoundedCornerShape(8.dp))
                            .border(1.dp, RailBluePrimary.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = train.number,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = RailBluePrimary
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = train.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${train.origin.substringBefore(" (")} ➔ ${train.destination.substringBefore(" (")}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "Track →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RailBluePrimary
                )
            }
        }
    }
}
