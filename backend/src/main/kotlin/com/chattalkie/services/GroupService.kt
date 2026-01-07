package com.chattalkie.services

import com.chattalkie.domain.*
import io.ktor.server.plugins.NotFoundException
import com.chattalkie.models.GroupCreateRequest
import com.chattalkie.models.GroupMemberResponse
import com.chattalkie.models.GroupResponse
import com.chattalkie.repositories.GroupRepository


class GroupService(private val groupRepository: GroupRepository) {

    fun createGroup(request: GroupCreateRequest, userId: Int): Int {
        if (request.name.isBlank()) {
            throw ValidationException("Group name cannot be empty")
        }
        
        return groupRepository.createGroup(
            request.name, 
            UserId(userId), 
            request.memberIds.map { UserId(it) }
        ).value
    }

    fun getUserGroups(userId: Int): List<GroupResponse> {
        return groupRepository.findUserGroups(UserId(userId)).map { (group, role) ->
            group.toResponse(role)
        }
    }

    fun getGroupMembers(groupId: Int): List<GroupMemberResponse> {
        return groupRepository.findGroupMembers(GroupId(groupId)).map { it.toResponse() }
    }

    fun isMember(groupId: Int, userId: Int): Boolean {
        return groupRepository.isUserMember(GroupId(groupId), UserId(userId))
    }

    fun addMember(groupId: Int, adminUserId: Int, targetUserId: Int) {
        val gId = GroupId(groupId)
        val adminId = UserId(adminUserId)
        val targetId = UserId(targetUserId)

        if (!groupRepository.isUserAdmin(gId, adminId)) {
            throw PermissionDeniedException("Only admins can add members")
        }

        if (groupRepository.isUserMember(gId, targetId)) {
            throw ValidationException("User is already a member")
        }

        groupRepository.addMember(gId, targetId)
    }

    fun kickMember(groupId: Int, adminUserId: Int, targetUserId: Int) {
        val gId = GroupId(groupId)
        val adminId = UserId(adminUserId)
        val targetId = UserId(targetUserId)
        
        println("GroupService: kickMember called - groupId=$groupId, adminId=$adminUserId, targetId=$targetUserId")
        
        val isAdmin = groupRepository.isUserAdmin(gId, adminId)
        println("GroupService: isUserAdmin check result: $isAdmin")
        
        if (!isAdmin) {
            println("GroupService: DENIED - user is not admin")
            throw PermissionDeniedException("Only admins can kick members")
        }
        
        if (adminUserId == targetUserId) {
            println("GroupService: DENIED - admin trying to kick self")
            throw ValidationException("Admin cannot kick themselves")
        }

        val success = groupRepository.removeMember(gId, targetId)
        println("GroupService: removeMember result: $success")
        
        if (!success) {
            println("GroupService: FAILED - member not found")
            throw ResourceNotFoundException("Member", targetUserId.toString())
        }
        
        println("GroupService: SUCCESS - member kicked")
    }

    fun deleteGroup(groupId: Int, userId: Int) {
        val gId = GroupId(groupId)
        val adminId = UserId(userId)
        
        if (!groupRepository.isUserAdmin(gId, adminId)) {
            throw PermissionDeniedException("Only admins can delete groups")
        }
        
        val success = groupRepository.deleteGroup(gId)
        if (!success) {
            throw ResourceNotFoundException("Group", groupId.toString())
        }
    }

    fun leaveGroup(groupId: Int, userId: Int) {
        val gId = GroupId(groupId)
        val uId = UserId(userId)
        
        if (!groupRepository.isUserMember(gId, uId)) {
            throw ResourceNotFoundException("Member", userId.toString())
        }
        
        // If admin is leaving, check if there are other members
        val isAdmin = groupRepository.isUserAdmin(gId, uId)
        if (isAdmin) {
            val members = groupRepository.findGroupMembers(gId)
            if (members.size > 1) {
                throw ValidationException("Admin cannot leave group with other members. Delete the group or kick all members first.")
            }
            // Admin is the only member, delete the group
            groupRepository.deleteGroup(gId)
        } else {
            // Regular member just leaves
            groupRepository.removeMember(gId, uId)
        }
    }

    fun getGroup(groupId: Int, userId: Int): GroupResponse {
        val gId = GroupId(groupId)
        val uId = UserId(userId)

        if (!groupRepository.isUserMember(gId, uId)) {
            throw PermissionDeniedException("Not a member of this group")
        }

        val group = groupRepository.findGroup(gId) ?: throw NotFoundException("Group not found")
        val admin = groupRepository.isUserAdmin(gId, uId)
        val role = if (admin) MemberRole.ADMIN else MemberRole.MEMBER
        
        return group.toResponse(role)
    }
    
    private fun Group.toResponse(myRole: MemberRole): GroupResponse {
        return GroupResponse(
            id = id.value,
            name = name,
            createdBy = createdBy.value,
            createdAt = createdAt,
            myRole = myRole.name.lowercase()
        )
    }
    
    private fun GroupMembership.toResponse(): GroupMemberResponse {
        return GroupMemberResponse(
            userId = userId.value,
            name = user?.displayName ?: "",
            username = user?.username ?: "",
            avatarUrl = user?.avatarUrl,
            role = role.name.lowercase(),
            joinedAt = joinedAt
        )
    }
}
