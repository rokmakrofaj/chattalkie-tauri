package com.chattalkie.routes

import com.chattalkie.services.SyncService
import com.chattalkie.utils.getUserId
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.syncRoutes(syncService: SyncService) {
    route("/api/sync") {
        authenticate("auth-jwt") {
            get {
                try {
                    val userId = call.getUserId()
                    val lastTs = call.request.queryParameters["last_ts"]?.toLongOrNull() ?: 0L
                    
                    val response = syncService.getDeltaSync(userId, lastTs)
                    call.respond(response)
                } catch (e: Exception) {
                    val errorLog = "SYNC ERROR: ${e.message}\n${e.stackTraceToString()}\n"
                    println(errorLog)
                    try {
                        java.io.File("/tmp/backend_error.log").appendText(errorLog)
                    } catch (ignored: Exception) {}
                    call.respond(io.ktor.http.HttpStatusCode.InternalServerError, "Sync Failed")
                }
            }
        }
    }
}