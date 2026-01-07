package com.chattalkie.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chattalkie.app.data.FriendRepository
import com.chattalkie.app.data.GroupRepository
import com.chattalkie.app.data.remote.model.FriendResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupViewModel : ViewModel() {
    private val _friends = MutableStateFlow<List<FriendResponse>>(emptyList())
    val friends: StateFlow<List<FriendResponse>> = _friends.asStateFlow()

    private val _selectedFriendIds = MutableStateFlow<Set<Int>>(emptySet())
    val selectedFriendIds: StateFlow<Set<Int>> = _selectedFriendIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _groupCreated = MutableStateFlow<Int?>(null)
    val groupCreated: StateFlow<Int?> = _groupCreated.asStateFlow()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _friends.value = FriendRepository.getFriends()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun toggleFriendSelection(friendId: Int) {
        val current = _selectedFriendIds.value
        if (current.contains(friendId)) {
            _selectedFriendIds.value = current - friendId
        } else {
            _selectedFriendIds.value = current + friendId
        }
    }

    fun createGroup(name: String) {
        if (name.isBlank()) return
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val groupId = GroupRepository.createGroup(name, _selectedFriendIds.value.toList())
                _groupCreated.value = groupId
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun resetGroupCreated() {
        _groupCreated.value = null
    }
}
