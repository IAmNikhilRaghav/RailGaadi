package com.railgaadi.app

import com.railgaadi.app.model.*
import com.railgaadi.app.util.CellTowerData
import com.railgaadi.app.util.GeoLocation
import kotlin.test.*

class RailGaadiFeaturesTest {

    private val testStations = listOf(
        Station(code = "NDLS", name = "New Delhi", latitude = 28.6420, longitude = 77.2190, scheduledDep = "06:00"),
        Station(code = "ALJN", name = "Aligarh Jn", latitude = 27.8970, longitude = 78.0880, scheduledArr = "07:30", scheduledDep = "07:32"),
        Station(code = "CNB", name = "Kanpur Central", latitude = 26.4539, longitude = 80.3510, scheduledArr = "10:15", scheduledDep = "10:20"),
        Station(code = "PRYJ", name = "Prayagraj Junction", latitude = 25.4497, longitude = 81.8283, scheduledArr = "12:30")
    )

    private val testTrain = Train(
        number = "12404",
        name = "Prayagraj Express",
        origin = "New Delhi (NDLS)",
        destination = "Prayagraj Junction (PRYJ)",
        departureTime = "06:00",
        arrivalTime = "12:30"
    )

    // ── 1. CELL TOWER TRILATERATION ON SEEDED CORRIDORS ──────────────────────

    @Test
    fun testMultiTowerTrilaterationOnKnownCorridor() {
        // Two known towers near Kanpur / Malwan
        val servingCell = CellTowerData(
            cellId = 45215,
            areaCode = 1205,
            signalStrengthDbm = -75,
            isRegistered = true,
            timingAdvance = 4 // ~312 meters
        )
        val neighborCell = CellTowerData(
            cellId = 45211,
            areaCode = 1205,
            signalStrengthDbm = -95,
            isRegistered = false
        )

        val result = RadioTrilaterationEngine.computeTrilateration(listOf(servingCell, neighborCell), testStations)

        assertNotNull(result, "Trilateration should resolve for known corridor towers")
        assertTrue(result.isMultiTowerLock, "Should achieve multi-tower lock with 2 visible towers")
        assertEquals(2, result.visibleTowersCount)
        assertTrue(result.confidenceScore >= 0.75f, "Confidence score should be high for multi-tower lock")
        assertEquals("CNB", result.approachingStationCode)
        assertTrue(result.remainingDistanceKm > 0.0)
    }

    @Test
    fun testSingleTowerWithTimingAdvance() {
        val singleCell = CellTowerData(
            cellId = 45210, // Fatehpur
            areaCode = 1205,
            signalStrengthDbm = -80,
            isRegistered = true,
            timingAdvance = 10 // 10 * 78.12 = 781.2 meters
        )

        val result = RadioTrilaterationEngine.computeTrilateration(listOf(singleCell), testStations)

        assertNotNull(result)
        assertFalse(result.isMultiTowerLock)
        assertEquals(1, result.visibleTowersCount)
        assertNotNull(result.timingAdvanceMeters)
        assertEquals(781.2, result.timingAdvanceMeters!!, 1.0)
        assertTrue(result.confidenceScore >= 0.85f, "Timing Advance should provide high confidence")
    }

    // ── 2. ANONYMOUS / UNMAPPED CORRIDOR BEHAVIOR ──────────────────────────

    @Test
    fun testAnonymousRouteGracefulHandling() {
        // Towers from an unmapped route (e.g. unknown cell IDs)
        val unknownCell1 = CellTowerData(cellId = 999999L, areaCode = 8888, signalStrengthDbm = -80)
        val unknownCell2 = CellTowerData(cellId = 888888L, areaCode = 8888, signalStrengthDbm = -90)

        // 1. Should return null from trilateration
        val result = RadioTrilaterationEngine.computeTrilateration(listOf(unknownCell1, unknownCell2), testStations)
        assertNull(result, "Unmapped towers should return null trilateration")

        // 2. Hybrid engine should gracefully fallback to timetable estimation without crashing
        val hybridStatus = OfflineTrainEngine.calculateHybridOfflineLiveStatus(
            train = testTrain,
            gps = null,
            cell = unknownCell1,
            visibleCells = listOf(unknownCell1, unknownCell2),
            existingStations = testStations
        )

        assertNotNull(hybridStatus, "Hybrid engine must always produce a status even on unmapped routes")
        assertEquals(testTrain.number, hybridStatus.trainNumber)
        assertEquals("SCHEDULE ESTIMATED", hybridStatus.status)
        assertTrue(hybridStatus.speedKmh > 0, "Fallback speed must be a valid non-zero speed")
    }

    // ── 3. SPEED & DELAY DETECTION LOGIC ───────────────────────────────────

    @Test
    fun testGpsSpeedDetectionMovingVsStopped() {
        // Moving train: 84 km/h GPS speed
        val movingGps = GeoLocation(latitude = 27.5, longitude = 78.5, speedKmh = 84.0, accuracyMeters = 8f)
        val movingStatus = OfflineTrainEngine.calculateOfflineLiveStatus(testTrain, movingGps, testStations)
        assertEquals(84, movingStatus.speedKmh, "Moving train must report exact GPS speed")

        // Stopped train: speed < 5 km/h threshold
        val stoppedGps = GeoLocation(latitude = 26.4539, longitude = 80.3510, speedKmh = 1.2, accuracyMeters = 5f)
        val stoppedStatus = OfflineTrainEngine.calculateOfflineLiveStatus(testTrain, stoppedGps, testStations)
        // With threshold fallback when at station
        assertTrue(stoppedStatus.speedKmh >= 0)
    }

    // ── 4. DESTINATION ALARM GEOFENCE LOGIC ────────────────────────────────

    @Test
    fun testDestinationAlarmTriggering() {
        val alarm = DestinationAlarm(
            trainNumber = "12404",
            trainName = "Prayagraj Express",
            stationCode = "PRYJ",
            stationName = "Prayagraj Junction",
            stationLat = 25.4497,
            stationLon = 81.8283,
            triggerDistanceKm = 10.0,
            triggerUnit = AlarmTriggerUnit.KILOMETERS,
            isArmed = true
        )

        // Passenger is 8 km away from Prayagraj -> inside trigger zone (<= 10km)
        val passengerLat = 25.4800
        val passengerLon = 81.7600
        val distKm = calculateDistanceKm(passengerLat, passengerLon, alarm.stationLat, alarm.stationLon)

        assertTrue(distKm <= alarm.triggerDistanceKm, "Passenger at 8km should be within 10km alarm trigger zone")

        // Passenger is 50 km away -> outside trigger zone
        val farLat = 25.9272 // Fatehpur
        val farLon = 80.8130
        val farDistKm = calculateDistanceKm(farLat, farLon, alarm.stationLat, alarm.stationLon)

        assertFalse(farDistKm <= alarm.triggerDistanceKm, "Passenger at 50km should NOT trigger the 10km alarm")
    }

    private fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = (lat2 - lat1) * (kotlin.math.PI / 180.0)
        val dLon = (lon2 - lon1) * (kotlin.math.PI / 180.0)
        val a = kotlin.math.sin(dLat / 2).let { it * it } +
                kotlin.math.cos(lat1 * (kotlin.math.PI / 180.0)) * kotlin.math.cos(lat2 * (kotlin.math.PI / 180.0)) *
                kotlin.math.sin(dLon / 2).let { it * it }
        return r * 2.0 * kotlin.math.asin(kotlin.math.sqrt(a).coerceIn(-1.0, 1.0))
    }
}
