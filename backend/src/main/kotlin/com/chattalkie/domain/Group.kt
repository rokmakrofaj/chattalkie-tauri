package com.chattalkie.domain

data class Group(
    val id: GroupId,
    val name: String,
    val createdBy: UserId,
    val createdAt: Long
) {
    init {
        require(name.isNotBlank()) { "Group name cannot be blank" }
    }
}
