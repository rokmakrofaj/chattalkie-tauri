package com.chattalkie.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.chattalkie.app.data.local.dao.ChatDao
import com.chattalkie.app.data.local.dao.MessageDao
import com.chattalkie.app.data.local.dao.UserDao
import com.chattalkie.app.data.local.entity.ChatEntity
import com.chattalkie.app.data.local.entity.MessageEntity
import com.chattalkie.app.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class, 
        ChatEntity::class, 
        MessageEntity::class,
        com.chattalkie.app.data.local.entity.DraftEntity::class
    ],
    version = 2
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun draftDao(): com.chattalkie.app.data.local.dao.DraftDao
}
