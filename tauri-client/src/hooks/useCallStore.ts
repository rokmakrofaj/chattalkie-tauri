import { create } from 'zustand';

export interface CallSignal {
    type: 'OFFER' | 'ANSWER' | 'ICE_CANDIDATE' | 'HANGUP' | 'BUSY';
    senderId: number;
    receiverId: number;
    payload: string;
    kind: 'signal';
}

interface CallState {
    isCallActive: boolean;
    callStatus: 'IDLE' | 'RINGING' | 'CONNECTED' | 'BUSY' | 'ENDED';
    activeCallerId: number | null;
    isIncoming: boolean;
    callType: 'VIDEO' | 'AUDIO';
    localStream: MediaStream | null;
    remoteStream: MediaStream | null;
    incomingSignal: CallSignal | null;
    pendingSignals: CallSignal[];
    isMinimized: boolean;
    callStartTime: number | null; // Timestamp when call connected

    setCallActive: (active: boolean) => void;
    setCallStatus: (status: CallState['callStatus']) => void;
    setIncomingCall: (callerId: number) => void;
    setCallType: (type: 'VIDEO' | 'AUDIO') => void;
    setIncomingSignal: (signal: CallSignal) => void;
    addPendingSignal: (signal: CallSignal) => void;
    popPendingSignal: () => CallSignal | undefined;
    setLocalStream: (stream: MediaStream | null) => void;
    setRemoteStream: (stream: MediaStream | null) => void;
    setMinimized: (minimized: boolean) => void;
    endCall: () => void;
}

export const useCallStore = create<CallState>((set, get) => ({
    isCallActive: false,
    callStatus: 'IDLE',
    activeCallerId: null,
    isIncoming: false,
    callType: 'VIDEO',
    localStream: null,
    remoteStream: null,
    incomingSignal: null,
    pendingSignals: [],
    isMinimized: false,
    callStartTime: null,

    setCallActive: (active) => set({ isCallActive: active }),
    setCallStatus: (status) => set((state) => ({
        callStatus: status,
        // Set callStartTime when status becomes CONNECTED
        callStartTime: status === 'CONNECTED' && !state.callStartTime ? Date.now() : state.callStartTime
    })),
    setIncomingCall: (callerId) => set({
        isCallActive: true,
        isIncoming: true,
        activeCallerId: callerId,
        callStatus: 'RINGING',
        isMinimized: false,
        callStartTime: null
    }),
    setCallType: (type) => set({ callType: type }),
    setIncomingSignal: (signal) => set({ incomingSignal: signal }),
    addPendingSignal: (signal) => set((state) => ({
        pendingSignals: [...state.pendingSignals, signal]
    })),
    popPendingSignal: () => {
        const signals = get().pendingSignals;
        if (signals.length === 0) return undefined;
        const [first, ...rest] = signals;
        set({ pendingSignals: rest });
        return first;
    },
    setLocalStream: (stream) => set({ localStream: stream }),
    setRemoteStream: (stream) => set({ remoteStream: stream }),
    setMinimized: (minimized) => set({ isMinimized: minimized }),
    endCall: () => {
        set((state) => {
            state.localStream?.getTracks().forEach(track => track.stop());
            state.remoteStream?.getTracks().forEach(track => track.stop());
            return {
                isCallActive: false,
                callStatus: 'IDLE',
                activeCallerId: null,
                isIncoming: false,
                localStream: null,
                remoteStream: null,
                incomingSignal: null,
                pendingSignals: [],
                isMinimized: false,
                callStartTime: null
            };
        });
    }
}));
