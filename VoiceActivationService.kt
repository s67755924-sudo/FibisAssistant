package com.sabrina.fibis

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class VoiceActivationService : Service() {
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startVoiceActivation()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                "fibis_channel",
                "Фибис",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фибис работает в фоновом режиме"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun startVoiceActivation() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                // Перезапускаем через 1 секунду
                android.os.Handler(mainLooper).postDelayed({
                    startListening()
                }, 1000)
            }

            override fun onResults(results: android.os.Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.get(0)?.let { spokenText ->
                    if (spokenText.lowercase().contains("фибис")) {
                        // Открываем всплывающее окно
                        openPopup()
                    }
                }
                // Снова начинаем слушать
                startListening()
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {}
            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        startListening()
    }

    private fun startListening() {
        if (!isListening) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }
            try {
                speechRecognizer.startListening(intent)
                isListening = true
            } catch (_: Exception) {
                // Игнорируем ошибки
            }
        }
    }

    private fun openPopup() {
        try {
            val intent = Intent(this, PopupActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: Exception) {
            // Если не удалось открыть, игнорируем
        }
    }

    private fun stopListening() {
        if (isListening) {
            try {
                speechRecognizer.stopListening()
                isListening = false
            } catch (_: Exception) {
                // Игнорируем ошибки
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        speechRecognizer.destroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Создаем простое уведомление
        val notification = Notification.Builder(this, "fibis_channel")
            .setContentTitle("Фибис")
            .setContentText("Работает в фоновом режиме")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(1, notification)
        return START_STICKY
    }
}