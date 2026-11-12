package com.railgaadi.app.util

expect object PlatformDate {
    /** Returns Triple(dayOfMonth, monthNumber 1..12, year) for today in local device time */
    fun getToday(): Triple<Int, Int, Int>
    /** Returns Pair(hour 0..23, minute 0..59) for current time */
    fun getCurrentTime(): Pair<Int, Int>
}

