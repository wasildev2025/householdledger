import { useMemo } from 'react';
import { startOfMonth, endOfMonth, startOfWeek, endOfWeek, startOfYear, endOfYear, parseISO, isWithinInterval } from 'date-fns';
import { useAuthStore } from '../../auth/store';
import { useTransactionStore } from '../../transactions/store';
import { usePeopleStore } from '../../people/store';
import { useCategoryStore } from '../../categories/store';

export type TimeRange = 'week' | 'month' | 'year';
export type FilterType = 'all' | 'admin' | 'servant' | 'member';

export function useHouseholdMetrics(timeRange: TimeRange, filter: FilterType, currentDate: Date) {
    const profile = useAuthStore(s => s.profile);
    const transactions = useTransactionStore(s => s.transactions);
    const servants = usePeopleStore(s => s.servants);
    const members = usePeopleStore(s => s.members);
    const categories = useCategoryStore(s => s.categories);

    const isServant = profile?.role === 'servant';
    const isMember = profile?.role === 'member';

    const { start, end } = useMemo(() => {
        switch (timeRange) {
            case 'week': return { start: startOfWeek(currentDate), end: endOfWeek(currentDate) };
            case 'month': return { start: startOfMonth(currentDate), end: endOfMonth(currentDate) };
            case 'year': return { start: startOfYear(currentDate), end: endOfYear(currentDate) };
        }
    }, [timeRange, currentDate]);

    const allMonthTransactions = useMemo(() => {
        return transactions.filter((t) => {
            const date = parseISO(t.date);
            return isWithinInterval(date, { start, end });
        });
    }, [transactions, start, end]);

    const totalIncome = allMonthTransactions
        .filter(t => t.type === 'income')
        .reduce((sum, t) => sum + t.amount, 0);

    const personalExpenses = allMonthTransactions
        .filter(t => t.type === 'expense' && !t.servantId && !t.memberId)
        .reduce((sum, t) => sum + t.amount, 0);

    const staffTransfers = allMonthTransactions
        .filter(t => t.type === 'transfer' && !!t.servantId)
        .reduce((sum, t) => sum + t.amount, 0);

    const getExtraSpent = (people: any[], isStaff: boolean) => {
        return people.reduce((sum, p) => {
            const allocation = allMonthTransactions
                .filter(t => t.type === 'transfer' && (isStaff ? t.servantId === p.id : t.memberId === p.id))
                .reduce((s, t) => s + t.amount, 0);
            const utilization = allMonthTransactions
                .filter(t => t.type === 'expense' && (isStaff ? t.servantId === p.id : t.memberId === p.id))
                .reduce((s, t) => s + t.amount, 0);
            return sum + (utilization > allocation ? (utilization - allocation) : 0);
        }, 0);
    };

    const staffExtraSpent = getExtraSpent(servants, true);
    const memberExtraSpent = getExtraSpent(members, false);
    const totalExtraSpent = staffExtraSpent + memberExtraSpent;

    const totalSpentReal = (isServant || isMember)
        ? allMonthTransactions.filter(t => t.type === 'expense' && (isServant ? t.servantId === profile?.servantId : t.memberId === profile?.memberId)).reduce((s, t) => s + t.amount, 0)
        : (personalExpenses + staffTransfers);

    const displayIncome = (isServant || isMember)
        ? allMonthTransactions.filter(t => t.type === 'transfer' && (isServant ? t.servantId === profile?.servantId : t.memberId === profile?.memberId)).reduce((s, t) => s + t.amount, 0)
        : totalIncome;

    const netBalance = displayIncome - totalSpentReal;
    const remainingSalary = netBalance;

    const filteredTransactions = useMemo(() => {
        let result = [...allMonthTransactions];
        if (isServant) {
            return result.filter(t => t.servantId === profile?.servantId);
        }
        if (isMember) {
            return result.filter(t => t.memberId === profile?.memberId);
        }
        if (filter === 'admin') {
            result = result.filter(t => !t.servantId && !t.memberId && t.type !== 'transfer');
        } else if (filter === 'servant') {
            result = result.filter(t => !!t.servantId);
        } else if (filter === 'member') {
            result = result.filter(t => !!t.memberId);
        }
        return result;
    }, [allMonthTransactions, isServant, isMember, filter, profile]);

    const memberStats = useMemo(() => {
        return members.map(member => {
            const allocation = allMonthTransactions
                .filter(t => t.type === 'transfer' && t.memberId === member.id)
                .reduce((s, t) => s + t.amount, 0);
            const utilization = allMonthTransactions
                .filter(t => t.type === 'expense' && t.memberId === member.id)
                .reduce((s, t) => s + t.amount, 0);
            return {
                ...member,
                allocation,
                utilization,
                balance: allocation - utilization,
                isOverspent: utilization > allocation
            };
        });
    }, [members, allMonthTransactions]);

    const staffStats = useMemo(() => {
        return servants.map(servant => {
            const allocation = allMonthTransactions
                .filter(t => t.type === 'transfer' && t.servantId === servant.id)
                .reduce((s, t) => s + t.amount, 0);
            const utilization = allMonthTransactions
                .filter(t => t.type === 'expense' && t.servantId === servant.id)
                .reduce((s, t) => s + t.amount, 0);
            return {
                ...servant,
                allocation,
                utilization,
                balance: allocation - utilization,
                isOverspent: utilization > allocation
            };
        });
    }, [servants, allMonthTransactions]);

    return {
        start,
        end,
        totalIncome,
        personalExpenses,
        staffTransfers,
        staffExtraSpent,
        totalExtraSpent,
        totalSpentReal,
        displayIncome,
        netBalance,
        remainingSalary,
        filteredTransactions,
        memberStats,
        staffStats,
        categories,
        isServant,
        isMember,
        isAdmin: profile?.role === 'admin'
    };
}
