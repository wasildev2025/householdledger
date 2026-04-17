import React, { useLayoutEffect } from 'react';
import { View, Text, SectionList, TouchableOpacity, StyleSheet, StatusBar, Platform, RefreshControl } from 'react-native';
import { useNavigation } from '@react-navigation/native';
import { StackNavigationProp } from '@react-navigation/stack';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { Transaction, RootStackParamList } from '../types';
import { format, parseISO, isWithinInterval, startOfDay, endOfDay } from 'date-fns';
import DateTimePicker from '@react-native-community/datetimepicker';
import { SIZES, SHADOWS, COLORS } from '../constants/theme';
import { useTheme } from '../hooks/useTheme';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';

import { useAuthStore } from '../features/auth/store';
import { useTransactionStore } from '../features/transactions/store';
import { usePeopleStore } from '../features/people/store';
import { useCategoryStore } from '../features/categories/store';
import { useSettingsStore } from '../features/settings/store';

type NavigationProp = StackNavigationProp<RootStackParamList>;

export default function TransactionListScreen() {
  const { colors, isDarkMode } = useTheme();
  const navigation = useNavigation<NavigationProp>();

  const { profile } = useAuthStore();
  const { transactions, fetchPage } = useTransactionStore();
  const { servants, members } = usePeopleStore();
  const { categories } = useCategoryStore();
  const { currencySymbol } = useSettingsStore();

  const [refreshing, setRefreshing] = React.useState(false);
  const [filter, setFilter] = React.useState<'all' | 'admin' | 'servant' | 'member'>('all');
  const [startDate, setStartDate] = React.useState<Date | null>(null);
  const [endDate, setEndDate] = React.useState<Date | null>(null);
  const [showStartPicker, setShowStartPicker] = React.useState(false);
  const [showEndPicker, setShowEndPicker] = React.useState(false);

  const onRefresh = React.useCallback(async () => {
    setRefreshing(true);
    try {
      await fetchPage(true);
    } finally {
      setRefreshing(false);
    }
  }, [fetchPage]);

  const isServant = profile?.role === 'servant';
  const isMember = profile?.role === 'member';

  const getCategory = (id?: string) => categories.find((c) => c.id === id);
  const getServantName = (id?: string | null) => {
    if (!id) return null;
    const s = servants.find((servant) => servant.id === id);
    return s ? s.name : 'Unknown';
  };
  const getMemberName = (id?: string | null) => {
    if (!id) return null;
    const m = members.find((member) => member.id === id);
    return m ? m.name : 'Unknown';
  };

  const filteredTransactions = React.useMemo(() => {
    let result = transactions;

    if (isServant && profile?.servantId) {
      result = result.filter(t => t.servantId === profile.servantId);
    } else if (isMember && profile?.memberId) {
      result = result.filter(t => t.memberId === profile.memberId);
    } else {
      if (filter === 'admin') {
        result = result.filter(t => !t.servantId && !t.memberId && t.type !== 'transfer');
      } else if (filter === 'servant') {
        result = result.filter(t => !!t.servantId);
      } else if (filter === 'member') {
        result = result.filter(t => !!t.memberId);
      }
    }

    if (startDate && endDate) {
      result = result.filter(t => {
        const txDate = parseISO(t.date);
        return isWithinInterval(txDate, { start: startOfDay(startDate), end: endOfDay(endDate) });
      });
    }

    return result;
  }, [transactions, isServant, isMember, filter, profile, startDate, endDate]);

  const groupedTransactions = filteredTransactions
    .sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime())
    .reduce((acc, transaction) => {
      const date = parseISO(transaction.date);
      const title = format(date, 'EEE, d MMM yyyy');
      const existing = acc.find((group) => group.title === title);
      if (existing) {
        existing.data.push(transaction as any);
      } else {
        acc.push({ title, data: [transaction as any] });
      }
      return acc;
    }, [] as { title: string; data: Transaction[] }[]);

  const renderItem = ({ item }: { item: Transaction }) => {
    const category = getCategory(item.categoryId);

    let iconName: any = 'receipt-outline';
    let iconColor = '#3B82F6';
    let amountColor = COLORS.white;
    let amountPrefix = '';

    if (item.type === 'expense') {
      iconName = (category?.icon as any) || 'pricetag-outline';
      iconColor = category?.color || '#EF4444';
      amountColor = '#EF4444';
      amountPrefix = '-';
    } else if (item.type === 'income') {
      iconName = 'cash-outline';
      iconColor = '#10B981';
      amountColor = '#10B981';
      amountPrefix = '+';
    } else if (item.type === 'transfer') {
      iconName = 'swap-horizontal-outline';
      iconColor = '#3B82F6';
      amountColor = '#3B82F6';
    }

    return (
      <TouchableOpacity
        style={styles.card}
        activeOpacity={0.7}
        onPress={() => navigation.navigate('AddTransaction', { transactionId: item.id })}
      >
        <View style={[styles.iconContainer, { backgroundColor: `${iconColor}15` }]}>
          <Ionicons name={iconName} size={22} color={iconColor} />
        </View>
        <View style={styles.content}>
          <Text style={styles.desc} numberOfLines={1}>
            {item.description || (item.type === 'expense' ? category?.name : item.type)}
          </Text>
          <Text style={styles.meta}>
            {item.type === 'transfer'
              ? `To: ${getServantName(item.servantId) || getMemberName(item.memberId) || 'Unknown'}`
              : `By: ${getServantName(item.servantId) || getMemberName(item.memberId) || 'Owner'}`}
          </Text>
        </View>
        <View style={{ alignItems: 'flex-end' }}>
          <Text style={[styles.amount, { color: amountColor }]}>
            {amountPrefix}{currencySymbol} {item.amount.toLocaleString()}
          </Text>
          <Ionicons name="chevron-forward" size={12} color="rgba(255,255,255,0.2)" style={{ marginTop: 4 }} />
        </View>
      </TouchableOpacity>
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <StatusBar barStyle={isDarkMode ? "light-content" : "dark-content"} />
      <LinearGradient colors={isDarkMode ? ['#0F172A', '#1E293B'] : [colors.background, colors.background]} style={StyleSheet.absoluteFill} />

      <View style={styles.header}>
        <Text style={[styles.headerTitle, { color: colors.textDark }]}>History</Text>
        <TouchableOpacity
          style={styles.addBtn}
          onPress={() => navigation.navigate('AddTransaction', {})}
        >
          <Ionicons name="add" size={24} color="white" />
        </TouchableOpacity>
      </View>

      {!isServant && (
        <View style={styles.filterContainer}>
          <TouchableOpacity
            style={[styles.filterBtn, filter === 'all' && styles.filterBtnActive]}
            onPress={() => setFilter('all')}
          >
            <Text style={[styles.filterText, filter === 'all' && styles.filterTextActive]}>All</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.filterBtn, filter === 'admin' && styles.filterBtnActive]}
            onPress={() => setFilter('admin')}
          >
            <Text style={[styles.filterText, filter === 'admin' && styles.filterTextActive]}>My Expenses</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.filterBtn, filter === 'servant' && styles.filterBtnActive]}
            onPress={() => setFilter('servant')}
          >
            <Text style={[styles.filterText, filter === 'servant' && styles.filterTextActive]}>Staff</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.filterBtn, filter === 'member' && styles.filterBtnActive]}
            onPress={() => setFilter('member')}
          >
            <Text style={[styles.filterText, filter === 'member' && styles.filterTextActive]}>Family</Text>
          </TouchableOpacity>
        </View>
      )}

      <View style={styles.dateFilterContainer}>
        <TouchableOpacity style={styles.dateBtn} onPress={() => setShowStartPicker(true)}>
          <Ionicons name="calendar-outline" size={16} color={colors.textGrey} />
          <Text style={[styles.dateText, { color: startDate ? colors.textDark : colors.textGrey }]}>
            {startDate ? format(startDate, 'dd MMM yyyy') : 'Start Date'}
          </Text>
        </TouchableOpacity>
        <Text style={{ color: colors.textGrey, marginHorizontal: 8 }}>-</Text>
        <TouchableOpacity style={styles.dateBtn} onPress={() => setShowEndPicker(true)}>
          <Ionicons name="calendar-outline" size={16} color={colors.textGrey} />
          <Text style={[styles.dateText, { color: endDate ? colors.textDark : colors.textGrey }]}>
            {endDate ? format(endDate, 'dd MMM yyyy') : 'End Date'}
          </Text>
        </TouchableOpacity>
        {(startDate || endDate) && (
          <TouchableOpacity
            style={styles.clearDateBtn}
            onPress={() => { setStartDate(null); setEndDate(null); }}
          >
            <Ionicons name="close-circle" size={18} color="#EF4444" />
          </TouchableOpacity>
        )}
      </View>

      {showStartPicker && (
        <DateTimePicker
          value={startDate || new Date()}
          mode="date"
          display="default"
          onChange={(event, date) => {
            setShowStartPicker(Platform.OS === 'ios');
            if (date) setStartDate(date);
          }}
        />
      )}
      {showEndPicker && (
        <DateTimePicker
          value={endDate || new Date()}
          mode="date"
          display="default"
          minimumDate={startDate || undefined}
          onChange={(event, date) => {
            setShowEndPicker(Platform.OS === 'ios');
            if (date) setEndDate(date);
          }}
        />
      )}

      <SectionList
        sections={groupedTransactions}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        renderSectionHeader={({ section: { title } }) => (
          <BlurView intensity={20} tint={isDarkMode ? "dark" : "light"} style={[styles.sectionHeaderBlur, { backgroundColor: isDarkMode ? 'rgba(15, 23, 42, 0.8)' : 'rgba(255, 255, 255, 0.8)' }]}>
            <Text style={[styles.sectionHeaderText, { color: colors.textGrey }]}>{title.toUpperCase()}</Text>
          </BlurView>
        )}
        contentContainerStyle={styles.list}
        showsVerticalScrollIndicator={false}
        stickySectionHeadersEnabled={true}
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <MaterialCommunityIcons name="receipt-text-outline" size={64} color={isDarkMode ? "rgba(255,255,255,0.05)" : "rgba(0,0,0,0.05)"} />
            <Text style={[styles.emptyText, { color: colors.textDark }]}>No Transactions</Text>
            <Text style={[styles.emptySubText, { color: colors.textGrey }]}>Your transaction history is empty.</Text>
          </View>
        }
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={isDarkMode ? "#ffffff" : "#000000"} />
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    paddingTop: Platform.OS === 'ios' ? 60 : 40,
    paddingHorizontal: 20,
    paddingBottom: 16,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  filterContainer: {
    flexDirection: 'row',
    paddingHorizontal: 20,
    marginBottom: 16,
  },
  filterBtn: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 20,
    marginRight: 8,
    backgroundColor: 'rgba(150, 150, 150, 0.1)',
  },
  dateFilterContainer: {
    flexDirection: 'row',
    paddingHorizontal: 20,
    alignItems: 'center',
    marginBottom: 16,
  },
  dateBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(150, 150, 150, 0.1)',
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 12,
  },
  dateText: {
    fontSize: 13,
    fontWeight: '600',
    marginLeft: 6,
  },
  clearDateBtn: {
    padding: 8,
    marginLeft: 8,
  },
  filterBtnActive: {
    backgroundColor: '#3B82F6',
  },
  filterText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#888',
  },
  filterTextActive: {
    color: 'white',
    fontWeight: '700',
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
  list: { paddingBottom: 120 },
  sectionHeaderBlur: {
    paddingHorizontal: 20,
    paddingVertical: 12,
  },
  sectionHeaderText: {
    fontSize: 11,
    fontWeight: '900',
    letterSpacing: 2,
  },
  card: {
    backgroundColor: '#FFFFFF', // Clean White for Light Mode
    padding: 20,
    marginHorizontal: 20,
    borderRadius: 24,
    marginBottom: 16,
    flexDirection: 'row',
    alignItems: 'center',
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.08,
    shadowRadius: 16,
    elevation: 4,
    borderWidth: 1,
    borderColor: 'rgba(0,0,0,0.03)',
  },
  iconContainer: {
    width: 48,
    height: 48,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  content: { flex: 1 },
  desc: { fontSize: 16, fontWeight: '700' },
  meta: { fontSize: 12, marginTop: 4, fontWeight: '500' },
  amount: { fontSize: 16, fontWeight: '800' },
  emptyContainer: { alignItems: 'center', marginTop: 100, paddingHorizontal: 40 },
  emptyText: { fontSize: 20, fontWeight: '800', marginTop: 20 },
  emptySubText: { fontSize: 14, marginTop: 8, textAlign: 'center', lineHeight: 20 },
});
