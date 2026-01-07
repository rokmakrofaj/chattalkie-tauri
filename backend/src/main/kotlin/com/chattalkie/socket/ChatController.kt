package com.chattalkie.socket

import com.chattalkie.models.WsMessage
import com.chattalkie.models.ChatMessage
import com.chattalkie.models.StatusUpdate
import com.chattalkie.models.OutgoingMessage
import com.chattalkie.models.CallSignal
import com.chattalkie.models.TypingSignal
import com.chattalkie.models.DeliveryStatus
import com.chattalkie.services.ChatService
import io.ktor.websocket.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Collections

/**
 * ChatController manages WebSocket connections and message routing.
 * 
 * Responsibilities:
 * - Connection lifecycle (connect/disconnect)
 * - Message routing to recipients
 * - Status broadcast to friends
 * 
 * NO database access - delegates to ChatService for persistence.
 */
class ChatController(
    private val chatService: ChatService,
    private val presenceService: com.chattalkie.services.PresenceService
) {
    private val connections = Collections.synchronizedSet<Connection>(LinkedHashSet())
    private val json = Json { 
        classDiscriminator = "kind" // Must match Serialization.kt config
        ignoreUnknownKeys = true
    }

    /**
     * Called when a new WebSocket connection is established.
     * Auth is already validated by Route layer.
     */
    suspend fun onConnect(connection: Connection) {
        // println("WS CONNECT -> User ${connection.userId} (${connection.username})")
        connections += connection
        
        // Use In-Memory Presence Service (No DB Write)
        val isFirstDevice = presenceService.connect(connection.userId)
        
        // Only broadcast if this is the first device coming online
        if (isFirstDevice) {
            broadcastStatus(connection.userId, "online")
        }
        
        sendInitialPresence(connection)
    }

    /**
     * Called when a WebSocket connection is closed.
     */
    suspend fun onDisconnect(connection: Connection) {
        connections -= connection
        
        // Use In-Memory Presence Service (No DB Write)
        val isLastDevice = presenceService.disconnect(connection.userId)
        
        // Only broadcast offline if this was the last device
        if (isLastDevice) {
            broadcastStatus(connection.userId, "offline")
        }
    }

    /**
     * Sends the list of currently online friends to the newly connected user.
     */
    private suspend fun sendInitialPresence(connection: Connection) {
        val friendIds = chatService.getFriendIds(connection.userId)
        
        // Filter friends who are online using the PresenceService
        val onlineFriendIds = presenceService.filterOnlineUsers(friendIds)
            
        if (onlineFriendIds.isNotEmpty()) {
            val presenceList = com.chattalkie.models.PresenceList(onlineFriendIds)
            sendWsMessage(connection.session, presenceList)
        }
    }

    /**
     * Broadcasts user status to all online friends.
     */
    private suspend fun broadcastStatus(userId: Int, status: String) {
        val friendIds = chatService.getFriendIds(userId)
        
        val update = StatusUpdate(userId, status)
        
        // Find connections of friends who are currently connected to this instance
        // Note: In a distributed system, this would need Redis Pub/Sub.
        // For mono-server, iterating local connections is fine.
        connections.filter { it.userId in friendIds }.forEach { 
            sendWsMessage(it.session, update)
        }
    }

    /**
     * Sends a direct message to a specific user.
     */
    /**
     * Sends a direct message to a specific user.
     */
    suspend fun sendMessage(recipientId: Int, message: OutgoingMessage) {
        // STRICT PROTOCOL: cid is mandatory.
        val broadcastCid = message.cid ?: message.messageId
        
        // 1. Prepare WebSocket frame
        val wsMessage = ChatMessage(
            messageId = message.messageId,
            cid = broadcastCid,
            senderId = message.senderId,
            senderName = message.senderName,
            content = message.content,
            timestamp = message.timestamp,
            mediaKey = message.mediaKey,
            receiverId = recipientId,
            messageType = message.messageType
        )
        
        // 2. BROADCAST IMMEDIATELY (Latency Optimization)
        // Push to recipient AND sender (for multi-device sync)
        val targets = setOf(recipientId, message.senderId)
        connections.filter { it.userId in targets }.forEach { 
            sendWsMessage(it.session, wsMessage)
        }

        // 3. Persist (Async/IO)
        try {
            chatService.persistDirectMessage(
                senderId = message.senderId,
                recipientId = recipientId,
                messageId = message.messageId,
                cid = message.cid,
                content = message.content,
                mediaKey = message.mediaKey,
                messageType = message.messageType
            )

            // 4. Send ACK to sender (Only after successful persistence)
            val ack = com.chattalkie.models.Ack(
                id = message.messageId,
                cid = message.cid ?: message.messageId,
                status = "SENT"
            )
            connections.find { it.userId == message.senderId }?.let { 
                sendWsMessage(it.session, ack)
            }
        } catch (e: Exception) {
            println("❌ Persistence Failed for ${message.messageId}: ${e.message}")
            // TODO: Ideally send error ACK
        }
    }

    /**
     * Sends a group message to all group members.
     */
    suspend fun sendGroupMessage(groupId: Int, message: OutgoingMessage) {
        val broadcastCid = message.cid ?: message.messageId

        // 1. Get Members (Read-Only - Fast)
        val memberIds = chatService.getGroupMemberIds(groupId)

        // 2. Prepare WebSocket frame
        val wsMessage = ChatMessage(
            messageId = message.messageId,
            cid = broadcastCid,
            senderId = message.senderId,
            senderName = message.senderName,
            content = message.content,
            timestamp = message.timestamp,
            groupId = groupId,
            mediaKey = message.mediaKey,
            messageType = message.messageType
        )

        // 3. BROADCAST IMMEDIATELY (Latency Optimization)
        // println("ChatController: Broadcasting group message ${message.messageId} to $memberIds")
        connections.filter { it.userId in memberIds }.forEach { 
             sendWsMessage(it.session, wsMessage)
        }

        // 4. Persist (IO)
        try {
             chatService.persistGroupMessage(
                senderId = message.senderId,
                groupId = groupId,
                messageId = message.messageId,
                cid = message.cid,
                content = message.content,
                mediaKey = message.mediaKey,
                messageType = message.messageType
            )

            // 5. Send ACK to Sender (After persistence)
            val ack = com.chattalkie.models.Ack(
                id = message.messageId,
                cid = message.cid ?: message.messageId,
                status = "SENT"
            )
            connections.find { it.userId == message.senderId }?.let {
                sendWsMessage(it.session, ack)
            }
        } catch (e: Exception) {
             println("❌ Persistence Failed for Group Msg ${message.messageId}: ${e.message}")
        }
    }

    suspend fun broadcastTyping(signal: TypingSignal) {
        if (signal.groupId != null) {
            val memberIds = chatService.getGroupMemberIds(signal.groupId)
            connections.filter { it.userId in memberIds && it.userId != signal.senderId }.forEach {
                sendWsMessage(it.session, signal)
            }
        } else if (signal.recipientId != null) {
             connections.find { it.userId == signal.recipientId }?.let {
                sendWsMessage(it.session, signal)
            }
        }
    }

    suspend fun broadcastDeliveryStatus(status: DeliveryStatus, recipientId: Int?) {
         if (status.groupId != null) {
            val memberIds = chatService.getGroupMemberIds(status.groupId)
            connections.filter { it.userId in memberIds && it.userId != status.userId }.forEach {
                sendWsMessage(it.session, status)
            }
         } else if (recipientId != null) {
             connections.find { it.userId == recipientId }?.let {
                 sendWsMessage(it.session, status)
             }
         }
    }

    private suspend fun sendWsMessage(session: WebSocketSession, message: WsMessage) {
        val payload = json.encodeToString<WsMessage>(message)
        if (payload.isBlank()) {
            // println("❌ WS SEND BLOCKED: empty payload for ${message::class.simpleName}")
            return
        }
        // println("WS SEND -> type=${message::class.simpleName} payload=$payload")
        session.send(Frame.Text(payload))
    }

    fun getConnectedUserIds(): List<Int> {
        return connections.map { it.userId }
    }

    suspend fun routeCallSignal(signal: CallSignal) {
        println("SIGNAL ROUTE: ${signal.type} from ${signal.senderId} to ${signal.receiverId}")
        val target = connections.find { it.userId == signal.receiverId }
        
        if (target != null) {
            // Serialize CallSignal directly, NOT as WsMessage, to avoid discriminator conflict
            sendCallSignal(target.session, signal)
        } else {
            println("SIGNAL FAILED: Target ${signal.receiverId} not connected.")
            // Notify sender that target is offline if it was an OFFER
            if (signal.type == "OFFER") {
                val busySignal = signal.copy(
                    type = "BUSY",
                    senderId = signal.receiverId, // As if target sent it
                    receiverId = signal.senderId,
                    payload = "offline"
                )
                connections.find { it.userId == signal.senderId }?.let {
                    sendCallSignal(it.session, busySignal)
                }
            }
        }
    }

    /**
     * Sends a CallSignal directly without using sealed class polymorphism.
     * This avoids the "kind" discriminator conflict.
     */
    private suspend fun sendCallSignal(session: WebSocketSession, signal: CallSignal) {
        // Manually construct JSON with "kind": "signal" to match frontend expectation
        val payload = """{"kind":"signal","type":"${signal.type}","senderId":${signal.senderId},"receiverId":${signal.receiverId},"payload":${json.encodeToString(signal.payload)}}"""
        session.send(Frame.Text(payload))
    }
}
