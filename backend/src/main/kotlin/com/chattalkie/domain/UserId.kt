package com.chattalkie.domain

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class UserId(val value: Int) {
    init {
        require(value > 0) { "UserId must be positive" }
    }
}
