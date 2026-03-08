package com.sabrina.fibis

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaController(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    companion object {
        private val musicKeywords = listOf(
            "включи", "поставь", "запусти", "играй",
            "песн", "трек", "музык", "групп", "исполнитель"
        )
    }

    suspend fun handleMediaCommand(message: String): String = withContext(Dispatchers.IO) {
        val lower = message.lowercase()

        return@withContext when {
            containsMusicRequest(lower) -> processSpecificMusicRequest(message)

            lower.contains("включи музыку") || lower.contains("поставь музыку") -> {
                launchMusicApp()
            }

            lower.contains("пауз") || lower.contains("останови") -> {
                sendMediaKey(88)
                "Ставлю на паузу"
            }

            lower.contains("продолжи") || lower.contains("возобнов") -> {
                sendMediaKey(86)
                "Продолжаю воспроизведение"
            }

            lower.contains("следующ") -> {
                sendMediaKey(87)
                "Переключаю на следующий трек"
            }

            lower.contains("предыдущ") -> {
                sendMediaKey(85)
                "Переключаю на предыдущий трек"
            }

            lower.contains("громч") -> {
                adjustVolume(1)
                "Увеличиваю громкость"
            }

            lower.contains("тиш") -> {
                adjustVolume(-1)
                "Уменьшаю громкость"
            }

            lower.contains("беззвуч") -> {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
                "Включаю беззвучный режим"
            }

            else -> "Что сделать с музыкой?"
        }
    }

    private fun containsMusicRequest(message: String): Boolean {
        return musicKeywords.any { message.contains(it) }
    }

    private fun processSpecificMusicRequest(message: String): String {
        val (artist, song, isRandom) = parseMusicRequest(message)

        return if (artist.isNotEmpty()) {
            if (song.isNotEmpty()) {
                searchAndPlaySpecificSong(artist, song)
            } else if (isRandom) {
                searchAndPlayRandomSong(artist)
            } else {
                searchAndPlayArtist(artist)
            }
        } else {
            "Не понял, какую музыку включить. Уточни, пожалуйста, исполнителя или название песни."
        }
    }

    private fun parseMusicRequest(message: String): Triple<String, String, Boolean> {
        var artist = ""
        var song = ""
        var isRandom = false

        val lowerMessage = message.lowercase()

        val artistPatterns = listOf(
            Regex("""групп[ауы]?\s+([^\s]+[^,.!?]+)"""),
            Regex("""исполнител[ья]\s+([^\s]+[^,.!?]+)"""),
            Regex("""включи\s+([^\s]+[^,.!?]+)"""),
            Regex("""поставь\s+([^\s]+[^,.!?]+)""")
        )

        for (pattern in artistPatterns) {
            val match = pattern.find(lowerMessage)
            if (match != null) {
                var extracted = match.groupValues[1].trim()

                extracted = extracted.replace("(пожалуйста|мне|свою|любую|песню|трек)".toRegex(), "")
                    .replace("""\s+""".toRegex(), " ")
                    .trim()

                if (extracted.contains("песн") || extracted.contains("трек")) {
                    val parts = extracted.split("песн", "трек").map { it.trim() }
                    if (parts.size > 1) {
                        artist = parts[0]
                        song = parts[1].replace("ю", "").replace("у", "").trim()
                    }
                } else {
                    artist = extracted
                }
                break
            }
        }

        isRandom = lowerMessage.contains("любую") ||
                lowerMessage.contains("рандом") ||
                lowerMessage.contains("случайн") ||
                lowerMessage.contains("любой")

        if (artist.isEmpty()) {
            artist = extractArtistFromMessage(lowerMessage)
        }

        if (song.isEmpty() && lowerMessage.contains("песн")) {
            song = extractSongFromMessage(lowerMessage, artist)
        }

        return Triple(artist, song, isRandom)
    }

    private fun extractArtistFromMessage(message: String): String {
        var cleaned = message
            .replace("(включи|поставь|пожалуйста|мне|группу|исполнителя|песню|трек|любую|рандом)".toRegex(), "")
            .replace("""\s+""".toRegex(), " ")
            .trim()

        cleaned = cleaned.split("песн", "трек")[0].trim()

        return cleaned
    }

    private fun extractSongFromMessage(message: String, artist: String): String {
        var cleaned = message
            .replace(artist, "")
            .replace("(включи|поставь|пожалуйста|мне|группу|исполнителя|песню|трек)".toRegex(), "")
            .replace("""\s+""".toRegex(), " ")
            .trim()

        cleaned = cleaned.replace("(любую|рандом|случайную)".toRegex(), "").trim()

        return cleaned
    }

    private fun searchAndPlaySpecificSong(artist: String, song: String): String {
        val searchQuery = "$artist $song"
        return openInYandexMusic(searchQuery)
    }

    private fun searchAndPlayRandomSong(artist: String): String {
        return openInYandexMusic(artist, true)
    }

    private fun searchAndPlayArtist(artist: String): String {
        return openInYandexMusic(artist)
    }

    private fun openInYandexMusic(searchQuery: String, isRandom: Boolean = false): String {
        return try {
            val encodedQuery = Uri.encode(searchQuery.trim())
            val yandexMusicUri = if (isRandom) {
                "yandexmusic://search?text=$encodedQuery"
            } else {
                "yandexmusic://search?text=$encodedQuery"
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(yandexMusicUri)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                if (isRandom) {
                    "Включаю случайную песню $searchQuery в Яндекс.Музыке"
                } else {
                    "Ищу $searchQuery в Яндекс.Музыке"
                }
            } else {
                openInBrowser(searchQuery, isRandom)
            }
        } catch (_: Exception) {
            "Не удалось открыть музыку"
        }
    }

    private fun openInBrowser(searchQuery: String, isRandom: Boolean): String {
        return try {
            val encodedQuery = Uri.encode(searchQuery.trim())
            val webUrl = "https://music.yandex.ru/search?text=$encodedQuery"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(webUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(intent)
            if (isRandom) {
                "Открываю случайные песни $searchQuery в браузере"
            } else {
                "Открываю поиск $searchQuery в Яндекс.Музыке через браузер"
            }
        } catch (_: Exception) {
            "Не удалось открыть музыку в браузере. Установите Яндекс.Музыку для лучшего опыта."
        }
    }

    private fun launchMusicApp(): String {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage("ru.yandex.music")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                "Запускаю Яндекс.Музыку"
            } else {
                val musicIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/*")
                }
                context.startActivity(musicIntent)
                "Запускаю музыкальный плеер"
            }
        } catch (_: Exception) {
            "Не удалось запустить музыкальное приложение"
        }
    }

    private fun sendMediaKey(keyCode: Int) {
        try {
            val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                putExtra(Intent.EXTRA_KEY_EVENT, android.view.KeyEvent(keyCode, 0))
            }
            context.sendOrderedBroadcast(intent, null)
        } catch (_: Exception) {
        }
    }

    private fun adjustVolume(direction: Int) {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (direction > 0) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            0
        )
    }
}