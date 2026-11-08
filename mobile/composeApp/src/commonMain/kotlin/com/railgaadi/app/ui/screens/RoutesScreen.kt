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

@Composable
fun RoutesScreen(onTrackTrain: (Train) -> Unit = {}) {
    val service = remember { RailRadarService() }
    val coroutineScope = rememberCoroutineScope()

    var fromStationQuery by remember { mutableStateOf("Mathura Jn (MTJ)") }
    var toStationQuery by remember { mutableStateOf("Jaipur Jn (JP)") }

    var isFromDropdownOpen by remember { mutableStateOf(false) }
    var isToDropdownOpen by remember { mutableStateOf(false) }

    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Train>>(emptyList()) }
    var hasSearchedOnce by remember { mutableStateOf(false) }

    // Execute dynamic search between stations
    fun triggerSearch(fromText: String, toText: String) {
        val fCode = StationDataService.resolveStationCode(fromText)
        val tCode = StationDataService.resolveStationCode(toText)

        coroutineScope.launch {
            isSearching = true
            val results = service.searchTrainsBetweenStations(fCode, tCode)
            searchResults = results
            hasSearchedOnce = true
            isSearching = false
        }
    }

    // Auto-search on initial launch with default corridor
    LaunchedEffect(Unit) {
        triggerSearch(fromStationQuery, toStationQuery)
    }

    var liveFromSuggestions by remember { mutableStateOf<List<StationInfo>>(emptyList()) }
    var liveToSuggestions by remember { mutableStateOf<List<StationInfo>>(emptyList()) }

    // Live Pan-India Station Search for 'From' Input (matches all 8,690+ local stations)
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

    // Live Pan-India Station Search for 'To' Input (matches all 8,690+ local stations)
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
        val local = StationDataService.searchStations(fromStationQuery)
        (liveFromSuggestions + local).distinctBy { it.code.uppercase() }.take(15)
    }

    val toSuggestions = remember(toStationQuery, liveToSuggestions) {
        val local = StationDataService.searchStations(toStationQuery)
        (liveToSuggestions + local).distinctBy { it.code.uppercase() }.take(15)
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Station-to-Station Search Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Station to Station Route Search",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // From Station Input
                    Column {
                        OutlinedTextField(
                            value = fromStationQuery,
                            onValueChange = {
                                fromStationQuery = it
                                isFromDropdownOpen = true
                                isToDropdownOpen = false
                            },
                            label = { Text("From Station") },
                            trailingIcon = {
                                if (fromStationQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        fromStationQuery = ""
                                        isFromDropdownOpen = true
                                    }) {
                                        Text("✕", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
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
                                triggerSearch(fromStationQuery, toStationQuery)
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
                            },
                            label = { Text("To Station") },
                            trailingIcon = {
                                if (toStationQuery.isNotEmpty()) {
                                    IconButton(onClick = {
                                        toStationQuery = ""
                                        isToDropdownOpen = true
                                    }) {
                                        Text("✕", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
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

        // 3. Train Results Header
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
                    text = "Weekly Schedule",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }

        // 4. Train Cards List or Empty State
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
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        } else {
            items(filteredTrains) { train ->
                TrainCardItem(train = train, onTrackClick = { onTrackTrain(train) })
            }
        }
    }
}

@Composable
fun TrainCardItem(train: Train, onTrackClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                        maxLines = 1
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

            Spacer(modifier = Modifier.height(14.dp))

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
                        maxLines = 1
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
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Running Days Row (S M T W T F S) & Track Live Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")
                    val runsList = if (train.runsOnDays.size == 7) train.runsOnDays else List(7) { true }
                    runsList.forEachIndexed { idx, runs ->
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    if (runs) RailBluePrimary else Color(0xFFE2E8F0),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                dayLetters[idx],
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
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
