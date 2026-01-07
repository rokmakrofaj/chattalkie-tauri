import axios from 'axios';
import { useAuthStore } from '../hooks/useAuth';

// Use localhost for development.
// In production, this might be configurable or point to a real server.
// For Tauri android/emulator, it's 10.0.2.2, but for Desktop it's localhost or the LAN IP.
// User's backend seems to be running locally.
// Hardcoded LAN IP for consistent "Single IP" usage as requested.
export const BASE_URL = 'http://localhost:8080';

export const apiClient = axios.create({
    baseURL: BASE_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Add interceptor to inject token
apiClient.interceptors.request.use((config) => {
    const token = useAuthStore.getState().token;
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});
