import React, { useState } from 'react';
import { createPortal } from 'react-dom';
import { groupService } from '../../services/groupService';
import './JoinGroupModal.css';

interface JoinGroupModalProps {
    onClose: () => void;
    onJoined: () => void;
}

const JoinGroupModal: React.FC<JoinGroupModalProps> = ({ onClose, onJoined }) => {
    const [token, setToken] = useState('');
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const handleJoin = async () => {
        if (!token.trim()) return;
        setLoading(true);
        setError(null);
        try {
            // Check if token is full URL or just token
            let actualToken = token;
            if (token.includes('chattalkie://join/')) {
                actualToken = token.split('chattalkie://join/')[1];
            } else if (token.includes('/join/')) {
                actualToken = token.split('/join/')[1];
            }

            await groupService.joinGroup(actualToken);
            onJoined();
            onClose();
            alert("Gruba başarıyla katıldın!");
        } catch (e: any) {
            console.error("Join failed", e);
            if (e.response && e.response.status === 409) {
                setError("Zaten bu grubun üyesisin.");
            } else if (e.response && e.response.status === 404) {
                setError("Geçersiz veya süresi dolmuş davet linki.");
            } else {
                setError("Gruba katılınamadı. Lütfen tekrar dene.");
            }
        } finally {
            setLoading(false);
        }
    };

    return createPortal(
        <div className="join-modal-overlay" onClick={(e) => {
            if (e.target === e.currentTarget) onClose();
        }}>
            <div className="join-modal-content">
                <div className="join-modal-header">
                    <h2>Gruba Katıl</h2>
                    <button onClick={onClose} className="close-btn">
                        <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                    </button>
                </div>

                <div className="join-modal-body">
                    <p className="join-instruction">
                        Gruba katılmak için davet linkini veya kodunu aşağıya yapıştır.
                    </p>
                    <input
                        type="text"
                        className="join-input"
                        placeholder="chattalkie://join/..."
                        value={token}
                        onChange={(e) => setToken(e.target.value)}
                    />
                    {error && <p className="join-error">{error}</p>}
                </div>

                <div className="join-modal-footer">
                    <button onClick={onClose} className="cancel-btn">İptal</button>
                    <button
                        onClick={handleJoin}
                        className="join-btn"
                        disabled={loading || !token.trim()}
                    >
                        {loading ? 'Katılıyor...' : 'Katıl'}
                    </button>
                </div>
            </div>
        </div>,
        document.body
    );
};

export default JoinGroupModal;
