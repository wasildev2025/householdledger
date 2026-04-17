/**
 * AI Insight Store
 *
 * Singleton cache for Gemini AI spending insights.
 * Prevents duplicate API calls from HomeScreen and ReportsScreen
 * firing concurrently with the same data.
 *
 * Cache TTL: 30 minutes. Only refetches if transaction signature changes
 * AND the last fetch was more than 30 minutes ago.
 */

import { create } from 'zustand';
import { aiService } from '../../lib/aiService';
import { secureLog } from '../../lib/securityUtils';
import type { Category } from '../../types';

const CACHE_TTL_MS = 30 * 60 * 1000; // 30 minutes

interface AiInsightState {
    insight: { title: string; insight: string; type: 'warning' | 'tip' | 'positive' } | null;
    isLoading: boolean;
    lastFetchSig: string;
    lastFetchTime: number;

    fetchInsight: (transactions: any[], categories: Category[]) => Promise<void>;
    clearInsight: () => void;
}

export const useAiInsightStore = create<AiInsightState>()((set, get) => ({
    insight: null,
    isLoading: false,
    lastFetchSig: '',
    lastFetchTime: 0,

    fetchInsight: async (transactions, categories) => {
        if (!transactions || transactions.length === 0) return;

        // Build a lightweight signature: count + total sum (no JSON.stringify of the whole array)
        const sig = `${transactions.length}-${transactions.reduce((s, t) => s + t.amount, 0).toFixed(0)}`;
        const now = Date.now();
        const state = get();

        // Cache hit: same data signature AND fetched recently
        const isFresh =
            state.lastFetchSig === sig && now - state.lastFetchTime < CACHE_TTL_MS;
        if (isFresh || state.isLoading) return;

        set({ isLoading: true, lastFetchSig: sig, lastFetchTime: now });

        try {
            const result = await aiService.generateSpendingInsight(transactions, categories);
            set({ insight: result, isLoading: false });
        } catch (err) {
            secureLog.info('AI Insight store: fetch error silenced');
            set({ isLoading: false });
        }
    },

    clearInsight: () => set({ insight: null, lastFetchSig: '', lastFetchTime: 0 }),
}));
