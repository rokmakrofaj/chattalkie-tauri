package com.chattalkie.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: Int,
    val username: String,
    val name: String,
    val avatarUrl: String?,
    val status: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: Int, // Chat ID or Group ID
    val type: String, // DIRECT / GROUP
    val name: String?,
    val lastMessageContent: String?,
    val lastMessageTime: Long,
    val unreadCount: Int = 0
)

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["cid"], unique = true),
        Index(value = ["chatId", "timestamp"])
    ]
)
data class MessageEntity(
    @PrimaryKey val cid: String, // Client Generated ID (UUID)
    val messageId: String?, // Server ID (nullable until ACK)
    val chatId: Int,
    val senderId: Int,
    val senderName: String? = null,
    val content: String,
    val mediaKey: String? = null,
    val messageType: String? = null,
    val timestamp: Long,
    val status: MessageStatus = MessageStatus.SENDING,
    val isMine: Boolean
)

enum class MessageStatus {
    SENDING,
    SENT,
    FAILED,
    SYNCED
}

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val chatId: Int, // One draft per chat
    val content: String,
    val type: String = "text", // future proof for void
    val updatedAt: Long = System.currentTimeMillis()
)
