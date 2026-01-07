import { useRef, useCallback } from 'react';
import { db } from '../db/db';
import { useAuthStore } from './useAuth';
import { usePresenceStore } from './usePresenceStore';
import { useCallStore } from './useCallStore';
import { normalizeMessage } from '../utils/normalizeMessage';
import { v4 as uuidv4 } from 'uuid';


export const useSocket = () => {
    const socketRef = useRef<WebSocket | null>(null);
    const token = useAuthStore((state) => state.token);
    const userId = useAuthStore((state) => state.userId);

    const sendDeliveryReceipt = (cid: string, messageId: string | undefined, senderId: number, isGroup: boolean, groupId?: number) => {
        if (socketRef.current?.readyState === WebSocket.OPEN && userId) {
            const payload = {
                kind: 'delivery_status',
                messageId: messageId || cid,
                cid,
                status: 'DELIVERED',
                userId: userId,
                recipientId: senderId,
                timestamp: Date.now(),
                ...(isGroup ? { groupId } : {})
            };
            socketRef.current.send(JSON.stringify(payload));
        }
    };

    const connect = useCallback(() => {
        if (!token) return;
        if (socketRef.current?.readyState === WebSocket.OPEN) return;

        const ws = new WebSocket(`ws://localhost:8080/chat?token=${token}`);

        ws.onopen = () => {
            console.log('WS Connected');
        };

        ws.onmessage = async (event) => {
            try {
                const msg = JSON.parse(event.data);
                const type = msg.kind || msg.type;

                switch (type) {
                    case 'ack':
                        if (msg.cid && msg.id) {
                            await db.messages.update(msg.cid, {
                                status: 'SENT',
                                serverId: msg.id
                            });
                        }
                        break;

                    case 'chat':
                        // STRICT: Normalize or Drop
                        if (userId) {
                            const normalized = await normalizeMessage(msg, userId);
                            if (normalized) {
                                await db.messages.put(normalized);

                                // Update Chat Preview
                                const preview = (normalized.messageType === 'voice') ? '🎤 Sesli Mesaj' : (normalized.content || (normalized.mediaKey ? '📷 Fotoğraf' : ''));
                                await db.chats.update(normalized.chatId, {
                                    lastMessage: preview,
                                    lastMessageTime: normalized.createdAt
                                });

                                // Send Delivered if it's not mine
                                if (!normalized.isMine) {
                                    sendDeliveryReceipt(normalized.cid, normalized.serverId, normalized.senderId, !!msg.groupId, msg.groupId);
                                }
                            }
                        }
                        break;

                    case 'status':
                        if (msg.status === 'online') {
                            usePresenceStore.getState().setUserOnline(msg.userId);
                        } else {
                            usePresenceStore.getState().setUserOffline(msg.userId);
                        }
                        break;

                    case 'presence_list':
                        if (msg.onlineUserIds) {
                            usePresenceStore.getState().setOnlineUsers(msg.onlineUserIds);
                        }
                        break;

                    case 'typing':
                        // Ephemeral Typing - No DB
                        // msg: { kind: 'typing', senderId, isTyping, groupId?, recipientId? }
                        const tChatId = msg.groupId || msg.senderId; // If group, use groupId. If DM, user is typing TO me, so show in THEIR chat (senderId).
                        if (tChatId && userId && msg.senderId !== userId) {
                            usePresenceStore.getState().setTyping(tChatId, msg.senderId, msg.isTyping);
                        }
                        break;

                    case 'delivery_status':
                        if (msg.cid) {
                            await db.messages.update(msg.cid, { status: msg.status });
                        }
                        break;

                    case 'signal':
                        console.log('📲 Received signal:', msg.type, 'from', msg.senderId);
                        if (msg.type === 'OFFER') {
                            console.log('🔔 Incoming call! Storing OFFER...');
                            useCallStore.getState().setIncomingCall(msg.senderId);
                            useCallStore.getState().setIncomingSignal(msg);
                        } else if (msg.type === 'ICE_CANDIDATE' || msg.type === 'ANSWER') {
                            // Queue these - they must NOT overwrite the OFFER
                            useCallStore.getState().addPendingSignal(msg);
                        } else if (msg.type === 'HANGUP' || msg.type === 'BUSY') {
                            console.log('📴 Call ended by remote');
                            useCallStore.getState().endCall();
                        }
                        break;
                }
            } catch (e) {
                console.error('WS Error', e);
            }
        };

        ws.onclose = () => {
            setTimeout(connect, 3000);
        };

        socketRef.current = ws;
    }, [token, userId, sendDeliveryReceipt]);

    const sendSocketMessage = (cid: string, content: string, chatId: number, isGroup: boolean, mediaKey?: string, messageType?: string) => {
        if (socketRef.current?.readyState === WebSocket.OPEN) {
            const payload = {
                type: 'chat',
                messageId: cid,
                cid,
                content,
                mediaKey,
                messageType,
                ...(isGroup ? { groupId: chatId } : { recipientId: chatId })
            };
            socketRef.current.send(JSON.stringify(payload));
        } else {
            // Mark as FAILED immediately if socket is down
            db.messages.update(cid, { status: 'FAILED' });
        }
    };

    const sendMessage = async (content: string, chatId: number, isGroup: boolean, mediaKey?: string, messageType?: string) => {
        if (!userId) return;

        // Get user details for optimistic UI
        const { name, avatar_url } = useAuthStore.getState();

        const cid = uuidv4();
        const timestamp = Date.now();

        // 1. Optimistic Insert
        await db.transaction('rw', db.messages, db.chats, async () => {
            await db.messages.add({
                cid,
                chatId,
                senderId: userId,
                senderName: name || undefined,
                senderAvatar: avatar_url || undefined,
                content,
                mediaKey,
                messageType,
                createdAt: timestamp,
                status: 'SENDING',
                isMine: true,
            });

            await db.chats.update(chatId, {
                lastMessage: messageType === 'voice' ? '🎤 Sesli Mesaj' : (content || (mediaKey ? '📷 Fotoğraf' : '')),
                lastMessageTime: timestamp
            });
        });

        // 2. Send over Socket
        sendSocketMessage(cid, content, chatId, isGroup, mediaKey, messageType);
    };

    const resendMessage = async (cid: string) => {
        const msg = await db.messages.get(cid);
        if (!msg) return;

        // Reset to SENDING
        await db.messages.update(cid, { status: 'SENDING' });

        // infer isGroup roughly or fetch chat.
        // For now, assume if chatId > 100000 it might be group? No, safe way is to query Chat.
        const chat = await db.chats.get(msg.chatId);
        const isGroup = chat ? chat.isGroup : false; // fallback check

        sendSocketMessage(cid, msg.content, msg.chatId, isGroup, msg.mediaKey, msg.messageType);
    };

    const sendSignal = (signal: any) => {
        console.log('📞 Sending signal:', signal.type, 'to', signal.receiverId);
        if (socketRef.current?.readyState === WebSocket.OPEN) {
            socketRef.current.send(JSON.stringify(signal));
            console.log('✅ Signal sent successfully');
        } else {
            console.error("❌ Socket not open, cannot send signal. State:", socketRef.current?.readyState);
        }
    };

    const sendTyping = (chatId: number, isGroup: boolean, isTyping: boolean) => {
        if (socketRef.current?.readyState === WebSocket.OPEN && userId) {
            const payload = {
                kind: 'typing',
                senderId: userId,
                isTyping,
                ...(isGroup ? { groupId: chatId } : { recipientId: chatId })
            };
            socketRef.current.send(JSON.stringify(payload));
        }
    };

    const sendReadReceipt = (cid: string, messageId: string | undefined, senderId: number, isGroup: boolean, groupId?: number) => {
        if (socketRef.current?.readyState === WebSocket.OPEN && userId) {
            const payload = {
                kind: 'delivery_status',
                messageId: messageId || cid,
                cid,
                status: 'READ',
                userId: userId,
                recipientId: senderId,
                timestamp: Date.now(),
                ...(isGroup ? { groupId } : {})
            };
            socketRef.current.send(JSON.stringify(payload));
        }
    };

    return { connect, sendMessage, resendMessage, sendSignal, sendTyping, sendReadReceipt };
};
