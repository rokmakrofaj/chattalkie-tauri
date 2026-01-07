package com.chattalkie.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class InviteLinkResponse(
    @SerializedName("token") val token: String,
    @SerializedName("groupName") val groupName: String,
    @SerializedName("groupId") val groupId: Int,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("expiresAt") val expiresAt: Long? = null
)

data class JoinGroupRequest(
    @SerializedName("inviteToken") val inviteToken: String
)

data class JoinGroupResponse(
    @SerializedName("message") val message: String,
    @SerializedName("groupName") val groupName: String
)
