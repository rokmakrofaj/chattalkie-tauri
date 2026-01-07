import { useRef, useLayoutEffect, useState, useEffect } from 'react';
import ChatBubble from './ChatBubble';
import InputArea from './InputArea';
import GroupInfoModal from './GroupInfoModal';
import CallBar from './CallBar';
import { getAvatarColor, getInitials } from '../../utils/uiUtils';
import { LocalMessage, db } from '../../db/db';
import { usePresenceStore } from '../../hooks/usePresenceStore';
import { performSync } from '../../services/syncService';
import './ChatWindow.css';

interface ChatWindowProps {
    selectedChatId: number | null; // Added prop
    selectedChat: any | null;
    messages: LocalMessage[] | undefined;
    onSend: (text: string, mediaKey?: string) => void;
    onResend?: (cid: string) => void; // Added onResend
    onTyping: (chatId: number, isGroup: boolean, isTyping: boolean) => void;
    onRead: (cid: string, messageId: string | undefined, senderId: number, isGroup: boolean, groupId?: number) => void;
}

const ChatWindow = ({ selectedChatId, selectedChat, messages, onSend, onResend, onTyping, onRead }: ChatWindowProps) => {
    const messagesEndRef = useRef<HTMLDivElement>(null);
    const onlineUsers = usePresenceStore(state => state.onlineUsers);
    const typingUsers = usePresenceStore(state => state.typingUsers);
    const isOnline = selectedChat ? onlineUsers.has(selectedChat.id) : false;
    const isTyping = selectedChat && typingUsers[selectedChat.id]?.length > 0;
    const [showGroupInfo, setShowGroupInfo] = useState(false);
    const [isSearchOpen, setIsSearchOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');

    // Auto-scroll
    useLayoutEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'auto' });
    }, [messages, selectedChat]);

    // Read Receipt Logic
    useEffect(() => {
        if (!messages || !selectedChat) return;

        // Find unread incoming messages and mark as read
        const unreadMessages = messages.filter(m => !m.isMine && m.status !== 'READ');
        if (unreadMessages.length > 0) {
            unreadMessages.forEach(msg => {
                onRead(msg.cid, msg.serverId, msg.senderId, selectedChat.isGroup, selectedChat.isGroup ? selectedChat.id : undefined);
                db.messages.update(msg.cid, { status: 'READ' }).catch(() => { });
            });
        }
    }, [messages, selectedChat, onRead]);

    if (!selectedChat) {
        // If we have an ID but no chat object (loading or error), show loading or fallback
        if (selectedChatId) {
            return (
                <div className="chat-window" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <p>Sohbet yükleniyor... ({selectedChatId})</p>
                </div>
            );
        }

        return (
            <div className="chat-window">
                <div className="no-chat-selected">
                    <div className="no-chat-icon">
                        <svg width="40" height="40" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"></path></svg>
                    </div>
                    <h3>Hoş Geldiniz!</h3>
                    <p>Mesajlaşmaya başlamak için soldan bir sohbet seçin.</p>
                </div>
            </div>
        );
    }

    const sortedMessages = messages ? [...messages].reverse() : [];

    // Filter messages based on search query
    const displayedMessages = searchQuery
        ? sortedMessages.filter(m => m.content?.toLowerCase().includes(searchQuery.toLowerCase()))
        : sortedMessages;

    return (
        <div className="chat-window">
            {/* Header */}
            <div className="chat-window-header">
                <div className="header-user">
                    <div
                        className="header-avatar"
                        style={{
                            backgroundColor: selectedChat.name === 'Kayıtlı Mesajlar' ? '#3b82f6' : (selectedChat.avatarUrl ? 'transparent' : getAvatarColor(selectedChat.name)),
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center'
                        }}
                    >
                        {selectedChat.name === 'Kayıtlı Mesajlar' ? (
                            <svg width="24" height="24" fill="none" stroke="white" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z" />
                            </svg>
                        ) : selectedChat.avatarUrl ? (
                            <img
                                src={selectedChat.avatarUrl}
                                alt={selectedChat.name}
                                style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }}
                            />
                        ) : (
                            getInitials(selectedChat.name)
                        )}
                    </div>
                    <div className="header-info">
                        <h2>{selectedChat.name}</h2>
                        <div className="header-status">
                            {isTyping ? (
                                <span style={{ color: '#22c55e', fontWeight: 'bold' }}>Yazıyor...</span>
                            ) : selectedChat.name === 'Kayıtlı Mesajlar' ? (
                                <span className="text-blue-500">Kayıtlı Mesajlar</span>
                            ) : isOnline ? (
                                <>
                                    <span className="status-dot"></span>
                                    <span>Çevrimiçi</span>
                                </>
                            ) : (
                                <span style={{ opacity: 0.6 }}>Çevrimdışı</span>
                            )}
                        </div>
                    </div>
                </div>
                <div className="header-actions">
                    <button
                        onClick={() => {
                            setIsSearchOpen(!isSearchOpen);
                            setSearchQuery('');
                        }}
                        className={isSearchOpen ? 'active' : ''}
                        title="Ara"
                    >
                        <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path></svg>
                    </button>

                    <button
                        onClick={() => {
                            if (selectedChat && !selectedChat.isGroup) {
                                // Use in-app call with native media capture
                                import('../../hooks/useCallStore').then(({ useCallStore }) => {
                                    useCallStore.setState({
                                        isCallActive: true,
                                        isIncoming: false,
                                        activeCallerId: selectedChat.id,
                                        callStatus: 'IDLE',
                                        callType: 'AUDIO'
                                    });
                                });
                            }
                        }}
                        title="Sesli Ara"
                    >
                        <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"></path></svg>
                    </button>
                    <button
                        onClick={() => {
                            if (selectedChat && !selectedChat.isGroup) {
                                // Use in-app call with native media capture
                                import('../../hooks/useCallStore').then(({ useCallStore }) => {
                                    useCallStore.setState({
                                        isCallActive: true,
                                        isIncoming: false,
                                        activeCallerId: selectedChat.id,
                                        callStatus: 'IDLE',
                                        callType: 'VIDEO'
                                    });
                                });
                            }
                        }}
                        title="Görüntülü Ara"
                    >
                        <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"></path></svg>
                    </button>
                    <button
                        onClick={() => selectedChat.isGroup && setShowGroupInfo(true)}
                        title={selectedChat.isGroup ? "Grup Bilgisi" : "Bilgi"}
                        disabled={!selectedChat.isGroup}
                        style={!selectedChat.isGroup ? { opacity: 0.5, cursor: 'not-allowed' } : {}}
                    >
                        <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 5v.01M12 12v.01M12 19v.01M12 6a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2zm0 7a1 1 0 110-2 1 1 0 010 2z"></path></svg>
                    </button>
                </div>
            </div>

            {/* Search Bar */}
            {isSearchOpen && (
                <div className="chat-search-bar">
                    <input
                        autoFocus
                        className="chat-search-input"
                        placeholder="Sohbet içinde ara..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                    <button onClick={() => { setIsSearchOpen(false); setSearchQuery(''); }} className="chat-search-close">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                    </button>
                </div>
            )}

            {/* Call Bar - Above Messages */}
            <CallBar />

            {/* Messages */}
            <div className="messages-list">
                {displayedMessages.map(msg => (
                    <ChatBubble key={msg.cid} message={msg} onResend={onResend} />
                ))}
                <div ref={messagesEndRef} style={{ height: '1px' }} />
            </div>

            {/* Input */}
            <InputArea
                chatId={selectedChat?.id} // Pass chatId for drafts
                onSend={onSend}
                onTyping={(isTyping) => onTyping(selectedChat.id, selectedChat.isGroup, isTyping)}
            />

            {/* Group Info Modal */}
            {showGroupInfo && selectedChat && selectedChat.isGroup && (
                <GroupInfoModal
                    groupId={selectedChat.id}
                    groupName={selectedChat.name}
                    onClose={() => setShowGroupInfo(false)}
                    onGroupDeleted={() => {
                        performSync();
                        setShowGroupInfo(false);
                        window.location.reload(); // Simple reload to clear selection safely
                    }}
                    onMemberRemoved={() => {
                        // Maybe toast?
                    }}
                />
            )}
        </div>
    );
};

export default ChatWindow;
