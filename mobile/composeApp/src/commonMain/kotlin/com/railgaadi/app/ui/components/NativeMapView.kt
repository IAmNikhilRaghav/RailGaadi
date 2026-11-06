package com.railgaadi.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun NativeMapView(
    modifier: Modifier = Modifier,
    isNavigationMode: Boolean = false,
    userLat: Double = 27.8974,
    userLon: Double = 78.0880,
    destLat: Double = 27.9150,
    destLon: Double = 78.0750,
    stationName: String = "Aligarh Junction",
    trainNumber: String = "12404",
    trainProgress: Float = 0.68f
)
