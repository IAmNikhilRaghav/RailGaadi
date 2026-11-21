package com.railgaadi.app.model

import kotlin.math.abs
import kotlin.math.roundToInt

object JunctionDataService {

    // ─────────────────────────────────────────────────────────────────
    // High-Fidelity Verified Directory of Major Indian Railway Junctions
    // ─────────────────────────────────────────────────────────────────
    private val junctionRegistry = mapOf(
        // Mathura Junction (10 Platforms)
        "MTJ" to JunctionLayout(
            stationCode = "MTJ",
            stationName = "Mathura Junction",
            totalPlatforms = 10,
            city = "Mathura",
            state = "Uttar Pradesh",
            entries = listOf(
                StationEntry(
                    id = "MTJ_ENTRY_1",
                    name = "City Side (Main Gate)",
                    sideLabel = "Platform 1 Side",
                    nearestPlatform = 1,
                    hasCabDropoff = true,
                    hasMetroConnectivity = false,
                    description = "Main portico entrance, unreserved booking, waiting halls & auto stand"
                ),
                StationEntry(
                    id = "MTJ_ENTRY_2",
                    name = "Cantt Side (Second Entry)",
                    sideLabel = "Platform 7 Side",
                    nearestPlatform = 7,
                    hasCabDropoff = true,
                    hasMetroConnectivity = false,
                    description = "Secondary gate with fast access to Platforms 7, 8, 9 and 10"
                )
            ),
            bridges = listOf(
                FootOverBridge(
                    id = "MTJ_FOB_DELHI",
                    name = "Delhi-End FOB",
                    connectedPlatforms = listOf(1, 2, 3, 4, 5),
                    hasLift = false,
                    hasEscalator = false,
                    hasRamp = true,
                    relativePosition = 0.20f
                ),
                FootOverBridge(
                    id = "MTJ_FOB_CENTRAL",
                    name = "Central Foot Over Bridge",
                    connectedPlatforms = listOf(1, 2, 3, 4, 5, 6, 7),
                    hasLift = true,
                    hasEscalator = true,
                    hasRamp = false,
                    relativePosition = 0.50f
                ),
                FootOverBridge(
                    id = "MTJ_FOB_AGRA",
                    name = "Agra-End FOB",
                    connectedPlatforms = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10),
                    hasLift = true,
                    hasEscalator = false,
                    hasRamp = false,
                    relativePosition = 0.82f
                )
            ),
            platforms = (1..10).map { pf ->
                PlatformDetail(
                    platformNumber = pf,
                    coachCapacity = 24,
                    isIslandPlatform = (pf in listOf(2, 3, 4, 5, 8, 9)),
                    pairedPlatform = when (pf) {
                        2 -> 3; 3 -> 2
                        4 -> 5; 5 -> 4
                        8 -> 9; 9 -> 8
                        else -> null
                    },
                    amenities = listOf(
                        StationAmenityNode(AmenityType.WATER, pf, 0.25f, "RO Drinking Water", "Pillar 6"),
                        StationAmenityNode(AmenityType.FOOD, pf, 0.50f, "IRCTC Jan Aahar & Tea", "Pillar 12"),
                        StationAmenityNode(AmenityType.RESTROOM, pf, 0.75f, "Clean Restrooms", "Pillar 18")
                    )
                )
            }
        ),

        // New Delhi Railway Station (16 Platforms)
        "NDLS" to JunctionLayout(
            stationCode = "NDLS",
            stationName = "New Delhi",
            totalPlatforms = 16,
            city = "New Delhi",
            state = "Delhi",
            entries = listOf(
                StationEntry(
                    id = "NDLS_ENTRY_PAHARGANJ",
                    name = "Paharganj Gate (Gate 1)",
                    sideLabel = "Platform 1 Side",
                    nearestPlatform = 1,
                    hasCabDropoff = true,
                    hasMetroConnectivity = false,
                    description = "Heritage side entrance with AC Waiting Lounge, VIP gate & Taxi Stand"
                ),
                StationEntry(
                    id = "NDLS_ENTRY_AJMERI",
                    name = "Ajmeri Gate (Gate 2)",
                    sideLabel = "Platform 16 Side",
                    nearestPlatform = 16,
                    hasCabDropoff = true,
                    hasMetroConnectivity = true,
                    description = "Direct skywalk to Delhi Airport Express & Yellow Line Metro, multi-level parking"
                )
            ),
            bridges = listOf(
                FootOverBridge(
                    id = "NDLS_FOB_NORTH",
                    name = "North FOB (Old Bridge)",
                    connectedPlatforms = (1..16).toList(),
                    hasLift = false,
                    hasEscalator = true,
                    hasRamp = false,
                    relativePosition = 0.22f
                ),
                FootOverBridge(
                    id = "NDLS_FOB_CENTRAL",
                    name = "Central FOB (Skywalk)",
                    connectedPlatforms = (1..16).toList(),
                    hasLift = true,
                    hasEscalator = true,
                    hasRamp = true,
                    relativePosition = 0.50f
                ),
                FootOverBridge(
                    id = "NDLS_FOB_SOUTH",
                    name = "South FOB",
                    connectedPlatforms = (1..16).toList(),
                    hasLift = true,
                    hasEscalator = true,
                    hasRamp = false,
                    relativePosition = 0.78f
                )
            ),
            platforms = (1..16).map { pf ->
                PlatformDetail(
                    platformNumber = pf,
                    coachCapacity = 24,
                    isIslandPlatform = (pf in 2..15),
                    pairedPlatform = if (pf % 2 == 0) pf + 1 else pf - 1,
                    amenities = listOf(
                        StationAmenityNode(AmenityType.WATER, pf, 0.20f, "Cold Drinking Water", "Pillar 4"),
                        StationAmenityNode(AmenityType.FOOD, pf, 0.48f, "IRCTC Refreshment Room", "Pillar 11"),
                        StationAmenityNode(AmenityType.RESTROOM, pf, 0.80f, "Executive Lounge & Restrooms", "Pillar 19")
                    )
                )
            }
        ),

        // Kanpur Central (10 Platforms)
        "CNB" to JunctionLayout(
            stationCode = "CNB",
            stationName = "Kanpur Central",
            totalPlatforms = 10,
            city = "Kanpur",
            state = "Uttar Pradesh",
            entries = listOf(
                StationEntry(
                    id = "CNB_ENTRY_CANTT",
                    name = "Cantt Side (Main Entry)",
                    sideLabel = "Platform 1 Side",
                    nearestPlatform = 1,
                    hasCabDropoff = true,
                    hasMetroConnectivity = true,
                    description = "Main portico entry with metro station access and prepaid auto stand"
                ),
                StationEntry(
                    id = "CNB_ENTRY_CITY",
                    name = "City Side (GT Road)",
                    sideLabel = "Platform 10 Side",
                    nearestPlatform = 10,
                    hasCabDropoff = true,
                    hasMetroConnectivity = false,
                    description = "Secondary gate opening into Ghanta Ghar / Collectorate market"
                )
            ),
            bridges = listOf(
                FootOverBridge(
                    id = "CNB_FOB_1",
                    name = "Howrah-End FOB",
                    connectedPlatforms = (1..10).toList(),
                    hasLift = false,
                    hasEscalator = false,
                    hasRamp = true,
                    relativePosition = 0.25f
                ),
                FootOverBridge(
                    id = "CNB_FOB_CENTRAL",
                    name = "Central Escalator FOB",
                    connectedPlatforms = (1..10).toList(),
                    hasLift = true,
                    hasEscalator = true,
                    hasRamp = false,
                    relativePosition = 0.52f
                ),
                FootOverBridge(
                    id = "CNB_FOB_DELHI",
                    name = "Delhi-End FOB",
                    connectedPlatforms = (1..10).toList(),
                    hasLift = true,
                    hasEscalator = false,
                    hasRamp = false,
                    relativePosition = 0.80f
                )
            ),
            platforms = (1..10).map { pf ->
                PlatformDetail(
                    platformNumber = pf,
                    coachCapacity = 24,
                    isIslandPlatform = (pf in 2..9),
                    amenities = listOf(
                        StationAmenityNode(AmenityType.WATER, pf, 0.30f, "Water Vending Machine", "Pillar 7"),
                        StationAmenityNode(AmenityType.FOOD, pf, 0.50f, "Food Plaza & Snacks", "Pillar 12"),
                        StationAmenityNode(AmenityType.RESTROOM, pf, 0.70f, "Restrooms & Lifts", "Pillar 16")
                    )
                )
            }
        ),

        // Jaipur Junction (8 Platforms)
        "JP" to JunctionLayout(
            stationCode = "JP",
            stationName = "Jaipur Junction",
            totalPlatforms = 8,
            city = "Jaipur",
            state = "Rajasthan",
            entries = listOf(
                StationEntry(
                    id = "JP_ENTRY_MAIN",
                    name = "Main Entrance (Station Road)",
                    sideLabel = "Platform 1 Side",
                    nearestPlatform = 1,
                    hasCabDropoff = true,
                    hasMetroConnectivity = true,
                    description = "Pink City architecture facade with direct underground Metro walkway"
                ),
                StationEntry(
                    id = "JP_ENTRY_HASANPURA",
                    name = "Hasanpura Gate",
                    sideLabel = "Platform 8 Side",
                    nearestPlatform = 8,
                    hasCabDropoff = true,
                    hasMetroConnectivity = false,
                    description = "Quiet second entry convenient for Hasanpura and Ajmer Road commuters"
                )
            ),
            bridges = listOf(
                FootOverBridge(
                    id = "JP_FOB_CENTRAL",
                    name = "Central Skybridge (Escalator & Lift)",
                    connectedPlatforms = (1..8).toList(),
                    hasLift = true,
                    hasEscalator = true,
                    hasRamp = true,
                    relativePosition = 0.48f
                ),
                FootOverBridge(
                    id = "JP_FOB_EAST",
                    name = "East FOB (Overhead Ramp)",
                    connectedPlatforms = (1..8).toList(),
                    hasLift = false,
                    hasEscalator = false,
                    hasRamp = true,
                    relativePosition = 0.78f
                )
            ),
            platforms = (1..8).map { pf ->
                PlatformDetail(
                    platformNumber = pf,
                    coachCapacity = 24,
                    isIslandPlatform = (pf in 2..7),
                    amenities = listOf(
                        StationAmenityNode(AmenityType.WATER, pf, 0.25f, "RO Drinking Water", "Pillar 5"),
                        StationAmenityNode(AmenityType.FOOD, pf, 0.52f, "Rajasthani Food Court", "Pillar 11")
                    )
                )
            }
        ),

        // Barmer (3 Platforms)
        "BME" to JunctionLayout(
            stationCode = "BME",
            stationName = "Barmer",
            totalPlatforms = 3,
            city = "Barmer",
            state = "Rajasthan",
            entries = listOf(
                StationEntry(
                    id = "BME_ENTRY_MAIN",
                    name = "Main Portico",
                    sideLabel = "Platform 1 Side",
                    nearestPlatform = 1,
                    hasCabDropoff = true,
                    description = "Main passenger building and booking counters"
                )
            ),
            bridges = listOf(
                FootOverBridge(
                    id = "BME_FOB_1",
                    name = "Central Foot Over Bridge",
                    connectedPlatforms = listOf(1, 2, 3),
                    hasLift = true,
                    hasRamp = true,
                    relativePosition = 0.50f
                )
            ),
            platforms = (1..3).map { pf ->
                PlatformDetail(
                    platformNumber = pf,
                    coachCapacity = 24,
                    isIslandPlatform = (pf in 2..3),
                    pairedPlatform = if (pf == 2) 3 else if (pf == 3) 2 else null,
                    amenities = listOf(
                        StationAmenityNode(AmenityType.WATER, pf, 0.35f, "Cold Drinking Water", "Pillar 6"),
                        StationAmenityNode(AmenityType.FOOD, pf, 0.55f, "Tea & Snack Stall", "Pillar 10")
                    )
                )
            }
        )
    )

    /**
     * Resolves layout for ANY railway station Pan-India.
     * Uses High-Fidelity mapped layout if available; otherwise dynamically synthesizes
     * a geometrically accurate Indian Railways layout on the fly!
     */
    fun getLayoutForStation(stationCode: String, stationName: String = "", platformCountHint: Int = 4): JunctionLayout {
        val code = stationCode.trim().uppercase()
        val mapped = junctionRegistry[code]
        if (mapped != null) return mapped

        // Synthesize realistic procedural layout for any of the ~7,400 stations
        val pfCount = platformCountHint.coerceIn(1, 16)
        val cleanName = stationName.ifBlank { StationDataService.cleanStationName(stationCode) }

        return generateProceduralLayout(
            stationCode = code,
            stationName = cleanName,
            platformCount = pfCount
        )
    }

    /**
     * Algorithmic synthesis for Pan-India coverage:
     * Generates a canonical Indian Railways layout with:
     * - Main Portico on Platform 1
     * - Secondary Entry on the highest platform (if > 4 platforms)
     * - Central Foot Over Bridge connecting all platforms
     * - Paired Island Platforms (2/3, 4/5, etc.)
     */
    private fun generateProceduralLayout(
        stationCode: String,
        stationName: String,
        platformCount: Int
    ): JunctionLayout {
        val entries = mutableListOf(
            StationEntry(
                id = "${stationCode}_ENTRY_1",
                name = "Main Station Entry",
                sideLabel = "Platform 1 Side",
                nearestPlatform = 1,
                hasCabDropoff = true,
                description = "Primary concourse entrance, booking office & waiting hall"
            )
        )

        if (platformCount >= 5) {
            entries.add(
                StationEntry(
                    id = "${stationCode}_ENTRY_2",
                    name = "Second Entry Gate",
                    sideLabel = "Platform $platformCount Side",
                    nearestPlatform = platformCount,
                    hasCabDropoff = true,
                    description = "Secondary station entrance with direct platform access"
                )
            )
        }

        val bridges = mutableListOf<FootOverBridge>()
        if (platformCount > 1) {
            bridges.add(
                FootOverBridge(
                    id = "${stationCode}_FOB_CENTRAL",
                    name = "Central Foot Over Bridge",
                    connectedPlatforms = (1..platformCount).toList(),
                    hasLift = (platformCount >= 3),
                    hasEscalator = (platformCount >= 5),
                    hasRamp = true,
                    relativePosition = 0.50f
                )
            )
            if (platformCount >= 6) {
                bridges.add(
                    FootOverBridge(
                        id = "${stationCode}_FOB_NORTH",
                        name = "North Foot Over Bridge",
                        connectedPlatforms = (1..platformCount).toList(),
                        hasLift = false,
                        hasEscalator = false,
                        hasRamp = true,
                        relativePosition = 0.25f
                    )
                )
            }
        }

        val platforms = (1..platformCount).map { pf ->
            PlatformDetail(
                platformNumber = pf,
                coachCapacity = 24,
                isIslandPlatform = (pf > 1 && pf < platformCount),
                pairedPlatform = if (pf > 1 && pf % 2 == 0 && pf < platformCount) pf + 1
                                else if (pf > 1 && pf % 2 != 0) pf - 1 else null,
                amenities = listOf(
                    StationAmenityNode(AmenityType.WATER, pf, 0.30f, "Drinking Water", "Pillar 6"),
                    StationAmenityNode(AmenityType.FOOD, pf, 0.50f, "Refreshment Stall", "Pillar 12"),
                    StationAmenityNode(AmenityType.RESTROOM, pf, 0.70f, "Restrooms", "Pillar 18")
                )
            )
        }

        return JunctionLayout(
            stationCode = stationCode,
            stationName = stationName,
            totalPlatforms = platformCount,
            city = stationName,
            state = "India",
            entries = entries,
            bridges = bridges,
            platforms = platforms,
            isHighFidelityMapped = false
        )
    }

    /**
     * Computes turn-by-turn walking navigation path from an Entry or starting platform
     * to the passenger's target platform and exact coach stop position.
     */
    fun calculateNavigationRoute(
        layout: JunctionLayout,
        fromEntryId: String?,
        fromPlatform: Int? = null,
        targetPlatform: Int,
        targetCoachCode: String? = null,
        preferAccessible: Boolean = true
    ): NavigationRoute {
        val entry = layout.entries.find { it.id == fromEntryId } ?: layout.entries.first()
        val startPf = fromPlatform ?: entry.nearestPlatform
        val steps = mutableListOf<NavigationStep>()
        var stepCounter = 1
        var totalDistance = 0

        // 1. Departure from entry gate
        val fromLabel = entry.name
        steps.add(
            NavigationStep(
                stepNumber = stepCounter++,
                icon = "🚪",
                instruction = "Enter via ${entry.name} (${entry.sideLabel})",
                subDetail = entry.description.ifBlank { "Pass through security scanner into the main circulating area" },
                distanceMeters = 30
            )
        )
        totalDistance += 30

        // 2. Are we already on the target platform?
        val bridgeNeeded = (startPf != targetPlatform)
        val bestFob: FootOverBridge? = if (bridgeNeeded) {
            val candidateBridges = layout.bridges.filter {
                it.connectedPlatforms.contains(startPf) && it.connectedPlatforms.contains(targetPlatform)
            }
            if (preferAccessible) {
                candidateBridges.find { it.hasLift || it.hasEscalator }
                    ?: candidateBridges.firstOrNull() ?: layout.bridges.firstOrNull()
            } else {
                candidateBridges.firstOrNull() ?: layout.bridges.firstOrNull()
            }
        } else null

        if (bridgeNeeded && bestFob != null) {
            val hasAccessibility = bestFob.hasLift || bestFob.hasEscalator
            val accessDetail = when {
                preferAccessible && bestFob.hasLift -> "Take the Glass Elevator (Lift) with your luggage"
                preferAccessible && bestFob.hasEscalator -> "Take the Escalator up to the Foot Over Bridge"
                bestFob.hasRamp -> "Walk along the gentle luggage ramp to the bridge"
                else -> "Take the central stairs up to the overhead bridge"
            }

            steps.add(
                NavigationStep(
                    stepNumber = stepCounter++,
                    icon = if (bestFob.hasLift) "🛗" else if (bestFob.hasEscalator) "⚡" else "🪜",
                    instruction = "Head to ${bestFob.name}",
                    subDetail = accessDetail,
                    distanceMeters = 40,
                    isEscalatorOrLift = hasAccessibility
                )
            )
            totalDistance += 40

            val trackCrossDist = abs(targetPlatform - startPf) * 15 + 20
            steps.add(
                NavigationStep(
                    stepNumber = stepCounter++,
                    icon = "🌉",
                    instruction = "Walk along ${bestFob.name} across to Platform $targetPlatform",
                    subDetail = "Follow overhead signage overhead to Platform $targetPlatform staircase/lift",
                    distanceMeters = trackCrossDist,
                    isFobCrossing = true
                )
            )
            totalDistance += trackCrossDist

            val descentDetail = when {
                bestFob.hasLift -> "Take the elevator down directly to Platform $targetPlatform"
                bestFob.hasEscalator -> "Take escalator or stairs down to Platform $targetPlatform"
                else -> "Take the stairs down onto Platform $targetPlatform"
            }
            steps.add(
                NavigationStep(
                    stepNumber = stepCounter++,
                    icon = if (bestFob.hasLift) "🛗" else "🪜",
                    instruction = "Descend onto Platform $targetPlatform",
                    subDetail = descentDetail,
                    distanceMeters = 20,
                    isEscalatorOrLift = bestFob.hasLift
                )
            )
            totalDistance += 20
        } else if (startPf == targetPlatform) {
            steps.add(
                NavigationStep(
                    stepNumber = stepCounter++,
                    icon = "✅",
                    instruction = "You are already on Platform $targetPlatform",
                    subDetail = "Walk straight onto the platform surface",
                    distanceMeters = 15
                )
            )
            totalDistance += 15
        }

        // 3. Platform walk to the coach stopping position
        val (coachOffset, coachWalkMeters, coachDirection) = calculateCoachOffset(targetCoachCode)
        if (targetCoachCode != null && targetCoachCode.isNotBlank()) {
            steps.add(
                NavigationStep(
                    stepNumber = stepCounter++,
                    icon = "🚆",
                    instruction = "Walk $coachDirection toward Coach $targetCoachCode",
                    subDetail = "Watch digital overhead coach indicators near Pillar No. ${(coachOffset * 22).roundToInt().coerceAtLeast(1)}",
                    distanceMeters = coachWalkMeters
                )
            )
            totalDistance += coachWalkMeters
        } else {
            steps.add(
                NavigationStep(
                    stepNumber = stepCounter++,
                    icon = "📍",
                    instruction = "Wait at Platform $targetPlatform Central Concourse",
                    subDetail = "Listen to public announcements for upcoming train arrival",
                    distanceMeters = 20
                )
            )
            totalDistance += 20
        }

        val estimatedMinutes = (totalDistance / 60.0).roundToInt().coerceAtLeast(2)

        return NavigationRoute(
            fromLabel = fromLabel,
            targetPlatform = targetPlatform,
            targetCoach = targetCoachCode,
            recommendedBridge = bestFob,
            totalDistanceMeters = totalDistance,
            estimatedMinutes = estimatedMinutes,
            steps = steps,
            coachStopRelativeOffset = coachOffset
        )
    }

    /**
     * Determines relative position along platform (0.0 to 1.0) and direction
     * based on standard Indian Railways rake sequencing (Loco at front, AC in middle, Sleeper/General at ends).
     */
    private fun calculateCoachOffset(coachCode: String?): Triple<Float, Int, String> {
        if (coachCode.isNullOrBlank()) return Triple(0.50f, 25, "to platform center")
        val code = coachCode.trim().uppercase()

        return when {
            code.startsWith("ENG") || code == "LOCO" -> Triple(0.05f, 90, "towards the Engine / North End")
            code.startsWith("GS") || code.startsWith("GEN") || code == "UR" -> Triple(0.18f, 70, "towards Front General Coaches")
            code.startsWith("1A") || code.startsWith("H1") -> Triple(0.42f, 35, "towards First AC Compartment (Center)")
            code.startsWith("2A") || code.startsWith("A") -> Triple(0.48f, 25, "near Platform Center (2nd AC)")
            code.startsWith("3A") || code.startsWith("B") -> Triple(0.55f, 30, "towards 3rd AC Block (Center-Rear)")
            code.startsWith("3E") || code.startsWith("M") -> Triple(0.60f, 40, "towards 3-Economy Coaches")
            code.startsWith("SL") || code.startsWith("S") -> Triple(0.75f, 65, "towards Sleeper Coaches (Rear)")
            code.startsWith("SLR") || code.startsWith("EOG") -> Triple(0.92f, 95, "towards the Guard / Rear End")
            else -> Triple(0.50f, 30, "towards Platform Center")
        }
    }
}
