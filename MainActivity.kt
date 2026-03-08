package com.sabrina.fibis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import com.google.android.material.snackbar.Snackbar
import androidx.core.view.isVisible
import com.airbnb.lottie.LottieAnimationView

class MainActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: androidx.recyclerview.widget.RecyclerView
    private lateinit var messageInput: com.google.android.material.textfield.TextInputEditText
    private lateinit var sendButton: com.google.android.material.textfield.TextInputLayout
    private lateinit var voiceButton: android.widget.ImageView
    private lateinit var attachButton: android.widget.ImageView
    private lateinit var statusText: android.widget.TextView
    private lateinit var voiceAnimation: LottieAnimationView
    private lateinit var toolbar: com.google.android.material.appbar.MaterialToolbar

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var fibisAssistant: FibisAssistant
    private lateinit var speechRecognizer: SpeechRecognizer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Устанавливаем прозрачный статус бар
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = ContextCompat.getColor(this, R.color.background)

        initViews()
        setupToolbar()
        setupFibis()
        setupRecyclerView()
        setupClickListeners()

        // Показываем приветствие
        showWelcomeMessage()
    }

    private fun initViews() {
        chatRecyclerView = findViewById(R.id.chat_recycler_view)
        messageInput = findViewById(R.id.message_input)
        sendButton = findViewById(R.id.input_layout)
        voiceButton = findViewById(R.id.voice_button)
        attachButton = findViewById(R.id.attach_button)
        statusText = findViewById(R.id.status_text)
        voiceAnimation = findViewById(R.id.voice_animation)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupToolbar() {
        toolbar.setNavigationOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_clear -> {
                    clearChat()
                    true
                }
                R.id.menu_info -> {
                    showAppInfo()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFibis() {
        fibisAssistant = FibisAssistant(this)
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter()
        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = chatAdapter
        chatRecyclerView.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                // Можно добавить скрытие/показ элементов при скролле
            }
        })
    }

    private fun setupClickListeners() {
        // Отправка сообщения по кнопке отправки
        sendButton.setEndIconOnClickListener {
            sendMessage()
        }

        // Отправка сообщения по Enter
        messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendMessage()
                true
            } else {
                false
            }
        }

        // Голосовой ввод
        voiceButton.setOnClickListener {
            startVoiceRecognition()
        }

        // Прикрепление файлов
        attachButton.setOnClickListener {
            showAttachmentOptions()
        }
    }

    private fun sendMessage() {
        val message = messageInput.text.toString().trim()
        if (message.isNotEmpty()) {
            processUserMessage(message)
            messageInput.text?.clear()
            hideKeyboard()
        }
    }

    private fun processUserMessage(message: String) {
        addUserMessage(message)

        // Блокируем ввод на время обработки
        messageInput.isEnabled = false
        sendButton.isEnabled = false

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = fibisAssistant.processMessage(message)
                addBotMessage(response)

                // Озвучка ответа
                if (getTTSEnabled()) {
                    fibisAssistant.speakResponse(response)
                }

            } catch (e: Exception) {
                addBotMessage("Извини, произошла ошибка: ${e.message}")
            } finally {
                messageInput.isEnabled = true
                sendButton.isEnabled = true
                messageInput.requestFocus()
            }
        }
    }

    private fun addUserMessage(text: String) {
        chatAdapter.addMessage(ChatMessage(text, true))
        scrollToBottom()
    }

    private fun addBotMessage(text: String) {
        chatAdapter.addMessage(ChatMessage(text, false))
        scrollToBottom()
    }

    private fun scrollToBottom() {
        if (chatAdapter.itemCount > 0) {
            chatRecyclerView.smoothScrollToPosition(chatAdapter.itemCount - 1)
        }
    }

    private fun startVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            setupSpeechRecognizer()
            showVoiceAnimation(true)

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...")
            }

            try {
                speechRecognizer.startListening(intent)
            } catch (e: Exception) {
                showVoiceAnimation(false)
                addBotMessage("Не удалось запустить распознавание речи")
            }
        } else {
            requestAudioPermission()
        }
    }

    private fun setupSpeechRecognizer() {
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                statusText.isVisible = true
                statusText.text = "Слушаю..."
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                statusText.text = "Обработка..."
            }

            override fun onError(error: Int) {
                showVoiceAnimation(false)
                statusText.isVisible = false

                when (error) {
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        addBotMessage("Не услышал речь. Попробуйте еще раз.")
                    }
                    else -> {
                        addBotMessage("Ошибка распознавания. Попробуйте позже.")
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                showVoiceAnimation(false)
                statusText.isVisible = false

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val spokenText = matches[0]
                    processUserMessage(spokenText)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun showVoiceAnimation(show: Boolean) {
        if (show) {
            voiceAnimation.isVisible = true
            voiceAnimation.playAnimation()
        } else {
            voiceAnimation.isVisible = false
            voiceAnimation.pauseAnimation()
        }
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            123
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 123 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startVoiceRecognition()
        } else {
            Snackbar.make(
                findViewById(android.R.id.content),
                "Для голосового ввода нужно разрешение",
                Snackbar.LENGTH_LONG
            ).setAction("Настройки") {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }.show()
        }
    }

    private fun showWelcomeMessage() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when {
            hour in 5..11 -> "Доброе утро! Чем могу помочь?"
            hour in 12..17 -> "Добрый день! Рад вас видеть."
            hour in 18..22 -> "Добрый вечер! Как прошел день?"
            else -> "Доброй ночи! Поздно засиделись."
        }

        addBotMessage(greeting)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(messageInput.windowToken, 0)
    }

    private fun getTTSEnabled(): Boolean {
        val prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this)
        return prefs.getBoolean("tts_enabled", true)
    }
    private fun clearChat() {
        chatAdapter.clearMessages()
        addBotMessage("История очищена. Чем могу помочь?")
    }

    private fun showAppInfo() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("О Фибис")
            .setMessage("Фибис v1.0\nВаш личный AI-помощник")
            .setPositiveButton("ОК", null)
            .create()
        dialog.show()
    }

    private fun showAttachmentOptions() {
        val items = arrayOf("Фото", "Документ", "Аудио", "Местоположение")

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Прикрепить")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> addBotMessage("Функция загрузки фото в разработке")
                    1 -> addBotMessage("Функция загрузки документов в разработке")
                    2 -> addBotMessage("Функция загрузки аудио в разработке")
                    3 -> addBotMessage("Функция отправки местоположения в разработке")
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
    }
}