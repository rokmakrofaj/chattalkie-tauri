
import { useState, useEffect } from 'react';
import { apiClient, BASE_URL } from '../../api/client';
import { useAuthStore } from '../../hooks/useAuth';

interface SecureImageProps {
    mediaKey: string;
    content?: string;
}

const SecureImage = ({ mediaKey, content }: SecureImageProps) => {
    const [src, setSrc] = useState<string | undefined>(undefined);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(false);
    const { token } = useAuthStore();

    useEffect(() => {
        if (!mediaKey) return;

        setLoading(true);
        const fetchImage = async () => {
            try {
                // Get presigned URL from backend
                const response = await fetch(`${BASE_URL}/api/media/${mediaKey}`, {
                    headers: { Authorization: `Bearer ${token}` }
                });

                if (!response.ok) {
                    if (response.status === 404) {
                        console.warn(`SecureImage: Media not found (404) for key: ${mediaKey}`);
                        setSrc(undefined); // Keep as null/undefined to show placeholder or nothing
                        return;
                    }
                    throw new Error(`Failed to fetch media URL: ${response.status} ${response.statusText}`);
                }

                const blob = await response.blob();
                const url = URL.createObjectURL(blob);

                setSrc(url);
            } catch (e) {
                console.error("SecureImage Error:", e);
                setError(true);
            } finally {
                setLoading(false);
            }
        };
        fetchImage();
    }, [mediaKey, token]);

    if (error) return <div style={{ color: '#ef4444', fontSize: '12px', padding: '4px' }}>⚠️ Görsel yüklenemedi</div>;
    if (loading) return <div style={{ padding: '20px', backgroundColor: '#f1f5f9', borderRadius: '8px' }}>Yükleniyor...</div>;
    if (!src) return null;

    return (
        <div className="message-image" style={{ marginBottom: content ? '8px' : '0' }}>
            <img
                src={src}
                alt="Attachment"
                style={{ maxWidth: '100%', borderRadius: '8px', cursor: 'pointer', maxHeight: '300px', objectFit: 'cover' }} // Corrected borderRadius
                onClick={() => window.open(src, '_blank')}
                onError={() => setError(true)}
            />
        </div>
    );
};

export default SecureImage;
