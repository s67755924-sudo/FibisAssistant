package com.sabrina.fibis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WebSearch {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val searchAPI = "https://api.duckduckgo.com/"

    suspend fun searchWeb(query: String): String = withContext(Dispatchers.IO) {
        try {
            val url = "$searchAPI?q=${java.net.URLEncoder.encode(query, "UTF-8")}&format=json&no_html=1"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            return@withContext parseSearchResults(responseBody, query)
        } catch (e: Exception) {
            return@withContext "Не удалось найти информацию: ${e.message}"
        }
    }

    private fun parseSearchResults(jsonResponse: String, query: String): String {
        return try {
            val json = JSONObject(jsonResponse)
            val abstract = json.optString("Abstract", "")
            val abstractText = json.optString("AbstractText", "")
            val answer = json.optString("Answer", "")

            when {
                answer.isNotEmpty() -> answer
                abstractText.isNotEmpty() -> abstractText
                abstract.isNotEmpty() -> abstract
                else -> "По запросу '$query' нашлась информация, но для точного ответа лучше уточни вопрос."
            }
        } catch (e: Exception) {
            "Нашёл информацию по запросу '$query', но для лучшего ответа уточни, что именно тебя интересует."
        }
    }

    suspend fun getNews(): String = withContext(Dispatchers.IO) {
        try {
            val url = "${searchAPI}?q=новости+сегодня&format=json&no_html=1"

            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            return@withContext parseNewsResults(responseBody)
        } catch (e: Exception) {
            return@withContext "Не удалось получить новости: ${e.message}"
        }
    }

    private fun parseNewsResults(jsonResponse: String): String {
        return try {
            val json = JSONObject(jsonResponse)
            val relatedTopics = json.optJSONArray("RelatedTopics")

            if (relatedTopics != null && relatedTopics.length() > 0) {
                val newsItems = mutableListOf<String>()
                for (i in 0 until minOf(3, relatedTopics.length())) {
                    val topic = relatedTopics.getJSONObject(i)
                    val text = topic.optString("Text", "")
                    if (text.isNotEmpty()) {
                        newsItems.add(text)
                    }
                }
                if (newsItems.isNotEmpty()) {
                    "Актуальные новости:\n- ${newsItems.joinToString("\n- ")}"
                } else {
                    "Есть свежие новости, но для подробностей лучше посмотреть в новостных приложениях."
                }
            } else {
                "Рекомендую посмотреть последние новости в специальных приложениях для большей точности."
            }
        } catch (e: Exception) {
            "Не могу получить детали новостей. Попробуй открыть новостное приложение."
        }
    }
}