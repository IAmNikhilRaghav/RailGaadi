package com.railgaadi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.model.StationBoardTrain
import com.railgaadi.app.model.StationDataService
import com.railgaadi.app.model.StationInfo
import com.railgaadi.app.model.Train
import com.railgaadi.app.network.RailRadarService
import com.railgaadi.app.state.TrainTrackerState
import com.railgaadi.app.theme.*
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.launch

enum class StationBoardMode {
    DEPARTURES, ARRIVALS
}

@Composable
fun StationBoardScreen(
    onTrackTrain: (Train) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val railRadarService = remember { RailRadarService() }

    // Popular railway junction suggestions for quick 1-tap switching
    val topJunctions = remember {
        listOf(
            "NDLS" to "New Delhi",
            "MTJ" to "Mathura",
            "JP" to "Jaipur",
            "HWH" to "Howrah",
            "CSMT" to "Mumbai",
            "CNB" to "Kanpur",
            "BSB" to "Varanasi",
            "PRYJ" to "Prayagraj",
            "MAS" to "Chennai",
            "BPL" to "Bhopal"
        )
    }

    // Default station: use boarding station if available, or NDLS
    var currentStationCode by remember {
        mutableStateOf(TrainTrackerState.userBoardingStation?.code ?: "NDLS")
    }
    var currentStationName by remember {
        mutableStateOf(TrainTrackerState.userBoardingStation?.name ?: "New Delhi")
    }

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchDropdownOpen by remember { mutableStateOf(false) }
    var searchSuggestions by remember { mutableStateOf<List<StationInfo>>(emptyList()) }

    // Board filter state
    var boardMode by remember { mutableStateOf(StationBoardMode.DEPARTURES) }
    var selectedCategory by remember { mutableStateOf("All") }

    // Data state
    var allTrains by remember { mutableStateOf<List<StationBoardTrain>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Function to load station board data
    fun loadBoardForStation(code: String, name: String) {
        currentStationCode = code
        currentStationName = name
        searchQuery = ""
        isSearchDropdownOpen = false
        scope.launch {
            isLoading = true
            try {
                val fetched = railRadarService.fetchStationBoard(code)
                allTrains = fetched
            } catch (_: Exception) {
                // Handled in fallback
            } finally {
                isLoading = false
            }
        }
    }

    // Initial load
    LaunchedEffect(currentStationCode) {
        loadBoardForStation(currentStationCode, currentStationName)
    }

    // Handle station search debouncing
    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (q.length >= 2) {
            delay(150.milliseconds)
            searchSuggestions = StationDataService.searchStations(q)
            isSearchDropdownOpen = searchSuggestions.isNotEmpty()
        } else {
            searchSuggestions = emptyList()
            isSearchDropdownOpen = false
        }
    }

    // Filter trains by mode (Departures vs Arrivals) and Category
    val filteredTrains = remember(allTrains, boardMode, selectedCategory) {
        allTrains.filter { train ->
            val hasScheduledTime = when (boardMode) {
                StationBoardMode.DEPARTURES -> !train.scheduledDep.isNullOrBlank() && train.scheduledDep != "—"
                StationBoardMode.ARRIVALS -> !train.scheduledArr.isNullOrBlank() && train.scheduledArr != "—"
            }
            if (!hasScheduledTime) return@filter false

            if (selectedCategory == "All") return@filter true

            when (selectedCategory) {
                "Vande Bharat" -> train.type.contains("Vande Bharat", ignoreCase = true) || train.name.contains("Vande Bharat", ignoreCase = true)
                "Rajdhani" -> train.type.contains("Rajdhani", ignoreCase = true) || train.name.contains("Rajdhani", ignoreCase = true)
                "Shatabdi" -> train.type.contains("Shatabdi", ignoreCase = true) || train.name.contains("Shatabdi", ignoreCase = true)
                "Superfast" -> train.type.contains("Superfast", ignoreCase = true) || train.type.contains("SF", ignoreCase = true) || train.name.contains("SF", ignoreCase = true)
                "Express" -> train.type.contains("Express", ignoreCase = true) || train.type.contains("Mail", ignoreCase = true)
                "Local/EMU" -> train.type.contains("Passenger", ignoreCase = true) || train.type.contains("Local", ignoreCase = true) || train.type.contains("EMU", ignoreCase = true)
                else -> true
            }
        }.sortedBy { train ->
            when (boardMode) {
                StationBoardMode.DEPARTURES -> train.scheduledDep ?: "99:99"
                StationBoardMode.ARRIVALS -> train.scheduledArr ?: "99:99"
            }
        }
    }

    val departuresCount = remember(allTrains) {
        allTrains.count { !it.scheduledDep.isNullOrBlank() && it.scheduledDep != "—" }
    }
    val arrivalsCount = remember(allTrains) {
        allTrains.count { !it.scheduledArr.isNullOrBlank() && it.scheduledArr != "—" }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DarkBackground else LightBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // -------------------------------------------------------------
            // 1. TOP HEADER & SEARCH CARD
            // -------------------------------------------------------------
            item {
                Card(
                    shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) DarkSurface else LightSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Title row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🚉 " + I18n.t("tab_station") + " Board",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                                )
                                Text(
                                    text = "Live Pan-India Departures & Arrivals",
                                    fontSize = 12.sp,
                                    color = if (isDark) DarkTextSecondary else LightTextSecondary
                                )
                            }

                            // Refresh Live Feed Button
                            IconButton(
                                onClick = { loadBoardForStation(currentStationCode, currentStationName) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(
                                        color = RailBluePrimary.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    )
                            ) {
                                Text("🔄", fontSize = 16.sp)
                            }
                        }

                        // Search Station Box
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = {
                                    Text(
                                        "Search station (e.g. NDLS, MTJ, HWH)...",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                },
                                leadingIcon = {
                                    Text("🔍", fontSize = 15.sp)
                                },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        Text(
                                            "✕",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Gray,
                                            modifier = Modifier
                                                .clickable { searchQuery = "" }
                                                .padding(8.dp)
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RailBluePrimary,
                                    unfocusedBorderColor = if (isDark) DarkBorder else LightBorder,
                                    focusedContainerColor = if (isDark) DarkBackground else Color(0xFFF1F5F9),
                                    unfocusedContainerColor = if (isDark) DarkBackground else Color(0xFFF8FAFC)
                                ),
                                singleLine = true
                            )
                        }

                        // Autocomplete Suggestions Dropdown
                        if (isSearchDropdownOpen && searchSuggestions.isNotEmpty()) {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) DarkSurface else LightSurface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, RailBluePrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(4.dp)) {
                                    searchSuggestions.take(6).forEach { station ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    loadBoardForStation(station.code, station.name)
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(RailBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        station.code,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp,
                                                        color = RailBluePrimary
                                                    )
                                                }
                                                Text(
                                                    station.name,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (isDark) DarkTextPrimary else LightTextPrimary
                                                )
                                            }

                                            if (station.state.isNotBlank()) {
                                                Text(
                                                    station.state,
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = if (isDark) DarkBorder.copy(alpha = 0.5f) else LightBorder.copy(alpha = 0.5f))
                                    }
                                }
                            }
                        }

                        // Quick Boarding Station Shortcut (if set in state)
                        TrainTrackerState.userBoardingStation?.let { boardingStn ->
                            if (boardingStn.code != currentStationCode) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            loadBoardForStation(boardingStn.code, boardingStn.name)
                                        }
                                        .background(
                                            color = RailGreenOnTime.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = RailGreenOnTime.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("📍", fontSize = 13.sp)
                                    Text(
                                        text = "Your Boarding Station: ${boardingStn.name} (${boardingStn.code})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = RailGreenOnTime
                                    )
                                    Spacer(modifier = Modifier.weight(1f))
                                    Text(
                                        text = "View Board →",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RailGreenOnTime
                                    )
                                }
                            }
                        }

                        // Popular Junction Quick Chips
                        Text(
                            text = "Major Junctions:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) DarkTextSecondary else LightTextSecondary
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(topJunctions) { (code, name) ->
                                val isSelected = code == currentStationCode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { loadBoardForStation(code, name) }
                                        .background(
                                            if (isSelected) RailBluePrimary
                                            else if (isDark) DarkBackground
                                            else Color(0xFFF1F5F9)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) RailBluePrimary else if (isDark) DarkBorder else LightBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "$code • $name",
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else if (isDark) DarkTextPrimary else LightTextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 2. ACTIVE STATION OVERVIEW BANNER
            // -------------------------------------------------------------
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else LightSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(
                                        Brush.linearGradient(listOf(RailBluePrimary, RailBlueDark)),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🚉", fontSize = 22.sp)
                            }

                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentStationName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) DarkTextPrimary else LightTextPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(RailBluePrimary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = currentStationCode,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RailBluePrimary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(RailGreenOnTime, CircleShape)
                                    )
                                    Text(
                                        text = "Live RailRadar Timetable",
                                        fontSize = 11.sp,
                                        color = RailGreenOnTime,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Summary pill
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${filteredTrains.size} Trains",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = RailBluePrimary
                            )
                            Text(
                                text = if (boardMode == StationBoardMode.DEPARTURES) "Departing" else "Arriving",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 3. DUAL-MODE TOGGLE (DEPARTURES VS ARRIVALS)
            // -------------------------------------------------------------
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(
                            if (isDark) DarkSurface else Color(0xFFE2E8F0),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Departures Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { boardMode = StationBoardMode.DEPARTURES }
                            .background(
                                if (boardMode == StationBoardMode.DEPARTURES) RailBluePrimary
                                else Color.Transparent
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🛫 Departures",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (boardMode == StationBoardMode.DEPARTURES) Color.White else if (isDark) DarkTextSecondary else LightTextSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (boardMode == StationBoardMode.DEPARTURES) Color.White.copy(alpha = 0.25f)
                                        else Color.Gray.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$departuresCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (boardMode == StationBoardMode.DEPARTURES) Color.White else if (isDark) DarkTextPrimary else LightTextPrimary
                                )
                            }
                        }
                    }

                    // Arrivals Pill
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { boardMode = StationBoardMode.ARRIVALS }
                            .background(
                                if (boardMode == StationBoardMode.ARRIVALS) RailBluePrimary
                                else Color.Transparent
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🛬 Arrivals",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (boardMode == StationBoardMode.ARRIVALS) Color.White else if (isDark) DarkTextSecondary else LightTextSecondary
                            )
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (boardMode == StationBoardMode.ARRIVALS) Color.White.copy(alpha = 0.25f)
                                        else Color.Gray.copy(alpha = 0.2f),
                                        CircleShape
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$arrivalsCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (boardMode == StationBoardMode.ARRIVALS) Color.White else if (isDark) DarkTextPrimary else LightTextPrimary
                                )
                            }
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 4. CATEGORY FILTER CHIPS
            // -------------------------------------------------------------
            item {
                val categories = listOf("All", "Vande Bharat", "Rajdhani", "Shatabdi", "Superfast", "Express", "Local/EMU")
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { selectedCategory = cat }
                                .background(
                                    if (isSelected) RailBluePrimary
                                    else if (isDark) DarkSurface
                                    else LightSurface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) RailBluePrimary else if (isDark) DarkBorder else LightBorder,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else if (isDark) DarkTextPrimary else LightTextPrimary
                            )
                        }
                    }
                }
            }

            // -------------------------------------------------------------
            // 5. TRAINS LIST OR LOADING STATE
            // -------------------------------------------------------------
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = RailBluePrimary,
                                modifier = Modifier.size(36.dp),
                                strokeWidth = 3.dp
                            )
                            Text(
                                text = "Loading live timetable for $currentStationCode...",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else if (filteredTrains.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) DarkSurface else LightSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("🔍", fontSize = 40.sp)
                            Text(
                                text = "No ${if (boardMode == StationBoardMode.DEPARTURES) "Departures" else "Arrivals"} Found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) DarkTextPrimary else LightTextPrimary
                            )
                            Text(
                                text = if (selectedCategory != "All") "No $selectedCategory trains match this filter at $currentStationName."
                                else "No scheduled trains found at $currentStationName for this mode.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )

                            if (selectedCategory != "All") {
                                Button(
                                    onClick = { selectedCategory = "All" },
                                    colors = ButtonDefaults.buttonColors(containerColor = RailBluePrimary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Show All Trains", fontSize = 12.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            } else {
                items(filteredTrains) { train ->
                    StationBoardTrainCard(
                        train = train,
                        boardMode = boardMode,
                        isDark = isDark,
                        onTrackTrain = onTrackTrain
                    )
                }
            }
        }
    }
}

/**
 * Individual Train Timetable Card with Category Color Stripe, Platform Badge, Delay Pill & Track Live button
 */
@Composable
private fun StationBoardTrainCard(
    train: StationBoardTrain,
    boardMode: StationBoardMode,
    isDark: Boolean,
    onTrackTrain: (Train) -> Unit
) {
    // Train type accent color matching official IR categories
    val typeColor = when {
        train.type.contains("Vande Bharat", ignoreCase = true) || train.name.contains("Vande Bharat", ignoreCase = true) -> Color(0xFF7C3AED) // Royal Purple
        train.type.contains("Rajdhani", ignoreCase = true) || train.name.contains("Rajdhani", ignoreCase = true) -> Color(0xFFDC2626) // Ruby Red
        train.type.contains("Shatabdi", ignoreCase = true) || train.name.contains("Shatabdi", ignoreCase = true) -> Color(0xFFEA580C) // Amber Orange
        train.type.contains("Duronto", ignoreCase = true) || train.name.contains("Duronto", ignoreCase = true) -> Color(0xFF0284C7) // Sky Blue
        train.type.contains("Superfast", ignoreCase = true) || train.type.contains("SF", ignoreCase = true) -> Color(0xFF2563EB) // Cobalt
        train.type.contains("Passenger", ignoreCase = true) || train.type.contains("Local", ignoreCase = true) -> Color(0xFF059669) // Emerald
        else -> Color(0xFF1E40AF) // Slate Blue
    }

    val primaryTime = when (boardMode) {
        StationBoardMode.DEPARTURES -> train.scheduledDep ?: train.scheduledArr ?: "—"
        StationBoardMode.ARRIVALS -> train.scheduledArr ?: train.scheduledDep ?: "—"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurface else LightSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Left Category Color Stripe
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(typeColor)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Top Row: Train Number, Name, Category Pill, Time & Platform
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = train.number,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                color = RailBluePrimary
                            )

                            Box(
                                modifier = Modifier
                                    .background(typeColor.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = train.type,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = typeColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = train.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary
                        )
                    }

                    // Primary Time & Platform Pill
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = primaryTime,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 17.sp,
                            color = if (isDark) DarkTextPrimary else LightTextPrimary
                        )

                        // Platform badge
                        Box(
                            modifier = Modifier
                                .background(
                                    color = RailBluePrimary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = RailBluePrimary.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (train.platform.startsWith("PF")) train.platform else "PF ${train.platform}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = RailBluePrimary
                            )
                        }
                    }
                }

                // Middle Row: Origin → Destination Route & Halt info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${train.origin} ➔ ${train.destination}",
                        fontSize = 12.sp,
                        color = if (isDark) DarkTextSecondary else LightTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Transit halt details if both arr and dep exist
                    if (!train.scheduledArr.isNullOrBlank() && !train.scheduledDep.isNullOrBlank()) {
                        Text(
                            text = "Arr ${train.scheduledArr} • Dep ${train.scheduledDep}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                HorizontalDivider(color = if (isDark) DarkBorder.copy(alpha = 0.5f) else LightBorder.copy(alpha = 0.5f))

                // Bottom Row: Status Badge + Track Live Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Delay / Status Pill
                    val (statusText, statusBg, statusColor) = when {
                        train.delayMinutes == 0 -> Triple(
                            "● ON TIME",
                            RailGreenOnTime.copy(alpha = 0.12f),
                            RailGreenOnTime
                        )
                        train.delayMinutes > 0 -> Triple(
                            "● +${train.delayMinutes}m LATE",
                            RailRedDelayed.copy(alpha = 0.12f),
                            RailRedDelayed
                        )
                        else -> Triple(
                            "● ${-train.delayMinutes}m EARLY",
                            RailGreenOnTime.copy(alpha = 0.12f),
                            RailGreenOnTime
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(statusBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }

                    // Track Live Button
                    Button(
                        onClick = { onTrackTrain(train.toTrain()) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RailBluePrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🚆", fontSize = 12.sp)
                            Text(
                                text = "Track Live",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
