package com.railgaadi.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.model.Train
import com.railgaadi.app.theme.*

data class StationOption(val code: String, val name: String)

@Composable
fun RoutesScreen(onTrackTrain: (Train) -> Unit = {}) {
    var fromStationQuery by remember { mutableStateOf("New Delhi (NDLS)") }
    var toStationQuery by remember { mutableStateOf("Varanasi Jn (BSB)") }
    var isFromFocused by remember { mutableStateOf(false) }
    var isToFocused by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf("All") }
    var selectedDayFilter by remember { mutableStateOf("All Days") }

    val majorStations = remember {
        listOf(
            StationOption("NDLS", "New Delhi"),
            StationOption("BSB", "Varanasi Jn"),
            StationOption("MMCT", "Mumbai Central"),
            StationOption("HWH", "Howrah Jn (Kolkata)"),
            StationOption("PRYJ", "Prayagraj Jn"),
            StationOption("CNB", "Kanpur Central"),
            StationOption("BPL", "Bhopal Jn"),
            StationOption("JP", "Jaipur Jn"),
            StationOption("LKO", "Lucknow Charbagh"),
            StationOption("ASR", "Amritsar Jn"),
            StationOption("ADI", "Ahmedabad Jn"),
            StationOption("MAS", "Chennai Central"),
            StationOption("SBC", "KSR Bengaluru")
        )
    }

    val allCatalogTrains = remember {
        listOf(
            Train("22436", "Vande Bharat Express", "Vande Bharat", "New Delhi (NDLS)", "Varanasi Jn (BSB)", "06:00", "14:00", 480, "16", listOf(false, true, true, false, true, true, true)),
            Train("12404", "Prayagraj SF Express", "Superfast", "Lalgarh Jn (LGNH)", "Prayagraj Jn (PRYJ)", "10:30", "04:45", 1095, "2", List(7) { true }),
            Train("12952", "Mumbai Tejas Rajdhani", "Rajdhani", "New Delhi (NDLS)", "Mumbai Central (MMCT)", "16:55", "08:35", 940, "3", List(7) { true }),
            Train("12002", "New Delhi Bhopal Shatabdi", "Shatabdi", "New Delhi (NDLS)", "Rani Kamlapati (RKMP)", "06:00", "14:40", 520, "1", List(7) { true }),
            Train("12302", "Howrah Rajdhani Express", "Rajdhani", "New Delhi (NDLS)", "Howrah Jn (HWH)", "16:50", "09:55", 1025, "9", List(7) { true }),
            Train("12004", "Lucknow Shatabdi Express", "Shatabdi", "New Delhi (NDLS)", "Lucknow Charbagh (LKO)", "06:10", "12:40", 390, "1", List(7) { true }),
            Train("20901", "Vande Bharat Express", "Vande Bharat", "Mumbai Central (MMCT)", "Ahmedabad Jn (ADI)", "06:10", "11:25", 315, "5", listOf(true, true, true, true, true, true, false)),
            Train("12259", "Sealdah Bikaner AC Duronto", "Duronto", "Sealdah (SDAH)", "Bikaner Jn (BKN)", "17:00", "13:30", 1230, "12", listOf(false, true, false, true, false, true, false)),
            Train("12013", "Amritsar Shatabdi Express", "Shatabdi", "New Delhi (NDLS)", "Amritsar Jn (ASR)", "16:30", "22:30", 360, "2", List(7) { true })
        )
    }

    // Filter trains dynamically
    val filteredTrains = remember(fromStationQuery, toStationQuery, selectedCategoryFilter) {
        allCatalogTrains.filter { train ->
            val matchCategory = selectedCategoryFilter == "All" || train.type.contains(selectedCategoryFilter, ignoreCase = true)
            matchCategory
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
                        text = "🚆 Station to Station Route Search",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // From Station
                    OutlinedTextField(
                        value = fromStationQuery,
                        onValueChange = {
                            fromStationQuery = it
                            isFromFocused = true
                        },
                        label = { Text("🟢 From Station (Origin)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    // Swap Button
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
                        IconButton(
                            onClick = {
                                val temp = fromStationQuery
                                fromStationQuery = toStationQuery
                                toStationQuery = temp
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .background(RailBluePrimary.copy(alpha = 0.12f), CircleShape)
                        ) {
                            Text("⇅", fontSize = 18.sp, color = RailBluePrimary, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // To Station
                    OutlinedTextField(
                        value = toStationQuery,
                        onValueChange = {
                            toStationQuery = it
                            isToFocused = true
                        },
                        label = { Text("🔴 To Station (Destination)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Route Suggestions
                    Text("Popular Corridors:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val popularCorridors = listOf(
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
                                        fromStationQuery = parts[0]
                                        toStationQuery = parts[1]
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

        // 2. Train Type Filters (All, Vande Bharat, Rajdhani, Shatabdi, Superfast)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${I18n.t("rs_filter_by")} Category",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val categories = listOf("All", "Vande Bharat", "Rajdhani", "Shatabdi", "Superfast", "Duronto")
                    items(categories) { cat ->
                        val isSelected = selectedCategoryFilter == cat
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) RailBluePrimary else MaterialTheme.colorScheme.surface,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredTrains.size} Direct Trains Found",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Weekly Schedule",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // 4. Train Cards List
        items(filteredTrains) { train ->
            TrainCardItem(train = train, onTrackClick = { onTrackTrain(train) })
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
            // Top Row: Number & Category
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            when (train.type) {
                                "Vande Bharat" -> Color(0xFFEFF6FF)
                                "Rajdhani" -> Color(0xFFFEF2F2)
                                "Shatabdi" -> Color(0xFFF0FDF4)
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
                        color = when (train.type) {
                            "Vande Bharat" -> Color(0xFF1D4ED8)
                            "Rajdhani" -> Color(0xFFB91C1C)
                            "Shatabdi" -> Color(0xFF15803D)
                            else -> Color(0xFF475569)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Timings Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dep
                Column {
                    Text(train.departureTime, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(train.origin.substringBefore(" ("), fontSize = 12.sp, color = Color.Gray)
                }

                // Duration & Line
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val hrs = train.durationMinutes / 60
                    val mins = train.durationMinutes % 60
                    Text("${hrs}h ${mins}m", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(RailBluePrimary, CircleShape))
                        HorizontalDivider(modifier = Modifier.width(60.dp), thickness = 2.dp, color = RailBluePrimary)
                        Box(modifier = Modifier.size(6.dp).background(RailBluePrimary, CircleShape))
                    }
                    Text("Daily", fontSize = 10.sp, color = Color.Gray)
                }

                // Arr
                Column(horizontalAlignment = Alignment.End) {
                    Text(train.arrivalTime, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(train.destination.substringBefore(" ("), fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Running Days Row (S M T W T F S)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val dayLetters = listOf("S", "M", "T", "W", "T", "F", "S")
                    train.runsOnDays.forEachIndexed { idx, runs ->
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

                // Track Live Button
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
