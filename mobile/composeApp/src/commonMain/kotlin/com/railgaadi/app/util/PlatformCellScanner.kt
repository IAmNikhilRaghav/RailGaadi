package com.railgaadi.app.util

/**
 * Encapsulates physical radio telecommunications cell tower information
 * received by the smartphone modem (2G/3G/4G LTE/5G NR).
 */
data class CellTowerData(
    val cellId: Long,              // CI (LTE/GSM) or NCI (5G NR)
    val areaCode: Int,             // LAC (GSM) or TAC (LTE/5G)
    val mcc: Int = 404,            // Mobile Country Code (404/405 for India)
    val mnc: Int = 0,              // Mobile Network Code (Jio, Airtel, Vi, BSNL)
    val signalStrengthDbm: Int = -85, // Signal strength in dBm (-50dBm excellent, -115dBm weak)
    val radioType: String = "LTE", // "LTE", "NR", "GSM", "WCDMA"
    val timestamp: Long = 0L
) {
    val isValid: Boolean
        get() = cellId > 0 && cellId != 2147483647L && cellId != 268435455L && areaCode > 0
}

/**
 * Cross-platform radio environment listener.
 * On Android, reads hardware TelephonyManager to track cell tower transitions.
 * On iOS, gracefully delegates to Apple's CoreLocation framework.
 */
expect object PlatformCellScanner {
    fun isSupported(): Boolean
    fun startCellScanning(onCellUpdate: (CellTowerData) -> Unit)
    fun stopCellScanning()
    fun getCurrentCell(): CellTowerData?
    fun hasPermission(): Boolean
}
