import { useState, useRef, useEffect } from 'react';
import { apiClient, BASE_URL } from '../../api/client';
import { useAuthStore } from '../../hooks/useAuth';

interface SecureAudioProps {
    mediaKey: string;
}

const SecureAudio = ({ mediaKey }: SecureAudioProps) => {
    const [audioUrl, setAudioUrl] = useState<string | null>(null);
    const [error, setError] = useState(false);
    const { token } = useAuthStore();
    const audioRef = useRef<HTMLAudioElement | null>(null);

    useEffect(() => {
        let isMounted = true;

        const fetchImage = async () => {
            try {
                const res = await fetch(`${BASE_URL}/api/media/${mediaKey}`, {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });

                if (!res.ok) {
                    if (res.status === 404) {
                        console.warn(`SecureAudio: Media not found (404) for key: ${mediaKey}`);
                        if (isMounted) setError(true);
                        return;
                    }
                    throw new Error(`Failed to fetch media URL: ${res.status} ${res.statusText}`);
                }

                const blob = await res.blob();
                const url = URL.createObjectURL(blob);

                if (isMounted) {
                    setAudioUrl(url);
                }
            } catch (e) {
                console.error('SecureAudio load failed', e);
                if (isMounted) setError(true);
            }
        };

        if (mediaKey) {
            fetchImage();
        }

        return () => {
            isMounted = false;
        };
    }, [mediaKey, token]);

    if (error) {
        return (
            <div className="p-3 bg-red-100 rounded text-red-500 text-xs flex items-center gap-2">
                ⚠️ Ses yüklenemedi
            </div>
        );
    }

    if (!audioUrl) {
        return (
            <div className="p-2 flex items-center gap-2 text-gray-400">
                <div className="w-2 h-2 rounded-full bg-gray-400 animate-pulse" />
                <span className="text-xs">Ses yükleniyor...</span>
            </div>
        );
    }

    return (
        <div className="voice-message-container p-1" style={{ minWidth: '200px' }}>
            <audio ref={audioRef} controls controlsList="nodownload noplaybackrate" style={{ width: '100%', height: '36px' }}>
                <source src={audioUrl} type="audio/webm" />
                <source src={audioUrl} type="audio/ogg" />
                <source src={audioUrl} type="audio/mpeg" />
                Tarayıcınız ses elementini desteklemiyor.
            </audio>
        </div>
    );
};

export default SecureAudio;
