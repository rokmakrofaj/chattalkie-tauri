package com.chattalkie.app.data

import com.chattalkie.app.data.remote.NetworkModule
import com.chattalkie.app.data.remote.model.LoginRequest
import com.chattalkie.app.data.remote.model.RegisterRequest
import com.chattalkie.app.data.remote.model.UserResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AuthRepository {
    private val _currentUser = MutableStateFlow<UserResponse?>(null)
    val currentUser: StateFlow<UserResponse?> = _currentUser.asStateFlow()
    
    var token: String? = null

    suspend fun login(username: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = NetworkModule.authService.login(LoginRequest(username, password))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    token = authResponse.token
                    _currentUser.value = authResponse.user
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun register(name: String, username: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = NetworkModule.authService.register(RegisterRequest(name, username, password))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    token = authResponse.token
                    _currentUser.value = authResponse.user
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    fun logout() {
        token = null
        _currentUser.value = null
    }
}

