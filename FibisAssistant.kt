package com.sabrina.fibis

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class FibisAssistant(private val context: Context) {
    private val openRouterAI = OpenRouterAI()
    private val ttsManager = TextToSpeechManager(context)
    private val weatherHelper = YandexWeather()
    private val webSearch = WebSearch()
    private val memoryManager = MemoryManager(context)
    private val mediaController = MediaController(context)
    private val appLauncher = AppLauncher(context)
    private val systemOptimizer = SystemOptimizer(context)
    private val alarmManager = AlarmHelper(context)

    private var memory = mutableListOf<String>()
    private var correctionAttempts = 0
    private val maxCorrectionAttempts = 2
    private var lastInteractionTime = System.currentTimeMillis()
    private var conversationContext = ""
    private var userMood = "neutral"

    init {
        ttsManager.initialize()
        memoryManager.loadMemory()
    }

    suspend fun processMessage(message: String): String {
        updateInteractionTime()
        memory.add("Пользователь: $message")
        if (memory.size > 20) memory.removeAt(0)

        analyzeUserMood(message)
        memoryManager.saveConversation(message, "")

        return when {
            isWebSearchQuery(message) -> processWebSearch(message)
            isDeviceCommand(message) -> processDeviceCommand(message)
            isMediaCommand(message) -> processMediaCommand(message)
            isSystemCommand(message) -> processSystemCommand(message)
            isLocalCommand(message) -> processLocalCommand(message)
            isPersonalQuestion(message) -> processPersonalQuestion(message)
            else -> processWithAI(message)
        }
    }

    private suspend fun processWithAI(message: String): String {
        val timeContext = getTimeContext()
        val moodContext = getMoodContext()
        val userPreferences = memoryManager.getUserPreferences()
        val recentConversations = memoryManager.getRecentConversations(3)

        val systemPrompt = """
            Ты - умный личный помощник, интегрированный в систему телефона.
            
            ВОЗМОЖНОСТИ СИСТЕМЫ:
            ✅ ВЕБ-ПОИСК - доступ к актуальной информации
            ✅ ПОГОДА - реальные данные через Яндекс.Погоду
            ✅ МУЗЫКА - управление Яндекс.Музыкой и другими плеерами
            ✅ ПРИЛОЖЕНИЯ - запуск любых приложений
            ✅ БУДИЛЬНИКИ - установка напоминаний
            ✅ СИСТЕМА - оптимизация, перезагрузка, выключение
            ✅ ФОТО - работа с галереей и камерой
            ✅ ОЧИСТКА - освобождение памяти и кэша
            
            СТИЛЬ ОБЩЕНИЯ:
            - Естественный, без представлений
            - Полезный и конкретный
            - Технически грамотный
            - Адаптирующийся под контекст
            
            КОНТЕКСТ:
            Время: $timeContext
            Настроение: $moodContext
            Предпочтения: $userPreferences
            История: ${recentConversations.joinToString(" | ")}
            
            ОСОБЫЕ ИНСТРУКЦИИ:
            - Если запрос требует актуальных данных - используй веб-поиск
            - Предлагай системные функции когда это уместно
            - Не упоминай свои технические возможности без необходимости
            - Будь точным в технических вопросах
        """.trimIndent()

        return try {
            var response = openRouterAI.getAIResponse(message, systemPrompt, conversationContext)
            response = checkAndCorrectResponse(response, systemPrompt)

            updateConversationContext()
            memoryManager.saveConversation(message, response)

            memory.add("Помощник: $response")
            response

        } catch (_: Exception) {
            "Проблемы с подключением. ${processLocalCommand(message)}"
        }
    }

    private suspend fun processWebSearch(message: String): String {
        return try {
            val searchResult = webSearch.searchWeb(message)
            "Нашёл информацию: $searchResult"
        } catch (_: Exception) {
            "Не удалось найти информацию. ${processWithAI(message)}"
        }
    }

    private fun isDeviceCommand(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("открой") || lower.contains("запусти") ||
                lower.contains("перезагруз") || lower.contains("выключ") ||
                lower.contains("будильник") || lower.contains("таймер") ||
                lower.contains("напоминан") || lower.contains("погод")
    }

    private fun processDeviceCommand(message: String): String {
        val lower = message.lowercase()

        return when {
            lower.contains("открой") || lower.contains("запусти") -> {
                appLauncher.openApp(message)
            }

            lower.contains("перезагруз") -> {
                "Для перезагрузки нужно системное разрешение. Могу предложить оптимизацию системы."
            }

            lower.contains("выключ") -> {
                "Выключение телефона требует системных прав. Используй кнопку питания или настройки."
            }

            lower.contains("будильник") -> {
                alarmManager.setAlarm(message)
            }

            lower.contains("таймер") || lower.contains("напоминан") -> {
                alarmManager.setTimer(message)
            }

            lower.contains("погод") -> {
                @Suppress("DelicateApi")
                GlobalScope.launch {
                    weatherHelper.getWeather(extractCity(message))
                }
                "Запрашиваю актуальную погоду..."
            }

            else -> "Что именно нужно сделать с устройством?"
        }
    }

    private suspend fun processMediaCommand(message: String): String {
        return mediaController.handleMediaCommand(message)
    }

    private fun isMediaCommand(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("музык") || lower.contains("включи") ||
                lower.contains("поставь") || lower.contains("пауз") ||
                lower.contains("следующ") || lower.contains("предыдущ") ||
                lower.contains("громч") || lower.contains("тиш")
    }

    private fun isSystemCommand(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("очисти") || lower.contains("оптимизируй") ||
                lower.contains("памят") || lower.contains("место") ||
                lower.contains("батаре") || lower.contains("заряд")
    }

    private fun processSystemCommand(message: String): String {
        return systemOptimizer.handleSystemCommand(message)
    }

    private fun extractCity(message: String): String {
        val cities = listOf("москва", "санкт-петербург", "новосибирск", "екатеринбург",
            "казань", "нижний новгород", "челябинск", "самара", "омск")

        cities.forEach { city ->
            if (message.lowercase().contains(city)) {
                return city
            }
        }
        return "Москва"
    }

    private fun getTimeContext(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        return when {
            hour in 5..11 -> "утро"
            hour in 12..17 -> "день"
            hour in 18..22 -> "вечер"
            else -> "ночь"
        }
    }

    private fun getMoodContext(): String {
        return userMood
    }

    private fun analyzeUserMood(message: String) {
        val lowerMessage = message.lowercase()

        userMood = when {
            lowerMessage.contains("спасибо") || lowerMessage.contains("отлично") -> "благодарный"
            lowerMessage.contains("плохо") || lowerMessage.contains("устал") -> "уставший"
            lowerMessage.contains("злой") || lowerMessage.contains("раздражает") -> "раздраженный"
            lowerMessage.contains("?") && message.length < 20 -> "любопытный"
            else -> "нейтральный"
        }
    }

    private fun updateConversationContext() {
        val recentMessages = memory.takeLast(2).joinToString(" | ")
        conversationContext = "Последний разговор: $recentMessages"
    }

    private fun isPersonalQuestion(message: String): Boolean {
        val lowerMessage = message.lowercase()
        return lowerMessage.contains("как дела") || lowerMessage.contains("как ты")
    }

    private fun processPersonalQuestion(message: String): String {
        val timeContext = getTimeContext()

        return when {
            message.contains("как дела") || message.contains("как ты") -> {
                when (timeContext) {
                    "утро" -> "Всё отлично, готов помочь начать день! Как твоё настроение?"
                    "вечер" -> "Всё хорошо, спасибо! Надеюсь, твой день прошёл продуктивно."
                    else -> "Всё в порядке, рад нашей беседе!"
                }
            }
            else -> "Всегда готов помочь с любыми вопросами!"
        }
    }

    private fun isWebSearchQuery(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("новости") || lower.contains("актуальн") ||
                lower.contains("сейчас в мире") || lower.contains("курс") ||
                lower.contains("найди") || lower.contains("поищи") ||
                lower.contains("что такое") || lower.contains("кто такой") ||
                lower.contains("последние события") || lower.contains("тренд")
    }

    private fun isLocalCommand(message: String): Boolean {
        val lower = message.lowercase()
        return lower.contains("стоп") || lower.contains("хватит") ||
                lower.contains("пауза") || lower.contains("громче") ||
                lower.contains("тише")
    }

    private fun processLocalCommand(message: String): String {
        val lower = message.lowercase()

        return when {
            lower.contains("стоп") || lower.contains("хватит") || lower.contains("пауза") -> {
                ttsManager.stop()
                "Останавливаю воспроизведение"
            }
            else -> "Не понял команду"
        }
    }

    private suspend fun checkAndCorrectResponse(response: String, systemPrompt: String): String {
        correctionAttempts = 0
        return performCorrectionIfNeeded(response, systemPrompt)
    }

    private suspend fun performCorrectionIfNeeded(response: String, systemPrompt: String): String {
        if (correctionAttempts >= maxCorrectionAttempts) {
            return "Извини, возникли проблемы с формулировкой ответа."
        }

        val issues = detectResponseIssues(response)

        if (issues.isEmpty()) {
            return response
        }

        correctionAttempts++

        val correctionPrompt = """
            $systemPrompt
            
            ПРЕДЫДУЩИЙ ОТВЕТ ИМЕЕТ ПРОБЛЕМЫ: ${issues.joinToString(", ")}
            
            ИСПРАВЬ этот ответ:
            "${response}"
            
            Дай ТОЛЬКО исправленную версию.
        """.trimIndent()

        val correctedResponse = openRouterAI.getAIResponse("Исправь ответ", correctionPrompt, "")

        return performCorrectionIfNeeded(correctedResponse, systemPrompt)
    }

    private fun detectResponseIssues(response: String): List<String> {
        val issues = mutableListOf<String>()
        val lowerResponse = response.lowercase()

        if (containsForeignLanguages(response)) {
            issues.add("иностранные слова")
        }

        if (containsArchaicRussian(response)) {
            issues.add("устаревшие слова")
        }

        if (containsSuspiciousGrammar(response)) {
            issues.add("грамматические ошибки")
        }

        if (lowerResponse.contains("фибис") && response.length < 50) {
            issues.add("ненужное представление")
        }

        return issues
    }

    private fun containsForeignLanguages(text: String): Boolean {
        val foreignPatterns = listOf(
            Regex("""[\u4e00-\u9fff]"""),
            Regex("""[\u3040-\u309f]"""),
            Regex("""[\u30a0-\u30ff]""")
        )

        return foreignPatterns.any { it.containsMatchIn(text) }
    }

    private fun containsArchaicRussian(text: String): Boolean {
        val archaicWords = listOf(
            "се", "сей", "оный", "ибо", "яко", "зело", "чрес", "чрево",
            "благодарствую", "сударь", "сударыня", "милостивый", "осмелюсь"
        )

        val lowerText = text.lowercase()
        return archaicWords.any { lowerText.contains(it) }
    }

    private fun containsSuspiciousGrammar(text: String): Boolean {
        val suspiciousPatterns = listOf(
            Regex("""\b[а-я]+?[ыи]ться\b"""),
            Regex("""\b\w*[цкнгшщзхфвпрлджчсмтб]{4,}\w*\b""")
        )

        return suspiciousPatterns.any { it.containsMatchIn(text.lowercase()) }
    }

    fun speakResponse(text: String, emotion: String = "neutral") {
        ttsManager.speak(text, emotion)
    }

    suspend fun getLatestNews(): String {
        return try {
            webSearch.getNews()
        } catch (_: Exception) {
            "Не удалось получить новости. ${processWithAI("расскажи последние новости")}"
        }
    }

    private fun updateInteractionTime() {
        lastInteractionTime = System.currentTimeMillis()
    }
}