package com.chattalkie.app.data.remote.model

import com.google.gson.annotations.SerializedName

data class SyncResponse(
    @SerializedName("lastTs") val lastTs: Long,
    @SerializedName("hasMore") val hasMore: Boolean,
    // Note: Backend sends 'messages' as polymorphic domain objects.
    // Retrofit + Gson might struggle with Sealed Classes unless we write a custom adapter.
    // Simplifying assumption: Backend maps them to MessageResponse-like items for Sync too,
    // OR we use the models defined in backend/domain/Message.kt if we can share them (KMP).
    // DTO approach:
    @SerializedName("messages") val messages: List<MessageResponse>,
    @SerializedName("tombstones") val tombstones: List<Tombstone>
)

data class Tombstone(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("type") val type: String,
    @SerializedName("deletedAt") val deletedAt: Long
)
