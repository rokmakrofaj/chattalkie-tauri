package com.chattalkie.domain

import kotlinx.serialization.Serializable

@Serializable
data class SyncResponse(
    val messages: List<Message>,
    val chats: List<Chat>, // Using existing Chat logic or simplified
    val tombstones: List<Tombstone>,
    val nextTs: Long
)

@Serializable
data class Tombstone(
    val id: String, // UUID of the deleted item
    val type: TombstoneType,
    val deletedAt: Long
)

enum class TombstoneType {
    MESSAGE,
    CHAT,
    GROUP
}

// Optional: for Chat model if not already present
@Serializable
data class Chat(
    val id: Int,
    val type: ChatType,
    val name: String?,
    val updatedAt: Long,
    val unreadCount: Int = 0,
    val lastMessage: Message? = null
)

enum class ChatType {
    DIRECT,
    GROUP
}
