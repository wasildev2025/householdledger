/**
 * Auth Store
 *
 * Manages authentication state, user session, and profile.
 * Roles are ALWAYS determined server-side from the profiles table.
 * No client-side role switching is possible.
 */

import { create } from 'zustand';
import { supabase } from '../../lib/supabase';
import type { Session, User } from '@supabase/supabase-js';

// ---- Types ----

export type UserRole = 'admin' | 'servant' | 'member';

export interface UserProfile {
    role: UserRole;
    servantId?: string;
    memberId?: string;
    householdId?: string;
    name: string;
}

interface AuthState {
    // State
    session: Session | null;
    user: User | null;
    profile: UserProfile | null;
    isProfileLoading: boolean;
    hasCompletedOnboarding: boolean;

    // Derived
    isAdmin: () => boolean;
    isServant: () => boolean;
    isMember: () => boolean;
    getDisplayName: () => string;

    // Actions
    setSession: (session: Session | null) => void;
    loadProfile: () => Promise<void>;
    signOut: () => Promise<void>;
    completeOnboarding: () => void;
}

// ---- Store ----

export const useAuthStore = create<AuthState>()((set, get) => ({
    session: null,
    user: null,
    profile: null,
    isProfileLoading: false,
    hasCompletedOnboarding: false,

    // Derived helpers
    isAdmin: () => get().profile?.role === 'admin',
    isServant: () => get().profile?.role === 'servant',
    isMember: () => get().profile?.role === 'member',

    getDisplayName: () => {
        const { profile } = get();
        if (!profile) return 'User';
        return profile.name || 'User';
    },

    setSession: (session) => {
        set({ session, user: session?.user ?? null });
    },

    /**
     * Loads the user's profile from the server.
     * The role is ALWAYS server-determined — never set client-side.
     */
    loadProfile: async () => {
        const { user } = get();
        if (!user) {
            set({ profile: null, isProfileLoading: false });
            return;
        }

        set({ isProfileLoading: true });
        try {
            const { data, error } = await supabase
                .from('profiles')
                .select('role, servant_id, member_id, household_id, name')
                .eq('id', user.id)
                .single();

            if (error || !data) {
                set({ profile: null });
                return;
            }

            set({
                profile: {
                    role: data.role,
                    servantId: data.servant_id ?? undefined,
                    memberId: data.member_id ?? undefined,
                    householdId: data.household_id ?? undefined,
                    name: data.name ?? 'User',
                },
            });
        } finally {
            set({ isProfileLoading: false });
        }
    },

    signOut: async () => {
        await supabase.auth.signOut();
        set({ session: null, user: null, profile: null });
    },

    completeOnboarding: () => set({ hasCompletedOnboarding: true }),
}));
