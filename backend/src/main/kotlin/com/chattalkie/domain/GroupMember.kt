package com.chattalkie.domain

data class GroupMember(
    val userId: Int,
    val groupId: Int,
    val role: String,
    val joinedAt: Long,
    val user: User? = null
)
