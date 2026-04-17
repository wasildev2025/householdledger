import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';
import { MaterialCommunityIcons, Ionicons } from '@expo/vector-icons';
import { format } from 'date-fns';
import { FilterType } from '../hooks/useHouseholdMetrics';
import { useTheme } from '../../../hooks/useTheme';

interface AdminWalletCardProps {
    filter: FilterType;
    netBalance: number;
    displayIncome: number;
    totalSpentReal: number;
    currencySymbol: string;
    currentDate: Date;
}

export function AdminWalletCard({ filter, netBalance, displayIncome, totalSpentReal, currencySymbol, currentDate }: AdminWalletCardProps) {
    const { isDarkMode } = useTheme();

    return (
        <LinearGradient
            colors={isDarkMode ? ['#1E293B', '#0F172A', '#020617'] : ['#2563EB', '#3B82F6', '#60A5FA']}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.mainBalanceCard}
        >
            <View style={[styles.cardOrb, { top: -20, right: -10, width: 140, height: 140, backgroundColor: 'rgba(255,255,255,0.12)', opacity: 0.6 }]} />
            <View style={[styles.cardOrb, { top: 40, left: -20, width: 100, height: 100, backgroundColor: isDarkMode ? 'rgba(59, 130, 246, 0.2)' : 'rgba(255,255,255,0.1)', opacity: 0.5 }]} />

            <View style={styles.cardHeader}>
                <View>
                    <Text style={[styles.balanceLabel, { color: 'rgba(255,255,255,0.85)', letterSpacing: 2 }]}>
                        {filter === 'all' ? 'NET HOUSEHOLD BALANCE' : filter === 'servant' ? 'STAFF CONTROL' : 'MY SPENDING'}
                    </Text>
                </View>
                <BlurView intensity={25} tint="light" style={styles.chipIconContainer}>
                    <MaterialCommunityIcons name="integrated-circuit-chip" size={22} color="rgba(255,255,255,0.95)" />
                </BlurView>
            </View>

            <View style={styles.balanceRow}>
                <View>
                    {netBalance < 0 && (
                        <View style={[styles.negativeBadge, { alignSelf: 'flex-start', marginBottom: 12, backgroundColor: 'rgba(255, 255, 255, 0.15)', borderColor: 'rgba(255,255,255,0.3)' }]}>
                            <View style={[styles.negativeDot, { backgroundColor: '#FF8A8A' }]} />
                            <Text style={[styles.negativeText, { color: 'white' }]}>OVERSPENT</Text>
                        </View>
                    )}
                    <Text style={[styles.balanceAmount, { color: 'white', textShadowColor: 'rgba(0, 0, 0, 0.1)', textShadowOffset: { width: 0, height: 2 }, textShadowRadius: 4 }]}>
                        {netBalance < 0 ? '-' : ''}{currencySymbol} {Math.abs(netBalance).toLocaleString()}
                    </Text>
                    <Text style={{ color: 'rgba(255,255,255,0.7)', fontSize: 12, fontWeight: '700', marginTop: 4, letterSpacing: 0.5 }}>
                        {format(currentDate, 'MMMM yyyy').toUpperCase()}
                    </Text>
                </View>
            </View>

            <View style={styles.statsRow}>
                <BlurView intensity={20} tint="light" style={styles.statItemSubCard}>
                    <View style={[styles.iconBox, { backgroundColor: 'rgba(255,255,255,0.2)' }]}>
                        <Ionicons name="arrow-down" size={12} color="#4ADE80" />
                    </View>
                    <View>
                        <Text style={[styles.statLabel, { color: 'rgba(255,255,255,0.7)' }]}>Income</Text>
                        <Text style={[styles.statValue, { color: 'white', fontSize: 13 }]}>{currencySymbol} {displayIncome.toLocaleString()}</Text>
                    </View>
                </BlurView>

                <View style={{ width: 10 }} />

                <BlurView intensity={20} tint="light" style={styles.statItemSubCard}>
                    <View style={[styles.iconBox, { backgroundColor: 'rgba(255,255,255,0.2)' }]}>
                        <Ionicons name="arrow-up" size={12} color="#FCA5A5" />
                    </View>
                    <View>
                        <Text style={[styles.statLabel, { color: 'rgba(255,255,255,0.7)' }]}>Expense</Text>
                        <Text style={[styles.statValue, { color: 'white', fontSize: 13 }]}>{currencySymbol} {totalSpentReal.toLocaleString()}</Text>
                    </View>
                </BlurView>
            </View>
        </LinearGradient>
    );
}

const styles = StyleSheet.create({
    mainBalanceCard: {
        borderRadius: 24,
        padding: 16,
        shadowColor: "#1e3c72",
        shadowOffset: { width: 0, height: 8 },
        shadowOpacity: 0.25,
        shadowRadius: 15,
        elevation: 10,
        position: 'relative',
        overflow: 'hidden',
    },
    cardOrb: {
        position: 'absolute',
        borderRadius: 100,
    },
    chipIconContainer: {
        padding: 6,
        borderRadius: 10,
        overflow: 'hidden',
        borderWidth: 1,
        borderColor: 'rgba(255,255,255,0.25)',
    },
    cardHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: 16,
    },
    balanceLabel: {
        fontSize: 8,
        fontWeight: '900',
        textTransform: 'uppercase',
    },
    balanceRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 24,
        justifyContent: 'space-between',
    },
    balanceAmount: {
        fontSize: 36,
        fontWeight: '900',
        letterSpacing: -1,
    },
    negativeBadge: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 10,
        borderWidth: 1,
    },
    negativeDot: {
        width: 5,
        height: 5,
        borderRadius: 2.5,
        marginRight: 5,
    },
    negativeText: {
        fontSize: 8,
        fontWeight: '900',
        letterSpacing: 1,
    },
    statsRow: {
        flexDirection: 'row',
        justifyContent: 'space-between',
    },
    statItemSubCard: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        padding: 10,
        borderRadius: 14,
        overflow: 'hidden',
        borderWidth: 1,
        borderColor: 'rgba(255,255,255,0.15)',
    },
    iconBox: {
        width: 28,
        height: 28,
        borderRadius: 8,
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 10,
    },
    statLabel: {
        fontSize: 9,
        fontWeight: '800',
        marginBottom: 2,
        textTransform: 'uppercase',
        letterSpacing: 0.4,
    },
    statValue: {
        fontSize: 12,
        fontWeight: '900',
    },
});
