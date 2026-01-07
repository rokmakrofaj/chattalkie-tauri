import { LocalMessage } from '../../db/db';
import SecureImage from './SecureImage';
import SecureAudio from './SecureAudio';
// import { getTime } from '../../utils/uiUtils'; // Unused
import './ChatBubble.css';
import './ChatBubble.css';

interface ChatBubbleProps {
    message: LocalMessage;
    onResend?: (cid: string) => void;
}

const ChatBubble = ({ message, onResend }: ChatBubbleProps) => {
    const isMine = message.isMine;
    const hasMediaOnly = !!message.mediaKey && !message.content;

    // console.log('ChatBubble:', message.cid, 'isMine:', isMine, 'avatar:', message.senderAvatar);

    return (
        <div className={`message-row ${isMine ? 'mine' : 'theirs'}`}>
            {!isMine && (
                <div style={{ marginRight: '8px', flexShrink: 0 }}>
                    {message.senderAvatar ? (
                        <img
                            src={message.senderAvatar}
                            alt="avatar"
                            style={{ width: '32px', height: '32px', borderRadius: '50%', objectFit: 'cover' }}
                        />
                    ) : (
                        <div style={{ width: '32px', height: '32px', borderRadius: '50%', backgroundColor: '#cbd5e1', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '12px', fontWeight: 'bold', color: '#475569' }}>
                            {(message.senderName || '?').charAt(0).toUpperCase()}
                        </div>
                    )}
                </div>
            )}

            <div className={`message-bubble ${isMine ? 'mine' : 'theirs'} ${hasMediaOnly ? 'has-media' : ''}`}>
                {!isMine && <div className="sender-name">{message.senderName || 'Bilinmeyen'}</div>}

                {message.mediaKey && (
                    (message.messageType === 'voice' || message.mediaKey.endsWith('.webm') || message.mediaKey.endsWith('.m4a')) ? (
                        <SecureAudio mediaKey={message.mediaKey} />
                    ) : (
                        <SecureImage
                            mediaKey={message.mediaKey}
                            content={message.content}
                        />
                    )
                )}

                {message.content && <div className="message-text">{message.content}</div>}

                <div className="message-footer" style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '4px', marginTop: '2px', fontSize: '0.7rem', opacity: 0.7 }}>
                    <span className="timestamp">
                        {new Date(message.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                    </span>
                    {isMine && (
                        <span className="status-ticks" style={{ display: 'flex', alignItems: 'center' }}>
                            {message.status === 'SENDING' && (
                                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="12" r="10" /><path d="M12 6v6l4 2" /></svg>
                            )}
                            {(message.status === 'SENT' || !message.status) && (
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M20 6L9 17l-5-5" /></svg>
                            )}
                            {message.status === 'DELIVERED' && (
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M18 6L7 17l-5-5" /><path d="M22 6L11 17l-5-5" /></svg>
                            )}
                            {message.status === 'READ' && (
                                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#a5f3fc" strokeWidth="2"><path d="M18 6L7 17l-5-5" /><path d="M22 6L11 17l-5-5" /></svg>
                            )}
                            {message.status === 'FAILED' && (
                                <div
                                    onClick={() => onResend?.(message.cid)}
                                    title="Tekrar Gönder"
                                    style={{ cursor: 'pointer', display: 'flex', alignItems: 'center', color: '#ef4444' }}
                                >
                                    ⚠️ <span style={{ fontSize: '10px', marginLeft: '2px', fontWeight: 'bold' }}>Tekrarla</span>
                                </div>
                            )}
                        </span>
                    )}
                </div>
            </div>
        </div>
    );
};

export default ChatBubble;
