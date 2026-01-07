package com.chattalkie.app.data

import com.chattalkie.app.data.remote.WebSocketManager
import com.chattalkie.app.data.remote.model.MessageResponse
import com.chattalkie.app.data.remote.NetworkModule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

import com.chattalkie.app.data.remote.model.ChatWsMessage
import com.chattalkie.app.data.remote.model.StatusUpdateWsMessage
import okhttp3.MediaType.Companion.toMediaTypeOrNull

object ChatRepository {
    private var webSocketManager: WebSocketManager? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var messageDao: com.chattalkie.app.data.local.dao.MessageDao? = null
    private var draftDao: com.chattalkie.app.data.local.dao.DraftDao? = null
    private var chatDao: com.chattalkie.app.data.local.dao.ChatDao? = null
    
    // UI observes this flow (Source of Truth is local DB)
    fun getMessages(chatId: Int): kotlinx.coroutines.flow.Flow<List<com.chattalkie.app.data.local.entity.MessageEntity>>? {
        return messageDao?.getMessagesByChatId(chatId)
    }

    private val _statusUpdates = MutableSharedFlow<StatusUpdateWsMessage>(replay = 1)
    val statusUpdates = _statusUpdates.asSharedFlow()
    
    private val _typingUpdates = MutableSharedFlow<com.chattalkie.app.data.remote.model.TypingWsMessage>(replay = 0)
    val typingUpdates = _typingUpdates.asSharedFlow()

    // For MessagesViewModel to observe new messages and updates
    private val _incomingMessages = MutableSharedFlow<MessageResponse>(replay = 0)
    val messages = _incomingMessages.asSharedFlow()


    fun initialize(context: android.content.Context) {
        val db = com.chattalkie.app.data.local.DatabaseModule.getDatabase(context)
        messageDao = db.messageDao()
        draftDao = db.draftDao()
        chatDao = db.chatDao()
    }

    suspend fun saveDraft(chatId: Int, content: String) {
        if (content.isBlank()) {
            draftDao?.deleteDraft(chatId)
        } else {
            draftDao?.saveDraft(com.chattalkie.app.data.local.entity.DraftEntity(chatId, content))
        }
    }

    suspend fun getDraft(chatId: Int): String? {
        return draftDao?.getDraft(chatId)?.content
    }

    suspend fun deleteDraft(chatId: Int) {
        draftDao?.deleteDraft(chatId)
    }

    fun resendMessage(cid: String) {
        scope.launch {
            val message = messageDao?.getMessageByCid(cid) ?: return@launch
            val chat = chatDao?.getChatById(message.chatId) ?: return@launch
            
            // 1. Update status to SENDING
            messageDao?.updateStatus(cid, com.chattalkie.app.data.local.entity.MessageStatus.SENDING.name, null)
            
            // 2. Retry Send via WS
            try {
                val isGroup = chat.type == "GROUP"
                webSocketManager?.sendMessage(
                    content = message.content,
                    cid = cid, // REUSE CID
                    recipientId = if (!isGroup) message.chatId else null,
                    groupId = if (isGroup) message.chatId else null,
                    messageId = java.util.UUID.randomUUID().toString(),
                    mediaKey = message.mediaKey,
                    messageType = message.messageType
                )
            } catch (e: Exception) {
                e.printStackTrace()
                messageDao?.updateStatus(cid, com.chattalkie.app.data.local.entity.MessageStatus.FAILED.name, null)
            }
        }
    }

    fun connect() {
        if (webSocketManager != null) return
        val token = AuthRepository.token
        if (token != null) {
            webSocketManager = WebSocketManager(token)
            webSocketManager?.connect()
            
            scope.launch {
                webSocketManager?.incomingMessages?.collectLatest { message ->
                    when (message) {
                        is ChatWsMessage -> {
                            // Incoming new message from WS (Real-time)
                            // Insert into DB. ID is from server.
                            val currentUser = AuthRepository.currentUser.value
                            val isMe = currentUser?.id == message.senderId

                            val entity = com.chattalkie.app.data.local.entity.MessageEntity(
                                cid = message.cid ?: message.messageId, // Fallback to messageId
                                messageId = message.messageId,
                                chatId = message.groupId ?: message.recipientId ?: message.senderId, // Try recipientId for Echo messages
                                senderId = message.senderId,
                                senderName = message.senderName,
                                content = message.content,
                                mediaKey = message.mediaKey,
                                messageType = message.messageType,
                                timestamp = message.timestamp,
                                status = com.chattalkie.app.data.local.entity.MessageStatus.SYNCED,
                                isMine = isMe
                            )
                            messageDao?.insertMessage(entity)
                            
                            // Notify listeners (MessagesViewModel)
                            val response = MessageResponse(
                                messageId = message.messageId,
                                cid = message.cid,
                                senderId = message.senderId,
                                senderName = message.senderName,
                                content = message.content,
                                mediaKey = message.mediaKey,
                                messageType = message.messageType,
                                timestamp = message.timestamp,
                                groupId = message.groupId
                            )
                            _incomingMessages.emit(response)
                        }
                        is com.chattalkie.app.data.remote.model.AckWsMessage -> {
                            // ACK from server
                            android.util.Log.d("ChatRepo", "Received ACK for cid: ${message.cid}, id: ${message.id}")
                            if (message.cid != null) {
                                val updatedCount = messageDao?.updateStatus(
                                    cid = message.cid, 
                                    status = com.chattalkie.app.data.local.entity.MessageStatus.SENT.name,
                                    serverMessageId = message.id
                                )
                                android.util.Log.d("ChatRepo", "Updated DB status to SENT for cid: ${message.cid}. Rows affected: $updatedCount")
                            } else {
                                android.util.Log.w("ChatRepo", "ACK received with null CID")
                            }
                        }
                        is StatusUpdateWsMessage -> {
                            _statusUpdates.emit(message)
                        }
                        is com.chattalkie.app.data.remote.model.TypingWsMessage -> {
                            _typingUpdates.emit(message)
                        }
                    }
                }
            }
        }
    }

    fun sendMessage(content: String, recipientId: Int? = null, messageId: String, groupId: Int? = null, mediaKey: String? = null, messageType: String? = null) {
        val currentUser = AuthRepository.currentUser.value ?: return // Don't send if not logged in
        
        // 1. Generate CID
        val cid = java.util.UUID.randomUUID().toString()
        
        // 2. Insert to DB as SENDING
        val myId = currentUser.id
        val entity = com.chattalkie.app.data.local.entity.MessageEntity(
            cid = cid,
            messageId = null,
            chatId = groupId ?: recipientId ?: 0,
            senderId = myId, 
            content = content,
            timestamp = System.currentTimeMillis(),
            status = com.chattalkie.app.data.local.entity.MessageStatus.SENDING,
            isMine = true,
            mediaKey = mediaKey,
            messageType = messageType
        )
        
        scope.launch {
            // Paralel execute to prevent DB blocking WS
            launch { 
                try {
                    messageDao?.insertMessage(entity)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            launch { 
                try {
                    webSocketManager?.sendMessage(content, cid, recipientId, messageId, groupId, mediaKey, messageType)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun sendImage(file: java.io.File, recipientId: Int? = null, groupId: Int? = null) {
        val token = AuthRepository.token ?: return
        try {
            // 1. Upload
            val requestFile = okhttp3.RequestBody.create("image/*".toMediaTypeOrNull(), file)
            val body = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)
            
            val response = NetworkModule.chatService.uploadMedia("Bearer $token", body)
            val key = response["key"] ?: throw Exception("Upload failed: No key returned")
            
            // 2. Send Message via WS with Key
            val mId = java.util.UUID.randomUUID().toString()
            sendMessage("", recipientId, mId, groupId, key, "image")
            
        } catch (e: Exception) {
            e.printStackTrace()
            // TODO: Handle upload error UI
        }
    }

    suspend fun sendVoice(file: java.io.File, recipientId: Int? = null, groupId: Int? = null) {
        val token = AuthRepository.token ?: return
        try {
            // 1. Upload
            val requestFile = okhttp3.RequestBody.create("audio/*".toMediaTypeOrNull(), file)
            val body = okhttp3.MultipartBody.Part.createFormData("file", file.name, requestFile)
            
            val response = NetworkModule.chatService.uploadMedia("Bearer $token", body)
            val key = response["key"] ?: throw Exception("Upload failed: No key returned")
            
            // 2. Send Message via WS with Key
            val mId = java.util.UUID.randomUUID().toString()
            sendMessage("", recipientId, mId, groupId, key, "voice")
            
        } catch (e: Exception) {
            e.printStackTrace()
            // TODO: Handle upload error UI (e.g. expose SharedFlow<Error>)
        }
    }

    // Deprecated for direct use, prefer observing getMessages()
    suspend fun fetchRemoteHistory(partnerId: Int) {
        val token = AuthRepository.token ?: return
        try {
            val response = NetworkModule.chatService.getChatHistory("Bearer $token", partnerId)
            // Convert to Entities and Insert
            // ... Logic duplicated with SyncRepository.Ideally call SyncRepository or share logic.
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getUserStatus(userId: Int): String {
        val token = AuthRepository.token ?: return "offline"
        return try {
            val user = NetworkModule.userService.getUserDetail("Bearer $token", userId)
            user.status ?: "offline"
        } catch (e: Exception) {
            e.printStackTrace()
            "offline"
        }
    }
    
    fun sendTyping(chatId: Int, isGroup: Boolean) {
        val currentUser = AuthRepository.currentUser.value ?: return
        webSocketManager?.sendTyping(currentUser.id, chatId, isGroup, true) // True for typing start. Logic for stop?
        // Usually we send true, then false after timeout? Or simple "pulse"?
        // Desktop code sends "isTyping".
        // ViewModel handles throttle.
    }

    fun sendReadReceipt(cid: String?, messageId: String?, senderId: Int, isGroup: Boolean, groupId: Int?) {
        val currentUser = AuthRepository.currentUser.value ?: return
        val currentCid = cid ?: return // Need at least CID
        
        webSocketManager?.sendReadReceipt(
            userId = currentUser.id,
            cid = currentCid,
            messageId = messageId,
            senderId = senderId,
            isGroup = isGroup,
            groupId = groupId
        )
    }

    fun disconnect() {
        webSocketManager?.close()
        webSocketManager = null
    }
}
