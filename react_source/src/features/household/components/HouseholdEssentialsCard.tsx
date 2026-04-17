import React from 'react';
import { View, Text, TouchableOpacity } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { MaterialCommunityIcons, Ionicons } from '@expo/vector-icons';
import { format, parseISO, isWithinInterval, startOfMonth, endOfMonth } from 'date-fns';
import { SHADOWS } from '../../../constants/theme';
import { useTheme } from '../../../hooks/useTheme';

interface HouseholdEssentialsCardProps {
    dairyLogs: any[];
    currencySymbol: string;
    onPress: () => void;
}

export function HouseholdEssentialsCard({ dairyLogs, currencySymbol, onPress }: HouseholdEssentialsCardProps) {
    const { colors, isDarkMode } = useTheme();

    const currentMonthBill = (dairyLogs || [])
        .filter(l => isWithinInterval(parseISO(l.date), { start: startOfMonth(new Date()), end: endOfMonth(new Date()) }))
        .reduce((sum, l) => sum + l.totalBill, 0);

    return (
        <View style={{ marginHorizontal: 20, marginBottom: 24 }}>
            <Text style={{ color: colors.textGrey, fontSize: 12, fontWeight: '800', letterSpacing: 1, marginBottom: 12 }}>HOUSEHOLD ESSENTIALS</Text>
            <TouchableOpacity onPress={onPress} activeOpacity={0.9}>
                <LinearGradient
                    colors={isDarkMode ? ['#1E293B', '#0F172A'] : ['#FFFFFF', '#F8FAFC']}
                    style={{
                        borderRadius: 24,
                        padding: 20,
                        borderWidth: 1,
                        borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)',
                        ...SHADOWS.premium,
                        overflow: 'hidden'
                    }}
                >
                    <MaterialCommunityIcons
                        name="cow"
                        size={80}
                        color={isDarkMode ? 'rgba(255,255,255,0.03)' : 'rgba(59, 130, 246, 0.03)'}
                        style={{ position: 'absolute', right: -10, bottom: -10 }}
                    />

                    <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' }}>
                        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                            <View style={{
                                width: 44,
                                height: 44,
                                borderRadius: 14,
                                backgroundColor: isDarkMode ? 'rgba(59, 130, 246, 0.1)' : '#EFF6FF',
                                justifyContent: 'center',
                                alignItems: 'center',
                                marginRight: 12
                            }}>
                                <MaterialCommunityIcons name="bottle-wine-outline" size={24} color="#3B82F6" />
                            </View>
                            <View>
                                <Text style={{ fontSize: 16, fontWeight: '900', color: colors.textDark }}>Daily Dairy Tracker</Text>
                                <Text style={{ fontSize: 12, fontWeight: '600', color: colors.textGrey }}>Milk & Yogurt Inventory</Text>
                            </View>
                        </View>
                        <Ionicons name="chevron-forward" size={20} color={colors.textGrey} />
                    </View>

                    <View style={{ height: 1, backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', marginVertical: 16 }} />

                    <View style={{ flexDirection: 'row', justifyContent: 'space-between' }}>
                        <View>
                            <Text style={{ fontSize: 10, fontWeight: '800', color: colors.textGrey, letterSpacing: 1, textTransform: 'uppercase' }}>This Month Bill</Text>
                            <Text style={{ fontSize: 20, fontWeight: '900', color: '#10B981', marginTop: 2 }}>
                                {currencySymbol} {currentMonthBill.toLocaleString()}
                            </Text>
                        </View>
                        <View style={{ alignItems: 'flex-end' }}>
                            <Text style={{ fontSize: 10, fontWeight: '800', color: colors.textGrey, letterSpacing: 1, textTransform: 'uppercase' }}>Last Logged</Text>
                            <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 4 }}>
                                <Ionicons name="time-outline" size={14} color={colors.textGrey} style={{ marginRight: 4 }} />
                                <Text style={{ fontSize: 13, fontWeight: '700', color: colors.textDark }}>
                                    {(dairyLogs || []).length > 0
                                        ? format(parseISO(dairyLogs[0].date), 'MMM dd')
                                        : 'No Entry'}
                                </Text>
                            </View>
                        </View>
                    </View>
                </LinearGradient>
            </TouchableOpacity>
        </View>
    );
}
