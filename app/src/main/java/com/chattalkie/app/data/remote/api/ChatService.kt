package com.chattalkie.app.data.remote.api

import com.chattalkie.app.data.remote.model.MessageResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ChatService {
    @GET("api/messages/history/{partnerId}")
    suspend fun getChatHistory(
        @Header("Authorization") token: String,
        @Path("partnerId") partnerId: Int
    ): List<MessageResponse>

    @GET("api/messages/group/{groupId}")
    suspend fun getGroupChatHistory(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int
    ): List<MessageResponse>

    @GET("api/sync")
    suspend fun getSync(
        @Header("Authorization") token: String,
        @retrofit2.http.Query("last_ts") lastTs: Long
    ): com.chattalkie.app.data.remote.model.SyncResponse

    @retrofit2.http.Multipart
    @retrofit2.http.POST("api/chat/upload")
    suspend fun uploadMedia(
        @Header("Authorization") token: String,
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part
    ): Map<String, String> // Returns {"key": "..."}
}
