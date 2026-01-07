package com.chattalkie.app.ui.model

data class ConversationUiState(
    val id: String,
    val partnerName: String,
    val lastMessage: String,
    val timestamp: String,
    val isGroup: Boolean = false,
    val role: String? = null,
    val isOnline: Boolean = false
)
