package com.chattalkie.routes

import com.chattalkie.database.tables.Friends
import com.chattalkie.database.tables.Users
import com.chattalkie.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

fun Route.friendRoutes() {
    authenticate("auth-jwt") {
        route("/api/friends") {
            // Get all accepted friends
            get {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt() ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val friendsList = transaction {
                    Friends.select { 
                        ((Friends.userId eq userId) or (Friends.friendId eq userId)) and (Friends.status eq "ACCEPTED")
                    }.map {
                        val friendId = if (it[Friends.userId].value == userId) it[Friends.friendId].value else it[Friends.userId].value
                        val user = Users.select { Users.id eq friendId }.first()
                        FriendResponse(
                            id = user[Users.id].value,
                            username = user[Users.username],
                            name = user[Users.name],
                            avatarUrl = user[Users.avatarUrl],
                            status = user[Users.status],
                            friendShipStatus = "ACCEPTED"
                        )
                    }
                }
                call.respond(friendsList)
            }

            // List pending requests
            get("/pending") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt() ?: return@get call.respond(HttpStatusCode.Unauthorized)

                val pendingRequests = transaction {
                    Friends.select { (Friends.friendId eq userId) and (Friends.status eq "PENDING") }
                        .map {
                            val requesterId = it[Friends.userId].value
                            val user = Users.select { Users.id eq requesterId }.first()
                            FriendResponse(
                                id = user[Users.id].value,
                                username = user[Users.username],
                                name = user[Users.name],
                                avatarUrl = user[Users.avatarUrl],
                                status = user[Users.status],
                                friendShipStatus = "PENDING"
                            )
                        }
                }
                call.respond(pendingRequests)
            }

            // Send friend request
            post("/request") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val request = call.receive<FriendRequest>()

                if (userId == request.friendId) {
                    return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Cannot add yourself"))
                }

                val status = transaction {
                    val existing = Friends.select { 
                        (Friends.userId eq userId and (Friends.friendId eq request.friendId)) or 
                        (Friends.userId eq request.friendId and (Friends.friendId eq userId))
                    }.singleOrNull()

                    if (existing != null) {
                        return@transaction "EXISTS"
                    } else {
                        Friends.insert {
                            it[Friends.userId] = userId
                            it[Friends.friendId] = request.friendId
                            it[Friends.status] = "PENDING"
                        }
                        "CREATED"
                    }
                }

                if (status == "EXISTS") {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("Request already exists or you are already friends"))
                } else {
                    call.respond(HttpStatusCode.Created, mapOf("status" to "Request sent"))
                }
            }

            // Accept/Reject friend request
            post("/action") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.getClaim("userId")?.asInt() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val action = call.receive<FriendActionRequest>()

                transaction {
                    if (action.action == "ACCEPT") {
                        Friends.update({ (Friends.userId eq action.userId) and (Friends.friendId eq userId) }) {
                            it[status] = "ACCEPTED"
                        }
                    } else if (action.action == "REJECT") {
                        Friends.deleteWhere { (Friends.userId eq action.userId) and (Friends.friendId eq userId) }
                    }
                    Unit
                }
                call.respond(HttpStatusCode.OK, mapOf("status" to "Success"))
            }
        }
    }
}
