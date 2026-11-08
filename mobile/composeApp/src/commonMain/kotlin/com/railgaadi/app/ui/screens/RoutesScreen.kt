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
    val monthNumber: Int, // 1..12
    val monthName: String,
    val year: Int,
    val dayOfWeek: String,       // "Sun", "Mon", "Tue", ...
    val dayOfWeekFull: String,   // "Sunday", "Monday", ...
    val dayOfWeekIndex: Int,     // 0 = Sun, 1 = Mon, 2 = Tue, 3 = Wed, 4 = Thu, 5 = Fri, 6 = Sat
    val isToday: Boolean = false,
    val isTomorrow: Boolean = false
) {
    val displayLabel: String
        get() = when {
            isToday -> "Today, $dayNumber $monthName"
            isTomorrow -> "Tomorrow, $dayNumber $monthName"
            else -> "$dayOfWeek, $dayNumber $monthName"
        }

    val fullFormatted: String
        get() = "$dayOfWeekFull, $dayNumber $monthName $year"
}

fun generateTravelDates(startDate: Int = 8, startMonth: Int = 11, startYear: Int = 2026): List<RouteTravelDate> {
    val monthNames = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val monthFullNames = listOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    val monthDays = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val dayFullNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

    val list = mutableListOf<RouteTravelDate>()
    var curDay = startDate
    var curMonth = startMonth
    var curYear = startYear
    var curDayOfWeek = 0 // Nov 8, 2026 is Sunday

    for (i in 0 until 60) {
        list.add(
            RouteTravelDate(
                dayNumber = curDay,
                monthNumber = curMonth,
                monthName = monthNames[curMonth],
                year = curYear,
                dayOfWeek = dayNames[curDayOfWeek],
                dayOfWeekFull = dayFullNames[curDayOfWeek],
                dayOfWeekIndex = curDayOfWeek,
                isToday = (i == 0),
                isTomorrow = (i == 1)
            )
        )

        curDayOfWeek = (curDayOfWeek + 1) % 7
        curDay++
        val maxDays = monthDays[curMonth]
        if (curDay > maxDays) {
            curDay = 1
            curMonth++
            if (curMonth > 12) {
                curMonth = 1
                curYear++
            }
        }
    }
    return list
}

@Composable
fun RoutesScreen(onTrackTrain: (Train, String) -> Unit = { _, _ -> }) {
    val service = remember { RailRadarService() }
    val coroutineScope = rememberCoroutineScope()

    // 1. Clean & Empty by default per user request
    var fromStationQuery by remember { mutableStateOf("") }
    var toStationQuery by remember { mutableStateOf("") }

    var isFromDropdownOpen by remember { mutableStateOf(false) }
    var isToDropdownOpen by remember { mutableStateOf(false) }

    // 2. Travel Dates & Interactive Calendar State
    val travelDates = remember { generateTravelDates(8, 11, 2026) }
    var selectedTravelDate by remember { mutableStateOf(travelDates.first()) }
    var showCalendarDialog by remember { mutableStateOf(false) }

    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Train>>(emptyList()) }
    var hasSearchedOnce by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    // Dynamic Station-to-Station search
    fun triggerSearch(fromText: String, toText: String) {
        val cleanFrom = fromText.trim()
        val cleanTo = toText.trim()

        if (cleanFrom.isEmpty() || cleanTo.isEmpty()) {
            validationError = "Please enter both From and To stations to search trains"
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

    var liveFromSuggestions by remember { mutableStateOf<List<StationInfo>>(emptyList()) }
    var liveToSuggestions by remember { mutableStateOf<List<StationInfo>>(emptyList()) }

    // Live Pan-India Station Search for 'From'
    LaunchedEffect(fromStationQuery) {
        val q = fromStationQuery.trim()
        if (q.length >= 2 && !q.contains("(")) {
            delay(150)
            val apiStations = service.searchStations(q)
            if (apiStations.isNotEmpty()) {
                liveFromSuggestions = apiStations
            }
        } else {
            liveFromSuggestions = emptyList()
        }
    }

    // Live Pan-India Station Search for 'To'
    LaunchedEffect(toStationQuery) {
        val q = toStationQuery.trim()
        if (q.length >= 2 && !q.contains("(")) {
            delay(150)
            val apiStations = service.searchStations(q)
            if (apiStations.isNotEmpty()) {
                liveToSuggestions = apiStations
            }
        } else {
            liveToSuggestions = emptyList()
        }
    }

    val fromSuggestions = remember(fromStationQuery, liveFromSuggestions) {
        if (fromStationQuery.isBlank()) emptyList()
        else {
            val local = StationDataService.searchStations(fromStationQuery)
            (liveFromSuggestions + local).distinctBy { it.code.uppercase() }.take(15)
        }
    }

    val toSuggestions = remember(toStationQuery, liveToSuggestions) {
        if (toStationQuery.isBlank()) emptyList()
        else {
            val local = StationDataService.searchStations(toStationQuery)
            (liveToSuggestions + local).distinctBy { it.code.uppercase() }.take(15)
        }
    }

    // Filter trains dynamically by category
    val filteredTrains = remember(searchResults, selectedCategoryFilter) {
        if (selectedCategoryFilter == "All") {
            searchResults
        } else {
            searchResults.filter { train ->
                train.type.contains(selectedCategoryFilter, ignoreCase = true) ||
                train.name.contains(selectedCategoryFilter, ignoreCase = true)
            }
        }
    }

    // Interactive Monthly Calendar Dialog
    if (showCalendarDialog) {
        CalendarPickerDialog(
            currentSelected = selectedTravelDate,
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Station-to-Station Search Card (Modern Minimalist & Modular)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Station to Station Route Search",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // From Station Input
                    Column {
                        OutlinedTextField(
                            value = fromStationQuery,
                            onValueChange = {
                                fromStationQuery = it
                                isFromDropdownOpen = true
                                isToDropdownOpen = false
                                validationError = null
                            },
                            label = { Text("From") },
                            placeholder = { Text("Enter station or code", color = Color.Gray, fontSize = 13.sp) },
                            trailingIcon = {
                                if (fromStationQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        fromStationQuery = ""
                                        isFromDropdownOpen = false
                                    }) {
                                        Text("✕", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        // Autocomplete Dropdown for From Station
                        AnimatedVisibility(
                            visible = isFromDropdownOpen && fromSuggestions.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .heightIn(max = 200.dp)
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
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    stn.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (stn.city.isNotEmpty()) {
                                                    Text(
                                                        "${stn.city}, ${stn.state}",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    stn.code,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = RailBluePrimary
                                                )
                                            }
                                        }
                                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                                    }
                                }
                            }
                        }
                    }

                    // Swap Button
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                            modifier = Modifier
                                .size(36.dp)
                                .background(RailBluePrimary.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Text("⇅", fontSize = 18.sp, color = RailBluePrimary, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // To Station Input
                    Column {
                        OutlinedTextField(
                            value = toStationQuery,
                            onValueChange = {
                                toStationQuery = it
                                isToDropdownOpen = true
                                isFromDropdownOpen = false
                                validationError = null
                            },
                            label = { Text("To") },
                            placeholder = { Text("Enter station or code", color = Color.Gray, fontSize = 13.sp) },
                            trailingIcon = {
                                if (toStationQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        toStationQuery = ""
                                        isToDropdownOpen = false
                                    }) {
                                        Text("✕", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            singleLine = true
                        )

                        // Autocomplete Dropdown for To Station
                        AnimatedVisibility(
                            visible = isToDropdownOpen && toSuggestions.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .heightIn(max = 200.dp)
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
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    stn.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                if (stn.city.isNotEmpty()) {
                                                    Text(
                                                        "${stn.city}, ${stn.state}",
                                                        fontSize = 11.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .background(Color(0xFFE2E8F0), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    stn.code,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = RailBluePrimary
                                                )
                                            }
                                        }
                                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // -------------------------------------------------------------
                    // 📅 Date of Journey & Calendar Feature (Seamless Modular Strip)
                    // -------------------------------------------------------------
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date of Journey",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        // Clickable Date Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .background(RailBluePrimary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                .clickable { showCalendarDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("📅", fontSize = 12.sp)
                            Text(
                                selectedTravelDate.fullFormatted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RailBluePrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Dates Horizontal Selector
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // First 4 upcoming dates
                        items(travelDates.take(4)) { tDate ->
                            val isSelected = (selectedTravelDate.dayNumber == tDate.dayNumber && selectedTravelDate.monthNumber == tDate.monthNumber)
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) RailBluePrimary else Color(0xFFF1F5F9),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedTravelDate = tDate }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    tDate.displayLabel,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF334155)
                                )
                            }
                        }

                        // Pick Date / Calendar Action Pill
                        item {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
                                    .clickable { showCalendarDialog = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("🗓️", fontSize = 11.sp)
                                    Text("Calendar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Validation Message Hint (if user taps search without stations)
                    if (validationError != null) {
                        Text(
                            text = validationError ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    // Find Trains Action Button
                    Button(
                        onClick = {
                            isFromDropdownOpen = false
                            isToDropdownOpen = false
                            triggerSearch(fromStationQuery, toStationQuery)
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF044E42))
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Searching Pan-India Trains…",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🔍", fontSize = 16.sp)
                                Text(
                                    "Find Trains",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Popular Corridors Chips
                    Text("Popular Corridors:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val popularCorridors = listOf(
                            "MTJ → JP",
                            "NDLS → BSB",
                            "NDLS → MMCT",
                            "HWH → NDLS",
                            "NDLS → BPL",
                            "MMCT → ADI"
                        )
                        items(popularCorridors) { corridor ->
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFF1F5F9), RoundedCornerShape(10.dp))
                                    .clickable {
                                        val parts = corridor.split(" → ")
                                        val fromCode = parts[0]
                                        val toCode = parts[1]
                                        val fromStn = StationDataService.allStations.find { it.code == fromCode }
                                        val toStn = StationDataService.allStations.find { it.code == toCode }

                                        fromStationQuery = if (fromStn != null) "${fromStn.name} (${fromStn.code})" else fromCode
                                        toStationQuery = if (toStn != null) "${toStn.name} (${toStn.code})" else toCode

                                        isFromDropdownOpen = false
                                        isToDropdownOpen = false
                                        validationError = null
                                        triggerSearch(fromStationQuery, toStationQuery)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(corridor, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)
                            }
                        }
                    }
                }
            }
        }

        // 2. Category Filters (All, Vande Bharat, Rajdhani, Shatabdi, Superfast, Express, Passenger)
        if (searchResults.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "${I18n.t("rs_filter_by")} Category",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val categories = listOf("All", "Vande Bharat", "Rajdhani", "Shatabdi", "Superfast", "Express", "Passenger")
                        items(categories) { cat ->
                            val isSelected = selectedCategoryFilter == cat
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSelected) RailBluePrimary else MaterialTheme.colorScheme.surface,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) RailBluePrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedCategoryFilter = cat }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    cat,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Train Results Header
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
                        text = if (isSearching) "Searching trains…" else "${filteredTrains.size} Direct Trains Found ($fCode → $tCode)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = selectedTravelDate.displayLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RailBluePrimary
                    )
                }
            }
        }

        // 4. Initial Welcoming State (When user opens tab before searching)
        if (!hasSearchedOnce) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🗺️", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "Plan Your Train Journey",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Search direct trains between any of the 8,690+ Indian Railway stations, pick your departure date, and track live schedules.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FeaturePill(icon = "⚡", label = "Live GPS", modifier = Modifier.weight(1f))
                            FeaturePill(icon = "⏱️", label = "Delays", modifier = Modifier.weight(1f))
                            FeaturePill(icon = "📅", label = "Calendar", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // 5. Train Cards List or No Results State
        if (filteredTrains.isEmpty() && !isSearching && hasSearchedOnce) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("🚆", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No Direct Trains Found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Try searching with popular junction station codes such as MTJ, JP, NDLS, AGC, or MMCT.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
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
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(icon, fontSize = 13.sp)
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
        }
    }
}

// -------------------------------------------------------------
// Interactive Calendar Dialog for Journey Planning
// -------------------------------------------------------------
@Composable
fun CalendarPickerDialog(
    currentSelected: RouteTravelDate,
    allDates: List<RouteTravelDate>,
    onDismiss: () -> Unit,
    onDateSelected: (RouteTravelDate) -> Unit
) {
    var viewingMonth by remember { mutableStateOf(currentSelected.monthNumber) }
    var viewingYear by remember { mutableStateOf(currentSelected.year) }

    val monthTitle = when (viewingMonth) {
        11 -> "November 2026"
        12 -> "December 2026"
        1 -> "January 2027"
        else -> "Month $viewingMonth $viewingYear"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", fontWeight = FontWeight.Bold, color = RailBluePrimary)
            }
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Journey Date", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                IconButton(onClick = onDismiss) {
                    Text("✕", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                // Month Header with Prev/Next
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (viewingMonth > 11 || viewingYear > 2026) {
                                if (viewingMonth == 1) {
                                    viewingMonth = 12
                                    viewingYear--
                                } else {
                                    viewingMonth--
                                }
                            }
                        },
                        enabled = !(viewingMonth == 11 && viewingYear == 2026)
                    ) {
                        Text(
                            "‹",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewingMonth == 11 && viewingYear == 2026) Color.LightGray else RailBluePrimary
                        )
                    }

                    Text(
                        monthTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = {
                            if (viewingMonth == 12) {
                                viewingMonth = 1
                                viewingYear++
                            } else if (viewingMonth < 12) {
                                viewingMonth++
                            }
                        },
                        enabled = viewingMonth < 12 || viewingYear == 2026
                    ) {
                        Text("›", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RailBluePrimary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Weekday header: Su Mo Tu We Th Fr Sa
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach { d ->
                        Text(
                            d,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray,
                            modifier = Modifier.width(34.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar Grid
                val daysInMonth = if (viewingMonth == 11) 30 else 31
                val firstDayOffset = if (viewingMonth == 11) 0 else if (viewingMonth == 12) 2 else 5

                val totalCells = firstDayOffset + daysInMonth
                val rows = (totalCells + 6) / 7

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (row in 0 until rows) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                            for (col in 0 until 7) {
                                val cellIndex = row * 7 + col
                                val dayNum = cellIndex - firstDayOffset + 1
                                if (cellIndex < firstDayOffset || dayNum > daysInMonth) {
                                    Box(modifier = Modifier.size(34.dp))
                                } else {
                                    val isPast = (viewingMonth == 11 && viewingYear == 2026 && dayNum < 8)
                                    val isSelected = (currentSelected.dayNumber == dayNum && currentSelected.monthNumber == viewingMonth && currentSelected.year == viewingYear)
                                    val isToday = (viewingMonth == 11 && viewingYear == 2026 && dayNum == 8)

                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(
                                                when {
                                                    isSelected -> RailBluePrimary
                                                    isToday -> RailBluePrimary.copy(alpha = 0.12f)
                                                    else -> Color.Transparent
                                                },
                                                CircleShape
                                            )
                                            .border(
                                                width = if (isToday && !isSelected) 1.dp else 0.dp,
                                                color = if (isToday && !isSelected) RailBluePrimary else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable(enabled = !isPast) {
                                                val matched = allDates.find { it.dayNumber == dayNum && it.monthNumber == viewingMonth && it.year == viewingYear }
                                                if (matched != null) {
                                                    onDateSelected(matched)
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = when {
                                                isSelected -> Color.White
                                                isPast -> Color.LightGray
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
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

// -------------------------------------------------------------
// Train Card Item with Date Specific Running Verification
// -------------------------------------------------------------
@Composable
fun TrainCardItem(
    train: Train,
    selectedDate: RouteTravelDate,
    onTrackClick: () -> Unit
) {
    val dayIndex = selectedDate.dayOfWeekIndex // 0 = Sun..6 = Sat
    val runsOnSelectedDate = train.runsOnDays.getOrElse(dayIndex) { true }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Number, Name & Category Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(train.number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(
                        train.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        train.type,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
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

            // Running verification pill on selected date
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (runsOnSelectedDate) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        if (runsOnSelectedDate) "✓ Runs on ${selectedDate.dayOfWeek}, ${selectedDate.dayNumber} ${selectedDate.monthName}"
                        else "✕ Doesn't run on ${selectedDate.dayOfWeek}, ${selectedDate.dayNumber} ${selectedDate.monthName}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (runsOnSelectedDate) RailGreenOnTime else Color(0xFFDC2626)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timings Row: Dep Time -> Duration -> Arr Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Departure Station & Time
                Column {
                    Text(
                        train.departureTime,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        train.origin.substringBefore(" ("),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Duration Indicator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val hrs = train.durationMinutes / 60
                    val mins = train.durationMinutes % 60
                    val durText = if (hrs > 0) "${hrs}h ${mins}m" else if (mins > 0) "${mins}m" else "Direct"
                    Text(durText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(RailBluePrimary, CircleShape))
                        HorizontalDivider(modifier = Modifier.width(55.dp), thickness = 2.dp, color = RailBluePrimary)
                        Box(modifier = Modifier.size(6.dp).background(RailBluePrimary, CircleShape))
                    }
                    Text(if (train.platform.isNotEmpty()) "PF ${train.platform}" else "Daily", fontSize = 10.sp, color = Color.Gray)
                }

                // Arrival Station & Time
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        train.arrivalTime,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        train.destination.substringBefore(" ("),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Running Days Row (S M T W T F S) with highlighted selected travel day
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")
                    val runsList = if (train.runsOnDays.size == 7) train.runsOnDays else List(7) { true }
                    runsList.forEachIndexed { idx, runs ->
                        val isTravelDay = (idx == dayIndex)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (runs) RailBluePrimary else Color(0xFFE2E8F0),
                                    CircleShape
                                )
                                .border(
                                    width = if (isTravelDay) 2.dp else 0.dp,
                                    color = if (isTravelDay) Color(0xFF044E42) else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                dayLetters[idx],
                                fontSize = 10.sp,
                                fontWeight = if (isTravelDay) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (runs) Color.White else Color.Gray
                            )
                        }
                    }
                }

                // Track Live Button -> Directly opens train in Live Map tab
                Button(
                    onClick = onTrackClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RailBluePrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text("🚆 Track Live", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
