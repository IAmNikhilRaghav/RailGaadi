package com.railgaadi.app.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.model.StationDataService
import com.railgaadi.app.model.StationInfo
import com.railgaadi.app.model.Train
import com.railgaadi.app.network.RailRadarService
import com.railgaadi.app.theme.*
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

fun generateTravelDates(startDate: Int = 7, startMonth: Int = 11, startYear: Int = 2026): List<RouteTravelDate> {
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

    val list = mutableListOf<RouteTravelDate>()
    var curDay = startDate
    var curMonth = startMonth
    var curYear = startYear

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

// Recent route search history entry
data class RecentRouteSearch(
    val fromLabel: String,
    val toLabel: String,
    val fromQuery: String,
    val toQuery: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(onTrackTrain: (Train, String) -> Unit = { _, _ -> }) {
    val service = remember { RailRadarService() }
    val coroutineScope = rememberCoroutineScope()

    var fromStationQuery by remember { mutableStateOf("") }
    var toStationQuery by remember { mutableStateOf("") }

    var isFromDropdownOpen by remember { mutableStateOf(false) }
    var isToDropdownOpen by remember { mutableStateOf(false) }

    // Travel Dates & Calendar
    val travelDates = remember { generateTravelDates(7, 11, 2026) }
    var selectedTravelDate by remember { mutableStateOf(travelDates.first { it.isToday }) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    // Show bottom sheet style: "Yesterday / Today / Tomorrow / Choose from Calendar"
    var showDateQuickPicker by remember { mutableStateOf(false) }

    // Recent search history (max 3)
    var recentRoutes by remember { mutableStateOf<List<RecentRouteSearch>>(emptyList()) }

    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Train>>(emptyList()) }
    var hasSearchedOnce by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun triggerSearch(fromText: String, toText: String, addToHistory: Boolean = true) {
        val cleanFrom = fromText.trim()
        val cleanTo = toText.trim()

        if (cleanFrom.isEmpty() || cleanTo.isEmpty()) {
            validationError = "Please enter both From and To stations"
            return
        }

        validationError = null
        val fCode = StationDataService.resolveStationCode(cleanFrom)
        val tCode = StationDataService.resolveStationCode(cleanTo)

        // Add to recent history
        if (addToHistory) {
            val entry = RecentRouteSearch(
                fromLabel = cleanFrom.substringBefore(" (").take(18),
                toLabel = cleanTo.substringBefore(" (").take(18),
                fromQuery = cleanFrom,
                toQuery = cleanTo
            )
            val already = recentRoutes.any { it.fromQuery == cleanFrom && it.toQuery == cleanTo }
            if (!already) {
                recentRoutes = (listOf(entry) + recentRoutes).take(3)
            }
        }

        coroutineScope.launch {
            isSearching = true
            val results = service.searchTrainsBetweenStations(fCode, tCode)
            searchResults = results
            hasSearchedOnce = true
            isSearching = false
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
        // 1. COMPACT Station-to-Station Search Card
        // ─────────────────────────────────────────────────────
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
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
                            value = fromStationQuery,
                            onValueChange = {
                                fromStationQuery = it
                                isFromDropdownOpen = true
                                isToDropdownOpen = false
                                validationError = null
                            },
                            label = { Text("From", fontSize = 12.sp) },
                            placeholder = { Text("Station name or code", color = Color.Gray, fontSize = 12.sp) },
                            trailingIcon = {
                                if (fromStationQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        fromStationQuery = ""
                                        isFromDropdownOpen = false
                                    }) {
                                        Text("✕", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
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
                                                    fromStationQuery = "${stn.name} (${stn.code})"
                                                    isFromDropdownOpen = false
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

                    // Swap button between From / To
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                val temp = fromStationQuery
                                fromStationQuery = toStationQuery
                                toStationQuery = temp
                                isFromDropdownOpen = false
                                isToDropdownOpen = false
                                if (fromStationQuery.isNotEmpty() && toStationQuery.isNotEmpty()) {
                                    triggerSearch(fromStationQuery, toStationQuery)
                                }
                            },
                            modifier = Modifier.size(32.dp).background(RailBluePrimary.copy(alpha = 0.1f), CircleShape)
                        ) {
                            Text("⇅", fontSize = 16.sp, color = RailBluePrimary, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // TO input
                    Column {
                        OutlinedTextField(
                            value = toStationQuery,
                            onValueChange = {
                                toStationQuery = it
                                isToDropdownOpen = true
                                isFromDropdownOpen = false
                                validationError = null
                            },
                            label = { Text("To", fontSize = 12.sp) },
                            placeholder = { Text("Station name or code", color = Color.Gray, fontSize = 12.sp) },
                            trailingIcon = {
                                if (toStationQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        toStationQuery = ""
                                        isToDropdownOpen = false
                                    }) {
                                        Text("✕", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
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
                                                    toStationQuery = "${stn.name} (${stn.code})"
                                                    isToDropdownOpen = false
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

                    // ── FIND TRAINS BUTTON – directly below To field ──
                    if (validationError != null) {
                        Text(
                            text = validationError ?: "",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Button(
                        onClick = {
                            isFromDropdownOpen = false
                            isToDropdownOpen = false
                            triggerSearch(fromStationQuery, toStationQuery)
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF044E42))
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Searching…", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                        } else {
                            Text("🔍  Find Trains", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Popular Corridors
                    Text("Popular Corridors:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(5.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        val popularCorridors = listOf("MTJ → JP", "NDLS → BSB", "NDLS → MMCT", "HWH → NDLS", "NDLS → BPL", "MMCT → ADI")
                        items(popularCorridors) { corridor ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .clickable {
                                        val parts = corridor.split(" → ")
                                        val fromStn = StationDataService.allStations.find { it.code == parts[0] }
                                        val toStn = StationDataService.allStations.find { it.code == parts[1] }
                                        fromStationQuery = if (fromStn != null) "${fromStn.name} (${fromStn.code})" else parts[0]
                                        toStationQuery = if (toStn != null) "${toStn.name} (${toStn.code})" else parts[1]
                                        isFromDropdownOpen = false; isToDropdownOpen = false; validationError = null
                                        triggerSearch(fromStationQuery, toStationQuery)
                                    }
                                    .padding(horizontal = 9.dp, vertical = 5.dp)
                            ) {
                                Text(corridor, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)
                            }
                        }
                    }
                }
            }
        }

        // ─────────────────────────────────────────────────────
        // 2. RECENT SEARCHES – max 3, with 🗑 delete per row
        // ─────────────────────────────────────────────────────
        if (recentRoutes.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Text(
                        "Recent Searches",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    recentRoutes.forEachIndexed { index, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    if (index == 0 && recentRoutes.size == 1) RoundedCornerShape(12.dp)
                                    else if (index == 0) RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                                    else if (index == recentRoutes.size - 1) RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                                    else RoundedCornerShape(0.dp)
                                )
                                .border(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    if (index == 0 && recentRoutes.size == 1) RoundedCornerShape(12.dp)
                                    else if (index == 0) RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                                    else if (index == recentRoutes.size - 1) RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                                    else RoundedCornerShape(0.dp)
                                )
                                .clickable {
                                    fromStationQuery = entry.fromQuery
                                    toStationQuery = entry.toQuery
                                    isFromDropdownOpen = false; isToDropdownOpen = false
                                    triggerSearch(entry.fromQuery, entry.toQuery, addToHistory = false)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🕒", fontSize = 14.sp)
                                Column {
                                    Text(
                                        "${entry.fromLabel}  →  ${entry.toLabel}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            // 🗑 Red bin delete button
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(Color(0xFFFEE2E2), CircleShape)
                                    .clickable {
                                        recentRoutes = recentRoutes.toMutableList().also { it.removeAt(index) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🗑", fontSize = 13.sp)
                            }
                        }
                        if (index < recentRoutes.size - 1) {
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 0.5.dp)
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
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        val categories = listOf("All", "Vande Bharat", "Rajdhani", "Shatabdi", "Superfast", "Express", "Passenger")
                        items(categories) { cat ->
                            val isSelected = selectedCategoryFilter == cat
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) RailBluePrimary else MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(18.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) RailBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        RoundedCornerShape(18.dp)
                                    )
                                    .clickable { selectedCategoryFilter = cat }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    cat,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
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
            .background(Color(0xFFF8FAFC), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            .padding(vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 12.sp)
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
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
                    val canGoBack = !(viewingMonth == 11 && viewingYear == 2026)
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

                    val canGoForward = !(viewingMonth == 2 && viewingYear == 2027)
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
            val rows = (firstOffset + daysInViewingMonth + 6) / 7
            val todayDay = 8; val todayMonth = 11; val todayYear = 2026

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
                                val isPast = (viewingMonth == todayMonth && viewingYear == todayYear && dayNum < (todayDay - 1))
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
                                            }
                                            if (matched != null) onDateSelected(matched)
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
// Train Card Item with Date Specific Running Verification
// ─────────────────────────────────────────────────────────────────
@Composable
fun TrainCardItem(
    train: Train,
    selectedDate: RouteTravelDate,
    onTrackClick: () -> Unit
) {
    val dayIndex = selectedDate.dayOfWeekIndex
    val runsOnSelectedDate = train.runsOnDays.getOrElse(dayIndex) { true }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
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
                        modifier = Modifier.background(Color(0xFF0F172A), RoundedCornerShape(5.dp)).padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(train.number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Text(train.name, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Box(
                    modifier = Modifier
                        .background(
                            when {
                                train.type.contains("Vande Bharat", ignoreCase = true) -> Color(0xFFEFF6FF)
                                train.type.contains("Rajdhani", ignoreCase = true) -> Color(0xFFFEF2F2)
                                train.type.contains("Shatabdi", ignoreCase = true) -> Color(0xFFF0FDF4)
                                train.type.contains("Superfast", ignoreCase = true) -> Color(0xFFFAF5FF)
                                else -> Color(0xFFF8FAFC)
                            },
                            RoundedCornerShape(7.dp)
                        )
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        train.type, fontWeight = FontWeight.Bold, fontSize = 10.sp,
                        color = when {
                            train.type.contains("Vande Bharat", ignoreCase = true) -> Color(0xFF1D4ED8)
                            train.type.contains("Rajdhani", ignoreCase = true) -> Color(0xFFB91C1C)
                            train.type.contains("Shatabdi", ignoreCase = true) -> Color(0xFF15803D)
                            train.type.contains("Superfast", ignoreCase = true) -> Color(0xFF7C3AED)
                            else -> Color(0xFF475569)
                        }
                    )
                }
            }

            // Date-specific run badge
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .background(
                        if (runsOnSelectedDate) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                        RoundedCornerShape(5.dp)
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    if (runsOnSelectedDate) "✓ Runs on ${selectedDate.dayOfWeek}, ${selectedDate.dayNumber} ${selectedDate.monthName}"
                    else "✕ Doesn't run on ${selectedDate.dayOfWeek}, ${selectedDate.dayNumber} ${selectedDate.monthName}",
                    fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    color = if (runsOnSelectedDate) RailGreenOnTime else Color(0xFFDC2626)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timings
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    val letters = listOf("S","M","T","W","T","F","S")
                    val runsList = if (train.runsOnDays.size == 7) train.runsOnDays else List(7) { true }
                    runsList.forEachIndexed { idx, runs ->
                        val isTravelDay = (idx == dayIndex)
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .background(if (runs) RailBluePrimary else Color(0xFFE2E8F0), CircleShape)
                                .border(if (isTravelDay) 2.dp else 0.dp, if (isTravelDay) Color(0xFF044E42) else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(letters[idx], fontSize = 9.sp, fontWeight = if (isTravelDay) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (runs) Color.White else Color.Gray)
                        }
                    }
                }

                Button(
                    onClick = onTrackClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RailBluePrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text("🚆 Track Live", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
