
import { useState, useRef, useEffect } from 'react';
import { useAuthStore } from '../../hooks/useAuth';
import { apiClient, BASE_URL } from '../../api/client';
import { db } from '../../db/db';
import './InputArea.css';

interface InputAreaProps {
    chatId?: number; // Added chatId
    onSend: (text: string, mediaKey?: string, messageType?: string) => void;
    onTyping?: (isTyping: boolean) => void;
}

const InputArea = ({ chatId, onSend, onTyping }: InputAreaProps) => {
    const [text, setText] = useState('');
    const typingTimeoutRef = useRef<number | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);
    const { token } = useAuthStore();
    const [isUploading, setIsUploading] = useState(false);

    // Voice Recording State
    const [isRecording, setIsRecording] = useState(false);
    const mediaRecorderRef = useRef<MediaRecorder | null>(null);
    const audioChunksRef = useRef<Blob[]>([]);

    // Draft Persistence
    useEffect(() => {
        if (!chatId) return;

        // Load Draft
        db.drafts.get(chatId).then(draft => {
            if (draft) setText(draft.content);
            else setText('');
        });
    }, [chatId]);

    // Save Draft (Debounced)
    useEffect(() => {
        if (!chatId) return;
        const handler = setTimeout(() => {
            if (text.trim()) {
                db.drafts.put({
                    chatId,
                    content: text,
                    type: 'text',
                    updatedAt: Date.now()
                });
            } else {
                // Clean up empty drafts
                db.drafts.delete(chatId);
            }
        }, 500);
        return () => clearTimeout(handler);
    }, [text, chatId]);


    const handleSend = () => {
        if (!text.trim()) return;
        onSend(text, undefined, 'text');
        setText('');

        // Clear draft immediately
        if (chatId) db.drafts.delete(chatId);

        if (onTyping) {
            if (typingTimeoutRef.current) clearTimeout(typingTimeoutRef.current);
            onTyping(false);
        }
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter') {
            handleSend();
        }
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        setText(e.target.value);

        if (onTyping) {
            onTyping(true);

            if (typingTimeoutRef.current) {
                clearTimeout(typingTimeoutRef.current);
            }

            typingTimeoutRef.current = setTimeout(() => {
                onTyping(false);
            }, 3000);
        }
    };

    const handleAttachClick = () => {
        fileInputRef.current?.click();
    };

    const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        setIsUploading(true);
        try {
            const formData = new FormData();
            formData.append('file', file);

            const response = await fetch(`${BASE_URL} /api/chat / upload`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token} `
                },
                body: formData
            });

            if (!response.ok) throw new Error('Upload failed');

            const data = await response.json();
            // Infer type from mime
            const type = file.type.startsWith('image/') ? 'image' : 'file';
            onSend('', data.key, type);
        } catch (error) {
            console.error('Upload Error:', error);
            alert('Dosya yüklenemedi!');
        } finally {
            setIsUploading(false);
            if (fileInputRef.current) fileInputRef.current.value = '';
        }
    };

    const startRecording = async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            const mediaRecorder = new MediaRecorder(stream);
            mediaRecorderRef.current = mediaRecorder;
            audioChunksRef.current = [];

            mediaRecorder.ondataavailable = (event) => {
                if (event.data.size > 0) {
                    audioChunksRef.current.push(event.data);
                }
            };

            mediaRecorder.onstop = async () => {
                const audioBlob = new Blob(audioChunksRef.current, { type: 'audio/webm' });
                // Upload
                setIsUploading(true);
                try {
                    const formData = new FormData();
                    formData.append('file', audioBlob, 'voice_message.webm');

                    const response = await fetch(`${BASE_URL} /api/chat / upload`, {
                        method: 'POST',
                        headers: {
                            'Authorization': `Bearer ${token} `
                        },
                        body: formData
                    });

                    if (!response.ok) throw new Error('Upload failed');
                    const data = await response.json();

                    onSend('', data.key, 'voice');
                } catch (e) {
                    console.error('Voice upload failed', e);
                    alert('Sesli mesaj gönderilemedi.');
                } finally {
                    setIsUploading(false);
                    // Stop tracks
                    stream.getTracks().forEach(track => track.stop());
                }
            };

            mediaRecorder.start();
            setIsRecording(true);
        } catch (e) {
            console.error('Microphone access denied', e);
            alert('Mikrofon erişimi reddedildi.');
        }
    };

    const stopRecording = () => {
        if (mediaRecorderRef.current && isRecording) {
            mediaRecorderRef.current.stop();
            setIsRecording(false);
        }
    };

    return (
        <div className="chat-input-container">
            <div className="chat-input-bar">
                <button className="attach-btn" onClick={handleAttachClick} disabled={isUploading || isRecording}>
                    {isUploading ? (
                        <div className="animate-spin h-5 w-5 border-2 border-gray-500 rounded-full border-t-transparent"></div>
                    ) : (
                        <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828l6.414-6.586a4 4 0 00-5.656-5.656l-6.415 6.585a6 6 0 108.486 8.486L20.5 13"></path></svg>
                    )}
                </button>
                <input
                    type="file"
                    ref={fileInputRef}
                    style={{ display: 'none' }}
                    onChange={handleFileChange}
                    accept="image/*"
                />

                {isRecording ? (
                    <div className="flex-1 flex items-center justify-center text-red-500 font-medium animate-pulse">
                        🔴 Ses Kaydediliyor...
                    </div>
                ) : (
                    <input
                        className="message-input"
                        placeholder={isUploading ? "Yükleniyor..." : "Bir mesaj yazın..."}
                        value={text}
                        onChange={handleChange}
                        onKeyDown={handleKeyDown}
                        disabled={isUploading}
                    />
                )}

                {text.trim() ? (
                    <button
                        className="send-btn"
                        onClick={handleSend}
                        disabled={isUploading}
                    >
                        <svg width="20" height="20" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"></path></svg>
                    </button>
                ) : (
                    <button
                        type="button"
                        className={`mic - btn ${isRecording ? 'recording' : ''} `}
                        onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            if (isRecording) stopRecording();
                            else startRecording();
                        }}
                        disabled={isUploading}
                    >
                        {isRecording ? (
                            <svg width="24" height="24" fill="red" stroke="red" viewBox="0 0 24 24"><rect x="6" y="6" width="12" height="12"></rect></svg>
                        ) : (
                            <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z"></path></svg>
                        )}
                    </button>
                )}

                {isRecording && (
                    <button
                        type="button"
                        className="cancel-btn text-gray-500 hover:text-red-500 ml-2"
                        onClick={(e) => {
                            e.preventDefault();
                            e.stopPropagation();
                            if (mediaRecorderRef.current) {
                                mediaRecorderRef.current.onstop = null; // Detach listener to prevent send
                                mediaRecorderRef.current.stop();
                                setIsRecording(false);
                                audioChunksRef.current = [];
                            }
                        }}
                    >
                        <svg width="24" height="24" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path></svg>
                    </button>
                )}
            </div>
            <div className="footer-text">
                ChatTalkie &bull; Uçtan uca şifresiz iletişim 🔓
            </div>
        </div >
    );
};

export default InputArea;
