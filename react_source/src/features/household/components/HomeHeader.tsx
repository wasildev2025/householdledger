import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Household } from '../../../types';
import { useTheme } from '../../../hooks/useTheme';

interface HomeHeaderProps {
    household: Household | null;
    unreadCount: number;
    isOffline: boolean;
    onNotificationsPress: () => void;
}

export function HomeHeader({ household, unreadCount, isOffline, onNotificationsPress }: HomeHeaderProps) {
    const { colors, isDarkMode } = useTheme();

    return (
        <View style={styles.headerContent}>
            <View style={styles.householdInfo}>
                <View style={[styles.avatarContainer, { backgroundColor: isDarkMode ? '#334155' : '#E2E8F0' }]}>
                    <Text style={[styles.avatarText, { color: colors.textDark }]}>
                        {household?.name?.charAt(0) || 'H'}
                    </Text>
                </View>
                <View>
                    <Text style={[styles.welcomeLabel, { color: colors.textGrey }]}>Member of</Text>
                    <Text style={[styles.houseNameText, { color: colors.textDark }]}>
                        {household?.name || 'Household'}
                    </Text>
                </View>
            </View>
            <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                {isOffline && (
                    <View style={styles.offlineBadge}>
                        <Ionicons name="cloud-offline-outline" size={14} color="#EF4444" style={{ marginRight: 4 }} />
                        <Text style={styles.offlineText}>Offline</Text>
                    </View>
                )}
                <TouchableOpacity
                    style={[styles.actionBtn, { backgroundColor: isDarkMode ? '#334155' : 'white' }]}
                    onPress={onNotificationsPress}
                >
                    <Ionicons name="notifications-outline" size={24} color={colors.textDark} />
                    {unreadCount > 0 && <View style={styles.badge} />}
                </TouchableOpacity>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    headerContent: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 24,
    },
    householdInfo: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    avatarContainer: {
        width: 48,
        height: 48,
        borderRadius: 16,
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: 12,
        shadowColor: "#000",
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.1,
        shadowRadius: 12,
        elevation: 3,
    },
    avatarText: {
        fontSize: 20,
        fontWeight: '900',
    },
    welcomeLabel: {
        fontSize: 12,
        fontWeight: '600',
        marginBottom: 2,
    },
    houseNameText: {
        fontSize: 20,
        fontWeight: '900',
        letterSpacing: -0.5,
    },
    actionBtn: {
        width: 48,
        height: 48,
        borderRadius: 16,
        justifyContent: 'center',
        alignItems: 'center',
        shadowColor: "#000",
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.1,
        shadowRadius: 12,
        elevation: 3,
    },
    badge: {
        position: 'absolute',
        top: 12,
        right: 12,
        width: 10,
        height: 10,
        borderRadius: 5,
        backgroundColor: '#EF4444',
        borderWidth: 2,
        borderColor: '#fff',
    },
    offlineBadge: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'rgba(239, 68, 68, 0.1)',
        paddingHorizontal: 8,
        paddingVertical: 5,
        borderRadius: 12,
        marginRight: 10,
        borderWidth: 1,
        borderColor: 'rgba(239, 68, 68, 0.05)',
    },
    offlineText: {
        fontSize: 10,
        fontWeight: '800',
        color: '#EF4444',
        letterSpacing: 0.5,
    }
});
