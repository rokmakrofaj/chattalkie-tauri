package com.chattalkie.socket

import io.ktor.websocket.*

/**
 * Represents an authenticated WebSocket connection.
 * Contains only serializable session info - no HTTP/Ktor request dependencies.
 */
data class Connection(
    val session: DefaultWebSocketSession,
    val userId: Int,
    val username: String
)
