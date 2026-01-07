import { useState, useEffect } from 'react';
import { useAuthStore } from './hooks/useAuth';
import { userService } from './services/userService';
import LoginScreen from './components/auth/LoginScreen';
import RegisterScreen from './components/auth/RegisterScreen';
import ChatInterface from './components/chat/ChatInterface';
import CallPage from './components/call/CallPage';

// Check if this is a call page (URL has /call or ?caller= param)
function isCallPage(): boolean {
    const path = window.location.pathname;
    const search = window.location.search;
    return path === '/call' || path.endsWith('/call') || search.includes('caller=');
}

// Main Auth Container
const AuthScreen = () => {
    const [currentPage, setCurrentPage] = useState<'login' | 'register'>('login');

    if (currentPage === 'login') {
        return <LoginScreen onNavigateToRegister={() => setCurrentPage('register')} />;
    } else {
        return <RegisterScreen onNavigateToLogin={() => setCurrentPage('login')} />;
    }
};

function App() {
    const token = useAuthStore((state) => state.token);
    const setAuth = useAuthStore((state) => state.setAuth);

    useEffect(() => {
        if (token) {
            userService.getMe().then(user => {
                setAuth(token, user.id, user.username, user.name, user.avatarUrl);
            }).catch(err => {
                console.error("Failed to refresh profile:", err);
                // Optional: Logout if token is invalid
                if (err.response?.status === 401) {
                    useAuthStore.getState().logout();
                }
            });
        }
    }, [token, setAuth]);

    // If this is a call page, render only the call UI
    if (isCallPage()) {
        return <CallPage />;
    }

    return (
        <div className="h-full w-full">
            {token ? <ChatInterface /> : <AuthScreen />}
        </div>
    );
}

export default App;
