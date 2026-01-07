package com.chattalkie.repositories

import com.chattalkie.database.tables.Friends
import com.chattalkie.database.tables.Users
import com.chattalkie.domain.*
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

interface UserRepository {
    fun findById(id: UserId): User?
    fun findByUsername(username: String): User?
    fun searchUsers(query: String, excludeUserId: UserId): List<Pair<User, FriendshipStatus?>>
    fun create(username: String, name: String, passwordHash: String): UserId
    fun getPasswordHash(username: String): String?
    fun updateStatus(userId: UserId, status: UserStatus)
    fun updateProfile(userId: UserId, name: String, username: String)
    fun updateAvatar(userId: UserId, avatarUrl: String)
    fun getFriendIds(userId: UserId): List<Int>
}

class UserRepositoryImpl : UserRepository {

    override fun findById(id: UserId): User? {
        return transaction {
            Users.select { Users.id eq EntityID(id.value, Users) }
                .singleOrNull()
                ?.let { mapUser(it) }
        }
    }

    override fun findByUsername(username: String): User? {
        return transaction {
            Users.select { Users.username eq username }
                .singleOrNull()
                ?.let { mapUser(it) }
        }
    }

    override fun getPasswordHash(username: String): String? {
        return transaction {
            Users.select { Users.username eq username }
                .singleOrNull()
                ?.get(Users.passwordHash)
        }
    }

    override fun create(username: String, name: String, passwordHash: String): UserId {
        return transaction {
            val id = Users.insertAndGetId {
                it[Users.username] = username
                it[Users.name] = name
                it[Users.passwordHash] = passwordHash
                it[Users.status] = UserStatus.OFFLINE.name.lowercase()
            }
            UserId(id.value)
        }
    }

    override fun searchUsers(query: String, excludeUserId: UserId): List<Pair<User, FriendshipStatus?>> {
        return transaction {
            val q = query.lowercase()
            Users.select {
                ((Users.username.lowerCase() like "%$q%") or (Users.name.lowerCase() like "%$q%")) and
                (Users.id neq EntityID(excludeUserId.value, Users))
            }.map { userRow ->
                val targetUserId = UserId(userRow[Users.id].value)
                val friendship = Friends.select {
                    ((Friends.userId eq EntityID(excludeUserId.value, Users)) and (Friends.friendId eq EntityID(targetUserId.value, Users))) or
                    ((Friends.userId eq EntityID(targetUserId.value, Users)) and (Friends.friendId eq EntityID(excludeUserId.value, Users)))
                }.firstOrNull()

                val friendshipStatus = friendship?.let { FriendshipStatus.fromString(it[Friends.status]) }
                
                val user = mapUser(userRow)
                user to friendshipStatus
            }
        }
    }

    private fun mapUser(row: ResultRow): User {
        return User(
            id = UserId(row[Users.id].value),
            username = row[Users.username],
            displayName = row[Users.name],
            avatarUrl = row[Users.avatarUrl],
            status = UserStatus.fromString(row[Users.status])
        )
    }

    override fun updateStatus(userId: UserId, status: UserStatus) {
        // NO-OP: Deprecated. Presence is now in-memory only.
        // Keeping interface method to avoid breaking other consumers if any, 
        // but removing DB write.
    }

    override fun updateProfile(userId: UserId, name: String, username: String) {
        transaction {
            Users.update({ Users.id eq userId.value }) {
                it[Users.name] = name
                it[Users.username] = username
            }
        }
    }

    override fun updateAvatar(userId: UserId, avatarUrl: String) {
        transaction {
            Users.update({ Users.id eq userId.value }) {
                it[Users.avatarUrl] = avatarUrl
            }
        }
    }


    override fun getFriendIds(userId: UserId): List<Int> {
        return transaction {
            Friends.select {
                ((Friends.userId eq EntityID(userId.value, Users)) or (Friends.friendId eq EntityID(userId.value, Users))) and
                (Friends.status.lowerCase() eq "accepted")
            }.map {
                if (it[Friends.userId].value == userId.value) it[Friends.friendId].value else it[Friends.userId].value
            }
        }
    }
}
