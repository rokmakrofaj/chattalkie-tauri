package com.chattalkie.services

import com.chattalkie.domain.*
import com.chattalkie.models.OutgoingMessage
import com.chattalkie.repositories.MessageRepository

class MessageService(
    private val messageRepository: MessageRepository
) {

    fun getChatHistory(userId: Int, partnerId: Int): List<OutgoingMessage> {
        return messageRepository.findDirectMessages(UserId(userId), UserId(partnerId))
            .map { it.toResponse() }
    }

    fun getGroupHistory(groupId: Int): List<OutgoingMessage> {
        return messageRepository.findGroupMessages(GroupId(groupId))
            .map { it.toResponse() }
    }
    
    private fun Message.DirectMessage.toResponse(): OutgoingMessage {
        return OutgoingMessage(
            messageId = messageId.value,
            cid = cid,
            senderId = senderId.value,
            senderName = senderName ?: "",
            senderAvatar = senderAvatar,
            content = content,
            mediaKey = mediaKey, // Only mediaKey, client fetches via /api/media/{key}
            timestamp = timestamp,
            groupId = null
        )
    }
    
    private fun Message.GroupMessage.toResponse(): OutgoingMessage {
        return OutgoingMessage(
            messageId = messageId.value,
            cid = cid,
            senderId = senderId.value,
            senderName = senderName ?: "",
            senderAvatar = senderAvatar,
            content = content,
            mediaKey = mediaKey, // Only mediaKey
            timestamp = timestamp,
            groupId = groupId.value
        )
    }
}

