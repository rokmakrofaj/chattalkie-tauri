import { useState, useEffect } from 'react';
import { useAuthStore } from '../../hooks/useAuth';
import { useSocket } from '../../hooks/useSocket';
import { performSync } from '../../services/syncService';
import { useChats, useMessages } from '../../hooks/useQueries';
import { db } from '../../db/db';

import Sidebar from './Sidebar';
import ChatWindow from './ChatWindow';
import ContactsModal from './ContactsModal';
import ProfileModal from './ProfileModal';
import CreateGroupModal from './CreateGroupModal';
import CallModal from './CallModal';
import './ChatInterface.css';

const ChatInterface = () => {
    const { connect, sendMessage, resendMessage, sendSignal, sendTyping, sendReadReceipt } = useSocket();
    const chats = useChats();
    const [selectedChatId, setSelectedChatId] = useState<number | null>(null);
    const [showContacts, setShowContacts] = useState(false);
    const [showProfile, setShowProfile] = useState(false);
    const [showCreateGroup, setShowCreateGroup] = useState(false);

    const currentUser = useAuthStore(state => state.username);
    const userId = useAuthStore(state => state.userId);
    const storeLogout = useAuthStore(state => state.logout);

    const messages = useMessages(selectedChatId || 0);

    // Connection & Sync Logic
    useEffect(() => {
        connect();
        performSync();
        const syncInterval = setInterval(performSync, 30000);
        return () => clearInterval(syncInterval);
    }, [connect]);

    let selectedChat = chats?.find(c => c.id === selectedChatId);

    // Logic for Saved Messages (Self Chat)
    if (selectedChatId && userId && selectedChatId === userId && !selectedChat) {
        selectedChat = {
            id: userId,
            name: 'Kayıtlı Mesajlar',
            isGroup: false,
            lastMessage: '',
            lastMessageTime: Date.now(),
            unreadCount: 0,
            avatarUrl: useAuthStore.getState().avatar_url || undefined
        };
    } else if (selectedChatId && userId && selectedChatId === userId && selectedChat) {
        // Override name if it exists but we want to show it as Saved Messages in the header
        selectedChat = { ...selectedChat, name: 'Kayıtlı Mesajlar' };
    }

    // Handlers
    const handleSend = (text: string, mediaUrl?: string, messageType?: string) => {
        if (!selectedChatId) return;
        sendMessage(text, selectedChatId, selectedChat?.isGroup || false, mediaUrl, messageType);
    };

    const handleLogout = async () => {
        if (confirm('Çıkış yapıp tüm yerel verileri temizlemek istiyor musunuz?')) {
            await db.messages.clear();
            await db.chats.clear();
            await db.kvStore.clear();
            storeLogout();
            window.location.reload();
        }
    };

    const handleNewChat = () => {
        setShowContacts(true);
    };

    return (
        <div className="chat-interface">
            <Sidebar
                chats={chats || []}
                currentUser={currentUser}
                selectedChatId={selectedChatId}
                onSelectChat={setSelectedChatId}
                onLogout={handleLogout}
                onNewChat={handleNewChat}
                onOpenProfile={() => setShowProfile(true)}
                onCreateGroup={() => setShowCreateGroup(true)}
                onRefresh={performSync}
            />
            <ChatWindow
                selectedChatId={selectedChatId}
                selectedChat={selectedChat}
                messages={messages}
                onSend={handleSend}
                onResend={resendMessage}
                onTyping={sendTyping}
                onRead={sendReadReceipt}
            />
            {showContacts && (
                <ContactsModal
                    chats={chats || []}
                    onSelectChat={setSelectedChatId}
                    onClose={() => setShowContacts(false)}
                />
            )}
            {showProfile && (
                <ProfileModal onClose={() => setShowProfile(false)} />
            )}
            {showCreateGroup && (
                <CreateGroupModal
                    onClose={() => setShowCreateGroup(false)}
                    onGroupCreated={(newGroupId) => {
                        performSync(); // Refresh chats
                        setSelectedChatId(newGroupId);
                    }}
                />
            )}
            <CallModal sendSignal={sendSignal} />
        </div>
    );
};

export default ChatInterface;
