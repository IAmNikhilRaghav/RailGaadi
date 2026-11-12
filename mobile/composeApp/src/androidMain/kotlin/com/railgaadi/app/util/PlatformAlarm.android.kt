package com.railgaadi.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

actual object PlatformAlarm {
    private var appContext: Context? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    actual fun playAlarm(alarmTone: String) {
        val ctx = appContext ?: return
        stopAlarm()
        try {
            val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(ctx, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Throwable) {
            println("PlatformAlarm Android play error: ${e.message}")
        }
    }

    actual fun stopAlarm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
        } catch (_: Throwable) {}
    }

    actual fun vibrate(pattern: LongArray) {
        try {
            val v = vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = repeat from start
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(pattern, 0)
            }
        } catch (_: Throwable) {}
    }

    actual fun stopVibrate() {
        try {
            vibrator?.cancel()
        } catch (_: Throwable) {}
    }
}
