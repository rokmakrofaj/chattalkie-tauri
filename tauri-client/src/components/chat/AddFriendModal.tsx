import { useState, useEffect, useCallback } from 'react';
import { useAuthStore } from '../../hooks/useAuth';
import { apiClient } from '../../api/client';
import { getAvatarColor, getInitials } from '../../utils/uiUtils';
import './AddFriendModal.css';

interface User {
    id: number;
    username: string;
    name: string;
    avatarUrl?: string;
    friendshipStatus?: string; // "pending", "accepted", or null
}

interface AddFriendModalProps {
    onClose: () => void;
    onFriendAdded?: () => void; // Callback when a friend is added
}

const AddFriendModal = ({ onClose, onFriendAdded }: AddFriendModalProps) => {
    const token = useAuthStore(state => state.token);
    const [activeTab, setActiveTab] = useState<'search' | 'pending'>('search');
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<User[]>([]);
    const [pendingRequests, setPendingRequests] = useState<User[]>([]);
    const [loading, setLoading] = useState(false);
    const [actionLoading, setActionLoading] = useState<number | null>(null);

    // Debounced search
    useEffect(() => {
        if (!searchQuery.trim() || searchQuery.length < 2) {
            setSearchResults([]);
            return;
        }

        const timer = setTimeout(async () => {
            setLoading(true);
            try {
                const res = await apiClient.get(`/api/users/search?query=${encodeURIComponent(searchQuery)}`);
                setSearchResults(res.data || []);
            } catch (e) {
                console.error('Search failed:', e);
            } finally {
                setLoading(false);
            }
        }, 300);

        return () => clearTimeout(timer);
    }, [searchQuery, token]);

    // Fetch pending requests
    const fetchPendingRequests = useCallback(async () => {
        try {
            const res = await apiClient.get('/api/friends/pending');
            setPendingRequests(res.data || []);
        } catch (e) {
            console.error('Failed to fetch pending requests:', e);
        }
    }, [token]);

    useEffect(() => {
        if (activeTab === 'pending') {
            fetchPendingRequests();
        }
    }, [activeTab, fetchPendingRequests]);

    // Send friend request
    const sendFriendRequest = async (userId: number) => {
        setActionLoading(userId);
        try {
            await apiClient.post('/api/friends/request', { friendId: userId });
            // Update the user's status in search results
            setSearchResults(prev => prev.map(u =>
                u.id === userId ? { ...u, friendshipStatus: 'pending' } : u
            ));
        } catch (e) {
            console.error('Failed to send friend request:', e);
        } finally {
            setActionLoading(null);
        }
    };

    // Accept friend request
    const acceptRequest = async (userId: number) => {
        setActionLoading(userId);
        try {
            await apiClient.post('/api/friends/action', { userId, action: 'ACCEPT' });
            setPendingRequests(prev => prev.filter(u => u.id !== userId));
            onFriendAdded?.();
        } catch (e) {
            console.error('Failed to accept request:', e);
        } finally {
            setActionLoading(null);
        }
    };

    // Reject friend request
    const rejectRequest = async (userId: number) => {
        setActionLoading(userId);
        try {
            await apiClient.post('/api/friends/action', { userId, action: 'REJECT' });
            setPendingRequests(prev => prev.filter(u => u.id !== userId));
        } catch (e) {
            console.error('Failed to reject request:', e);
        } finally {
            setActionLoading(null);
        }
    };

    const renderActionButton = (user: User) => {
        const isLoading = actionLoading === user.id;
        const status = user.friendshipStatus?.toLowerCase();

        if (status === 'accepted') {
            return <button className="action-btn friend">Arkadaş</button>;
        }
        if (status === 'pending') {
            return <button className="action-btn pending">Bekliyor</button>;
        }
        return (
            <button
                className="action-btn add"
                onClick={() => sendFriendRequest(user.id)}
                disabled={isLoading}
            >
                {isLoading ? '...' : 'Ekle'}
            </button>
        );
    };

    return (
        <div className="add-friend-modal" onClick={onClose}>
            <div className="add-friend-content" onClick={e => e.stopPropagation()}>
                {/* Header */}
                <div className="add-friend-header">
                    <h3>Arkadaş Ekle</h3>
                    <button className="close-btn" onClick={onClose}>
                        <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                    </button>
                </div>

                {/* Tabs */}
                <div className="add-friend-tabs">
                    <button
                        className={`tab-btn ${activeTab === 'search' ? 'active' : ''}`}
                        onClick={() => setActiveTab('search')}
                    >
                        Kullanıcı Ara
                    </button>
                    <button
                        className={`tab-btn ${activeTab === 'pending' ? 'active' : ''}`}
                        onClick={() => setActiveTab('pending')}
                    >
                        Gelen İstekler
                        {pendingRequests.length > 0 && (
                            <span className="pending-count">{pendingRequests.length}</span>
                        )}
                    </button>
                </div>

                {/* Search Tab */}
                {activeTab === 'search' && (
                    <>
                        <div className="search-container">
                            <input
                                type="text"
                                className="search-input"
                                placeholder="Kullanıcı adı veya isim ara..."
                                value={searchQuery}
                                onChange={e => setSearchQuery(e.target.value)}
                                autoFocus
                            />
                        </div>
                        <div className="results-list">
                            {loading ? (
                                <div className="loading-spinner">
                                    <div className="spinner" />
                                </div>
                            ) : searchResults.length > 0 ? (
                                searchResults.map(user => (
                                    <div key={user.id} className="user-item">
                                        <div
                                            className="user-avatar"
                                            style={{ backgroundColor: getAvatarColor(user.name) }}
                                        >
                                            {user.avatarUrl ? (
                                                <img src={user.avatarUrl} alt={user.name} />
                                            ) : (
                                                getInitials(user.name)
                                            )}
                                        </div>
                                        <div className="user-info">
                                            <div className="user-name">{user.name}</div>
                                            <div className="user-username">@{user.username}</div>
                                        </div>
                                        {renderActionButton(user)}
                                    </div>
                                ))
                            ) : searchQuery.length >= 2 ? (
                                <div className="empty-state">
                                    <svg width="48" height="48" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                                    </svg>
                                    <p>Kullanıcı bulunamadı</p>
                                </div>
                            ) : (
                                <div className="empty-state">
                                    <svg width="48" height="48" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                                    </svg>
                                    <p>Aramak için en az 2 karakter girin</p>
                                </div>
                            )}
                        </div>
                    </>
                )}

                {/* Pending Tab */}
                {activeTab === 'pending' && (
                    <div className="results-list" style={{ paddingTop: 16 }}>
                        {pendingRequests.length > 0 ? (
                            pendingRequests.map(user => (
                                <div key={user.id} className="user-item">
                                    <div
                                        className="user-avatar"
                                        style={{ backgroundColor: getAvatarColor(user.name) }}
                                    >
                                        {user.avatarUrl ? (
                                            <img src={user.avatarUrl} alt={user.name} />
                                        ) : (
                                            getInitials(user.name)
                                        )}
                                    </div>
                                    <div className="user-info">
                                        <div className="user-name">{user.name}</div>
                                        <div className="user-username">@{user.username}</div>
                                    </div>
                                    <button
                                        className="action-btn accept"
                                        onClick={() => acceptRequest(user.id)}
                                        disabled={actionLoading === user.id}
                                    >
                                        {actionLoading === user.id ? '...' : 'Kabul'}
                                    </button>
                                    <button
                                        className="action-btn reject"
                                        onClick={() => rejectRequest(user.id)}
                                        disabled={actionLoading === user.id}
                                    >
                                        Reddet
                                    </button>
                                </div>
                            ))
                        ) : (
                            <div className="empty-state">
                                <svg width="48" height="48" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
                                </svg>
                                <p>Bekleyen istek yok</p>
                            </div>
                        )}
                    </div>
                )}
            </div>
        </div>
    );
};

export default AddFriendModal;
