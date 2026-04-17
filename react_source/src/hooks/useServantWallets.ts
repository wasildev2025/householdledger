import { useMemo } from 'react';
import { useTransactionStore } from '../features/transactions/store';
import { usePeopleStore } from '../features/people/store';
import { useSettingsStore } from '../features/settings/store';

export const useServantWallets = (servantId: string | undefined | null) => {
    const { transactions } = useTransactionStore();
    const { members } = usePeopleStore();
    const { adminName } = useSettingsStore();

    const wallets = useMemo(() => {
        if (!servantId) return [];

        const myTransactions = transactions.filter(t => t.servantId === servantId);

        // 1. Admin Wallet
        const adminIncome = myTransactions
            .filter(t => t.type === 'transfer' && !t.memberId)
            .reduce((sum, t) => sum + t.amount, 0);
        const adminSpent = myTransactions
            .filter(t => t.type === 'expense' && !t.memberId)
            .reduce((sum, t) => sum + t.amount, 0);

        const adminWallet = {
            id: 'admin',
            name: adminName || 'Admin',
            balance: adminIncome - adminSpent,
            memberId: null
        };

        // 2. Member Wallets
        const memberWallets = members.map(m => {
            const mIncome = myTransactions
                .filter(t => t.type === 'transfer' && t.memberId === m.id)
                .reduce((sum, t) => sum + t.amount, 0);
            const mSpent = myTransactions
                .filter(t => t.type === 'expense' && t.memberId === m.id)
                .reduce((sum, t) => sum + t.amount, 0);

            return {
                id: m.id,
                name: m.name,
                balance: mIncome - mSpent,
                memberId: m.id
            };
        }).filter(w => w.balance !== 0); // Show if balance is non-zero (positive or negative)

        // Sort: Admin first, then by balance descending
        return [adminWallet, ...memberWallets].sort((a, b) => {
            if (a.id === 'admin') return -1;
            if (b.id === 'admin') return 1;
            return b.balance - a.balance;
        });
    }, [transactions, servantId, members, adminName]);

    const totalBalance = useMemo(() => {
        return wallets.reduce((sum, w) => sum + w.balance, 0);
    }, [wallets]);

    return { wallets, totalBalance };
};
