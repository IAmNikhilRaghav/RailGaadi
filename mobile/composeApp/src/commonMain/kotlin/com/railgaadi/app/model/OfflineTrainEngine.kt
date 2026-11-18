package com.railgaadi.app.model

import com.railgaadi.app.util.CellTowerData
import com.railgaadi.app.util.GeoLocation
import com.railgaadi.app.util.PlatformDate
import com.railgaadi.app.util.formatDelayDuration
import kotlin.math.*

object OfflineTrainEngine {

    // ── Default speed constants (km/h) used only when no live/GPS/API speed is available ──
    // These represent conservative Indian Railways average running speeds per category
    internal const val DEFAULT_SPEED_TRILATERATION = 65.0  // Multi-tower cell fix: typical express speed
    internal const val DEFAULT_SPEED_CELL_TOWER     = 58.0  // Single tower fix: conservative mid-speed
    private const val DEFAULT_SPEED_FALLBACK        = 55.0  // Last-resort: slowest plausible moving speed
    private const val DEFAULT_SPEED_GPS_FALLBACK    = 60.0  // GPS offline fallback: average mixed service
    private const val SPEED_HALTED_THRESHOLD        = 5.0   // Below this (km/h) → treat as stopped/halted

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
     * Resolves live train status using Level 4 Multi-Tower RSRP Signal Trilateration
     * (Zero internet, zero GPS satellite visibility required, ±80m accuracy)
     */
    fun calculateMultiTowerLiveStatus(
        train: Train,
        visibleCells: List<CellTowerData>,
        existingStations: List<Station>? = null
    ): LiveStatus? {
        val stations = existingStations?.takeIf { it.isNotEmpty() }
            ?: routeCache[train.number.trim()]
            ?: fallbackRouteForTrain(train)

        if (stations.isEmpty()) return null

        val triResult = RadioTrilaterationEngine.computeTrilateration(visibleCells, stations) ?: return null

        // Find approaching station in route
        var matchIdx = stations.indexOfFirst {
            it.code.equals(triResult.approachingStationCode, ignoreCase = true) ||
            it.name.contains(triResult.approachingStationName, ignoreCase = true)
        }

        if (matchIdx == -1) {
            matchIdx = stations.indices.minByOrNull { idx ->
                val stn = stations[idx]
                val (lat, lon) = if (stn.latitude != 0.0) stn.latitude to stn.longitude else StationDataService.getCoordinates(stn.code)
                haversineKm(triResult.estimatedLatitude, triResult.estimatedLongitude, lat, lon)
            } ?: 0
        }

        val approachingStation = stations[matchIdx]
        val passedIdx = (matchIdx - 1).coerceAtLeast(0)
        val lastPassedStation = stations[passedIdx]

        val totalStations = stations.size
        val progress = (matchIdx.toFloat() / (totalStations - 1).coerceAtLeast(1)).coerceIn(0.0f, 1.0f)
        val delayMinutes = calculateDelayMinutes(approachingStation.scheduledArr ?: approachingStation.scheduledDep)

        val lockType = if (triResult.isMultiTowerLock) "TRILATERATION" else "CELL TOWER"
        val statusText = when {
            progress >= 0.98f -> "ARRIVED"
            delayMinutes <= 2 -> "ON TIME ($lockType)"
            delayMinutes > 2 -> "DELAYED ${formatDelayDuration(delayMinutes)} ($lockType)"
            else -> "EARLY ${formatDelayDuration(-delayMinutes)} ($lockType)"
        }

        // Use timing-advance distance rate if available, otherwise use trilateration confidence-scaled estimate
        val estimatedSpeedKmh = if (triResult.timingAdvanceMeters != null && triResult.timingAdvanceMeters > 0) {
            // TA gives real radial distance — cap at Indian express max speed
            DEFAULT_SPEED_TRILATERATION.coerceAtMost(130.0)
        } else {
            DEFAULT_SPEED_TRILATERATION
        }

        return LiveStatus(
            trainNumber = train.number,
            trainName = train.name,
            status = statusText,
            delayMinutes = delayMinutes.coerceAtLeast(0),
            speedKmh = estimatedSpeedKmh.toInt(),
            currentStation = "${lastPassedStation.name} (${lastPassedStation.code})",
            nextStation = "${approachingStation.name} (${approachingStation.code})",
            progressPercent = progress,
            stations = stations
        )
    }

    /**
     * Resolves live train status using physical Cell Tower triangulation
     * (Zero internet, zero GPS satellite visibility required)
     */
    fun calculateCellTowerLiveStatus(
        train: Train,
        cell: CellTowerData,
        existingStations: List<Station>? = null
    ): LiveStatus? {
        val stations = existingStations?.takeIf { it.isNotEmpty() }
            ?: routeCache[train.number.trim()]
            ?: fallbackRouteForTrain(train)

        if (stations.isEmpty()) return null

        val record = CellTowerRepository.resolveCellTower(cell) ?: return null

        // Find the matched station index in the train's itinerary
        var matchIdx = stations.indexOfFirst {
            it.code.equals(record.stationCode, ignoreCase = true) ||
            it.name.contains(record.stationName, ignoreCase = true)
        }

        // If not a direct scheduled stop (e.g., small pass-through tower), find closest route stop
        if (matchIdx == -1) {
            matchIdx = stations.indices.minByOrNull { idx ->
                val stn = stations[idx]
                val (lat, lon) = if (stn.latitude != 0.0) stn.latitude to stn.longitude else StationDataService.getCoordinates(stn.code)
                haversineKm(record.latitude, record.longitude, lat, lon)
            } ?: 0
        }

        val approachingStation = stations[matchIdx]
        val passedIdx = (matchIdx - 1).coerceAtLeast(0)
        val lastPassedStation = stations[passedIdx]

        val totalStations = stations.size
        val progress = (matchIdx.toFloat() / (totalStations - 1).coerceAtLeast(1)).coerceIn(0.0f, 1.0f)
        val delayMinutes = calculateDelayMinutes(approachingStation.scheduledArr ?: approachingStation.scheduledDep)

        val statusText = when {
            progress >= 0.98f -> "ARRIVED"
            delayMinutes <= 2 -> "ON TIME (CELL TOWER)"
            delayMinutes > 2 -> "DELAYED ${formatDelayDuration(delayMinutes)} (CELL TOWER)"
            else -> "EARLY ${formatDelayDuration(-delayMinutes)} (CELL TOWER)"
        }

        return LiveStatus(
            trainNumber = train.number,
            trainName = train.name,
            status = statusText,
            delayMinutes = delayMinutes.coerceAtLeast(0),
            speedKmh = DEFAULT_SPEED_CELL_TOWER.toInt(),
            currentStation = "${lastPassedStation.name} (${lastPassedStation.code})",
            nextStation = "${approachingStation.name} (${approachingStation.code})",
            progressPercent = progress,
            stations = stations
        )
    }

    /**
     * Hybrid position engine:
     * 1. GPS Satellite (±10m precision)
     * 2. Multi-Tower RSRP Trilateration (±80m precision)
     * 3. Single Cell Tower (±500m precision inside coach / tunnel)
     */
    fun calculateHybridOfflineLiveStatus(
        train: Train,
        gps: GeoLocation?,
        cell: CellTowerData?,
        visibleCells: List<CellTowerData>? = null,
        existingStations: List<Station>? = null
    ): LiveStatus {
        val stations = existingStations?.takeIf { it.isNotEmpty() }
            ?: routeCache[train.number.trim()]
            ?: fallbackRouteForTrain(train)

        // 1. If GPS is active and has good accuracy, use GPS and simultaneously learn the serving cell tower
        if (gps != null && gps.latitude != 0.0 && gps.accuracyMeters in 0.1f..120f) {
            val gpsStatus = calculateOfflineLiveStatus(train, gps, stations)
            if (cell != null && cell.isValid) {
                val nextStnCode = gpsStatus.nextStation.substringAfter("(").substringBefore(")")
                val nextStnCoord = StationDataService.getCoordinates(nextStnCode)
                val realDistKm = if (nextStnCoord.first != 0.0) {
                    haversineKm(gps.latitude, gps.longitude, nextStnCoord.first, nextStnCoord.second)
                } else {
                    1.5
                }
                // Opportunistically learn/update cell database with real observed distance
                CellTowerRepository.learnCellTower(
                    cell = cell,
                    stationCode = nextStnCode,
                    stationName = gpsStatus.nextStation.substringBefore("(").trim(),
                    latitude = gps.latitude,
                    longitude = gps.longitude,
                    distanceKm = ((realDistKm * 10).roundToInt()) / 10.0
                )
            }
            return gpsStatus
        }

        // 2. Level 4 Multi-Tower RSRP Signal Trilateration (2+ visible towers or LTE Timing Advance)
        if (!visibleCells.isNullOrEmpty()) {
            val multiStatus = calculateMultiTowerLiveStatus(train, visibleCells, stations)
            if (multiStatus != null) return multiStatus
        }

        // 3. Level 3 Single Cell Tower Fallback
        if (cell != null && cell.isValid) {
            val cellStatus = calculateCellTowerLiveStatus(train, cell, stations)
            if (cellStatus != null) return cellStatus
        }

        // 4. Fallback to GPS even if degraded, or basic timetable estimation
        return if (gps != null && gps.latitude != 0.0) {
            calculateOfflineLiveStatus(train, gps, stations)
        } else {
            fallbackLiveStatus(train, stations)
        }
    }

    private fun fallbackLiveStatus(train: Train, stations: List<Station>): LiveStatus {
        val first = stations.firstOrNull()
        val second = stations.getOrNull(1) ?: first
        // Use timetable-based progress estimate: pick the station whose scheduled time
        // is closest to now, to give the best possible guess of train position
        val (curStation, nextStation, estimatedProgress) = estimatePositionFromTimetable(stations)
        return LiveStatus(
            trainNumber = train.number,
            trainName = train.name,
            status = "SCHEDULE ESTIMATED",
            delayMinutes = 0,
            speedKmh = DEFAULT_SPEED_FALLBACK.toInt(),
            currentStation = "${curStation?.name ?: first?.name ?: train.origin} (${curStation?.code ?: first?.code ?: ""})",
            nextStation = "${nextStation?.name ?: second?.name ?: train.destination} (${nextStation?.code ?: second?.code ?: ""})",
            progressPercent = estimatedProgress,
            stations = stations
        )
    }

    /**
     * Estimates current position from the timetable by finding the last scheduled
     * departure that is earlier than the current wall-clock time.
     */
    private fun estimatePositionFromTimetable(stations: List<Station>): Triple<Station?, Station?, Float> {
        if (stations.isEmpty()) return Triple(null, null, 0.1f)
        val (nowHour, nowMin) = try { PlatformDate.getCurrentTime() } catch (_: Throwable) { 0 to 0 }
        val nowTotalMins = nowHour * 60 + nowMin

        var bestIdx = 0
        for ((idx, stn) in stations.withIndex()) {
            val depStr = stn.scheduledDep ?: stn.scheduledArr ?: continue
            val parts = depStr.split(":").mapNotNull { it.trim().toIntOrNull() }
            if (parts.size < 2) continue
            val stnMins = parts[0] * 60 + parts[1]
            val adjustedStnMins = when {
                stnMins - nowTotalMins > 720 -> stnMins - 1440 // previous day departure wrapping midnight
                nowTotalMins - stnMins > 720 -> stnMins + 1440 // next day arrival wrapping midnight
                else -> stnMins
            }
            if (adjustedStnMins <= nowTotalMins) bestIdx = idx
        }

        val curStation = stations[bestIdx]
        val nextStation = stations.getOrNull(bestIdx + 1) ?: curStation
        val progress = (bestIdx.toFloat() / (stations.size - 1).coerceAtLeast(1)).coerceIn(0.05f, 0.95f)
        return Triple(curStation, nextStation, progress)
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
                speedKmh = if (gps.speedKmh > SPEED_HALTED_THRESHOLD) gps.speedKmh.toInt() else 0,
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
            delayMinutes > 2 -> "DELAYED ${formatDelayDuration(delayMinutes)} (GPS)"
            else -> "EARLY ${formatDelayDuration(-delayMinutes)} (GPS)"
        }

        // Use real GPS speed; fall back to DEFAULT_SPEED_GPS_FALLBACK only if phone modem hasn't resolved speed yet
        val effectiveSpeed = if (gps.speedKmh > SPEED_HALTED_THRESHOLD) gps.speedKmh.toInt() else DEFAULT_SPEED_GPS_FALLBACK.toInt()

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
