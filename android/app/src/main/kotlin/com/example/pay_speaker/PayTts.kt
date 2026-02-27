package com.example.paynotifier

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

object PayTts {
    private var tts: TextToSpeech? = null
    private val isReady = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
                tts?.defaultVoice?.let { /* keep default */ }
                tts?.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                isReady.set(true)
            }
        }
    }

    fun speak(context: Context, text: String) {
        init(context)
        if (isReady.get()) {
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            tts?.speak(text, TextToSpeech.QUEUE_ADD, params, System.currentTimeMillis().toString())
        } else {
            handler.postDelayed({ speak(context, text) }, 300)
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isReady.set(false)
    }
}