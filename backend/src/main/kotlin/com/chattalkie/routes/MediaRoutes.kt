package com.chattalkie.routes

import com.chattalkie.repositories.StorageRepository
import com.chattalkie.models.ErrorResponse
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.HttpStatusCode
fun Route.mediaRoutes(storageRepository: StorageRepository) {
    route("/api/media") {
        authenticate("auth-jwt") {
            get("/{key}") {
                call.safeExecute {
                    val key = call.parameters["key"] ?: throw IllegalArgumentException("Missing media key")
                    
                    // Generate PRE-SIGNED GET URL (valid for 1 hour)
                    val presignedUrl = storageRepository.generateDownloadUrl(key)
                    
                    // Return JSON with URL (avoids CORS issues with redirect/fetch)
                    call.respond(mapOf("url" to presignedUrl))
                }
            }
        }
    }
}
