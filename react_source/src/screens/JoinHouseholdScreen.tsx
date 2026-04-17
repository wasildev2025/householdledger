import React, { useState } from 'react';
import {
    View,
    Text,
    StyleSheet,
    TextInput,
    TouchableOpacity,
    Alert,
    KeyboardAvoidingView,
    Platform,
    ActivityIndicator,
    ScrollView,
    StatusBar,
    Dimensions
} from 'react-native';
import { BlurView } from 'expo-blur';
import { LinearGradient } from 'expo-linear-gradient';
import { useAuthStore } from '../features/auth/store';
import { useHouseholdStore } from '../features/household/store';
import { COLORS, SHADOWS } from '../constants/theme';
import { useTheme } from '../hooks/useTheme';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import { VALIDATION, VALIDATION_MESSAGES } from '../constants/validation';
import { handleError } from '../lib/errorHandler';
import {
    accessibleButton,
    accessibleInput,
    accessibleHeader
} from '../lib/accessibility';

const { width } = Dimensions.get('window');
type Step = 'choice' | 'join' | 'create';

export default function JoinHouseholdScreen() {
    const { colors, isDarkMode } = useTheme();
    const [step, setStep] = useState<Step>('choice');
    const [inviteCode, setInviteCode] = useState('');
    const [householdName, setHouseholdName] = useState('');
    const [loading, setLoading] = useState(false);

    const { joinHousehold, createHousehold } = useHouseholdStore();
    const { signOut: logout } = useAuthStore();

    const handleCreate = async () => {
        const trimmedName = householdName.trim();

        // Validation
        if (!trimmedName) {
            Alert.alert('Required', VALIDATION_MESSAGES.HOUSEHOLD_NAME_REQUIRED);
            return;
        }

        if (trimmedName.length < VALIDATION.MIN_HOUSEHOLD_NAME_LENGTH) {
            Alert.alert('Invalid', VALIDATION_MESSAGES.HOUSEHOLD_NAME_TOO_SHORT);
            return;
        }

        if (trimmedName.length > VALIDATION.MAX_HOUSEHOLD_NAME_LENGTH) {
            Alert.alert('Invalid', VALIDATION_MESSAGES.HOUSEHOLD_NAME_TOO_LONG);
            return;
        }

        if (!VALIDATION.HOUSEHOLD_NAME_PATTERN.test(trimmedName)) {
            Alert.alert('Invalid', VALIDATION_MESSAGES.HOUSEHOLD_NAME_INVALID_CHARS);
            return;
        }

        setLoading(true);
        try {
            await createHousehold(trimmedName);
        } catch (error) {
            handleError(error, { context: 'Creating household' });
        } finally {
            setLoading(false);
        }
    };

    const handleJoin = async () => {
        // Validation
        if (!inviteCode.trim()) {
            Alert.alert('Required', VALIDATION_MESSAGES.INVITE_CODE_REQUIRED);
            return;
        }

        if (!VALIDATION.INVITE_CODE_PATTERN.test(inviteCode)) {
            Alert.alert('Invalid', VALIDATION_MESSAGES.INVITE_CODE_INVALID);
            return;
        }

        setLoading(true);
        try {
            await joinHousehold(inviteCode);
        } catch (error) {
            handleError(error, { context: 'Joining household', customMessage: 'Invalid or expired invite code. Please check and try again.' });
        } finally {
            setLoading(false);
        }
    };

    const styles = StyleSheet.create({
        mainContainer: { flex: 1 },
        scrollContent: {
            flexGrow: 1,
            padding: 24,
            justifyContent: 'center',
            paddingTop: Platform.OS === 'ios' ? 60 : 40,
        },
        header: {
            alignItems: 'center',
            marginBottom: 40,
        },
        iconCircle: {
            width: 90,
            height: 90,
            borderRadius: 30,
            justifyContent: 'center',
            alignItems: 'center',
            borderWidth: 1,
            borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.05)',
            backgroundColor: isDarkMode ? 'rgba(255, 255, 255, 0.03)' : 'rgba(15, 23, 42, 0.03)',
        },
        title: {
            fontSize: 32,
            fontWeight: '900',
            marginTop: 24,
            letterSpacing: -1,
            color: colors.textDark,
            textAlign: 'center',
        },
        subtitle: {
            fontSize: 13,
            textAlign: 'center',
            marginTop: 12,
            lineHeight: 20,
            paddingHorizontal: 20,
            fontWeight: '600',
            color: colors.textGrey,
            letterSpacing: 0.2,
        },
        cardWrapper: {
            borderRadius: 32,
            overflow: 'hidden',
            ...SHADOWS.large,
        },
        glassCard: {
            padding: 24,
            borderRadius: 32,
            borderWidth: 1,
            borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.08)' : 'rgba(255, 255, 255, 0.6)',
            backgroundColor: isDarkMode ? 'rgba(0, 0, 0, 0.2)' : 'rgba(255, 255, 255, 0.3)',
        },
        choiceItem: {
            flexDirection: 'row',
            alignItems: 'center',
            paddingVertical: 16,
        },
        choiceIcon: {
            width: 56,
            height: 56,
            borderRadius: 16,
            justifyContent: 'center',
            alignItems: 'center',
            ...SHADOWS.small,
        },
        choiceText: {
            flex: 1,
            marginLeft: 18,
        },
        choiceTitle: {
            fontSize: 17,
            fontWeight: '800',
            color: colors.textDark,
        },
        choiceDesc: {
            fontSize: 13,
            marginTop: 4,
            color: colors.textGrey,
            fontWeight: '500',
        },
        choiceDivider: {
            height: 1,
            marginVertical: 12,
            backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)',
        },
        inputLabel: {
            fontSize: 10,
            fontWeight: '900',
            letterSpacing: 2,
            color: colors.textGrey,
            marginBottom: 12,
            marginLeft: 4,
            textTransform: 'uppercase',
        },
        keyInput: {
            borderRadius: 16,
            height: 72,
            fontSize: 32,
            fontWeight: '900',
            textAlign: 'center',
            letterSpacing: 10,
            marginBottom: 24,
            borderWidth: 1,
            backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : 'rgba(255,255,255,0.6)',
            borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)',
            color: colors.textDark,
        },
        premiumTextInput: {
            borderRadius: 16,
            height: 58,
            fontSize: 16,
            fontWeight: '600',
            paddingHorizontal: 20,
            marginBottom: 24,
            borderWidth: 1,
            backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : 'rgba(255,255,255,0.6)',
            borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)',
            color: colors.textDark,
        },
        primaryButton: {
            borderRadius: 16,
            height: 58,
            overflow: 'hidden',
            ...SHADOWS.medium,
        },
        buttonGradient: {
            flex: 1,
            justifyContent: 'center',
            alignItems: 'center',
        },
        buttonText: {
            color: 'white',
            fontSize: 15,
            fontWeight: '900',
            letterSpacing: 1,
            textTransform: 'uppercase',
        },
        minimalButton: {
            marginTop: 20,
            alignItems: 'center',
        },
        minimalButtonText: {
            fontSize: 13,
            fontWeight: '700',
            color: colors.textGrey,
        },
        signOutLink: {
            marginTop: 40,
            alignItems: 'center',
        },
        signOutText: {
            fontSize: 13,
            fontWeight: '800',
            color: colors.danger,
            textTransform: 'uppercase',
            letterSpacing: 1,
        },
    });

    const renderHeader = () => (
        <View style={styles.header}>
            <View style={styles.iconCircle}>
                <MaterialCommunityIcons
                    name={step === 'choice' ? "shield-home" : step === 'join' ? "key-link" : "office-building-marker"}
                    size={36}
                    color={colors.textDark}
                />
            </View>
            <Text style={styles.title}>
                {step === 'choice' ? 'GET STARTED' : step === 'join' ? 'JOIN' : 'CREATE'}
            </Text>
            <Text style={styles.subtitle}>
                {step === 'choice'
                    ? 'Create a new household or join an existing one using an invite code.'
                    : step === 'join'
                        ? 'Enter the 6-digit code shared with you by the household owner.'
                        : 'Give your household a name to get started.'}
            </Text>
        </View>
    );

    const renderChoice = () => (
        <View style={styles.cardWrapper}>
            <BlurView intensity={Platform.OS === 'ios' ? 25 : 100} tint={isDarkMode ? "dark" : "light"} style={styles.glassCard}>
                <TouchableOpacity
                    style={styles.choiceItem}
                    onPress={() => setStep('join')}
                    activeOpacity={0.7}
                    {...accessibleButton('Join Household', 'Enter an invite code to join an existing household')}
                >
                    <LinearGradient
                        colors={['#3B82F6', '#1D4ED8']}
                        style={styles.choiceIcon}
                    >
                        <MaterialCommunityIcons name="link-variant" size={24} color="white" />
                    </LinearGradient>
                    <View style={styles.choiceText}>
                        <Text style={styles.choiceTitle}>Join Household</Text>
                        <Text style={styles.choiceDesc}>Enter your invite code</Text>
                    </View>
                    <MaterialCommunityIcons name="chevron-right" size={20} color={colors.textGrey} />
                </TouchableOpacity>

                <View style={styles.choiceDivider} />

                <TouchableOpacity
                    style={styles.choiceItem}
                    onPress={() => setStep('create')}
                    activeOpacity={0.7}
                    {...accessibleButton('Create New Household', 'Set up a new household and generate invite codes')}
                >
                    <LinearGradient
                        colors={['#10B981', '#059669']}
                        style={styles.choiceIcon}
                    >
                        <MaterialCommunityIcons name="plus-thick" size={24} color="white" />
                    </LinearGradient>
                    <View style={styles.choiceText}>
                        <Text style={styles.choiceTitle}>Create New</Text>
                        <Text style={styles.choiceDesc}>Setup your household</Text>
                    </View>
                    <MaterialCommunityIcons name="chevron-right" size={20} color={colors.textGrey} />
                </TouchableOpacity>
            </BlurView>
        </View>
    );

    const renderJoin = () => (
        <View style={styles.cardWrapper}>
            <BlurView intensity={Platform.OS === 'ios' ? 25 : 100} tint={isDarkMode ? "dark" : "light"} style={styles.glassCard}>
                <Text style={styles.inputLabel}>Invite Code</Text>
                <TextInput
                    style={styles.keyInput}
                    placeholder="000000"
                    placeholderTextColor={isDarkMode ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)"}
                    value={inviteCode}
                    onChangeText={setInviteCode}
                    keyboardType="numeric"
                    maxLength={6}
                    autoFocus
                    {...accessibleInput('Invite Code', 'Enter your 6-digit invite code')}
                />
                <TouchableOpacity
                    style={[styles.primaryButton, loading && { opacity: 0.5 }]}
                    onPress={handleJoin}
                    disabled={loading}
                    {...accessibleButton('Join Household')}
                >
                    <LinearGradient
                        colors={['#3B82F6', '#1D4ED8']}
                        style={styles.buttonGradient}
                        start={{ x: 0, y: 0 }}
                        end={{ x: 1, y: 0 }}
                    >
                        {loading ? <ActivityIndicator color="white" /> : (
                            <Text style={styles.buttonText}>Join Household</Text>
                        )}
                    </LinearGradient>
                </TouchableOpacity>
                <TouchableOpacity style={styles.minimalButton} onPress={() => setStep('choice')}>
                    <Text style={styles.minimalButtonText}>Go Back</Text>
                </TouchableOpacity>
            </BlurView>
        </View>
    );

    const renderCreate = () => (
        <View style={styles.cardWrapper}>
            <BlurView intensity={Platform.OS === 'ios' ? 25 : 100} tint={isDarkMode ? "dark" : "light"} style={styles.glassCard}>
                <Text style={styles.inputLabel}>Household Name</Text>
                <TextInput
                    style={styles.premiumTextInput}
                    placeholder="e.g. My Home"
                    placeholderTextColor={isDarkMode ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)"}
                    value={householdName}
                    onChangeText={setHouseholdName}
                    autoFocus
                    {...accessibleInput('Household Name', 'Enter a name for your new household')}
                />
                <TouchableOpacity
                    style={[styles.primaryButton, loading && { opacity: 0.5 }]}
                    onPress={handleCreate}
                    disabled={loading}
                    {...accessibleButton('Create Household')}
                >
                    <LinearGradient
                        colors={['#10B981', '#059669']}
                        style={styles.buttonGradient}
                        start={{ x: 0, y: 0 }}
                        end={{ x: 1, y: 0 }}
                    >
                        {loading ? <ActivityIndicator color="white" /> : (
                            <Text style={styles.buttonText}>Create Household</Text>
                        )}
                    </LinearGradient>
                </TouchableOpacity>
                <TouchableOpacity style={styles.minimalButton} onPress={() => setStep('choice')}>
                    <Text style={styles.minimalButtonText}>Go Back</Text>
                </TouchableOpacity>
            </BlurView>
        </View>
    );

    return (
        <View style={[styles.mainContainer, { backgroundColor: colors.background }]}>
            <StatusBar barStyle={isDarkMode ? "light-content" : "dark-content"} />
            <LinearGradient
                colors={isDarkMode ? ['#020617', '#0F172A'] : ['#F8FAFC', '#F1F5F9', '#CBD5E1']}
                style={StyleSheet.absoluteFill}
            />
            <KeyboardAvoidingView
                behavior={Platform.OS === 'ios' ? 'padding' : undefined}
                style={{ flex: 1 }}
            >
                <ScrollView
                    contentContainerStyle={styles.scrollContent}
                    showsVerticalScrollIndicator={false}
                >
                    {renderHeader()}
                    {step === 'choice' ? renderChoice() : step === 'join' ? renderJoin() : renderCreate()}

                    <TouchableOpacity
                        style={styles.signOutLink}
                        onPress={() => logout()}
                        {...accessibleButton('Sign Out', 'Logout of your account')}
                    >
                        <Text style={styles.signOutText}>Sign Out</Text>
                    </TouchableOpacity>
                </ScrollView>
            </KeyboardAvoidingView>
        </View>
    );
}
