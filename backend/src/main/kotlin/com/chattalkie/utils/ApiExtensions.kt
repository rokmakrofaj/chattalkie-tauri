package com.chattalkie.utils

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.*

// Extension to get User ID safely
suspend fun ApplicationCall.getUserId(): Int {
    val principal = principal<JWTPrincipal>()
    return principal?.payload?.getClaim("userId")?.asInt() 
        ?: throw AuthenticationException("User not authenticated")
}

class AuthenticationException(message: String) : RuntimeException(message)
