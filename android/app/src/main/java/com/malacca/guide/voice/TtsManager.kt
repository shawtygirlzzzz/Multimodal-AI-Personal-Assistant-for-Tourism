package com.malacca.guide.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private var isReady = false
    private var pendingText: String? = null
    private var pendingLang: String? = null

    init {
        tts = TextToSpeech(context) { status ->
            isReady = status == TextToSpeech.SUCCESS
            if (isReady) {
                val t = pendingText
                val l = pendingLang
                if (t != null) {
                    speakInternal(t, l ?: "EN")
                    pendingText = null
                    pendingLang = null
                }
            }
        }
    }

    fun speak(text: String, languageCode: String) {
        if (isReady) {
            speakInternal(text, languageCode)
        } else {
            pendingText = text
            pendingLang = languageCode
        }
    }

    private fun speakInternal(text: String, languageCode: String) {
        val locale = when (languageCode) {
            "ZH" -> Locale.CHINESE
            "MS" -> Locale("ms", "MY")
            else -> Locale.ENGLISH
        }
        tts?.language = locale
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "heycyan_tts")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
