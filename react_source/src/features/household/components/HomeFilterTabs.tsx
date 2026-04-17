import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { useTheme } from '../../../hooks/useTheme';
import { FilterType } from '../hooks/useHouseholdMetrics';

interface HomeFilterTabsProps {
    filter: FilterType;
    setFilter: (filter: FilterType) => void;
}

export function HomeFilterTabs({ filter, setFilter }: HomeFilterTabsProps) {
    const { colors, isDarkMode } = useTheme();

    return (
        <View style={[styles.filterSegment, {
            backgroundColor: isDarkMode ? 'rgba(30, 41, 59, 0.8)' : 'rgba(255, 255, 255, 0.8)',
            borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)'
        }]}>
            {(['all', 'admin', 'servant', 'member'] as FilterType[]).map((f) => {
                const isActive = filter === f;
                let label = 'Household';
                if (f === 'admin') label = 'My Expenses';
                if (f === 'servant') label = 'Staff';
                if (f === 'member') label = 'Family';

                return (
                    <TouchableOpacity
                        key={f}
                        style={[styles.segmentBtn, isActive && styles.segmentBtnActive]}
                        onPress={() => setFilter(f)}
                    >
                        {isActive && <View style={[styles.activeIndicator, { backgroundColor: isDarkMode ? '#3B82F6' : '#2563EB' }]} />}
                        <Text style={[
                            styles.segmentText,
                            isActive && styles.segmentTextActive,
                            { color: isActive ? '#fff' : colors.textGrey }
                        ]}>
                            {label}
                        </Text>
                    </TouchableOpacity>
                );
            })}
        </View>
    );
}

const styles = StyleSheet.create({
    filterSegment: {
        flexDirection: 'row',
        padding: 6,
        borderRadius: 20,
        borderWidth: 1,
    },
    segmentBtn: {
        flex: 1,
        paddingVertical: 10,
        alignItems: 'center',
        justifyContent: 'center',
        borderRadius: 14,
        overflow: 'hidden',
    },
    segmentBtnActive: {},
    activeIndicator: {
        ...StyleSheet.absoluteFillObject,
        borderRadius: 14,
        shadowColor: "#3B82F6",
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.3,
        shadowRadius: 8,
        elevation: 4,
    },
    segmentText: {
        fontSize: 12,
        fontWeight: '600',
        zIndex: 1,
    },
    segmentTextActive: {
        fontWeight: '800',
    },
});
