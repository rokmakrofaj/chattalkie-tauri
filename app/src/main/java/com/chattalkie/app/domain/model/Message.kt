package com.chattalkie.app.domain.model

/**
 * Represents the core data model for a single message.
 */
data class Message(
    val id: String,
    val cid: String? = null,
    val userId: String,
    val text: String,
    val createdAt: Long,
    val isMe: Boolean,
    val senderName: String? = null,
    val timestamp: String = "", // e.g. "10:42"
    val status: MessageStatus = MessageStatus.SENT,
    val mediaKey: String? = null,
    val messageType: MessageType = MessageType.TEXT
)

enum class MessageStatus {
    SENDING,
    SENT,
    SYNCED,
    FAILED,
    DELIVERED,
    READ
}

enum class MessageType {
    TEXT,
    IMAGE,
    VOICE,
    FILE,
    VIDEO
}
