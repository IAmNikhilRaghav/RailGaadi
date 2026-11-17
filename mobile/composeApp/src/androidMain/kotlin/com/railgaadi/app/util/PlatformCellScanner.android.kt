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
    private var multiCellUpdateCallback: ((List<CellTowerData>) -> Unit)? = null
    private var currentCellData: CellTowerData? = null
    private var currentVisibleCells: List<CellTowerData> = emptyList()
    private var isScanning = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!isScanning) return
            refreshCellInfo()
            // Poll radio baseband every 2.5 seconds for multi-tower RSRP handovers along track
            mainHandler.postDelayed(this, 2500L)
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
        if (!isScanning) {
            isScanning = true
            refreshCellInfo()
            mainHandler.postDelayed(pollRunnable, 2500L)
        }
    }

    @SuppressLint("MissingPermission")
    actual fun startMultiCellScanning(onCellsUpdate: (List<CellTowerData>) -> Unit) {
        if (!hasPermission()) return
        multiCellUpdateCallback = onCellsUpdate
        if (!isScanning) {
            isScanning = true
            refreshCellInfo()
            mainHandler.postDelayed(pollRunnable, 2500L)
        }
    }

    actual fun stopCellScanning() {
        isScanning = false
        mainHandler.removeCallbacks(pollRunnable)
        cellUpdateCallback = null
        multiCellUpdateCallback = null
    }

    actual fun getCurrentCell(): CellTowerData? = currentCellData

    actual fun getVisibleCells(): List<CellTowerData> = currentVisibleCells

    @SuppressLint("MissingPermission")
    private fun refreshCellInfo() {
        val tm = telephonyManager ?: return
        if (!hasPermission()) return

        try {
            val cellInfoList: List<CellInfo>? = tm.allCellInfo
            if (cellInfoList.isNullOrEmpty()) return

            val parsedList = mutableListOf<CellTowerData>()
            for (info in cellInfoList) {
                parseCellInfo(info)?.let { parsed ->
                    if (parsed.isValid) {
                        parsedList.add(parsed)
                    }
                }
            }

            if (parsedList.isEmpty()) return

            // Sort: Serving (registered) primary cell first, then remaining neighbors ordered by strongest RSRP
            parsedList.sortWith(compareByDescending<CellTowerData> { it.isRegistered }.thenByDescending { it.signalStrengthDbm })

            currentVisibleCells = parsedList
            val primary = parsedList.firstOrNull { it.isRegistered } ?: parsedList.firstOrNull()
            if (primary != null) {
                currentCellData = primary
                cellUpdateCallback?.invoke(primary)
            }
            multiCellUpdateCallback?.invoke(parsedList)
        } catch (_: Throwable) {
            // Radio layer may be temporarily busy during handover
        }
    }

    private fun parseCellInfo(cellInfo: CellInfo): CellTowerData? {
        val now = System.currentTimeMillis()
        val isRegistered = cellInfo.isRegistered

        return when (cellInfo) {
            is CellInfoLte -> {
                val id = cellInfo.cellIdentity
                val ci = id.ci.toLong()
                val tac = id.tac
                val pci = id.pci
                val mcc = id.mccString?.toIntOrNull() ?: 404
                val mnc = id.mncString?.toIntOrNull() ?: 0
                val sig = cellInfo.cellSignalStrength
                val dbm = sig.dbm
                val ta = sig.timingAdvance

                val rsrp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) sig.rsrp else dbm
                val rsrq = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) sig.rsrq else -10

                CellTowerData(
                    cellId = if (ci > 0 && ci != 2147483647L) ci else 0L,
                    areaCode = if (tac > 0 && tac != 65535) tac else 0,
                    mcc = mcc,
                    mnc = mnc,
                    signalStrengthDbm = dbm,
                    radioType = "LTE",
                    isRegistered = isRegistered,
                    timingAdvance = ta,
                    rsrpDbm = rsrp,
                    rsrqDb = rsrq,
                    pci = pci,
                    timestamp = now
                )
            }
            is CellInfoGsm -> {
                val id = cellInfo.cellIdentity
                val cid = id.cid.toLong()
                val lac = id.lac
                val mcc = id.mccString?.toIntOrNull() ?: 404
                val mnc = id.mncString?.toIntOrNull() ?: 0
                val sig = cellInfo.cellSignalStrength
                val dbm = sig.dbm
                val ta = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) sig.timingAdvance else -1

                CellTowerData(
                    cellId = if (cid > 0 && cid != 65535L) cid else 0L,
                    areaCode = if (lac > 0 && lac != 65535) lac else 0,
                    mcc = mcc,
                    mnc = mnc,
                    signalStrengthDbm = dbm,
                    radioType = "GSM",
                    isRegistered = isRegistered,
                    timingAdvance = ta,
                    rsrpDbm = dbm,
                    rsrqDb = -10,
                    pci = -1,
                    timestamp = now
                )
            }
            is CellInfoWcdma -> {
                val id = cellInfo.cellIdentity
                val cid = id.cid.toLong()
                val lac = id.lac
                val psc = id.psc
                val mcc = id.mccString?.toIntOrNull() ?: 404
                val mnc = id.mncString?.toIntOrNull() ?: 0
                val dbm = cellInfo.cellSignalStrength.dbm

                CellTowerData(
                    cellId = if (cid > 0 && cid != 268435455L) cid else 0L,
                    areaCode = if (lac > 0 && lac != 65535) lac else 0,
                    mcc = mcc,
                    mnc = mnc,
                    signalStrengthDbm = dbm,
                    radioType = "WCDMA",
                    isRegistered = isRegistered,
                    timingAdvance = -1,
                    rsrpDbm = dbm,
                    rsrqDb = -10,
                    pci = psc,
                    timestamp = now
                )
            }
            else -> {
                // 5G NR on Android 10+ (API 29+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr) {
                    val id = cellInfo.cellIdentity as? CellIdentityNr
                    val nci = id?.nci ?: -1L
                    val tac = id?.tac ?: -1
                    val pci = id?.pci ?: -1
                    val mcc = id?.mccString?.toIntOrNull() ?: 404
                    val mnc = id?.mncString?.toIntOrNull() ?: 0
                    val sig = cellInfo.cellSignalStrength
                    val dbm = sig.dbm

                    CellTowerData(
                        cellId = if (nci > 0 && nci != Long.MAX_VALUE) nci else 0L,
                        areaCode = if (tac > 0 && tac != 65535) tac else 0,
                        mcc = mcc,
                        mnc = mnc,
                        signalStrengthDbm = dbm,
                        radioType = "NR_5G",
                        isRegistered = isRegistered,
                        timingAdvance = -1,
                        rsrpDbm = dbm,
                        rsrqDb = -10,
                        pci = pci,
                        timestamp = now
                    )
                } else null
            }
        }
    }
}
