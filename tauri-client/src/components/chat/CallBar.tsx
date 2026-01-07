import React, { useState, useEffect } from 'react';
import { useCallStore } from '../../hooks/useCallStore';
import './CallBar.css';

// Helper to format seconds as mm:ss
const formatDuration = (seconds: number): string => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
};

const CallBar: React.FC = () => {
    const { isCallActive, isMinimized, callStatus, callType, callStartTime, setMinimized, endCall } = useCallStore();

    // Timer state
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

    // Only show when call is active AND minimized
    if (!isCallActive || !isMinimized) return null;

    const handleExpand = () => {
        setMinimized(false);
    };

    const handleHangup = (e: React.MouseEvent) => {
        e.stopPropagation();
        endCall();
    };

    return (
        <div className="call-bar" onClick={handleExpand}>
            <div className="call-bar-info">
                <div className="call-bar-icon">
                    {callType === 'VIDEO' ? (
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
                        </svg>
                    ) : (
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z"></path>
                        </svg>
                    )}
                </div>
                <span className="call-bar-text">
                    {callStatus === 'CONNECTED' ? formatDuration(elapsed) :
                        callStatus === 'RINGING' ? 'Aranıyor...' : 'Arama'}
                </span>
                <span className="call-bar-hint">Genişletmek için tıkla</span>
            </div>
            <button className="call-bar-hangup" onClick={handleHangup} title="Sonlandır">
                <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M16 8l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2M5 3a2 2 0 00-2 2v1c0 8.284 6.716 15 15 15h1a2 2 0 002-2v-3.28a1 1 0 00-.684-.948l-4.493-1.498a1 1 0 00-1.21.502l-1.13 2.257a11.042 11.042 0 01-5.516-5.516l2.257-1.13a1 1 0 00.502-1.21L8.228 3.683A1 1 0 007.28 3H5z"></path>
                </svg>
            </button>
        </div>
    );
};

export default CallBar;
