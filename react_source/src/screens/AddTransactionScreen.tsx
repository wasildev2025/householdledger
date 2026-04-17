import React, { useState, useEffect } from 'react';
import { View, Text, TextInput, StyleSheet, TouchableOpacity, ScrollView, Alert, StatusBar, Platform } from 'react-native';
import { useNavigation, useRoute, RouteProp } from '@react-navigation/native';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { RootStackParamList, TransactionType } from '../types';
import { DateTimePickerEvent } from '../types/auth';
import { format, parseISO } from 'date-fns';
import DateTimePicker from '@react-native-community/datetimepicker';
import { SHADOWS, COLORS } from '../constants/theme';
import { useTheme } from '../hooks/useTheme';
import { useServantWallets } from '../hooks/useServantWallets';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';
import { aiService } from '../lib/aiService';
import * as ImagePicker from 'expo-image-picker';
import { VALIDATION, VALIDATION_MESSAGES } from '../constants/validation';
import { handleError } from '../lib/errorHandler';
import {
  A11Y_LABELS,
  A11Y_HINTS,
  accessibleButton,
  accessibleInput,
  formatCurrencyForA11y
} from '../lib/accessibility';

import { useAuthStore } from '../features/auth/store';
import { useTransactionStore } from '../features/transactions/store';
import { usePeopleStore } from '../features/people/store';
import { useCategoryStore } from '../features/categories/store';
import { useSettingsStore } from '../features/settings/store';

type AddTransactionRouteProp = RouteProp<RootStackParamList, 'AddTransaction'>;

export default function AddTransactionScreen() {
  const { colors, isDarkMode } = useTheme();
  const navigation = useNavigation();
  const route = useRoute<AddTransactionRouteProp>();
  const { transactionId, type: initialType, servantId: initialServantId, memberId: initialMemberId } = route.params || {};

  const { profile } = useAuthStore();
  const { transactions, addTransaction, updateTransaction, deleteTransaction } = useTransactionStore();
  const { categories } = useCategoryStore();
  const { servants } = usePeopleStore();
  const { currencySymbol } = useSettingsStore();

  const isServant = profile?.role === 'servant';
  const isMember = profile?.role === 'member';

  const [type, setType] = useState<TransactionType>(initialType || 'expense');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [servantId, setServantId] = useState<string | null>(initialServantId || (isServant ? profile?.servantId || null : null));
  const [date, setDate] = useState(new Date());
  const [isAnalyzing, setIsAnalyzing] = useState(false);

  // Wallet Logic
  const [selectedWalletMemberId, setSelectedWalletMemberId] = useState<string | null>(null); // null = Admin

  const { wallets } = useServantWallets(isServant ? profile?.servantId : null);

  const [showDatePicker, setShowDatePicker] = useState(false);

  useEffect(() => {
    if (transactionId) {
      const transaction = transactions.find((t) => t.id === transactionId);
      if (transaction) {
        setAmount(transaction.amount.toString());
        setDescription(transaction.description);
        setCategoryId(transaction.categoryId || '');
        setServantId(transaction.servantId || null);
        setDate(parseISO(transaction.date));
        setType(transaction.type);
        setSelectedWalletMemberId(transaction.memberId || null);
      }
    } else if (categories.length > 0 && !categoryId) {
      setCategoryId(categories[0].id);
    }

    if (!transactionId && initialMemberId) {
      setSelectedWalletMemberId(initialMemberId);
    }

    if (isServant && profile?.servantId) {
      setServantId(profile.servantId);
      setType('expense');
    }
  }, [transactionId, transactions, categories, isServant]);

  // Auto-switch to Transfer if Member selects a specific Servant (and hasn't explicitly set Expense)
  useEffect(() => {
    if (isMember && servantId && type === 'expense' && !transactionId) {
      setType('transfer');
    }
  }, [isMember, servantId]);

  const handleScanReceipt = async () => {
    const permissionResult = await ImagePicker.requestMediaLibraryPermissionsAsync();

    if (permissionResult.granted === false) {
      Alert.alert("Permission Required", "You need to allow access to your photos to scan receipts.");
      return;
    }

    const pickerResult = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      quality: 0.8,
    });

    if (pickerResult.canceled) return;

    setIsAnalyzing(true);
    try {
      const result = await aiService.analyzeReceipt(pickerResult.assets[0].uri, categories);
      setAmount(result.amount.toString());
      setDescription(result.description);
      setCategoryId(result.categoryId);
      Alert.alert("Scan Complete", "Transaction details have been pre-filled from your receipt.");
    } catch (error) {
      handleError(error, { context: 'Receipt analysis', customMessage: 'Could not analyze the receipt. Please enter details manually.' });
    } finally {
      setIsAnalyzing(false);
    }
  };

  const handleSave = () => {
    // Validate amount
    if (!amount || amount.trim() === '') {
      Alert.alert('Required', VALIDATION_MESSAGES.AMOUNT_REQUIRED);
      return;
    }

    const numAmount = parseFloat(amount);

    if (isNaN(numAmount)) {
      Alert.alert('Invalid', VALIDATION_MESSAGES.AMOUNT_INVALID);
      return;
    }

    if (numAmount <= VALIDATION.MIN_AMOUNT) {
      Alert.alert('Invalid', VALIDATION_MESSAGES.AMOUNT_TOO_SMALL);
      return;
    }

    if (numAmount > VALIDATION.MAX_AMOUNT) {
      Alert.alert('Invalid', VALIDATION_MESSAGES.AMOUNT_TOO_LARGE);
      return;
    }

    // Validate description length
    if (description.length > VALIDATION.MAX_DESCRIPTION_LENGTH) {
      Alert.alert('Invalid', VALIDATION_MESSAGES.DESCRIPTION_TOO_LONG);
      return;
    }

    // Validate category for expenses
    if (type === 'expense' && !categoryId) {
      Alert.alert('Required', VALIDATION_MESSAGES.CATEGORY_REQUIRED);
      return;
    }

    // Validate recipient for transfers
    if (type === 'transfer' && !servantId) {
      Alert.alert('Required', VALIDATION_MESSAGES.STAFF_REQUIRED);
      return;
    }

    // Validate Fund Source for Servant Expense
    let finalMemberId = isMember ? profile?.memberId : undefined;

    const transactionData = {
      amount: numAmount,
      description,
      categoryId: type === 'expense' ? categoryId : undefined,
      servantId: isServant ? profile?.servantId : servantId,
      memberId: finalMemberId, // Logic: For servant expense, this is the SOURCE. For member transfer/expense, this is the ACTOR.
      date: date.toISOString(),
      type,
    };

    const submitTransaction = (data: any) => {
      if (transactionId) {
        const existingTx = transactions.find(t => t.id === transactionId);
        if (isServant && existingTx?.type === 'transfer') {
          Alert.alert('Permission Denied', 'Staff members are not allowed to modify transfer records.');
          return;
        }
        updateTransaction(transactionId, data);
      } else {
        addTransaction(data);
      }
      navigation.goBack();
    };

    if (isServant && type === 'expense') {
      const wallet = wallets.find(w => w.memberId === selectedWalletMemberId);
      if (wallet && wallet.balance < numAmount) {
        Alert.alert(
          'Low Balance',
          `This will put your balance negative (current: ${currencySymbol} ${wallet.balance}). Proceed anyway?`,
          [
            { text: 'Cancel', style: 'cancel' },
            {
              text: 'Proceed',
              style: 'destructive',
              onPress: () => submitTransaction(transactionData),
            },
          ]
        );
        return;
      }
    }

    submitTransaction(transactionData);
  };

  const handleDelete = () => {
    if (transactionId) {
      const transaction = transactions.find((t) => t.id === transactionId);
      if (isServant && transaction?.type === 'transfer') {
        Alert.alert('Permission Denied', 'Staff members are not allowed to delete transfer records.');
        return;
      }

      Alert.alert('Delete Transaction', 'Are you sure you want to delete this record?', [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete', style: 'destructive', onPress: () => {
            deleteTransaction(transactionId);
            navigation.goBack();
          }
        },
      ]);
    }
  };

  const onDateChange = (event: DateTimePickerEvent, selectedDate?: Date) => {
    setShowDatePicker(false);
    if (selectedDate) setDate(selectedDate);
  };

  const getTypeLabel = (t: TransactionType) => {
    switch (t) {
      case 'expense': return 'Spending';
      case 'income': return 'Income';
      case 'transfer': return 'Transfer';
    }
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <StatusBar barStyle={isDarkMode ? "light-content" : "dark-content"} />
      <LinearGradient colors={isDarkMode ? ['#0F172A', '#1E293B'] : [colors.background, colors.background]} style={StyleSheet.absoluteFill} />

      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={[styles.backBtn, { backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' }]}>
          <Ionicons name="chevron-back" size={24} color={colors.textDark} />
        </TouchableOpacity>
        <Text style={[styles.headerTitle, { color: colors.textDark }]}>{transactionId ? 'Edit Transaction' : 'Add Transaction'}</Text>
        {type === 'expense' && !transactionId ? (
          <TouchableOpacity
            onPress={handleScanReceipt}
            style={[styles.scanBtn, { backgroundColor: isDarkMode ? 'rgba(59, 130, 246, 0.1)' : 'rgba(59, 130, 246, 0.05)' }]}
            disabled={isAnalyzing}
            {...accessibleButton('Scan Receipt', 'Analyze receipt image to auto-fill details')}
          >
            <Ionicons name="scan-outline" size={18} color="#3B82F6" style={{ marginRight: 6 }} />
            <Text style={{ color: '#3B82F6', fontWeight: '800', fontSize: 13 }}>{isAnalyzing ? 'Analyzing...' : 'Scan'}</Text>
          </TouchableOpacity>
        ) : (transactionId && (!isServant || (transactions.find(t => t.id === transactionId)?.type !== 'transfer'))) ? (
          <TouchableOpacity
            onPress={handleDelete}
            style={styles.deleteBtn}
            {...accessibleButton('Delete Transaction', 'Permanently remove this transaction')}
          >
            <Ionicons name="trash-outline" size={20} color="#EF4444" />
          </TouchableOpacity>
        ) : (
          <View style={{ width: 44 }} />
        )}
      </View>

      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={styles.scrollContent}>
        {/* Type Selector */}
        {!isServant && (
          <View style={[styles.typeContainer, { backgroundColor: isDarkMode ? 'rgba(0,0,0,0.3)' : 'rgba(0,0,0,0.05)' }]}>
            {(['expense', 'income', 'transfer'] as TransactionType[]).map((t) => (
              <TouchableOpacity
                key={t}
                style={[styles.typeBtn, type === t && (isDarkMode ? styles.typeBtnActiveDark : styles.typeBtnActiveLight)]}
                onPress={() => setType(t)}
              >
                <Text style={[styles.typeBtnText, { color: type === t ? colors.textDark : colors.textGrey }]}>
                  {getTypeLabel(t).toUpperCase()}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        )}

        <BlurView intensity={10} tint={isDarkMode ? "dark" : "light"} style={[styles.mainCard, { borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
          <Text style={[styles.inputLabel, { color: colors.textGrey }]}>AMOUNT ({currencySymbol})</Text>
          <View style={styles.amountInputRow}>
            <TextInput
              style={[styles.amountInput, { color: colors.textDark }]}
              value={amount}
              onChangeText={setAmount}
              placeholder="0.00"
              placeholderTextColor={isDarkMode ? "rgba(255,255,255,0.1)" : "rgba(0,0,0,0.1)"}
              keyboardType="decimal-pad"
              autoFocus={!transactionId}
              {...accessibleInput('Amount', 'Enter the transaction amount')}
            />
          </View>

          <View style={[styles.divider, { backgroundColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' }]} />

          <Text style={[styles.inputLabel, { color: colors.textGrey }]}>DESCRIPTION</Text>
          <TextInput
            style={[styles.descriptionInput, { color: colors.textDark }]}
            value={description}
            onChangeText={async (text) => {
              setDescription(text);
              if (type === 'expense' && text.length > 2) {
                const suggestedId = await aiService.suggestCategory(text, categories);
                if (suggestedId && suggestedId !== categoryId) {
                  setCategoryId(suggestedId);
                }
              }
            }}
            placeholder="What was this for?"
            placeholderTextColor={isDarkMode ? "rgba(255,255,255,0.2)" : "rgba(0,0,0,0.2)"}
            multiline
            {...accessibleInput('Description', 'Enter a description for the transaction')}
          />
        </BlurView>

        <View style={styles.section}>
          <Text style={[styles.sectionTitle, { color: colors.textGrey }]}>Details</Text>

          <TouchableOpacity
            style={styles.dateSelector}
            onPress={() => setShowDatePicker(true)}
            {...accessibleButton('Change Date', `Current date is ${format(date, 'MMMM dd, yyyy')}`)}
          >
            <BlurView intensity={5} tint={isDarkMode ? "dark" : "light"} style={[styles.dateSelectorBlur, { borderColor: isDarkMode ? 'rgba(255,255,255,0.03)' : 'rgba(0,0,0,0.05)', backgroundColor: isDarkMode ? 'transparent' : 'rgba(255,255,255,0.8)' }]}>
              <Ionicons name="calendar-outline" size={20} color="#3B82F6" />
              <Text style={[styles.dateValue, { color: colors.textDark }]}>{format(date, 'MMMM dd, yyyy')}</Text>
              <Ionicons name="chevron-forward" size={16} color={colors.textGrey} />
            </BlurView>
          </TouchableOpacity>

          {showDatePicker && (
            <DateTimePicker
              value={date}
              mode="date"
              display={Platform.OS === 'ios' ? 'spinner' : 'default'}
              onChange={onDateChange}
              textColor={isDarkMode ? "white" : "black"}
            />
          )}

          {type === 'expense' && (
            <>
              <Text style={[styles.subSectionTitle, { color: colors.textGrey }]}>Category</Text>
              <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipRow}>
                {categories.map((cat) => (
                  <TouchableOpacity
                    key={cat.id}
                    style={[
                      styles.categoryChip,
                      { backgroundColor: isDarkMode ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.02)', borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' },
                      categoryId === cat.id && { backgroundColor: `${cat.color}30`, borderColor: cat.color }
                    ]}
                    onPress={() => setCategoryId(cat.id)}
                  >
                    <Ionicons
                      name={cat.icon as any}
                      size={16}
                      color={categoryId === cat.id ? cat.color : colors.textGrey}
                      style={{ marginRight: 8 }}
                    />
                    <Text style={[
                      styles.chipText,
                      { color: colors.textGrey },
                      categoryId === cat.id && { color: cat.color, fontWeight: '800' }
                    ]}>{cat.name}</Text>
                  </TouchableOpacity>
                ))}
                <TouchableOpacity
                  style={[
                    styles.categoryChip,
                    { backgroundColor: isDarkMode ? 'rgba(59, 130, 246, 0.1)' : 'rgba(59, 130, 246, 0.05)', borderColor: 'rgba(59, 130, 246, 0.2)', borderStyle: 'dashed' }
                  ]}
                  onPress={() => (navigation as any).navigate('AddCategory', {})}
                >
                  <Ionicons name="add-circle-outline" size={16} color="#3B82F6" style={{ marginRight: 8 }} />
                  <Text style={[styles.chipText, { color: '#3B82F6', fontWeight: '800' }]}>Add New</Text>
                </TouchableOpacity>
              </ScrollView>
            </>
          )}

          {/* Wallet Selector for Servants */}
          {isServant && type === 'expense' && (
            <View style={{ marginTop: 20 }}>
              <Text style={[styles.subSectionTitle, { color: colors.textGrey }]}>SOURCE OF FUNDS</Text>
              <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ flexDirection: 'row' }}>
                {wallets.map(wallet => {
                  const isSelected = selectedWalletMemberId === wallet.memberId;
                  return (
                    <TouchableOpacity
                      key={wallet.id}
                      onPress={() => setSelectedWalletMemberId(wallet.memberId)}
                      style={{
                        marginRight: 10,
                        padding: 12,
                        borderRadius: 16,
                        backgroundColor: isSelected ? (isDarkMode ? '#3B82F6' : '#2563EB') : (isDarkMode ? 'rgba(255,255,255,0.05)' : 'white'),
                        borderWidth: 1,
                        borderColor: isSelected ? 'transparent' : (isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)'),
                        minWidth: 100
                      }}
                    >
                      <Text style={{
                        color: isSelected ? 'white' : colors.textDark,
                        fontSize: 14,
                        fontWeight: '700',
                        marginBottom: 4
                      }}>
                        {wallet.name}
                      </Text>
                      <Text style={{
                        color: isSelected ? 'rgba(255,255,255,0.8)' : colors.textGrey,
                        fontSize: 12
                      }}>
                        {currencySymbol} {wallet.balance.toLocaleString()}
                      </Text>
                    </TouchableOpacity>
                  );
                })}
              </ScrollView>
            </View>
          )}

          {(type === 'transfer' || (type === 'expense' && !isServant)) && (
            <>
              <Text style={[styles.subSectionTitle, { color: colors.textGrey }]}>Assigned To</Text>
              <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipRow}>
                {servants.map((s) => (
                  <TouchableOpacity
                    key={s.id}
                    style={[
                      styles.entityChip,
                      { backgroundColor: isDarkMode ? 'rgba(255,255,255,0.02)' : 'rgba(0,0,0,0.02)', borderColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' },
                      servantId === s.id && styles.entityChipActive
                    ]}
                    onPress={() => setServantId(s.id)}
                  >
                    <View style={[styles.entityAvatar, { backgroundColor: isDarkMode ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)' }]}>
                      <Text style={[styles.entityAvatarText, { color: colors.textDark }]}>{s.name.charAt(0)}</Text>
                    </View>
                    <Text style={[
                      styles.chipText,
                      { color: colors.textGrey },
                      servantId === s.id && styles.entityChipTextActive
                    ]}>{s.name}</Text>
                  </TouchableOpacity>
                ))}
              </ScrollView>
            </>
          )}
        </View>

        <View style={{ height: 100 }} />
      </ScrollView>

      <BlurView intensity={30} tint={isDarkMode ? "dark" : "light"} style={[styles.footer, { borderTopColor: isDarkMode ? 'rgba(255,255,255,0.05)' : 'rgba(0,0,0,0.05)' }]}>
        <TouchableOpacity
          style={[
            styles.saveBtn,
            (isServant && transactions.find(t => t.id === transactionId)?.type === 'transfer') && { opacity: 0.5 }
          ]}
          onPress={handleSave}
          disabled={isServant && transactions.find(t => t.id === transactionId)?.type === 'transfer'}
          {...accessibleButton(transactionId ? 'Save Changes' : 'Add Transaction', 'Submit transaction details')}
        >
          <LinearGradient colors={['#3B82F6', '#1D4ED8']} style={styles.saveBtnGradient}>
            <Text style={styles.saveBtnText}>
              {transactionId ? (isServant && transactions.find(t => t.id === transactionId)?.type === 'transfer' ? 'Locked' : 'Save Changes') : 'Add Transaction'}
            </Text>
            <Ionicons name={transactionId && isServant && transactions.find(t => t.id === transactionId)?.type === 'transfer' ? "lock-closed" : "arrow-forward"} size={20} color="white" style={{ marginLeft: 8 }} />
          </LinearGradient>
        </TouchableOpacity>
      </BlurView>
    </View >
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    paddingTop: Platform.OS === 'ios' ? 60 : 40,
    paddingHorizontal: 16,
    paddingBottom: 16,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  backBtn: {
    width: 44,
    height: 44,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: '900',
    letterSpacing: -0.5,
  },
  deleteBtn: {
    width: 44,
    height: 44,
    borderRadius: 16,
    backgroundColor: 'rgba(239, 68, 68, 0.1)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  scrollContent: {
    padding: 20,
  },
  typeContainer: {
    flexDirection: 'row',
    borderRadius: 20,
    padding: 6,
    marginBottom: 24,
  },
  typeBtn: {
    flex: 1,
    paddingVertical: 10,
    alignItems: 'center',
    borderRadius: 15,
  },
  typeBtnActiveDark: {
    backgroundColor: 'rgba(255,255,255,0.05)',
  },
  typeBtnActiveLight: {
    backgroundColor: 'rgba(255,255,255,1)',
    ...SHADOWS.small,
  },
  typeBtnText: {
    fontSize: 11,
    fontWeight: '900',
    letterSpacing: 1,
  },
  mainCard: {
    borderRadius: 32,
    padding: 24,
    borderWidth: 1,
    overflow: 'hidden',
  },
  inputLabel: {
    fontSize: 10,
    fontWeight: '900',
    letterSpacing: 1,
    marginBottom: 8,
  },
  amountInputRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  amountInput: {
    fontSize: 48,
    fontWeight: '900',
    padding: 0,
  },
  divider: {
    height: 1,
    marginVertical: 24,
  },
  descriptionInput: {
    fontSize: 18,
    fontWeight: '600',
    minHeight: 60,
    textAlignVertical: 'top',
  },
  section: {
    marginTop: 32,
  },
  sectionTitle: {
    fontSize: 12,
    fontWeight: '900',
    letterSpacing: 1,
    marginBottom: 16,
    textTransform: 'uppercase',
  },
  dateSelector: {
    marginBottom: 24,
  },
  dateSelectorBlur: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: 16,
    borderRadius: 20,
    borderWidth: 1,
    overflow: 'hidden',
  },
  dateValue: {
    flex: 1,
    marginLeft: 12,
    fontSize: 16,
    fontWeight: '700',
  },
  subSectionTitle: {
    fontSize: 11,
    fontWeight: '800',
    marginBottom: 12,
    marginTop: 8,
  },
  chipRow: {
    flexDirection: 'row',
    marginBottom: 28,
  },
  categoryChip: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 16,
    borderWidth: 1,
    marginRight: 10,
  },
  entityChip: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 16,
    borderWidth: 1,
    marginRight: 10,
  },
  entityChipActive: {
    backgroundColor: 'rgba(59, 130, 246, 0.1)',
    borderColor: '#3B82F6',
  },
  entityChipTextActive: {
    color: '#3B82F6',
    fontWeight: '800',
  },
  entityAvatar: {
    width: 24,
    height: 24,
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 10,
  },
  entityAvatarText: {
    fontSize: 12,
    fontWeight: '900',
  },
  chipText: {
    fontSize: 14,
    fontWeight: '600',
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    padding: 20,
    paddingBottom: Platform.OS === 'ios' ? 40 : 20,
    borderTopWidth: 1,
  },
  saveBtn: {
    borderRadius: 24,
    overflow: 'hidden',
    ...SHADOWS.premium,
  },
  saveBtnGradient: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: 18,
  },
  saveBtnText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '900',
    letterSpacing: 0.5,
  },
  scanBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: 'rgba(59, 130, 246, 0.2)',
  },
});