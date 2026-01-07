package com.chattalkie.routes

import com.chattalkie.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

import com.chattalkie.services.AuthService

fun Route.authRoutes(authService: AuthService) {
    route("/api/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            
            try {
                val response = authService.register(request)
                call.respond(HttpStatusCode.Created, response)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid request"))
            } catch (e: com.chattalkie.domain.UserAlreadyExistsException) {
                call.respond(HttpStatusCode.Conflict, ErrorResponse(e.message ?: "User already exists"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Unknown error"))
            }
        }
        
        post("/login") {
            val request = call.receive<LoginRequest>()
            
            try {
                val response = authService.login(request)
                call.respond(response)
            } catch (e: com.chattalkie.domain.InvalidCredentialsException) {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse(e.message ?: "Invalid credentials"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse(e.message ?: "Unknown error"))
            }
        }
    }
}
