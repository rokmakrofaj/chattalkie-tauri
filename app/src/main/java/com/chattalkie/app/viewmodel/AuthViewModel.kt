package com.chattalkie.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chattalkie.app.data.AuthRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthEvent {
    object LoginSuccess : AuthEvent()
    object RegisterSuccess : AuthEvent()
    data class Error(val message: String) : AuthEvent()
}

class AuthViewModel : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableSharedFlow<AuthEvent>()
    val events: SharedFlow<AuthEvent> = _events.asSharedFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            if (AuthRepository.login(username, password)) {
                _events.emit(AuthEvent.LoginSuccess)
            } else {
                _events.emit(AuthEvent.Error("Invalid username or password"))
            }
            _isLoading.value = false
        }
    }

    fun register(name: String, username: String, password: String) {
        if (name.isBlank() || username.isBlank() || password.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            if (AuthRepository.register(name, username, password)) {
                _events.emit(AuthEvent.RegisterSuccess)
            } else {
                // Determine error message potentially from Repo result if refactored, generic for now
                _events.emit(AuthEvent.Error("Username already exists or registration failed"))
            }
            _isLoading.value = false
        }
    }
}
