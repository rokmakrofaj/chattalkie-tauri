package com.chattalkie.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class MessageRequest(
    @SerializedName("messageId") val messageId: String,
    @SerializedName("cid") val cid: String,
    @SerializedName("content") val content: String,
    @SerializedName("recipientId") val recipientId: Int? = null,
    @SerializedName("groupId") val groupId: Int? = null,
    @SerializedName("mediaKey") val mediaKey: String? = null,
    @SerializedName("messageType") val messageType: String? = null
)

data class MessageResponse(
    @SerializedName("messageId") val messageId: String = "",
    @SerializedName("cid") val cid: String? = null,
    @SerializedName("senderId") val senderId: Int,
    @SerializedName("senderName") val senderName: String? = null,
    @SerializedName("content") val content: String,
    @SerializedName("mediaKey") val mediaKey: String? = null,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("groupId") val groupId: Int? = null,
    @SerializedName("receiverId") val receiverId: Int? = null,
    @SerializedName("kind") val kind: String? = null,
    @SerializedName("messageType") val messageType: String? = null
)

// WebSocket Polymorphic Models
open class WsMessage(
    @SerializedName("kind") val kind: String = ""
)

data class ChatWsMessage(
    @SerializedName("messageId") val messageId: String,
    @SerializedName("cid") val cid: String? = null,
    @SerializedName("senderId") val senderId: Int,
    @SerializedName("senderName") val senderName: String,
    @SerializedName("content") val content: String,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("groupId") val groupId: Int? = null,
    @SerializedName("recipientId") val recipientId: Int? = null,
    @SerializedName("mediaKey") val mediaKey: String? = null,
    @SerializedName("messageType") val messageType: String? = null
) : WsMessage("chat")

data class StatusUpdateWsMessage(
    @SerializedName("userId") val userId: Int,
    @SerializedName("status") val status: String
) : WsMessage("status")

data class AckWsMessage(
    @SerializedName("id") val id: String,
    @SerializedName("cid") val cid: String? = null,
    @SerializedName("status") val status: String
) : WsMessage("ack")

data class TypingWsMessage(
    @SerializedName("chatId") val chatId: Int
) : WsMessage("typing")

// Group Models
data class GroupRequest(
    @SerializedName("name") val name: String,
    @SerializedName("memberIds") val memberIds: List<Int>
)

data class GroupResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("createdBy") val createdBy: Int,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("memberCount") val memberCount: Int? = null,
    @SerializedName("myRole") val myRole: String? = null
)

data class GroupMemberResponse(
    @SerializedName("userId") val userId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("username") val username: String,
    @SerializedName("avatarUrl") val avatarUrl: String? = null,
    @SerializedName("role") val role: String,
    @SerializedName("joinedAt") val joinedAt: Long
)

data class MemberKickRequest(
    @SerializedName("userId") val userId: Int
)

data class TypingRequest(
    @SerializedName("kind") val kind: String = "typing",
    @SerializedName("senderId") val senderId: Int,
    @SerializedName("isTyping") val isTyping: Boolean,
    @SerializedName("recipientId") val recipientId: Int? = null,
    @SerializedName("groupId") val groupId: Int? = null
)

data class DeliveryStatusRequest(
    @SerializedName("kind") val kind: String = "delivery_status",
    @SerializedName("messageId") val messageId: String?,
    @SerializedName("cid") val cid: String?,
    @SerializedName("status") val status: String,
    @SerializedName("userId") val userId: Int,
    @SerializedName("recipientId") val recipientId: Int,
    @SerializedName("timestamp") val timestamp: Long,
    @SerializedName("groupId") val groupId: Int? = null
)
