import React, { useState, useRef, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Image,
  Alert,
  SafeAreaView,
  Platform,
  StatusBar,
  ScrollView,
  Dimensions,
  Animated,
  KeyboardAvoidingView,
} from 'react-native';
import * as WebBrowser from 'expo-web-browser';
import * as AppleAuthentication from 'expo-apple-authentication';
import { makeRedirectUri } from 'expo-auth-session';
import * as Crypto from 'expo-crypto';
import { supabase } from '../lib/supabase';
import { useTheme } from '../hooks/useTheme';
import { STRINGS } from '../constants/strings';
import { LoginForm, SocialLoginButtons } from '../components/auth';
import { useAuthStore } from '../features/auth/store';

const { width } = Dimensions.get('window');

type AuthMethod = 'email' | 'social';

export default function LoginScreen() {
  const { colors, isDarkMode } = useTheme();

  // New auth store
  const { setSession, loadProfile } = useAuthStore();

  // Theme configuration for children
  const theme = {
    background: colors.background,
    cardBg: isDarkMode ? 'rgba(30, 41, 59, 0.95)' : 'rgba(255, 255, 255, 0.98)',
    cardBorder: isDarkMode ? 'rgba(255, 255, 255, 0.08)' : 'rgba(0, 0, 0, 0.04)',
    inputBg: isDarkMode ? 'rgba(255, 255, 255, 0.04)' : 'rgba(0, 0, 0, 0.02)',
    inputBorder: isDarkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0, 0, 0, 0.06)',
    inputFocusBorder: colors.accentBlue,
    textPrimary: colors.textDark,
    textSecondary: colors.textGrey,
    textMuted: isDarkMode ? 'rgba(255, 255, 255, 0.35)' : 'rgba(0, 0, 0, 0.3)',
    accent: colors.accentBlue,
    buttonGradient: [colors.accentBlue, '#1D4ED8'],
  };

  // Auth State
  const [authMethod, setAuthMethod] = useState<AuthMethod>('email');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [isSignUp, setIsSignUp] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  // Animations
  const fadeAnim = useRef(new Animated.Value(0)).current;
  const slideAnim = useRef(new Animated.Value(30)).current;
  const scaleAnim = useRef(new Animated.Value(0.95)).current;

  useEffect(() => {
    Animated.parallel([
      Animated.timing(fadeAnim, {
        toValue: 1,
        duration: 800,
        useNativeDriver: true,
      }),
      Animated.timing(slideAnim, {
        toValue: 0,
        duration: 800,
        useNativeDriver: true,
      }),
      Animated.spring(scaleAnim, {
        toValue: 1,
        tension: 50,
        friction: 8,
        useNativeDriver: true,
      }),
    ]).start();
  }, []);

  WebBrowser.maybeCompleteAuthSession();

  const redirectTo = makeRedirectUri({
    scheme: 'household-ledger',
  });

  const handlePostAuth = async (userSession: any) => {
    setSession(userSession);
    await loadProfile();
  };

  // Handlers
  const handleEmailAuth = async () => {
    if (!email || !password) {
      Alert.alert(STRINGS.login.missingInfo, STRINGS.login.fillAllFields);
      return;
    }

    if (isSignUp && !name) {
      Alert.alert(STRINGS.login.missingInfo, STRINGS.login.enterName);
      return;
    }

    setLoading(true);
    try {
      if (isSignUp) {
        const { data, error } = await supabase.auth.signUp({
          email,
          password,
          options: {
            data: {
              name: name, // Custom claim for profiles trigger
            },
          },
        });
        if (error) throw error;
        if (data.session) {
          await handlePostAuth(data.session);
          Alert.alert(STRINGS.common.success, STRINGS.login.accountCreated);
        } else if (data.user) {
          Alert.alert(STRINGS.common.success, 'Registration successful. Please check your email to verify your account.');
        }
      } else {
        const { data, error } = await supabase.auth.signInWithPassword({
          email,
          password,
        });
        if (error) throw error;
        if (data.session) {
          await handlePostAuth(data.session);
        }
      }
    } catch (error: any) {
      Alert.alert(STRINGS.common.error, error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    try {
      setLoading(true);
      const { data, error } = await supabase.auth.signInWithOAuth({
        provider: 'google',
        options: {
          redirectTo,
          skipBrowserRedirect: false,
        },
      });

      if (error) throw error;

      if (data?.url) {
        const result = await WebBrowser.openAuthSessionAsync(data.url, redirectTo);
        if (result.type === 'success' && result.url) {
          const url = new URL(result.url);
          const accessToken = url.searchParams.get('access_token');
          const refreshToken = url.searchParams.get('refresh_token');

          if (accessToken && refreshToken) {
            const { data: { session: newSession }, error: setSessionError } = await supabase.auth.setSession({
              access_token: accessToken,
              refresh_token: refreshToken,
            });
            if (setSessionError) throw setSessionError;
            if (newSession) await handlePostAuth(newSession);
          }
        }
      }
    } catch (error: any) {
      Alert.alert(STRINGS.login.googleLoginError, error.message);
    } finally {
      setLoading(false);
    }
  };

  const handleAppleLogin = async () => {
    try {
      setLoading(true);
      const rawNonce = await Crypto.getRandomBytesAsync(32);
      const nonce = Array.from(rawNonce)
        .map((b) => b.toString(16).padStart(2, '0'))
        .join('');

      const hashedNonce = await Crypto.digestStringAsync(
        Crypto.CryptoDigestAlgorithm.SHA256,
        nonce
      );

      const appleCredential = await AppleAuthentication.signInAsync({
        requestedScopes: [
          AppleAuthentication.AppleAuthenticationScope.FULL_NAME,
          AppleAuthentication.AppleAuthenticationScope.EMAIL,
        ],
        nonce: hashedNonce,
      });

      const { data, error } = await supabase.auth.signInWithIdToken({
        provider: 'apple',
        token: appleCredential.identityToken!,
        nonce,
      });

      if (error) throw error;
      if (data.session) await handlePostAuth(data.session);
    } catch (e: any) {
      if (e.code !== 'ERR_CANCELED') {
        Alert.alert(STRINGS.login.appleLoginError, e.message);
      }
    } finally {
      setLoading(false);
    }
  };

  const styles = StyleSheet.create({
    container: {
      flex: 1,
      backgroundColor: theme.background,
    },
    backgroundLayer: {
      ...StyleSheet.absoluteFillObject,
      overflow: 'hidden',
    },
    orb: {
      position: 'absolute',
      width: width * 1.5,
      height: width * 1.5,
      borderRadius: width * 0.75,
      opacity: isDarkMode ? 0.3 : 0.15,
    },
    orb1: {
      top: -width * 0.5,
      left: -width * 0.5,
      backgroundColor: colors.accentBlue,
    },
    orb2: {
      bottom: -width * 0.5,
      right: -width * 0.5,
      backgroundColor: '#1D4ED8',
    },
    orb3: {
      top: '30%',
      left: '20%',
      width: width * 0.8,
      height: width * 0.8,
      backgroundColor: '#EC4899',
      opacity: isDarkMode ? 0.1 : 0.05,
    },
    scrollContent: {
      flexGrow: 1,
      paddingHorizontal: 24,
      paddingTop: Platform.OS === 'ios' ? 40 : 60,
      paddingBottom: 40,
    },
    logoSection: {
      alignItems: 'center',
      marginBottom: 32,
    },
    logoMark: {
      width: 100,
      height: 100,
      borderRadius: 28,
      backgroundColor: isDarkMode ? 'rgba(255, 255, 255, 0.05)' : '#FFFFFF',
      alignItems: 'center',
      justifyContent: 'center',
      marginBottom: 20,
      shadowColor: '#000',
      shadowOffset: { width: 0, height: 12 },
      shadowOpacity: 0.1,
      shadowRadius: 16,
      elevation: 10,
    },
    brandName: {
      fontSize: 32,
      fontWeight: '900',
      color: theme.textPrimary,
      letterSpacing: -1,
    },
    brandTagline: {
      fontSize: 16,
      color: theme.textSecondary,
      fontWeight: '500',
      marginTop: 4,
    },
    card: {
      backgroundColor: theme.cardBg,
      borderRadius: 32,
      padding: 32,
      borderWidth: 1,
      borderColor: theme.cardBorder,
      shadowColor: '#000',
      shadowOffset: { width: 0, height: 20 },
      shadowOpacity: isDarkMode ? 0.4 : 0.08,
      shadowRadius: 40,
      elevation: 20,
    },
    tabRow: {
      flexDirection: 'row',
      backgroundColor: theme.inputBg,
      borderRadius: 16,
      padding: 4,
      marginBottom: 32,
    },
    tab: {
      flex: 1,
      paddingVertical: 14,
      alignItems: 'center',
      borderRadius: 12,
    },
    tabActive: {
      backgroundColor: isDarkMode ? 'rgba(59, 130, 246, 0.2)' : '#FFFFFF',
    },
    tabText: {
      fontSize: 14,
      fontWeight: '600',
      color: theme.textMuted,
    },
    tabTextActive: {
      color: theme.accent,
      fontWeight: '700',
    },
    footerLabel: {
      marginTop: 40,
      alignItems: 'center',
    },
    footerText: {
      fontSize: 13,
      color: theme.textMuted,
      textAlign: 'center',
      lineHeight: 20,
    },
    footerLink: {
      color: theme.accent,
      fontWeight: '600',
    },
  });

  return (
    <View style={styles.container}>
      <StatusBar barStyle={isDarkMode ? 'light-content' : 'dark-content'} />

      <View style={styles.backgroundLayer}>
        <View style={[styles.orb, styles.orb1]} />
        <View style={[styles.orb, styles.orb2]} />
        <View style={[styles.orb, styles.orb3]} />
      </View>

      <SafeAreaView style={{ flex: 1 }}>
        <KeyboardAvoidingView
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
          style={{ flex: 1 }}
        >
          <ScrollView
            contentContainerStyle={styles.scrollContent}
            showsVerticalScrollIndicator={false}
            keyboardShouldPersistTaps="handled"
          >
            <Animated.View
              style={{
                opacity: fadeAnim,
                transform: [{ translateY: slideAnim }, { scale: scaleAnim }],
              }}
            >
              <View style={styles.logoSection}>
                <View style={styles.logoMark}>
                  <Image
                    source={require('../../assets/logo.png')}
                    style={{ width: 72, height: 72 }}
                    resizeMode="contain"
                  />
                </View>
                <Text style={styles.brandName}>{STRINGS.login.brandName}</Text>
                <Text style={styles.brandTagline}>{STRINGS.login.brandTagline}</Text>
              </View>

              <View style={styles.card}>
                <View style={styles.tabRow}>
                  {(['email', 'social'] as AuthMethod[]).map((method) => (
                    <TouchableOpacity
                      key={method}
                      style={[styles.tab, authMethod === method && styles.tabActive]}
                      onPress={() => setAuthMethod(method)}
                    >
                      <Text style={[styles.tabText, authMethod === method && styles.tabTextActive]}>
                        {method === 'email' ? STRINGS.auth.email : STRINGS.auth.social}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </View>

                {authMethod === 'email' ? (
                  <LoginForm
                    isSignUp={isSignUp}
                    name={name}
                    email={email}
                    password={password}
                    loading={loading}
                    showPassword={showPassword}
                    theme={theme}
                    onNameChange={setName}
                    onEmailChange={setEmail}
                    onPasswordChange={setPassword}
                    onTogglePassword={() => setShowPassword(!showPassword)}
                    onSubmit={handleEmailAuth}
                    onToggleMode={() => setIsSignUp(!isSignUp)}
                  />
                ) : (
                  <SocialLoginButtons
                    onGooglePress={handleGoogleLogin}
                    onApplePress={handleAppleLogin}
                    theme={theme}
                  />
                )}
              </View>

              <View style={styles.footerLabel}>
                <Text style={styles.footerText}>
                  By continuing, you agree to our{' '}
                  <Text style={styles.footerLink}>{STRINGS.login.terms}</Text>
                  {'\n'}and{' '}
                  <Text style={styles.footerLink}>{STRINGS.login.privacy}</Text>
                </Text>
              </View>
            </Animated.View>
          </ScrollView>
        </KeyboardAvoidingView>
      </SafeAreaView>
    </View>
  );
}