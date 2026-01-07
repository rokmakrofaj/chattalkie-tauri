// src/lib/media-utils.ts

// Polyfill for WebKitGTK / Safari (Old versions)
if (typeof window !== 'undefined') {
    if (typeof window.RTCPeerConnection === 'undefined') {
        // @ts-ignore - Check for prefixed versions
        if (typeof window.webkitRTCPeerConnection !== 'undefined') {
            // @ts-ignore
            window.RTCPeerConnection = window.webkitRTCPeerConnection;
        }
    }
}

/**
 * WebRTC desteğini kontrol eder
 */
export interface WebRTCSupport {
    supported: boolean;
    issues: string[];
    details: {
        hasMediaDevices: boolean;
        hasGetUserMedia: boolean;
        hasRTCPeerConnection: boolean;
        isSecureContext: boolean;
    };
}

export async function checkWebRTCSupport(): Promise<WebRTCSupport> {
    const issues: string[] = [];

    const hasMediaDevices = typeof navigator !== 'undefined' && !!navigator.mediaDevices;
    const hasGetUserMedia = hasMediaDevices && typeof navigator.mediaDevices.getUserMedia === 'function';
    const hasRTCPeerConnection = typeof RTCPeerConnection !== 'undefined';
    const isSecureContext = typeof window !== 'undefined' && window.isSecureContext;

    if (!hasMediaDevices) {
        issues.push('navigator.mediaDevices desteklenmiyor');
    }

    if (!hasGetUserMedia) {
        issues.push('getUserMedia API desteklenmiyor');
    }

    if (!hasRTCPeerConnection) {
        issues.push('RTCPeerConnection desteklenmiyor');
    }

    if (!isSecureContext) {
        issues.push('Güvenli context gerekli (HTTPS veya localhost)');
    }

    // Device enumeration test
    if (hasMediaDevices) {
        try {
            const devices = await navigator.mediaDevices.enumerateDevices();
            const hasAudio = devices.some(d => d.kind === 'audioinput');
            const hasVideo = devices.some(d => d.kind === 'videoinput');

            if (!hasAudio) {
                issues.push('Mikrofon bulunamadı');
            }
            if (!hasVideo) {
                issues.push('Kamera bulunamadı');
            }

            console.log('📹 Bulunan cihazlar:', {
                audio: devices.filter(d => d.kind === 'audioinput').length,
                video: devices.filter(d => d.kind === 'videoinput').length
            });
        } catch (error) {
            issues.push(`Cihaz listesi alınamadı: ${error}`);
        }
    }

    return {
        supported: issues.length === 0,
        issues,
        details: {
            hasMediaDevices,
            hasGetUserMedia,
            hasRTCPeerConnection,
            isSecureContext
        }
    };
}

/**
 * Güvenli getUserMedia wrapper
 * Detaylı hata mesajları ve fallback logic ile
 */
export async function getUserMediaSafe(
    constraints: MediaStreamConstraints
): Promise<MediaStream> {
    // Feature check
    if (!navigator?.mediaDevices) {
        throw new Error(
            'Bu tarayıcı/webview media cihazlarını desteklemiyor. ' +
            'Lütfen uygulamayı güncelleyin veya farklı bir tarayıcı deneyin.'
        );
    }

    if (typeof navigator.mediaDevices.getUserMedia !== 'function') {
        throw new Error(
            'getUserMedia API desteklenmiyor. ' +
            'WebRTC özellikleri bu ortamda kullanılamıyor.'
        );
    }

    // HTTPS check (production için)
    if (typeof window !== 'undefined' &&
        !window.isSecureContext &&
        location.hostname !== 'localhost' &&
        !location.hostname.startsWith('127.')) {
        console.warn(
            '⚠️ getUserMedia genellikle HTTPS gerektirir. ' +
            'Production\'da sorun yaşayabilirsiniz.'
        );
    }

    try {
        console.log('🎤 Requesting media stream:', constraints);

        const stream = await navigator.mediaDevices.getUserMedia(constraints);

        console.log('✅ Media stream obtained:', {
            id: stream.id,
            audioTracks: stream.getAudioTracks().length,
            videoTracks: stream.getVideoTracks().length
        });

        return stream;

    } catch (error: any) {
        console.error('❌ getUserMedia error:', error);

        // User-friendly error messages
        switch (error.name) {
            case 'NotAllowedError':
            case 'PermissionDeniedError':
                throw new Error(
                    'Mikrofon/kamera izni reddedildi. ' +
                    'Lütfen tarayıcı/sistem ayarlarından izin verin ve tekrar deneyin.'
                );

            case 'NotFoundError':
            case 'DevicesNotFoundError':
                throw new Error(
                    'Mikrofon veya kamera bulunamadı. ' +
                    'Cihazınızın bağlı ve çalışır durumda olduğundan emin olun.'
                );

            case 'NotReadableError':
            case 'TrackStartError':
                throw new Error(
                    'Mikrofon/kamera kullanılamıyor. ' +
                    'Başka bir uygulama tarafından kullanılıyor olabilir.'
                );

            case 'OverconstrainedError':
                throw new Error(
                    'İstenen video/ses kalitesi desteklenmiyor. ' +
                    'Daha düşük kalite ayarlarıyla tekrar deneyin.'
                );

            case 'TypeError':
                throw new Error(
                    'Geçersiz media parametreleri. ' +
                    'Lütfen geliştiriciye bildirin.'
                );

            case 'AbortError':
                throw new Error(
                    'Media akışı başlatılamadı. ' +
                    'Lütfen tekrar deneyin.'
                );

            default:
                throw new Error(
                    `Media hatası: ${error.message || 'Bilinmeyen hata'}`
                );
        }
    }
}

/**
 * Track'leri güvenli şekilde durdur
 */
export function stopMediaStream(stream: MediaStream | null): void {
    if (!stream) return;

    try {
        stream.getTracks().forEach(track => {
            track.stop();
            console.log(`🛑 Stopped ${track.kind} track:`, track.label);
        });
    } catch (error) {
        console.error('Error stopping media stream:', error);
    }
}

/**
 * RTCPeerConnection için güvenli wrapper
 */
export function createPeerConnection(config?: RTCConfiguration): RTCPeerConnection {
    // Explicit runtime check with fallback
    const PeerConnectionImpl =
        window.RTCPeerConnection ||
        // @ts-ignore
        window.webkitRTCPeerConnection ||
        // @ts-ignore
        window.mozRTCPeerConnection;

    if (!PeerConnectionImpl) {
        throw new Error('RTCPeerConnection desteklenmiyor (Prefixler dahil kontrol edildi)');
    }

    const defaultConfig: RTCConfiguration = {
        iceServers: [
            { urls: 'stun:stun.l.google.com:19302' },
            { urls: 'stun:stun1.l.google.com:19302' }
        ]
    };

    return new PeerConnectionImpl(config || defaultConfig);
}
