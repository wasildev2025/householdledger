import React from 'react';
import { View, Text } from 'react-native';
import { BlurView } from 'expo-blur';
import { useTheme } from '../../../hooks/useTheme';

interface SalaryTrackerCardProps {
    remainingSalary: number;
    displayIncome: number;
    totalSpentReal: number;
    staffExtraSpent: number;
    staffTransfers: number;
    totalExtraSpent: number;
    currencySymbol: string;
}

export function SalaryTrackerCard({
    remainingSalary,
    displayIncome,
    totalSpentReal,
    staffExtraSpent,
    staffTransfers,
    totalExtraSpent,
    currencySymbol
}: SalaryTrackerCardProps) {
    const { colors, isDarkMode } = useTheme();

    return (
        <View style={{ marginHorizontal: 20, marginBottom: 24 }}>
            <BlurView
                intensity={20}
                tint={isDarkMode ? "dark" : "light"}
                style={{
                    borderRadius: 20,
                    padding: 20,
                    overflow: 'hidden',
                    backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'white',
                    borderWidth: 1,
                    borderColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)'
                }}
            >
                <Text style={{ color: colors.textGrey, fontSize: 12, fontWeight: '800', letterSpacing: 1, marginBottom: 12 }}>MONTHLY SALARY TRACKER</Text>

                <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
                    <View>
                        <Text style={{ color: colors.textDark, fontSize: 28, fontWeight: '900' }}>
                            {currencySymbol} {remainingSalary.toLocaleString()}
                        </Text>
                        <Text style={{ color: colors.textGrey, fontSize: 12, fontWeight: '600' }}>Remaining Salary</Text>
                    </View>
                    <View style={{ alignItems: 'flex-end' }}>
                        <Text style={{ color: colors.textDark, fontSize: 16, fontWeight: '700' }}>
                            {currencySymbol} {displayIncome.toLocaleString()}
                        </Text>
                        <Text style={{ color: colors.textGrey, fontSize: 10, fontWeight: '600' }}>Total Income</Text>
                    </View>
                </View>

                <View style={{ height: 8, backgroundColor: isDarkMode ? 'rgba(255,255,255,0.1)' : '#F1F5F9', borderRadius: 4, overflow: 'hidden' }}>
                    <View style={{
                        height: '100%',
                        width: `${Math.min(((totalSpentReal + staffExtraSpent) / (displayIncome || 1)) * 100, 100)}%`,
                        backgroundColor: remainingSalary < 0 ? '#EF4444' : '#3B82F6',
                        borderRadius: 4
                    }} />
                </View>

                <View style={{ flexDirection: 'row', justifyContent: 'space-between', marginTop: 12 }}>
                    <View style={{ alignItems: 'flex-start' }}>
                        <Text style={{ color: colors.textGrey, fontSize: 10, marginBottom: 2 }}>Transfers/Staff</Text>
                        <Text style={{ color: '#60A5FA', fontSize: 12, fontWeight: '700' }}>
                            {currencySymbol} {staffTransfers.toLocaleString()}
                        </Text>
                    </View>
                    {totalExtraSpent > 0 && (
                        <View style={{ alignItems: 'center' }}>
                            <Text style={{ color: '#EF4444', fontSize: 10, fontWeight: '800', marginBottom: 2 }}>OVERDUE (EXTRA)</Text>
                            <Text style={{ color: '#EF4444', fontSize: 12, fontWeight: '900' }}>
                                {currencySymbol} {totalExtraSpent.toLocaleString()}
                            </Text>
                        </View>
                    )}
                    <View style={{ alignItems: 'flex-end' }}>
                        <Text style={{ color: colors.textGrey, fontSize: 10, marginBottom: 2 }}>Total Spent</Text>
                        <Text style={{ color: colors.textDark, fontSize: 14, fontWeight: '900' }}>
                            {currencySymbol} {totalSpentReal.toLocaleString()}
                        </Text>
                    </View>
                </View>
            </BlurView>
        </View>
    );
}
