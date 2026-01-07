package com.chattalkie.services

import com.chattalkie.domain.*
import com.chattalkie.models.InviteLinkResponse
import com.chattalkie.repositories.GroupRepository
import com.chattalkie.repositories.InviteRepository

class InviteService(
    private val inviteRepository: InviteRepository,
    private val groupRepository: GroupRepository
) {

    fun createInviteLink(groupId: Int, userId: Int): InviteLinkResponse {
        val gId = GroupId(groupId)
        val uId = UserId(userId)
        
        if (!groupRepository.isUserAdmin(gId, uId)) {
            throw PermissionDeniedException("Only admins can create invite links")
        }
        return inviteRepository.createInviteLink(gId, uId).toResponse()
    }

    fun getInviteLink(groupId: Int): InviteLinkResponse? {
        return inviteRepository.findByGroupId(GroupId(groupId))?.toResponse()
    }

    sealed class JoinResult {
        data class Success(val groupName: String) : JoinResult()
        data class AlreadyMember(val groupName: String) : JoinResult()
        object InvalidToken : JoinResult()
    }

    fun joinGroupByToken(token: String, userId: Int): JoinResult {
        val inviteToken = try {
            InviteToken(token)
        } catch (e: IllegalArgumentException) {
            return JoinResult.InvalidToken
        }
        
        val inviteLink = inviteRepository.findByToken(inviteToken) ?: return JoinResult.InvalidToken
        val uId = UserId(userId)
        
        if (groupRepository.isUserMember(inviteLink.groupId, uId)) {
            return JoinResult.AlreadyMember(inviteLink.groupName ?: "Unknown Group")
        }

        inviteRepository.addMemberToGroup(inviteLink.groupId, uId, MemberRole.MEMBER)
        return JoinResult.Success(inviteLink.groupName ?: "Unknown Group")
    }
    
    private fun InviteLink.toResponse(): InviteLinkResponse {
        return InviteLinkResponse(
            token = token.value,
            groupName = groupName ?: "Unknown Group",
            groupId = groupId.value,
            createdAt = createdAt,
            expiresAt = expiresAt
        )
    }
}
