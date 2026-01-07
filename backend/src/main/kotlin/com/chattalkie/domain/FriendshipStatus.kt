package com.chattalkie.domain

enum class FriendshipStatus {
    PENDING,
    ACCEPTED,
    REJECTED;
    
    companion object {
        fun fromString(value: String): FriendshipStatus = 
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
    }
}
