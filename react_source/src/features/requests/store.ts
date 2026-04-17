/**
 * Requests Store
 *
 * Manages money requests with realtime subscriptions.
 * Staff/members can create requests, admins can approve/reject.
 */

import { create } from 'zustand';
import { supabase } from '../../lib/supabase';
import { useAuthStore } from '../auth/store';
import { useOfflineStore } from '../../shared/store/offline';
import { secureLog } from '../../lib/securityUtils';
import type { RealtimeChannel } from '@supabase/supabase-js';

// ---- Types ----

export type RequestStatus = 'pending' | 'approved' | 'rejected';

export interface MoneyRequest {
    id: string;
    servantId?: string;
    memberId?: string;
    amount: number;
    reason: string;
    status: RequestStatus;
    date: string;
    householdId?: string;
}

interface RequestsState {
    requests: MoneyRequest[];
    isLoading: boolean;
    _channel: RealtimeChannel | null;

    // Actions
    fetchRequests: () => Promise<void>;
    addRequest: (request: Omit<MoneyRequest, 'id' | 'status' | 'householdId'>) => Promise<void>;
    updateRequestStatus: (id: string, status: RequestStatus) => Promise<void>;
    setupRealtimeSubscription: () => void;
    cleanupSubscription: () => void;
    clearRequests: () => void;
}

// ---- DB Mapping ----

function mapDbToRequest(row: any): MoneyRequest {
    return {
        id: row.id,
        servantId: row.servant_id,
        memberId: row.member_id,
        amount: row.amount,
        reason: row.reason,
        status: row.status,
        date: row.date ?? row.created_at,
        householdId: row.household_id,
    };
}

// ---- Store ----

export const useRequestsStore = create<RequestsState>()((set, get) => ({
    requests: [],
    isLoading: false,
    _channel: null,

    fetchRequests: async () => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        set({ isLoading: true });

        try {
            const { data, error } = await supabase
                .from('requests')
                .select('*')
                .eq('household_id', profile.householdId)
                .order('date', { ascending: false });

            if (error) {
                secureLog.error('Error fetching requests:', error);
                return;
            }

            set({ requests: (data ?? []).map(mapDbToRequest) });
        } finally {
            set({ isLoading: false });
        }
    },

    addRequest: async (request) => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        const newRequest = {
            id: crypto.randomUUID(),
            servant_id: request.servantId,
            member_id: request.memberId,
            amount: request.amount,
            reason: request.reason,
            status: 'pending' as const,
            date: request.date,
            household_id: profile.householdId,
        };

        set((s) => ({
            requests: [mapDbToRequest(newRequest), ...s.requests],
        }));

        const { isOffline } = useOfflineStore.getState();

        if (isOffline) {
            useOfflineStore.getState().queueMutation({
                action: 'insert',
                table: 'requests',
                data: newRequest,
            });
            return;
        }

        const { error } = await supabase.from('requests').insert([newRequest]);

        if (error) {
            secureLog.error('Error adding request:', error);
        }
    },

    updateRequestStatus: async (id, status) => {
        set((s) => ({
            requests: s.requests.map((r) => (r.id === id ? { ...r, status } : r)),
        }));

        const { error } = await supabase
            .from('requests')
            .update({ status })
            .eq('id', id);

        if (error) {
            secureLog.error('Error updating request status:', error);
        }
    },

    setupRealtimeSubscription: () => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        get().cleanupSubscription();

        const channel = supabase
            .channel(`requests-${profile.householdId}`)
            .on(
                'postgres_changes',
                {
                    event: '*',
                    schema: 'public',
                    table: 'requests',
                    filter: `household_id=eq.${profile.householdId}`,
                },
                (payload) => {
                    if (payload.eventType === 'INSERT') {
                        const req = mapDbToRequest(payload.new);
                        set((s) => {
                            if (s.requests.some((r) => r.id === req.id)) return s;
                            return { requests: [req, ...s.requests] };
                        });
                    } else if (payload.eventType === 'UPDATE') {
                        const updated = mapDbToRequest(payload.new);
                        set((s) => ({
                            requests: s.requests.map((r) => (r.id === updated.id ? updated : r)),
                        }));
                    }
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

    clearRequests: () => {
        get().cleanupSubscription();
        set({ requests: [], _channel: null });
    },
}));
