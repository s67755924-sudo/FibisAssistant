package com.sabrina.fibis

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.view.setPadding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PopupActivity : Activity() {
    private lateinit var fibisAssistant: FibisAssistant
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var voiceButton: ImageButton
    private lateinit var closeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Для Android 10+ используем bubble-режим
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setupBubbleWindow()
        } else {
            setupPopupWindow()
        }

        setupUI()
        setupFibis()
    }

    private fun setupPopupWindow() {
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, 600)
        window.setGravity(Gravity.BOTTOM)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes.dimAmount = 0.3f
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
    }

    @android.annotation.TargetApi(Build.VERSION_CODES.Q)
    private fun setupBubbleWindow() {
        // Настройки для bubble-окна
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, 600)
        window.setGravity(Gravity.BOTTOM)
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Делаем окно поверх всего
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setTurnScreenOn(true)
            setShowWhenLocked(true)
        }
    }

    private fun setupUI() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xCC1E1E1E.toInt())
            setPadding(16)
        }

        // Верхняя панель с кнопкой закрытия
        val topPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 16)
        }

        closeButton = Button(this).apply {
            text = "✕"
            setBackgroundColor(0x44FFFFFF.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(50, 50)
        }

        val title = android.widget.TextView(this).apply {
            text = "Фибис"
            setTextColor(0xFF00FF00.toInt())
            textSize = 20f
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                50
            )
        }

        topPanel.addView(closeButton)
        topPanel.addView(title)

        // Поле ввода сообщения
        messageInput = EditText(this).apply {
            hint = "Спроси Фибиса..."
            setTextColor(0xFFFFFFFF.toInt())
            setHintTextColor(0x88FFFFFF.toInt())
            background = android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 25f
                setColor(0x44FFFFFF.toInt())
            }
            setPadding(16, 16, 16, 16)
        }

        // Панель с кнопками
        val buttonPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }

        sendButton = Button(this).apply {
            text = "➤"
            setBackgroundColor(0xFF00FF00.toInt())
            setTextColor(0xFF000000.toInt())
            layoutParams = LinearLayout.LayoutParams(60, 60).apply {
                marginEnd = 8
            }
        }

        voiceButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_btn_speak_now)
            setBackgroundColor(0xFF00FF00.toInt())
            layoutParams = LinearLayout.LayoutParams(60, 60)
        }

        buttonPanel.addView(sendButton)
        buttonPanel.addView(voiceButton)

        layout.addView(topPanel)
        layout.addView(messageInput)
        layout.addView(buttonPanel)

        setContentView(layout)

        setupClickListeners()
    }

    private fun setupFibis() {
        fibisAssistant = FibisAssistant(this)
    }

    private fun setupClickListeners() {
        sendButton.setOnClickListener {
            val message = messageInput.text.toString()
            if (message.isNotEmpty()) {
                processMessage(message)
                messageInput.text.clear()
            }
        }

        voiceButton.setOnClickListener {
            startVoiceInput()
        }

        closeButton.setOnClickListener {
            finish()
        }

        messageInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendButton.performClick()
                true
            } else false
        }

        // Закрытие при клике вне окна
        window.decorView.setOnTouchListener { _, event ->
            if (event.action == android.view.MotionEvent.ACTION_OUTSIDE) {
                finish()
                true
            } else {
                false
            }
        }
    }

    private fun processMessage(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val response = fibisAssistant.processMessage(message)
            fibisAssistant.speakResponse(response)
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...")
        }
        startActivityForResult(intent, 111)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 111 && resultCode == RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            results?.get(0)?.let { spokenText ->
                messageInput.setText(spokenText)
                processMessage(spokenText)
            }
        }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun finish() {
        super.finish()
        // Анимация закрытия
        overridePendingTransition(0, R.anim.slide_out_bottom)
    }
}