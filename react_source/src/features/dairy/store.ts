/**
 * Dairy Store
 *
 * Manages daily dairy (milk/yogurt) log tracking.
 * Realtime subscription syncs changes across all household devices.
 */

import { create } from 'zustand';
import { supabase } from '../../lib/supabase';
import { useAuthStore } from '../auth/store';
import { useHouseholdStore } from '../household/store';
import { useOfflineStore } from '../../shared/store/offline';
import { secureLog } from '../../lib/securityUtils';
import type { RealtimeChannel } from '@supabase/supabase-js';

// ---- Types ----

export interface DairyLog {
    id: string;
    date: string;
    milkQuantity: number;
    yogurtQuantity: number;
    totalBill: number;
    householdId: string;
    notes?: string;
}

interface DairyState {
    dairyLogs: DairyLog[];
    isLoading: boolean;
    _channel: RealtimeChannel | null;

    // Actions
    fetchDairyLogs: () => Promise<void>;
    addDairyLog: (log: Omit<DairyLog, 'id' | 'householdId' | 'totalBill'>) => Promise<void>;
    updateDairyLog: (id: string, updates: Partial<DairyLog>) => Promise<void>;
    deleteDairyLog: (id: string) => Promise<void>;
    setupRealtimeSubscription: () => void;
    cleanupSubscription: () => void;
    clearLogs: () => void;
}

// ---- DB Mapping ----

function mapDbToLog(row: any): DairyLog {
    return {
        id: row.id,
        date: row.date,
        milkQuantity: row.milk_quantity,
        yogurtQuantity: row.yogurt_quantity,
        totalBill: row.total_bill,
        householdId: row.household_id,
        notes: row.notes,
    };
}

// ---- Store ----

export const useDairyStore = create<DairyState>()((set, get) => ({
    dairyLogs: [],
    isLoading: false,
    _channel: null,

    fetchDairyLogs: async () => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        set({ isLoading: true });

        try {
            const { data, error } = await supabase
                .from('dairy_logs')
                .select('*')
                .eq('household_id', profile.householdId)
                .order('date', { ascending: false })
                .limit(90); // ~3 months

            if (error) {
                secureLog.error('Error fetching dairy logs:', error);
                return;
            }

            set({ dairyLogs: (data ?? []).map(mapDbToLog) });
        } finally {
            set({ isLoading: false });
        }
    },

    addDairyLog: async (log) => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        const { milkPrice, yogurtPrice } = useHouseholdStore.getState();
        const totalBill = (log.milkQuantity * milkPrice) + (log.yogurtQuantity * yogurtPrice);

        const newLog = {
            id: crypto.randomUUID(),
            date: log.date,
            milk_quantity: log.milkQuantity,
            yogurt_quantity: log.yogurtQuantity,
            total_bill: totalBill,
            household_id: profile.householdId,
            notes: log.notes,
        };

        // Optimistic update
        set((s) => ({
            dairyLogs: [mapDbToLog(newLog), ...s.dairyLogs],
        }));

        const { error } = await supabase.from('dairy_logs').insert([newLog]);

        if (error) {
            secureLog.error('Error adding dairy log:', error);
            useOfflineStore.getState().queueMutation({
                action: 'insert',
                table: 'dairy_logs',
                data: newLog,
            });
        }
    },

    updateDairyLog: async (id, updates) => {
        set((s) => ({
            dairyLogs: s.dairyLogs.map((l) => (l.id === id ? { ...l, ...updates } : l)),
        }));

        const dbUpdate: Record<string, unknown> = {};
        if (updates.milkQuantity !== undefined) dbUpdate.milk_quantity = updates.milkQuantity;
        if (updates.yogurtQuantity !== undefined) dbUpdate.yogurt_quantity = updates.yogurtQuantity;
        if (updates.totalBill !== undefined) dbUpdate.total_bill = updates.totalBill;
        if (updates.notes !== undefined) dbUpdate.notes = updates.notes;

        const { error } = await supabase.from('dairy_logs').update(dbUpdate).eq('id', id);

        if (error) {
            secureLog.error('Error updating dairy log:', error);
        }
    },

    deleteDairyLog: async (id) => {
        set((s) => ({
            dairyLogs: s.dairyLogs.filter((l) => l.id !== id),
        }));

        const { error } = await supabase.from('dairy_logs').delete().eq('id', id);

        if (error) {
            secureLog.error('Error deleting dairy log:', error);
        }
    },

    setupRealtimeSubscription: () => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        get().cleanupSubscription();

        const channel = supabase
            .channel(`dairy-logs-${profile.householdId}`)
            .on(
                'postgres_changes',
                {
                    event: '*',
                    schema: 'public',
                    table: 'dairy_logs',
                    filter: `household_id=eq.${profile.householdId}`,
                },
                (payload) => {
                    if (payload.eventType === 'INSERT') {
                        const log = mapDbToLog(payload.new);
                        set((s) => {
                            if (s.dairyLogs.some((l) => l.id === log.id)) return s;
                            // Insert in date-descending order
                            return {
                                dairyLogs: [log, ...s.dairyLogs].sort(
                                    (a, b) => new Date(b.date).getTime() - new Date(a.date).getTime()
                                ),
                            };
                        });
                    } else if (payload.eventType === 'UPDATE') {
                        const updated = mapDbToLog(payload.new);
                        set((s) => ({
                            dairyLogs: s.dairyLogs.map((l) => l.id === updated.id ? updated : l),
                        }));
                    } else if (payload.eventType === 'DELETE') {
                        set((s) => ({
                            dairyLogs: s.dairyLogs.filter((l) => l.id !== payload.old.id),
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

    clearLogs: () => {
        get().cleanupSubscription();
        set({ dairyLogs: [], _channel: null });
    },
}));
