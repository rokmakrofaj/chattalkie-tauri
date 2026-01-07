package com.chattalkie.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chattalkie.app.domain.model.Message
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.UUID

import com.chattalkie.app.data.ChatRepository
import com.chattalkie.app.data.AuthRepository
import com.chattalkie.app.data.GroupRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private var targetId: Int = -1
    private var partnerName: String = ""
    private var isGroupChat: Boolean = false

    private val _partnerStatus = MutableStateFlow("offline")
    val partnerStatus: StateFlow<String> = _partnerStatus.asStateFlow()

    private var currentPresence = "offline"
    private var typingJob: kotlinx.coroutines.Job? = null

    private val _groupCreatedAt = MutableStateFlow<Long?>(null)
    val groupCreatedAt: StateFlow<Long?> = _groupCreatedAt.asStateFlow()

    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private var draftSaveJob: kotlinx.coroutines.Job? = null

    fun initChat(id: Int, name: String, isGroup: Boolean = false) {
        this.targetId = id
        this.partnerName = name
        this.isGroupChat = isGroup
        
        // Observe messages from offline-first repository
        viewModelScope.launch {
            ChatRepository.getMessages(id)?.collect { entities ->
                val mappedMessages = entities.map { entity ->
                    Message(
                        id = entity.messageId ?: entity.cid, // Use server ID if available, else CID
                        cid = entity.cid, // Mapping real internal CID
                        userId = entity.senderId.toString(),
                        text = entity.content,
                        createdAt = entity.timestamp,
                        isMe = entity.isMine,
                        senderName = entity.senderName ?: (if (entity.isMine) "Ben" else (if (isGroup) "Kullanıcı ${entity.senderId}" else partnerName)),
                        timestamp = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entity.timestamp)),
                        status = when (entity.status) {
                            com.chattalkie.app.data.local.entity.MessageStatus.SENDING -> com.chattalkie.app.domain.model.MessageStatus.SENDING
                            com.chattalkie.app.data.local.entity.MessageStatus.SENT -> com.chattalkie.app.domain.model.MessageStatus.SENT
                            com.chattalkie.app.data.local.entity.MessageStatus.SYNCED -> com.chattalkie.app.domain.model.MessageStatus.SYNCED
                            com.chattalkie.app.data.local.entity.MessageStatus.FAILED -> com.chattalkie.app.domain.model.MessageStatus.FAILED
                             else -> com.chattalkie.app.domain.model.MessageStatus.SENT
                        },
                        mediaKey = entity.mediaKey,
                        messageType = when (entity.messageType) {
                            "image" -> com.chattalkie.app.domain.model.MessageType.IMAGE
                            "voice" -> com.chattalkie.app.domain.model.MessageType.VOICE
                            "file" -> com.chattalkie.app.domain.model.MessageType.FILE
                            "video" -> com.chattalkie.app.domain.model.MessageType.VIDEO
                            else -> if (entity.mediaKey != null) {
                                val key = entity.mediaKey.lowercase()
                                if (key.endsWith(".m4a") || key.endsWith(".mp3") || key.endsWith(".wav") || key.endsWith(".ogg") || key.endsWith(".webm")) {
                                    com.chattalkie.app.domain.model.MessageType.VOICE
                                } else {
                                    com.chattalkie.app.domain.model.MessageType.IMAGE
                                }
                            } else {
                                com.chattalkie.app.domain.model.MessageType.TEXT
                            }
                        }
                    )
                }
                _messages.value = mappedMessages
            }
        }
        
        // Partner/Grup durumunu çek
        if (!isGroup) {
            viewModelScope.launch {
                val status = ChatRepository.getUserStatus(id)
                currentPresence = status
                _partnerStatus.value = status
            }
        } else {
            _partnerStatus.value = "Grup Sohbeti"
        }

        if (isGroup) {
            viewModelScope.launch {
                try {
                    val group = GroupRepository.getGroup(id)
                    _groupCreatedAt.value = group?.createdAt
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Load Draft
        viewModelScope.launch {
            val savedDraft = ChatRepository.getDraft(id)
            if (savedDraft != null) {
                _draft.value = savedDraft
            }
        }
    }

    init {
        viewModelScope.launch {
            launch {
                ChatRepository.statusUpdates.collect { update ->
                    if (!isGroupChat && update.userId == targetId) {
                        currentPresence = update.status
                        if (typingJob?.isActive != true) {
                            _partnerStatus.value = update.status
                        }
                    }
                }
            }
            launch {
                ChatRepository.typingUpdates.collect { update ->
                    if (update.chatId == targetId) {
                        typingJob?.cancel()
                        _partnerStatus.value = "Yazıyor..."
                        typingJob = launch {
                            delay(3000)
                            _partnerStatus.value = if (isGroupChat) "Grup Sohbeti" else currentPresence
                        }
                    }
                }
            }
        }
    }
    
    private var lastTypingTime = 0L

    fun onDraftChanged(newDraft: String) {
        _draft.value = newDraft
        
        // Typing Indicator Throttle (3s)
        val now = System.currentTimeMillis()
        if (now - lastTypingTime > 3000 && targetId != -1) {
            lastTypingTime = now
            viewModelScope.launch {
                ChatRepository.sendTyping(targetId, isGroupChat)
            }
        }
        
        draftSaveJob?.cancel()
        draftSaveJob = viewModelScope.launch {
            delay(500) // Debounce 500ms
            if (targetId != -1) {
                ChatRepository.saveDraft(targetId, newDraft)
            }
        }
    }
    
    fun markMessageAsRead(message: Message) {
        if (message.isMe || message.status == com.chattalkie.app.domain.model.MessageStatus.READ) return
        
        viewModelScope.launch {
            ChatRepository.sendReadReceipt(
                cid = message.cid,
                messageId = message.id, // Domain model maps id to messageId if available
                senderId = message.userId.toIntOrNull() ?: 0,
                isGroup = isGroupChat,
                groupId = if (isGroupChat) targetId else null
            )
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank() || targetId == -1) return

        val mId = UUID.randomUUID().toString()
        
        // Repository handles: 1. DB Insert (Optimistic) -> 2. Network Call
        if (isGroupChat) {
            ChatRepository.sendMessage(text.trim(), messageId = mId, groupId = targetId)
        } else {
            ChatRepository.sendMessage(text.trim(), recipientId = targetId, messageId = mId)
        }
        
        // Clear Draft
        _draft.value = ""
        viewModelScope.launch {
             ChatRepository.deleteDraft(targetId)
        }
    }
    
    fun resendMessage(cid: String) {
        ChatRepository.resendMessage(cid)
    }



    fun sendImage(file: java.io.File) {
        if (targetId == -1) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (isGroupChat) {
                ChatRepository.sendImage(file, groupId = targetId)
            } else {
                ChatRepository.sendImage(file, recipientId = targetId)
            }
        }
    }

    fun handleImageSelection(uri: android.net.Uri, context: android.content.Context) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    val file = java.io.File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
                    val outputStream = java.io.FileOutputStream(file)
                    inputStream.copyTo(outputStream)
                    inputStream.close()
                    outputStream.close()
                    
                    sendImage(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendVoice(file: java.io.File) {
        if (targetId == -1) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            if (isGroupChat) {
                ChatRepository.sendVoice(file, groupId = targetId)
            } else {
                ChatRepository.sendVoice(file, recipientId = targetId)
            }
        }
    }
}
