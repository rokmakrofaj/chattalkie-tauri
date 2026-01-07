package com.chattalkie.domain

data class InviteLink(
    val id: Int,
    val groupId: GroupId,
    val token: InviteToken,
    val createdBy: UserId,
    val createdAt: Long,
    val expiresAt: Long?,
    val groupName: String? = null
) {
    fun isExpired(currentTime: Long): Boolean {
        return expiresAt?.let { it < currentTime } ?: false
    }
}
