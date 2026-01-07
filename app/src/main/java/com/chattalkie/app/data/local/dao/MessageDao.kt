package com.chattalkie.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chattalkie.app.data.local.entity.MessageEntity
import com.chattalkie.app.data.local.entity.MessageStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp DESC")
    fun getMessagesByChatId(chatId: Int): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE cid = :cid LIMIT 1")
    suspend fun getMessageByCid(cid: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status, messageId = :serverMessageId WHERE cid = :cid")
    suspend fun updateStatus(cid: String, status: String, serverMessageId: String?): Int // Return updated count
    
    @Query("DELETE FROM messages WHERE cid = :cid")
    suspend fun deleteByCid(cid: String)
}
