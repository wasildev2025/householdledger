import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import { FilterType } from '../hooks/useHouseholdMetrics';
import { useTheme } from '../../../hooks/useTheme';

interface MemberWalletCardProps {
    person: {
        id: string;
        name: string;
        balance: number;
        allocation: number;
        utilization: number;
        isOverspent: boolean;
    };
    filter: FilterType;
    currencySymbol: string;
    onRefillPress: (personId: string, filter: FilterType) => void;
}

export function MemberWalletCard({ person, filter, currencySymbol, onRefillPress }: MemberWalletCardProps) {
    const { isDarkMode } = useTheme();

    return (
        <LinearGradient
            colors={person.isOverspent ? ['#EF4444', '#B91C1C'] : (isDarkMode ? ['#1E293B', '#0F172A', '#020617'] : ['#2563EB', '#3B82F6', '#60A5FA'])}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={[styles.mainBalanceCard, { marginBottom: 10, height: 210 }]}
        >
            <View style={styles.cardHeader}>
                <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                    <BlurView intensity={25} tint="light" style={{ width: 34, height: 34, borderRadius: 10, justifyContent: 'center', alignItems: 'center', marginRight: 10 }}>
                        <Ionicons name={filter === 'servant' ? "person" : "people"} size={18} color="white" />
                    </BlurView>
                    <Text style={[styles.balanceLabel, { color: 'rgba(255,255,255,0.9)', letterSpacing: 1 }]}>
                        {person.name.toUpperCase()} CONTROL
                    </Text>
                </View>
                <TouchableOpacity
                    onPress={() => onRefillPress(person.id, filter)}
                    style={{ backgroundColor: 'rgba(255,255,255,0.2)', paddingHorizontal: 12, paddingVertical: 6, borderRadius: 12, borderWidth: 1, borderColor: 'rgba(255,255,255,0.3)', flexDirection: 'row', alignItems: 'center' }}
                >
                    <Text style={{ color: 'white', fontSize: 10, fontWeight: '900', marginRight: 4 }}>{person.isOverspent ? 'REFILL NEEDED' : 'REFILL'}</Text>
                    <Ionicons name="arrow-forward" size={10} color="white" />
                </TouchableOpacity>
            </View>

            <View style={styles.balanceRow}>
                <View>
                    <Text style={[styles.balanceAmount, { color: 'white', fontSize: 28, textShadowColor: 'rgba(0, 0, 0, 0.1)', textShadowOffset: { width: 0, height: 2 }, textShadowRadius: 4 }]}>
                        {person.balance < 0 ? '-' : ''}{currencySymbol} {Math.abs(person.balance).toLocaleString()}
                    </Text>
                    <Text style={{ color: 'rgba(255,255,255,0.7)', fontSize: 10, fontWeight: '700', marginTop: 4, letterSpacing: 0.5 }}>
                        HANDOVER BALANCE
                    </Text>
                </View>
            </View>

            <View style={styles.statsRow}>
                <BlurView intensity={20} tint="light" style={styles.statItemSubCard}>
                    <View style={[styles.iconBox, { backgroundColor: 'rgba(255,255,255,0.2)' }]}>
                        <Ionicons name="arrow-down" size={12} color="#4ADE80" />
                    </View>
                    <View>
                        <Text style={[styles.statLabel, { color: 'rgba(255,255,255,0.7)' }]}>Given</Text>
                        <Text style={[styles.statValue, { color: 'white', fontSize: 13 }]}>{currencySymbol} {person.allocation.toLocaleString()}</Text>
                    </View>
                </BlurView>

                <View style={{ width: 10 }} />

                <BlurView intensity={20} tint="light" style={styles.statItemSubCard}>
                    <View style={[styles.iconBox, { backgroundColor: 'rgba(255,255,255,0.2)' }]}>
                        <Ionicons name="arrow-up" size={12} color="#FCA5A5" />
                    </View>
                    <View>
                        <Text style={[styles.statLabel, { color: 'rgba(255,255,255,0.7)' }]}>Spent</Text>
                        <Text style={[styles.statValue, { color: 'white', fontSize: 13 }]}>{currencySymbol} {person.utilization.toLocaleString()}</Text>
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
