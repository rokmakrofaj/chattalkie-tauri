package com.chattalkie.app.data.remote.api

import com.chattalkie.app.data.remote.model.*
import retrofit2.Response
import retrofit2.http.*

interface FriendService {
    @GET("api/friends")
    suspend fun getFriends(@Header("Authorization") token: String): Response<List<FriendResponse>>

    @GET("api/friends/pending")
    suspend fun getPendingRequests(@Header("Authorization") token: String): Response<List<FriendResponse>>

    @POST("api/friends/request")
    suspend fun sendFriendRequest(
        @Header("Authorization") token: String,
        @Body request: FriendRequest
    ): Response<Unit>

    @POST("api/friends/action")
    suspend fun respondToRequest(
        @Header("Authorization") token: String,
        @Body request: FriendActionRequest
    ): Response<Unit>

    @GET("api/users/search")
    suspend fun searchUsers(
        @Header("Authorization") token: String,
        @Query("query") query: String
    ): Response<List<UserResponse>>
}
