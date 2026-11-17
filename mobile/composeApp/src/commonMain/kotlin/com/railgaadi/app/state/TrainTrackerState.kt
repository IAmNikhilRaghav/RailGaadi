package com.railgaadi.app.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.railgaadi.app.model.AlarmTone
import com.railgaadi.app.model.AlarmTriggerUnit
import com.railgaadi.app.model.CellTowerRepository
import com.railgaadi.app.model.DestinationAlarm
import com.railgaadi.app.model.LiveStatus
import com.railgaadi.app.model.OfflineTrainEngine
import com.railgaadi.app.model.RadioTrilaterationEngine
import com.railgaadi.app.model.Station
import com.railgaadi.app.model.StationDataService
import com.railgaadi.app.model.TrackCellRecord
import com.railgaadi.app.model.Train
import com.railgaadi.app.model.TrilaterationResult
import com.railgaadi.app.network.LandmarkService
import com.railgaadi.app.network.RailRadarService
import com.railgaadi.app.network.WeatherService
import com.railgaadi.app.util.CellTowerData
import com.railgaadi.app.util.GeoLocation
import com.railgaadi.app.util.PlatformAlarm
import com.railgaadi.app.util.PlatformCellScanner
import com.railgaadi.app.util.PlatformDate
import com.railgaadi.app.util.PlatformLocation
import com.railgaadi.app.util.PlatformSpeech
import com.railgaadi.app.util.PlatformStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object TrainTrackerState {
    private val service = RailRadarService()
    private val weatherService = WeatherService()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var searchDebounceJob: Job? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private const val KEY_RECENT_TRAINS = "saved_recent_trains"
    private const val KEY_ACTIVE_ALARM = "saved_active_alarm"
    private const val KEY_ODOMETER = "saved_odometer"

    var searchQuery by mutableStateOf("")
    var isSearchDropdownOpen by mutableStateOf(false)
    var isSearching by mutableStateOf(false)
    var searchResults by mutableStateOf<List<Train>>(emptyList())
    // Start with empty history — populated as user searches
    var recentSearches by mutableStateOf(listOf<String>())

    private fun loadSavedRecentTrains(): List<Train> {
        return try {
            val raw = PlatformStorage.getString(KEY_RECENT_TRAINS)
            if (!raw.isNullOrBlank()) {
                json.decodeFromString<List<Train>>(raw)
            } else {
                emptyList()
            }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    // Recently searched trains (persisted to platform storage, empty on fresh install)
    var recentTrains by mutableStateOf<List<Train>>(loadSavedRecentTrains())

    fun addRecentTrain(train: Train) {
        if (train.number.isBlank()) return
        val filtered = recentTrains.filter { it.number != train.number }
        recentTrains = (listOf(train) + filtered).take(5)
        try {
            PlatformStorage.saveString(KEY_RECENT_TRAINS, json.encodeToString(recentTrains))
        } catch (_: Throwable) {}
    }

    fun removeRecentTrain(trainNumber: String) {
        recentTrains = recentTrains.filter { it.number != trainNumber }
        try {
            PlatformStorage.saveString(KEY_RECENT_TRAINS, json.encodeToString(recentTrains))
        } catch (_: Throwable) {}
    }

    fun clearRecentTrains() {
        recentTrains = emptyList()
        try {
            PlatformStorage.remove(KEY_RECENT_TRAINS)
        } catch (_: Throwable) {}
    }

    // No default train pre-loaded; map starts clean
    var activeTrain by mutableStateOf(
        Train("", "", "", "", "", "", "", 0, "")
    )
    var liveStatus by mutableStateOf<LiveStatus?>(null)
    var isLoadingStatus by mutableStateOf(false)

    // Weather & AQI state for WeatherScreen
    var weatherData by mutableStateOf<com.railgaadi.app.model.StationWeatherData?>(null)
    var isLoadingWeather by mutableStateOf(false)
    var nearbyLandmarks by mutableStateOf<List<com.railgaadi.app.model.Landmark>>(emptyList())

    // User's Onboarding / Boarding Station (Source of user's train journey)
    var userBoardingStation by mutableStateOf<Station?>(null)

    // Global Theme State: Default to Light mode (false), toggled by sun/moon button
    var isDarkMode by mutableStateOf(false)

    // Web App Preferences & Features
    var currentMapStyle by mutableStateOf("terrain") // "light", "dark", "satellite", "terrain"
    var tripDelayAlerts by mutableStateOf(false)
    var savedTrains by mutableStateOf(listOf<String>())

    // ── Phase 4: Destination Station Wake-Up Alarm & Geofence State ──────────
    var activeAlarm by mutableStateOf<DestinationAlarm?>(null)
    var showAlarmSetupModal by mutableStateOf(false)
    var targetAlarmStation by mutableStateOf<Station?>(null)
    var isAlarmRinging by mutableStateOf(false)
    var snoozeRemainingSeconds by mutableStateOf(0)

    // ── Phase 5: Satellite GPS Tracking Engine (Hybrid Foreground & Background) ──
    var isOfflineModeEnabled by mutableStateOf(false)
    var isGpsTrackingActive by mutableStateOf(false)
    var currentGpsLocation by mutableStateOf<GeoLocation?>(null)
    var offlineGpsStatusText by mutableStateOf("Standby")
    private var isGpsListening = false

    // ── Phase 6 & Phase 7: On-Board Passenger "Inside Train Mode" & Cell Tower / GPS Telemetry ──
    var isInsideTrain by mutableStateOf(false)
    var inTrainLocationText by mutableStateOf("")
    var inTrainNextHaltText by mutableStateOf("")
    var inTrainEstArrivalMinutes by mutableStateOf(0)
    var inTrainRemainingKm by mutableStateOf(0.0)

    // True Cell Tower & Multi-Tower Trilateration Tracking State
    var currentCellTower by mutableStateOf<CellTowerData?>(null)
    var currentCellRecord by mutableStateOf<TrackCellRecord?>(null)
    var visibleCellTowers by mutableStateOf<List<CellTowerData>>(emptyList())
    var currentTrilaterationResult by mutableStateOf<TrilaterationResult?>(null)
    var activeTrackingProvider by mutableStateOf("Cloud") // "Cloud", "GPS", "Trilateration", "Cell Tower"
    private var isCellScanningActive = false

    fun toggleInsideTrainMode(enabled: Boolean? = null) {
        val target = enabled ?: !isInsideTrain
        isInsideTrain = target
        if (target) {
            ensureGpsTracking()
            ensureCellScanning()
            currentGpsLocation?.let { updateInTrainCockpit(it) }
        } else {
            inTrainLocationText = ""
            inTrainNextHaltText = ""
            inTrainEstArrivalMinutes = 0
            inTrainRemainingKm = 0.0
            stopGpsTrackingIfIdle()
            stopCellScanningIfIdle()
        }
    }

    fun updateInTrainCockpit(loc: GeoLocation) {
        if (!isInsideTrain || activeTrain.number.isBlank()) return
        val status = liveStatus ?: return
        val stations = status.stations
        if (stations.isEmpty()) return

        // 1. Calculate distance from passenger's physical GPS to each station
        val distances = stations.mapIndexed { idx, stn ->
            val (lat, lon) = if (stn.latitude != 0.0 && stn.longitude != 0.0) {
                stn.latitude to stn.longitude
            } else {
                StationDataService.getCoordinates(stn.code)
            }
            Triple(idx, stn, calculateHaversineDistanceKm(loc.latitude, loc.longitude, lat, lon))
        }

        val closest = distances.minByOrNull { it.third } ?: return
        val closestIdx = closest.first
        val closestStation = closest.second
        val distToClosest = closest.third

        // 2. Identify upcoming halt
        val nextIdx = if (closestIdx < stations.size - 1) {
            val nextStn = stations[closestIdx + 1]
            val (nextLat, nextLon) = if (nextStn.latitude != 0.0) nextStn.latitude to nextStn.longitude else StationDataService.getCoordinates(nextStn.code)
            val dToNext = calculateHaversineDistanceKm(loc.latitude, loc.longitude, nextLat, nextLon)
            val (closestLat, closestLon) = if (closestStation.latitude != 0.0) closestStation.latitude to closestStation.longitude else StationDataService.getCoordinates(closestStation.code)
            val dClosestToNext = calculateHaversineDistanceKm(closestLat, closestLon, nextLat, nextLon)
            if (dToNext < dClosestToNext) closestIdx + 1 else closestIdx
        } else closestIdx

        val upcomingStation = stations[nextIdx]
        val (uLat, uLon) = if (upcomingStation.latitude != 0.0) upcomingStation.latitude to upcomingStation.longitude else StationDataService.getCoordinates(upcomingStation.code)
        val remainingDist = calculateHaversineDistanceKm(loc.latitude, loc.longitude, uLat, uLon)
        inTrainRemainingKm = ((remainingDist * 10).toInt() / 10.0).coerceAtLeast(0.0)

        val speed = if (loc.speedKmh > 10.0) loc.speedKmh else (status.speedKmh.toDouble().takeIf { it > 10.0 } ?: 65.0)
        inTrainEstArrivalMinutes = ((remainingDist / speed) * 60).toInt().coerceAtLeast(1)

        // 3. Craft exact human-readable location description
        inTrainLocationText = when {
            distToClosest < 0.4 -> "At ${closestStation.name} (${closestStation.code}) Platform"
            distToClosest < 1.5 -> "Near ${closestStation.name} (${closestStation.code})"
            nextIdx > 0 && nextIdx > closestIdx -> "Between ${closestStation.name} & ${upcomingStation.name}"
            nextIdx > 0 -> "Between ${stations[nextIdx - 1].name} & ${upcomingStation.name}"
            else -> "Near ${closestStation.name} (${closestStation.code})"
        }

        inTrainNextHaltText = "${upcomingStation.name} (${upcomingStation.code}) • ${inTrainRemainingKm} km away"
    }

    fun ensureGpsTracking() {
        if (isGpsListening) return
        try {
            isGpsListening = true
            isGpsTrackingActive = true
            PlatformLocation.startLocationUpdates { loc ->
                onGpsLocationUpdated(loc)
            }
        } catch (e: Throwable) {
            println("ensureGpsTracking error: ${e.message}")
        }
    }

    fun stopGpsTrackingIfIdle() {
        if (!isOfflineModeEnabled && activeAlarm?.isArmed != true && !isInsideTrain) {
            try {
                PlatformLocation.stopLocationUpdates()
                isGpsListening = false
                isGpsTrackingActive = false
            } catch (_: Throwable) {}
        }
    }

    fun ensureCellScanning() {
        if (isCellScanningActive || !PlatformCellScanner.isSupported()) return
        try {
            isCellScanningActive = true
            PlatformCellScanner.startMultiCellScanning { cells ->
                onMultiCellsUpdated(cells)
            }
        } catch (e: Throwable) {
            println("ensureCellScanning error: ${e.message}")
        }
    }

    fun stopCellScanningIfIdle() {
        if (!isOfflineModeEnabled && !isInsideTrain) {
            try {
                PlatformCellScanner.stopCellScanning()
                isCellScanningActive = false
            } catch (_: Throwable) {}
        }
    }

    private fun onMultiCellsUpdated(cells: List<CellTowerData>) {
        visibleCellTowers = cells
        val primary = cells.firstOrNull { it.isRegistered } ?: cells.firstOrNull() ?: return
        currentCellTower = primary
        val record = CellTowerRepository.resolveCellTower(primary)
        currentCellRecord = record

        // Compute Level 4 Multi-Tower RSRP Signal Trilateration
        val stations = liveStatus?.stations ?: emptyList()
        val triResult = RadioTrilaterationEngine.computeTrilateration(cells, stations)
        currentTrilaterationResult = triResult

        // If passenger is inside train and GPS is degraded/offline, let Trilateration/Cell Tower drive the cockpit
        if (isInsideTrain && (currentGpsLocation == null || (currentGpsLocation?.accuracyMeters ?: 999f) > 80f)) {
            if (triResult != null) {
                activeTrackingProvider = if (triResult.isMultiTowerLock) "Trilateration" else "Cell Tower"
                inTrainLocationText = triResult.statusDescription
                inTrainNextHaltText = triResult.approachingStationName
                inTrainRemainingKm = triResult.remainingDistanceKm
                inTrainEstArrivalMinutes = ((triResult.remainingDistanceKm / 60.0) * 60).toInt().coerceAtLeast(1)
            } else if (record != null) {
                activeTrackingProvider = "Cell Tower"
                inTrainLocationText = "Near ${record.lineDescription} (${record.distanceToStationKm} km to ${record.stationName})"
                inTrainNextHaltText = record.stationName
                inTrainRemainingKm = record.distanceToStationKm
                inTrainEstArrivalMinutes = ((record.distanceToStationKm / 55.0) * 60).toInt().coerceAtLeast(1)
            }
        }

        // Dynamically update timeline if inside train or in offline mode without satellite lock
        if ((isInsideTrain || isOfflineModeEnabled) && activeTrain.number.isNotBlank() && (currentGpsLocation == null || (currentGpsLocation?.accuracyMeters ?: 999f) > 80f)) {
            if (triResult != null || record != null) {
                val hybridLive = OfflineTrainEngine.calculateHybridOfflineLiveStatus(
                    train = activeTrain,
                    gps = currentGpsLocation,
                    cell = primary,
                    visibleCells = cells,
                    existingStations = stations
                )
                onLiveStatusReceived(hybridLive)
            }
        }
    }

    private fun onGpsLocationUpdated(loc: GeoLocation) {
        currentGpsLocation = loc
        if (loc.accuracyMeters in 0.1f..80f) {
            activeTrackingProvider = "GPS"
        }
        val speedText = if (loc.speedKmh > 5.0) "${loc.speedKmh.toInt()} km/h" else "±${loc.accuracyMeters.toInt().coerceAtLeast(1)}m"
        if (isOfflineModeEnabled) {
            offlineGpsStatusText = "🛰️ GPS ACTIVE • $speedText"
        }

        // Opportunistic self-learning: map current cell tower to this GPS coordinate
        val cell = currentCellTower
        if (cell != null && cell.isValid && inTrainNextHaltText.isNotBlank()) {
            CellTowerRepository.learnCellTower(
                cell = cell,
                stationCode = inTrainNextHaltText.substringAfterLast("(").substringBeforeLast(")").ifBlank { inTrainNextHaltText },
                stationName = inTrainNextHaltText.substringBeforeLast("(").trim(),
                latitude = loc.latitude,
                longitude = loc.longitude,
                distanceKm = inTrainRemainingKm
            )
        }

        // 1. Immediately evaluate destination alarm geofence with physical GPS
        checkAlarmGeofence(loc)

        // 2. Update inside train cockpit telemetry if user is on board
        if (isInsideTrain) {
            updateInTrainCockpit(loc)
        }
    }

    fun toggleOfflineGpsMode() {
        if (isOfflineModeEnabled) {
            disableOfflineGpsMode()
        } else {
            enableOfflineGpsMode()
        }
    }

    fun enableOfflineGpsMode() {
        isOfflineModeEnabled = true
        isGpsTrackingActive = true
        offlineGpsStatusText = "Connecting to Satellites / Cell Towers..."
        ensureGpsTracking()
        ensureCellScanning()
    }

    fun disableOfflineGpsMode() {
        isOfflineModeEnabled = false
        offlineGpsStatusText = "Standby"
        if (activeAlarm?.isArmed != true) {
            stopGpsTrackingIfIdle()
        }
        if (activeTrain.number.isNotBlank()) {
            triggerManualRefresh()
        }
    }

    fun openAlarmSetup(station: Station? = null) {
        val stn = station ?: liveStatus?.stations?.lastOrNull()
        targetAlarmStation = stn
        showAlarmSetupModal = true
    }

    fun closeAlarmSetup() {
        showAlarmSetupModal = false
    }

    fun armDestinationAlarm(
        station: Station,
        triggerDistanceKm: Double = 10.0,
        triggerMinutesBefore: Int = 15,
        triggerUnit: AlarmTriggerUnit = AlarmTriggerUnit.KILOMETERS,
        voiceAnnouncement: Boolean = true,
        vibrate: Boolean = true,
        tone: AlarmTone = AlarmTone.RAILWAY_CHIME
    ) {
        val (lat, lon) = if (station.latitude != 0.0 && station.longitude != 0.0) {
            station.latitude to station.longitude
        } else {
            StationDataService.getCoordinates(station.code)
        }

        val alarm = DestinationAlarm(
            trainNumber = activeTrain.number,
            trainName = activeTrain.name,
            stationCode = station.code,
            stationName = station.name,
            stationLat = lat,
            stationLon = lon,
            triggerDistanceKm = triggerDistanceKm,
            triggerMinutesBefore = triggerMinutesBefore,
            triggerUnit = triggerUnit,
            voiceAnnouncement = voiceAnnouncement,
            vibrate = vibrate,
            tone = tone,
            isArmed = true,
            isRinging = false
        )
        activeAlarm = alarm
        showAlarmSetupModal = false

        // Persist alarm to platform storage (survives app backgrounding & exit)
        try {
            PlatformStorage.saveString(KEY_ACTIVE_ALARM, json.encodeToString(alarm))
        } catch (_: Throwable) {}

        // Ensure real-time physical GPS tracking is active
        ensureGpsTracking()

        checkAlarmGeofence()
    }

    private var alarmRingingJob: Job? = null
    private var snoozeJob: Job? = null

    fun disarmAlarm() {
        snoozeJob?.cancel()
        snoozeJob = null
        snoozeRemainingSeconds = 0
        alarmRingingJob?.cancel()
        alarmRingingJob = null
        activeAlarm = null
        isAlarmRinging = false
        try {
            PlatformStorage.remove(KEY_ACTIVE_ALARM)
        } catch (_: Throwable) {}
        PlatformAlarm.stopAlarm()
        PlatformAlarm.stopVibrate()
        PlatformSpeech.stop()
        stopGpsTrackingIfIdle()
    }

    fun triggerAlarm() {
        val alarm = activeAlarm ?: return
        if (isAlarmRinging) return
        isAlarmRinging = true
        activeAlarm = alarm.copy(isRinging = true)

        alarmRingingJob?.cancel()
        alarmRingingJob = scope.launch {
            // Initial alert
            PlatformAlarm.playAlarm(alarm.tone.id)
            if (alarm.vibrate) {
                PlatformAlarm.vibrate()
            }
            if (alarm.voiceAnnouncement) {
                val dist = alarm.distanceRemainingKm?.toInt()?.let { "$it kilometers" } ?: "${alarm.triggerDistanceKm.toInt()} kilometers"
                PlatformSpeech.speak(
                    "Attention passenger. Your destination station ${alarm.stationName} is approaching in $dist. Please wake up and prepare to deboard."
                )
            }

            // Continuous nonstop loop until dismissed or disarmed by user
            var loopIndex = 0
            while (isAlarmRinging && activeAlarm?.isRinging == true) {
                delay(2500)
                loopIndex++
                if (!isAlarmRinging || activeAlarm?.isRinging != true) break

                PlatformAlarm.playAlarm(alarm.tone.id)
                if (alarm.vibrate) {
                    PlatformAlarm.vibrate()
                }

                // Periodic vocal wake-up call every 20 seconds
                if (alarm.voiceAnnouncement && loopIndex % 8 == 0) {
                    PlatformSpeech.speak(
                        "Attention passenger. Approaching destination station ${alarm.stationName}. Please wake up."
                    )
                }
            }
        }
    }

    fun dismissAlarm() {
        snoozeJob?.cancel()
        snoozeJob = null
        snoozeRemainingSeconds = 0
        alarmRingingJob?.cancel()
        alarmRingingJob = null
        isAlarmRinging = false
        activeAlarm = activeAlarm?.copy(isArmed = false, isRinging = false, isSnoozed = false)
        try {
            PlatformStorage.remove(KEY_ACTIVE_ALARM)
        } catch (_: Throwable) {}
        PlatformAlarm.stopAlarm()
        PlatformAlarm.stopVibrate()
        PlatformSpeech.stop()
        stopGpsTrackingIfIdle()
    }

    fun snoozeAlarm(minutes: Int = 2) {
        val validMinutes = minutes.coerceAtLeast(1)
        snoozeJob?.cancel()
        alarmRingingJob?.cancel()
        alarmRingingJob = null
        isAlarmRinging = false
        snoozeRemainingSeconds = validMinutes * 60
        activeAlarm = activeAlarm?.copy(isRinging = false, isSnoozed = true)
        PlatformAlarm.stopAlarm()
        PlatformAlarm.stopVibrate()
        PlatformSpeech.stop()

        snoozeJob = scope.launch {
            while (snoozeRemainingSeconds > 0) {
                delay(1000)
                snoozeRemainingSeconds--
            }
            if (activeAlarm?.isArmed == true) {
                activeAlarm = activeAlarm?.copy(isSnoozed = false)
                triggerAlarm()
            }
        }
    }

    fun testAlarmSound(tone: AlarmTone, voice: Boolean, vibrate: Boolean) {
        scope.launch {
            PlatformAlarm.playAlarm(tone.id)
            if (vibrate) PlatformAlarm.vibrate()
            if (voice) {
                PlatformSpeech.speak("Attention passenger. Wake up alarm test successful.")
            }
            delay(3500)
            PlatformAlarm.stopAlarm()
            PlatformAlarm.stopVibrate()
            PlatformSpeech.stop()
        }
    }

    fun checkAlarmGeofence(gpsLoc: GeoLocation? = null) {
        val alarm = activeAlarm ?: return
        if (!alarm.isArmed || alarm.isRinging) return

        val currentStatus = liveStatus
        val stations = currentStatus?.stations ?: emptyList()

        // 1. Locate destination station on the train route
        val targetIdx = if (stations.isNotEmpty()) {
            stations.indexOfFirst {
                it.code.equals(alarm.stationCode, ignoreCase = true) ||
                it.name.contains(alarm.stationName, ignoreCase = true) ||
                alarm.stationName.contains(it.name, ignoreCase = true)
            }
        } else -1
        val targetStation = if (targetIdx >= 0) stations[targetIdx] else null

        // 2. Calculate remaining distance (km) to the destination station
        val effectiveKm: Double? = when {
            // Case A: Passenger physically inside the train with GPS active
            isInsideTrain && (gpsLoc ?: currentGpsLocation) != null -> {
                val loc = gpsLoc ?: currentGpsLocation!!
                val (stnLat, stnLon) = if (alarm.stationLat != 0.0 && alarm.stationLon != 0.0) {
                    alarm.stationLat to alarm.stationLon
                } else if (targetStation != null && targetStation.latitude != 0.0) {
                    targetStation.latitude to targetStation.longitude
                } else {
                    StationDataService.getCoordinates(alarm.stationCode)
                }
                if (stnLat != 0.0 && stnLon != 0.0) {
                    calculateHaversineDistanceKm(loc.latitude, loc.longitude, stnLat, stnLon)
                } else null
            }

            // Case B: Live train tracking along route (uses train's live position & odometer)
            targetStation != null -> {
                val rem = getLiveRemainingKmToStation(targetStation)
                if (rem != null) {
                    rem
                } else {
                    // rem is null when train has already reached or passed targetStation
                    val curIdx = findStationIndex(lastConfirmedStationName.ifBlank { currentStatus?.currentStation ?: "" }, stations)
                    if (curIdx >= targetIdx && targetIdx >= 0) 0.0 else null
                }
            }

            // Case C: Fallback coordinates when route stations not loaded yet
            alarm.stationLat != 0.0 && alarm.stationLon != 0.0 && isInsideTrain && (gpsLoc ?: currentGpsLocation) != null -> {
                val loc = (gpsLoc ?: currentGpsLocation)!!
                calculateHaversineDistanceKm(loc.latitude, loc.longitude, alarm.stationLat, alarm.stationLon)
            }

            else -> null
        }

        if (effectiveKm == null) return

        val roundedKm = ((effectiveKm * 10).toInt() / 10.0).coerceAtLeast(0.0)
        val speed = (currentStatus?.speedKmh?.takeIf { it > 10 } ?: 65).toDouble()
        val remainingMinutes = ((roundedKm / speed) * 60).toInt().coerceAtLeast(1)

        activeAlarm = alarm.copy(
            distanceRemainingKm = roundedKm,
            minutesRemaining = remainingMinutes
        )

        // While snooze timer is active, do not trigger alarm from background geofence polling
        if (alarm.isSnoozed || snoozeRemainingSeconds > 0) return

        val shouldTrigger = when (alarm.triggerUnit) {
            AlarmTriggerUnit.KILOMETERS -> roundedKm <= alarm.triggerDistanceKm
            AlarmTriggerUnit.MINUTES -> remainingMinutes <= alarm.triggerMinutesBefore
        }

        if (shouldTrigger) {
            triggerAlarm()
        }
    }

    private fun calculateHaversineDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1 == 0.0 || lat2 == 0.0) return 999.0
        val r = 6371.0
        val dLat = (lat2 - lat1) * (kotlin.math.PI / 180.0)
        val dLon = (lon2 - lon1) * (kotlin.math.PI / 180.0)
        val a = kotlin.math.sin(dLat / 2).let { it * it } +
                kotlin.math.cos(lat1 * (kotlin.math.PI / 180.0)) * kotlin.math.cos(lat2 * (kotlin.math.PI / 180.0)) *
                kotlin.math.sin(dLon / 2).let { it * it }
        return r * 2.0 * kotlin.math.asin(kotlin.math.sqrt(a).coerceIn(-1.0, 1.0))
    }

    init {
        // Restore active destination alarm if user previously armed one and exited
        try {
            val savedAlarmJson = PlatformStorage.getString(KEY_ACTIVE_ALARM)
            if (!savedAlarmJson.isNullOrBlank()) {
                val restored = json.decodeFromString<DestinationAlarm>(savedAlarmJson)
                if (restored.isArmed) {
                    activeAlarm = restored
                    ensureGpsTracking()
                }
            }
        } catch (e: Throwable) {
            println("Error restoring alarm: ${e.message}")
        }
    }

    fun onQueryChanged(newQuery: String) {
        searchQuery = newQuery
        if (newQuery.trim().isEmpty()) {
            isSearchDropdownOpen = false
            searchResults = emptyList()
            searchDebounceJob?.cancel()
            return
        }

        isSearchDropdownOpen = true
        searchDebounceJob?.cancel()
        searchDebounceJob = scope.launch {
            delay(250) // 250ms debounce
            isSearching = true
            searchResults = service.searchTrains(newQuery)
            isSearching = false
        }
    }

    fun closeSearchDropdown() {
        isSearchDropdownOpen = false
    }

    // ── Phase 7: Real-Time Continuous Odometer & Monotonic Dead Reckoning Engine ──────
    var liveDistanceOffsetKm by mutableStateOf(0.0)
    var lastLiveStatusUpdateTimeMs by mutableStateOf(0L)
    var lastConfirmedStationName by mutableStateOf("")
    private val remainingKmCache = mutableMapOf<String, Double>()
    private var deadReckoningJob: Job? = null

    // ── Odometer persistence: survives app quit/reopen for same train ──────────
    @kotlinx.serialization.Serializable
    private data class OdometerSnapshot(
        val trainNumber: String,
        val confirmedStationName: String,
        val distanceOffsetKm: Double,
        val remainingKmCache: Map<String, Double>,
        val snapshotTimeMs: Long
    )

    private fun saveOdometerSnapshot() {
        val num = activeTrain.number
        if (num.isBlank()) return
        try {
            val snap = OdometerSnapshot(
                trainNumber = num,
                confirmedStationName = lastConfirmedStationName,
                distanceOffsetKm = liveDistanceOffsetKm,
                remainingKmCache = remainingKmCache.toMap(),
                snapshotTimeMs = com.railgaadi.app.util.PlatformDate.nowMillis()
            )
            PlatformStorage.saveString(KEY_ODOMETER, json.encodeToString(snap))
        } catch (_: Throwable) {}
    }

    /**
     * Restores the odometer from storage for the same train, projecting forward the
     * distance the train has likely covered while the app was closed, based on the
     * last known speed from the API.  Returns true if a valid snapshot was loaded.
     */
    private fun restoreOdometerSnapshot(trainNumber: String): Boolean {
        return try {
            val raw = PlatformStorage.getString(KEY_ODOMETER) ?: return false
            val snap = json.decodeFromString<OdometerSnapshot>(raw)
            if (snap.trainNumber != trainNumber) return false

            // How long was the app closed?
            val nowMs = com.railgaadi.app.util.PlatformDate.nowMillis()
            val gapSeconds = ((nowMs - snap.snapshotTimeMs) / 1000.0).coerceIn(0.0, 7200.0) // cap at 2h

            // Project forward with last known live speed (or conservative 60 km/h if unavailable)
            val speedKmh = liveStatus?.speedKmh?.takeIf { it > 0 }?.toDouble() ?: 60.0
            val projectedExtraKm = speedKmh * (gapSeconds / 3600.0)

            lastConfirmedStationName = snap.confirmedStationName
            liveDistanceOffsetKm = (snap.distanceOffsetKm + projectedExtraKm)
            remainingKmCache.clear()
            remainingKmCache.putAll(snap.remainingKmCache.mapValues { (_, v) ->
                // Reduce every cached remaining-distance by the same projected travel
                (v - projectedExtraKm).coerceAtLeast(0.1)
            })
            true
        } catch (_: Throwable) { false }
    }

    /**
     * Resilient pan-India station name resolver that strips prefixes like "Crossing",
     * "Before", "Near", "At", "Departed" and matches station codes or clean names.
     */
    fun findStationIndex(rawName: String, stations: List<Station>): Int {
        if (stations.isEmpty() || rawName.isBlank()) return -1
        val clean = rawName
            .replace(Regex("(?i)\\b(crossing|before|after|near|at|departed|approaching|halted at|passed)\\b"), "")
            .trim()

        // 1. Exact match on clean or raw
        val idxExact = stations.indexOfFirst {
            it.name.equals(clean, ignoreCase = true) || it.name.equals(rawName, ignoreCase = true)
        }
        if (idxExact >= 0) return idxExact

        // 2. Contains match (longer than 2 chars)
        val idxContains = stations.indexOfFirst {
            (clean.length >= 3 && it.name.contains(clean, ignoreCase = true)) ||
            (clean.length >= 3 && clean.contains(it.name, ignoreCase = true)) ||
            (it.code.isNotBlank() && (rawName.contains(it.code, ignoreCase = true) || clean.contains(it.code, ignoreCase = true)))
        }
        if (idxContains >= 0) return idxContains

        // 3. Token word matching (words with length >= 3)
        val tokens = clean.split(Regex("[\\s,]+")).filter { it.length >= 3 && !it.equals("express", ignoreCase = true) }
        if (tokens.isNotEmpty()) {
            val idxToken = stations.indexOfFirst { stn ->
                tokens.any { token -> stn.name.contains(token, ignoreCase = true) }
            }
            if (idxToken >= 0) return idxToken
        }

        return -1
    }

    fun onLiveStatusReceived(updated: LiveStatus) {
        liveStatus = updated
        lastLiveStatusUpdateTimeMs = PlatformDate.nowMillis()
        val stations = updated.stations

        val newStn = updated.currentStation.trim()
        if (newStn.isNotBlank() && stations.isNotEmpty()) {
            val newIdx = findStationIndex(newStn, stations)
                .takeIf { it >= 0 }
                // Fallback: derive index from progressPercent if name doesn't match
                ?: if (updated.progressPercent > 0f)
                    ((stations.size - 1) * updated.progressPercent).toInt().coerceIn(0, stations.size - 1)
                else -1

            val oldIdx = findStationIndex(lastConfirmedStationName, stations)
                .takeIf { it >= 0 } ?: -1

            if (newIdx >= 0 && (newIdx > oldIdx || lastConfirmedStationName.isBlank())) {
                // Train officially progressed to a new station along the route —
                // reset dead-reckoning offset from this new confirmed anchor point
                lastConfirmedStationName = newStn
                liveDistanceOffsetKm = 0.0

                // Evict remaining-km cache entries for stations now behind the train,
                // so the UI doesn't show stale distances for already-passed stations.
                // Also clear the current station's own entry so "0 km" shows correctly.
                val passedStations = stations.take(newIdx + 1).map { it.code }
                passedStations.forEach { code ->
                    remainingKmCache.keys.removeAll { it.startsWith("${activeTrain.number}_${code}_") }
                }
                // Additionally wipe any cache entry for the current station itself,
                // in case it was the previous "next station" with a stale distance.
                stations.getOrNull(newIdx)?.let { stn ->
                    val key = "${activeTrain.number}_${stn.code}_${stn.distanceKm.toInt()}"
                    remainingKmCache.remove(key)
                }
            }
        }

        if (stations.isNotEmpty()) {
            com.railgaadi.app.model.OfflineTrainEngine.cacheRoute(activeTrain.number, stations)
        }

        // Persist odometer so app-reopen resumes from correct position
        saveOdometerSnapshot()
        checkAlarmGeofence()
    }

    fun startDeadReckoningEngine() {
        deadReckoningJob?.cancel()
        deadReckoningJob = scope.launch {
            var saveTickCounter = 0
            while (true) {
                delay(1000) // 1-second tick
                val status = liveStatus ?: continue
                val stations = status.stations
                if (stations.isEmpty() || activeTrain.number.isBlank()) continue

                val speed = status.speedKmh
                if (isInsideTrain && currentGpsLocation != null) {
                    // Physical on-train GPS tracking — no dead reckoning needed
                    updateInTrainCockpit(currentGpsLocation!!)
                } else if (speed > 0) {
                    // Dead reckoning: add distance covered this second
                    val addedKm = speed.toDouble() / 3600.0

                    // Cap offset so it never overshoots into the next station
                    val curIdx = findStationIndex(lastConfirmedStationName.ifBlank { status.currentStation }, stations)
                    if (curIdx >= 0 && curIdx + 1 < stations.size) {
                        val segLength = (stations[curIdx + 1].distanceKm - stations[curIdx].distanceKm).coerceAtLeast(1.0)
                        val maxAllowed = (segLength - 0.1).coerceAtLeast(0.0)
                        liveDistanceOffsetKm = (liveDistanceOffsetKm + addedKm).coerceAtMost(maxAllowed)
                    } else {
                        liveDistanceOffsetKm += addedKm
                    }

                    // Persist odometer state to storage every 10 seconds
                    saveTickCounter++
                    if (saveTickCounter >= 10) {
                        saveTickCounter = 0
                        saveOdometerSnapshot()
                    }
                }
                // If speed == 0 (train halted at station), do nothing — distance stays frozen
            }
        }
    }

    fun getRelativeUpdateTimeText(): String {
        val ms = lastLiveStatusUpdateTimeMs
        if (ms <= 0L) return "Updated a few seconds ago"
        val diffSec = ((PlatformDate.nowMillis() - ms) / 1000).coerceAtLeast(0)
        return when {
            diffSec < 15 -> "Updated a few seconds ago"
            diffSec < 60 -> "Updated ${diffSec}s ago"
            diffSec < 3600 -> "Updated ${diffSec / 60}m ago"
            else -> "Updated a while ago"
        }
    }

    fun getLiveRemainingKmToStation(station: Station, baseStation: Station? = null): Double? {
        val status = liveStatus ?: return null
        val stations = status.stations
        if (stations.isEmpty()) return null

        val stationKey = "${activeTrain.number}_${station.code}_${station.distanceKm.toInt()}"

        // 1. If passenger is inside the train with physical GPS active
        if (isInsideTrain && currentGpsLocation != null) {
            val loc = currentGpsLocation!!
            val (stnLat, stnLon) = if (station.latitude != 0.0 && station.longitude != 0.0) {
                station.latitude to station.longitude
            } else {
                StationDataService.getCoordinates(station.code)
            }
            if (stnLat != 0.0 && stnLon != 0.0) {
                val rawDist = calculateHaversineDistanceKm(loc.latitude, loc.longitude, stnLat, stnLon)
                val rounded = ((rawDist * 10).toInt() / 10.0).coerceAtLeast(0.0)

                // Strictly non-increasing: remaining distance can never increase
                val prev = remainingKmCache[stationKey]
                val effective = if (prev != null && prev > 0.0) minOf(prev, rounded) else rounded
                remainingKmCache[stationKey] = effective
                return effective
            }
        }

        // 1b. If passenger is inside the train with Multi-Tower Trilateration lock (±80m accuracy)
        if (isInsideTrain && currentTrilaterationResult != null) {
            val tri = currentTrilaterationResult!!
            if (tri.approachingStationCode.equals(station.code, ignoreCase = true) ||
                station.name.contains(tri.approachingStationName, ignoreCase = true)) {
                val rounded = ((tri.remainingDistanceKm * 10).toInt() / 10.0).coerceAtLeast(0.0)
                val prev = remainingKmCache[stationKey]
                val effective = if (prev != null && prev > 0.0) minOf(prev, rounded) else rounded
                remainingKmCache[stationKey] = effective
                return effective
            }
        }

        // 1c. If passenger is inside the train with physical Cell Tower lock (offline / metal coach)
        if (isInsideTrain && currentCellRecord != null) {
            val record = currentCellRecord!!
            if (record.stationCode.equals(station.code, ignoreCase = true) ||
                station.name.contains(record.stationName, ignoreCase = true)) {
                val rounded = ((record.distanceToStationKm * 10).toInt() / 10.0).coerceAtLeast(0.0)
                val prev = remainingKmCache[stationKey]
                val effective = if (prev != null && prev > 0.0) minOf(prev, rounded) else rounded
                remainingKmCache[stationKey] = effective
                return effective
            }
        }

        // 2. Track-based continuous dead-reckoning odometer
        val curIdx = findStationIndex(lastConfirmedStationName.ifBlank { status.currentStation }, stations)
            .takeIf { it >= 0 }
            ?: baseStation?.let { bs -> findStationIndex(bs.name, stations).takeIf { it >= 0 } }
            ?: ((stations.size - 1) * status.progressPercent).toInt().coerceIn(0, stations.size - 1)

        val curStn = stations.getOrNull(curIdx) ?: return null

        val targetIdx = findStationIndex(station.name, stations).takeIf { it >= 0 }
            ?: stations.indexOfFirst { it.code.equals(station.code, ignoreCase = true) }

        // If the station is already behind the train along the route, clear cache and return null
        if (targetIdx in 0 until curIdx) {
            remainingKmCache.remove(stationKey)
            return null
        }

        // If the train has arrived at this exact station
        if (targetIdx == curIdx) {
            remainingKmCache.remove(stationKey)
            return 0.0
        }

        // Target station is ahead of current train position
        val baseDistanceDiff = if (station.distanceKm > curStn.distanceKm) {
            station.distanceKm - curStn.distanceKm
        } else {
            // Coordinate-based Haversine fallback if distanceKm is 0 or missing in API
            val (cLat, cLon) = if (curStn.latitude != 0.0 && curStn.longitude != 0.0) {
                curStn.latitude to curStn.longitude
            } else {
                StationDataService.getCoordinates(curStn.code)
            }
            val (sLat, sLon) = if (station.latitude != 0.0 && station.longitude != 0.0) {
                station.latitude to station.longitude
            } else {
                StationDataService.getCoordinates(station.code)
            }
            if (cLat != 0.0 && sLat != 0.0) {
                calculateHaversineDistanceKm(cLat, cLon, sLat, sLon)
            } else {
                val hopCount = if (targetIdx > curIdx) targetIdx - curIdx else 1
                hopCount * 12.0
            }
        }

        val estimatedRemaining = (baseDistanceDiff - liveDistanceOffsetKm).coerceAtLeast(0.0)
        val rounded = ((estimatedRemaining * 10).toInt() / 10.0)

        // Strictly non-increasing: remaining distance to any upcoming station can never increase
        val prev = remainingKmCache[stationKey]
        val effective = if (prev != null && prev > 0.0) minOf(prev, rounded) else rounded
        remainingKmCache[stationKey] = effective
        return effective
    }

    // Live Auto-Refresh Polling Engine (Seamless 3-second live updates in background)
    const val AUTO_REFRESH_INTERVAL_SECONDS = 3
    var autoRefreshCountdown by mutableStateOf(AUTO_REFRESH_INTERVAL_SECONDS)
    var isAutoRefreshing by mutableStateOf(false)
    var isAutoRefreshEnabled by mutableStateOf(true)
    private var autoRefreshJob: Job? = null

    fun startAutoRefresh(intervalSeconds: Int = AUTO_REFRESH_INTERVAL_SECONDS) {
        if (!isAutoRefreshEnabled) return
        autoRefreshJob?.cancel()
        autoRefreshCountdown = intervalSeconds

        autoRefreshJob = scope.launch {
            while (true) {
                delay(1000)
                if (!isAutoRefreshEnabled || activeTrain.number.isBlank()) continue

                if (autoRefreshCountdown > 1) {
                    autoRefreshCountdown -= 1
                } else {
                    autoRefreshCountdown = 0
                    isAutoRefreshing = true
                    try {
                        if (!isOfflineModeEnabled) {
                            val updated = service.getLiveStatus(activeTrain.number)
                            onLiveStatusReceived(updated)
                            refreshWeather(silent = true)
                        } else {
                            // In offline mode, calculate position using Hybrid engine (GPS / Multi-Tower Trilateration / Cell Tower)
                            val loc = currentGpsLocation ?: PlatformLocation.getLastLocation()
                            val offlineLive = com.railgaadi.app.model.OfflineTrainEngine.calculateHybridOfflineLiveStatus(
                                train = activeTrain,
                                gps = loc,
                                cell = currentCellTower,
                                visibleCells = visibleCellTowers,
                                existingStations = liveStatus?.stations
                            )
                            onLiveStatusReceived(offlineLive)
                        }
                    } catch (_: Exception) {
                        // Resilient offline fallback: calculate position via Hybrid Engine (GPS / Cell Tower Trilateration) if internet drops
                        if (activeTrain.number.isNotBlank()) {
                            val loc = currentGpsLocation ?: PlatformLocation.getLastLocation()
                            val offlineLive = com.railgaadi.app.model.OfflineTrainEngine.calculateHybridOfflineLiveStatus(
                                train = activeTrain,
                                gps = loc,
                                cell = currentCellTower,
                                visibleCells = visibleCellTowers,
                                existingStations = liveStatus?.stations
                            )
                            onLiveStatusReceived(offlineLive)
                        }
                    } finally {
                        isAutoRefreshing = false
                        autoRefreshCountdown = intervalSeconds
                    }
                }
            }
        }
    }

    fun stopAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun triggerManualRefresh() {
        if (activeTrain.number.isBlank()) return
        autoRefreshCountdown = AUTO_REFRESH_INTERVAL_SECONDS
        scope.launch {
            isAutoRefreshing = true
            try {
                val updated = service.getLiveStatus(activeTrain.number)
                onLiveStatusReceived(updated)
                refreshWeather(silent = true)
            } catch (_: Exception) {
            } finally {
                isAutoRefreshing = false
            }
        }
    }

    fun clearActiveTrain() {
        activeTrain = Train("", "", "", "", "", "", "", 0, "")
        liveStatus = null
        searchQuery = ""
        isSearchDropdownOpen = false
        searchResults = emptyList()
        selectedWeatherStationName = null
        autoRefreshJob?.cancel()
        deadReckoningJob?.cancel()
        deadReckoningJob = null
        lastConfirmedStationName = ""
        liveDistanceOffsetKm = 0.0
        remainingKmCache.clear()
        lastLiveStatusUpdateTimeMs = 0L
        isInsideTrain = false
        inTrainLocationText = ""
        inTrainNextHaltText = ""
        inTrainEstArrivalMinutes = 0
        inTrainRemainingKm = 0.0
        currentCellTower = null
        currentCellRecord = null
        visibleCellTowers = emptyList()
        currentTrilaterationResult = null
        activeTrackingProvider = "Cloud"
        try { PlatformStorage.remove(KEY_ODOMETER) } catch (_: Throwable) {}
        stopGpsTrackingIfIdle()
        stopCellScanningIfIdle()
    }

    fun selectTrain(train: Train, boardingStationInput: String? = null) {
        if (train.number.isBlank()) return   // guard: no-op for empty placeholder train

        val isSameTrain = (activeTrain.number == train.number && train.number.isNotBlank())

        activeTrain = train
        searchQuery = "${train.number} - ${train.name}"
        isSearchDropdownOpen = false

        // If switching to a DIFFERENT train, fully reset the odometer
        if (!isSameTrain) {
            deadReckoningJob?.cancel()
            deadReckoningJob = null
            lastConfirmedStationName = ""
            liveDistanceOffsetKm = 0.0
            remainingKmCache.clear()
        }
        // If re-opening same train: odometer will be restored from storage after the first fetch

        if (!boardingStationInput.isNullOrBlank()) {
            val resolved = StationDataService.resolveStation(boardingStationInput)
            if (resolved != null) {
                userBoardingStation = resolved
            }
        }

        // Update recent searches
        if (!recentSearches.contains(train.number)) {
            recentSearches = (listOf(train.number) + recentSearches).take(6)
        }
        addRecentTrain(train)

        // Ensure real-time physical GPS tracking is active
        ensureGpsTracking()

        selectedWeatherStationName = null
        scope.launch {
            isLoadingStatus = true
            try {
                val status = service.getLiveStatus(train.number)
                // Try to restore saved odometer for this train (app was re-opened)
                // restoreOdometerSnapshot must run before onLiveStatusReceived so the
                // confirmed-station comparison uses the saved anchor, not a blank slate
                if (!isSameTrain || lastConfirmedStationName.isBlank()) {
                    restoreOdometerSnapshot(train.number)
                }
                onLiveStatusReceived(status)
                startDeadReckoningEngine()
            } catch (_: Exception) {
                val loc = currentGpsLocation ?: PlatformLocation.getLastLocation()
                if (!isSameTrain || lastConfirmedStationName.isBlank()) {
                    restoreOdometerSnapshot(train.number)
                }
                val offlineLive = com.railgaadi.app.model.OfflineTrainEngine.calculateHybridOfflineLiveStatus(
                    train = train,
                    gps = loc,
                    cell = currentCellTower,
                    visibleCells = visibleCellTowers,
                    existingStations = liveStatus?.stations
                )
                onLiveStatusReceived(offlineLive)
                startDeadReckoningEngine()
            }
            isLoadingStatus = false
            refreshWeather(silent = false)
            startAutoRefresh(AUTO_REFRESH_INTERVAL_SECONDS)
        }
    }

    fun refreshLiveStatus() {
        triggerManualRefresh()
    }

    fun selectByNumber(number: String) {
        scope.launch {
            isLoadingStatus = true
            val trains = service.searchTrains(number)
            val train = trains.find { it.number == number } ?: trains.firstOrNull()
            if (train != null) {
                selectTrain(train)
            }
            isLoadingStatus = false
        }
    }

    var selectedWeatherStationName by mutableStateOf<String?>(null)

    fun selectWeatherStation(stationName: String?) {
        selectedWeatherStationName = stationName
        refreshWeather(silent = false)
    }

    fun refreshWeather(silent: Boolean = false) {
        val stations = liveStatus?.stations ?: return
        if (stations.isEmpty()) return

        // 1. Determine target station name (user selected corridor station OR live train current station)
        val selectedTarget = selectedWeatherStationName?.substringBefore(" (")?.trim()
        val liveTarget = (liveStatus?.currentStation ?: "").substringBefore(" (").trim()
        val query = if (!selectedTarget.isNullOrBlank()) selectedTarget else liveTarget

        // 2. Resolve matching station from live train route
        val currentStation = stations.find { stn ->
            val cleanName = stn.name.substringBefore(" (").trim()
            stn.code.equals(query, ignoreCase = true) ||
            cleanName.contains(query, ignoreCase = true) ||
            (query.isNotEmpty() && query.contains(cleanName, ignoreCase = true))
        } ?: stations.firstOrNull() ?: return

        // 3. Obtain real GPS coordinates (provided dynamically by API or station cache)
        val coords = if (currentStation.latitude != 0.0 && currentStation.longitude != 0.0) {
            Pair(currentStation.latitude, currentStation.longitude)
        } else {
            StationDataService.getCoordinates(currentStation.code)
        }

        val lat = coords.first
        val lon = coords.second
        if (lat == 0.0 && lon == 0.0) return

        scope.launch {
            if (!silent) isLoadingWeather = true
            try {
                weatherData = weatherService.getStationWeatherData(currentStation.name, lat, lon)
                nearbyLandmarks = LandmarkService.getNearbyLandmarks(lat, lon)
            } catch (_: Exception) { }
            if (!silent) isLoadingWeather = false
        }
    }
}
