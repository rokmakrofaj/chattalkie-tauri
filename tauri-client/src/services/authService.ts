import { apiClient } from '../api/client';
import { useAuthStore } from '../hooks/useAuth';

// Matches backend AuthResponse
export interface LoginResponse {
    token: string;
    user: {
        id: number;
        username: string;
        name: string;
        avatarUrl?: string;
        status: string;
    };
}

export const authService = {
    login: async (username: string, password: string): Promise<LoginResponse> => {
        const response = await apiClient.post<LoginResponse>('/api/auth/login', { username, password });

        if (response.data.token) {
            useAuthStore.getState().setAuth(
                response.data.token,
                response.data.user.id,
                response.data.user.username,
                response.data.user.name,
                response.data.user.avatarUrl
            );
        }

        return response.data;
    },

    register: async (name: string, username: string, password: string): Promise<LoginResponse> => {
        const response = await apiClient.post<LoginResponse>('/api/auth/register', { name, username, password });
        return response.data;
    }
};
