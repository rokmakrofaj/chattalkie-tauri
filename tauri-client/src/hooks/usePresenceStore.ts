import { create } from 'zustand';

interface PresenceState {
    onlineUsers: Set<number>;
    typingUsers: Record<number, number[]>; // chatId -> userIds
    setOnlineUsers: (userIds: number[]) => void;
    setUserOnline: (userId: number) => void;
    setUserOffline: (userId: number) => void;
    setTyping: (chatId: number, userId: number, isTyping: boolean) => void;
    isOnline: (userId: number) => boolean;
}

export const usePresenceStore = create<PresenceState>((set, get) => ({
    onlineUsers: new Set(),
    typingUsers: {}, // chatId -> [userId1, userId2]

    setOnlineUsers: (userIds) => set({ onlineUsers: new Set(userIds) }),

    setUserOnline: (userId) => set((state) => {
        const newSet = new Set(state.onlineUsers);
        newSet.add(userId);
        return { onlineUsers: newSet };
    }),

    setUserOffline: (userId) => set((state) => {
        const newSet = new Set(state.onlineUsers);
        newSet.delete(userId);
        return { onlineUsers: newSet };
    }),

    setTyping: (chatId, userId, isTyping) => set((state) => {
        const currentTypers = state.typingUsers[chatId] || [];
        if (isTyping) {
            if (!currentTypers.includes(userId)) {
                return { typingUsers: { ...state.typingUsers, [chatId]: [...currentTypers, userId] } };
            }
        } else {
            // Remove
            const updated = currentTypers.filter(id => id !== userId);
            // If empty, cleaner to keep it or delete key? Keep it simple.
            return { typingUsers: { ...state.typingUsers, [chatId]: updated } };
        }
        return {};
    }),

    isOnline: (userId) => get().onlineUsers.has(userId)
}));
