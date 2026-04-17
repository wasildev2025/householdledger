/**
 * Messaging Store
 *
 * Manages household messages with realtime subscriptions.
 * Channel references are stored properly in state (not on window).
 */

import { create } from 'zustand';
import { supabase } from '../../lib/supabase';
import { useAuthStore } from '../auth/store';
import { useOfflineStore } from '../../shared/store/offline';
import { secureLog } from '../../lib/securityUtils';
import type { RealtimeChannel } from '@supabase/supabase-js';

// ---- Types ----

export type MessageType = 'text' | 'image' | 'voice';

export interface Message {
    id: string;
    senderId: string;
    senderName: string;
    content: string;
    createdAt: string;
    householdId?: string;
    messageType?: MessageType;
    mediaUrl?: string;
    mediaDuration?: number;
}

interface MessagesState {
    messages: Message[];
    isLoading: boolean;
    lastNotificationCheck: string | null;

    // Internal
    _channel: RealtimeChannel | null;

    // Actions
    fetchMessages: () => Promise<void>;
    addMessage: (
        content: string,
        messageType?: MessageType,
        mediaUrl?: string,
        mediaDuration?: number
    ) => Promise<void>;
    setupRealtimeSubscription: () => void;
    cleanupSubscription: () => void;
    setLastNotificationCheck: (date: string) => void;
    clearMessages: () => void;
}

// ---- DB Mapping ----

function mapDbToMessage(row: any): Message {
    return {
        id: row.id,
        senderId: row.sender_id,
        senderName: row.sender_name,
        content: row.content,
        createdAt: row.created_at,
        householdId: row.household_id,
        messageType: row.message_type ?? 'text',
        mediaUrl: row.media_url,
        mediaDuration: row.media_duration,
    };
}

// ---- Store ----

export const useMessagesStore = create<MessagesState>()((set, get) => ({
    messages: [],
    isLoading: false,
    lastNotificationCheck: null,
    _channel: null,

    fetchMessages: async () => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        set({ isLoading: true });

        try {
            const { data, error } = await supabase
                .from('messages')
                .select('*')
                .eq('household_id', profile.householdId)
                .order('created_at', { ascending: false })
                .limit(100);

            if (error) {
                secureLog.error('Error fetching messages:', error);
                return;
            }

            set({ messages: (data ?? []).map(mapDbToMessage) });
        } finally {
            set({ isLoading: false });
        }
    },

    addMessage: async (content, messageType = 'text', mediaUrl, mediaDuration) => {
        const profile = useAuthStore.getState().profile;
        const user = useAuthStore.getState().user;
        if (!profile?.householdId || !user) return;

        const newMessage: Record<string, unknown> = {
            id: crypto.randomUUID(),
            sender_id: user.id,
            sender_name: profile.name,
            content,
            created_at: new Date().toISOString(),
            household_id: profile.householdId,
            message_type: messageType,
        };

        if (mediaUrl) newMessage.media_url = mediaUrl;
        if (mediaDuration) newMessage.media_duration = mediaDuration;

        // Optimistic update
        set((s) => ({
            messages: [mapDbToMessage(newMessage), ...s.messages],
        }));

        const { isOffline } = useOfflineStore.getState();

        if (isOffline) {
            useOfflineStore.getState().queueMutation({
                action: 'insert',
                table: 'messages',
                data: newMessage,
            });
            return;
        }

        const { error } = await supabase.from('messages').insert([newMessage]);

        if (error) {
            secureLog.error('Error sending message:', error);
            useOfflineStore.getState().queueMutation({
                action: 'insert',
                table: 'messages',
                data: newMessage,
            });
        }
    },

    setupRealtimeSubscription: () => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        // Cleanup existing subscription
        get().cleanupSubscription();

        const channel = supabase
            .channel(`messages-${profile.householdId}`)
            .on(
                'postgres_changes',
                {
                    event: 'INSERT',
                    schema: 'public',
                    table: 'messages',
                    filter: `household_id=eq.${profile.householdId}`,
                },
                (payload) => {
                    const newMessage = mapDbToMessage(payload.new);
                    set((state) => {
                        // Avoid duplicates
                        if (state.messages.some((m) => m.id === newMessage.id)) return state;
                        return { messages: [newMessage, ...state.messages] };
                    });
                }
            )
            .subscribe();

        set({ _channel: channel });
    },

    cleanupSubscription: () => {
        const channel = get()._channel;
        if (channel) {
            supabase.removeChannel(channel);
            set({ _channel: null });
        }
    },

    setLastNotificationCheck: (date) => set({ lastNotificationCheck: date }),

    clearMessages: () => {
        get().cleanupSubscription();
        set({ messages: [], _channel: null });
    },
}));
