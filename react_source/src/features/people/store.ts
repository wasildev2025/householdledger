/**
 * People Store
 *
 * Manages servants (staff) and members (family) CRUD.
 * Admin-only write operations enforced by RLS on the server.
 * Realtime subscriptions keep all devices in sync.
 */

import { create } from 'zustand';
import { supabase } from '../../lib/supabase';
import { useAuthStore } from '../auth/store';
import { useOfflineStore } from '../../shared/store/offline';
import { generateSecureInviteCode, secureLog } from '../../lib/securityUtils';
import type { RealtimeChannel } from '@supabase/supabase-js';

// ---- Types ----

export interface Servant {
    id: string;
    name: string;
    role: string;
    phoneNumber?: string;
    salary?: number;
    budget?: number;
    balance: number;
    inviteCode?: string | null;
    householdId?: string;
}

export interface Member {
    id: string;
    name: string;
    role: 'member';
    inviteCode?: string | null;
    householdId?: string;
}

interface PeopleState {
    servants: Servant[];
    members: Member[];
    isLoading: boolean;
    _servantsChannel: RealtimeChannel | null;
    _membersChannel: RealtimeChannel | null;

    // Actions
    fetchPeople: () => Promise<void>;
    addServant: (servant: Omit<Servant, 'id' | 'balance' | 'inviteCode' | 'householdId'>) => Promise<string | undefined>;
    updateServant: (id: string, updates: Partial<Servant>) => Promise<void>;
    deleteServant: (id: string) => Promise<void>;
    addMember: (member: Omit<Member, 'id' | 'role' | 'inviteCode' | 'householdId'>) => Promise<string | undefined>;
    updateMember: (id: string, updates: Partial<Member>) => Promise<void>;
    deleteMember: (id: string) => Promise<void>;
    setupRealtimeSubscription: () => void;
    cleanupSubscription: () => void;
    clearPeople: () => void;
}

// ---- DB Mapping ----

function mapDbToServant(row: any): Servant {
    return {
        id: row.id,
        name: row.name,
        role: row.role,
        phoneNumber: row.phone_number ?? undefined,
        salary: row.salary,
        budget: row.budget,
        balance: 0, // Recalculated from transactions
        inviteCode: row.invite_code,
        householdId: row.household_id,
    };
}

function mapDbToMember(row: any): Member {
    return {
        id: row.id,
        name: row.name,
        role: 'member',
        inviteCode: row.invite_code,
        householdId: row.household_id,
    };
}

// ---- Store ----

export const usePeopleStore = create<PeopleState>()((set, get) => ({
    servants: [],
    members: [],
    isLoading: false,
    _servantsChannel: null,
    _membersChannel: null,

    fetchPeople: async () => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        set({ isLoading: true });

        try {
            const [servantsRes, membersRes] = await Promise.all([
                supabase.from('servants').select('*').eq('household_id', profile.householdId),
                supabase.from('members').select('*').eq('household_id', profile.householdId),
            ]);

            set({
                servants: (servantsRes.data ?? []).map(mapDbToServant),
                members: (membersRes.data ?? []).map(mapDbToMember),
                isLoading: false,
            });
        } catch (err) {
            secureLog.error('Error fetching people:', err);
            set({ isLoading: false });
        }
    },

    addServant: async (servant) => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        const inviteCode = generateSecureInviteCode();
        const newServant = {
            id: crypto.randomUUID(),
            name: servant.name,
            role: servant.role,
            phone_number: servant.phoneNumber,
            salary: servant.salary,
            budget: servant.budget,
            invite_code: inviteCode,
            household_id: profile.householdId,
        };

        // Optimistic update
        set((s) => ({
            servants: [
                ...s.servants,
                { ...mapDbToServant(newServant), inviteCode },
            ],
        }));

        const { error } = await supabase.from('servants').insert([newServant]);

        if (error) {
            secureLog.error('Error adding servant:', error);
            useOfflineStore.getState().queueMutation({
                action: 'insert',
                table: 'servants',
                data: newServant,
            });
        }
        return inviteCode;
    },

    updateServant: async (id, updates) => {
        set((s) => ({
            servants: s.servants.map((sv) => (sv.id === id ? { ...sv, ...updates } : sv)),
        }));

        const dbUpdate: Record<string, unknown> = {};
        if (updates.name !== undefined) dbUpdate.name = updates.name;
        if (updates.role !== undefined) dbUpdate.role = updates.role;
        if (updates.phoneNumber !== undefined) dbUpdate.phone_number = updates.phoneNumber;
        if (updates.salary !== undefined) dbUpdate.salary = updates.salary;
        if (updates.budget !== undefined) dbUpdate.budget = updates.budget;

        const { error } = await supabase.from('servants').update(dbUpdate).eq('id', id);

        if (error) {
            secureLog.error('Error updating servant:', error);
            useOfflineStore.getState().queueMutation({
                action: 'update',
                table: 'servants',
                data: { ...dbUpdate, id },
            });
        }
    },

    deleteServant: async (id) => {
        set((s) => ({
            servants: s.servants.filter((sv) => sv.id !== id),
        }));

        const { error } = await supabase.from('servants').delete().eq('id', id);

        if (error) {
            secureLog.error('Error deleting servant:', error);
            useOfflineStore.getState().queueMutation({
                action: 'delete',
                table: 'servants',
                data: { id },
            });
        }
    },

    addMember: async (member) => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        const inviteCode = generateSecureInviteCode();
        const newMember = {
            id: crypto.randomUUID(),
            name: member.name,
            role: 'member',
            invite_code: inviteCode,
            household_id: profile.householdId,
        };

        set((s) => ({
            members: [...s.members, mapDbToMember(newMember)],
        }));

        const { error } = await supabase.from('members').insert([newMember]);

        if (error) {
            secureLog.error('Error adding member:', error);
            useOfflineStore.getState().queueMutation({
                action: 'insert',
                table: 'members',
                data: newMember,
            });
        }
        return inviteCode;
    },

    updateMember: async (id, updates) => {
        set((s) => ({
            members: s.members.map((m) => (m.id === id ? { ...m, ...updates } : m)),
        }));

        const dbUpdate: Record<string, unknown> = {};
        if (updates.name !== undefined) dbUpdate.name = updates.name;

        const { error } = await supabase.from('members').update(dbUpdate).eq('id', id);

        if (error) {
            secureLog.error('Error updating member:', error);
            useOfflineStore.getState().queueMutation({
                action: 'update',
                table: 'members',
                data: { ...dbUpdate, id },
            });
        }
    },

    deleteMember: async (id) => {
        set((s) => ({
            members: s.members.filter((m) => m.id !== id),
        }));

        const { error } = await supabase.from('members').delete().eq('id', id);

        if (error) {
            secureLog.error('Error deleting member:', error);
            useOfflineStore.getState().queueMutation({
                action: 'delete',
                table: 'members',
                data: { id },
            });
        }
    },

    setupRealtimeSubscription: () => {
        const profile = useAuthStore.getState().profile;
        if (!profile?.householdId) return;

        // Clean up old channels
        get().cleanupSubscription();

        // Channel 1: Servants
        const servantsChannel = supabase
            .channel(`servants-${profile.householdId}`)
            .on(
                'postgres_changes',
                {
                    event: '*',
                    schema: 'public',
                    table: 'servants',
                    filter: `household_id=eq.${profile.householdId}`,
                },
                (payload) => {
                    if (payload.eventType === 'INSERT') {
                        const sv = mapDbToServant(payload.new);
                        set((s) => {
                            if (s.servants.some((x) => x.id === sv.id)) return s;
                            return { servants: [...s.servants, sv] };
                        });
                    } else if (payload.eventType === 'UPDATE') {
                        const updated = mapDbToServant(payload.new);
                        set((s) => ({
                            servants: s.servants.map((x) => x.id === updated.id ? { ...updated, balance: x.balance } : x),
                        }));
                    } else if (payload.eventType === 'DELETE') {
                        set((s) => ({
                            servants: s.servants.filter((x) => x.id !== payload.old.id),
                        }));
                    }
                }
            )
            .subscribe();

        // Channel 2: Members
        const membersChannel = supabase
            .channel(`members-${profile.householdId}`)
            .on(
                'postgres_changes',
                {
                    event: '*',
                    schema: 'public',
                    table: 'members',
                    filter: `household_id=eq.${profile.householdId}`,
                },
                (payload) => {
                    if (payload.eventType === 'INSERT') {
                        const m = mapDbToMember(payload.new);
                        set((s) => {
                            if (s.members.some((x) => x.id === m.id)) return s;
                            return { members: [...s.members, m] };
                        });
                    } else if (payload.eventType === 'UPDATE') {
                        const updated = mapDbToMember(payload.new);
                        set((s) => ({
                            members: s.members.map((x) => x.id === updated.id ? updated : x),
                        }));
                    } else if (payload.eventType === 'DELETE') {
                        set((s) => ({
                            members: s.members.filter((x) => x.id !== payload.old.id),
                        }));
                    }
                }
            )
            .subscribe();

        set({ _servantsChannel: servantsChannel, _membersChannel: membersChannel });
    },

    cleanupSubscription: () => {
        const { _servantsChannel, _membersChannel } = get();
        if (_servantsChannel) supabase.removeChannel(_servantsChannel);
        if (_membersChannel) supabase.removeChannel(_membersChannel);
        set({ _servantsChannel: null, _membersChannel: null });
    },

    clearPeople: () => {
        get().cleanupSubscription();
        set({ servants: [], members: [], _servantsChannel: null, _membersChannel: null });
    },
}));
