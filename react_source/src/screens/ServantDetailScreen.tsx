import React, { useLayoutEffect } from 'react';
import { View, Text, SectionList, TouchableOpacity, StyleSheet, Alert, StatusBar, Platform, ScrollView } from 'react-native';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';
import { Ionicons } from '@expo/vector-icons';
import { useAuthStore } from '../features/auth/store';
import { usePeopleStore } from '../features/people/store';
import { useTransactionStore } from '../features/transactions/store';
import { useCategoryStore } from '../features/categories/store';
import { useSettingsStore } from '../features/settings/store';
import { Transaction, RootStackParamList } from '../types';
import { format, parseISO } from 'date-fns';
import { SHADOWS, SIZES } from '../constants/theme';
import { useTheme } from '../hooks/useTheme';
import { useServantWallets } from '../hooks/useServantWallets';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';

type ServantDetailRouteProp = RouteProp<RootStackParamList, 'ServantDetail'>;
type NavigationProp = StackNavigationProp<RootStackParamList>;

export default function ServantDetailScreen() {
  const navigation = useNavigation<NavigationProp>();
  const route = useRoute<ServantDetailRouteProp>();
  const { colors, isDarkMode } = useTheme();
  const { servantId } = route.params;

  const { profile } = useAuthStore();
  const { servants } = usePeopleStore();
  const { transactions } = useTransactionStore();
  const { categories } = useCategoryStore();
  const { currencySymbol } = useSettingsStore();

  const { wallets } = useServantWallets(servantId);

  const servant = servants.find(s => s.id === servantId);
  const isAdmin = profile?.role === 'admin';

  useLayoutEffect(() => {
    navigation.setOptions({
      headerTitle: servant?.name || 'Staff Profile',
      headerStyle: { backgroundColor: colors.background, shadowColor: 'transparent', elevation: 0 },
      headerTitleStyle: { color: colors.textDark, fontWeight: '900', fontSize: 18 },
      headerTintColor: colors.textDark,
      headerRight: () => isAdmin ? (
        <TouchableOpacity
          style={{ marginRight: 20 }}
          onPress={() => navigation.navigate('AddServant', { servantId })}
        >
          <Ionicons name="create-outline" size={24} color={colors.primary} />
        </TouchableOpacity>
      ) : null
    });
  }, [navigation, servant, servantId, isAdmin, colors]);

  if (!servant) {
    return (
      <View style={[styles.container, { backgroundColor: colors.background, justifyContent: 'center', alignItems: 'center' }]}>
        <Text style={{ color: colors.textDark }}>Profile not found</Text>
      </View>
    );
  }

  const servantTransactions = transactions.filter(t => t.servantId === servantId);

  const groupedTransactions = servantTransactions
    .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
    .reduce((acc, transaction) => {
      const date = parseISO(transaction.date);
      const title = format(date, 'MMMM yyyy');
      const existing = acc.find((group) => group.title === title);
      if (existing) {
        existing.data.push(transaction);
      } else {
        acc.push({ title, data: [transaction] });
      }
      return acc;
    }, [] as { title: string; data: Transaction[] }[]);

  const getCategory = (id?: string) => categories.find((c) => c.id === id);

  const renderItem = ({ item }: { item: Transaction }) => {
    const category = getCategory(item.categoryId);
    const date = format(parseISO(item.date), 'MMM dd');

    let iconName: any = 'help';
    let iconColor = colors.textGrey;
    let amountColor = colors.textDark;
    let amountPrefix = '';

    if (item.type === 'expense') {
      iconName = category?.icon || 'pricetag';
      iconColor = category?.color || '#EF4444';
      amountColor = '#EF4444';
      amountPrefix = '-';
    } else if (item.type === 'transfer') {
      iconName = 'cash';
      iconColor = '#10B981';
      amountColor = '#10B981';
      amountPrefix = '+';
    }

    return (
      <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.card, { borderColor: isDarkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
        <TouchableOpacity
          style={styles.cardInner}
          onPress={() => navigation.navigate('AddTransaction', { transactionId: item.id })}
        >
          <View style={styles.iconContainer}>
            <View style={[styles.iconBg, { backgroundColor: `${iconColor}20` }]}>
              <Ionicons name={iconName} size={20} color={iconColor} />
            </View>
          </View>
          <View style={styles.content}>
            <Text style={[styles.desc, { color: colors.textDark }]} numberOfLines={1}>
              {item.description || (item.type === 'expense' ? category?.name : 'Income')}
            </Text>
            <Text style={[styles.meta, { color: colors.textGrey }]}>
              {date} • {item.type === 'transfer' ? 'INCOME' : item.type.toUpperCase()}
            </Text>
          </View>
          <View style={{ alignItems: 'flex-end' }}>
            <Text style={[styles.amount, { color: amountColor }]}>{amountPrefix}{currencySymbol} {item.amount.toLocaleString()}</Text>
            <Ionicons name="chevron-forward" size={14} color={colors.textGrey} style={{ marginTop: 4 }} />
          </View>
        </TouchableOpacity>
      </BlurView>
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <StatusBar barStyle={isDarkMode ? "light-content" : "dark-content"} />
      <LinearGradient colors={isDarkMode ? ['#0F172A', '#1E293B'] : [colors.background, colors.background]} style={StyleSheet.absoluteFill} />

      <View style={styles.headerCardWrapper}>
        <BlurView intensity={25} tint={isDarkMode ? "dark" : "light"} style={[styles.headerCard, { borderColor: isDarkMode ? 'rgba(255, 255, 255, 0.1)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
          <View style={styles.headerRow}>
            <View>
              <Text style={[styles.roleLabel, { color: colors.textGrey }]}>{servant.role.toUpperCase()}</Text>
              <Text style={[styles.balanceLabel, { color: colors.textGrey }]}>CURRENT BALANCE</Text>
              <Text style={[styles.balance, { color: (servant.balance || 0) < 0 ? '#EF4444' : colors.textDark }]}>
                {(servant.balance || 0) < 0 ? '-' : ''}{currencySymbol}{Math.abs(servant.balance || 0).toLocaleString()}
              </Text>
            </View>
            <TouchableOpacity
              style={styles.giveBtn}
              onPress={() => navigation.navigate('AddTransaction', { type: 'transfer', servantId: servant.id })}
            >
              <LinearGradient colors={['#3B82F6', '#1D4ED8']} style={styles.giveBtnGradient}>
                <Ionicons name="cash-outline" size={18} color="white" style={{ marginRight: 6 }} />
                <Text style={styles.giveBtnText}>Give Money</Text>
              </LinearGradient>
            </TouchableOpacity>
          </View>
          {servant.phoneNumber && (
            <View style={styles.infoRow}>
              <Ionicons name="call-outline" size={14} color={colors.textGrey} />
              <Text style={[styles.infoText, { color: colors.textGrey }]}>{servant.phoneNumber}</Text>
            </View>
          )}
        </BlurView>
      </View>

      {wallets.length > 0 && (
        <View style={styles.walletsContainer}>
          <Text style={[styles.sectionHeader, { color: colors.textGrey, marginLeft: 20, marginBottom: 12 }]}>WALLET BREAKDOWN</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={{ paddingHorizontal: 20, paddingBottom: 20 }}>
            {wallets.map(wallet => (
              <BlurView key={wallet.id} intensity={15} tint={isDarkMode ? "dark" : "light"} style={[styles.walletCard, { borderColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'rgba(0,0,0,0.2)' : 'rgba(255,255,255,0.6)' }]}>
                <Text style={[styles.walletName, { color: colors.textDark }]}>{wallet.name}</Text>
                <Text style={[styles.walletBalance, { color: wallet.balance < 0 ? '#EF4444' : colors.textDark }]}>
                  {currencySymbol}{wallet.balance.toLocaleString()}
                </Text>
              </BlurView>
            ))}
          </ScrollView>
        </View>
      )}

      <SectionList
        sections={groupedTransactions}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        renderSectionHeader={({ section: { title } }) => (
          <View style={[styles.sectionHeaderContainer, { borderBottomColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' }]}>
            <Text style={[styles.sectionHeader, { color: colors.textGrey }]}>{title.toUpperCase()}</Text>
          </View>
        )}
        contentContainerStyle={styles.list}
        showsVerticalScrollIndicator={false}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <Ionicons name="receipt-outline" size={60} color={isDarkMode ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)"} />
            <Text style={[styles.emptyText, { color: colors.textGrey }]}>No transactions yet</Text>
          </View>
        }
        ListHeaderComponent={<Text style={[styles.historyTitle, { color: colors.textDark }]}>Recent Activity</Text>}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  headerCardWrapper: {
    padding: 20,
    paddingBottom: 10,
  },
  walletsContainer: {
    marginBottom: 10,
  },
  walletCard: {
    padding: 16,
    borderRadius: 20,
    borderWidth: 1,
    marginRight: 12,
    minWidth: 120,
    alignItems: 'center',
    justifyContent: 'center',
  },
  walletName: {
    fontSize: 12,
    fontWeight: '700',
    marginBottom: 4,
    textAlign: 'center',
  },
  walletBalance: {
    fontSize: 16,
    fontWeight: '900',
  },
  headerCard: {
    padding: 24,
    borderRadius: 32,
    borderWidth: 1,
    ...SHADOWS.large,
  },
  headerRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  roleLabel: {
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 1.5,
    marginBottom: 8,
  },
  balanceLabel: {
    fontSize: 10,
    fontWeight: '600',
    textTransform: 'uppercase',
    letterSpacing: 2,
    marginBottom: 2,
  },
  balance: {
    fontSize: 34,
    fontWeight: '900',
    letterSpacing: -1,
  },
  giveBtn: {
    borderRadius: 16,
    overflow: 'hidden',
    ...SHADOWS.premium,
  },
  giveBtnGradient: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 12,
    paddingHorizontal: 16,
  },
  giveBtnText: {
    color: 'white',
    fontWeight: '800',
    fontSize: 14,
  },
  infoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 8,
  },
  infoText: {
    marginLeft: 6,
    fontSize: 14,
    fontWeight: '500',
  },
  historyTitle: {
    fontSize: 18,
    fontWeight: '900',
    marginBottom: 20,
    marginTop: 10,
    letterSpacing: 0.5,
  },
  list: { paddingHorizontal: 20, paddingBottom: 50 },
  sectionHeaderContainer: {
    paddingVertical: 12,
    borderBottomWidth: 1,
    marginBottom: 16,
    marginTop: 10,
  },
  sectionHeader: {
    fontSize: 12,
    fontWeight: '800',
    letterSpacing: 1,
  },
  card: {
    borderRadius: 24,
    marginBottom: 12,
    borderWidth: 1,
    overflow: 'hidden',
  },
  cardInner: {
    padding: 16,
    flexDirection: 'row',
    alignItems: 'center',
  },
  iconContainer: { marginRight: 16 },
  iconBg: {
    width: 44,
    height: 44,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
  },
  content: { flex: 1 },
  desc: { fontSize: 16, fontWeight: '700' },
  meta: { fontSize: 13, fontWeight: '500', marginTop: 3 },
  amount: { fontSize: 16, fontWeight: '900' },
  emptyContainer: { alignItems: 'center', marginTop: 60 },
  emptyText: { fontSize: 15, fontWeight: '600', marginTop: 16 },
});
