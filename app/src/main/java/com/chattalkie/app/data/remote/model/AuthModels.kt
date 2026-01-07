package com.chattalkie.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("name") val name: String,
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: UserResponse
)

data class UserResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("name") val name: String,
    @SerializedName("avatarUrl") val avatarUrl: String?,
    @SerializedName("status") val status: String,
    @SerializedName("friendshipStatus") val friendshipStatus: String? = null
)

data class ErrorResponse(
    @SerializedName("error") val error: String
)
