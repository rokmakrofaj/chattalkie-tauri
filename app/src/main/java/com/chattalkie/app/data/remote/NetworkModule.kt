package com.chattalkie.app.data.remote

import com.chattalkie.app.data.remote.api.AuthService
import com.chattalkie.app.data.remote.api.FriendService
import com.chattalkie.app.data.remote.api.ChatService
import com.chattalkie.app.data.remote.api.UserService
import com.chattalkie.app.data.remote.api.GroupService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    // TÜM Cihazlar için (Emülatörler ve Fiziksel Cihazlar):
    // Terminalde şu komutu çalıştırın: adb reverse tcp:8080 tcp:8080
    // Bu sayede telefonun 127.0.0.1 adresi, bilgisayarın 127.0.0.1 adresine yönlenir.
    const val BASE_URL = "http://127.0.0.1:8080/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authService: AuthService = retrofit.create(AuthService::class.java)
    val friendService: FriendService = retrofit.create(FriendService::class.java)
    val chatService: ChatService = retrofit.create(ChatService::class.java)
    val userService: UserService = retrofit.create(UserService::class.java)
    val groupService: GroupService = retrofit.create(GroupService::class.java)
}
