package com.chattalkie.app.data.remote

import com.chattalkie.app.data.remote.model.WsMessage
import com.chattalkie.app.data.remote.model.ChatWsMessage
import com.chattalkie.app.data.remote.model.StatusUpdateWsMessage
import com.chattalkie.app.data.remote.model.AckWsMessage
import com.chattalkie.app.data.remote.model.TypingWsMessage
import com.google.gson.JsonObject
import com.chattalkie.app.data.remote.model.TypingRequest
import com.chattalkie.app.data.remote.model.DeliveryStatusRequest
import com.chattalkie.app.data.remote.model.MessageRequest
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.*
import java.util.concurrent.TimeUnit
import android.util.Log

class WebSocketManager(private val token: String) {
    private val client = OkHttpClient.Builder()
        .pingInterval(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()
        
    private var webSocket: WebSocket? = null
    private val gson = Gson()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _incomingMessages = MutableSharedFlow<WsMessage>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incomingMessages = _incomingMessages.asSharedFlow()

    fun connect() {
        // Use localhost for local dev if 172... doesn't work, but keep user's current IP
        val request = Request.Builder()
            .url("ws://127.0.0.1:8080/chat")
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocket", "Connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocket", "RAW: $text")
                try {
                    val jsonObj = gson.fromJson(text, JsonObject::class.java)
                    // Check for 'kind' first (new backend), fallback to 'type' if needed or legacy
                    val kind = jsonObj.get("kind")?.asString ?: jsonObj.get("type")?.asString ?: "unknown"
                    
                    val message = when (kind) {
                        "chat" -> gson.fromJson(text, ChatWsMessage::class.java)
                        "status" -> gson.fromJson(text, StatusUpdateWsMessage::class.java)
                        "ack" -> gson.fromJson(text, AckWsMessage::class.java)
                        "typing" -> gson.fromJson(text, TypingWsMessage::class.java)

                        else -> {
                            Log.e("WebSocket", "UNKNOWN KIND: $kind")
                            null
                        }
                    }
                    
                    if (message != null) {
                        scope.launch {
                            _incomingMessages.emit(message)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocket", "Closing: $code / $reason")
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Error: ${t.message}")
                t.printStackTrace()
            }
        })
    }

    fun sendMessage(content: String, cid: String, recipientId: Int? = null, messageId: String, groupId: Int? = null, mediaKey: String? = null, messageType: String? = null) {
        val request = MessageRequest(messageId, cid, content, recipientId, groupId, mediaKey, messageType)
        val json = gson.toJson(request)
        webSocket?.send(json)
    }

    fun sendTyping(senderId: Int, chatId: Int, isGroup: Boolean, isTyping: Boolean) {
        val message = TypingRequest(
            senderId = senderId,
            isTyping = isTyping,
            recipientId = if (!isGroup) chatId else null,
            groupId = if (isGroup) chatId else null
        )
        val json = gson.toJson(message)
        webSocket?.send(json)
    }

    fun sendReadReceipt(userId: Int, cid: String, messageId: String?, senderId: Int, isGroup: Boolean, groupId: Int?) {
        val payload = DeliveryStatusRequest(
            messageId = messageId,
            cid = cid,
            status = "READ",
            userId = userId,
            recipientId = senderId,
            timestamp = System.currentTimeMillis(),
            groupId = if (isGroup) groupId else null
        )
        val json = gson.toJson(payload)
        webSocket?.send(json)
    }

    fun close() {
        webSocket?.close(1000, "Goodbye")
    }
}
