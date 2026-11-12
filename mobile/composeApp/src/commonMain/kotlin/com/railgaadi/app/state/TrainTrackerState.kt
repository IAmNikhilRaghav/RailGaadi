package com.railgaadi.app.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.railgaadi.app.model.LiveStatus
import com.railgaadi.app.model.Station
import com.railgaadi.app.model.StationDataService
import com.railgaadi.app.model.Train
import com.railgaadi.app.network.LandmarkService
import com.railgaadi.app.network.RailRadarService
import com.railgaadi.app.network.WeatherService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object TrainTrackerState {
    private val service = RailRadarService()
    private val weatherService = WeatherService()
    private val scope = CoroutineScope(Dispatchers.Main)
    private var searchDebounceJob: Job? = null

    var searchQuery by mutableStateOf("")
    var isSearchDropdownOpen by mutableStateOf(false)
    var isSearching by mutableStateOf(false)
    var searchResults by mutableStateOf<List<Train>>(emptyList())
    // Start with empty history — populated as user searches
    var recentSearches by mutableStateOf(listOf<String>())

    // Recently searched trains (max 3 with delete feature)
    var recentTrains by mutableStateOf<List<Train>>(
        listOf(
            Train("12404", "Prayagraj SF Express", "Superfast Express", "Lalgarh Jn (LGNH)", "Prayagraj Jn (PRYJ)", "10:30", "04:45", 1095, "2"),
            Train("22436", "Vande Bharat Express", "Vande Bharat", "New Delhi (NDLS)", "Varanasi Jn (BSB)", "06:00", "14:00", 480, "16"),
            Train("12952", "Mumbai Tejas Rajdhani", "Rajdhani Express", "New Delhi (NDLS)", "Mumbai Central (MMCT)", "16:55", "08:35", 940, "3")
        )
    )

    fun addRecentTrain(train: Train) {
        if (train.number.isBlank()) return
        val filtered = recentTrains.filter { it.number != train.number }
        recentTrains = (listOf(train) + filtered).take(3)
    }

    fun removeRecentTrain(trainNumber: String) {
        recentTrains = recentTrains.filter { it.number != trainNumber }
    }

    fun clearRecentTrains() {
        recentTrains = emptyList()
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
    var activeAlarm by mutableStateOf<com.railgaadi.app.model.DestinationAlarm?>(null)
    var showAlarmSetupModal by mutableStateOf(false)
    var targetAlarmStation by mutableStateOf<Station?>(null)
    var isAlarmRinging by mutableStateOf(false)

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
        triggerUnit: com.railgaadi.app.model.AlarmTriggerUnit = com.railgaadi.app.model.AlarmTriggerUnit.KILOMETERS,
        voiceAnnouncement: Boolean = true,
        vibrate: Boolean = true,
        tone: com.railgaadi.app.model.AlarmTone = com.railgaadi.app.model.AlarmTone.RAILWAY_CHIME
    ) {
        val (lat, lon) = if (station.latitude != 0.0 && station.longitude != 0.0) {
            station.latitude to station.longitude
        } else {
            StationDataService.getCoordinates(station.code)
        }

        activeAlarm = com.railgaadi.app.model.DestinationAlarm(
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
        showAlarmSetupModal = false
        checkAlarmGeofence()
    }

    fun disarmAlarm() {
        activeAlarm = null
        isAlarmRinging = false
        com.railgaadi.app.util.PlatformAlarm.stopAlarm()
        com.railgaadi.app.util.PlatformAlarm.stopVibrate()
        com.railgaadi.app.util.PlatformSpeech.stop()
    }

    fun triggerAlarm() {
        val alarm = activeAlarm ?: return
        if (isAlarmRinging) return
        isAlarmRinging = true
        activeAlarm = alarm.copy(isRinging = true)

        com.railgaadi.app.util.PlatformAlarm.playAlarm(alarm.tone.id)
        if (alarm.vibrate) {
            com.railgaadi.app.util.PlatformAlarm.vibrate()
        }
        if (alarm.voiceAnnouncement) {
            val dist = alarm.distanceRemainingKm?.toInt()?.let { "$it kilometers" } ?: "${alarm.triggerDistanceKm.toInt()} kilometers"
            com.railgaadi.app.util.PlatformSpeech.speak(
                "Attention passenger. Your destination station ${alarm.stationName} is approaching in $dist. Please wake up and prepare to deboard."
            )
        }
    }

    fun dismissAlarm() {
        isAlarmRinging = false
        activeAlarm = activeAlarm?.copy(isArmed = false, isRinging = false)
        com.railgaadi.app.util.PlatformAlarm.stopAlarm()
        com.railgaadi.app.util.PlatformAlarm.stopVibrate()
        com.railgaadi.app.util.PlatformSpeech.stop()
    }

    fun snoozeAlarm(minutes: Int = 5) {
        isAlarmRinging = false
        activeAlarm = activeAlarm?.copy(isRinging = false, isSnoozed = true)
        com.railgaadi.app.util.PlatformAlarm.stopAlarm()
        com.railgaadi.app.util.PlatformAlarm.stopVibrate()
        com.railgaadi.app.util.PlatformSpeech.stop()

        scope.launch {
            delay(minutes * 60 * 1000L)
            if (activeAlarm?.isArmed == true) {
                triggerAlarm()
            }
        }
    }

    fun testAlarmSound(tone: com.railgaadi.app.model.AlarmTone, voice: Boolean, vibrate: Boolean) {
        scope.launch {
            com.railgaadi.app.util.PlatformAlarm.playAlarm(tone.id)
            if (vibrate) com.railgaadi.app.util.PlatformAlarm.vibrate()
            if (voice) {
                com.railgaadi.app.util.PlatformSpeech.speak("Attention passenger. Wake up alarm test successful.")
            }
            delay(3500)
            com.railgaadi.app.util.PlatformAlarm.stopAlarm()
            com.railgaadi.app.util.PlatformAlarm.stopVibrate()
            com.railgaadi.app.util.PlatformSpeech.stop()
        }
    }

    fun checkAlarmGeofence() {
        val alarm = activeAlarm ?: return
        if (!alarm.isArmed || alarm.isRinging) return

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

        val shouldTrigger = when (alarm.triggerUnit) {
            com.railgaadi.app.model.AlarmTriggerUnit.KILOMETERS -> remainingKm <= alarm.triggerDistanceKm
            com.railgaadi.app.model.AlarmTriggerUnit.MINUTES -> remainingMinutes <= alarm.triggerMinutesBefore
        }

        if (shouldTrigger && curIdx <= targetIdx) {
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
        // No default train loaded on start
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
                        // Silent background live fetch (no blocking spinner)
                        val updated = service.getLiveStatus(activeTrain.number)
                        if (updated != null) {
                            liveStatus = updated
                            refreshWeather(silent = true)
                            checkAlarmGeofence()
                        }
                    } catch (_: Exception) {
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
                if (updated != null) {
                    liveStatus = updated
                    refreshWeather(silent = true)
                    checkAlarmGeofence()
                }
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

        selectedWeatherStationName = null
        scope.launch {
            isLoadingStatus = true
            liveStatus = service.getLiveStatus(train.number)
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

