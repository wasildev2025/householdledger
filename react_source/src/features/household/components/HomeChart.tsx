import React, { useMemo } from 'react';
import { View, Text, StyleSheet, Dimensions } from 'react-native';
import { PieChart } from 'react-native-chart-kit';
import { useTheme } from '../../../hooks/useTheme';
import { Transaction } from '../../transactions/store';
import { Category } from '../../categories/store';

const { width } = Dimensions.get('window');

interface HomeChartProps {
    transactions: Transaction[];
    categories: Category[];
}

export function HomeChart({ transactions, categories }: HomeChartProps) {
    const { colors, isDarkMode } = useTheme();

    const chartData = useMemo(() => {
        return categories.map((c) => {
            const amount = transactions
                .filter((t) => t.type === 'expense' && t.categoryId === c.id)
                .reduce((sum, t) => sum + t.amount, 0);
            return {
                name: c.name,
                population: amount,
                color: c.color,
                legendFontColor: isDarkMode ? 'rgba(255,255,255,0.7)' : 'rgba(0,0,0,0.7)',
                legendFontSize: 12
            };
        }).filter(c => c.population > 0).sort((a, b) => b.population - a.population);
    }, [transactions, categories, isDarkMode]);

    if (chartData.length === 0) return null;

    return (
        <View style={styles.insightSection}>
            <Text style={[styles.sectionTitle, { color: colors.textDark }]}>Breakdown</Text>
            <View style={[styles.chartCard, { backgroundColor: isDarkMode ? '#1E293B' : '#FFFFFF' }]}>
                <PieChart
                    data={chartData}
                    width={width - 40}
                    height={220}
                    chartConfig={{
                        color: (opacity = 1) => isDarkMode ? `rgba(255, 255, 255, ${opacity})` : `rgba(0, 0, 0, ${opacity})`,
                        labelColor: (opacity = 1) => isDarkMode ? `rgba(255, 255, 255, ${opacity})` : `rgba(0, 0, 0, ${opacity})`,
                    }}
                    accessor={"population"}
                    backgroundColor={"transparent"}
                    paddingLeft={"15"}
                    center={[0, 0]}
                    absolute
                />
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    insightSection: {
        marginTop: 24,
        paddingHorizontal: 20,
    },
    sectionTitle: {
        fontSize: 22,
        fontWeight: '900',
        letterSpacing: -0.5,
    },
    chartCard: {
        borderRadius: 32,
        padding: 24,
        marginTop: 16,
        alignItems: 'center',
        shadowColor: "#000",
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.05,
        shadowRadius: 12,
        elevation: 4,
    },
});
