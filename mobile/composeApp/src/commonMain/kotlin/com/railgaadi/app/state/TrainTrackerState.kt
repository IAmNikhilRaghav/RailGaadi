package com.railgaadi.app.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.railgaadi.app.model.AlarmTone
import com.railgaadi.app.model.AlarmTriggerUnit
import com.railgaadi.app.model.DestinationAlarm
import com.railgaadi.app.model.LiveStatus
import com.railgaadi.app.model.Station
import com.railgaadi.app.model.StationDataService
import com.railgaadi.app.model.Train
import com.railgaadi.app.network.LandmarkService
import com.railgaadi.app.network.RailRadarService
import com.railgaadi.app.network.WeatherService
import com.railgaadi.app.util.GeoLocation
import com.railgaadi.app.util.PlatformAlarm
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

    // ── Phase 5: Satellite GPS Tracking Engine (Hybrid Foreground & Background) ──
    var isOfflineModeEnabled by mutableStateOf(false)
    var isGpsTrackingActive by mutableStateOf(false)
    var currentGpsLocation by mutableStateOf<GeoLocation?>(null)
    var offlineGpsStatusText by mutableStateOf("Standby")
    private var isGpsListening = false

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
        if (!isOfflineModeEnabled && activeAlarm?.isArmed != true) {
            try {
                PlatformLocation.stopLocationUpdates()
                isGpsListening = false
                isGpsTrackingActive = false
            } catch (_: Throwable) {}
        }
    }

    private fun onGpsLocationUpdated(loc: GeoLocation) {
        currentGpsLocation = loc
        val speedText = if (loc.speedKmh > 5.0) "${loc.speedKmh.toInt()} km/h" else "±${loc.accuracyMeters.toInt().coerceAtLeast(1)}m"
        if (isOfflineModeEnabled) {
            offlineGpsStatusText = "🛰️ GPS ACTIVE • $speedText"
        }

        // 1. Immediately evaluate destination alarm geofence with physical GPS
        checkAlarmGeofence(loc)

        // 2. Dynamically synchronize train status and station crossings along route
        syncLiveStatusWithGps(loc)
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
        offlineGpsStatusText = "Connecting to Satellites..."
        ensureGpsTracking()
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
        val validMinutes = minutes.coerceAtLeast(2)
        snoozeJob?.cancel()
        alarmRingingJob?.cancel()
        alarmRingingJob = null
        isAlarmRinging = false
        activeAlarm = activeAlarm?.copy(isRinging = false, isSnoozed = true)
        PlatformAlarm.stopAlarm()
        PlatformAlarm.stopVibrate()
        PlatformSpeech.stop()

        snoozeJob = scope.launch {
            delay(validMinutes * 60 * 1000L)
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

        val loc = gpsLoc ?: currentGpsLocation ?: PlatformLocation.getLastLocation()

        // 1. High-precision real device GPS geofence:
        // Calculate remaining physical distance to station coordinates directly
        if (loc != null && alarm.stationLat != 0.0 && alarm.stationLon != 0.0) {
            val remainingKm = calculateHaversineDistanceKm(loc.latitude, loc.longitude, alarm.stationLat, alarm.stationLon)
            val speed = if (loc.speedKmh > 10.0) loc.speedKmh else (liveStatus?.speedKmh?.toDouble()?.takeIf { it > 10.0 } ?: 65.0)
            val remainingMinutes = ((remainingKm / speed) * 60).toInt().coerceAtLeast(1)

            val roundedKm = ((remainingKm * 10).toInt() / 10.0).coerceAtLeast(0.0)
            val updated = alarm.copy(
                distanceRemainingKm = roundedKm,
                minutesRemaining = remainingMinutes
            )
            activeAlarm = updated

            // While snooze timer is active, do not trigger alarm from background geofence polling
            if (alarm.isSnoozed) return

            val shouldTrigger = when (alarm.triggerUnit) {
                AlarmTriggerUnit.KILOMETERS -> remainingKm <= alarm.triggerDistanceKm
                AlarmTriggerUnit.MINUTES -> remainingMinutes <= alarm.triggerMinutesBefore
            }

            if (shouldTrigger) {
                triggerAlarm()
            }
            return
        }

        // 2. Fallback: If device GPS is not acquired yet, calculate from timetable
        val currentStatus = liveStatus ?: return
        val stations = currentStatus.stations
        if (stations.isEmpty()) return

        val targetIdx = stations.indexOfFirst {
            it.code.equals(alarm.stationCode, ignoreCase = true) ||
            it.name.contains(alarm.stationName, ignoreCase = true)
        }
        if (targetIdx < 0) return

        val curStationName = currentStatus.currentStation
        val curIdx = stations.indexOfFirst {
            it.name.contains(curStationName, ignoreCase = true) ||
            (curStationName.contains(it.code, ignoreCase = true) && it.code.isNotBlank())
        }.takeIf { it >= 0 } ?: ((stations.size - 1) * currentStatus.progressPercent).toInt().coerceIn(0, stations.size - 1)

        val curStation = stations.getOrNull(curIdx) ?: return
        val targetStation = stations[targetIdx]

        val remainingKm = if (targetStation.distanceKm >= curStation.distanceKm && curStation.distanceKm > 0) {
            (targetStation.distanceKm - curStation.distanceKm).toDouble()
        } else {
            val (lat1, lon1) = if (curStation.latitude != 0.0) curStation.latitude to curStation.longitude else StationDataService.getCoordinates(curStation.code)
            val (lat2, lon2) = if (targetStation.latitude != 0.0) targetStation.latitude to targetStation.longitude else StationDataService.getCoordinates(targetStation.code)
            calculateHaversineDistanceKm(lat1, lon1, lat2, lon2)
        }

        val speed = (currentStatus.speedKmh.takeIf { it > 10 } ?: 65).toDouble()
        val remainingMinutes = ((remainingKm / speed) * 60).toInt().coerceAtLeast(1)

        activeAlarm = alarm.copy(
            distanceRemainingKm = ((remainingKm * 10).toInt() / 10.0).coerceAtLeast(0.0),
            minutesRemaining = remainingMinutes
        )

        // While snooze timer is active, do not trigger alarm from background geofence polling
        if (alarm.isSnoozed) return

        val shouldTrigger = when (alarm.triggerUnit) {
            AlarmTriggerUnit.KILOMETERS -> remainingKm <= alarm.triggerDistanceKm
            AlarmTriggerUnit.MINUTES -> remainingMinutes <= alarm.triggerMinutesBefore
        }

        if (shouldTrigger && curIdx <= targetIdx) {
            triggerAlarm()
        }
    }

    private fun computeGpsStationCrossing(
        loc: GeoLocation,
        stations: List<Station>
    ): Triple<Station?, Station?, Float> {
        if (stations.isEmpty()) return Triple(null, null, 0f)
        val distances = stations.mapIndexed { idx, stn ->
            val (lat, lon) = if (stn.latitude != 0.0 && stn.longitude != 0.0) {
                stn.latitude to stn.longitude
            } else {
                StationDataService.getCoordinates(stn.code)
            }
            Triple(idx, stn, calculateHaversineDistanceKm(loc.latitude, loc.longitude, lat, lon))
        }

        val closest = distances.minByOrNull { it.third } ?: return Triple(null, null, 0f)
        if (closest.third > 20.0) return Triple(null, null, 0f)

        val closestIdx = closest.first
        val nextIdx = if (closestIdx < stations.size - 1) {
            val nextStn = stations[closestIdx + 1]
            val (nextLat, nextLon) = if (nextStn.latitude != 0.0) nextStn.latitude to nextStn.longitude else StationDataService.getCoordinates(nextStn.code)
            val dToNext = calculateHaversineDistanceKm(loc.latitude, loc.longitude, nextLat, nextLon)
            val (closestLat, closestLon) = if (closest.second.latitude != 0.0) closest.second.latitude to closest.second.longitude else StationDataService.getCoordinates(closest.second.code)
            val dClosestToNext = calculateHaversineDistanceKm(closestLat, closestLon, nextLat, nextLon)
            if (dToNext < dClosestToNext) closestIdx + 1 else closestIdx
        } else closestIdx

        val passedIdx = (nextIdx - 1).coerceAtLeast(0)
        val passedStation = stations[passedIdx]
        val approachingStation = stations[nextIdx]
        val progress = (nextIdx.toFloat() / (stations.size - 1).coerceAtLeast(1)).coerceIn(0f, 1f)

        return Triple(passedStation, approachingStation, progress)
    }

    private fun syncLiveStatusWithGps(loc: GeoLocation) {
        if (activeTrain.number.isBlank()) return
        val current = liveStatus ?: return
        val stations = current.stations
        if (stations.isEmpty()) return

        val (passedStation, approachingStation, progress) = computeGpsStationCrossing(loc, stations)
        if (passedStation != null && approachingStation != null) {
            val effectiveSpeed = if (loc.speedKmh > 5.0) loc.speedKmh.toInt() else current.speedKmh
            liveStatus = current.copy(
                currentStation = "${passedStation.name} (${passedStation.code})",
                nextStation = "${approachingStation.name} (${approachingStation.code})",
                progressPercent = progress,
                speedKmh = effectiveSpeed
            )
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
                            val loc = currentGpsLocation ?: PlatformLocation.getLastLocation()
                            val effectiveStatus = if (loc != null && updated.stations.isNotEmpty()) {
                                val (passedStn, apprStn, prog) = computeGpsStationCrossing(loc, updated.stations)
                                if (passedStn != null && apprStn != null) {
                                    updated.copy(
                                        currentStation = "${passedStn.name} (${passedStn.code})",
                                        nextStation = "${apprStn.name} (${apprStn.code})",
                                        progressPercent = prog,
                                        speedKmh = if (loc.speedKmh > 5.0) loc.speedKmh.toInt() else updated.speedKmh
                                    )
                                } else updated
                            } else updated

                            liveStatus = effectiveStatus
                            if (updated.stations.isNotEmpty()) {
                                com.railgaadi.app.model.OfflineTrainEngine.cacheRoute(activeTrain.number, updated.stations)
                            }
                            refreshWeather(silent = true)
                            checkAlarmGeofence()
                        }
                    } catch (_: Exception) {
                        // Resilient offline fallback: calculate position via on-device GPS if internet drops
                        val loc = currentGpsLocation ?: PlatformLocation.getLastLocation()
                        if (loc != null && activeTrain.number.isNotBlank()) {
                            val offlineLive = com.railgaadi.app.model.OfflineTrainEngine.calculateOfflineLiveStatus(
                                train = activeTrain,
                                gps = loc,
                                existingStations = liveStatus?.stations
                            )
                            liveStatus = offlineLive
                            checkAlarmGeofence()
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
                val loc = currentGpsLocation ?: PlatformLocation.getLastLocation()
                val effectiveStatus = if (loc != null && updated.stations.isNotEmpty()) {
                    val (passedStn, apprStn, prog) = computeGpsStationCrossing(loc, updated.stations)
                    if (passedStn != null && apprStn != null) {
                        updated.copy(
                            currentStation = "${passedStn.name} (${passedStn.code})",
                            nextStation = "${apprStn.name} (${apprStn.code})",
                            progressPercent = prog,
                            speedKmh = if (loc.speedKmh > 5.0) loc.speedKmh.toInt() else updated.speedKmh
                        )
                    } else updated
                } else updated

                liveStatus = effectiveStatus
                refreshWeather(silent = true)
                checkAlarmGeofence()
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
        stopGpsTrackingIfIdle()
    }

    fun selectTrain(train: Train, boardingStationInput: String? = null) {
        if (train.number.isBlank()) return   // guard: no-op for empty placeholder train
        activeTrain = train
        searchQuery = "${train.number} - ${train.name}"
        isSearchDropdownOpen = false

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
                liveStatus = status
                if (status.stations.isNotEmpty()) {
                    com.railgaadi.app.model.OfflineTrainEngine.cacheRoute(train.number, status.stations)
                }
            } catch (_: Exception) {
                val loc = currentGpsLocation ?: PlatformLocation.getLastLocation()
                if (loc != null) {
                    liveStatus = com.railgaadi.app.model.OfflineTrainEngine.calculateOfflineLiveStatus(train, loc)
                }
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
