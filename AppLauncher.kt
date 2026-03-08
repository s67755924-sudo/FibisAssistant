package com.sabrina.fibis

import android.content.Context
import android.content.Intent
import android.provider.Settings

class AppLauncher(private val context: Context) {

    fun openApp(message: String): String {
        val lower = message.lowercase()

        return when {
            lower.contains("яндекс.музыка") || lower.contains("яндекс музыку") -> {
                launchApp("ru.yandex.music", "Яндекс.Музыка")
            }
            lower.contains("вк") || lower.contains("вконтакте") -> {
                launchApp("com.vkontakte.android", "ВКонтакте")
            }
            lower.contains("телеграм") || lower.contains("telegram") -> {
                launchApp("org.telegram.messenger", "Telegram")
            }
            lower.contains("whatsapp") -> {
                launchApp("com.whatsapp", "WhatsApp")
            }
            lower.contains("камера") -> {
                launchApp("com.android.camera", "Камеру")
            }
            lower.contains("галере") || lower.contains("фото") -> {
                launchApp("com.android.gallery3d", "Галерею")
            }
            lower.contains("настройки") || lower.contains("настройки") -> {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                })
                "Открываю настройки"
            }
            else -> "Какое приложение открыть? Уточни, пожалуйста."
        }
    }

    private fun launchApp(packageName: String, appName: String): String {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Открываю $appName"
            } else {
                "Приложение $appName не найдено"
            }
        } catch (e: Exception) {
            "Не удалось открыть $appName: ${e.message}"
        }
    }
}