package com.chattalkie.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chattalkie.app.data.ChatRepository
import com.chattalkie.app.data.FriendRepository
import com.chattalkie.app.data.GroupRepository
import com.chattalkie.app.ui.model.ConversationUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessagesViewModel : ViewModel() {

    private val _conversations =
        MutableStateFlow<List<ConversationUiState>>(emptyList())
    val conversations: StateFlow<List<ConversationUiState>> =
        _conversations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // 1️⃣ İlk açılışta SADECE 1 kere yükle
        loadConversations()

        // 2️⃣ Socket bağlantısı
        ChatRepository.connect()

        // 3️⃣ Realtime mesajları PATCH et (FULL reload YOK)
        observeIncomingMessages()

        // 4️⃣ Online / offline durumlarını PATCH et
        observeStatusUpdates()
    }

    // ----------------------------------------------------
    // INITIAL / MANUAL LOAD
    // ----------------------------------------------------
    fun loadConversations(force: Boolean = false) {
        if (_conversations.value.isNotEmpty() && !force) return

        viewModelScope.launch {
            _isLoading.value = true
            try {
                val friends = FriendRepository.getFriends()
                val groups = GroupRepository.getGroups()

                val friendItems = friends.map { friend ->
                    ConversationUiState(
                        id = "p_${friend.id}",
                        partnerName = friend.name,
                        lastMessage = "", // Initial load doesn't fetch messages yet. TODO: Fetch last message
                        timestamp = "",
                        isGroup = false,
                        isOnline = friend.status == "online"
                    )
                }

                val groupItems = groups.map { group ->
                    ConversationUiState(
                        id = "g_${group.id}",
                        partnerName = group.name,
                        lastMessage = "Grup Sohbeti",
                        timestamp = "",
                        isGroup = true,
                        role = group.myRole,
                         isOnline = false
                    )
                }

                _conversations.value =
                    (groupItems + friendItems)
                        .sortedBy { it.partnerName }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ----------------------------------------------------
    // REALTIME MESSAGE PATCH
    // ----------------------------------------------------
    private fun observeIncomingMessages() {
        viewModelScope.launch {
            ChatRepository.messages.collect { message ->
                // Derive conversation ID
                val conversationId = if (message.groupId != null) "g_${message.groupId}" else "p_${message.senderId}"
                
                _conversations.update { list ->
                    list.map {
                        if (it.id == conversationId) {
                            it.copy(
                                lastMessage = when (message.messageType) {
                                    "image" -> "📷 Fotoğraf"
                                    "voice" -> "🎤 Sesli Mesaj"
                                    "video" -> "🎥 Video"
                                    "file" -> "📁 Dosya"
                                    else -> {
                                         if (message.content.isNotBlank()) message.content
                                         else if (message.mediaKey != null) {
                                              val k = message.mediaKey.lowercase()
                                              if (k.endsWith(".m4a") || k.endsWith(".mp3") || k.endsWith(".wav") || k.endsWith(".webm")) "🎤 Sesli Mesaj"
                                              else "📷 Fotoğraf"
                                         }
                                         else ""
                                    }
                                },
                                timestamp = message.timestamp.toString() // Long -> String
                            )
                        } else it
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // REALTIME STATUS PATCH
    // ----------------------------------------------------
    private fun observeStatusUpdates() {
        viewModelScope.launch {
            ChatRepository.statusUpdates.collect { status ->
                _conversations.update { list ->
                    list.map {
                        if (it.id == "p_${status.userId}") {
                            it.copy(
                                isOnline = status.status == "online"
                            )
                        } else it
                    }
                }
            }
        }
    }

    // ----------------------------------------------------
    // JOIN GROUP BY INVITE
    // ----------------------------------------------------
    fun joinGroup(
        rawInput: String,
        onResult: (String) -> Unit
    ) {
        val token = rawInput
            .replace("chattalkie://join/", "")
            .replace("https://chattalkie.com/join/", "")
            .replace("https://chattalkie.app/join/", "")
            .trim()

        viewModelScope.launch {
            try {
                val response = GroupRepository.joinGroupByInvite(token)
                if (response != null) {
                    onResult(response.message ?: "Gruba katıldı")
                    // 🔥 Bilinçli ve kontrollü reload
                    loadConversations(force = true)
                } else {
                    onResult("Katılma başarısız")
                }
            } catch (e: Exception) {
                onResult("Hata: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Eğer ChatRepository global değilse:
        // ChatRepository.disconnect()
    }
}
