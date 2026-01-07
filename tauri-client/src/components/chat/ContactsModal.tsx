import { useState } from 'react';
import { getAvatarColor, getInitials } from '../../utils/uiUtils';
import { usePresenceStore } from '../../hooks/usePresenceStore';
import './ContactsModal.css';

interface ContactsModalProps {
    chats: any[];
    onSelectChat: (id: number) => void;
    onClose: () => void;
}

const ContactsModal = ({ chats, onSelectChat, onClose }: ContactsModalProps) => {
    const [searchQuery, setSearchQuery] = useState('');
    const onlineUsers = usePresenceStore(state => state.onlineUsers);

    // Filter chats based on search query
    // Show ALL chats, not just active ones (this is the key difference from Sidebar)
    const filteredChats = chats.filter(chat =>
        chat.name.toLowerCase().includes(searchQuery.toLowerCase())
    );

    // Sort: Online first, then alphabetical
    const sortedChats = [...filteredChats].sort((a, b) => {
        const aOnline = onlineUsers.has(a.id);
        const bOnline = onlineUsers.has(b.id);
        if (aOnline === bOnline) {
            return a.name.localeCompare(b.name);
        }
        return aOnline ? -1 : 1;
    });

    return (
        <div className="contacts-modal-overlay" onClick={onClose}>
            <div className="contacts-modal" onClick={e => e.stopPropagation()}>
                <div className="contacts-header">
                    <h2>Kişiler</h2>
                    <button className="close-btn" onClick={onClose}>
                        <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
                        </svg>
                    </button>
                </div>

                <div className="contacts-search">
                    <input
                        type="text"
                        placeholder="Kişi ara..."
                        autoFocus
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </div>

                <div className="contacts-list">
                    {sortedChats.map(chat => {
                        const isOnline = onlineUsers.has(chat.id);
                        return (
                            <div
                                key={chat.id}
                                className="contact-item"
                                onClick={() => {
                                    onSelectChat(chat.id);
                                    onClose();
                                }}
                            >
                                <div className="contact-avatar" style={{ backgroundColor: getAvatarColor(chat.name), position: 'relative' }}>
                                    {getInitials(chat.name)}
                                    {isOnline && (
                                        <span style={{
                                            position: 'absolute',
                                            bottom: '0',
                                            right: '0',
                                            width: '10px',
                                            height: '10px',
                                            backgroundColor: '#22c55e',
                                            borderRadius: '50%',
                                            border: '2px solid white'
                                        }} />
                                    )}
                                </div>
                                <div className="contact-info">
                                    <span className="contact-name">{chat.name}</span>
                                    <span className="contact-status">
                                        {isOnline ? 'Çevrimiçi' : 'Çevrimdışı'} • {chat.isGroup ? 'Grup' : 'Kişi'}
                                    </span>
                                </div>
                            </div>
                        );
                    })}

                    {sortedChats.length === 0 && (
                        <div className="contacts-empty">
                            Kişi bulunamadı.
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ContactsModal;
