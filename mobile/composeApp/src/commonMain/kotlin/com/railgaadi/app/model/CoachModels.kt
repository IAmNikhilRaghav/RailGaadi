package com.railgaadi.app.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/**
 * Standard Indian Railways Coach Classes
 */
enum class CoachClass(
    val code: String,
    val displayName: String,
    val shortName: String,
    val defaultSeats: Int
) {
    LOCOMOTIVE("ENG", "Locomotive (Engine)", "Engine", 0),
    FIRST_AC("1A", "AC First Class", "1A", 24),
    SECOND_AC("2A", "AC 2 Tier", "2A", 54),
    THIRD_AC("3A", "AC 3 Tier", "3A", 72),
    THIRD_AC_ECONOMY("3E", "AC 3 Economy", "3E", 83),
    AC_CHAIR_CAR("CC", "AC Chair Car", "CC", 78),
    EXECUTIVE_CHAIR_CAR("EC", "Executive Chair Car", "EC", 52),
    SLEEPER("SL", "Sleeper Class", "SL", 80),
    SECOND_SITTING("2S", "Second Sitting", "2S", 108),
    GENERAL_UNRESERVED("GS", "General Unreserved", "UR", 90),
    PANTRY_CAR("PC", "Pantry / Hot Buffet Car", "Pantry", 0),
    LUGGAGE_BRAKE_VAN("SLR", "Seating cum Luggage Rake", "SLR", 30),
    GENERATOR_CAR("EOG", "End on Generation Car", "EOG", 0),
    MOTOR_COACH("MC", "Motor Coach (EMU)", "MC", 76),
    LADIES_COACH("L", "Ladies Compartment", "Ladies", 76),
    FIRST_CLASS_EMU("FC", "First Class (EMU)", "First", 50)
}

/**
 * Detailed Information for an Individual Coach in the Train Rake
 */
data class CoachPosition(
    val sequenceNumber: Int,
    val coachCode: String,
    val coachClass: CoachClass,
    val displayName: String,
    val totalSeats: Int,
    val isAirConditioned: Boolean,
    val primaryColor: Color,
    val secondaryColor: Color,
    val hasToilets: Boolean = true,
    val hasChargingPoints: Boolean = true,
    val isLocomotive: Boolean = false
)

/**
 * Individual Seat or Berth Details
 */
data class SeatBerth(
    val seatNumber: Int,
    val berthType: String, // "Lower", "Middle", "Upper", "Side Lower", "Side Upper", "Window", "Aisle", "Middle Seat"
    val isWindow: Boolean,
    val cabinNumber: Int
)

/**
 * Full Rake Composition for a Train
 */
data class TrainRakeComposition(
    val trainNumber: String,
    val trainName: String,
    val trainType: String,
    val totalCoaches: Int,
    val engineType: String, // e.g. "WAP-7 Electric (6000 HP)", "Vande Bharat DTC Aerodynamic"
    val direction: String = "Engine at Front",
    val coaches: List<CoachPosition>
)

