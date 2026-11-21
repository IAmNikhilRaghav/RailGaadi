package com.railgaadi.app.model

import kotlinx.serialization.Serializable

/**
 * Types of public passenger amenities located on railway station platforms
 */
enum class AmenityType(val icon: String, val displayName: String) {
    WATER("💧", "RO Drinking Water"),
    FOOD("🍲", "Food Stall / IRCTC"),
    RESTROOM("🚻", "Restroom / Toilets"),
    WAITING_ROOM("🛋️", "Waiting Room"),
    CLOAKROOM("🛅", "Cloak Room"),
    MEDICAL("⚕️", "First Aid / Medical"),
    ESCALATOR("⚡", "Escalator"),
    LIFT("🛗", "Elevator / Lift"),
    BOOK_STALL("📚", "Book Stall"),
    TEA_COFFEE("☕", "Tea & Snacks")
}

/**
 * Specific amenity located at a relative position on a platform
 */
@Serializable
data class StationAmenityNode(
    val type: AmenityType,
    val platformNumber: Int,
    val relativePosition: Float, // 0.0 (Engine/North end) to 1.0 (Rear/South end)
    val label: String,
    val pillarNumber: String = ""
)

/**
 * Gate / Portico Entry Point into the railway junction
 */
@Serializable
data class StationEntry(
    val id: String,
    val name: String,
    val sideLabel: String,         // e.g. "City Side", "Cantt Side", "Ajmeri Gate", "Paharganj"
    val nearestPlatform: Int,
    val hasCabDropoff: Boolean = true,
    val hasMetroConnectivity: Boolean = false,
    val description: String = ""
)

/**
 * Overhead Foot Over Bridge (FOB) or Subway connecting platforms
 */
@Serializable
data class FootOverBridge(
    val id: String,
    val name: String,
    val connectedPlatforms: List<Int>,
    val hasLift: Boolean = false,
    val hasEscalator: Boolean = false,
    val hasRamp: Boolean = false,
    val relativePosition: Float = 0.5f, // 0.0 to 1.0 along platform length
    val isSubway: Boolean = false
)

/**
 * Detailed specification for an individual platform line
 */
@Serializable
data class PlatformDetail(
    val platformNumber: Int,
    val coachCapacity: Int = 24,
    val isIslandPlatform: Boolean = false,
    val pairedPlatform: Int? = null, // e.g. PF 2 and 3 share island
    val trackName: String = "Broad Gauge Main",
    val amenities: List<StationAmenityNode> = emptyList()
)

/**
 * Complete Architectural & Spatial Layout of a Railway Junction
 */
@Serializable
data class JunctionLayout(
    val stationCode: String,
    val stationName: String,
    val totalPlatforms: Int,
    val city: String,
    val state: String,
    val entries: List<StationEntry>,
    val bridges: List<FootOverBridge>,
    val platforms: List<PlatformDetail>,
    val isHighFidelityMapped: Boolean = true
)

/**
 * Step-by-Step Navigation Instruction for a Passenger
 */
data class NavigationStep(
    val stepNumber: Int,
    val icon: String,
    val instruction: String,
    val subDetail: String,
    val distanceMeters: Int,
    val isFobCrossing: Boolean = false,
    val isEscalatorOrLift: Boolean = false
)

/**
 * Calculated Navigation Route to Reach the Target Platform and Coach
 */
data class NavigationRoute(
    val fromLabel: String,
    val targetPlatform: Int,
    val targetCoach: String? = null,
    val recommendedBridge: FootOverBridge?,
    val totalDistanceMeters: Int,
    val estimatedMinutes: Int,
    val steps: List<NavigationStep>,
    val coachStopRelativeOffset: Float = 0.5f // 0.0 to 1.0 where coach stops
)
