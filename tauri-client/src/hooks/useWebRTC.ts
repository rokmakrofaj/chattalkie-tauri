import { useEffect, useRef, useCallback } from 'react';
import { useCallStore } from './useCallStore';
import { useAuthStore } from './useAuth';
import { createPeerConnection, getUserMediaSafe } from '../lib/media-utils'; // Import safe wrappers

export const useWebRTC = (sendSignal: (signal: any) => void) => {
    const { userId } = useAuthStore();
    const {
        incomingSignal,
        activeCallerId,
        setLocalStream,
        setRemoteStream,
        setCallStatus,
        endCall
    } = useCallStore();

    const peerRef = useRef<RTCPeerConnection | null>(null);

    // Platform detection - Native removed, strictly using Browser API
    const useNative = false;

    // Cleanup
    useEffect(() => {
        return () => {
            peerRef.current?.close();
            peerRef.current = null;
        };
    }, []);


    // Initialize Peer Connection (Browser Only)
    const createPeer = useCallback(() => {
        if (peerRef.current) return peerRef.current;

        // Use safe wrapper instead of new RTCPeerConnection()
        const peer = createPeerConnection();

        peer.onicecandidate = (event) => {
            if (event.candidate && activeCallerId) {
                sendSignal({
                    kind: 'signal',
                    type: 'ICE_CANDIDATE',
                    senderId: userId,
                    receiverId: activeCallerId,
                    payload: JSON.stringify(event.candidate)
                });
            }
        };

        peer.ontrack = (event) => {
            console.log("WebRTC: Remote stream received");
            setRemoteStream(event.streams[0]);
        };

        peer.onconnectionstatechange = () => {
            console.log("WebRTC State:", peer.connectionState);
            if (peer.connectionState === 'disconnected' || peer.connectionState === 'failed') {
                endCall();
            }
            if (peer.connectionState === 'connected') {
                setCallStatus('CONNECTED');
            }
        };

        return peer;
    }, [activeCallerId, userId, sendSignal, setRemoteStream, setCallStatus, endCall]);

    // Handle incoming signals
    useEffect(() => {
        const unsubscribe = useCallStore.subscribe(async (state, prevState) => {
            if (state.pendingSignals.length > prevState.pendingSignals.length) {
                // Browser Logic
                const peer = peerRef.current;
                if (!peer) return;

                while (true) {
                    const signal = useCallStore.getState().popPendingSignal();
                    if (!signal) break;
                    try {
                        if (signal.type === 'ICE_CANDIDATE') {
                            await peer.addIceCandidate(new RTCIceCandidate(JSON.parse(signal.payload)));
                        } else if (signal.type === 'ANSWER') {
                            const data = JSON.parse(signal.payload);
                            const remoteSdp = data.sdp || data;
                            await peer.setRemoteDescription(new RTCSessionDescription(remoteSdp));
                        }
                    } catch (e) {
                        console.warn('Signal processing error:', e);
                    }
                }
            }
        });
        return () => unsubscribe();
    }, []);


    // Start Call
    const startCall = useCallback(async (receiverId: number, isVideo: boolean = true) => {
        try {
            useCallStore.getState().setCallType(isVideo ? 'VIDEO' : 'AUDIO');
            setCallStatus('RINGING');

            // Browser implementation
            console.log('🌐 Starting Browser Call...');
            // Use safe wrapper
            const stream = await getUserMediaSafe({ video: isVideo, audio: true });

            setLocalStream(stream);

            const peer = createPeer();
            peerRef.current = peer;
            stream.getTracks().forEach(track => peer.addTrack(track, stream));

            const offer = await peer.createOffer();
            await peer.setLocalDescription(offer);

            const payload = JSON.stringify({
                sdp: offer,
                callType: isVideo ? 'VIDEO' : 'AUDIO'
            });

            sendSignal({
                kind: 'signal',
                type: 'OFFER',
                senderId: userId,
                receiverId: receiverId,
                payload: payload
            });

        } catch (e) {
            console.error("Failed to start call:", e);
            alert(`Call failed: ${(e as Error).message}`);
            endCall();
        }
    }, [userId, sendSignal, setLocalStream, setCallStatus, endCall, createPeer]);

    // Accept Call
    const acceptIncomingCall = useCallback(async () => {
        const offerSignal = useCallStore.getState().incomingSignal;
        if (!offerSignal || offerSignal.type !== 'OFFER') return;

        try {
            const data = JSON.parse(offerSignal.payload);
            const incomingCallType = data.callType || 'VIDEO';
            useCallStore.getState().setCallType(incomingCallType);

            // Extract remote SDP safely. 
            const remoteSdpObj = data.sdp;

            // Browser implementation
            console.log('🌐 Accepting Browser Call...');
            // Use safe wrapper
            const stream = await getUserMediaSafe({
                video: incomingCallType === 'VIDEO',
                audio: true
            });
            setLocalStream(stream);
            setCallStatus('CONNECTED');

            const peer = createPeer();
            peerRef.current = peer;
            stream.getTracks().forEach(track => peer.addTrack(track, stream));

            // Construct proper RTCSessionDescriptionInit
            const descInit = typeof remoteSdpObj === 'string'
                ? { type: 'offer' as RTCSdpType, sdp: remoteSdpObj }
                : remoteSdpObj;

            await peer.setRemoteDescription(new RTCSessionDescription(descInit));

            const answer = await peer.createAnswer();
            await peer.setLocalDescription(answer);

            const answerPayload = JSON.stringify({
                sdp: answer,
                callType: incomingCallType
            });

            sendSignal({
                kind: 'signal',
                type: 'ANSWER',
                senderId: userId,
                receiverId: offerSignal.senderId,
                payload: answerPayload
            });

            useCallStore.getState().setIncomingSignal(null as any);

        } catch (e) {
            console.error("Accept call failed:", e);
            alert(`Accept failed: ${(e as Error).message}`);
            endCall();
        }
    }, [userId, sendSignal, setLocalStream, setCallStatus, endCall, createPeer]);

    return { startCall, acceptIncomingCall };
};
