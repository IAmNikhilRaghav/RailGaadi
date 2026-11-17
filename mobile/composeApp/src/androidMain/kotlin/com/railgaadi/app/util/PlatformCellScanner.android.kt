package com.railgaadi.app.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.*
import androidx.core.content.ContextCompat

actual object PlatformCellScanner {
    private var appContext: Context? = null
    private var telephonyManager: TelephonyManager? = null
    private var cellUpdateCallback: ((CellTowerData) -> Unit)? = null
    private var currentCellData: CellTowerData? = null
    private var isScanning = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!isScanning) return
            refreshCellInfo()
            // Poll radio baseband every 3 seconds for cell handovers along the track
            mainHandler.postDelayed(this, 3000L)
        }
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    }

    actual fun isSupported(): Boolean = true

    actual fun hasPermission(): Boolean {
        val ctx = appContext ?: return false
        val fineLocation = ContextCompat.checkSelfPermission(
            ctx,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = ContextCompat.checkSelfPermission(
            ctx,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineLocation || coarseLocation
    }

    @SuppressLint("MissingPermission")
    actual fun startCellScanning(onCellUpdate: (CellTowerData) -> Unit) {
        if (!hasPermission()) return
        cellUpdateCallback = onCellUpdate
        if (isScanning) return
        isScanning = true

        // Immediate first poll
        refreshCellInfo()
        mainHandler.postDelayed(pollRunnable, 3000L)
    }

    actual fun stopCellScanning() {
        isScanning = false
        mainHandler.removeCallbacks(pollRunnable)
        cellUpdateCallback = null
    }

    actual fun getCurrentCell(): CellTowerData? = currentCellData

    @SuppressLint("MissingPermission")
    private fun refreshCellInfo() {
        val tm = telephonyManager ?: return
        if (!hasPermission()) return

        try {
            val cellInfoList: List<CellInfo>? = tm.allCellInfo
            if (cellInfoList.isNullOrEmpty()) return

            // Prefer registered (serving) cell
            val servingCell = cellInfoList.firstOrNull { it.isRegistered } ?: cellInfoList.firstOrNull()
            if (servingCell == null) return

            val parsed = parseCellInfo(servingCell)
            if (parsed != null && parsed.isValid) {
                currentCellData = parsed
                cellUpdateCallback?.invoke(parsed)
            }
        } catch (t: Throwable) {
            // Log warning or ignore if radio layer is momentarily busy
        }
    }

    private fun parseCellInfo(cellInfo: CellInfo): CellTowerData? {
        val now = System.currentTimeMillis()
        return when (cellInfo) {
            is CellInfoLte -> {
                val id = cellInfo.cellIdentity
                val ci = id.ci.toLong()
                val tac = id.tac
                val mcc = id.mccString?.toIntOrNull() ?: 404
                val mnc = id.mncString?.toIntOrNull() ?: 0
                val dbm = cellInfo.cellSignalStrength.dbm
                if (ci > 0 && ci != 2147483647L) {
                    CellTowerData(
                        cellId = ci,
                        areaCode = tac,
                        mcc = mcc,
                        mnc = mnc,
                        signalStrengthDbm = dbm,
                        radioType = "LTE",
                        timestamp = now
                    )
                } else null
            }
            is CellInfoGsm -> {
                val id = cellInfo.cellIdentity
                val cid = id.cid.toLong()
                val lac = id.lac
                val mcc = id.mccString?.toIntOrNull() ?: 404
                val mnc = id.mncString?.toIntOrNull() ?: 0
                val dbm = cellInfo.cellSignalStrength.dbm
                if (cid > 0 && cid != 65535L) {
                    CellTowerData(
                        cellId = cid,
                        areaCode = lac,
                        mcc = mcc,
                        mnc = mnc,
                        signalStrengthDbm = dbm,
                        radioType = "GSM",
                        timestamp = now
                    )
                } else null
            }
            is CellInfoWcdma -> {
                val id = cellInfo.cellIdentity
                val cid = id.cid.toLong()
                val lac = id.lac
                val mcc = id.mccString?.toIntOrNull() ?: 404
                val mnc = id.mncString?.toIntOrNull() ?: 0
                val dbm = cellInfo.cellSignalStrength.dbm
                if (cid > 0 && cid != 268435455L) {
                    CellTowerData(
                        cellId = cid,
                        areaCode = lac,
                        mcc = mcc,
                        mnc = mnc,
                        signalStrengthDbm = dbm,
                        radioType = "WCDMA",
                        timestamp = now
                    )
                } else null
            }
            else -> {
                // Handle 5G NR on API 29+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr) {
                    val id = cellInfo.cellIdentity as? CellIdentityNr
                    val nci = id?.nci ?: -1L
                    val tac = id?.tac ?: -1
                    val mcc = id?.mccString?.toIntOrNull() ?: 404
                    val mnc = id?.mncString?.toIntOrNull() ?: 0
                    val dbm = cellInfo.cellSignalStrength.dbm
                    if (nci > 0 && nci != Long.MAX_VALUE) {
                        CellTowerData(
                            cellId = nci,
                            areaCode = tac,
                            mcc = mcc,
                            mnc = mnc,
                            signalStrengthDbm = dbm,
                            radioType = "NR_5G",
                            timestamp = now
                        )
                    } else null
                } else null
            }
        }
    }
}
