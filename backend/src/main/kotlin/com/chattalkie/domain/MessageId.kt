package com.chattalkie.domain

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
value class MessageId(val value: String) {
    init {
        require(value.isNotBlank()) { "MessageId cannot be blank" }
    }
}
