package com.chattalkie.repositories

import com.chattalkie.database.tables.Groups
import com.chattalkie.database.tables.Messages
import com.chattalkie.database.tables.Users
import com.chattalkie.domain.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

interface MessageRepository {
    fun findDirectMessages(userId: UserId, partnerId: UserId, limit: Int = 50, beforeId: String? = null): List<Message.DirectMessage>
    fun findGroupMessages(groupId: GroupId, limit: Int = 50, beforeId: String? = null): List<Message.GroupMessage>
    fun saveDirectMessage(senderId: UserId, recipientId: UserId, messageId: String, cid: String?, content: String, mediaKey: String? = null, messageType: String? = null): Message.DirectMessage?
    fun saveGroupMessage(senderId: UserId, groupId: GroupId, messageId: String, cid: String?, content: String, mediaKey: String? = null, messageType: String? = null): Message.GroupMessage?
}

class MessageRepositoryImpl : MessageRepository {

    override fun saveDirectMessage(senderId: UserId, recipientId: UserId, messageId: String, cid: String?, content: String, mediaKey: String?, messageType: String?): Message.DirectMessage? {
        return transaction {
            // Idempotency Check - only if cid is provided
            if (cid != null) {
                val existing = Messages.select { Messages.cid eq cid }.singleOrNull()
                if (existing != null) return@transaction toDirectMessage(existing)
            }

            Messages.insert {
                it[Messages.senderId] = EntityID(senderId.value, Users)
                it[Messages.receiverId] = EntityID(recipientId.value, Users)
                it[Messages.messageId] = messageId
                it[Messages.cid] = cid
                it[Messages.content] = content
                it[Messages.messageType] = messageType ?: (if (mediaKey != null) "image" else "text")
                it[Messages.mediaKey] = mediaKey
            }
            
            // Return the newly created message (fetched to get timestamp)
            Messages.select { Messages.messageId eq messageId }
                .single()
                .let { toDirectMessage(it) }
        }
    }

    override fun saveGroupMessage(senderId: UserId, groupId: GroupId, messageId: String, cid: String?, content: String, mediaKey: String?, messageType: String?): Message.GroupMessage? {
        return transaction {
            // Idempotency Check - only if cid is provided
            if (cid != null) {
                val existing = Messages.select { Messages.cid eq cid }.singleOrNull()
                if (existing != null) return@transaction toGroupMessage(existing)
            }

            Messages.insert {
                it[Messages.senderId] = EntityID(senderId.value, Users)
                it[Messages.groupId] = EntityID(groupId.value, Groups)
                it[Messages.messageId] = messageId
                it[Messages.cid] = cid
                it[Messages.content] = content
                it[Messages.messageType] = messageType ?: (if (mediaKey != null) "image" else "text")
                it[Messages.mediaKey] = mediaKey
            }
            
             Messages.select { Messages.messageId eq messageId }
                .single()
                .let { toGroupMessage(it) }
        }
    }

    override fun findDirectMessages(userId: UserId, partnerId: UserId, limit: Int, beforeId: String?): List<Message.DirectMessage> {
        return transaction {
            val baseQuery = Messages.join(Users, JoinType.INNER, Messages.senderId, Users.id)
                .select {
                    (Messages.senderId eq EntityID(userId.value, Users) and (Messages.receiverId eq EntityID(partnerId.value, Users))) or
                    (Messages.senderId eq EntityID(partnerId.value, Users) and (Messages.receiverId eq EntityID(userId.value, Users)))
                }
            
            // Cursor pagination (simplified, ideally uses Timestamp or Sequential ID)
            // If beforeId is provided, we'd add logic here. For now adhering to signature.
            
            baseQuery.orderBy(Messages.createdAt, SortOrder.DESC) // Latest first
                .limit(limit)
                .map { toDirectMessage(it) }
                .reversed() // Return oldest-to-newest for chat UI
        }
    }

    override fun findGroupMessages(groupId: GroupId, limit: Int, beforeId: String?): List<Message.GroupMessage> {
        return transaction {
            Messages.join(Users, JoinType.INNER, Messages.senderId, Users.id)
                .select { Messages.groupId eq EntityID(groupId.value, Groups) }
                .orderBy(Messages.createdAt, SortOrder.DESC)
                .limit(limit)
                .map { toGroupMessage(it) }
                .reversed()
        }
    }
    
    private fun toDirectMessage(row: ResultRow): Message.DirectMessage {
        val senderName = try { row[Users.name] } catch (e: Exception) { null }
        val senderAvatar = try { row[Users.avatarUrl] } catch (e: Exception) { null }
        val mediaKey = row[Messages.mediaKey]
        
        return Message.DirectMessage(
            messageId = MessageId(row[Messages.messageId]),
            cid = row[Messages.cid],
            senderId = UserId(row[Messages.senderId].value),
            senderName = senderName,
            senderAvatar = senderAvatar,
            receiverId = UserId(row[Messages.receiverId]!!.value),
            content = row[Messages.content],
            mediaKey = mediaKey,
            timestamp = row[Messages.createdAt].toEpochMilli(),
            messageType = when (row[Messages.messageType]) {
                "image" -> MessageType.IMAGE
                "voice" -> MessageType.VOICE
                "video" -> MessageType.VIDEO
                "file" -> MessageType.FILE
                else -> MessageType.TEXT
            }
        )
    }
    
    private fun toGroupMessage(row: ResultRow): Message.GroupMessage {
        val senderName = try { row[Users.name] } catch (e: Exception) { null }
        val senderAvatar = try { row[Users.avatarUrl] } catch (e: Exception) { null }
        val mediaKey = row[Messages.mediaKey]

        return Message.GroupMessage(
            messageId = MessageId(row[Messages.messageId]),
            cid = row[Messages.cid],
            senderId = UserId(row[Messages.senderId].value),
            senderName = senderName,
            senderAvatar = senderAvatar,
            groupId = GroupId(row[Messages.groupId]!!.value),
            content = row[Messages.content],
            mediaKey = mediaKey,
            timestamp = row[Messages.createdAt].toEpochMilli(),
            messageType = when (row[Messages.messageType]) {
                "image" -> MessageType.IMAGE
                "voice" -> MessageType.VOICE
                "video" -> MessageType.VIDEO
                "file" -> MessageType.FILE
                else -> MessageType.TEXT
            }
        )
    }
}
