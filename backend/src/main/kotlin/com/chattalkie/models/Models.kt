package com.chattalkie.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class RegisterRequest(
    val name: String,
    val username: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class UserUpdateRequest(
    val name: String,
    val username: String
)

@Serializable
data class AuthResponse(
    val token: String,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: Int,
    val username: String,
    val name: String,
    val avatarUrl: String? = null,
    val status: String,
    val friendshipStatus: String? = null // "PENDING", "ACCEPTED" or null
)

@Serializable
data class ErrorResponse(
    val error: String
)

@Serializable
data class IncomingMessage(
    val messageId: String? = null,
    val cid: String? = null, // Client ID
    val content: String,
    val recipientId: Int? = null,
    val groupId: Int? = null,
    val type: String? = null,
    val kind: String? = null,
    val mediaKey: String? = null,
    val messageType: String? = null
)

@Serializable
sealed class WsMessage

@Serializable
@SerialName("chat")
data class ChatMessage(
    val messageId: String,
    val cid: String,
    val senderId: Int,
    val senderName: String,
    val content: String,
    val timestamp: Long,
    val groupId: Int? = null,
    val receiverId: Int? = null,
    val mediaKey: String? = null,
    val messageType: String? = null
) : WsMessage()

@Serializable
@SerialName("status")
data class StatusUpdate(
    val userId: Int,
    val status: String // "online" or "offline"
) : WsMessage()

@Serializable
@SerialName("presence_list")
data class PresenceList(
    val onlineUserIds: List<Int>
) : WsMessage()


@Serializable
@SerialName("ack")
data class Ack(
    val id: String, // Message UUID
    val cid: String, // Client UUID - nullable for legacy messages
    val status: String // "SENT", "FAILED"
) : WsMessage()

@Serializable
@SerialName("delivery_status")
data class DeliveryStatus(
    val messageId: String,
    val cid: String?,
    val status: String, // "DELIVERED", "READ"
    val userId: Int,    // Who read/received it
    val recipientId: Int? = null, // Who to notify (Original Sender)
    val groupId: Int? = null,
    val timestamp: Long
) : WsMessage()

@Serializable
@SerialName("typing")
data class TypingSignal(
    val senderId: Int,
    val recipientId: Int? = null,
    val groupId: Int? = null,
    val isTyping: Boolean
) : WsMessage()

@Serializable
@SerialName("signal") // Changed from "call_signal" to match frontend's "kind": "signal"
data class CallSignal(
    val type: String, // OFFER, ANSWER, ICE_CANDIDATE, HANGUP, BUSY
    val senderId: Int,
    val receiverId: Int,
    val payload: String // SDP or ICE Candidate JSON
    // Removed 'kind' property - the @SerialName annotation handles the discriminator
) : WsMessage()

@Serializable
data class OutgoingMessage(
    val messageId: String,
    val cid: String?,
    val senderId: Int,
    val senderName: String,
    val senderAvatar: String? = null,
    val content: String,
    val mediaKey: String? = null,
    val timestamp: Long,
    val groupId: Int? = null,
    val messageType: String? = null
)

@Serializable
data class FriendRequest(
    val friendId: Int
)

@Serializable
data class FriendActionRequest(
    val userId: Int,
    val action: String // "ACCEPT" or "REJECT"
)

@Serializable
data class FriendResponse(
    val id: Int,
    val username: String,
    val name: String,
    val avatarUrl: String?,
    val status: String,
    val friendShipStatus: String // "PENDING", "ACCEPTED"
)
@Serializable
data class GroupCreateRequest(
    val name: String,
    val memberIds: List<Int>
)

@Serializable
data class GroupResponse(
    val id: Int,
    val name: String,
    val createdBy: Int,
    val createdAt: Long,
    val memberCount: Int? = null,
    val myRole: String? = null
)

@Serializable
data class GroupMemberResponse(
    val userId: Int,
    val name: String,
    val username: String,
    val avatarUrl: String? = null,
    val role: String,
    val joinedAt: Long
)

@Serializable
data class MemberKickRequest(
    val userId: Int
)

@Serializable
data class MemberAddRequest(
    val userId: Int
)

@Serializable
data class InviteLinkResponse(
    val token: String,
    val groupName: String,
    val groupId: Int,
    val createdAt: Long,
    val expiresAt: Long? = null
)

@Serializable
data class JoinGroupRequest(
    val inviteToken: String
)
