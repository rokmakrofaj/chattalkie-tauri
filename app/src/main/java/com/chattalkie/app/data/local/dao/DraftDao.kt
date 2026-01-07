package com.chattalkie.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.chattalkie.app.data.local.entity.DraftEntity

@Dao
interface DraftDao {
    @Query("SELECT * FROM drafts WHERE chatId = :chatId")
    suspend fun getDraft(chatId: Int): DraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDraft(draft: DraftEntity)

    @Query("DELETE FROM drafts WHERE chatId = :chatId")
    suspend fun deleteDraft(chatId: Int)
}
