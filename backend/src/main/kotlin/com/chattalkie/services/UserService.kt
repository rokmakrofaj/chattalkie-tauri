package com.chattalkie.services

import com.chattalkie.domain.*
import com.chattalkie.models.UserResponse
import com.chattalkie.models.UserUpdateRequest
import com.chattalkie.repositories.UserRepository

class UserService(private val userRepository: UserRepository) {

    fun getUser(id: Int): UserResponse? {
        return userRepository.findById(UserId(id))?.toResponse()
    }

    fun searchUsers(query: String, currentUserId: Int): List<UserResponse> {
        return userRepository.searchUsers(query, UserId(currentUserId))
            .map { (user, friendshipStatus) -> user.toResponse(friendshipStatus) }
    }
    
    private fun User.toResponse(friendshipStatus: FriendshipStatus? = null): UserResponse {
        return UserResponse(
            id = id.value,
            username = username,
            name = displayName,
            avatarUrl = avatarUrl,
            status = status.name.lowercase(),
            friendshipStatus = friendshipStatus?.name?.lowercase()
        )
    }
    fun updateProfile(userId: Int, request: UserUpdateRequest) {
        userRepository.updateProfile(UserId(userId), request.name, request.username)
    }

    fun updateAvatar(userId: Int, avatarUrl: String) {
        userRepository.updateAvatar(UserId(userId), avatarUrl)
    }
}
