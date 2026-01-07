import { useState, useEffect } from 'react';
import { apiClient, BASE_URL } from '../../api/client';
import { useAuthStore } from '../../hooks/useAuth';
import { getAvatarColor, getInitials } from '../../utils/uiUtils';
import './ProfileModal.css';

interface ProfileModalProps {
    onClose: () => void;
}

const ProfileModal = ({ onClose }: ProfileModalProps) => {
    const storeUsername = useAuthStore(state => state.username) || '';
    const token = useAuthStore(state => state.token);

    const [name, setName] = useState('');
    const [username, setUsername] = useState(storeUsername);
    const [isLoading, setIsLoading] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    useEffect(() => {
        console.log('ProfileModal mounted');
        const fetchProfile = async () => {
            setIsLoading(true);
            try {
                const response = await fetch(`${BASE_URL}/api/users/me`, {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });
                if (response.ok) {
                    const data = await response.json();
                    setName(data.name || '');
                    setUsername(data.username || ''); // Update username from profile
                }
            } catch (error) {
                console.error('Failed to fetch profile', error);
            } finally {
                setIsLoading(false);
            }
        };

        if (token) fetchProfile();
    }, [token]);

    const handleSave = async () => {
        setIsSaving(true);
        try {
            const response = await fetch(`${BASE_URL}/api/users/me`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${token}`
                },
                body: JSON.stringify({ name, username }) // Send name and username
            });

            if (response.ok) {
                alert('Profil güncellendi!');
                onClose();
            } else {
                alert('Hata oluştu');
            }
        } catch (e) {
            console.error(e);
            alert('Bağlantı hatası');
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <div className="profile-overlay" onClick={onClose}>
            <div className="profile-modal" onClick={e => e.stopPropagation()}>
                <div className="profile-header">
                    <div className="profile-avatar-xl" style={{ backgroundColor: getAvatarColor(username) }}>
                        {getInitials(username)}
                    </div>
                    <div className="profile-username">@{username}</div>
                </div>

                <div className="profile-form">
                    <div className="form-group">
                        <label className="form-label">Görünen Ad</label>
                        {isLoading ? (
                            <div style={{ padding: '12px', color: '#666' }}>Yükleniyor...</div>
                        ) : (
                            <input
                                className="form-input"
                                value={name}
                                onChange={(e) => setName(e.target.value)}
                                placeholder="Adınız"
                            />
                        )}
                    </div>

                    <div className="form-group">
                        <label className="form-label">Kullanıcı Adı</label>
                        <input
                            className="form-input"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            placeholder="Kullanıcı adınız"
                        />
                    </div>
                </div>

                <div className="profile-footer">
                    <button className="btn btn-secondary" onClick={onClose}>iptal</button>
                    <button className="btn btn-primary" onClick={handleSave} disabled={isSaving || isLoading}>
                        {isSaving ? 'Kaydediliyor...' : 'Kaydet'}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ProfileModal;
