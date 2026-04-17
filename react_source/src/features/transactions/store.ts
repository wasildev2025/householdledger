/**
 * Transaction Store
 *
 * CRUD operations for transactions with cursor-based pagination.
 * Uses optimistic updates with offline queue fallback.
 * Realtime subscription syncs changes from other devices/users.
 */

import { create } from 'zustand';
import { supabase } from '../../lib/supabase';
import { useAuthStore } from '../auth/store';
import { useOfflineStore } from '../../shared/store/offline';
import { secureLog } from '../../lib/securityUtils';
import type { RealtimeChannel } from '@supabase/supabase-js';

// ---- Types ----

export type TransactionType = 'expense' | 'income' | 'transfer';

export interface Transaction {
    id: string;
    amount: number;
    date: string;
    type: TransactionType;
    categoryId?: string;
    servantId?: string | null;
    memberId?: string | null;
    description: string;
    householdId?: string;
}

interface TransactionState {
    transactions: Transaction[];
    isLoading: boolean;
    cursor: string | null;
    hasMore: boolean;
    _channel: RealtimeChannel | null;

    // Actions
    fetchPage: (reset?: boolean) => Promise<void>;
    addTransaction: (tx: Omit<Transaction, 'id' | 'householdId'>) => Promise<void>;
    updateTransaction: (id: string, updates: Partial<Transaction>) => Promise<void>;
    deleteTransaction: (id: string) => Promise<void>;
    setupRealtimeSubscription: () => void;
    cleanupSubscription: () => void;
    clearTransactions: () => void;
}

const PAGE_SIZE = 50;

// ---- DB Mapping ----

function mapDbToTransaction(row: any): Transaction {
    return {
        id: row.id,
        amount: row.amount,
        date: row.date,
        type: row.type,
        categoryId: row.category_id,
        servantId: row.servant_id,
        memberId: row.member_id,
        description: row.description,
        householdId: row.household_id,
    };
}

function mapTransactionToDb(tx: Transaction): Record<string, unknown> {
    return {
        id: tx.id,
        amount: tx.amount,
        date: tx.date,
        type: tx.type,
        category_id: tx.categoryId,
        servant_id: tx.servantId,
        member_id: tx.memberId,
        description: tx.description,
        household_id: tx.householdId,
    };
}

// ---- Store ----

export const useTransactionStore = create<TransactionState>()((set, get) => ({
    transactions: [],
    isLoading: false,
    cursor: null,
    hasMore: true,
    _channel: null,

    fetchPage: async (reset = false) => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        set({ isLoading: true });

        try {
            let query = supabase
                .from('transactions')
                .select('*')
                .eq('household_id', profile.householdId)
                .order('date', { ascending: false })
                .limit(PAGE_SIZE);

            // Cursor-based pagination: fetch records older than the last seen
            const cursor = reset ? null : get().cursor;
            if (cursor) {
                query = query.lt('date', cursor);
            }

            // Role-based UX filtering (RLS handles security)
            if (profile.role === 'servant' && profile.servantId) {
                query = query.eq('servant_id', profile.servantId);
            } else if (profile.role === 'member' && profile.memberId) {
                query = query.eq('member_id', profile.memberId);
            }

            const { data, error } = await query;

            if (error) {
                secureLog.error('Error fetching transactions:', error);
                set({ isLoading: false });
                return;
            }

            const mapped = (data ?? []).map(mapDbToTransaction);

            set((state) => ({
                transactions: reset ? mapped : [...state.transactions, ...mapped],
                cursor: mapped.length > 0 ? mapped[mapped.length - 1].date : state.cursor,
                hasMore: mapped.length === PAGE_SIZE,
                isLoading: false,
            }));
        } catch (err) {
            secureLog.error('Transaction fetch error:', err);
            set({ isLoading: false });
        }
    },

    addTransaction: async (tx) => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        const newTx: Transaction = {
            ...tx,
            id: crypto.randomUUID(),
            householdId: profile.householdId,
        };

        // Optimistic update
        set((s) => ({ transactions: [newTx, ...s.transactions] }));

        const dbData = mapTransactionToDb(newTx);
        const { isOffline } = useOfflineStore.getState();

        if (isOffline) {
            useOfflineStore.getState().queueMutation({
                action: 'insert',
                table: 'transactions',
                data: dbData,
            });
            return;
        }

        const { error } = await supabase.from('transactions').insert([dbData]);

        if (error) {
            secureLog.error('Error adding transaction:', error);
            useOfflineStore.getState().queueMutation({
                action: 'insert',
                table: 'transactions',
                data: dbData,
            });
        }
    },

    updateTransaction: async (id, updates) => {
        // Optimistic update
        set((state) => ({
            transactions: state.transactions.map((t) =>
                t.id === id ? { ...t, ...updates } : t
            ),
        }));

        const dbUpdate: Record<string, unknown> = {};
        if (updates.amount !== undefined) dbUpdate.amount = updates.amount;
        if (updates.date !== undefined) dbUpdate.date = updates.date;
        if (updates.type !== undefined) dbUpdate.type = updates.type;
        if (updates.categoryId !== undefined) dbUpdate.category_id = updates.categoryId;
        if (updates.servantId !== undefined) dbUpdate.servant_id = updates.servantId;
        if (updates.memberId !== undefined) dbUpdate.member_id = updates.memberId;
        if (updates.description !== undefined) dbUpdate.description = updates.description;

        const { isOffline } = useOfflineStore.getState();

        if (isOffline) {
            useOfflineStore.getState().queueMutation({
                action: 'update',
                table: 'transactions',
                data: { ...dbUpdate, id },
            });
            return;
        }

        const { error } = await supabase.from('transactions').update(dbUpdate).eq('id', id);

        if (error) {
            secureLog.error('Error updating transaction:', error);
            useOfflineStore.getState().queueMutation({
                action: 'update',
                table: 'transactions',
                data: { ...dbUpdate, id },
            });
        }
    },

    deleteTransaction: async (id) => {
        const transaction = get().transactions.find((t) => t.id === id);
        if (!transaction) return;

        // Optimistic delete
        set((state) => ({
            transactions: state.transactions.filter((t) => t.id !== id),
        }));

        const { isOffline } = useOfflineStore.getState();

        if (isOffline) {
            useOfflineStore.getState().queueMutation({
                action: 'delete',
                table: 'transactions',
                data: { id },
            });
            return;
        }

        const { error } = await supabase.from('transactions').delete().eq('id', id);

        if (error) {
            secureLog.error('Error deleting transaction:', error);
            useOfflineStore.getState().queueMutation({
                action: 'delete',
                table: 'transactions',
                data: { id },
            });
        }
    },

    setupRealtimeSubscription: () => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        // Clean up any existing channel first
        get().cleanupSubscription();

        const channel = supabase
            .channel(`transactions-${profile.householdId}`)
            .on(
                'postgres_changes',
                {
                    event: '*',
                    schema: 'public',
                    table: 'transactions',
                    filter: `household_id=eq.${profile.householdId}`,
                },
                (payload) => {
                    if (payload.eventType === 'INSERT') {
                        const tx = mapDbToTransaction(payload.new);
                        set((s) => {
                            // Skip if already exists (our own optimistic insert)
                            if (s.transactions.some((t) => t.id === tx.id)) return s;

                            // Role-based client-side filter to match what fetchPage does
                            if (profile.role === 'servant' && profile.servantId) {
                                if (tx.servantId !== profile.servantId) return s;
                            } else if (profile.role === 'member' && profile.memberId) {
                                if (tx.memberId !== profile.memberId) return s;
                            }

                            return { transactions: [tx, ...s.transactions] };
                        });
                    } else if (payload.eventType === 'UPDATE') {
                        const updated = mapDbToTransaction(payload.new);
                        set((s) => ({
                            transactions: s.transactions.map((t) =>
                                t.id === updated.id ? updated : t
                            ),
                        }));
                    } else if (payload.eventType === 'DELETE') {
                        set((s) => ({
                            transactions: s.transactions.filter((t) => t.id !== payload.old.id),
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

    clearTransactions: () => {
        get().cleanupSubscription();
        set({ transactions: [], cursor: null, hasMore: true, _channel: null });
    },
}));
