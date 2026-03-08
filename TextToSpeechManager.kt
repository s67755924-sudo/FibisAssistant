package com.sabrina.fibis

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.*

@Suppress("DEPRECATION")
class TextToSpeechManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    // Детальные настройки для каждой эмоции
    private val emotionSettings = mapOf(
        "neutral" to EmotionSettings(speechRate = 1.0f, pitch = 1.0f, volume = 1.0f),
        "happy" to EmotionSettings(speechRate = 1.15f, pitch = 1.25f, volume = 1.1f),
        "sad" to EmotionSettings(speechRate = 0.85f, pitch = 0.85f, volume = 0.9f),
        "excited" to EmotionSettings(speechRate = 1.35f, pitch = 1.35f, volume = 1.15f),
        "angry" to EmotionSettings(speechRate = 0.95f, pitch = 0.9f, volume = 1.2f),
        "surprised" to EmotionSettings(speechRate = 1.1f, pitch = 1.3f, volume = 1.05f),
        "thoughtful" to EmotionSettings(speechRate = 0.75f, pitch = 0.95f, volume = 0.95f),
        "playful" to EmotionSettings(speechRate = 1.2f, pitch = 1.4f, volume = 1.0f)
    )

    data class EmotionSettings(
        val speechRate: Float,
        val pitch: Float,
        val volume: Float
    )

    fun initialize(onInit: (Boolean) -> Unit = {}) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale("ru", "RU"))
                if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                isInitialized = true

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        Log.d("FibisTTS", "Начало речи: $utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.d("FibisTTS", "Конец речи: $utteranceId")
                    }

                    override fun onError(utteranceId: String?) {
                        Log.e("FibisTTS", "Ошибка речи: $utteranceId")
                    }
                })
            } else {
                isInitialized = false
                Log.e("FibisTTS", "Ошибка инициализации TTS")
            }
            onInit(isInitialized)
        }
    }

    fun speak(text: String, emotion: String = "neutral") {
        if (!isInitialized) {
            Log.w("FibisTTS", "TTS не инициализирован")
            return
        }

        val settings = emotionSettings[emotion] ?: emotionSettings["neutral"]!!

        // Настраиваем параметры речи
        tts?.setSpeechRate(settings.speechRate)
        tts?.setPitch(settings.pitch)

        val processedText = addEmotionalEffects(text, emotion)
        Log.d("FibisTTS", "Говорю с эмоцией: $emotion")

        val utteranceId = "fibis_${System.currentTimeMillis()}"
        tts?.speak(processedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun addEmotionalEffects(text: String, emotion: String): String {
        var processed = text

        when (emotion) {
            "excited" -> {
                if (!processed.contains("!") && processed.length < 50) {
                    processed += "!"
                }
            }
            "sad" -> {
                processed = processed.replace(". ", "... ")
            }
            "angry" -> {
                processed = processed.replace(", ", "! ").replace(". ", "! ")
            }
            "thoughtful" -> {
                processed = processed.replace(" ", " ... ")
            }
            "playful" -> {
                if (processed.contains("может")) {
                    processed = processed.replace("может", "мо-о-ожет")
                }
            }
        }

        return processed
    }

    fun detectEmotionFromText(text: String): String {
        val lowerText = text.lowercase()

        return when {
            lowerText.contains("!") &&
                    (lowerText.contains("ура") || lowerText.contains("супер") ||
                            lowerText.contains("отлично") || lowerText.contains("прекрасно")) -> "excited"

            lowerText.contains("хорошо") || lowerText.contains("отлично") ||
                    lowerText.contains("рад") || lowerText.contains("приятно") -> "happy"

            lowerText.contains("...") || lowerText.contains("жаль") ||
                    lowerText.contains("печально") || lowerText.contains("грустно") -> "sad"

            lowerText.contains("?") &&
                    (lowerText.contains("неужели") || lowerText.contains("правда")) -> "surprised"

            lowerText.contains("!") &&
                    (lowerText.contains("сердит") || lowerText.contains("злюсь")) -> "angry"

            lowerText.contains("думаю") || lowerText.contains("возможно") ||
                    lowerText.contains("наверное") -> "thoughtful"

            lowerText.contains("ха-ха") || lowerText.contains("хм") ||
                    lowerText.contains("интересно") -> "playful"

            else -> "neutral"
        }
    }

    @Suppress("unused")
    fun speakWithAutoEmotion(text: String) {
        val emotion = detectEmotionFromText(text)
        speak(text, emotion)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}