/**
 * ProfileSelector Component
 * Displays profile selection buttons for Admin, Staff, and Family login
 */

import React from 'react';
import {
    View,
    Text,
    TouchableOpacity,
    StyleSheet,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { LinearGradient } from 'expo-linear-gradient';
import { accessibleButton } from '../../lib/accessibility';

interface ProfileSelectorProps {
    adminName: string;
    hasServants: boolean;
    hasMembers: boolean;
    isDarkMode: boolean;
    theme: {
        textPrimary: string;
        textSecondary: string;
        textMuted: string;
        cardBg: string;
        cardBorder: string;
        accent: string;
    };
    onAdminLogin: () => void;
    onStaffPress: () => void;
    onFamilyPress: () => void;
}

export const ProfileSelector: React.FC<ProfileSelectorProps> = ({
    adminName,
    hasServants,
    hasMembers,
    isDarkMode,
    theme,
    onAdminLogin,
    onStaffPress,
    onFamilyPress,
}) => {
    const styles = StyleSheet.create({
        container: {
            marginTop: 16,
        },
        divider: {
            flexDirection: 'row',
            alignItems: 'center',
            marginVertical: 24,
        },
        dividerLine: {
            flex: 1,
            height: 1,
            backgroundColor: theme.cardBorder,
        },
        dividerText: {
            color: theme.textMuted,
            fontSize: 13,
            fontWeight: '600',
            marginHorizontal: 16,
        },
        profileButton: {
            flexDirection: 'row',
            alignItems: 'center',
            padding: 16,
            borderRadius: 16,
            marginBottom: 12,
            backgroundColor: `${theme.textMuted}08`,
            borderWidth: 1,
            borderColor: theme.cardBorder,
        },
        adminButton: {
            borderRadius: 16,
            marginBottom: 12,
            overflow: 'hidden',
        },
        adminButtonContent: {
            flexDirection: 'row',
            alignItems: 'center',
            padding: 16,
        },
        iconContainer: {
            width: 44,
            height: 44,
            borderRadius: 14,
            justifyContent: 'center',
            alignItems: 'center',
            marginRight: 14,
        },
        adminIconContainer: {
            backgroundColor: 'rgba(255, 255, 255, 0.2)',
        },
        staffIconContainer: {
            backgroundColor: `${theme.accent}15`,
        },
        familyIconContainer: {
            backgroundColor: `${theme.accent}15`,
        },
        profileInfo: {
            flex: 1,
        },
        profileLabel: {
            fontSize: 12,
            fontWeight: '600',
            color: theme.textSecondary,
            marginBottom: 2,
        },
        adminProfileLabel: {
            color: 'rgba(255, 255, 255, 0.7)',
        },
        profileName: {
            fontSize: 16,
            fontWeight: '800',
            color: theme.textPrimary,
        },
        adminProfileName: {
            color: '#FFFFFF',
        },
        chevron: {
            opacity: 0.5,
        },
        disabledButton: {
            opacity: 0.5,
        },
        disabledText: {
            fontStyle: 'italic',
        },
    });

    return (
        <View style={styles.container}>
            {/* Divider */}
            <View style={styles.divider}>
                <View style={styles.dividerLine} />
                <Text style={styles.dividerText}>or continue as</Text>
                <View style={styles.dividerLine} />
            </View>

            {/* Admin Button */}
            <TouchableOpacity
                onPress={onAdminLogin}
                style={styles.adminButton}
                {...accessibleButton(`Login as ${adminName}`, 'Login as household administrator')}
            >
                <LinearGradient
                    colors={isDarkMode ? ['#3B82F6', '#1D4ED8'] : ['#2563EB', '#1D4ED8']}
                    start={{ x: 0, y: 0 }}
                    end={{ x: 1, y: 1 }}
                    style={styles.adminButtonContent}
                >
                    <View style={[styles.iconContainer, styles.adminIconContainer]}>
                        <Ionicons name="shield-checkmark" size={22} color="#FFFFFF" />
                    </View>
                    <View style={styles.profileInfo}>
                        <Text style={[styles.profileLabel, styles.adminProfileLabel]}>
                            HOUSEHOLD OWNER
                        </Text>
                        <Text style={[styles.profileName, styles.adminProfileName]}>
                            {adminName}
                        </Text>
                    </View>
                    <Ionicons
                        name="chevron-forward"
                        size={20}
                        color="rgba(255, 255, 255, 0.5)"
                    />
                </LinearGradient>
            </TouchableOpacity>

            {/* Staff Button */}
            <TouchableOpacity
                style={[
                    styles.profileButton,
                    !hasServants && styles.disabledButton,
                ]}
                onPress={onStaffPress}
                disabled={!hasServants}
                {...accessibleButton(
                    'Login as Staff',
                    hasServants ? 'View staff profiles' : 'No staff profiles available'
                )}
            >
                <View style={[styles.iconContainer, styles.staffIconContainer]}>
                    <Ionicons name="people" size={22} color={theme.accent} />
                </View>
                <View style={styles.profileInfo}>
                    <Text style={styles.profileLabel}>HOUSEHOLD STAFF</Text>
                    <Text style={[
                        styles.profileName,
                        !hasServants && styles.disabledText,
                    ]}>
                        {hasServants ? 'Select Staff Profile' : 'No Staff Added'}
                    </Text>
                </View>
                <Ionicons
                    name="chevron-forward"
                    size={20}
                    color={theme.textMuted}
                    style={styles.chevron}
                />
            </TouchableOpacity>

            {/* Family Button */}
            <TouchableOpacity
                style={[
                    styles.profileButton,
                    !hasMembers && styles.disabledButton,
                ]}
                onPress={onFamilyPress}
                disabled={!hasMembers}
                {...accessibleButton(
                    'Login as Family Member',
                    hasMembers ? 'View family member profiles' : 'No family members available'
                )}
            >
                <View style={[styles.iconContainer, styles.familyIconContainer]}>
                    <Ionicons name="home" size={22} color={theme.accent} />
                </View>
                <View style={styles.profileInfo}>
                    <Text style={styles.profileLabel}>FAMILY MEMBER</Text>
                    <Text style={[
                        styles.profileName,
                        !hasMembers && styles.disabledText,
                    ]}>
                        {hasMembers ? 'Select Family Member' : 'No Members Added'}
                    </Text>
                </View>
                <Ionicons
                    name="chevron-forward"
                    size={20}
                    color={theme.textMuted}
                    style={styles.chevron}
                />
            </TouchableOpacity>
        </View>
    );
};
