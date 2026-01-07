package com.chattalkie.app.data

import com.chattalkie.app.data.remote.NetworkModule
import com.chattalkie.app.data.remote.model.GroupRequest
import com.chattalkie.app.data.remote.model.GroupResponse
import com.chattalkie.app.data.remote.model.GroupMemberResponse
import com.chattalkie.app.data.remote.model.InviteLinkResponse
import com.chattalkie.app.data.remote.model.JoinGroupResponse

object GroupRepository {
    suspend fun createGroup(name: String, memberIds: List<Int>): Int? {
        val token = AuthRepository.token ?: return null
        return try {
            val response = NetworkModule.groupService.createGroup("Bearer $token", GroupRequest(name, memberIds))
            response["id"]
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getGroups(): List<GroupResponse> {
        val token = AuthRepository.token ?: return emptyList()
        return NetworkModule.groupService.getGroups("Bearer $token")
    }

    suspend fun getGroup(groupId: Int): GroupResponse? {
        val token = AuthRepository.token ?: return null
        return try {
            NetworkModule.groupService.getGroup("Bearer $token", groupId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getGroupMembers(groupId: Int): List<GroupMemberResponse> {
        val token = AuthRepository.token ?: return emptyList()
        return try {
            NetworkModule.groupService.getGroupMembers("Bearer $token", groupId)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun kickMember(groupId: Int, userId: Int): Boolean {
        val token = AuthRepository.token ?: return false
        return try {
            android.util.Log.d("KickDebug", "🌐 Calling API: groupId=$groupId, userId=$userId")
            NetworkModule.groupService.kickMember("Bearer $token", groupId, com.chattalkie.app.data.remote.model.MemberKickRequest(userId))
            android.util.Log.d("KickDebug", "✅ API call successful")
            true
        } catch (e: Exception) {
            android.util.Log.e("KickDebug", "❌ Exception in kickMember: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    suspend fun createInviteLink(groupId: Int): InviteLinkResponse? {
        val token = AuthRepository.token ?: return null
        return try {
            NetworkModule.groupService.createInviteLink("Bearer $token", groupId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getInviteLink(groupId: Int): InviteLinkResponse? {
        val token = AuthRepository.token ?: return null
        return try {
            NetworkModule.groupService.getInviteLink("Bearer $token", groupId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun joinGroupByInvite(inviteToken: String): JoinGroupResponse? {
        val token = AuthRepository.token ?: return null
        return try {
            NetworkModule.groupService.joinGroupByInvite("Bearer $token", inviteToken)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun deleteGroup(groupId: Int): Boolean {
        val token = AuthRepository.token ?: return false
        return try {
            NetworkModule.groupService.deleteGroup("Bearer $token", groupId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun leaveGroup(groupId: Int): Boolean {
        val token = AuthRepository.token ?: return false
        return try {
            NetworkModule.groupService.leaveGroup("Bearer $token", groupId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
