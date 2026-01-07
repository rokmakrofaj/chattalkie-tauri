package com.chattalkie.domain

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class GroupId(val value: Int) {
    init {
        require(value > 0) { "GroupId must be positive" }
    }
}
