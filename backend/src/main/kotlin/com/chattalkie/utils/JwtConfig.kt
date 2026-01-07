package com.chattalkie.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

object JwtConfig {
    private var secret: String = ""
    private var issuer: String = ""
    private var audience: String = ""
    private const val validityInMs = 36_000_00 * 24 * 7 // 7 days

    fun initialize(secret: String, issuer: String, audience: String) {
        this.secret = secret
        this.issuer = issuer
        this.audience = audience
        println("JwtConfig initialized: issuer=$issuer, audience=$audience")
    }
    
    fun generateToken(userId: Int, username: String): String {
        println("Generating token for userId=$userId, username=$username with issuer=$issuer, audience=$audience")
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("userId", userId)
            .withClaim("username", username)
            .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
            .sign(Algorithm.HMAC256(secret))
    }
}
