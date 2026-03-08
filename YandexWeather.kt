package com.sabrina.fibis

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class YandexWeather {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey = "45ce8e6e-1a44-4c12-a999-2b18faff27d0"

    suspend fun getWeather(city: String = "Москва"): String = withContext(Dispatchers.IO) {
        try {
            val coords = getCityCoordinates(city)
            if (coords != null) {
                val (lat, lon) = coords
                return@withContext getWeatherByCoordinates(lat, lon)
            } else {
                return@withContext "Не могу найти город '$city'. Использую общий прогноз."
            }
        } catch (e: Exception) {
            return@withContext "Ошибка получения погоды: ${e.message}"
        }
    }

    private suspend fun getWeatherByCoordinates(lat: Double, lon: Double): String {
        try {
            val url = "https://api.weather.yandex.ru/v2/forecast?lat=$lat&lon=$lon&extra=true"

            val request = Request.Builder()
                .url(url)
                .header("X-Yandex-API-Key", apiKey)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                return parseWeatherData(responseBody)
            } else {
                return "Не удалось получить данные погоды. Код ошибки: ${response.code}"
            }
        } catch (e: Exception) {
            return "Проблема с подключением к сервису погоды: ${e.message}"
        }
    }

    private fun parseWeatherData(jsonData: String): String {
        return try {
            val json = JSONObject(jsonData)
            val fact = json.getJSONObject("fact")
            val temp = fact.getInt("temp")
            val feelsLike = fact.getInt("feels_like")
            val condition = fact.getString("condition")
            val windSpeed = fact.getDouble("wind_speed")
            val humidity = fact.getInt("humidity")

            val conditionText = when (condition) {
                "clear" -> "ясно"
                "partly-cloudy" -> "малооблачно"
                "cloudy" -> "облачно"
                "overcast" -> "пасмурно"
                "drizzle" -> "морось"
                "light-rain" -> "небольшой дождь"
                "rain" -> "дождь"
                "moderate-rain" -> "умеренный дождь"
                "heavy-rain" -> "сильный дождь"
                "continuous-heavy-rain" -> "длительный сильный дождь"
                "showers" -> "ливень"
                "wet-snow" -> "мокрый снег"
                "light-snow" -> "небольшой снег"
                "snow" -> "снег"
                "snow-showers" -> "снегопад"
                "hail" -> "град"
                "thunderstorm" -> "гроза"
                "thunderstorm-with-rain" -> "дождь с грозой"
                "thunderstorm-with-hail" -> "гроза с градом"
                else -> condition
            }

            "Сейчас $temp°C (ощущается как $feelsLike°C), $conditionText. " +
                    "Ветер $windSpeed м/с, влажность $humidity%."

        } catch (e: Exception) {
            "Получил данные о погоде, но возникла ошибка при обработке. ${getSeasonalWeather()}"
        }
    }

    private fun getSeasonalWeather(): String {
        return "Судя по сезону и времени, стоит одеваться по погоде и брать зонт на всякий случай."
    }

    private suspend fun getCityCoordinates(city: String): Pair<Double, Double>? {
        val cities = mapOf(
            "москва" to Pair(55.7558, 37.6173),
            "санкт-петербург" to Pair(59.9343, 30.3351),
            "новосибирск" to Pair(55.0084, 82.9357),
            "екатеринбург" to Pair(56.8389, 60.6057),
            "казань" to Pair(55.7963, 49.1083),
            "нижний новгород" to Pair(56.3269, 44.0059),
            "челябинск" to Pair(55.1644, 61.4368),
            "самара" to Pair(53.1951, 50.1068),
            "омск" to Pair(54.9885, 73.3242),
            "ростов-на-дону" to Pair(47.2225, 39.7188)
        )

        return cities[city.lowercase()]
    }
}