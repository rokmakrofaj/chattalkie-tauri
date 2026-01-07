package com.chattalkie.services

import com.chattalkie.database.tables.DeletedItems
import com.chattalkie.database.tables.Messages
import com.chattalkie.domain.*
import com.chattalkie.repositories.MessageRepository
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

class SyncService(
    private val messageRepository: MessageRepository,
    private val groupRepository: com.chattalkie.repositories.GroupRepository,
    private val userRepository: com.chattalkie.repositories.UserRepository
) {
    
    fun getDeltaSync(userId: Int, lastTs: Long): SyncResponse {
        return transaction {
            // Get user's group IDs for filtering
            val userGroups = groupRepository.findUserGroups(UserId(userId)).map { it.first.id.value }

            // 1. Get new messages since lastTs WHERE user is participant
            // Joined with Users to get senderName
            val messages = Messages.join(
                com.chattalkie.database.tables.Users,
                JoinType.INNER,
                onColumn = Messages.senderId,
                otherColumn = com.chattalkie.database.tables.Users.id
            )
                .slice(Messages.columns + com.chattalkie.database.tables.Users.name + com.chattalkie.database.tables.Users.avatarUrl)
                .selectAll()
                .where { 
                    val timeFilter = Messages.createdAt greaterEq Instant.ofEpochMilli(lastTs)
                    val directMsgFilter = (Messages.senderId eq userId) or (Messages.receiverId eq userId)
                    
                    if (userGroups.isNotEmpty()) {
                        timeFilter and (directMsgFilter or (Messages.groupId inList userGroups))
                    } else {
                        timeFilter and directMsgFilter
                    }
                }
                .orderBy(Messages.createdAt, SortOrder.ASC)
                .mapNotNull { toMessage(it) }

            // 2. Get tombstones since lastTs
            val tombstones = DeletedItems
                .selectAll()
                .where { DeletedItems.deletedAt greaterEq Instant.ofEpochMilli(lastTs) }
                .map { 
                     Tombstone(
                         id = it[DeletedItems.itemId],
                         type = TombstoneType.valueOf(it[DeletedItems.itemType]),
                         deletedAt = it[DeletedItems.deletedAt].toEpochMilli()
                     )
                }
                
            // 3. Determine nextTs (max of result or current)
            val nextTs = System.currentTimeMillis()
            
            SyncResponse(
                messages = messages,
                chats = emptyList(), // Chats sync logic omitted for brevity
                tombstones = tombstones,
                nextTs = nextTs
            )
        }
    }

    private fun toMessage(row: ResultRow): Message? {
        val msgId = MessageId(row[Messages.messageId])
        val sender = UserId(row[Messages.senderId].value)
        val senderName = row[com.chattalkie.database.tables.Users.name]
        val senderAvatar = row[com.chattalkie.database.tables.Users.avatarUrl]
        val cid = row[Messages.cid]
        val content = row[Messages.content]
        val mediaKey = row[Messages.mediaKey]
        val ts = row[Messages.createdAt].toEpochMilli()
        val typeStr = row[Messages.messageType]
        var messageType = when (typeStr.lowercase()) {
            "voice" -> MessageType.VOICE
            "image" -> MessageType.IMAGE
            "video" -> MessageType.VIDEO
            "file" -> MessageType.FILE
            else -> MessageType.TEXT
        }

        if (mediaKey != null) {
            val key = mediaKey.lowercase()
            if (key.endsWith(".m4a") || key.endsWith(".mp3") || key.endsWith(".wav") || key.endsWith(".ogg") || key.endsWith(".webm")) {
                messageType = MessageType.VOICE
            } else if (messageType == MessageType.TEXT) {
                messageType = MessageType.IMAGE
            }
        }
        
        println("DEBUG: SyncService toMessage id=${msgId.value} mediaKey=$mediaKey type=$messageType")

        val groupIdVal = row[Messages.groupId]?.value
        val receiverIdVal = row[Messages.receiverId]?.value

        return if (groupIdVal != null) {
            Message.GroupMessage(
                messageId = msgId,
                cid = cid,
                senderId = sender,
                senderName = senderName,
                senderAvatar = senderAvatar,
                groupId = GroupId(groupIdVal),
                content = content,
                timestamp = ts,
                messageType = messageType,
                mediaKey = mediaKey
            )
        } else if (receiverIdVal != null) {
             Message.DirectMessage(
                messageId = msgId,
                cid = cid,
                senderId = sender,
                senderName = senderName,
                senderAvatar = senderAvatar,
                receiverId = UserId(receiverIdVal),
                content = content,
                timestamp = ts,
                messageType = messageType,
                mediaKey = mediaKey
            )
        } else {
            // Malformed message (neither group nor receiver)
            println("SyncService: Skipping malformed message ${msgId.value} (No group or receiver)")
            null
        }
    }
}
