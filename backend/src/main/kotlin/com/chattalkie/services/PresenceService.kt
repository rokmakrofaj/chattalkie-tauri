package com.chattalkie.services

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages user presence (online/offline) in memory.
 * 
 * CRITICAL ARCHITECTURE DECISION:
 * Presence is NOT persisted to the database to prevent transaction conflicts 
 * during high-concurrency connect/disconnect events.
 * 
 * - Thread-safe (ConcurrentHashMap)
 * - Connection counting (AtomicInteger) to support multiple devices
 */
class PresenceService {
    // Map<UserId, ConnectionCount>
    private val onlineUsers = ConcurrentHashMap<Int, AtomicInteger>()

    /**
     * Registers a new connection for a user.
     * @return true if the user just went online (was previously offline)
     */
    fun connect(userId: Int): Boolean {
        val count = onlineUsers.computeIfAbsent(userId) { AtomicInteger(0) }
        val current = count.incrementAndGet()
        // If count is 1, they just came online
        return current == 1
    }

    /**
     * Unregisters a connection for a user.
     * @return true if the user just went offline (last connection closed)
     */
    fun disconnect(userId: Int): Boolean {
        val count = onlineUsers[userId] ?: return false
        val current = count.decrementAndGet()
        
        if (current <= 0) {
            onlineUsers.remove(userId)
            return true
        }
        return false
    }

    /**
     * Checks if a user is currently online.
     */
    fun isOnline(userId: Int): Boolean {
        return onlineUsers.containsKey(userId)
    }

    /**
     * Bulk check for a list of users.
     * @return List of userIds that are online.
     */
    fun filterOnlineUsers(userIds: List<Int>): List<Int> {
        return userIds.filter { isOnline(it) }
    }
}
