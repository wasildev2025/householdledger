import React, { useState } from 'react';
import { View, Text, ScrollView, StyleSheet, Dimensions, TouchableOpacity, StatusBar, Platform } from 'react-native';
import { BlurView } from 'expo-blur';
import { useAuthStore } from '../features/auth/store';
import { useTransactionStore } from '../features/transactions/store';
import { useCategoryStore } from '../features/categories/store';
import { useSettingsStore } from '../features/settings/store';
import { useTheme } from '../hooks/useTheme';
import { COLORS, SHADOWS, SIZES } from '../constants/theme';
import { STRINGS } from '../constants/strings';
import { TransactionType } from '../types';
import { BarChart, PieChart, LineChart } from 'react-native-chart-kit';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { format, startOfMonth, endOfMonth, subMonths, isWithinInterval, parseISO, eachDayOfInterval, startOfDay, endOfDay } from 'date-fns';
import { LinearGradient } from 'expo-linear-gradient';
import { useAiInsightStore } from '../features/ai/store';
import { secureLog } from '../lib/securityUtils';

const { width } = Dimensions.get('window');

export default function ReportsScreen() {
    const { colors, isDarkMode } = useTheme();

    const { profile } = useAuthStore();
    const { transactions } = useTransactionStore();
    const { categories } = useCategoryStore();
    const { currencySymbol } = useSettingsStore();

    const [selectedMonth, setSelectedMonth] = useState(new Date());

    const { insight: aiInsight, isLoading: isAiLoading, fetchInsight } = useAiInsightStore();
    const [aiError, setAiError] = useState<string | null>(null);

    const isServant = profile?.role === 'servant';
    const isMember = profile?.role === 'member';

    const currentUser = profile ? {
        servantId: profile.servantId,
        memberId: profile.memberId
    } : null;

    const monthStart = React.useMemo(() => startOfMonth(selectedMonth), [selectedMonth]);
    const monthEnd = React.useMemo(() => endOfMonth(selectedMonth), [selectedMonth]);
    const prevMonthStart = React.useMemo(() => startOfMonth(subMonths(selectedMonth, 1)), [selectedMonth]);
    const prevMonthEnd = React.useMemo(() => endOfMonth(subMonths(selectedMonth, 1)), [selectedMonth]);

    const currentMonthTransactions = React.useMemo(() =>
        transactions.filter(t => {
            const inMonth = isWithinInterval(parseISO(t.date), { start: monthStart, end: monthEnd });
            if (!inMonth) return false;
            if (isServant) return t.servantId === currentUser?.servantId;
            if (isMember) return t.memberId === currentUser?.memberId;
            return true;
        }),
        [transactions, monthStart, monthEnd, isServant, isMember, currentUser]
    );

    const prevMonthTransactions = React.useMemo(() =>
        transactions.filter(t => {
            const inMonth = isWithinInterval(parseISO(t.date), { start: prevMonthStart, end: prevMonthEnd });
            if (!inMonth) return false;
            if (isServant) return t.servantId === currentUser?.servantId;
            if (isMember) return t.memberId === currentUser?.memberId;
            return true;
        }),
        [transactions, prevMonthStart, prevMonthEnd, isServant, isMember, currentUser]
    );

    const calcMetrics = React.useCallback((txs: any[]) => {
        if (isServant || isMember) {
            // Servant/Member's perspective (Personal Wallet)
            const personId = isServant ? currentUser?.servantId : currentUser?.memberId;
            const income = txs
                .filter(t => t.type === 'transfer' && (isServant ? t.servantId === personId : t.memberId === personId))
                .reduce((sum, t) => sum + t.amount, 0);
            const expense = txs
                .filter(t => t.type === 'expense' && (isServant ? t.servantId === personId : t.memberId === personId))
                .reduce((sum, t) => sum + t.amount, 0);
            return { income, expense };
        } else {
            // Admin/Owner's perspective
            const income = txs
                .filter(t => t.type === 'income')
                .reduce((sum, t) => sum + t.amount, 0);
            const personalEx = txs
                .filter(t => t.type === 'expense' && !t.servantId && !t.memberId)
                .reduce((sum, t) => sum + t.amount, 0);
            const transfers = txs
                .filter(t => t.type === 'transfer')
                .reduce((sum, t) => sum + t.amount, 0);
            return { income, expense: personalEx + transfers };
        }
    }, [isServant, isMember, currentUser]);

    const currentMetrics = React.useMemo(() => calcMetrics(currentMonthTransactions), [currentMonthTransactions, calcMetrics]);
    const prevMetrics = React.useMemo(() => calcMetrics(prevMonthTransactions), [prevMonthTransactions, calcMetrics]);

    const currentExpense = currentMetrics.expense;
    const currentIncome = currentMetrics.income;
    const prevExpense = prevMetrics.expense;
    const prevIncome = prevMetrics.income;

    // Chart Data for Budget vs Actual
    const budgetData = categories
        .filter(c => c.budget && c.budget > 0)
        .map(c => {
            const actual = currentMonthTransactions
                .filter(t => t.categoryId === c.id && t.type === 'expense')
                .reduce((sum, t) => sum + t.amount, 0);
            return {
                name: c.name,
                budget: c.budget || 0,
                actual,
                color: c.color,
                percentage: Math.min((actual / (c.budget || 1)) * 100, 100)
            };
        });

    const [trendPeriod, setTrendPeriod] = useState<'1M' | '6M' | '1Y'>('6M');

    // Trend Chart Data
    const trendData = React.useMemo(() => {
        if (trendPeriod === '1M') {
            const days = eachDayOfInterval({ start: monthStart, end: monthEnd });
            const labels = days.map((d, i) => (i % 5 === 0 || i === days.length - 1) ? format(d, 'd') : '');

            const metricsData = days.map(d => {
                const s = startOfDay(d);
                const e = endOfDay(d);
                const txs = transactions.filter(t => {
                    const inRange = isWithinInterval(parseISO(t.date), { start: s, end: e });
                    if (!inRange) return false;
                    if (isServant) return t.servantId === currentUser?.servantId;
                    if (isMember) return t.memberId === currentUser?.memberId;
                    return true;
                });
                return calcMetrics(txs);
            });

            return {
                labels,
                datasets: [
                    { data: metricsData.map(m => m.income), color: (opacity = 1) => `rgba(16, 185, 129, ${opacity})`, strokeWidth: 2 },
                    { data: metricsData.map(m => m.expense), color: (opacity = 1) => `rgba(239, 68, 68, ${opacity})`, strokeWidth: 2 }
                ],
                legend: ["Income", "Expense"]
            };
        }

        let monthsToFetch = 6;
        if (trendPeriod === '1Y') monthsToFetch = 12;

        const months = Array.from({ length: monthsToFetch }).map((_, i) => subMonths(new Date(), (monthsToFetch - 1) - i));
        let labels = months.map(m => format(m, 'MMM'));

        if (trendPeriod === '1Y') {
            labels = months.map((m, i) => i % 2 === 0 ? format(m, 'MMM') : '');
        }

        const metricsData = months.map(m => {
            const start = startOfMonth(m);
            const end = endOfMonth(m);
            const txs = transactions.filter(t => {
                const inMonth = isWithinInterval(parseISO(t.date), { start, end });
                if (!inMonth) return false;
                if (isServant) return t.servantId === currentUser?.servantId;
                if (isMember) return t.memberId === currentUser?.memberId;
                return true;
            });
            return calcMetrics(txs);
        });

        const incomeData = metricsData.map(d => d.income);
        const expenseData = metricsData.map(d => d.expense);

        return {
            labels: labels.length > 0 ? labels : ["Current"],
            datasets: [
                {
                    data: incomeData.length > 0 ? incomeData : [0],
                    color: (opacity = 1) => `rgba(16, 185, 129, ${opacity})`,
                    strokeWidth: 3
                },
                {
                    data: expenseData.length > 0 ? expenseData : [0],
                    color: (opacity = 1) => `rgba(239, 68, 68, ${opacity})`,
                    strokeWidth: 3
                }
            ],
            legend: ["Income", "Expense"]
        };
    }, [transactions, trendPeriod]);

    const txSignature = React.useMemo(() =>
        `${format(selectedMonth, 'yyyy-MM')}-${currentMonthTransactions.length}-${currentMonthTransactions.reduce((acc, t) => acc + t.amount, 0).toFixed(0)}`,
        [selectedMonth, currentMonthTransactions]
    );

    React.useEffect(() => {
        const fetchRemoteInsight = async () => {
            setAiError(null);
            try {
                await fetchInsight(currentMonthTransactions, categories);
            } catch (error) {
                secureLog.error("Reports AI fetch failed:", error);
                setAiError(STRINGS.reports.aiError);
            }
        };

        const timeout = setTimeout(fetchRemoteInsight, 800);
        return () => clearTimeout(timeout);
    }, [txSignature, categories.length]);

    return (
        <View style={[styles.container, { backgroundColor: colors.background }]}>
            <StatusBar barStyle={isDarkMode ? "light-content" : "dark-content"} />

            {/* Background Gradient */}
            <LinearGradient
                colors={isDarkMode ? ['#0F172A', '#1E293B'] : [colors.background, colors.background]}
                style={StyleSheet.absoluteFill}
            />

            <ScrollView
                showsVerticalScrollIndicator={false}
                contentContainerStyle={styles.scrollContent}
            >
                <View style={styles.header}>
                    <Text style={[styles.title, { color: colors.textDark }]}>{STRINGS.reports.title}</Text>
                    <BlurView intensity={20} tint={isDarkMode ? "dark" : "light"} style={[styles.monthSelector, { borderColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.5)' }]}>
                        <TouchableOpacity onPress={() => setSelectedMonth(subMonths(selectedMonth, 1))}>
                            <Ionicons name="chevron-back" size={20} color={colors.textDark} />
                        </TouchableOpacity>
                        <Text style={[styles.subtitle, { color: colors.textDark }]}>{format(selectedMonth, STRINGS.reports.monthSelector).toUpperCase()}</Text>
                        <TouchableOpacity onPress={() => setSelectedMonth(new Date())}>
                            <Ionicons name="refresh" size={20} color={colors.textDark} />
                        </TouchableOpacity>
                    </BlurView>
                </View>

                {/* AI Insights Card */}
                {aiError ? (
                    <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.aiCard, { borderColor: 'rgba(239, 68, 68, 0.2)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
                        <View style={styles.aiHeader}>
                            <View style={[styles.aiIcon, { backgroundColor: 'rgba(239, 68, 68, 0.1)' }]}>
                                <MaterialCommunityIcons name="alert-circle-outline" size={24} color="#EF4444" />
                            </View>
                            <View style={styles.aiTitleContainer}>
                                <Text style={[styles.aiTitle, { color: colors.textDark }]}>{STRINGS.common.error}</Text>
                                <Text style={[styles.aiLabel, { color: colors.textGrey }]}>{STRINGS.reports.aiInsightTitle}</Text>
                            </View>
                        </View>
                        <Text style={[styles.aiText, { color: colors.textDark }]}>{aiError}</Text>
                    </BlurView>
                ) : aiInsight && (
                    <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.aiCard, { borderColor: aiInsight.type === 'warning' ? 'rgba(239, 68, 68, 0.2)' : 'rgba(59, 130, 246, 0.2)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
                        <View style={styles.aiHeader}>
                            <View style={[styles.aiIcon, { backgroundColor: aiInsight.type === 'warning' ? 'rgba(239, 68, 68, 0.1)' : 'rgba(59, 130, 246, 0.1)' }]}>
                                <MaterialCommunityIcons
                                    name={aiInsight.type === 'warning' ? "alert-decagram" : "auto-fix"}
                                    size={24}
                                    color={aiInsight.type === 'warning' ? '#EF4444' : '#3B82F6'}
                                />
                            </View>
                            <View style={styles.aiTitleContainer}>
                                <Text style={[styles.aiTitle, { color: colors.textDark }]}>{aiInsight.title}</Text>
                                <Text style={[styles.aiLabel, { color: colors.textGrey }]}>{STRINGS.reports.aiInsightTitle} {isAiLoading && STRINGS.reports.aiRefreshing}</Text>
                            </View>
                        </View>
                        <Text style={[styles.aiText, { color: colors.textDark }]}>{aiInsight.insight}</Text>
                    </BlurView>
                )}

                {/* Summary Section */}
                <View style={styles.summaryRow}>
                    <BlurView intensity={15} tint={isDarkMode ? "dark" : "light"} style={[styles.summaryCard, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
                        <LinearGradient
                            colors={['rgba(239, 68, 68, 0.2)', 'rgba(239, 68, 68, 0.05)']}
                            style={styles.cardGradient}
                        >
                            <Text style={[styles.summaryLabel, { color: colors.textGrey }]}>{STRINGS.reports.totalSpending}</Text>
                            <Text style={[styles.summaryValue, { color: '#EF4444' }]}>{currencySymbol} {currentExpense.toLocaleString()}</Text>
                            <View style={styles.diffRow}>
                                <Ionicons name={currentExpense > prevExpense ? "arrow-up" : "arrow-down"} size={12} color={currentExpense > prevExpense ? "#EF4444" : "#10B981"} />
                                <Text style={[styles.diffText, { color: currentExpense > prevExpense ? "#EF4444" : "#10B981" }]}>
                                    {Math.abs(currentExpense - prevExpense).toLocaleString()} Δ
                                </Text>
                            </View>
                        </LinearGradient>
                    </BlurView>

                    <BlurView intensity={15} tint={isDarkMode ? "dark" : "light"} style={[styles.summaryCard, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
                        <LinearGradient
                            colors={['rgba(16, 185, 129, 0.2)', 'rgba(16, 185, 129, 0.05)']}
                            style={styles.cardGradient}
                        >
                            <Text style={[styles.summaryLabel, { color: colors.textGrey }]}>{STRINGS.reports.totalIncome}</Text>
                            <Text style={[styles.summaryValue, { color: '#10B981' }]}>{currencySymbol} {currentIncome.toLocaleString()}</Text>
                            <View style={styles.diffRow}>
                                <Ionicons name={currentIncome >= prevIncome ? "arrow-up" : "arrow-down"} size={12} color={currentIncome >= prevIncome ? "#10B981" : "#EF4444"} />
                                <Text style={[styles.diffText, { color: currentIncome >= prevIncome ? "#10B981" : "#EF4444" }]}>
                                    {Math.abs(currentIncome - prevIncome).toLocaleString()} Δ
                                </Text>
                            </View>
                        </LinearGradient>
                    </BlurView>
                </View>

                {/* Trend Section */}
                <View style={[styles.sectionHeader, { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }]}>
                    <View>
                        <Text style={[styles.sectionTitle, { color: colors.textDark }]}>{STRINGS.reports.financialTrend}</Text>
                        <Text style={{ fontSize: 10, color: colors.textGrey, fontWeight: '700', marginTop: 4 }}>{STRINGS.reports.historyAnalysis}</Text>
                    </View>
                    <View style={styles.periodFilter}>
                        {(['1M', '6M', '1Y'] as const).map(p => (
                            <TouchableOpacity
                                key={p}
                                onPress={() => setTrendPeriod(p)}
                                style={[styles.periodBtn, trendPeriod === p && styles.periodBtnActive]}
                            >
                                <Text style={[styles.periodText, trendPeriod === p && styles.periodTextActive]}>{p}</Text>
                            </TouchableOpacity>
                        ))}
                    </View>
                </View>

                <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.chartWrapper, { marginBottom: 32, borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
                    <LineChart
                        data={trendData}
                        width={width - 50}
                        height={240}
                        yAxisLabel={currencySymbol}
                        chartConfig={{
                            backgroundColor: 'transparent',
                            backgroundGradientFrom: isDarkMode ? '#0F172A' : '#FFFFFF',
                            backgroundGradientTo: isDarkMode ? '#1E293B' : '#FFFFFF',
                            decimalPlaces: 0,
                            color: (opacity = 1) => `rgba(59, 130, 246, ${opacity})`,
                            labelColor: (opacity = 1) => colors.textGrey,
                            propsForDots: { r: "4", strokeWidth: "2", stroke: "#3B82F6" },
                            style: { borderRadius: 16 },
                        }}
                        bezier
                        style={{ marginVertical: 8, borderRadius: 16 }}
                        fromZero
                    />
                </BlurView>

                {/* Allocation Section */}
                <View style={[styles.sectionHeader, { marginTop: 32 }]}>
                    <Text style={[styles.sectionTitle, { color: colors.textDark }]}>{STRINGS.reports.budgetTracking}</Text>
                </View>

                <View style={styles.budgetList}>
                    {budgetData.length > 0 ? budgetData.map((item, index) => (
                        <BlurView key={index} intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.budgetItem, { borderColor: isDarkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
                            <View style={styles.budgetHeader}>
                                <View style={styles.budgetRow}>
                                    <View style={[styles.dot, { backgroundColor: item.color }]} />
                                    <Text style={[styles.budgetCategory, { color: colors.textDark }]}>{item.name}</Text>
                                </View>
                                <Text style={[styles.budgetText, { color: colors.textGrey }]}>
                                    {currencySymbol} {item.actual} / {currencySymbol} {item.budget}
                                </Text>
                            </View>
                            <View style={styles.progressContainer}>
                                <View style={[styles.progressBarBg, { backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' }]}>
                                    <View
                                        style={[
                                            styles.progressBarFill,
                                            {
                                                width: `${item.percentage}%`,
                                                backgroundColor: item.percentage > 90 ? '#EF4444' : item.color
                                            }
                                        ]}
                                    />
                                </View>
                                <Text style={[styles.percentageText, { color: item.percentage > 90 ? '#EF4444' : colors.textGrey }]}>
                                    {Math.round(item.percentage)}%
                                </Text>
                            </View>
                        </BlurView>
                    )) : (
                        <BlurView intensity={5} tint={isDarkMode ? "dark" : "light"} style={[styles.emptyCard, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
                            <Ionicons name="leaf-outline" size={40} color={isDarkMode ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)"} />
                            <Text style={[styles.emptyText, { color: colors.textGrey }]}>{STRINGS.reports.noBudgets}</Text>
                        </BlurView>
                    )}
                </View>

                <View style={{ height: 120 }} />
            </ScrollView>
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    scrollContent: {
        paddingTop: Platform.OS === 'ios' ? 70 : 50,
    },
    header: {
        paddingHorizontal: 20,
        marginBottom: 32,
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    title: {
        fontSize: 24,
        fontWeight: '900',
        letterSpacing: -0.5,
    },
    monthSelector: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 8,
        paddingHorizontal: 16,
        borderRadius: 20,
        borderWidth: 1,
        overflow: 'hidden',
    },
    subtitle: {
        fontSize: 12,
        fontWeight: '800',
        marginHorizontal: 12,
        letterSpacing: 1,
    },
    summaryRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        paddingHorizontal: 20,
        marginBottom: 32,
    },
    summaryCard: {
        width: '48%',
        borderRadius: 24,
        overflow: 'hidden',
        borderWidth: 1,
    },
    cardGradient: {
        padding: 16,
        flex: 1,
    },
    summaryLabel: {
        fontSize: 9,
        fontWeight: '900',
        letterSpacing: 1,
        marginBottom: 8,
    },
    summaryValue: {
        fontSize: 20,
        fontWeight: '900',
        marginBottom: 8,
    },
    diffRow: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    diffText: {
        fontSize: 10,
        fontWeight: '700',
        marginLeft: 4,
    },
    sectionHeader: {
        paddingHorizontal: 20,
        marginBottom: 16,
    },
    sectionTitle: {
        fontSize: 18,
        fontWeight: '800',
    },
    chartWrapper: {
        marginHorizontal: 20,
        padding: 16,
        borderRadius: 32,
        borderWidth: 1,
        overflow: 'hidden',
    },
    budgetList: {
        paddingHorizontal: 20,
    },
    budgetItem: {
        borderRadius: 24,
        padding: 16,
        marginBottom: 12,
        borderWidth: 1,
        overflow: 'hidden',
    },
    budgetHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 12,
    },
    budgetRow: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    dot: {
        width: 8,
        height: 8,
        borderRadius: 4,
        marginRight: 8,
    },
    budgetCategory: {
        fontSize: 14,
        fontWeight: '700',
    },
    budgetText: {
        fontSize: 12,
        fontWeight: '600',
    },
    progressContainer: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    progressBarBg: {
        flex: 1,
        height: 6,
        borderRadius: 3,
        overflow: 'hidden',
        marginRight: 12,
    },
    progressBarFill: {
        height: '100%',
        borderRadius: 3,
    },
    percentageText: {
        fontSize: 12,
        fontWeight: '800',
        width: 40,
        textAlign: 'right',
    },
    emptyCard: {
        padding: 40,
        alignItems: 'center',
        borderRadius: 32,
        borderWidth: 1,
        overflow: 'hidden',
    },
    emptyText: {
        fontSize: 14,
        fontWeight: '600',
        marginTop: 12,
        textAlign: 'center',
    },
    aiCard: {
        marginHorizontal: 20,
        borderRadius: 28,
        padding: 24,
        marginBottom: 24,
        borderWidth: 1,
        overflow: 'hidden',
    },
    aiHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 16,
    },
    aiIcon: {
        width: 48,
        height: 48,
        borderRadius: 16,
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 16,
    },
    aiTitleContainer: {
        flex: 1,
    },
    aiTitle: {
        fontSize: 18,
        fontWeight: '900',
        letterSpacing: -0.5,
    },
    aiLabel: {
        fontSize: 9,
        fontWeight: '900',
        letterSpacing: 1,
        marginTop: 2,
    },
    aiText: {
        fontSize: 15,
        lineHeight: 22,
        fontWeight: '600',
        opacity: 0.8,
    },
    periodFilter: {
        flexDirection: 'row',
        backgroundColor: 'rgba(0,0,0,0.05)',
        borderRadius: 12,
        padding: 4,
    },
    periodBtn: {
        paddingVertical: 6,
        paddingHorizontal: 10,
        borderRadius: 8,
    },
    periodBtnActive: {
        backgroundColor: '#3B82F6',
    },
    periodText: {
        fontSize: 10,
        fontWeight: '800',
        color: '#64748B',
    },
    periodTextActive: {
        color: 'white',
    },
});
