package com.railgaadi.app.model

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Train Category Enum  — matches all IR train types used in RailGaadi
// ─────────────────────────────────────────────────────────────────────────────
enum class TrainCategory(val displayName: String, val emoji: String) {
    RAJDHANI_TEJAS("Rajdhani / Tejas", "🔵"),
    VANDE_BHARAT("Vande Bharat 2.0", "🟠"),
    SHATABDI("Shatabdi Express", "🟡"),
    DURONTO("Duronto Express", "🔴"),
    GARIB_RATH("Garib Rath", "🟣"),
    NAMO_BHARAT("Namo Bharat RRTS", "🟤"),
    MEMU("MEMU / EMU", "🟢"),
    SUPERFAST("SuperFast Express", "⚫"),
    MAIL_EXPRESS("Mail / Express", "⬜"),
    PASSENGER("Passenger / Ordinary", "🔲"),
    ANTYODAYA("Antyodaya Express", "🔶"),
    UDAY("UDAY Express", "🔷"),
    SUVIDHA("Suvidha Trains", "🔸"),
    SAMPARK_KRANTI("Sampark Kranti", "🔹")
}

// ─────────────────────────────────────────────────────────────────────────────
// Coach unit data — one coach in the rake
// ─────────────────────────────────────────────────────────────────────────────
data class RakeCoach(
    val code: String,           // e.g. "B4", "A1", "PC", "LOCO"
    val classType: String,      // "1A","2A","3A","3E","EC","CC","SL","2S","UR","LOCO","CAB","PANTRY","GEN","EOG"
    val bodyColor: Long,        // ARGB Long  (avoid Color in data layer for multiplatform safety)
    val roofColor: Long,
    val stripeColor: Long? = null,
    val widthDp: Float = 78f,
    val isAeroNose: Boolean = false   // loco / aerodynamic cab car
)

// ─────────────────────────────────────────────────────────────────────────────
// Coach colour palette constants (ARGB)
// ─────────────────────────────────────────────────────────────────────────────
private object CoachColors {
    // Rajdhani / Tejas — deep blue body, silver roof
    const val RAJDHANI_BODY  = 0xFF1E3A6E
    const val RAJDHANI_ROOF  = 0xFF8FAACC
    const val RAJDHANI_STRIPE= 0xFFFFD700

    // Vande Bharat — orange-blue nose, white body
    const val VB_BODY        = 0xFFFFFFFF
    const val VB_ROOF        = 0xFFE5E7EB
    const val VB_STRIPE_BLUE = 0xFF1D4ED8
    const val VB_NOSE_ORANGE = 0xFFEA580C
    const val VB_NOSE_BODY   = 0xFF1D4ED8

    // Shatabdi — white/grey stripe
    const val SHAT_BODY      = 0xFFF1F5F9
    const val SHAT_ROOF      = 0xFFCBD5E1
    const val SHAT_STRIPE    = 0xFF0369A1

    // Duronto — red body
    const val DURONTO_BODY   = 0xFF991B1B
    const val DURONTO_ROOF   = 0xFF7F1D1D
    const val DURONTO_STRIPE = 0xFFEF4444

    // Garib Rath — maroon/purple
    const val GR_BODY        = 0xFF6B21A8
    const val GR_ROOF        = 0xFF581C87
    const val GR_STRIPE      = 0xFFD8B4FE

    // Namo Bharat / RRTS — silver-grey aerodynamic
    const val RRTS_BODY      = 0xFFE2E8F0
    const val RRTS_ROOF      = 0xFFCBD5E1
    const val RRTS_STRIPE    = 0xFF0EA5E9
    const val RRTS_NOSE      = 0xFF075985

    // MEMU — green
    const val MEMU_BODY      = 0xFF14532D
    const val MEMU_ROOF      = 0xFF166534
    const val MEMU_STRIPE    = 0xFF4ADE80

    // SuperFast / Mail-Express — ICF red-cream
    const val SF_BODY        = 0xFFC23B22
    const val SF_CREAM       = 0xFFF5DEB3
    const val SF_ROOF        = 0xFF78350F

    // Pantry / EOG / Luggage — shared
    const val PANTRY_BODY    = 0xFFB45309
    const val EOG_BODY       = 0xFF374151
    const val LOCO_BODY      = 0xFF1F2937
    const val LOCO_ROOF      = 0xFF111827

    // General / UR
    const val GEN_BODY       = 0xFF78716C
    const val GEN_ROOF       = 0xFF57534E
}

// ─────────────────────────────────────────────────────────────────────────────
// Rake templates — the actual coach composition for each category
// ─────────────────────────────────────────────────────────────────────────────
object TrainRakeDataService {

    /**
     * Returns the full rake (coach list) for a given category,
     * with the user's PNR coach flagged as [isUser] = true (by code match).
     * The list is in loco-first → tail order.
     */
    fun getRakeForCategory(category: TrainCategory, userCoachCode: String?): List<RakeCoach> {
        val base = when (category) {
            TrainCategory.RAJDHANI_TEJAS   -> rajdhaniRake()
            TrainCategory.VANDE_BHARAT     -> vandeBharatRake()
            TrainCategory.SHATABDI         -> shatabdiRake()
            TrainCategory.DURONTO          -> durontoRake()
            TrainCategory.GARIB_RATH       -> garibRathRake()
            TrainCategory.NAMO_BHARAT      -> namoBharatRake()
            TrainCategory.MEMU             -> memuRake()
            TrainCategory.SUPERFAST        -> superFastRake()
            TrainCategory.MAIL_EXPRESS     -> mailExpressRake()
            TrainCategory.PASSENGER        -> passengerRake()
            TrainCategory.ANTYODAYA        -> antyodayaRake()
            TrainCategory.UDAY             -> udayRake()
            TrainCategory.SUVIDHA          -> suvidhaRake()
            TrainCategory.SAMPARK_KRANTI   -> samparkKrantiRake()
        }
        // User coach matching is handled by the UI layer using userCoachCode comparison
        return base
    }

    /**
     * Smart category detection from train name + type strings.
     * Covers all IR naming conventions.
     */
    fun detectCategory(trainName: String, trainType: String): TrainCategory {
        val n = trainName.uppercase()
        val t = trainType.uppercase()
        return when {
            n.contains("VANDE") || n.contains("VANDE BHARAT")           -> TrainCategory.VANDE_BHARAT
            n.contains("NAMO BHARAT") || n.contains("RRTS")
                    || n.contains("RAPIDX")                              -> TrainCategory.NAMO_BHARAT
            n.contains("RAJDHANI") || n.contains("TEJAS")               -> TrainCategory.RAJDHANI_TEJAS
            n.contains("SHATABDI") || n.contains("GATIMAAN")            -> TrainCategory.SHATABDI
            n.contains("DURONTO")                                        -> TrainCategory.DURONTO
            n.contains("GARIB RATH")                                     -> TrainCategory.GARIB_RATH
            n.contains("SAMPARK KRANTI") || n.contains("SK EXP")        -> TrainCategory.SAMPARK_KRANTI
            n.contains("SUVIDHA")                                        -> TrainCategory.SUVIDHA
            n.contains("UDAY")                                           -> TrainCategory.UDAY
            n.contains("ANTYODAYA")                                      -> TrainCategory.ANTYODAYA
            n.contains("MEMU") || n.contains("EMU") || t.contains("MEMU")-> TrainCategory.MEMU
            n.contains("PASSENGER") || t.contains("PASSENGER")          -> TrainCategory.PASSENGER
            t.contains("SUPERFAST") || t.contains("SF") || t.contains("SUPER FAST") -> TrainCategory.SUPERFAST
            t.contains("MAIL") || t.contains("EXPRESS")                 -> TrainCategory.MAIL_EXPRESS
            else                                                          -> TrainCategory.MAIL_EXPRESS
        }
    }

    // ─── Individual rake builders ─────────────────────────────────────────────

    private fun rajdhaniRake(): List<RakeCoach> = buildList {
        add(loco("WAP7"))
        add(eog("EOG1"))
        add(coach("H1",  "1A",  CoachColors.RAJDHANI_BODY, CoachColors.RAJDHANI_ROOF, CoachColors.RAJDHANI_STRIPE))
        add(coach("A1",  "2A",  CoachColors.RAJDHANI_BODY, CoachColors.RAJDHANI_ROOF, CoachColors.RAJDHANI_STRIPE))
        add(coach("A2",  "2A",  CoachColors.RAJDHANI_BODY, CoachColors.RAJDHANI_ROOF, CoachColors.RAJDHANI_STRIPE))
        add(coach("B1",  "3A",  CoachColors.RAJDHANI_BODY, CoachColors.RAJDHANI_ROOF, CoachColors.RAJDHANI_STRIPE))
        add(coach("B2",  "3A",  CoachColors.RAJDHANI_BODY, CoachColors.RAJDHANI_ROOF, CoachColors.RAJDHANI_STRIPE))
        add(coach("B3",  "3A",  CoachColors.RAJDHANI_BODY, CoachColors.RAJDHANI_ROOF, CoachColors.RAJDHANI_STRIPE))
        add(coach("B4",  "3A",  CoachColors.RAJDHANI_BODY, CoachColors.RAJDHANI_ROOF, CoachColors.RAJDHANI_STRIPE))
        add(pantry("PC"))
        add(coach("B5",  "3A",  CoachColors.RAJDHANI_BODY, CoachColors.RAJDHANI_ROOF, CoachColors.RAJDHANI_STRIPE))
        add(coach("B6",  "3A",  CoachColors.RAJDHANI_BODY, CoachColors.RAJDHANI_ROOF, CoachColors.RAJDHANI_STRIPE))
        add(coach("B7",  "3A",  CoachColors.RAJDHANI_BODY, CoachColors.RAJDHANI_ROOF, CoachColors.RAJDHANI_STRIPE))
        add(coach("B8",  "3A",  CoachColors.RAJDHANI_BODY, CoachColors.RAJDHANI_ROOF, CoachColors.RAJDHANI_STRIPE))
        add(coach("B9",  "3A",  CoachColors.RAJDHANI_BODY, CoachColors.RAJDHANI_ROOF, CoachColors.RAJDHANI_STRIPE))
        add(coach("B10", "3A",  CoachColors.RAJDHANI_BODY, CoachColors.RAJDHANI_ROOF, CoachColors.RAJDHANI_STRIPE))
        add(eog("EOG2"))
    }

    private fun vandeBharatRake(): List<RakeCoach> = buildList {
        // Vande Bharat 2.0 — 16-car EMU set, aerodynamic nose on both ends
        add(cabCar("TC1", CoachColors.VB_NOSE_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, isNose = true))
        add(coach("EC1", "EC", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 72f))
        add(coach("EC2", "EC", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 72f))
        add(coach("CC3", "CC", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 72f))
        add(coach("CC4", "CC", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 72f))
        add(coach("CC5", "CC", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 72f))
        add(coach("CC6", "CC", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 72f))
        add(coach("CC7", "CC", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 72f))
        add(coach("CC8", "CC", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 72f))
        add(coach("CC9", "CC", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 72f))
        add(coach("CC10","CC", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 72f))
        add(coach("CC11","CC", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 72f))
        add(coach("CC12","CC", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 72f))
        add(coach("CC13","CC", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 72f))
        add(coach("EC14","EC", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 72f))
        add(cabCar("TC2", CoachColors.VB_NOSE_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, isNose = true))
    }

    private fun shatabdiRake(): List<RakeCoach> = buildList {
        add(loco("WAP5"))
        add(coach("EC1", "EC", CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, CoachColors.SHAT_STRIPE))
        add(coach("EC2", "EC", CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, CoachColors.SHAT_STRIPE))
        add(coach("CC1", "CC", CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, CoachColors.SHAT_STRIPE))
        add(coach("CC2", "CC", CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, CoachColors.SHAT_STRIPE))
        add(coach("CC3", "CC", CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, CoachColors.SHAT_STRIPE))
        add(pantry("PC"))
        add(coach("CC4", "CC", CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, CoachColors.SHAT_STRIPE))
        add(coach("CC5", "CC", CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, CoachColors.SHAT_STRIPE))
        add(coach("CC6", "CC", CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, CoachColors.SHAT_STRIPE))
        add(coach("CC7", "CC", CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, CoachColors.SHAT_STRIPE))
        add(coach("CC8", "CC", CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, CoachColors.SHAT_STRIPE))
    }

    private fun durontoRake(): List<RakeCoach> = buildList {
        add(loco("WAP7"))
        add(eog("EOG1"))
        add(coach("H1", "1A",  CoachColors.DURONTO_BODY, CoachColors.DURONTO_ROOF, CoachColors.DURONTO_STRIPE))
        add(coach("A1", "2A",  CoachColors.DURONTO_BODY, CoachColors.DURONTO_ROOF, CoachColors.DURONTO_STRIPE))
        add(coach("A2", "2A",  CoachColors.DURONTO_BODY, CoachColors.DURONTO_ROOF, CoachColors.DURONTO_STRIPE))
        add(coach("B1", "3A",  CoachColors.DURONTO_BODY, CoachColors.DURONTO_ROOF, CoachColors.DURONTO_STRIPE))
        add(coach("B2", "3A",  CoachColors.DURONTO_BODY, CoachColors.DURONTO_ROOF, CoachColors.DURONTO_STRIPE))
        add(coach("B3", "3A",  CoachColors.DURONTO_BODY, CoachColors.DURONTO_ROOF, CoachColors.DURONTO_STRIPE))
        add(pantry("PC"))
        add(coach("S1", "SL",  CoachColors.DURONTO_BODY, CoachColors.DURONTO_ROOF, CoachColors.DURONTO_STRIPE))
        add(coach("S2", "SL",  CoachColors.DURONTO_BODY, CoachColors.DURONTO_ROOF, CoachColors.DURONTO_STRIPE))
        add(coach("S3", "SL",  CoachColors.DURONTO_BODY, CoachColors.DURONTO_ROOF, CoachColors.DURONTO_STRIPE))
        add(coach("S4", "SL",  CoachColors.DURONTO_BODY, CoachColors.DURONTO_ROOF, CoachColors.DURONTO_STRIPE))
        add(eog("EOG2"))
    }

    private fun garibRathRake(): List<RakeCoach> = buildList {
        add(loco("WAP4"))
        add(eog("EOG1"))
        add(coach("A1", "3A",  CoachColors.GR_BODY, CoachColors.GR_ROOF, CoachColors.GR_STRIPE))
        add(coach("A2", "3A",  CoachColors.GR_BODY, CoachColors.GR_ROOF, CoachColors.GR_STRIPE))
        add(coach("A3", "3A",  CoachColors.GR_BODY, CoachColors.GR_ROOF, CoachColors.GR_STRIPE))
        add(coach("A4", "3A",  CoachColors.GR_BODY, CoachColors.GR_ROOF, CoachColors.GR_STRIPE))
        add(coach("A5", "3E",  CoachColors.GR_BODY, CoachColors.GR_ROOF, CoachColors.GR_STRIPE))
        add(coach("A6", "3E",  CoachColors.GR_BODY, CoachColors.GR_ROOF, CoachColors.GR_STRIPE))
        add(pantry("PC"))
        add(coach("A7", "3E",  CoachColors.GR_BODY, CoachColors.GR_ROOF, CoachColors.GR_STRIPE))
        add(coach("A8", "3E",  CoachColors.GR_BODY, CoachColors.GR_ROOF, CoachColors.GR_STRIPE))
        add(coach("A9", "3E",  CoachColors.GR_BODY, CoachColors.GR_ROOF, CoachColors.GR_STRIPE))
        add(eog("EOG2"))
    }

    private fun namoBharatRake(): List<RakeCoach> = buildList {
        // 6-car RRTS set
        add(cabCar("C1", CoachColors.RRTS_NOSE, CoachColors.RRTS_ROOF, CoachColors.RRTS_STRIPE, isNose = true))
        add(coach("P1", "CC", CoachColors.RRTS_BODY, CoachColors.RRTS_ROOF, CoachColors.RRTS_STRIPE, 70f))
        add(coach("P2", "CC", CoachColors.RRTS_BODY, CoachColors.RRTS_ROOF, CoachColors.RRTS_STRIPE, 70f))
        add(coach("P3", "CC", CoachColors.RRTS_BODY, CoachColors.RRTS_ROOF, CoachColors.RRTS_STRIPE, 70f))
        add(coach("P4", "EC", CoachColors.RRTS_BODY, CoachColors.RRTS_ROOF, CoachColors.RRTS_STRIPE, 70f))
        add(cabCar("C2", CoachColors.RRTS_NOSE, CoachColors.RRTS_ROOF, CoachColors.RRTS_STRIPE, isNose = true))
    }

    private fun memuRake(): List<RakeCoach> = buildList {
        // 12-car MEMU
        add(cabCar("DPC1", CoachColors.MEMU_BODY, CoachColors.MEMU_ROOF, CoachColors.MEMU_STRIPE, isNose = true))
        for (i in 1..10) {
            add(coach("TC$i", if (i % 3 == 0) "2S" else "GEN", CoachColors.MEMU_BODY, CoachColors.MEMU_ROOF, CoachColors.MEMU_STRIPE, 68f))
        }
        add(cabCar("DPC2", CoachColors.MEMU_BODY, CoachColors.MEMU_ROOF, CoachColors.MEMU_STRIPE, isNose = true))
    }

    private fun superFastRake(): List<RakeCoach> = buildList {
        add(loco("WAP4"))
        add(eog("EOG1"))
        add(coach("A1", "2A",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("B1", "3A",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("B2", "3A",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("B3", "3A",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(pantry("PC"))
        add(coach("S1", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("S2", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("S3", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("S4", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("S5", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("G1", "GEN", CoachColors.GEN_BODY, CoachColors.GEN_ROOF, null))
        add(coach("G2", "GEN", CoachColors.GEN_BODY, CoachColors.GEN_ROOF, null))
        add(eog("EOG2"))
    }

    private fun mailExpressRake(): List<RakeCoach> = buildList {
        add(loco("WAM4"))
        add(eog("EOG1"))
        add(coach("A1", "2A",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("B1", "3A",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("B2", "3A",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(pantry("PC"))
        add(coach("S1", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("S2", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("S3", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("S4", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("S5", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("S6", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, CoachColors.SF_CREAM))
        add(coach("G1", "GEN", CoachColors.GEN_BODY, CoachColors.GEN_ROOF, null))
        add(coach("G2", "GEN", CoachColors.GEN_BODY, CoachColors.GEN_ROOF, null))
        add(coach("G3", "GEN", CoachColors.GEN_BODY, CoachColors.GEN_ROOF, null))
        add(eog("EOG2"))
    }

    private fun passengerRake(): List<RakeCoach> = buildList {
        add(loco("WDM3"))
        for (i in 1..8) {
            add(coach("G$i", "GEN", CoachColors.GEN_BODY, CoachColors.GEN_ROOF, null, 68f))
        }
        add(coach("S1", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, null, 68f))
        add(coach("S2", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, null, 68f))
    }

    private fun antyodayaRake(): List<RakeCoach> = buildList {
        add(loco("WAP7"))
        add(eog("EOG1"))
        for (i in 1..12) {
            add(coach("D$i", "2S", 0xFF7C3AED, 0xFF6D28D9, 0xFFA78BFA, 76f))
        }
        add(eog("EOG2"))
    }

    private fun udayRake(): List<RakeCoach> = buildList {
        add(loco("WAP7"))
        add(eog("EOG1"))
        add(coach("UEC1", "EC", 0xFF1E3A6E, CoachColors.RAJDHANI_ROOF, 0xFF60A5FA))
        add(coach("UEC2", "EC", 0xFF1E3A6E, CoachColors.RAJDHANI_ROOF, 0xFF60A5FA))
        add(coach("UCC1", "CC", 0xFF1E3A6E, CoachColors.RAJDHANI_ROOF, 0xFF60A5FA))
        add(coach("UCC2", "CC", 0xFF1E3A6E, CoachColors.RAJDHANI_ROOF, 0xFF60A5FA))
        add(coach("UCC3", "CC", 0xFF1E3A6E, CoachColors.RAJDHANI_ROOF, 0xFF60A5FA))
        add(pantry("PC"))
        add(coach("UCC4", "CC", 0xFF1E3A6E, CoachColors.RAJDHANI_ROOF, 0xFF60A5FA))
        add(coach("UCC5", "CC", 0xFF1E3A6E, CoachColors.RAJDHANI_ROOF, 0xFF60A5FA))
        add(eog("EOG2"))
    }

    private fun suvidhaRake(): List<RakeCoach> = buildList {
        add(loco("WAP7"))
        add(eog("EOG1"))
        add(coach("A1", "2A",  0xFF0C4A6E, CoachColors.RAJDHANI_ROOF, 0xFF38BDF8))
        add(coach("B1", "3A",  0xFF0C4A6E, CoachColors.RAJDHANI_ROOF, 0xFF38BDF8))
        add(coach("B2", "3A",  0xFF0C4A6E, CoachColors.RAJDHANI_ROOF, 0xFF38BDF8))
        add(coach("B3", "3A",  0xFF0C4A6E, CoachColors.RAJDHANI_ROOF, 0xFF38BDF8))
        add(pantry("PC"))
        add(coach("S1", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, 0xFF38BDF8))
        add(coach("S2", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, 0xFF38BDF8))
        add(coach("S3", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, 0xFF38BDF8))
        add(coach("G1", "GEN", CoachColors.GEN_BODY, CoachColors.GEN_ROOF, null))
        add(eog("EOG2"))
    }

    private fun samparkKrantiRake(): List<RakeCoach> = buildList {
        add(loco("WAP4"))
        add(eog("EOG1"))
        add(coach("A1", "2A",  0xFF0F4C81, CoachColors.RAJDHANI_ROOF, 0xFFFF6B35))
        add(coach("B1", "3A",  0xFF0F4C81, CoachColors.RAJDHANI_ROOF, 0xFFFF6B35))
        add(coach("B2", "3A",  0xFF0F4C81, CoachColors.RAJDHANI_ROOF, 0xFFFF6B35))
        add(coach("B3", "3A",  0xFF0F4C81, CoachColors.RAJDHANI_ROOF, 0xFFFF6B35))
        add(pantry("PC"))
        add(coach("S1", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, 0xFFFF6B35))
        add(coach("S2", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, 0xFFFF6B35))
        add(coach("S3", "SL",  CoachColors.SF_BODY, CoachColors.SF_ROOF, 0xFFFF6B35))
        add(coach("G1", "GEN", CoachColors.GEN_BODY, CoachColors.GEN_ROOF, null))
        add(eog("EOG2"))
    }

    // ─── Coach builder helpers ────────────────────────────────────────────────

    private fun loco(code: String) = RakeCoach(
        code = code, classType = "LOCO",
        bodyColor = CoachColors.LOCO_BODY, roofColor = CoachColors.LOCO_ROOF,
        widthDp = 86f, isAeroNose = false
    )

    private fun cabCar(code: String, body: Long, roof: Long, stripe: Long, isNose: Boolean = false) = RakeCoach(
        code = code, classType = "CAB",
        bodyColor = body, roofColor = roof, stripeColor = stripe,
        widthDp = 80f, isAeroNose = isNose
    )

    private fun eog(code: String) = RakeCoach(
        code = code, classType = "EOG",
        bodyColor = CoachColors.EOG_BODY, roofColor = CoachColors.LOCO_ROOF,
        widthDp = 72f
    )

    private fun pantry(code: String) = RakeCoach(
        code = code, classType = "PANTRY",
        bodyColor = CoachColors.PANTRY_BODY, roofColor = CoachColors.SF_ROOF,
        widthDp = 72f
    )

    private fun coach(
        code: String, classType: String,
        body: Long, roof: Long, stripe: Long?,
        width: Float = 78f
    ) = RakeCoach(code = code, classType = classType, bodyColor = body, roofColor = roof, stripeColor = stripe, widthDp = width)

    // ─── Class display helpers ────────────────────────────────────────────────

    fun classDisplayName(classType: String): String = when (classType) {
        "1A"     -> "First AC"
        "2A"     -> "AC 2-Tier"
        "3A"     -> "AC 3-Tier"
        "3E"     -> "AC 3-Tier Economy"
        "EC"     -> "Executive Chair Car"
        "CC"     -> "AC Chair Car"
        "SL"     -> "Sleeper Class"
        "2S"     -> "Second Sitting"
        "GEN","UR" -> "General / UR"
        "LOCO"   -> "Locomotive"
        "CAB"    -> "Cab Car"
        "EOG"    -> "Power Car"
        "PANTRY" -> "Pantry Car"
        else     -> classType
    }
}
