package com.chattalkie.domain

enum class MemberRole {
    ADMIN,
    MEMBER;
    
    fun canKickMembers(): Boolean = this == ADMIN
    fun canCreateInviteLink(): Boolean = this == ADMIN
    
    companion object {
        fun fromString(value: String): MemberRole = 
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEMBER
    }
}
