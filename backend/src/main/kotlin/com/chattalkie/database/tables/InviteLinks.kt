package com.chattalkie.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.timestamp
import java.time.Instant

/**
 * InviteLinks table - stores group invitation tokens
 * PK: id (auto-increment)
 * Unique: token
 * Business rule: One active link per group (old links deleted on creation)
 */
object InviteLinks : IntIdTable("invite_links") {
    val groupId = reference("group_id", Groups, onDelete = ReferenceOption.CASCADE)
    val token = varchar("token", 64).uniqueIndex("idx_invite_links_token")
    val createdBy = reference("created_by", Users, onDelete = ReferenceOption.SET_NULL).nullable()
    val createdAt = timestamp("created_at").clientDefault { Instant.now() }
    val expiresAt = timestamp("expires_at").nullable()

    init {
        index("idx_invite_links_group", false, groupId) // For lookup by group
    }
}
