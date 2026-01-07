package com.chattalkie.plugins

import com.chattalkie.routes.*
import com.chattalkie.services.*
import com.chattalkie.repositories.StorageRepository
import com.chattalkie.socket.ChatController
import org.koin.ktor.ext.inject
import io.ktor.server.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.http.HttpStatusCode
import java.io.File

fun Application.configureRouting() {
    val userService by inject<UserService>()
    val authService by inject<AuthService>()
    val messageService by inject<MessageService>()
    val groupService by inject<GroupService>()
    val inviteService by inject<InviteService>()
    val syncService by inject<SyncService>()
    val chatController by inject<ChatController>()
    val storageRepository by inject<StorageRepository>()

    routing {
        // Manual static file handling for debugging
        get("/uploads/{...}") {
            val uri = call.request.uri
            // Remove /uploads/ prefix
            val relativePath = uri.removePrefix("/uploads/")
            val file = File("uploads", relativePath)
            
            // println("DEBUG: Request static file: $relativePath")
            // println("DEBUG: Looking at: ${file.absolutePath}")
            
            if (file.exists()) {
                call.respondFile(file)
            } else {
                // Should we try absolute path relative to project? 
                // If CWD is root, we might need backend/uploads
                val backendFile = File("backend/uploads", relativePath)
                if (backendFile.exists()) {
                     // println("DEBUG: Found in fallback: ${backendFile.absolutePath}")
                     call.respondFile(backendFile)
                } else {
                     // println("DEBUG: File NOT FOUND at ${file.absolutePath} OR ${backendFile.absolutePath}")
                     call.respond(HttpStatusCode.NotFound, "File not found")
                }
            }
        }

        get("/") {
            call.respondText("ChatTalkie API is running!")
        }
        
        authRoutes(authService)
        userRoutes(userService)
        friendRoutes() // Legacy, not yet refactored to Service
        messageRoutes(messageService)
        chatUploadRoutes()
        groupRoutes(groupService, inviteService)
        syncRoutes(syncService)
        mediaRoutes(storageRepository)
        webSocketRoute(chatController, userService)
    }
}

