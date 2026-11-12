package com.railgaadi.app.util

import java.util.Calendar

actual object PlatformDate {
    actual fun getToday(): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance()
        return Triple(
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.YEAR)
        )
    }
}

