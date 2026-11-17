package com.railgaadi.app.util

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Multiplatform decimal formatter that works seamlessly across Kotlin/JVM (Android)
 * and Kotlin/Native (iOS) without relying on java.lang.String.format.
 */
fun Double.formatDecimals(decimals: Int): String {
    if (this.isNaN() || this.isInfinite()) return this.toString()
    if (decimals <= 0) return this.roundToLong().toString()

    var factor = 1.0
    repeat(decimals) { factor *= 10.0 }

    val sign = if (this < 0) "-" else ""
    val positiveVal = abs(this)
    val roundedScaled = kotlin.math.round(positiveVal * factor).toLong()

    val divisor = factor.toLong()
    val intPart = roundedScaled / divisor
    val fracPart = roundedScaled % divisor

    var fracStr = fracPart.toString()
    while (fracStr.length < decimals) {
        fracStr = "0$fracStr"
    }

    return "$sign$intPart.$fracStr"
}

fun Float.formatDecimals(decimals: Int): String = this.toDouble().formatDecimals(decimals)

/**
 * Formats train delay minutes into a human-readable duration string.
 * - Under 60 minutes: e.g. "48 mins", "55 mins"
 * - 60 minutes and above: counted in hours and remaining minutes, e.g.
 *   - 60 mins -> "1hr"
 *   - 84 mins -> "1hrs 24mins"
 *   - 120 mins -> "2hrs"
 *   - 145 mins -> "2hrs 25mins"
 */
fun formatDelayDuration(minutes: Int): String {
    val absM = abs(minutes)
    if (absM < 60) {
        return "$absM mins"
    }
    val hours = absM / 60
    val remMins = absM % 60
    val hrStr = if (hours == 1) "1hr" else "${hours}hrs"
    return if (remMins == 0) {
        hrStr
    } else {
        "${hours}hrs ${remMins}mins"
    }
}


