import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, Switch, TextInput, TouchableOpacity, Alert, ScrollView, StatusBar, Platform } from 'react-native';
import { SIZES, SHADOWS, COLORS } from '../constants/theme';
import { useTheme } from '../hooks/useTheme';
import { FeatureFlag, useFeature } from '../config/features';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import * as FileSystem from 'expo-file-system';
import * as Sharing from 'expo-sharing';
import { useNavigation } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';
import { RootStackParamList } from '../types';
import { getVersionString } from '../config/version';
import { secureLog, validateImportData } from '../lib/securityUtils';
import { isBiometricAvailable, authenticateWithBiometrics } from '../lib/biometrics';
import {
  accessibleButton,
  accessibleInput,
  accessibleToggle,
  accessibleLink
} from '../lib/accessibility';

// New Domain Stores
import { useAuthStore } from '../features/auth/store';
import { useSettingsStore } from '../features/settings/store';
import { usePeopleStore } from '../features/people/store';
import { useTransactionStore } from '../features/transactions/store';
import { useCategoryStore } from '../features/categories/store';

export default function SettingsScreen() {
  const { colors } = useTheme();
  const navigation = useNavigation<StackNavigationProp<RootStackParamList>>();

  const {
    adminName, setAdminName,
    adminBudget, setAdminBudget,
    isDarkMode, toggleDarkMode,
    notificationsEnabled, setNotificationsEnabled,
    currencySymbol, setCurrencySymbol,
    biometricAuthEnabled, setBiometricAuthEnabled
  } = useSettingsStore();

  const { profile, signOut } = useAuthStore();
  const { servants, members } = usePeopleStore();
  const { transactions } = useTransactionStore();
  const { categories } = useCategoryStore();

  const showExport = useFeature(FeatureFlag.EXPORT_DATA);
  const showMultiCurrency = useFeature(FeatureFlag.MULTI_CURRENCY);
  const showNotifications = useFeature(FeatureFlag.NOTIFICATIONS);

  const [exportText, setExportText] = useState('');
  const [isBiometricSupported, setIsBiometricSupported] = useState(false);

  useEffect(() => {
    const checkBiometrics = async () => {
      const available = await isBiometricAvailable();
      setIsBiometricSupported(available);
    };
    checkBiometrics();
  }, []);

  const handleToggleBiometrics = async (value: boolean) => {
    if (value) {
      const success = await authenticateWithBiometrics('Enable biometric login');
      if (success) {
        setBiometricAuthEnabled(true);
      }
    } else {
      setBiometricAuthEnabled(false);
    }
  };

  const isServant = profile?.role === 'servant';

  const handleExport = () => {
    // Sanitize export data - remove sensitive fields like PINs
    const sanitizedServants = servants.map(({ pin_hash, ...rest }: any) => rest);
    const sanitizedMembers = members.map(({ pin_hash, ...rest }: any) => rest);

    const payload = JSON.stringify({
      adminName,
      adminBudget,
      isDarkMode,
      notificationsEnabled,
      servants: sanitizedServants,
      members: sanitizedMembers,
      transactions,
      categories,
      currencySymbol,
      exportedAt: new Date().toISOString(),
      version: getVersionString()
    }, null, 2);
    setExportText(payload);
    Alert.alert('Backup Ready', 'Your data has been copied to the box below. Note: PINs are not exported for security.');
  };

  const handleCSVExport = async () => {
    if (transactions.length === 0) {
      Alert.alert('No Data', 'There are no transactions to export.');
      return;
    }

    const header = 'Date,Description,Amount,Category,Type,Staff,Member\n';
    const rows = transactions.map(t => {
      const category = categories.find(c => c.id === t.categoryId)?.name || 'N/A';
      const staff = servants.find(s => s.id === t.servantId)?.name || 'N/A';
      const member = members.find(m => m.id === t.memberId)?.name || 'N/A';
      return `${t.date},"${t.description.replace(/"/g, '""')}",${t.amount},${category},${t.type},${staff},${member}`;
    }).join('\n');

    const csvContent = header + rows;
    const fileUri = (FileSystem as any).cacheDirectory + `household_ledger_export_${new Date().getTime()}.csv`;

    try {
      await FileSystem.writeAsStringAsync(fileUri, csvContent, { encoding: (FileSystem as any).EncodingType.UTF8 });
      await Sharing.shareAsync(fileUri, {
        mimeType: 'text/csv',
        dialogTitle: 'Export Transactions',
        UTI: 'public.comma-separated-values-text'
      });
    } catch (error) {
      secureLog.error('Export Error:', error);
      Alert.alert('Export Failed', 'An error occurred while generating the CSV.');
    }
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <StatusBar barStyle={isDarkMode ? "light-content" : "dark-content"} />
      <LinearGradient colors={isDarkMode ? ['#0F172A', '#1E293B'] : [colors.background, colors.background]} style={StyleSheet.absoluteFill} />

      <View style={styles.header}>
        <Text style={[styles.headerTitle, { color: colors.textDark }]}>Settings</Text>
        <View style={styles.avatarMini}>
          <Text style={styles.avatarMiniText}>{(profile?.name || adminName || 'A')?.charAt(0).toUpperCase()}</Text>
        </View>
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={styles.scrollContent}
      >
        <Text style={[styles.sectionTitle, { color: colors.textGrey }]}>Preferences</Text>
        <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.section, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
          <View style={styles.row}>
            <View style={styles.rowLabel}>
              <Ionicons name={isDarkMode ? "moon-outline" : "sunny-outline"} size={18} color={colors.textDark} style={{ marginRight: 12 }} />
              <Text style={[styles.label, { color: colors.textDark }]}>Dark Mode</Text>
            </View>
            <Switch
              value={isDarkMode}
              onValueChange={toggleDarkMode}
              trackColor={{ false: 'rgba(0,0,0,0.1)', true: '#3B82F6' }}
              thumbColor={Platform.OS === 'ios' ? undefined : colors.white}
              {...accessibleToggle('Dark Mode', isDarkMode, 'Toggles between light and dark themes')}
            />
          </View>

          {showNotifications && (
            <View style={[styles.row, { borderTopWidth: 1, borderTopColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', marginTop: 8, paddingTop: 16 }]}>
              <View style={styles.rowLabel}>
                <Ionicons name="notifications-outline" size={18} color={colors.textDark} style={{ marginRight: 12 }} />
                <Text style={[styles.label, { color: colors.textDark }]}>Notifications</Text>
              </View>
              <Switch
                value={notificationsEnabled}
                onValueChange={setNotificationsEnabled}
                trackColor={{ false: 'rgba(0,0,0,0.1)', true: '#3B82F6' }}
                thumbColor={Platform.OS === 'ios' ? undefined : colors.white}
                {...accessibleToggle('Notifications', notificationsEnabled, 'Enables or disables app notifications')}
              />
            </View>
          )}

          {isBiometricSupported && (
            <View style={[styles.row, { borderTopWidth: 1, borderTopColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', marginTop: 8, paddingTop: 16 }]}>
              <View style={styles.rowLabel}>
                <Ionicons name="finger-print-outline" size={18} color={colors.textDark} style={{ marginRight: 12 }} />
                <Text style={[styles.label, { color: colors.textDark }]}>Biometric Login</Text>
              </View>
              <Switch
                value={biometricAuthEnabled}
                onValueChange={handleToggleBiometrics}
                trackColor={{ false: 'rgba(0,0,0,0.1)', true: '#3B82F6' }}
                thumbColor={Platform.OS === 'ios' ? undefined : colors.white}
                {...accessibleToggle('Biometric Login', biometricAuthEnabled, 'Enables or disables FaceID/TouchID login')}
              />
            </View>
          )}
        </BlurView>

        {showMultiCurrency && (
          <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.section, { paddingVertical: 16, marginTop: 12, borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
            <Text style={[styles.label, { marginBottom: 16, color: colors.textDark }]}>Currency</Text>
            <View style={styles.currencyWrapper}>
              {['$', '€', '£', '¥', '₹', 'PKR'].map(symbol => (
                <TouchableOpacity
                  key={symbol}
                  activeOpacity={0.7}
                  style={[
                    styles.currencyBtn,
                    { backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.03)' },
                    currencySymbol === symbol && styles.currencyBtnActive
                  ]}
                  onPress={() => setCurrencySymbol(symbol)}
                  {...accessibleButton(`Set currency to ${symbol}`, `Current symbol is ${currencySymbol}`)}
                >
                  <Text style={[
                    styles.currencyText,
                    { color: isDarkMode ? 'rgba(255,255,255,0.5)' : 'rgba(0,0,0,0.5)' },
                    currencySymbol === symbol && styles.currencyTextActive
                  ]}>{symbol}</Text>
                </TouchableOpacity>
              ))}
            </View>
          </BlurView>
        )}

        <Text style={[styles.sectionTitle, { color: colors.textGrey }]}>Financial Tools</Text>
        <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.section, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
          <TouchableOpacity
            style={styles.toolRow}
            onPress={() => navigation.navigate('RecurringTransactions')}
            {...accessibleButton('Recurring Templates', 'Navigate to transaction templates')}
          >
            <View style={styles.rowLabel}>
              <Ionicons name="repeat-outline" size={20} color={colors.textDark} style={{ marginRight: 12 }} />
              <Text style={[styles.label, { color: colors.textDark }]}>Recurring Templates</Text>
            </View>
            <Ionicons name="chevron-forward" size={18} color={colors.textGrey} />
          </TouchableOpacity>
        </BlurView>

        <Text style={[styles.sectionTitle, { color: colors.textGrey }]}>Your Profile</Text>
        <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.section, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
          <Text style={[styles.inputLabel, { color: colors.textGrey }]}>YOUR NAME</Text>
          <TextInput
            style={[styles.input, { color: colors.textDark, borderBottomColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' }, isServant && { color: isDarkMode ? 'rgba(255,255,255,0.3)' : 'rgba(0,0,0,0.3)' }]}
            value={adminName}
            onChangeText={setAdminName}
            placeholderTextColor={isDarkMode ? "rgba(255,255,255,0.2)" : "rgba(0,0,0,0.2)"}
            editable={!isServant}
            {...accessibleInput('Display Name', 'Enter your name')}
          />

          {!isServant && (
            <View style={{ marginTop: 24 }}>
              <Text style={[styles.inputLabel, { color: colors.textGrey }]}>MONTHLY BUDGET</Text>
              <View style={styles.amountContainer}>
                <Text style={styles.currencyPrefix}>{currencySymbol} </Text>
                <TextInput
                  style={[styles.amountInput, { color: colors.textDark }]}
                  value={adminBudget ? String(adminBudget) : ''}
                  onChangeText={(txt: string) => setAdminBudget(txt ? parseFloat(txt) : undefined)}
                  placeholder="0.00"
                  keyboardType="numeric"
                  placeholderTextColor={isDarkMode ? "rgba(255,255,255,0.2)" : "rgba(0,0,0,0.2)"}
                  {...accessibleInput('Monthly Budget', 'Enter your monthly budget amount')}
                />
              </View>
            </View>
          )}
        </BlurView>


        <View style={{ marginTop: 40 }}>
          <TouchableOpacity
            style={[styles.logoutBtn, { borderColor: 'rgba(239, 68, 68, 0.2)', backgroundColor: 'rgba(239, 68, 68, 0.05)' }]}
            onPress={signOut}
            {...accessibleButton('Logout', 'Log out of your account')}
          >
            <Ionicons name="log-out-outline" size={18} color="#EF4444" style={{ marginRight: 8 }} />
            <Text style={styles.logoutText}>Logout</Text>
          </TouchableOpacity>
        </View>

        {/* Version Info */}
        <View style={styles.versionContainer}>
          <Text style={[styles.versionText, { color: colors.textGrey }]}>Household Ledger {getVersionString()}</Text>
        </View>

        <View style={{ height: 120 }} />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    paddingTop: Platform.OS === 'ios' ? 60 : 40,
    paddingHorizontal: 20,
    paddingBottom: 20,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 26,
    fontWeight: '900',
    letterSpacing: -1,
  },
  avatarMini: {
    width: 36,
    height: 36,
    borderRadius: 12,
    backgroundColor: 'rgba(59, 130, 246, 0.2)',
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: 'rgba(59, 130, 246, 0.3)',
  },
  avatarMiniText: {
    color: '#3B82F6',
    fontSize: 16,
    fontWeight: '900',
  },
  scrollContent: {
    paddingHorizontal: 20,
  },
  sectionTitle: {
    fontSize: 12,
    fontWeight: '900',
    marginTop: 32,
    marginBottom: 12,
    letterSpacing: 1.5,
    textTransform: 'uppercase',
  },
  section: {
    borderRadius: 24,
    padding: 20,
    borderWidth: 1,
    overflow: 'hidden',
  },
  row: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  rowLabel: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  label: {
    fontSize: 16,
    fontWeight: '700',
  },
  inputLabel: {
    fontSize: 10,
    fontWeight: '900',
    marginBottom: 8,
    letterSpacing: 1,
  },
  input: {
    fontSize: 16,
    fontWeight: '600',
    paddingVertical: 12,
    borderBottomWidth: 1,
  },
  amountContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 4,
  },
  currencyPrefix: {
    fontSize: 24,
    fontWeight: '900',
    color: '#3B82F6',
    marginRight: 8,
  },
  amountInput: {
    flex: 1,
    fontSize: 32,
    fontWeight: '900',
  },
  currencyWrapper: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  currencyBtn: {
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 12,
    marginRight: 8,
    marginBottom: 8,
    borderWidth: 1,
    borderColor: 'transparent',
  },
  currencyBtnActive: {
    backgroundColor: 'rgba(59, 130, 246, 0.2)',
    borderColor: 'rgba(59, 130, 246, 0.3)',
  },
  currencyText: {
    fontWeight: '800',
    fontSize: 14,
  },
  currencyTextActive: {
    color: '#3B82F6',
  },
  logoutBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 16,
    borderRadius: 20,
    borderWidth: 1,
  },
  logoutText: {
    color: '#EF4444',
    fontWeight: '900',
    fontSize: 14,
    letterSpacing: 0.5,
  },
  toolRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  versionContainer: {
    alignItems: 'center',
    marginTop: 24,
  },
  versionText: {
    fontSize: 12,
    fontWeight: '600',
    letterSpacing: 1,
  }
});
