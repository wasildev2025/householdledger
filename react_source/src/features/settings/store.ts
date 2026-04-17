/**
 * Settings Store
 *
 * Manages user preferences that are persisted locally.
 * This is the ONLY store that should be persisted to AsyncStorage.
 * All entity data (transactions, servants, etc.) should be fetched from Supabase.
 */

import { create } from 'zustand';
import { createJSONStorage, persist } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';

// ---- Types ----

interface SettingsState {
    // Preferences
    isDarkMode: boolean;
    notificationsEnabled: boolean;
    currencySymbol: string;
    biometricAuthEnabled: boolean;
    adminName: string;
    adminBudget: number | undefined;

    // Actions
    toggleDarkMode: () => void;
    setNotificationsEnabled: (enabled: boolean) => void;
    setCurrencySymbol: (symbol: string) => void;
    setBiometricAuthEnabled: (enabled: boolean) => void;
    setAdminName: (name: string) => void;
    setAdminBudget: (budget: number | undefined) => void;
}

// ---- Store ----

export const useSettingsStore = create<SettingsState>()(
    persist(
        (set) => ({
            isDarkMode: false,
            notificationsEnabled: true,
            currencySymbol: '$',
            biometricAuthEnabled: false,
            adminName: 'Admin',
            adminBudget: undefined,

            toggleDarkMode: () => set((s) => ({ isDarkMode: !s.isDarkMode })),
            setNotificationsEnabled: (enabled) => set({ notificationsEnabled: enabled }),
            setCurrencySymbol: (symbol) => set({ currencySymbol: symbol }),
            setBiometricAuthEnabled: (enabled) => set({ biometricAuthEnabled: enabled }),
            setAdminName: (name) => set({ adminName: name }),
            setAdminBudget: (budget) => set({ adminBudget: budget }),
        }),
        {
            name: 'household-ledger-settings',
            storage: createJSONStorage(() => AsyncStorage),
        }
    )
);
