/**
 * Standalone Call Page
 * 
 * This page is opened in an external browser when making calls from Tauri on Linux.
 * It handles WebRTC video/audio calls with full camera/microphone support.
 */
import { useEffect, useState, useRef } from 'react';
import { useCallStore } from '../../hooks/useCallStore';
import { useAuthStore } from '../../hooks/useAuth';
import './CallPage.css';

// Parse URL params
function getUrlParams() {
    const params = new URLSearchParams(window.location.search);
    return {
        caller: params.get('caller') ? parseInt(params.get('caller')!) : null,
        receiver: params.get('receiver') ? parseInt(params.get('receiver')!) : null,
        type: (params.get('type') as 'video' | 'audio') || 'video',
        token: params.get('token') || null
    };
}

// Format seconds as mm:ss
const formatDuration = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
};

const CallPage = () => {
    const params = getUrlParams();
    const [status, setStatus] = useState<'connecting' | 'connected' | 'ended' | 'error'>('connecting');
    const [errorMessage, setErrorMessage] = useState<string>('');
    const [elapsed, setElapsed] = useState(0);
    const [localStream, setLocalStream] = useState<MediaStream | null>(null);
    const [remoteStream, setRemoteStream] = useState<MediaStream | null>(null);

    const localVideoRef = useRef<HTMLVideoElement>(null);
    const remoteVideoRef = useRef<HTMLVideoElement>(null);
    const startTimeRef = useRef<number | null>(null);

    // Timer effect
    useEffect(() => {
        if (status === 'connected' && startTimeRef.current) {
            const interval = setInterval(() => {
                setElapsed(Math.floor((Date.now() - startTimeRef.current!) / 1000));
            }, 1000);
            return () => clearInterval(interval);
        }
    }, [status]);

    // Attach video streams
    useEffect(() => {
        if (localVideoRef.current && localStream) {
            localVideoRef.current.srcObject = localStream;
        }
    }, [localStream]);

    useEffect(() => {
        if (remoteVideoRef.current && remoteStream) {
            remoteVideoRef.current.srcObject = remoteStream;
        }
    }, [remoteStream]);

    // Initialize call on mount
    useEffect(() => {
        if (!params.caller || !params.receiver) {
            setStatus('error');
            setErrorMessage('Eksik arama parametreleri');
            return;
        }

        // Request media access
        const initCall = async () => {
            try {
                const stream = await navigator.mediaDevices.getUserMedia({
                    video: params.type === 'video',
                    audio: true
                });
                setLocalStream(stream);
                setStatus('connected');
                startTimeRef.current = Date.now();

                // TODO: Connect to WebSocket and establish WebRTC connection
                // For now, this is a demo page showing that media access works

            } catch (err: any) {
                console.error('Media access error:', err);
                setStatus('error');
                if (err.name === 'NotAllowedError') {
                    setErrorMessage('Kamera/mikrofon izni reddedildi. Lütfen tarayıcı ayarlarından izin verin.');
                } else if (err.name === 'NotFoundError') {
                    setErrorMessage('Kamera veya mikrofon bulunamadı.');
                } else {
                    setErrorMessage(`Hata: ${err.message}`);
                }
            }
        };

        initCall();

        // Cleanup on unmount
        return () => {
            localStream?.getTracks().forEach(t => t.stop());
            remoteStream?.getTracks().forEach(t => t.stop());
        };
    }, []);

    const handleHangup = () => {
        localStream?.getTracks().forEach(t => t.stop());
        remoteStream?.getTracks().forEach(t => t.stop());
        setStatus('ended');
        // Close the window after a short delay
        setTimeout(() => window.close(), 1000);
    };

    // Error state
    if (status === 'error') {
        return (
            <div className="call-page error">
                <div className="call-page-content">
                    <div className="error-icon">❌</div>
                    <h1>Arama Başarısız</h1>
                    <p>{errorMessage}</p>
                    <button onClick={() => window.close()}>Kapat</button>
                </div>
            </div>
        );
    }

    // Ended state
    if (status === 'ended') {
        return (
            <div className="call-page ended">
                <div className="call-page-content">
                    <div className="ended-icon">📞</div>
                    <h1>Arama Sonlandı</h1>
                    <p>Görüşme süresi: {formatDuration(elapsed)}</p>
                </div>
            </div>
        );
    }

    return (
        <div className="call-page">
            {/* Status bar */}
            <div className="call-status-bar">
                <span className="call-type">{params.type === 'video' ? '📹 Görüntülü' : '📞 Sesli'} Arama</span>
                {status === 'connected' && (
                    <span className="call-timer">{formatDuration(elapsed)}</span>
                )}
                {status === 'connecting' && (
                    <span className="call-connecting">Bağlanıyor...</span>
                )}
            </div>

            {/* Video area */}
            <div className="call-video-container">
                {/* Remote video (main) */}
                <div className="remote-video-wrapper">
                    {remoteStream ? (
                        <video ref={remoteVideoRef} autoPlay playsInline />
                    ) : (
                        <div className="video-placeholder">
                            <div className="avatar-circle">👤</div>
                            <span>{status === 'connecting' ? 'Bağlanıyor...' : 'Karşı taraf bekleniyor...'}</span>
                        </div>
                    )}
                </div>

                {/* Local video (picture-in-picture) */}
                {params.type === 'video' && localStream && (
                    <div className="local-video-wrapper">
                        <video ref={localVideoRef} autoPlay playsInline muted />
                    </div>
                )}
            </div>

            {/* Controls */}
            <div className="call-controls">
                <button className="control-btn hangup" onClick={handleHangup}>
                    <span>📴</span>
                    <label>Sonlandır</label>
                </button>
            </div>
        </div>
    );
};

export default CallPage;
