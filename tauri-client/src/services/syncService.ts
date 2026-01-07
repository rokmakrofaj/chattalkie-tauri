import { db } from '../db/db';
import { useAuthStore } from '../hooks/useAuth';
import { apiClient } from '../api/client';

// Sync friends and groups to populate chats list
const syncChats = async () => {
    try {
        console.log('Syncing chats...');
        // Fetch friends (for DM chats)
        const friendsRes = await apiClient.get('/api/friends');
        console.log(`Sync: Found ${friendsRes.data?.length || 0} friends`);

        // Fetch groups
        const groupsRes = await apiClient.get('/api/groups');
        console.log(`Sync: Found ${groupsRes.data?.length || 0} groups`);

        await db.transaction('rw', db.chats, async () => {
            const existingChats = await db.chats.toArray();
            const existingMap = new Map(existingChats.map(c => [c.id, c]));

            // Map friends to chats
            const friendChats = (friendsRes.data || []).map((f: any) => {
                const existing = existingMap.get(f.friendId || f.id);
                return {
                    id: f.friendId || f.id,
                    name: f.name || f.username,
                    lastMessage: f.lastMessage || existing?.lastMessage, // Preserve local
                    lastMessageTime: f.lastMessageTime || existing?.lastMessageTime || 0,
                    unreadCount: f.unreadCount || existing?.unreadCount || 0,
                    isGroup: false,
                    avatarUrl: f.avatarUrl
                };
            });

            // Map groups to chats
            const groupChats = (groupsRes.data || []).map((g: any) => {
                const existing = existingMap.get(g.id);
                return {
                    id: g.id,
                    name: g.name,
                    lastMessage: g.lastMessage || existing?.lastMessage, // Preserve local
                    lastMessageTime: g.lastMessageTime || existing?.lastMessageTime || 0,
                    unreadCount: g.unreadCount || existing?.unreadCount || 0,
                    isGroup: true,
                    avatarUrl: g.avatarUrl
                };
            });

            const allRemoteChats = [...friendChats, ...groupChats];

            // Upsert all chats
            await db.chats.bulkPut(allRemoteChats);

            // Prune deleted chats (Hard Sync) - DISABLED to prevent accidental wipe on network error
            /*
            const remoteIds = new Set(allRemoteChats.map(c => c.id));
            const toDelete = existingChats
                .map(c => c.id)
                .filter(id => !remoteIds.has(id));

            if (toDelete.length > 0) {
                console.log('Sync pruning deleted chats:', toDelete);
                await db.chats.bulkDelete(toDelete);
            }
            */
        });
    } catch (e) {
        console.error('Chat sync failed:', e);
    }
};

// Sync messages from backend
const syncMessages = async (userId: number) => {
    try {
        let hasMore = true;

        while (hasMore) {
            // Get last sync timestamp
            const meta = await db.kvStore.get('last_sync_ts');
            const lastTs = meta?.value || 0;

            // Fetch delta from backend
            // Fetch delta from backend
            const res = await apiClient.get('/api/sync', {
                params: { last_ts: lastTs }
            });

            const { messages, tombstones, nextTs, hasMore: more } = res.data;
            const newTs = nextTs; // Map backend field to local variable
            hasMore = more ?? false;

            await db.transaction('rw', db.messages, db.chats, db.kvStore, async () => {
                // Upsert messages with proper isMine mapping
                if (messages?.length > 0) {
                    const mapped = messages
                        .filter((m: any) => m.cid)
                        .map((m: any) => {
                            // if (m.senderName) console.log('Sync received senderName:', m.senderName, 'for msg:', m.cid);
                            const isMine = m.senderId === userId;
                            // For direct messages: chatId should be partner's ID
                            // - If I sent it: chatId = receiverId (partner)
                            // - If they sent it: chatId = senderId (partner)
                            // For group messages: chatId = groupId
                            let chatId: number;
                            if (m.groupId) {
                                chatId = m.groupId;
                            } else {
                                // Direct message: use partner's ID
                                chatId = isMine ? (m.receiverId || m.senderId) : m.senderId;
                            }

                            return {
                                cid: m.cid,
                                chatId,
                                serverId: m.messageId,
                                senderId: m.senderId,
                                senderName: m.senderName,
                                senderAvatar: m.senderAvatar,
                                content: m.content,
                                mediaKey: m.mediaKey,
                                messageType: m.messageType || (m.mediaKey ? (
                                    m.mediaKey.endsWith('.webm') || m.mediaKey.endsWith('.m4a') || m.mediaKey.endsWith('.mp3') ? 'voice' : 'image'
                                ) : 'text'),
                                createdAt: m.timestamp,
                                status: 'SENT' as const,
                                isMine
                            };
                        });

                    await db.messages.bulkPut(mapped);

                    // Update chat lastMessage
                    for (const msg of mapped) {
                        const preview = (msg.messageType === 'voice') ? '🎤 Sesli Mesaj' : (msg.content || (msg.mediaKey ? '📷 Fotoğraf' : ''));
                        await db.chats.update(msg.chatId, {
                            lastMessage: preview,
                            lastMessageTime: msg.createdAt
                        });
                    }
                }

                // Handle tombstones (deleted messages)
                if (tombstones?.length > 0) {
                    const serverIds = tombstones
                        .filter((t: any) => t.type === 'MESSAGE')
                        .map((t: any) => t.itemId);

                    if (serverIds.length > 0) {
                        await db.messages.where('serverId').anyOf(serverIds).delete();
                    }
                }

                // Update sync cursor
                if (newTs) {
                    await db.kvStore.put({ key: 'last_sync_ts', value: newTs });
                }
            });
        }
    } catch (e) {
        console.error('Message sync failed:', e);
    }
};

// Lock to prevent overlapping syncs
let isSyncing = false;

// Main sync entry point
export const performSync = async () => {
    if (isSyncing) {
        console.log('Sync skipped: already in progress');
        return;
    }

    const { token, userId } = useAuthStore.getState();
    if (!token || !userId) {
        console.log('Sync skipped: not authenticated');
        return;
    }

    try {
        isSyncing = true;
        console.log('Starting sync...');

        // Sync chats first, then messages
        await syncChats();
        await syncMessages(userId);

        console.log('Sync completed');
    } catch (e) {
        console.error('Sync failed:', e);
    } finally {
        isSyncing = false;
    }
};

// Periodic sync (call from App.tsx)
export const startPeriodicSync = (intervalMs: number = 30000) => {
    performSync(); // Initial sync
    return setInterval(performSync, intervalMs);
};
