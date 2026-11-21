package com.railgaadi.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JunctionNavigatorModal(
    stationCode: String,
    stationName: String,
    targetPlatform: Int,
    targetCoachCode: String? = null,
    trainNumber: String? = null,
    trainName: String? = null,
    isDark: Boolean = false,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        StationLayoutVisualizer(
            stationCode = stationCode,
            stationName = stationName,
            targetPlatform = targetPlatform,
            targetCoachCode = targetCoachCode,
            trainNumber = trainNumber,
            trainName = trainName,
            isDark = isDark,
            onClose = onDismiss
        )
    }
}
