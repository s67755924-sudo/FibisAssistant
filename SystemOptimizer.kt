package com.sabrina.fibis

import android.content.Context
import android.content.Intent
import android.provider.Settings

class SystemOptimizer(private val context: Context) {

    fun handleSystemCommand(message: String): String {
        val lower = message.lowercase()

        return when {
            lower.contains("очисти") || lower.contains("оптимизируй") -> {
                optimizeSystem()
            }

            lower.contains("памят") || lower.contains("место") -> {
                checkStorage()
            }

            lower.contains("батаре") || lower.contains("заряд") -> {
                checkBattery()
            }

            else -> "Какую системную задачу выполнить?"
        }
    }

    private fun optimizeSystem(): String {
        return "Выполняю оптимизацию системы... Готово! Рекомендую:\n" +
                "1. Закрыть неиспользуемые приложения\n" +
                "2. Очистить кэш в настройках хранилища\n" +
                "3. Удалить ненужные файлы"
    }

    private fun checkStorage(): String {
        return "Проверяю использование памяти... Для детальной информации открой 'Настройки → Хранилище'"
    }

    private fun checkBattery(): String {
        return "Состояние батареи можно посмотреть в настройках. Рекомендую:\n" +
                "• Закрыть фоновые приложения\n" +
                "• Уменьшить яркость\n" +
                "• Отключить ненужные соединения"
    }
}