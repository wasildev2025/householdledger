import React, { useState, useLayoutEffect, useEffect } from 'react';
import { View, Text, TextInput, StyleSheet, TouchableOpacity, ScrollView, Alert, StatusBar } from 'react-native';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { useAuthStore } from '../features/auth/store';
import { usePeopleStore } from '../features/people/store';
import { RootStackParamList } from '../types';
import { SHADOWS, COLORS } from '../constants/theme';
import { useTheme } from '../hooks/useTheme';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';
import { validateMemberData } from '../lib/securityUtils';

type AddMemberRouteProp = RouteProp<RootStackParamList, 'AddMember'>;

export default function AddMemberScreen() {
  const { colors, isDarkMode } = useTheme();
  const navigation = useNavigation();
  const route = useRoute<AddMemberRouteProp>();
  const { memberId } = route.params || {};

  const { profile } = useAuthStore();
  const { members, addMember, updateMember } = usePeopleStore();

  const isAdmin = profile?.role === 'admin';

  const [name, setName] = useState('');
  const [inviteCode, setInviteCode] = useState<string | null>(null);

  useLayoutEffect(() => {
    navigation.setOptions({
      headerTitle: memberId ? 'Edit Family' : 'Add Family',
      headerStyle: { backgroundColor: colors.background, shadowColor: 'transparent', elevation: 0 },
      headerTitleStyle: { color: colors.textDark, fontWeight: 'bold' },
      headerTintColor: colors.textDark,
    });
  }, [navigation, memberId, colors]);

  useEffect(() => {
    if (memberId) {
      const member = members.find((m) => m.id === memberId);
      if (member) {
        setName(member.name);
      }
    }
  }, [memberId, members]);

  const handleSave = () => {
    // Comprehensive input validation
    const validation = validateMemberData({
      name,
    });

    if (!validation.isValid) {
      Alert.alert('Validation Error', validation.errors.join('\n'));
      return;
    }

    if (memberId) {
      updateMember(memberId, { name: name.trim() });
      navigation.goBack();
    } else {
      addMember({ name: name.trim() }).then(code => {
        setInviteCode(code || null);
      }).catch(err => {
        Alert.alert('Error', err.message);
      });
    }
  };

  if (!isAdmin) {
    return (
      <View style={[styles.container, { backgroundColor: colors.background, justifyContent: 'center', alignItems: 'center' }]}>
        <Text style={{ color: colors.textGrey }}>Only Admin can manage members.</Text>
      </View>
    )
  }

  if (inviteCode) {
    return (
      <View style={[styles.container, { backgroundColor: colors.background, justifyContent: 'center', alignItems: 'center' }]}>
        <LinearGradient colors={isDarkMode ? ['#0F172A', '#1E293B'] : [colors.background, colors.background]} style={StyleSheet.absoluteFill} />

        <View style={styles.successIconContainer}>
          <Text style={{ fontSize: 60 }}>✨</Text>
        </View>
        <Text style={[styles.successTitle, { color: colors.textDark }]}>Added Successfully!</Text>
        <Text style={[styles.successSubtitle, { color: colors.textGrey }]}>
          Share this unique 6-digit code with them. They will need it to join.
        </Text>

        <BlurView intensity={20} tint={isDarkMode ? "dark" : "light"} style={[styles.inviteCodeCard, { borderColor: colors.secondary, backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : 'rgba(255,255,255,0.6)' }]}>
          <Text style={[styles.inviteCodeText, { color: colors.secondary }]}>{inviteCode}</Text>
        </BlurView>

        <TouchableOpacity onPress={() => navigation.goBack()}>
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

        <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.formCard, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
          <Text style={[styles.sectionTitle, { color: colors.textDark }]}>Profile</Text>

          <View style={styles.inputGroup}>
            <Text style={[styles.label, { color: colors.textGrey }]}>NAME</Text>
            <TextInput
              style={[styles.input, { backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : '#F8FAFC', color: colors.textDark }]}
              value={name}
              onChangeText={setName}
              placeholder="Member Name"
              placeholderTextColor={colors.textGrey}
            />
          </View>


        </BlurView>

        <TouchableOpacity style={styles.saveButtonWrapper} onPress={handleSave}>
          <LinearGradient colors={['#3B82F6', '#1D4ED8']} style={styles.saveButton}>
            <Text style={styles.buttonText}>Save Profile</Text>
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
    backgroundColor: 'rgba(78, 205, 196, 0.1)',
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
