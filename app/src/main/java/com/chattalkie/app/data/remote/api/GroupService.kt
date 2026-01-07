package com.chattalkie.app.data.remote.api

import com.chattalkie.app.data.remote.model.GroupRequest
import com.chattalkie.app.data.remote.model.GroupResponse
import com.chattalkie.app.data.remote.model.GroupMemberResponse
import com.chattalkie.app.data.remote.model.InviteLinkResponse
import com.chattalkie.app.data.remote.model.JoinGroupResponse
import retrofit2.http.*

interface GroupService {
    @POST("api/groups")
    suspend fun createGroup(
        @Header("Authorization") token: String,
        @Body request: GroupRequest
    ): Map<String, Int>

    @GET("api/groups")
    suspend fun getGroups(
        @Header("Authorization") token: String
    ): List<GroupResponse>

    @GET("api/groups/{groupId}/members")
    suspend fun getGroupMembers(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int
    ): List<GroupMemberResponse>

    @GET("api/groups/{groupId}")
    suspend fun getGroup(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int
    ): GroupResponse

    @POST("api/groups/{groupId}/kick")
    suspend fun kickMember(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int,
        @Body request: com.chattalkie.app.data.remote.model.MemberKickRequest
    ): Map<String, String>

    @POST("api/groups/{groupId}/invite")
    suspend fun createInviteLink(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int
    ): InviteLinkResponse

    @GET("api/groups/{groupId}/invite")
    suspend fun getInviteLink(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int
    ): InviteLinkResponse

    @POST("api/groups/join/{token}")
    suspend fun joinGroupByInvite(
        @Header("Authorization") token: String,
        @Path("token") inviteToken: String
    ): JoinGroupResponse

    @DELETE("api/groups/{groupId}")
    suspend fun deleteGroup(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int
    ): Map<String, String>

    @POST("api/groups/{groupId}/leave")
    suspend fun leaveGroup(
        @Header("Authorization") token: String,
        @Path("groupId") groupId: Int
    ): Map<String, String>
}
