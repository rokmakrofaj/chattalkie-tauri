
import React, { useEffect, useState } from 'react';
import { createPortal } from 'react-dom';
import { groupService, GroupMember } from '../../services/groupService';
import { getAvatarColor, getInitials } from '../../utils/uiUtils';
import { useAuthStore } from '../../hooks/useAuth';
import { db } from '../../db/db';
import './GroupInfoModal.css';

interface GroupInfoModalProps {
    groupId: number;
    groupName: string;
    onClose: () => void;
    onGroupDeleted: () => void; // Callback to refresh chat list
    onMemberRemoved: () => void;
}

const GroupInfoModal: React.FC<GroupInfoModalProps> = ({ groupId, groupName, onClose, onGroupDeleted, onMemberRemoved }) => {
    const { userId } = useAuthStore();
    const [members, setMembers] = useState<GroupMember[]>([]);
    const [candidates, setCandidates] = useState<any[]>([]);
    const [isAdmin, setIsAdmin] = useState(false);
    const [showAddView, setShowAddView] = useState(false);
    const [loading, setLoading] = useState(true);
    const [inviteLink, setInviteLink] = useState<string | null>(null);

    useEffect(() => {
        loadGroupData();
    }, [groupId]);

    const loadGroupData = async () => {
        try {
            setLoading(true);
            const data = await groupService.getMembers(groupId);
            setMembers(data);

            const currentUser = data.find(m => m.userId === userId);
            setIsAdmin(currentUser?.role === 'admin');
        } catch (error) {
            console.error("Failed to load members", error);
        } finally {
            setLoading(false);
        }
    };

    const loadCandidates = async () => {
        const allChats = await db.chats.toArray();
        // Filter: Not a group AND Not already a member
        const potential = allChats.filter(c => !c.isGroup && !members.some(m => m.userId === c.id));
        setCandidates(potential);
    };

    const toggleAddView = async () => {
        if (!showAddView) {
            await loadCandidates();
        }
        setShowAddView(!showAddView);
    };

    const handleAddMember = async (targetId: number) => {
        try {
            await groupService.addMember(groupId, targetId);
            setShowAddView(false);
            loadGroupData();
            alert('Üye eklendi!');
        } catch (e) {
            alert('Üye eklenemedi.');
        }
    };

    const handleKick = async (targetId: number, targetName: string) => {
        if (!confirm(`${targetName} adlı üyeyi gruptan atmak istediğine emin misin ? `)) return;
        try {
            await groupService.kickMember(groupId, targetId);
            setMembers(prev => prev.filter(m => m.userId !== targetId));
            onMemberRemoved();
        } catch (e) {
            alert('Üye atılamadı.');
        }
    };

    const handleDeleteGroup = async () => {
        if (!confirm('DİKKAT: Grubu tamamen silmek üzeresin! Bu işlem geri alınamaz.\n\nDevam etmek istiyor musun?')) return;
        try {
            await groupService.deleteGroup(groupId);
            onGroupDeleted();
            onClose();
        } catch (e) {
            alert('Grup silinemedi.');
        }
    };

    const handleLeaveGroup = async () => {
        if (!confirm('Gruptan ayrılmak istediğine emin misin?')) return;
        try {
            await groupService.leaveGroup(groupId);
            onGroupDeleted(); // Treat leave same as delete/refresh
            onClose();
        } catch (e) {
            alert('Gruptan ayrılamadı.');
        }
    };

    const handleCopyInvite = async () => {
        try {
            let link = inviteLink;
            if (!link) {
                const response = await groupService.createInviteLink(groupId);
                link = response.token;
                setInviteLink(link);
            }
            navigator.clipboard.writeText(`chattalkie://join/${link}`);
            alert('Davet linki kopyalandı!');
        } catch (e) {
            alert('Link oluşturulamadı.');
        }
    };

    if (loading) return (
        <div className="group-modal-overlay">
            <div className="group-modal-content" style={{ alignItems: 'center', padding: '20px' }}>Loading...</div>
        </div>
    );

    return createPortal(
        <div className="group-modal-overlay" onClick={(e) => {
            if (e.target === e.currentTarget) onClose();
        }}>
            <div className="group-modal-content">
                {/* Header */}
                <div className="group-modal-header">
                    <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                        {showAddView && (
                            <button onClick={() => setShowAddView(false)} className="close-btn" title="Geri">
                                <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 19l-7-7 7-7"></path></svg>
                            </button>
                        )}
                        <h2>{showAddView ? 'Üye Ekle' : 'Grup Bilgisi'}</h2>
                    </div>
                    <button onClick={onClose} className="close-btn">
                        <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                    </button>
                </div>

                {!showAddView ? (
                    <>
                        {/* Group Profile */}
                        <div className="group-modal-profile">
                            <div
                                className="group-avatar-large"
                                style={{ backgroundColor: getAvatarColor(groupName) }}
                            >
                                {getInitials(groupName)}
                            </div>
                            <h3 className="group-name-large">{groupName}</h3>
                            <p className="group-meta">{members.length} Üye</p>

                            {isAdmin && (
                                <div style={{ display: 'flex', gap: '8px', marginTop: '10px', width: '100%', justifyContent: 'center' }}>
                                    <button onClick={handleCopyInvite} className="invite-btn" style={{ flex: '0 1 auto', padding: '8px 16px' }}>
                                        <svg width="16" height="16" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1"></path></svg>
                                        Kopyala
                                    </button>
                                    <button onClick={async () => {
                                        if (!confirm('Davet linkini yenilemek istediğine emin misin?\nEski link GEÇERSİZ olacaktır.')) return;
                                        try {
                                            const response = await groupService.createInviteLink(groupId);
                                            setInviteLink(response.token);
                                            alert('Link yenilendi ve kopyalandı!');
                                            navigator.clipboard.writeText(`chattalkie://join/${response.token}`);
                                        } catch (e) {
                                            alert('İşlem başarısız.');
                                        }
                                    }} className="invite-btn" style={{ backgroundColor: '#fee2e2', color: '#dc2626', width: 'auto' }} title="Linki Yenile ve Eskisini İptal Et">
                                        <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M23 4v6h-6" /><path d="M1 20v-6h6" /><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15" /></svg>
                                        Yenile
                                    </button>
                                </div>
                            )}
                        </div>

                        {/* Function Buttons */}
                        <div className="group-actions-row">
                            {isAdmin && (
                                <button onClick={toggleAddView} className="action-btn" style={{ color: '#2563eb', backgroundColor: '#eff6ff' }}>
                                    Üye Ekle
                                </button>
                            )}
                            <button onClick={handleLeaveGroup} className="action-btn danger">
                                Ayrıl
                            </button>
                            {isAdmin && (
                                <button onClick={handleDeleteGroup} className="action-btn danger">
                                    Sil
                                </button>
                            )}
                        </div>

                        {/* Members List */}
                        <div className="group-members-list">
                            <div className="list-header">Üyeler</div>
                            {members.map(member => (
                                <div key={member.userId} className="member-item">
                                    <div
                                        className="member-avatar"
                                        style={{ backgroundColor: member.avatarUrl ? 'transparent' : getAvatarColor(member.name) }}
                                    >
                                        {member.avatarUrl ? (
                                            <img
                                                src={member.avatarUrl}
                                                alt={member.name}
                                                style={{ width: '100%', height: '100%', borderRadius: '50%', objectFit: 'cover' }}
                                            />
                                        ) : (
                                            getInitials(member.name)
                                        )}
                                    </div>
                                    <div className="member-info">
                                        <div className="member-header">
                                            <span className="member-name">{member.name} {member.userId === userId && '(Sen)'}</span>
                                            {member.role === 'admin' && (
                                                <span className="admin-badge">Yönetici</span>
                                            )}
                                        </div>
                                        <p className="member-username">@{member.username}</p>
                                    </div>

                                    {/* Admin Actions */}
                                    {isAdmin && member.userId !== userId && (
                                        <button
                                            onClick={() => handleKick(member.userId, member.name)}
                                            className="kick-btn"
                                            title="Gruptan At"
                                        >
                                            <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 7a4 4 0 11-8 0 4 4 0 018 0zM9 14a6 6 0 00-6 6v1h12v-1a6 6 0 00-6-6zM21 12h-6"></path></svg>
                                        </button>
                                    )}
                                </div>
                            ))}
                        </div>
                    </>
                ) : (
                    /* Candidates List (Add Mode) */
                    <div className="group-members-list">
                        <div className="list-header" style={{ marginBottom: '10px' }}>Kişi Seç</div>
                        {candidates.length === 0 && (
                            <div style={{ padding: '20px', textAlign: 'center', color: '#6b7280' }}>
                                Eklenecek uygun arkadaş bulunamadı.
                            </div>
                        )}
                        {candidates.map(candidate => (
                            <div key={candidate.id} className="member-item" style={{ cursor: 'pointer' }} onClick={() => handleAddMember(candidate.id)}>
                                <div
                                    className="member-avatar"
                                    style={{ backgroundColor: getAvatarColor(candidate.name) }}
                                >
                                    {getInitials(candidate.name)}
                                </div>
                                <div className="member-info">
                                    <div className="member-header">
                                        <span className="member-name">{candidate.name}</span>
                                    </div>
                                </div>
                                <div style={{ padding: '8px', color: '#2563eb' }}>
                                    <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4"></path></svg>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>,
        document.body
    );
};

export default GroupInfoModal;
