/**
 * PinModal Component
 * Handles PIN input with rate limiting display
 */

import React, { useRef, useEffect } from 'react';
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    Modal,
    StyleSheet,
    TouchableWithoutFeedback,
    ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { BlurView } from 'expo-blur';
import { accessibleInput, accessibleButton } from '../../lib/accessibility';

interface PinModalProps {
    visible: boolean;
    pin: string;
    loading?: boolean;
    profileName: string;
    attemptsRemaining?: number;
    lockoutSeconds?: number;
    isDarkMode: boolean;
    theme: {
        textPrimary: string;
        textSecondary: string;
        textMuted: string;
        cardBg: string;
        cardBorder: string;
        inputBg: string;
        inputBorder: string;
        accent: string;
    };
    onPinChange: (text: string) => void;
    onSubmit: () => void;
    onClose: () => void;
    biometricAuthEnabled?: boolean;
    onBiometricPress?: () => void;
}

export const PinModal: React.FC<PinModalProps> = ({
    visible,
    pin,
    loading = false,
    profileName,
    attemptsRemaining,
    lockoutSeconds,
    isDarkMode,
    theme,
    onPinChange,
    onSubmit,
    onClose,
    biometricAuthEnabled = false,
    onBiometricPress,
}) => {
    const pinInputRef = useRef<TextInput>(null);

    useEffect(() => {
        if (visible) {
            setTimeout(() => pinInputRef.current?.focus(), 100);
        }
    }, [visible]);

    const isLocked = lockoutSeconds !== undefined && lockoutSeconds > 0;

    const styles = StyleSheet.create({
        overlay: {
            flex: 1,
            backgroundColor: 'rgba(0, 0, 0, 0.6)',
            justifyContent: 'center',
            alignItems: 'center',
            padding: 24,
        },
        container: {
            width: '100%',
            maxWidth: 340,
            borderRadius: 24,
            overflow: 'hidden',
            borderWidth: 1,
            borderColor: theme.cardBorder,
        },
        content: {
            padding: 28,
            alignItems: 'center',
        },
        iconContainer: {
            width: 64,
            height: 64,
            borderRadius: 32,
            backgroundColor: `${theme.accent}15`,
            justifyContent: 'center',
            alignItems: 'center',
            marginBottom: 20,
        },
        title: {
            fontSize: 20,
            fontWeight: '800',
            color: theme.textPrimary,
            marginBottom: 8,
        },
        subtitle: {
            fontSize: 14,
            color: theme.textSecondary,
            textAlign: 'center',
            marginBottom: 24,
            lineHeight: 20,
        },
        profileName: {
            fontWeight: '700',
            color: theme.accent,
        },
        pinInputContainer: {
            flexDirection: 'row',
            justifyContent: 'center',
            marginBottom: 24,
        },
        pinDot: {
            width: 48,
            height: 48,
            borderRadius: 12,
            borderWidth: 2,
            borderColor: theme.inputBorder,
            backgroundColor: theme.inputBg,
            marginHorizontal: 6,
            justifyContent: 'center',
            alignItems: 'center',
        },
        pinDotFilled: {
            backgroundColor: theme.accent,
            borderColor: theme.accent,
        },
        hiddenInput: {
            position: 'absolute',
            opacity: 0,
            height: 1,
            width: 1,
        },
        warningText: {
            fontSize: 12,
            color: '#EF4444',
            textAlign: 'center',
            marginBottom: 16,
        },
        lockoutText: {
            fontSize: 14,
            color: '#EF4444',
            fontWeight: '700',
            textAlign: 'center',
            marginBottom: 16,
        },
        buttonRow: {
            flexDirection: 'row',
            gap: 12,
        },
        biometricButton: {
            width: 48,
            height: 48,
            borderRadius: 12,
            justifyContent: 'center',
            alignItems: 'center',
            backgroundColor: `${theme.accent}15`,
            borderWidth: 1,
            borderColor: `${theme.accent}30`,
        },
        cancelButton: {
            flex: 1,
            height: 48,
            borderRadius: 12,
            justifyContent: 'center',
            alignItems: 'center',
            backgroundColor: `${theme.textMuted}20`,
        },
        cancelButtonText: {
            color: theme.textSecondary,
            fontSize: 15,
            fontWeight: '600',
        },
        submitButton: {
            flex: 1,
            height: 48,
            borderRadius: 12,
            justifyContent: 'center',
            alignItems: 'center',
            backgroundColor: theme.accent,
        },
        submitButtonDisabled: {
            opacity: 0.5,
        },
        submitButtonText: {
            color: '#FFFFFF',
            fontSize: 15,
            fontWeight: '700',
        },
    });

    return (
        <Modal
            visible={visible}
            transparent
            animationType="fade"
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
                                <View style={styles.iconContainer}>
                                    <Ionicons name="keypad" size={28} color={theme.accent} />
                                </View>

                                <Text style={styles.title}>Enter PIN</Text>
                                <Text style={styles.subtitle}>
                                    Enter PIN for{' '}
                                    <Text style={styles.profileName}>{profileName}</Text>
                                </Text>

                                {/* Hidden Input for PIN */}
                                <TextInput
                                    ref={pinInputRef}
                                    style={styles.hiddenInput}
                                    value={pin}
                                    onChangeText={(text) => {
                                        if (text.length <= 4 && /^\d*$/.test(text)) {
                                            onPinChange(text);
                                        }
                                    }}
                                    keyboardType="number-pad"
                                    maxLength={4}
                                    {...accessibleInput('PIN input', 'Enter your 4-digit PIN')}
                                />

                                {/* PIN Dots Display */}
                                <TouchableOpacity
                                    style={styles.pinInputContainer}
                                    onPress={() => pinInputRef.current?.focus()}
                                    activeOpacity={1}
                                    {...accessibleButton('PIN entry field', 'Tap to enter PIN')}
                                >
                                    {[0, 1, 2, 3].map((index) => (
                                        <View
                                            key={index}
                                            style={[
                                                styles.pinDot,
                                                pin.length > index && styles.pinDotFilled,
                                            ]}
                                        >
                                            {pin.length > index && (
                                                <View
                                                    style={{
                                                        width: 12,
                                                        height: 12,
                                                        borderRadius: 6,
                                                        backgroundColor: '#FFFFFF',
                                                    }}
                                                />
                                            )}
                                        </View>
                                    ))}
                                </TouchableOpacity>

                                {/* Attempts Warning */}
                                {attemptsRemaining !== undefined && attemptsRemaining < 5 && !isLocked && (
                                    <Text style={styles.warningText}>
                                        {attemptsRemaining} attempts remaining
                                    </Text>
                                )}

                                {/* Lockout Warning */}
                                {isLocked && (
                                    <Text style={styles.lockoutText}>
                                        Locked for {lockoutSeconds} seconds
                                    </Text>
                                )}

                                {/* Buttons */}
                                <View style={styles.buttonRow}>
                                    <TouchableOpacity
                                        style={styles.cancelButton}
                                        onPress={onClose}
                                        {...accessibleButton('Cancel', 'Close PIN entry')}
                                    >
                                        <Text style={styles.cancelButtonText}>Cancel</Text>
                                    </TouchableOpacity>

                                    {biometricAuthEnabled && onBiometricPress && (
                                        <TouchableOpacity
                                            style={styles.biometricButton}
                                            onPress={onBiometricPress}
                                            {...accessibleButton('Biometric Login', 'Login using FaceID or Fingerprint')}
                                        >
                                            <Ionicons name="finger-print" size={24} color={theme.accent} />
                                        </TouchableOpacity>
                                    )}

                                    <TouchableOpacity
                                        style={[
                                            styles.submitButton,
                                            (pin.length < 4 || isLocked || loading) && styles.submitButtonDisabled,
                                        ]}
                                        onPress={onSubmit}
                                        disabled={pin.length < 4 || isLocked || loading}
                                        {...accessibleButton('Confirm PIN')}
                                    >
                                        {loading ? (
                                            <ActivityIndicator color="#FFFFFF" size="small" />
                                        ) : (
                                            <Text style={styles.submitButtonText}>Confirm</Text>
                                        )}
                                    </TouchableOpacity>
                                </View>
                            </View>
                        </BlurView>
                    </TouchableWithoutFeedback>
                </View>
            </TouchableWithoutFeedback>
        </Modal>
    );
};
