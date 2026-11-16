package com.railgaadi.app.util

import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

actual object PlatformDate {
    actual fun getToday(): Triple<Int, Int, Int> {
        val calendar = NSCalendar.currentCalendar
        val date = NSDate()
        val components = calendar.components(
            NSCalendarUnitDay or NSCalendarUnitMonth or NSCalendarUnitYear,
            fromDate = date
        )
        return Triple(
            components.day.toInt(),
            components.month.toInt(),
            components.year.toInt()
        )
    }

    actual fun getCurrentTime(): Pair<Int, Int> {
        val calendar = NSCalendar.currentCalendar
        val date = NSDate()
        val components = calendar.components(
            NSCalendarUnitHour or NSCalendarUnitMinute,
            fromDate = date
        )
        return Pair(components.hour.toInt(), components.minute.toInt())
    }

    actual fun nowMillis(): Long {
        return (NSDate().timeIntervalSince1970 * 1000).toLong()
    }
}

