package com.chattalkie.routes

import com.chattalkie.models.*
import com.chattalkie.services.GroupService
import com.chattalkie.services.InviteService
import com.chattalkie.services.MessageService
import com.chattalkie.services.UserService
import com.chattalkie.socket.ChatController
import com.chattalkie.socket.Connection
import com.chattalkie.utils.getUserId
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.http.content.*
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.minio.MinioClient
import io.minio.PutObjectArgs
import org.koin.ktor.ext.inject
import java.io.File
import java.util.UUID
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.serialization.json.Json

// Helper to wrap route execution with exception handling
suspend fun ApplicationCall.safeExecute(block: suspend () -> Unit) {
    try {
        block()
    } catch (e: com.chattalkie.utils.AuthenticationException) {
        respond(HttpStatusCode.Unauthorized)
    } catch (e: com.chattalkie.domain.PermissionDeniedException) {
        respond(HttpStatusCode.Forbidden, ErrorResponse(e.message ?: "Access denied"))
    } catch (e: com.chattalkie.domain.ResourceNotFoundException) {
        respond(HttpStatusCode.NotFound, ErrorResponse(e.message ?: "Not found"))
    } catch (e: com.chattalkie.domain.ValidationException) {
        respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Validation error"))
    } catch (e: IllegalArgumentException) {
        respond(HttpStatusCode.BadRequest, ErrorResponse(e.message ?: "Invalid request parameter"))
    } catch (e: Exception) {
        e.printStackTrace()
        respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal server error"))
    }
}

fun Route.userRoutes(userService: UserService) {
    route("/api/debug") {
        get("/check/{username}") {
            call.safeExecute {
                val username = call.parameters["username"] ?: throw IllegalArgumentException("Missing username")
                // Pass 1 as currentUserId (must be positive)
                val users = userService.searchUsers(username, 1)
                call.respond(users) 
            }
        }
    }

    route("/api/users") {
        authenticate("auth-jwt") {
            get("/me") {
                call.safeExecute {
                    val userId = call.getUserId()
                    val user = userService.getUser(userId)
                    
                    if (user != null) {
                        call.respond(user)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            put("/me") {
                call.safeExecute {
                    val userId = call.getUserId()
                    val request = call.receive<UserUpdateRequest>()
                    
                    userService.updateProfile(userId, request)
                    
                    val updatedUser = userService.getUser(userId)
                    if (updatedUser != null) {
                         call.respond(updatedUser)
                    } else {
                         call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Failed to fetch updated profile"))
                    }
                }
            }

            get("/{id}") {
                call.safeExecute {
                    val id = call.parameters["id"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
                    val user = userService.getUser(id)
                    
                    if (user != null) {
                        call.respond(user)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }

            get("/search") {
                call.safeExecute {
                    val query = call.request.queryParameters["query"] ?: return@safeExecute call.respond(emptyList<UserResponse>())
                    val userId = call.getUserId()

                    val users = userService.searchUsers(query, userId)
                    call.respond(users)
                }
            }

            post("/me/avatar") {
                call.safeExecute {
                    val userId = call.getUserId()
                    val multipart = call.receiveMultipart()
                    var avatarUrl = ""

                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem) {
                            val originalName = part.originalFileName ?: "avatar.jpg"
                            val ext = File(originalName).extension.ifEmpty { "jpg" }
                            val fileName = "${userId}_${System.currentTimeMillis()}_${UUID.randomUUID()}.$ext"
                            
                            val cwd = System.getProperty("user.dir")
                            println("DEBUG: CWD = $cwd")
                            
                            val uploadDir = File("uploads/avatars")
                            println("DEBUG: Target Upload Dir = ${uploadDir.absolutePath}")
                            
                            if (!uploadDir.exists()) {
                                val created = uploadDir.mkdirs()
                                println("DEBUG: Created Dir: $created")
                            }
                            
                            val file = File(uploadDir, fileName)
                            
                            part.streamProvider().use { input ->
                                file.outputStream().buffered().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            // Return full URL assuming localhost:8080 for now or relative
                            avatarUrl = "http://localhost:8080/uploads/avatars/$fileName"
                            println("DEBUG: Avatar URL = $avatarUrl")
                        }
                        part.dispose()
                    }
                    
                    if (avatarUrl.isNotEmpty()) {
                        userService.updateAvatar(userId, avatarUrl)
                        call.respond(HttpStatusCode.OK, mapOf("avatarUrl" to avatarUrl))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("No file uploaded"))
                    }
                }
            }
        }
    }
}

fun Route.messageRoutes(messageService: MessageService) {
    route("/api/messages") {
        authenticate("auth-jwt") {
            get("/history/{partnerId}") {
                call.safeExecute {
                    val userId = call.getUserId()
                    val partnerId = call.parameters["partnerId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")

                    val history = messageService.getChatHistory(userId, partnerId)
                    call.respond(history)
                }
            }

            get("/group/{groupId}") {
                call.safeExecute {
                    val groupId = call.parameters["groupId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
                    
                    val history = messageService.getGroupHistory(groupId)
                    call.respond(history)
                }
            }
        }
    }
}

fun Route.groupRoutes(groupService: GroupService, inviteService: InviteService) {
    route("/api/groups") {
        authenticate("auth-jwt") {
            post {
                call.safeExecute {
                    val userId = call.getUserId()
                    val request = call.receive<GroupCreateRequest>()

                    val groupId = groupService.createGroup(request, userId)
                    call.respond(HttpStatusCode.Created, mapOf("id" to groupId))
                }
            }

            get {
                call.safeExecute {
                    val userId = call.getUserId()
                    val groups = groupService.getUserGroups(userId)
                    call.respond(groups)
                }
            }

            get("/{groupId}") {
                call.safeExecute {
                    val userId = call.getUserId()
                    val groupId = call.parameters["groupId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
                    val group = groupService.getGroup(groupId, userId)
                    call.respond(group)
                }
            }

            get("/{groupId}/members") {
                call.safeExecute {
                    val userId = call.getUserId()
                    val groupId = call.parameters["groupId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")

                    if (!groupService.isMember(groupId, userId)) {
                        throw com.chattalkie.domain.PermissionDeniedException("Not a member")
                    }

                    val members = groupService.getGroupMembers(groupId)
                    call.respond(members)
                }
            }

            post("/{groupId}/members") {
                call.safeExecute {
                    val userId = call.getUserId()
                    val groupId = call.parameters["groupId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
                    val request = call.receive<MemberAddRequest>()

                    groupService.addMember(groupId, userId, request.userId)
                    call.respond(HttpStatusCode.Created, mapOf("message" to "Member added"))
                }
            }

            post("/{groupId}/kick") {
                call.safeExecute {
                    val myUserId = call.getUserId()
                    val groupId = call.parameters["groupId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")
                    val request = call.receive<MemberKickRequest>()

                    groupService.kickMember(groupId, myUserId, request.userId)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Member kicked", "deleted" to "1"))
                }
            }

            delete("/{groupId}") {
                call.safeExecute {
                    val userId = call.getUserId()
                    val groupId = call.parameters["groupId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")

                    groupService.deleteGroup(groupId, userId)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Group deleted"))
                }
            }

            post("/{groupId}/leave") {
                call.safeExecute {
                    val userId = call.getUserId()
                    val groupId = call.parameters["groupId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")

                    groupService.leaveGroup(groupId, userId)
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Left group"))
                }
            }
            post("/{groupId}/invite") {
                call.safeExecute {
                    val userId = call.getUserId()
                    val groupId = call.parameters["groupId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")

                    val inviteLink = inviteService.createInviteLink(groupId, userId)
                    call.respond(HttpStatusCode.Created, inviteLink)
                }
            }

            get("/{groupId}/invite") {
                call.safeExecute {
                    val userId = call.getUserId()
                    val groupId = call.parameters["groupId"]?.toIntOrNull() ?: throw IllegalArgumentException("Invalid ID")

                    if (!groupService.isMember(groupId, userId)) {
                         throw com.chattalkie.domain.PermissionDeniedException("Not a member")
                    }

                    val inviteLink = inviteService.getInviteLink(groupId)
                    if (inviteLink != null) {
                        call.respond(inviteLink)
                    } else {
                        throw com.chattalkie.domain.ResourceNotFoundException("Invite Link", groupId.toString())
                    }
                }
            }

            post("/join/{token}") {
                call.safeExecute {
                    val userId = call.getUserId()
                    val token = call.parameters["token"] ?: throw IllegalArgumentException("Invalid token")

                    val result = inviteService.joinGroupByToken(token, userId)
                    
                    when (result) {
                        is InviteService.JoinResult.Success -> 
                            call.respond(HttpStatusCode.OK, mapOf("message" to "${result.groupName} grubuna başarıyla katıldınız!", "groupName" to result.groupName))
                        is InviteService.JoinResult.AlreadyMember -> 
                            call.respond(HttpStatusCode.Conflict, mapOf("message" to "Zaten bu grubun üyesisiniz", "groupName" to result.groupName))
                        is InviteService.JoinResult.InvalidToken -> 
                            call.respond(HttpStatusCode.NotFound, ErrorResponse("Geçersiz davet linki"))
                    }
                }
            }
        }
    }
}

fun Route.webSocketRoute(chatController: ChatController, userService: UserService) {
    get("/api/debug/presence") {
        call.respond(mapOf(
            "connected_users" to chatController.getConnectedUserIds()
        ))
    }

    authenticate("auth-jwt") {
        webSocket("/chat") {
            println("WS HANDSHAKE -> Connection established")
            val principal = call.principal<JWTPrincipal>()
            val userId = principal?.payload?.getClaim("userId")?.asInt()
            val username = principal?.payload?.getClaim("username")?.asString()

            if (userId == null) {
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No userId"))
                return@webSocket
            }

            // Security Patch: Verify user actually exists in DB (prevents Zombie/Ghost connections after DB wipe)
            val user = userService.getUser(userId)
            if (user == null) {
                println("Rejected zombie connection for userId: $userId (User not found in DB)")
                close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "User does not exist in DB"))
                return@webSocket
            }

            // Use Display Name instead of username for better UI experience
            val connection = Connection(this, userId, user.name)
            chatController.onConnect(connection)

            try {
                // Use lenient JSON decoder to handle extra fields gracefully
                val lenientJson = Json { ignoreUnknownKeys = true }

                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        // RAW PAYLOAD LOG DETECTOR
                        // println("WS RAW -> $text")
                        try {
                            // ... definitions ...

                            if (text.contains(""""kind":"signal"""") || text.contains(""""kind": "signal"""")) {
                                val signal = lenientJson.decodeFromString<CallSignal>(text)
                                chatController.routeCallSignal(signal)
                                continue
                            }

                            if (text.contains(""""kind":"typing"""") || text.contains(""""kind": "typing"""")) {
                                val signal = lenientJson.decodeFromString<TypingSignal>(text)
                                chatController.broadcastTyping(signal)
                                continue
                            }
                            
                            if (text.contains(""""kind":"delivery_status"""") || text.contains(""""kind": "delivery_status"""")) {
                                val status = lenientJson.decodeFromString<DeliveryStatus>(text)
                                chatController.broadcastDeliveryStatus(status, status.recipientId) 
                                continue
                            }

                            val incoming = lenientJson.decodeFromString<IncomingMessage>(text)
                            // println("DEBUG: Parsed Incoming mediaUrl=${incoming.mediaUrl}")
                            
                            // NORMALIZATION: Enforce CID and MessageID
                            // Desktop client drops messages without CID. Web client often omits it.
                            // Solution: Server acts as Source of Truth and generates them if missing.
                            val normalizedCid = incoming.cid ?: java.util.UUID.randomUUID().toString()
                            val normalizedMessageId = incoming.messageId ?: java.util.UUID.randomUUID().toString()
                            
                            // Ensure strict kind="chat" processing if needed, though we route by logic here.

                            // MANDATORY ROUTE LOG
                            // println("WS ROUTE -> sender=${userId} recipient=${incoming.recipientId} group=${incoming.groupId}")

                            // Refresh User to get latest Avatar/Name
                            val currentUser = userService.getUser(userId)

                            val outgoingMessage = OutgoingMessage(
                                messageId = normalizedMessageId,
                                cid = normalizedCid,
                                senderId = userId,
                                senderName = currentUser?.name ?: connection.username,
                                senderAvatar = currentUser?.avatarUrl,
                                content = incoming.content,
                                timestamp = System.currentTimeMillis(),
                                groupId = incoming.groupId,
                                mediaKey = incoming.mediaKey,
                                messageType = incoming.messageType ?: incoming.type
                            )
                            
                            // Wait, I suspect IncomingMessage in Models.kt misses mediaUrl too!
                            // I should verify IncomingMessage in Models.kt while I'm at it. I added it to OutgoingMessage, but IncomingMessage?
                            
                            if (incoming.groupId != null) {
                                chatController.sendGroupMessage(incoming.groupId, outgoingMessage)
                            } else {
                                // STRICT VALIDATION for DM: Must have recipientId
                                // If Web client fails to send it, we must reject/log.
                                val recipientId = incoming.recipientId
                                    ?: throw IllegalArgumentException("Direct Message missing recipientId")
                                
                                chatController.sendMessage(recipientId, outgoingMessage)
                            }
                            
                        } catch (e: Exception) {
                            // Log only major errors
                            println("WS Error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                chatController.onDisconnect(connection)
            }
        }
    }
}

fun Route.chatUploadRoutes() {
    val storageRepository by inject<com.chattalkie.repositories.StorageRepository>()
    val minioClient by inject<io.minio.MinioClient>()
    
    route("/api/media") {
        authenticate("auth-jwt") {
            get("/{key}") {
                call.safeExecute {
                     val key = call.parameters["key"] ?: throw IllegalArgumentException("Missing key")
                     
                     // Determine Content Type from extension
                     val ext = java.io.File(key).extension.lowercase()
                     val contentType = when(ext) {
                         "jpg", "jpeg" -> io.ktor.http.ContentType.Image.JPEG
                         "png" -> io.ktor.http.ContentType.Image.PNG
                         "gif" -> io.ktor.http.ContentType.Image.GIF
                         "webm" -> io.ktor.http.ContentType.Audio.OGG // WebM Audio
                         "mp3" -> io.ktor.http.ContentType.Audio.MPEG
                         "wav" -> io.ktor.http.ContentType("audio", "wav")
                         "mp4" -> io.ktor.http.ContentType.Video.MP4
                         else -> io.ktor.http.ContentType.Application.OctetStream
                     }

                     val stream = minioClient.getObject(
                         io.minio.GetObjectArgs.builder()
                             .bucket("chattalkie-media")
                             .`object`(key)
                             .build()
                     )
                     
                     call.respondOutputStream(contentType) {
                         stream.copyTo(this)
                     }
                }
            }
        }
    }

    route("/api/chat") {
        authenticate("auth-jwt") {
            post("/upload") {
                call.safeExecute {
                    val userId = call.getUserId()
                    val multipart = call.receiveMultipart()
                    var mediaKey = ""

                    multipart.forEachPart { part ->
                        if (part is PartData.FileItem) {
                            val originalName = part.originalFileName ?: "file"
                            val ext = java.io.File(originalName).extension.ifEmpty { "dat" }
                            val fileName = "${userId}_${System.currentTimeMillis()}_${UUID.randomUUID()}.$ext"
                            
                            // Upload directly to MinIO
                            val contentType = part.contentType?.toString() ?: "application/octet-stream"
                            val inputStream = part.streamProvider()
                            
                            minioClient.putObject(
                                io.minio.PutObjectArgs.builder()
                                    .bucket("chattalkie-media")
                                    .`object`(fileName)
                                    .stream(inputStream, -1, 10485760) // Unknown size, 10MB part
                                    .contentType(contentType)
                                    .build()
                            )
                            
                            mediaKey = fileName
                            println("MinIO: Uploaded $fileName")
                        }
                        part.dispose()
                    }
                    
                    if (mediaKey.isNotEmpty()) {
                        // Return key (not URL) - client will use /api/media/{key}
                        call.respond(HttpStatusCode.OK, mapOf("key" to mediaKey))
                    } else {
                        call.respond(HttpStatusCode.BadRequest, ErrorResponse("No file uploaded"))
                    }
                }
            }
        }
    }
}

