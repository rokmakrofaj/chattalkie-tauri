import Dexie, { Table } from 'dexie';

export interface LocalMessage {
    cid: string;       // Primary Key (UUID)
    chatId: number;    // GroupID or PartnerID
    serverId?: string; // Nullable (set on ACK/Sync)
    senderId: number;
    senderName?: string;
    senderAvatar?: string;
    content: string;
    createdAt: number;
    status: 'SENDING' | 'SENT' | 'DELIVERED' | 'READ' | 'FAILED'; // Simplified Status
    mediaKey?: string;
    messageType?: string; // 'text', 'image', 'voice' etc.
    isMine: boolean;
}

export interface LocalChat {
    id: number;
    name: string;
    lastMessage?: string;
    lastMessageTime: number;
    unreadCount: number;
    isGroup: boolean;
    avatarUrl?: string;
}

export interface Draft {
    chatId: number;
    content: string;
    type: 'text' | 'voice';
    mediaKey?: string;
    updatedAt: number;
}

export class ChatDatabase extends Dexie {
    messages!: Table<LocalMessage, string>; // PK is string (cid)
    chats!: Table<LocalChat, number>;
    kvStore!: Table<{ key: string; value: any }, string>;
    drafts!: Table<Draft, number>;

    constructor() {
        super('ChatTalkieDB');
        this.version(1).stores({
            messages: '&cid, [chatId+createdAt], serverId, status',
            chats: 'id, lastMessageTime',
            kvStore: '&key',
        });

        // Version 2: Add drafts table
        this.version(2).stores({
            messages: '&cid, [chatId+createdAt], serverId, status',
            chats: 'id, lastMessageTime',
            kvStore: '&key',
            drafts: '&chatId, updatedAt' // One draft per chat
        });
    }
}

export const db = new ChatDatabase();
