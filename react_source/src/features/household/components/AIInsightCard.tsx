import React from 'react';
import { View, Text } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import { useTheme } from '../../../hooks/useTheme';

interface AIInsightCardProps {
    insight: {
        title: string;
        insight: string;
        type: 'warning' | 'tip' | 'positive';
    } | null;
}

export function AIInsightCard({ insight }: AIInsightCardProps) {
    const { colors, isDarkMode } = useTheme();

    if (!insight) return null;

    return (
        <View style={{ marginHorizontal: 20, marginBottom: 20 }}>
            <BlurView
                intensity={isDarkMode ? 30 : 60}
                tint={isDarkMode ? "dark" : "light"}
                style={{
                    borderRadius: 24,
                    padding: 16,
                    overflow: 'hidden',
                    borderWidth: 1,
                    borderColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)',
                    backgroundColor: 'transparent'
                }}
            >
                <View style={{ flexDirection: 'row', alignItems: 'flex-start' }}>
                    <LinearGradient
                        colors={insight.type === 'tip' ? ['#60A5FA', '#3B82F6'] : insight.type === 'positive' ? ['#34D399', '#10B981'] : ['#FBBF24', '#F59E0B']}
                        style={{ width: 36, height: 36, borderRadius: 10, justifyContent: 'center', alignItems: 'center', marginRight: 12, marginTop: 2 }}
                    >
                        <Ionicons
                            name={insight.type === 'tip' ? "bulb" : insight.type === 'positive' ? "rocket" : "alert-circle"}
                            size={18}
                            color="white"
                        />
                    </LinearGradient>
                    <View style={{ flex: 1 }}>
                        <Text style={{ fontSize: 8, fontWeight: '900', color: colors.textGrey, letterSpacing: 1.2, textTransform: 'uppercase', marginBottom: 2 }}>GEN AI ANALYSIS</Text>
                        <Text style={{ fontSize: 14, fontWeight: '900', color: colors.textDark, letterSpacing: -0.4, marginBottom: 4 }}>{insight.title}</Text>
                        <Text style={{ fontSize: 13, lineHeight: 18, color: colors.textGrey, fontWeight: '600' }}>
                            {insight.insight}
                        </Text>
                    </View>
                </View>
            </BlurView>
        </View>
    );
}
