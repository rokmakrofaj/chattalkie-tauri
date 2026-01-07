package com.chattalkie.domain

data class User(
    val id: UserId,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val status: UserStatus
) {
    init {
        require(username.isNotBlank()) { "Username cannot be blank" }
        require(displayName.isNotBlank()) { "Display name cannot be blank" }
    }
}
