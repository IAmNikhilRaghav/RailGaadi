package com.railgaadi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.railgaadi.app.model.Station

@Composable
actual fun NativeMapView(
    modifier: Modifier,
    isNavigationMode: Boolean,
    userLat: Double,
    userLon: Double,
    destLat: Double,
    destLon: Double,
    stationName: String,
    trainNumber: String,
    trainProgress: Float,
    stations: List<Station>,
    is3DMode: Boolean,
    followTrain: Boolean,
    onStationClick: (Station) -> Unit,
    onNavigateToStation: (Station) -> Unit,
    onVoiceInstruction: (String) -> Unit,
    onCompassRotationChanged: (Float) -> Unit,
    onNavStatsUpdated: (distM: Double, durS: Double, instruction: String, icon: String) -> Unit
) {
    Box(
        modifier = modifier.background(Color(0xFFE2E8F0)),
        contentAlignment = Alignment.Center
    ) {
        Text("🗺️ Map View ($stationName)")
    }
}
