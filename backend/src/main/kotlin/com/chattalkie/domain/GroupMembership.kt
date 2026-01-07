package com.chattalkie.domain

data class GroupMembership(
    val groupId: GroupId,
    val userId: UserId,
    val role: MemberRole,
    val joinedAt: Long,
    val user: User? = null
)
