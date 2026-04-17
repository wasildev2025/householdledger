import React from 'react';
import { View, Text, TouchableOpacity, Modal, TouchableWithoutFeedback, StyleSheet, Dimensions } from 'react-native';
import { BlurView } from 'expo-blur';
import { Ionicons } from '@expo/vector-icons';
import { COLORS } from '../../../constants/theme';
import { useTheme } from '../../../hooks/useTheme';

const { width } = Dimensions.get('window');

interface ProfileMenuModalProps {
    visible: boolean;
    displayName: string;
    role: string | undefined;
    onClose: () => void;
    onSettingsPress: () => void;
    onLogoutPress: () => void;
}

export function ProfileMenuModal({ visible, displayName, role, onClose, onSettingsPress, onLogoutPress }: ProfileMenuModalProps) {
    const { colors, isDarkMode } = useTheme();

    return (
        <Modal
            visible={visible}
            transparent={true}
            animationType="fade"
            onRequestClose={onClose}
        >
            <TouchableWithoutFeedback onPress={onClose}>
                <View style={styles.modalOverlay}>
                    <TouchableWithoutFeedback>
                        <BlurView intensity={90} tint={isDarkMode ? "dark" : "light"} style={[styles.menuContainer, { borderColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)' }]}>
                            <View style={styles.menuHeader}>
                                <View style={[styles.menuAvatarLarge, { borderColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.1)', backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.02)' }]}>
                                    <Text style={[styles.menuAvatarText, { color: colors.textDark }]}>
                                        {displayName.charAt(0).toUpperCase()}
                                    </Text>
                                </View>
                                <Text style={[styles.menuUserName, { color: colors.textDark }]}>{displayName}</Text>
                                <Text style={[styles.menuUserRole, { color: colors.textGrey }]}>
                                    {role === 'admin' ? 'OWNER' : role === 'servant' ? 'STAFF' : 'FAMILY'}
                                </Text>
                            </View>
                            <TouchableOpacity style={[styles.menuItem, { borderBottomColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' }]} onPress={onSettingsPress}>
                                <Ionicons name="settings-outline" size={22} color={colors.textDark} />
                                <Text style={[styles.menuItemText, { color: colors.textDark }]}>Settings</Text>
                            </TouchableOpacity>
                            <TouchableOpacity style={styles.menuItem} onPress={onLogoutPress}>
                                <Ionicons name="log-out-outline" size={22} color={COLORS.danger} />
                                <Text style={[styles.menuItemText, { color: COLORS.danger }]}>Logout</Text>
                            </TouchableOpacity>
                        </BlurView>
                    </TouchableWithoutFeedback>
                </View>
            </TouchableWithoutFeedback>
        </Modal>
    );
}

const styles = StyleSheet.create({
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.6)',
        justifyContent: 'center',
        alignItems: 'center',
    },
    menuContainer: {
        width: width * 0.85,
        borderRadius: 40,
        padding: 30,
        overflow: 'hidden',
        borderWidth: 1,
    },
    menuHeader: {
        alignItems: 'center',
        marginBottom: 32,
    },
    menuAvatarLarge: {
        width: 80,
        height: 80,
        borderRadius: 40,
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 16,
        borderWidth: 1,
    },
    menuAvatarText: {
        fontSize: 28,
        fontWeight: '800',
    },
    menuUserName: {
        fontSize: 22,
        fontWeight: '800',
    },
    menuUserRole: {
        fontSize: 11,
        letterSpacing: 2,
        marginTop: 6,
        fontWeight: '800',
    },
    menuItem: {
        flexDirection: 'row',
        alignItems: 'center',
        paddingVertical: 16,
        borderBottomWidth: 1,
    },
    menuItemText: {
        fontSize: 15,
        fontWeight: '600',
        marginLeft: 15,
    },
});
