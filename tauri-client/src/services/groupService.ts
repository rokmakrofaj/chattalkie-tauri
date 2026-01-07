import { apiClient } from '../api/client';

export interface GroupMember {
    userId: number;
    name: string;
    username: string;
    avatarUrl?: string;
    role: 'admin' | 'member';
    joinedAt: number;
}

export interface InviteLinkResponse {
    token: string;
    groupName: string;
    groupId: number;
    createdAt: number;
}

export const groupService = {
    getMembers: async (groupId: number): Promise<GroupMember[]> => {
        const response = await apiClient.get<GroupMember[]>(`/api/groups/${groupId}/members`);
        return response.data;
    },

    kickMember: async (groupId: number, userId: number): Promise<void> => {
        await apiClient.post(`/api/groups/${groupId}/kick`, { userId });
    },

    addMember: async (groupId: number, userId: number): Promise<void> => {
        await apiClient.post(`/api/groups/${groupId}/members`, { userId });
    },

    joinGroup: async (token: string): Promise<void> => {
        await apiClient.post(`/api/groups/join/${token}`);
    },

    leaveGroup: async (groupId: number): Promise<void> => {
        await apiClient.post(`/api/groups/${groupId}/leave`);
    },

    deleteGroup: async (groupId: number): Promise<void> => {
        await apiClient.delete(`/api/groups/${groupId}`);
    },

    createInviteLink: async (groupId: number): Promise<InviteLinkResponse> => {
        const response = await apiClient.post<InviteLinkResponse>(`/api/groups/${groupId}/invite`);
        return response.data;
    },

    getInviteLink: async (groupId: number): Promise<InviteLinkResponse> => {
        const response = await apiClient.get<InviteLinkResponse>(`/api/groups/${groupId}/invite`);
        return response.data;
    }
};
