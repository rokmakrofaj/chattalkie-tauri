package com.chattalkie.domain

import kotlinx.serialization.Serializable

@Serializable
sealed class Message {
    abstract val messageId: MessageId
    abstract val cid: String?
    abstract val senderId: UserId
    abstract val content: String
    abstract val mediaKey: String?
    abstract val timestamp: Long
    abstract val messageType: MessageType
    abstract val senderAvatar: String?
    
    @Serializable
    data class DirectMessage(
        override val messageId: MessageId,
        override val cid: String?,
        override val senderId: UserId,
        val senderName: String? = null,
        override val senderAvatar: String? = null,
        val receiverId: UserId,
        override val content: String,
        override val timestamp: Long,
        override val messageType: MessageType,
        override val mediaKey: String? = null,
        val status: MessageStatus = MessageStatus.SENT
    ) : Message()
    
    @Serializable
    data class GroupMessage(
        override val messageId: MessageId,
        override val cid: String?,
        override val senderId: UserId,
        val senderName: String? = null,
        override val senderAvatar: String? = null,
        val groupId: GroupId,
        override val content: String,
        override val timestamp: Long,
        override val messageType: MessageType,
        override val mediaKey: String? = null,
        val status: MessageStatus = MessageStatus.SENT
    ) : Message()
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    FILE,
    VOICE
}

