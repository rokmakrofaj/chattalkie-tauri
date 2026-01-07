import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface AuthState {
    token: string | null;
    userId: number | null;
    username: string | null;
    name: string | null;
    avatar_url: string | null;
    setAuth: (token: string, userId: number, username: string, name: string, avatar_url?: string | null) => void;
    updateProfile: (name: string, username: string) => void;
    updateAvatar: (url: string) => void;
    logout: () => void;
}

export const useAuthStore = create<AuthState>()(
    persist(
        (set) => ({
            token: null,
            userId: null,
            username: null,
            name: null,
            avatar_url: null,
            setAuth: (token, userId, username, name, avatar_url = null) => set({ token, userId, username, name, avatar_url }),
            updateProfile: (name, username) => set({ name, username }),
            updateAvatar: (url: string) => set({ avatar_url: url }),
            logout: () => set({ token: null, userId: null, username: null, name: null, avatar_url: null }),
        }),
        {
            name: 'auth-storage',
        }
    )
);
