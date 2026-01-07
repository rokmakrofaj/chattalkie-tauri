import { useState, useEffect } from 'react';
import { useAuthStore } from '../../hooks/useAuth';
import { apiClient, BASE_URL } from '../../api/client';
import { getAvatarColor, getInitials } from '../../utils/uiUtils';
import './CreateGroupModal.css';

interface CreateGroupModalProps {
    onClose: () => void;
    onGroupCreated?: (groupId: number) => void;
}

interface Friend {
    id: number;
    username: string;
    name: string;
    avatarUrl?: string;
    status: string;
}

const CreateGroupModal = ({ onClose, onGroupCreated }: CreateGroupModalProps) => {
    const token = useAuthStore(state => state.token);

    const [groupName, setGroupName] = useState('');
    const [selectedMemberIds, setSelectedMemberIds] = useState<Set<number>>(new Set());
    const [searchQuery, setSearchQuery] = useState('');
    const [friends, setFriends] = useState<Friend[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    const [isCreating, setIsCreating] = useState(false);

    useEffect(() => {
        const fetchFriends = async () => {
            setIsLoading(true);
            try {
                const response = await fetch(`${BASE_URL}/api/friends`, {
                    headers: { 'Authorization': `Bearer ${token}` }
                });
                if (response.ok) {
                    const data = await response.json();
                    setFriends(data);
                }
            } catch (e) {
                console.error("Failed to fetch friends", e);
            } finally {
                setIsLoading(false);
            }
        };
        if (token) fetchFriends();
    }, [token]);

    const handleToggleMember = (id: number) => {
        const newSelected = new Set(selectedMemberIds);
        if (newSelected.has(id)) {
            newSelected.delete(id);
        } else {
            newSelected.add(id);
        }
        setSelectedMemberIds(newSelected);
    };

    const handleCreate = async () => {
        if (!groupName.trim()) {
            alert('Lütfen grup adı giriniz.');
            return;
        }
        if (selectedMemberIds.size === 0) {
            alert('Lütfen en az bir üye seçiniz.');
            return;
        }

        setIsCreating(true);
        try {
            const response = await fetch(`${BASE_URL}/api/groups`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({
                    name: groupName,
                    memberIds: Array.from(selectedMemberIds)
                })
            });

            if (response.ok) {
                const data = await response.json();
                alert('Grup oluşturuldu! 🎉');
                if (onGroupCreated) onGroupCreated(data.id);
                onClose();
            } else {
                alert('Grup oluşturulamadı.');
            }
        } catch (e) {
            console.error(e);
            alert('Bir hata oluştu.');
        } finally {
            setIsCreating(false);
        }
    };

    const filteredFriends = friends.filter(f =>
        f.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
        f.username.toLowerCase().includes(searchQuery.toLowerCase())
    );

    return (
        <div className="create-group-overlay" onClick={onClose}>
            <div className="create-group-modal" onClick={e => e.stopPropagation()}>
                <div className="group-modal-header">
                    <h2>Yeni Grup</h2>
                    <span className="step-indicator">{selectedMemberIds.size} üye seçildi</span>
                </div>

                <div className="group-name-section">
                    <div className="group-avatar-upload">
                        {groupName ? getInitials(groupName) : <span>📷</span>}
                    </div>
                    <input
                        className="group-name-input"
                        placeholder="Grup Adı"
                        value={groupName}
                        onChange={e => setGroupName(e.target.value)}
                        autoFocus
                    />
                </div>

                <div className="members-section">
                    <div className="members-search">
                        <input
                            placeholder="Kişi ara..."
                            value={searchQuery}
                            onChange={e => setSearchQuery(e.target.value)}
                        />
                    </div>
                    <div className="members-list">
                        {isLoading ? (
                            <div style={{ padding: '20px', textAlign: 'center', color: '#666' }}>Yükleniyor...</div>
                        ) : filteredFriends.length === 0 ? (
                            <div style={{ padding: '20px', textAlign: 'center', color: '#666' }}>Kişi bulunamadı.</div>
                        ) : (
                            filteredFriends.map(friend => {
                                const isSelected = selectedMemberIds.has(friend.id);
                                return (
                                    <div
                                        key={friend.id}
                                        className={`member-item ${isSelected ? 'selected' : ''}`}
                                        onClick={() => handleToggleMember(friend.id)}
                                    >
                                        <div className="member-checkbox">
                                            {isSelected && (
                                                <svg width="14" height="14" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="3" d="M5 13l4 4L19 7"></path></svg>
                                            )}
                                        </div>
                                        <div className="chat-avatar" style={{ backgroundColor: getAvatarColor(friend.name), width: 40, height: 40, fontSize: '0.9rem' }}>
                                            {getInitials(friend.name)}
                                        </div>
                                        <div className="member-info">
                                            <span className="member-name">{friend.name}</span>
                                            <span className="member-status">@{friend.username}</span>
                                        </div>
                                    </div>
                                );
                            })
                        )}
                    </div>
                </div>

                <div className="group-modal-footer">
                    <button className="btn btn-secondary" onClick={onClose}>İptal</button>
                    <button className="btn btn-primary" onClick={handleCreate} disabled={isCreating}>
                        {isCreating ? 'Oluşturuluyor...' : 'Oluştur'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default CreateGroupModal;
