package com.chattalkie.services

import com.chattalkie.domain.*
import com.chattalkie.repositories.MessageRepository

/**
 * ChatService handles business logic for real-time messaging.
 * Called by WebSocket layer - no HTTP/Ktor dependencies here.
 */
class ChatService(
    private val messageRepository: MessageRepository,
    private val userRepository: com.chattalkie.repositories.UserRepository,
    private val groupRepository: com.chattalkie.repositories.GroupRepository
) {
    
    /**
     * Persists a direct message and returns member IDs to notify
     */
    /**
     * Persists a direct message and returns member IDs to notify
     */
    fun persistDirectMessage(
        senderId: Int,
        recipientId: Int,
        messageId: String,
        cid: String?,
        content: String,
        mediaKey: String? = null,
        messageType: String? = null
    ): DirectMessageResult {
        messageRepository.saveDirectMessage(
            senderId = UserId(senderId),
            recipientId = UserId(recipientId),
            messageId = messageId,
            cid = cid,
            content = content,
            mediaKey = mediaKey,
            messageType = messageType
        )
        // Return both sender and recipient for notification
        return DirectMessageResult(senderId, recipientId)
    }

    /**
     * Persists a group message and returns member IDs to notify
     */
    fun persistGroupMessage(
        senderId: Int,
        groupId: Int,
        messageId: String,
        cid: String?,
        content: String,
        mediaKey: String? = null,
        messageType: String? = null
    ): GroupMessageResult {
        messageRepository.saveGroupMessage(
            senderId = UserId(senderId),
            groupId = GroupId(groupId),
            messageId = messageId,
            cid = cid,
            content = content,
            mediaKey = mediaKey,
            messageType = messageType
        )
        
        // Get all group members for broadcast
        val memberIds = groupRepository.findGroupMembers(GroupId(groupId))
            .map { it.userId.value }
        
        return GroupMessageResult(memberIds)
    }

    // Presence is now handled in memory by PresenceService
    // fun updateUserStatus(userId: Int, status: String) {
    //     userRepository.updateStatus(UserId(userId), UserStatus.fromString(status))
    // }

    /**
     * Gets friend IDs for status broadcast
     */
    fun getFriendIds(userId: Int): List<Int> {
        return userRepository.getFriendIds(UserId(userId))
    }

    /**
     * Gets group member IDs for broadcast
     */
    fun getGroupMemberIds(groupId: Int): List<Int> {
        return groupRepository.findGroupMembers(GroupId(groupId)).map { it.userId.value }
    }

    data class DirectMessageResult(val senderId: Int, val recipientId: Int)
    data class GroupMessageResult(val memberIds: List<Int>)
}
