package com.chattalkie.app.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chattalkie.app.data.GroupRepository
import com.chattalkie.app.data.remote.model.GroupMemberResponse
import com.chattalkie.app.data.remote.model.InviteLinkResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GroupSettingsViewModel : ViewModel() {

    private val _members = MutableStateFlow<List<GroupMemberResponse>>(emptyList())
    val members: StateFlow<List<GroupMemberResponse>> = _members.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _inviteLink = MutableStateFlow<InviteLinkResponse?>(null)
    val inviteLink: StateFlow<InviteLinkResponse?> = _inviteLink.asStateFlow()

    private val _createdAt = MutableStateFlow<Long?>(null)
    val createdAt: StateFlow<Long?> = _createdAt.asStateFlow()

    fun loadGroup(groupId: Int) {
        viewModelScope.launch {
            try {
                val group = GroupRepository.getGroup(groupId)
                _createdAt.value = group?.createdAt
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun loadMembers(groupId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = GroupRepository.getGroupMembers(groupId)
                _members.value = result
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadInviteLink(groupId: Int) {
        viewModelScope.launch {
            try {
                _inviteLink.value = GroupRepository.getInviteLink(groupId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createInviteLink(groupId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _inviteLink.value = GroupRepository.createInviteLink(groupId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun kickMember(groupId: Int, userId: Int) {
        android.util.Log.d("KickDebug", "🔥 kickMember called: groupId=$groupId, userId=$userId")
        android.util.Log.d("KickDebug", "🔥 Current members before: ${_members.value.map { it.userId }}")
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = GroupRepository.kickMember(groupId, userId)
                android.util.Log.d("KickDebug", "🔥 API success: $success")
                
                if (success) {
                    // 🔥 CRITICAL FIX: immutable + update
                    _members.update { list ->
                        val filtered = list.filterNot { it.userId == userId }
                        android.util.Log.d("KickDebug", "🔥 Filtered members: ${filtered.map { it.userId }}")
                        filtered
                    }
                    android.util.Log.d("KickDebug", "🔥 Members after update: ${_members.value.map { it.userId }}")
                } else {
                    android.util.Log.d("KickDebug", "❌ API returned false!")
                }
            } catch (e: Exception) {
                android.util.Log.e("KickDebug", "❌ Exception: ${e.message}", e)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun shareInviteLink(context: Context, token: String) {
        val inviteUrl = "https://chattalkie.com/join/$token"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "ChatTalkie grubuma katıl!\n$inviteUrl")
        }
        context.startActivity(Intent.createChooser(intent, "Davet Linkini Paylaş"))
    }

    fun deleteGroup(groupId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = GroupRepository.deleteGroup(groupId)
                if (success) {
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun leaveGroup(groupId: Int, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = GroupRepository.leaveGroup(groupId)
                if (success) {
                    onSuccess()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
