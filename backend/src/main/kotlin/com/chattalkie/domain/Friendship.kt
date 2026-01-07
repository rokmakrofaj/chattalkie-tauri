package com.chattalkie.domain

data class Friendship(
    val userId: UserId,
    val friendId: UserId,
    val status: FriendshipStatus,
    val createdAt: Long
)
