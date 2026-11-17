package com.railgaadi.app.util

/**
 * iOS implementation of PlatformCellScanner.
 * Apple sandbox prohibits third-party apps from inspecting raw baseband cell tower IDs.
 * Always reports unsupported on iOS and defers to CoreLocation.
 */
actual object PlatformCellScanner {
    actual fun isSupported(): Boolean = false

    actual fun startCellScanning(onCellUpdate: (CellTowerData) -> Unit) {
        // iOS does not expose raw cell IDs to third-party apps
    }

    actual fun stopCellScanning() {
        // No-op on iOS
    }

    actual fun getCurrentCell(): CellTowerData? = null

    actual fun hasPermission(): Boolean = false
}
