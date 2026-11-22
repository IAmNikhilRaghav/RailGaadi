package com.railgaadi.app.model

// ─────────────────────────────────────────────────────────────────────────────
// Train Category Enum — matches all IR train types used in RailGaadi
// ─────────────────────────────────────────────────────────────────────────────
enum class TrainCategory(val displayName: String, val emoji: String) {
    RAJDHANI_TEJAS("Rajdhani / Tejas", "🔴"),
    VANDE_BHARAT("Vande Bharat 2.0", "⚪"),
    SHATABDI("Shatabdi Express", "🔵"),
    DURONTO("Duronto Express", "🟢"),
    GARIB_RATH("Garib Rath", "🟡"),
    SUPERFAST("SuperFast / Mail", "🚆"),
    NAMO_BHARAT("Namo Bharat (RRTS)", "🚄"),
    MEMU("MEMU Commuter", "⚡"),
    PASSENGER("Passenger Ordinary", "🔲"),
    ANTYODAYA("Antyodaya Express", "🔶"),
    UDAY("UDAY Double Decker", "🔷"),
    SUVIDHA("Suvidha Express", "🔸"),
    SAMPARK_KRANTI("Sampark Kranti", "🔹")
}

// ─────────────────────────────────────────────────────────────────────────────
// Coach unit data — one coach in the rake
// ─────────────────────────────────────────────────────────────────────────────
data class RakeCoach(
    val code: String,           // e.g. "B4", "A1", "PC", "LOCO", "GEN", "S3"
    val classType: String,      // "1A","2A","3A","3E","EC","CC","SL","2S","UR","LOCO","CAB","PANTRY","GEN","EOG"
    val bodyColor: Long,        // ARGB Long
    val roofColor: Long,
    val stripeColor: Long? = null,
    val widthDp: Float = 84f,
    val isAeroNose: Boolean = false
)

// ─────────────────────────────────────────────────────────────────────────────
// Authentic Coach Liveries & Color Palette (ARGB)
// ─────────────────────────────────────────────────────────────────────────────
private object CoachColors {
    // Tejas / Rajdhani — Vibrant Crimson Red LHB livery
    const val TEJAS_BODY     = 0xFFB91C1C
    const val TEJAS_ROOF     = 0xFF7F1D1D
    const val TEJAS_STRIPE   = 0xFFFACC15 // Golden yellow stripe

    // Locomotive — Red WAP-7 / WAP-5 / WAP-4
    const val LOCO_BODY      = 0xFFDC2626
    const val LOCO_ROOF      = 0xFF991B1B

    // Vande Bharat 2.0 — White trainset with blue aerodynamic stripe
    const val VB_BODY        = 0xFFFFFFFF
    const val VB_ROOF        = 0xFF0C4A6E
    const val VB_STRIPE_BLUE = 0xFF0284C7
    const val VB_STRIPE_PURP = 0xFF7C3AED // Executive class

    // Shatabdi — Sky Blue & Silver Grey
    const val SHAT_BODY      = 0xFF38BDF8
    const val SHAT_ROOF      = 0xFF0369A1
    const val SHAT_EC        = 0xFF7C3AED

    // Duronto — Vibrant Green & Yellow Vinyl
    const val DURONTO_GREEN  = 0xFF15803D
    const val DURONTO_YELLOW = 0xFFFACC15
    const val DURONTO_ROOF_G = 0xFF14532D
    const val DURONTO_ROOF_Y = 0xFFCA8A04

    // Garib Rath — Forest Green with Gold Roof
    const val GR_BODY        = 0xFF15803D
    const val GR_ROOF        = 0xFFFACC15

    // SuperFast / Mail Express — Canonical Rail Blue LHB / ICF
    const val SF_RAIL_BLUE   = 0xFF0369A1
    const val SF_ROOF_BLUE   = 0xFF082F49
    const val SF_STRIPE_SKY  = 0xFF38BDF8

    // Sleeper Class (SL)
    const val SL_BODY        = 0xFF0284C7
    const val SL_ROOF        = 0xFF075985
    const val SL_STRIPE      = 0xFFBAE6FD

    // General / Unreserved (UR / GEN) — Distinctive grey/amber or Rail Blue with yellow hazard stripes
    const val GEN_BODY       = 0xFF334155
    const val GEN_ROOF       = 0xFF1E293B
    const val GEN_STRIPE     = 0xFFF59E0B // Amber warning stripe

    // Namo Bharat RRTS
    const val RRTS_BODY      = 0xFFF8FAFC
    const val RRTS_ROOF      = 0xFFDC2626
    const val RRTS_STRIPE    = 0xFF0284C7

    // MEMU
    const val MEMU_BODY      = 0xFFEA580C
    const val MEMU_ROOF      = 0xFF9A3412

    // Shared units
    const val PANTRY_BODY    = 0xFF991B1B
    const val PANTRY_ROOF    = 0xFF450A0A
    const val EOG_BODY       = 0xFFB91C1C
    const val EOG_ROOF       = 0xFF7F1D1D
}

// ─────────────────────────────────────────────────────────────────────────────
// Train Rake Database Service
// ─────────────────────────────────────────────────────────────────────────────
object TrainRakeDataService {

    fun getRakeForCategory(category: TrainCategory, userCoachCode: String?): List<RakeCoach> {
        return when (category) {
            TrainCategory.RAJDHANI_TEJAS   -> rajdhaniRake()
            TrainCategory.VANDE_BHARAT     -> vandeBharatRake()
            TrainCategory.SHATABDI         -> shatabdiRake()
            TrainCategory.DURONTO          -> durontoRake()
            TrainCategory.GARIB_RATH       -> garibRathRake()
            TrainCategory.SUPERFAST        -> superFastRake()
            TrainCategory.NAMO_BHARAT      -> namoBharatRake()
            TrainCategory.MEMU             -> memuRake()
            TrainCategory.PASSENGER        -> passengerRake()
            TrainCategory.ANTYODAYA        -> antyodayaRake()
            TrainCategory.UDAY             -> udayRake()
            TrainCategory.SUVIDHA          -> suvidhaRake()
            TrainCategory.SAMPARK_KRANTI   -> samparkKrantiRake()
        }
    }

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
            else                                                          -> TrainCategory.SUPERFAST
        }
    }

    // ─── Authentic Rakes ──────────────────────────────────────────────────────

    // Rajdhani / Tejas Rake (Matches HTML Prototype exactly)
    private fun rajdhaniRake(): List<RakeCoach> = listOf(
        RakeCoach("WAP-7", "LOCO",   CoachColors.LOCO_BODY,  CoachColors.LOCO_ROOF,  null, 94f, true),
        RakeCoach("EOG",   "EOG",    CoachColors.TEJAS_BODY, CoachColors.TEJAS_ROOF, CoachColors.TEJAS_STRIPE, 78f),
        RakeCoach("H1",    "1A",     CoachColors.TEJAS_BODY, CoachColors.TEJAS_ROOF, CoachColors.TEJAS_STRIPE, 84f),
        RakeCoach("A1",    "2A",     CoachColors.TEJAS_BODY, CoachColors.TEJAS_ROOF, CoachColors.TEJAS_STRIPE, 84f),
        RakeCoach("A2",    "2A",     CoachColors.TEJAS_BODY, CoachColors.TEJAS_ROOF, CoachColors.TEJAS_STRIPE, 84f),
        RakeCoach("B1",    "3A",     CoachColors.TEJAS_BODY, CoachColors.TEJAS_ROOF, CoachColors.TEJAS_STRIPE, 84f),
        RakeCoach("B2",    "3A",     CoachColors.TEJAS_BODY, CoachColors.TEJAS_ROOF, CoachColors.TEJAS_STRIPE, 84f),
        RakeCoach("B3",    "3A",     CoachColors.TEJAS_BODY, CoachColors.TEJAS_ROOF, CoachColors.TEJAS_STRIPE, 84f),
        RakeCoach("B4",    "3A",     CoachColors.TEJAS_BODY, CoachColors.TEJAS_ROOF, CoachColors.TEJAS_STRIPE, 84f), // User Default
        RakeCoach("PC",    "PANTRY", CoachColors.PANTRY_BODY,CoachColors.PANTRY_ROOF,null, 84f),
        RakeCoach("B5",    "3A",     CoachColors.TEJAS_BODY, CoachColors.TEJAS_ROOF, CoachColors.TEJAS_STRIPE, 84f),
        RakeCoach("B6",    "3A",     CoachColors.TEJAS_BODY, CoachColors.TEJAS_ROOF, CoachColors.TEJAS_STRIPE, 84f),
        RakeCoach("B7",    "3A",     CoachColors.TEJAS_BODY, CoachColors.TEJAS_ROOF, CoachColors.TEJAS_STRIPE, 84f),
        RakeCoach("B8",    "3A",     CoachColors.TEJAS_BODY, CoachColors.TEJAS_ROOF, CoachColors.TEJAS_STRIPE, 84f),
        RakeCoach("B9",    "3A",     CoachColors.TEJAS_BODY, CoachColors.TEJAS_ROOF, CoachColors.TEJAS_STRIPE, 84f),
        RakeCoach("EOG",   "EOG",    CoachColors.TEJAS_BODY, CoachColors.TEJAS_ROOF, CoachColors.TEJAS_STRIPE, 78f)
    )

    // Vande Bharat 2.0 (16-Car White/Blue Trainset with Aero Nose)
    private fun vandeBharatRake(): List<RakeCoach> = listOf(
        RakeCoach("DTC",  "CAB", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 96f, true),
        RakeCoach("MC",   "CC",  CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 84f),
        RakeCoach("TC",   "CC",  CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 84f),
        RakeCoach("MC2",  "CC",  CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 84f),
        RakeCoach("EC",   "EC",  CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_PURP, 84f),
        RakeCoach("C3",   "CC",  CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 84f),
        RakeCoach("MC3",  "CC",  CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 84f),
        RakeCoach("DTC2", "CAB", CoachColors.VB_BODY, CoachColors.VB_ROOF, CoachColors.VB_STRIPE_BLUE, 96f, true)
    )

    // Shatabdi Express
    private fun shatabdiRake(): List<RakeCoach> = listOf(
        RakeCoach("WAP-5", "LOCO", CoachColors.LOCO_BODY, CoachColors.LOCO_ROOF, null, 94f, true),
        RakeCoach("EOG",   "EOG",  CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, null, 78f),
        RakeCoach("E1",    "EC",   CoachColors.SHAT_EC,   CoachColors.SHAT_ROOF, null, 84f),
        RakeCoach("C1",    "CC",   CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, null, 84f),
        RakeCoach("C2",    "CC",   CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, null, 84f),
        RakeCoach("C3",    "CC",   CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, null, 84f),
        RakeCoach("C4",    "CC",   CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, null, 84f),
        RakeCoach("EOG2",  "EOG",  CoachColors.SHAT_BODY, CoachColors.SHAT_ROOF, null, 78f)
    )

    // Duronto Express
    private fun durontoRake(): List<RakeCoach> = listOf(
        RakeCoach("WAP-7", "LOCO",   CoachColors.LOCO_BODY,      CoachColors.LOCO_ROOF,     null, 94f, true),
        RakeCoach("EOG",   "EOG",    CoachColors.DURONTO_GREEN,  CoachColors.DURONTO_ROOF_G,null, 78f),
        RakeCoach("A1",    "2A",     CoachColors.DURONTO_GREEN,  CoachColors.DURONTO_ROOF_G,null, 84f),
        RakeCoach("B1",    "3A",     CoachColors.DURONTO_YELLOW, CoachColors.DURONTO_ROOF_Y,null, 84f),
        RakeCoach("B2",    "3A",     CoachColors.DURONTO_YELLOW, CoachColors.DURONTO_ROOF_Y,null, 84f),
        RakeCoach("PC",    "PANTRY", CoachColors.DURONTO_GREEN,  CoachColors.DURONTO_ROOF_G,null, 84f),
        RakeCoach("S1",    "SL",     CoachColors.DURONTO_YELLOW, CoachColors.DURONTO_ROOF_Y,null, 84f),
        RakeCoach("S2",    "SL",     CoachColors.DURONTO_YELLOW, CoachColors.DURONTO_ROOF_Y,null, 84f),
        RakeCoach("EOG2",  "EOG",    CoachColors.DURONTO_GREEN,  CoachColors.DURONTO_ROOF_G,null, 78f)
    )

    // Garib Rath
    private fun garibRathRake(): List<RakeCoach> = listOf(
        RakeCoach("WAP-7", "LOCO", CoachColors.LOCO_BODY, CoachColors.LOCO_ROOF, null, 94f, true),
        RakeCoach("G1",    "3A",   CoachColors.GR_BODY,   CoachColors.GR_ROOF,   null, 84f),
        RakeCoach("G2",    "3A",   CoachColors.GR_BODY,   CoachColors.GR_ROOF,   null, 84f),
        RakeCoach("G3",    "3A",   CoachColors.GR_BODY,   CoachColors.GR_ROOF,   null, 84f),
        RakeCoach("G4",    "3A",   CoachColors.GR_BODY,   CoachColors.GR_ROOF,   null, 84f),
        RakeCoach("G5",    "3A",   CoachColors.GR_BODY,   CoachColors.GR_ROOF,   null, 84f),
        RakeCoach("EOG",   "EOG",  CoachColors.GR_BODY,   CoachColors.GR_ROOF,   null, 78f)
    )

    // SuperFast / Mail Express (Canonical Rail Blue with GEN, SL, 3A, 2A, 1A)
    private fun superFastRake(): List<RakeCoach> = listOf(
        RakeCoach("WAP-4", "LOCO",   CoachColors.LOCO_BODY,    CoachColors.LOCO_ROOF,   null, 94f, true),
        RakeCoach("GEN",   "UR",     CoachColors.GEN_BODY,     CoachColors.GEN_ROOF,    CoachColors.GEN_STRIPE, 80f),
        RakeCoach("S1",    "SL",     CoachColors.SL_BODY,      CoachColors.SL_ROOF,     CoachColors.SL_STRIPE, 84f),
        RakeCoach("S2",    "SL",     CoachColors.SL_BODY,      CoachColors.SL_ROOF,     CoachColors.SL_STRIPE, 84f),
        RakeCoach("S3",    "SL",     CoachColors.SL_BODY,      CoachColors.SL_ROOF,     CoachColors.SL_STRIPE, 84f),
        RakeCoach("S4",    "SL",     CoachColors.SL_BODY,      CoachColors.SL_ROOF,     CoachColors.SL_STRIPE, 84f),
        RakeCoach("B1",    "3A",     CoachColors.SF_RAIL_BLUE, CoachColors.SF_ROOF_BLUE,CoachColors.SF_STRIPE_SKY, 84f),
        RakeCoach("A1",    "2A",     CoachColors.SF_RAIL_BLUE, CoachColors.SF_ROOF_BLUE,CoachColors.SF_STRIPE_SKY, 84f),
        RakeCoach("GEN",   "UR",     CoachColors.GEN_BODY,     CoachColors.GEN_ROOF,    CoachColors.GEN_STRIPE, 80f)
    )

    // Namo Bharat RRTS
    private fun namoBharatRake(): List<RakeCoach> = listOf(
        RakeCoach("DTC",  "CAB", CoachColors.RRTS_BODY, CoachColors.RRTS_ROOF, CoachColors.RRTS_STRIPE, 94f, true),
        RakeCoach("PREM", "EC",  CoachColors.RRTS_BODY, CoachColors.RRTS_ROOF, 0xFFF59E0B, 84f),
        RakeCoach("C2",   "CC",  CoachColors.RRTS_BODY, CoachColors.RRTS_ROOF, CoachColors.RRTS_STRIPE, 84f),
        RakeCoach("C3",   "CC",  CoachColors.RRTS_BODY, CoachColors.RRTS_ROOF, CoachColors.RRTS_STRIPE, 84f),
        RakeCoach("DTC2", "CAB", CoachColors.RRTS_BODY, CoachColors.RRTS_ROOF, CoachColors.RRTS_STRIPE, 94f, true)
    )

    // MEMU Commuter
    private fun memuRake(): List<RakeCoach> = listOf(
        RakeCoach("DMC",  "CAB", CoachColors.MEMU_BODY, CoachColors.MEMU_ROOF, null, 90f, true),
        RakeCoach("TC1",  "GEN", CoachColors.MEMU_BODY, CoachColors.MEMU_ROOF, null, 84f),
        RakeCoach("TC2",  "GEN", CoachColors.MEMU_BODY, CoachColors.MEMU_ROOF, null, 84f),
        RakeCoach("TC3",  "GEN", CoachColors.MEMU_BODY, CoachColors.MEMU_ROOF, null, 84f),
        RakeCoach("DMC2", "CAB", CoachColors.MEMU_BODY, CoachColors.MEMU_ROOF, null, 90f, true)
    )

    // Passenger Ordinary
    private fun passengerRake(): List<RakeCoach> = listOf(
        RakeCoach("WDM-3", "LOCO", CoachColors.LOCO_BODY, CoachColors.LOCO_ROOF, null, 90f, true),
        RakeCoach("GEN1",  "UR",   CoachColors.GEN_BODY,  CoachColors.GEN_ROOF,  CoachColors.GEN_STRIPE, 80f),
        RakeCoach("GEN2",  "UR",   CoachColors.GEN_BODY,  CoachColors.GEN_ROOF,  CoachColors.GEN_STRIPE, 80f),
        RakeCoach("S1",    "SL",   CoachColors.SL_BODY,   CoachColors.SL_ROOF,   CoachColors.SL_STRIPE, 84f),
        RakeCoach("S2",    "SL",   CoachColors.SL_BODY,   CoachColors.SL_ROOF,   CoachColors.SL_STRIPE, 84f),
        RakeCoach("GEN3",  "UR",   CoachColors.GEN_BODY,  CoachColors.GEN_ROOF,  CoachColors.GEN_STRIPE, 80f)
    )

    // Antyodaya Express (All Unreserved General Superfast)
    private fun antyodayaRake(): List<RakeCoach> = listOf(
        RakeCoach("WAP-7", "LOCO", CoachColors.LOCO_BODY, CoachColors.LOCO_ROOF, null, 94f, true),
        RakeCoach("D1",    "2S",   0xFF7C3AED, 0xFF6D28D9, 0xFFA78BFA, 84f),
        RakeCoach("D2",    "2S",   0xFF7C3AED, 0xFF6D28D9, 0xFFA78BFA, 84f),
        RakeCoach("D3",    "2S",   0xFF7C3AED, 0xFF6D28D9, 0xFFA78BFA, 84f),
        RakeCoach("D4",    "2S",   0xFF7C3AED, 0xFF6D28D9, 0xFFA78BFA, 84f),
        RakeCoach("EOG",   "EOG",  0xFF7C3AED, 0xFF6D28D9, null, 78f)
    )

    // UDAY Double Decker
    private fun udayRake(): List<RakeCoach> = listOf(
        RakeCoach("WAP-7", "LOCO", CoachColors.LOCO_BODY, CoachColors.LOCO_ROOF, null, 94f, true),
        RakeCoach("UEC1",  "EC",   0xFF1E3A6E, CoachColors.SF_ROOF_BLUE, 0xFF60A5FA, 86f),
        RakeCoach("UCC1",  "CC",   0xFF1E3A6E, CoachColors.SF_ROOF_BLUE, 0xFF60A5FA, 86f),
        RakeCoach("UCC2",  "CC",   0xFF1E3A6E, CoachColors.SF_ROOF_BLUE, 0xFF60A5FA, 86f),
        RakeCoach("UCC3",  "CC",   0xFF1E3A6E, CoachColors.SF_ROOF_BLUE, 0xFF60A5FA, 86f),
        RakeCoach("EOG",   "EOG",  0xFF1E3A6E, CoachColors.SF_ROOF_BLUE, null, 78f)
    )

    // Suvidha Express
    private fun suvidhaRake(): List<RakeCoach> = listOf(
        RakeCoach("WAP-7", "LOCO", CoachColors.LOCO_BODY, CoachColors.LOCO_ROOF, null, 94f, true),
        RakeCoach("A1",    "2A",   0xFF0C4A6E, CoachColors.SF_ROOF_BLUE, 0xFF38BDF8, 84f),
        RakeCoach("B1",    "3A",   0xFF0C4A6E, CoachColors.SF_ROOF_BLUE, 0xFF38BDF8, 84f),
        RakeCoach("B2",    "3A",   0xFF0C4A6E, CoachColors.SF_ROOF_BLUE, 0xFF38BDF8, 84f),
        RakeCoach("S1",    "SL",   CoachColors.SL_BODY, CoachColors.SL_ROOF, 0xFF38BDF8, 84f),
        RakeCoach("S2",    "SL",   CoachColors.SL_BODY, CoachColors.SL_ROOF, 0xFF38BDF8, 84f),
        RakeCoach("EOG",   "EOG",  0xFF0C4A6E, CoachColors.SF_ROOF_BLUE, null, 78f)
    )

    // Sampark Kranti
    private fun samparkKrantiRake(): List<RakeCoach> = listOf(
        RakeCoach("WAP-4", "LOCO", CoachColors.LOCO_BODY, CoachColors.LOCO_ROOF, null, 94f, true),
        RakeCoach("A1",    "2A",   0xFF0F4C81, CoachColors.SF_ROOF_BLUE, 0xFFFF6B35, 84f),
        RakeCoach("B1",    "3A",   0xFF0F4C81, CoachColors.SF_ROOF_BLUE, 0xFFFF6B35, 84f),
        RakeCoach("B2",    "3A",   0xFF0F4C81, CoachColors.SF_ROOF_BLUE, 0xFFFF6B35, 84f),
        RakeCoach("S1",    "SL",   CoachColors.SL_BODY, CoachColors.SL_ROOF, 0xFFFF6B35, 84f),
        RakeCoach("S2",    "SL",   CoachColors.SL_BODY, CoachColors.SL_ROOF, 0xFFFF6B35, 84f),
        RakeCoach("GEN",   "UR",   CoachColors.GEN_BODY, CoachColors.GEN_ROOF, CoachColors.GEN_STRIPE, 80f),
        RakeCoach("EOG",   "EOG",  0xFF0F4C81, CoachColors.SF_ROOF_BLUE, null, 78f)
    )

    // ─── Class Helper Methods ─────────────────────────────────────────────────

    fun classDisplayName(classType: String): String = when (classType) {
        "1A"        -> "AC First Class (1A)"
        "2A"        -> "AC 2-Tier (2A)"
        "3A"        -> "AC 3-Tier (3A)"
        "3E"        -> "AC 3-Tier Economy (3E)"
        "EC"        -> "Executive Chair Car (EC)"
        "CC"        -> "AC Chair Car (CC)"
        "SL"        -> "Sleeper Class (SL)"
        "2S"        -> "Second Seating (2S)"
        "GEN", "UR" -> "Unreserved General (UR)"
        "LOCO"      -> "Electric Locomotive"
        "CAB"       -> "Driving Cab Trailer"
        "EOG"       -> "End on Generation Car"
        "PANTRY"    -> "AC Pantry Car (PC)"
        else        -> classType
    }

    /**
     * Distinctive pill background color for the class badge on coaches
     */
    fun classBadgeColor(classType: String): Long = when (classType) {
        "1A"        -> 0xFFB45309 // Gold / Amber
        "2A"        -> 0xFF0284C7 // Sky Blue
        "3A", "3E"  -> 0xFF059669 // Emerald Green
        "EC"        -> 0xFF7C3AED // Royal Purple
        "CC"        -> 0xFF2563EB // Cobalt Blue
        "SL"        -> 0xFF0284C7 // Cyan Blue
        "GEN", "UR" -> 0xFFD97706 // Warning Amber
        "2S"        -> 0xFF6D28D9 // Deep Violet
        "PANTRY"    -> 0xFFDC2626 // Red
        "LOCO", "CAB" -> 0xFFEF4444 // Bright Red
        "EOG"       -> 0xFF475569 // Slate
        else        -> 0xFF334155
    }

    fun classShortTag(classType: String): String = when (classType) {
        "GEN", "UR" -> "GEN"
        "PANTRY"    -> "PC"
        else        -> classType
    }
}
