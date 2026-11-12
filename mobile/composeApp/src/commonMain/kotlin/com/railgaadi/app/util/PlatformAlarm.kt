package com.railgaadi.app.util

expect object PlatformAlarm {
    fun playAlarm(alarmTone: String = "chime")
    fun stopAlarm()
    fun vibrate(pattern: LongArray = longArrayOf(0, 500, 250, 500, 250, 500))
    fun stopVibrate()
}

