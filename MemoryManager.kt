package com.sabrina.fibis

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class MemoryManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("FibisMemory", Context.MODE_PRIVATE)

    fun saveConversation(userMessage: String, assistantResponse: String) {
        val conversations = getRecentConversations(50)
        conversations.add("П: $userMessage | Ф: $assistantResponse")

        if (conversations.size > 50) {
            conversations.removeAt(0)
        }

        prefs.edit().putString("conversations", JSONArray(conversations).toString()).apply()
    }

    fun getRecentConversations(count: Int): MutableList<String> {
        val jsonString = prefs.getString("conversations", "[]") ?: "[]"
        val jsonArray = JSONArray(jsonString)
        val conversations = mutableListOf<String>()

        for (i in maxOf(0, jsonArray.length() - count) until jsonArray.length()) {
            conversations.add(jsonArray.getString(i))
        }

        return conversations
    }

    fun getUserPreferences(): String {
        val preferences = prefs.getString("user_preferences", "") ?: ""
        return if (preferences.isNotEmpty()) "Предпочтения: $preferences" else "Предпочтения не заданы"
    }

    fun saveUserPreference(key: String, value: String) {
        val current = prefs.getString("user_preferences", "") ?: ""
        val newPreferences = if (current.isEmpty()) "$key:$value" else "$current,$key:$value"
        prefs.edit().putString("user_preferences", newPreferences).apply()
    }

    fun loadMemory(): Boolean {
        return prefs.getBoolean("memory_loaded", false)
    }
}