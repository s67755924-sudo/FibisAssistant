package com.sabrina.fibis

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*

class AlarmHelper(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun setAlarm(message: String): String {
        val time = extractTimeFromMessage(message)
        return if (time != null) {
            "Установлю будильник на $time. Для точной настройки лучше использовать стандартное приложение будильника."
        } else {
            "На какое время установить будильник? Скажи, например, 'будильник на 7 утра'"
        }
    }

    fun setTimer(message: String): String {
        val duration = extractDurationFromMessage(message)
        return if (duration > 0) {
            "Запускаю таймер на $duration минут. Не забудь активировать его в приложении таймера."
        } else {
            "На сколько минут поставить таймер?"
        }
    }

    private fun extractTimeFromMessage(message: String): String? {
        val regex = Regex("""(\d{1,2})[\s:]?(\d{2})?\s*([уУ]тра|[вВ]ечера|утром|вечером|ночи)?""")
        val match = regex.find(message)

        return match?.groups?.get(1)?.value?.let { hour ->
            val minute = match.groups[2]?.value ?: "00"
            val period = match.groups[3]?.value ?: ""
            "$hour:$minute $period"
        }
    }

    private fun extractDurationFromMessage(message: String): Int {
        val regex = Regex("""(\d+)\s*(минут|минуты|час|часа|часов)""")
        val match = regex.find(message)

        return match?.groups?.get(1)?.value?.toIntOrNull() ?: 0
    }
}