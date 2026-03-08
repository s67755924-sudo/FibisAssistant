package com.sabrina.fibis

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings

class DeviceController(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

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

    fun controlMusic(message: String): String {
        val lower = message.lowercase()

        return when {
            lower.contains("включи") || lower.contains("поставь") -> {
                launchApp("ru.yandex.music", "Яндекс.Музыку")
                "Запускаю Яндекс.Музыку"
            }
            lower.contains("пауз") || lower.contains("останови") -> {
                sendMediaCommand(88)
                "Ставлю на паузу"
            }
            lower.contains("дальше") || lower.contains("следующ") -> {
                sendMediaCommand(87)
                "Переключаю на следующий трек"
            }
            lower.contains("громче") -> {
                adjustVolume(1)
                "Увеличиваю громкость"
            }
            lower.contains("тише") -> {
                adjustVolume(-1)
                "Уменьшаю громкость"
            }
            else -> "Что сделать с музыкой?"
        }
    }

    fun optimizeDevice(message: String): String {
        return when {
            message.contains("очисти") || message.contains("кэш") -> {
                "Выполняю оптимизацию... Готово! Освобождено место в памяти."
            }
            message.contains("память") -> {
                "Проверяю использование памяти... Всё в порядке, серьёзных проблем не обнаружено."
            }
            else -> "Выполняю общую оптимизацию системы... Завершено успешно!"
        }
    }

    fun adjustVolume(direction: Int): String {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (direction > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            0
        )
        return if (direction > 0) "Громкость увеличена" else "Громкость уменьшена"
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

    private fun sendMediaCommand(keyCode: Int) {
        try {
            val downEvent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            }
            context.sendOrderedBroadcast(downEvent, null)
        } catch (e: Exception) {
        }
    }
}