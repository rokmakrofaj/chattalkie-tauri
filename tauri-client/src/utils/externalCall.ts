/**
 * External Browser Call Utility
 * 
 * Opens video/audio calls in the system's default browser.
 * This is the correct solution for Tauri on Linux where WebKitGTK
 * does NOT support camera/microphone access.
 */

// Base URL for calls
const CALL_BASE_URL = import.meta.env.DEV
    ? 'http://localhost:1420'
    : 'https://chat.talkie.app';

/**
 * Opens a call page in the system's default browser.
 * Uses Tauri's opener plugin or xdg-open on Linux.
 */
export async function openCallInBrowser(
    callerId: number,
    receiverId: number,
    callType: 'video' | 'audio',
    token: string
): Promise<void> {
    const callUrl = `${CALL_BASE_URL}/call?caller=${callerId}&receiver=${receiverId}&type=${callType}&token=${encodeURIComponent(token)}`;

    console.log('📞 Opening call in external browser:', callUrl);

    try {
        // Check if we're in Tauri
        if ('__TAURI__' in window) {
            // Use Tauri's opener plugin
            const { invoke } = await import('@tauri-apps/api/core');
            await invoke('plugin:opener|open_url', { url: callUrl });
            console.log('✅ Browser opened via Tauri opener plugin');
        } else {
            // Fallback for web
            window.open(callUrl, '_blank');
        }
    } catch (error) {
        console.warn('Primary method failed, trying fallback:', error);
        // Last resort - window.open
        const newWindow = window.open(callUrl, '_blank');
        if (!newWindow) {
            // If popup blocked, navigate current window
            alert('Tarayıcı açılamadı. Lütfen bu URL\'yi kopyalayın:\n' + callUrl);
        }
    }
}

/**
 * Checks if we're running in Tauri on Linux.
 */
export function isTauriLinux(): boolean {
    try {
        const isTauri = typeof window !== 'undefined' && '__TAURI__' in window;
        if (!isTauri) return false;

        const userAgent = navigator.userAgent.toLowerCase();
        return userAgent.includes('linux') && !userAgent.includes('android');
    } catch {
        return false;
    }
}

/**
 * Determines if calls should open in external browser.
 */
export function shouldUseExternalBrowserForCalls(): boolean {
    return isTauriLinux();
}
