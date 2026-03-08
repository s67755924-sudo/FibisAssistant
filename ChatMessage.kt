package com.sabrina.fibis

import java.util.*

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Date = Date()
)