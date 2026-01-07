import React, { useEffect, useRef, useState } from 'react';
import { useCallStore } from '../../hooks/useCallStore';
import { useWebRTC } from '../../hooks/useWebRTC';
import { useAuthStore } from '../../hooks/useAuth';
import './CallModal.css';

// Helper to format seconds as mm:ss
const formatDuration = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
};

interface CallModalProps {
    sendSignal: (signal: any) => void;
}

const CallModal: React.FC<CallModalProps> = ({ sendSignal }) => {
    const {
        isCallActive,
        isIncoming,
        callStatus,
        callType,
        activeCallerId,
        localStream,
        remoteStream,
        isMinimized,
        callStartTime,
        endCall,
        setMinimized,
        setIncomingSignal
    } = useCallStore();



    // Call duration timer
    const [elapsed, setElapsed] = useState(0);

    useEffect(() => {
        if (callStatus === 'CONNECTED' && callStartTime) {
            const interval = setInterval(() => {
                setElapsed(Math.floor((Date.now() - callStartTime) / 1000));
            }, 1000);
            return () => clearInterval(interval);
        } else {
            setElapsed(0);
        }
    }, [callStatus, callStartTime]);

    console.log('CallModal Render:', { isCallActive, isIncoming, callStatus, activeCallerId });

    const { userId } = useAuthStore();
    const { startCall, acceptIncomingCall } = useWebRTC(sendSignal);

    const localVideoRef = useRef<HTMLVideoElement>(null);
    const remoteVideoRef = useRef<HTMLVideoElement>(null);
    const hasStartedRef = useRef(false);

    // Reset ref when call ends
    useEffect(() => {
        if (!isCallActive) {
            hasStartedRef.current = false;
        }
    }, [isCallActive]);

    // Auto-Start Outgoing Call
    useEffect(() => {
        if (isCallActive && !isIncoming && callStatus === 'IDLE' && activeCallerId && !hasStartedRef.current) {
            hasStartedRef.current = true;
            startCall(activeCallerId, callType === 'VIDEO');
        }
    }, [isCallActive, isIncoming, activeCallerId, callType, startCall, callStatus]);

    // Auto-attach streams
    useEffect(() => {
        if (localStream && localVideoRef.current && callType === 'VIDEO') {
            localVideoRef.current.srcObject = localStream;
        }
    }, [localStream, callType]);

    useEffect(() => {
        if (remoteStream && remoteVideoRef.current && callType === 'VIDEO') {
            remoteVideoRef.current.srcObject = remoteStream;
        }
    }, [remoteStream, callType]);

    // Handle Reject
    const handleReject = () => {
        if (activeCallerId) {
            sendSignal({
                kind: 'signal',
                type: 'HANGUP',
                senderId: userId,
                receiverId: activeCallerId,
                payload: ''
            });
        }
        setIncomingSignal(null as any);
        endCall();
    };

    // Handle Hangup
    const handleHangup = () => {
        console.log('🔴 handleHangup called, activeCallerId:', activeCallerId);
        if (activeCallerId) {
            console.log('📤 Sending HANGUP signal to:', activeCallerId);
            sendSignal({
                kind: 'signal',
                type: 'HANGUP',
                senderId: userId,
                receiverId: activeCallerId,
                payload: ''
            });
        }
        endCall();
    };

    // Handle Minimize
    const handleMinimize = () => {
        setMinimized(true);
    };

    // Don't render if not active OR if minimized
    if (!isCallActive || isMinimized) return null;

    return (
        <div className="call-modal-overlay">
            <div className="call-modal-container">

                {/* Minimize Button (top-right) */}
                {callStatus === 'CONNECTED' && (
                    <button className="call-minimize-btn" onClick={handleMinimize} title="Küçült">
                        <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 9l-7 7-7-7"></path>
                        </svg>
                    </button>
                )}

                {/* Status Header */}
                <div className="call-status-header">
                    <h2>
                        {isIncoming && callStatus === 'RINGING' ? 'Gelen Arama...' :
                            callStatus === 'CONNECTED' ? 'Görüşme Sürüyor' :
                                callStatus === 'RINGING' ? 'Aranıyor...' : callStatus}
                    </h2>
                    {/* Call Duration Timer */}
                    {callStatus === 'CONNECTED' && (
                        <span className="call-timer">{formatDuration(elapsed)}</span>
                    )}


                </div>

                {/* Video Area */}
                <div className="call-video-area">
                    {/* Remote Video (Main) - Hidden in Audio Mode */}
                    <div className={`call-remote-video ${callType === 'AUDIO' ? 'hidden' : ''}`}>
                        {remoteStream ? (
                            <video
                                ref={remoteVideoRef}
                                autoPlay
                                playsInline
                            />
                        ) : (
                            <div className="placeholder">Kamera Bekleniyor...</div>
                        )}
                    </div>

                    {/* Local Video (PiP) - Hidden in Audio Mode */}
                    {localStream && (
                        <div className={`call-local-video ${callType === 'AUDIO' ? 'hidden' : ''}`}>
                            <video
                                ref={localVideoRef}
                                autoPlay
                                playsInline
                                muted
                            />
                        </div>
                    )}

                    {/* Audio View Overlay */}
                    {callType === 'AUDIO' && (
                        <div className="call-audio-overlay">
                            <div className="call-audio-avatar">
                                <svg width="80" height="80" fill="white" viewBox="0 0 24 24">
                                    <path d="M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3z" />
                                    <path d="M17 11c0 2.76-2.24 5-5 5s-5-2.24-5-5H5c0 3.53 2.61 6.43 6 6.92V21h2v-3.08c3.39-.49 6-3.39 6-6.92h-2z" />
                                </svg>
                            </div>
                            <div className="call-audio-label">Sesli Arama</div>
                        </div>
                    )}
                </div>

                {/* Controls */}
                <div className="call-controls">
                    {isIncoming && callStatus === 'RINGING' ? (
                        <>
                            <div className="call-control-item">
                                <button onClick={acceptIncomingCall} className="call-btn accept">
                                    <svg width="32" height="32" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"></path></svg>
                                </button>
                                <span>Yanıtla</span>
                            </div>

                            <div className="call-control-item">
                                <button onClick={handleReject} className="call-btn reject">
                                    <svg width="32" height="32" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                                </button>
                                <span>Reddet</span>
                            </div>
                        </>
                    ) : (
                        <div className="call-control-item">
                            <button onClick={handleHangup} className="call-btn hangup">
                                <svg width="32" height="32" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 8l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2M5 3a2 2 0 00-2 2v1c0 8.284 6.716 15 15 15h1a2 2 0 002-2v-3.28a1 1 0 00-.684-.948l-4.493-1.498a1 1 0 00-1.21.502l-1.13 2.257a11.042 11.042 0 01-5.516-5.516l2.257-1.13a1 1 0 00.502-1.21L8.228 3.683A1 1 0 007.28 3H5z"></path></svg>
                            </button>
                            <span>Sonlandır</span>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default CallModal;
