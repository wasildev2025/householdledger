/**
 * LoginForm Component
 * Handles email/password authentication with name field for sign up
 */

import React, { useState } from 'react';
import {
    View,
    Text,
    TextInput,
    TouchableOpacity,
    StyleSheet,
    ActivityIndicator,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { STRINGS } from '../../constants/strings';
import { accessibleInput, accessibleButton } from '../../lib/accessibility';

interface LoginFormProps {
    isSignUp: boolean;
    name: string;
    email: string;
    password: string;
    loading: boolean;
    showPassword: boolean;
    theme: {
        textPrimary: string;
        textSecondary: string;
        textMuted: string;
        inputBg: string;
        inputBorder: string;
        inputFocusBorder: string;
        accent: string;
        buttonGradient: string[];
    };
    onNameChange: (text: string) => void;
    onEmailChange: (text: string) => void;
    onPasswordChange: (text: string) => void;
    onTogglePassword: () => void;
    onSubmit: () => void;
    onToggleMode: () => void;
}

export const LoginForm: React.FC<LoginFormProps> = ({
    isSignUp,
    name,
    email,
    password,
    loading,
    showPassword,
    theme,
    onNameChange,
    onEmailChange,
    onPasswordChange,
    onTogglePassword,
    onSubmit,
    onToggleMode,
}) => {
    const [focusedInput, setFocusedInput] = useState<string | null>(null);

    const styles = StyleSheet.create({
        inputContainer: {
            marginBottom: 16,
        },
        inputLabel: {
            fontSize: 13,
            fontWeight: '600',
            color: theme.textSecondary,
            marginBottom: 8,
            marginLeft: 4,
        },
        inputWrapper: {
            flexDirection: 'row',
            alignItems: 'center',
            backgroundColor: theme.inputBg,
            borderRadius: 14,
            borderWidth: 1.5,
            borderColor: theme.inputBorder,
            paddingHorizontal: 14,
            height: 52,
        },
        inputWrapperFocused: {
            borderColor: theme.inputFocusBorder,
            backgroundColor: `${theme.accent}08`,
        },
        inputIcon: {
            marginRight: 12,
        },
        input: {
            flex: 1,
            fontSize: 16,
            color: theme.textPrimary,
            height: '100%',
        },
        eyeButton: {
            padding: 8,
            marginRight: -8,
        },
        submitButton: {
            height: 54,
            borderRadius: 14,
            justifyContent: 'center',
            alignItems: 'center',
            backgroundColor: theme.accent,
            marginTop: 8,
        },
        submitButtonText: {
            color: '#FFFFFF',
            fontSize: 16,
            fontWeight: '700',
        },
        toggleText: {
            textAlign: 'center',
            color: theme.textSecondary,
            fontSize: 14,
            marginTop: 20,
        },
        toggleLink: {
            color: theme.accent,
            fontWeight: '700',
        },
    });

    return (
        <View>
            {/* Name Field (Sign Up only) */}
            {isSignUp && (
                <View style={styles.inputContainer}>
                    <Text style={styles.inputLabel}>{STRINGS.auth.fullName}</Text>
                    <View
                        style={[
                            styles.inputWrapper,
                            focusedInput === 'name' && styles.inputWrapperFocused,
                        ]}
                    >
                        <Ionicons
                            name="person-outline"
                            size={20}
                            color={focusedInput === 'name' ? theme.accent : theme.textMuted}
                            style={styles.inputIcon}
                        />
                        <TextInput
                            style={styles.input}
                            placeholder={STRINGS.auth.fullNamePlaceholder}
                            placeholderTextColor={theme.textMuted}
                            value={name}
                            onChangeText={onNameChange}
                            onFocus={() => setFocusedInput('name')}
                            onBlur={() => setFocusedInput(null)}
                            {...accessibleInput(STRINGS.auth.fullName, STRINGS.auth.fullNamePlaceholder)}
                        />
                    </View>
                </View>
            )}

            {/* Email Field */}
            <View style={styles.inputContainer}>
                <Text style={styles.inputLabel}>{STRINGS.auth.emailAddress}</Text>
                <View
                    style={[
                        styles.inputWrapper,
                        focusedInput === 'email' && styles.inputWrapperFocused,
                    ]}
                >
                    <Ionicons
                        name="mail-outline"
                        size={20}
                        color={focusedInput === 'email' ? theme.accent : theme.textMuted}
                        style={styles.inputIcon}
                    />
                    <TextInput
                        style={styles.input}
                        placeholder={STRINGS.auth.emailPlaceholder}
                        placeholderTextColor={theme.textMuted}
                        value={email}
                        onChangeText={onEmailChange}
                        autoCapitalize="none"
                        keyboardType="email-address"
                        onFocus={() => setFocusedInput('email')}
                        onBlur={() => setFocusedInput(null)}
                        {...accessibleInput(STRINGS.auth.emailAddress, STRINGS.auth.emailPlaceholder)}
                    />
                </View>
            </View>

            {/* Password Field */}
            <View style={styles.inputContainer}>
                <Text style={styles.inputLabel}>{STRINGS.auth.password}</Text>
                <View
                    style={[
                        styles.inputWrapper,
                        focusedInput === 'password' && styles.inputWrapperFocused,
                    ]}
                >
                    <Ionicons
                        name="lock-closed-outline"
                        size={20}
                        color={focusedInput === 'password' ? theme.accent : theme.textMuted}
                        style={styles.inputIcon}
                    />
                    <TextInput
                        style={styles.input}
                        placeholder={STRINGS.auth.passwordPlaceholder}
                        placeholderTextColor={theme.textMuted}
                        value={password}
                        onChangeText={onPasswordChange}
                        secureTextEntry={!showPassword}
                        onFocus={() => setFocusedInput('password')}
                        onBlur={() => setFocusedInput(null)}
                        {...accessibleInput(STRINGS.auth.password, STRINGS.auth.passwordPlaceholder)}
                    />
                    <TouchableOpacity
                        style={styles.eyeButton}
                        onPress={onTogglePassword}
                        {...accessibleButton(
                            showPassword ? STRINGS.auth.hidePassword : STRINGS.auth.showPassword,
                            STRINGS.auth.togglePasswordHint
                        )}
                    >
                        <Ionicons
                            name={showPassword ? 'eye-off-outline' : 'eye-outline'}
                            size={20}
                            color={theme.textMuted}
                        />
                    </TouchableOpacity>
                </View>
            </View>

            {/* Submit Button */}
            <TouchableOpacity
                style={styles.submitButton}
                onPress={onSubmit}
                disabled={loading}
                {...accessibleButton(isSignUp ? STRINGS.auth.createAccount : STRINGS.auth.signIn)}
            >
                {loading ? (
                    <ActivityIndicator color="#FFFFFF" />
                ) : (
                    <Text style={styles.submitButtonText}>
                        {isSignUp ? STRINGS.auth.createAccount : STRINGS.auth.signIn}
                    </Text>
                )}
            </TouchableOpacity>

            {/* Toggle Sign Up / Sign In */}
            <TouchableOpacity onPress={onToggleMode}>
                <Text style={styles.toggleText}>
                    {isSignUp ? STRINGS.auth.alreadyHaveAccount : STRINGS.auth.dontHaveAccount}
                    <Text style={styles.toggleLink}>
                        {isSignUp ? STRINGS.auth.signIn : STRINGS.auth.signUp}
                    </Text>
                </Text>
            </TouchableOpacity>
        </View>
    );
};
