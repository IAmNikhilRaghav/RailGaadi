package com.railgaadi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.railgaadi.app.i18n.I18n
import com.railgaadi.app.theme.*

data class StationBoardItem(
    val trainNo: String,
    val trainName: String,
    val time: String,
    val platform: String,
    val destination: String,
    val status: String
)

@Composable
fun StationBoardScreen() {
    val items = remember {
        listOf(
            StationBoardItem("12404", "Prayagraj SF", "10:30", "PF 2", "Prayagraj Jn", "ON TIME"),
            StationBoardItem("22436", "Vande Bharat", "06:00", "PF 1", "Varanasi Jn", "ON TIME"),
            StationBoardItem("12301", "Howrah Rajdhani", "16:50", "PF 9", "New Delhi", "ON TIME"),
            StationBoardItem("12952", "Mumbai Rajdhani", "17:00", "PF 3", "Mumbai Central", "DELAYED 15m")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "🚉 " + I18n.t("tab_station") + " Board",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("Live Timetable for NDLS (New Delhi)", fontSize = 13.sp, color = Color.Gray)
        }

        items(items) { item ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item.trainNo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            Text(item.trainName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text("To: ${item.destination}", fontSize = 12.sp, color = Color.Gray)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(item.time, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text(item.platform, fontSize = 11.sp, color = RailBluePrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

