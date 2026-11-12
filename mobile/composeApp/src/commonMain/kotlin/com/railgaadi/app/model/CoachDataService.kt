package com.railgaadi.app.model

import androidx.compose.ui.graphics.Color

object CoachDataService {

    // Official IR Livery Color Palettes
    private val VandeBharatWhite = Color(0xFFF8FAFC)
    private val VandeBharatBlue = Color(0xFF1E3A8A)
    private val RajdhaniRed = Color(0xFFDC2626)
    private val RajdhaniCream = Color(0xFFFEF3C7)
    private val LhbBlue = Color(0xFF2563EB)
    private val LhbGray = Color(0xFFE2E8F0)
    private val ShatabdiCyan = Color(0xFF0284C7)
    private val EmuPurple = Color(0xFF7E22CE)
    private val EmuYellow = Color(0xFFFBBF24)
    private val EngineRed = Color(0xFFB91C1C)

    /**
     * Resolves the full Coach Rake Composition for any Indian Railways train.
     */
    fun getRakeComposition(trainNumber: String, trainName: String = "", trainType: String = ""): TrainRakeComposition {
        val tNo = trainNumber.trim()
        val lowerName = trainName.lowercase()
        val lowerType = trainType.lowercase()

        return when {
            // ── 1. VANDE BHARAT EXPRESS ─────────────────────────────────────
            lowerName.contains("vande bharat") || lowerType.contains("vande bharat") || tNo.startsWith("208") || tNo.startsWith("206") || tNo == "22436" || tNo == "22435" -> {
                buildVandeBharatRake(tNo, trainName.ifBlank { "Vande Bharat Express" })
            }

            // ── 2. RAJDHANI & TEJAS EXPRESS ────────────────────────────────
            lowerName.contains("rajdhani") || lowerType.contains("rajdhani") || lowerName.contains("tejas") || tNo in listOf("12951", "12952", "12423", "12424", "12301", "12302", "12431", "12432", "22691", "22692") -> {
                buildRajdhaniRake(tNo, trainName.ifBlank { "Rajdhani Express" })
            }

            // ── 3. SHATABDI EXPRESS ─────────────────────────────────────────
            lowerName.contains("shatabdi") || lowerType.contains("shatabdi") || tNo.startsWith("120") -> {
                buildShatabdiRake(tNo, trainName.ifBlank { "Shatabdi Express" })
            }

            // ── 4. SUBURBAN LOCAL / EMU / MEMU ──────────────────────────────
            lowerName.contains("emu") || lowerName.contains("memu") || lowerName.contains("local") || lowerType.contains("emu") || tNo.startsWith("64") || tNo.startsWith("90") -> {
                buildEmuLocalRake(tNo, trainName.ifBlank { "Suburban EMU Local" })
            }

            // ── 5. STANDARD SUPERFAST / MAIL / EXPRESS (Default) ─────────────
            else -> {
                buildStandardLhbRake(tNo, trainName.ifBlank { "Superfast Express" }, trainType)
            }
        }
    }

    // ── VANDE BHARAT RAKE (16 or 8 coaches) ───────────────────────────────────
    private fun buildVandeBharatRake(trainNumber: String, trainName: String): TrainRakeComposition {
        val is8Car = trainNumber in listOf("20825", "20826", "22457", "22458", "20835", "20836")
        val coaches = mutableListOf<CoachPosition>()
        var seq = 1

        // Front Driving Trailer Coach (DTC)
        coaches.add(
            CoachPosition(
                sequenceNumber = seq++,
                coachCode = "D1",
                coachClass = CoachClass.AC_CHAIR_CAR,
                displayName = "Driving Trailer Coach (D1)",
                totalSeats = 44,
                isAirConditioned = true,
                primaryColor = VandeBharatWhite,
                secondaryColor = VandeBharatBlue,
                isLocomotive = true
            )
        )

        val chairCarCount = if (is8Car) 5 else 12
        val ecPosition = if (is8Car) 3 else 7

        for (i in 1..chairCarCount) {
            coaches.add(
                CoachPosition(
                    sequenceNumber = seq++,
                    coachCode = "C$i",
                    coachClass = CoachClass.AC_CHAIR_CAR,
                    displayName = "AC Chair Car (C$i)",
                    totalSeats = 78,
                    isAirConditioned = true,
                    primaryColor = VandeBharatWhite,
                    secondaryColor = VandeBharatBlue
                )
            )

            // Insert Executive Class in the middle
            if (i == ecPosition) {
                val ecCoaches = if (is8Car) listOf("E1") else listOf("E1", "E2")
                ecCoaches.forEach { ecCode ->
                    coaches.add(
                        CoachPosition(
                            sequenceNumber = seq++,
                            coachCode = ecCode,
                            coachClass = CoachClass.EXECUTIVE_CHAIR_CAR,
                            displayName = "Executive Chair Car ($ecCode)",
                            totalSeats = 52,
                            isAirConditioned = true,
                            primaryColor = VandeBharatWhite,
                            secondaryColor = Color(0xFF7C3AED)
                        )
                    )
                }
            }
        }

        // Rear Driving Trailer Coach (DTC)
        coaches.add(
            CoachPosition(
                sequenceNumber = seq,
                coachCode = "D2",
                coachClass = CoachClass.AC_CHAIR_CAR,
                displayName = "Rear Driving Coach (D2)",
                totalSeats = 44,
                isAirConditioned = true,
                primaryColor = VandeBharatWhite,
                secondaryColor = VandeBharatBlue,
                isLocomotive = true
            )
        )

        return TrainRakeComposition(
            trainNumber = trainNumber,
            trainName = trainName,
            trainType = "Vande Bharat Express",
            totalCoaches = coaches.size,
            engineType = "Self-Propelled Electric Multiple Unit (160 km/h)",
            coaches = coaches
        )
    }

    // ── RAJDHANI & TEJAS RAKE ────────────────────────────────────────────────
    private fun buildRajdhaniRake(trainNumber: String, trainName: String): TrainRakeComposition {
        val coaches = mutableListOf<CoachPosition>()
        var seq = 1

        // Locomotive WAP-7
        coaches.add(createEngine(seq++, "WAP-7", "WAP-7 3-Phase AC Electric Locomotive (6000 HP)", RajdhaniRed))
        // Power Car
        coaches.add(createEog(seq++, "EOG 1", RajdhaniRed, RajdhaniCream))

        // 3A Coaches (B1 to B6)
        for (i in 1..6) {
            coaches.add(
                CoachPosition(
                    sequenceNumber = seq++,
                    coachCode = "B$i",
                    coachClass = CoachClass.THIRD_AC,
                    displayName = "AC 3 Tier (B$i)",
                    totalSeats = 72,
                    isAirConditioned = true,
                    primaryColor = RajdhaniRed,
                    secondaryColor = RajdhaniCream
                )
            )
        }

        // 3E Economy Coaches
        for (i in 1..2) {
            coaches.add(
                CoachPosition(
                    sequenceNumber = seq++,
                    coachCode = "M$i",
                    coachClass = CoachClass.THIRD_AC_ECONOMY,
                    displayName = "AC 3 Economy (M$i)",
                    totalSeats = 83,
                    isAirConditioned = true,
                    primaryColor = RajdhaniRed,
                    secondaryColor = RajdhaniCream
                )
            )
        }

        // Hot Buffet / Pantry Car
        coaches.add(
            CoachPosition(
                sequenceNumber = seq++,
                coachCode = "PC",
                coachClass = CoachClass.PANTRY_CAR,
                displayName = "Hot Buffet / Pantry Car",
                totalSeats = 0,
                isAirConditioned = true,
                primaryColor = RajdhaniRed,
                secondaryColor = Color(0xFFE11D48)
            )
        )

        // 2A Coaches (A1 to A3)
        for (i in 1..3) {
            coaches.add(
                CoachPosition(
                    sequenceNumber = seq++,
                    coachCode = "A$i",
                    coachClass = CoachClass.SECOND_AC,
                    displayName = "AC 2 Tier (A$i)",
                    totalSeats = 54,
                    isAirConditioned = true,
                    primaryColor = RajdhaniRed,
                    secondaryColor = RajdhaniCream
                )
            )
        }

        // 1A First Class (H1)
        coaches.add(
            CoachPosition(
                sequenceNumber = seq++,
                coachCode = "H1",
                coachClass = CoachClass.FIRST_AC,
                displayName = "AC First Class (H1)",
                totalSeats = 24,
                isAirConditioned = true,
                primaryColor = RajdhaniRed,
                secondaryColor = Color(0xFFFBBF24)
            )
        )

        // Additional 3A Coaches (B7 to B11)
        for (i in 7..11) {
            coaches.add(
                CoachPosition(
                    sequenceNumber = seq++,
                    coachCode = "B$i",
                    coachClass = CoachClass.THIRD_AC,
                    displayName = "AC 3 Tier (B$i)",
                    totalSeats = 72,
                    isAirConditioned = true,
                    primaryColor = RajdhaniRed,
                    secondaryColor = RajdhaniCream
                )
            )
        }

        // Rear Power Car
        coaches.add(createEog(seq, "EOG 2", RajdhaniRed, RajdhaniCream))

        return TrainRakeComposition(
            trainNumber = trainNumber,
            trainName = trainName,
            trainType = "Rajdhani Express",
            totalCoaches = coaches.size,
            engineType = "WAP-7 High-Speed Electric (140 km/h)",
            coaches = coaches
        )
    }

    // ── SHATABDI RAKE ────────────────────────────────────────────────────────
    private fun buildShatabdiRake(trainNumber: String, trainName: String): TrainRakeComposition {
        val coaches = mutableListOf<CoachPosition>()
        var seq = 1

        coaches.add(createEngine(seq++, "WAP-5", "WAP-5 High-Speed Electric Locomotive (5450 HP)", ShatabdiCyan))
        coaches.add(createEog(seq++, "EOG 1", ShatabdiCyan, LhbGray))

        // Chair Car (C1 to C8)
        for (i in 1..8) {
            coaches.add(
                CoachPosition(
                    sequenceNumber = seq++,
                    coachCode = "C$i",
                    coachClass = CoachClass.AC_CHAIR_CAR,
                    displayName = "AC Chair Car (C$i)",
                    totalSeats = 78,
                    isAirConditioned = true,
                    primaryColor = ShatabdiCyan,
                    secondaryColor = LhbGray
                )
            )
        }

        // Executive Chair Car (E1, E2)
        for (i in 1..2) {
            coaches.add(
                CoachPosition(
                    sequenceNumber = seq++,
                    coachCode = "E$i",
                    coachClass = CoachClass.EXECUTIVE_CHAIR_CAR,
                    displayName = "Executive Chair Car (E$i)",
                    totalSeats = 52,
                    isAirConditioned = true,
                    primaryColor = ShatabdiCyan,
                    secondaryColor = Color(0xFFFBBF24)
                )
            )
        }

        // Additional Chair Car (C9 to C14)
        for (i in 9..14) {
            coaches.add(
                CoachPosition(
                    sequenceNumber = seq++,
                    coachCode = "C$i",
                    coachClass = CoachClass.AC_CHAIR_CAR,
                    displayName = "AC Chair Car (C$i)",
                    totalSeats = 78,
                    isAirConditioned = true,
                    primaryColor = ShatabdiCyan,
                    secondaryColor = LhbGray
                )
            )
        }

        coaches.add(createEog(seq, "EOG 2", ShatabdiCyan, LhbGray))

        return TrainRakeComposition(
            trainNumber = trainNumber,
            trainName = trainName,
            trainType = "Shatabdi Express",
            totalCoaches = coaches.size,
            engineType = "WAP-5 Electric Locomotive (150 km/h)",
            coaches = coaches
        )
    }

    // ── SUBURBAN LOCAL / EMU / MEMU RAKE (12 Cars) ───────────────────────────
    private fun buildEmuLocalRake(trainNumber: String, trainName: String): TrainRakeComposition {
        val coaches = mutableListOf<CoachPosition>()
        var seq = 1

        val emuSchema = listOf(
            Triple("MC1", CoachClass.MOTOR_COACH, "Driving Motor Coach"),
            Triple("GS1", CoachClass.GENERAL_UNRESERVED, "Second Class General"),
            Triple("FC1", CoachClass.FIRST_CLASS_EMU, "First Class"),
            Triple("L1", CoachClass.LADIES_COACH, "Ladies Compartment"),
            Triple("GS2", CoachClass.GENERAL_UNRESERVED, "Second Class General"),
            Triple("MC2", CoachClass.MOTOR_COACH, "Middle Motor Coach"),
            Triple("GS3", CoachClass.GENERAL_UNRESERVED, "Second Class General"),
            Triple("FC2", CoachClass.FIRST_CLASS_EMU, "First Class"),
            Triple("L2", CoachClass.LADIES_COACH, "Ladies Compartment"),
            Triple("GS4", CoachClass.GENERAL_UNRESERVED, "Second Class General"),
            Triple("GS5", CoachClass.GENERAL_UNRESERVED, "Second Class General"),
            Triple("MC3", CoachClass.MOTOR_COACH, "Rear Driving Motor Coach")
        )

        emuSchema.forEach { (code, cls, name) ->
            coaches.add(
                CoachPosition(
                    sequenceNumber = seq++,
                    coachCode = code,
                    coachClass = cls,
                    displayName = name,
                    totalSeats = cls.defaultSeats,
                    isAirConditioned = false,
                    primaryColor = if (cls == CoachClass.FIRST_CLASS_EMU) Color(0xFFDC2626) else if (cls == CoachClass.LADIES_COACH) Color(0xFF059669) else EmuPurple,
                    secondaryColor = EmuYellow,
                    hasToilets = false,
                    isLocomotive = code.startsWith("MC")
                )
            )
        }

        return TrainRakeComposition(
            trainNumber = trainNumber,
            trainName = trainName,
            trainType = "Suburban Local / EMU",
            totalCoaches = coaches.size,
            engineType = "25kV AC Suburban EMU (100 km/h)",
            coaches = coaches
        )
    }

    // ── STANDARD LHB SUPERFAST / EXPRESS RAKE (22 Coaches) ───────────────────
    private fun buildStandardLhbRake(trainNumber: String, trainName: String, trainType: String): TrainRakeComposition {
        val coaches = mutableListOf<CoachPosition>()
        var seq = 1

        // 1. Locomotive (WAP-7 or WDP-4D)
        val isDiesel = trainName.contains("Special", ignoreCase = true) || trainNumber.startsWith("0")
        val locoModel = if (isDiesel) "WDP-4D Diesel (4500 HP)" else "WAP-7 Electric (6000 HP)"
        coaches.add(createEngine(seq++, if (isDiesel) "WDP" else "WAP", locoModel, EngineRed))

        // 2. Front SLR (Luggage cum Guard)
        coaches.add(
            CoachPosition(
                sequenceNumber = seq++,
                coachCode = "SLR",
                coachClass = CoachClass.LUGGAGE_BRAKE_VAN,
                displayName = "Seating cum Luggage (SLR)",
                totalSeats = 30,
                isAirConditioned = false,
                primaryColor = LhbBlue,
                secondaryColor = LhbGray
            )
        )

        // 3. General Unreserved (GS1, GS2)
        for (i in 1..2) {
            coaches.add(
                CoachPosition(
                    sequenceNumber = seq++,
                    coachCode = "GS$i",
                    coachClass = CoachClass.GENERAL_UNRESERVED,
                    displayName = "General Second Class (GS$i)",
                    totalSeats = 90,
                    isAirConditioned = false,
                    primaryColor = LhbBlue,
                    secondaryColor = Color(0xFFF59E0B)
                )
            )
        }

        // 4. Sleeper Coaches (S1 to S6)
        for (i in 1..6) {
            coaches.add(
                CoachPosition(
                    sequenceNumber = seq++,
                    coachCode = "S$i",
                    coachClass = CoachClass.SLEEPER,
                    displayName = "Sleeper Class (S$i)",
                    totalSeats = 80,
                    isAirConditioned = false,
                    primaryColor = LhbBlue,
                    secondaryColor = LhbGray
                )
            )
        }

        // 5. Pantry Car (PC)
        coaches.add(
            CoachPosition(
                sequenceNumber = seq++,
                coachCode = "PC",
                coachClass = CoachClass.PANTRY_CAR,
                displayName = "Pantry Car / Catering",
                totalSeats = 0,
                isAirConditioned = true,
                primaryColor = LhbBlue,
                secondaryColor = Color(0xFFE11D48)
            )
        )

        // 6. AC 3 Tier (B1 to B6)
        for (i in 1..6) {
            coaches.add(
                CoachPosition(
                    sequenceNumber = seq++,
                    coachCode = "B$i",
                    coachClass = CoachClass.THIRD_AC,
                    displayName = "AC 3 Tier (B$i)",
                    totalSeats = 72,
                    isAirConditioned = true,
                    primaryColor = LhbBlue,
                    secondaryColor = Color(0xFF38BDF8)
                )
            )
        }

        // 7. AC 3 Economy (M1)
        coaches.add(
            CoachPosition(
                sequenceNumber = seq++,
                coachCode = "M1",
                coachClass = CoachClass.THIRD_AC_ECONOMY,
                displayName = "AC 3 Economy (M1)",
                totalSeats = 83,
                isAirConditioned = true,
                primaryColor = LhbBlue,
                secondaryColor = Color(0xFF06B6D4)
            )
        )

        // 8. AC 2 Tier (A1, A2)
        for (i in 1..2) {
            coaches.add(
                CoachPosition(
                    sequenceNumber = seq++,
                    coachCode = "A$i",
                    coachClass = CoachClass.SECOND_AC,
                    displayName = "AC 2 Tier (A$i)",
                    totalSeats = 54,
                    isAirConditioned = true,
                    primaryColor = LhbBlue,
                    secondaryColor = Color(0xFF10B981)
                )
            )
        }

        // 9. AC First Class (H1)
        coaches.add(
            CoachPosition(
                sequenceNumber = seq++,
                coachCode = "H1",
                coachClass = CoachClass.FIRST_AC,
                displayName = "AC First Class (H1)",
                totalSeats = 24,
                isAirConditioned = true,
                primaryColor = LhbBlue,
                secondaryColor = Color(0xFFFBBF24)
            )
        )

        // 10. Rear General Unreserved (GS3, GS4)
        for (i in 3..4) {
            coaches.add(
                CoachPosition(
                    sequenceNumber = seq++,
                    coachCode = "GS$i",
                    coachClass = CoachClass.GENERAL_UNRESERVED,
                    displayName = "General Second Class (GS$i)",
                    totalSeats = 90,
                    isAirConditioned = false,
                    primaryColor = LhbBlue,
                    secondaryColor = Color(0xFFF59E0B)
                )
            )
        }

        // 11. Rear Guard Van (SLRD)
        coaches.add(
            CoachPosition(
                sequenceNumber = seq,
                coachCode = "SLRD",
                coachClass = CoachClass.LUGGAGE_BRAKE_VAN,
                displayName = "Guard & Divyangjan Van",
                totalSeats = 30,
                isAirConditioned = false,
                primaryColor = LhbBlue,
                secondaryColor = LhbGray
            )
        )

        return TrainRakeComposition(
            trainNumber = trainNumber,
            trainName = trainName,
            trainType = trainType.ifBlank { "Superfast Express" },
            totalCoaches = coaches.size,
            engineType = locoModel,
            coaches = coaches
        )
    }

    // ── Helper Creators ──────────────────────────────────────────────────────
    private fun createEngine(seq: Int, code: String, model: String, color: Color) = CoachPosition(
        sequenceNumber = seq,
        coachCode = code,
        coachClass = CoachClass.LOCOMOTIVE,
        displayName = model,
        totalSeats = 0,
        isAirConditioned = true,
        primaryColor = color,
        secondaryColor = Color(0xFF0F172A),
        hasToilets = false,
        isLocomotive = true
    )

    private fun createEog(seq: Int, code: String, primary: Color, secondary: Color) = CoachPosition(
        sequenceNumber = seq,
        coachCode = code,
        coachClass = CoachClass.GENERATOR_CAR,
        displayName = "End on Generation & Luggage ($code)",
        totalSeats = 0,
        isAirConditioned = false,
        primaryColor = primary,
        secondaryColor = secondary
    )

    /**
     * Generates standard Indian Railways Berth / Seat Layout for any Coach.
     */
    fun generateSeatLayout(coach: CoachPosition): List<SeatBerth> {
        val seats = mutableListOf<SeatBerth>()
        val total = coach.totalSeats
        if (total <= 0) return emptyList()

        when (coach.coachClass) {
            CoachClass.THIRD_AC, CoachClass.SLEEPER -> {
                // Standard 8-berth bay: 1-LB, 2-MB, 3-UB, 4-LB, 5-MB, 6-UB, 7-SL, 8-SU
                for (s in 1..total) {
                    val mod = (s - 1) % 8
                    val cabin = ((s - 1) / 8) + 1
                    val (type, isWindow) = when (mod) {
                        0 -> "Lower Berth" to true
                        1 -> "Middle Berth" to false
                        2 -> "Upper Berth" to false
                        3 -> "Lower Berth" to false
                        4 -> "Middle Berth" to false
                        5 -> "Upper Berth" to false
                        6 -> "Side Lower" to true
                        7 -> "Side Upper" to true
                        else -> "Berth" to false
                    }
                    seats.add(SeatBerth(seatNumber = s, berthType = type, isWindow = isWindow, cabinNumber = cabin))
                }
            }

            CoachClass.SECOND_AC -> {
                // Standard 6-berth bay: 1-LB, 2-UB, 3-LB, 4-UB, 5-SL, 6-SU
                for (s in 1..total) {
                    val mod = (s - 1) % 6
                    val cabin = ((s - 1) / 6) + 1
                    val (type, isWindow) = when (mod) {
                        0 -> "Lower Berth" to true
                        1 -> "Upper Berth" to false
                        2 -> "Lower Berth" to false
                        3 -> "Upper Berth" to false
                        4 -> "Side Lower" to true
                        5 -> "Side Upper" to true
                        else -> "Berth" to false
                    }
                    seats.add(SeatBerth(seatNumber = s, berthType = type, isWindow = isWindow, cabinNumber = cabin))
                }
            }

            CoachClass.AC_CHAIR_CAR -> {
                // 3x2 Seating: Window, Middle, Aisle | Aisle, Window
                for (s in 1..total) {
                    val mod = (s - 1) % 5
                    val row = ((s - 1) / 5) + 1
                    val (type, isWindow) = when (mod) {
                        0 -> "Window Seat (Left)" to true
                        1 -> "Middle Seat" to false
                        2 -> "Aisle Seat (Left)" to false
                        3 -> "Aisle Seat (Right)" to false
                        4 -> "Window Seat (Right)" to true
                        else -> "Chair" to false
                    }
                    seats.add(SeatBerth(seatNumber = s, berthType = type, isWindow = isWindow, cabinNumber = row))
                }
            }

            CoachClass.EXECUTIVE_CHAIR_CAR -> {
                // 2x2 Seating: Window, Aisle | Aisle, Window
                for (s in 1..total) {
                    val mod = (s - 1) % 4
                    val row = ((s - 1) / 4) + 1
                    val (type, isWindow) = when (mod) {
                        0 -> "Window Seat (Left)" to true
                        1 -> "Aisle Seat (Left)" to false
                        2 -> "Aisle Seat (Right)" to false
                        3 -> "Window Seat (Right)" to true
                        else -> "Executive Chair" to false
                    }
                    seats.add(SeatBerth(seatNumber = s, berthType = type, isWindow = isWindow, cabinNumber = row))
                }
            }

            CoachClass.FIRST_AC -> {
                // Cabins (4 berths) & Coupes (2 berths)
                for (s in 1..total) {
                    val cabin = ((s - 1) / 4) + 1
                    val mod = (s - 1) % 4
                    val type = if (s > 18) {
                        if (s % 2 == 1) "Coupe Lower" else "Coupe Upper"
                    } else {
                        when (mod) {
                            0 -> "Cabin Lower"
                            1 -> "Cabin Upper"
                            2 -> "Cabin Lower"
                            3 -> "Cabin Upper"
                            else -> "Berth"
                        }
                    }
                    seats.add(SeatBerth(seatNumber = s, berthType = type, isWindow = (mod == 0 || mod == 2), cabinNumber = cabin))
                }
            }

            else -> {
                for (s in 1..total) {
                    seats.add(SeatBerth(seatNumber = s, berthType = "Seat", isWindow = (s % 2 == 1), cabinNumber = ((s - 1) / 6) + 1))
                }
            }
        }

        return seats
    }
}
