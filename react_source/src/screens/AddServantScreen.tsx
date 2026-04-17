import React, { useState, useEffect, useLayoutEffect } from 'react';
import { View, Text, TextInput, StyleSheet, TouchableOpacity, Alert, ScrollView, StatusBar, Platform } from 'react-native';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { useAuthStore } from '../features/auth/store';
import { usePeopleStore } from '../features/people/store';
import { useSettingsStore } from '../features/settings/store';
import { RootStackParamList } from '../types';
import { COLORS, SIZES, SHADOWS } from '../constants/theme';
import { useTheme } from '../hooks/useTheme';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';
import { validateServantData } from '../lib/securityUtils';
import {
  accessibleButton,
  accessibleInput,
  accessibleHeader
} from '../lib/accessibility';

type AddServantRouteProp = RouteProp<RootStackParamList, 'AddServant'>;

export default function AddServantScreen() {
  const { colors, isDarkMode } = useTheme();
  const navigation = useNavigation();
  const route = useRoute<AddServantRouteProp>();
  const { servantId } = route.params || {};

  const { profile } = useAuthStore();
  const { servants, addServant, updateServant } = usePeopleStore();
  const { currencySymbol } = useSettingsStore();

  const [inviteCode, setInviteCode] = useState<string | null>(null);
  const isAdmin = profile?.role === 'admin';

  const [name, setName] = useState('');
  const [role, setRole] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [salary, setSalary] = useState('');
  const [budget, setBudget] = useState('');

  useLayoutEffect(() => {
    navigation.setOptions({
      headerTitle: servantId ? 'Edit Staff' : 'Add Staff',
      headerStyle: { backgroundColor: colors.background, shadowColor: 'transparent', elevation: 0 },
      headerTitleStyle: { color: colors.textDark, fontWeight: 'bold' },
      headerTintColor: colors.textDark,
    });
  }, [navigation, servantId, colors]);

  useEffect(() => {
    if (servantId) {
      const servant = servants.find((s) => s.id === servantId);
      if (servant) {
        setName(servant.name);
        setRole(servant.role);
        setPhoneNumber(servant.phoneNumber || '');
        setSalary(servant.salary ? servant.salary.toString() : '');
        setBudget(servant.budget ? servant.budget.toString() : '');
      }
    }
  }, [servantId, servants]);

  const handleSave = () => {
    // Comprehensive input validation
    const validation = validateServantData({
      name,
      role,
      phoneNumber,
      salary,
      budget,
    });

    if (!validation.isValid) {
      Alert.alert('Validation Error', validation.errors.join('\n'));
      return;
    }

    const servantData = {
      name: name.trim(),
      role: role.trim(),
      phoneNumber: phoneNumber.trim() || undefined,
      salary: salary ? parseFloat(salary) : undefined,
      budget: budget ? parseFloat(budget) : undefined,
    };

    if (servantId) {
      updateServant(servantId, servantData);
      navigation.goBack();
    } else {
      addServant(servantData).then(code => {
        setInviteCode(code || null);
      }).catch(err => {
        Alert.alert('Error', err.message);
      });
    }
  };

  if (inviteCode) {
    return (
      <View style={[styles.container, { backgroundColor: colors.background, justifyContent: 'center', alignItems: 'center' }]}>
        <LinearGradient colors={isDarkMode ? ['#0F172A', '#1E293B'] : [colors.background, colors.background]} style={StyleSheet.absoluteFill} />
        <View style={[styles.successIconContainer, { backgroundColor: isDarkMode ? 'rgba(78, 205, 196, 0.1)' : 'rgba(78, 205, 196, 0.2)' }]}>
          <Text style={{ fontSize: 60 }}>✨</Text>
        </View>
        <Text style={[styles.successTitle, { color: colors.textDark }]}>Staff Added!</Text>
        <Text style={[styles.successSubtitle, { color: colors.textGrey }]}>
          Share this unique 6-digit code with them. They will need it to join your ledger.
        </Text>

        <BlurView intensity={20} tint={isDarkMode ? "dark" : "light"} style={[styles.inviteCodeCard, { borderColor: colors.secondary, backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : 'rgba(255,255,255,0.6)' }]}>
          <Text style={[styles.inviteCodeText, { color: colors.secondary }]}>{inviteCode}</Text>
        </BlurView>

        <TouchableOpacity
          onPress={() => navigation.goBack()}
          {...accessibleButton('Got it', 'Dismiss success screen')}
        >
          <LinearGradient colors={['#3B82F6', '#1D4ED8']} style={styles.doneButton}>
            <Text style={styles.buttonText}>Got it!</Text>
          </LinearGradient>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <StatusBar barStyle={isDarkMode ? "light-content" : "dark-content"} />
      <LinearGradient colors={isDarkMode ? ['#0F172A', '#1E293B'] : [colors.background, colors.background]} style={StyleSheet.absoluteFill} />

      <ScrollView contentContainerStyle={{ padding: 20, paddingBottom: 60 }} showsVerticalScrollIndicator={false}>

        {/* Basic Info Section */}
        <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.formCard, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
          <Text style={[styles.sectionTitle, { color: colors.textDark }]}>Profile</Text>

          <View style={styles.inputGroup}>
            <Text style={[styles.label, { color: colors.textGrey }]}>FULL NAME</Text>
            <TextInput
              style={[styles.input, { backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : '#F8FAFC', color: colors.textDark }]}
              value={name}
              onChangeText={setName}
              placeholder="e.g. John Doe"
              placeholderTextColor={colors.textGrey}
              {...accessibleInput('Full Name', 'Enter staff member full name')}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={[styles.label, { color: colors.textGrey }]}>ROLE</Text>
            <TextInput
              style={[styles.input, { backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : '#F8FAFC', color: colors.textDark }]}
              value={role}
              onChangeText={setRole}
              placeholder="e.g. Driver, Maid..."
              placeholderTextColor={colors.textGrey}
              {...accessibleInput('Role', 'Enter staff role (e.g. Driver, Maid)')}
            />
          </View>

          <View style={styles.inputGroup}>
            <Text style={[styles.label, { color: colors.textGrey }]}>PHONE (OPTIONAL)</Text>
            <TextInput
              style={[styles.input, { backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : '#F8FAFC', color: colors.textDark }]}
              value={phoneNumber}
              onChangeText={setPhoneNumber}
              placeholder="e.g. 123-456-7890"
              keyboardType="phone-pad"
              placeholderTextColor={colors.textGrey}
              {...accessibleInput('Phone Number', 'Enter staff phone number (optional)')}
            />
          </View>
        </BlurView>

        {/* Financials Section */}
        <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.formCard, { marginTop: 24, borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
          <Text style={[styles.sectionTitle, { color: colors.textDark }]}>Financials</Text>

          <View style={styles.inputGroup}>
            <Text style={[styles.label, { color: colors.textGrey }]}>MONTHLY SALARY</Text>
            <View style={[styles.amountInputContainer, { backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : '#F8FAFC' }]}>
              <Text style={[styles.currencyPrefix, { color: colors.textDark }]}>{currencySymbol} </Text>
              <TextInput
                style={[styles.amountInput, { color: colors.textDark }, !isAdmin && { color: colors.textGrey }]}
                value={salary}
                onChangeText={setSalary}
                placeholder="0"
                keyboardType="numeric"
                placeholderTextColor={colors.textGrey}
                editable={isAdmin}
                {...accessibleInput('Monthly Salary', 'Enter staff monthly salary amount')}
              />
            </View>
          </View>

          <View style={styles.inputGroup}>
            <Text style={[styles.label, { color: colors.textGrey }]}>SPENDING LIMIT</Text>
            <View style={[styles.amountInputContainer, { backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : '#F8FAFC' }]}>
              <Text style={[styles.currencyPrefix, { color: colors.textDark }]}>{currencySymbol} </Text>
              <TextInput
                style={[styles.amountInput, { color: colors.textDark }, !isAdmin && { color: colors.textGrey }]}
                value={budget}
                onChangeText={setBudget}
                placeholder="0"
                keyboardType="numeric"
                placeholderTextColor={colors.textGrey}
                editable={isAdmin}
                {...accessibleInput('Spending Limit', 'Enter staff monthly spending limit')}
              />
            </View>
          </View>
        </BlurView>




        <TouchableOpacity
          style={styles.saveButtonWrapper}
          onPress={handleSave}
          {...accessibleButton(servantId ? 'Update Staff Profile' : 'Save Staff Profile')}
        >
          <LinearGradient colors={['#3B82F6', '#1D4ED8']} style={styles.saveButton}>
            <Text style={styles.buttonText}>{servantId ? 'Update Staff Profile' : 'Save Staff Profile'}</Text>
          </LinearGradient>
        </TouchableOpacity>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  formCard: {
    borderRadius: 24,
    padding: 24,
    borderWidth: 1,
    overflow: 'hidden',
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '900',
    marginBottom: 20,
    letterSpacing: -0.5,
  },
  inputGroup: {
    marginBottom: 20,
  },
  label: {
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 1,
    marginBottom: 8,
  },
  input: {
    borderRadius: 16,
    padding: 16,
    fontSize: 16,
    fontWeight: '600',
  },
  amountInputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    borderRadius: 16,
    paddingHorizontal: 16,
  },
  currencyPrefix: {
    fontSize: 18,
    fontWeight: '700',
    marginRight: 8,
  },
  amountInput: {
    flex: 1,
    paddingVertical: 16,
    fontSize: 18,
    fontWeight: '700',
  },
  saveButtonWrapper: {
    marginTop: 32,
    borderRadius: 16,
    overflow: 'hidden',
    ...SHADOWS.premium,
  },
  saveButton: {
    padding: 20,
    alignItems: 'center',
  },
  buttonText: {
    color: '#FFF',
    fontSize: 16,
    fontWeight: '900',
    letterSpacing: 1,
  },
  successIconContainer: {
    width: 100,
    height: 100,
    borderRadius: 50,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 24,
  },
  successTitle: {
    fontSize: 32,
    fontWeight: '900',
    marginBottom: 8,
    textAlign: 'center',
  },
  successSubtitle: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 40,
    paddingHorizontal: 40,
    lineHeight: 24,
    fontWeight: '500',
  },
  inviteCodeCard: {
    paddingVertical: 30,
    paddingHorizontal: 50,
    borderRadius: 24,
    borderWidth: 1,
    marginBottom: 48,
    borderStyle: 'dashed',
  },
  inviteCodeText: {
    fontSize: 42,
    fontWeight: '900',
    letterSpacing: 8,
    textAlign: 'center',
  },
  doneButton: {
    paddingVertical: 18,
    paddingHorizontal: 60,
    borderRadius: 32,
    alignItems: 'center',
  },
});
