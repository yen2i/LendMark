package com.example.lendmark.data.model

data class ChatMessage(
    val message: String,
    val isUser: Boolean,   // true = 사용자, false = AI
    val timestamp: Long = System.currentTimeMillis(),
    val roomList: List<String>? = null
)

