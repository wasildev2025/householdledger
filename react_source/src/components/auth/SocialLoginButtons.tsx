/**
 * SocialLoginButtons Component
 * Renders social authentication buttons (Google, Apple)
 */

import React from 'react';
import { View, TouchableOpacity, Text, StyleSheet, Image } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { accessibleButton } from '../../lib/accessibility';

interface SocialLoginButtonsProps {
    onGooglePress: () => void;
    onApplePress: () => void;
    theme: {
        inputBg: string;
        inputBorder: string;
        textPrimary: string;
    };
}

export const SocialLoginButtons: React.FC<SocialLoginButtonsProps> = ({
    onGooglePress,
    onApplePress,
    theme,
}) => {
    const styles = StyleSheet.create({
        container: {
            flexDirection: 'row',
            gap: 12,
        },
        socialButton: {
            flex: 1,
            height: 56,
            borderRadius: 16,
            backgroundColor: theme.inputBg,
            borderWidth: 1.5,
            borderColor: theme.inputBorder,
            flexDirection: 'row',
            alignItems: 'center',
            justifyContent: 'center',
        },
        socialButtonText: {
            marginLeft: 10,
            fontSize: 15,
            fontWeight: '600',
            color: theme.textPrimary,
        },
    });

    return (
        <View style={styles.container}>
            <TouchableOpacity
                style={styles.socialButton}
                onPress={onGooglePress}
                {...accessibleButton('Sign in with Google')}
            >
                <Ionicons name="logo-google" size={20} color="#EA4335" />
                <Text style={styles.socialButtonText}>Google</Text>
            </TouchableOpacity>
            <TouchableOpacity
                style={styles.socialButton}
                onPress={onApplePress}
                {...accessibleButton('Sign in with Apple')}
            >
                <Ionicons name="logo-apple" size={20} color={theme.textPrimary} />
                <Text style={styles.socialButtonText}>Apple</Text>
            </TouchableOpacity>
        </View>
    );
};
