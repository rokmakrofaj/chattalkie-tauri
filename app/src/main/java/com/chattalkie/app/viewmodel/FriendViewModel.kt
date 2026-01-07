package com.chattalkie.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chattalkie.app.data.FriendRepository
import com.chattalkie.app.data.remote.model.FriendResponse
import com.chattalkie.app.data.remote.model.UserResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FriendViewModel : ViewModel() {

    private val _searchResults = MutableStateFlow<List<UserResponse>>(emptyList())
    val searchResults: StateFlow<List<UserResponse>> = _searchResults.asStateFlow()

    private val _pendingRequests = MutableStateFlow<List<FriendResponse>>(emptyList())
    val pendingRequests: StateFlow<List<FriendResponse>> = _pendingRequests.asStateFlow()

    private val _friends = MutableStateFlow<List<FriendResponse>>(emptyList())
    val friends: StateFlow<List<FriendResponse>> = _friends.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun searchUsers(query: String) {
        android.util.Log.d("FriendSearch", "searchUsers called with query: $query")
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val results = FriendRepository.searchUsers(query)
                android.util.Log.d("FriendSearch", "Search results received: ${results.size} users")
                _searchResults.value = results
            } catch (e: Exception) {
                android.util.Log.e("FriendSearch", "Error searching users", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPendingRequests() {
        viewModelScope.launch {
            _pendingRequests.value = FriendRepository.getPendingRequests()
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            _friends.value = FriendRepository.getFriends()
        }
    }

    fun sendFriendRequest(friendId: Int) {
        viewModelScope.launch {
            if (FriendRepository.sendFriendRequest(friendId)) {
                // Optionally show toast or update UI
            }
        }
    }

    fun respondToRequest(userId: Int, action: String) {
        viewModelScope.launch {
            if (FriendRepository.respondToRequest(userId, action)) {
                loadPendingRequests()
                loadFriends()
            }
        }
    }
}
