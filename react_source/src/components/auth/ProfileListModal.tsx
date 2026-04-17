/**
 * ProfileListModal Component
 * Displays a list of profiles (staff or family members) for selection
 */

import React from 'react';
import {
    View,
    Text,
    TouchableOpacity,
    Modal,
    StyleSheet,
    FlatList,
    TouchableWithoutFeedback,
    Dimensions,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { BlurView } from 'expo-blur';
import { accessibleButton } from '../../lib/accessibility';

const { height } = Dimensions.get('window');

interface Profile {
    id: string;
    name: string;
    role?: string;
    pin?: string;
}

interface ProfileListModalProps {
    visible: boolean;
    title: string;
    profiles: Profile[];
    isDarkMode: boolean;
    theme: {
        textPrimary: string;
        textSecondary: string;
        textMuted: string;
        cardBg: string;
        cardBorder: string;
        accent: string;
    };
    onSelect: (id: string) => void;
    onClose: () => void;
}

export const ProfileListModal: React.FC<ProfileListModalProps> = ({
    visible,
    title,
    profiles,
    isDarkMode,
    theme,
    onSelect,
    onClose,
}) => {
    const styles = StyleSheet.create({
        overlay: {
            flex: 1,
            backgroundColor: 'rgba(0, 0, 0, 0.6)',
            justifyContent: 'flex-end',
        },
        container: {
            maxHeight: height * 0.7,
            borderTopLeftRadius: 24,
            borderTopRightRadius: 24,
            overflow: 'hidden',
            borderWidth: 1,
            borderColor: theme.cardBorder,
            borderBottomWidth: 0,
        },
        content: {
            paddingBottom: 34, // Safe area
        },
        header: {
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: 20,
            borderBottomWidth: 1,
            borderBottomColor: theme.cardBorder,
        },
        title: {
            fontSize: 18,
            fontWeight: '800',
            color: theme.textPrimary,
        },
        closeButton: {
            width: 36,
            height: 36,
            borderRadius: 18,
            backgroundColor: `${theme.textMuted}20`,
            justifyContent: 'center',
            alignItems: 'center',
        },
        listContent: {
            padding: 16,
        },
        profileItem: {
            flexDirection: 'row',
            alignItems: 'center',
            padding: 16,
            backgroundColor: `${theme.textMuted}08`,
            borderRadius: 16,
            marginBottom: 12,
            borderWidth: 1,
            borderColor: theme.cardBorder,
        },
        avatar: {
            width: 48,
            height: 48,
            borderRadius: 24,
            backgroundColor: `${theme.accent}15`,
            justifyContent: 'center',
            alignItems: 'center',
            marginRight: 14,
        },
        avatarText: {
            fontSize: 18,
            fontWeight: '800',
            color: theme.accent,
        },
        profileInfo: {
            flex: 1,
        },
        profileName: {
            fontSize: 16,
            fontWeight: '700',
            color: theme.textPrimary,
            marginBottom: 2,
        },
        profileRole: {
            fontSize: 13,
            color: theme.textSecondary,
        },
        pinBadge: {
            flexDirection: 'row',
            alignItems: 'center',
            backgroundColor: `${theme.accent}15`,
            paddingHorizontal: 10,
            paddingVertical: 4,
            borderRadius: 8,
        },
        pinBadgeText: {
            fontSize: 11,
            fontWeight: '700',
            color: theme.accent,
            marginLeft: 4,
        },
        emptyText: {
            textAlign: 'center',
            color: theme.textSecondary,
            fontSize: 14,
            padding: 24,
        },
    });

    const renderProfile = ({ item }: { item: Profile }) => (
        <TouchableOpacity
            style={styles.profileItem}
            onPress={() => onSelect(item.id)}
            {...accessibleButton(`Select ${item.name}`, item.role)}
        >
            <View style={styles.avatar}>
                <Text style={styles.avatarText}>
                    {item.name.charAt(0).toUpperCase()}
                </Text>
            </View>
            <View style={styles.profileInfo}>
                <Text style={styles.profileName}>{item.name}</Text>
                {item.role && <Text style={styles.profileRole}>{item.role}</Text>}
            </View>
            {item.pin && (
                <View style={styles.pinBadge}>
                    <Ionicons name="lock-closed" size={12} color={theme.accent} />
                    <Text style={styles.pinBadgeText}>PIN</Text>
                </View>
            )}
        </TouchableOpacity>
    );

    return (
        <Modal
            visible={visible}
            transparent
            animationType="slide"
            onRequestClose={onClose}
        >
            <TouchableWithoutFeedback onPress={onClose}>
                <View style={styles.overlay}>
                    <TouchableWithoutFeedback>
                        <BlurView
                            intensity={isDarkMode ? 60 : 80}
                            tint={isDarkMode ? 'dark' : 'light'}
                            style={styles.container}
                        >
                            <View style={styles.content}>
                                <View style={styles.header}>
                                    <Text style={styles.title}>{title}</Text>
                                    <TouchableOpacity
                                        style={styles.closeButton}
                                        onPress={onClose}
                                        {...accessibleButton('Close', 'Close profile list')}
                                    >
                                        <Ionicons name="close" size={20} color={theme.textPrimary} />
                                    </TouchableOpacity>
                                </View>

                                {profiles.length > 0 ? (
                                    <FlatList
                                        data={profiles}
                                        renderItem={renderProfile}
                                        keyExtractor={(item) => item.id}
                                        contentContainerStyle={styles.listContent}
                                        showsVerticalScrollIndicator={false}
                                    />
                                ) : (
                                    <Text style={styles.emptyText}>
                                        No profiles available
                                    </Text>
                                )}
                            </View>
                        </BlurView>
                    </TouchableWithoutFeedback>
                </View>
            </TouchableWithoutFeedback>
        </Modal>
    );
};
