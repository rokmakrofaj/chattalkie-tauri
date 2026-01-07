package com.chattalkie.domain

enum class UserStatus {
    ONLINE,
    OFFLINE,
    BUSY;
    
    companion object {
        fun fromString(value: String): UserStatus = 
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OFFLINE
    }
}
