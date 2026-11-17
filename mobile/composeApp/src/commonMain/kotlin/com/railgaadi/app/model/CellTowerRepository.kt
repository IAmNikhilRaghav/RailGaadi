package com.railgaadi.app.model

import com.railgaadi.app.util.CellTowerData

data class TrackCellRecord(
    val cellId: Long,
    val areaCode: Int,
    val stationCode: String,
    val stationName: String,
    val latitude: Double,
    val longitude: Double,
    val lineDescription: String,
    val distanceToStationKm: Double,
    val isApproaching: Boolean = true
)

object CellTowerRepository {

    // In-memory lookup map of cell towers along Indian railway corridors
    // Key: "areaCode_cellId" e.g. "1205_45210" or "cellId"
    private val corridorCellDb = mutableMapOf<String, TrackCellRecord>()

    // Learned / crowdsourced cell tower records
    private val learnedCellDb = mutableMapOf<String, TrackCellRecord>()

    init {
        seedCorridorCells()
    }

    /**
     * Resolves a physical cellular tower to a specific railway section / station
     */
    fun resolveCellTower(cell: CellTowerData): TrackCellRecord? {
        val exactKey = "${cell.areaCode}_${cell.cellId}"
        corridorCellDb[exactKey]?.let { return it }
        learnedCellDb[exactKey]?.let { return it }

        // Fallback by cellId alone if area code shifts during handover
        val cellIdKey = cell.cellId.toString()
        corridorCellDb[cellIdKey]?.let { return it }
        learnedCellDb[cellIdKey]?.let { return it }

        return null
    }

    /**
     * Crowdsourcing / Self-Learning mechanism:
     * When user is on train with GPS active, pair the serving cell tower with the train's verified track position.
     */
    fun learnCellTower(
        cell: CellTowerData,
        stationCode: String,
        stationName: String,
        latitude: Double,
        longitude: Double,
        distanceKm: Double,
        lineName: String = "Indian Railways Corridor"
    ) {
        if (!cell.isValid) return
        val record = TrackCellRecord(
            cellId = cell.cellId,
            areaCode = cell.areaCode,
            stationCode = stationCode,
            stationName = stationName,
            latitude = latitude,
            longitude = longitude,
            lineDescription = lineName,
            distanceToStationKm = distanceKm,
            isApproaching = distanceKm > 0.5
        )
        val key = "${cell.areaCode}_${cell.cellId}"
        learnedCellDb[key] = record
        learnedCellDb[cell.cellId.toString()] = record
    }

    /**
     * Seed major Indian Railways corridor cell towers
     * (Delhi-Kanpur-Prayagraj, Bandra-Ajmer-SGNR, Mumbai-Ahmedabad)
     */
    private fun seedCorridorCells() {
        // --- 1. Kanpur - Prayagraj - Fatehpur Corridor (e.g. Train 20404 Prayagraj Express) ---
        addTower(45210, 1205, "FTP", "Fatehpur", 25.9272, 80.8130, "Kanpur-Prayagraj Trunk Line", 12.0)
        addTower(45211, 1205, "MWH", "Malwan", 25.9980, 80.6870, "Kanpur-Prayagraj Trunk Line", 2.0)
        addTower(45212, 1205, "KBN", "Karbigwan", 26.1200, 80.5100, "Kanpur-Prayagraj Trunk Line", 1.5)
        addTower(45213, 1205, "AUNG", "Aung", 26.0600, 80.5800, "Kanpur-Prayagraj Trunk Line", 1.0)
        addTower(45214, 1205, "BKO", "Bindki Road", 26.0350, 80.6120, "Kanpur-Prayagraj Trunk Line", 3.0)
        addTower(45215, 1205, "CNB", "Kanpur Central", 26.4539, 80.3510, "Kanpur-Prayagraj Trunk Line", 0.8)
        addTower(45216, 1205, "PRYJ", "Prayagraj Junction", 25.4497, 81.8283, "Kanpur-Prayagraj Trunk Line", 0.5)

        // --- 2. Western Railway / Rajasthan Corridor (e.g. Train 14702 Aravali Express, 12015 Ajmer Shatabdi) ---
        addTower(62101, 3401, "SGNR", "Shri Ganganagar", 29.9200, 73.8800, "Bhatinda-Suratgarh Line", 2.0)
        addTower(62102, 3401, "SDS", "Sadulshahr", 29.8000, 73.9800, "Bhatinda-Suratgarh Line", 1.2)
        addTower(62103, 3401, "HMH", "Hanumangarh Jn", 29.5800, 74.3200, "Hanumangarh Junction", 0.9)
        addTower(62104, 3401, "SOD", "Somesar", 25.7500, 73.4800, "Marwar-Ajmer Section", 3.5)
        addTower(62105, 3401, "MJ", "Marwar Jn", 25.6800, 73.5500, "Marwar Junction", 0.5)
        addTower(62106, 3401, "RANI", "Rani", 25.3500, 73.3000, "Marwar-Abu Road Line", 1.8)
        addTower(62107, 3401, "FA", "Falna", 25.2200, 73.2300, "Marwar-Abu Road Line", 0.7)
        addTower(62108, 3401, "ABR", "Abu Road", 24.4800, 72.7800, "Ajmer-Ahmedabad Mainline", 1.1)
        addTower(62109, 3401, "BER", "Beawar", 26.1011, 74.3197, "Marwar-Ajmer Section", 1.2)
        addTower(62110, 3401, "AII", "Ajmer Jn", 26.4566, 74.6375, "Ajmer Junction Division", 0.6)
        addTower(62111, 3401, "KSG", "Kishangarh", 26.5768, 74.8647, "Ajmer-Jaipur Section", 1.5)
        addTower(62112, 3401, "FL", "Phulera Jn", 26.8720, 75.2410, "Ajmer-Jaipur Section", 0.8)
        addTower(62113, 3401, "JP", "Jaipur Jn", 26.9202, 75.7869, "North Western Railway HQ", 0.7)

        // --- 3. Mumbai Suburban & Gujarat Corridor (e.g. Train 12951 Rajdhani, 20901 Vande Bharat) ---
        addTower(88101, 4010, "BDTS", "Bandra Terminus", 19.0600, 72.8400, "Western Railway Mainline", 0.4)
        addTower(88102, 4010, "BVI", "Borivali", 19.2300, 72.8600, "Western Railway Mainline", 0.6)
        addTower(88103, 4010, "ST", "Surat", 21.2000, 72.8400, "Mumbai-Delhi Trunk Line", 1.0)
        addTower(88104, 4010, "BRC", "Vadodara Jn", 22.3100, 73.1800, "Mumbai-Delhi Trunk Line", 0.8)
        addTower(88105, 4010, "ADI", "Ahmedabad Jn", 23.0200, 72.6000, "Gujarat Mainline", 0.9)
        addTower(88106, 4010, "MMCT", "Mumbai Central", 18.9696, 72.8193, "Western Railway Terminus", 0.5)

        // --- 4. Delhi NCR, Mathura, Agra & Northern Railway Corridor ---
        addTower(99101, 1101, "NDLS", "New Delhi", 28.6420, 77.2190, "Delhi Division", 0.3)
        addTower(99102, 1101, "NZM", "Hazrat Nizamuddin", 28.5880, 77.2530, "Delhi Division", 0.5)
        addTower(99103, 1101, "GZB", "Ghaziabad Jn", 28.6670, 77.4330, "Northern Railway", 1.2)
        addTower(99104, 1101, "ALJN", "Aligarh Jn", 27.8970, 78.0880, "NCR Mainline", 1.5)
        addTower(99105, 1101, "TDL", "Tundla Jn", 27.2050, 78.2430, "NCR Mainline", 0.9)
        addTower(99106, 1101, "MTJ", "Mathura Jn", 27.4801, 77.6731, "NCR-WCR Junction", 0.8)
        addTower(99107, 1101, "AGC", "Agra Cantt", 27.1580, 77.9902, "NCR Mainline", 0.6)
        addTower(99108, 1101, "BSB", "Varanasi Jn", 25.3273, 82.9865, "Northern/North Eastern Railway", 0.7)
        addTower(99109, 1101, "HWH", "Howrah Jn", 22.5841, 88.3410, "Eastern/South Eastern Railway", 0.5)
    }

    private fun addTower(
        cellId: Long,
        areaCode: Int,
        stationCode: String,
        stationName: String,
        lat: Double,
        lon: Double,
        line: String,
        distKm: Double
    ) {
        val record = TrackCellRecord(
            cellId = cellId,
            areaCode = areaCode,
            stationCode = stationCode,
            stationName = stationName,
            latitude = lat,
            longitude = lon,
            lineDescription = line,
            distanceToStationKm = distKm,
            isApproaching = distKm > 0.3
        )
        corridorCellDb["${areaCode}_${cellId}"] = record
        corridorCellDb[cellId.toString()] = record
    }
}
