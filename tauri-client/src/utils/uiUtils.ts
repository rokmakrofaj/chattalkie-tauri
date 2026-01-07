export const getAvatarColor = (name: string) => {
    if (!name) return '#cbd5e1';
    const colors = [
        '#ef4444', '#f97316', '#f59e0b',
        '#22c55e', '#10b981', '#14b8a6',
        '#06b6d4', '#3b82f6', '#6366f1',
        '#8b5cf6', '#a855f7', '#d946ef',
        '#ec4899', '#f43f5e'
    ];
    let hash = 0;
    for (let i = 0; i < name.length; i++) {
        hash = name.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
};

export const getInitials = (name: string) => {
    if (!name) return '?';
    return name
        .split(' ')
        .map(word => word[0])
        .slice(0, 2)
        .join('')
        .toUpperCase();
};

export const getTime = (timestamp: number) => {
    return new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};
