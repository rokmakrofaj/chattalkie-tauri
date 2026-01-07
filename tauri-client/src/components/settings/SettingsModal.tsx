import React, { useState, useEffect } from 'react';
import { userService } from '../../services/userService';
import { useSettings } from '../../hooks/useSettings';
import { useAuthStore } from '../../hooks/useAuth';
import ProfileModal from '../chat/ProfileModal';
import './SettingsModal.css';

interface SettingsModalProps {
    isOpen: boolean;
    onClose: () => void;
}

const SettingsModal: React.FC<SettingsModalProps> = ({ isOpen, onClose }) => {
    const { theme, soundEnabled, notificationsEnabled, setTheme, setSoundEnabled, setNotificationsEnabled } = useSettings();
    const { name, username, avatar_url, logout, updateAvatar } = useAuthStore();
    const [activeTab, setActiveTab] = useState<'general' | 'appearance' | 'about'>('general');
    const [showProfile, setShowProfile] = useState(false);
    const [animateIn, setAnimateIn] = useState(false);

    useEffect(() => {
        if (isOpen) {
            setTimeout(() => setAnimateIn(true), 10);
        } else {
            setAnimateIn(false);
        }
    }, [isOpen]);

    const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (file) {
            try {
                const newUrl = await userService.uploadAvatar(file);
                updateAvatar(newUrl);
            } catch (error: any) {
                console.error(error);
                alert(`Resim yüklenemedi: ${error.response?.data?.message || error.message || "Bilinmeyen hata"}`);
            }
        }
    };

    if (!isOpen) return null;

    if (showProfile) {
        return <ProfileModal onClose={() => setShowProfile(false)} />;
    }

    const tabs = [
        { id: 'general', label: 'Genel', icon: '⚙️' },
        { id: 'appearance', label: 'Görünüm', icon: '🎨' },
        { id: 'about', label: 'Hakkında', icon: 'ℹ️' },
    ];

    return (
        <div className={`settings-overlay ${animateIn ? 'open' : ''}`} onClick={onClose}>
            <div className="settings-modal" onClick={e => e.stopPropagation()}>

                {/* Sidebar */}
                <div className="settings-sidebar">
                    <div className="sidebar-group">
                        <h2 className="settings-title">Ayarlar</h2>
                        {tabs.map((tab) => (
                            <button
                                key={tab.id}
                                onClick={() => setActiveTab(tab.id as any)}
                                className={`settings-tab ${activeTab === tab.id ? 'active' : ''}`}
                            >
                                <span className="tab-icon">{tab.icon}</span>
                                <span>{tab.label}</span>
                            </button>
                        ))}
                    </div>

                    <button onClick={logout} className="settings-logout">
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24" style={{ marginRight: '8px' }}>
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                        </svg>
                        <span>Çıkış Yap</span>
                    </button>
                </div>

                {/* Content Area */}
                <div className="settings-content-area">
                    {/* Header */}
                    <div className="settings-header">
                        <h1>{tabs.find(t => t.id === activeTab)?.label}</h1>
                        <button onClick={onClose} className="close-btn">
                            <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                            </svg>
                        </button>
                    </div>

                    <div className="settings-body">
                        {activeTab === 'general' && (
                            <div className="fade-in">
                                {/* Profile Card */}
                                <div className="profile-card">
                                    <div className="profile-info">
                                        <div
                                            className="profile-avatar"
                                            onClick={() => document.getElementById('avatar-settings-input')?.click()}
                                            style={{ cursor: 'pointer', position: 'relative' }}
                                            title="Değiştirmek için tıkla"
                                        >
                                            <input
                                                type="file"
                                                id="avatar-settings-input"
                                                style={{ display: 'none' }}
                                                accept="image/*"
                                                onChange={handleFileChange}
                                            />
                                            {avatar_url ? (
                                                <img
                                                    src={avatar_url}
                                                    alt={name || ""}
                                                    onError={(e) => {
                                                        console.error("Image load error for:", avatar_url);
                                                        e.currentTarget.style.display = 'none';
                                                    }}
                                                />
                                            ) : (
                                                (name || username || "?").charAt(0).toUpperCase()
                                            )}
                                        </div>
                                        <div className="profile-details">
                                            <h3>{name || 'İsimsiz'}</h3>
                                            <p>@{username || 'kullanici'}</p>
                                        </div>
                                    </div>
                                    <button onClick={() => setShowProfile(true)} className="edit-btn">
                                        Düzenle
                                    </button>
                                </div>

                                {/* Toggles */}
                                <div className="settings-section">
                                    <span className="settings-section-title">Bildirimler</span>

                                    <div className="settings-row">
                                        <div>
                                            <div style={{ fontWeight: 500 }}>Uygulama Sesi</div>
                                            <div style={{ fontSize: '12px', color: '#6b7280' }}>Mesaj gelirken ses çal</div>
                                        </div>
                                        <Toggle checked={soundEnabled} onChange={() => setSoundEnabled(!soundEnabled)} />
                                    </div>

                                    <div className="settings-row">
                                        <div>
                                            <div style={{ fontWeight: 500 }}>Masaüstü Bildirimi</div>
                                            <div style={{ fontSize: '12px', color: '#6b7280' }}>Arka planda bildirim göster</div>
                                        </div>
                                        <Toggle checked={notificationsEnabled} onChange={() => setNotificationsEnabled(!notificationsEnabled)} />
                                    </div>
                                </div>
                            </div>
                        )}

                        {activeTab === 'appearance' && (
                            <div className="fade-in">
                                <span className="settings-section-title">Tema Seçimi</span>
                                <div className="theme-grid">
                                    <ThemeCard
                                        type="system"
                                        active={theme === 'system'}
                                        onClick={() => setTheme('system')}
                                        label="Sistem"
                                    />
                                    <ThemeCard
                                        type="light"
                                        active={theme === 'light'}
                                        onClick={() => setTheme('light')}
                                        label="Açık"
                                    />
                                    <ThemeCard
                                        type="dark"
                                        active={theme === 'dark'}
                                        onClick={() => setTheme('dark')}
                                        label="Koyu"
                                    />
                                </div>
                            </div>
                        )}

                        {activeTab === 'about' && (
                            <div className="fade-in" style={{ textAlign: 'center', padding: '40px 0' }}>
                                <div style={{ fontSize: '64px', marginBottom: '16px' }}>👻</div>
                                <h2 style={{ fontSize: '24px', fontWeight: 'bold', marginBottom: '8px' }}>ChatTalkie</h2>
                                <p style={{ color: '#6b7280', marginBottom: '24px' }}>Version 1.0.0 (Beta)</p>
                                <div style={{ border: '1px solid var(--settings-border)', padding: '16px', borderRadius: '12px', display: 'inline-block', backgroundColor: 'var(--settings-sidebar-bg)' }}>
                                    <p style={{ fontSize: '14px', marginBottom: '8px' }}>Hızlı, güvenli ve modern mesajlaşma.</p>
                                    <p style={{ fontSize: '12px', color: '#9ca3af' }}>© 2026 Makrofaj Corp</p>
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};

const Toggle: React.FC<{ checked: boolean; onChange: () => void }> = ({ checked, onChange }) => (
    <button
        onClick={onChange}
        className={`toggle-switch ${checked ? 'checked' : ''}`}
    >
        <span className="toggle-knob" />
    </button>
);

const ThemeCard: React.FC<{ type: string; active: boolean; label: string; onClick: () => void }> = ({ type, active, label, onClick }) => {
    // Custom inline styles for theme previews since they are specific visualization
    const getPreviewStyle = () => {
        if (type === 'dark') return { background: '#1e1e1e', borderColor: '#374151' };
        if (type === 'light') return { background: '#ffffff', borderColor: '#e5e7eb' };
        return { background: 'linear-gradient(135deg, #ffffff 50%, #1e1e1e 50%)', borderColor: '#9ca3af' };
    };

    return (
        <button
            onClick={onClick}
            className={`theme-card ${active ? 'active' : ''}`}
        >
            <div className="theme-preview" style={getPreviewStyle()}>
                {/* Mini UI Representation */}
                <div style={{ padding: '8px', display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    <div style={{ width: '70%', height: '8px', borderRadius: '4px', background: type === 'dark' ? '#374151' : '#f3f4f6' }}></div>
                    <div style={{ width: '40%', height: '8px', borderRadius: '4px', background: type === 'dark' ? '#4b5563' : '#e5e7eb' }}></div>
                </div>
            </div>
            <span className="theme-label">{label}</span>
        </button>
    );
};

export default SettingsModal;
