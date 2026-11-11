package com.railgaadi.app.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.setActive

@OptIn(ExperimentalForeignApi::class)
actual object PlatformSpeech {
    private val synthesizer by lazy { AVSpeechSynthesizer() }

    actual fun speak(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, null)
            session.setActive(true, null)

            if (synthesizer.isSpeaking()) {
                synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
            }

            val utterance = AVSpeechUtterance(string = trimmed).apply {
                rate = 0.5f // Natural speaking speed
                pitchMultiplier = 1.0f
                volume = 1.0f
                voice = AVSpeechSynthesisVoice.voiceWithLanguage("en-IN")
                    ?: AVSpeechSynthesisVoice.voiceWithLanguage("en-US")
                    ?: AVSpeechSynthesisVoice.voiceWithLanguage("en-GB")
            }

            synthesizer.speakUtterance(utterance)
        } catch (e: Throwable) {
            println("PlatformSpeech iOS error: ${e.message}")
        }
    }

    actual fun stop() {
        try {
            if (synthesizer.isSpeaking()) {
                synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
            }
        } catch (_: Throwable) {}
    }
}
