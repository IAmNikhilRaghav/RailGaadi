package com.railgaadi.app.util

/**
 * Encapsulates physical radio telecommunications cell tower telemetry
 * received by the smartphone modem (2G/3G/4G LTE/5G NR).
 */
data class CellTowerData(
    val cellId: Long,                  // CI (LTE/GSM) or NCI (5G NR)
    val areaCode: Int,                 // LAC (GSM) or TAC (LTE/5G)
    val mcc: Int = 404,                // Mobile Country Code (404/405 for India)
    val mnc: Int = 0,                  // Mobile Network Code (Jio, Airtel, Vi, BSNL)
    val signalStrengthDbm: Int = -85,  // Signal strength in dBm (-50dBm excellent, -115dBm weak)
    val radioType: String = "LTE",     // "LTE", "NR_5G", "GSM", "WCDMA"
    val isRegistered: Boolean = true,  // true = serving primary cell, false = neighboring candidate cell
    val timingAdvance: Int = -1,       // LTE/5G Timing Advance step (1 step ≈ 78.12 meters physical distance)
    val rsrpDbm: Int = -85,            // Reference Signal Received Power
    val rsrqDb: Int = -10,             // Reference Signal Received Quality
    val pci: Int = -1,                 // Physical Cell ID (0..1007)
    val timestamp: Long = 0L
) {
    val isValid: Boolean
        get() = (cellId > 0 && cellId != 2147483647L && cellId != 268435455L && areaCode > 0) ||
                (pci > 0 && signalStrengthDbm in -140..-40)

    val estimatedDistanceByTimingAdvanceMeters: Double?
        get() = if (timingAdvance in 0..1282) timingAdvance * 78.12 else null
}

/**
 * Cross-platform radio environment listener.
 * Level 4: Supports multi-tower RSRP / RSSI signal scanning across primary & neighboring towers.
 */
expect object PlatformCellScanner {
    fun isSupported(): Boolean
    fun startCellScanning(onCellUpdate: (CellTowerData) -> Unit)
    fun startMultiCellScanning(onCellsUpdate: (List<CellTowerData>) -> Unit)
    fun stopCellScanning()
    fun getCurrentCell(): CellTowerData?
    fun getVisibleCells(): List<CellTowerData>
    fun hasPermission(): Boolean
}
