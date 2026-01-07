package com.chattalkie.app.data.remote.api

import com.chattalkie.app.data.remote.model.AuthResponse
import com.chattalkie.app.data.remote.model.LoginRequest
import com.chattalkie.app.data.remote.model.RegisterRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
}
