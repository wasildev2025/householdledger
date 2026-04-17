import React, { useState, useLayoutEffect, useCallback, useEffect, useRef } from 'react';
import { View, ScrollView, StyleSheet, TouchableOpacity, Dimensions, Platform, StatusBar, RefreshControl, Text } from 'react-native';
import { CompositeNavigationProp, useNavigation } from '@react-navigation/native';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { StackNavigationProp } from '@react-navigation/stack';
import { RootStackParamList, TabParamList } from '../types';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';
import { useTheme } from '../hooks/useTheme';
import { aiService } from '../lib/aiService';
import { secureLog } from '../lib/securityUtils';
import { SHADOWS } from '../constants/theme';

// Stores
import { useAuthStore } from '../features/auth/store';
import { useHouseholdStore } from '../features/household/store';
import { useTransactionStore } from '../features/transactions/store';
import { useMessagesStore } from '../features/messaging/store';
import { useRequestsStore } from '../features/requests/store';
import { useDairyStore } from '../features/dairy/store';
import { useOfflineStore } from '../shared/store/offline';
import { useSettingsStore } from '../features/settings/store';
import { useAiInsightStore } from '../features/ai/store';

// Components & Hooks
import { useHouseholdMetrics, FilterType, TimeRange } from '../features/household/hooks/useHouseholdMetrics';
import { HomeHeader } from '../features/household/components/HomeHeader';
import { HomeFilterTabs } from '../features/household/components/HomeFilterTabs';
import { AdminWalletCard } from '../features/household/components/AdminWalletCard';
import { MemberWalletCard } from '../features/household/components/MemberWalletCard';
import { AIInsightCard } from '../features/household/components/AIInsightCard';
import { SalaryTrackerCard } from '../features/household/components/SalaryTrackerCard';
import { HouseholdEssentialsCard } from '../features/household/components/HouseholdEssentialsCard';
import { RecentTransactionsList } from '../features/household/components/RecentTransactionsList';
import { HomeChart } from '../features/household/components/HomeChart';
import { ProfileMenuModal } from '../features/household/components/ProfileMenuModal';

type HomeScreenNavigationProp = CompositeNavigationProp<
    BottomTabNavigationProp<TabParamList, 'Home'>,
    StackNavigationProp<RootStackParamList>
>;

export default function HomeScreen() {
    const { colors, isDarkMode } = useTheme();
    const navigation = useNavigation<HomeScreenNavigationProp>();

    // Auth & Household State
    const { profile, signOut } = useAuthStore();
    const { household } = useHouseholdStore();
    const { currencySymbol } = useSettingsStore();
    const { isOffline } = useOfflineStore();

    // Domain Data
    const { fetchPage: fetchTransactions } = useTransactionStore();
    const { messages } = useMessagesStore();
    const { requests } = useRequestsStore();
    const { dairyLogs } = useDairyStore();

    const [refreshing, setRefreshing] = useState(false);
    const [timeRange, setTimeRange] = useState<TimeRange>('month');
    const [filter, setFilter] = useState<FilterType>('all');
    const [currentDate] = useState(new Date());
    const [isProfileMenuVisible, setIsProfileMenuVisible] = useState(false);

    // Mock last notification check (should be moved to settings store eventually)
    const [lastNotificationCheck, setLastNotificationCheck] = useState<string | null>(null);

    const onRefresh = useCallback(async () => {
        setRefreshing(true);
        try {
            await fetchTransactions(true);
            // In a real app, dispatch fetches to other stores too
        } finally {
            setRefreshing(false);
        }
    }, [fetchTransactions]);

    useLayoutEffect(() => {
        navigation.setOptions({ headerShown: false });
    }, [navigation]);

    // Smart unread notification count
    const unreadCount = React.useMemo(() => {
        const lastCheck = lastNotificationCheck ? new Date(lastNotificationCheck) : new Date(0);
        const unreadMessages = messages.filter((m: any) => new Date(m.createdAt || m.created_at) > lastCheck).length;
        const unreadRequests = requests.filter((r: any) => new Date(r.createdAt || r.created_at) > lastCheck).length;
        return unreadMessages + unreadRequests;
    }, [messages.length, requests.length, lastNotificationCheck]);

    // Derived Metrics Hook
    const metrics = useHouseholdMetrics(timeRange, filter, currentDate);

    // AI Insight Fetching via shared store
    const { insight: aiInsight, fetchInsight } = useAiInsightStore();

    // Stable signature so we only run the effect when the data conceptually changes
    const txSignature = React.useMemo(() =>
        `${metrics.filteredTransactions.length}-${metrics.filteredTransactions.reduce((acc, t) => acc + t.amount, 0).toFixed(0)}`,
        [metrics.filteredTransactions]
    );

    useEffect(() => {
        const timer = setTimeout(() => {
            fetchInsight(metrics.filteredTransactions, metrics.categories);
        }, 1500); // Small debounce
        return () => clearTimeout(timer);
    }, [txSignature, metrics.categories.length]);

    return (
        <View style={[styles.container, { backgroundColor: colors.background }]}>
            <StatusBar barStyle={isDarkMode ? "light-content" : "dark-content"} />

            <View style={StyleSheet.absoluteFill}>
                <LinearGradient
                    colors={isDarkMode ? ['#020617', '#0F172A', '#1E293B'] : ['#F8FAFC', '#F1F5F9', '#E2E8F0']}
                    style={StyleSheet.absoluteFill}
                />
                <View style={[styles.bgOrb, { top: -100, left: -50, backgroundColor: isDarkMode ? 'rgba(59, 130, 246, 0.05)' : 'rgba(59, 130, 246, 0.03)' }]} />
                <View style={[styles.bgOrb, { bottom: 100, right: -80, width: 400, height: 400, backgroundColor: isDarkMode ? 'rgba(168, 85, 247, 0.04)' : 'rgba(168, 85, 247, 0.02)' }]} />
            </View>

            <ScrollView
                showsVerticalScrollIndicator={false}
                contentContainerStyle={styles.scrollContent}
                refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={isDarkMode ? "#ffffff" : "#000000"} />}
            >
                <View style={styles.headerContainer}>
                    <HomeHeader
                        household={household}
                        unreadCount={unreadCount}
                        isOffline={isOffline}
                        onNotificationsPress={() => {
                            setLastNotificationCheck(new Date().toISOString());
                            navigation.navigate('Messages');
                        }}
                    />

                    {!metrics.isServant && (
                        <HomeFilterTabs filter={filter} setFilter={setFilter} />
                    )}
                </View>

                <View style={styles.balanceSection}>
                    {!metrics.isServant && !metrics.isMember && (filter === 'servant' || filter === 'member') ? (
                        <View style={{ marginBottom: 12 }}>
                            <Text style={{ color: colors.textGrey, fontSize: 12, fontWeight: '800', letterSpacing: 1, marginBottom: 12, marginLeft: 4 }}>
                                {filter === 'servant' ? 'STAFF WALLET MONITOR' : 'FAMILY WALLET MONITOR'}
                            </Text>
                            {(filter === 'servant' ? metrics.staffStats : metrics.memberStats).map((person) => (
                                <MemberWalletCard
                                    key={person.id}
                                    person={person}
                                    filter={filter}
                                    currencySymbol={currencySymbol}
                                    onRefillPress={(id, f) => navigation.navigate('AddTransaction', {
                                        type: 'transfer',
                                        servantId: f === 'servant' ? id : undefined,
                                        memberId: f === 'member' ? id : undefined
                                    })}
                                />
                            ))}
                        </View>
                    ) : (
                        <AdminWalletCard
                            filter={filter}
                            netBalance={metrics.netBalance}
                            displayIncome={metrics.displayIncome}
                            totalSpentReal={metrics.totalSpentReal}
                            currencySymbol={currencySymbol}
                            currentDate={currentDate}
                        />
                    )}
                </View>

                <AIInsightCard insight={aiInsight} />

                {!metrics.isServant && !metrics.isMember && (
                    <SalaryTrackerCard
                        remainingSalary={metrics.remainingSalary}
                        displayIncome={metrics.displayIncome}
                        totalSpentReal={metrics.totalSpentReal}
                        staffExtraSpent={metrics.staffExtraSpent}
                        staffTransfers={metrics.staffTransfers}
                        totalExtraSpent={metrics.totalExtraSpent}
                        currencySymbol={currencySymbol}
                    />
                )}

                <HouseholdEssentialsCard
                    dairyLogs={dairyLogs}
                    currencySymbol={currencySymbol}
                    onPress={() => navigation.navigate('DairyTracker')}
                />

                <RecentTransactionsList
                    transactions={metrics.filteredTransactions}
                    categories={metrics.categories}
                    currencySymbol={currencySymbol}
                    onSeeAllPress={() => navigation.navigate('Transactions')}
                />

                <HomeChart
                    transactions={metrics.filteredTransactions}
                    categories={metrics.categories}
                />

                <View style={{ height: 120 }} />
            </ScrollView>

            <TouchableOpacity style={styles.avatarFab} onPress={() => setIsProfileMenuVisible(true)}>
                <BlurView intensity={40} tint={isDarkMode ? "dark" : "light"} style={[styles.avatarFabBlur, { borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.1)', backgroundColor: isDarkMode ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0,0,0,0.03)' }]}>
                    <Text style={[styles.avatarFabText, { color: colors.textDark }]}>
                        {profile?.name?.charAt(0).toUpperCase() || 'U'}
                    </Text>
                </BlurView>
            </TouchableOpacity>

            <ProfileMenuModal
                visible={isProfileMenuVisible}
                displayName={profile?.name || 'User'}
                role={profile?.role}
                onClose={() => setIsProfileMenuVisible(false)}
                onSettingsPress={() => { setIsProfileMenuVisible(false); navigation.navigate('Settings' as any); }}
                onLogoutPress={signOut}
            />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
    },
    bgOrb: {
        position: 'absolute',
        width: 300,
        height: 300,
        borderRadius: 150,
    },
    scrollContent: {
        paddingTop: Platform.OS === 'ios' ? 60 : 40,
        paddingBottom: 40,
    },
    headerContainer: {
        paddingHorizontal: 20,
        marginBottom: 16,
    },
    balanceSection: {
        paddingHorizontal: 20,
        marginBottom: 32,
    },
    avatarFab: {
        position: 'absolute',
        bottom: 30,
        left: 20,
        width: 50,
        height: 50,
        borderRadius: 25,
        overflow: 'hidden',
        ...SHADOWS.medium,
    },
    avatarFabBlur: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    avatarFabText: {
        fontSize: 18,
        fontWeight: '800',
    },
});
