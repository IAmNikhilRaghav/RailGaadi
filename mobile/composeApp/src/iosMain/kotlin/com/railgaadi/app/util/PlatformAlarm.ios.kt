package com.railgaadi.app.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AudioToolbox.AudioServicesPlayAlertSound
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive

@OptIn(ExperimentalForeignApi::class)
actual object PlatformAlarm {
    private var isPlaying = false

    actual fun playAlarm(alarmTone: String) {
        isPlaying = true
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)
            // System Sound 1304 is the iOS alarm chime; 1005 is SMS tone
            AudioServicesPlayAlertSound(1304u)
        } catch (e: Throwable) {
            println("PlatformAlarm iOS error: ${e.message}")
        }
    }

    actual fun stopAlarm() {
        isPlaying = false
    }

    actual fun vibrate(pattern: LongArray) {
        try {
            AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
        } catch (_: Throwable) {}
    }

    actual fun stopVibrate() {
        // iOS single vibrate pulse stops automatically
    }
}

