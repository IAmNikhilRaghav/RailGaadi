package com.railgaadi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun RoutesScreen() {
    val sampleTrains = remember {
        listOf(
            Train(
                number = "12404",
                name = "Prayagraj SF Express",
                type = "Superfast Express",
                origin = "Lalgarh Jn",
                destination = "Prayagraj Jn",
                departureTime = "10:30",
                arrivalTime = "04:45",
                durationMinutes = 1095,
                platform = "2"
            ),
            Train(
                number = "22436",
                name = "Vande Bharat Express",
                type = "Vande Bharat",
                origin = "New Delhi",
                destination = "Varanasi Jn",
                departureTime = "06:00",
                arrivalTime = "14:00",
                durationMinutes = 480,
                platform = "1"
            ),
            Train(
                number = "12301",
                name = "Howrah Rajdhani Express",
                type = "Rajdhani Express",
                origin = "Howrah Jn",
                destination = "New Delhi",
                departureTime = "16:50",
                arrivalTime = "10:05",
                durationMinutes = 1035,
                platform = "9"
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Header
            Text(
                text = "🚆 " + I18n.t("rs_pan_india"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${I18n.t("rs_filter_by")} ${I18n.t("rs_all_dates")}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(sampleTrains) { train ->
            TrainCardItem(train)
        }
    }
}

@Composable
fun TrainCardItem(train: Train) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(train.number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Text(
                        train.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color(0xFFDCFCE7), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        I18n.t("running_today"),
                        color = RailGreenOnTime,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "${train.origin} → ${train.destination}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Time & duration row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(train.departureTime, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("DEP", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }

                Text(
                    "${train.durationMinutes / 60} hrs ${train.durationMinutes % 60} mins",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(train.arrivalTime, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text("ARR", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekly schedule row
            Text(
                I18n.t("weekly_schedule"),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val dayLabels = I18n.dayLabels()
                dayLabels.forEachIndexed { i, label ->
                    val runs = train.runsOnDays.getOrNull(i) ?: true
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (runs) RailBluePrimary.copy(alpha = 0.15f) else Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (runs) RailBluePrimary else Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Platform tag & Track live button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF0284C7), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        "🚆 PF ${train.platform}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("▶ ${I18n.t("search_track_live")}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

