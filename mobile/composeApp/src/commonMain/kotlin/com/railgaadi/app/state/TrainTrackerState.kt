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

    // Web App Preferences & Features
    var currentMapStyle by mutableStateOf("terrain") // "light", "dark", "satellite", "terrain"
    var tripDelayAlerts by mutableStateOf(false)
    var savedTrains by mutableStateOf(listOf<String>())

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

        scope.launch {
            isLoadingStatus = true
            liveStatus = service.getLiveStatus(train.number)
            isLoadingStatus = false
        }
    }

    fun refreshLiveStatus() {
        if (activeTrain.number.isBlank()) return
        scope.launch {
            isLoadingStatus = true
            liveStatus = service.getLiveStatus(activeTrain.number)
            isLoadingStatus = false
        }
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

    fun refreshWeather() {
        val stations = liveStatus?.stations ?: return
        val currentStation = stations.getOrNull(
            stations.indexOfFirst { it.name.contains(liveStatus?.currentStation ?: "", ignoreCase = true) }.takeIf { it >= 0 } ?: 0
        ) ?: stations.firstOrNull() ?: return

        val coords = if (currentStation.latitude != 0.0 && currentStation.longitude != 0.0) {
            Pair(currentStation.latitude, currentStation.longitude)
        } else {
            StationDataService.getCoordinates(currentStation.code)
        }

        val lat = coords.first
        val lon = coords.second
        if (lat == 0.0 && lon == 0.0) return

        scope.launch {
            isLoadingWeather = true
            try {
                weatherData = weatherService.getStationWeatherData(currentStation.name, lat, lon)
                nearbyLandmarks = LandmarkService.getNearbyLandmarks(lat, lon)
            } catch (_: Exception) { }
            isLoadingWeather = false
        }
    }
}

