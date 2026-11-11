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
