package com.chattalkie.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class FriendRequest(@SerializedName("friendId") val friendId: Int)

data class FriendActionRequest(
    @SerializedName("userId") val userId: Int,
    @SerializedName("action") val action: String // "ACCEPT" or "REJECT"
)

data class FriendResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("username") val username: String,
    @SerializedName("name") val name: String,
    @SerializedName("avatarUrl") val avatarUrl: String?,
    @SerializedName("status") val status: String,
    @SerializedName("friendShipStatus") val friendShipStatus: String // "PENDING", "ACCEPTED"
)
