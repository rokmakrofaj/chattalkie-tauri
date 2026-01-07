package com.chattalkie.app.data.repository

import android.content.Context
import android.util.Log
import com.chattalkie.app.data.local.AppDatabase
import com.chattalkie.app.data.local.DatabaseModule
import com.chattalkie.app.data.local.entity.MessageEntity
import com.chattalkie.app.data.local.entity.MessageStatus
import com.chattalkie.app.data.remote.NetworkModule
import com.chattalkie.app.data.remote.model.MessageResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncRepository(private val context: Context) {
    private val messageDao = DatabaseModule.getDatabase(context).messageDao()
    private val chatService = NetworkModule.chatService
    
    // In a real app, store this in DataStore or SharedPreferences
    private var lastSyncTs: Long = 0 

    suspend fun performSync(token: String): Unit = withContext(Dispatchers.IO) {
        try {
            Log.d("SyncRepository", "Starting sync from ts: $lastSyncTs")
            val response = chatService.getSync("Bearer $token", lastSyncTs)
            
            // Get current user ID to determine isMine
            val currentUserId = com.chattalkie.app.data.AuthRepository.currentUser.value?.id ?: 0
            
            // 1. Process Messages (Upsert)
            val newMessages = response.messages.map { it.toEntity(currentUserId) }
            
            if (newMessages.isNotEmpty()) {
                messageDao.insertMessages(newMessages)
                Log.d("SyncRepository", "Inserted ${newMessages.size} messages")
            }
            
            // 2. Process Tombstones (Delete)
            response.tombstones.forEach { tombstone ->
                if (tombstone.type == "MESSAGE") {
                    messageDao.deleteByCid(tombstone.itemId)
                }
            }
            
            // 3. Update Cursor
            lastSyncTs = response.lastTs
            
            // 4. Recurse if hasMore
            if (response.hasMore) {
                performSync(token)
            }
            
        } catch (e: Exception) {
            Log.e("SyncRepository", "Sync failed", e)
        }
    }
    
    private fun MessageResponse.toEntity(currentUserId: Int): MessageEntity {
        val isMine = senderId == currentUserId
        
        // Chat ID Logic:
        // Group: groupId
        // Direct (Mine): receiverId (Partner)
        // Direct (Theirs): senderId (Partner)
        val chatId = if (groupId != null) {
            groupId
        } else {
            if (isMine) (receiverId ?: senderId) else senderId
        }

        return MessageEntity(
            cid = cid ?: messageId,
            messageId = messageId,
            chatId = chatId,
            senderId = senderId,
            content = content,
            mediaKey = mediaKey,
            messageType = messageType,
            timestamp = timestamp,
            status = MessageStatus.SYNCED,
            isMine = isMine
        )
    }
}
