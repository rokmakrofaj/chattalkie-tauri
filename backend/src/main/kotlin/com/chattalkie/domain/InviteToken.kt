package com.chattalkie.domain

@JvmInline
value class InviteToken(val value: String) {
    init {
        require(value.length == 16) { "InviteToken must be 16 characters" }
        require(value.all { it.isLetterOrDigit() }) { "InviteToken must be alphanumeric" }
    }
}
