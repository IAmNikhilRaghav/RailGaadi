package com.railgaadi.app.util

expect object PlatformDate {
    /** Returns Triple(dayOfMonth, monthNumber 1..12, year) for today in local device time */
    fun getToday(): Triple<Int, Int, Int>
}

