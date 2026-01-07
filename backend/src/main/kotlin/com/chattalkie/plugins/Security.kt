package com.chattalkie.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*

fun Application.configureSecurity() {
    val jwtSecret = environment.config.property("jwt.secret").getString()
    val jwtIssuer = environment.config.property("jwt.issuer").getString()
    val jwtAudience = environment.config.property("jwt.audience").getString()
    val jwtRealm = environment.config.property("jwt.realm").getString()

    println("JWT Config: Issuer=$jwtIssuer, Audience=$jwtAudience, Realm=$jwtRealm")
    println("JWT Secret starts with: ${jwtSecret.take(3)}...")

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asInt() != null) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
            
            authHeader { call ->
                // First try Authorization header
                val authHeader = call.request.headers[HttpHeaders.Authorization]
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    return@authHeader parseAuthorizationHeader(authHeader)
                }
                
                // Fallback to query param for WebSocket
                val token = call.request.queryParameters["token"]
                if (token != null) {
                    return@authHeader HttpAuthHeader.Single("Bearer", token)
                }
                
                null
            }
            challenge { _, _ ->
                call.respond(io.ktor.http.HttpStatusCode.Unauthorized, "Token is not valid or has expired")
            }
        }
    }
}
