package com.railgaadi.app.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

actual object PlatformSpeech {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingSpeech: String? = null

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                tts?.language = Locale("en", "IN")
                pendingSpeech?.let {
                    speak(it)
                    pendingSpeech = null
                }
            }
        }
    }

    actual fun speak(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        if (!isInitialized || tts == null) {
            pendingSpeech = trimmed
            return
        }

        tts?.speak(trimmed, TextToSpeech.QUEUE_FLUSH, null, "NavGuidance")
    }

    actual fun stop() {
        tts?.stop()
    }
}

