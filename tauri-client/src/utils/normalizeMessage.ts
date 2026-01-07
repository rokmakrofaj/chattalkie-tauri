
import { LocalMessage } from '../db/db';

export const normalizeMessage = (raw: any, currentUserId?: number): LocalMessage | null => {
    // STRICT: Inbound must have CID. If it's internal creation, cid might be passed, but typically we require it.
    // If raw is from WS/Sync, it MUST have a CID.
    if (!raw.cid && !raw.messageId) {
        // Only if we are creating it ourselves locally (optimistic) might it lack CID initially, but usually we pass it.
        // For inbound, we drop it.
        console.warn('Dropped message with no CID/MessageID:', raw);
        return null;
    }

    const cid = raw.cid || raw.messageId; // Fallback if needed, but preferably cid

    let messageType = raw.messageType || raw.type || 'text';

    // Robust Inference only if type is missing or generic 'text' but has mediaKey
    if ((!raw.messageType || raw.messageType === 'text') && raw.mediaKey) {
        const ext = raw.mediaKey.split('.').pop()?.toLowerCase();
        if (['webm', 'm4a', 'mp3', 'ogg', 'wav'].includes(ext)) {
            messageType = 'voice';
        } else if (['jpg', 'jpeg', 'png', 'gif'].includes(ext)) {
            messageType = 'image';
        } else {
            messageType = 'file';
        }
    }

    // Robust ChatID Derivation
    // For groups: groupId is paramount.
    // For DMs: If I am sender, stored under Recipient. If I am receiver, stored under Sender.
    let chatId: number = 0;
    if (raw.groupId) {
        chatId = raw.groupId;
    } else {
        const senderId = raw.senderId;
        const recipientId = raw.recipientId || raw.receiverId;

        if (currentUserId) {
            chatId = (senderId === currentUserId) ? recipientId : senderId;
        } else {
            // Fallback if userId not available (rare/bad), rely on what we have, might be wrong context
            chatId = senderId;
        }
    }

    return {
        cid: cid,
        chatId: chatId,
        serverId: raw.messageId,
        senderId: raw.senderId,
        senderName: raw.senderName,
        senderAvatar: raw.senderAvatar,
        content: raw.content || "",
        mediaKey: raw.mediaKey,
        messageType,
        status: raw.status || 'SENT', // Default to SENT for inbound
        isMine: currentUserId ? raw.senderId === currentUserId : false,
        createdAt: raw.timestamp || Date.now()
    };
};
