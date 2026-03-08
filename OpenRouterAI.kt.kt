package com.sabrina.fibis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenRouterAI {
    private val client = OkHttpClient.Builder()
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    // Ваш API ключ
    private val apiKey = "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"

    // Бесплатные модели (по приоритету)
    private val freeModels = listOf(
        "deepresearch/deepresearch-30b-a3b-free",    // Основная модель
        "gryphe/mythomax-l2-13b:free",              // Резервная 1
        "undi95/toppy-m-7b:free",                   // Резервная 2
        "huggingfaceh4/zephyr-7b-beta:free",        // Резервная 3
        "mistralai/mistral-7b-instruct:free"        // Резервная 4
    )

    private var currentModelIndex = 0

    suspend fun getAIResponse(
        userMessage: String,
        systemPrompt: String = "",
        context: String = ""
    ): String = withContext(Dispatchers.IO) {
        var retryCount = 0
        val maxRetries = 2

        while (retryCount < maxRetries) {
            try {
                val currentModel = freeModels[currentModelIndex]
                val result = tryGetAIResponse(userMessage, systemPrompt, context, currentModel)

                // Если ответ успешный
                if (result.isNotEmpty() && !result.contains("Ошибка") && !result.contains("превышен")) {
                    return@withContext result
                }

                // Переходим к следующей модели
                currentModelIndex = (currentModelIndex + 1) % freeModels.size
                retryCount++

            } catch (e: Exception) {
                retryCount++
                currentModelIndex = (currentModelIndex + 1) % freeModels.size

                if (retryCount == maxRetries) {
                    return@withContext "Извини, не могу подключиться к AI. Попробуй позже или используй локальные команды."
                }
            }
        }

        return@withContext "Не удалось получить ответ. Попробуйте другой запрос."
    }

    private suspend fun tryGetAIResponse(
        userMessage: String,
        systemPrompt: String = "",
        context: String = "",
        modelName: String
    ): String = withContext(Dispatchers.IO) {
        try {
            // Ограничиваем длину сообщения для бесплатной модели
            val cleanMessage = if (userMessage.length > 300) {
                userMessage.substring(0, 300) + "..."
            } else {
                userMessage.trim()
            }

            val processedMessage = if (context.isNotEmpty()) {
                "Контекст: $context\nВопрос: $cleanMessage"
            } else {
                cleanMessage
            }

            // Создаем сообщения для AI
            val messages = JSONArray().apply {
                if (systemPrompt.isNotEmpty()) {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                }
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", processedMessage)
                })
            }

            // Создаем тело запроса
            val jsonBody = JSONObject().apply {
                put("model", modelName)
                put("messages", messages)
                put("max_tokens", 500)      // Уменьшили для бесплатной модели
                put("temperature", 0.7)
                put("top_p", 0.9)
            }

            // Создаем HTTP запрос
            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .header("HTTP-Referer", "https://fibis-app.com")
                .header("X-Title", "Fibis Assistant")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            // Выполняем запрос
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorCode = response.code
                return@withContext when (errorCode) {
                    429 -> "Достигнут лимит запросов к бесплатной модели. Подождите немного."
                    402 -> "Бесплатные кредиты исчерпаны. Попробуйте позже."
                    503 -> "Модель временно недоступна."
                    else -> "Ошибка подключения: $errorCode"
                }
            }

            val responseBody = response.body?.string() ?: ""

            if (responseBody.isEmpty()) {
                return@withContext "Пустой ответ от сервера"
            }

            // Парсим ответ
            return@withContext parseAIResponse(responseBody)

        } catch (e: Exception) {
            return@withContext when {
                e.message?.contains("timeout", true) == true ->
                    "Превышено время ожидания. Модель перегружена."
                e.message?.contains("network", true) == true ->
                    "Проблемы с интернетом."
                else -> "Ошибка: ${e.message ?: "неизвестная"}"
            }
        }
    }

    private fun parseAIResponse(jsonResponse: String): String {
        return try {
            val jsonObject = JSONObject(jsonResponse)

            // Проверяем на ошибки
            if (jsonObject.has("error")) {
                val error = jsonObject.getJSONObject("error")
                val errorMsg = error.getString("message")

                return when {
                    errorMsg.contains("quota", true) -> "Бесплатные кредиты закончились."
                    errorMsg.contains("rate limit", true) -> "Слишком много запросов."
                    errorMsg.contains("model", true) -> "Модель недоступна."
                    else -> "Ошибка AI: $errorMsg"
                }
            }

            // Получаем ответ
            val choices = jsonObject.getJSONArray("choices")
            if (choices.length() == 0) {
                return "Нет ответа от AI"
            }

            val firstChoice = choices.getJSONObject(0)
            val message = firstChoice.getJSONObject("message")
            var content = message.getString("content").trim()

            // Очищаем ответ
            content = cleanAIResponse(content)

            if (content.length < 5) {
                return "Слишком короткий ответ. Попробуйте другой запрос."
            }

            content

        } catch (e: Exception) {
            "Не удалось обработать ответ AI: ${e.message}"
        }
    }

    private fun cleanAIResponse(response: String): String {
        var cleaned = response

        // Удаляем технические префиксы
        cleaned = cleaned.replace(Regex("^(Assistant:|AI:|Bot:|\\[.*?\\]:\\s*)"), "")

        // Убираем лишние пробелы
        cleaned = cleaned.replace(Regex("\\s+"), " ").trim()

        // Удаляем кавычки
        if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
            cleaned = cleaned.substring(1, cleaned.length - 1)
        }

        return cleaned
    }

    suspend fun searchWeb(query: String): String = withContext(Dispatchers.IO) {
        try {
            // Для поиска используем более простую модель
            val searchModel = "gryphe/mythomax-l2-13b:free"

            val searchPrompt = """
                Пользователь спрашивает: "$query"
                
                Дай краткий, фактический ответ на русском языке.
                Если не знаешь точно - честно скажи.
                Будь полезным и конкретным.
                Не более 3 предложений.
            """.trimIndent()

            val messages = JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", searchPrompt)
                })
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Ответь на запрос: $query")
                })
            }

            val jsonBody = JSONObject().apply {
                put("model", searchModel)
                put("messages", messages)
                put("max_tokens", 300)
                put("temperature", 0.3)
            }

            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            parseAIResponse(responseBody)

        } catch (e: Exception) {
            "Не удалось найти информацию: ${e.message}"
        }
    }
}