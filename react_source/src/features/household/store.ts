/**
 * Household Store
 *
 * Manages household creation, joining via invite codes,
 * and household-level data (dairy prices, summary).
 */

import { create } from 'zustand';
import { supabase } from '../../lib/supabase';
import { useAuthStore } from '../auth/store';
import { secureLog } from '../../lib/securityUtils';

import { Household } from '../../types';

interface HouseholdState {
    // State
    household: Household | null;
    milkPrice: number;
    yogurtPrice: number;

    // Actions
    setHousehold: (household: Household | null) => void;
    createHousehold: (name: string) => Promise<void>;
    joinHousehold: (inviteCode: string) => Promise<void>;
    loadHousehold: () => Promise<void>;
    setMilkPrice: (price: number) => Promise<void>;
    setYogurtPrice: (price: number) => Promise<void>;
}

// ---- Store ----

export const useHouseholdStore = create<HouseholdState>()((set, get) => ({
    household: null,
    milkPrice: 150,
    yogurtPrice: 200,

    setHousehold: (household) => set({ household }),

    loadHousehold: async () => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) {
            set({ household: null });
            return;
        }

        const { data, error } = await supabase
            .from('households')
            .select('*')
            .eq('id', profile.householdId)
            .single();

        if (error || !data) {
            secureLog.error('Error loading household:', error);
            set({ household: null });
            return;
        }

        set({
            household: {
                id: data.id,
                name: data.name,
                admin_id: data.owner_id,
                created_at: data.created_at,
            },
            milkPrice: data.milk_price ?? 150,
            yogurtPrice: data.yogurt_price ?? 200,
        });
    },

    createHousehold: async (name) => {
        const user = useAuthStore.getState().user;
        if (!user) throw new Error('User not logged in');

        const { data: household, error: hError } = await supabase
            .from('households')
            .insert([{ name, owner_id: user.id }])
            .select()
            .single();

        if (hError) throw hError;

        // Update profile to admin role
        await supabase
            .from('profiles')
            .update({ role: 'admin', household_id: household.id })
            .eq('id', user.id);

        // Reload profile (role is now admin) and then household
        await useAuthStore.getState().loadProfile();
        await get().loadHousehold();
    },

    joinHousehold: async (inviteCode) => {
        const user = useAuthStore.getState().user;
        if (!user) throw new Error('User not logged in');

        // Check servants table
        const { data: servant } = await supabase
            .from('servants')
            .select('*')
            .eq('invite_code', inviteCode)
            .single();

        if (servant) {
            await supabase
                .from('profiles')
                .update({
                    role: 'servant',
                    servant_id: servant.id,
                    household_id: servant.household_id,
                    name: servant.name,
                })
                .eq('id', user.id);

            // Invalidate invite code
            await supabase.from('servants').update({ invite_code: null }).eq('id', servant.id);

            await useAuthStore.getState().loadProfile();
            await get().loadHousehold();
            return;
        }

        // Check members table
        const { data: member } = await supabase
            .from('members')
            .select('*')
            .eq('invite_code', inviteCode)
            .single();

        if (member) {
            await supabase
                .from('profiles')
                .update({
                    role: 'member',
                    member_id: member.id,
                    household_id: member.household_id,
                    name: member.name,
                })
                .eq('id', user.id);

            await supabase.from('members').update({ invite_code: null }).eq('id', member.id);

            await useAuthStore.getState().loadProfile();
            await get().loadHousehold();
            return;
        }

        throw new Error('Invalid or expired invite code');
    },

    setMilkPrice: async (price) => {
        const { household } = get();
        if (!household) return;

        set({ milkPrice: price });

        const { error } = await supabase
            .from('households')
            .update({ milk_price: price })
            .eq('id', household.id);

        if (error) {
            secureLog.error('Error updating milk price:', error);
        }
    },

    setYogurtPrice: async (price) => {
        const { household } = get();
        if (!household) return;

        set({ yogurtPrice: price });

        const { error } = await supabase
            .from('households')
            .update({ yogurt_price: price })
            .eq('id', household.id);

        if (error) {
            secureLog.error('Error updating yogurt price:', error);
        }
    },
}));
