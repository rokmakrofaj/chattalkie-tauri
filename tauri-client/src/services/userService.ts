import { apiClient } from '../api/client';

export interface User {
    id: number;
    username: string;
    name: string;
    avatarUrl?: string | null;
    status: string;
}

export const userService = {
    getMe: async (): Promise<User> => {
        const response = await apiClient.get('/api/users/me');
        return response.data;
    },

    updateProfile: async (name: string, username: string): Promise<User> => {
        const response = await apiClient.put('/api/users/me', { name, username });
        return response.data;
    },

    uploadAvatar: async (file: File): Promise<string> => {
        const formData = new FormData();
        formData.append('image', file);
        const response = await apiClient.post('/api/users/me/avatar', formData, {
            headers: {
                'Content-Type': 'multipart/form-data',
            },
        });
        return response.data.avatarUrl;
    },

    searchUsers: async (query: string): Promise<User[]> => {
        const response = await apiClient.get(`/api/users/search?query=${encodeURIComponent(query)}`);
        return response.data;
    },
};
