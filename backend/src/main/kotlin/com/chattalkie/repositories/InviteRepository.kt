package com.chattalkie.repositories

import com.chattalkie.database.tables.GroupMembers
import com.chattalkie.database.tables.Groups
import com.chattalkie.database.tables.InviteLinks
import com.chattalkie.database.tables.Users
import com.chattalkie.domain.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

interface InviteRepository {
    fun createInviteLink(groupId: GroupId, creatorId: UserId): InviteLink
    fun findByGroupId(groupId: GroupId): InviteLink?
    fun findByToken(token: InviteToken): InviteLink?
    fun deleteByGroupId(groupId: GroupId)
    // Removed addMember from here, strictly keeping it repository specific or assume it belongs here? 
    // Wait, InviteService uses it to join group. 
    // Ideally InviteRepository shouldn't write to GroupMembers. But to keep refactor safe I will keep it in Impl or Interface.
    fun addMemberToGroup(groupId: GroupId, userId: UserId, role: MemberRole)
}

class InviteRepositoryImpl : InviteRepository {

    override fun createInviteLink(groupId: GroupId, creatorId: UserId): InviteLink {
        return transaction {
            // Delete old links
            InviteLinks.deleteWhere { 
                Op.build { InviteLinks.groupId eq EntityID(groupId.value, Groups) }
            }
            
            val newToken = UUID.randomUUID().toString().replace("-", "").take(16)
            val insertedId = InviteLinks.insertAndGetId {
                it[InviteLinks.groupId] = EntityID(groupId.value, Groups)
                it[InviteLinks.token] = newToken
                it[InviteLinks.createdBy] = EntityID(creatorId.value, Users)
            }
            
            val group = Groups.select { Groups.id eq EntityID(groupId.value, Groups) }.single()
            
            InviteLink(
                id = insertedId.value,
                groupId = groupId,
                token = InviteToken(newToken),
                createdBy = creatorId,
                createdAt = group[Groups.createdAt].toEpochMilli(),
                expiresAt = null,
                groupName = group[Groups.name]
            )
        }
    }

    override fun findByGroupId(groupId: GroupId): InviteLink? {
        return transaction {
            (InviteLinks innerJoin Groups)
                .select { InviteLinks.groupId eq EntityID(groupId.value, Groups) }
                .singleOrNull()
                ?.let { row ->
                    InviteLink(
                        id = row[InviteLinks.id].value,
                        groupId = GroupId(row[InviteLinks.groupId].value),
                        token = InviteToken(row[InviteLinks.token]),
                        createdBy = row[InviteLinks.createdBy]?.let { UserId(it.value) } ?: UserId(0),
                        createdAt = row[InviteLinks.createdAt].toEpochMilli(),
                        expiresAt = row[InviteLinks.expiresAt]?.toEpochMilli(),
                        groupName = row[Groups.name]
                    )
                }
        }
    }

    override fun findByToken(token: InviteToken): InviteLink? {
        return transaction {
            (InviteLinks innerJoin Groups)
                .select { InviteLinks.token eq token.value }
                .singleOrNull()
                ?.let { row ->
                    InviteLink(
                        id = row[InviteLinks.id].value,
                        groupId = GroupId(row[InviteLinks.groupId].value),
                        token = InviteToken(row[InviteLinks.token]),
                        createdBy = row[InviteLinks.createdBy]?.let { UserId(it.value) } ?: UserId(0),
                        createdAt = row[InviteLinks.createdAt].toEpochMilli(),
                        expiresAt = row[InviteLinks.expiresAt]?.toEpochMilli(),
                        groupName = row[Groups.name]
                    )
                }
        }
    }

    override fun deleteByGroupId(groupId: GroupId) {
        transaction {
            InviteLinks.deleteWhere {
                Op.build { InviteLinks.groupId eq EntityID(groupId.value, Groups) }
            }
        }
    }

    override fun addMemberToGroup(groupId: GroupId, userId: UserId, role: MemberRole) {
        transaction {
            GroupMembers.insert {
                it[GroupMembers.groupId] = EntityID(groupId.value, Groups)
                it[GroupMembers.userId] = EntityID(userId.value, Users)
                it[GroupMembers.role] = role.name.lowercase()
            }
        }
    }
}
