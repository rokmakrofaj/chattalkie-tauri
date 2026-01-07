import { useLiveQuery } from 'dexie-react-hooks';
import Dexie from 'dexie';
import { db } from '../db/db';

export const useChats = () => {
    return useLiveQuery(() => {
        return db.chats.orderBy('lastMessageTime').reverse().toArray();
    }, []);
};

export const useMessages = (chatId: number) => {
    return useLiveQuery(async () => {
        // Return messages for this chat, sorted by time
        // Limit to 50 for performance, simple pagination
        return await db.messages
            .where('[chatId+createdAt]')
            .between([chatId, Dexie.minKey], [chatId, Dexie.maxKey])
            .reverse()
            .limit(50)
            .toArray();
    }, [chatId]);
};
