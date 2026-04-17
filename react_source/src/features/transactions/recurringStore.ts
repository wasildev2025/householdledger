/**
 * Recurring Transactions Store
 *
 * Manages recurring transactions and their execution logic.
 */

import { create } from 'zustand';
import { supabase } from '../../lib/supabase';
import { useAuthStore } from '../auth/store';
import { useOfflineStore } from '../../shared/store/offline';
import { useTransactionStore } from './store';
import { secureLog } from '../../lib/securityUtils';
import { Frequency, TransactionType } from '../../types';
import { parseISO } from 'date-fns';
import 'react-native-get-random-values';
import { v4 as uuidv4 } from 'uuid';

// ---- Types ----

export interface RecurringTransaction {
    id: string;
    amount: number;
    description: string;
    type: TransactionType;
    categoryId?: string;
    servantId?: string | null;
    memberId?: string | null;
    frequency: Frequency;
    startDate: string;
    lastGeneratedDate?: string;
    active: boolean;
    householdId?: string;
}

interface RecurringState {
    recurringTransactions: RecurringTransaction[];
    isLoading: boolean;

    // Actions
    fetchRecurringTransactions: () => Promise<void>;
    addRecurringTransaction: (tx: Omit<RecurringTransaction, 'id' | 'householdId' | 'active'>) => Promise<void>;
    deleteRecurringTransaction: (id: string) => Promise<void>;
    processRecurringTransactions: () => Promise<void>;
    clearRecurringTransactions: () => void;
}

// ---- DB Mapping ----

function mapDbToRecurring(row: any): RecurringTransaction {
    return {
        id: row.id,
        amount: row.amount,
        description: row.description,
        type: row.type,
        categoryId: row.category_id,
        servantId: row.servant_id,
        memberId: row.member_id,
        frequency: row.frequency,
        startDate: row.start_date,
        lastGeneratedDate: row.last_generated_date,
        active: row.active,
        householdId: row.household_id,
    };
}

function mapRecurringToDb(tx: RecurringTransaction): Record<string, unknown> {
    return {
        id: tx.id,
        amount: tx.amount,
        description: tx.description,
        type: tx.type,
        category_id: tx.categoryId,
        servant_id: tx.servantId,
        member_id: tx.memberId,
        frequency: tx.frequency,
        start_date: tx.startDate,
        last_generated_date: tx.lastGeneratedDate,
        active: tx.active,
        household_id: tx.householdId,
    };
}

// ---- Store ----

export const useRecurringStore = create<RecurringState>()((set, get) => ({
    recurringTransactions: [],
    isLoading: false,

    fetchRecurringTransactions: async () => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        set({ isLoading: true });

        try {
            const { data, error } = await supabase
                .from('recurring_transactions')
                .select('*')
                .eq('household_id', profile.householdId)
                .order('start_date', { ascending: false });

            if (error) {
                secureLog.error('Error fetching recurring transactions:', error);
                return;
            }

            set({ recurringTransactions: (data ?? []).map(mapDbToRecurring) });
        } finally {
            set({ isLoading: false });
        }
    },

    addRecurringTransaction: async (tx) => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        const newRecurring: RecurringTransaction = {
            ...tx,
            id: uuidv4(),
            active: true,
            householdId: profile.householdId,
        };

        // Optimistic update
        set((s) => ({ recurringTransactions: [newRecurring, ...s.recurringTransactions] }));

        const dbData = mapRecurringToDb(newRecurring);
        const { isOffline } = useOfflineStore.getState();

        if (isOffline) {
            useOfflineStore.getState().queueMutation({
                action: 'insert',
                table: 'recurring_transactions',
                data: dbData,
            });
            return;
        }

        const { error } = await supabase.from('recurring_transactions').insert([dbData]);

        if (error) {
            secureLog.error('Error adding recurring transaction:', error);
            useOfflineStore.getState().queueMutation({
                action: 'insert',
                table: 'recurring_transactions',
                data: dbData,
            });
        }
    },

    deleteRecurringTransaction: async (id) => {
        // Optimistic update
        set((s) => ({
            recurringTransactions: s.recurringTransactions.filter((r) => r.id !== id),
        }));

        const { isOffline } = useOfflineStore.getState();

        if (isOffline) {
            useOfflineStore.getState().queueMutation({
                action: 'delete',
                table: 'recurring_transactions',
                data: { id },
            });
            return;
        }

        const { error } = await supabase.from('recurring_transactions').delete().eq('id', id);

        if (error) {
            secureLog.error('Error deleting recurring transaction:', error);
            useOfflineStore.getState().queueMutation({
                action: 'delete',
                table: 'recurring_transactions',
                data: { id },
            });
        }
    },

    processRecurringTransactions: async () => {
        const { recurringTransactions } = get();
        const { addTransaction } = useTransactionStore.getState();
        const now = new Date();
        const updates: { id: string; lastGeneratedDate: string }[] = [];

        for (const recurring of recurringTransactions) {
            if (!recurring.active) continue;

            let lastDate = recurring.lastGeneratedDate ? parseISO(recurring.lastGeneratedDate) : parseISO(recurring.startDate);
            let shouldGenerate = false;

            const checkDate = new Date(lastDate);
            if (recurring.frequency === 'daily') {
                checkDate.setDate(checkDate.getDate() + 1);
            } else if (recurring.frequency === 'weekly') {
                checkDate.setDate(checkDate.getDate() + 7);
            } else if (recurring.frequency === 'monthly') {
                checkDate.setMonth(checkDate.getMonth() + 1);
            }

            if (checkDate <= now) {
                shouldGenerate = true;
            }

            if (shouldGenerate) {
                await addTransaction({
                    amount: recurring.amount,
                    description: recurring.description,
                    type: recurring.type,
                    categoryId: recurring.categoryId,
                    servantId: recurring.servantId,
                    memberId: recurring.memberId,
                    date: now.toISOString(),
                });

                updates.push({ id: recurring.id, lastGeneratedDate: now.toISOString() });
            }
        }

        if (updates.length > 0) {
            for (const update of updates) {
                await supabase
                    .from('recurring_transactions')
                    .update({ last_generated_date: update.lastGeneratedDate })
                    .eq('id', update.id);
            }

            // Refresh store state locally
            set((state) => ({
                recurringTransactions: state.recurringTransactions.map((r) => {
                    const matchedUpdate = updates.find((u) => u.id === r.id);
                    return matchedUpdate ? { ...r, lastGeneratedDate: matchedUpdate.lastGeneratedDate } : r;
                }),
            }));
        }
    },

    clearRecurringTransactions: () => set({ recurringTransactions: [] }),
}));
