import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { format, parseISO } from 'date-fns';
import { useTheme } from '../../../hooks/useTheme';
import { Transaction } from '../../transactions/store';
import { Category } from '../../categories/store';

interface RecentTransactionsListProps {
    transactions: Transaction[];
    categories: Category[];
    currencySymbol: string;
    onSeeAllPress: () => void;
}

export function RecentTransactionsList({ transactions, categories, currencySymbol, onSeeAllPress }: RecentTransactionsListProps) {
    const { colors, isDarkMode } = useTheme();

    const recentTransactions = [...transactions]
        .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
        .slice(0, 5);

    return (
        <View>
            <View style={styles.sectionHeader}>
                <Text style={[styles.sectionTitle, { color: colors.textDark }]}>Recent Activity</Text>
                <TouchableOpacity onPress={onSeeAllPress}>
                    <Text style={styles.seeAllText}>See All</Text>
                </TouchableOpacity>
            </View>

            <View style={styles.transactionContainer}>
                {recentTransactions.length > 0 ? (
                    recentTransactions.map((item) => {
                        const category = categories.find(c => c.id === item.categoryId);
                        return (
                            <TouchableOpacity
                                key={item.id}
                                style={[styles.transactionItem, {
                                    backgroundColor: isDarkMode ? 'rgba(30, 41, 59, 0.7)' : '#FFFFFF',
                                    borderColor: isDarkMode ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.05)',
                                }]}
                                activeOpacity={0.7}
                                onPress={onSeeAllPress}
                            >
                                <View style={[styles.categoryIcon, { backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)' }]}>
                                    <Ionicons name={(category?.icon as any) || 'receipt-outline'} size={20} color={category?.color || '#3B82F6'} />
                                </View>
                                <View style={styles.transactionDetails}>
                                    <Text style={[styles.transactionDesc, { color: colors.textDark }]} numberOfLines={1}>
                                        {item.description || category?.name}
                                    </Text>
                                    <Text style={[styles.transactionDate, { color: colors.textGrey }]}>
                                        {format(parseISO(item.date), 'MMM dd, HH:mm')}
                                    </Text>
                                </View>
                                <View style={{ alignItems: 'flex-end' }}>
                                    <Text style={[
                                        styles.transactionAmount,
                                        { color: item.type === 'expense' ? (isDarkMode ? '#FCA5A5' : '#EF4444') : '#10B981' }
                                    ]}>
                                        {item.type === 'expense' ? '-' : '+'}{currencySymbol} {item.amount.toLocaleString()}
                                    </Text>
                                    <Text style={{ fontSize: 9, fontWeight: '700', color: colors.textGrey, marginTop: 4 }}>
                                        {item.type.toUpperCase()}
                                    </Text>
                                </View>
                            </TouchableOpacity>
                        );
                    })
                ) : (
                    <View style={styles.emptyState}>
                        <MaterialCommunityIcons name="inbox-outline" size={48} color={colors.textGrey} style={{ opacity: 0.5 }} />
                        <Text style={[styles.emptyText, { color: colors.textGrey }]}>No recent activity</Text>
                    </View>
                )}
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    sectionHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        paddingHorizontal: 20,
        marginBottom: 16,
    },
    sectionTitle: {
        fontSize: 22,
        fontWeight: '900',
        letterSpacing: -0.5,
    },
    seeAllText: {
        fontSize: 14,
        fontWeight: '700',
        color: '#3B82F6',
    },
    transactionContainer: {
        paddingHorizontal: 20,
    },
    transactionItem: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: 16,
        borderRadius: 24,
        marginBottom: 12,
        borderWidth: 1,
        shadowColor: "#000",
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.05,
        shadowRadius: 10,
        elevation: 2
    },
    categoryIcon: {
        width: 52,
        height: 52,
        borderRadius: 18,
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 16,
    },
    transactionDetails: {
        flex: 1,
    },
    transactionDesc: {
        fontSize: 16,
        fontWeight: '700',
        marginBottom: 6,
    },
    transactionDate: {
        fontSize: 12,
        fontWeight: '600',
    },
    transactionAmount: {
        fontSize: 17,
        fontWeight: '800',
        fontVariant: ['tabular-nums'],
    },
    emptyState: {
        alignItems: 'center',
        paddingVertical: 40,
    },
    emptyText: {
        marginTop: 12,
        fontWeight: '600',
    }
});
