/**
 * Offline Store
 *
 * Manages the offline mutation queue and sync status.
 * Mutations are queued when the device is offline and replayed when connectivity returns.
 * Persisted to AsyncStorage so queued mutations survive app restarts.
 */

import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { supabase } from '../../lib/supabase';
import { networkMonitor } from '../../lib/network';
import { secureLog } from '../../lib/securityUtils';

// ---- Types ----

export interface PendingMutation {
    id: string;
    table: string;
    action: 'insert' | 'update' | 'delete';
    data: Record<string, unknown>;
    timestamp: string;
    retries: number;
}

interface OfflineState {
    // State
    isOffline: boolean;
    isSyncing: boolean;
    pendingMutations: PendingMutation[];

    // Actions
    setIsOffline: (offline: boolean) => void;
    queueMutation: (mutation: Omit<PendingMutation, 'id' | 'retries' | 'timestamp'>) => void;
    processPendingMutations: () => Promise<void>;
}

const MAX_RETRIES = 5;

// ---- Store ----

export const useOfflineStore = create<OfflineState>()(
    persist(
        (set, get) => ({
            isOffline: !networkMonitor.getStatus(),
            isSyncing: false,
            pendingMutations: [],

            setIsOffline: (offline) => {
                const wasOffline = get().isOffline;
                set({ isOffline: offline });

                // Auto-replay queue when connectivity returns
                if (wasOffline && !offline) {
                    get().processPendingMutations();
                }
            },

            queueMutation: (mutation) => {
                const newMutation: PendingMutation = {
                    ...mutation,
                    id: crypto.randomUUID(),
                    timestamp: new Date().toISOString(),
                    retries: 0,
                };
                set((state) => ({
                    pendingMutations: [...state.pendingMutations, newMutation],
                }));
            },

            processPendingMutations: async () => {
                const { pendingMutations, isSyncing, isOffline } = get();
                if (isSyncing || isOffline || pendingMutations.length === 0) return;

                set({ isSyncing: true });
                const successfulIds: string[] = [];

                for (const mutation of pendingMutations) {
                    try {
                        let error;

                        if (mutation.action === 'insert') {
                            ({ error } = await supabase.from(mutation.table).insert([mutation.data]));
                        } else if (mutation.action === 'update') {
                            const { id, ...rest } = mutation.data;
                            ({ error } = await supabase.from(mutation.table).update(rest).eq('id', id));
                        } else if (mutation.action === 'delete') {
                            ({ error } = await supabase.from(mutation.table).delete().eq('id', mutation.data.id));
                        }

                        if (!error) {
                            successfulIds.push(mutation.id);
                        } else {
                            secureLog.warn(`Mutation retry failed for ${mutation.table}:`, error);
                            mutation.retries++;
                        }
                    } catch (err) {
                        secureLog.error(`Mutation processing error:`, err);
                        mutation.retries++;
                    }
                }

                set((state) => ({
                    pendingMutations: state.pendingMutations.filter(
                        (m) => !successfulIds.includes(m.id) && m.retries < MAX_RETRIES
                    ),
                    isSyncing: false,
                }));
            },
        }),
        {
            name: 'household-ledger-offline',
            storage: createJSONStorage(() => AsyncStorage),
            partialize: (state) => ({
                pendingMutations: state.pendingMutations,
            }),
        }
    )
);
