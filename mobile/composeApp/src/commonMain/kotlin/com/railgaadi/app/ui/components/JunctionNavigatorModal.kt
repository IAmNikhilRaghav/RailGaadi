package com.railgaadi.app.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    trainType: String? = null,
    isDark: Boolean = false,
    onDismiss: () -> Unit
) {
    // skipPartiallyExpanded = false enables both half-screen and full-screen pulling!
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(if (isDark) 0xFF080C16 else 0xFFF1F5F9),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.fillMaxHeight()
    ) {
        StationLayoutVisualizer(
            stationCode = stationCode,
            stationName = stationName,
            targetPlatform = targetPlatform,
            targetCoachCode = targetCoachCode,
            trainNumber = trainNumber,
            trainName = trainName,
            trainType = trainType,
            isDark = isDark,
            onClose = onDismiss
        )
    }
}
