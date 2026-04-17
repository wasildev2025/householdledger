/**
 * Categories Store
 *
 * Manages income and expense categories.
 * Realtime subscription syncs changes across all household devices.
 */

import { create } from 'zustand';
import { supabase } from '../../lib/supabase';
import { useAuthStore } from '../auth/store';
import { useOfflineStore } from '../../shared/store/offline';
import { secureLog } from '../../lib/securityUtils';
import type { RealtimeChannel } from '@supabase/supabase-js';

export interface Category {
    id: string;
    name: string;
    icon: string;
    color: string;
    budget: number;
    householdId: string;
}

interface CategoryState {
    categories: Category[];
    isLoading: boolean;
    _channel: RealtimeChannel | null;

    loadCategories: () => Promise<void>;
    addCategory: (category: Omit<Category, 'id' | 'householdId'>) => Promise<void>;
    updateCategory: (id: string, category: Partial<Category>) => Promise<void>;
    deleteCategory: (id: string) => Promise<void>;
    setupRealtimeSubscription: () => void;
    cleanupSubscription: () => void;
}

function mapDbToCategory(c: any): Category {
    return {
        id: c.id,
        name: c.name,
        icon: c.icon,
        color: c.color,
        budget: c.budget,
        householdId: c.household_id,
    };
}

export const useCategoryStore = create<CategoryState>()((set, get) => ({
    categories: [],
    isLoading: false,
    _channel: null,

    loadCategories: async () => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        set({ isLoading: true });

        const { data, error } = await supabase
            .from('categories')
            .select('*')
            .eq('household_id', profile.householdId);

        if (error) {
            secureLog.error('Error loading categories:', error);
            set({ isLoading: false });
            return;
        }

        set({
            categories: (data ?? []).map(mapDbToCategory),
            isLoading: false,
        });
    },

    addCategory: async (category) => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        const newId = crypto.randomUUID();
        const newCategory: Category = { ...category, id: newId, householdId: profile.householdId };

        // Optimistic update
        set((state) => ({ categories: [...state.categories, newCategory] }));

        const dbData = {
            id: newCategory.id,
            name: newCategory.name,
            icon: newCategory.icon,
            color: newCategory.color,
            budget: newCategory.budget,
            household_id: newCategory.householdId,
        };

        const { isOffline, queueMutation } = useOfflineStore.getState();

        if (isOffline) {
            queueMutation({ action: 'insert', table: 'categories', data: dbData });
            return;
        }

        const { error } = await supabase.from('categories').insert([dbData]);
        if (error) {
            secureLog.error('Error adding category:', error);
            queueMutation({ action: 'insert', table: 'categories', data: dbData });
        }
    },

    updateCategory: async (id, category) => {
        set((state) => ({
            categories: state.categories.map(c => c.id === id ? { ...c, ...category } : c)
        }));

        const dbUpdate: any = { ...category };
        if (dbUpdate.householdId) {
            dbUpdate.household_id = dbUpdate.householdId;
            delete dbUpdate.householdId;
        }

        const { isOffline, queueMutation } = useOfflineStore.getState();

        if (isOffline) {
            queueMutation({ action: 'update', table: 'categories', data: { ...dbUpdate, id } });
            return;
        }

        const { error } = await supabase.from('categories').update(dbUpdate).eq('id', id);
        if (error) {
            secureLog.error('Error updating category:', error);
            queueMutation({ action: 'update', table: 'categories', data: { ...dbUpdate, id } });
        }
    },

    deleteCategory: async (id) => {
        set((state) => ({
            categories: state.categories.filter(c => c.id !== id)
        }));

        const { isOffline, queueMutation } = useOfflineStore.getState();

        if (isOffline) {
            queueMutation({ action: 'delete', table: 'categories', data: { id } });
            return;
        }

        const { error } = await supabase.from('categories').delete().eq('id', id);
        if (error) {
            secureLog.error('Error deleting category:', error);
            queueMutation({ action: 'delete', table: 'categories', data: { id } });
        }
    },

    setupRealtimeSubscription: () => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        get().cleanupSubscription();

        const channel = supabase
            .channel(`categories-${profile.householdId}`)
            .on(
                'postgres_changes',
                {
                    event: '*',
                    schema: 'public',
                    table: 'categories',
                    filter: `household_id=eq.${profile.householdId}`,
                },
                (payload) => {
                    if (payload.eventType === 'INSERT') {
                        const cat = mapDbToCategory(payload.new);
                        set((s) => {
                            if (s.categories.some((c) => c.id === cat.id)) return s;
                            return { categories: [...s.categories, cat] };
                        });
                    } else if (payload.eventType === 'UPDATE') {
                        const updated = mapDbToCategory(payload.new);
                        set((s) => ({
                            categories: s.categories.map((c) => c.id === updated.id ? updated : c),
                        }));
                    } else if (payload.eventType === 'DELETE') {
                        set((s) => ({
                            categories: s.categories.filter((c) => c.id !== payload.old.id),
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
}));
