import React, { useState } from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet, Alert, Clipboard, Share, StatusBar, Platform } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { useAuthStore } from '../features/auth/store';
import { usePeopleStore } from '../features/people/store';
import { useTransactionStore } from '../features/transactions/store';
import { useSettingsStore } from '../features/settings/store';
import { Servant, Member, RootStackParamList } from '../types';
import { SHADOWS, SIZES, COLORS } from '../constants/theme';
import { useTheme } from '../hooks/useTheme';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';

type NavigationProp = StackNavigationProp<RootStackParamList>;

export default function PeopleScreen() {
  const { colors, isDarkMode } = useTheme();
  const navigation = useNavigation<NavigationProp>();

  const { profile } = useAuthStore();
  const { servants, members, deleteServant, deleteMember } = usePeopleStore();
  const { transactions } = useTransactionStore();
  const { currencySymbol } = useSettingsStore();

  const [activeTab, setActiveTab] = useState<'staff' | 'family'>('staff');

  const isAdmin = profile?.role === 'admin';

  const getPersonBalance = (id: string, isServant: boolean) => {
    const allocation = transactions
      .filter(t => t.type === 'transfer' && (isServant ? t.servantId === id : t.memberId === id))
      .reduce((s, t) => s + t.amount, 0);
    const utilization = transactions
      .filter(t => t.type === 'expense' && (isServant ? t.servantId === id : t.memberId === id))
      .reduce((s, t) => s + t.amount, 0);
    return allocation - utilization;
  };

  const handleShareCode = async (code: string, name: string) => {
    try {
      await Share.share({
        message: `Hey ${name}, join our Household Ledger using this invite code: ${code}\nDownload the app and select "Join Household"!`,
      });
    } catch (error) {
      Alert.alert('Error', 'Could not share code');
    }
  };

  const handleDeleteServant = (id: string) => {
    Alert.alert('Delete Staff', 'Are you sure?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: () => deleteServant(id) },
    ]);
  };

  const handleDeleteMember = (id: string) => {
    Alert.alert('Delete Family', 'Are you sure?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: () => deleteMember(id) },
    ]);
  };

  const renderServantItem = ({ item }: { item: Servant }) => {
    const balance = getPersonBalance(item.id, true);
    return (
      <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.card, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
        <View style={styles.cardHeader}>
          <TouchableOpacity
            style={styles.cardMain}
            onPress={() => navigation.navigate('ServantDetail', { servantId: item.id })}
          >
            <LinearGradient
              colors={['#3B82F6', '#2563EB']}
              style={styles.avatarContainer}
            >
              <Text style={styles.avatarText}>{item.name.charAt(0).toUpperCase()}</Text>
            </LinearGradient>
            <View style={styles.infoContainer}>
              <Text style={[styles.name, { color: colors.textDark }]}>{item.name}</Text>
              <Text style={[styles.role, { color: isDarkMode ? 'rgba(255,255,255,0.4)' : 'rgba(0,0,0,0.4)' }]}>{item.role.toUpperCase()}</Text>
            </View>
          </TouchableOpacity>
          <TouchableOpacity onPress={() => handleDeleteServant(item.id)} style={styles.deleteBtn}>
            <Ionicons name="trash-outline" size={18} color={isDarkMode ? "rgba(255,255,255,0.3)" : "rgba(0,0,0,0.3)"} />
          </TouchableOpacity>
        </View>

        {isAdmin && item.invite_code && (
          <View style={[styles.inviteBox, { backgroundColor: isDarkMode ? 'rgba(59, 130, 246, 0.05)' : 'rgba(59, 130, 246, 0.05)', borderColor: 'rgba(59, 130, 246, 0.1)' }]}>
            <View style={styles.inviteInfo}>
              <MaterialCommunityIcons name="shield-key-outline" size={14} color="#3B82F6" />
              <Text style={styles.inviteCodeText}>{item.invite_code}</Text>
            </View>
            <TouchableOpacity
              style={styles.shareBtnMini}
              onPress={() => handleShareCode(item.invite_code!, item.name)}
            >
              <Ionicons name="share-social-outline" size={14} color="white" />
            </TouchableOpacity>
          </View>
        )}

        <View style={styles.actionRow}>
          <View>
            <Text style={[styles.balanceLabel, { color: isDarkMode ? 'rgba(255,255,255,0.4)' : 'rgba(0,0,0,0.4)' }]}>CURRENT BALANCE</Text>
            <Text style={[styles.balanceAmount, { color: balance < 0 ? '#EF4444' : '#10B981' }]}>
              {currencySymbol}{balance.toLocaleString()}
            </Text>
          </View>
          <TouchableOpacity
            style={styles.giveBtn}
            onPress={() => navigation.navigate('AddTransaction', { type: 'transfer', servantId: item.id })}
          >
            <LinearGradient colors={['#3B82F6', '#1D4ED8']} style={styles.giveBtnGradient}>
              <Ionicons name="send" size={14} color="white" style={{ marginRight: 6 }} />
              <Text style={styles.giveBtnText}>Give Money</Text>
            </LinearGradient>
          </TouchableOpacity>
        </View>
      </BlurView>
    );
  };

  const renderMemberItem = ({ item }: { item: Member }) => {
    const balance = getPersonBalance(item.id, false);
    return (
      <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.card, { borderColor: isDarkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
        <View style={styles.cardHeader}>
          <TouchableOpacity
            style={styles.cardMain}
            onPress={() => navigation.navigate('AddMember', { memberId: item.id })}
          >
            <View style={[styles.memberIconContainer, { backgroundColor: isDarkMode ? 'rgba(59, 130, 246, 0.1)' : 'rgba(59, 130, 246, 0.05)' }]}>
              <Ionicons name="person" size={20} color="#3B82F6" />
            </View>
            <View style={styles.infoContainer}>
              <Text style={[styles.name, { color: colors.textDark }]}>{item.name}</Text>
              <Text style={[styles.role, { color: colors.textGrey }]}>HOUSEHOLD MEMBER</Text>
              {isAdmin && item.invite_code && (
                <Text style={styles.memberCode}>AUTH: {item.invite_code}</Text>
              )}
            </View>
          </TouchableOpacity>

          <View style={styles.memberActions}>
            {isAdmin && item.invite_code && (
              <TouchableOpacity
                onPress={() => handleShareCode(item.invite_code!, item.name)}
                style={styles.memberActionBtn}
              >
                <Ionicons name="share-outline" size={18} color={colors.textGrey} />
              </TouchableOpacity>
            )}
            <TouchableOpacity onPress={() => handleDeleteMember(item.id)} style={styles.memberActionBtn}>
              <Ionicons name="trash-outline" size={18} color="#EF4444" />
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.actionRow}>
          <View>
            <Text style={[styles.balanceLabel, { color: isDarkMode ? 'rgba(255,255,255,0.4)' : 'rgba(0,0,0,0.4)' }]}>CURRENT BALANCE</Text>
            <Text style={[styles.balanceAmount, { color: balance < 0 ? '#EF4444' : '#10B981' }]}>
              {currencySymbol}{balance.toLocaleString()}
            </Text>
          </View>
          <TouchableOpacity
            style={styles.giveBtn}
            onPress={() => navigation.navigate('AddTransaction', { type: 'transfer', memberId: item.id })}
          >
            <LinearGradient colors={['#3B82F6', '#1D4ED8']} style={styles.giveBtnGradient}>
              <Ionicons name="send" size={14} color="white" style={{ marginRight: 6 }} />
              <Text style={styles.giveBtnText}>Give Money</Text>
            </LinearGradient>
          </TouchableOpacity>
        </View>
      </BlurView>
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <StatusBar barStyle={isDarkMode ? "light-content" : "dark-content"} />
      <LinearGradient colors={isDarkMode ? ['#0F172A', '#1E293B'] : [colors.background, colors.background]} style={StyleSheet.absoluteFill} />

      {/* Custom Header */}
      <View style={styles.header}>
        <Text style={[styles.headerTitle, { color: colors.textDark }]}>People</Text>
        <TouchableOpacity
          style={styles.addBtn}
          onPress={() => {
            if (activeTab === 'staff') {
              navigation.navigate('AddServant', {});
            } else {
              navigation.navigate('AddMember', {});
            }
          }}
        >
          <Ionicons name="person-add" size={20} color="white" />
        </TouchableOpacity>
      </View>

      <View style={styles.tabsWrapper}>
        <BlurView intensity={20} tint={isDarkMode ? "dark" : "light"} style={[styles.tabsContainer, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : 'rgba(255,255,255,0.5)' }]}>
          <TouchableOpacity
            style={[styles.tab, activeTab === 'staff' && (isDarkMode ? styles.activeTabDark : styles.activeTabLight)]}
            onPress={() => setActiveTab('staff')}
          >
            <Text style={[styles.tabText, { color: activeTab === 'staff' ? colors.primary : colors.textGrey }]}>Staff</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.tab, activeTab === 'family' && (isDarkMode ? styles.activeTabDark : styles.activeTabLight)]}
            onPress={() => setActiveTab('family')}
          >
            <Text style={[styles.tabText, { color: activeTab === 'family' ? colors.primary : colors.textGrey }]}>Family</Text>
          </TouchableOpacity>
        </BlurView>
      </View>

      {activeTab === 'staff' ? (
        <FlatList
          data={servants}
          keyExtractor={(item) => item.id}
          renderItem={renderServantItem}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <MaterialCommunityIcons name="account-group-outline" size={64} color={isDarkMode ? "rgba(255,255,255,0.05)" : "rgba(0,0,0,0.05)"} />
              <Text style={[styles.emptyText, { color: colors.textDark }]}>No Staff Members</Text>
              <Text style={[styles.emptySubText, { color: colors.textGrey }]}>You haven&apos;t added any staff yet.</Text>
            </View>
          }
        />
      ) : (
        <FlatList
          data={members}
          keyExtractor={(item) => item.id}
          renderItem={renderMemberItem}
          contentContainerStyle={styles.list}
          showsVerticalScrollIndicator={false}
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <MaterialCommunityIcons name="home-account" size={64} color={isDarkMode ? "rgba(255,255,255,0.05)" : "rgba(0,0,0,0.05)"} />
              <Text style={[styles.emptyText, { color: colors.textDark }]}>No Family Members</Text>
              <Text style={[styles.emptySubText, { color: colors.textGrey }]}>You haven&apos;t added any family members yet.</Text>
            </View>
          }
        />
      )}
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
  addBtn: {
    width: 44,
    height: 44,
    borderRadius: 16,
    backgroundColor: '#3B82F6',
    justifyContent: 'center',
    alignItems: 'center',
    ...SHADOWS.premium,
  },
  tabsWrapper: {
    paddingHorizontal: 20,
    marginBottom: 24,
  },
  tabsContainer: {
    flexDirection: 'row',
    padding: 4,
    borderRadius: 20,
    borderWidth: 1,
    overflow: 'hidden',
  },
  tab: {
    flex: 1,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 16,
  },
  activeTabDark: {
    backgroundColor: 'rgba(255,255,255,0.1)',
  },
  activeTabLight: {
    backgroundColor: 'rgba(59, 130, 246, 0.1)',
  },
  tabText: {
    fontWeight: '800',
    fontSize: 14,
    letterSpacing: 0.5,
  },
  list: { padding: 20, paddingBottom: 120 },
  card: {
    borderRadius: 32,
    marginBottom: 16,
    borderWidth: 1,
    overflow: 'hidden',
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 20,
  },
  cardMain: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  },
  avatarContainer: {
    width: 60,
    height: 60,
    borderRadius: 20,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  avatarText: {
    color: '#FFF',
    fontSize: 24,
    fontWeight: '900',
  },
  infoContainer: {
    flex: 1,
  },
  name: {
    fontSize: 18,
    fontWeight: '800',
    marginBottom: 4,
  },
  role: {
    fontSize: 10,
    fontWeight: '900',
    letterSpacing: 1.5,
  },
  deleteBtn: {
    padding: 8,
  },
  inviteBox: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginHorizontal: 20,
    marginBottom: 20,
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 16,
    borderWidth: 1,
  },
  inviteInfo: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  inviteCodeText: {
    fontSize: 15,
    fontWeight: '900',
    color: '#3B82F6',
    letterSpacing: 2,
    marginLeft: 10,
  },
  shareBtnMini: {
    width: 32,
    height: 32,
    borderRadius: 10,
    backgroundColor: '#3B82F6',
    justifyContent: 'center',
    alignItems: 'center',
  },
  memberCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderRadius: 24,
    marginBottom: 12,
    borderWidth: 1,
    overflow: 'hidden',
  },
  memberMain: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
  },
  memberIconContainer: {
    width: 50,
    height: 50,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  memberCode: {
    fontSize: 10,
    fontWeight: '900',
    color: '#3B82F6',
    marginTop: 6,
    letterSpacing: 1,
  },
  memberActions: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  memberActionBtn: {
    padding: 8,
    marginLeft: 4,
  },
  actionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    paddingTop: 0,
  },
  balanceLabel: {
    fontSize: 9,
    fontWeight: '900',
    letterSpacing: 1,
    marginBottom: 4,
  },
  balanceAmount: {
    fontSize: 22,
    fontWeight: '900',
  },
  giveBtn: {
    overflow: 'hidden',
    borderRadius: 16,
    ...SHADOWS.small,
  },
  giveBtnGradient: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 16,
  },
  giveBtnText: {
    color: '#FFF',
    fontWeight: '800',
    fontSize: 13,
  },
  emptyContainer: {
    alignItems: 'center',
    marginTop: 100,
    paddingHorizontal: 40,
  },
  emptyText: {
    fontSize: 20,
    fontWeight: '900',
    marginTop: 20,
  },
  emptySubText: {
    fontSize: 14,
    marginTop: 8,
    textAlign: 'center',
    lineHeight: 20,
  }
});
