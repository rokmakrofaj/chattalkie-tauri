package com.chattalkie.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * Users table - stores user accounts
 * PK: id (auto-increment)
 * Unique: username
 */
object Users : IntIdTable("users") {
    val username = varchar("username", 50).uniqueIndex("idx_users_username")
    val name = varchar("name", 100)
    val passwordHash = varchar("password_hash", 255)
    val avatarUrl = varchar("avatar_url", 255).nullable()
    val status = varchar("status", 20).default("offline")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val updatedAt = timestamp("updated_at").clientDefault { Instant.now() }
}

/**
 * Messages table - stores chat messages (direct and group)
 * PK: id (auto-increment)
 * Business rule: Either receiverId OR groupId must be non-null (XOR)
 * Indexes: sender_id, receiver_id, group_id for query performance
 */
object Messages : IntIdTable("messages") {
    val senderId = reference("sender_id", Users, onDelete = ReferenceOption.CASCADE)
    val receiverId = reference("receiver_id", Users, onDelete = ReferenceOption.SET_NULL).nullable()
    val groupId = reference("group_id", Groups, onDelete = ReferenceOption.CASCADE).nullable()
    val messageId = varchar("message_id", 36).uniqueIndex("idx_messages_message_id") // UUID (Server ID)
    val cid = varchar("cid", 64).nullable().uniqueIndex("idx_messages_cid") // Client ID (Idempotency) - nullable for legacy messages
    val content = text("content")
    val messageType = varchar("message_type", 20).default("text")
    val mediaUrl = varchar("media_url", 255).nullable()
    val mediaKey = varchar("media_key", 255).nullable() // Added for durable persistence
    val isRead = bool("is_read").default(false)
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    init {
        // Composite indexes for common query patterns
        index("idx_messages_sender_receiver", false, senderId, receiverId)
        index("idx_messages_group", false, groupId, createdAt)
    }
}

/**
 * DeletedItems table - Tombstones for sync
 * Stores IDs of deleted entities (messages, chats) so clients can process deletions during sync.
 */
object DeletedItems : IntIdTable("deleted_items") {
    val itemId = varchar("item_id", 64) // ID of the deleted item (UUID or Int as String)
    val itemType = varchar("item_type", 20) // MESSAGE, CHAT, GROUP
    val deletedAt = timestamp("deleted_at").clientDefault { Instant.now() }

    init {
        index("idx_deleted_items_sync", false, deletedAt)
    }
}

/**
 * Friends table - stores friendship relationships
 * PK: id (auto-increment)
 * Unique: (user_id, friend_id) pair
 * Note: Relationship is directional - userId sends request to friendId
 */
object Friends : IntIdTable("friends") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val friendId = reference("friend_id", Users, onDelete = ReferenceOption.CASCADE)
    val status = varchar("status", 20).default("pending") // pending, accepted, blocked
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    init {
        uniqueIndex("idx_friends_user_friend", userId, friendId)
    }
}

/**
 * Groups table - stores group/chat room definitions
 * PK: id (auto-increment)
 */
object Groups : IntIdTable("groups") {
    val name = varchar("name", 100)
    val type = varchar("type", 20).default("private") // private, public
    val createdBy = reference("created_by", Users, onDelete = ReferenceOption.SET_NULL).nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    init {
        index("idx_groups_created_by", false, createdBy)
    }
}

/**
 * GroupMembers table - junction table for group membership
 * PK: id (auto-increment)
 * Unique: (group_id, user_id) pair
 */
object GroupMembers : IntIdTable("group_members") {
    val groupId = reference("group_id", Groups, onDelete = ReferenceOption.CASCADE)
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val role = varchar("role", 20).default("member") // admin, member
    val joinedAt = timestamp("joined_at").clientDefault { Instant.now() }

    init {
        uniqueIndex("idx_group_members_group_user", groupId, userId)
        index("idx_group_members_user", false, userId) // For "my groups" query
    }
}

/**
 * Sessions table - stores active user sessions (optional, JWT can be stateless)
 * PK: id (auto-increment)
 * Unique: token
 */
object Sessions : IntIdTable("sessions") {
    val userId = reference("user_id", Users, onDelete = ReferenceOption.CASCADE)
    val token = varchar("token", 255).uniqueIndex("idx_sessions_token")
    val expiresAt = timestamp("expires_at")
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }

    init {
        index("idx_sessions_user", false, userId) // For session cleanup by user
        index("idx_sessions_expires", false, expiresAt) // For expired session cleanup
    }
}
