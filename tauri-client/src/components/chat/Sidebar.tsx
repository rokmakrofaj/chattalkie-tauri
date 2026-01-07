import { useState } from 'react';
import { getAvatarColor, getInitials } from '../../utils/uiUtils';
import { usePresenceStore } from '../../hooks/usePresenceStore';
import { useAuthStore } from '../../hooks/useAuth';
import SettingsModal from '../settings/SettingsModal';
import JoinGroupModal from './JoinGroupModal';
import AddFriendModal from './AddFriendModal';
import './Sidebar.css';

interface SidebarProps {
    chats: any[];
    currentUser: string | null;
    selectedChatId: number | null;
    onSelectChat: (id: number) => void;
    onLogout: () => void;
    onNewChat: () => void;
    onOpenProfile: () => void;
    onCreateGroup: () => void;
    onRefresh: () => void;
}

const Sidebar = ({
    chats,
    currentUser,
    selectedChatId,
    onSelectChat,
    onLogout,
    onNewChat,
    onOpenProfile,
    onCreateGroup,
    onRefresh
}: SidebarProps) => {
    const { userId, avatar_url } = useAuthStore();
    const activeChats = chats?.filter(chat => chat.lastMessage || chat.id === selectedChatId || chat.isGroup) || [];
    const onlineUsers = usePresenceStore(state => state.onlineUsers);
    const [searchQuery, setSearchQuery] = useState('');
    const [isMenuOpen, setIsMenuOpen] = useState(false);
    const [isSettingsOpen, setIsSettingsOpen] = useState(false);
    const [isJoinGroupOpen, setIsJoinGroupOpen] = useState(false);
    const [isAddFriendOpen, setIsAddFriendOpen] = useState(false);

    const filteredChats = activeChats.filter(chat =>
        chat.name.toLowerCase().includes(searchQuery.toLowerCase())
    );

    return (
        <div className="sidebar">
            {/* Header */}
            {/* Header */}
            <div className="sidebar-header" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                <button className="menu-btn" onClick={() => setIsMenuOpen(true)} style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--text-secondary)', padding: '4px' }}>
                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 6h16M4 12h16M4 18h16"></path></svg>
                </button>

                <div className="sidebar-search" style={{ flex: 1 }}>
                    <input
                        type="text"
                        placeholder="Ara..."
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        style={{ paddingRight: '1rem' }}
                    />
                    <svg className="search-icon" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"></path>
                    </svg>
                </div>

                <button className="new-chat-btn" onClick={onNewChat} title="Yeni Sohbet" style={{ position: 'static', padding: '8px', backgroundColor: 'var(--bg-hover)', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                    <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M12 5v14M5 12h14" />
                    </svg>
                </button>
            </div>

            {/* List */}
            <div className="sidebar-list">
                {filteredChats.map(chat => {
                    // Ideally, we need to know if it's a user or group. Assuming 'FRIEND' type or just userId matching for now.
                    // If 'chat.id' corresponds to partner's userId in direct chats.
                    const online = onlineUsers.has(chat.id);

                    return (
                        <div
                            key={chat.id}
                            onClick={() => onSelectChat(chat.id)}
                            className={`chat-item ${selectedChatId === chat.id ? 'active' : ''}`}
                        >
                            <div className="chat-avatar" style={{ position: 'relative', backgroundColor: chat.avatarUrl ? 'transparent' : getAvatarColor(chat.name) }}>
                                {chat.avatarUrl ? (
                                    <img
                                        src={chat.avatarUrl}
                                        alt={chat.name}
                                        style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }}
                                    />
                                ) : (
                                    getInitials(chat.name)
                                )}
                                {online && (
                                    <span style={{
                                        position: 'absolute',
                                        bottom: '0',
                                        right: '0',
                                        width: '12px',
                                        height: '12px',
                                        backgroundColor: '#22c55e',
                                        borderRadius: '50%',
                                        border: '2px solid white'
                                    }} />
                                )}
                            </div>
                            <div className="chat-info">
                                <div className="chat-name-row">
                                    <span className="chat-name">{chat.name}</span>
                                </div>
                                <div className="chat-preview">
                                    {chat.lastMessage || "Henüz mesaj yok"}
                                </div>
                            </div>
                        </div>
                    );
                })}

                {!activeChats.length && (
                    <div className="sidebar-empty">
                        <span>📭 Sohbet kutusu boş</span>
                    </div>
                )}
            </div>

            {/* Menu Drawer */}
            {isMenuOpen && (
                <div className="drawer-overlay" onClick={() => setIsMenuOpen(false)}>
                    <div className="drawer-content" onClick={e => e.stopPropagation()}>
                        <div className="drawer-header">
                            <div className="drawer-avatar" style={{ backgroundColor: getAvatarColor(currentUser || '') }}>
                                {avatar_url ? (
                                    <img
                                        src={avatar_url}
                                        alt={currentUser || ''}
                                        style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }}
                                    />
                                ) : (
                                    getInitials(currentUser || '')
                                )}
                            </div>
                            <div className="drawer-user-info">
                                <span className="drawer-user-name">{currentUser || 'Kullanıcı'}</span>
                            </div>
                        </div>

                        <div className="drawer-menu">
                            <button className="drawer-item" onClick={() => { setIsMenuOpen(false); onOpenProfile(); }}>
                                <span className="drawer-icon">
                                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"></path></svg>
                                </span>
                                Profilim
                            </button>
                            <button className="drawer-item" onClick={() => { setIsMenuOpen(false); onCreateGroup(); }}>
                                <span className="drawer-icon">
                                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path></svg>
                                </span>
                                Yeni Grup
                            </button>
                            <button className="drawer-item" onClick={() => { setIsMenuOpen(false); setIsJoinGroupOpen(true); }}>
                                <span className="drawer-icon">
                                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" /><polyline points="10 17 15 12 10 7" /><line x1="15" y1="12" x2="3" y2="12" /></svg>
                                </span>
                                Gruba Katıl
                            </button>
                            <button className="drawer-item" onClick={() => { setIsMenuOpen(false); setIsAddFriendOpen(true); }}>
                                <span className="drawer-icon">
                                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z"></path></svg>
                                </span>
                                Arkadaş Ekle
                            </button>
                            <button className="drawer-item" onClick={() => alert('Aramalar yakında!')}>
                                <span className="drawer-icon">
                                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"></path></svg>
                                </span>
                                Aramalar
                            </button>
                            <button className="drawer-item" onClick={() => { setIsMenuOpen(false); if (userId) onSelectChat(userId); }}>
                                <span className="drawer-icon">
                                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 5a2 2 0 012-2h10a2 2 0 012 2v16l-7-3.5L5 21V5z"></path></svg>
                                </span>
                                Kayıtlı Mesajlar
                            </button>
                            <button className="drawer-item" onClick={() => { setIsMenuOpen(false); setIsSettingsOpen(true); }}>
                                <span className="drawer-icon">
                                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"></path><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"></path></svg>
                                </span>
                                Ayarlar
                            </button>

                            <button className="drawer-item danger" onClick={onLogout}>
                                <span className="drawer-icon">
                                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"></path></svg>
                                </span>
                                Çıkış Yap
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <SettingsModal isOpen={isSettingsOpen} onClose={() => setIsSettingsOpen(false)} />
            {isJoinGroupOpen && <JoinGroupModal onClose={() => setIsJoinGroupOpen(false)} onJoined={() => { setIsJoinGroupOpen(false); onRefresh(); }} />}
            {isAddFriendOpen && <AddFriendModal onClose={() => setIsAddFriendOpen(false)} onFriendAdded={() => { setIsAddFriendOpen(false); onRefresh(); }} />}
        </div>
    );
};

export default Sidebar;
