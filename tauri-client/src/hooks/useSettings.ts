import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface SettingsState {
    theme: 'system' | 'light' | 'dark';
    soundEnabled: boolean;
    notificationsEnabled: boolean;
    setTheme: (theme: 'system' | 'light' | 'dark') => void;
    setSoundEnabled: (enabled: boolean) => void;
    setNotificationsEnabled: (enabled: boolean) => void;
}

export const useSettings = create<SettingsState>()(
    persist(
        (set) => ({
            theme: 'system',
            soundEnabled: true,
            notificationsEnabled: true,
            setTheme: (theme) => {
                set({ theme });
                applyTheme(theme);
            },
            setSoundEnabled: (enabled) => set({ soundEnabled: enabled }),
            setNotificationsEnabled: (enabled) => set({ notificationsEnabled: enabled }),
        }),
        {
            name: 'chattalkie-settings',
            onRehydrateStorage: () => (state) => {
                if (state) {
                    applyTheme(state.theme);
                }
            }
        }
    )
);

function applyTheme(theme: 'system' | 'light' | 'dark') {
    const root = window.document.documentElement;
    const isDark =
        theme === 'dark' ||
        (theme === 'system' && window.matchMedia('(prefers-color-scheme: dark)').matches);

    if (isDark) {
        root.classList.add('dark');
    } else {
        root.classList.remove('dark');
    }
}

// Listen for system theme changes if 'system' is selected
window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    const currentState = JSON.parse(localStorage.getItem('chattalkie-settings') || '{}');
    if (currentState?.state?.theme === 'system') {
        applyTheme('system');
    }
});
