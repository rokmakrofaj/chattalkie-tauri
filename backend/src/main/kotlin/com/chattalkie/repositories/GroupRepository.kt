package com.chattalkie.repositories

import com.chattalkie.database.tables.GroupMembers
import com.chattalkie.database.tables.Groups
import com.chattalkie.database.tables.InviteLinks
import com.chattalkie.database.tables.Messages
import com.chattalkie.database.tables.Users
import com.chattalkie.domain.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
interface GroupRepository {
    fun createGroup(name: String, creatorId: UserId, memberIds: List<UserId>): GroupId
    fun findUserGroups(userId: UserId): List<Pair<Group, MemberRole>>
    fun findGroupMembers(groupId: GroupId): List<GroupMembership>
    fun isUserAdmin(groupId: GroupId, userId: UserId): Boolean
    fun isUserMember(groupId: GroupId, userId: UserId): Boolean
    fun removeMember(groupId: GroupId, userId: UserId): Boolean
    fun deleteGroup(groupId: GroupId): Boolean
    fun findGroup(groupId: GroupId): Group?
    fun addMember(groupId: GroupId, userId: UserId): Boolean
}

class GroupRepositoryImpl : GroupRepository {

    override fun createGroup(name: String, creatorId: UserId, memberIds: List<UserId>): GroupId {
        return transaction {
            val gId = Groups.insertAndGetId {
                it[Groups.name] = name
                it[Groups.createdBy] = EntityID(creatorId.value, Users)
            }

            // Add creator as member
            GroupMembers.insert {
                it[GroupMembers.groupId] = gId
                it[GroupMembers.userId] = EntityID(creatorId.value, Users)
                it[GroupMembers.role] = MemberRole.ADMIN.name.lowercase()
            }

            // Add other members
            for (mId in memberIds) {
                GroupMembers.insert {
                    it[GroupMembers.groupId] = gId
                    it[GroupMembers.userId] = EntityID(mId.value, Users)
                    it[GroupMembers.role] = MemberRole.MEMBER.name.lowercase()
                }
            }
            GroupId(gId.value)
        }
    }

    override fun findUserGroups(userId: UserId): List<Pair<Group, MemberRole>> {
        return transaction {
            (Groups innerJoin GroupMembers)
                .select { GroupMembers.userId eq EntityID(userId.value, Users) }
                .map {
                    val group = Group(
                        id = GroupId(it[Groups.id].value),
                        name = it[Groups.name],
                        createdBy = it[Groups.createdBy]?.let { id -> UserId(id.value) } ?: UserId(0),
                        createdAt = it[Groups.createdAt].toEpochMilli()
                    )
                    val role = MemberRole.fromString(it[GroupMembers.role])
                    group to role
                }
        }
    }

    override fun findGroupMembers(groupId: GroupId): List<GroupMembership> {
        return transaction {
            (GroupMembers innerJoin Users)
                .select { GroupMembers.groupId eq EntityID(groupId.value, Groups) }
                .map {
                    GroupMembership(
                        userId = UserId(it[Users.id].value),
                        groupId = groupId,
                        role = MemberRole.fromString(it[GroupMembers.role]),
                        joinedAt = it[GroupMembers.joinedAt].toEpochMilli(),
                        user = User(
                            id = UserId(it[Users.id].value),
                            username = it[Users.username],
                            displayName = it[Users.name],
                            avatarUrl = it[Users.avatarUrl],
                            status = UserStatus.fromString(it[Users.status])
                        )
                    )
                }
        }
    }

    override fun isUserAdmin(groupId: GroupId, userId: UserId): Boolean {
        return transaction {
            GroupMembers.select {
                (GroupMembers.groupId eq EntityID(groupId.value, Groups)) and
                (GroupMembers.userId eq EntityID(userId.value, Users)) and
                (GroupMembers.role eq MemberRole.ADMIN.name.lowercase())
            }.any()
        }
    }

    override fun isUserMember(groupId: GroupId, userId: UserId): Boolean {
        return transaction {
            GroupMembers.select {
                (GroupMembers.groupId eq EntityID(groupId.value, Groups)) and 
                (GroupMembers.userId eq EntityID(userId.value, Users))
            }.any()
        }
    }

    override fun removeMember(groupId: GroupId, userId: UserId): Boolean {
        println("GroupRepository: Attempting to remove member. GroupId: ${groupId.value}, UserId: ${userId.value}")
        return transaction {
            val beforeCount = GroupMembers.select { (GroupMembers.groupId eq EntityID(groupId.value, Groups)) and (GroupMembers.userId eq EntityID(userId.value, Users)) }.count()
            println("GroupRepository: Exists before delete: $beforeCount")
            
            val deleted = GroupMembers.deleteWhere {
                Op.build { (GroupMembers.groupId eq EntityID(groupId.value, Groups)) and (GroupMembers.userId eq EntityID(userId.value, Users)) }
            }
            println("GroupRepository: Deleted count: $deleted")
            
            val afterCount = GroupMembers.select { (GroupMembers.groupId eq EntityID(groupId.value, Groups)) and (GroupMembers.userId eq EntityID(userId.value, Users)) }.count()
            println("GroupRepository: Exists after delete: $afterCount")
            
            deleted > 0
        }
    }

    override fun deleteGroup(groupId: GroupId): Boolean {
        return transaction {
            // First delete all invite links (to satisfy FK constraints)
            InviteLinks.deleteWhere { Op.build { InviteLinks.groupId eq EntityID(groupId.value, Groups) } }
            
            // Delete all messages in the group
            Messages.deleteWhere { Op.build { Messages.groupId eq EntityID(groupId.value, Groups) } }

            // Then delete all members
            GroupMembers.deleteWhere { Op.build { GroupMembers.groupId eq EntityID(groupId.value, Groups) } }
            
            val deleted = Groups.deleteWhere { Op.build { Groups.id eq EntityID(groupId.value, Groups) } }
            deleted > 0
        }
    }

    override fun addMember(groupId: GroupId, userId: UserId): Boolean {
        return transaction {
            GroupMembers.insert {
                it[GroupMembers.groupId] = EntityID(groupId.value, Groups)
                it[GroupMembers.userId] = EntityID(userId.value, Users)
                it[GroupMembers.role] = MemberRole.MEMBER.name.lowercase()
                // joinedAt is handled by DB default or nullable
            }
            true
        }
    }

    override fun findGroup(groupId: GroupId): Group? {
        return transaction {
            Groups.select { Groups.id eq EntityID(groupId.value, Groups) }
                .map {
                    Group(
                        id = GroupId(it[Groups.id].value),
                        name = it[Groups.name],
                        createdBy = it[Groups.createdBy]?.let { id -> UserId(id.value) } ?: UserId(0),
                        createdAt = it[Groups.createdAt].toEpochMilli()
                    )
                }
                .singleOrNull()
        }
    }
}
