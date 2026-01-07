package com.chattalkie.app.data

import com.chattalkie.app.data.remote.NetworkModule
import com.chattalkie.app.data.remote.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FriendRepository {
    
    private fun getAuthToken(): String? {
        return AuthRepository.token?.let { "Bearer $it" }
    }

    suspend fun getFriends(): List<FriendResponse> {
        return withContext(Dispatchers.IO) {
            val token = getAuthToken() ?: return@withContext emptyList()
            val response = NetworkModule.friendService.getFriends(token)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        }
    }

    suspend fun getPendingRequests(): List<FriendResponse> {
        return withContext(Dispatchers.IO) {
            val token = getAuthToken() ?: return@withContext emptyList()
            val response = NetworkModule.friendService.getPendingRequests(token)
            if (response.isSuccessful) {
                response.body() ?: emptyList()
            } else {
                emptyList()
            }
        }
    }

    suspend fun searchUsers(query: String): List<UserResponse> {
        android.util.Log.d("FriendSearch", "Repository.searchUsers entered: query=$query")
        return withContext(Dispatchers.IO) {
            val token = getAuthToken()
            android.util.Log.d("FriendSearch", "Repository.searchUsers: query=$query, hasToken=${token != null}")
            if (token == null) return@withContext emptyList()
            
            try {
                val response = NetworkModule.friendService.searchUsers(token, query)
                android.util.Log.d("FriendSearch", "API Response: isSuccessful=${response.isSuccessful}, code=${response.code()}")
                if (response.isSuccessful) {
                    response.body() ?: emptyList()
                } else {
                    android.util.Log.e("FriendSearch", "API Error Body: ${response.errorBody()?.string()}")
                    emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("FriendSearch", "API Exception", e)
                emptyList()
            }
        }
    }

    suspend fun sendFriendRequest(friendId: Int): Boolean {
        return withContext(Dispatchers.IO) {
            val token = getAuthToken() ?: return@withContext false
            val response = NetworkModule.friendService.sendFriendRequest(token, FriendRequest(friendId))
            response.isSuccessful
        }
    }

    suspend fun respondToRequest(userId: Int, action: String): Boolean {
        return withContext(Dispatchers.IO) {
            val token = getAuthToken() ?: return@withContext false
            val response = NetworkModule.friendService.respondToRequest(token, FriendActionRequest(userId, action))
            response.isSuccessful
        }
    }
}
