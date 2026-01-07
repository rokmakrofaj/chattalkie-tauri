package com.chattalkie.app.data.remote.api

import com.chattalkie.app.data.remote.model.UserResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface UserService {
    @GET("api/users/{id}")
    suspend fun getUserDetail(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): UserResponse
}
