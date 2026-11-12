package com.railgaadi.app.model

import com.railgaadi.app.util.GeoLocation
import com.railgaadi.app.util.PlatformDate
import kotlin.math.*

object OfflineTrainEngine {

    // Persistent in-memory cache of route timetables (survives network disconnects)
    private val routeCache = mutableMapOf<String, List<Station>>()

    /**
     * Cache the complete station list of a train so it can be tracked offline forever
     */
    fun cacheRoute(trainNumber: String, stations: List<Station>) {
        val clean = trainNumber.trim()
        if (clean.isNotEmpty() && stations.isNotEmpty()) {
            routeCache[clean] = stations
        }
    }

    fun getCachedRoute(trainNumber: String): List<Station>? {
        return routeCache[trainNumber.trim()]
    }

    /**
     * Given the passenger's live GPS location, calculate real-time train status offline
     */
    fun calculateOfflineLiveStatus(
        train: Train,
        gps: GeoLocation,
        existingStations: List<Station>? = null
    ): LiveStatus {
        val stations = existingStations?.takeIf { it.isNotEmpty() }
            ?: routeCache[train.number.trim()]
            ?: fallbackRouteForTrain(train)

        if (stations.isEmpty()) {
            return LiveStatus(
                trainNumber = train.number,
                trainName = train.name,
                status = "OFFLINE GPS ACTIVE",
                delayMinutes = 0,
                speedKmh = gps.speedKmh.toInt().coerceAtLeast(0),
                currentStation = train.origin,
                nextStation = train.destination,
                progressPercent = 0.1f,
                stations = emptyList()
            )
        }

        // Calculate distance from GPS location to each station
        val distancesWithIndex = stations.mapIndexed { idx, stn ->
            val (lat, lon) = if (stn.latitude != 0.0) stn.latitude to stn.longitude else StationDataService.getCoordinates(stn.code)
            val d = haversineKm(gps.latitude, gps.longitude, lat, lon)
            Triple(idx, stn, d)
        }

        // Find the closest station to current GPS position
        val closest = distancesWithIndex.minByOrNull { it.third } ?: distancesWithIndex.first()
        val closestIdx = closest.first

        // Determine if train has passed the closest station or is still approaching it
        val nextStationIdx = if (closestIdx < stations.size - 1) {
            val nextStn = stations[closestIdx + 1]
            val prevStn = if (closestIdx > 0) stations[closestIdx - 1] else null

            val dToNext = haversineKm(gps.latitude, gps.longitude, nextStn.latitude, nextStn.longitude)
            val dPrevToNext = prevStn?.let { haversineKm(it.latitude, it.longitude, nextStn.latitude, nextStn.longitude) } ?: 999.0
            val dCurrentToNext = haversineKm(closest.second.latitude, closest.second.longitude, nextStn.latitude, nextStn.longitude)

            // If we are closer to the next station than the closest station is to next, we've passed it
            if (dToNext < dCurrentToNext) closestIdx + 1 else closestIdx
        } else {
            closestIdx
        }

        val passedIdx = (nextStationIdx - 1).coerceAtLeast(0)
        val approachingStation = stations[nextStationIdx]
        val lastPassedStation = stations[passedIdx]

        // Calculate progress percentage along route
        val totalStations = stations.size
        val progress = (nextStationIdx.toFloat() / (totalStations - 1).coerceAtLeast(1))
            .coerceIn(0.0f, 1.0f)

        // Calculate delay by comparing device wall-clock time with scheduled arrival at approaching station
        val delayMinutes = calculateDelayMinutes(approachingStation.scheduledArr ?: approachingStation.scheduledDep)

        val statusText = when {
            progress >= 0.98f -> "ARRIVED"
            delayMinutes <= 2 -> "ON TIME (GPS)"
            delayMinutes > 2 -> "DELAYED ${delayMinutes}m (GPS)"
            else -> "EARLY ${-delayMinutes}m (GPS)"
        }

        val effectiveSpeed = if (gps.speedKmh > 5.0) gps.speedKmh.toInt() else 65

        return LiveStatus(
            trainNumber = train.number,
            trainName = train.name,
            status = statusText,
            delayMinutes = delayMinutes.coerceAtLeast(0),
            speedKmh = effectiveSpeed,
            currentStation = "${lastPassedStation.name} (${lastPassedStation.code})",
            nextStation = "${approachingStation.name} (${approachingStation.code})",
            progressPercent = progress,
            stations = stations
        )
    }

    private fun calculateDelayMinutes(scheduledTimeStr: String?): Int {
        if (scheduledTimeStr.isNullOrBlank() || scheduledTimeStr == "—") return 0
        return try {
            val parts = scheduledTimeStr.split(":").map { it.trim().toInt() }
            if (parts.size < 2) return 0
            val schedHour = parts[0]
            val schedMin = parts[1]
            val schedTotalMins = schedHour * 60 + schedMin

            val (nowHour, nowMin) = PlatformDate.getCurrentTime()
            val nowTotalMins = nowHour * 60 + nowMin

            val diff: Int = nowTotalMins - schedTotalMins
            // Handle day wrapping if near midnight
            when {
                diff > 720 -> diff - 1440
                diff < -720 -> diff + 1440
                else -> diff
            }
        } catch (_: Throwable) {
            0
        }
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        if (lat1 == 0.0 || lat2 == 0.0) return 999.0
        val r = 6371.0
        val dLat = (lat2 - lat1) * (PI / 180.0)
        val dLon = (lon2 - lon1) * (PI / 180.0)
        val a = sin(dLat / 2).let { it * it } +
                cos(lat1 * (PI / 180.0)) * cos(lat2 * (PI / 180.0)) *
                sin(dLon / 2).let { it * it }
        return r * 2.0 * asin(sqrt(a).coerceIn(-1.0, 1.0))
    }

    private fun fallbackRouteForTrain(train: Train): List<Station> {
        val originCode = StationDataService.resolveStationCode(train.origin)
        val destCode = StationDataService.resolveStationCode(train.destination)
        val originCoord = StationDataService.getCoordinates(originCode)
        val destCoord = StationDataService.getCoordinates(destCode)

        return listOf(
            Station(originCode, train.origin, originCoord.first, originCoord.second, scheduledDep = train.departureTime, platform = "1"),
            Station(destCode, train.destination, destCoord.first, destCoord.second, scheduledArr = train.arrivalTime, platform = "2")
        )
    }
}
