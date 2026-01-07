package com.chattalkie.services

import com.chattalkie.domain.InvalidCredentialsException
import com.chattalkie.domain.UserAlreadyExistsException
import com.chattalkie.models.*
import com.chattalkie.repositories.UserRepository
import com.chattalkie.utils.JwtConfig
import org.mindrot.jbcrypt.BCrypt

class AuthService(private val userRepository: UserRepository) {

    fun register(request: RegisterRequest): AuthResponse {
        // Business Validation
        if (request.username.isBlank() || request.password.isBlank() || request.name.isBlank()) {
            throw IllegalArgumentException("All fields are required")
        }
        
        // Check uniqueness
        if (userRepository.findByUsername(request.username) != null) {
            throw UserAlreadyExistsException(request.username)
        }
        
        // Hash password
        val passwordHash = BCrypt.hashpw(request.password, BCrypt.gensalt())
        
        // Create User
        val userId = userRepository.create(request.username, request.name, passwordHash)
        
        // Retrieve created user to return full object (or construct it if we trust inputs)
        val user = userRepository.findById(userId) 
            ?: throw IllegalStateException("User creation failed retrieval")

        // Generate Token
        val token = JwtConfig.generateToken(user.id.value, user.username)
        
        return AuthResponse(
            token = token,
            user = UserResponse(
                id = user.id.value,
                username = user.username,
                name = user.displayName,
                avatarUrl = user.avatarUrl,
                status = user.status.name.lowercase()
            )
        )
    }
    
    fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByUsername(request.username)
            ?: throw InvalidCredentialsException()
            
        val storedHash = userRepository.getPasswordHash(request.username)
            ?: throw InvalidCredentialsException()
            
        if (!BCrypt.checkpw(request.password, storedHash)) {
            throw InvalidCredentialsException()
        }
                
        val token = JwtConfig.generateToken(user.id.value, user.username)
        
        return AuthResponse(
            token = token,
            user = UserResponse(
                id = user.id.value,
                username = user.username,
                name = user.displayName,
                avatarUrl = user.avatarUrl,
                status = user.status.name.lowercase()
            )
        )
    }
}
